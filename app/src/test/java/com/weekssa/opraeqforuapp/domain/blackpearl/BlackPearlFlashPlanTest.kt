package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlFlashPlanTest {
    @Test
    fun zeroPreampPeakAndShelvesProduceExactEqOnlyFlashPlan() {
        val profile = profile(
            preamp = 0.0,
            bands = listOf(
                OpraBand("low_shelf", 105.0, 4.0, 0.71, null),
                OpraBand("peak_dip", 1_000.0, -2.5, 1.2, null),
                OpraBand("high_shelf", 8_000.0, -1.5, 0.71, null),
            ),
        )

        val plan = buildBlackPearlFlashPlan(profile, activeSlot = 0x05)
        assertTrue(plan is BlackPearlFlashPlan.Ready)
        plan as BlackPearlFlashPlan.Ready
        assertEquals(DevicePresetFidelity.EXACT, plan.fidelity)
        assertEquals(0, plan.omittedBandCount)
        assertEquals(12, plan.reports.size)
        assertTrue(plan.reports.none { it[2].u8() == 0x03 })
    }

    @Test
    fun nonzeroSourcePreampIsRejectedRatherThanChangingGlobalVolume() {
        val plan = buildBlackPearlFlashPlan(profile(preamp = -6.0), activeSlot = 0x00)

        assertTrue(plan is BlackPearlFlashPlan.NotRepresentable)
        val reason = (plan as BlackPearlFlashPlan.NotRepresentable).reason
        assertTrue(reason.contains("will not change global DAC volume"))
    }

    @Test
    fun generatedSafetyHeadroomIsAlsoRejectedForDirectFlash() {
        val source = profile(preamp = null).copy(eqLibrarySafetyHeadroomDb = -4.5)
        val plan = buildBlackPearlFlashPlan(source, activeSlot = 0x00)

        assertTrue(plan is BlackPearlFlashPlan.NotRepresentable)
        assertTrue((plan as BlackPearlFlashPlan.NotRepresentable).reason.contains("preamp/headroom"))
        assertEquals(null, source.preampGainDb)
        assertEquals(-4.5, source.eqLibrarySafetyHeadroomDb!!, 0.0)
    }

    @Test
    fun moreThanTenBandsUsesFirstTenInSourcePriorityOrderAndWarns() {
        val bands = (1..12).map { index ->
            OpraBand("peak_dip", index * 100.0, index / 10.0, 1.0, null)
        }
        val source = profile(preamp = 0.0, bands = bands)

        val plan = buildBlackPearlFlashPlan(source, activeSlot = 0x02) as BlackPearlFlashPlan.Ready

        assertEquals(DevicePresetFidelity.OPTIMIZED, plan.fidelity)
        assertEquals(2, plan.omittedBandCount)
        assertTrue(plan.warning.orEmpty().contains("first 10 source-priority bands"))
        assertEquals(12, source.bands!!.size)
        assertEquals(bands, source.bands)
    }

    @Test
    fun unsupportedOrOutOfRangeBandIsNotSilentlyAltered() {
        val unsupported = buildBlackPearlFlashPlan(
            profile(preamp = 0.0, bands = listOf(OpraBand("low_pass", 1_000.0, 0.0, 1.0, null))),
            activeSlot = 0x00,
        )
        val outOfRange = buildBlackPearlFlashPlan(
            profile(preamp = 0.0, bands = listOf(OpraBand("peak_dip", 1_000.0, 12.0, 1.0, null))),
            activeSlot = 0x00,
        )

        assertTrue(unsupported is BlackPearlFlashPlan.NotRepresentable)
        assertTrue(outOfRange is BlackPearlFlashPlan.NotRepresentable)
    }

    private fun profile(
        preamp: Double?,
        bands: List<OpraBand> = listOf(OpraBand("peak_dip", 1_000.0, 0.0, 1.0, null)),
    ) = OpraEqProfile(
        id = "black-pearl-test",
        productId = "product",
        author = "Tester",
        details = "Test",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = preamp,
        bands = bands,
    )

    private fun Byte.u8(): Int = toInt() and 0xFF
}
