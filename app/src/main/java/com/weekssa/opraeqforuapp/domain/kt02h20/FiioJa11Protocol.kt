package com.weekssa.opraeqforuapp.domain.kt02h20

import kotlin.math.roundToInt

/**
 * Clean-room codec for the observable FiiO/JadeAudio JA11 run-mode HID protocol.
 *
 * Software support is intentionally broader than the physically-qualified surface: PEQ/global EQ
 * gain, ordinary device volume, firmware/sample-rate readback, headset/remote control, active EQ
 * program, and UAC mode are encoded only from independently corroborated run-mode protocol facts.
 * Physical qualification remains a separate gate. There are deliberately no firmware-update,
 * bootloader, USB-identity mutation, raw-register, or speculative SPDIF commands here.
 */
object FiioJa11Protocol {
    const val VENDOR_ID = 0x2972
    const val PRODUCT_ID_UAC_1 = 0x0101
    const val PRODUCT_ID_UAC_2 = 0x0102

    /** Compatibility alias for older code/tests that referred only to the UAC 2.0 identity. */
    const val PRODUCT_ID = PRODUCT_ID_UAC_2

    val SUPPORTED_PRODUCT_IDS: Set<Int> = setOf(PRODUCT_ID_UAC_1, PRODUCT_ID_UAC_2)

    const val REPORT_ID = 0x02
    const val BAND_COUNT = 5

    const val MIN_FREQUENCY_HZ = 20.0
    const val MAX_FREQUENCY_HZ = 20_000.0
    const val MIN_BAND_GAIN_DB = -24.0
    const val MAX_BAND_GAIN_DB = 12.0
    const val MIN_Q = 0.1
    const val MAX_Q = 10.0
    const val MIN_GLOBAL_GAIN_DB = -12.0
    const val MAX_GLOBAL_GAIN_DB = 12.0
    const val MIN_OUTPUT_VOLUME = 0
    const val MAX_OUTPUT_VOLUME = 60

    private const val SET_1 = 0xAA
    private const val SET_2 = 0x0A
    private const val READ_1 = 0xBB
    private const val READ_2 = 0x0B
    private const val FOOTER = 0xEE

    private const val CMD_OUTPUT_VOLUME = 0x02
    private const val CMD_SAMPLE_RATE = 0x09
    private const val CMD_FIRMWARE = 0x0B
    private const val CMD_HEADSET_CONTROL = 0x12
    private const val CMD_FILTER = 0x15
    private const val CMD_EQ_PROGRAM = 0x16
    private const val CMD_GLOBAL_GAIN = 0x17
    private const val CMD_APPLY = 0x18
    private const val CMD_SAVE = 0x19
    private const val CMD_UAC_MODE = 0x20

    private const val TYPE_PEAK = 0
    private const val TYPE_LOW_SHELF = 1
    private const val TYPE_HIGH_SHELF = 2

    /** JA11 command 0x17 uses signed tenths of a dB, transmitted high byte first. */
    private const val GLOBAL_GAIN_RAW_PER_DB = 10.0

    enum class EqProgram(val code: Int, val technicalLabel: String) {
        VOCAL(0, "Vocal"),
        CLASSIC(1, "Classic"),
        BASS(2, "Bass"),
        USER_1(3, "User 1"),
        OFF(4, "Off"),
        ;

        companion object {
            fun fromCode(code: Int): EqProgram? = entries.firstOrNull { it.code == code }
        }
    }

    enum class UacMode(val code: Int, val productId: Int, val technicalLabel: String) {
        UAC_1(0, PRODUCT_ID_UAC_1, "UAC 1.0"),
        UAC_2(1, PRODUCT_ID_UAC_2, "UAC 2.0"),
        ;

        companion object {
            fun fromCode(code: Int): UacMode? = entries.firstOrNull { it.code == code }
            fun fromProductId(productId: Int): UacMode? = entries.firstOrNull { it.productId == productId }
        }
    }

    data class Band(
        val type: String,
        val frequencyHz: Double,
        val gainDb: Double,
        val q: Double,
    )

    fun supportsProductId(productId: Int): Boolean = productId in SUPPORTED_PRODUCT_IDS

    fun readOutputVolumeReport(): ByteArray = readOneByteReport(CMD_OUTPUT_VOLUME)

    fun writeOutputVolumeReport(level: Int): ByteArray {
        require(level in MIN_OUTPUT_VOLUME..MAX_OUTPUT_VOLUME) {
            "JA11 output volume must be in $MIN_OUTPUT_VOLUME..$MAX_OUTPUT_VOLUME."
        }
        return writeOneByteReport(CMD_OUTPUT_VOLUME, level)
    }

    fun readSampleRateReport(): ByteArray = readOneByteReport(CMD_SAMPLE_RATE)

    fun readFirmwareVersionReport(): ByteArray = readOneByteReport(CMD_FIRMWARE)

    fun readHeadsetControlReport(): ByteArray = readOneByteReport(CMD_HEADSET_CONTROL)

    fun writeHeadsetControlReport(enabled: Boolean): ByteArray =
        writeOneByteReport(CMD_HEADSET_CONTROL, if (enabled) 1 else 0)

    fun readEqProgramReport(): ByteArray = readOneByteReport(CMD_EQ_PROGRAM)

    fun writeEqProgramReport(program: EqProgram): ByteArray =
        writeOneByteReport(CMD_EQ_PROGRAM, program.code)

    fun readUacModeReport(): ByteArray = readOneByteReport(CMD_UAC_MODE)

    fun writeUacModeReport(mode: UacMode): ByteArray =
        writeOneByteReport(CMD_UAC_MODE, mode.code)

    fun readBandReport(index: Int): ByteArray {
        require(index in 0 until BAND_COUNT)
        return wire(byteArrayOf(READ_1.b(), READ_2.b(), 0, 0, CMD_FILTER.b(), 1, index.b(), FOOTER.b()))
    }

    fun readGlobalGainReport(): ByteArray =
        wire(byteArrayOf(READ_1.b(), READ_2.b(), 0, 0, CMD_GLOBAL_GAIN.b(), 0, 0, FOOTER.b()))

    fun writeBandReport(index: Int, band: Band): ByteArray {
        require(index in 0 until BAND_COUNT)
        validateBand(band)
        val gainRaw = (band.gainDb * 10.0).roundToInt().toSigned16Raw()
        val frequencyRaw = band.frequencyHz.roundToInt()
        val qRaw = (band.q * 100.0).roundToInt()
        val packet = byteArrayOf(
            SET_1.b(), SET_2.b(), 0, 0, CMD_FILTER.b(), 8,
            index.b(),
            ((gainRaw ushr 8) and 0xFF).b(), (gainRaw and 0xFF).b(),
            ((frequencyRaw ushr 8) and 0xFF).b(), (frequencyRaw and 0xFF).b(),
            ((qRaw ushr 8) and 0xFF).b(), (qRaw and 0xFF).b(),
            typeCode(band.type).b(), 0, FOOTER.b(),
        )
        return wire(packet)
    }

    fun writeGlobalGainReport(gainDb: Double): ByteArray {
        require(gainDb.isFinite() && gainDb in MIN_GLOBAL_GAIN_DB..MAX_GLOBAL_GAIN_DB) {
            "JA11 global preamp is outside the current validated range."
        }
        val raw = globalGainRaw(gainDb)
        val packet = byteArrayOf(
            SET_1.b(), SET_2.b(), 0, 0, CMD_GLOBAL_GAIN.b(), 2,
            ((raw ushr 8) and 0xFF).b(), (raw and 0xFF).b(), 0, FOOTER.b(),
        )
        return wire(packet)
    }

    /** The exact dB value represented by the signed 16-bit JA11 global-gain field. */
    fun quantizedGlobalGainDb(gainDb: Double): Double {
        require(gainDb.isFinite() && gainDb in MIN_GLOBAL_GAIN_DB..MAX_GLOBAL_GAIN_DB) {
            "JA11 global preamp is outside the current validated range."
        }
        return globalGainRaw(gainDb) / GLOBAL_GAIN_RAW_PER_DB
    }

    fun applyReport(): ByteArray =
        wire(byteArrayOf(SET_1.b(), SET_2.b(), 0, 0, CMD_APPLY.b(), 1, 1, 0, FOOTER.b()))

    fun saveToFlashReport(): ByteArray =
        wire(byteArrayOf(SET_1.b(), SET_2.b(), 0, 0, CMD_SAVE.b(), 1, 3, 0, FOOTER.b()))

    fun outputVolumeFromResponse(report: ByteArray): Int? =
        oneByteValueFromResponse(report, CMD_OUTPUT_VOLUME)
            ?.takeIf { it in MIN_OUTPUT_VOLUME..MAX_OUTPUT_VOLUME }

    fun sampleRateCodeFromResponse(report: ByteArray): Int? =
        oneByteValueFromResponse(report, CMD_SAMPLE_RATE)?.takeIf { it in SAMPLE_RATE_LABELS.indices }

    fun sampleRateLabelFromResponse(report: ByteArray): String? =
        sampleRateCodeFromResponse(report)?.let(SAMPLE_RATE_LABELS::get)

    fun firmwareVersionFromResponse(report: ByteArray): String? {
        val packet = packetView(report) ?: return null
        if (packet.size < 8 || packet[4].u8() != CMD_FIRMWARE) return null
        val major = packet[6].u8()
        val minor = packet[7].u8()
        return "$major.${minor.toString().padStart(2, '0')}"
    }

    fun headsetControlFromResponse(report: ByteArray): Boolean? =
        when (oneByteValueFromResponse(report, CMD_HEADSET_CONTROL)) {
            0 -> false
            1 -> true
            else -> null
        }

    fun eqProgramFromResponse(report: ByteArray): EqProgram? =
        oneByteValueFromResponse(report, CMD_EQ_PROGRAM)?.let(EqProgram::fromCode)

    fun uacModeFromResponse(report: ByteArray): UacMode? =
        oneByteValueFromResponse(report, CMD_UAC_MODE)?.let(UacMode::fromCode)

    fun bandFromResponse(report: ByteArray): Pair<Int, Band>? {
        val packet = packetView(report) ?: return null
        if (packet.size < 15 || packet[4].u8() != CMD_FILTER) return null
        val index = packet[6].u8()
        if (index !in 0 until BAND_COUNT) return null
        val gainRaw = signed16(packet[7].u8(), packet[8].u8())
        val frequency = (packet[9].u8() shl 8) or packet[10].u8()
        val qRaw = (packet[11].u8() shl 8) or packet[12].u8()
        val type = typeName(packet[13].u8() and 0x03) ?: return null
        val band = Band(
            type = type,
            frequencyHz = frequency.toDouble(),
            gainDb = gainRaw.toDouble() / 10.0,
            q = qRaw.toDouble() / 100.0,
        )
        return index to band
    }

    fun globalGainFromResponse(report: ByteArray): Double? {
        val packet = packetView(report) ?: return null
        if (packet.size < 8 || packet[4].u8() != CMD_GLOBAL_GAIN) return null
        val raw = signed16(packet[6].u8(), packet[7].u8())
        val gain = raw.toDouble() / GLOBAL_GAIN_RAW_PER_DB
        return gain.takeIf { it.isFinite() && it in MIN_GLOBAL_GAIN_DB..MAX_GLOBAL_GAIN_DB }
    }

    fun completeBands(bands: List<Kt02h20Band>): List<Band> {
        require(bands.size <= BAND_COUNT)
        val mapped = bands.map { band -> Band(band.type, band.frequencyHz, band.gainDb, band.q) }
        return mapped + FLAT_BANDS.drop(mapped.size)
    }

    fun nearlyMatches(expected: Band, actual: Band): Boolean =
        expected.type == actual.type &&
            kotlin.math.abs(expected.frequencyHz - actual.frequencyHz) <= 0.5 &&
            kotlin.math.abs(expected.gainDb - actual.gainDb) <= 0.051 &&
            kotlin.math.abs(expected.q - actual.q) <= 0.0051

    private fun readOneByteReport(command: Int): ByteArray {
        require(command in 0..0xFF)
        return wire(byteArrayOf(READ_1.b(), READ_2.b(), 0, 0, command.b(), 0, 0, FOOTER.b()))
    }

    private fun writeOneByteReport(command: Int, value: Int): ByteArray {
        require(command in 0..0xFF)
        require(value in 0..0xFF)
        return wire(byteArrayOf(SET_1.b(), SET_2.b(), 0, 0, command.b(), 1, value.b(), 0, FOOTER.b()))
    }

    private fun oneByteValueFromResponse(report: ByteArray, expectedCommand: Int): Int? {
        val packet = packetView(report) ?: return null
        if (packet.size < 7 || packet[4].u8() != expectedCommand) return null
        return packet[6].u8()
    }

    private fun validateBand(band: Band) {
        require(band.type in SUPPORTED_TYPES)
        require(band.frequencyHz.isFinite() && band.frequencyHz in MIN_FREQUENCY_HZ..MAX_FREQUENCY_HZ)
        require(band.gainDb.isFinite() && band.gainDb in MIN_BAND_GAIN_DB..MAX_BAND_GAIN_DB)
        require(band.q.isFinite() && band.q in MIN_Q..MAX_Q)
    }

    private fun wire(packet: ByteArray): ByteArray = ByteArray(packet.size + 1).also { wire ->
        wire[0] = REPORT_ID.b()
        packet.copyInto(wire, destinationOffset = 1)
    }

    private fun packetView(report: ByteArray): ByteArray? {
        if (report.isEmpty()) return null
        val start = when {
            report.size >= 3 && report[0].u8() == REPORT_ID && report[1].u8() == READ_1 && report[2].u8() == READ_2 -> 1
            report.size >= 3 && report[1].u8() == READ_1 && report[2].u8() == READ_2 -> 1
            report.size >= 2 && report[0].u8() == READ_1 && report[1].u8() == READ_2 -> 0
            else -> return null
        }
        return report.copyOfRange(start, report.size)
    }

    private fun typeCode(type: String): Int = when (type) {
        "peak_dip" -> TYPE_PEAK
        "low_shelf" -> TYPE_LOW_SHELF
        "high_shelf" -> TYPE_HIGH_SHELF
        else -> error("Unsupported JA11 filter type")
    }

    private fun typeName(type: Int): String? = when (type) {
        TYPE_PEAK -> "peak_dip"
        TYPE_LOW_SHELF -> "low_shelf"
        TYPE_HIGH_SHELF -> "high_shelf"
        else -> null
    }

    private fun signed16(high: Int, low: Int): Int {
        val raw = (high shl 8) or low
        return if (raw >= 0x8000) raw - 0x10000 else raw
    }

    private fun globalGainRaw(gainDb: Double): Int =
        (gainDb * GLOBAL_GAIN_RAW_PER_DB).toInt().toSigned16Raw()

    private fun Int.toSigned16Raw(): Int {
        require(this in Short.MIN_VALUE.toInt()..Short.MAX_VALUE.toInt())
        return this and 0xFFFF
    }

    private fun Byte.u8(): Int = toInt() and 0xFF
    private fun Int.b(): Byte = (this and 0xFF).toByte()

    private val SUPPORTED_TYPES = setOf("peak_dip", "low_shelf", "high_shelf")
    private val FLAT_BANDS = listOf(80.0, 250.0, 1_000.0, 4_000.0, 12_000.0)
        .map { frequency -> Band("peak_dip", frequency, 0.0, 0.7) }

    private val SAMPLE_RATE_LABELS = listOf(
        "32 kHz",
        "44.1 kHz",
        "48 kHz",
        "88.2 kHz",
        "96 kHz",
        "176.4 kHz",
        "192 kHz",
        "352.8 kHz",
        "384 kHz",
        "705.6 kHz",
        "768 kHz",
        "DSD64",
        "DSD128",
        "DSD256",
        "DSD512",
    )
}
