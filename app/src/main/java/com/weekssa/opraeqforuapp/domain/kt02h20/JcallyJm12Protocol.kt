package com.weekssa.opraeqforuapp.domain.kt02h20

/**
 * Independently implemented codec for the stock JCALLY JM12 KT02H20 run-mode register protocol.
 *
 * Only DAC-side PEQ, the DAC digital playback-gain register needed to represent source preamp, and
 * the read/write handshake are exposed. Firmware/bootloader, ADC EQ, DRC, USB identity writes,
 * reconstruction controls, and other unrelated DSP registers are intentionally absent.
 */
object JcallyJm12Protocol {
    const val VENDOR_ID = 0x31B2
    const val PRODUCT_ID = 0x0111
    const val REPORT_ID = 0x4B
    const val REPORT_SIZE = 11
    const val BAND_COUNT = 5

    const val REG_PROTOCOL_FLAGS = 0x01
    const val REG_EQ_DAC_ENABLE = 0x24
    const val REG_EQ_DAC_BASE = 0x26
    const val REG_DIGITAL_DAC_GAIN = 0x66

    private const val CMD_HANDSHAKE = 0x43
    private const val CMD_READ = 0x52
    private const val CMD_WRITE = 0x57

    private const val TYPE_PEAK = 0
    private const val TYPE_LOW_SHELF = 3
    private const val TYPE_HIGH_SHELF = 4
    private const val SINGLE_DAC_FLAG = 0x0200

    data class BandRegisters(val a: Int, val b: Int)

    fun handshakeReport(): ByteArray = ByteArray(REPORT_SIZE).apply {
        this[0] = REPORT_ID.b()
        this[5] = CMD_HANDSHAKE.b()
    }

    fun readRegisterReport(address: Int): ByteArray {
        require(address in 0..0xFF)
        return ByteArray(REPORT_SIZE).apply {
            this[0] = REPORT_ID.b()
            putLe32(this, 1, address)
            this[5] = CMD_READ.b()
        }
    }

    fun writeRegisterReport(address: Int, value: Int): ByteArray {
        require(address in 0..0xFF)
        return ByteArray(REPORT_SIZE).apply {
            this[0] = REPORT_ID.b()
            putLe32(this, 1, address)
            this[5] = CMD_WRITE.b()
            putLe32(this, 7, value)
        }
    }

    fun handshakeAccepted(response: ByteArray): Boolean =
        response.size >= 11 && response[0].u8() == REPORT_ID &&
            response[5].u8() == CMD_HANDSHAKE &&
            (response[7].u8() == 0x03 || response[7].u8() == 0x4F)

    fun readRegisterValue(address: Int, response: ByteArray): Int? {
        if (response.size < REPORT_SIZE || response[0].u8() != REPORT_ID || response[5].u8() != CMD_READ) return null
        if (le32(response, 1) != address) return null
        return le32(response, 7)
    }

    fun writeAcknowledged(address: Int, response: ByteArray): Boolean =
        response.size >= REPORT_SIZE && response[0].u8() == REPORT_ID &&
            le32(response, 1) == address && response[5].u8() == CMD_WRITE && le32(response, 7) == 0x03

    fun bandRegisterAddress(index: Int): Int {
        require(index in 0 until BAND_COUNT)
        return REG_EQ_DAC_BASE + index * 2
    }

    fun encodeBand(band: Kt02h20Band): BandRegisters {
        require(band.type in SUPPORTED_TYPES)
        require(band.frequencyHz.isFinite() && band.frequencyHz in 20.0..20_000.0)
        require(band.gainDb.isFinite() && band.gainDb in -30.0..30.0)
        require(band.q.isFinite() && band.q in 0.1..20.0)
        val frequency = band.frequencyHz.roundToIntChecked(0..0xFFFF)
        val gainRaw = (band.gainDb * 10.0).roundToIntChecked(Short.MIN_VALUE.toInt()..Short.MAX_VALUE.toInt()) and 0xFFFF
        val qRaw = (band.q * 1000.0).roundToIntChecked(0..0xFFFF)
        val a = (frequency shl 16) or gainRaw
        val b = (typeCode(band.type) shl 16) or qRaw
        return BandRegisters(a, b)
    }

    fun decodeBand(a: Int, b: Int): Kt02h20Band? {
        val frequency = (a ushr 16) and 0xFFFF
        var gainRaw = a and 0xFFFF
        if (gainRaw >= 0x8000) gainRaw -= 0x10000
        val type = typeName((b ushr 16) and 0x7) ?: return null
        val qRaw = b and 0xFFFF
        val band = Kt02h20Band(type, frequency.toDouble(), gainRaw / 10.0, qRaw / 1000.0)
        return band.takeIf {
            it.frequencyHz in 20.0..20_000.0 && it.gainDb in -30.0..30.0 && it.q in 0.1..20.0
        }
    }

    fun setEqEnabled(registerValue: Int, enabled: Boolean): Int =
        if (enabled) registerValue or 0x01 else registerValue and 0x01.inv()

    fun isEqEnabled(registerValue: Int): Boolean = registerValue and 0x01 != 0

    fun isSingleDac(protocolFlags: Int): Boolean = protocolFlags and SINGLE_DAC_FLAG != 0

    /** Digital gain is signed byte(s) in 0.5 dB steps. */
    fun decodeDigitalGainSteps(registerValue: Int, protocolFlags: Int): IntArray {
        val left = signedByte(registerValue and 0xFF)
        return if (isSingleDac(protocolFlags)) {
            intArrayOf(left)
        } else {
            intArrayOf(left, signedByte((registerValue ushr 8) and 0xFF))
        }
    }

    /**
     * Replace only the DAC gain byte(s), preserving every unrelated byte in register 0x66.
     * `targetSteps` contains one value for single-DAC models and two for stereo models.
     */
    fun withDigitalGainSteps(registerValue: Int, protocolFlags: Int, targetSteps: IntArray): Int {
        val expected = if (isSingleDac(protocolFlags)) 1 else 2
        require(targetSteps.size == expected)
        targetSteps.forEach { require(it in -128..127) }
        var result = registerValue and 0xFFFF0000.toInt()
        result = result or (targetSteps[0] and 0xFF)
        if (!isSingleDac(protocolFlags)) result = result or ((targetSteps[1] and 0xFF) shl 8)
        return result
    }

    fun gainDbToSteps(gainDb: Double): Int {
        require(gainDb.isFinite())
        return (gainDb * 2.0).roundToIntChecked(-128..127)
    }

    fun gainStepsToDb(steps: Int): Double {
        require(steps in -128..127)
        return steps / 2.0
    }

    fun completeBands(bands: List<Kt02h20Band>): List<Kt02h20Band> {
        require(bands.size <= BAND_COUNT)
        return bands + FLAT_BANDS.drop(bands.size)
    }

    fun nearlyMatches(expected: Kt02h20Band, actual: Kt02h20Band): Boolean =
        expected.type == actual.type &&
            kotlin.math.abs(expected.frequencyHz - actual.frequencyHz) <= 0.5 &&
            kotlin.math.abs(expected.gainDb - actual.gainDb) <= 0.051 &&
            kotlin.math.abs(expected.q - actual.q) <= 0.00051

    private fun typeCode(type: String): Int = when (type) {
        "peak_dip" -> TYPE_PEAK
        "low_shelf" -> TYPE_LOW_SHELF
        "high_shelf" -> TYPE_HIGH_SHELF
        else -> error("Unsupported stock JM12 filter type")
    }

    private fun typeName(type: Int): String? = when (type) {
        TYPE_PEAK -> "peak_dip"
        TYPE_LOW_SHELF -> "low_shelf"
        TYPE_HIGH_SHELF -> "high_shelf"
        else -> null
    }

    private fun signedByte(raw: Int): Int = if (raw >= 0x80) raw - 0x100 else raw

    private fun putLe32(target: ByteArray, offset: Int, value: Int) {
        repeat(4) { byteIndex -> target[offset + byteIndex] = ((value ushr (byteIndex * 8)) and 0xFF).b() }
    }

    private fun le32(source: ByteArray, offset: Int): Int =
        source[offset].u8() or
            (source[offset + 1].u8() shl 8) or
            (source[offset + 2].u8() shl 16) or
            (source[offset + 3].u8() shl 24)

    private fun Double.roundToIntChecked(range: IntRange): Int {
        val rounded = kotlin.math.round(this).toLong()
        require(rounded in range.first.toLong()..range.last.toLong())
        return rounded.toInt()
    }

    private fun Byte.u8(): Int = toInt() and 0xFF
    private fun Int.b(): Byte = (this and 0xFF).toByte()

    private val SUPPORTED_TYPES = setOf("peak_dip", "low_shelf", "high_shelf")
    private val FLAT_BANDS = listOf(80.0, 250.0, 1_000.0, 4_000.0, 12_000.0)
        .map { frequency -> Kt02h20Band("peak_dip", frequency, 0.0, 0.7) }
}
