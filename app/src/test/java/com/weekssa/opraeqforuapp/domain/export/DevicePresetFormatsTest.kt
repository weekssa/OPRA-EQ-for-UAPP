package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            OpraBand("low_shelf", 105.0, 4.0, 0.71, null),
            OpraBand("peak_dip", 1_000.0, -2.5, 1.2, null),
            OpraBand("high_shelf", 8_000.0, -1.5, 0.71, null),
        ),
    )

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
    fun blackPearlContainsOnlyPeakFiltersAndHonorsItsCurrentCapabilityLimit() {
        val variants = buildTextDeviceVariants(profile)
        val blackPearl = variants.single { it.device == ExportDevice.BLACK_PEARL }
        val filterLines = filterLines(blackPearl.content)
        assertFalse(filterLines.isEmpty())
        assertTrue(filterLines.size <= ExportDevice.BLACK_PEARL.eqCapabilities!!.maxBands!!)
        assertTrue(filterLines.all { it.contains(" ON PK ") })
        assertFalse(blackPearl.content.contains(" LSC "))
        assertFalse(blackPearl.content.contains(" HSC "))
        assertEquals(DevicePresetFidelity.OPTIMIZED, blackPearl.fidelity)
        assertTrue(blackPearl.transformation.contains("EQ Library optimized conversion"))
    }

    @Test
    fun oneSourceProfileProducesThreeTextDeviceVariants() {
        val variants = buildTextDeviceVariants(profile)
        assertEquals(
            setOf(ExportDevice.BLACK_PEARL, ExportDevice.TOPPING_DX5_II, ExportDevice.TOPPING_DX1_II),
            variants.mapTo(mutableSetOf()) { it.device },
        )
    }

    @Test
    fun singleDeviceFormatterReturnsOnlyTheRequestedTarget() {
        val blackPearl = buildTextDeviceVariant(profile, ExportDevice.BLACK_PEARL)
        assertEquals(ExportDevice.BLACK_PEARL, blackPearl?.device)
        assertTrue(buildTextDeviceVariant(profile, ExportDevice.UAPP) == null)
    }

    @Test
    fun toppingTargetsRemainAvailableButAreMarkedUntested() {
        assertEquals("Untested", ExportDevice.TOPPING_DX5_II.validationStatus)
        assertEquals("Untested", ExportDevice.TOPPING_DX1_II.validationStatus)
        assertTrue(ExportDevice.UAPP.validationStatus == null)
        assertTrue(ExportDevice.BLACK_PEARL.validationStatus == null)
    }

    @Test
    fun everyExportTargetDeclaresEqCapabilities() {
        assertTrue(ExportDevice.entries.all { it.eqCapabilities != null })

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
    fun missingSourcePreampIsNeverMisrepresentedAsExact() {
        val source = profile.copy(
            preampGainDb = null,
            bands = listOf(OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null)),
        )

        val variant = buildTextDeviceVariant(source, ExportDevice.TOPPING_DX5_II)!!

        assertEquals(DevicePresetFidelity.OPTIMIZED, variant.fidelity)
        assertTrue(variant.transformation.contains("EQ Library optimized conversion"))
    }

    @Test
    fun unsupportedFilterTypeIsNeverMisrepresentedAsExact() {
        val source = profile.copy(
            bands = listOf(OpraBand("low_shelf", 100.0, 3.0, 0.71, null)),
        )

        val variant = buildTextDeviceVariant(source, ExportDevice.BLACK_PEARL)!!

        assertEquals(DevicePresetFidelity.OPTIMIZED, variant.fidelity)
    }

    private fun filterLines(content: String): List<String> =
        content.lineSequence().filter { it.startsWith("Filter ") }.toList()
}
