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
    fun fingerprintIgnoresBandOrderAndHarmlessPrecisionDifferences() {
        val a = listOf(
            EqFilter(EqFilterType.PEAK, 1000.0, -2.0, 1.4),
            EqFilter(EqFilterType.LOW_SHELF, 105.0, 3.0, 0.71),
        )
        val b = listOf(
            EqFilter(EqFilterType.LOW_SHELF, 105.0001, 3.0001, 0.71001),
            EqFilter(EqFilterType.PEAK, 1000.0001, -2.0001, 1.40001),
        )

        assertEquals(AcousticFingerprint.of(-5.0, a), AcousticFingerprint.of(-5.0001, b))
    }

    @Test
    fun fingerprintChangesForMaterialGainChange() {
        val base = listOf(EqFilter(EqFilterType.PEAK, 1000.0, -2.0, 1.4))
        val changed = listOf(EqFilter(EqFilterType.PEAK, 1000.0, -1.5, 1.4))

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
            discoveredAtEpochSeconds = 1_788_134_400L,
        )

        assertNotNull(adapted)
        adapted!!
        assertEquals("Harman", adapted.target.name)
        assertEquals(EqTargetKind.EXPLICIT_TARGET, adapted.target.kind)
        val source = adapted.latestRevision.sourceReferences.single()
        assertEquals(ProvenanceTier.AUTHORITATIVE, source.provenanceTier)
        assertEquals("opra", source.sourceId)
        assertEquals("example-profile", source.sourceRecordId)
        assertEquals(adapted.latestRevision.revisionId, adapted.revisions.single().revisionId)
    }
}
