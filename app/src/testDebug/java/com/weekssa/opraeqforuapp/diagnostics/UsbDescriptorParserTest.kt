package com.weekssa.opraeqforuapp.diagnostics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UsbDescriptorParserTest {
    @Test
    fun `extracts EW300 declared HID report length for interface three`() {
        val descriptors = hex(
            "09 04 03 00 02 03 00 00 00 " +
                "09 21 10 01 21 01 22 4A 00 " +
                "07 05 82 03 10 00 01 " +
                "07 05 02 03 10 00 01",
        )

        assertThat(UsbDescriptorParser.hidReportDescriptorLength(descriptors, 3)).isEqualTo(74)
    }

    @Test
    fun `does not use a HID descriptor from another interface`() {
        val descriptors = hex("09 04 03 00 02 03 00 00 00 09 21 10 01 21 01 22 4A 00")

        assertThat(UsbDescriptorParser.hidReportDescriptorLength(descriptors, 2)).isNull()
    }

    @Test
    fun `rejects a truncated descriptor stream`() {
        val descriptors = hex("09 04 03 00 02 03 00 00")

        assertThat(UsbDescriptorParser.hidReportDescriptorLength(descriptors, 3)).isNull()
    }

    private fun hex(value: String): ByteArray = value
        .split(" ")
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
