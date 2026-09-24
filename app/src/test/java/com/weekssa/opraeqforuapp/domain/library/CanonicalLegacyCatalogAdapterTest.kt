package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.catalog.EqBandOrderProvenance
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConversionException
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConverter
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneSelection
import com.weekssa.opraeqforuapp.domain.managed.StoredProfileSelection
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalLegacyCatalogAdapterTest {
    @Test
    fun canonicalNonOpraOverBudgetSourceCannotUseOpraPriorityTruncation() {
        val source = EqSourceReference(
            sourceId = "community",
            sourceKind = EqSourceKind.COMMUNITY,
            sourceRecordId = "community-11-band",
            url = "https://example.com/community-11-band",
            creator = "Community author",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.LINK_ONLY,
            isPrimary = true,
        )
        val canonical = CanonicalEqProfile(
            canonicalProfileId = "community-11-band",
            headphone = HeadphoneIdentity("Maker", "Over Budget Model"),
            creator = "Community author",
            target = EqTarget(null, EqTargetKind.UNKNOWN),
            tuningLabel = "Eleven-band community profile",
            revisions = listOf(
                EqRevision(
                    revisionId = "r1",
                    acousticFingerprint = "fingerprint-11-band",
                    preampGainDb = -3.0,
                    filters = (1..11).map { index ->
                        EqFilter(EqFilterType.PEAK, 100.0 + index, 0.0, 1.0)
                    },
                    sourceReferences = listOf(source),
                    isLatest = true,
                ),
            ),
        )

        val projected = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-09-23T00:00:00Z", "test", listOf(canonical)),
        ).profiles.single()

        assertThat(projected.bandOrderProvenance).isNull()
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.convert(projected, "Over-budget community profile")
        }

        val opraReference = source.copy(
            sourceId = "opra",
            sourceKind = EqSourceKind.STRUCTURED_CATALOG,
            sourceRecordId = "opra-record",
            sourceVendorId = "opra-vendor",
            sourceProductId = "opra-product",
            provenanceTier = ProvenanceTier.AUTHORITATIVE,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
        )
        val mixedSource = canonical.copy(
            revisions = listOf(
                canonical.latestRevision.copy(sourceReferences = listOf(source, opraReference)),
            ),
        )
        val mixedProjection = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-09-23T00:00:00Z", "test", listOf(mixedSource)),
        ).profiles.single()
        assertThat(mixedProjection.bandOrderProvenance).isNull()
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.convert(mixedProjection, "Over-budget mixed-source profile")
        }
    }

    @Test
    fun opraPriorityRequiresPrimaryVendorAndProductToMatchProjectedIdentity() {
        val expectedSource = EqSourceReference(
            sourceId = "opra",
            sourceKind = EqSourceKind.STRUCTURED_CATALOG,
            sourceRecordId = "older-opra-record",
            sourceVendorId = "expected-vendor",
            sourceProductId = "shared-product-id",
            url = "https://example.com/older-opra-record",
            creator = "OPRA",
            provenanceTier = ProvenanceTier.AUTHORITATIVE,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            isPrimary = true,
        )
        val wrongVendorPrimary = expectedSource.copy(
            sourceRecordId = "newer-wrong-vendor-record",
            sourceVendorId = "different-vendor",
            isPrimary = true,
        )
        val canonical = CanonicalEqProfile(
            canonicalProfileId = "same-headphone",
            headphone = HeadphoneIdentity("Maker", "Model"),
            creator = "OPRA",
            target = EqTarget(null, EqTargetKind.UNKNOWN),
            tuningLabel = "Over-budget tuning",
            revisions = listOf(
                EqRevision(
                    revisionId = "older",
                    acousticFingerprint = "older-fingerprint",
                    preampGainDb = 0.0,
                    filters = listOf(EqFilter(EqFilterType.PEAK, 100.0, 0.0, 1.0)),
                    sourceReferences = listOf(expectedSource),
                    isLatest = false,
                ),
                EqRevision(
                    revisionId = "newer",
                    acousticFingerprint = "newer-fingerprint",
                    preampGainDb = 0.0,
                    filters = (1..11).map { index ->
                        EqFilter(EqFilterType.PEAK, index * 100.0, 0.0, 1.0)
                    },
                    sourceReferences = listOf(wrongVendorPrimary),
                    isLatest = true,
                ),
            ),
        )

        val projected = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-09-23T00:00:00Z", "test", listOf(canonical)),
        )
        val latest = projected.profiles.single { it.bands?.size == 11 }

        assertThat(projected.products.single().vendorId).isEqualTo("expected-vendor")
        assertThat(projected.products.single().id).isEqualTo("shared-product-id")
        assertThat(latest.bandOrderProvenance).isNull()
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.convert(latest, "Mismatched OPRA vendor")
        }

        val wrongProductPrimary = expectedSource.copy(
            sourceRecordId = "newer-wrong-product-record",
            sourceProductId = "different-product",
            isPrimary = true,
        )
        val wrongProductCanonical = canonical.copy(
            revisions = canonical.revisions.map { revision ->
                if (revision.isLatest) revision.copy(sourceReferences = listOf(wrongProductPrimary)) else revision
            },
        )
        val wrongProductProjection = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-09-23T00:00:00Z", "test", listOf(wrongProductCanonical)),
        )
        val wrongProductLatest = wrongProductProjection.profiles.single { it.bands?.size == 11 }

        assertThat(wrongProductProjection.products.single().id).isEqualTo("shared-product-id")
        assertThat(wrongProductLatest.bandOrderProvenance).isNull()
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.convert(wrongProductLatest, "Mismatched OPRA product")
        }
    }

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

        val latest = legacy.profiles.first { it.bands!!.single().frequency == 110.0 }
        val historical = legacy.profiles.first { it.bands!!.single().frequency == 100.0 }
        assertThat(latest.id)
            .isEqualTo("legacy-profile-id")
        assertThat(historical.id)
            .isEqualTo("eq-library:opra-history@old")
        assertThat(latest.bandOrderProvenance).isEqualTo(EqBandOrderProvenance.OPRA_SOURCE_PRIORITY)
        assertThat(historical.bandOrderProvenance).isEqualTo(EqBandOrderProvenance.OPRA_SOURCE_PRIORITY)
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

    @Test
    fun resolvesExactHeadphoneProjectionToFullProfileAndSelectedRevision() {
        val primary = EqSourceReference(
            sourceId = "community",
            sourceKind = EqSourceKind.COMMUNITY,
            sourceRecordId = "source-record",
            url = "https://example.com/source-record",
            creator = "Tester",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.LINK_ONLY,
            isPrimary = true,
        )
        val latest = revision("new", 200.0, primary, isLatest = true)
        val older = revision("old", 100.0, primary.copy(sourceRecordId = "source-record-old"), isLatest = false)
        val canonical = profile("canonical-headphone", HeadphoneIdentity("Maker", "Model"), "Tester", primary)
            .copy(revisions = listOf(older, latest))
        val snapshot = CatalogSnapshot(1, "2026-09-23T00:00:00Z", "test", listOf(canonical))
        val displayed = CanonicalLegacyCatalogAdapter.adapt(snapshot).profiles.single { it.id == "eq-library:canonical-headphone@new" }

        val selection = CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, displayed)

        assertThat(selection?.profile).isEqualTo(canonical)
        assertThat(selection?.selectedRevisionId).isEqualTo("new")
        assertThat(selection?.profile?.revisions?.map { it.revisionId }).containsExactly("old", "new").inOrder()
        assertThat(selection?.selectedRevision?.sourceReferences).containsExactly(latest.sourceReferences.single())
        assertThat(
            CanonicalLegacyCatalogAdapter.resolveSelection(
                snapshot,
                displayed.copy(bands = displayed.bands!!.map { it.copy(gainDb = requireNotNull(it.gainDb) + 0.25) }),
            ),
        ).isNull()
    }

    @Test
    fun resolvesGeneralPresetOnlyWhenAllProjectedValuesMatch() {
        val source = EqSourceReference(
            sourceId = "community",
            sourceKind = EqSourceKind.COMMUNITY,
            sourceRecordId = "general-record",
            url = "https://example.com/general-record",
            creator = "Tester",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.LINK_ONLY,
            isPrimary = true,
        )
        val canonical = CanonicalEqProfile(
            canonicalProfileId = "general-bass-boost",
            scope = EqProfileScope.GENERAL,
            purpose = EqPresetPurpose.EFFECT,
            creator = "Tester",
            target = EqTarget(null, EqTargetKind.UNKNOWN),
            tuningLabel = "Bass Boost",
            revisions = listOf(revision("general-r1", 120.0, source, isLatest = true)),
        )
        val snapshot = CatalogSnapshot(1, "2026-09-23T00:00:00Z", "test", listOf(canonical))
        val preset = CanonicalLegacyCatalogAdapter.adapt(snapshot).generalPresets.single()

        val selection = CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, preset)

        assertThat(selection?.profile).isEqualTo(canonical)
        assertThat(selection?.selectedRevisionId).isEqualTo("general-r1")
        assertThat(selection).isNotNull()
        assertThat(CanonicalLegacyCatalogAdapter.projectGeneralSelection(requireNotNull(selection), preset.id).bands)
            .isEqualTo(preset.bands)
        assertThat(CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, preset.copy(preampGainDb = 1.0))).isNull()
    }

    @Test
    fun selectedProjectionRetainsCompatibilityIdentityFromAnotherRevisionWithoutGrantingPriority() {
        val community = EqSourceReference(
            sourceId = "community",
            sourceKind = EqSourceKind.COMMUNITY,
            sourceRecordId = "community-revision",
            url = "https://example.com/community-revision",
            creator = "Community author",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.LINK_ONLY,
            isPrimary = true,
        )
        val opra = EqSourceReference(
            sourceId = "opra",
            sourceKind = EqSourceKind.STRUCTURED_CATALOG,
            sourceRecordId = "opra-revision",
            sourceVendorId = "opra-vendor",
            sourceProductId = "shared-product",
            url = "https://example.com/opra-revision",
            creator = "OPRA",
            provenanceTier = ProvenanceTier.AUTHORITATIVE,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            isPrimary = false,
        )
        val olderOpraRevision = EqRevision(
            revisionId = "mixed-r0",
            acousticFingerprint = "mixed-fingerprint-r0",
            preampGainDb = 0.0,
            filters = listOf(EqFilter(EqFilterType.PEAK, 800.0, 0.5, 1.0)),
            sourceReferences = listOf(opra),
            isLatest = false,
        )
        val currentCommunityRevision = EqRevision(
            revisionId = "mixed-r1",
            acousticFingerprint = "mixed-fingerprint-r1",
            preampGainDb = 0.0,
            filters = listOf(EqFilter(EqFilterType.PEAK, 1000.0, 1.0, 1.0)),
            sourceReferences = listOf(community),
            isLatest = true,
        )
        val canonical = CanonicalEqProfile(
            canonicalProfileId = "mixed-source-headphone",
            headphone = HeadphoneIdentity("Maker", "Mixed source model"),
            creator = "Community author",
            target = EqTarget(null, EqTargetKind.UNKNOWN),
            tuningLabel = "Mixed source",
            revisions = listOf(olderOpraRevision, currentCommunityRevision),
        )
        val snapshot = CatalogSnapshot(1, "2026-09-23T00:00:00Z", "test", listOf(canonical))
        val displayed = CanonicalLegacyCatalogAdapter.adapt(snapshot).profiles
            .single { it.id == "eq-library:mixed-source-headphone@mixed-r1" }
        val selection = requireNotNull(CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, displayed))

        assertThat(selection.compatibilityVendorId).isEqualTo("opra-vendor")
        assertThat(selection.compatibilityProductId).isEqualTo("shared-product")
        assertThat(CanonicalLegacyCatalogAdapter.matchesSelection(selection, displayed)).isTrue()
        assertThat(displayed.id).isEqualTo("eq-library:mixed-source-headphone@mixed-r1")
        assertThat(displayed.bandOrderProvenance).isNull()
        assertThat(CanonicalLegacyCatalogAdapter.projectSelection(selection, displayed.productId).bandOrderProvenance)
            .isNull()
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
