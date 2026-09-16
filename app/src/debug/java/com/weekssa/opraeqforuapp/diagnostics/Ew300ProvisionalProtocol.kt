package com.weekssa.opraeqforuapp.diagnostics

internal object Ew300ProvisionalProtocol {
    const val REPORT_ID = 0x4B
    const val COMMAND_READ = 0x52
    const val CURRENT_SLOT_REGISTER = 0x24
    const val FIRST_FILTER_REGISTER = 0x26
    const val GLOBAL_GAIN_REGISTER = 0x66
    const val FILTER_COUNT = 5
    const val PAYLOAD_SIZE = 10
    const val WIRE_REPORT_SIZE = PAYLOAD_SIZE + 1

    val EXPECTED_HID_DESCRIPTOR = byteArrayOf(
        0x05, 0x0C, 0x09, 0x01, 0xA1.toByte(), 0x01, 0x85.toByte(), 0x01,
        0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x02,
        0x09, 0xE9.toByte(), 0x09, 0xEA.toByte(), 0x81.toByte(), 0x02,
        0x95.toByte(), 0x04, 0x09, 0xCD.toByte(), 0x09, 0xCF.toByte(),
        0x09, 0xB6.toByte(), 0x09, 0xB5.toByte(), 0x81.toByte(), 0x02,
        0x95.toByte(), 0x02, 0x81.toByte(), 0x01, 0x06, 0x01, 0xFF.toByte(),
        0x85.toByte(), 0x4B, 0x75, 0x08, 0x95.toByte(), 0x0A, 0x09, 0x01,
        0x81.toByte(), 0x03, 0x95.toByte(), 0x0A, 0x09, 0x02, 0x91.toByte(),
        0x02, 0x85.toByte(), 0x54, 0x75, 0x08, 0x95.toByte(), 0x0A,
        0x09, 0x03, 0x81.toByte(), 0x03, 0x95.toByte(), 0x0A, 0x09, 0x04,
        0x91.toByte(), 0x02, 0xC0.toByte(),
    )

    val STOCK_RESPONSE_PAYLOADS: Map<Int, ByteArray> = linkedMapOf(
        0x24 to hex("24 00 00 00 52 00 01 00 00 00"),
        0x26 to hex("26 00 00 00 52 00 F5 FF 64 00"),
        0x27 to hex("27 00 00 00 52 00 20 03 00 00"),
        0x28 to hex("28 00 00 00 52 00 F7 FF C8 00"),
        0x29 to hex("29 00 00 00 52 00 20 03 00 00"),
        0x2A to hex("2A 00 00 00 52 00 FC FF 2C 01"),
        0x2B to hex("2B 00 00 00 52 00 E8 03 00 00"),
        0x2C to hex("2C 00 00 00 52 00 D0 FF 40 1F"),
        0x2D to hex("2D 00 00 00 52 00 DC 05 00 00"),
        0x2E to hex("2E 00 00 00 52 00 FB FF 58 1B"),
        0x2F to hex("2F 00 00 00 52 00 F4 01 00 00"),
        0x66 to hex("66 00 00 00 52 00 F8 F8 00 00"),
    )

    fun readPayload(register: Int, slotHint: Int = 0): ByteArray = byteArrayOf(
        register.toByte(),
        0,
        0,
        0,
        COMMAND_READ.toByte(),
        0,
        slotHint.toByte(),
        0,
        0,
        0,
    )

    fun wireReport(payload: ByteArray): ByteArray {
        require(payload.size == PAYLOAD_SIZE)
        return byteArrayOf(REPORT_ID.toByte()) + payload
    }

    fun responsePayload(wireReport: ByteArray, expectedRegister: Int): ByteArray? {
        if (wireReport.size != WIRE_REPORT_SIZE) return null
        if (wireReport[0].toUnsignedInt() != REPORT_ID) return null
        val payload = wireReport.copyOfRange(1, wireReport.size)
        if (payload[0].toUnsignedInt() != expectedRegister) return null
        if (payload[4].toUnsignedInt() != COMMAND_READ) return null
        return payload
    }

    fun snapshotRegisters(): List<Int> = buildList {
        add(CURRENT_SLOT_REGISTER)
        repeat(FILTER_COUNT * 2) { add(FIRST_FILTER_REGISTER + it) }
        add(GLOBAL_GAIN_REGISTER)
    }

    fun matchesStockSnapshot(responses: Map<Int, ByteArray>): Boolean =
        STOCK_RESPONSE_PAYLOADS.all { (register, expected) ->
            responses[register]?.contentEquals(expected) == true
        }

    fun describe(register: Int, payload: ByteArray): String = when {
        register == CURRENT_SLOT_REGISTER -> "current slot=${payload[6].toUnsignedInt()}"
        register == GLOBAL_GAIN_REGISTER -> "global gain raw=${payload[6].toSignedInt()}"
        (register - FIRST_FILTER_REGISTER) in 0 until FILTER_COUNT * 2 &&
            (register - FIRST_FILTER_REGISTER) % 2 == 0 -> {
            val gainRaw = payload.leSigned16(6)
            val frequencyRaw = payload.leUnsigned16(8)
            "band ${(register - FIRST_FILTER_REGISTER) / 2 + 1} gain=${gainRaw / 10.0} dB; " +
                "frequency raw=$frequencyRaw (fallback interpretation=${frequencyRaw * 2} Hz)"
        }
        (register - FIRST_FILTER_REGISTER) in 0 until FILTER_COUNT * 2 -> {
            val qRaw = payload.leUnsigned16(6)
            "band ${(register - FIRST_FILTER_REGISTER) / 2 + 1} Q=${qRaw / 1000.0}; " +
                "filter type raw=${payload[8].toUnsignedInt()}"
        }
        else -> "unrecognized register"
    }

    private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF

    private fun Byte.toSignedInt(): Int = toInt()

    private fun ByteArray.leUnsigned16(offset: Int): Int =
        this[offset].toUnsignedInt() or (this[offset + 1].toUnsignedInt() shl 8)

    private fun ByteArray.leSigned16(offset: Int): Int =
        leUnsigned16(offset).let { if (it > 0x7FFF) it - 0x10000 else it }

    private fun hex(value: String): ByteArray = value
        .split(" ")
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
