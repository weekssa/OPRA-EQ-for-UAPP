package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DeviceEqCapabilities
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/** Device-independent PEQ band used by all direct-hardware output adapters. */
data class Kt02h20Band(
    val type: String,
    val frequencyHz: Double,
    val gainDb: Double,
    val q: Double,
)

/**
 * Native storage resolution for a hardware EQ target.
 *
 * The legacy FiveBand name is retained for source compatibility with the first JA11/JM12
 * implementation, but this model is intentionally band-count agnostic.
 */
data class FiveBandQuantization(
    val frequencyStepHz: Double = 1.0,
    val gainStepDb: Double = 0.1,
    val qStep: Double = 0.01,
    val preampStepDb: Double? = null,
) {
    init {
        require(frequencyStepHz > 0.0)
        require(gainStepDb > 0.0)
        require(qStep > 0.0)
        require(preampStepDb == null || preampStepDb > 0.0)
    }
}

/**
 * Shared direct-hardware adaptation profile.
 *
 * The legacy class name is kept to avoid a persisted/API migration, but maxBands may now be any
 * positive finite hardware band budget. `representationVersion` invalidates derived device plans
 * when fitting/capability behavior intentionally changes; canonical EQ data is never mutated.
 */
data class FiveBandDeviceSpec(
    val stableId: String,
    val displayName: String,
    val capabilities: DeviceEqCapabilities,
    val quantization: FiveBandQuantization,
    val maxRmsErrorDb: Double = 2.0,
    val maxAbsoluteErrorDb: Double = 6.0,
    val representationVersion: Int = 1,
) {
    init {
        require(capabilities.maxBands != null && capabilities.maxBands > 0) {
            "Hardware response adapter requires a finite positive band budget."
        }
        require(maxRmsErrorDb > 0.0)
        require(maxAbsoluteErrorDb > 0.0)
        require(representationVersion > 0)
    }
}

data class FiveBandRepresentation(
    val bands: List<Kt02h20Band>,
    val playbackGainDb: Double,
    val fidelity: DevicePresetFidelity,
    val rmsErrorDb: Double,
    val maxAbsoluteErrorDb: Double,
    val usesGeneratedHeadroom: Boolean,
    val representationVersion: Int = 1,
)

sealed interface FiveBandOptimizationResult {
    data class Ready(val representation: FiveBandRepresentation) : FiveBandOptimizationResult
    data class NotSuitable(val reason: String) : FiveBandOptimizationResult
}

/**
 * Deterministically adapts a complete canonical parametric response to a finite hardware PEQ budget.
 *
 * Exact source filters pass through only when they are natively representable at the target's actual
 * storage resolution. Otherwise the full source response is fitted to the available device bands and
 * the quantized result is measured against fixed RMS/max-error gates. Unsupported source filter types
 * fail rather than being silently ignored.
 *
 * Source-authored preamp is preserved subject to native device quantization. When the source omitted
 * preamp, safety headroom is derived from the final quantized device response rather than copied from
 * a different representation. This keeps the policy consistent while allowing a 5-band and 10-band
 * target to require slightly different safe attenuation.
 */
object Kt02h20FiveBandOptimizer {
    private const val SAMPLE_RATE_HZ = 48_000.0
    private const val RESPONSE_POINTS = 96
    private const val EPSILON = 1e-9
    private val cache = ConcurrentHashMap<CacheKey, FiveBandOptimizationResult>()

    fun optimize(
        profile: OpraEqProfile,
        spec: FiveBandDeviceSpec,
    ): FiveBandOptimizationResult = cache.getOrPut(CacheKey(profile, spec)) {
        optimizeUncached(profile, spec)
    }

    fun clearCache() = cache.clear()

    private fun optimizeUncached(
        profile: OpraEqProfile,
        spec: FiveBandDeviceSpec,
    ): FiveBandOptimizationResult {
        val capabilities = spec.capabilities
        val maxBands = requireNotNull(capabilities.maxBands)
        val sourceBands = profile.bands.orEmpty()
        if (sourceBands.isEmpty()) {
            return FiveBandOptimizationResult.NotSuitable("This profile does not contain parametric EQ bands.")
        }
        if (sourceBands.any { it.type !in capabilities.supportedBandTypes }) {
            val unsupported = sourceBands.firstOrNull { it.type !in capabilities.supportedBandTypes }?.type ?: "unknown"
            return FiveBandOptimizationResult.NotSuitable(
                "${spec.displayName} cannot represent the source filter type $unsupported.",
            )
        }
        val parsedSource = sourceBands.mapIndexed { index, band ->
            band.toHardwareBandOrNull()?.let { IndexedBand(index, it) }
                ?: return FiveBandOptimizationResult.NotSuitable(
                    "Source filter ${index + 1} has missing, non-finite, or invalid frequency/gain/Q data.",
                )
        }
        val sourceResponse = response(parsedSource.map(IndexedBand::band))
            ?: return FiveBandOptimizationResult.NotSuitable("The source EQ response could not be evaluated safely.")

        val exactBands = parsedSource.map(IndexedBand::band)
        val exactBandFit = exactBands.size <= maxBands && exactBands.all { it.fitsExactly(capabilities) }
        val exactQuantizedBands = if (exactBandFit) exactBands.map { quantizeBand(it, spec) } else emptyList()
        val bandQuantizationExact = exactBandFit && exactBands.zip(exactQuantizedBands).all { (a, b) -> a.nearlyEquals(b) }
        if (bandQuantizationExact) {
            val playback = playbackGainFor(profile, exactQuantizedBands, spec)
                ?: return FiveBandOptimizationResult.NotSuitable(
                    "The required playback gain is outside ${spec.displayName}'s current capability profile.",
                )
            val sourcePreampExact = profile.preampGainDb?.takeIf(Double::isFinite)?.let { sourcePreamp ->
                abs(playback.valueDb - sourcePreamp) <= EPSILON
            } ?: true
            if (sourcePreampExact) {
                return FiveBandOptimizationResult.Ready(
                    FiveBandRepresentation(
                        bands = exactQuantizedBands,
                        playbackGainDb = playback.valueDb,
                        fidelity = DevicePresetFidelity.EXACT,
                        rmsErrorDb = 0.0,
                        maxAbsoluteErrorDb = 0.0,
                        usesGeneratedHeadroom = playback.generated,
                        representationVersion = spec.representationVersion,
                    ),
                )
            }
        }

        val seeds = parsedSource
            .map { indexed -> indexed.copy(band = quantizeBand(indexed.band.coerceTo(capabilities), spec)) }
            .distinctBy { it.band }
        if (seeds.isEmpty()) {
            return FiveBandOptimizationResult.NotSuitable(
                "No source filters can seed a ${maxBands}-band hardware fit.",
            )
        }

        val fitted = fitGreedy(sourceResponse, seeds, spec)
        val metrics = responseError(sourceResponse, fitted.map(IndexedBand::band))
            ?: return FiveBandOptimizationResult.NotSuitable("The optimized response could not be evaluated safely.")
        if (metrics.rmsDb > spec.maxRmsErrorDb || metrics.maxAbsDb > spec.maxAbsoluteErrorDb) {
            return FiveBandOptimizationResult.NotSuitable(
                "A reliable ${maxBands}-band approximation could not be produced for ${spec.displayName} " +
                    "(RMS ${formatDb(metrics.rmsDb)} dB, max ${formatDb(metrics.maxAbsDb)} dB).",
            )
        }
        val targetBands = fitted.map(IndexedBand::band)
        val playback = playbackGainFor(profile, targetBands, spec)
            ?: return FiveBandOptimizationResult.NotSuitable(
                "The required playback gain is outside ${spec.displayName}'s current capability profile.",
            )

        return FiveBandOptimizationResult.Ready(
            FiveBandRepresentation(
                bands = targetBands,
                playbackGainDb = playback.valueDb,
                fidelity = DevicePresetFidelity.OPTIMIZED,
                rmsErrorDb = metrics.rmsDb,
                maxAbsoluteErrorDb = metrics.maxAbsDb,
                usesGeneratedHeadroom = playback.generated,
                representationVersion = spec.representationVersion,
            ),
        )
    }

    private fun playbackGainFor(
        profile: OpraEqProfile,
        targetBands: List<Kt02h20Band>,
        spec: FiveBandDeviceSpec,
    ): PlaybackGain? {
        val sourcePreamp = profile.preampGainDb?.takeIf(Double::isFinite)
        val generated = sourcePreamp == null
        val rawValue = sourcePreamp ?: generatedHeadroomDb(targetBands) ?: return null
        val quantized = quantizePreamp(rawValue, spec, conservativeAttenuation = generated) ?: return null
        return PlaybackGain(quantized, generated)
    }

    private fun generatedHeadroomDb(targetBands: List<Kt02h20Band>): Double? {
        val targetResponse = response(targetBands) ?: return null
        val peakBoostDb = targetResponse.maxOrNull()?.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: return null
        return -peakBoostDb
    }

    private fun fitGreedy(
        targetResponse: DoubleArray,
        sourceSeeds: List<IndexedBand>,
        spec: FiveBandDeviceSpec,
    ): List<IndexedBand> {
        val maxBands = requireNotNull(spec.capabilities.maxBands)
        val remaining = sourceSeeds.toMutableList()
        var selected = emptyList<IndexedBand>()
        while (selected.size < maxBands && remaining.isNotEmpty()) {
            val best = remaining
                .mapIndexedNotNull { remainingIndex, candidate ->
                    responseError(targetResponse, (selected + candidate).map(IndexedBand::band))?.let { metrics ->
                        CandidateScore(remainingIndex, candidate, metrics.rmsDb, metrics.maxAbsDb)
                    }
                }
                .minWithOrNull(
                    compareBy<CandidateScore>({ it.rmsDb }, { it.maxAbsDb }, { it.band.sourceIndex }, { it.remainingIndex }),
                ) ?: break
            selected = refine(selected + best.band, targetResponse, sourceSeeds, spec, passes = 1)
            val removeIndex = remaining.indexOfFirst { it.sourceIndex == best.band.sourceIndex && it.band == best.band.band }
            if (removeIndex >= 0) remaining.removeAt(removeIndex) else break
        }
        return refine(selected, targetResponse, sourceSeeds, spec, passes = 2)
            .take(maxBands)
    }

    private fun refine(
        initial: List<IndexedBand>,
        targetResponse: DoubleArray,
        allSeeds: List<IndexedBand>,
        spec: FiveBandDeviceSpec,
        passes: Int,
    ): List<IndexedBand> {
        if (initial.isEmpty()) return initial
        var current = initial
        repeat(passes) {
            for (index in current.indices) {
                var band = current[index].band

                band = chooseBestBand(
                    current,
                    index,
                    gainCandidates(band, spec),
                    targetResponse,
                )
                band = chooseBestBand(
                    current,
                    index,
                    frequencyCandidates(band, allSeeds, spec),
                    targetResponse,
                )
                band = chooseBestBand(
                    current,
                    index,
                    qCandidates(band, spec),
                    targetResponse,
                )
                current = current.toMutableList().also { list ->
                    list[index] = list[index].copy(band = band)
                }
            }
        }
        return current
    }

    private fun chooseBestBand(
        current: List<IndexedBand>,
        index: Int,
        candidates: List<Kt02h20Band>,
        targetResponse: DoubleArray,
    ): Kt02h20Band {
        val baseline = current[index].band
        return candidates
            .distinct()
            .mapNotNull { candidate ->
                val list = current.toMutableList().also { mutable ->
                    mutable[index] = mutable[index].copy(band = candidate)
                }
                responseError(targetResponse, list.map(IndexedBand::band))?.let { metrics ->
                    BandScore(candidate, metrics.rmsDb, metrics.maxAbsDb)
                }
            }
            .minWithOrNull(compareBy<BandScore>({ it.rmsDb }, { it.maxAbsDb }, { stableBandOrder(it.band) }))
            ?.band ?: baseline
    }

    private fun gainCandidates(band: Kt02h20Band, spec: FiveBandDeviceSpec): List<Kt02h20Band> {
        val cap = spec.capabilities
        val scanStep = maxOf(spec.quantization.gainStepDb, 0.5)
        val values = buildList {
            var value = cap.minGainDb
            while (value <= cap.maxGainDb + EPSILON) {
                add(value)
                value += scanStep
            }
            add(band.gainDb)
            add(0.0.coerceIn(cap.minGainDb, cap.maxGainDb))
        }
        return values.map { gain -> quantizeBand(band.copy(gainDb = gain), spec) }
    }

    private fun frequencyCandidates(
        band: Kt02h20Band,
        allSeeds: List<IndexedBand>,
        spec: FiveBandDeviceSpec,
    ): List<Kt02h20Band> {
        val cap = spec.capabilities
        val factors = listOf(0.5, 0.70710678, 0.84089642, 1.0, 1.18920712, 1.41421356, 2.0)
        val values = buildList {
            factors.forEach { factor -> add((band.frequencyHz * factor).coerceIn(cap.minFrequencyHz, cap.maxFrequencyHz)) }
            allSeeds.forEach { seed -> add(seed.band.frequencyHz.coerceIn(cap.minFrequencyHz, cap.maxFrequencyHz)) }
            add(cap.minFrequencyHz)
            add(cap.maxFrequencyHz)
        }
        return values.map { frequency -> quantizeBand(band.copy(frequencyHz = frequency), spec) }
    }

    private fun qCandidates(band: Kt02h20Band, spec: FiveBandDeviceSpec): List<Kt02h20Band> {
        val cap = spec.capabilities
        val standard = listOf(0.1, 0.2, 0.3, 0.5, 0.7, 1.0, 1.4, 2.0, 3.0, 4.0, 6.0, 8.0, 10.0, 15.0, 20.0)
        val values = (standard + listOf(band.q * 0.5, band.q * 0.75, band.q, band.q * 1.33333333, band.q * 2.0))
            .map { it.coerceIn(cap.minQ, cap.maxQ) }
        return values.map { q -> quantizeBand(band.copy(q = q), spec) }
    }

    private fun responseError(target: DoubleArray, bands: List<Kt02h20Band>): ErrorMetrics? {
        val candidate = response(bands) ?: return null
        if (candidate.size != target.size) return null
        var squared = 0.0
        var maxAbs = 0.0
        candidate.indices.forEach { index ->
            val delta = candidate[index] - target[index]
            if (!delta.isFinite()) return null
            squared += delta * delta
            maxAbs = maxOf(maxAbs, abs(delta))
        }
        return ErrorMetrics(sqrt(squared / candidate.size), maxAbs)
    }

    private fun response(bands: List<Kt02h20Band>): DoubleArray? {
        val grid = responseGrid()
        val result = DoubleArray(grid.size)
        grid.forEachIndexed { index, frequency ->
            var sumDb = 0.0
            bands.forEach { band ->
                sumDb += bandResponseDb(band, frequency) ?: return null
            }
            result[index] = sumDb
        }
        return result
    }

    private fun bandResponseDb(band: Kt02h20Band, frequency: Double): Double? {
        val biquad = rbjBiquad(band) ?: return null
        val omega = 2.0 * PI * frequency / SAMPLE_RATE_HZ
        val c1 = cos(omega)
        val s1 = sin(omega)
        val c2 = cos(2.0 * omega)
        val s2 = sin(2.0 * omega)
        val numeratorReal = biquad.b0 + biquad.b1 * c1 + biquad.b2 * c2
        val numeratorImag = -(biquad.b1 * s1 + biquad.b2 * s2)
        val denominatorReal = 1.0 + biquad.a1 * c1 + biquad.a2 * c2
        val denominatorImag = -(biquad.a1 * s1 + biquad.a2 * s2)
        val n2 = numeratorReal * numeratorReal + numeratorImag * numeratorImag
        val d2 = denominatorReal * denominatorReal + denominatorImag * denominatorImag
        if (n2 <= 0.0 || d2 <= 0.0 || !n2.isFinite() || !d2.isFinite()) return null
        return 10.0 * ln(n2 / d2) / ln(10.0)
    }

    private fun rbjBiquad(band: Kt02h20Band): Biquad? {
        if (band.frequencyHz <= 0.0 || band.frequencyHz >= SAMPLE_RATE_HZ / 2.0 || band.q <= 0.0) return null
        val a = 10.0.pow(band.gainDb / 40.0)
        val w0 = 2.0 * PI * band.frequencyHz / SAMPLE_RATE_HZ
        val cw = cos(w0)
        val sw = sin(w0)
        val alpha = sw / (2.0 * band.q)
        val values = when (band.type) {
            "peak_dip" -> doubleArrayOf(
                1.0 + alpha * a,
                -2.0 * cw,
                1.0 - alpha * a,
                1.0 + alpha / a,
                -2.0 * cw,
                1.0 - alpha / a,
            )
            "low_shelf" -> {
                val shelf = 2.0 * sqrt(a) * alpha
                doubleArrayOf(
                    a * ((a + 1.0) - (a - 1.0) * cw + shelf),
                    2.0 * a * ((a - 1.0) - (a + 1.0) * cw),
                    a * ((a + 1.0) - (a - 1.0) * cw - shelf),
                    (a + 1.0) + (a - 1.0) * cw + shelf,
                    -2.0 * ((a - 1.0) + (a + 1.0) * cw),
                    (a + 1.0) + (a - 1.0) * cw - shelf,
                )
            }
            "high_shelf" -> {
                val shelf = 2.0 * sqrt(a) * alpha
                doubleArrayOf(
                    a * ((a + 1.0) + (a - 1.0) * cw + shelf),
                    -2.0 * a * ((a - 1.0) + (a + 1.0) * cw),
                    a * ((a + 1.0) + (a - 1.0) * cw - shelf),
                    (a + 1.0) - (a - 1.0) * cw + shelf,
                    2.0 * ((a - 1.0) - (a + 1.0) * cw),
                    (a + 1.0) - (a - 1.0) * cw - shelf,
                )
            }
            else -> return null
        }
        val a0 = values[3]
        if (a0 == 0.0 || !a0.isFinite()) return null
        return Biquad(values[0] / a0, values[1] / a0, values[2] / a0, values[4] / a0, values[5] / a0)
    }

    private fun responseGrid(): DoubleArray = DoubleArray(RESPONSE_POINTS) { index ->
        val fraction = index.toDouble() / (RESPONSE_POINTS - 1).toDouble()
        20.0 * (20_000.0 / 20.0).pow(fraction)
    }

    private fun OpraBand.toHardwareBandOrNull(): Kt02h20Band? {
        val type = type ?: return null
        val frequency = frequency?.takeIf(Double::isFinite) ?: return null
        val gain = gainDb?.takeIf(Double::isFinite) ?: return null
        val qValue = q?.takeIf(Double::isFinite)?.takeIf { it > 0.0 } ?: return null
        return Kt02h20Band(type, frequency, gain, qValue)
    }

    private fun Kt02h20Band.fitsExactly(cap: DeviceEqCapabilities): Boolean =
        type in cap.supportedBandTypes &&
            frequencyHz in cap.minFrequencyHz..cap.maxFrequencyHz &&
            gainDb in cap.minGainDb..cap.maxGainDb &&
            q in cap.minQ..cap.maxQ

    private fun Kt02h20Band.coerceTo(cap: DeviceEqCapabilities): Kt02h20Band = copy(
        frequencyHz = frequencyHz.coerceIn(cap.minFrequencyHz, cap.maxFrequencyHz),
        gainDb = gainDb.coerceIn(cap.minGainDb, cap.maxGainDb),
        q = q.coerceIn(cap.minQ, cap.maxQ),
    )

    private fun quantizeBand(band: Kt02h20Band, spec: FiveBandDeviceSpec): Kt02h20Band {
        val cap = spec.capabilities
        return band.copy(
            frequencyHz = quantize(band.frequencyHz, spec.quantization.frequencyStepHz)
                .coerceIn(cap.minFrequencyHz, cap.maxFrequencyHz),
            gainDb = quantize(band.gainDb, spec.quantization.gainStepDb)
                .coerceIn(cap.minGainDb, cap.maxGainDb),
            q = quantize(band.q, spec.quantization.qStep).coerceIn(cap.minQ, cap.maxQ),
        )
    }

    private fun quantizePreamp(
        value: Double,
        spec: FiveBandDeviceSpec,
        conservativeAttenuation: Boolean,
    ): Double? {
        val cap = spec.capabilities
        val min = cap.minPreampDb
        val max = cap.maxPreampDb
        if (min != null && max != null && value !in min..max) return null
        val step = spec.quantization.preampStepDb
        val quantized = when {
            step == null -> value
            conservativeAttenuation && value < 0.0 -> floor(value / step) * step
            else -> quantize(value, step)
        }
        if (min != null && max != null && quantized !in min..max) return null
        return quantized
    }

    private fun quantize(value: Double, step: Double): Double = round(value / step) * step

    private fun Kt02h20Band.nearlyEquals(other: Kt02h20Band): Boolean =
        type == other.type && abs(frequencyHz - other.frequencyHz) <= EPSILON &&
            abs(gainDb - other.gainDb) <= EPSILON && abs(q - other.q) <= EPSILON

    private fun stableBandOrder(band: Kt02h20Band): String =
        "%s:%020.8f:%020.8f:%020.8f".format(
            java.util.Locale.US,
            band.type,
            band.frequencyHz,
            band.gainDb,
            band.q,
        )

    private fun formatDb(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)

    private data class PlaybackGain(val valueDb: Double, val generated: Boolean)
    private data class CacheKey(val profile: OpraEqProfile, val spec: FiveBandDeviceSpec)
    private data class IndexedBand(val sourceIndex: Int, val band: Kt02h20Band)
    private data class CandidateScore(
        val remainingIndex: Int,
        val band: IndexedBand,
        val rmsDb: Double,
        val maxAbsDb: Double,
    )
    private data class BandScore(val band: Kt02h20Band, val rmsDb: Double, val maxAbsDb: Double)
    private data class ErrorMetrics(val rmsDb: Double, val maxAbsDb: Double)
    private data class Biquad(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double)
}
