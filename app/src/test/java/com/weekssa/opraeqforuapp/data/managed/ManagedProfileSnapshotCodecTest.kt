package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ManagedProfileSnapshotCodecTest {
    private val codec = ManagedProfileSnapshotCodec()

    @Test
    fun snapshotRoundTripPreservesUnicodeAndAcousticData() {
        val profile = sampleProfile(details = "測定 • édition")

        assertEquals(profile, codec.decode(codec.encode(profile)))
    }

    @Test
    fun fingerprintIsDeterministicAndChangesWithProfileContent() {
        val original = sampleProfile(details = "Original")
        val changed = original.copy(bands = original.bands!!.mapIndexed { index, band ->
            if (index == 0) band.copy(gainDb = band.gainDb!! + 0.5) else band
        })

        assertEquals(codec.fingerprint(original), codec.fingerprint(original.copy()))
        assertNotEquals(codec.fingerprint(original), codec.fingerprint(changed))
    }

    private fun sampleProfile(details: String) = OpraEqProfile(
        id = "profile-1",
        productId = "product-1",
        author = "Créateur",
        details = details,
        link = "https://example.invalid/source",
        profileType = "parametric_eq",
        preampGainDb = -3.5,
        bands = listOf(
            OpraBand("low_shelf", 80.0, 2.0, 0.7, null),
            OpraBand("peak_dip", 1_200.0, -1.25, 1.1, null),
        ),
    )
}
