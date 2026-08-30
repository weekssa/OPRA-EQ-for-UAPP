package com.weekssa.opraeqforuapp.domain.settings

import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTargetPreferencesTest {
    @Test
    fun `defaults keep UAPP selected and show otherwise unexportable presets`() {
        val preferences = ExportTargetPreferences()

        assertEquals(setOf(ExportDevice.UAPP), preferences.selectedTargets)
        assertTrue(preferences.showUnexportablePresets)
    }

    @Test
    fun `targets can be enabled and disabled without changing visibility preference`() {
        val preferences = ExportTargetPreferences()
            .withTarget(ExportDevice.BLACK_PEARL, true)
            .withTarget(ExportDevice.UAPP, false)

        assertEquals(setOf(ExportDevice.BLACK_PEARL), preferences.selectedTargets)
        assertTrue(preferences.showUnexportablePresets)
        assertFalse(preferences.isSelected(ExportDevice.UAPP))
        assertTrue(preferences.isSelected(ExportDevice.BLACK_PEARL))
    }
}
