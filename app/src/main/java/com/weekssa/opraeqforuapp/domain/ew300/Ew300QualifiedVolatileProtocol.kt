package com.weekssa.opraeqforuapp.domain.ew300

/**
 * Exact, raw transport evidence from the owner's SIMGOT EW300 DSP cable.
 *
 * This is intentionally not a production DAC adapter. It does not decode acoustic values, infer a
 * filter type, provide a persistence command, or register the cable as a supported hardware target.
 * It exists so the already-qualified volatile read/write/readback/restore boundary has one tested
 * source of truth before a user-facing implementation is considered.
 */
object Ew300QualifiedVolatileProtocol {
    const val REPORT_ID = 0x4B
    const val REPORT_SIZE = 11
    const val READ_COMMAND = 0x52
    const val WRITE_COMMAND = 0x57
    const val BAND_COUNT = 5

    /**
     * Raw filter-type codes observed during the owner's reversible qualification. The labels are
     * still provisional and must not be presented as production acoustic claims until measured.
     */
    enum class ProvisionalFilterType(val rawCode: Int, val label: String) {
        LPF(1, "LPF"),
        HPF(2, "HPF"),
        LOW_SHELF(3, "low-shelf"),
        HIGH_SHELF(4, "high-shelf"),
    }

    private const val FIRST_BAND_REGISTER = 0x26
    private const val LAST_BAND_REGISTER = 0x2F
    private const val DATA_OFFSET = 7

    /**
     * The only writable addresses established by physical qualification. Each contains a four-byte
     * band field. Slot, global gain, commit, clear, save, reset, and firmware controls are absent.
     */
    fun qualifiedVolatileRegisterAddresses(): List<Int> =
        (FIRST_BAND_REGISTER..LAST_BAND_REGISTER).toList()

    fun isQualifiedVolatileRegister(address: Int): Boolean =
        address in FIRST_BAND_REGISTER..LAST_BAND_REGISTER

    fun provisionalFilterType(rawCode: Int): ProvisionalFilterType? =
        ProvisionalFilterType.entries.firstOrNull { it.rawCode == rawCode }

    /** Changes only the observed type byte; Q/reserved bytes are preserved exactly. */
    fun withProvisionalFilterType(data: ByteArray, type: ProvisionalFilterType): ByteArray {
        require(data.size == 4) { "An EW300 band field must contain exactly four bytes." }
        return data.copyOf().also { it[2] = type.rawCode.toByte() }
    }

    fun readReport(address: Int): ByteArray {
        require(address in 0..0xFF) { "EW300 register address must fit one byte." }
        return byteArrayOf(
            REPORT_ID.byte(),
            address.byte(), 0, 0, 0,
            READ_COMMAND.byte(),
            0, 0, 0, 0, 0,
        )
    }

    fun temporaryWriteReport(address: Int, data: ByteArray): ByteArray {
        require(isQualifiedVolatileRegister(address)) {
            "Only physically qualified EW300 volatile band registers may be written."
        }
        require(data.size == 4) { "An EW300 band field must contain exactly four bytes." }
        return byteArrayOf(
            REPORT_ID.byte(),
            address.byte(), 0, 0, 0,
            WRITE_COMMAND.byte(),
            0,
            data[0], data[1], data[2], data[3],
        )
    }

    /** Returns the four raw band bytes only when this is the exact expected READ response. */
    fun readbackData(address: Int, report: ByteArray): ByteArray? {
        if (report.size != REPORT_SIZE || report[0].unsigned() != REPORT_ID) return null
        if (report[1].unsigned() != address || report[5].unsigned() != READ_COMMAND) return null
        return report.copyOfRange(DATA_OFFSET, REPORT_SIZE)
    }

    fun matchesExpectedReadback(address: Int, expectedData: ByteArray, report: ByteArray): Boolean =
        expectedData.size == 4 && readbackData(address, report)?.contentEquals(expectedData) == true

    private fun Int.byte(): Byte = (this and 0xFF).toByte()
    private fun Byte.unsigned(): Int = toInt() and 0xFF
}
