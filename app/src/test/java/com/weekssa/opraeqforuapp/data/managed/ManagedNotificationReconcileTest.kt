package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedNotificationReconcileTest {
    private val codec = ManagedProfileSnapshotCodec()

    @Test
    fun newProfileWithNotificationsOffStaysUnselectedAndDoesNotCreateReviewAttention() {
        val profile = profile("new")

        val result = reconcileManagedProfiles(
            productId = "product",
            productName = "Headphone",
            currentProfiles = listOf(profile),
            existingProfiles = emptyList(),
            autoIncludeNewProfiles = false,
            nowMillis = 100L,
            snapshotCodec = codec,
        )

        val reconciled = result.profiles.single()
        assertFalse(reconciled.selected)
        assertFalse(reconciled.isNewUnreviewed)
        assertFalse(reconciled.isUpdatedUnreviewed)
    }

    @Test
    fun changedSelectedProfileWithNotificationsOffRegeneratesWithoutReviewAttention() {
        val original = profile("selected")
        val originalFingerprint = codec.fingerprint(original)
        val existing = ManagedProfileEntity(
            profileId = original.id,
            productId = original.productId,
            selected = true,
            explicitlyExcluded = false,
            snapshotJson = codec.encode(original),
            fingerprint = originalFingerprint,
            firstSeenAtMillis = 1L,
            lastSeenAtMillis = 1L,
            isNewUnreviewed = false,
            isUpdatedUnreviewed = false,
            noLongerAvailable = false,
            generatedPresetName = "Old preset",
            generatedXml = "old xml",
            generatedFromFingerprint = originalFingerprint,
            generatedAtMillis = 1L,
        )
        val changed = original.copy(
            bands = listOf(OpraBand("peak_dip", 1_000.0, 2.0, 1.0, null)),
        )

        val result = reconcileManagedProfiles(
            productId = "product",
            productName = "Headphone",
            currentProfiles = listOf(changed),
            existingProfiles = listOf(existing),
            autoIncludeNewProfiles = false,
            nowMillis = 200L,
            snapshotCodec = codec,
        )

        val reconciled = result.profiles.single()
        assertTrue(reconciled.selected)
        assertFalse(reconciled.isUpdatedUnreviewed)
        assertNotEquals(originalFingerprint, reconciled.generatedFromFingerprint)
    }

    private fun profile(id: String) = OpraEqProfile(
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
