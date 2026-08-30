package com.weekssa.opraeqforuapp.domain.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParametricEqTextParserTest {
    @Test
    fun `parses standard AutoEq parametric text and ignores disabled filters`() {
        val parsed = ParametricEqTextParser.parse(
            """
            Preamp: -6.4 dB
            Filter 1: ON PK Fc 31.5 Hz Gain 2.1 dB Q 0.70
            Filter 2: OFF PK Fc 120 Hz Gain -3.0 dB Q 1.40
            Filter 3: ON LS Fc 105 Hz Gain 1.8 dB Q 0.71
            Filter 4: ON HS Fc 8000 Hz Gain -1.2 dB Q 0.80
            """.trimIndent(),
        )

        assertEquals(-6.4, parsed.preampGainDb!!, 0.0001)
        assertEquals(3, parsed.filters.size)
        assertEquals(EqFilterType.PEAK, parsed.filters[0].type)
        assertEquals(31.5, parsed.filters[0].frequencyHz, 0.0001)
        assertEquals(2.1, parsed.filters[0].gainDb!!, 0.0001)
        assertEquals(EqFilterType.LOW_SHELF, parsed.filters[1].type)
        assertEquals(EqFilterType.HIGH_SHELF, parsed.filters[2].type)
    }

    @Test
    fun `preserves arbitrary active filter counts without filling or truncating`() {
        val text = (1..31).joinToString("\n") { index ->
            "Filter $index: ON PK Fc ${100 + index} Hz Gain ${index / 10.0} dB Q 1.0"
        }

        val parsed = ParametricEqTextParser.parse(text)

        assertNull(parsed.preampGainDb)
        assertEquals(31, parsed.filters.size)
        assertEquals(101.0, parsed.filters.first().frequencyHz, 0.0001)
        assertEquals(131.0, parsed.filters.last().frequencyHz, 0.0001)
    }

    @Test
    fun `rejects malformed gain filters without poisoning valid lines`() {
        val parsed = ParametricEqTextParser.parse(
            """
            Filter 1: ON PK Fc 100 Hz Q 1.0
            Filter 2: ON PK Fc 1000 Hz Gain -2 dB Q 2.0
            nonsense
            """.trimIndent(),
        )

        assertNull(parsed.preampGainDb)
        assertEquals(1, parsed.filters.size)
        assertEquals(1000.0, parsed.filters.single().frequencyHz, 0.0001)
    }

    @Test
    fun `AutoEq adapter preserves measurement provenance as measurement derived`() {
        val profile = AutoEqProfileAdapter.adapt(
            metadata = AutoEqProfileAdapter.Metadata(
                manufacturer = "Sennheiser",
                model = "HD 650",
                sourceRecordId = "results/700x/650",
                sourceUrl = "https://example.invalid/autoeq/hd650",
                measurementSource = "oratory1990",
                targetName = "Harman Over-Ear 2018",
                sourceVersionLabel = "2026-08-29",
                discoveredAtEpochSeconds = 1_788_000_000,
            ),
            parametricEqText = """
                Preamp: -5.5 dB
                Filter 1: ON PK Fc 105 Hz Gain 4.2 dB Q 0.70
                Filter 2: ON PK Fc 3200 Hz Gain -2.1 dB Q 2.00
            """.trimIndent(),
        )!!

        assertEquals("AutoEq", profile.creator)
        assertEquals(EqTargetKind.EXPLICIT_TARGET, profile.target.kind)
        assertEquals("Harman Over-Ear 2018", profile.target.name)
        assertEquals(ProvenanceTier.MEASUREMENT_DERIVED, profile.latestRevision.sourceReferences.single().provenanceTier)
        assertEquals(EqSourceKind.MEASUREMENT_DERIVED, profile.latestRevision.sourceReferences.single().sourceKind)
        assertEquals("Harman Over-Ear 2018 from oratory1990 measurement", profile.tuningLabel)
    }
}
