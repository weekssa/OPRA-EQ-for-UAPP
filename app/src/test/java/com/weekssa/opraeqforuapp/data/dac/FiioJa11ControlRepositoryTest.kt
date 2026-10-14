package com.weekssa.opraeqforuapp.data.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol.EqProgram
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class FiioJa11ControlRepositoryTest {
    @Test
    fun settingsSaveWritesTargetOnceThenSavesVerifiedActiveProgram() = runBlocking {
        val delegate = FakeSource()
        val source = FakePersistenceSource(delegate)
        val result = FiioJa11ControlRepository(source, sourceCommit = "candidate-sha").writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )

        assertThat(result.outcome).isEqualTo("SUCCESS")
        assertThat(delegate.volumeWrites).containsExactly(58)
        assertThat(source.savePrograms).containsExactly(EqProgram.USER_1)
        assertThat(result.targetedWriteCount).isEqualTo(1)
        assertThat(result.saveCount).isEqualTo(1)
        assertThat(result.finalState?.outputVolume).isEqualTo(58)
        assertThat(result.eqComparison).isEqualTo("USER_1_STORED_BANK_VERIFIED")
        assertThat(result.sourceCommit).isEqualTo("candidate-sha")
        assertThat(Json.parseToJsonElement(result.toJson()).jsonObject["softwareRecordedSaveCount"]?.jsonPrimitive?.int).isEqualTo(1)
        Unit
    }

    @Test
    fun programSaveUsesTheNewlyVerifiedProgramAndDoesNotClaimItsStoredBank() = runBlocking {
        val source = FakePersistenceSource(FakeSource())
        val result = FiioJa11ControlRepository(source).writeControlAndSave(
            DacWriteIntent(
                FiioJa11DeviceControls.EQ_PROGRAM,
                DacControlValue.Discrete("vocal"),
                7L,
            ),
        )

        assertThat(result.outcome).isEqualTo("SUCCESS")
        assertThat(source.savePrograms).containsExactly(EqProgram.VOCAL)
        assertThat(result.requestedProgram).isEqualTo("VOCAL")
        assertThat(result.eqComparison).isEqualTo("SAME_PROGRAM_RAW_VERIFIED")
        assertThat(result.targetedWriteCount).isEqualTo(1)
        assertThat(result.saveCount).isEqualTo(1)
        Unit
    }

    @Test
    fun lateDetachDuringFinalEqReadUsesOneReadOnlyRecoveryWithoutReplayingMutations() = runBlocking {
        val delegate = FakeSource()
        val source = FakePersistenceSource(delegate, detachDuringFinalEqRead = true)

        val result = FiioJa11ControlRepository(source).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )

        assertThat(result.outcome).isEqualTo("SUCCESS")
        assertThat(result.boundedReadOnlyRetryCount).isEqualTo(1)
        assertThat(source.reconnectWaits).isEqualTo(1)
        assertThat(delegate.volumeWrites).containsExactly(58)
        assertThat(source.savePrograms).containsExactly(EqProgram.USER_1)
        assertThat(result.targetedWriteCount).isEqualTo(1)
        assertThat(result.saveCount).isEqualTo(1)
        assertThat(result.sessionGenerations).contains(8L)
        Unit
    }

    @Test
    fun secondFinalReadDetachFailsClosedAfterOnlyOneRecovery() = runBlocking {
        val delegate = FakeSource()
        val source = FakePersistenceSource(delegate, detachOnEqReads = setOf(3, 4))

        val result = FiioJa11ControlRepository(source).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )

        assertThat(result.outcome).isEqualTo("FINAL_EQ_READ_FAILED")
        assertThat(result.boundedReadOnlyRetryCount).isEqualTo(1)
        assertThat(source.reconnectWaits).isEqualTo(1)
        assertThat(delegate.volumeWrites).containsExactly(58)
        assertThat(source.savePrograms).containsExactly(EqProgram.USER_1)
        assertThat(result.targetedWriteCount).isEqualTo(1)
        assertThat(result.saveCount).isEqualTo(1)
        Unit
    }

    @Test
    fun rejectedLateReadOnlyRecoveryDoesNotReplayMutationOrSave() = runBlocking {
        val delegate = FakeSource()
        val source = FakePersistenceSource(
            delegate,
            detachDuringFinalEqRead = true,
            reconnectSucceeds = false,
        )

        val result = FiioJa11ControlRepository(source).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )

        assertThat(result.outcome).isEqualTo("FINAL_EQ_READ_FAILED")
        assertThat(result.boundedReadOnlyRetryCount).isEqualTo(1)
        assertThat(source.reconnectWaits).isEqualTo(1)
        assertThat(delegate.volumeWrites).containsExactly(58)
        assertThat(source.savePrograms).containsExactly(EqProgram.USER_1)
        assertThat(result.targetedWriteCount).isEqualTo(1)
        assertThat(result.saveCount).isEqualTo(1)
        Unit
    }

    @Test
    fun mismatchedReplacementIdentityStopsFinalReadbackWithoutReplayingMutations() = runBlocking {
        val delegate = FakeSource()
        val source = FakePersistenceSource(
            delegate,
            detachDuringFinalEqRead = true,
            replacementMatchesIdentity = false,
        )

        val result = FiioJa11ControlRepository(source).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )

        assertThat(result.outcome).isEqualTo("FINAL_EQ_READ_FAILED")
        assertThat(result.boundedReadOnlyRetryCount).isEqualTo(1)
        assertThat(source.reconnectWaits).isEqualTo(1)
        assertThat(delegate.volumeWrites).containsExactly(58)
        assertThat(source.savePrograms).containsExactly(EqProgram.USER_1)
        assertThat(result.targetedWriteCount).isEqualTo(1)
        assertThat(result.saveCount).isEqualTo(1)
        Unit
    }

    @Test
    fun staleOrAmbiguousBaselineStopsBeforeAnyMutation() = runBlocking {
        val staleDelegate = FakeSource()
        val staleSource = FakePersistenceSource(staleDelegate)
        val stale = FiioJa11ControlRepository(staleSource).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 6L),
        )
        assertThat(stale.outcome).isEqualTo("STALE_BASELINE")
        assertThat(staleDelegate.volumeWrites).isEmpty()
        assertThat(staleSource.savePrograms).isEmpty()

        val ambiguousDelegate = FakeSource(candidateCount = 2)
        val ambiguousSource = FakePersistenceSource(ambiguousDelegate)
        val ambiguous = FiioJa11ControlRepository(ambiguousSource).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )
        assertThat(ambiguous.outcome).isEqualTo("NOT_CONNECTED")
        assertThat(ambiguousDelegate.volumeWrites).isEmpty()
        assertThat(ambiguousSource.savePrograms).isEmpty()
        Unit
    }

    @Test
    fun postSaveTargetOrCompanionDriftFailsWithoutMutationReplay() = runBlocking {
        val targetDelegate = FakeSource()
        val targetSource = FakePersistenceSource(targetDelegate, mutateVolumeOnSave = 57)
        val targetMismatch = FiioJa11ControlRepository(targetSource).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )
        assertThat(targetMismatch.outcome).isEqualTo("FINAL_TARGET_MISMATCH")
        assertThat(targetDelegate.volumeWrites).containsExactly(58)
        assertThat(targetSource.savePrograms).containsExactly(EqProgram.USER_1)
        assertThat(targetMismatch.targetedWriteCount).isEqualTo(1)
        assertThat(targetMismatch.saveCount).isEqualTo(1)

        val companionDelegate = FakeSource()
        val companionSource = FakePersistenceSource(companionDelegate, mutateMicOnSave = false)
        val companionMismatch = FiioJa11ControlRepository(companionSource).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )
        assertThat(companionMismatch.outcome).isEqualTo("FINAL_COMPANION_MISMATCH")
        assertThat(companionDelegate.volumeWrites).containsExactly(58)
        assertThat(companionSource.savePrograms).containsExactly(EqProgram.USER_1)
        assertThat(companionMismatch.targetedWriteCount).isEqualTo(1)
        assertThat(companionMismatch.saveCount).isEqualTo(1)
        Unit
    }

    @Test
    fun bothSupportedSaveActionsWorkForEveryProgramAndUacMode() = runBlocking {
        for (program in EqProgram.entries) {
            for (uacMode in FiioJa11Protocol.UacMode.entries) {
                val volumeDelegate = FakeSource().apply {
                    eqProgram = program
                    this.uacMode = uacMode
                    this.productId = uacMode.productId
                }
                val volumeSource = FakePersistenceSource(volumeDelegate)
                val volumeReport = FiioJa11ControlRepository(volumeSource).writeControlAndSave(
                    DacWriteIntent(
                        controlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                        requestedValue = DacControlValue.Numeric(58.0),
                        expectedSessionGeneration = 7L,
                    ),
                )
                assertThat(volumeReport.outcome).isEqualTo("SUCCESS")
                assertThat(volumeSource.savePrograms).containsExactly(program)
                assertThat(volumeDelegate.volumeWrites).containsExactly(58)

                val programDelegate = FakeSource().apply {
                    eqProgram = program
                    this.uacMode = uacMode
                    this.productId = uacMode.productId
                }
                val programSource = FakePersistenceSource(programDelegate)
                val requestedProgram = EqProgram.entries[(program.ordinal + 1) % EqProgram.entries.size]
                val programReport = FiioJa11ControlRepository(programSource).writeControlAndSave(
                    DacWriteIntent(
                        controlId = FiioJa11DeviceControls.EQ_PROGRAM,
                        requestedValue = DacControlValue.Discrete(requestedProgram.name.lowercase()),
                        expectedSessionGeneration = 7L,
                    ),
                )
                assertThat(programReport.outcome).isEqualTo("SUCCESS")
                assertThat(programSource.savePrograms).containsExactly(requestedProgram)
                assertThat(programReport.targetedWriteCount).isEqualTo(1)
                assertThat(programReport.saveCount).isEqualTo(1)
            }
        }
        Unit
    }

    @Test
    fun missingPersistenceCapabilityFailsBeforeAnySettingMutation() = runBlocking {
        val source = FakeSource()
        val result = FiioJa11ControlRepository(source).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )

        assertThat(result.outcome).isEqualTo("UNSUPPORTED")
        assertThat(result.targetedWriteCount).isEqualTo(0)
        assertThat(result.saveCount).isEqualTo(0)
        assertThat(source.volumeWrites).isEmpty()
        Unit
    }

    @Test
    fun builtInRawParametersMayRemainUnknownWithoutInventingUserOneBankEvidence() = runBlocking {
        val delegate = FakeSource().apply { eqProgram = EqProgram.VOCAL }
        val source = FakePersistenceSource(delegate, eqParametersAvailable = false)
        val result = FiioJa11ControlRepository(source).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )

        assertThat(result.outcome).isEqualTo("SUCCESS")
        assertThat(result.eqComparison).isEqualTo("INSUFFICIENT_EVIDENCE")
        assertThat(result.baselineEq).isNull()
        assertThat(result.finalEq).isNull()
        assertThat(source.savePrograms).containsExactly(EqProgram.VOCAL)
        Unit
    }

    @Test
    fun missingUserOneBankReadStopsBeforeAnyMutation() = runBlocking {
        val delegate = FakeSource()
        val source = FakePersistenceSource(delegate, eqParametersAvailable = false)
        val result = FiioJa11ControlRepository(source).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )

        assertThat(result.outcome).isEqualTo("BASELINE_EQ_READ_FAILED")
        assertThat(delegate.volumeWrites).isEmpty()
        assertThat(source.savePrograms).isEmpty()
        Unit
    }

    @Test
    fun volatileMismatchStopsBeforeSaveAndSaveFailureDoesNotReplay() = runBlocking {
        val mismatchDelegate = FakeSource(ignoreVolumeWrite = true)
        val mismatchSource = FakePersistenceSource(mismatchDelegate)
        val mismatch = FiioJa11ControlRepository(mismatchSource).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )
        assertThat(mismatch.outcome).isEqualTo("VOLATILE_READBACK_MISMATCH")
        assertThat(mismatchSource.savePrograms).isEmpty()

        val failureDelegate = FakeSource()
        val failureSource = FakePersistenceSource(failureDelegate, saveSucceeds = false)
        val failedSave = FiioJa11ControlRepository(failureSource).writeControlAndSave(
            DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(58.0), 7L),
        )
        assertThat(failedSave.outcome).isEqualTo("SAVE_UNCONFIRMED")
        assertThat(failedSave.targetedWriteCount).isEqualTo(1)
        assertThat(failedSave.saveCount).isEqualTo(1)
        assertThat(failureDelegate.volumeWrites).containsExactly(58)
        assertThat(failureSource.savePrograms).containsExactly(EqProgram.USER_1)
        Unit
    }

    @Test
    fun volumeExtremesRemainWithinNativeZeroToSixtyRange() = runBlocking {
        for (level in listOf(0, 60)) {
            val delegate = FakeSource()
            val source = FakePersistenceSource(delegate)
            val result = FiioJa11ControlRepository(source).writeControlAndSave(
                DacWriteIntent(FiioJa11DeviceControls.OUTPUT_VOLUME, DacControlValue.Numeric(level.toDouble()), 7L),
            )
            assertThat(result.outcome).isEqualTo("SUCCESS")
            assertThat(delegate.volumeWrites).containsExactly(level)
            assertThat(source.savePrograms).containsExactly(EqProgram.USER_1)
        }
        Unit
    }

    @Test
    fun completeReadReturnsOneCurrentSnapshot() = runBlocking {
        val source = FakeSource()
        val result = FiioJa11ControlRepository(source).readSnapshot()

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
        val source = FakeSource()
        val repository = FiioJa11ControlRepository(source)

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
    fun staleExpectedGenerationCannotWrite() = runBlocking {
        val source = FakeSource()
        val result = FiioJa11ControlRepository(source).writeControl(
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
        val result = FiioJa11ControlRepository(source).writeControl(
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
        val repository = FiioJa11ControlRepository(source)

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
        val repository = FiioJa11ControlRepository(source)
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
        val repository = FiioJa11ControlRepository(source)
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
        val repository = FiioJa11ControlRepository(source)
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
        val repository = FiioJa11ControlRepository(source)
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
        val result = FiioJa11ControlRepository(source).writeControl(
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
        val repository = FiioJa11ControlRepository(source)
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
        val repository = FiioJa11ControlRepository(source)
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
        val repository = FiioJa11ControlRepository(source)
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

        val result = FiioJa11ControlRepository(source).readSnapshot()

        assertThat(result).isEqualTo(FiioJa11ControlReadResult.ReadFailed("UAC mode / USB identity"))
    }

    private class FakeSource(
        private val ignoreVolumeWrite: Boolean = false,
        private val candidateCount: Int = 1,
        private val permissionDelayMillis: Long = 0L,
        var replacementReady: Boolean = true,
    ) : FiioJa11DeviceControlSource {
        override var sessionGeneration: Long = 7L
        override var detachGeneration: Long = 0L
        override var connectedDeviceSerial: String? = null
        override val supportedCandidateCount: Int = candidateCount
        var productId: Int = FiioJa11Protocol.PRODUCT_ID_UAC_2
        var current = true
        var outputVolume = 59
        var sampleRate = "352.8 kHz"
        var firmware = "2.20"
        var headsetControlEnabled = true
        var eqProgram = FiioJa11Protocol.EqProgram.USER_1
        var uacMode = FiioJa11Protocol.UacMode.UAC_2
        val volumeWrites = mutableListOf<Int>()
        val headsetControlWrites = mutableListOf<Boolean>()
        var replacementWaits = 0

        override val connectedProductId: Int?
            get() = productId

        override fun isSessionCurrent(sessionGeneration: Long): Boolean =
            current && candidateCount == 1 && sessionGeneration == this.sessionGeneration

        override suspend fun awaitExpectedReplacementSession(
            previousGeneration: Long,
            previousDetachGeneration: Long,
            previousSerial: String?,
            timeoutMillis: Long,
        ): Boolean {
            replacementWaits += 1
            if (!replacementReady) return false
            if (permissionDelayMillis > 0L) delay(permissionDelayMillis)
            if (sessionGeneration == previousGeneration) sessionGeneration += 1L
            if (detachGeneration == previousDetachGeneration) detachGeneration += 1L
            val expectedSerial = previousSerial?.trim()?.takeIf(String::isNotEmpty)
            val replacementSerial = connectedDeviceSerial?.trim()?.takeIf(String::isNotEmpty)
            return candidateCount == 1 &&
                (expectedSerial == null || replacementSerial == null || expectedSerial == replacementSerial)
        }

        override suspend fun readOutputVolume(): Int? = outputVolume
        override suspend fun readSampleRateLabel(): String? = sampleRate
        override suspend fun readFirmwareVersion(): String? = firmware
        override suspend fun readHeadsetControlEnabled(): Boolean? = headsetControlEnabled
        override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? = eqProgram
        override suspend fun readUacMode(): FiioJa11Protocol.UacMode? = uacMode

        override suspend fun writeOutputVolume(level: Int): Boolean {
            volumeWrites += level
            if (!ignoreVolumeWrite) outputVolume = level
            return true
        }

        override suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean {
            headsetControlWrites += enabled
            headsetControlEnabled = enabled
            return true
        }

        override suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean {
            eqProgram = program
            return true
        }

        override suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean {
            uacMode = mode
            return true
        }
    }

    private class FakePersistenceSource(
        private val delegate: FakeSource,
        private val eqParametersAvailable: Boolean = true,
        private val saveSucceeds: Boolean = true,
        private val detachDuringFinalEqRead: Boolean = false,
        private val reconnectSucceeds: Boolean = true,
        private val replacementMatchesIdentity: Boolean = true,
        private val detachOnEqReads: Set<Int> = if (detachDuringFinalEqRead) setOf(3) else emptySet(),
        private val mutateVolumeOnSave: Int? = null,
        private val mutateMicOnSave: Boolean? = null,
    ) : FiioJa11DeviceControlSource by delegate, FiioJa11SettingsPersistenceSource {
        val savePrograms = mutableListOf<EqProgram>()
        var eqReadCount = 0
        var reconnectWaits = 0

        override suspend fun readEqParameters(program: EqProgram): FiioJa11EqParameterSnapshot? {
            eqReadCount += 1
            if (eqReadCount in detachOnEqReads) delegate.current = false
            return if (!eqParametersAvailable) null else FiioJa11EqParameterSnapshot(
                program = program,
                bands = (0..4).map { index -> FiioJa11Protocol.Band("PEAK", 100.0 + index, index.toDouble(), 1.0) },
                globalGainDb = 0.0,
            )
        }

        override suspend fun saveCurrentSettings(program: EqProgram): Boolean {
            savePrograms += program
            mutateVolumeOnSave?.let { delegate.outputVolume = it }
            mutateMicOnSave?.let { delegate.headsetControlEnabled = it }
            return saveSucceeds
        }

        override suspend fun awaitFinalReadbackReconnect(): Boolean {
            reconnectWaits += 1
            if (!reconnectSucceeds || !replacementMatchesIdentity) return false
            delegate.current = true
            delegate.sessionGeneration += 1L
            delegate.detachGeneration += 1L
            return true
        }
        override fun beginSettingsTrace(operationId: String) = Unit
        override fun endSettingsTrace(): List<String> = emptyList()
    }
}
