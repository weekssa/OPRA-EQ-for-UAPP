package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneSelection
import com.weekssa.opraeqforuapp.domain.managed.StoredProfileSelection
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
    fun projectsExplicitMeasurementDatabaseSeparatelyFromCarrierSource() {
        val headphone = HeadphoneIdentity("HIFIMAN", "Edition XS")
        val source = EqSourceReference(
            sourceId = "autoeq",
            sourceKind = EqSourceKind.MEASUREMENT_DERIVED,
            sourceRecordId = "results/HypetheSonics/over-ear/HIFIMAN Edition XS/HIFIMAN Edition XS ParametricEQ.txt",
            sourceDataset = "HypetheSonics",
            url = "https://example.com/autoeq",
            creator = "AutoEq",
            provenanceTier = ProvenanceTier.MEASUREMENT_DERIVED,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            isPrimary = true,
        )
        val canonical = CanonicalEqProfile(
            canonicalProfileId = "edition-xs-autoeq",
            headphone = headphone,
            creator = "AutoEq",
            target = EqTarget(null, EqTargetKind.UNKNOWN),
            tuningLabel = "AutoEq (HypetheSonics measurement)",
            revisions = listOf(revision("r1", 100.0, source, isLatest = true)),
        )

        val legacy = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-08-29T17:00:00Z", "test", listOf(canonical)),
        )

        assertThat(legacy.profiles.single().details).contains("Database: HypetheSonics")
        assertThat(legacy.profiles.single().details).contains("Measurement: HypetheSonics")
        assertThat(legacy.profiles.single().details).contains("Source: AutoEQ")
    }

    @Test
    fun recoversMeasurementDatabaseForOlderCatalogRecordsWithoutDatasetField() {
        val source = EqSourceReference(
            sourceId = "opra",
            sourceKind = EqSourceKind.STRUCTURED_CATALOG,
            sourceRecordId = "legacy-profile",
            sourceVendorId = "vendor",
            sourceProductId = "product",
            url = "https://example.com/opra",
            creator = "AutoEq",
            provenanceTier = ProvenanceTier.MEASUREMENT_DERIVED,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            isPrimary = true,
        )
        val canonical = CanonicalEqProfile(
            canonicalProfileId = "legacy-measurement",
            headphone = HeadphoneIdentity("Aero", "Test"),
            creator = "AutoEq",
            target = EqTarget(null, EqTargetKind.UNKNOWN),
            tuningLabel = "AutoEq (HypetheSonics / ANC Off measurement)",
            revisions = listOf(revision("r1", 100.0, source, isLatest = true)),
        )

        val legacy = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-08-29T17:00:00Z", "test", listOf(canonical)),
        )

        assertThat(legacy.profiles.single().details).contains("Database: HypetheSonics")
        assertThat(legacy.profiles.single().details).contains("Source: OPRA")
    }

    @Test
    fun projectsHistoricalRevisionsAsExplicitSelectableProfiles() {
        val source = EqSourceReference(
            sourceId = "community",
            sourceKind = EqSourceKind.COMMUNITY,
            sourceRecordId = "post-1",
            url = "https://example.com/post-1",
            creator = "User",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.LINK_ONLY,
            publishedAtEpochSeconds = 1_700_000_000,
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

        assertThat(legacy.profiles).hasSize(2)
        val latest = legacy.profiles.first { it.bands!!.single().frequency == 90.0 }
        val previous = legacy.profiles.first { it.bands!!.single().frequency == 80.0 }
        assertThat(latest.id).isEqualTo("eq-library:community-hd650@new")
        assertThat(previous.id).isEqualTo("eq-library:community-hd650@old")
        assertThat(latest.details).contains("Latest")
        assertThat(previous.details).contains("Previous revision")
        assertThat(previous.details).contains("Revision: 2023-11-14")
        assertThat(previous.details).contains("Target: Custom")
        assertThat(previous.details).contains("Source: Community")
        assertThat(previous.details).contains("Adds bass.")
        assertThat(previous.details).doesNotContain("Provenance:")
        assertThat(previous.details).doesNotContain("Version:")
    }

    @Test
    fun latestOpraRevisionKeepsLegacyIdButOlderOpraRevisionDoesNot() {
        val source = EqSourceReference(
            sourceId = "opra",
            sourceKind = EqSourceKind.STRUCTURED_CATALOG,
            sourceRecordId = "legacy-profile-id",
            sourceVendorId = "vendor",
            sourceProductId = "product",
            url = "https://example.com/opra",
            creator = "Creator",
            provenanceTier = ProvenanceTier.AUTHORITATIVE,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            isPrimary = true,
        )
        val profile = CanonicalEqProfile(
            canonicalProfileId = "opra-history",
            headphone = HeadphoneIdentity("Maker", "Model"),
            creator = "Creator",
            target = EqTarget("Target", EqTargetKind.EXPLICIT_TARGET),
            tuningLabel = "Tuning",
            revisions = listOf(
                revision("old", 100.0, source, isLatest = false),
                revision("new", 110.0, source, isLatest = true),
            ),
        )

        val legacy = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-08-29T17:00:00Z", "test", listOf(profile)),
        )

        assertThat(legacy.profiles.first { it.bands!!.single().frequency == 110.0 }.id)
            .isEqualTo("legacy-profile-id")
        assertThat(legacy.profiles.first { it.bands!!.single().frequency == 100.0 }.id)
            .isEqualTo("eq-library:opra-history@old")
    }

    @Test
    fun v02StoredSelectionStillSelectsLatestOpraRevisionAfterCanonicalCutover() {
        val source = EqSourceReference(
            sourceId = "opra",
            sourceKind = EqSourceKind.STRUCTURED_CATALOG,
            sourceRecordId = "legacy-profile-id",
            sourceVendorId = "legacy-vendor",
            sourceProductId = "legacy-product",
            url = "https://example.com/opra",
            creator = "Creator",
            provenanceTier = ProvenanceTier.AUTHORITATIVE,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            isPrimary = true,
        )
        val profile = CanonicalEqProfile(
            canonicalProfileId = "opra-history",
            headphone = HeadphoneIdentity("Maker", "Model"),
            creator = "Creator",
            target = EqTarget("Target", EqTargetKind.EXPLICIT_TARGET),
            tuningLabel = "Tuning",
            revisions = listOf(
                revision("old", 100.0, source, isLatest = false),
                revision("new", 110.0, source, isLatest = true),
            ),
        )
        val legacy = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-08-29T17:00:00Z", "test", listOf(profile)),
        )
        val v02Selection = ManagedHeadphoneSelection(
            productId = "legacy-product",
            autoIncludeNewProfiles = false,
            profileSelections = mapOf(
                "legacy-profile-id" to StoredProfileSelection(selected = true, explicitlyExcluded = false),
            ),
        )

        val latest = legacy.profiles.single { it.id == "legacy-profile-id" }
        assertThat(legacy.products.single().id).isEqualTo(v02Selection.productId)
        assertThat(v02Selection.isSelected(latest)).isTrue()
        assertThat(legacy.profiles.any { it.id == "eq-library:opra-history@old" }).isTrue()
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
