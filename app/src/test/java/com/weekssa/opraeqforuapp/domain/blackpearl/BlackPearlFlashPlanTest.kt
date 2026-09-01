package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlFlashPlanTest {
    @Test
    fun zeroPreampPeakAndShelvesProduceExactEqPlan() {
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
        assertEquals(0.0, plan.requiredPlaybackGainDb, 0.0)
        assertEquals(0, plan.omittedBandCount)
        assertEquals(null, plan.warning)
        assertEquals(12, plan.reports.size)
        assertTrue(plan.reports.none { it[2].u8() == 0x03 })
    }

    @Test
    fun nonzeroSourcePreampIsCarriedIntoFlashPlan() {
        val plan = buildBlackPearlFlashPlan(profile(preamp = -6.0), activeSlot = 0x00)

        assertTrue(plan is BlackPearlFlashPlan.Ready)
        assertEquals(-6.0, (plan as BlackPearlFlashPlan.Ready).requiredPlaybackGainDb, 0.0)
    }

    @Test
    fun generatedSafetyHeadroomIsCarriedSeparatelyIntoFlashPlan() {
        val source = profile(preamp = null).copy(eqLibrarySafetyHeadroomDb = -4.5)
        val plan = buildBlackPearlFlashPlan(source, activeSlot = 0x00)

        assertTrue(plan is BlackPearlFlashPlan.Ready)
        assertEquals(-4.5, (plan as BlackPearlFlashPlan.Ready).requiredPlaybackGainDb, 0.0)
        assertEquals(null, source.preampGainDb)
        assertEquals(-4.5, source.eqLibrarySafetyHeadroomDb!!, 0.0)
    }

    @Test
    fun missingBothSourcePreampAndGeneratedHeadroomIsRejected() {
        val source = profile(preamp = null).copy(eqLibrarySafetyHeadroomDb = null)

        val plan = buildBlackPearlFlashPlan(source, activeSlot = 0x00)

        assertTrue(plan is BlackPearlFlashPlan.NotRepresentable)
        assertTrue((plan as BlackPearlFlashPlan.NotRepresentable).reason.contains("cannot determine"))
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
    fun protocolEncodableGainOutsideValidatedRangeIsReadyWithExplicitCaution() {
        val source = profile(
            preamp = -3.9,
            bands = listOf(OpraBand("peak_dip", 13_500.0, -11.9, 4.0, null)),
        )

        val plan = buildBlackPearlFlashPlan(source, activeSlot = 0x00)

        assertTrue(plan is BlackPearlFlashPlan.Ready)
        plan as BlackPearlFlashPlan.Ready
        assertEquals(DevicePresetFidelity.EXACT, plan.fidelity)
        assertTrue(plan.warning.orEmpty().contains("Band 1 -11.90 dB"))
        assertTrue(plan.warning.orEmpty().contains("outside EQ Library's currently validated"))
        assertTrue(plan.warning.orEmpty().contains("sent unchanged"))
        assertTrue(plan.warning.orEmpty().contains("not be clamped"))
    }

    @Test
    fun unsupportedOrTrulyUnrepresentableBandIsStillRejected() {
        val unsupported = buildBlackPearlFlashPlan(
            profile(preamp = 0.0, bands = listOf(OpraBand("low_pass", 1_000.0, 0.0, 1.0, null))),
            activeSlot = 0x00,
        )
        val unencodableGain = buildBlackPearlFlashPlan(
            profile(preamp = 0.0, bands = listOf(OpraBand("peak_dip", 1_000.0, 200.0, 1.0, null))),
            activeSlot = 0x00,
        )

        assertTrue(unsupported is BlackPearlFlashPlan.NotRepresentable)
        assertTrue(unencodableGain is BlackPearlFlashPlan.NotRepresentable)
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
