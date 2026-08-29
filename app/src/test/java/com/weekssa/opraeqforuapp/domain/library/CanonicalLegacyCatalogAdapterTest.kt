package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanonicalLegacyCatalogAdapterTest {
    @Test
    fun preservesOpraIdsAndAddsOtherSourcesToSameHeadphone() {
        val headphone = HeadphoneIdentity("Sennheiser", "HD 650")
        val opra = profile(
            id = "hd650-opra",
            headphone = headphone,
            creator = "Original Creator",
            source = EqSourceReference(
                sourceId = "opra",
                sourceKind = EqSourceKind.STRUCTURED_CATALOG,
                sourceRecordId = "opra-profile-123",
                sourceVendorId = "opra-vendor-sennheiser",
                sourceProductId = "opra-product-hd650",
                url = "https://example.com/opra",
                creator = "Original Creator",
                provenanceTier = ProvenanceTier.AUTHORITATIVE,
                redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
                isPrimary = true,
            ),
        )
        val autoEq = profile(
            id = "hd650-autoeq",
            headphone = headphone,
            creator = "AutoEq",
            source = EqSourceReference(
                sourceId = "autoeq",
                sourceKind = EqSourceKind.MEASUREMENT_DERIVED,
                sourceRecordId = "results/Sennheiser HD 650",
                url = "https://example.com/autoeq",
                creator = "AutoEq",
                provenanceTier = ProvenanceTier.MEASUREMENT_DERIVED,
                redistributionPolicy = RedistributionPolicy.UNKNOWN_REVIEW,
                isPrimary = true,
            ),
        )

        val legacy = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(
                schemaVersion = 1,
                generatedAt = "2026-08-29T17:00:00Z",
                sourceRegistryVersion = "test",
                profiles = listOf(opra, autoEq),
            ),
        )

        assertThat(legacy.vendors).containsExactly(
            com.weekssa.opraeqforuapp.domain.catalog.OpraVendor(
                id = "opra-vendor-sennheiser",
                name = "Sennheiser",
            ),
        )
        assertThat(legacy.products).hasSize(1)
        assertThat(legacy.products.single().id).isEqualTo("opra-product-hd650")
        assertThat(legacy.profiles).hasSize(2)
        assertThat(legacy.profiles.map { it.productId }.distinct()).containsExactly("opra-product-hd650")
        assertThat(legacy.profiles.first { it.author == "Original Creator" }.id)
            .isEqualTo("opra-profile-123")
        assertThat(legacy.profiles.first { it.author == "AutoEq" }.id)
            .startsWith("eq-library:hd650-autoeq@")
    }

    @Test
    fun projectsOnlyLatestRevisionIntoLegacySelectionEngine() {
        val source = EqSourceReference(
            sourceId = "community",
            sourceKind = EqSourceKind.COMMUNITY,
            sourceRecordId = "post-1",
            url = "https://example.com/post-1",
            creator = "User",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.LINK_ONLY,
            isPrimary = true,
        )
        val profile = CanonicalEqProfile(
            canonicalProfileId = "community-hd650",
            headphone = HeadphoneIdentity("Sennheiser", "HD 650"),
            creator = "User",
            target = EqTarget("Custom", EqTargetKind.CUSTOM_USER),
            tuningLabel = "Bass",
            revisions = listOf(
                revision("old", 80.0, source, isLatest = false),
                revision("new", 90.0, source, isLatest = true),
            ),
        )

        val legacy = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-08-29T17:00:00Z", "test", listOf(profile)),
        )

        assertThat(legacy.profiles).hasSize(1)
        assertThat(legacy.profiles.single().bands!!.single().frequency).isEqualTo(90.0)
    }

    private fun profile(
        id: String,
        headphone: HeadphoneIdentity,
        creator: String,
        source: EqSourceReference,
    ) = CanonicalEqProfile(
        canonicalProfileId = id,
        headphone = headphone,
        creator = creator,
        target = EqTarget("Harman", EqTargetKind.EXPLICIT_TARGET),
        tuningLabel = "Neutral",
        revisions = listOf(revision("r1", 100.0, source, isLatest = true)),
    )

    private fun revision(
        id: String,
        frequency: Double,
        source: EqSourceReference,
        isLatest: Boolean,
    ) = EqRevision(
        revisionId = id,
        acousticFingerprint = "fingerprint-$id",
        preampGainDb = -3.0,
        filters = listOf(EqFilter(EqFilterType.PEAK, frequency, 2.0, 1.0)),
        sourceReferences = listOf(source),
        soundImpactSummary = "Adds bass.",
        isLatest = isLatest,
    )
}
