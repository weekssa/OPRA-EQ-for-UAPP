package com.weekssa.opraeqforuapp.data.dac

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.junit.Test

class FiioJa11ControlRepositoryTest {
    private fun micRepository(source: FakeSource) =
        FiioJa11ControlRepository(source, allowMicRoundTripFallback = true)

    @Test
    fun nonUserOneDefaultsKeepsVerifiedUnavailableEqComparisonOnMicPath() = runBlocking {
        val builtInSource = FakeSource()
        val builtInRepository = micRepository(builtInSource)
        val builtInReviewed = (builtInRepository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot
        val builtInResult = builtInRepository.setDefaults(
            FiioJa11DefaultsRequest(
                builtInReviewed,
                builtInReviewed.outputVolume,
                FiioJa11Protocol.EqProgram.CLASSIC,
                FiioJa11DeviceControls.EQ_PROGRAM,
            ),
        ) as FiioJa11DefaultsResult.Verified
        assertThat(builtInResult.report.mechanism).isEqualTo(FiioJa11DefaultsMechanism.MIC_RECONNECT)
        assertThat(builtInResult.report.eqReadback).isEqualTo(FiioJa11DefaultsEqReadback.UNAVAILABLE)
        assertThat(builtInSource.headsetControlWrites).containsExactly(false, true).inOrder()
        assertThat(builtInSource.eqBandWrites).isEmpty()
        assertThat(builtInSource.saveWrites).isEqualTo(0)
    }

    @Test
    fun completeReadReturnsOneCurrentSnapshot() = runBlocking {
        val source = FakeSource().apply { eqProgram = FiioJa11Protocol.EqProgram.USER_1 }
        val result = micRepository(source).readSnapshot()

        assertThat(result).isInstanceOf(FiioJa11ControlReadResult.Success::class.java)
        val snapshot = (result as FiioJa11ControlReadResult.Success).snapshot
        assertThat(snapshot.sessionGeneration).isEqualTo(7L)
        assertThat(snapshot.firmwareVersion).isEqualTo("2.20")
        assertThat(snapshot.outputVolume).isEqualTo(59)
        assertThat(snapshot.eqProgram).isEqualTo(FiioJa11Protocol.EqProgram.USER_1)
        assertThat(snapshot.uacMode).isEqualTo(FiioJa11Protocol.UacMode.UAC_2)
    }

    @Test
    fun outputVolumeUsesFreshBaselineTargetedWriteAndVerifiedReadback() = runBlocking {
        val source = FakeSource().apply { eqProgram = FiioJa11Protocol.EqProgram.USER_1 }
        val repository = micRepository(source)

        val result = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Numeric(58.0),
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.volumeWrites).containsExactly(58)
        assertThat(source.outputVolume).isEqualTo(58)
        assertThat(source.eqProgram).isEqualTo(FiioJa11Protocol.EqProgram.USER_1)
        assertThat(source.uacMode).isEqualTo(FiioJa11Protocol.UacMode.UAC_2)
    }

    @Test
    fun confirmedChangedVolumeDefaultsWritesOnceAndRestoresMicOnAcrossTwoReplacements() = runBlocking {
        val source = FakeSource().apply { headsetControlEnabled = true }
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(
                reviewedSnapshot = reviewed,
                requestedVolume = 58,
                requestedProgram = reviewed.eqProgram,
                targetControlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Verified::class.java)
        val verified = result as FiioJa11DefaultsResult.Verified
        assertThat(verified.snapshot.outputVolume).isEqualTo(58)
        assertThat(verified.snapshot.eqProgram).isEqualTo(reviewed.eqProgram)
        assertThat(verified.snapshot.headsetControlEnabled).isTrue()
        assertThat(verified.report.counts).isEqualTo(
            FiioJa11DefaultsWriteCounts(volumeWrites = 1, micWrites = 2),
        )
        assertThat(source.volumeWrites).containsExactly(58)
        assertThat(source.headsetControlWrites).containsExactly(false, true).inOrder()
        assertThat(source.writeEvents).containsExactly("volume:58", "mic:false", "mic:true").inOrder()
        assertThat(source.replacementWaits).isEqualTo(2)
        assertThat(source.uacMode).isEqualTo(reviewed.uacMode)
        assertThat(source.eqProgram).isEqualTo(reviewed.eqProgram)
    }

    @Test
    fun confirmedUnchangedProgramDefaultsSkipsTargetWriteAndRestoresMicOff() = runBlocking {
        val source = FakeSource().apply {
            headsetControlEnabled = false
            eqProgram = FiioJa11Protocol.EqProgram.CLASSIC
        }
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(
                reviewedSnapshot = reviewed,
                requestedVolume = reviewed.outputVolume,
                requestedProgram = reviewed.eqProgram,
                targetControlId = FiioJa11DeviceControls.EQ_PROGRAM,
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Verified::class.java)
        val verified = result as FiioJa11DefaultsResult.Verified
        assertThat(verified.report.counts).isEqualTo(FiioJa11DefaultsWriteCounts(micWrites = 2))
        assertThat(source.volumeWrites).isEmpty()
        assertThat(source.programWrites).isEmpty()
        assertThat(source.headsetControlWrites).containsExactly(true, false).inOrder()
        assertThat(verified.snapshot.headsetControlEnabled).isFalse()
    }

    @Test
    fun changedProgramDefaultsWritesTargetOnceAndPreservesRuntimeVolume() = runBlocking {
        val source = FakeSource().apply { eqProgram = FiioJa11Protocol.EqProgram.USER_1 }
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(
                reviewedSnapshot = reviewed,
                requestedVolume = reviewed.outputVolume,
                requestedProgram = FiioJa11Protocol.EqProgram.CLASSIC,
                targetControlId = FiioJa11DeviceControls.EQ_PROGRAM,
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Verified::class.java)
        val verified = result as FiioJa11DefaultsResult.Verified
        assertThat(verified.report.counts).isEqualTo(FiioJa11DefaultsWriteCounts(programWrites = 1, micWrites = 2))
        assertThat(source.programWrites).containsExactly(FiioJa11Protocol.EqProgram.CLASSIC)
        assertThat(source.volumeWrites).isEmpty()
        assertThat(verified.snapshot.outputVolume).isEqualTo(reviewed.outputVolume)
    }

    @Test
    fun secondReconnectFailureDoesNotReplayMicOrClaimRestoration() = runBlocking {
        val source = FakeSource(replacementReadyThroughWait = 1)
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(
                reviewedSnapshot = reviewed,
                requestedVolume = reviewed.outputVolume,
                requestedProgram = reviewed.eqProgram,
                targetControlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        val failed = result as FiioJa11DefaultsResult.Failed
        assertThat(failed.report.stage).isEqualTo(FiioJa11DefaultsStage.SECOND_REPLACEMENT)
        assertThat(failed.report.counts).isEqualTo(FiioJa11DefaultsWriteCounts(micWrites = 2))
        assertThat(source.headsetControlWrites).containsExactly(false, true).inOrder()
        assertThat(source.replacementWaits).isEqualTo(2)
    }

    @Test
    fun nonUserOneProgramsAndBothVolumeLimitsUseAtMostOneTargetWriteOnMicPath() = runBlocking {
        for (program in FiioJa11Protocol.EqProgram.entries.filter { it != FiioJa11Protocol.EqProgram.USER_1 }) {
            val source = FakeSource().apply { eqProgram = FiioJa11Protocol.EqProgram.USER_1 }
            val repository = micRepository(source)
            val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot
            val result = repository.setDefaults(
                FiioJa11DefaultsRequest(reviewed, reviewed.outputVolume, program, FiioJa11DeviceControls.EQ_PROGRAM),
            )
            assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Verified::class.java)
            assertThat(source.programWrites.size).isAtMost(1)
            assertThat(source.eqProgram).isEqualTo(program)
            assertThat(source.headsetControlWrites).hasSize(2)
            assertThat(source.eqBandWrites).isEmpty()
            assertThat(source.eqGlobalGainWrites).isEmpty()
            assertThat(source.applyWrites).isEqualTo(0)
            assertThat(source.saveWrites).isEqualTo(0)
            assertThat(source.writeEvents.first()).isEqualTo("program:${program.name}")
            assertThat((result as FiioJa11DefaultsResult.Verified).report.mechanism)
                .isEqualTo(FiioJa11DefaultsMechanism.MIC_RECONNECT)
        }
        for (level in listOf(FiioJa11Protocol.MIN_OUTPUT_VOLUME, FiioJa11Protocol.MAX_OUTPUT_VOLUME)) {
            val source = FakeSource()
            val repository = micRepository(source)
            val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot
            val result = repository.setDefaults(
                FiioJa11DefaultsRequest(reviewed, level, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
            )
            assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Verified::class.java)
            assertThat(source.volumeWrites).containsExactly(level)
        }
    }

    @Test
    fun twoConfirmedDefaultsOperationsSerializeAndRejectTheStaleSecondReview() = runBlocking {
        val source = FakeSource()
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot
        val first = async {
            repository.setDefaults(FiioJa11DefaultsRequest(reviewed, 58, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME))
        }
        val second = async {
            repository.setDefaults(
                FiioJa11DefaultsRequest(reviewed, reviewed.outputVolume, FiioJa11Protocol.EqProgram.CLASSIC, FiioJa11DeviceControls.EQ_PROGRAM),
            )
        }
        val results = listOf(first.await(), second.await())
        assertThat(results.count { it is FiioJa11DefaultsResult.Verified }).isEqualTo(1)
        assertThat(results.count { it is FiioJa11DefaultsResult.ReviewChanged }).isEqualTo(1)
        assertThat(source.volumeWrites).containsExactly(58)
        assertThat(source.programWrites).isEmpty()
        assertThat(source.headsetControlWrites).hasSize(2)
    }

    @Test
    fun targetReadbackMismatchStopsBeforeMicRoundTripWithoutRetry() = runBlocking {
        val source = FakeSource(ignoreVolumeWrite = true)
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 58, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage).isEqualTo(FiioJa11DefaultsStage.TARGET_VERIFY)
        assertThat(source.volumeWrites).containsExactly(58)
        assertThat(source.headsetControlWrites).isEmpty()
    }

    @Test
    fun rejectedTargetWriteIsNeverRetriedAndDoesNotStartMicRoundTrip() = runBlocking {
        val source = FakeSource(rejectVolumeWrite = true)
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 58, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        val failed = result as FiioJa11DefaultsResult.Failed
        assertThat(failed.report.stage).isEqualTo(FiioJa11DefaultsStage.TARGET_WRITE)
        assertThat(failed.report.counts.volumeWrites).isEqualTo(1)
        assertThat(source.volumeWrites).containsExactly(58)
        assertThat(source.headsetControlWrites).isEmpty()
    }

    @Test
    fun mismatchingReplacementIdentityStopsWithoutRetryingMicWrite() = runBlocking {
        val source = FakeSource(mismatchingSerialAfterFirstReplacement = true).apply {
            connectedDeviceSerial = "ja11-original"
        }
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, reviewed.outputVolume, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage).isEqualTo(FiioJa11DefaultsStage.FIRST_REPLACEMENT)
        assertThat(source.headsetControlWrites).containsExactly(false)
        assertThat(source.replacementWaits).isEqualTo(1)
    }

    @Test
    fun changedCompanionOnReplacementStopsBeforeMicRestore() = runBlocking {
        val source = FakeSource(changeFirmwareAfterFirstReplacement = true)
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, reviewed.outputVolume, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage).isEqualTo(FiioJa11DefaultsStage.FIRST_REPLACEMENT)
        assertThat(source.headsetControlWrites).containsExactly(false)
        assertThat(source.replacementWaits).isEqualTo(1)
        assertThat(source.firmware).isNotEqualTo(reviewed.firmwareVersion)
    }

    @Test
    fun rejectedMicTransitionIsNeverRetried() = runBlocking {
        val source = FakeSource(rejectMicWriteNumber = 1)
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, reviewed.outputVolume, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage).isEqualTo(FiioJa11DefaultsStage.MIC_TO_OPPOSITE)
        assertThat(source.headsetControlWrites).containsExactly(false)
        assertThat(source.replacementWaits).isEqualTo(0)
    }

    @Test
    fun failedMicRestoreIsNeverRetried() = runBlocking {
        val source = FakeSource(rejectMicWriteNumber = 2)
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, reviewed.outputVolume, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage).isEqualTo(FiioJa11DefaultsStage.MIC_RESTORE)
        assertThat(source.headsetControlWrites).containsExactly(false, true).inOrder()
        assertThat(source.replacementWaits).isEqualTo(1)
    }

    @Test
    fun finalReadbackMismatchStopsWithoutAnotherMutation() = runBlocking {
        val source = FakeSource(changeVolumeAfterSecondReplacement = true)
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 58, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage).isEqualTo(FiioJa11DefaultsStage.FINAL_VERIFY)
        assertThat(source.volumeWrites).containsExactly(58)
        assertThat(source.headsetControlWrites).containsExactly(false, true).inOrder()
        assertThat(source.replacementWaits).isEqualTo(2)
    }

    @Test
    fun secondReplacementIdentityMismatchStopsAfterOneRestoreWrite() = runBlocking {
        val source = FakeSource(mismatchingSerialAfterSecondReplacement = true).apply {
            connectedDeviceSerial = "ja11-original"
        }
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, reviewed.outputVolume, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage).isEqualTo(FiioJa11DefaultsStage.SECOND_REPLACEMENT)
        assertThat(source.headsetControlWrites).containsExactly(false, true).inOrder()
        assertThat(source.replacementWaits).isEqualTo(2)
    }

    @Test
    fun ambiguousSecondReplacementStopsAfterMicRestoreWithoutRetry() = runBlocking {
        val source = FakeSource(ambiguousCandidateCountAfterSecondReplacement = true)
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, reviewed.outputVolume, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage).isEqualTo(FiioJa11DefaultsStage.SECOND_REPLACEMENT)
        assertThat(source.headsetControlWrites).containsExactly(false, true).inOrder()
        assertThat(source.replacementWaits).isEqualTo(2)
    }

    @Test
    fun replacementReadFailuresStopOnTheAffectedLegWithoutMutationReplay() = runBlocking {
        for (failedLeg in 1..2) {
            val source = FakeSource(failVolumeReadAfterReplacement = failedLeg)
            val repository = micRepository(source)
            val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

            val result = repository.setDefaults(
                FiioJa11DefaultsRequest(reviewed, 58, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
            )

            assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
            val failed = result as FiioJa11DefaultsResult.Failed
            assertThat(failed.report.stage).isEqualTo(
                if (failedLeg == 1) FiioJa11DefaultsStage.FIRST_REPLACEMENT else FiioJa11DefaultsStage.SECOND_REPLACEMENT,
            )
            assertThat(source.volumeWrites).containsExactly(58)
            assertThat(source.headsetControlWrites).hasSize(failedLeg)
            assertThat(source.replacementWaits).isEqualTo(failedLeg)
        }
    }

    @Test
    fun finalMicReadbackMismatchStopsWithoutRetrying() = runBlocking {
        val source = FakeSource(misreportMicAfterSecondReplacement = true)
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, reviewed.outputVolume, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage).isEqualTo(FiioJa11DefaultsStage.FINAL_VERIFY)
        assertThat(source.headsetControlWrites).containsExactly(false, true).inOrder()
        assertThat(source.replacementWaits).isEqualTo(2)
    }

    @Test
    fun finalUacFirmwareAndPidDriftAreEachReportedWithoutFurtherWrites() = runBlocking {
        for (changedCompanion in listOf("uac", "firmware", "pid")) {
            val source = FakeSource(changeCompanionAfterSecondReplacement = changedCompanion)
            val repository = micRepository(source)
            val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

            val result = repository.setDefaults(
                FiioJa11DefaultsRequest(reviewed, reviewed.outputVolume, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
            )

            assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
            assertThat(source.headsetControlWrites).containsExactly(false, true).inOrder()
            assertThat(source.replacementWaits).isEqualTo(2)
        }
    }

    @Test
    fun reviewedCompanionDriftStopsBeforeAnyWrite() = runBlocking {
        val source = FakeSource()
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot
        source.eqProgram = FiioJa11Protocol.EqProgram.OFF

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(
                reviewedSnapshot = reviewed,
                requestedVolume = 58,
                requestedProgram = reviewed.eqProgram,
                targetControlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.ReviewChanged::class.java)
        assertThat(source.volumeWrites).isEmpty()
        assertThat(source.headsetControlWrites).isEmpty()
    }

    @Test
    fun firstReconnectFailureStopsWithoutRetryOrMicFallback() = runBlocking {
        val source = FakeSource(replacementReady = false)
        val repository = micRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(
                reviewedSnapshot = reviewed,
                requestedVolume = 58,
                requestedProgram = reviewed.eqProgram,
                targetControlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        val failed = result as FiioJa11DefaultsResult.Failed
        assertThat(failed.report.stage).isEqualTo(FiioJa11DefaultsStage.FIRST_REPLACEMENT)
        assertThat(failed.report.counts).isEqualTo(
            FiioJa11DefaultsWriteCounts(volumeWrites = 1, micWrites = 1),
        )
        assertThat(source.volumeWrites).containsExactly(58)
        assertThat(source.headsetControlWrites).containsExactly(false)
        assertThat(source.replacementWaits).isEqualTo(1)
    }

    @Test
    fun staleExpectedGenerationCannotWrite() = runBlocking {
        val source = FakeSource()
        val result = micRepository(source).writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                expectedSessionGeneration = 6L,
                requestedValue = DacControlValue.Numeric(58.0),
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.StaleBaseline::class.java)
        assertThat(source.volumeWrites).isEmpty()
    }

    @Test
    fun sameSessionReadbackMismatchNeverReportsSuccess() = runBlocking {
        val source = FakeSource(ignoreVolumeWrite = true)
        val result = micRepository(source).writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Numeric(58.0),
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.ReadbackMismatch::class.java)
    }

    @Test
    fun headsetWriteRequiresAReplacementSessionBeforeVerification() = runBlocking {
        val source = FakeSource(replacementReady = false)
        val repository = micRepository(source)

        val first = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        )

        assertThat(first).isInstanceOf(FiioJa11ControlWriteResult.ReconnectRequired::class.java)
        val pending = (first as FiioJa11ControlWriteResult.ReconnectRequired).pending
        assertThat(source.headsetControlEnabled).isFalse()

        val sameSession = repository.verifyRestartedControl(pending)
        assertThat(sameSession).isInstanceOf(FiioJa11ControlWriteResult.StaleBaseline::class.java)

        source.sessionGeneration = 8L
        source.detachGeneration = 1L
        source.replacementReady = true
        val verified = repository.verifyRestartedControl(pending)
        assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
    }

    @Test
    fun uacWriteIsVerifiedOnlyAfterNewPidAndNewSessionAgree() = runBlocking {
        val source = FakeSource()
        val repository = micRepository(source)
        val first = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.UAC_MODE,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Discrete("uac_1"),
            ),
        )
        val pending = (first as FiioJa11ControlWriteResult.ReconnectRequired).pending

        source.sessionGeneration = 8L
        source.detachGeneration = 1L
        source.productId = FiioJa11Protocol.PRODUCT_ID_UAC_1
        val verified = repository.verifyRestartedControl(pending)

        assertThat(verified).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.uacMode).isEqualTo(FiioJa11Protocol.UacMode.UAC_1)
    }

    @Test
    fun seriallessReplacementWithExpectedDetachIsAccepted() = runBlocking {
        val source = FakeSource()
        val repository = micRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        source.sessionGeneration = 8L
        source.detachGeneration = 1L
        source.connectedDeviceSerial = null
        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
    }

    @Test
    fun matchingOptionalSerialReplacementIsAccepted() = runBlocking {
        val source = FakeSource().apply { connectedDeviceSerial = "JA11-A" }
        val repository = micRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
    }

    @Test
    fun mismatchingAvailableSerialFailsClosedAfterExpectedDetach() = runBlocking {
        val source = FakeSource().apply { connectedDeviceSerial = "JA11-A" }
        val repository = micRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        source.sessionGeneration = 8L
        source.detachGeneration = 1L
        source.connectedDeviceSerial = "JA11-B"
        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.StaleBaseline::class.java)
    }

    @Test
    fun multipleSupportedCandidatesBlockExpectedRestartReadback() = runBlocking {
        val source = FakeSource(candidateCount = 2)
        val result = micRepository(source).writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.NotConnected::class.java)
        assertThat(source.headsetControlWrites).isEmpty()
    }

    @Test
    fun delayedPermissionReplacementIsWaitedForOnceWithoutReplayingWrite() = runBlocking {
        val source = FakeSource(permissionDelayMillis = 20L)
        val repository = micRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.headsetControlWrites).containsExactly(false)
        assertThat(source.replacementWaits).isEqualTo(1)
    }

    @Test
    fun replacementTimeoutDoesNotVerifyOrReplayTheExpectedResetWrite() = runBlocking {
        val source = FakeSource(replacementReady = false)
        val repository = micRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        val result = repository.verifyRestartedControl(pending)

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.StaleBaseline::class.java)
        assertThat(source.headsetControlWrites).containsExactly(false)
        Unit
    }

    @Test
    fun cancelledReplacementWaitDoesNotReplayTheExpectedResetWrite() = runBlocking {
        val source = FakeSource(permissionDelayMillis = 500L)
        val repository = micRepository(source)
        val pending = repository.writeControl(
            DacWriteIntent(
                controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
                expectedSessionGeneration = 7L,
                requestedValue = DacControlValue.Toggle(false),
            ),
        ).let { (it as FiioJa11ControlWriteResult.ReconnectRequired).pending }

        try {
            withTimeout(10L) { repository.verifyRestartedControl(pending) }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            // The caller cancelled while Android permission/reconnect was still pending.
        }

        assertThat(source.headsetControlWrites).containsExactly(false)
        assertThat(source.replacementWaits).isEqualTo(1)
    }

    @Test
    fun readFailsClosedWhenReportedUacModeDisagreesWithUsbIdentity() = runBlocking {
        val source = FakeSource().apply {
            productId = FiioJa11Protocol.PRODUCT_ID_UAC_1
            uacMode = FiioJa11Protocol.UacMode.UAC_2
        }

        val result = micRepository(source).readSnapshot()

        assertThat(result).isEqualTo(FiioJa11ControlReadResult.ReadFailed("UAC mode / USB identity"))
    }

    private class FakeSource(
        private val ignoreVolumeWrite: Boolean = false,
        private val rejectVolumeWrite: Boolean = false,
        private val candidateCount: Int = 1,
        private val permissionDelayMillis: Long = 0L,
        var replacementReady: Boolean = true,
        private val replacementReadyThroughWait: Int? = null,
        private val mismatchingSerialAfterFirstReplacement: Boolean = false,
        private val mismatchingSerialAfterSecondReplacement: Boolean = false,
        private val changeFirmwareAfterFirstReplacement: Boolean = false,
        private val rejectMicWriteNumber: Int? = null,
        private val changeVolumeAfterSecondReplacement: Boolean = false,
        private val ambiguousCandidateCountAfterSecondReplacement: Boolean = false,
        private val failVolumeReadAfterReplacement: Int? = null,
        private val misreportMicAfterSecondReplacement: Boolean = false,
        private val changeCompanionAfterSecondReplacement: String? = null,
        private val rejectBandWriteAt: Int? = null,
        private val rejectGainWrite: Boolean = false,
        private val rejectApply: Boolean = false,
        private val saveSucceeds: Boolean = true,
        private val changeFirmwareOnSave: Boolean = false,
        private val finalReconnectReady: Boolean = true,
        private val replaceSessionOnEqSnapshotRead: Int? = null,
        private val replaceSessionAtBandWrite: Int? = null,
        private val replaceSessionAfterApply: Boolean = false,
    ) : FiioJa11DeviceControlSource {
        override var sessionGeneration: Long = 7L
        override var detachGeneration: Long = 0L
        override var connectedDeviceSerial: String? = null
        override val supportedCandidateCount: Int
            get() = if (replacementWaits >= 2 && ambiguousCandidateCountAfterSecondReplacement) 2 else candidateCount
        var productId: Int = FiioJa11Protocol.PRODUCT_ID_UAC_2
        var current = true
        var outputVolume = 59
        var sampleRate = "352.8 kHz"
        var firmware = "2.20"
        var headsetControlEnabled = true
        var eqProgram = FiioJa11Protocol.EqProgram.CLASSIC
        var uacMode = FiioJa11Protocol.UacMode.UAC_2
        val volumeWrites = mutableListOf<Int>()
        val programWrites = mutableListOf<FiioJa11Protocol.EqProgram>()
        val headsetControlWrites = mutableListOf<Boolean>()
        val writeEvents = mutableListOf<String>()
        val eqBandWrites = mutableListOf<Pair<Int, FiioJa11Protocol.Band>>()
        val eqGlobalGainWrites = mutableListOf<Double>()
        var applyWrites = 0
        var saveWrites = 0
        val saveEntryGenerations = mutableListOf<Long>()
        var finalReconnectWaits = 0
        var bandWriteGate: CompletableDeferred<Unit>? = null
        val bandWriteEntered = CompletableDeferred<Unit>()
        private var eqSnapshotReads = 0
        var replacementWaits = 0
        var eqSnapshots: MutableList<HardwareEqSnapshotBundle?> = mutableListOf()

        override val connectedProductId: Int?
            get() = productId

        override fun isSessionCurrent(sessionGeneration: Long): Boolean =
            current && supportedCandidateCount == 1 && sessionGeneration == this.sessionGeneration

        override suspend fun awaitExpectedReplacementSession(
            previousGeneration: Long,
            previousDetachGeneration: Long,
            previousSerial: String?,
            timeoutMillis: Long,
        ): Boolean {
            replacementWaits += 1
            if (!replacementReady) return false
            if (replacementReadyThroughWait != null && replacementWaits > replacementReadyThroughWait) return false
            if (permissionDelayMillis > 0L) delay(permissionDelayMillis)
            if (sessionGeneration == previousGeneration) sessionGeneration += 1L
            if (detachGeneration == previousDetachGeneration) detachGeneration += 1L
            if ((replacementWaits == 1 && mismatchingSerialAfterFirstReplacement) ||
                (replacementWaits == 2 && mismatchingSerialAfterSecondReplacement)
            ) {
                connectedDeviceSerial = "different-ja11"
            }
            if (replacementWaits == 1 && changeFirmwareAfterFirstReplacement) firmware = "unexpected"
            if (replacementWaits == 2 && changeVolumeAfterSecondReplacement) outputVolume = 57
            if (replacementWaits == 2) {
                when (changeCompanionAfterSecondReplacement) {
                    "uac" -> uacMode = FiioJa11Protocol.UacMode.UAC_1
                    "firmware" -> firmware = "unexpected"
                    "pid" -> productId = FiioJa11Protocol.PRODUCT_ID_UAC_1
                }
            }
            val expectedSerial = previousSerial?.trim()?.takeIf(String::isNotEmpty)
            val replacementSerial = connectedDeviceSerial?.trim()?.takeIf(String::isNotEmpty)
            return supportedCandidateCount == 1 &&
                (expectedSerial == null || replacementSerial == null || expectedSerial == replacementSerial)
        }

        override suspend fun readOutputVolume(): Int? =
            if (replacementWaits == failVolumeReadAfterReplacement) null else outputVolume
        override suspend fun readSampleRateLabel(): String? = sampleRate
        override suspend fun readFirmwareVersion(): String? = firmware
        override suspend fun readHeadsetControlEnabled(): Boolean? =
            if (replacementWaits >= 2 && misreportMicAfterSecondReplacement) !headsetControlEnabled else headsetControlEnabled
        override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? = eqProgram
        override suspend fun readEqSnapshot(): HardwareEqSnapshotBundle? {
            val readIndex = eqSnapshotReads++
            if (replaceSessionOnEqSnapshotRead == readIndex) {
                sessionGeneration += 1L
                detachGeneration += 1L
            }
            return eqSnapshots.removeFirstOrNull()
        }
        override suspend fun readUacMode(): FiioJa11Protocol.UacMode? = uacMode

        override suspend fun writeOutputVolume(level: Int): Boolean {
            writeEvents += "volume:$level"
            volumeWrites += level
            if (rejectVolumeWrite) return false
            if (!ignoreVolumeWrite) outputVolume = level
            return true
        }

        override suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean {
            writeEvents += "mic:$enabled"
            headsetControlWrites += enabled
            if (headsetControlWrites.size == rejectMicWriteNumber) return false
            headsetControlEnabled = enabled
            return true
        }

        override suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean {
            writeEvents += "program:${program.name}"
            programWrites += program
            eqProgram = program
            return true
        }

        override suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean {
            writeEvents += "uac:${mode.name}"
            uacMode = mode
            return true
        }

        override suspend fun writeEqBand(index: Int, band: FiioJa11Protocol.Band): Boolean {
            writeEvents += "band:$index"
            eqBandWrites += index to band
            bandWriteEntered.complete(Unit)
            bandWriteGate?.await()
            if (replaceSessionAtBandWrite == index) {
                sessionGeneration += 1L
                detachGeneration += 1L
            }
            return rejectBandWriteAt != index
        }

        override suspend fun writeEqGlobalGain(gainDb: Double): Boolean {
            writeEvents += "gain:$gainDb"
            eqGlobalGainWrites += gainDb
            return !rejectGainWrite
        }

        override suspend fun applyEqChanges(): Boolean {
            writeEvents += "apply"
            applyWrites += 1
            if (replaceSessionAfterApply) {
                sessionGeneration += 1L
                detachGeneration += 1L
            }
            return !rejectApply
        }

        override suspend fun saveEqToFlash(): Boolean {
            writeEvents += "save"
            saveWrites += 1
            saveEntryGenerations += sessionGeneration
            if (!saveSucceeds) return false
            sessionGeneration += 1L
            detachGeneration += 1L
            if (changeFirmwareOnSave) firmware = "unexpected"
            return true
        }

        override suspend fun awaitFinalReadbackReconnect(): Boolean {
            finalReconnectWaits += 1
            if (!finalReconnectReady) return false
            sessionGeneration += 1L
            detachGeneration += 1L
            return true
        }
    }
}
