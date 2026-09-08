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
    fun toppingPreservesSupportedShelfAndPeakTypes() {
        val variants = buildTextDeviceVariants(profile)
        val dx5 = variants.single { it.device == ExportDevice.TOPPING_DX5_II }
        assertTrue(dx5.content.contains("Preamp: -5.50 dB"))
        assertTrue(dx5.content.contains("ON LSC"))
        assertTrue(dx5.content.contains("ON PK"))
        assertTrue(dx5.content.contains("ON HSC"))
        assertEquals(DevicePresetFidelity.EXACT, dx5.fidelity)
        assertTrue(dx5.transformation.contains("Source EQ preserved"))
    }

    @Test
    fun blackPearlFileRepresentationPreservesNativeShelfAndPeakTypes() {
        val blackPearl = buildFileExportDeviceVariant(blackPearlProfile, ExportDevice.BLACK_PEARL)!!
        val filterLines = filterLines(blackPearl.content)

        assertEquals(3, filterLines.size)
        assertTrue(blackPearl.content.contains("Preamp: 0.00 dB"))
        assertTrue(blackPearl.content.contains("ON LSC"))
        assertTrue(blackPearl.content.contains("ON PK"))
        assertTrue(blackPearl.content.contains("ON HSC"))
        assertEquals(DevicePresetFidelity.EXACT, blackPearl.fidelity)
        assertTrue(blackPearl.transformation.contains("same quantized filters"))
        assertEquals(2, blackPearl.representationVersion)
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
        assertTrue(variant.transformation.contains("fitted the complete source response"))
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
    fun blackPearlFileRepresentationCarriesNonzeroPlaybackGain() {
        val variant = buildFileExportDeviceVariant(profile, ExportDevice.BLACK_PEARL)!!
        assertTrue(variant.content.contains("Preamp: -5.50 dB"))
        assertEquals(DevicePresetFidelity.EXACT, variant.fidelity)
    }

    @Test
    fun textVariantSetCoversAllFileTextAndGraphicTargetsExceptSeparatelyBuiltBlackPearlAndUapp() {
        val variants = buildTextDeviceVariants(blackPearlProfile)
        assertEquals(
            setOf(
                ExportDevice.UNIVERSAL_PARAMETRIC,
                ExportDevice.POWERAMP,
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
        assertEquals("Hardware validation pending", ExportDevice.FIIO_JA11.validationStatus)
        assertEquals("Hardware validation pending", ExportDevice.JCALLY_JM12.validationStatus)
        assertTrue(ExportDevice.UAPP.validationStatus == null)
        assertTrue(ExportDevice.BLACK_PEARL.validationStatus == null)
        assertTrue(ExportDevice.BLACK_PEARL.isHardwareOutput)
        assertTrue(!ExportDevice.EASY_EFFECTS.isHardwareOutput)
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

        assertEquals(32, ExportDevice.EASY_EFFECTS.eqCapabilities!!.maxBands)
        assertEquals(64, ExportDevice.POWERAMP.eqCapabilities!!.maxBands)
        assertEquals(null, ExportDevice.EQUALIZER_APO.eqCapabilities!!.maxBands)
        assertEquals(null, ExportDevice.UNIVERSAL_PARAMETRIC.eqCapabilities!!.maxBands)
    }

    @Test
    fun deviceBandLimitCanGrowWithoutChangingCanonicalSourceProfile() {
        val sourceBands = (1..12).map { index ->
            OpraBand("peak_dip", 100.0 * index, index / 10.0, 1.0, null)
        }
        val source = profile.copy(bands = sourceBands)
        val current = ExportDevice.TOPPING_DX5_II.eqCapabilities!!
        val futureCapabilities = current.copy(maxBands = 12)

        val currentOutput = formatToppingTunePreset(source, current)!!
        val futureOutput = formatToppingTunePreset(source, futureCapabilities)!!

        assertEquals(10, filterLines(currentOutput).size)
        assertEquals(12, filterLines(futureOutput).size)
        assertEquals(DevicePresetFidelity.OPTIMIZED, determineDeviceFidelity(source, current))
        assertEquals(DevicePresetFidelity.EXACT, determineDeviceFidelity(source, futureCapabilities))
        assertEquals(12, source.bands!!.size)
        assertEquals(sourceBands, source.bands)
    }

    @Test
    fun deviceBandLimitCanShrinkWithoutChangingCanonicalSourceProfile() {
        val sourceBands = (1..8).map { index ->
            OpraBand("peak_dip", 200.0 * index, -index / 10.0, 1.0, null)
        }
        val source = profile.copy(bands = sourceBands)
        val futureCapabilities = ExportDevice.TOPPING_DX5_II.eqCapabilities!!.copy(maxBands = 6)

        val output = formatToppingTunePreset(source, futureCapabilities)!!

        assertEquals(6, filterLines(output).size)
        assertEquals(DevicePresetFidelity.OPTIMIZED, determineDeviceFidelity(source, futureCapabilities))
        assertEquals(8, source.bands!!.size)
        assertEquals(sourceBands, source.bands)
    }

    @Test
    fun deviceCapabilityRangesDriveOutputClampingAndOptimizedLabel() {
        val source = profile.copy(
            preampGainDb = -15.0,
            bands = listOf(OpraBand("peak_dip", 15.0, 15.0, 20.0, null)),
        )
        val capabilities = DeviceEqCapabilities(
            maxBands = 4,
            supportedBandTypes = setOf("peak_dip"),
            minFrequencyHz = 30.0,
            maxFrequencyHz = 18_000.0,
            minGainDb = -6.0,
            maxGainDb = 6.0,
            minQ = 0.2,
            maxQ = 8.0,
            minPreampDb = -10.0,
            maxPreampDb = 6.0,
        )

        val output = formatToppingTunePreset(source, capabilities)!!

        assertTrue(output.contains("Preamp: -10.00 dB"))
        assertTrue(output.contains("Fc 30 Hz"))
        assertTrue(output.contains("Gain 6.00 dB"))
        assertTrue(output.contains("Q 8.000"))
        assertEquals(DevicePresetFidelity.OPTIMIZED, determineDeviceFidelity(source, capabilities))
    }

    @Test
    fun deviceCanDeclareNoFixedBandLimit() {
        val source = profile.copy(
            bands = (1..14).map { index ->
                OpraBand("peak_dip", 100.0 * index, 0.5, 1.0, null)
            },
        )
        val capabilities = ExportDevice.TOPPING_DX5_II.eqCapabilities!!.copy(maxBands = null)

        val output = formatToppingTunePreset(source, capabilities)!!

        assertEquals(14, filterLines(output).size)
        assertEquals(DevicePresetFidelity.EXACT, determineDeviceFidelity(source, capabilities))
    }

    @Test
    fun missingSourcePreampIsNeverMisrepresentedAsExactByLegacyToppingFormatter() {
        val source = profile.copy(
            preampGainDb = null,
            bands = listOf(OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null)),
        )

        val variant = buildTextDeviceVariant(source, ExportDevice.TOPPING_DX5_II)!!

        assertEquals(DevicePresetFidelity.OPTIMIZED, variant.fidelity)
        assertTrue(variant.transformation.contains("EQ Library optimized conversion"))
    }

    @Test
    fun generatedSafetyHeadroomMetadataRemainsAvailableToLegacyTargetsThatUseIt() {
        val source = profile.copy(
            preampGainDb = null,
            eqLibrarySafetyHeadroomDb = -6.75,
            bands = listOf(OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null)),
        )

        val topping = buildTextDeviceVariant(source, ExportDevice.TOPPING_DX5_II)!!

        assertTrue(topping.content.contains("Preamp: -6.75 dB"))
        assertEquals(DevicePresetFidelity.OPTIMIZED, topping.fidelity)
        assertEquals(null, source.preampGainDb)
    }

    @Test
    fun generatedHardwareHeadroomUsesFinalBlackPearlResponseNotStoredCanonicalHint() {
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
        assertEquals(DevicePresetFidelity.EXACT, blackPearl.fidelity)
        assertEquals(null, source.preampGainDb)
        assertEquals(-9.0, source.eqLibrarySafetyHeadroomDb!!, 0.0)
    }

    private fun filterLines(content: String): List<String> =
        content.lineSequence().filter { it.startsWith("Filter ") }.toList()
}
