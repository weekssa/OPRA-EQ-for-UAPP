package com.weekssa.opraeqforuapp.domain.kt02h20

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FiioJa11ProtocolTest {
    @Test
    fun bandWriteGoldenVectorUsesObservedWireEncoding() {
        val report = FiioJa11Protocol.writeBandReport(
            index = 2,
            band = FiioJa11Protocol.Band(
                type = "low_shelf",
                frequencyHz = 105.0,
                gainDb = -3.5,
                q = 0.71,
            ),
        )

        assertArrayEquals(
            bytes(0x02, 0xAA, 0x0A, 0x00, 0x00, 0x15, 0x08, 0x02, 0xFF, 0xDD, 0x00, 0x69, 0x00, 0x47, 0x01, 0x00, 0xEE),
            report,
        )
    }

    @Test
    fun globalGainGoldenVectorUsesSignedLittleEndian2560Scale() {
        assertArrayEquals(
            bytes(0x02, 0xAA, 0x0A, 0x00, 0x00, 0x17, 0x02, 0x00, 0xC9, 0x00, 0xEE),
            FiioJa11Protocol.writeGlobalGainReport(-5.5),
        )
    }

    @Test
    fun applyAndSaveCommandsAreDistinctRunModeOperations() {
        assertArrayEquals(
            bytes(0x02, 0xAA, 0x0A, 0x00, 0x00, 0x18, 0x01, 0x01, 0x00, 0xEE),
            FiioJa11Protocol.applyReport(),
        )
        assertArrayEquals(
            bytes(0x02, 0xAA, 0x0A, 0x00, 0x00, 0x19, 0x01, 0x03, 0x00, 0xEE),
            FiioJa11Protocol.saveToFlashReport(),
        )
    }

    @Test
    fun readbackDecodesBandAndGlobalGainWithOrWithoutReportIdPrefix() {
        val bandResponse = bytes(
            0x02, 0xBB, 0x0B, 0x00, 0x00, 0x15, 0x08, 0x02,
            0xFF, 0xDD, 0x00, 0x69, 0x00, 0x47, 0x01, 0x00, 0xEE,
        )
        val parsed = FiioJa11Protocol.bandFromResponse(bandResponse)
        assertNotNull(parsed)
        assertEquals(2, parsed!!.first)
        assertEquals("low_shelf", parsed.second.type)
        assertEquals(105.0, parsed.second.frequencyHz, 0.0)
        assertEquals(-3.5, parsed.second.gainDb, 0.0)
        assertEquals(0.71, parsed.second.q, 0.0)

        val gainResponse = bytes(0xBB, 0x0B, 0x00, 0x00, 0x17, 0x02, 0x00, 0xC9, 0xEE)
        assertEquals(-5.5, FiioJa11Protocol.globalGainFromResponse(gainResponse)!!, 0.000_001)
    }

    @Test
    fun completeBandsAlwaysOverwritesAllFiveSlotsWithFlatPadding() {
        val completed = FiioJa11Protocol.completeBands(
            listOf(Kt02h20Band("peak_dip", 1_000.0, 2.0, 1.0)),
        )

        assertEquals(5, completed.size)
        assertEquals(2.0, completed.first().gainDb, 0.0)
        completed.drop(1).forEach { band ->
            assertEquals("peak_dip", band.type)
            assertEquals(0.0, band.gainDb, 0.0)
            assertTrue(band.frequencyHz in 20.0..20_000.0)
        }
    }

    @Test
    fun malformedOrWrongCommandReadbackIsRejected() {
        assertNull(FiioJa11Protocol.bandFromResponse(bytes(0x02, 0xAA, 0x0A, 0x00)))
        assertNull(FiioJa11Protocol.globalGainFromResponse(bytes(0x02, 0xBB, 0x0B, 0x00, 0x00, 0x15, 0x00, 0x00)))
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
        (values[index] and 0xFF).toByte()
    }
}
