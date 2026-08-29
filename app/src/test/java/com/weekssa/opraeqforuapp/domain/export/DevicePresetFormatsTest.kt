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
    }

    @Test
    fun blackPearlContainsOnlyPeakFiltersAndNoMoreThanTenBands() {
        val variants = buildTextDeviceVariants(profile)
        val blackPearl = variants.single { it.device == ExportDevice.BLACK_PEARL }
        val filterLines = blackPearl.content.lineSequence().filter { it.startsWith("Filter ") }.toList()
        assertFalse(filterLines.isEmpty())
        assertTrue(filterLines.size <= 10)
        assertTrue(filterLines.all { it.contains(" ON PK ") })
        assertFalse(blackPearl.content.contains(" LSC "))
        assertFalse(blackPearl.content.contains(" HSC "))
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
}
