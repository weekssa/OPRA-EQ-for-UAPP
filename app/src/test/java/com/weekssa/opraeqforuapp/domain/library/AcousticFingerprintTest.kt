package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConversionException
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConverter
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    fun unsupportedSourceFilterTypesRemainDistinctInCanonicalFingerprints() {
        val bandPass = listOf(
            EqFilter(EqFilterType.OTHER, 1_000.0, 0.0, 1.0, sourceType = "band_pass"),
        )
        val bandStop = listOf(
            EqFilter(EqFilterType.OTHER, 1_000.0, 0.0, 1.0, sourceType = "band_stop"),
        )

        assertNotEquals(AcousticFingerprint.of(0.0, bandPass), AcousticFingerprint.of(0.0, bandStop))
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
                profileType = "parametric_eq",
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

    @Test
    fun opraRevisionIdentityChangesWhenSourcePriorityOrderChanges() {
        val vendor = OpraVendor("hifiman", "HIFIMAN")
        val product = OpraProduct("edition-xs", "hifiman", "Edition XS", "headphones", "over-ear")
        val firstOrder = listOf(
            OpraBand("peak_dip", 100.0, 2.0, 1.0, null),
            OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null),
        )
        val baseProfile = OpraEqProfile(
            id = "priority-profile",
            productId = product.id,
            author = "Example Author",
            details = null,
            link = null,
            profileType = "parametric_eq",
            preampGainDb = 0.0,
            bands = firstOrder,
        )

        val firstRevision = requireNotNull(OpraProfileAdapter.adapt(vendor, product, baseProfile))
            .latestRevision
        val reorderedRevision = requireNotNull(
            OpraProfileAdapter.adapt(vendor, product, baseProfile.copy(bands = firstOrder.reversed())),
        ).latestRevision

        assertEquals(firstRevision.acousticFingerprint, reorderedRevision.acousticFingerprint)
        assertNotEquals(firstRevision.revisionId, reorderedRevision.revisionId)
        assertEquals(listOf(100.0, 1_000.0), firstRevision.filters.map(EqFilter::frequencyHz))
        assertEquals(listOf(1_000.0, 100.0), reorderedRevision.filters.map(EqFilter::frequencyHz))
    }

    @Test
    fun opraAdapterRejectsTheWholeProfileWhenAnyRequiredBandFieldIsMissing() {
        val vendor = OpraVendor("hifiman", "HIFIMAN")
        val product = OpraProduct("edition-xs", "hifiman", "Edition XS", "headphones", "over-ear")
        val profile = OpraEqProfile(
            id = "incomplete-profile",
            productId = product.id,
            author = "Example Author",
            details = null,
            link = null,
            profileType = "parametric_eq",
            preampGainDb = 0.0,
            bands = listOf(
                OpraBand("peak_dip", 1_000.0, 1.0, 1.0, null),
                OpraBand(null, 2_000.0, 1.0, 1.0, null),
            ),
        )

        assertNull(OpraProfileAdapter.adapt(vendor, product, profile))
    }

    @Test
    fun opraAdapterRejectsARecordThatIsNotParametricEq() {
        val adapted = OpraProfileAdapter.adapt(
            vendor = OpraVendor("hifiman", "HIFIMAN"),
            product = OpraProduct("edition-xs", "hifiman", "Edition XS", "headphones", "over-ear"),
            profile = OpraEqProfile(
                id = "graphic-profile",
                productId = "edition-xs",
                author = "Example Author",
                details = null,
                link = null,
                profileType = "graphic_eq",
                preampGainDb = 0.0,
                bands = listOf(OpraBand("peak_dip", 1_000.0, 0.0, 1.0, null)),
            ),
        )

        assertNull(adapted)
    }

    @Test
    fun opraAdapterPreservesSchemaDefaultGainAndExactUnsupportedFilterType() {
        val adapted = requireNotNull(
            OpraProfileAdapter.adapt(
                vendor = OpraVendor("hifiman", "HIFIMAN"),
                product = OpraProduct("edition-xs", "hifiman", "Edition XS", "headphones", "over-ear"),
                profile = OpraEqProfile(
                    id = "band-stop-profile",
                    productId = "edition-xs",
                    author = "Example Author",
                    details = null,
                    link = null,
                    profileType = "parametric_eq",
                    preampGainDb = 0.0,
                    bands = listOf(OpraBand("Band_Stop", 1_000.0, null, 1.0, null)),
                ),
            ),
        )
        val serialized = Json.encodeToString(adapted)
        val restored = Json.decodeFromString<CanonicalEqProfile>(serialized)
        val filter = restored.latestRevision.filters.single()

        assertEquals(EqFilterType.OTHER, filter.type)
        assertEquals("Band_Stop", filter.sourceType)
        assertEquals(0.0, filter.gainDb!!, 0.0)
        val legacy = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(
                schemaVersion = 1,
                generatedAt = "2026-09-23T00:00:00Z",
                sourceRegistryVersion = "test",
                profiles = listOf(restored),
            ),
        )
        assertEquals("Band_Stop", legacy.profiles.single().bands!!.single().type)
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.convert(legacy.profiles.single(), "Unsupported Band Stop")
        }
    }

    @Test
    fun opraAdapterAppliesSchemaDefaultSlopeToLowAndHighPassFilters() {
        val adapted = requireNotNull(
            OpraProfileAdapter.adapt(
                vendor = OpraVendor("v", "Vendor"),
                product = OpraProduct("p", "v", "Product", "headphones", "over-ear"),
                profile = OpraEqProfile(
                    id = "pass-filter-default-slope",
                    productId = "p",
                    author = "Author",
                    details = null,
                    link = null,
                    profileType = "parametric_eq",
                    preampGainDb = 0.0,
                    bands = listOf(
                        OpraBand("low_pass", 8_000.0, 0.0, null, null),
                        OpraBand("high_pass", 80.0, 0.0, null, null),
                    ),
                ),
            ),
        )

        assertEquals(listOf(12.0, 12.0), adapted.latestRevision.filters.map(EqFilter::slope))
    }

    @Test
    fun opraAdapterRejectsNonFinitePreampAndPreservesDistinctUnknownSourceTypes() {
        val vendor = OpraVendor("hifiman", "HIFIMAN")
        val product = OpraProduct("edition-xs", "hifiman", "Edition XS", "headphones", "over-ear")
        val invalidPreamp = OpraEqProfile(
            id = "non-finite-preamp",
            productId = product.id,
            author = "Example Author",
            details = null,
            link = null,
            profileType = "parametric_eq",
            preampGainDb = Double.POSITIVE_INFINITY,
            bands = listOf(OpraBand("peak_dip", 1_000.0, 0.0, 1.0, null)),
        )
        assertNull(OpraProfileAdapter.adapt(vendor, product, invalidPreamp))

        val adapted = requireNotNull(
            OpraProfileAdapter.adapt(
                vendor = vendor,
                product = product,
                profile = OpraEqProfile(
                    id = "unknown-types",
                    productId = product.id,
                    author = "Example Author",
                    details = null,
                    link = null,
                    profileType = "parametric_eq",
                    preampGainDb = 0.0,
                    bands = listOf(
                        OpraBand("tilt_filter", 500.0, 1.0, null, null),
                        OpraBand("notch_custom", 2_000.0, -1.0, null, null),
                    ),
                ),
            ),
        )
        assertEquals(listOf("tilt_filter", "notch_custom"), adapted.latestRevision.filters.map(EqFilter::sourceType))
        assertEquals(
            listOf("tilt_filter", "notch_custom"),
            CanonicalLegacyCatalogAdapter.adapt(
                CatalogSnapshot(
                    schemaVersion = 1,
                    generatedAt = "2026-09-23T00:00:00Z",
                    sourceRegistryVersion = "test",
                    profiles = listOf(adapted),
                ),
            ).profiles.single().bands!!.map { it.type },
        )
    }
}
