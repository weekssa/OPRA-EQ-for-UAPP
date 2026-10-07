package com.weekssa.opraeqforuapp.data.library

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.data.catalog.AppCatalogRepository
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import com.weekssa.opraeqforuapp.domain.library.CanonicalEqProfile
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
import com.weekssa.opraeqforuapp.domain.library.ProvenanceTier
import com.weekssa.opraeqforuapp.domain.library.RedistributionPolicy
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class CanonicalFirstCatalogRepositoryTest {
    @Test
    fun resolvesEveryExactLegacyOpraProfileInFallbackFixtureAcrossSourceFamilies() = runBlocking {
        val profiles = listOf(
            legacyProfile(
                id = "hifiman:edition_xs::rtings_target_rtings_com_consolidated_parametric_5band",
                productId = "hifiman::edition_xs",
                author = "Rtings/AutoEQ",
                preampGainDb = -5.1,
            ),
            legacyProfile(
                id = "sennheiser:hd600::autoeq_oratory1990",
                productId = "sennheiser::hd600",
                author = "oratory1990",
                preampGainDb = -5.5,
            ),
            legacyProfile(
                id = "audio-technica:m50x::community_listener",
                productId = "audio-technica::m50x",
                author = "Community",
                preampGainDb = -3.2,
            ),
        )
        val legacy = FakeCatalogRepository(
            OpraCatalog(
                vendors = listOf(
                    OpraVendor("hifiman", "HIFIMAN"),
                    OpraVendor("sennheiser", "Sennheiser"),
                    OpraVendor("audio-technica", "Audio-Technica"),
                ),
                products = listOf(
                    OpraProduct(
                        id = "hifiman::edition_xs",
                        vendorId = "hifiman",
                        name = "Edition XS",
                        type = "headphones",
                        subtype = "over_the_ear",
                    ),
                    OpraProduct(
                        id = "sennheiser::hd600",
                        vendorId = "sennheiser",
                        name = "HD 600",
                        type = "headphones",
                        subtype = "over_the_ear",
                    ),
                    OpraProduct(
                        id = "audio-technica::m50x",
                        vendorId = "audio-technica",
                        name = "ATH-M50x",
                        type = "headphones",
                        subtype = "over_the_ear",
                    ),
                ),
                profiles = profiles,
            ),
        )
        val canonical = CanonicalCatalogRepository(
            filesDir = Files.createTempDirectory("canonical-first-family-test").toFile(),
            source = { throw IOException("canonical snapshot unavailable") },
        )
        val repository = CanonicalFirstCatalogRepository(canonical, legacy)

        repository.initialize()

        profiles.forEach { profile ->
            val selection = repository.resolveCanonicalSelection(profile)
            assertThat(selection).isNotNull()
            assertThat(requireNotNull(selection).selectedRevision.sourceReferences.single().sourceRecordId)
                .isEqualTo(profile.id)
        }
        assertThat(repository.resolveCanonicalSelection(profiles.first().copy(preampGainDb = -4.9))).isNull()
    }

    @Test
    fun resolvesExactLegacyProfileWhenCanonicalSnapshotHasNotPublishedTheRow() = runBlocking {
        val profile = OpraEqProfile(
            id = "hifiman:edition_xs::rtings_target_rtings_com_consolidated_parametric_5band",
            productId = "hifiman::edition_xs",
            author = "Rtings/AutoEQ",
            details = "Target_Rtings_com · Consolidated",
            link = null,
            profileType = "parametric_eq",
            preampGainDb = -5.1,
            bands = listOf(
                OpraBand("peak_dip", 82.0, 2.7, 1.41, null),
                OpraBand("peak_dip", 125.0, -1.2, 1.41, null),
                OpraBand("peak_dip", 4000.0, 1.0, 1.41, null),
                OpraBand("peak_dip", 8000.0, 0.5, 1.41, null),
                OpraBand("peak_dip", 12000.0, -2.0, 1.41, null),
            ),
        )
        val legacy = FakeCatalogRepository(
            OpraCatalog(
                vendors = listOf(OpraVendor("hifiman", "HIFIMAN")),
                products = listOf(
                    OpraProduct(
                        id = profile.productId,
                        vendorId = "hifiman",
                        name = "Edition XS",
                        type = "headphones",
                        subtype = "over_the_ear",
                    ),
                ),
                profiles = listOf(profile),
            ),
        )
        val canonical = CanonicalCatalogRepository(
            filesDir = Files.createTempDirectory("canonical-first-test").toFile(),
            source = { throw IOException("canonical snapshot unavailable") },
        )
        val repository = CanonicalFirstCatalogRepository(canonical, legacy)

        repository.initialize()

        val selection = repository.resolveCanonicalSelection(profile)
        assertThat(selection).isNotNull()
        assertThat(requireNotNull(selection).selectedRevision.sourceReferences.single().sourceRecordId)
            .isEqualTo(profile.id)
        assertThat(repository.resolveCanonicalSelection(profile.copy(preampGainDb = -5.0))).isNull()
    }

    @Test
    fun mergedCatalogReleasesLegacySourceAndResolvesItsVisibleRows() = runBlocking {
        val profile = legacyProfile(
            id = "legacy-visible-profile",
            productId = "hifiman::edition_xs",
            author = "Rtings/AutoEQ",
            preampGainDb = -5.1,
        )
        val legacy = FakeCatalogRepository(
            OpraCatalog(
                vendors = listOf(OpraVendor("hifiman", "HIFIMAN")),
                products = listOf(
                    OpraProduct("hifiman::edition_xs", "hifiman", "Edition XS", "headphones", "over_the_ear"),
                ),
                profiles = listOf(profile),
            ),
        )
        val canonical = CanonicalCatalogRepository(
            filesDir = Files.createTempDirectory("canonical-first-release-test").toFile(),
            source = { destination -> destination.writeText(Json.encodeToString(unrelatedGeneralSnapshot())) },
            nowMillis = { 1234L },
        )
        val repository = CanonicalFirstCatalogRepository(canonical, legacy)

        repository.initialize()

        assertThat(legacy.releaseCount).isEqualTo(1)
        assertThat(legacy.state.value).isEqualTo(CatalogState.Loading)
        val visibleProfile = (repository.state.value as CatalogState.Ready).catalog.profiles
            .single { it.id == profile.id }
        assertThat(repository.resolveCanonicalSelection(visibleProfile)).isNotNull()

        val refresh = repository.refresh()
        assertThat(refresh).isInstanceOf(CatalogRefreshResult.Failure::class.java)
        assertThat((refresh as CatalogRefreshResult.Failure).usingSavedCatalog).isTrue()
        assertThat((repository.state.value as CatalogState.Ready).catalog.profiles.map(OpraEqProfile::id))
            .contains("legacy-visible-profile")
    }

    private fun legacyProfile(
        id: String,
        productId: String,
        author: String,
        preampGainDb: Double,
    ) = OpraEqProfile(
        id = id,
        productId = productId,
        author = author,
        details = "Consolidated",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = preampGainDb,
        bands = listOf(
            OpraBand("peak_dip", 100.0, 1.0, 1.0, null),
            OpraBand("peak_dip", 1000.0, -1.0, 1.0, null),
            OpraBand("peak_dip", 8000.0, 0.5, 1.0, null),
        ),
    )

    private fun unrelatedGeneralSnapshot() = CatalogSnapshot(
        schemaVersion = 1,
        generatedAt = "2026-10-04T00:00:00Z",
        sourceRegistryVersion = "test",
        profiles = listOf(
            CanonicalEqProfile(
                canonicalProfileId = "general:unrelated",
                scope = EqProfileScope.GENERAL,
                purpose = EqPresetPurpose.EFFECT,
                creator = "Tester",
                target = EqTarget(null, EqTargetKind.UNKNOWN),
                tuningLabel = "Bass boost",
                revisions = listOf(
                    EqRevision(
                        revisionId = "general-rev",
                        acousticFingerprint = "general-fingerprint",
                        preampGainDb = -3.0,
                        filters = listOf(EqFilter(EqFilterType.LOW_SHELF, 100.0, 3.0, 0.7)),
                        sourceReferences = listOf(
                            EqSourceReference(
                                sourceId = "test",
                                sourceKind = EqSourceKind.STRUCTURED_CATALOG,
                                sourceRecordId = "general-record",
                                url = "https://example.com/eq",
                                creator = "Tester",
                                provenanceTier = ProvenanceTier.AUTHORITATIVE,
                                redistributionPolicy = RedistributionPolicy.ALLOWED,
                                isPrimary = true,
                            ),
                        ),
                        isLatest = true,
                    ),
                ),
            ),
        ),
    )

    private class FakeCatalogRepository(catalog: OpraCatalog) : AppCatalogRepository {
        private val mutableState = MutableStateFlow<CatalogState>(
            CatalogState.Ready(catalog = catalog, lastSuccessfulRefreshMillis = 1L),
        )
        override val state: StateFlow<CatalogState> = mutableState
        var releaseCount: Int = 0
            private set

        override suspend fun initialize() = Unit

        override fun releaseInMemoryCatalog() {
            releaseCount += 1
            mutableState.value = CatalogState.Loading
        }

        override suspend fun refresh(): CatalogRefreshResult = CatalogRefreshResult.Failure(
            reason = CatalogRefreshFailureReason.Network,
            usingSavedCatalog = true,
        )
    }
}
