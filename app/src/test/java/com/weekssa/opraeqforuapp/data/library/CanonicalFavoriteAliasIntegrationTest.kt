package com.weekssa.opraeqforuapp.data.library

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.weekssa.opraeqforuapp.data.catalog.AppCatalogRepository
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.catalog.EqBandOrderProvenance
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import com.weekssa.opraeqforuapp.domain.library.AcousticFingerprint
import com.weekssa.opraeqforuapp.domain.library.CanonicalEqProfile
import com.weekssa.opraeqforuapp.domain.library.CanonicalEqSelection
import com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapter
import com.weekssa.opraeqforuapp.domain.library.CatalogSnapshot
import com.weekssa.opraeqforuapp.domain.library.EqFilter
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.EqPresetPurpose
import com.weekssa.opraeqforuapp.domain.library.EqProfileScope
import com.weekssa.opraeqforuapp.domain.library.EqRevision
import com.weekssa.opraeqforuapp.domain.library.EqSourceKind
import com.weekssa.opraeqforuapp.domain.library.EqSourceReference
import com.weekssa.opraeqforuapp.domain.library.EqTarget
import com.weekssa.opraeqforuapp.domain.library.EqTargetKind
import com.weekssa.opraeqforuapp.domain.library.HeadphoneIdentity
import com.weekssa.opraeqforuapp.domain.library.ProvenanceTier
import com.weekssa.opraeqforuapp.domain.library.RedistributionPolicy
import com.weekssa.opraeqforuapp.domain.library.VerificationStatus
import java.nio.file.Files
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

@Serializable
private data class FavoriteSourceSampleFixture(
    @SerialName("schema_version") val schemaVersion: Int,
    val samples: List<FavoriteSourceSample>,
    @SerialName("excluded_registered_sources") val excludedRegisteredSources: List<FavoriteSourceExclusion>,
    val snapshot: CatalogSnapshot,
)

@Serializable
private data class FavoriteSourceSample(
    @SerialName("source_id") val sourceId: String,
    @SerialName("canonical_profile_id") val canonicalProfileId: String,
    @SerialName("revision_id") val revisionId: String,
    @SerialName("source_record_id") val sourceRecordId: String?,
    @SerialName("source_kind") val sourceKind: String,
    val scope: String,
    @SerialName("is_latest_revision") val isLatestRevision: Boolean,
    @SerialName("is_primary_reference") val isPrimaryReference: Boolean,
)

@Serializable
private data class FavoriteSourceExclusion(
    @SerialName("source_id") val sourceId: String,
    val reason: String,
)

class CanonicalFavoriteAliasIntegrationTest {
    @Test
    fun communityFavoriteUsesExactSelectionAcrossLegacyProductAlias() = runBlocking {
        val snapshot = currentAfulCommunitySnapshot()
        val filesDir = Files.createTempDirectory("aful-community-canonical").toFile()
        val canonicalRepository = CanonicalCatalogRepository(
            filesDir = filesDir,
            source = { destination ->
                destination.writeText(Json.encodeToString(snapshot), Charsets.UTF_8)
            },
        )
        val legacyRepository = FakeCatalogRepository(
            OpraCatalog(
                vendors = listOf(OpraVendor(id = "aful", name = "AFUL")),
                products = listOf(
                    OpraProduct(
                        id = LEGACY_EXPLORER_PRODUCT_ID,
                        vendorId = "aful",
                        name = "Explorer",
                        type = "headphones",
                        subtype = "in_ear",
                    ),
                ),
                profiles = emptyList(),
            ),
        )
        try {
            val repository = CanonicalFirstCatalogRepository(canonicalRepository, legacyRepository)

            repository.initialize()

            val effectiveCatalog = (repository.state.value as CatalogState.Ready).catalog
            val canonicalProjection = CanonicalLegacyCatalogAdapter.adapt(snapshot)
            val canonicalCompatibilityProductId = canonicalProjection.products.single().id
            assertThat(canonicalCompatibilityProductId).isEqualTo("eq-library-product:aful|explorer||")
            assertThat(effectiveCatalog.productAliases[canonicalCompatibilityProductId])
                .isEqualTo(LEGACY_EXPLORER_PRODUCT_ID)

            snapshot.profiles.forEach { canonicalProfile ->
                val displayed = effectiveCatalog.profiles.single {
                    it.canonicalProfileId == canonicalProfile.canonicalProfileId
                }
                assertThat(displayed.productId).isEqualTo(LEGACY_EXPLORER_PRODUCT_ID)

                val preFixSelection = requireNotNull(
                    CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, displayed),
                )
                assertThat(preFixSelection.compatibilityProductId).isEqualTo(canonicalCompatibilityProductId)
                assertThat(CanonicalLegacyCatalogAdapter.matchesSelection(preFixSelection, displayed)).isFalse()
                assertThat(
                    CanonicalLegacyCatalogAdapter.matchesSelection(
                        preFixSelection,
                        displayed.copy(productId = requireNotNull(preFixSelection.compatibilityProductId)),
                    ),
                ).isTrue()

                val selection = repository.resolveCanonicalSelection(displayed)
                assertThat(selection).isNotNull()
                val exactSelection = requireNotNull(selection)
                assertThat(exactSelection.compatibilityVendorId).isEqualTo("aful")
                assertThat(exactSelection.compatibilityProductId).isEqualTo(displayed.productId)
                assertThat(CanonicalLegacyCatalogAdapter.matchesSelection(exactSelection, displayed)).isTrue()
                assertThat(exactSelection.profile.canonicalProfileId)
                    .isEqualTo(canonicalProfile.canonicalProfileId)
                assertThat(exactSelection.selectedRevisionId)
                    .isEqualTo(canonicalProfile.latestRevision.revisionId)
                assertThat(exactSelection.selectedRevision.acousticFingerprint)
                    .isEqualTo(canonicalProfile.latestRevision.acousticFingerprint)
                assertThat(exactSelection.selectedRevision.sourceReferences)
                    .containsExactlyElementsIn(canonicalProfile.latestRevision.sourceReferences)
                assertThat(exactSelection.selectedRevision.sourceReferences.single().sourceVendorId)
                    .isEqualTo("AFUL")
                assertThat(exactSelection.selectedRevision.sourceReferences.single().sourceProductId)
                    .isEqualTo("Explorer")

                val stale = displayed.copy(
                    bands = displayed.bands!!.mapIndexed { index, band ->
                        if (index == 0) band.copy(gainDb = requireNotNull(band.gainDb) + 0.1) else band
                    },
                )
                assertThat(repository.resolveCanonicalSelection(stale)).isNull()
                assertThat(
                    repository.resolveCanonicalSelection(
                        displayed.copy(canonicalProfileId = "community-not-current"),
                    ),
                ).isNull()

                val roundTripped = CanonicalEqSelectionCodec().decode(
                    CanonicalEqSelectionCodec().encode(exactSelection),
                )
                assertThat(roundTripped).isEqualTo(exactSelection)
            }
        } finally {
            filesDir.deleteRecursively()
        }
    }

    @Test
    fun favoriteAliasResolutionIsIndependentOfCatalogSourceKindAndHeadphoneProduct() = runBlocking {
        val sourceCases = catalogSourceCases()
        val inScopeCatalogKinds = EqSourceKind.values().toSet() - setOf(
            EqSourceKind.DEVICE_CAPTURE,
            EqSourceKind.PERSONAL_IMPORT,
        )
        val coveredKinds = sourceCases.map(SourceCase::sourceKind)
        assertThat(coveredKinds.size).isEqualTo(inScopeCatalogKinds.size)
        assertThat(coveredKinds.toSet()).isEqualTo(inScopeCatalogKinds)
        val baseProfile = currentAfulCommunitySnapshot().profiles.first()
        val profiles = sourceCases.mapIndexed { index, sourceCase ->
            val filters = baseProfile.latestRevision.filters.mapIndexed { filterIndex, filter ->
                if (filterIndex == 0) {
                    filter.copy(gainDb = requireNotNull(filter.gainDb) + index * 0.125)
                } else {
                    filter
                }
            }
            val creator = "Alias fixture $index"
            val sourceReference = baseProfile.latestRevision.sourceReferences.single().copy(
                sourceId = sourceCase.sourceId,
                sourceKind = sourceCase.sourceKind,
                sourceRecordId = "${sourceCase.sourceId}-record-$index",
                sourceVendorId = if (sourceCase.sourceKind == EqSourceKind.STRUCTURED_CATALOG) {
                    sourceCase.vendorId
                } else {
                    sourceCase.manufacturer
                },
                sourceProductId = if (sourceCase.sourceKind == EqSourceKind.STRUCTURED_CATALOG) {
                    sourceCase.productId
                } else {
                    sourceCase.model
                },
                creator = creator,
                provenanceTier = sourceCase.provenanceTier,
                redistributionPolicy = sourceCase.redistributionPolicy,
            )
            val revision = baseProfile.latestRevision.copy(
                revisionId = "${sourceCase.sourceId}-revision-$index",
                acousticFingerprint = AcousticFingerprint.of(
                    baseProfile.latestRevision.preampGainDb,
                    filters,
                ),
                filters = filters,
                sourceReferences = listOf(sourceReference),
            )
            baseProfile.copy(
                canonicalProfileId = "${sourceCase.sourceId}-profile-$index",
                headphone = HeadphoneIdentity(sourceCase.manufacturer, sourceCase.model),
                creator = creator,
                tuningLabel = "${sourceCase.sourceKind} alias fixture",
                revisions = listOf(revision),
            )
        }
        val snapshot = currentAfulCommunitySnapshot().copy(
            sourceRegistryVersion = "favorite-alias-source-matrix",
            profiles = profiles,
        )
        val legacyCatalog = OpraCatalog(
            vendors = sourceCases.distinctBy(SourceCase::vendorId).map { sourceCase ->
                OpraVendor(id = sourceCase.vendorId, name = sourceCase.manufacturer)
            },
            products = sourceCases.distinctBy(SourceCase::productId).map { sourceCase ->
                OpraProduct(
                    id = sourceCase.productId,
                    vendorId = sourceCase.vendorId,
                    name = sourceCase.model,
                    type = "headphones",
                    subtype = sourceCase.productSubtype,
                )
            },
            profiles = emptyList(),
        )
        val filesDir = Files.createTempDirectory("canonical-favorite-source-matrix").toFile()
        val canonicalRepository = CanonicalCatalogRepository(
            filesDir = filesDir,
            source = { destination ->
                destination.writeText(Json.encodeToString(snapshot), Charsets.UTF_8)
            },
        )
        val repository = CanonicalFirstCatalogRepository(
            canonicalRepository,
            FakeCatalogRepository(legacyCatalog),
        )

        try {
            repository.initialize()

            val effectiveCatalog = (repository.state.value as CatalogState.Ready).catalog
            val canonicalProjection = CanonicalLegacyCatalogAdapter.adapt(snapshot)
            sourceCases.forEachIndexed { index, sourceCase ->
                val canonicalProfile = profiles[index]
                val projectedProfile = canonicalProjection.profiles.single {
                    it.canonicalProfileId == canonicalProfile.canonicalProfileId
                }
                assertThat(effectiveCatalog.canonicalProductId(projectedProfile.productId))
                    .isEqualTo(sourceCase.productId)

                val displayedProfile = effectiveCatalog.profiles.single {
                    it.canonicalProfileId == canonicalProfile.canonicalProfileId
                }
                assertThat(displayedProfile.productId).isEqualTo(sourceCase.productId)

                val originalSelection = requireNotNull(
                    CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, displayedProfile),
                )
                if (sourceCase.sourceKind == EqSourceKind.STRUCTURED_CATALOG) {
                    assertThat(originalSelection.compatibilityProductId).isEqualTo(displayedProfile.productId)
                } else {
                    assertThat(originalSelection.compatibilityProductId).isNotEqualTo(displayedProfile.productId)
                }

                val selection = repository.resolveCanonicalSelection(displayedProfile)
                assertThat(selection).isNotNull()
                val exactSelection = requireNotNull(selection)
                assertThat(exactSelection.compatibilityProductId).isEqualTo(sourceCase.productId)
                assertThat(exactSelection.compatibilityVendorId).isEqualTo(sourceCase.vendorId)
                assertThat(CanonicalLegacyCatalogAdapter.matchesSelection(exactSelection, displayedProfile)).isTrue()
                assertThat(exactSelection.profile).isEqualTo(canonicalProfile)
                assertThat(exactSelection.selectedRevision).isEqualTo(canonicalProfile.latestRevision)
                assertThat(exactSelection.selectedRevision.sourceReferences.single().sourceKind)
                    .isEqualTo(sourceCase.sourceKind)
                assertThat(exactSelection.selectedRevision.sourceReferences)
                    .containsExactlyElementsIn(canonicalProfile.latestRevision.sourceReferences)
            }
        } finally {
            filesDir.deleteRecursively()
        }
    }

    @Test
    fun everyIngestedSourceSampleResolvesThroughItsExactFavoriteBoundary() = runBlocking {
        val fixture = loadFavoriteSourceSampleFixture()
        val snapshot = fixture.snapshot
        val sampleSourceIds = fixture.samples.map(FavoriteSourceSample::sourceId)
        val profileSourceIds = snapshot.profiles
            .flatMap(CanonicalEqProfile::revisions)
            .flatMap(EqRevision::sourceReferences)
            .map(EqSourceReference::sourceId)
            .toSet()

        assertThat(fixture.schemaVersion).isEqualTo(1)
        assertThat(sampleSourceIds).containsNoDuplicates()
        assertThat(sampleSourceIds).containsExactlyElementsIn(profileSourceIds)
        val excludedSourceIds = fixture.excludedRegisteredSources.map(FavoriteSourceExclusion::sourceId)
        assertThat(excludedSourceIds).containsNoDuplicates()
        assertThat(fixture.excludedRegisteredSources.all { it.reason.isNotBlank() }).isTrue()
        assertThat(sampleSourceIds.toSet().intersect(excludedSourceIds.toSet())).isEmpty()

        val canonicalProjection = CanonicalLegacyCatalogAdapter.adapt(snapshot)
        val legacyVendorIds = canonicalProjection.vendors
            .sortedBy(OpraVendor::id)
            .mapIndexed { index, vendor -> vendor.id to "favorite-source-legacy-vendor-$index" }
            .toMap()
        val legacyVendors = canonicalProjection.vendors.map { vendor ->
            OpraVendor(
                id = requireNotNull(legacyVendorIds[vendor.id]),
                name = vendor.name,
            )
        }
        val canonicalToDisplayedProductId = canonicalProjection.products
            .sortedBy(OpraProduct::id)
            .mapIndexed { index, product -> product.id to "favorite-source-legacy-product-$index" }
            .toMap()
        val legacyProducts = canonicalProjection.products.map { product ->
            product.copy(
                id = requireNotNull(canonicalToDisplayedProductId[product.id]),
                vendorId = requireNotNull(legacyVendorIds[product.vendorId]),
            )
        }
        val legacyCatalog = canonicalProjection.copy(
            vendors = legacyVendors,
            products = legacyProducts,
            profiles = emptyList(),
            generalPresets = emptyList(),
        )
        val filesDir = Files.createTempDirectory("canonical-favorite-real-source-samples").toFile()
        val canonicalRepository = CanonicalCatalogRepository(
            filesDir = filesDir,
            source = { destination ->
                destination.writeText(Json.encodeToString(snapshot), Charsets.UTF_8)
            },
        )
        val repository = CanonicalFirstCatalogRepository(
            canonicalRepository,
            FakeCatalogRepository(legacyCatalog),
        )

        try {
            repository.initialize()
            val effectiveCatalog = (repository.state.value as CatalogState.Ready).catalog

            fixture.samples.forEach { sample ->
                val canonicalProfile = snapshot.profiles.single {
                    it.canonicalProfileId == sample.canonicalProfileId
                }
                val selectedRevision = canonicalProfile.revisions.single {
                    it.revisionId == sample.revisionId
                }
                val sourceReference = selectedRevision.sourceReferences.single {
                    it.sourceId == sample.sourceId && it.sourceRecordId == sample.sourceRecordId
                }

                assertThat(sample.scope).isEqualTo(canonicalProfile.scope.name.lowercase())
                assertThat(sample.isLatestRevision).isEqualTo(selectedRevision.isLatest)
                assertThat(sample.isPrimaryReference).isEqualTo(sourceReference.isPrimary)
                assertThat(sample.sourceKind).isEqualTo(sourceReference.sourceKind.name.lowercase())

                when (canonicalProfile.scope) {
                    EqProfileScope.HEADPHONE -> {
                        val projectedId = projectedProfileId(canonicalProfile, selectedRevision)
                        val canonicalDisplayedProjection = canonicalProjection.profiles.single {
                            it.canonicalProfileId == canonicalProfile.canonicalProfileId && it.id == projectedId
                        }
                        val displayedProfile = effectiveCatalog.profiles.single {
                            it.canonicalProfileId == canonicalProfile.canonicalProfileId && it.id == projectedId
                        }
                        assertThat(displayedProfile.bandOrderProvenance)
                            .isEqualTo(canonicalDisplayedProjection.bandOrderProvenance)
                        if (sample.sourceId == "opra" && sourceReference.isPrimary) {
                            assertThat(displayedProfile.bandOrderProvenance)
                                .isEqualTo(EqBandOrderProvenance.OPRA_SOURCE_PRIORITY)
                        }
                        val canonicalProductId = canonicalDisplayedProjection.productId
                        val displayedProductId = requireNotNull(canonicalToDisplayedProductId[canonicalProductId])

                        assertThat(effectiveCatalog.canonicalProductId(canonicalProductId))
                            .isEqualTo(displayedProductId)
                        assertThat(displayedProfile.productId).isEqualTo(displayedProductId)
                        assertThat(displayedProfile.productId).isNotEqualTo(canonicalProductId)

                        val beforeRebase = requireNotNull(
                            CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, displayedProfile),
                        )
                        assertThat(beforeRebase.compatibilityProductId).isEqualTo(canonicalProductId)
                        assertThat(beforeRebase.compatibilityProductId).isNotEqualTo(displayedProfile.productId)

                        val selection = requireNotNull(repository.resolveCanonicalSelection(displayedProfile))
                        assertThat(selection.compatibilityProductId).isEqualTo(displayedProfile.productId)
                        assertThat(selection.compatibilityVendorId).isEqualTo(displayedProfile.let {
                            requireNotNull(effectiveCatalog.product(it.productId)).vendorId
                        })
                        assertWithMessage("strict canonical selection match for ${sample.sourceId}")
                            .that(CanonicalLegacyCatalogAdapter.matchesSelection(selection, displayedProfile))
                            .isTrue()
                        assertThat(selection.profile).isEqualTo(canonicalProfile)
                        assertThat(selection.selectedRevisionId).isEqualTo(sample.revisionId)
                        assertThat(selection.selectedRevision).isEqualTo(selectedRevision)
                        assertThat(selection.selectedRevision.acousticFingerprint)
                            .isEqualTo(selectedRevision.acousticFingerprint)
                        assertThat(selection.selectedRevision.filters).containsExactlyElementsIn(selectedRevision.filters)
                        assertThat(selection.selectedRevision.sourceReferences)
                            .containsExactlyElementsIn(selectedRevision.sourceReferences)
                        assertThat(selection.selectedRevision.sourceReferences).contains(sourceReference)

                        val editableBandIndex = requireNotNull(displayedProfile.bands)
                            .indexOfFirst { it.gainDb != null }
                        assertThat(editableBandIndex).isAtLeast(0)
                        val staleProjection = displayedProfile.copy(
                            bands = requireNotNull(displayedProfile.bands).mapIndexed { index, band ->
                                if (index == editableBandIndex) {
                                    band.copy(gainDb = requireNotNull(band.gainDb) + 0.1)
                                } else {
                                    band
                                }
                            },
                        )
                        assertThat(repository.resolveCanonicalSelection(staleProjection)).isNull()

                        val roundTripped = CanonicalEqSelectionCodec().decode(
                            CanonicalEqSelectionCodec().encode(selection),
                        )
                        assertThat(roundTripped).isEqualTo(selection)
                    }

                    EqProfileScope.GENERAL -> {
                        val displayedPreset = effectiveCatalog.generalPresets.single {
                            it.canonicalProfileId == canonicalProfile.canonicalProfileId &&
                                it.id.endsWith("@${sample.revisionId}")
                        }
                        val selection = requireNotNull(repository.resolveCanonicalSelection(displayedPreset))

                        assertThat(CanonicalLegacyCatalogAdapter.isSameGeneralSelection(selection, displayedPreset))
                            .isTrue()
                        assertThat(selection.profile).isEqualTo(canonicalProfile)
                        assertThat(selection.selectedRevisionId).isEqualTo(sample.revisionId)
                        assertThat(selection.selectedRevision).isEqualTo(selectedRevision)
                        assertThat(selection.selectedRevision.sourceReferences)
                            .containsExactlyElementsIn(selectedRevision.sourceReferences)
                        assertThat(selection.selectedRevision.sourceReferences).contains(sourceReference)
                        assertThat(selection.compatibilityProductId).isNull()
                    }
                }
            }
        } finally {
            filesDir.deleteRecursively()
        }
    }

    @Test
    fun ambiguousCanonicalProjectionDoesNotResolve() {
        val original = currentAfulCommunitySnapshot().profiles.first()
        val alternateSource = original.latestRevision.sourceReferences.single().copy(
            sourceRecordId = "alternate-record-with-same-projection",
        )
        val ambiguousProfile = original.copy(
            revisions = listOf(
                original.latestRevision.copy(sourceReferences = listOf(alternateSource)),
            ),
        )
        val ambiguousSnapshot = currentAfulCommunitySnapshot().copy(
            profiles = listOf(original, ambiguousProfile),
        )
        val displayed = CanonicalLegacyCatalogAdapter.adapt(ambiguousSnapshot).profiles.single()

        assertThat(CanonicalLegacyCatalogAdapter.resolveSelection(ambiguousSnapshot, displayed)).isNull()
    }

    private fun currentAfulCommunitySnapshot() = CatalogSnapshot(
        schemaVersion = 1,
        generatedAt = "2026-09-30T00:00:00Z",
        sourceRegistryVersion = "aful-favorite-regression",
        profiles = listOf(
            communityProfile(
                canonicalProfileId = "community-96571b708868cdf52c109d5f",
                creator = "LoboNautics",
                tuningLabel = "Basshead tuning",
                revisionId = "rev-b2b639d1d1dfee12e6b87f97",
                acousticFingerprint = "b2b639d1d1dfee12e6b87f970fbe8e3d0d4e0a86493d1a3bcee1dfbe4783b5e9",
                preampGainDb = -3.7,
                sourceId = "reddit-audio",
                sourceRecordId = "reddit-iems-1pi6g5d-lobonautics-explorer-basshead",
                sourceUrl = "https://www.reddit.com/r/iems/comments/1pi6g5d/aful_explorer_basshead_tuning/",
                sourceVersionLabel = "2025-12-09",
                soundImpactSummary = "Estimated signature: fuller/stronger bass, more forward upper mids, brighter upper treble. Strongest broad lift: presence +2.7 dB; strongest broad reduction: lower mids +0.3 dB. Analysis preserves the source's original 8 filters; no missing bands were invented.",
                filters = listOf(
                    EqFilter(EqFilterType.LOW_SHELF, 20.0, 2.0, 0.3),
                    EqFilter(EqFilterType.LOW_SHELF, 90.0, 3.0, 0.3),
                    EqFilter(EqFilterType.PEAK, 200.0, -1.0, 1.3),
                    EqFilter(EqFilterType.PEAK, 1500.0, -0.6, 1.2),
                    EqFilter(EqFilterType.PEAK, 2700.0, 1.0, 2.0),
                    EqFilter(EqFilterType.PEAK, 3500.0, 3.7, 1.1),
                    EqFilter(EqFilterType.PEAK, 3800.0, -2.0, 1.2),
                    EqFilter(EqFilterType.PEAK, 8000.0, 1.5, 0.3),
                ),
            ),
            communityProfile(
                canonicalProfileId = "community-d11e5db85fc6a1bb1fa7c825",
                creator = "Jaytiss",
                tuningLabel = "Jaytiss community tuning",
                revisionId = "rev-47f31da216daad79be8a19bd",
                acousticFingerprint = "47f31da216daad79be8a19bd0cfc463596c7470ff925010976b30aff7a462318",
                preampGainDb = -3.9,
                sourceId = "hifiguides",
                sourceRecordId = "hifiguides-aful-explorer-jaytiss",
                sourceUrl = "https://forum.hifiguides.com/t/aful-explorer-1dd-2ba-hybrid-in-ear-monitors/42713",
                sourceVersionLabel = null,
                soundImpactSummary = "Estimated signature: more forward upper mids. Strongest broad lift: presence +1.5 dB; strongest broad reduction: bass -0.9 dB. Analysis preserves the source's original 9 filters; no missing bands were invented.",
                filters = listOf(
                    EqFilter(EqFilterType.PEAK, 20.0, 0.8, 0.9),
                    EqFilter(EqFilterType.PEAK, 42.0, 0.9, 1.5),
                    EqFilter(EqFilterType.PEAK, 93.0, -0.7, 2.0),
                    EqFilter(EqFilterType.PEAK, 220.0, -1.4, 1.2),
                    EqFilter(EqFilterType.PEAK, 1300.0, -1.4, 1.9),
                    EqFilter(EqFilterType.PEAK, 2500.0, 3.6, 2.0),
                    EqFilter(EqFilterType.PEAK, 4000.0, 3.3, 2.0),
                    EqFilter(EqFilterType.PEAK, 5100.0, -2.4, 2.0),
                    EqFilter(EqFilterType.PEAK, 15000.0, 2.9, 1.9),
                ),
            ),
        ),
    )

    private fun communityProfile(
        canonicalProfileId: String,
        creator: String,
        tuningLabel: String,
        revisionId: String,
        acousticFingerprint: String,
        preampGainDb: Double,
        sourceId: String,
        sourceRecordId: String,
        sourceUrl: String,
        sourceVersionLabel: String?,
        soundImpactSummary: String,
        filters: List<EqFilter>,
    ) = CanonicalEqProfile(
        canonicalProfileId = canonicalProfileId,
        headphone = HeadphoneIdentity("AFUL", "Explorer"),
        scope = EqProfileScope.HEADPHONE,
        purpose = EqPresetPurpose.CORRECTION_TUNING,
        creator = creator,
        target = EqTarget(null, EqTargetKind.UNKNOWN),
        tuningLabel = tuningLabel,
        revisions = listOf(
            EqRevision(
                revisionId = revisionId,
                acousticFingerprint = acousticFingerprint,
                preampGainDb = preampGainDb,
                filters = filters,
                sourceReferences = listOf(
                    EqSourceReference(
                        sourceId = sourceId,
                        sourceKind = EqSourceKind.COMMUNITY,
                        sourceRecordId = sourceRecordId,
                        sourceVendorId = "AFUL",
                        sourceProductId = "Explorer",
                        url = sourceUrl,
                        creator = creator,
                        provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
                        redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
                        isPrimary = true,
                    ),
                ),
                sourceVersionLabel = sourceVersionLabel,
                soundImpactSummary = soundImpactSummary,
                verificationStatus = VerificationStatus.UNVERIFIED,
                isLatest = true,
            ),
        ),
    )

    private fun loadFavoriteSourceSampleFixture(): FavoriteSourceSampleFixture =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("catalog/favorite-source-samples.json")) {
            "Favorite source sample fixture was not found on the test classpath"
        }.bufferedReader().use { reader ->
            Json { ignoreUnknownKeys = true }.decodeFromString(reader.readText())
        }

    private fun projectedProfileId(profile: CanonicalEqProfile, revision: EqRevision): String {
        val primary = revision.sourceReferences.singleOrNull(EqSourceReference::isPrimary)
        val opra = revision.sourceReferences.firstOrNull { reference ->
            reference.sourceId == "opra" && !reference.sourceRecordId.isNullOrBlank()
        }
        val idSource = primary?.takeIf {
            it.sourceId == "opra" && !it.sourceRecordId.isNullOrBlank()
        } ?: opra
        return if (revision.isLatest && idSource != null) {
            requireNotNull(idSource.sourceRecordId)
        } else {
            "eq-library:${profile.canonicalProfileId}@${revision.revisionId}"
        }
    }

    private class FakeCatalogRepository(catalog: OpraCatalog) : AppCatalogRepository {
        override val state: StateFlow<CatalogState> = MutableStateFlow(
            CatalogState.Ready(catalog = catalog, lastSuccessfulRefreshMillis = 1L),
        )

        override suspend fun initialize() = Unit

        override suspend fun refresh(): CatalogRefreshResult = CatalogRefreshResult.Failure(
            reason = CatalogRefreshFailureReason.Network,
            usingSavedCatalog = true,
        )
    }

    private data class SourceCase(
        val sourceKind: EqSourceKind,
        val sourceId: String,
        val manufacturer: String,
        val model: String,
        val vendorId: String,
        val productId: String,
        val productSubtype: String,
        val provenanceTier: ProvenanceTier,
        val redistributionPolicy: RedistributionPolicy,
    )

    private fun catalogSourceCases() = listOf(
        SourceCase(
            sourceKind = EqSourceKind.STRUCTURED_CATALOG,
            sourceId = "opra",
            manufacturer = "AFUL",
            model = "Explorer",
            vendorId = "aful",
            productId = "aful::explorer",
            productSubtype = "in_ear",
            provenanceTier = ProvenanceTier.AUTHORITATIVE,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
        ),
        SourceCase(
            sourceKind = EqSourceKind.MEASUREMENT_DERIVED,
            sourceId = "autoeq",
            manufacturer = "Sony",
            model = "WH-1000XM5",
            vendorId = "sony",
            productId = "sony::wh-1000xm5",
            productSubtype = "over_ear",
            provenanceTier = ProvenanceTier.MEASUREMENT_DERIVED,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
        ),
        SourceCase(
            sourceKind = EqSourceKind.CREATOR,
            sourceId = "creator-feed",
            manufacturer = "Sennheiser",
            model = "HD 600",
            vendorId = "sennheiser",
            productId = "sennheiser::hd-600",
            productSubtype = "over_ear",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.LINK_ONLY,
        ),
        SourceCase(
            sourceKind = EqSourceKind.COMMUNITY,
            sourceId = "reddit-audio",
            manufacturer = "Sony",
            model = "WH-1000XM5",
            vendorId = "sony",
            productId = "sony::wh-1000xm5",
            productSubtype = "over_ear",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.LINK_ONLY,
        ),
        SourceCase(
            sourceKind = EqSourceKind.REPOSITORY,
            sourceId = "oratory1990",
            manufacturer = "Sony",
            model = "WH-1000XM5",
            vendorId = "sony",
            productId = "sony::wh-1000xm5",
            productSubtype = "over_ear",
            provenanceTier = ProvenanceTier.MIRROR,
            redistributionPolicy = RedistributionPolicy.LINK_ONLY,
        ),
        SourceCase(
            sourceKind = EqSourceKind.DEVICE_COMMUNITY,
            sourceId = "device-community",
            manufacturer = "Sennheiser",
            model = "HD 600",
            vendorId = "sennheiser",
            productId = "sennheiser::hd-600",
            productSubtype = "over_ear",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.LINK_ONLY,
        ),
        SourceCase(
            sourceKind = EqSourceKind.USER_SUBMISSION,
            sourceId = "user-submission",
            manufacturer = "Sennheiser",
            model = "HD 600",
            vendorId = "sennheiser",
            productId = "sennheiser::hd-600",
            productSubtype = "over_ear",
            provenanceTier = ProvenanceTier.NEEDS_REVIEW,
            redistributionPolicy = RedistributionPolicy.UNKNOWN_REVIEW,
        ),
    )

    private companion object {
        const val LEGACY_EXPLORER_PRODUCT_ID = "aful::explorer"
    }
}
