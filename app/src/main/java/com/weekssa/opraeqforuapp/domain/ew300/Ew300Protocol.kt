package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import kotlin.math.roundToInt

/** Production-safe subset of the qualified EW300 five-band HID protocol. */
object Ew300Protocol {
    const val VENDOR_ID = 0x31B2
    const val PRODUCT_ID = 0x0111
    const val REPORT_ID = 0x4B
    const val REPORT_SIZE = 11
    const val READ_COMMAND = 0x52
    const val WRITE_COMMAND = 0x57
    const val COMMIT_COMMAND = 0x53
    const val BAND_COUNT = 5
    const val FIRST_BAND_REGISTER = 0x26
    const val GLOBAL_GAIN_REGISTER = 0x66
    const val GLOBAL_GAIN_MIN_STEPS = -128
    const val GLOBAL_GAIN_MAX_STEPS = 127
    const val GLOBAL_GAIN_STEPS_PER_DB = 2.0

    private val bandTypes = mapOf(
        "peak_dip" to 0,
        "low_shelf" to 3,
        "high_shelf" to 4,
    )

    fun bandRegister(index: Int): Int {
        require(index in 0 until BAND_COUNT)
        return FIRST_BAND_REGISTER + index * 2
    }

    fun readRegisterReport(register: Int): ByteArray = wire(
        byteArrayOf(register.byte(), 0, 0, 0, READ_COMMAND.byte(), 0, 0, 0, 0, 0),
    )

    fun writeRegisterReport(register: Int, data: ByteArray): ByteArray {
        require(data.size == 4)
        return wire(byteArrayOf(register.byte(), 0, 0, 0, WRITE_COMMAND.byte(), 0) + data)
    }

    fun commitReport(): ByteArray = wire(
        byteArrayOf(0, 0, 0, 0, COMMIT_COMMAND.byte(), 0, 0, 0, 0, 0),
    )

    /** Gated EW300 gain qualification mapping: signed byte 0 in 0.5 dB steps. */
    fun globalGainSteps(data: ByteArray): Int {
        require(data.size == 4)
        return data[0].toInt()
    }

    fun globalGainDb(data: ByteArray): Double =
        globalGainSteps(data) / GLOBAL_GAIN_STEPS_PER_DB

    fun gainDbToSteps(gainDb: Double): Int {
        require(gainDb.isFinite())
        val steps = (gainDb * GLOBAL_GAIN_STEPS_PER_DB).roundToInt()
        require(steps in GLOBAL_GAIN_MIN_STEPS..GLOBAL_GAIN_MAX_STEPS)
        return steps
    }

    fun withGlobalGainSteps(data: ByteArray, steps: Int): ByteArray {
        require(data.size == 4)
        require(steps in GLOBAL_GAIN_MIN_STEPS..GLOBAL_GAIN_MAX_STEPS)
        return data.copyOf().also { it[0] = steps.toByte() }
    }

    fun decodeRead(register: Int, report: ByteArray): ByteArray? {
        if (report.size != REPORT_SIZE || report[0].u8() != REPORT_ID) return null
        if (report[1].u8() != register || report[5].u8() != READ_COMMAND) return null
        return report.copyOfRange(7, REPORT_SIZE)
    }

    fun encodeBand(band: Kt02h20Band): Pair<ByteArray, ByteArray> {
        val type = bandTypes[band.type]
            ?: error("EW300 does not support ${band.type} filter fields.")
        require(band.frequencyHz in 20.0..20_000.0)
        require(band.gainDb in -12.0..12.0)
        require(band.q in 0.1..10.0)
        val gain = (band.gainDb * 10.0).roundToInt().signed16()
        val frequency = band.frequencyHz.roundToInt().coerceIn(20, 20_000)
        val q = (band.q * 1000.0).roundToInt().coerceIn(100, 10_000)
        val gainField = byteArrayOf(
            (gain and 0xFF).byte(), ((gain ushr 8) and 0xFF).byte(),
            (frequency and 0xFF).byte(), ((frequency ushr 8) and 0xFF).byte(),
        )
        val qField = byteArrayOf(
            (q and 0xFF).byte(), ((q ushr 8) and 0xFF).byte(), type.byte(), 0,
        )
        return gainField to qField
    }

    fun decodeBand(index: Int, gainField: ByteArray, qField: ByteArray): Kt02h20Band? {
        if (index !in 0 until BAND_COUNT || gainField.size != 4 || qField.size != 4) return null
        val type = when (qField[2].u8()) {
            0 -> "peak_dip"
            3 -> "low_shelf"
            4 -> "high_shelf"
            else -> return null
        }
        val gainRaw = signed16(gainField[0].u8(), gainField[1].u8())
        val frequency = unsigned16(gainField[2].u8(), gainField[3].u8())
        val qRaw = unsigned16(qField[0].u8(), qField[1].u8())
        return Kt02h20Band(type, frequency.toDouble(), gainRaw / 10.0, qRaw / 1000.0)
    }

    private fun wire(payload: ByteArray): ByteArray = byteArrayOf(REPORT_ID.byte()) + payload
    private fun Int.byte(): Byte = (this and 0xFF).toByte()
    private fun Byte.u8(): Int = toInt() and 0xFF
    private fun signed16(low: Int, high: Int): Int {
        val raw = (high shl 8) or low
        return if (raw >= 0x8000) raw - 0x10000 else raw
    }
    private fun Int.signed16(): Int {
        require(this in Short.MIN_VALUE..Short.MAX_VALUE)
        return this and 0xFFFF
    }
    private fun unsigned16(low: Int, high: Int): Int = low or (high shl 8)
}
