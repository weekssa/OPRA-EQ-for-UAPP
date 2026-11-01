package com.weekssa.opraeqforuapp.data.kt02h20

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Ja11FilterProbeDiagnosticsTest {
    @Test
    fun aNullResponseReportsNoReplyWithoutInventingBytes() {
        val line = Ja11FilterProbeDiagnostics.describe(
            generation = 4L,
            detachGeneration = 2L,
            requestHex = "02bb0b0000040000ee",
            response = null,
            sessionStable = false,
        )
        assertThat(line).contains("generation=4")
        assertThat(line).contains("detach=2")
        assertThat(line).contains("request=02bb0b0000040000ee")
        assertThat(line).contains("responseLength=-1")
        assertThat(line).contains("response=none")
        assertThat(line).contains("sessionStable=false")
    }

    @Test
    fun aReplyIsReportedWithItsExactLengthAndBytes() {
        val line = Ja11FilterProbeDiagnostics.describe(
            generation = 7L,
            detachGeneration = 0L,
            requestHex = "02bb0b0000040000ee",
            response = byteArrayOf(0x02, 0xBB.toByte(), 0x0B, 0x00, 0x00, 0x04, 0x00, 0x02, 0xEE.toByte()),
            sessionStable = true,
        )
        assertThat(line).contains("responseLength=9")
        assertThat(line).contains("response=02bb0b0000040002ee")
        assertThat(line).contains("sessionStable=true")
    }
}
