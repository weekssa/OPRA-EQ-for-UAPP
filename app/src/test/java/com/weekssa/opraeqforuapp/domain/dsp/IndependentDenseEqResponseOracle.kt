package com.weekssa.opraeqforuapp.domain.dsp

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqFilter
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import java.util.TreeSet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Test-only response implementation for independent PEQ fit and headroom assertions.
 *
 * It owns its coefficient calculation and 12,001-point grid. It never calls the production
 * optimizer, response evaluator, or their reported metrics.
 */
internal object IndependentDenseEqResponseOracle {
    private const val SAMPLE_RATE_HZ = 48_000.0
    private const val MIN_FREQUENCY_HZ = 20.0
    private const val MAX_FREQUENCY_HZ = 20_000.0
    private const val GRID_POINT_COUNT = 12_001
    private const val LN_10 = 2.302585092994046
    private val centerOffsets = listOf(
        0.75, 0.90, 0.95, 0.98, 0.99, 0.995,
        1.005, 1.01, 1.02, 1.05, 1.10, 1.25,
    )

    data class Filter(
        val type: String,
        val frequencyHz: Double,
        val gainDb: Double,
        val q: Double,
    )

    data class ErrorMetrics(
        val rmsDb: Double,
        val maxAbsoluteDb: Double,
    )

    fun fromOpra(band: OpraBand): Filter = Filter(
        type = requireNotNull(band.type),
        frequencyHz = requireNotNull(band.frequency),
        gainDb = requireNotNull(band.gainDb),
        q = requireNotNull(band.q),
    )

    fun fromHardware(band: Kt02h20Band): Filter = Filter(
        type = band.type,
        frequencyHz = band.frequencyHz,
        gainDb = band.gainDb,
        q = band.q,
    )

    fun fromHardware(filter: HardwareEqFilter): Filter = Filter(
        type = when (filter.type) {
            EqFilterType.PEAK -> "peak_dip"
            EqFilterType.LOW_SHELF -> "low_shelf"
            EqFilterType.HIGH_SHELF -> "high_shelf"
            else -> error("Unsupported active type in dense oracle fixture: ${filter.type}")
        },
        frequencyHz = filter.frequencyHz,
        gainDb = filter.gainDb,
        q = filter.q,
    )

    fun maximumGainDb(filters: List<Filter>): Double {
        val coefficients = filters.map(::coefficients)
        var maximum = Double.NEGATIVE_INFINITY
        frequencies(filters).forEach { frequency ->
            val total = coefficients.sumOf { responseDb(it, frequency) }
            if (total > maximum) maximum = total
        }
        return maximum
    }

    fun error(source: List<Filter>, target: List<Filter>): ErrorMetrics {
        val sourceCoefficients = source.map(::coefficients)
        val targetCoefficients = target.map(::coefficients)
        val grid = frequencies(source + target)
        var squared = 0.0
        var maximumAbsolute = 0.0
        grid.forEach { frequency ->
            val sourceGain = sourceCoefficients.sumOf { responseDb(it, frequency) }
            val targetGain = targetCoefficients.sumOf { responseDb(it, frequency) }
            val delta = targetGain - sourceGain
            squared += delta * delta
            maximumAbsolute = maxOf(maximumAbsolute, abs(delta))
        }
        return ErrorMetrics(
            rmsDb = sqrt(squared / grid.size),
            maxAbsoluteDb = maximumAbsolute,
        )
    }

    private fun frequencies(filters: List<Filter>): List<Double> {
        val values = TreeSet<Double>()
        repeat(GRID_POINT_COUNT) { index ->
            val fraction = index.toDouble() / (GRID_POINT_COUNT - 1).toDouble()
            values += MIN_FREQUENCY_HZ * (MAX_FREQUENCY_HZ / MIN_FREQUENCY_HZ).pow(fraction)
        }
        filters.forEach { filter ->
            if (filter.frequencyHz in MIN_FREQUENCY_HZ..MAX_FREQUENCY_HZ) {
                values += filter.frequencyHz
                centerOffsets.forEach { scale ->
                    val nearby = filter.frequencyHz * scale
                    if (nearby in MIN_FREQUENCY_HZ..MAX_FREQUENCY_HZ) values += nearby
                }
            }
        }
        return values.toList()
    }

    private data class Coefficients(
        val b0: Double,
        val b1: Double,
        val b2: Double,
        val a1: Double,
        val a2: Double,
    )

    private fun coefficients(filter: Filter): Coefficients {
        require(filter.frequencyHz > 0.0 && filter.frequencyHz < SAMPLE_RATE_HZ / 2.0)
        require(filter.q > 0.0)
        val amplitude = 10.0.pow(filter.gainDb / 40.0)
        val angle = 2.0 * PI * filter.frequencyHz / SAMPLE_RATE_HZ
        val cosine = cos(angle)
        val sine = sin(angle)
        val alpha = sine / (2.0 * filter.q)
        val raw = when (filter.type) {
            "peak_dip" -> doubleArrayOf(
                1.0 + alpha * amplitude,
                -2.0 * cosine,
                1.0 - alpha * amplitude,
                1.0 + alpha / amplitude,
                -2.0 * cosine,
                1.0 - alpha / amplitude,
            )
            "low_shelf" -> {
                val blend = 2.0 * sqrt(amplitude) * alpha
                doubleArrayOf(
                    amplitude * ((amplitude + 1.0) - (amplitude - 1.0) * cosine + blend),
                    2.0 * amplitude * ((amplitude - 1.0) - (amplitude + 1.0) * cosine),
                    amplitude * ((amplitude + 1.0) - (amplitude - 1.0) * cosine - blend),
                    (amplitude + 1.0) + (amplitude - 1.0) * cosine + blend,
                    -2.0 * ((amplitude - 1.0) + (amplitude + 1.0) * cosine),
                    (amplitude + 1.0) + (amplitude - 1.0) * cosine - blend,
                )
            }
            "high_shelf" -> {
                val blend = 2.0 * sqrt(amplitude) * alpha
                doubleArrayOf(
                    amplitude * ((amplitude + 1.0) + (amplitude - 1.0) * cosine + blend),
                    -2.0 * amplitude * ((amplitude - 1.0) + (amplitude + 1.0) * cosine),
                    amplitude * ((amplitude + 1.0) + (amplitude - 1.0) * cosine - blend),
                    (amplitude + 1.0) - (amplitude - 1.0) * cosine + blend,
                    2.0 * ((amplitude - 1.0) - (amplitude + 1.0) * cosine),
                    (amplitude + 1.0) - (amplitude - 1.0) * cosine - blend,
                )
            }
            else -> error("Unsupported dense oracle filter type: ${filter.type}")
        }
        val a0 = raw[3]
        require(a0.isFinite() && a0 != 0.0)
        return Coefficients(
            b0 = raw[0] / a0,
            b1 = raw[1] / a0,
            b2 = raw[2] / a0,
            a1 = raw[4] / a0,
            a2 = raw[5] / a0,
        )
    }

    private fun responseDb(coefficients: Coefficients, frequencyHz: Double): Double {
        val omega = 2.0 * PI * frequencyHz / SAMPLE_RATE_HZ
        val cos1 = cos(omega)
        val sin1 = sin(omega)
        val cos2 = cos(2.0 * omega)
        val sin2 = sin(2.0 * omega)
        val realNumerator = coefficients.b0 + coefficients.b1 * cos1 + coefficients.b2 * cos2
        val imaginaryNumerator = -(coefficients.b1 * sin1 + coefficients.b2 * sin2)
        val realDenominator = 1.0 + coefficients.a1 * cos1 + coefficients.a2 * cos2
        val imaginaryDenominator = -(coefficients.a1 * sin1 + coefficients.a2 * sin2)
        val numeratorMagnitudeSquared = realNumerator * realNumerator + imaginaryNumerator * imaginaryNumerator
        val denominatorMagnitudeSquared = realDenominator * realDenominator + imaginaryDenominator * imaginaryDenominator
        return 10.0 * ln(numeratorMagnitudeSquared / denominatorMagnitudeSquared) / LN_10
    }
}
