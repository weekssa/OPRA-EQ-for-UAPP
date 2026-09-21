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
import org.junit.Test

class Ew300EditorApplyTest {
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

    private fun stockBands(): List<Kt02h20Band> = listOf(
        Kt02h20Band("peak_dip", 2_500.0, 4.5, 1.4),
        Kt02h20Band("peak_dip", 120.0, 0.0, 1.0),
        Kt02h20Band("peak_dip", 500.0, 0.0, 1.0),
        Kt02h20Band("peak_dip", 2_000.0, 0.0, 1.0),
        Kt02h20Band("peak_dip", 12_000.0, 0.0, 1.0),
    )

    private inner class FakeTransport(
        bundle: com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle,
    ) : Ew300Transport {
        override val deviceFingerprintKey: String =
            "vid=31b2|pid=111|manufacturer=LE XIAN|product=SIMGOT EW300 DSP|serial=2024-07-03-0000-0000-0000|interface=3"
        val state = mutableMapOf<Int, ByteArray>().apply {
            put(0x24, bytes(0, 0, 0, 0))
            put(
                Ew300Protocol.GLOBAL_GAIN_REGISTER,
                Ew300Protocol.withGlobalGainSteps(bytes(0, 0, 0, 0), -58),
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

        override suspend fun readRegister(register: Int): ByteArray? = state[register]?.copyOf()

        override suspend fun writeRegister(register: Int, data: ByteArray): Boolean {
            writeCount++
            state[register] = data.copyOf()
            return true
        }

        override suspend fun commit(): Boolean {
            commitCount++
            return true
        }
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
        (values[index] and 0xFF).toByte()
    }
}
