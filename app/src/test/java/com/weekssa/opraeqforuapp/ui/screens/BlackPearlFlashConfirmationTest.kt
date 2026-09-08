package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlFlashConfirmationTest {
    @Test
    fun optimizedAdaptationIsShownWithoutInventingASafetyCaution() {
        val text = blackPearlFlashConfirmation(
            displayName = "Test EQ",
            gainAdjustmentDb = -4.0,
            fidelity = DevicePresetFidelity.OPTIMIZED,
            adaptationSummary = "12 → 10 bands · full-response fit",
            warning = null,
        )

        assertTrue(text.contains("Optimized · 12 → 10 bands · full-response fit."))
        assertFalse(text.contains("Caution:"))
    }

    @Test
    fun actualHardwareCautionIsAppendedSeparately() {
        val warning = "Caution: Band 1 -12.00 dB is outside the currently validated range."
        val text = blackPearlFlashConfirmation(
            displayName = "Test EQ",
            gainAdjustmentDb = -4.0,
            fidelity = DevicePresetFidelity.EXACT,
            adaptationSummary = "source values preserved",
            warning = warning,
        )

        assertTrue(text.contains("Exact · source values preserved."))
        assertTrue(text.contains(warning))
    }
}
