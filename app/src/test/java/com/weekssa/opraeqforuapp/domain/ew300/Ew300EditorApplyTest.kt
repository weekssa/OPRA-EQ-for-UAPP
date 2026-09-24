package com.weekssa.opraeqforuapp.domain.ew300

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditor
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditSpecs
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditorStartResult
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqFilter
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300EditorApplyTest {
    @Test
    fun unqualifiedShelfEditorFilterIsRejectedBeforeAnyWriteOrSave() = runBlocking {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = stockBands(),
                globalGainDb = -29.0,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1L,
            ),
        )
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = HardwareEqSnapshotState().publishCurrent(bundle),
            spec = HardwareEqEditSpecs.SIMGOT_EW300,
        ) as HardwareEqEditorStartResult.Ready
        val edited = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.SIMGOT_EW300,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 2_500.0,
            gainDb = 4.0,
            q = 1.4,
        )
        val safeWorkingCopy = HardwareEqEditor.useSafeGain(edited, HardwareEqEditSpecs.SIMGOT_EW300)
        val invalidWorkingCopy = safeWorkingCopy.copy(
            filters = safeWorkingCopy.filters.map { filter ->
                if (filter.index == 0) filter.copy(type = EqFilterType.HIGH_SHELF) else filter
            },
        )
        val transport = FakeTransport(bundle)

        val result = Ew300EditorApplier(transport).apply(
            workingCopy = invalidWorkingCopy,
            allowCautions = false,
            isSessionCurrent = { it == 1L },
        )

        assertTrue(result is Ew300EditorApplyResult.InvalidPlan)
        assertThat(transport.writeCount).isEqualTo(0)
        assertThat(transport.commitCount).isEqualTo(0)
    }

    @Test
    fun applyUsesAbsoluteGlobalGainBaselineAndWritesOnlyAfterFreshValidation() = runBlocking {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = stockBands(),
                globalGainDb = -29.0,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1L,
            ),
        )
        val state = HardwareEqSnapshotState().publishCurrent(bundle)
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = state,
            spec = HardwareEqEditSpecs.SIMGOT_EW300,
        ) as HardwareEqEditorStartResult.Ready
        val edited = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.SIMGOT_EW300,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 2_500.0,
            gainDb = 4.0,
            q = 1.4,
        )
        val working = HardwareEqEditor.useSafeGain(edited, HardwareEqEditSpecs.SIMGOT_EW300)
        val transport = FakeTransport(bundle)
        var beforeFirstWriteCount = -1

        val result = Ew300EditorApplier(transport).apply(
            workingCopy = working,
            allowCautions = false,
            isSessionCurrent = { it == 1L },
            beforeFirstWrite = { beforeFirstWriteCount = transport.writeCount },
        )

        assertThat(result).isEqualTo(Ew300EditorApplyResult.Verified)
        assertThat(beforeFirstWriteCount).isEqualTo(0)
        assertThat(transport.writeCount).isGreaterThan(0)
        assertThat(transport.commitCount).isEqualTo(1)
        assertThat(Ew300Protocol.globalGainDb(transport.state.getValue(Ew300Protocol.GLOBAL_GAIN_REGISTER)))
            .isEqualTo(working.plannedHeadroomGainDb)
    }

    @Test
    fun applyRejectsAnUnverifiedReplacementSessionBeforeFinalReadback() = runBlocking {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = stockBands(),
                globalGainDb = -29.0,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1L,
            ),
        )
        val state = HardwareEqSnapshotState().publishCurrent(bundle)
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = state,
            spec = HardwareEqEditSpecs.SIMGOT_EW300,
        ) as HardwareEqEditorStartResult.Ready
        val edited = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.SIMGOT_EW300,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 2_500.0,
            gainDb = 4.0,
            q = 1.4,
        )
        val working = HardwareEqEditor.useSafeGain(edited, HardwareEqEditSpecs.SIMGOT_EW300)
        val transport = FakeTransport(bundle, replacementFingerprint = "other-ew300")

        val result = Ew300EditorApplier(transport).apply(
            workingCopy = working,
            allowCautions = false,
            isSessionCurrent = { it == 1L },
        )

        assertTrue(result is Ew300EditorApplyResult.VerificationFailed)
        assertThat(transport.commitCount).isEqualTo(1)
        assertThat(transport.finalReadCount).isEqualTo(0)
    }

    @Test
    fun applyRejectsUnequalStereoGainBaselineBeforeAnyWriteOrSave() = runBlocking {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = stockBands(),
                globalGainDb = -29.0,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1L,
            ),
        )
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = HardwareEqSnapshotState().publishCurrent(bundle),
            spec = HardwareEqEditSpecs.SIMGOT_EW300,
        ) as HardwareEqEditorStartResult.Ready
        val edited = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.SIMGOT_EW300,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 2_500.0,
            gainDb = 4.0,
            q = 1.4,
        )
        val transport = FakeTransport(bundle, initialGain = bytes(0x96, 0xF8, 0, 0))

        val result = Ew300EditorApplier(transport).apply(
            workingCopy = HardwareEqEditor.useSafeGain(edited, HardwareEqEditSpecs.SIMGOT_EW300),
            allowCautions = false,
            isSessionCurrent = { it == 1L },
        )

        assertTrue(result is Ew300EditorApplyResult.StaleBaseline)
        assertThat(transport.writeCount).isEqualTo(0)
        assertThat(transport.commitCount).isEqualTo(0)
    }

    private fun stockBands(): List<Kt02h20Band> = listOf(
        Kt02h20Band("peak_dip", 2_500.0, 4.5, 1.4),
        Kt02h20Band("peak_dip", 120.0, 0.0, 1.0),
        Kt02h20Band("peak_dip", 500.0, 0.0, 1.0),
        Kt02h20Band("peak_dip", 2_000.0, 0.0, 1.0),
        Kt02h20Band("peak_dip", 12_000.0, 0.0, 1.0),
    )

    private inner class FakeTransport(
        bundle: com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle,
        private val replacementFingerprint: String? = null,
        initialGain: ByteArray = bytes(0, 0, 0, 0),
    ) : Ew300Transport {
        override var deviceFingerprintKey: String =
            "vid=31b2|pid=111|manufacturer=LE XIAN|product=SIMGOT EW300 DSP|serial=2024-07-03-0000-0000-0000|interface=3"
        override var sessionGeneration: Long = 1L
        override var detachGeneration: Long = 0L
        val state = mutableMapOf<Int, ByteArray>().apply {
            put(Ew300Protocol.PROTOCOL_FLAGS_REGISTER, bytes(0, 0, 0, 0))
            put(0x24, bytes(0, 0, 0, 0))
            put(
                Ew300Protocol.GLOBAL_GAIN_REGISTER,
                initialGain.copyOf(),
            )
            bundle.snapshot.filters.sortedBy(HardwareEqFilter::index).forEach { filter ->
                val (gain, q) = Ew300Protocol.encodeBand(
                    Kt02h20Band("peak_dip", filter.frequencyHz, filter.gainDb, filter.q),
                )
                put(Ew300Protocol.bandRegister(filter.index), gain)
                put(Ew300Protocol.bandRegister(filter.index) + 1, q)
            }
        }
        var writeCount = 0
        var commitCount = 0
        var finalReadCount = 0

        override suspend fun readRegister(register: Int): ByteArray? {
            if (commitCount > 0) finalReadCount++
            return state[register]?.copyOf()
        }

        override suspend fun writeRegister(register: Int, data: ByteArray): Boolean {
            writeCount++
            state[register] = data.copyOf()
            return true
        }

        override suspend fun commit(): Boolean {
            commitCount++
            if (replacementFingerprint != null) {
                deviceFingerprintKey = replacementFingerprint
                sessionGeneration = 2L
                detachGeneration = 1L
            }
            return true
        }
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
        (values[index] and 0xFF).toByte()
    }
}
