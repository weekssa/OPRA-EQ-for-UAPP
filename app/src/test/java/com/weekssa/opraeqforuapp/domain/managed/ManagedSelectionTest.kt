package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedSelectionTest {
    @Test
    fun firstTimeDefaultsStartEmptyAndNewEqNotificationsOn() {
        val selected = defaultStagedSelectedProfileIds(listOf(compatibleProfile("profile")))
        assertTrue(DEFAULT_NOTIFY_NEW_PROFILES)
        assertTrue(selected.isEmpty())
    }

    @Test
    fun explicitSelectAllCanIncludeVerifiedAndUnverifiedUsableProfiles() {
        val verified = compatibleProfile("verified")
        val unverified = compatibleProfile("unverified").copy(isVerified = false)
        val selected = selectableProfileIds(listOf(verified, unverified))
        assertTrue("verified" in selected)
        assertTrue("unverified" in selected)
    }

    @Test
    fun notificationPreferenceDoesNotSelectFutureVerifiedOrUnverifiedProfiles() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = emptyMap(),
        )
        assertFalse(state.isSelected(compatibleProfile("verified")))
        assertFalse(state.isSelected(compatibleProfile("unverified").copy(isVerified = false)))
    }

    @Test
    fun explicitlyStoredSelectionRemainsSelected() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = mapOf(
                "selected" to StoredProfileSelection(selected = true, explicitlyExcluded = false),
            ),
        )
        assertTrue(state.isSelected(compatibleProfile("selected")))
        assertFalse(state.isSelected(compatibleProfile("future")))
    }

    @Test
    fun savingUsesExactSelectionAndCreatesNoAutomaticExclusions() {
        val profiles = listOf(compatibleProfile("selected"), compatibleProfile("not-selected"))
        val updates = selectionUpdatesForSave(
            profiles = profiles,
            stagedSelectedProfileIds = setOf("selected"),
            autoIncludeNewProfiles = true,
        )
        assertTrue(updates.getValue("selected").selected)
        assertFalse(updates.getValue("selected").explicitlyExcluded)
        assertFalse(updates.getValue("not-selected").selected)
        assertFalse(updates.getValue("not-selected").explicitlyExcluded)
    }

    @Test
    fun notificationOnlyChangeDoesNotMakeSelectionEditorDirty() {
        assertFalse(
            managedSelectionCommitEnabled(
                isManaged = true,
                stagedSelectedProfileIds = setOf("profile"),
                baselineSelectedProfileIds = setOf("profile"),
                autoIncludeNewProfiles = true,
                baselineAutoIncludeNewProfiles = false,
            ),
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
        bands = listOf(OpraBand("peak_dip", 1_000.0, 1.0, 1.0, null)),
    )
}
