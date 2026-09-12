package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqCategory
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import org.junit.Test

class BlackPearlMyEqsCandidatesTest {
    @Test
    fun managedCanonicalProfileWinsOverFavoriteDuplicateAndUnselectedProfilesAreExcluded() {
        val canonical = profile("canonical-1", author = "Creator", details = "Target")
        val unselected = profile("not-selected")
        val managed = managedHeadphone(
            profiles = listOf(
                managedProfile(canonical, selected = true),
                managedProfile(unselected, selected = false),
            ),
        )
        val favorite = SavedEqRecord(
            entryId = "favorite-entry",
            kind = SavedEqKind.Favorite,
            sourceProfileId = canonical.id,
            productId = canonical.productId,
            manufacturer = "Maker",
            model = "Headphone X",
            displayName = "Favorite copy",
            profile = canonical,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )

        val candidates = buildBlackPearlMyEqsCandidates(
            managedHeadphones = listOf(managed),
            savedEqs = listOf(favorite),
            savedGeneralEqs = emptyList(),
        )

        assertThat(candidates).hasSize(1)
        assertThat(candidates.single().identity.savedEqKey).isEqualTo("canonical:canonical-1")
        assertThat(candidates.single().identity.displayName)
            .isEqualTo("Headphone X · Creator · Target")
        assertThat(candidates.single().profile).isEqualTo(canonical)
    }

    @Test
    fun personalAndGeneralEqsKeepSeparateLocalIdentities() {
        val personalProfile = profile("personal-eq:123", author = "Personal", details = "Personal import")
        val personal = SavedEqRecord(
            entryId = "personal:123",
            kind = SavedEqKind.Personal,
            sourceProfileId = null,
            productId = personalProfile.productId,
            manufacturer = "Custom",
            model = "My Headphone",
            displayName = "Desk EQ",
            profile = personalProfile,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val generalProfile = profile("general-profile")
        val general = SavedGeneralEqRecord(
            presetId = "general-preset",
            displayName = "Bass Lift",
            category = GeneralEqCategory.SOUND,
            profile = generalProfile,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )

        val candidates = buildBlackPearlMyEqsCandidates(
            managedHeadphones = emptyList(),
            savedEqs = listOf(personal),
            savedGeneralEqs = listOf(general),
        )

        assertThat(candidates.map { it.identity.savedEqKey })
            .containsExactly("saved:personal:123", "general:general-preset")
            .inOrder()
        assertThat(candidates.map { it.identity.displayName })
            .containsExactly("My Headphone · Desk EQ", "Bass Lift")
            .inOrder()
    }

    private fun managedHeadphone(profiles: List<ManagedProfileRecord>) = ManagedHeadphoneRecord(
        productId = "headphone",
        vendorId = "maker",
        vendorName = "Maker",
        productName = "Headphone X",
        autoIncludeNewProfiles = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        profiles = profiles,
    )

    private fun managedProfile(profile: OpraEqProfile, selected: Boolean) = ManagedProfileRecord(
        profileId = profile.id,
        selected = selected,
        explicitlyExcluded = !selected,
        lastKnownProfile = profile,
        fingerprint = "fingerprint:${profile.id}",
        firstSeenAtMillis = 1,
        lastSeenAtMillis = 1,
        isNewUnreviewed = false,
        isUpdatedUnreviewed = false,
        noLongerAvailable = false,
        generatedPresetName = null,
        generatedXml = null,
        generatedFromFingerprint = null,
        generatedAtMillis = null,
    )

    private fun profile(
        id: String,
        author: String? = "Creator",
        details: String? = "Test",
    ) = OpraEqProfile(
        id = id,
        productId = "headphone",
        author = author,
        details = details,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = -3.0,
        bands = listOf(OpraBand("peak_dip", 1000.0, 2.0, 1.0, null)),
    )
}
