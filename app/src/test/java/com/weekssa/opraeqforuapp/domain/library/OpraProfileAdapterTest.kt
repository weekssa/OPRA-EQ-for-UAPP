package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpraProfileAdapterTest {
    private val vendor = OpraVendor(id = "vendor", name = "Example")
    private val product = OpraProduct(
        id = "product",
        vendorId = vendor.id,
        name = "Headphone",
        type = "headphones",
        subtype = "over-ear",
    )

    @Test
    fun `canonical OPRA peak_dip spelling is recognized and source order is preserved`() {
        val adapted = OpraProfileAdapter.adapt(
            vendor = vendor,
            product = product,
            profile = profile(
                listOf(
                    OpraBand("peak_dip", 2_000.0, -2.0, 2.0, null),
                    OpraBand("low_shelf", 100.0, 3.0, 0.7, null),
                    OpraBand("high_shelf", 8_000.0, -1.0, 0.8, null),
                ),
            ),
        )!!

        val filters = adapted.latestRevision.filters
        assertEquals(listOf(2_000.0, 100.0, 8_000.0), filters.map(EqFilter::frequencyHz))
        assertEquals(
            listOf(EqFilterType.PEAK, EqFilterType.LOW_SHELF, EqFilterType.HIGH_SHELF),
            filters.map(EqFilter::type),
        )
    }

    @Test
    fun `missing required field rejects the whole OPRA profile`() {
        val adapted = OpraProfileAdapter.adapt(
            vendor = vendor,
            product = product,
            profile = profile(
                listOf(
                    OpraBand("peak_dip", 100.0, 2.0, 1.0, null),
                    OpraBand("peak_dip", 1_000.0, -2.0, null, null),
                ),
            ),
        )

        assertNull(adapted)
    }

    @Test
    fun `unsupported active OPRA band rejects the whole profile`() {
        val adapted = OpraProfileAdapter.adapt(
            vendor = vendor,
            product = product,
            profile = profile(
                listOf(
                    OpraBand("peak_dip", 100.0, 2.0, 1.0, null),
                    OpraBand("band_stop", 1_000.0, null, 1.0, null),
                ),
            ),
        )

        assertNull(adapted)
    }

    @Test
    fun `pass filters require OPRA slope`() {
        assertNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = profile(listOf(OpraBand("low_pass", 12_000.0, null, null, null))),
            ),
        )
    }

    private fun profile(bands: List<OpraBand>) = OpraEqProfile(
        id = "profile",
        productId = product.id,
        author = "Creator",
        details = "Harman",
        link = "https://example.invalid/profile",
        profileType = "parametric_eq",
        preampGainDb = -3.0,
        bands = bands,
    )
}
