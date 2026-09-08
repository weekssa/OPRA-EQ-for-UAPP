package com.weekssa.opraeqforuapp.domain.kt02h20

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JcallyJm12ProtocolTest {
    @Test
    fun registerReadAndWriteGoldenVectorsUseElevenByteReportFormat() {
        assertArrayEquals(
            bytes(0x4B, 0x24, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00, 0x00, 0x00),
            JcallyJm12Protocol.readRegisterReport(0x24),
        )
        assertArrayEquals(
            bytes(0x4B, 0x26, 0x00, 0x00, 0x00, 0x57, 0x00, 0xEC, 0xFF, 0xE8, 0x03),
            JcallyJm12Protocol.writeRegisterReport(0x26, 0x03E8FFEC),
        )
    }

    @Test
    fun bandRegisterEncodingPreservesFrequencySignedGainQAndType() {
        val band = Kt02h20Band("low_shelf", 105.0, 4.0, 0.71)
        val encoded = JcallyJm12Protocol.encodeBand(band)

        assertEquals(0x00690028, encoded.a)
        assertEquals(0x000302C6, encoded.b)
        assertEquals(band, JcallyJm12Protocol.decodeBand(encoded.a, encoded.b))
    }

    @Test
    fun digitalGainReplacementPreservesUnrelatedRegisterBytes() {
        val original = 0xA5C30000.toInt()
        val stereoFlags = 0x0000
        val replaced = JcallyJm12Protocol.withDigitalGainSteps(
            registerValue = original,
            protocolFlags = stereoFlags,
            targetSteps = intArrayOf(-12, 7),
        )

        assertEquals(0xA5C307F4.toInt(), replaced)
        assertArrayEquals(intArrayOf(-12, 7), JcallyJm12Protocol.decodeDigitalGainSteps(replaced, stereoFlags))
        assertEquals(0xA5C30000.toInt(), replaced and 0xFFFF0000.toInt())
    }

    @Test
    fun singleDacFlagChangesOnlyOneDigitalGainByte() {
        val flags = 0x0200
        val original = 0x12345678
        val replaced = JcallyJm12Protocol.withDigitalGainSteps(original, flags, intArrayOf(-8))

        assertEquals(0x123456F8, replaced)
        assertArrayEquals(intArrayOf(-8), JcallyJm12Protocol.decodeDigitalGainSteps(replaced, flags))
    }

    @Test
    fun eqEnableTogglesOnlyBitZero() {
        val original = 0xA6
        val enabled = JcallyJm12Protocol.setEqEnabled(original, true)
        val disabled = JcallyJm12Protocol.setEqEnabled(enabled, false)

        assertEquals(0xA7, enabled)
        assertEquals(0xA6, disabled)
        assertTrue(JcallyJm12Protocol.isEqEnabled(enabled))
        assertFalse(JcallyJm12Protocol.isEqEnabled(disabled))
    }

    @Test
    fun completeBandsAlwaysProducesFiveSlotsAndFlatPadding() {
        val complete = JcallyJm12Protocol.completeBands(
            listOf(Kt02h20Band("high_shelf", 8_000.0, -2.0, 0.8)),
        )

        assertEquals(5, complete.size)
        assertEquals(-2.0, complete.first().gainDb, 0.0)
        complete.drop(1).forEach { assertEquals(0.0, it.gainDb, 0.0) }
    }

    @Test
    fun responsesMustEchoExpectedIdentityCommandAndAddress() {
        val goodRead = bytes(0x4B, 0x24, 0x00, 0x00, 0x00, 0x52, 0x00, 0x01, 0x00, 0x00, 0x00)
        assertEquals(1, JcallyJm12Protocol.readRegisterValue(0x24, goodRead))
        assertNull(JcallyJm12Protocol.readRegisterValue(0x25, goodRead))

        val goodAck = bytes(0x4B, 0x26, 0x00, 0x00, 0x00, 0x57, 0x00, 0x03, 0x00, 0x00, 0x00)
        assertTrue(JcallyJm12Protocol.writeAcknowledged(0x26, goodAck))
        assertFalse(JcallyJm12Protocol.writeAcknowledged(0x27, goodAck))

        val handshake = bytes(0x4B, 0x00, 0x00, 0x00, 0x00, 0x43, 0x00, 0x03, 0x00, 0x00, 0x00)
        assertTrue(JcallyJm12Protocol.handshakeAccepted(handshake))
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
        (values[index] and 0xFF).toByte()
    }
}
