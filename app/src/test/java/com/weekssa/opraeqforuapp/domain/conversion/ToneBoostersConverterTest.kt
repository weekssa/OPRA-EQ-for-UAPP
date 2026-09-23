package com.weekssa.opraeqforuapp.domain.conversion

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.catalog.assessUappCompatibility
import com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapter
import com.weekssa.opraeqforuapp.domain.library.OpraCanonicalCatalogAdapter
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneBoostersConverterTest {
    @Test
    fun knownNormalizationMatchesReferenceBounds() {
        assertEquals(0.0, ToneBoostersConverter.normalizeGain(-20.0), 0.0)
        assertEquals(0.5, ToneBoostersConverter.normalizeGain(0.0), 0.0)
        assertEquals(1.0, ToneBoostersConverter.normalizeGain(20.0), 0.0)
        assertEquals(0.0, ToneBoostersConverter.normalizeFrequency(16.0), 0.0)
        assertEquals(1.0, ToneBoostersConverter.normalizeFrequency(20_000.0), 0.0)
        assertEquals(0.0, ToneBoostersConverter.normalizeQ(0.1), 0.0)
        assertEquals(1.0, ToneBoostersConverter.normalizeQ(10.0), 0.0)
    }

    @Test
    fun twoBandGoldenXmlMatchesPythonReference() {
        val result = ToneBoostersConverter.buildXml(
            presetName = "Test",
            gainDb = -5.4,
            bands = listOf(
                OpraBand("peak_dip", 70.0, -2.5, 0.5, null),
                OpraBand("low_shelf", 105.0, 5.5, 0.71, null),
            ),
        )
        val expected = requireNotNull(javaClass.getResource("/golden/two_band_reference.xml")).readText()

        assertEquals(expected, result.xml)
        assertTrue(result.warnings.isEmpty())
        assertEquals(2, result.convertedBandCount)
    }

    @Test
    fun recognizedFilterAliasesExportExactlyLikeTheirCanonicalToken() {
        val canonical = ToneBoostersConverter.buildXml(
            "Alias parity",
            0.0,
            listOf(OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null)),
        )
        val alias = ToneBoostersConverter.buildXml(
            "Alias parity",
            0.0,
            listOf(OpraBand(" PK ", 1_000.0, -2.0, 1.0, null)),
        )

        assertEquals(canonical.xml, alias.xml)
    }

    @Test
    fun moreThanTenBandsUsesFirstTenAndWarns() {
        val bands = (1..12).map { index ->
            OpraBand("peak_dip", 100.0 * index, index / 10.0, 1.0, null)
        }
        val result = ToneBoostersConverter.buildXml("Twelve", -2.0, bands)

        assertEquals(12, result.sourceBandCount)
        assertEquals(10, result.convertedBandCount)
        assertEquals(
            listOf(
                "Source has 12 bands; the current UAPP/ToneBoosters target supports 10, so only the first 10 in the supplied source order were used.",
            ),
            result.warnings,
        )
        assertEquals(66, Regex("<Value>").findAll(result.xml).count())
        val values = Regex("<Value>(.*?)</Value>").findAll(result.xml)
            .map { it.groupValues[1].toDouble() }
            .toList()
        assertEquals(ToneBoostersConverter.normalizeFrequency(bands.first().frequency!!), values[0], 0.0000001)
        assertEquals(ToneBoostersConverter.normalizeFrequency(bands[9].frequency!!), values[54], 0.0000001)
    }

    @Test
    fun opraPriorityOrderSurvivesCanonicalProjectionAndToneBoostersExport() {
        val orderedSourceBands = (1..12).map { index ->
            OpraBand("peak_dip", 100.0 * index, index / 10.0, 1.0, null)
        }
        val canonical = OpraCanonicalCatalogAdapter.adapt(
            catalog = OpraCatalog(
                vendors = listOf(OpraVendor("vendor", "Fixture")),
                products = listOf(OpraProduct("product", "vendor", "Headphone", "headphone", "over-ear")),
                profiles = listOf(
                    OpraEqProfile(
                        id = "priority-profile",
                        productId = "product",
                        author = "Fixture Author",
                        details = null,
                        link = null,
                        profileType = "parametric_eq",
                        preampGainDb = 0.0,
                        bands = orderedSourceBands,
                    ),
                ),
            ),
            generatedAt = "2026-09-23T00:00:00Z",
            sourceRegistryVersion = "test",
        )
        val legacy = CanonicalLegacyCatalogAdapter.adapt(canonical.snapshot).profiles.single()

        val exported = ToneBoostersConverter.convert(legacy, "Priority test")
        val values = Regex("<Value>(.*?)</Value>").findAll(exported.xml)
            .map { it.groupValues[1].toDouble() }
            .toList()

        assertEquals(12, exported.sourceBandCount)
        assertEquals(10, exported.convertedBandCount)
        assertEquals(ToneBoostersConverter.normalizeFrequency(orderedSourceBands.first().frequency!!), values[0], 0.0000001)
        assertEquals(ToneBoostersConverter.normalizeFrequency(orderedSourceBands[9].frequency!!), values[54], 0.0000001)
        assertTrue(exported.warnings.single().contains("supplied source order"))
    }

    @Test
    fun `unsupported band outside the explicit ten-band source-priority budget is not mis-exported`() {
        val profile = OpraEqProfile(
            id = "over-budget-unsupported",
            productId = "product",
            author = "Creator",
            details = null,
            link = null,
            profileType = "parametric_eq",
            preampGainDb = 0.0,
            bands = (1..10).map { index ->
                OpraBand("peak_dip", 100.0 * index, 0.0, 1.0, null)
            } + OpraBand("band_stop", 2_000.0, 0.0, 1.0, null),
        )

        assertEquals(ProfileCompatibility.CompatibleWithLimitation, profile.assessUappCompatibility().category)
        val converted = ToneBoostersConverter.convert(profile, "Ten-band source-priority boundary")
        assertEquals(10, converted.convertedBandCount)
        assertEquals(11, converted.sourceBandCount)
    }

    @Test
    fun malformedUnsupportedFilterPastOutputLimitStillInvalidatesSource() {
        val profile = OpraEqProfile(
            id = "malformed-over-budget-filter",
            productId = "product",
            author = "Creator",
            details = null,
            link = null,
            profileType = "parametric_eq",
            preampGainDb = 0.0,
            bands = (1..10).map { index ->
                OpraBand("peak_dip", 100.0 * index, 0.0, 1.0, null)
            } + OpraBand("band_stop", 2_000.0, 0.0, null, null),
        )

        assertEquals(ProfileCompatibility.NotCompatible, profile.assessUappCompatibility().category)
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.convert(profile, "Malformed over-budget filter")
        }
    }

    @Test
    fun canonicalUnknownFilterTypeRemainsUnexportableRatherThanBeingDiscarded() {
        val profile = OpraEqProfile(
            id = "unknown-filter",
            productId = "product",
            author = "Creator",
            details = null,
            link = null,
            profileType = "parametric_eq",
            preampGainDb = 0.0,
            bands = listOf(OpraBand("tilt_filter", 1_000.0, 0.0, 1.0, null)),
        )

        assertEquals(ProfileCompatibility.FullyCompatible, profile.assessCompatibility().category)
        assertEquals(ProfileCompatibility.NotCompatible, profile.assessUappCompatibility().category)
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.convert(profile, "Unknown filter")
        }
    }

    @Test
    fun generatedSafetyHeadroomIsUsedForPlaybackButSourcePreampStaysNull() {
        val bands = listOf(OpraBand("peak_dip", 1_000.0, 4.0, 1.0, null))
        val profile = OpraEqProfile(
            id = "generated-headroom",
            productId = "product",
            author = "Creator",
            details = "Community tuning",
            link = "https://example.invalid/source",
            profileType = "parametric_eq",
            preampGainDb = null,
            bands = bands,
            eqLibrarySafetyHeadroomDb = -6.25,
        )

        val converted = ToneBoostersConverter.convert(profile, "Generated headroom")
        val expected = ToneBoostersConverter.buildXml("Generated headroom", -6.25, bands)

        assertEquals(expected.xml, converted.xml)
        assertNull(profile.preampGainDb)
        assertEquals(-6.25, profile.eqLibrarySafetyHeadroomDb!!, 0.0)
        assertTrue(converted.warnings.first().contains("EQ Library-generated safety headroom of -6.25 dB"))
    }

    @Test
    fun unsupportedFilterCannotProduceXml() {
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.buildXml(
                "Unsupported",
                0.0,
                listOf(OpraBand("low_pass", 1_000.0, 0.0, 1.0, 12.0)),
            )
        }
    }

    @Test
    fun deviceIndependentSelectionDoesNotBypassUappConversionGate() {
        val profile = OpraEqProfile(
            id = "blocked",
            productId = "product",
            author = "Creator",
            details = null,
            link = null,
            profileType = "parametric_eq",
            preampGainDb = 0.0,
            bands = listOf(OpraBand("band_stop", 1_000.0, 0.0, 1.0, null)),
        )

        assertTrue(profile.assessCompatibility().category.isSelectable)
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.convert(profile, "Blocked")
        }
    }

    @Test
    fun missingPreampCanRemainSelectableButCannotProduceUappXmlWithoutHeadroom() {
        val profile = OpraEqProfile(
            id = "missing-preamp",
            productId = "product",
            author = "Creator",
            details = null,
            link = null,
            profileType = "parametric_eq",
            preampGainDb = null,
            bands = listOf(OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null)),
        )

        assertTrue(profile.assessCompatibility().category.isSelectable)
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.convert(profile, "Missing preamp")
        }
    }

    @Test
    fun safeNameAndHeadphoneFirstNamingMatchReference() {
        assertEquals("RTINGS - Studio", ToneBoostersConverter.uappSafeName("RTINGS • Studio"))
        assertEquals("Unicode -", ToneBoostersConverter.uappSafeName("Unicode 測"))
        assertEquals(
            "HD650 - oratory1990 - Harman Target",
            ToneBoostersConverter.buildPresetName("HD650", "oratory1990", "Harman Target"),
        )
        assertEquals(
            "EW300 Gold - AutoEQ - Fahryst",
            ToneBoostersConverter.buildPresetName(
                modelLabel = "EW300 Gold",
                creator = "AutoEQ",
                details = "Measured by Fahryst (gold)",
                verifiedVariantLabel = "Gold",
            ),
        )
    }

    @Test
    fun missingCreatorUsesApprovedLiteralLabelWithoutChangingCompatibility() {
        val profile = OpraEqProfile(
            id = "missing-author",
            productId = "product",
            author = null,
            details = "Harman Target",
            link = null,
            profileType = "parametric_eq",
            preampGainDb = -3.0,
            bands = listOf(OpraBand("peak_dip", 1_000.0, 1.0, 1.0, null)),
        )

        assertTrue(profile.assessCompatibility().category.isSelectable)
        assertEquals(
            "HD650 - Creator information missing - Harman Target",
            ToneBoostersConverter.buildPresetName("HD650", profile.author, profile.details),
        )
        assertEquals(null, profile.author)
    }

    @Test
    fun outOfRangeValuesAreRejectedRatherThanClamped() {
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.normalizeFrequency(15.9)
        }
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.normalizeGain(20.1)
        }
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.normalizeQ(10.1)
        }
    }
}
