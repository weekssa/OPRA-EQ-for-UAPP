package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManagedProfileSnapshotCodecTest {
    private val codec = ManagedProfileSnapshotCodec()

    @Test
    fun snapshotRoundTripPreservesUnicodeAndAcousticData() {
        val profile = sampleProfile(details = "測定 • édition")

        assertEquals(profile, codec.decode(codec.encode(profile)))
    }

    @Test
    fun snapshotRoundTripKeepsGeneratedSafetyHeadroomSeparateFromSourcePreamp() {
        val profile = sampleProfile(details = "Community tuning").copy(
            preampGainDb = null,
            eqLibrarySafetyHeadroomDb = -5.75,
        )

        val restored = codec.decode(codec.encode(profile))

        assertNull(restored.preampGainDb)
        assertEquals(-5.75, restored.eqLibrarySafetyHeadroomDb!!, 0.0)
        assertEquals(profile.bands, restored.bands)
    }

    @Test
    fun oldStoredSnapshotWithoutSafetyFieldStillDecodes() {
        val encoded = """
            {
              "id":"legacy",
              "productId":"product",
              "author":"Creator",
              "details":"Target",
              "link":null,
              "profileType":"parametric_eq",
              "preampGainDb":-3.0,
              "bands":[{"type":"peak_dip","frequency":1000.0,"gainDb":-2.0,"q":1.0,"slope":null}]
            }
        """.trimIndent()

        val restored = codec.decode(encoded)

        assertEquals(-3.0, restored.preampGainDb!!, 0.0)
        assertNull(restored.eqLibrarySafetyHeadroomDb)
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

    @Test
    fun fingerprintIgnoresProvenanceLinkOnlyChanges() {
        val original = sampleProfile(details = "Original")
        val linkChanged = original.copy(link = "https://example.invalid/other-source")

        assertEquals(codec.fingerprint(original), codec.fingerprint(linkChanged))
    }

    @Test
    fun fingerprintTreatsAuthorAndDetailsCaseAsReferenceEquivalent() {
        val original = sampleProfile(details = "Harman Target")
        val caseChanged = original.copy(
            author = original.author?.uppercase(),
            details = original.details?.uppercase(),
        )

        assertEquals(codec.fingerprint(original), codec.fingerprint(caseChanged))
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
