package com.weekssa.opraeqforuapp.diagnostics

/**
 * One consolidated, debug-only persistence qualification marker.
 *
 * The marker changes one raw byte in every qualified band field, changes Band 1's provisional
 * type byte to code 3, and changes one raw global-gain byte. It is deliberately not a production
 * EQ profile. The caller must read the complete stock state first, verify every marker readback,
 * send the public web-tool's provisional COMMIT command once, and restore the captured bytes.
 */
internal object Ew300PersistenceQualification {
    const val COMMIT_COMMAND = 0x53

    val MARKER_FIELDS: Map<Int, ByteArray> = linkedMapOf(
        0x26 to hex("F6 FF 64 00"),
        0x27 to hex("20 03 03 00"),
        0x28 to hex("F7 FF C9 00"),
        0x29 to hex("2A 03 00 00"),
        0x2A to hex("FC FF 2D 01"),
        0x2B to hex("F2 03 00 00"),
        0x2C to hex("D0 FF 41 1F"),
        0x2D to hex("E6 05 00 00"),
        0x2E to hex("05 00 59 1B"),
        0x2F to hex("FE 01 00 00"),
        0x66 to hex("F9 F8 00 00"),
    )

    fun commitReport(): ByteArray = Ew300ProvisionalProtocol.wireReport(
        byteArrayOf(0, 0, 0, 0, COMMIT_COMMAND.toByte(), 0, 0, 0, 0, 0),
    )

    private fun hex(value: String): ByteArray = value
        .split(" ")
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
