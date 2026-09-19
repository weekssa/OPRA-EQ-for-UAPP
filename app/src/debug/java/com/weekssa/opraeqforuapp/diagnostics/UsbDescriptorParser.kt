package com.weekssa.opraeqforuapp.diagnostics

internal object UsbDescriptorParser {
    fun hidReportDescriptorLength(rawDescriptors: ByteArray, interfaceNumber: Int): Int? {
        var offset = 0
        var currentInterface: Int? = null
        while (offset + 2 <= rawDescriptors.size) {
            val length = rawDescriptors[offset].toInt() and 0xFF
            val type = rawDescriptors[offset + 1].toInt() and 0xFF
            if (length < 2 || offset + length > rawDescriptors.size) return null

            if (type == USB_DESCRIPTOR_TYPE_INTERFACE && length >= 9) {
                currentInterface = rawDescriptors[offset + 2].toInt() and 0xFF
            } else if (
                type == USB_DESCRIPTOR_TYPE_HID &&
                currentInterface == interfaceNumber &&
                length >= 9
            ) {
                val descriptorCount = rawDescriptors[offset + 5].toInt() and 0xFF
                repeat(descriptorCount) { descriptorIndex ->
                    val descriptorOffset = offset + 6 + descriptorIndex * 3
                    if (descriptorOffset + 3 > offset + length) return null
                    val descriptorType = rawDescriptors[descriptorOffset].toInt() and 0xFF
                    if (descriptorType == USB_DESCRIPTOR_TYPE_REPORT) {
                        val low = rawDescriptors[descriptorOffset + 1].toInt() and 0xFF
                        val high = rawDescriptors[descriptorOffset + 2].toInt() and 0xFF
                        return low or (high shl 8)
                    }
                }
            }
            offset += length
        }
        return null
    }

    private const val USB_DESCRIPTOR_TYPE_INTERFACE = 0x04
    private const val USB_DESCRIPTOR_TYPE_HID = 0x21
    private const val USB_DESCRIPTOR_TYPE_REPORT = 0x22
}
