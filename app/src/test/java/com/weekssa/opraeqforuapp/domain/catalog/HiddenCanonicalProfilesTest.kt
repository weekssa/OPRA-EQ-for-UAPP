package com.weekssa.opraeqforuapp.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenCanonicalProfilesTest {
    @Test
    fun `hiding canonical lineage removes all revisions from browse but not unrelated EQs`() {
        val catalog = OpraCatalog(
            vendors = listOf(
                OpraVendor("v1", "Maker One"),
                OpraVendor("v2", "Maker Two"),
            ),
            products = listOf(
                OpraProduct("p1", "v1", "Model One", "headphones", ""),
                OpraProduct("p2", "v2", "Model Two", "headphones", ""),
            ),
            profiles = listOf(
                profile("latest-1", "p1", "canonical-one"),
                profile("history-1", "p1", "canonical-one"),
                profile("latest-2", "p2", "canonical-two"),
            ),
            generalPresets = listOf(
                GeneralEqPreset(
                    id = "g1@r1",
                    displayName = "Bass",
                    category = GeneralEqCategory.SOUND,
                    creator = "Creator",
                    soundImpactSummary = null,
                    sourceUrl = null,
                    preampGainDb = null,
                    bands = listOf(OpraBand("peak_dip", 100.0, 1.0, 1.0, null)),
                    canonicalProfileId = "general-one",
                ),
            ),
        )

        val visible = catalog.excludingHiddenCanonicalProfiles(setOf("canonical-one", "general-one"))

        assertEquals(listOf("latest-2"), visible.profiles.map(OpraEqProfile::id))
        assertEquals(listOf("p2"), visible.products.map(OpraProduct::id))
        assertEquals(listOf("v2"), visible.vendors.map(OpraVendor::id))
        assertTrue(visible.generalPresets.isEmpty())
        assertEquals(3, catalog.profiles.size) // original archive projection is untouched
    }

    @Test
    fun `future revision with same canonical identity remains hidden`() {
        val catalog = OpraCatalog(
            vendors = listOf(OpraVendor("v", "Maker")),
            products = listOf(OpraProduct("p", "v", "Model", "headphones", "")),
            profiles = listOf(
                profile("revision-a", "p", "lineage"),
                profile("revision-b", "p", "lineage"),
            ),
        )
        assertTrue(catalog.excludingHiddenCanonicalProfiles(setOf("lineage")).profiles.isEmpty())
    }

    private fun profile(id: String, productId: String, canonicalId: String) = OpraEqProfile(
        id = id,
        productId = productId,
        author = "Creator",
        details = "Latest",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = listOf(OpraBand("peak_dip", 1000.0, -1.0, 1.0, null)),
        canonicalProfileId = canonicalId,
    )
}
