package com.weekssa.opraeqforuapp.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300ProvisionalProtocolTest {
    @Test
    fun `safety gate uses exact owner-captured descriptor`() {
        assertEquals(74, Ew300ProvisionalProtocol.EXPECTED_HID_DESCRIPTOR.size)
        assertEquals(
            "05 0C 09 01 A1 01 85 01 15 00 25 01 75 01 95 02 09 E9 09 EA 81 02 " +
                "95 04 09 CD 09 CF 09 B6 09 B5 81 02 95 02 81 01 06 01 FF 85 4B 75 " +
                "08 95 0A 09 01 81 03 95 0A 09 02 91 02 85 54 75 08 95 0A 09 03 " +
                "81 03 95 0A 09 04 91 02 C0",
            Ew300ProvisionalProtocol.EXPECTED_HID_DESCRIPTOR.toHex(),
        )
    }

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
    fun `owner stock snapshot is preserved byte for byte`() {
        assertEquals(
            listOf(0x24, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x66),
            Ew300ProvisionalProtocol.STOCK_RESPONSE_PAYLOADS.keys.toList(),
        )
        assertTrue(
            Ew300ProvisionalProtocol.matchesStockSnapshot(
                Ew300ProvisionalProtocol.STOCK_RESPONSE_PAYLOADS.mapValues { it.value.copyOf() },
            ),
        )
        assertEquals(
            emptyList<Int>(),
            Ew300ProvisionalProtocol.stockSnapshotMismatchRegisters(
                Ew300ProvisionalProtocol.STOCK_RESPONSE_PAYLOADS.mapValues { it.value.copyOf() },
            ),
        )
        assertEquals(
            "2E 00 00 00 52 00 05 00 58 1B",
            Ew300ProvisionalProtocol.STOCK_RESPONSE_PAYLOADS.getValue(0x2E).toHex(),
        )
        assertEquals(
            "66 00 00 00 52 00 F8 F8 00 00",
            Ew300ProvisionalProtocol.STOCK_RESPONSE_PAYLOADS.getValue(0x66).toHex(),
        )
    }

    @Test
    fun `stock gate reports the exact mismatched register and remains fail closed`() {
        val responses = Ew300ProvisionalProtocol.STOCK_RESPONSE_PAYLOADS.mapValues { it.value.copyOf() }
            .toMutableMap()
        responses[0x2C] = responses.getValue(0x2C).also { it[6] = 0 }

        assertEquals(listOf(0x2C), Ew300ProvisionalProtocol.stockSnapshotMismatchRegisters(responses))
        assertTrue(!Ew300ProvisionalProtocol.matchesStockSnapshot(responses))
    }

    @Test
    fun `stock gate accepts the owner captured 0x2F response byte for byte`() {
        val responses = Ew300ProvisionalProtocol.STOCK_RESPONSE_PAYLOADS.mapValues { it.value.copyOf() }
            .toMutableMap()
        responses[0x2F] = byteArrayOf(0x2F, 0, 0, 0, 0x52, 0, 0xF4.toByte(), 0x01, 0, 0)

        assertTrue(Ew300ProvisionalProtocol.matchesStockSnapshot(responses))
        assertEquals(emptyList<Int>(), Ew300ProvisionalProtocol.stockSnapshotMismatchRegisters(responses))
    }

    @Test
    fun `approved remaining field batch stays within captured EQ field bytes`() {
        assertEquals(8, Ew300ProvisionalProtocol.APPROVED_REVERSIBLE_PROBES.size)
        assertEquals(
            listOf(0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F),
            Ew300ProvisionalProtocol.APPROVED_REVERSIBLE_PROBES.map { it.register },
        )
        assertEquals("F7 FF C8 00", Ew300ProvisionalProtocol.stockData(0x28).toHex())
        assertEquals(
            listOf(
                "F7 FF C9 00",
                "2A 03 00 00",
                "FC FF 2D 01",
                "F2 03 00 00",
                "D0 FF 41 1F",
                "E6 05 00 00",
                "05 00 59 1B",
                "FE 01 00 00",
            ),
            Ew300ProvisionalProtocol.APPROVED_REVERSIBLE_PROBES.map { it.temporaryData.toHex() },
        )
        Ew300ProvisionalProtocol.APPROVED_REVERSIBLE_PROBES.forEach { probe ->
            val stock = Ew300ProvisionalProtocol.stockData(probe.register)
            assertEquals(
                stock.size,
                probe.temporaryData.size,
            )
            assertEquals(
                "${probe.label} must change exactly one raw field byte",
                1,
                stock.indices.count { index -> stock[index] != probe.temporaryData[index] },
            )
            val expectedChangedIndex = if (probe.register % 2 == 0) 2 else 0
            assertTrue(
                "${probe.label} must leave gain and filter-type bytes untouched",
                stock.indices.filter { index -> stock[index] != probe.temporaryData[index] }
                    .all { index -> index == expectedChangedIndex },
            )
        }
        assertEquals(
            "28 00 00 00 57 00 F7 FF C9 00",
            Ew300ProvisionalProtocol.writePayload(
                0x28,
                Ew300ProvisionalProtocol.APPROVED_REVERSIBLE_PROBES.first().temporaryData,
            ).toHex(),
        )
        assertEquals(
            "28 00 00 00 52 00 F7 FF C9 00",
            Ew300ProvisionalProtocol.expectedReadPayload(
                0x28,
                Ew300ProvisionalProtocol.APPROVED_REVERSIBLE_PROBES.first().temporaryData,
            ).toHex(),
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
