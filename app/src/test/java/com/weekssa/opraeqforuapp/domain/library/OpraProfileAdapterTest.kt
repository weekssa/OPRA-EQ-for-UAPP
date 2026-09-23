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
    fun `OPRA schema default for omitted band gain is preserved as zero`() {
        val adapted = OpraProfileAdapter.adapt(
            vendor = vendor,
            product = product,
            profile = profile(listOf(OpraBand("peak_dip", 1_000.0, null, 1.0, null))),
        )!!

        assertEquals(0.0, adapted.latestRevision.filters.single().gainDb!!, 0.0)
    }

    @Test
    fun `missing required OPRA profile gain rejects the profile`() {
        assertNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = profile(
                    bands = listOf(OpraBand("peak_dip", 1_000.0, 1.0, 1.0, null)),
                    preampGainDb = null,
                ),
            ),
        )
    }

    @Test
    fun `missing author or non-parametric profile type rejects the profile`() {
        assertNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = profile(
                    bands = listOf(OpraBand("peak_dip", 1_000.0, 1.0, 1.0, null)),
                    author = null,
                ),
            ),
        )
        assertNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = profile(
                    bands = listOf(OpraBand("peak_dip", 1_000.0, 1.0, 1.0, null)),
                    profileType = "graphic_eq",
                ),
            ),
        )
    }

    @Test
    fun `non-finite OPRA profile gain rejects the profile`() {
        assertNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = profile(
                    bands = listOf(OpraBand("peak_dip", 1_000.0, 1.0, 1.0, null)),
                    preampGainDb = Double.POSITIVE_INFINITY,
                ),
            ),
        )
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
    fun `non-schema filter aliases reject the whole OPRA profile`() {
        assertNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = profile(listOf(OpraBand("PK", 1_000.0, 1.0, 1.0, null))),
            ),
        )
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

    @Test
    fun `pass filters accept only slopes defined by the OPRA schema`() {
        assertNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = profile(listOf(OpraBand("low_pass", 12_000.0, null, null, 0.0))),
            ),
        )
        assertNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = profile(listOf(OpraBand("low_pass", 12_000.0, null, null, 13.0))),
            ),
        )
        val accepted = OpraProfileAdapter.adapt(
            vendor = vendor,
            product = product,
            profile = profile(listOf(OpraBand("low_pass", 12_000.0, null, null, 12.0))),
        )!!
        assertEquals(12.0, accepted.latestRevision.filters.single().slope!!, 0.0)
    }

    @Test
    fun `OPRA q minimum is inclusive and provided slopes are schema validated`() {
        val atMinimum = OpraProfileAdapter.adapt(
            vendor = vendor,
            product = product,
            profile = profile(listOf(OpraBand("peak_dip", 1_000.0, 1.0, 0.1, null))),
        )
        assertEquals(0.1, atMinimum!!.latestRevision.filters.single().q!!, 0.0)

        assertNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = profile(listOf(OpraBand("peak_dip", 1_000.0, 1.0, 0.099, null))),
            ),
        )
        assertNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = profile(listOf(OpraBand("peak_dip", 1_000.0, 1.0, 1.0, 13.0))),
            ),
        )
    }

    private fun profile(
        bands: List<OpraBand>,
        preampGainDb: Double? = -3.0,
        author: String? = "Creator",
        profileType: String? = "parametric_eq",
    ) = OpraEqProfile(
        id = "profile",
        productId = product.id,
        author = author,
        details = "Harman",
        link = "https://example.invalid/profile",
        profileType = profileType,
        preampGainDb = preampGainDb,
        bands = bands,
    )
}
