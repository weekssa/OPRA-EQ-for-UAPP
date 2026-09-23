package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Ew300ProtocolTest {
    @Test
    fun reportsUseQualifiedKtMicroFraming() {
        assertArrayEquals(
            bytes(0x4B, 0x26, 0, 0, 0, 0x52, 0, 0, 0, 0, 0),
            Ew300Protocol.readRegisterReport(0x26),
        )
        assertArrayEquals(
            bytes(0x4B, 0x26, 0, 0, 0, 0x57, 0, 0xF5, 0xFF, 0x64, 0),
            Ew300Protocol.writeRegisterReport(0x26, bytes(0xF5, 0xFF, 0x64, 0)),
        )
        assertArrayEquals(
            bytes(0x4B, 0, 0, 0, 0, 0x53, 0, 0, 0, 0, 0),
            Ew300Protocol.commitReport(),
        )
    }

    @Test
    fun bandRoundTripPreservesWireQuantizationAndFilterCode() {
        val source = Kt02h20Band("high_shelf", 7_000.0, 0.5, 0.9)
        val (gain, q) = Ew300Protocol.encodeBand(source)

        assertEquals(source, Ew300Protocol.decodeBand(4, gain, q))
        assertEquals(0x04, q[2].toInt() and 0xFF)
    }

    @Test
    fun unsupportedReportsAndFiltersAreRejected() {
        assertNull(Ew300Protocol.decodeRead(0x26, bytes(0x4B, 0x26, 0, 0, 0, 0x57, 0, 0, 0, 0, 0)))
        assertNull(Ew300Protocol.decodeBand(0, bytes(0, 0, 0, 0), bytes(0, 0, 1, 0)))
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
        (values[index] and 0xFF).toByte()
    }
}
