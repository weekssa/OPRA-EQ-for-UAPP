package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlFlasherTest {
    @Test
    fun successfulZeroPreampFlashReadsSlotAndGainThenSendsCompleteEqSequence() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x06, globalGainRaw = -2_000)
        val store = FakeGainStore()
        val result = BlackPearlFlasher(transport, store).flash(profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.Success)
        result as BlackPearlFlashResult.Success
        assertEquals(DevicePresetFidelity.EXACT, result.fidelity)
        assertEquals(0.0, result.appliedPlaybackGainDb, 0.0)
        assertEquals(12, transport.sent.size)
        assertTrue(transport.sent.none { it[2].u8() == 0x03 })
        assertTrue(transport.sent.take(10).all { it[36].u8() == 0x06 })
    }

    @Test
    fun negativePreampAdjustsGlobalGainBeforeEqWrites() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x02, globalGainRaw = -2_000)
        val store = FakeGainStore()

        val result = BlackPearlFlasher(transport, store).flash(profile(preamp = -4.5))

        assertTrue(result is BlackPearlFlashResult.Success)
        assertEquals(13, transport.sent.size)
        assertEquals(0x03, transport.sent.first()[2].u8())
        assertEquals(-2_000 + BlackPearlProtocol.gainDbToRawDelta(-4.5), gainRaw(transport.sent.first()))
        assertEquals(BlackPearlProtocol.gainDbToRawDelta(-4.5), store.appliedRaw)
    }

    @Test
    fun laterFlashReplacesPriorEqGainInsteadOfAccumulatingIt() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x01, globalGainRaw = -2_000)
        val store = FakeGainStore()
        val flasher = BlackPearlFlasher(transport, store)

        flasher.flash(profile(preamp = -4.0))
        assertEquals(-3_024, transport.globalGainRaw)

        transport.sent.clear()
        flasher.flash(profile(preamp = -6.0))

        assertEquals(-3_536, transport.globalGainRaw)
        assertEquals(-1_536, store.appliedRaw)
        assertEquals(-3_536, gainRaw(transport.sent.first()))
    }

    @Test
    fun zeroPreampRestoresPriorEqLibraryAttenuation() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x01, globalGainRaw = -2_000)
        val store = FakeGainStore()
        val flasher = BlackPearlFlasher(transport, store)

        flasher.flash(profile(preamp = -4.0))
        transport.sent.clear()
        flasher.flash(profile(preamp = 0.0))

        assertEquals(-2_000, transport.globalGainRaw)
        assertEquals(0, store.appliedRaw)
        assertEquals(-2_000, gainRaw(transport.sent.first()))
    }

    @Test
    fun missingActiveSlotFailsBeforeAnyWrite() = runBlocking {
        val transport = FakeTransport(activeSlot = null, globalGainRaw = -2_000)
        val result = BlackPearlFlasher(transport, FakeGainStore()).flash(profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.DeviceUnavailable)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun missingGlobalGainFailsBeforeAnyWrite() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x00, globalGainRaw = null)
        val result = BlackPearlFlasher(transport, FakeGainStore()).flash(profile(preamp = -5.0))

        assertTrue(result is BlackPearlFlashResult.DeviceUnavailable)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun unrepresentableTargetGainFailsWithoutClampingOrWrites() = runBlocking {
        val transport = FakeTransport(
            activeSlot = 0x00,
            globalGainRaw = BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW + 100,
        )
        val result = BlackPearlFlasher(transport, FakeGainStore()).flash(profile(preamp = -5.0))

        assertTrue(result is BlackPearlFlashResult.NotRepresentable)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun transportFailureStopsSequenceImmediately() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x01, globalGainRaw = -2_000, failAtSend = 3)
        val result = BlackPearlFlasher(transport, FakeGainStore()).flash(profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.TransferFailed)
        assertEquals(3, transport.sent.size)
    }

    private fun profile(preamp: Double?) = OpraEqProfile(
        id = "p",
        productId = "product",
        author = "Tester",
        details = "Target",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = preamp,
        bands = listOf(
            OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null),
            OpraBand("low_shelf", 105.0, 3.0, 0.71, null),
        ),
    )

    private class FakeGainStore(
        var appliedRaw: Int = 0,
    ) : BlackPearlGainStateStore {
        override fun readAppliedGainDeltaRaw(): Int = appliedRaw
        override fun writeAppliedGainDeltaRaw(rawDelta: Int) {
            appliedRaw = rawDelta
        }
    }

    private class FakeTransport(
        private val activeSlot: Byte?,
        globalGainRaw: Int?,
        private val failAtSend: Int? = null,
    ) : BlackPearlTransport {
        val sent = mutableListOf<ByteArray>()
        var globalGainRaw: Int? = globalGainRaw

        override suspend fun readActiveSlot(): Byte? = activeSlot

        override suspend fun readGlobalGainRaw(): Int? = globalGainRaw

        override suspend fun sendReport(report: ByteArray): Boolean {
            sent += report.copyOf()
            val succeeds = failAtSend == null || sent.size != failAtSend
            if (succeeds && report[2].u8() == 0x03) {
                globalGainRaw = gainRaw(report)
            }
            return succeeds
        }
    }

    private fun gainRaw(report: ByteArray): Int = ByteBuffer.wrap(report, 4, 2)
        .order(ByteOrder.LITTLE_ENDIAN)
        .short
        .toInt()

    private fun Byte.u8(): Int = toInt() and 0xFF
}
