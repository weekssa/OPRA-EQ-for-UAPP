package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import java.nio.charset.StandardCharsets
import org.junit.Test

class UnclaimedEqParserTest {
    @Test
    fun `valid parametric legacy file is recoverable without changing values`() {
        val result = UnclaimedEqParser.parse(
            "Legacy.txt",
            """
                Preamp: -6.25 dB
                Filter 1: ON PK Fc 105 Hz Gain -3.5 dB Q 1.25
                Filter 2: ON LSC Fc 55 Hz Gain 2.0 dB Q 0.70
                Filter 3: ON HSC Fc 8000 Hz Gain -1.25 dB Q 0.80
            """.trimIndent().toByteArray(),
        )

        assertThat(result.format).isEqualTo(UnclaimedEqFormat.PARAMETRIC_TEXT)
        assertThat(result.state).isEqualTo(UnclaimedEqParseState.RECOVERABLE)
        val content = checkNotNull(result.content)
        assertThat(content.preampGainDb).isWithin(0.000001).of(-6.25)
        assertThat(content.bands).hasSize(3)
        assertThat(content.bands[0].type).isEqualTo("peak_dip")
        assertThat(content.bands[0].frequency).isWithin(0.000001).of(105.0)
        assertThat(content.bands[0].gainDb).isWithin(0.000001).of(-3.5)
        assertThat(content.bands[0].q).isWithin(0.000001).of(1.25)
        assertThat(content.bands[1].type).isEqualTo("low_shelf")
        assertThat(content.bands[2].type).isEqualTo("high_shelf")
    }

    @Test
    fun `parseable EQ with no headphone association remains recoverable for explicit assignment`() {
        val result = UnclaimedEqParser.parse(
            "unknown-headphone.txt",
            "Filter 1: ON PK Fc 1000 Hz Gain 1.5 dB Q 1.1".toByteArray(),
        )

        assertThat(result.state).isEqualTo(UnclaimedEqParseState.RECOVERABLE)
        assertThat(result.content?.suggestedName).isNull()
        assertThat(result.content?.bands).hasSize(1)
    }

    @Test
    fun `malformed and incomplete parametric files stay visible but are not recoverable`() {
        val malformed = UnclaimedEqParser.parse(
            "legacy.txt",
            "Filter 1: ON PK Fc nope Hz Gain 2 dB Q 1".toByteArray(),
        )
        val incomplete = UnclaimedEqParser.parse(
            "legacy.txt",
            "Preamp: -3.0 dB".toByteArray(),
        )

        assertThat(malformed.state).isEqualTo(UnclaimedEqParseState.INVALID)
        assertThat(malformed.content).isNull()
        assertThat(incomplete.state).isEqualTo(UnclaimedEqParseState.INVALID)
        assertThat(incomplete.content).isNull()
    }

    @Test
    fun `unsupported filter is never silently discarded`() {
        val result = UnclaimedEqParser.parse(
            "legacy.txt",
            """
                Filter 1: ON PK Fc 100 Hz Gain 1 dB Q 1
                Filter 2: ON LP Fc 5000 Hz Q 0.7
            """.trimIndent().toByteArray(),
        )

        assertThat(result.state).isEqualTo(UnclaimedEqParseState.INVALID)
        assertThat(result.content).isNull()
        assertThat(result.message).contains("unsupported")
    }

    @Test
    fun `GraphicEQ cannot be promoted as lossless parametric EQ`() {
        val result = UnclaimedEqParser.parse(
            "legacy.txt",
            "GraphicEQ: 20 -1; 1000 2; 20000 -3".toByteArray(),
        )

        assertThat(result.state).isEqualTo(UnclaimedEqParseState.UNSUPPORTED)
        assertThat(result.content).isNull()
    }

    @Test
    fun `EQ Library ToneBoosters XML can be recovered deterministically`() {
        val values = MutableList(66) { 0.0 }
        // Band 1: 1000 Hz, -4 dB, enabled, Q 1.25, peak, channel both.
        values[0] = ((1000.0 - 16.0) / (20_000.0 - 16.0)).powOneThird()
        values[1] = (-4.0 + 20.0) / 40.0
        values[2] = 1.0
        values[3] = ((1.25 - 0.1) / 9.9).powOneThird()
        values[4] = 0.21428572
        values[5] = 0.0
        // ToneBoosters global gain/preamp at index 61.
        values[61] = (-6.0 + 20.0) / 40.0
        val xml = buildString {
            append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>")
            append("<PresetInfo Type=\"Graphical EQ\" Name=\"Legacy &amp; Safe\" TenBand=\"1\"><Preset>")
            values.forEach { append("<Value>").append(it).append("</Value>") }
            append("</Preset></PresetInfo>")
        }

        val result = UnclaimedEqParser.parse(
            "Legacy.xml",
            xml.toByteArray(StandardCharsets.ISO_8859_1),
        )

        assertThat(result.state).isEqualTo(UnclaimedEqParseState.RECOVERABLE)
        val content = checkNotNull(result.content)
        assertThat(content.suggestedName).isEqualTo("Legacy & Safe")
        assertThat(content.preampGainDb).isWithin(0.000001).of(-6.0)
        assertThat(content.bands).hasSize(1)
        assertThat(content.bands.single().type).isEqualTo("peak_dip")
        assertThat(content.bands.single().frequency).isWithin(0.0001).of(1000.0)
        assertThat(content.bands.single().gainDb).isWithin(0.000001).of(-4.0)
        assertThat(content.bands.single().q).isWithin(0.0001).of(1.25)
    }

    @Test
    fun `incomplete ToneBoosters XML is not recoverable`() {
        val result = UnclaimedEqParser.parse(
            "old.xml",
            "<PresetInfo TenBand=\"1\"><Preset><Value>0.5</Value></Preset></PresetInfo>"
                .toByteArray(StandardCharsets.ISO_8859_1),
        )

        assertThat(result.state).isEqualTo(UnclaimedEqParseState.INVALID)
        assertThat(result.content).isNull()
    }
}

private fun Double.powOneThird(): Double = kotlin.math.pow(1.0 / 3.0)
