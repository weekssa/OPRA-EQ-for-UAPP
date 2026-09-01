package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertEquals
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

    @Test
    fun hiddenLineagesAreExcludedFromNewAndUpdatedReviewQueues() {
        val visibleNew = managedProfile(
            profile = compatibleProfile("new-visible").copy(canonicalProfileId = "lineage-visible-new"),
            isNewUnreviewed = true,
        )
        val hiddenNew = managedProfile(
            profile = compatibleProfile("new-hidden").copy(canonicalProfileId = "lineage-hidden"),
            isNewUnreviewed = true,
        )
        val visibleUpdated = managedProfile(
            profile = compatibleProfile("updated-visible").copy(canonicalProfileId = "lineage-visible-updated"),
            isUpdatedUnreviewed = true,
            selected = true,
        )
        val hiddenUpdated = managedProfile(
            profile = compatibleProfile("updated-hidden").copy(canonicalProfileId = "lineage-hidden"),
            isUpdatedUnreviewed = true,
            selected = true,
        )
        val unavailable = managedProfile(
            profile = compatibleProfile("unavailable").copy(canonicalProfileId = "lineage-unavailable"),
            isNewUnreviewed = true,
            noLongerAvailable = true,
        )
        val headphone = managedHeadphone(
            listOf(visibleNew, hiddenNew, visibleUpdated, hiddenUpdated, unavailable),
        )

        assertEquals(
            listOf("new-visible"),
            headphone.reviewableNewEqProfiles(setOf("lineage-hidden")).map(ManagedProfileRecord::profileId),
        )
        assertEquals(
            listOf("updated-visible"),
            headphone.reviewableUpdatedEqProfiles(setOf("lineage-hidden")).map(ManagedProfileRecord::profileId),
        )
    }

    @Test
    fun hiddenReviewProjectionSuppressesNagWithoutChangingSelectionOrStoredProfile() {
        val hiddenProfile = managedProfile(
            profile = compatibleProfile("hidden-selected").copy(canonicalProfileId = "lineage-hidden"),
            selected = true,
            isNewUnreviewed = true,
            isUpdatedUnreviewed = true,
        )
        val visibleProfile = managedProfile(
            profile = compatibleProfile("visible-new").copy(canonicalProfileId = "lineage-visible"),
            selected = false,
            isNewUnreviewed = true,
        )
        val headphone = managedHeadphone(listOf(hiddenProfile, visibleProfile))

        val projected = headphone.withHiddenReviewPromptsSuppressed(setOf("lineage-hidden"))
        val projectedHidden = projected.profiles.first { it.profileId == "hidden-selected" }
        val projectedVisible = projected.profiles.first { it.profileId == "visible-new" }

        assertTrue(projectedHidden.selected)
        assertFalse(projectedHidden.isNewUnreviewed)
        assertFalse(projectedHidden.isUpdatedUnreviewed)
        assertEquals(hiddenProfile.lastKnownProfile, projectedHidden.lastKnownProfile)
        assertTrue(projectedVisible.isNewUnreviewed)
    }

    private fun managedHeadphone(profiles: List<ManagedProfileRecord>) = ManagedHeadphoneRecord(
        productId = "product",
        vendorId = "vendor",
        vendorName = "Vendor",
        productName = "Product",
        autoIncludeNewProfiles = true,
        createdAtMillis = 1L,
        updatedAtMillis = 2L,
        profiles = profiles,
    )

    private fun managedProfile(
        profile: OpraEqProfile,
        selected: Boolean = false,
        isNewUnreviewed: Boolean = false,
        isUpdatedUnreviewed: Boolean = false,
        noLongerAvailable: Boolean = false,
    ) = ManagedProfileRecord(
        profileId = profile.id,
        selected = selected,
        explicitlyExcluded = false,
        lastKnownProfile = profile,
        fingerprint = "fingerprint:${profile.id}",
        firstSeenAtMillis = 1L,
        lastSeenAtMillis = 2L,
        isNewUnreviewed = isNewUnreviewed,
        isUpdatedUnreviewed = isUpdatedUnreviewed,
        noLongerAvailable = noLongerAvailable,
        generatedPresetName = null,
        generatedXml = null,
        generatedFromFingerprint = null,
        generatedAtMillis = null,
    )

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