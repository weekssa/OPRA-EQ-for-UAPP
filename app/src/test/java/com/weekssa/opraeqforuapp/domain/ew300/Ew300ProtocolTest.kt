package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
    fun peakBandRoundTripPreservesWireQuantizationAndFilterCode() {
        val source = Kt02h20Band("peak_dip", 7_000.0, 0.5, 0.9)
        val (gain, q) = Ew300Protocol.encodeBand(source)

        assertEquals(source, Ew300Protocol.decodeBand(4, gain, q))
        assertEquals(0x00, q[2].toInt() and 0xFF)
    }

    @Test
    fun unqualifiedNativeShelfWritesAreRejected() {
        assertThrows(IllegalStateException::class.java) {
            Ew300Protocol.encodeBand(Kt02h20Band("low_shelf", 120.0, 2.0, 0.7))
        }
        assertThrows(IllegalStateException::class.java) {
            Ew300Protocol.encodeBand(Kt02h20Band("high_shelf", 7_000.0, 0.5, 0.9))
        }
    }

    @Test
    fun verificationUsesWireQuantizationForFloatingPointValues() {
        val displayed = Kt02h20Band("peak_dip", 80.0, -1.0, 0.7000000000000001)
        val deviceReadback = Ew300Protocol.decodeBand(
            0,
            Ew300Protocol.encodeBand(displayed).first,
            Ew300Protocol.encodeBand(displayed).second,
        )

        val readback = requireNotNull(deviceReadback)
        assertEquals(0.7, readback.q, 1e-9)
        assertArrayEquals(
            Ew300Protocol.encodeBand(displayed).first,
            Ew300Protocol.encodeBand(readback).first,
        )
        assertArrayEquals(
            Ew300Protocol.encodeBand(displayed).second,
            Ew300Protocol.encodeBand(readback).second,
        )
    }

    @Test
    fun unsupportedReportsAndFiltersAreRejected() {
        assertNull(Ew300Protocol.decodeRead(0x26, bytes(0x4B, 0x26, 0, 0, 0, 0x57, 0, 0, 0, 0, 0)))
        assertNull(Ew300Protocol.decodeBand(0, bytes(0, 0, 0, 0), bytes(0, 0, 1, 0)))
    }

    @Test
    fun globalGainUsesSignedHalfDbStepsAndPreservesOtherBytes() {
        val stock = bytes(0xF8, 0xF8, 0x00, 0x00)
        val stereoFlags = bytes(0x00, 0x00, 0x00, 0x00)

        assertEquals(-8, Ew300Protocol.globalGainSteps(stock))
        assertEquals(-4.0, Ew300Protocol.globalGainDb(stock), 0.0)
        assertEquals(2, Ew300Protocol.gainDbToSteps(1.0))
        assertArrayEquals(
            bytes(0xF9, 0xF9, 0x00, 0x00),
            Ew300Protocol.withGlobalGainSteps(stock, -7, stereoFlags),
        )
        assertEquals(
            -3.5,
            requireNotNull(Ew300Protocol.globalGainDb(bytes(0xF9, 0xF9, 0, 0), stereoFlags)),
            0.0,
        )
    }

    @Test
    fun singleDacGainLayoutPreservesTheSecondByte() {
        val singleDacFlags = bytes(0x00, 0x02, 0x00, 0x00)
        val stock = bytes(0xF8, 0xF7, 0x12, 0x34)

        assertArrayEquals(
            bytes(0xF9, 0xF7, 0x12, 0x34),
            Ew300Protocol.withGlobalGainSteps(stock, -7, singleDacFlags),
        )
        assertEquals(Ew300Protocol.GlobalGainLayout.SINGLE_DAC, Ew300Protocol.globalGainLayout(singleDacFlags))
    }

    @Test
    fun stereoMismatchCannotBePresentedAsOneGlobalGainValue() {
        assertNull(
            Ew300Protocol.globalGainDb(
                bytes(0x96, 0xF8, 0x00, 0x00),
                bytes(0, 0, 0, 0),
            ),
        )
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
        (values[index] and 0xFF).toByte()
    }
}
