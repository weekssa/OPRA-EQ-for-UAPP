package com.weekssa.opraeqforuapp.diagnostics

internal data class HidInputReport(
    val reportId: Int,
    val payloadBytes: Int,
)

internal object HidReportDescriptorParser {
    fun vendorInputReports(descriptor: ByteArray): List<HidInputReport> {
        var offset = 0
        var usagePage: Int? = null
        var reportId: Int? = null
        var reportSizeBits: Int? = null
        var reportCount: Int? = null
        val reports = linkedSetOf<HidInputReport>()

        while (offset < descriptor.size) {
            val prefix = descriptor[offset].toInt() and 0xFF
            if (prefix == LONG_ITEM_PREFIX) return emptyList()
            val encodedSize = prefix and 0x03
            val dataSize = if (encodedSize == 3) 4 else encodedSize
            if (offset + 1 + dataSize > descriptor.size) return emptyList()
            val type = (prefix shr 2) and 0x03
            val tag = (prefix shr 4) and 0x0F
            val value = littleEndianValue(descriptor, offset + 1, dataSize)

            when {
                type == GLOBAL_ITEM && tag == USAGE_PAGE_TAG -> usagePage = value
                type == GLOBAL_ITEM && tag == REPORT_SIZE_TAG -> reportSizeBits = value
                type == GLOBAL_ITEM && tag == REPORT_ID_TAG -> reportId = value
                type == GLOBAL_ITEM && tag == REPORT_COUNT_TAG -> reportCount = value
                type == MAIN_ITEM && tag == INPUT_TAG && usagePage != null && usagePage >= VENDOR_PAGE_MIN -> {
                    val id = reportId
                    val size = reportSizeBits
                    val count = reportCount
                    if (id == null || id !in 1..255 || size == null || size <= 0 || count == null || count <= 0) {
                        return emptyList()
                    }
                    reports += HidInputReport(id, (size * count + 7) / 8)
                }
            }
            offset += 1 + dataSize
        }
        return reports.toList()
    }

    private fun littleEndianValue(bytes: ByteArray, start: Int, count: Int): Int {
        var value = 0
        repeat(count) { index ->
            value = value or ((bytes[start + index].toInt() and 0xFF) shl (index * 8))
        }
        return value
    }

    private const val LONG_ITEM_PREFIX = 0xFE
    private const val MAIN_ITEM = 0
    private const val GLOBAL_ITEM = 1
    private const val INPUT_TAG = 0x08
    private const val USAGE_PAGE_TAG = 0x00
    private const val REPORT_SIZE_TAG = 0x07
    private const val REPORT_ID_TAG = 0x08
    private const val REPORT_COUNT_TAG = 0x09
    private const val VENDOR_PAGE_MIN = 0xFF00
}
