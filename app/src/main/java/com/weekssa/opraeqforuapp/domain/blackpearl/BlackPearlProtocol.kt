package com.weekssa.opraeqforuapp.domain.blackpearl

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Independently implemented packet codec for the observable TRN Black Pearl HID protocol used by
 * EQ Library. Besides PEQ data, direct Flash may use the device's global playback-gain command when
 * required to represent source preamp/headroom. No other DAC-control commands belong here.
 */
object BlackPearlProtocol {
    const val VENDOR_ID: Int = 0x3302
    const val PRODUCT_ID: Int = 0x43E8
    const val REPORT_SIZE: Int = 64
    const val BAND_COUNT: Int = 10

    const val GLOBAL_GAIN_MIN_RAW: Int = -9472
    const val GLOBAL_GAIN_MAX_RAW: Int = 6440
    const val GLOBAL_GAIN_RAW_PER_DB: Int = 256

    private const val REPORT_ID: Int = 0x4B
    private const val WRITE: Int = 0x01
    private const val READ: Int = 0x80
    private const val END: Int = 0x00
    private const val CMD_FLASH_EQ: Int = 0x01
    private const val CMD_GLOBAL_GAIN: Int = 0x03
    private const val CMD_PEQ_VALUES: Int = 0x09
    private const val CMD_LATCH: Int = 0x0A
    private const val TYPE_PEAK: Int = 0x02
    private const val TYPE_LOW_SHELF: Int = 0x03
    private const val TYPE_HIGH_SHELF: Int = 0x04
    private const val SAMPLE_RATE_HZ: Double = 48_000.0

    data class Band(
        val type: String,
        val frequencyHz: Double,
        val gainDb: Double,
        val q: Double,
    )

    fun readGlobalGainReport(): ByteArray = ByteArray(REPORT_SIZE).apply {
        this[0] = REPORT_ID.toByte()
        this[1] = READ.toByte()
        this[2] = CMD_GLOBAL_GAIN.toByte()
        this[3] = END.toByte()
        this[4] = 0x00
        this[5] = 0x00
        this[6] = END.toByte()
    }

    fun globalGainRawFromResponse(report: ByteArray): Int? {
        if (report.size < 6) return null
        if (report[0].u8() != REPORT_ID || report[1].u8() != READ || report[2].u8() != CMD_GLOBAL_GAIN) {
            return null
        }
        val raw = ByteBuffer.wrap(report, 4, 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .short
            .toInt()
        return raw.takeIf { it in GLOBAL_GAIN_MIN_RAW..GLOBAL_GAIN_MAX_RAW }
    }

    fun writeGlobalGainReport(rawGain: Int): ByteArray {
        require(rawGain in GLOBAL_GAIN_MIN_RAW..GLOBAL_GAIN_MAX_RAW) {
            "Black Pearl global gain is outside the validated hardware range."
        }
        return ByteArray(REPORT_SIZE).apply {
            this[0] = REPORT_ID.toByte()
            this[1] = WRITE.toByte()
            this[2] = CMD_GLOBAL_GAIN.toByte()
            this[3] = 0x03
            this[4] = (rawGain and 0xFF).toByte()
            this[5] = ((rawGain shr 8) and 0xFF).toByte()
            this[6] = END.toByte()
        }
    }

    fun gainDbToRawDelta(gainDb: Double): Int {
        require(gainDb.isFinite()) { "Black Pearl playback-gain adjustment must be finite." }
        return (gainDb * GLOBAL_GAIN_RAW_PER_DB).roundToInt()
    }

    fun rawDeltaToGainDb(rawDelta: Int): Double = rawDelta.toDouble() / GLOBAL_GAIN_RAW_PER_DB

    fun readBandReport(index: Int): ByteArray {
        require(index in 0 until BAND_COUNT) { "Black Pearl band index must be 0..9." }
        return ByteArray(REPORT_SIZE).apply {
            this[0] = REPORT_ID.toByte()
            this[1] = READ.toByte()
            this[2] = CMD_PEQ_VALUES.toByte()
            this[3] = 0x00
            this[4] = 0x00
            this[5] = index.toByte()
            this[6] = END.toByte()
        }
    }

    fun activeSlotFromBandResponse(report: ByteArray): Byte? {
        if (report.size < 37) return null
        if (report[0].u8() != REPORT_ID || report[1].u8() != READ || report[2].u8() != CMD_PEQ_VALUES) {
            return null
        }
        return report[36]
    }

    fun writeBandReport(index: Int, band: Band, activeSlot: Byte): ByteArray {
        require(index in 0 until BAND_COUNT) { "Black Pearl band index must be 0..9." }
        validateBand(band)
        val coefficients = coefficients(band)
        val report = ByteArray(REPORT_SIZE)
        report[0] = REPORT_ID.toByte()
        report[1] = WRITE.toByte()
        report[2] = CMD_PEQ_VALUES.toByte()
        report[3] = 0x18
        report[4] = 0x00
        report[5] = index.toByte()
        report[6] = 0x00
        report[7] = 0x00

        ByteBuffer.wrap(report).order(ByteOrder.LITTLE_ENDIAN).apply {
            position(8)
            coefficients.forEach { putFloat(it.toFloat()) }
            putShort(band.frequencyHz.roundToInt().toShort())
            putShort((band.q * 256.0).roundToInt().toShort())
            putShort((band.gainDb * 256.0).roundToInt().toShort())
            put(typeCode(band.type).toByte())
            put(0x00)
            put(activeSlot)
            put(END.toByte())
        }
        return report
    }

    fun latchReport(): ByteArray = ByteArray(REPORT_SIZE).apply {
        this[0] = REPORT_ID.toByte()
        this[1] = WRITE.toByte()
        this[2] = CMD_LATCH.toByte()
        this[3] = 0x04
        this[4] = 0xFF.toByte()
        this[5] = 0xFF.toByte()
        this[6] = 0xFF.toByte()
        this[7] = 0xFF.toByte()
        this[8] = END.toByte()
    }

    fun flashReport(): ByteArray = ByteArray(REPORT_SIZE).apply {
        this[0] = REPORT_ID.toByte()
        this[1] = WRITE.toByte()
        this[2] = CMD_FLASH_EQ.toByte()
        this[3] = 0x01
        this[4] = END.toByte()
    }

    /**
     * Produces a complete PEQ write sequence. Unused hardware bands are explicitly flattened so a
     * shorter preset cannot leave stale EQ bands from a previous preset active.
     */
    fun flashSequence(bands: List<Band>, activeSlot: Byte): List<ByteArray> {
        require(bands.size <= BAND_COUNT) { "Black Pearl supports at most 10 hardware EQ bands." }
        val padded = bands + FLAT_PADDING_BANDS.drop(bands.size)
        return buildList {
            padded.forEachIndexed { index, band -> add(writeBandReport(index, band, activeSlot)) }
            add(latchReport())
            add(flashReport())
        }
    }

    private fun validateBand(band: Band) {
        require(band.type in SUPPORTED_TYPES) { "Unsupported Black Pearl filter type: ${band.type}" }
        require(band.frequencyHz.isFinite() && band.frequencyHz in 20.0..20_000.0) {
            "Black Pearl frequency must be within 20 Hz..20 kHz."
        }
        require(band.gainDb.isFinite() && band.gainDb in -10.0..10.0) {
            "Black Pearl gain must be within -10 dB..+10 dB."
        }
        require(band.q.isFinite() && band.q in 0.1..10.0) {
            "Black Pearl Q must be within 0.1..10.0."
        }
    }

    private fun coefficients(band: Band): DoubleArray {
        val a = Math.pow(10.0, band.gainDb / 40.0)
        val omega = 2.0 * PI * band.frequencyHz / SAMPLE_RATE_HZ
        val cosine = cos(omega)
        val alpha = sin(omega) / (2.0 * band.q)
        val rootA = sqrt(a)

        val raw = when (band.type) {
            "peak_dip" -> doubleArrayOf(
                1.0 + alpha * a,
                -2.0 * cosine,
                1.0 - alpha * a,
                1.0 + alpha / a,
                -2.0 * cosine,
                1.0 - alpha / a,
            )
            "low_shelf" -> doubleArrayOf(
                a * ((a + 1.0) - (a - 1.0) * cosine + 2.0 * rootA * alpha),
                2.0 * a * ((a - 1.0) - (a + 1.0) * cosine),
                a * ((a + 1.0) - (a - 1.0) * cosine - 2.0 * rootA * alpha),
                (a + 1.0) + (a - 1.0) * cosine + 2.0 * rootA * alpha,
                -2.0 * ((a - 1.0) + (a + 1.0) * cosine),
                (a + 1.0) + (a - 1.0) * cosine - 2.0 * rootA * alpha,
            )
            "high_shelf" -> doubleArrayOf(
                a * ((a + 1.0) + (a - 1.0) * cosine + 2.0 * rootA * alpha),
                -2.0 * a * ((a - 1.0) + (a + 1.0) * cosine),
                a * ((a + 1.0) + (a - 1.0) * cosine - 2.0 * rootA * alpha),
                (a + 1.0) - (a - 1.0) * cosine + 2.0 * rootA * alpha,
                2.0 * ((a - 1.0) - (a + 1.0) * cosine),
                (a + 1.0) - (a - 1.0) * cosine - 2.0 * rootA * alpha,
            )
            else -> error("Validated Black Pearl type unexpectedly missing.")
        }
        val a0 = raw[3]
        require(a0.isFinite() && a0 != 0.0) { "Invalid Black Pearl biquad normalization." }
        return doubleArrayOf(raw[0] / a0, raw[1] / a0, raw[2] / a0, raw[4] / a0, raw[5] / a0)
    }

    private fun typeCode(type: String): Int = when (type) {
        "peak_dip" -> TYPE_PEAK
        "low_shelf" -> TYPE_LOW_SHELF
        "high_shelf" -> TYPE_HIGH_SHELF
        else -> error("Validated Black Pearl type unexpectedly missing.")
    }

    private fun Byte.u8(): Int = toInt() and 0xFF

    private val SUPPORTED_TYPES = setOf("peak_dip", "low_shelf", "high_shelf")
    private val FLAT_PADDING_BANDS = listOf(31, 63, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)
        .map { frequency -> Band("peak_dip", frequency.toDouble(), 0.0, 1.0) }
}
