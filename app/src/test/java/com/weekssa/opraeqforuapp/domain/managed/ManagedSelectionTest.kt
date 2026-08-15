package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedSelectionTest {
    @Test
    fun autoIncludeSelectsFutureCompatibleProfile() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = emptyMap(),
        )

        assertTrue(state.isSelected(compatibleProfile("new")))
    }

    @Test
    fun fixedSelectionDoesNotSelectFutureProfile() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = false,
            profileSelections = emptyMap(),
        )

        assertFalse(state.isSelected(compatibleProfile("new")))
    }

    @Test
    fun explicitExclusionRemainsUnselectedWhileAutoIncludeIsOn() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = mapOf(
                "excluded" to StoredProfileSelection(selected = false, explicitlyExcluded = true),
            ),
        )

        assertFalse(state.isSelected(compatibleProfile("excluded")))
        assertTrue(state.isSelected(compatibleProfile("future")))
    }

    @Test
    fun notCompatibleProfileIsNeverSelectedAutomatically() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = emptyMap(),
        )

        assertFalse(state.isSelected(unsupportedProfile("blocked")))
    }

    @Test
    fun savingWithAutoIncludePersistsUncheckedProfilesAsExplicitExclusions() {
        val profiles = listOf(compatibleProfile("selected"), compatibleProfile("excluded"))
        val updates = selectionUpdatesForSave(
            profiles = profiles,
            stagedSelectedProfileIds = setOf("selected"),
            autoIncludeNewProfiles = true,
        )

        assertTrue(updates.getValue("selected").selected)
        assertFalse(updates.getValue("selected").explicitlyExcluded)
        assertFalse(updates.getValue("excluded").selected)
        assertTrue(updates.getValue("excluded").explicitlyExcluded)
    }

    @Test
    fun savingFixedSelectionDoesNotCreateExplicitExclusions() {
        val profiles = listOf(compatibleProfile("selected"), compatibleProfile("not-selected"))
        val updates = selectionUpdatesForSave(
            profiles = profiles,
            stagedSelectedProfileIds = setOf("selected"),
            autoIncludeNewProfiles = false,
        )

        assertFalse(updates.getValue("not-selected").selected)
        assertFalse(updates.getValue("not-selected").explicitlyExcluded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun savingCannotSelectNotCompatibleProfile() {
        selectionUpdatesForSave(
            profiles = listOf(unsupportedProfile("blocked")),
            stagedSelectedProfileIds = setOf("blocked"),
            autoIncludeNewProfiles = true,
        )
    }

    private fun compatibleProfile(id: String) = OpraEqProfile(
        id = id,
        productId = "product",
        author = "Creator",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = listOf(
            OpraBand(
                type = "peak_dip",
                frequency = 1_000.0,
                gainDb = 1.0,
                q = 1.0,
                slope = null,
            ),
        ),
    )

    private fun unsupportedProfile(id: String) = compatibleProfile(id).copy(
        bands = listOf(
            OpraBand(
                type = "low_pass",
                frequency = 1_000.0,
                gainDb = 0.0,
                q = 1.0,
                slope = 12.0,
            ),
        ),
    )
}
