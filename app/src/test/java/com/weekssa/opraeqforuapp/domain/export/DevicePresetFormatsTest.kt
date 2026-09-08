package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePresetFormatsTest {
    private val profile = OpraEqProfile(
        id = "profile-1",
        productId = "product-1",
        author = "Tester",
        details = "Test target",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = -5.5,
        bands = listOf(
            OpraBand("low_shelf", 105.0, 4.0, 0.75, null),
            OpraBand("peak_dip", 1_000.0, -2.5, 1.25, null),
            OpraBand("high_shelf", 8_000.0, -1.5, 0.75, null),
        ),
    )

    private val blackPearlProfile = profile.copy(preampGainDb = 0.0)

    @Test
    fun toppingTuneUsesStandardAutoEqShelfAndPeakTokens() {
        val variant = buildTextDeviceVariant(profile, ExportDevice.TOPPING_TUNE)!!

        assertTrue(variant.content.contains("Preamp: -5.50 dB"))
        assertTrue(variant.content.contains("ON LSC"))
        assertTrue(variant.content.contains("ON PK"))
        assertTrue(variant.content.contains("ON HSC"))
        assertEquals(DevicePresetFidelity.EXACT, variant.fidelity)
        assertTrue(variant.transformation.contains("TOPPING Tune"))
        assertEquals(1, variant.representationVersion)
    }

    @Test
    fun toppingTuneFitsCompleteResponseAboveTenBandsWithoutFirstTenTruncation() {
        val sourceBands = (1..12).map { index ->
            OpraBand(
                type = "peak_dip",
                frequency = 100.0 * index,
                gainDb = (index - 6) / 2.0,
                q = 1.0,
                slope = null,
            )
        }
        val source = profile.copy(preampGainDb = -6.0, bands = sourceBands)

        val variant = buildTextDeviceVariant(source, ExportDevice.TOPPING_TUNE)!!

        assertTrue(filterLines(variant.content).size <= 10)
        assertEquals(DevicePresetFidelity.OPTIMIZED, variant.fidelity)
        assertTrue(variant.transformation.contains("fitted the complete source response"))
        assertTrue(variant.transformation.contains("no source bands were silently truncated"))
        assertEquals(sourceBands, source.bands)
    }

    @Test
    fun toppingTuneDoesNotClampOutOfRangeSourcePreampToMakeAFile() {
        val source = profile.copy(preampGainDb = -15.0)

        assertNull(buildTextDeviceVariant(source, ExportDevice.TOPPING_TUNE))
        assertEquals(-15.0, source.preampGainDb!!, 0.0)
    }

    @Test
    fun blackPearlFileRepresentationUsesVerifiedPyBlackPearlShelfTokens() {
        val blackPearl = buildFileExportDeviceVariant(blackPearlProfile, ExportDevice.BLACK_PEARL)!!
        val filterLines = filterLines(blackPearl.content)

        assertEquals(3, filterLines.size)
        assertTrue(blackPearl.content.contains("Preamp: 0.00 dB"))
        assertTrue(blackPearl.content.contains("ON LS "))
        assertTrue(blackPearl.content.contains("ON PK "))
        assertTrue(blackPearl.content.contains("ON HS "))
        assertTrue(!blackPearl.content.contains("ON LSC"))
        assertTrue(!blackPearl.content.contains("ON HSC"))
        assertEquals(DevicePresetFidelity.EXACT, blackPearl.fidelity)
        assertTrue(blackPearl.transformation.contains("same filters and playback gain as Direct Flash"))
        assertEquals(3, blackPearl.representationVersion)
    }

    @Test
    fun blackPearlFitsCompleteResponseAboveTenBandsWithoutMutatingSource() {
        val sourceBands = (1..12).map { index ->
            OpraBand(
                type = "peak_dip",
                frequency = 100.0 * index,
                gainDb = (index - 6) / 2.0,
                q = 1.0,
                slope = null,
            )
        }
        val source = blackPearlProfile.copy(bands = sourceBands)

        val variant = buildFileExportDeviceVariant(source, ExportDevice.BLACK_PEARL)!!

        assertTrue(filterLines(variant.content).size <= 10)
        assertEquals(DevicePresetFidelity.OPTIMIZED, variant.fidelity)
        assertTrue(variant.transformation.contains("full-response fit"))
        assertEquals(12, source.bands!!.size)
        assertEquals(sourceBands, source.bands)
    }

    @Test
    fun blackPearlPreservesExactProtocolGainOutsideValidatedRangeWithCaution() {
        val source = blackPearlProfile.copy(
            preampGainDb = -4.0,
            bands = listOf(OpraBand("peak_dip", 1_000.0, -12.0, 1.0, null)),
        )

        val variant = buildFileExportDeviceVariant(source, ExportDevice.BLACK_PEARL)!!

        assertEquals(DevicePresetFidelity.EXACT, variant.fidelity)
        assertTrue(variant.content.contains("Gain -12.00 dB"))
        assertTrue(variant.transformation.contains("outside the currently validated"))
        assertTrue(variant.transformation.contains("not clamped"))
    }

    @Test
    fun blackPearlFileExportsTruePreampEvenWhenPyBlackPearlWillLimitIt() {
        val source = blackPearlProfile.copy(
            preampGainDb = -18.0,
            bands = listOf(OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null)),
        )

        val variant = buildFileExportDeviceVariant(source, ExportDevice.BLACK_PEARL)!!

        assertTrue(variant.content.contains("Preamp: -18.00 dB"))
        assertTrue(variant.transformation.contains("pyBlackPearl accepts this AutoEq text"))
        assertTrue(variant.transformation.contains("limits imported preamp"))
        assertTrue(variant.transformation.contains("exports the true -18.00 dB value unchanged"))
        assertTrue(variant.transformation.contains("Direct Flash remains independent"))
    }

    @Test
    fun blackPearlFileRepresentationCarriesNonzeroPlaybackGain() {
        val variant = buildFileExportDeviceVariant(profile, ExportDevice.BLACK_PEARL)!!
        assertTrue(variant.content.contains("Preamp: -5.50 dB"))
        assertEquals(DevicePresetFidelity.EXACT, variant.fidelity)
    }

    @Test
    fun textVariantSetCoversSelectableAndInternalTextTargetsExceptSeparatelyBuiltBlackPearlAndUapp() {
        val variants = buildTextDeviceVariants(blackPearlProfile)
        assertEquals(
            setOf(
                ExportDevice.UNIVERSAL_PARAMETRIC,
                ExportDevice.POWERAMP,
                ExportDevice.TOPPING_TUNE,
                ExportDevice.WAVELET,
                ExportDevice.EASY_EFFECTS,
                ExportDevice.EQUALIZER_APO,
                ExportDevice.UNIVERSAL_GRAPHIC_EQ,
                ExportDevice.TOPPING_DX5_II,
                ExportDevice.TOPPING_DX1_II,
            ),
            variants.mapTo(mutableSetOf()) { it.device },
        )
    }

    @Test
    fun requestedFormatterUsesDedicatedBlackPearlPathAndHardwareOnlyTargetsHaveNoFileVariant() {
        assertNull(buildTextDeviceVariant(blackPearlProfile, ExportDevice.BLACK_PEARL))
        assertEquals(
            ExportDevice.BLACK_PEARL,
            buildFileExportDeviceVariant(blackPearlProfile, ExportDevice.BLACK_PEARL)?.device,
        )
        assertNull(buildFileExportDeviceVariant(profile, ExportDevice.FIIO_JA11))
        assertNull(buildFileExportDeviceVariant(profile, ExportDevice.JCALLY_JM12))
        assertNull(buildTextDeviceVariant(profile, ExportDevice.UAPP))
    }

    @Test
    fun outputRegistryGroupsSelectableTargetsAndKeepsPendingHardwareLabels() {
        assertEquals("Untested", ExportDevice.TOPPING_DX5_II.validationStatus)
        assertEquals("Untested", ExportDevice.TOPPING_DX1_II.validationStatus)
        assertEquals("Official AutoEq import path", ExportDevice.TOPPING_TUNE.validationStatus)
        assertEquals("Hardware validation pending", ExportDevice.FIIO_JA11.validationStatus)
        assertEquals("Hardware validation pending · persistence pending", ExportDevice.JCALLY_JM12.validationStatus)
        assertTrue(ExportDevice.UAPP.validationStatus == null)
        assertTrue(ExportDevice.BLACK_PEARL.validationStatus == null)
        assertTrue(ExportDevice.BLACK_PEARL.isHardwareOutput)
        assertTrue(!ExportDevice.TOPPING_TUNE.isHardwareOutput)
        assertTrue(!ExportDevice.EASY_EFFECTS.isHardwareOutput)
        assertTrue(ExportDevice.TOPPING_TUNE in ExportDevice.selectableOutputs)
        assertTrue(ExportDevice.TOPPING_DX5_II !in ExportDevice.selectableOutputs)
        assertTrue(ExportDevice.TOPPING_DX1_II !in ExportDevice.selectableOutputs)
        assertTrue(ExportDevice.selectableOutputs.zipWithNext().all { (a, b) ->
            a.category.sortOrder < b.category.sortOrder ||
                (a.category == b.category && a.displayName.lowercase() <= b.displayName.lowercase())
        })
    }

    @Test
    fun parametricAndGraphicTargetsDeclareAppropriateCapabilitiesAndFormats() {
        assertEquals(OutputFormatKind.GRAPHIC_EQ_127, ExportDevice.WAVELET.formatKind)
        assertEquals(OutputFormatKind.GRAPHIC_EQ_127, ExportDevice.UNIVERSAL_GRAPHIC_EQ.formatKind)
        assertEquals(null, ExportDevice.WAVELET.eqCapabilities)
        assertEquals(null, ExportDevice.UNIVERSAL_GRAPHIC_EQ.eqCapabilities)

        val wavelet = buildTextDeviceVariant(profile, ExportDevice.WAVELET)!!
        val universalGraphic = buildTextDeviceVariant(profile, ExportDevice.UNIVERSAL_GRAPHIC_EQ)!!
        assertEquals(DevicePresetFidelity.OPTIMIZED, wavelet.fidelity)
        assertEquals(DevicePresetFidelity.OPTIMIZED, universalGraphic.fidelity)
        assertTrue(wavelet.transformation.contains("127-point GraphicEQ"))
        assertTrue(universalGraphic.transformation.contains("127-point AutoEq GraphicEQ"))

        val uapp = ExportDevice.UAPP.eqCapabilities!!
        assertEquals(10, uapp.maxBands)
        assertEquals(setOf("peak_dip", "low_shelf", "high_shelf"), uapp.supportedBandTypes)
        assertEquals(16.0, uapp.minFrequencyHz, 0.0)
        assertEquals(20_000.0, uapp.maxFrequencyHz, 0.0)
        assertEquals(-20.0, uapp.minGainDb, 0.0)
        assertEquals(20.0, uapp.maxGainDb, 0.0)
        assertEquals(0.1, uapp.minQ, 0.0)
        assertEquals(10.0, uapp.maxQ, 0.0)
        assertEquals(-20.0, uapp.minPreampDb!!, 0.0)
        assertEquals(20.0, uapp.maxPreampDb!!, 0.0)

        val blackPearl = ExportDevice.BLACK_PEARL.eqCapabilities!!
        assertEquals(10, blackPearl.maxBands)
        assertEquals(setOf("peak_dip", "low_shelf", "high_shelf"), blackPearl.supportedBandTypes)
        assertTrue(blackPearl.minGainDb < -10.0)
        assertTrue(blackPearl.maxGainDb > 10.0)
        assertEquals(0.1, blackPearl.minQ, 0.0)
        assertEquals(10.0, blackPearl.maxQ, 0.0)

        val topping = ExportDevice.TOPPING_TUNE.eqCapabilities!!
        assertEquals(10, topping.maxBands)
        assertEquals(setOf("peak_dip", "low_shelf", "high_shelf"), topping.supportedBandTypes)
        assertEquals(-12.0, topping.minGainDb, 0.0)
        assertEquals(12.0, topping.maxGainDb, 0.0)
        assertEquals(0.1, topping.minQ, 0.0)
        assertEquals(15.0, topping.maxQ, 0.0)
        assertEquals(-12.0, topping.minPreampDb!!, 0.0)
        assertEquals(12.0, topping.maxPreampDb!!, 0.0)

        assertEquals(32, ExportDevice.EASY_EFFECTS.eqCapabilities!!.maxBands)
        assertEquals(64, ExportDevice.POWERAMP.eqCapabilities!!.maxBands)
        assertEquals(null, ExportDevice.EQUALIZER_APO.eqCapabilities!!.maxBands)
        assertEquals(null, ExportDevice.UNIVERSAL_PARAMETRIC.eqCapabilities!!.maxBands)
    }

    @Test
    fun missingSourcePreampIsNeverMisrepresentedAsExactForToppingTune() {
        val source = profile.copy(
            preampGainDb = null,
            bands = listOf(OpraBand("peak_dip", 1_000.0, 2.0, 1.0, null)),
        )

        val variant = buildTextDeviceVariant(source, ExportDevice.TOPPING_TUNE)!!

        assertEquals(DevicePresetFidelity.OPTIMIZED, variant.fidelity)
        assertTrue(variant.transformation.contains("generated"))
        assertEquals(null, source.preampGainDb)
    }

    @Test
    fun generatedHardwareHeadroomUsesFinalBlackPearlResponseNotStoredCanonicalHintAndIsOptimized() {
        val source = OpraEqProfile(
            id = "generated",
            productId = "product-1",
            author = "Tester",
            details = "Generated",
            link = null,
            profileType = "parametric_eq",
            preampGainDb = null,
            bands = listOf(OpraBand("peak_dip", 1_000.0, 4.0, 1.0, null)),
            eqLibrarySafetyHeadroomDb = -9.0,
        )

        val blackPearl = buildFileExportDeviceVariant(source, ExportDevice.BLACK_PEARL)!!

        assertTrue(blackPearl.content.contains("Preamp: -4.00 dB"))
        assertEquals(DevicePresetFidelity.OPTIMIZED, blackPearl.fidelity)
        assertTrue(blackPearl.transformation.contains("generated headroom"))
        assertEquals(null, source.preampGainDb)
        assertEquals(-9.0, source.eqLibrarySafetyHeadroomDb!!, 0.0)
    }

    private fun filterLines(content: String): List<String> =
        content.lineSequence().filter { it.startsWith("Filter ") }.toList()
}
