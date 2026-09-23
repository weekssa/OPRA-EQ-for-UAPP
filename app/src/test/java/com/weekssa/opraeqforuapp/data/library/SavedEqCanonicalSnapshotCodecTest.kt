package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.HeadphoneIdentity
import com.weekssa.opraeqforuapp.domain.library.EqSourceKind
import com.weekssa.opraeqforuapp.domain.library.EqSourceReference
import com.weekssa.opraeqforuapp.domain.library.EqTarget
import com.weekssa.opraeqforuapp.domain.library.EqTargetKind
import com.weekssa.opraeqforuapp.domain.library.LocalSavedEqAdapter
import com.weekssa.opraeqforuapp.domain.library.ProvenanceTier
import com.weekssa.opraeqforuapp.domain.library.RedistributionPolicy
import com.weekssa.opraeqforuapp.domain.library.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedEqCanonicalSnapshotCodecTest {
    private val codec = SavedEqCanonicalSnapshotCodec()

    @Test
    fun roundTripPreservesOrderNullsUnsupportedTypeAndLocalProvenance() {
        val snapshot = requireNotNull(
            LocalSavedEqAdapter.adapt(
                profile = OpraEqProfile(
                    id = "personal-eq:2",
                    productId = "personal-product:2",
                    author = null,
                    details = null,
                    link = null,
                    profileType = "parametric_eq",
                    preampGainDb = null,
                    bands = listOf(
                        OpraBand("odd_filter", 250.0, null, null, null),
                        OpraBand("peak_dip", 1_000.0, 1.5, 1.2, null),
                    ),
                ),
                displayName = "Local curve",
                headphone = null,
                target = EqTarget(name = "Custom target", kind = EqTargetKind.CUSTOM_USER),
                sourceReference = EqSourceReference(
                    sourceId = "personal_import",
                    sourceKind = EqSourceKind.PERSONAL_IMPORT,
                    sourceRecordId = "row-2",
                    url = null,
                    creator = null,
                    provenanceTier = ProvenanceTier.NEEDS_REVIEW,
                    redistributionPolicy = RedistributionPolicy.UNKNOWN_REVIEW,
                    discoveredAtEpochSeconds = 99L,
                ),
                verificationStatus = VerificationStatus.UNVERIFIED,
                observedAtEpochSeconds = 99L,
            ),
        )

        val encoded = codec.encode(snapshot)
        val decoded = codec.decode(encoded)

        assertEquals(snapshot, decoded)
        assertEquals(listOf(EqFilterType.OTHER, EqFilterType.PEAK), decoded.revision.filters.map { it.type })
        assertEquals("odd_filter", decoded.revision.filters.first().sourceType)
        assertNull(decoded.revision.filters.first().gainDb)
        assertNull(decoded.revision.preampGainDb)
        assertEquals(VerificationStatus.UNVERIFIED, decoded.revision.verificationStatus)
        assertTrue(encoded.contains("\"gain_db\":null"))
        assertTrue(encoded.contains("\"source_type\":\"odd_filter\""))
    }

    @Test
    fun canonicalSnapshotIsAuthoritativeAndLegacyRowsRemainReadable() {
        val legacyCodec = ManagedProfileSnapshotCodec()
        val canonical = requireNotNull(
            LocalSavedEqAdapter.adapt(
                profile = profile("canonical", 1_000.0),
                displayName = "Canonical row",
                headphone = HeadphoneIdentity("Canonical maker", "Canonical model"),
                target = EqTarget(name = null, kind = EqTargetKind.UNKNOWN),
                sourceReference = source("canonical-record"),
                verificationStatus = VerificationStatus.UNVERIFIED,
                observedAtEpochSeconds = 10L,
            ),
        )
        val canonicalEntity = entity(
            profile = profile("stale-legacy", 3_000.0),
            canonicalSnapshotJson = codec.encode(canonical),
            legacyCodec = legacyCodec,
        )

        val canonicalRecord = SavedEqRecordMapper.toDomain(
            entity = canonicalEntity,
            legacyCodec = legacyCodec,
            canonicalCodec = codec,
            captureMetadataCodec = SavedEqCaptureMetadataCodec(),
        )
        assertEquals(canonical, canonicalRecord.canonicalSnapshot)
        assertEquals("canonical", canonicalRecord.profile.id)
        assertEquals("Canonical row", canonicalRecord.displayName)
        assertEquals("Canonical maker", canonicalRecord.manufacturer)
        assertEquals("Canonical model", canonicalRecord.model)
        assertEquals(1_000.0, canonicalRecord.profile.bands!!.single().frequency!!, 0.0)

        val legacyEntity = entity(profile("old", 2_000.0), null, legacyCodec)
        val legacyRecord = SavedEqRecordMapper.toDomain(
            entity = legacyEntity,
            legacyCodec = legacyCodec,
            canonicalCodec = codec,
            captureMetadataCodec = SavedEqCaptureMetadataCodec(),
        )
        assertNull(legacyRecord.canonicalSnapshot)
        assertEquals("old", legacyRecord.profile.id)
        assertEquals(2_000.0, legacyRecord.profile.bands!!.single().frequency!!, 0.0)
    }

    @Test
    fun malformedCanonicalPayloadIsQuarantinedAndLegacyProjectionIsDisplayOnly() {
        val legacyCodec = ManagedProfileSnapshotCodec()
        val entity = entity(
            profile = profile("legacy", 2_000.0),
            canonicalSnapshotJson = "{not-json}",
            legacyCodec = legacyCodec,
        )

        val record = SavedEqRecordMapper.toDomain(
            entity = entity,
            legacyCodec = legacyCodec,
            canonicalCodec = codec,
            captureMetadataCodec = SavedEqCaptureMetadataCodec(),
        )

        assertTrue(record.savedEqDataInvalid)
        assertNull(record.canonicalSnapshot)
        assertEquals("legacy", record.profile.id)
    }

    @Test
    fun semanticallyInvalidLegacyProfileIsQuarantinedWithoutDiscardingItsDisplayValues() {
        val legacyCodec = ManagedProfileSnapshotCodec()
        val invalidProfile = profile("legacy-invalid", 0.0)
        val record = SavedEqRecordMapper.toDomain(
            entity = entity(invalidProfile, null, legacyCodec),
            legacyCodec = legacyCodec,
            canonicalCodec = codec,
            captureMetadataCodec = SavedEqCaptureMetadataCodec(),
        )

        assertTrue(record.savedEqDataInvalid)
        assertEquals(0.0, record.profile.bands!!.single().frequency!!, 0.0)
    }

    @Test
    fun legacyProfileWithMismatchedProductIdentityIsQuarantinedButRemainsReadable() {
        val legacyCodec = ManagedProfileSnapshotCodec()
        val validProfile = profile("legacy-product-mismatch", 2_000.0)

        val record = SavedEqRecordMapper.toDomain(
            entity = entity(validProfile, null, legacyCodec).copy(productId = "different-product"),
            legacyCodec = legacyCodec,
            canonicalCodec = codec,
            captureMetadataCodec = SavedEqCaptureMetadataCodec(),
        )

        assertTrue(record.savedEqDataInvalid)
        assertEquals("different-product", record.productId)
        assertEquals(validProfile, record.profile)
    }

    @Test
    fun favoriteProfileWithMismatchedSourceIdentityIsQuarantinedButRemainsReadable() {
        val legacyCodec = ManagedProfileSnapshotCodec()
        val validProfile = profile("favorite-profile", 2_000.0)

        val record = SavedEqRecordMapper.toDomain(
            entity = entity(validProfile, null, legacyCodec).copy(
                kind = SavedEqRepository.KIND_FAVORITE,
                sourceProfileId = "different-profile",
            ),
            legacyCodec = legacyCodec,
            canonicalCodec = codec,
            captureMetadataCodec = SavedEqCaptureMetadataCodec(),
        )

        assertTrue(record.savedEqDataInvalid)
        assertEquals("favorite-profile", record.profile.id)
        assertEquals("different-profile", record.sourceProfileId)
    }

    @Test
    fun parametricLegacyProfileMissingRequiredQIsQuarantined() {
        val legacyCodec = ManagedProfileSnapshotCodec()
        val invalidProfile = profile("legacy-missing-q", 1_000.0).copy(
            bands = listOf(OpraBand("peak_dip", 1_000.0, 1.0, null, null)),
        )

        val record = SavedEqRecordMapper.toDomain(
            entity = entity(invalidProfile, null, legacyCodec),
            legacyCodec = legacyCodec,
            canonicalCodec = codec,
            captureMetadataCodec = SavedEqCaptureMetadataCodec(),
        )

        assertTrue(record.savedEqDataInvalid)
        assertNull(record.profile.bands!!.single().q)
    }

    @Test
    fun validUnsupportedLegacyProfileIsPreservedWithoutBeingMisreportedAsCorrupt() {
        val legacyCodec = ManagedProfileSnapshotCodec()
        val unsupportedProfile = profile("legacy-graphic", 1_000.0).copy(
            profileType = "graphic_eq",
            bands = listOf(OpraBand("graphic_band", null, 1.0, null, null)),
        )

        val record = SavedEqRecordMapper.toDomain(
            entity = entity(unsupportedProfile, null, legacyCodec),
            legacyCodec = legacyCodec,
            canonicalCodec = codec,
            captureMetadataCodec = SavedEqCaptureMetadataCodec(),
        )

        assertEquals(false, record.savedEqDataInvalid)
        assertEquals(unsupportedProfile, record.profile)
    }

    @Test
    fun canonicalUnassociatedEqDoesNotInheritStaleLegacyHeadphoneMetadata() {
        val legacyCodec = ManagedProfileSnapshotCodec()
        val canonical = requireNotNull(
            LocalSavedEqAdapter.adapt(
                profile = profile("unassociated", 1_000.0),
                displayName = "Unassociated",
                headphone = null,
                target = EqTarget(name = null, kind = EqTargetKind.UNKNOWN),
                sourceReference = source("capture"),
                verificationStatus = VerificationStatus.VERIFIED,
                observedAtEpochSeconds = 10L,
            ),
        )
        val entity = entity(
            profile = profile("old", 2_000.0),
            canonicalSnapshotJson = codec.encode(canonical),
            legacyCodec = legacyCodec,
        ).copy(manufacturer = "Stale maker", model = "Stale model")

        val record = SavedEqRecordMapper.toDomain(
            entity = entity,
            legacyCodec = legacyCodec,
            canonicalCodec = codec,
            captureMetadataCodec = SavedEqCaptureMetadataCodec(),
        )

        assertEquals("", record.manufacturer)
        assertEquals("", record.model)
        assertEquals("Unassociated", record.displayName)
    }

    private fun entity(
        profile: OpraEqProfile,
        canonicalSnapshotJson: String?,
        legacyCodec: ManagedProfileSnapshotCodec,
    ) = SavedEqEntity(
        entryId = "personal:test",
        kind = "personal",
        sourceProfileId = null,
        productId = profile.productId,
        manufacturer = "",
        model = "",
        displayName = "Saved",
        profileJson = legacyCodec.encode(profile),
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
        canonicalSnapshotJson = canonicalSnapshotJson,
    )

    private fun profile(id: String, frequency: Double) = OpraEqProfile(
        id = id,
        productId = "personal-product:$id",
        author = "Personal",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = null,
        bands = listOf(OpraBand("peak_dip", frequency, 1.0, 1.0, null)),
    )

    private fun source(recordId: String) = EqSourceReference(
        sourceId = "personal_import",
        sourceKind = EqSourceKind.PERSONAL_IMPORT,
        sourceRecordId = recordId,
        url = null,
        creator = null,
        provenanceTier = ProvenanceTier.NEEDS_REVIEW,
        redistributionPolicy = RedistributionPolicy.UNKNOWN_REVIEW,
    )
}
