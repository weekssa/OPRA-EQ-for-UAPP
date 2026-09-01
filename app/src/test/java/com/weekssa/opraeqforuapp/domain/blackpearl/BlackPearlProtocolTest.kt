package com.weekssa.opraeqforuapp.domain.blackpearl

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlProtocolTest {
    @Test
    fun readBandReportUsesObservedHidEnvelope() {
        val report = BlackPearlProtocol.readBandReport(4)

        assertEquals(64, report.size)
        assertEquals(0x4B, report[0].u8())
        assertEquals(0x80, report[1].u8())
        assertEquals(0x09, report[2].u8())
        assertEquals(4, report[5].u8())
        assertTrue(report.drop(7).all { it == 0.toByte() })
    }

    @Test
    fun globalGainReadAndWriteUseObservedCommandAndSignedLittleEndianRawValue() {
        val read = BlackPearlProtocol.readGlobalGainReport()
        assertEquals(0x4B, read[0].u8())
        assertEquals(0x80, read[1].u8())
        assertEquals(0x03, read[2].u8())

        val write = BlackPearlProtocol.writeGlobalGainReport(-2_560)
        assertEquals(0x4B, write[0].u8())
        assertEquals(0x01, write[1].u8())
        assertEquals(0x03, write[2].u8())
        assertEquals(0x03, write[3].u8())
        assertEquals(-2_560, ByteBuffer.wrap(write, 4, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt())

        val response = read.copyOf().apply {
            ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).putShort(4, (-2_560).toShort())
        }
        assertEquals(-2_560, BlackPearlProtocol.globalGainRawFromResponse(response))
        assertEquals(-4.5, BlackPearlProtocol.rawDeltaToGainDb(BlackPearlProtocol.gainDbToRawDelta(-4.5)), 0.0)
    }

    @Test
    fun globalGainWriteRejectsValuesOutsideValidatedHardwareRange() {
        val below = runCatching {
            BlackPearlProtocol.writeGlobalGainReport(BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW - 1)
        }.exceptionOrNull()
        val above = runCatching {
            BlackPearlProtocol.writeGlobalGainReport(BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW + 1)
        }.exceptionOrNull()

        assertTrue(below is IllegalArgumentException)
        assertTrue(above is IllegalArgumentException)
    }

    @Test
    fun activeSlotComesFromBandResponse() {
        val response = BlackPearlProtocol.readBandReport(0).apply {
            this[1] = 0x80.toByte()
            this[36] = 0x07
        }

        assertEquals(0x07, BlackPearlProtocol.activeSlotFromBandResponse(response)?.u8())
        assertEquals(null, BlackPearlProtocol.activeSlotFromBandResponse(ByteArray(10)))
    }

    @Test
    fun writeBandReportPreservesPeakAndShelfTypeCodes() {
        val peak = BlackPearlProtocol.writeBandReport(
            0,
            BlackPearlProtocol.Band("peak_dip", 1_000.0, -2.5, 1.2),
            activeSlot = 0x03,
        )
        val lowShelf = BlackPearlProtocol.writeBandReport(
            1,
            BlackPearlProtocol.Band("low_shelf", 105.0, 4.0, 0.71),
            activeSlot = 0x03,
        )
        val highShelf = BlackPearlProtocol.writeBandReport(
            2,
            BlackPearlProtocol.Band("high_shelf", 8_000.0, -1.5, 0.71),
            activeSlot = 0x03,
        )

        assertEquals(0x02, peak[34].u8())
        assertEquals(0x03, lowShelf[34].u8())
        assertEquals(0x04, highShelf[34].u8())
        listOf(peak, lowShelf, highShelf).forEach { report ->
            assertEquals(64, report.size)
            assertEquals(0x4B, report[0].u8())
            assertEquals(0x01, report[1].u8())
            assertEquals(0x09, report[2].u8())
            assertEquals(0x18, report[3].u8())
            assertEquals(0x03, report[36].u8())
        }
    }

    @Test
    fun metadataIsLittleEndianAndUses256UnitsForQAndGain() {
        val report = BlackPearlProtocol.writeBandReport(
            5,
            BlackPearlProtocol.Band("peak_dip", 1_234.0, -2.5, 1.25),
            activeSlot = 0x02,
        )
        val buffer = ByteBuffer.wrap(report).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(1_234, buffer.getShort(28).toInt() and 0xFFFF)
        assertEquals(320, buffer.getShort(30).toInt() and 0xFFFF)
        assertEquals(-640, buffer.getShort(32).toInt())
        assertTrue((8 until 28 step 4).all { offset -> buffer.getFloat(offset).isFinite() })
    }

    @Test
    fun protocolEncodableGainOutsideValidatedRangeIsPreservedWithoutClamping() {
        val gainDb = -11.9
        assertFalse(BlackPearlProtocol.isBandGainWithinValidatedRange(gainDb))
        assertTrue(BlackPearlProtocol.isBandGainProtocolEncodable(gainDb))

        val report = BlackPearlProtocol.writeBandReport(
            9,
            BlackPearlProtocol.Band("peak_dip", 13_500.0, gainDb, 4.0),
            activeSlot = 0x01,
        )
        val rawGain = ByteBuffer.wrap(report).order(ByteOrder.LITTLE_ENDIAN).getShort(32).toInt()

        assertEquals((gainDb * 256.0).roundToInt(), rawGain)
    }

    @Test
    fun completePeqSequenceAlwaysOverwritesTenBandsThenLatchesAndFlashes() {
        val sequence = BlackPearlProtocol.flashSequence(
            bands = listOf(BlackPearlProtocol.Band("peak_dip", 1_000.0, 2.0, 1.0)),
            activeSlot = 0x04,
        )

        assertEquals(12, sequence.size)
        sequence.take(10).forEachIndexed { index, report ->
            assertEquals(0x09, report[2].u8())
            assertEquals(index, report[5].u8())
            assertEquals(0x04, report[36].u8())
        }
        assertEquals(0x0A, sequence[10][2].u8())
        assertEquals(0x01, sequence[11][2].u8())
        // Global gain is intentionally orchestrated separately by BlackPearlFlasher.
        assertTrue(sequence.none { report -> report[2].u8() == 0x03 })
    }

    @Test
    fun flatPaddingHasZeroGainSoOldHardwareBandsCannotLeakIntoShortPreset() {
        val sequence = BlackPearlProtocol.flashSequence(emptyList(), activeSlot = 0x00)
        val expectedZeroGain = byteArrayOf(0x00, 0x00)

        sequence.take(10).forEach { report ->
            assertArrayEquals(expectedZeroGain, report.copyOfRange(32, 34))
        }
    }

    @Test
    fun trulyUnrepresentableHardwareValuesAreRejectedInsteadOfSilentlyClamped() {
        val badBands = listOf(
            BlackPearlProtocol.Band("other", 1_000.0, 0.0, 1.0),
            BlackPearlProtocol.Band("peak_dip", 10.0, 0.0, 1.0),
            BlackPearlProtocol.Band("peak_dip", 1_000.0, 200.0, 1.0),
            BlackPearlProtocol.Band("peak_dip", 1_000.0, 0.0, 11.0),
        )

        badBands.forEach { band ->
            val failure = runCatching { BlackPearlProtocol.writeBandReport(0, band, 0x00) }.exceptionOrNull()
            assertNotNull(failure)
            assertTrue(failure is IllegalArgumentException)
        }
    }

    private fun Byte.u8(): Int = toInt() and 0xFF
}
