package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AcousticFingerprintTest {
    @Test
    fun fingerprintIgnoresBandOrderAndCommonAliases() {
        val a = listOf(
            EqFilter("PK", 1000.0, -2.0, 1.4),
            EqFilter("LOW_SHELF", 105.0, 3.0, 0.71),
        )
        val b = listOf(
            EqFilter("LS", 105.0001, 3.0001, 0.71001),
            EqFilter("PEAKING", 1000.0001, -2.0001, 1.40001),
        )

        assertEquals(AcousticFingerprint.of(-5.0, a), AcousticFingerprint.of(-5.0001, b))
    }

    @Test
    fun fingerprintChangesForMaterialGainChange() {
        val base = listOf(EqFilter("PK", 1000.0, -2.0, 1.4))
        val changed = listOf(EqFilter("PK", 1000.0, -1.5, 1.4))

        assertNotEquals(AcousticFingerprint.of(-5.0, base), AcousticFingerprint.of(-5.0, changed))
    }

    @Test
    fun opraAdapterCreatesCanonicalRevisionWithProvenance() {
        val adapted = OpraProfileAdapter.adapt(
            vendor = OpraVendor("hifiman", "HIFIMAN"),
            product = OpraProduct("edition-xs", "hifiman", "Edition XS", "headphone", "over-ear"),
            profile = OpraEqProfile(
                id = "example-profile",
                productId = "edition-xs",
                author = "Example Author",
                details = "Harman target",
                link = "https://example.com/eq",
                profileType = "parametric",
                preampGainDb = -5.0,
                bands = listOf(OpraBand("PK", 1000.0, -2.0, 1.4, null)),
            ),
            discoveredAt = "2026-08-29T00:00:00Z",
        )

        assertNotNull(adapted)
        adapted!!
        assertEquals("Harman", adapted.target)
        assertEquals(ProvenanceTier.AUTHORITATIVE, adapted.provenanceTier)
        assertEquals("opra:example-profile", adapted.sourceReferences.single().sourceId)
        assertEquals(adapted.latestRevisionId, adapted.latestRevision.revisionId)
    }
}
