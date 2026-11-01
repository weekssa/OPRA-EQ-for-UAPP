package com.weekssa.opraeqforuapp.data.dac

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

/**
 * Native program/volume defaults for every built-in JA11 program.
 *
 * The fake keeps runtime and retained (flash) state separately and models a power cycle, so a
 * transaction that forgets Save is observable. This is simulation, never hardware evidence.
 */
class FiioJa11NativeProgramDefaultsTest {
    @Test
    fun everyProgramVolumeBoundaryAndMicBaselineSavesTheSelectedPreset() = runBlocking {
        for (program in FiioJa11Protocol.EqProgram.entries) {
            for (requestedVolume in listOf(0, 60, 40)) {
                for (micEnabled in listOf(false, true)) {
                    val label = "$program volume=$requestedVolume mic=$micEnabled"
                    val source = StatefulJa11Source(startMic = micEnabled)
                    val repository = FiioJa11ControlRepository(source)
                    val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot
                    val target = if (requestedVolume != reviewed.outputVolume) {
                        FiioJa11DeviceControls.OUTPUT_VOLUME
                    } else {
                        FiioJa11DeviceControls.EQ_PROGRAM
                    }

                    val result = repository.setDefaults(
                        FiioJa11DefaultsRequest(reviewed, requestedVolume, program, target),
                    )

                    assertWithMessage(label).that(result).isInstanceOf(FiioJa11DefaultsResult.Verified::class.java)
                    val verified = result as FiioJa11DefaultsResult.Verified
                    assertWithMessage(label).that(verified.report.mechanism)
                        .isEqualTo(FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE)
                    assertWithMessage(label).that(verified.report.eqReadback)
                        .isEqualTo(FiioJa11DefaultsEqReadback.MATCHED)
                    assertWithMessage(label).that(verified.report.counts).isEqualTo(
                        FiioJa11DefaultsWriteCounts(
                            volumeWrites = if (requestedVolume != 40) 1 else 0,
                            programWrites = if (program != FiioJa11Protocol.EqProgram.USER_1) 1 else 0,
                            eqBandWrites = FiioJa11Protocol.BAND_COUNT,
                            eqGainWrites = 1,
                            eqApplyWrites = 1,
                            eqSaveWrites = 1,
                        ),
                    )
                    // FiiO Control sends the selected preset as the 0x19 payload; a constant 3
                    // would write the active working curve into User 1 (J034).
                    assertWithMessage(label).that(source.savePrograms).containsExactly(program)
                    assertWithMessage(label).that(verified.snapshot.eqProgram).isEqualTo(program)
                    assertWithMessage(label).that(verified.snapshot.outputVolume).isEqualTo(requestedVolume)
                    assertWithMessage(label).that(verified.snapshot.headsetControlEnabled).isEqualTo(micEnabled)
                    assertWithMessage(label).that(source.headsetWrites).isEmpty()
                    assertWithMessage(label).that(source.uacWrites).isEmpty()
                    assertWithMessage(label).that(source.bandWrites.map { it.first })
                        .containsExactly(0, 1, 2, 3, 4).inOrder()
                    assertWithMessage(label).that(source.applyWrites).isEqualTo(1)
                    assertWithMessage(label).that(source.saveWrites).isEqualTo(1)

                    source.powerCycle()
                    assertWithMessage(label).that(source.runtimeProgram).isEqualTo(program)
                    assertWithMessage(label).that(source.runtimeVolume).isEqualTo(requestedVolume)
                    assertWithMessage(label).that(source.runtimeMic).isEqualTo(micEnabled)
                }
            }
        }
    }
    @Test
    fun runtimeOnlyChangeRevertsOnPowerCycleAndSavedPairSurvives() = runBlocking {
        val source = StatefulJa11Source(startProgram = FiioJa11Protocol.EqProgram.USER_1, startVolume = 40)
        val repository = FiioJa11ControlRepository(source)

        val runtime = repository.writeControl(
            DacWriteIntent(
                FiioJa11DeviceControls.OUTPUT_VOLUME,
                DacControlValue.Numeric(55.0),
                source.sessionGeneration,
            ),
        )
        assertThat(runtime).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.saveWrites).isEqualTo(0)

        source.powerCycle()
        assertThat(source.runtimeVolume).isEqualTo(40)

        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot
        val saved = repository.setDefaults(
            FiioJa11DefaultsRequest(
                reviewed,
                55,
                FiioJa11Protocol.EqProgram.USER_1,
                FiioJa11DeviceControls.OUTPUT_VOLUME,
            ),
        ) as FiioJa11DefaultsResult.Verified
        assertThat(saved.report.counts.micWrites).isEqualTo(0)

        source.powerCycle()
        assertThat(source.runtimeVolume).isEqualTo(55)
        assertThat(source.runtimeProgram).isEqualTo(FiioJa11Protocol.EqProgram.USER_1)
    }

    @Test
    fun unchangedPairStillRestagesAppliesAndSavesOnce() = runBlocking {
        val source = StatefulJa11Source()
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(
                reviewed,
                reviewed.outputVolume,
                reviewed.eqProgram,
                FiioJa11DeviceControls.OUTPUT_VOLUME,
            ),
        ) as FiioJa11DefaultsResult.Verified

        assertThat(result.report.counts.volumeWrites).isEqualTo(0)
        assertThat(result.report.counts.programWrites).isEqualTo(0)
        assertThat(source.events).containsExactly(
            "band:0", "band:1", "band:2", "band:3", "band:4", "gain:-3.7", "apply", "save",
        ).inOrder()
        Unit
    }

    @Test
    fun changedBankAfterApplyStopsBeforeSave() = runBlocking {
        val source = StatefulJa11Source().apply { flattenAfterApply = true }
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 45, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        val report = (result as FiioJa11DefaultsResult.Failed).report
        assertThat(report.stage).isEqualTo(FiioJa11DefaultsStage.NATIVE_VOLATILE_VERIFY)
        assertThat(report.eqReadback).isEqualTo(FiioJa11DefaultsEqReadback.CHANGED)
        assertThat(source.saveWrites).isEqualTo(0)
        assertThat(source.headsetWrites).isEmpty()
    }

    @Test
    fun changedBankAtFinalReadbackFailsAfterOneSaveWithoutReplay() = runBlocking {
        val source = StatefulJa11Source().apply { flattenOnSave = true }
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 45, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        val report = (result as FiioJa11DefaultsResult.Failed).report
        assertThat(report.stage).isEqualTo(FiioJa11DefaultsStage.NATIVE_FINAL_VERIFY)
        assertThat(report.eqReadback).isEqualTo(FiioJa11DefaultsEqReadback.CHANGED)
        assertThat(source.saveWrites).isEqualTo(1)
        assertThat(source.applyWrites).isEqualTo(1)
        assertThat(source.events.count { it == "save" }).isEqualTo(1)
    }

    @Test
    fun saveFailureIsNeverRetriedAndWritesNoMic() = runBlocking {
        val source = StatefulJa11Source().apply { saveSucceeds = false }
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 45, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage)
            .isEqualTo(FiioJa11DefaultsStage.NATIVE_SAVE)
        assertThat(source.saveWrites).isEqualTo(1)
        assertThat(source.headsetWrites).isEmpty()
        assertThat(source.events.count { it == "save" }).isEqualTo(1)
    }

    @Test
    fun incompleteBankReadStopsBeforeAnyStaging() = runBlocking {
        val source = StatefulJa11Source().apply { failBandReadAt = 2 }
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 45, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage)
            .isEqualTo(FiioJa11DefaultsStage.NATIVE_SNAPSHOT)
        assertThat(source.bandWrites).isEmpty()
        assertThat(source.gainWrites).isEmpty()
        assertThat(source.applyWrites).isEqualTo(0)
        assertThat(source.saveWrites).isEqualTo(0)
    }

    @Test
    fun sessionChangeDuringStagingStopsBeforeApplyOrSave() = runBlocking {
        val source = StatefulJa11Source().apply { replaceSessionAtBandWrite = 2 }
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 45, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage)
            .isEqualTo(FiioJa11DefaultsStage.NATIVE_STAGE)
        assertThat(source.bandWrites).hasSize(3)
        assertThat(source.gainWrites).isEmpty()
        assertThat(source.applyWrites).isEqualTo(0)
        assertThat(source.saveWrites).isEqualTo(0)
    }

    @Test
    fun cancellationDuringStagingReleasesGateAndNeverSaves() = runBlocking {
        val source = StatefulJa11Source()
        val gate = CompletableDeferred<Unit>()
        source.bandWriteGate = gate
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val job = launch {
            repository.setDefaults(
                FiioJa11DefaultsRequest(reviewed, 45, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
            )
        }
        source.bandWriteEntered.await()
        job.cancelAndJoin()
        gate.complete(Unit)

        assertThat(source.saveWrites).isEqualTo(0)
        assertThat(source.applyWrites).isEqualTo(0)
        val reread = withTimeout(5_000) { repository.readSnapshot() }
        assertThat(reread).isInstanceOf(FiioJa11ControlReadResult.Success::class.java)
    }

    @Test
    fun unexpectedMicrophoneChangeFailsWithoutBeingReportedAsVerified() = runBlocking {
        val source = StatefulJa11Source().apply { flipMicAfterApply = true }
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 45, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat((result as FiioJa11DefaultsResult.Failed).report.stage)
            .isEqualTo(FiioJa11DefaultsStage.NATIVE_VOLATILE_VERIFY)
        assertThat(source.saveWrites).isEqualTo(0)
    }

    @Test
    fun unexpectedUacChangeFailsWithoutBeingReportedAsVerified() = runBlocking {
        val source = StatefulJa11Source().apply { changeUacAfterApply = true }
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 45, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Failed::class.java)
        assertThat(source.saveWrites).isEqualTo(0)
    }

    @Test
    fun unstableReadAfterSaveUsesOneBoundedReconnectBeforeVerifying() = runBlocking {
        val source = StatefulJa11Source().apply { unstableFirstFinalRead = true }
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 45, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.Verified::class.java)
        assertThat(source.finalReconnectWaits).isEqualTo(1)
        assertThat(source.saveWrites).isEqualTo(1)
    }

    @Test
    fun staleReviewedSessionIsRejectedBeforeAnyWrite() = runBlocking {
        val source = StatefulJa11Source()
        val repository = FiioJa11ControlRepository(source)
        val reviewed = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot
        source.sessionGeneration += 1

        val result = repository.setDefaults(
            FiioJa11DefaultsRequest(reviewed, 45, reviewed.eqProgram, FiioJa11DeviceControls.OUTPUT_VOLUME),
        )

        assertThat(result).isInstanceOf(FiioJa11DefaultsResult.ReviewChanged::class.java)
        assertThat(source.events).isEmpty()
    }

    @Test
    fun supportedDigitalFilterWriteIsVerified() = runBlocking {
        val source = StatefulJa11Source()
        val repository = FiioJa11ControlRepository(source)
        val snapshot = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot
        assertThat(snapshot.digitalFilterMode).isEqualTo(FiioJa11Protocol.DigitalFilterMode.FAST_LL)

        val result = repository.writeControl(
            DacWriteIntent(
                FiioJa11DeviceControls.DIGITAL_FILTER,
                DacControlValue.Discrete("non_os"),
                snapshot.sessionGeneration,
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.Verified::class.java)
        assertThat(source.runtimeFilter).isEqualTo(FiioJa11Protocol.DigitalFilterMode.NON_OS)
        assertThat(source.filterWrites).containsExactly(FiioJa11Protocol.DigitalFilterMode.NON_OS)
        Unit
    }

    @Test
    fun unsupportedDigitalFilterSendsNoWriteAndFailsClosed() = runBlocking {
        val source = StatefulJa11Source(reportedFilter = null)
        val repository = FiioJa11ControlRepository(source)
        val snapshot = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot
        assertThat(snapshot.digitalFilterMode).isNull()

        val result = repository.writeControl(
            DacWriteIntent(
                FiioJa11DeviceControls.DIGITAL_FILTER,
                DacControlValue.Discrete("fast_ll"),
                snapshot.sessionGeneration,
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.InvalidRequest::class.java)
        assertThat(source.filterWrites).isEmpty()
        assertThat(source.events).isEmpty()
    }

    @Test
    fun ignoredDigitalFilterWriteIsNeverReportedAsVerified() = runBlocking {
        val source = StatefulJa11Source().apply { ignoreFilterWrite = true }
        val repository = FiioJa11ControlRepository(source)
        val snapshot = (repository.readSnapshot() as FiioJa11ControlReadResult.Success).snapshot

        val result = repository.writeControl(
            DacWriteIntent(
                FiioJa11DeviceControls.DIGITAL_FILTER,
                DacControlValue.Discrete("slow_pc"),
                snapshot.sessionGeneration,
            ),
        )

        assertThat(result).isInstanceOf(FiioJa11ControlWriteResult.ReadbackMismatch::class.java)
    }

    private class StatefulJa11Source(
        startProgram: FiioJa11Protocol.EqProgram = FiioJa11Protocol.EqProgram.USER_1,
        startVolume: Int = 40,
        startMic: Boolean = true,
        startGain: Double = -3.7,
        private val reportedFilter: FiioJa11Protocol.DigitalFilterMode? =
            FiioJa11Protocol.DigitalFilterMode.FAST_LL,
    ) : FiioJa11DeviceControlSource {
        private val banks = FiioJa11Protocol.EqProgram.entries.associateWith { program ->
            curves.getValue(program).toMutableList()
        }

        override var sessionGeneration: Long = 3L
        override var detachGeneration: Long = 0L
        override val connectedDeviceSerial: String? = null
        override val supportedCandidateCount: Int = 1
        override val connectedProductId: Int? = FiioJa11Protocol.PRODUCT_ID_UAC_2

        var current = true
        var uacMode = FiioJa11Protocol.UacMode.UAC_2
        var runtimeProgram = startProgram
        var runtimeVolume = startVolume
        var runtimeMic = startMic
        var runtimeGain = startGain
        var runtimeBands = banks.getValue(startProgram).toMutableList()
        var runtimeFilter = reportedFilter

        var savedProgram = startProgram
        var savedVolume = startVolume
        var savedMic = startMic
        var savedGain = startGain

        var failBandReadAt: Int? = null
        var rejectBandAt: Int? = null
        var rejectGain = false
        var rejectApply = false
        var saveSucceeds = true
        var flattenAfterApply = false
        var flattenOnSave = false
        var flipMicAfterApply = false
        var changeUacAfterApply = false
        var replaceSessionAtBandWrite: Int? = null
        var ignoreFilterWrite = false
        var unstableFirstFinalRead = false
        var bandWriteGate: CompletableDeferred<Unit>? = null
        var bandWriteEntered = CompletableDeferred<Unit>()
        private var bumpOnNextSnapshotRead = false

        val events = mutableListOf<String>()
        val volumeWrites = mutableListOf<Int>()
        val programWrites = mutableListOf<FiioJa11Protocol.EqProgram>()
        val headsetWrites = mutableListOf<Boolean>()
        val uacWrites = mutableListOf<FiioJa11Protocol.UacMode>()
        val bandWrites = mutableListOf<Pair<Int, FiioJa11Protocol.Band>>()
        val gainWrites = mutableListOf<Double>()
        val filterWrites = mutableListOf<FiioJa11Protocol.DigitalFilterMode>()
        var applyWrites = 0
        var saveWrites = 0
        val savePrograms = mutableListOf<FiioJa11Protocol.EqProgram>()
        var finalReconnectWaits = 0

        override fun isSessionCurrent(sessionGeneration: Long): Boolean =
            current && sessionGeneration == this.sessionGeneration

        override suspend fun awaitExpectedReplacementSession(
            previousGeneration: Long,
            previousDetachGeneration: Long,
            previousSerial: String?,
            timeoutMillis: Long,
        ): Boolean {
            sessionGeneration += 1
            detachGeneration += 1
            return true
        }

        override suspend fun readOutputVolume(): Int? = runtimeVolume
        override suspend fun readSampleRateLabel(): String? = "44.1 kHz"
        override suspend fun readFirmwareVersion(): String? {
            if (bumpOnNextSnapshotRead) {
                bumpOnNextSnapshotRead = false
                sessionGeneration += 1
                detachGeneration += 1
            }
            return "2.20"
        }
        override suspend fun readHeadsetControlEnabled(): Boolean? = runtimeMic
        override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? = runtimeProgram
        override suspend fun readEqBand(index: Int): FiioJa11Protocol.Band? =
            if (failBandReadAt == index) null else runtimeBands[index]
        override suspend fun readEqGlobalGainDb(): Double? = runtimeGain
        override suspend fun readDigitalFilterMode(): FiioJa11Protocol.DigitalFilterMode? = runtimeFilter
        override suspend fun readUacMode(): FiioJa11Protocol.UacMode? = uacMode

        override suspend fun writeOutputVolume(level: Int): Boolean {
            events += "volume:$level"
            volumeWrites += level
            runtimeVolume = level
            return true
        }

        override suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean {
            events += "program:${program.name}"
            programWrites += program
            runtimeProgram = program
            runtimeBands = banks.getValue(program).toMutableList()
            return true
        }

        override suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean {
            events += "mic:$enabled"
            headsetWrites += enabled
            runtimeMic = enabled
            return true
        }

        override suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean {
            events += "uac:${mode.name}"
            uacWrites += mode
            uacMode = mode
            return true
        }

        override suspend fun writeDigitalFilterMode(mode: FiioJa11Protocol.DigitalFilterMode): Boolean {
            events += "filter:${mode.name}"
            filterWrites += mode
            if (!ignoreFilterWrite) runtimeFilter = mode
            return true
        }

        override suspend fun writeEqBand(index: Int, band: FiioJa11Protocol.Band): Boolean {
            events += "band:$index"
            bandWrites += index to band
            bandWriteEntered.complete(Unit)
            bandWriteGate?.await()
            if (replaceSessionAtBandWrite == index) {
                sessionGeneration += 1
                detachGeneration += 1
            }
            if (rejectBandAt == index) return false
            runtimeBands[index] = band
            return true
        }

        override suspend fun writeEqGlobalGain(gainDb: Double): Boolean {
            events += "gain:$gainDb"
            gainWrites += gainDb
            if (rejectGain) return false
            runtimeGain = gainDb
            return true
        }

        override suspend fun applyEqChanges(): Boolean {
            events += "apply"
            applyWrites += 1
            if (rejectApply) return false
            if (flattenAfterApply) runtimeBands = flatBands().toMutableList()
            if (flipMicAfterApply) runtimeMic = !runtimeMic
            if (changeUacAfterApply) uacMode = FiioJa11Protocol.UacMode.UAC_1
            return true
        }

        override suspend fun saveEqToFlash(program: FiioJa11Protocol.EqProgram): Boolean {
            savePrograms += program
            events += "save"
            saveWrites += 1
            if (!saveSucceeds) return false
            if (flattenOnSave) runtimeBands = flatBands().toMutableList()
            savedProgram = runtimeProgram
            savedVolume = runtimeVolume
            savedMic = runtimeMic
            savedGain = runtimeGain
            sessionGeneration += 1
            detachGeneration += 1
            if (unstableFirstFinalRead) bumpOnNextSnapshotRead = true
            return true
        }

        override suspend fun awaitFinalReadbackReconnect(): Boolean {
            finalReconnectWaits += 1
            sessionGeneration += 1
            detachGeneration += 1
            return true
        }

        fun powerCycle() {
            runtimeProgram = savedProgram
            runtimeVolume = savedVolume
            runtimeMic = savedMic
            runtimeGain = savedGain
            runtimeBands = banks.getValue(savedProgram).toMutableList()
            runtimeFilter = reportedFilter
            sessionGeneration += 1
            detachGeneration += 1
        }

        private companion object {
            fun flatBands(): List<FiioJa11Protocol.Band> = listOf(80.0, 250.0, 1_000.0, 4_000.0, 12_000.0)
                .map { FiioJa11Protocol.Band("peak_dip", it, 0.0, 0.7) }

            val curves: Map<FiioJa11Protocol.EqProgram, List<FiioJa11Protocol.Band>> = mapOf(
                FiioJa11Protocol.EqProgram.VOCAL to listOf(
                    FiioJa11Protocol.Band("peak_dip", 200.0, 1.5, 1.0),
                    FiioJa11Protocol.Band("peak_dip", 900.0, 0.5, 1.2),
                    FiioJa11Protocol.Band("peak_dip", 2_500.0, 2.0, 1.4),
                    FiioJa11Protocol.Band("high_shelf", 6_000.0, 1.0, 0.7),
                    FiioJa11Protocol.Band("high_shelf", 12_000.0, 0.5, 0.7),
                ),
                FiioJa11Protocol.EqProgram.CLASSIC to listOf(
                    FiioJa11Protocol.Band("low_shelf", 120.0, 2.0, 0.7),
                    FiioJa11Protocol.Band("peak_dip", 500.0, -1.0, 1.0),
                    FiioJa11Protocol.Band("peak_dip", 1_500.0, 1.0, 1.0),
                    FiioJa11Protocol.Band("peak_dip", 5_000.0, -1.5, 1.0),
                    FiioJa11Protocol.Band("high_shelf", 10_000.0, 1.5, 0.7),
                ),
                FiioJa11Protocol.EqProgram.BASS to listOf(
                    FiioJa11Protocol.Band("low_shelf", 80.0, 6.0, 0.7),
                    FiioJa11Protocol.Band("peak_dip", 200.0, 3.0, 1.0),
                    FiioJa11Protocol.Band("peak_dip", 800.0, -1.0, 1.0),
                    FiioJa11Protocol.Band("peak_dip", 3_000.0, -2.0, 1.0),
                    FiioJa11Protocol.Band("high_shelf", 12_000.0, -1.0, 0.7),
                ),
                FiioJa11Protocol.EqProgram.USER_1 to listOf(
                    FiioJa11Protocol.Band("low_shelf", 100.0, 4.5, 0.8),
                    FiioJa11Protocol.Band("peak_dip", 320.0, -3.2, 1.3),
                    FiioJa11Protocol.Band("peak_dip", 1_200.0, 2.1, 2.2),
                    FiioJa11Protocol.Band("peak_dip", 4_800.0, -4.4, 3.1),
                    FiioJa11Protocol.Band("high_shelf", 11_500.0, 3.3, 0.9),
                ),
                FiioJa11Protocol.EqProgram.OFF to flatBands(),
            )
        }
    }
}
