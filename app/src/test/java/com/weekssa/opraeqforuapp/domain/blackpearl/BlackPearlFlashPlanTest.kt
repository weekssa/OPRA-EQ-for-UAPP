package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlFlashPlanTest {
    @Test
    fun nativeQuantizedPeakAndShelvesProduceExactEqPlan() {
        val profile = profile(
            preamp = 0.0,
            bands = listOf(
                OpraBand("low_shelf", 105.0, 4.0, 0.75, null),
                OpraBand("peak_dip", 1_000.0, -2.5, 1.25, null),
                OpraBand("high_shelf", 8_000.0, -1.5, 0.75, null),
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
    fun generatedHeadroomIsDerivedFromFinalHardwareResponseWithoutMutatingCanonicalMetadata() {
        val source = profile(
            preamp = null,
            bands = listOf(OpraBand("peak_dip", 1_000.0, 4.0, 1.0, null)),
        ).copy(eqLibrarySafetyHeadroomDb = -9.0)

        val plan = buildBlackPearlFlashPlan(source, activeSlot = 0x00)

        assertTrue(plan is BlackPearlFlashPlan.Ready)
        plan as BlackPearlFlashPlan.Ready
        assertEquals(DevicePresetFidelity.OPTIMIZED, plan.fidelity)
        assertTrue(plan.warning.orEmpty().contains("generated headroom"))
        assertTrue(plan.requiredPlaybackGainDb <= -3.9)
        assertTrue(plan.requiredPlaybackGainDb >= -4.1)
        assertEquals(null, source.preampGainDb)
        assertEquals(-9.0, source.eqLibrarySafetyHeadroomDb!!, 0.0)
    }

    @Test
    fun missingSourcePreampAndStoredHeadroomStillGetsSafeDerivedHardwareHeadroom() {
        val source = profile(
            preamp = null,
            bands = listOf(OpraBand("peak_dip", 1_000.0, 2.0, 1.0, null)),
        ).copy(eqLibrarySafetyHeadroomDb = null)

        val plan = buildBlackPearlFlashPlan(source, activeSlot = 0x00)

        assertTrue(plan is BlackPearlFlashPlan.Ready)
        plan as BlackPearlFlashPlan.Ready
        assertEquals(DevicePresetFidelity.OPTIMIZED, plan.fidelity)
        assertTrue(plan.warning.orEmpty().contains("generated headroom"))
        assertTrue(plan.requiredPlaybackGainDb <= -1.9)
        assertTrue(plan.requiredPlaybackGainDb >= -2.1)
        assertEquals(null, source.preampGainDb)
        assertEquals(null, source.eqLibrarySafetyHeadroomDb)
    }

    @Test
    fun moreThanTenBandsFitsCompleteResponseAndDoesNotMutateSource() {
        val bands = (1..12).map { index ->
            OpraBand("peak_dip", index * 100.0, index / 10.0, 1.0, null)
        }
        val source = profile(preamp = 0.0, bands = bands)

        val plan = buildBlackPearlFlashPlan(source, activeSlot = 0x02) as BlackPearlFlashPlan.Ready

        assertEquals(DevicePresetFidelity.OPTIMIZED, plan.fidelity)
        assertEquals(2, plan.omittedBandCount)
        assertTrue(plan.warning.orEmpty().contains("12 → 10 bands · full-response fit"))
        assertTrue(plan.rmsErrorDb >= 0.0)
        assertTrue(plan.maxAbsoluteErrorDb >= 0.0)
        assertEquals(12, source.bands!!.size)
        assertEquals(bands, source.bands)
    }

    @Test
    fun protocolEncodableGainOutsideValidatedRangeIsReadyWithExplicitCaution() {
        val source = profile(
            preamp = -4.0,
            bands = listOf(OpraBand("peak_dip", 13_500.0, -12.0, 4.0, null)),
        )

        val plan = buildBlackPearlFlashPlan(source, activeSlot = 0x00)

        assertTrue(plan is BlackPearlFlashPlan.Ready)
        plan as BlackPearlFlashPlan.Ready
        assertEquals(DevicePresetFidelity.EXACT, plan.fidelity)
        assertTrue(plan.warning.orEmpty().contains("Band 1 -12.00 dB"))
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
