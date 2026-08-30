package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceExportabilityTest {
    @Test
    fun `UAPP reports exact for a source that fits current capabilities`() {
        assertEquals(DeviceExportability.EXACT, assessDeviceExportability(profile(), ExportDevice.UAPP))
    }

    @Test
    fun `UAPP reports optimized when canonical source exceeds ten bands`() {
        val source = profile().copy(
            bands = (1..14).map { index -> OpraBand("peak_dip", 100.0 * index, 0.5, 1.0, null) },
        )

        assertEquals(DeviceExportability.OPTIMIZED, assessDeviceExportability(source, ExportDevice.UAPP))
        assertEquals(14, source.bands?.size)
    }

    @Test
    fun `unsupported source can be not representable for every selected target`() {
        val source = profile().copy(
            bands = listOf(OpraBand("low_pass", 8_000.0, 0.0, 0.7, null)),
        )

        assertEquals(DeviceExportability.NOT_REPRESENTABLE, assessDeviceExportability(source, ExportDevice.UAPP))
        assertEquals(DeviceExportability.NOT_REPRESENTABLE, assessDeviceExportability(source, ExportDevice.BLACK_PEARL))
        assertFalse(source.isExportableToAny(setOf(ExportDevice.UAPP, ExportDevice.BLACK_PEARL)))
    }

    @Test
    fun `one usable selected target keeps a profile exportable`() {
        val source = profile().copy(
            bands = listOf(OpraBand("low_shelf", 105.0, 3.0, 0.71, null)),
        )

        assertEquals(DeviceExportability.OPTIMIZED, assessDeviceExportability(source, ExportDevice.BLACK_PEARL))
        assertTrue(source.isExportableToAny(setOf(ExportDevice.BLACK_PEARL)))
    }

    private fun profile() = OpraEqProfile(
        id = "profile-1",
        productId = "product-1",
        author = "Tester",
        details = "Target",
        link = "https://example.com/source",
        profileType = "parametric_eq",
        preampGainDb = -3.0,
        bands = listOf(OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null)),
    )
}
