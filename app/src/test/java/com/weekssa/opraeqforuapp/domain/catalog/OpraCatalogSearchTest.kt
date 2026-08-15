package com.weekssa.opraeqforuapp.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpraCatalogSearchTest {
    private val catalog = OpraCatalog(
        vendors = listOf(
            OpraVendor("sennheiser", "Sennheiser"),
            OpraVendor("beyerdynamic", "Beyerdynamic"),
        ),
        products = listOf(
            OpraProduct("hd600", "sennheiser", "HD 600", "headphones", "over_the_ear"),
            OpraProduct("dt1990", "beyerdynamic", "DT 1990 Pro", "headphones", "over_the_ear"),
        ),
        profiles = listOf(
            profile("eq1", "hd600"),
            profile("eq2", "hd600"),
            profile("eq3", "dt1990"),
        ),
    )

    @Test
    fun searchIgnoresOrdinarySpacingAndPunctuation() {
        assertEquals("HD 600", catalog.searchProducts("HD600").single().product.name)
        assertEquals("DT 1990 Pro", catalog.searchProducts("DT-1990").single().product.name)
    }

    @Test
    fun searchCanMatchManufacturerAndModelTokensTogether() {
        val result = catalog.searchProducts("senn hd600").single()

        assertEquals("Sennheiser", result.vendor.name)
        assertEquals(2, result.profileCount)
    }

    @Test
    fun searchDoesNotUseEqAuthorOrDetails() {
        assertTrue(catalog.searchProducts("oratory1990").isEmpty())
    }

    private fun profile(id: String, productId: String) = OpraEqProfile(
        id = id,
        productId = productId,
        author = "oratory1990",
        details = "Target",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = emptyList(),
    )
}
