package com.weekssa.opraeqforuapp.domain.ew300

import org.junit.Assert.assertEquals
import org.junit.Test

class Ew300ProvisionalDecoderTest {
    @Test
    fun decodesObservedPublicFallbackWordsWithoutHidingRawType() {
        val values = Ew300ProvisionalDecoder.decode(
            Ew300VolatileTransaction.RawField.of(0x26, bytes(0xF5, 0xFF, 0x64, 0x00)),
            Ew300VolatileTransaction.RawField.of(0x27, bytes(0x20, 0x03, 0x00, 0x00)),
        )

        assertEquals(1, values.bandIndex)
        assertEquals(-1.1, values.gainDb, 0.0001)
        assertEquals(100, values.frequencyRaw)
        assertEquals(100.0, values.referenceFrequencyHz, 0.0001)
        assertEquals(0.8, values.q, 0.0001)
        assertEquals(0, values.filterTypeRaw)
        assertEquals(Ew300QualifiedVolatileProtocol.ProvisionalFilterType.PEAK, values.filterType)
    }

    @Test
    fun mapsOnlyTheFourPhysicallyTestedNonzeroTypeCodes() {
        val values = Ew300ProvisionalDecoder.decode(
            Ew300VolatileTransaction.RawField.of(0x2E, bytes(0x05, 0x00, 0x58, 0x1B)),
            Ew300VolatileTransaction.RawField.of(0x2F, bytes(0x00, 0x00, 0x04, 0x00)),
        )

        assertEquals(5, values.bandIndex)
        assertEquals(7000.0, values.referenceFrequencyHz, 0.0001)
        assertEquals(Ew300QualifiedVolatileProtocol.ProvisionalFilterType.HIGH_SHELF, values.filterType)
    }

    @Test
    fun rejectsNonAdjacentOrOddGainFields() {
        val validQ = Ew300VolatileTransaction.RawField.of(0x27, bytes(0, 0, 0, 0))
        assertFails {
            Ew300ProvisionalDecoder.decode(
                Ew300VolatileTransaction.RawField.of(0x27, bytes(0, 0, 0, 0)),
                validQ,
            )
        }
        assertFails {
            Ew300ProvisionalDecoder.decode(
                Ew300VolatileTransaction.RawField.of(0x26, bytes(0, 0, 0, 0)),
                Ew300VolatileTransaction.RawField.of(0x29, bytes(0, 0, 0, 0)),
            )
        }
    }

    private fun assertFails(block: () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("Expected IllegalArgumentException")
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
        (values[index] and 0xFF).toByte()
    }
}
