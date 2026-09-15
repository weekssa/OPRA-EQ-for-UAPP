package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.library.EqFilterType

/** Pure decoder for the already-observed Black Pearl PEQ native report payload. No write behavior lives here. */
object BlackPearlReadCodec {
    private const val REPORT_ID = 0x4B
    private const val WRITE = 0x01
    private const val READ = 0x80
    private const val CMD_PEQ_VALUES = 0x09
    private const val TYPE_PEAK = 0x02
    private const val TYPE_LOW_SHELF = 0x03
    private const val TYPE_HIGH_SHELF = 0x04

    data class NativeBand(
        val index: Int,
        val type: EqFilterType,
        val frequencyRawHz: Int,
        val gainRaw256: Int,
        val qRaw256: Int,
        val activeSlot: Int,
    ) {
        val frequencyHz: Double get() = frequencyRawHz.toDouble()
        val gainDb: Double get() = gainRaw256.toDouble() / 256.0
        val q: Double get() = qRaw256.toDouble() / 256.0
    }

    fun bandFromResponse(report: ByteArray): NativeBand? = bandFromReport(report, READ)

    /**
     * Decodes the PEQ value payload from one deterministic EQ Library write report.
     *
     * My DAC saved-EQ matching uses this to fingerprint the exact native representation the existing
     * Flash planner would send, rather than maintaining a second target-quantization algorithm.
     */
    fun bandFromWriteReport(report: ByteArray): NativeBand? = bandFromReport(report, WRITE)

    private fun bandFromReport(report: ByteArray, expectedOperation: Int): NativeBand? {
        if (report.size < 37) return null
        if (
            report[0].u8() != REPORT_ID ||
            report[1].u8() != expectedOperation ||
            report[2].u8() != CMD_PEQ_VALUES
        ) {
            return null
        }

        val index = report[5].u8()
        if (index !in 0 until BlackPearlProtocol.BAND_COUNT) return null

        val frequency = report.leUnsigned16(28)
        val qRaw = report.leUnsigned16(30)
        val gainRaw = report.leSigned16(32)
        val type = when (report[34].u8()) {
            TYPE_PEAK -> EqFilterType.PEAK
            TYPE_LOW_SHELF -> EqFilterType.LOW_SHELF
            TYPE_HIGH_SHELF -> EqFilterType.HIGH_SHELF
            else -> return null
        }

        if (frequency !in 20..20_000) return null
        if (qRaw <= 0) return null
        val q = qRaw.toDouble() / 256.0
        if (q !in 0.1..10.0) return null

        return NativeBand(
            index = index,
            type = type,
            frequencyRawHz = frequency,
            gainRaw256 = gainRaw,
            qRaw256 = qRaw,
            activeSlot = report[36].u8(),
        )
    }

    private fun ByteArray.leUnsigned16(offset: Int): Int =
        this[offset].u8() or (this[offset + 1].u8() shl 8)

    private fun ByteArray.leSigned16(offset: Int): Int {
        val raw = leUnsigned16(offset)
        return if (raw >= 0x8000) raw - 0x10000 else raw
    }

    private fun Byte.u8(): Int = toInt() and 0xFF
}
