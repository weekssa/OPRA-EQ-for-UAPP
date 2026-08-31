package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlFlasherTest {
    @Test
    fun successfulFlashReadsSlotThenSendsOnlyCompleteEqSequence() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x06)
        val result = BlackPearlFlasher(transport).flash(profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.Success)
        assertEquals(DevicePresetFidelity.EXACT, (result as BlackPearlFlashResult.Success).fidelity)
        assertEquals(12, transport.sent.size)
        assertTrue(transport.sent.none { it[2].u8() == 0x03 })
        assertTrue(transport.sent.take(10).all { it[36].u8() == 0x06 })
    }

    @Test
    fun missingActiveSlotFailsBeforeAnyWrite() = runBlocking {
        val transport = FakeTransport(activeSlot = null)
        val result = BlackPearlFlasher(transport).flash(profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.DeviceUnavailable)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun preampConstraintFailsBeforeAnyWriteAfterSafeSlotRead() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x00)
        val result = BlackPearlFlasher(transport).flash(profile(preamp = -5.0))

        assertTrue(result is BlackPearlFlashResult.NotRepresentable)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun transportFailureStopsSequenceImmediately() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x01, failAtSend = 3)
        val result = BlackPearlFlasher(transport).flash(profile(preamp = 0.0))

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

    private class FakeTransport(
        private val activeSlot: Byte?,
        private val failAtSend: Int? = null,
    ) : BlackPearlTransport {
        val sent = mutableListOf<ByteArray>()

        override suspend fun readActiveSlot(): Byte? = activeSlot

        override suspend fun sendReport(report: ByteArray): Boolean {
            sent += report.copyOf()
            return failAtSend == null || sent.size != failAtSend
        }
    }

    private fun Byte.u8(): Int = toInt() and 0xFF
}
