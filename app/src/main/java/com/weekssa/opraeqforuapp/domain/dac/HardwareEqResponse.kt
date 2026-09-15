package com.weekssa.opraeqforuapp.domain.dac

import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** One deterministic sample of a device-native parametric EQ response. */
data class HardwareEqResponsePoint(
    val frequencyHz: Double,
    val gainDb: Double,
) {
    init {
        require(frequencyHz.isFinite() && frequencyHz > 0.0) {
            "Response frequency must be finite and positive."
        }
        require(gainDb.isFinite()) { "Response gain must be finite." }
    }
}

/**
 * Device-native PEQ response rendered from verified/planned hardware filter values.
 *
 * This is domain data. Compose may draw it but must not reimplement the filter math.
 */
data class HardwareEqResponseCurve(
    val points: List<HardwareEqResponsePoint>,
) {
    init {
        require(points.size >= 2) { "Hardware EQ response requires at least two samples." }
        require(points.zipWithNext().all { (left, right) -> left.frequencyHz < right.frequencyHz }) {
            "Hardware EQ response frequencies must be strictly increasing."
        }
    }

    val minimumGainDb: Double
        get() = points.minOf(HardwareEqResponsePoint::gainDb)

    val maximumGainDb: Double
        get() = points.maxOf(HardwareEqResponsePoint::gainDb)

    val peakAbsoluteGainDb: Double
        get() = maxOf(abs(minimumGainDb), abs(maximumGainDb))

    fun gainDbAt(frequencyHz: Double): Double? {
        if (!frequencyHz.isFinite() || frequencyHz <= 0.0) return null
        if (frequencyHz <= points.first().frequencyHz) return points.first().gainDb
        if (frequencyHz >= points.last().frequencyHz) return points.last().gainDb

        val upperIndex = points.indexOfFirst { point -> point.frequencyHz >= frequencyHz }
        if (upperIndex <= 0) return null
        val lower = points[upperIndex - 1]
        val upper = points[upperIndex]
        val logLower = ln(lower.frequencyHz)
        val logUpper = ln(upper.frequencyHz)
        val logTarget = ln(frequencyHz)
        val fraction = (logTarget - logLower) / (logUpper - logLower)
        return lower.gainDb + (upper.gainDb - lower.gainDb) * fraction
    }
}

/**
 * Shared deterministic evaluator for My DAC response visualization and later editor/headroom work.
 *
 * The equations intentionally match the established finite-hardware response adapter: 48 kHz
 * reference sample rate, RBJ-style Peak/Low Shelf/High Shelf biquads, and a fixed logarithmic
 * 20 Hz..20 kHz response grid. Unsupported active filter types fail rather than disappearing from
 * the displayed response.
 */
object HardwareEqResponseEvaluator {
    const val SAMPLE_RATE_HZ: Double = 48_000.0
    const val MIN_FREQUENCY_HZ: Double = 20.0
    const val MAX_FREQUENCY_HZ: Double = 20_000.0
    const val DEFAULT_POINT_COUNT: Int = 96

    fun evaluate(
        filters: List<HardwareEqFilter>,
        pointCount: Int = DEFAULT_POINT_COUNT,
    ): HardwareEqResponseCurve? {
        require(pointCount >= 2) { "Hardware EQ response requires at least two samples." }
        val frequencies = logarithmicFrequencyGrid(pointCount)
        val points = ArrayList<HardwareEqResponsePoint>(frequencies.size)
        frequencies.forEach { frequency ->
            var gainDb = 0.0
            filters.forEach { filter ->
                if (!filter.enabled) return@forEach
                gainDb += responseDb(filter, frequency) ?: return null
            }
            if (!gainDb.isFinite()) return null
            points += HardwareEqResponsePoint(frequencyHz = frequency, gainDb = gainDb)
        }
        return HardwareEqResponseCurve(points)
    }

    fun logarithmicFrequencyGrid(
        pointCount: Int = DEFAULT_POINT_COUNT,
    ): DoubleArray {
        require(pointCount >= 2) { "Hardware EQ response requires at least two samples." }
        return DoubleArray(pointCount) { index ->
            val fraction = index.toDouble() / (pointCount - 1).toDouble()
            MIN_FREQUENCY_HZ * (MAX_FREQUENCY_HZ / MIN_FREQUENCY_HZ).pow(fraction)
        }
    }

    private fun responseDb(filter: HardwareEqFilter, frequencyHz: Double): Double? {
        val biquad = biquad(filter) ?: return null
        if (!frequencyHz.isFinite() || frequencyHz <= 0.0 || frequencyHz >= SAMPLE_RATE_HZ / 2.0) return null

        val omega = 2.0 * PI * frequencyHz / SAMPLE_RATE_HZ
        val c1 = cos(omega)
        val s1 = sin(omega)
        val c2 = cos(2.0 * omega)
        val s2 = sin(2.0 * omega)
        val numeratorReal = biquad.b0 + biquad.b1 * c1 + biquad.b2 * c2
        val numeratorImag = -(biquad.b1 * s1 + biquad.b2 * s2)
        val denominatorReal = 1.0 + biquad.a1 * c1 + biquad.a2 * c2
        val denominatorImag = -(biquad.a1 * s1 + biquad.a2 * s2)
        val numeratorSquared = numeratorReal * numeratorReal + numeratorImag * numeratorImag
        val denominatorSquared = denominatorReal * denominatorReal + denominatorImag * denominatorImag
        if (
            numeratorSquared <= 0.0 ||
            denominatorSquared <= 0.0 ||
            !numeratorSquared.isFinite() ||
            !denominatorSquared.isFinite()
        ) {
            return null
        }
        return 10.0 * ln(numeratorSquared / denominatorSquared) / ln(10.0)
    }

    private fun biquad(filter: HardwareEqFilter): Biquad? {
        if (filter.frequencyHz >= SAMPLE_RATE_HZ / 2.0) return null
        val amplitude = 10.0.pow(filter.gainDb / 40.0)
        val omega = 2.0 * PI * filter.frequencyHz / SAMPLE_RATE_HZ
        val cosine = cos(omega)
        val sine = sin(omega)
        val alpha = sine / (2.0 * filter.q)
        val values = when (filter.type) {
            EqFilterType.PEAK -> doubleArrayOf(
                1.0 + alpha * amplitude,
                -2.0 * cosine,
                1.0 - alpha * amplitude,
                1.0 + alpha / amplitude,
                -2.0 * cosine,
                1.0 - alpha / amplitude,
            )

            EqFilterType.LOW_SHELF -> {
                val shelf = 2.0 * sqrt(amplitude) * alpha
                doubleArrayOf(
                    amplitude * ((amplitude + 1.0) - (amplitude - 1.0) * cosine + shelf),
                    2.0 * amplitude * ((amplitude - 1.0) - (amplitude + 1.0) * cosine),
                    amplitude * ((amplitude + 1.0) - (amplitude - 1.0) * cosine - shelf),
                    (amplitude + 1.0) + (amplitude - 1.0) * cosine + shelf,
                    -2.0 * ((amplitude - 1.0) + (amplitude + 1.0) * cosine),
                    (amplitude + 1.0) + (amplitude - 1.0) * cosine - shelf,
                )
            }

            EqFilterType.HIGH_SHELF -> {
                val shelf = 2.0 * sqrt(amplitude) * alpha
                doubleArrayOf(
                    amplitude * ((amplitude + 1.0) + (amplitude - 1.0) * cosine + shelf),
                    -2.0 * amplitude * ((amplitude - 1.0) + (amplitude + 1.0) * cosine),
                    amplitude * ((amplitude + 1.0) + (amplitude - 1.0) * cosine - shelf),
                    (amplitude + 1.0) - (amplitude - 1.0) * cosine + shelf,
                    2.0 * ((amplitude - 1.0) - (amplitude + 1.0) * cosine),
                    (amplitude + 1.0) - (amplitude - 1.0) * cosine - shelf,
                )
            }

            EqFilterType.LOW_PASS,
            EqFilterType.HIGH_PASS,
            EqFilterType.OTHER,
            -> return null
        }

        val a0 = values[3]
        if (!a0.isFinite() || a0 == 0.0) return null
        return Biquad(
            b0 = values[0] / a0,
            b1 = values[1] / a0,
            b2 = values[2] / a0,
            a1 = values[4] / a0,
            a2 = values[5] / a0,
        )
    }

    private data class Biquad(
        val b0: Double,
        val b1: Double,
        val b2: Double,
        val a1: Double,
        val a2: Double,
    )
}

/** A marker-worthy hardware band has verified native state and a non-flat acoustic contribution. */
fun HardwareEqFilter.isAcousticallyActive(): Boolean =
    enabled &&
        abs(gainDb) > ACOUSTICALLY_FLAT_GAIN_EPSILON_DB &&
        type in setOf(EqFilterType.PEAK, EqFilterType.LOW_SHELF, EqFilterType.HIGH_SHELF)

private const val ACOUSTICALLY_FLAT_GAIN_EPSILON_DB = 0.000_001
