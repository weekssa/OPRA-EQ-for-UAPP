package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import java.util.Locale

/**
 * Builds file representations for outputs whose exported file should match the same derived hardware
 * plan used by Direct Flash.
 *
 * Black Pearl file export and USB Flash remain independent delivery actions, but they now consume one
 * shared response-adaptation policy. A >10-band canonical source is fitted to the complete response,
 * not first-N truncated. Exact protocol-encodable source gains outside the currently validated +/-10
 * dB region remain unchanged and receive the same caution rather than being clamped.
 */
internal fun buildFileExportDeviceVariant(
    profile: OpraEqProfile,
    device: ExportDevice,
): DevicePresetVariant? = when (device) {
    ExportDevice.BLACK_PEARL -> buildBlackPearlFileExportVariant(profile)
    else -> buildTextDeviceVariant(profile, device)
}

private fun buildBlackPearlFileExportVariant(profile: OpraEqProfile): DevicePresetVariant? {
    val representation = when (
        val result = Kt02h20FiveBandOptimizer.optimize(profile, HardwareEqDeviceSpecs.TRN_BLACK_PEARL)
    ) {
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

    val baseTransformation = when (representation.fidelity) {
        DevicePresetFidelity.EXACT ->
            "Source EQ is natively representable in the Black Pearl hardware plan; file export uses the same quantized filters and playback gain as Direct Flash."
        DevicePresetFidelity.OPTIMIZED ->
            "EQ Library fitted the complete source response to the Black Pearl's 10-band hardware plan (RMS ${formatMetric(representation.rmsErrorDb)} dB, max ${formatMetric(representation.maxAbsoluteErrorDb)} dB). The file uses the same derived filters and playback gain as Direct Flash."
    }
    val outsideValidatedGainRange = representation.bands.mapIndexedNotNull { index, band ->
        band.gainDb.takeIf { gain -> gain !in -10.0..10.0 }?.let { gain ->
            "Band ${index + 1} ${formatSignedDb(gain)} dB"
        }
    }
    val transformation = if (outsideValidatedGainRange.isEmpty()) {
        baseTransformation
    } else {
        "$baseTransformation Caution: ${outsideValidatedGainRange.joinToString()} is outside the currently validated Black Pearl filter-gain range; the exact source value is preserved unchanged and is not clamped."
    }

    return DevicePresetVariant(
        device = ExportDevice.BLACK_PEARL,
        content = content,
        transformation = transformation,
        fidelity = representation.fidelity,
    )
}

private fun formatHz(value: Double): String =
    if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.1f", value)
    }

private fun formatDb(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun formatSignedDb(value: Double): String = String.format(Locale.US, "%+.2f", value)
private fun formatQ(value: Double): String = String.format(Locale.US, "%.3f", value)
private fun formatMetric(value: Double): String = String.format(Locale.US, "%.2f", value)
