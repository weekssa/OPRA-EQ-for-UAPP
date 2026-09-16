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
}
