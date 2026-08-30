package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import org.junit.Test

class OpraCanonicalCatalogAdapterTest {
    @Test
    fun adaptsOpraIntoCanonicalSnapshotAndDeduplicatesExactAcousticMatches() {
        val catalog = OpraCatalog(
            vendors = listOf(OpraVendor("v1", "HIFIMAN")),
            products = listOf(OpraProduct("p1", "v1", "Edition XS", "headphone", "over-ear")),
            profiles = listOf(
                profile("eq-1", "Author A"),
                profile("eq-2", "Author B"),
            ),
        )

        val snapshot = OpraCanonicalCatalogAdapter.adapt(
            catalog = catalog,
            generatedAt = "2026-08-29T12:00:00Z",
            sourceRegistryVersion = "test",
            discoveredAtEpochSeconds = 1_777_000_000,
        )

        assertThat(snapshot.profiles).hasSize(1)
        assertThat(requireNotNull(snapshot.profiles.single().headphone).model).isEqualTo("Edition XS")
        assertThat(snapshot.sources.single().sourceId).isEqualTo("opra")
        assertThat(snapshot.sources.single().lifecycle).isEqualTo(SourceLifecycle.ACTIVE)
    }

    private fun profile(id: String, author: String) = OpraEqProfile(
        id = id,
        productId = "p1",
        author = author,
        details = "Harman",
        link = "https://example.com/$id",
        profileType = "parametric",
        preampGainDb = -5.0,
        bands = listOf(OpraBand("PK", 100.0, 2.0, 1.0, null)),
    )
}
