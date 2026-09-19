package com.weekssa.opraeqforuapp.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300PersistenceQualificationTest {
    @Test
    fun markerChangesOnlyPreviouslyQualifiedBytesAndGlobalGain() {
        assertEquals(
            listOf(0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x66),
            Ew300PersistenceQualification.MARKER_FIELDS.keys.toList(),
        )
        assertEquals("4B 00 00 00 00 53 00 00 00 00 00", Ew300PersistenceQualification.commitReport().toHex())
        Ew300PersistenceQualification.MARKER_FIELDS.forEach { (register, marker) ->
            val stock = if (register == 0x66) {
                byteArrayOf(0xF8.toByte(), 0xF8.toByte(), 0, 0)
            } else {
                Ew300ProvisionalProtocol.stockData(register)
            }
            assertEquals(4, marker.size)
            assertEquals(
                "marker for 0x%02X must change exactly one raw byte".format(register),
                1,
                stock.indices.count { stock[it] != marker[it] },
            )
            assertTrue(marker.contentEquals(marker.copyOf()))
        }
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
}
