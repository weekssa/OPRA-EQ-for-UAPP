package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandDeviceSpec
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandQuantization
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.adaptationSummary
import java.util.Locale

/**
 * Product-facing TOPPING Tune AutoEq text export.
 *
 * TOPPING documents AutoEq import, ten PEQ bands, +/-12 dB preamp/filter gain, Q 0.1..15, and
 * direct numeric entry, but its public guide does not establish the downstream device storage
 * quantization used after import. Therefore the adapter quantization below is only EQ Library's text
 * serialization precision; it must not be interpreted as a claim about TOPPING hardware resolution.
 * Until import/storage precision is independently qualified, TOPPING Tune variants are reported
 * conservatively as Optimized even when the source is preserved at file precision.
 */
internal fun buildToppingTuneFileExportVariant(profile: OpraEqProfile): DevicePresetVariant? {
    val capabilities = requireNotNull(ExportDevice.TOPPING_TUNE.eqCapabilities)
    val textSpec = FiveBandDeviceSpec(
        stableId = "app-topping-tune-autoeq-text",
        displayName = ExportDevice.TOPPING_TUNE.displayName,
        capabilities = capabilities,
        quantization = FiveBandQuantization(
            frequencyStepHz = 0.1,
            gainStepDb = 0.01,
            qStep = 0.001,
            preampStepDb = 0.01,
        ),
        representationVersion = TOPPING_TUNE_TEXT_REPRESENTATION_VERSION,
    )
    val representation = when (val result = Kt02h20FiveBandOptimizer.optimize(profile, textSpec)) {
        is FiveBandOptimizationResult.NotSuitable -> return null
        is FiveBandOptimizationResult.Ready -> result.representation
    }
    if (representation.bands.isEmpty()) return null

    val content = buildString {
        appendLine("Preamp: ${formatDb(representation.playbackGainDb)} dB")
        representation.bands.forEachIndexed { index, band ->
            val type = parametricType(band.type) ?: return null
            appendLine(
                "Filter ${index + 1}: ON $type Fc ${formatHz(band.frequencyHz)} Hz " +
                    "Gain ${formatDb(band.gainDb)} dB Q ${formatQ(band.q)}",
            )
        }
    }.trimEnd()

    val adaptation = when {
        representation.usedResponseFit -> {
            "${representation.adaptationSummary()} (RMS ${formatMetric(representation.rmsErrorDb)} dB, " +
                "max ${formatMetric(representation.maxAbsoluteErrorDb)} dB)"
        }
        representation.usesGeneratedHeadroom || representation.usesNativeQuantization ->
            representation.adaptationSummary()
        else -> "source values preserved at AutoEq text precision"
    }
    val transformation =
        "TOPPING Tune: Optimized · $adaptation. TOPPING documents direct AutoEq text import, but " +
            "device-side PEQ storage precision after import is not publicly specified, so EQ Library " +
            "does not claim Exact fidelity until that precision is independently qualified."

    return DevicePresetVariant(
        device = ExportDevice.TOPPING_TUNE,
        content = content,
        transformation = transformation,
        fidelity = DevicePresetFidelity.OPTIMIZED,
        representationVersion = TOPPING_TUNE_TEXT_REPRESENTATION_VERSION,
    )
}

private fun formatHz(value: Double): String =
    if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value)
    else String.format(Locale.US, "%.1f", value)

private fun formatDb(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun formatQ(value: Double): String = String.format(Locale.US, "%.3f", value)
private fun formatMetric(value: Double): String = String.format(Locale.US, "%.2f", value)

private const val TOPPING_TUNE_TEXT_REPRESENTATION_VERSION = 2
