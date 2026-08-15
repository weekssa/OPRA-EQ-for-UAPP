package com.weekssa.opraeqforuapp.domain

import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoundationDomainTest {
    @Test
    fun notCompatibleProfilesAreNeverSelectableOrExportable() {
        assertFalse(ProfileCompatibility.NotCompatible.isSelectable)
        assertFalse(ProfileCompatibility.NotCompatible.isExportable)
        assertTrue(ProfileCompatibility.FullyCompatible.isSelectable)
        assertTrue(ProfileCompatibility.CompatibleWithLimitation.isExportable)
    }

    @Test
    fun profileVisibilityDefaultsToShowingAllThreeOutcomes() {
        val preferences = ProfileVisibilityPreferences()

        assertTrue(preferences.isVisible(ProfileVisibilityCategory.FullyCompatible))
        assertTrue(preferences.isVisible(ProfileVisibilityCategory.CompatibleWithLimitation))
        assertTrue(preferences.isVisible(ProfileVisibilityCategory.NotCompatible))
    }

    @Test
    fun hidingOneCompatibilityOutcomeDoesNotMutateTheOthers() {
        val updated = ProfileVisibilityPreferences().withVisibility(
            ProfileVisibilityCategory.NotCompatible,
            visible = false,
        )

        assertTrue(updated.showFullyCompatible)
        assertTrue(updated.showCompatibleWithLimitation)
        assertFalse(updated.showNotCompatible)
    }

    @Test
    fun unknownThemeValueFallsBackToSystemDefault() {
        assertEquals(ThemeMode.System, ThemeMode.fromStorageValue("unexpected"))
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorageValue("dark"))
    }
}
