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

    @Test
    fun diffuseFieldReferenceGetsHumanReadableTargetName() {
        val legacy = OpraCatalog(
            vendors = listOf(vendor("hifiman", "HIFIMAN")),
            products = listOf(product("edition-xs", "hifiman", "Edition XS")),
            profiles = listOf(
                profileWithBand(
                    id = "diffuse-field",
                    productId = "edition-xs",
                    details = "HRTF_5128_Diffuse_Field · Consolidated",
                    type = "peak_dip",
                    frequency = 80.0,
                ),
            ),
        )

        val merged = overlayCanonicalCatalog(
            legacy,
            OpraCatalog(vendors = emptyList(), products = emptyList(), profiles = emptyList()),
        )

        assertThat(merged.profiles.single().details)
            .isEqualTo("Target: B&K 5128 Diffuse Field (reference) · Source: OPRA · Slightly adds bass.")
    }

    @Test
    fun qualifiedModelAliasesConsolidateDuplicateHeadphoneRowsAndPreserveOldIds() {
        val legacy = OpraCatalog(
            vendors = listOf(vendor("7hz", "7Hz")),
            products = listOf(
                product("salnotes-zero", "7hz", "Salnotes Zero"),
                product("salnotes-zero-2", "7hz", "Salnotes Zero 2"),
                product("crinacle-zero-2", "7hz", "x Crinacle Zero 2"),
                product("zero-2-space", "7hz", "Zero 2"),
                product("zero-2-colon", "7hz", "Zero:2"),
            ),
            profiles = listOf(
                profileWithBand("original-zero", "salnotes-zero", "Original Zero", "peak_dip", 70.0),
                profileWithBand("salnotes-two", "salnotes-zero-2", "Zero 2 A", "peak_dip", 80.0),
                profileWithBand("crinacle-two", "crinacle-zero-2", "Zero 2 B", "peak_dip", 90.0),
                profileWithBand("space-two", "zero-2-space", "Zero 2 C", "peak_dip", 100.0),
                profileWithBand("colon-two", "zero-2-colon", "Zero 2 D", "peak_dip", 110.0),
            ),
        )
        val canonical = OpraCatalog(
            vendors = listOf(vendor("eq-library-vendor:7hz", "7Hz")),
            products = listOf(
                product(
                    id = "eq-library-product:7hz-zero2",
                    vendorId = "eq-library-vendor:7hz",
                    name = "Zero:2",
                    aliases = listOf("Zero 2", "Salnotes Zero 2", "x Crinacle Zero 2"),
                ),
            ),
            profiles = listOf(
                profileWithBand(
                    "eq-library:mrchillstorm-zero2@latest",
                    "eq-library-product:7hz-zero2",
                    "Latest · Target: ISO 226 · Source: MrChillStorm",
                    "peak_dip",
                    120.0,
                ),
            ),
        )

        val merged = overlayCanonicalCatalog(legacy, canonical)

        assertThat(merged.products.map(OpraProduct::name)).containsExactly("Salnotes Zero", "Zero:2")
        assertThat(merged.searchProducts("7hz zer").map { it.product.name })
            .containsExactly("Salnotes Zero", "Zero:2")
        assertThat(merged.product("crinacle-zero-2")?.name).isEqualTo("Zero:2")
        assertThat(merged.product("salnotes-zero-2")?.name).isEqualTo("Zero:2")
        assertThat(merged.profilesForProduct("crinacle-zero-2").map(OpraEqProfile::id)).containsExactly(
            "salnotes-two",
            "crinacle-two",
            "space-two",
            "colon-two",
            "eq-library:mrchillstorm-zero2@latest",
        )
        assertThat(merged.profilesForProduct("salnotes-zero").map(OpraEqProfile::id))
            .containsExactly("original-zero")
    }

    private fun vendor(id: String, name: String) = OpraVendor(id, name)

    private fun product(
        id: String,
        vendorId: String,
        name: String,
        aliases: List<String> = emptyList(),
    ) = OpraProduct(id, vendorId, name, "headphones", "", aliases)

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
