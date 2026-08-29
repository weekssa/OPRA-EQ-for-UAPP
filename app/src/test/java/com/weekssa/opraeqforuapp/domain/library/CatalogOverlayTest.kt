package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import org.junit.Test

class CatalogOverlayTest {
    @Test
    fun canonicalOverlayPreservesUnrepresentedLegacyProductsAndProfiles() {
        val legacy = OpraCatalog(
            vendors = listOf(vendor("hifiman", "HIFIMAN"), vendor("sennheiser", "Sennheiser")),
            products = listOf(
                product("edition-xs", "hifiman", "Edition XS"),
                product("hd650", "sennheiser", "HD 650"),
            ),
            profiles = listOf(
                profile("opra-edition", "edition-xs", "Legacy Edition XS"),
                profile("opra-hd650", "hd650", "Legacy HD 650"),
            ),
            ignoredEntryCount = 3,
        )
        val canonical = OpraCatalog(
            vendors = listOf(vendor("hifiman", "HIFIMAN")),
            products = listOf(product("edition-xs", "hifiman", "Edition XS")),
            profiles = listOf(
                profile("opra-edition", "edition-xs", "Canonical latest"),
                profile("eq-library:autoeq-edition@latest", "edition-xs", "AutoEq"),
                profile("eq-library:autoeq-edition@old", "edition-xs", "AutoEq previous"),
            ),
            ignoredEntryCount = 1,
        )

        val merged = overlayCanonicalCatalog(legacy, canonical)

        assertThat(merged.products.map { it.id }).containsExactly("edition-xs", "hd650")
        assertThat(merged.profiles.map { it.id }).containsExactly(
            "opra-edition",
            "opra-hd650",
            "eq-library:autoeq-edition@latest",
            "eq-library:autoeq-edition@old",
        )
        assertThat(merged.profiles.single { it.id == "opra-edition" }.details).isEqualTo("Canonical latest")
        assertThat(merged.profiles.single { it.id == "opra-hd650" }.details)
            .isEqualTo("Legacy HD 650 · Source: OPRA")
        assertThat(merged.ignoredEntryCount).isEqualTo(4)
    }

    @Test
    fun canonicalOnlyHeadphoneIsAddedWithoutRemovingLegacyCatalog() {
        val legacy = OpraCatalog(
            vendors = listOf(vendor("legacy-vendor", "Legacy")),
            products = listOf(product("legacy-product", "legacy-vendor", "Legacy Model")),
            profiles = listOf(profile("legacy-profile", "legacy-product", "Legacy")),
        )
        val canonical = OpraCatalog(
            vendors = listOf(vendor("eq-library-vendor:new", "New Maker")),
            products = listOf(product("eq-library-product:new", "eq-library-vendor:new", "New Model")),
            profiles = listOf(profile("eq-library:new@rev", "eq-library-product:new", "Community")),
        )

        val merged = overlayCanonicalCatalog(legacy, canonical)

        assertThat(merged.vendors).hasSize(2)
        assertThat(merged.products).hasSize(2)
        assertThat(merged.profiles).hasSize(2)
    }

    @Test
    fun exactLegacyTuningDuplicatesCollapseAndMachineLabelsBecomeReadable() {
        val legacy = OpraCatalog(
            vendors = listOf(vendor("hifiman", "HIFIMAN")),
            products = listOf(product("edition-xs", "hifiman", "Edition XS")),
            profiles = listOf(
                profileWithBand(
                    id = "rtings-a",
                    productId = "edition-xs",
                    details = "Target_Rtings_com · Consolidated",
                    type = "PK",
                    frequency = 100.0,
                ),
                profileWithBand(
                    id = "rtings-b",
                    productId = "edition-xs",
                    details = "Target_Rtings_com · Consolidated",
                    type = "peak_dip",
                    frequency = 100.0,
                ),
                profileWithBand(
                    id = "senselab",
                    productId = "edition-xs",
                    details = "Target_SenseLab_Aizu · Consolidated",
                    type = "peak_dip",
                    frequency = 110.0,
                ),
            ),
        )
        val canonical = OpraCatalog(vendors = emptyList(), products = emptyList(), profiles = emptyList())

        val merged = overlayCanonicalCatalog(legacy, canonical)

        assertThat(merged.profiles.map { it.id }).containsExactly("rtings-a", "senselab")
        assertThat(merged.profiles.single { it.id == "rtings-a" }.details)
            .isEqualTo("Target: RTINGS.com · Source: OPRA · Slightly adds bass.")
        assertThat(merged.profiles.single { it.id == "senselab" }.details)
            .isEqualTo("Target: SenseLab Aizu · Source: OPRA · Slightly adds bass.")
    }

    private fun vendor(id: String, name: String) = OpraVendor(id, name)

    private fun product(id: String, vendorId: String, name: String) =
        OpraProduct(id, vendorId, name, "headphones", "")

    private fun profile(id: String, productId: String, details: String) = OpraEqProfile(
        id = id,
        productId = productId,
        author = "Tester",
        details = details,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = emptyList(),
    )

    private fun profileWithBand(
        id: String,
        productId: String,
        details: String,
        type: String,
        frequency: Double,
    ) = OpraEqProfile(
        id = id,
        productId = productId,
        author = "Rtings/AutoEQ",
        details = details,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = -1.0,
        bands = listOf(OpraBand(type, frequency, 2.0, 1.0, null)),
    )
}
