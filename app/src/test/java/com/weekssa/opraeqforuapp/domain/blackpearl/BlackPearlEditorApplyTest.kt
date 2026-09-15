package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditSpecs
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditor
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditorStartResult
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqFilter
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.runBlocking
import org.junit.Test

class BlackPearlEditorApplyTest {
    @Test
    fun cautionRequiresExplicitConfirmationWithoutUsbTraffic() = runBlocking {
        val fixture = fixture(firstGainDb = 0.0, trackedGainDb = 0.0)
        var working = editFirstBand(fixture, gainDb = 11.0)
        working = HardwareEqEditor.useSafeGain(working, HardwareEqEditSpecs.TRN_BLACK_PEARL)
        assertThat(working.cautions).isNotEmpty()

        val result = fixture.applier.apply(
            workingCopy = working,
            allowCautions = false,
            isSessionCurrent = { true },
        )

        assertThat(result).isInstanceOf(BlackPearlEditorApplyResult.ConfirmationRequired::class.java)
        assertThat(fixture.transport.operations).isEmpty()
    }

    @Test
    fun staleSessionRejectsBeforeUsbTraffic() = runBlocking {
        val fixture = fixture(firstGainDb = 0.0, trackedGainDb = 0.0)
        val working = HardwareEqEditor.useSafeGain(
            editFirstBand(fixture, gainDb = 3.0),
            HardwareEqEditSpecs.TRN_BLACK_PEARL,
        )

        val result = fixture.applier.apply(
            workingCopy = working,
            allowCautions = true,
            isSessionCurrent = { false },
        )

        assertThat(result).isInstanceOf(BlackPearlEditorApplyResult.StaleBaseline::class.java)
        assertThat(fixture.transport.operations).isEmpty()
    }

    @Test
    fun externallyChangedBandRejectsWithoutWrites() = runBlocking {
        val fixture = fixture(firstGainDb = 0.0, trackedGainDb = 0.0)
        val working = HardwareEqEditor.useSafeGain(
            editFirstBand(fixture, gainDb = 3.0),
            HardwareEqEditSpecs.TRN_BLACK_PEARL,
        )
        fixture.transport.nativeBands[4] = fixture.transport.nativeBands[4].copy(gainRaw256 = 128)

        val result = fixture.applier.apply(
            workingCopy = working,
            allowCautions = true,
            isSessionCurrent = { true },
        )

        assertThat(result).isInstanceOf(BlackPearlEditorApplyResult.StaleBaseline::class.java)
        assertThat(fixture.transport.operations.none { it.startsWith("write-") }).isTrue()
    }

    @Test
    fun changedTrackedGainRejectsWithoutWrites() = runBlocking {
        val fixture = fixture(firstGainDb = 0.0, trackedGainDb = -1.0)
        val working = HardwareEqEditor.useSafeGain(
            editFirstBand(fixture, gainDb = 3.0),
            HardwareEqEditSpecs.TRN_BLACK_PEARL,
        )
        fixture.store.appliedRaw = BlackPearlProtocol.gainDbToRawDelta(-2.0)

        val result = fixture.applier.apply(
            workingCopy = working,
            allowCautions = true,
            isSessionCurrent = { true },
        )

        assertThat(result).isInstanceOf(BlackPearlEditorApplyResult.StaleBaseline::class.java)
        assertThat(fixture.transport.operations.none { it.startsWith("write-") }).isTrue()
    }

    @Test
    fun boostedEqLowersGainBeforeWritingEqAndVerifiesReadback() = runBlocking {
        val fixture = fixture(firstGainDb = 0.0, trackedGainDb = 0.0, userVolumeRaw = -2_000)
        var working = editFirstBand(fixture, gainDb = 6.0)
        working = HardwareEqEditor.useSafeGain(working, HardwareEqEditSpecs.TRN_BLACK_PEARL)

        val result = fixture.applier.apply(
            workingCopy = working,
            allowCautions = true,
            isSessionCurrent = { true },
        )

        assertThat(result).isInstanceOf(BlackPearlEditorApplyResult.Verified::class.java)
        val firstGainWrite = fixture.transport.operations.indexOf("write-gain")
        val firstBandWrite = fixture.transport.operations.indexOf("write-band-0")
        assertThat(firstGainWrite).isAtLeast(0)
        assertThat(firstGainWrite).isLessThan(firstBandWrite)
        assertThat(fixture.store.appliedRaw).isLessThan(0)
        assertThat(fixture.transport.nativeBands.first().gainRaw256).isEqualTo(6 * 256)
        assertThat(fixture.transport.operations.last()).isEqualTo("read-gain")
    }

    @Test
    fun reducedBoostVerifiesEqBeforeRaisingGain() = runBlocking {
        val fixture = fixture(firstGainDb = 6.0, trackedGainDb = -6.0, userVolumeRaw = -2_000)
        var working = editFirstBand(fixture, gainDb = 2.0)
        working = HardwareEqEditor.useSafeGain(working, HardwareEqEditSpecs.TRN_BLACK_PEARL)

        val result = fixture.applier.apply(
            workingCopy = working,
            allowCautions = true,
            isSessionCurrent = { true },
        )

        assertThat(result).isInstanceOf(BlackPearlEditorApplyResult.Verified::class.java)
        val flashIndex = fixture.transport.operations.indexOf("write-flash")
        val gainWriteIndex = fixture.transport.operations.indexOf("write-gain")
        assertThat(flashIndex).isAtLeast(0)
        assertThat(flashIndex).isLessThan(gainWriteIndex)
        val readbackBeforeGain = fixture.transport.operations
            .subList(flashIndex + 1, gainWriteIndex)
            .count { it.startsWith("read-band-") }
        assertThat(readbackBeforeGain).isEqualTo(BlackPearlProtocol.BAND_COUNT)
        assertThat(fixture.transport.nativeBands.first().gainRaw256).isEqualTo(2 * 256)
        assertThat(fixture.store.appliedRaw).isGreaterThan(BlackPearlProtocol.gainDbToRawDelta(-6.0))
    }

    @Test
    fun eqVerificationFailureNeverRaisesPlaybackGain() = runBlocking {
        val fixture = fixture(
            firstGainDb = 6.0,
            trackedGainDb = -6.0,
            userVolumeRaw = -2_000,
            ignoreBandWrites = true,
        )
        val initialGainRaw = fixture.transport.globalGainRaw
        val initialTrackedRaw = fixture.store.appliedRaw
        var working = editFirstBand(fixture, gainDb = 2.0)
        working = HardwareEqEditor.useSafeGain(working, HardwareEqEditSpecs.TRN_BLACK_PEARL)

        val result = fixture.applier.apply(
            workingCopy = working,
            allowCautions = true,
            isSessionCurrent = { true },
        )

        assertThat(result).isInstanceOf(BlackPearlEditorApplyResult.VerificationFailed::class.java)
        assertThat(fixture.transport.operations).doesNotContain("write-gain")
        assertThat(fixture.transport.globalGainRaw).isEqualTo(initialGainRaw)
        assertThat(fixture.store.appliedRaw).isEqualTo(initialTrackedRaw)
    }

    @Test
    fun failureAfterQuieterGainKeepsSaferTrackedDeltaForRetry() = runBlocking {
        val fixture = fixture(
            firstGainDb = 0.0,
            trackedGainDb = 0.0,
            userVolumeRaw = -2_000,
            failSendAt = 3,
        )
        var working = editFirstBand(fixture, gainDb = 6.0)
        working = HardwareEqEditor.useSafeGain(working, HardwareEqEditSpecs.TRN_BLACK_PEARL)

        val result = fixture.applier.apply(
            workingCopy = working,
            allowCautions = true,
            isSessionCurrent = { true },
        )

        assertThat(result).isInstanceOf(BlackPearlEditorApplyResult.TransferFailed::class.java)
        assertThat(fixture.transport.operations.first { it.startsWith("write-") }).isEqualTo("write-gain")
        assertThat(fixture.store.appliedRaw).isLessThan(0)
        assertThat(fixture.transport.globalGainRaw).isLessThan(-2_000)
    }

    @Test
    fun unrepresentableAbsoluteGainTargetProducesNoWrites() = runBlocking {
        val fixture = fixture(
            firstGainDb = 0.0,
            trackedGainDb = 0.0,
            userVolumeRaw = BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW + 100,
        )
        var working = editFirstBand(fixture, gainDb = 6.0)
        working = HardwareEqEditor.useSafeGain(working, HardwareEqEditSpecs.TRN_BLACK_PEARL)

        val result = fixture.applier.apply(
            workingCopy = working,
            allowCautions = true,
            isSessionCurrent = { true },
        )

        assertThat(result).isInstanceOf(BlackPearlEditorApplyResult.InvalidPlan::class.java)
        assertThat(fixture.transport.operations.none { it.startsWith("write-") }).isTrue()
    }

    private fun fixture(
        firstGainDb: Double,
        trackedGainDb: Double,
        userVolumeRaw: Int = -2_000,
        ignoreBandWrites: Boolean = false,
        failSendAt: Int? = null,
    ): Fixture {
        val filters = blackPearlFilters(firstGainDb)
        val nativeBands = filters.map(::nativeBand)
        val trackedRaw = BlackPearlProtocol.gainDbToRawDelta(trackedGainDb)
        val globalRaw = userVolumeRaw + trackedRaw
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.blackPearl(
                nativeBands = nativeBands,
                globalGainRaw = globalRaw,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1_000L,
            ),
        )
        val state = HardwareEqSnapshotState().publishCurrent(bundle)
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = state,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            trackedPlaybackGainDeltaDb = trackedGainDb,
        ) as HardwareEqEditorStartResult.Ready
        val transport = FakeTransport(
            nativeBands = nativeBands.toMutableList(),
            globalGainRaw = globalRaw,
            ignoreBandWrites = ignoreBandWrites,
            failSendAt = failSendAt,
        )
        val store = FakeGainStore(trackedRaw)
        return Fixture(
            working = started.workingCopy,
            transport = transport,
            store = store,
            applier = BlackPearlEditorApplier(transport, store),
        )
    }

    private fun editFirstBand(fixture: Fixture, gainDb: Double) = HardwareEqEditor.updateFilter(
        workingCopy = fixture.working,
        spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
        bandIndex = 0,
        type = EqFilterType.PEAK,
        frequencyHz = 1_000.0,
        gainDb = gainDb,
        q = 1.0,
    )

    private fun blackPearlFilters(firstGainDb: Double): List<HardwareEqFilter> =
        listOf(31.0, 63.0, 125.0, 250.0, 500.0, 1_000.0, 2_000.0, 4_000.0, 8_000.0, 16_000.0)
            .mapIndexed { index, frequency ->
                HardwareEqFilter(
                    index = index,
                    enabled = true,
                    type = EqFilterType.PEAK,
                    frequencyHz = if (index == 0) 1_000.0 else frequency,
                    gainDb = if (index == 0) firstGainDb else 0.0,
                    q = 1.0,
                )
            }

    private fun nativeBand(filter: HardwareEqFilter): BlackPearlReadCodec.NativeBand = requireNotNull(
        BlackPearlReadCodec.bandFromWriteReport(
            BlackPearlProtocol.writeBandReport(
                index = filter.index,
                band = BlackPearlProtocol.Band(
                    type = "peak_dip",
                    frequencyHz = filter.frequencyHz,
                    gainDb = filter.gainDb,
                    q = filter.q,
                ),
                activeSlot = 2,
            ),
        ),
    )

    private data class Fixture(
        val working: com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy,
        val transport: FakeTransport,
        val store: FakeGainStore,
        val applier: BlackPearlEditorApplier,
    )

    private class FakeGainStore(
        var appliedRaw: Int,
    ) : BlackPearlGainStateStore {
        override fun readAppliedGainDeltaRaw(): Int = appliedRaw

        override fun writeAppliedGainDeltaRaw(rawDelta: Int) {
            appliedRaw = rawDelta
        }
    }

    private class FakeTransport(
        val nativeBands: MutableList<BlackPearlReadCodec.NativeBand>,
        var globalGainRaw: Int,
        private val ignoreBandWrites: Boolean,
        private val failSendAt: Int?,
    ) : BlackPearlTransport {
        val operations = mutableListOf<String>()
        private var sendCount = 0

        override suspend fun readActiveSlot(): Byte? = nativeBands.firstOrNull()?.activeSlot?.toByte()

        override suspend fun readGlobalGainRaw(): Int {
            operations += "read-gain"
            return globalGainRaw
        }

        override suspend fun readNativeBand(index: Int): BlackPearlReadCodec.NativeBand? {
            operations += "read-band-$index"
            return nativeBands.getOrNull(index)
        }

        override suspend fun sendReport(report: ByteArray): Boolean {
            sendCount += 1
            val command = report[2].toInt() and 0xff
            val succeeds = failSendAt == null || sendCount != failSendAt
            when (command) {
                0x03 -> operations += "write-gain"
                0x09 -> operations += "write-band-${report[5].toInt() and 0xff}"
                0x0a -> operations += "write-latch"
                0x01 -> operations += "write-flash"
                else -> operations += "write-command-$command"
            }
            if (!succeeds) return false

            when (command) {
                0x03 -> globalGainRaw = ByteBuffer.wrap(report, 4, 2)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .short
                    .toInt()
                0x09 -> if (!ignoreBandWrites) {
                    val band = requireNotNull(BlackPearlReadCodec.bandFromWriteReport(report))
                    nativeBands[band.index] = band
                }
            }
            return true
        }
    }
}
