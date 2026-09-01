package com.weekssa.opraeqforuapp.domain.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalEqStrictImportTest {
    @Test
    fun `strict personal import preserves supported Equalizer APO values`() {
        val parsed = ParametricEqTextParser.parseStrictPersonal(
            """
            Preamp: -6.4 dB
            Filter 1: ON PK Fc 31.5 Hz Gain 2.1 dB Q 0.70
            Filter 2: OFF PK Fc 120 Hz Gain -3.0 dB Q 1.40
            Filter 3: ON LS Fc 105 Hz Gain 1.8 dB Q 0.71
            Filter 4: ON HS Fc 8000 Hz Gain -1.2 dB Q 0.80
            """.trimIndent(),
        )

        assertTrue(parsed.errors.toString(), parsed.isValid)
        assertEquals(-6.4, parsed.parsedEq.preampGainDb!!, 0.0001)
        assertEquals(3, parsed.parsedEq.filters.size)
        assertEquals(EqFilterType.PEAK, parsed.parsedEq.filters[0].type)
        assertEquals(31.5, parsed.parsedEq.filters[0].frequencyHz, 0.0001)
        assertEquals(2.1, parsed.parsedEq.filters[0].gainDb!!, 0.0001)
        assertEquals(EqFilterType.LOW_SHELF, parsed.parsedEq.filters[1].type)
        assertEquals(EqFilterType.HIGH_SHELF, parsed.parsedEq.filters[2].type)
    }

    @Test
    fun `missing preamp remains null`() {
        val parsed = ParametricEqTextParser.parseStrictPersonal(
            "Filter 1: ON PK Fc 1000 Hz Gain -2 dB Q 2.0",
        )
        assertTrue(parsed.isValid)
        assertNull(parsed.parsedEq.preampGainDb)
    }

    @Test
    fun `malformed filter blocks save instead of silently importing partial EQ`() {
        val parsed = ParametricEqTextParser.parseStrictPersonal(
            """
            Filter 1: ON PK Fc 100 Hz Gain 1 dB Q 1.0
            Filter 2: ON PK Fc 500 Hz Q 1.0
            Filter 3: ON PK Fc 1000 Hz Gain -2 dB Q 2.0
            """.trimIndent(),
        )
        assertFalse(parsed.isValid)
        assertEquals(2, parsed.parsedEq.filters.size)
        assertTrue(parsed.errors.any { it.contains("Line 2") && it.contains("Gain") })
    }

    @Test
    fun `unsupported enabled filter blocks personal import`() {
        val parsed = ParametricEqTextParser.parseStrictPersonal(
            "Filter 1: ON LP Fc 12000 Hz Q 0.7",
        )
        assertFalse(parsed.isValid)
        assertTrue(parsed.errors.any { it.contains("unsupported active filter type LP") })
    }

    @Test
    fun `unsupported file contents fail clearly`() {
        val parsed = ParametricEqTextParser.parseStrictPersonal("{\"filters\": []}")
        assertFalse(parsed.isValid)
        assertTrue(parsed.errors.single().contains("isn't supported yet"))
    }
}
