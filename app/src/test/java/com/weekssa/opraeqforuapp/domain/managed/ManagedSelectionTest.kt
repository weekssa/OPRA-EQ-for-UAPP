package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedSelectionTest {
    @Test
    fun firstTimeDefaultsStartEmptyAndAutomaticInclusionOff() {
        val profiles = listOf(
            compatibleProfile("fully"),
            compatibleProfile("limited").copy(
                bands = (1..11).map { index ->
                    OpraBand("peak_dip", 100.0 * index, 0.0, 1.0, null)
                },
            ),
            uappUnsupportedProfile("uapp-unsupported"),
            unusableProfile("unusable"),
            compatibleProfile("historical").copy(
                details = "Previous revision · Revision: 2023-10-29 · Source: AutoEQ",
            ),
            compatibleProfile("unverified").copy(isVerified = false),
        )

        val selected = defaultStagedSelectedProfileIds(profiles)

        assertFalse(DEFAULT_AUTO_INCLUDE_NEW_PROFILES)
        assertTrue(selected.isEmpty())
    }

    @Test
    fun explicitSelectAllCanStillIncludeVerifiedAndUnverifiedUsableProfiles() {
        val verified = compatibleProfile("verified")
        val unverified = compatibleProfile("unverified").copy(isVerified = false)

        val selected = selectableProfileIds(listOf(verified, unverified))

        assertTrue("verified" in selected)
        assertTrue("unverified" in selected)
    }

    @Test
    fun explicitSelectAllCanStillIncludeUnverifiedProfiles() {
        val unverified = compatibleProfile("unverified").copy(isVerified = false)

        val selected = selectableProfileIds(listOf(unverified))

        assertTrue("unverified" in selected)
    }

    @Test
    fun explicitSelectAllCanIncludeOutputUnsupportedButUsableProfile() {
        val source = uappUnsupportedProfile("uapp-unsupported")

        val selected = selectableProfileIds(listOf(source))

        assertTrue("uapp-unsupported" in selected)
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
    fun neverManagedHeadphoneCanBeAddedAfterExplicitSelection() {
        val selected = setOf("profile")

        assertTrue(
            managedSelectionCommitEnabled(
                isManaged = false,
                stagedSelectedProfileIds = selected,
                baselineSelectedProfileIds = emptySet(),
                autoIncludeNewProfiles = false,
                baselineAutoIncludeNewProfiles = false,
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
                autoIncludeNewProfiles = false,
                baselineAutoIncludeNewProfiles = false,
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
    fun autoIncludeSelectsFutureUsableVerifiedProfileWhenUserTurnsItOn() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = emptyMap(),
        )

        assertTrue(state.isSelected(compatibleProfile("new")))
    }

    @Test
    fun autoIncludeAlsoSelectsFutureOutputUnsupportedButUsableProfile() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = emptyMap(),
        )

        assertTrue(state.isSelected(uappUnsupportedProfile("new")))
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
    fun unusableSourceIsNeverSelectedAutomatically() {
        val state = ManagedHeadphoneSelection(
            productId = "product",
            autoIncludeNewProfiles = true,
            profileSelections = emptyMap(),
        )

        assertFalse(state.isSelected(unusableProfile("blocked")))
    }

    @Test
    fun savingWithAutoIncludePersistsUncheckedVerifiedProfilesAsExplicitExclusions() {
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

    @Test
    fun savingCanSelectOutputUnsupportedButUsableProfile() {
        val source = uappUnsupportedProfile("saved")

        val updates = selectionUpdatesForSave(
            profiles = listOf(source),
            stagedSelectedProfileIds = setOf(source.id),
            autoIncludeNewProfiles = false,
        )

        assertTrue(updates.getValue(source.id).selected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun savingCannotSelectUnusableSourceProfile() {
        selectionUpdatesForSave(
            profiles = listOf(unusableProfile("blocked")),
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

    private fun uappUnsupportedProfile(id: String) = compatibleProfile(id).copy(
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

    private fun unusableProfile(id: String) = compatibleProfile(id).copy(
        profileType = "graphic_eq",
    )
}
