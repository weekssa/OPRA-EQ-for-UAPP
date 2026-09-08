package com.weekssa.opraeqforuapp.domain.kt02h20

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Independently implemented codec for the observable FiiO JA11/KT02H20 run-mode PEQ HID protocol.
 *
 * This object deliberately contains only PEQ/global-preamp/apply/persist/readback commands. Firmware
 * update/bootloader and unrelated DAC controls are outside EQ Library's protocol surface.
 */
object FiioJa11Protocol {
    const val VENDOR_ID = 0x2972
    const val PRODUCT_ID = 0x0102
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

    private const val SET_1 = 0xAA
    private const val SET_2 = 0x0A
    private const val READ_1 = 0xBB
    private const val READ_2 = 0x0B
    private const val FOOTER = 0xEE

    private const val CMD_FILTER = 0x15
    private const val CMD_GLOBAL_GAIN = 0x17
    private const val CMD_APPLY = 0x18
    private const val CMD_SAVE = 0x19

    private const val TYPE_PEAK = 0
    private const val TYPE_LOW_SHELF = 1
    private const val TYPE_HIGH_SHELF = 2

    private const val GLOBAL_GAIN_RAW_PER_DB = 2560.0

    data class Band(
        val type: String,
        val frequencyHz: Double,
        val gainDb: Double,
        val q: Double,
    )

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
        val raw = (gainDb * GLOBAL_GAIN_RAW_PER_DB).roundToInt().toSigned16Raw()
        val packet = byteArrayOf(
            SET_1.b(), SET_2.b(), 0, 0, CMD_GLOBAL_GAIN.b(), 2,
            (raw and 0xFF).b(), ((raw ushr 8) and 0xFF).b(), 0, FOOTER.b(),
        )
        return wire(packet)
    }

    fun applyReport(): ByteArray =
        wire(byteArrayOf(SET_1.b(), SET_2.b(), 0, 0, CMD_APPLY.b(), 1, 1, 0, FOOTER.b()))

    fun saveToFlashReport(): ByteArray =
        wire(byteArrayOf(SET_1.b(), SET_2.b(), 0, 0, CMD_SAVE.b(), 1, 3, 0, FOOTER.b()))

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
        val rawUnsigned = packet[6].u8() or (packet[7].u8() shl 8)
        val raw = if (rawUnsigned >= 0x8000) rawUnsigned - 0x10000 else rawUnsigned
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

    private fun Int.toSigned16Raw(): Int {
        require(this in Short.MIN_VALUE.toInt()..Short.MAX_VALUE.toInt())
        return this and 0xFFFF
    }

    private fun Byte.u8(): Int = toInt() and 0xFF
    private fun Int.b(): Byte = (this and 0xFF).toByte()

    private val SUPPORTED_TYPES = setOf("peak_dip", "low_shelf", "high_shelf")
    private val FLAT_BANDS = listOf(80.0, 250.0, 1_000.0, 4_000.0, 12_000.0)
        .map { frequency -> Band("peak_dip", frequency, 0.0, 0.7) }
}
