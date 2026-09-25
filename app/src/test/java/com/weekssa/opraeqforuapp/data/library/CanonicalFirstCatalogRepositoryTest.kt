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
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
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
}
