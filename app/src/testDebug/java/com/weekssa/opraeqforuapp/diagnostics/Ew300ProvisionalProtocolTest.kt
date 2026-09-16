package com.weekssa.opraeqforuapp.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300ProvisionalProtocolTest {
    @Test
    fun `read packet matches captured web fallback framing`() {
        assertEquals(
            "24 00 00 00 52 00 03 00 00 00",
            Ew300ProvisionalProtocol.readPayload(0x24, slotHint = 3).toHex(),
        )
        assertEquals(
            "4B 24 00 00 00 52 00 03 00 00 00",
            Ew300ProvisionalProtocol.wireReport(
                Ew300ProvisionalProtocol.readPayload(0x24, slotHint = 3),
            ).toHex(),
        )
    }

    @Test
    fun `snapshot is bounded to slot five bands and global gain`() {
        assertEquals(
            listOf(0x24, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x66),
            Ew300ProvisionalProtocol.snapshotRegisters(),
        )
    }

    @Test
    fun `response requires exact report register and read echo`() {
        val valid = byteArrayOf(0x4B, 0x24, 0, 0, 0, 0x52, 0, 3, 0, 0, 0)
        assertEquals(3, Ew300ProvisionalProtocol.responsePayload(valid, 0x24)?.get(6)?.toInt())
        assertNull(Ew300ProvisionalProtocol.responsePayload(valid.copyOf().also { it[0] = 0x54 }, 0x24))
        assertNull(Ew300ProvisionalProtocol.responsePayload(valid.copyOf().also { it[5] = 0x57 }, 0x24))
    }

    @Test
    fun `decoder preserves raw frequency ambiguity`() {
        val payload = byteArrayOf(0x26, 0, 0, 0, 0x52, 0, 0xF1.toByte(), 0xFF.toByte(), 50, 0)
        val text = Ew300ProvisionalProtocol.describe(0x26, payload)
        assertTrue(text.contains("gain=-1.5 dB"))
        assertTrue(text.contains("frequency raw=50"))
        assertTrue(text.contains("fallback interpretation=100 Hz"))
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
}
