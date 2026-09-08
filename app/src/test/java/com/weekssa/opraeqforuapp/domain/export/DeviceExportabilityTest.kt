package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `output unsupported source can remain saved but not representable for selected targets`() {
        val source = profile().copy(
            bands = listOf(OpraBand("low_pass", 8_000.0, 0.0, 0.7, 12.0)),
        )

        assertEquals(DeviceExportability.NOT_REPRESENTABLE, assessDeviceExportability(source, ExportDevice.UAPP))
        assertEquals(DeviceExportability.NOT_REPRESENTABLE, assessDeviceExportability(source, ExportDevice.BLACK_PEARL))
        assertFalse(source.isExportableToAny(setOf(ExportDevice.UAPP, ExportDevice.BLACK_PEARL)))
    }

    @Test
    fun `Black Pearl native shelf is exact when parameters are at device quantization`() {
        val source = profile().copy(
            preampGainDb = 0.0,
            bands = listOf(OpraBand("low_shelf", 105.0, 3.0, 0.75, null)),
        )

        assertEquals(DeviceExportability.EXACT, assessDeviceExportability(source, ExportDevice.BLACK_PEARL))
        assertTrue(source.isExportableToAny(setOf(ExportDevice.BLACK_PEARL)))
    }

    @Test
    fun `Black Pearl file export preserves a nonzero source preamp`() {
        assertEquals(
            DeviceExportability.EXACT,
            assessDeviceExportability(profile(), ExportDevice.BLACK_PEARL),
        )
    }

    @Test
    fun `Black Pearl file export keeps exact protocol gain outside currently validated range`() {
        val source = profile().copy(
            preampGainDb = -4.0,
            bands = listOf(OpraBand("peak_dip", 13_500.0, -12.0, 4.0, null)),
        )

        assertEquals(DeviceExportability.EXACT, assessDeviceExportability(source, ExportDevice.BLACK_PEARL))
        assertTrue(source.isExportableToAny(setOf(ExportDevice.BLACK_PEARL)))
    }

    @Test
    fun `Black Pearl derives missing playback headroom as optimized target adaptation`() {
        val source = profile().copy(
            preampGainDb = null,
            eqLibrarySafetyHeadroomDb = -9.0,
            bands = listOf(OpraBand("peak_dip", 1_000.0, 4.0, 1.0, null)),
        )

        assertEquals(
            DeviceExportability.OPTIMIZED,
            assessDeviceExportability(source, ExportDevice.BLACK_PEARL),
        )
        assertEquals(null, source.preampGainDb)
        assertEquals(-9.0, source.eqLibrarySafetyHeadroomDb!!, 0.0)
    }

    @Test
    fun `new registry outputs expose representability through the same API`() {
        val source = profile()
        assertTrue(assessDeviceExportability(source, ExportDevice.EASY_EFFECTS) != DeviceExportability.NOT_REPRESENTABLE)
        assertTrue(assessDeviceExportability(source, ExportDevice.EQUALIZER_APO) != DeviceExportability.NOT_REPRESENTABLE)
        assertTrue(assessDeviceExportability(source, ExportDevice.UNIVERSAL_GRAPHIC_EQ) != DeviceExportability.NOT_REPRESENTABLE)
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
