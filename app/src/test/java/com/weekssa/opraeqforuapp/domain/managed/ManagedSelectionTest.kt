package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedSelectionTest {
    @Test
    fun firstTimeDefaultsSelectAllCurrentSelectableProfiles() {
        val fullyCompatible = compatibleProfile("fully")
        val limited = compatibleProfile("limited").copy(
            bands = (1..11).map { index ->
                OpraBand("peak_dip", 100.0 * index, 0.0, 1.0, null)
            },
        )
        val blocked = unsupportedProfile("blocked")
        val historical = compatibleProfile("historical").copy(
            details = "Previous revision · Revision: 2023-10-29 · Source: AutoEQ",
        )

        val selected = defaultStagedSelectedProfileIds(
            listOf(fullyCompatible, limited, blocked, historical),
        )

        assertTrue(DEFAULT_AUTO_INCLUDE_NEW_PROFILES)
        assertTrue("fully" in selected)
        assertTrue("limited" in selected)
        assertFalse("blocked" in selected)
        assertFalse("historical" in selected)
    }

    @Test
    fun firstTimeDefaultsDoNotSilentlySelectUnverifiedProfiles() {
        val verified = compatibleProfile("verified")
        val unverified = compatibleProfile("unverified").copy(isVerified = false)

        val selected = defaultStagedSelectedProfileIds(listOf(verified, unverified))

        assertTrue("verified" in selected)
        assertFalse("unverified" in selected)
    }

    @Test
    fun explicitSelectAllCanStillIncludeUnverifiedProfiles() {
        val unverified = compatibleProfile("unverified").copy(isVerified = false)

        val selected = selectableProfileIds(listOf(unverified))

        assertTrue("unverified" in selected)
    }

    @Test
    fun historyCanStillBeSelectedExplicitly() {
        val current = compatibleProfile("current")
        val historical = compatibleProfile("historical").copy(
            details = "Previous revision · Revision: 2023-10-29 · Source: AutoEQ",
        )

        val selected = selectableProfileIds(
            listOf(current, historical),
            includeHistorical = true,
        )

        assertTrue("current" in selected)
        assertTrue("historical" in selected)
    }

    @Test
    fun neverManagedHeadphoneCanBeAddedWithoutArtificialSelectionChange() {
        val selected = setOf("profile")

        assertTrue(
            managedSelectionCommitEnabled(
                isManaged = false,
                stagedSelectedProfileIds = selected,
                baselineSelectedProfileIds = selected,
                autoIncludeNewProfiles = true,
                baselineAutoIncludeNewProfiles = true,
            ),
        )
    }

    @Test
    fun neverManagedHeadphoneWithNoSelectedProfilesCannotBeAdded() {
        assertFalse(
            managedSelectionCommitEnabled(
                isManaged = false,
                stagedSelectedProfileIds = emptySet(),
                baselineSelectedProfileIds = emptySet(),
                autoIncludeNewProfiles = true,
                baselineAutoIncludeNewProfiles = true,
            ),
        )
    }

    @Test
    fun unchangedManagedHeadphoneDoesNotNeedAnotherSave() {
        assertFalse(
            managedSelectionCommitEnabled(
                isManaged = true,
                stagedSelectedProfileIds = setOf("profile"),
                baselineSelectedProfileIds = setOf("profile"),
                autoIncludeNewProfiles = true,
                baselineAutoIncludeNewProfiles = true,
            ),
        )
    }

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
    fun autoIncludeDoesNotSelectFutureUnverifiedProfile() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = emptyMap(),
        )

        assertFalse(state.isSelected(compatibleProfile("new").copy(isVerified = false)))
    }

    @Test
    fun autoIncludeDoesNotSelectFutureHistoricalRevision() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = emptyMap(),
        )
        val historical = compatibleProfile("old").copy(
            details = "Previous revision · Revision: 2023-10-29 · Source: AutoEQ",
        )

        assertFalse(state.isSelected(historical))
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
    fun uncheckedUnverifiedProfileDoesNotBecomePermanentExplicitExclusion() {
        val unverified = compatibleProfile("unverified").copy(isVerified = false)
        val updates = selectionUpdatesForSave(
            profiles = listOf(unverified),
            stagedSelectedProfileIds = emptySet(),
            autoIncludeNewProfiles = true,
        )

        assertFalse(updates.getValue("unverified").selected)
        assertFalse(updates.getValue("unverified").explicitlyExcluded)
    }

    @Test
    fun unselectedHistoricalRevisionIsNotStoredAsExplicitExclusion() {
        val historical = compatibleProfile("historical").copy(
            details = "Previous revision · Revision: 2023-10-29 · Source: AutoEQ",
        )
        val updates = selectionUpdatesForSave(
            profiles = listOf(historical),
            stagedSelectedProfileIds = emptySet(),
            autoIncludeNewProfiles = true,
        )

        assertFalse(updates.getValue("historical").selected)
        assertFalse(updates.getValue("historical").explicitlyExcluded)
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
