package com.weekssa.opraeqforuapp.diagnostics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HidReportDescriptorParserTest {
    @Test
    fun `extracts exact EW300 vendor input reports`() {
        val descriptor = hex(
            "05 0C 09 01 A1 01 85 01 15 00 25 01 75 01 95 02 09 E9 09 EA 81 02 " +
                "95 04 09 CD 09 CF 09 B6 09 B5 81 02 95 02 81 01 06 01 FF 85 4B 75 " +
                "08 95 0A 09 01 81 03 95 0A 09 02 91 02 85 54 75 08 95 0A 09 03 " +
                "81 03 95 0A 09 04 91 02 C0",
        )

        assertThat(HidReportDescriptorParser.vendorInputReports(descriptor)).containsExactly(
            HidInputReport(reportId = 0x4B, payloadBytes = 10),
            HidInputReport(reportId = 0x54, payloadBytes = 10),
        ).inOrder()
    }

    @Test
    fun `does not treat consumer input as vendor input`() {
        assertThat(
            HidReportDescriptorParser.vendorInputReports(hex("05 0C 85 01 75 01 95 08 81 02")),
        ).isEmpty()
    }

    @Test
    fun `rejects truncated item`() {
        assertThat(HidReportDescriptorParser.vendorInputReports(hex("06 01"))).isEmpty()
    }

    private fun hex(value: String): ByteArray = value
        .split(" ")
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
