package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import org.junit.Test

class CatalogIdentityCleanupTest {
    @Test
    fun exactNormalizedLegacyNamesCollapseWithoutCanonicalProfile() {
        val legacy = OpraCatalog(
            vendors = listOf(OpraVendor("sennheiser", "Sennheiser")),
            products = listOf(
                product("hd650-space", "HD 650"),
                product("hd650-compact", "HD650"),
            ),
            profiles = listOf(
                profile("a", "hd650-space", 100.0),
                profile("b", "hd650-compact", 200.0),
            ),
        )

        val merged = overlayCanonicalCatalog(legacy, emptyCatalog())

        assertThat(merged.products).hasSize(1)
        assertThat(merged.product("hd650-space")?.id).isEqualTo(merged.product("hd650-compact")?.id)
        assertThat(merged.profilesForProduct("hd650-compact").map(OpraEqProfile::id))
            .containsExactly("a", "b")
    }

    @Test
    fun catalogAliasGroupConsolidatesOpraOnlyProducts() {
        val legacy = OpraCatalog(
            vendors = listOf(OpraVendor("maker", "Example Audio")),
            products = listOf(
                product("model-standard", "Model 2"),
                product("model-collab", "x Creator Model 2"),
                product("different", "Model 3"),
            ),
            profiles = listOf(
                profile("standard", "model-standard", 100.0),
                profile("collab", "model-collab", 200.0),
                profile("different-profile", "different", 300.0),
            ),
        )

        val merged = overlayCanonicalCatalog(
            legacy = legacy,
            canonical = emptyCatalog(),
            headphoneAliases = listOf(
                HeadphoneAliasGroup(
                    manufacturer = "Example Audio",
                    canonicalModel = "Model 2",
                    aliases = listOf("x Creator Model 2"),
                    evidence = listOf("qualified-source:test"),
                ),
            ),
        )

        assertThat(merged.products.map(OpraProduct::name)).containsExactly("Model 2", "Model 3")
        assertThat(merged.product("model-collab")?.name).isEqualTo("Model 2")
        assertThat(merged.profilesForProduct("model-collab").map(OpraEqProfile::id))
            .containsExactly("standard", "collab")
        assertThat(merged.profilesForProduct("different").map(OpraEqProfile::id))
            .containsExactly("different-profile")
    }

    private fun product(id: String, name: String) =
        OpraProduct(id, "sennheiser", name, "headphones", "")

    private fun profile(id: String, productId: String, frequency: Double) = OpraEqProfile(
        id = id,
        productId = productId,
        author = "Test",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = listOf(OpraBand("peak_dip", frequency, 1.0, 1.0, null)),
    )

    private fun emptyCatalog() = OpraCatalog(
        vendors = emptyList(),
        products = emptyList(),
        profiles = emptyList(),
    )
}
