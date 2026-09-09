package com.weekssa.opraeqforuapp.domain.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import org.junit.Test

class HardwareEqResponseTest {
    @Test
    fun emptyHardwareEqRendersFlatLogarithmicCurve() {
        val curve = requireNotNull(HardwareEqResponseEvaluator.evaluate(emptyList()))

        assertThat(curve.points).hasSize(HardwareEqResponseEvaluator.DEFAULT_POINT_COUNT)
        assertThat(curve.points.first().frequencyHz)
            .isWithin(1e-9)
            .of(HardwareEqResponseEvaluator.MIN_FREQUENCY_HZ)
        assertThat(curve.points.last().frequencyHz)
            .isWithin(1e-6)
            .of(HardwareEqResponseEvaluator.MAX_FREQUENCY_HZ)
        assertThat(curve.points.all { point -> kotlin.math.abs(point.gainDb) <= 1e-9 }).isTrue()
    }

    @Test
    fun peakFilterProducesItsGainAtCenterFrequency() {
        val filter = hardwareFilter(
            type = EqFilterType.PEAK,
            frequencyHz = 1_000.0,
            gainDb = 6.0,
            q = 1.0,
        )
        val curve = requireNotNull(HardwareEqResponseEvaluator.evaluate(listOf(filter), pointCount = 256))

        assertThat(requireNotNull(curve.gainDbAt(1_000.0))).isWithin(0.02).of(6.0)
        assertThat(curve.maximumGainDb).isGreaterThan(5.9)
    }

    @Test
    fun enabledFiltersCombineInDb() {
        val first = hardwareFilter(
            index = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 1_000.0,
            gainDb = 3.0,
            q = 1.0,
        )
        val second = first.copy(index = 1)
        val curve = requireNotNull(HardwareEqResponseEvaluator.evaluate(listOf(first, second), pointCount = 256))

        assertThat(requireNotNull(curve.gainDbAt(1_000.0))).isWithin(0.02).of(6.0)
    }

    @Test
    fun disabledUnsupportedFilterDoesNotAlterCurve() {
        val disabledUnsupported = hardwareFilter(
            type = EqFilterType.LOW_PASS,
            enabled = false,
            gainDb = 0.0,
        )
        val curve = requireNotNull(HardwareEqResponseEvaluator.evaluate(listOf(disabledUnsupported)))

        assertThat(curve.peakAbsoluteGainDb).isWithin(1e-9).of(0.0)
    }

    @Test
    fun enabledUnsupportedFilterFailsInsteadOfDisappearing() {
        val unsupported = hardwareFilter(
            type = EqFilterType.LOW_PASS,
            enabled = true,
            gainDb = 0.0,
        )

        assertThat(HardwareEqResponseEvaluator.evaluate(listOf(unsupported))).isNull()
    }

    @Test
    fun onlyNonFlatSupportedEnabledBandsAreMarkerWorthy() {
        assertThat(hardwareFilter(gainDb = 1.0).isAcousticallyActive()).isTrue()
        assertThat(hardwareFilter(gainDb = 0.0).isAcousticallyActive()).isFalse()
        assertThat(hardwareFilter(gainDb = 1.0, enabled = false).isAcousticallyActive()).isFalse()
        assertThat(hardwareFilter(type = EqFilterType.OTHER, gainDb = 1.0).isAcousticallyActive()).isFalse()
    }

    @Test
    fun logarithmicInterpolationPreservesEndpoints() {
        val curve = HardwareEqResponseCurve(
            listOf(
                HardwareEqResponsePoint(20.0, -2.0),
                HardwareEqResponsePoint(200.0, 2.0),
                HardwareEqResponsePoint(2_000.0, 6.0),
            ),
        )

        assertThat(requireNotNull(curve.gainDbAt(10.0))).isWithin(1e-9).of(-2.0)
        assertThat(requireNotNull(curve.gainDbAt(20_000.0))).isWithin(1e-9).of(6.0)
        assertThat(requireNotNull(curve.gainDbAt(200.0))).isWithin(1e-9).of(2.0)
    }

    private fun hardwareFilter(
        index: Int = 0,
        enabled: Boolean = true,
        type: EqFilterType = EqFilterType.PEAK,
        frequencyHz: Double = 1_000.0,
        gainDb: Double = 1.0,
        q: Double = 1.0,
    ): HardwareEqFilter = HardwareEqFilter(
        index = index,
        enabled = enabled,
        type = type,
        frequencyHz = frequencyHz,
        gainDb = gainDb,
        q = q,
    )
}
