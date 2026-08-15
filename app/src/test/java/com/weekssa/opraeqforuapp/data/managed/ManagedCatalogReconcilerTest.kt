package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedCatalogReconcilerTest {
    private val codec = ManagedProfileSnapshotCodec()

    @Test
    fun newProfilesRespectAutoIncludeAndCompatibility() {
        val compatible = compatibleProfile("new-compatible")
        val blocked = compatibleProfile("new-blocked").copy(
            bands = listOf(OpraBand("low_pass", 1_000.0, 0.0, 1.0, 12.0)),
        )

        val result = reconcileManagedProfiles(
            productId = "product",
            productName = "Headphone",
            currentProfiles = listOf(compatible, blocked),
            existingProfiles = emptyList(),
            autoIncludeNewProfiles = true,
            nowMillis = 100L,
            snapshotCodec = codec,
        )

        val compatibleResult = result.profiles.first { it.profileId == compatible.id }
        val blockedResult = result.profiles.first { it.profileId == blocked.id }
        assertTrue(compatibleResult.selected)
        assertNotNull(compatibleResult.generatedXml)
        assertFalse(blockedResult.selected)
        assertEquals(null, blockedResult.generatedXml)
        assertEquals(2, result.changes.newProfileCount)
    }

    @Test
    fun newCompatibleProfileRemainsUnselectedWhenAutoIncludeIsOff() {
        val profile = compatibleProfile("new")

        val result = reconcileManagedProfiles(
            productId = "product",
            productName = "Headphone",
            currentProfiles = listOf(profile),
            existingProfiles = emptyList(),
            autoIncludeNewProfiles = false,
            nowMillis = 100L,
            snapshotCodec = codec,
        )

        assertFalse(result.profiles.single().selected)
        assertEquals(null, result.profiles.single().generatedXml)
    }

    @Test
    fun changedSelectedCompatibleProfileRegeneratesAndMarksUpdated() {
        val oldProfile = compatibleProfile("profile")
        val existing = existingEntity(oldProfile, selected = true, generatedXml = "old xml")
        val changed = oldProfile.copy(
            bands = oldProfile.bands!!.mapIndexed { index, band ->
                if (index == 0) band.copy(gainDb = 2.0) else band
            },
        )

        val result = reconcileManagedProfiles(
            productId = "product",
            productName = "Headphone",
            currentProfiles = listOf(changed),
            existingProfiles = listOf(existing),
            autoIncludeNewProfiles = true,
            nowMillis = 200L,
            snapshotCodec = codec,
        )

        val reconciled = result.profiles.single()
        assertTrue(reconciled.selected)
        assertTrue(reconciled.isUpdatedUnreviewed)
        assertNotEquals("old xml", reconciled.generatedXml)
        assertEquals(codec.fingerprint(changed), reconciled.generatedFromFingerprint)
        assertEquals(1, result.changes.updatedSelectedProfileCount)
    }

    @Test
    fun selectedProfileBecomingNotCompatibleIsUnselectedButKeepsLastGeneratedPreset() {
        val oldProfile = compatibleProfile("profile")
        val existing = existingEntity(oldProfile, selected = true, generatedXml = "last good xml")
        val incompatible = oldProfile.copy(
            bands = listOf(OpraBand("band_stop", 1_000.0, 0.0, 1.0, null)),
        )

        val result = reconcileManagedProfiles(
            productId = "product",
            productName = "Headphone",
            currentProfiles = listOf(incompatible),
            existingProfiles = listOf(existing),
            autoIncludeNewProfiles = true,
            nowMillis = 200L,
            snapshotCodec = codec,
        )

        val reconciled = result.profiles.single()
        assertFalse(reconciled.selected)
        assertTrue(reconciled.isUpdatedUnreviewed)
        assertEquals("last good xml", reconciled.generatedXml)
        assertEquals(existing.generatedFromFingerprint, reconciled.generatedFromFingerprint)
        assertEquals(1, result.changes.becameNotCompatibleSelectedProfileCount)
        assertEquals(1, result.changes.updatedSelectedProfileCount)
    }

    @Test
    fun removedSelectedProfileIsRetainedWithGeneratedPresetAndMarkedUnavailable() {
        val profile = compatibleProfile("removed")
        val existing = existingEntity(profile, selected = true, generatedXml = "retained xml")

        val result = reconcileManagedProfiles(
            productId = "product",
            productName = "Headphone",
            currentProfiles = emptyList(),
            existingProfiles = listOf(existing),
            autoIncludeNewProfiles = true,
            nowMillis = 200L,
            snapshotCodec = codec,
        )

        val retained = result.profiles.single()
        assertTrue(retained.selected)
        assertTrue(retained.noLongerAvailable)
        assertEquals("retained xml", retained.generatedXml)
        assertEquals(1, result.changes.removedSelectedProfileCount)
    }

    @Test
    fun explicitExclusionPersistsAndLinkOnlyChangeDoesNotMarkUpdated() {
        val oldProfile = compatibleProfile("excluded")
        val existing = existingEntity(
            oldProfile,
            selected = false,
            explicitlyExcluded = true,
            generatedXml = null,
        )
        val linkChanged = oldProfile.copy(link = "https://example.invalid/new-link")

        val result = reconcileManagedProfiles(
            productId = "product",
            productName = "Headphone",
            currentProfiles = listOf(linkChanged),
            existingProfiles = listOf(existing),
            autoIncludeNewProfiles = true,
            nowMillis = 200L,
            snapshotCodec = codec,
        )

        val reconciled = result.profiles.single()
        assertFalse(reconciled.selected)
        assertTrue(reconciled.explicitlyExcluded)
        assertFalse(reconciled.isUpdatedUnreviewed)
        assertEquals(0, result.changes.updatedSelectedProfileCount)
    }

    private fun existingEntity(
        profile: OpraEqProfile,
        selected: Boolean,
        explicitlyExcluded: Boolean = false,
        generatedXml: String?,
    ): ManagedProfileEntity {
        val fingerprint = codec.fingerprint(profile)
        return ManagedProfileEntity(
            profileId = profile.id,
            productId = profile.productId,
            selected = selected,
            explicitlyExcluded = explicitlyExcluded,
            snapshotJson = codec.encode(profile),
            fingerprint = fingerprint,
            firstSeenAtMillis = 1L,
            lastSeenAtMillis = 1L,
            isNewUnreviewed = false,
            isUpdatedUnreviewed = false,
            noLongerAvailable = false,
            generatedPresetName = if (generatedXml != null) "Old preset" else null,
            generatedXml = generatedXml,
            generatedFromFingerprint = if (generatedXml != null) fingerprint else null,
            generatedAtMillis = if (generatedXml != null) 1L else null,
        )
    }

    private fun compatibleProfile(id: String) = OpraEqProfile(
        id = id,
        productId = "product",
        author = "Creator",
        details = "Target",
        link = "https://example.invalid/source",
        profileType = "parametric_eq",
        preampGainDb = -2.0,
        bands = listOf(OpraBand("peak_dip", 1_000.0, 1.0, 1.0, null)),
    )
}
