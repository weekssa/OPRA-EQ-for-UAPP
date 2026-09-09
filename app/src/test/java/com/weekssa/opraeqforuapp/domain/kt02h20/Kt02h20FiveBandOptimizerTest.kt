package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Kt02h20FiveBandOptimizerTest {
    @Test
    fun exactJa11ProfilePassesThroughWithoutAcousticAlteration() {
        val source = profile(
            preamp = -5.5,
            bands = listOf(
                band("low_shelf", 105.0, 4.0, 0.71),
                band("peak_dip", 1_000.0, -2.5, 1.2),
                band("high_shelf", 8_000.0, -1.5, 0.71),
            ),
        )

        val result = Kt02h20FiveBandOptimizer.optimize(source, Kt02h20DeviceSpecs.FIIO_JA11)
            as FiveBandOptimizationResult.Ready

        assertEquals(DevicePresetFidelity.EXACT, result.representation.fidelity)
        assertEquals(-5.5, result.representation.playbackGainDb, 0.0)
        assertEquals(3, result.representation.bands.size)
        assertEquals(105.0, result.representation.bands[0].frequencyHz, 0.0)
        assertEquals(4.0, result.representation.bands[0].gainDb, 0.0)
        assertEquals(0.0, result.representation.rmsErrorDb, 0.0)
        assertEquals(0.0, result.representation.maxAbsoluteErrorDb, 0.0)
        assertEquals("source values preserved", result.representation.adaptationSummary())
        assertEquals(3, source.bands!!.size)
    }

    @Test
    fun nativeHardwareRoundingKeepsSourceStructureWithoutInvokingResponseFit() {
        val source = profile(
            preamp = -3.0,
            bands = listOf(
                band("peak_dip", 1_000.4, 2.001, 1.002),
                band("high_shelf", 8_000.4, -1.001, 0.702),
            ),
        )

        val result = Kt02h20FiveBandOptimizer.optimize(source, HardwareEqDeviceSpecs.TRN_BLACK_PEARL)
            as FiveBandOptimizationResult.Ready

        assertEquals(DevicePresetFidelity.OPTIMIZED, result.representation.fidelity)
        assertEquals(2, result.representation.sourceBandCount)
        assertEquals(2, result.representation.bands.size)
        assertTrue(result.representation.usesNativeQuantization)
        assertFalse(result.representation.usedResponseFit)
        assertFalse(result.representation.usesGeneratedHeadroom)
        assertEquals("native hardware rounding only", result.representation.adaptationSummary())
        val sourceBands = requireNotNull(source.bands)
        assertEquals(2, sourceBands.size)
        assertEquals(1_000.4, sourceBands[0].frequency!!, 0.0)
    }

    @Test
    fun sixBandSourceIsDeterministicallyAdaptedInsteadOfTruncated() {
        val sourceBands = listOf(
            band("low_shelf", 80.0, 3.0, 0.7),
            band("peak_dip", 300.0, -1.0, 1.0),
            band("peak_dip", 1_000.0, 2.0, 1.2),
            band("peak_dip", 3_000.0, -2.0, 1.5),
            band("high_shelf", 9_000.0, 1.5, 0.8),
            band("peak_dip", 15_000.0, 0.0, 2.0),
        )
        val source = profile(preamp = -3.0, bands = sourceBands)

        Kt02h20FiveBandOptimizer.clearCache()
        val first = Kt02h20FiveBandOptimizer.optimize(source, Kt02h20DeviceSpecs.FIIO_JA11)
            as FiveBandOptimizationResult.Ready
        Kt02h20FiveBandOptimizer.clearCache()
        val second = Kt02h20FiveBandOptimizer.optimize(source, Kt02h20DeviceSpecs.FIIO_JA11)
            as FiveBandOptimizationResult.Ready

        assertEquals(DevicePresetFidelity.OPTIMIZED, first.representation.fidelity)
        assertTrue(first.representation.bands.size <= 5)
        assertEquals(6, first.representation.sourceBandCount)
        assertTrue(first.representation.usedResponseFit)
        assertTrue(first.representation.adaptationSummary().contains("6 →"))
        assertTrue(first.representation.adaptationSummary().contains("full-response fit"))
        assertEquals(first.representation, second.representation)
        assertTrue(first.representation.rmsErrorDb <= Kt02h20DeviceSpecs.FIIO_JA11.maxRmsErrorDb)
        assertTrue(first.representation.maxAbsoluteErrorDb <= Kt02h20DeviceSpecs.FIIO_JA11.maxAbsoluteErrorDb)
        assertEquals(sourceBands, source.bands)
    }

    @Test
    fun jm12PreampIsQuantizedToHalfDbWithoutChangingCanonicalSource() {
        val source = profile(
            preamp = -3.24,
            bands = listOf(band("peak_dip", 1_000.0, -2.0, 1.0)),
        )

        val result = Kt02h20FiveBandOptimizer.optimize(source, Kt02h20DeviceSpecs.JCALLY_JM12_STOCK)
            as FiveBandOptimizationResult.Ready

        assertEquals(DevicePresetFidelity.OPTIMIZED, result.representation.fidelity)
        assertEquals(-3.0, result.representation.playbackGainDb, 0.0)
        assertTrue(result.representation.usesNativeQuantization)
        assertFalse(result.representation.usedResponseFit)
        assertEquals("native hardware rounding only", result.representation.adaptationSummary())
        assertEquals(-3.24, source.preampGainDb!!, 0.0)
    }

    @Test
    fun unsupportedFilterTypeIsNotSuitableRatherThanIgnored() {
        val source = profile(
            preamp = -3.0,
            bands = listOf(band("band_pass", 1_000.0, 2.0, 1.0)),
        )

        val result = Kt02h20FiveBandOptimizer.optimize(source, Kt02h20DeviceSpecs.FIIO_JA11)

        assertTrue(result is FiveBandOptimizationResult.NotSuitable)
        assertTrue((result as FiveBandOptimizationResult.NotSuitable).reason.contains("cannot represent"))
    }

    @Test
    fun missingSourcePreampGeneratesSafeHeadroomFromFinalTargetResponseAndIsOptimized() {
        val source = profile(
            preamp = null,
            bands = listOf(band("peak_dip", 1_000.0, 2.0, 1.0)),
        )

        val result = Kt02h20FiveBandOptimizer.optimize(source, Kt02h20DeviceSpecs.FIIO_JA11)
            as FiveBandOptimizationResult.Ready

        assertEquals(DevicePresetFidelity.OPTIMIZED, result.representation.fidelity)
        assertTrue(result.representation.usesGeneratedHeadroom)
        assertFalse(result.representation.usedResponseFit)
        assertTrue(result.representation.adaptationSummary().startsWith("generated headroom"))
        assertTrue(result.representation.playbackGainDb <= -1.9)
        assertTrue(result.representation.playbackGainDb >= -2.1)
        assertEquals(null, source.preampGainDb)
        assertEquals(null, source.eqLibrarySafetyHeadroomDb)
    }

    @Test
    fun generatedHeadroomIgnoresStoredCanonicalHintAndLeavesCanonicalMetadataUntouched() {
        val withoutHint = profile(
            preamp = null,
            bands = listOf(band("peak_dip", 1_000.0, 2.0, 1.0)),
        )
        val withStaleHint = withoutHint.copy(eqLibrarySafetyHeadroomDb = -9.5)

        Kt02h20FiveBandOptimizer.clearCache()
        val first = Kt02h20FiveBandOptimizer.optimize(withoutHint, Kt02h20DeviceSpecs.FIIO_JA11)
            as FiveBandOptimizationResult.Ready
        Kt02h20FiveBandOptimizer.clearCache()
        val second = Kt02h20FiveBandOptimizer.optimize(withStaleHint, Kt02h20DeviceSpecs.FIIO_JA11)
            as FiveBandOptimizationResult.Ready

        assertEquals(DevicePresetFidelity.OPTIMIZED, second.representation.fidelity)
        assertEquals(first.representation.playbackGainDb, second.representation.playbackGainDb, 0.0)
        assertTrue(second.representation.usesGeneratedHeadroom)
        assertEquals(null, withStaleHint.preampGainDb)
        assertEquals(-9.5, withStaleHint.eqLibrarySafetyHeadroomDb!!, 0.0)
    }

    private fun profile(preamp: Double?, bands: List<OpraBand>): OpraEqProfile = OpraEqProfile(
        id = "optimizer-test",
        productId = "product",
        author = "Test",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = preamp,
        bands = bands,
    )

    private fun band(type: String, frequency: Double, gain: Double, q: Double): OpraBand =
        OpraBand(type, frequency, gain, q, null)
}
