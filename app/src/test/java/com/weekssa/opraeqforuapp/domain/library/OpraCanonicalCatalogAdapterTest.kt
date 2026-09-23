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

        val result = OpraCanonicalCatalogAdapter.adapt(
            catalog = catalog,
            generatedAt = "2026-08-29T12:00:00Z",
            sourceRegistryVersion = "test",
            discoveredAtEpochSeconds = 1_777_000_000,
        )
        val snapshot = result.snapshot

        assertThat(result.quarantinedProfiles).isEmpty()
        assertThat(snapshot.profiles).hasSize(1)
        assertThat(requireNotNull(snapshot.profiles.single().headphone).model).isEqualTo("Edition XS")
        assertThat(snapshot.sources.single().sourceId).isEqualTo("opra")
        assertThat(snapshot.sources.single().lifecycle).isEqualTo(SourceLifecycle.ACTIVE)
    }

    @Test
    fun malformedSourceBandIsExplicitlyQuarantined() {
        val catalog = OpraCatalog(
            vendors = listOf(OpraVendor("v1", "HIFIMAN")),
            products = listOf(OpraProduct("p1", "v1", "Edition XS", "headphone", "over-ear")),
            profiles = listOf(
                OpraEqProfile(
                    id = "partial-eq",
                    productId = "p1",
                    author = "Author",
                    details = null,
                    link = null,
                    profileType = "parametric_eq",
                    preampGainDb = 0.0,
                    bands = listOf(
                        OpraBand("peak_dip", 1_000.0, 0.0, 1.0, null),
                        OpraBand(null, 2_000.0, 0.0, 1.0, null),
                    ),
                ),
            ),
        )

        val result = OpraCanonicalCatalogAdapter.adapt(catalog, "2026-09-23T00:00:00Z", "test")
        assertThat(result.snapshot.profiles).isEmpty()
        assertThat(result.quarantinedProfiles.map { it.profileId }).containsExactly("partial-eq")
    }

    @Test
    fun profileWithNoMatchingProductIsIncludedInQuarantineDiagnostics() {
        val catalog = OpraCatalog(
            vendors = listOf(OpraVendor("v1", "HIFIMAN")),
            products = listOf(OpraProduct("p1", "v1", "Edition XS", "headphone", "over-ear")),
            profiles = listOf(profile("orphan-eq", "Author").copy(productId = "missing-product")),
        )

        val result = OpraCanonicalCatalogAdapter.adapt(catalog, "2026-09-23T00:00:00Z", "test")

        assertThat(result.snapshot.profiles).isEmpty()
        assertThat(result.quarantinedProfiles.map { it.profileId }).containsExactly("orphan-eq")
        assertThat(result.quarantinedProfiles.single().reason).contains("no matching product")
    }

    @Test
    fun `diagnostic adaptation quarantines only the malformed profile and retains valid siblings`() {
        val catalog = OpraCatalog(
            vendors = listOf(OpraVendor("v1", "HIFIMAN")),
            products = listOf(OpraProduct("p1", "v1", "Edition XS", "headphone", "over-ear")),
            profiles = listOf(
                profile("valid-eq", "Author"),
                OpraEqProfile(
                    id = "bad-eq",
                    productId = "p1",
                    author = "Author",
                    details = null,
                    link = null,
                    profileType = "parametric_eq",
                    preampGainDb = Double.NaN,
                    bands = listOf(OpraBand("peak_dip", 1_000.0, 0.0, 1.0, null)),
                ),
            ),
        )

        val result = OpraCanonicalCatalogAdapter.adapt(
            catalog = catalog,
            generatedAt = "2026-09-23T00:00:00Z",
            sourceRegistryVersion = "test",
        )

        assertThat(result.snapshot.profiles).hasSize(1)
        assertThat(result.snapshot.profiles.single().latestRevision.sourceReferences.single().sourceRecordId)
            .isEqualTo("valid-eq")
        assertThat(result.quarantinedProfiles).containsExactly(
            OpraCanonicalCatalogAdapter.QuarantinedProfile(
                profileId = "bad-eq",
                productId = "p1",
                reason = "The OPRA profile is incomplete or contains invalid numeric fields.",
            ),
        )
    }

    @Test
    fun `dedupe preserves source-priority order and gives reordered variants unique identities`() {
        val bands = listOf(
            OpraBand("peak_dip", 100.0, 2.0, 1.0, null),
            OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null),
        )
        val catalog = OpraCatalog(
            vendors = listOf(OpraVendor("v1", "HIFIMAN")),
            products = listOf(OpraProduct("p1", "v1", "Edition XS", "headphone", "over-ear")),
            profiles = listOf(
                profile("first", "Same author").copy(bands = bands),
                profile("same-order-duplicate", "Same author").copy(bands = bands),
                profile("reordered", "Same author").copy(bands = bands.reversed()),
            ),
        )

        val result = OpraCanonicalCatalogAdapter.adapt(
            catalog = catalog,
            generatedAt = "2026-09-23T00:00:00Z",
            sourceRegistryVersion = "test",
        )

        assertThat(result.snapshot.profiles).hasSize(2)
        assertThat(result.snapshot.profiles.map { it.canonicalProfileId }.distinct()).hasSize(2)
        assertThat(result.quarantinedProfiles).isEmpty()
        val ordered = result.snapshot.profiles.first { it.latestRevision.filters.first().frequencyHz == 100.0 }
        val reversed = result.snapshot.profiles.first { it.latestRevision.filters.first().frequencyHz == 1_000.0 }
        assertThat(ordered.latestRevision.filters.map(EqFilter::frequencyHz)).containsExactly(100.0, 1_000.0).inOrder()
        assertThat(reversed.latestRevision.filters.map(EqFilter::frequencyHz)).containsExactly(1_000.0, 100.0).inOrder()
    }

    private fun profile(id: String, author: String) = OpraEqProfile(
        id = id,
        productId = "p1",
        author = author,
        details = "Harman",
        link = "https://example.com/$id",
        profileType = "parametric_eq",
        preampGainDb = -5.0,
        bands = listOf(OpraBand("PK", 100.0, 2.0, 1.0, null)),
    )
}
