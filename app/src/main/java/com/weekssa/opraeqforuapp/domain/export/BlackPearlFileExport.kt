package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.adaptationSummary
import java.util.Locale

/**
 * Builds file representations for outputs whose exported file should match the same derived hardware
 * plan used by Direct Flash.
 *
 * Black Pearl file export and USB Flash remain independent delivery actions, but they consume one
 * shared response-adaptation policy. A >10-band canonical source is fitted to the complete response,
 * not first-N truncated. Exact protocol-encodable source gains outside the currently validated +/-10
 * dB region remain unchanged and receive the same caution rather than being clamped.
 *
 * The file syntax intentionally follows the verified pyBlackPearl AutoEq importer contract: Peak,
 * Low Shelf, and High Shelf are written as PK / LS / HS. This is a Black Pearl-specific serializer;
 * generic AutoEq/Equalizer APO targets continue to use their own standard shelf tokens.
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
            val type = blackPearlImportType(band.type) ?: return null
            appendLine(
                "Filter ${index + 1}: ON $type Fc ${formatHz(band.frequencyHz)} Hz " +
                    "Gain ${formatDb(band.gainDb)} dB Q ${formatQ(band.q)}",
            )
        }
    }.trimEnd()

    val baseTransformation = when (representation.fidelity) {
        DevicePresetFidelity.EXACT ->
            "Source values are preserved in the Black Pearl hardware plan; this AutoEq text uses the same filters and playback gain as Direct Flash."
        DevicePresetFidelity.OPTIMIZED -> {
            val metrics = if (representation.usedResponseFit) {
                " (RMS ${formatMetric(representation.rmsErrorDb)} dB, max ${formatMetric(representation.maxAbsoluteErrorDb)} dB)"
            } else {
                ""
            }
            "Black Pearl: Optimized · ${representation.adaptationSummary()}$metrics. This AutoEq text uses the same derived filters and playback gain as Direct Flash."
        }
    }

    val cautions = buildList {
        val outsideValidatedGainRange = representation.bands.mapIndexedNotNull { index, band ->
            band.gainDb.takeIf { gain -> gain !in -10.0..10.0 }?.let { gain ->
                "Band ${index + 1} ${formatSignedDb(gain)} dB"
            }
        }
        if (outsideValidatedGainRange.isNotEmpty()) {
            add(
                "Caution: ${outsideValidatedGainRange.joinToString()} is outside the currently validated Black Pearl " +
                    "filter-gain range; the value is preserved unchanged and is not clamped.",
            )
        }
        if (representation.playbackGainDb !in PYBLACKPEARL_IMPORT_PREAMP_MIN_DB..PYBLACKPEARL_IMPORT_PREAMP_MAX_DB) {
            add(
                "pyBlackPearl accepts this AutoEq text but limits imported preamp to " +
                    "${formatDb(PYBLACKPEARL_IMPORT_PREAMP_MIN_DB)}..${formatSignedDb(PYBLACKPEARL_IMPORT_PREAMP_MAX_DB)} dB. " +
                    "EQ Library exports the true ${formatSignedDb(representation.playbackGainDb)} dB value unchanged; " +
                    "pyBlackPearl will adjust it when imported. Direct Flash remains independent and uses the actual " +
                    "Black Pearl plan when that plan passes its hardware safety checks.",
            )
        }
    }
    val transformation = (listOf(baseTransformation) + cautions).joinToString(" ")

    return DevicePresetVariant(
        device = ExportDevice.BLACK_PEARL,
        content = content,
        transformation = transformation,
        fidelity = representation.fidelity,
        representationVersion = representation.representationVersion,
    )
}

private fun blackPearlImportType(type: String): String? = when (type) {
    "peak_dip" -> "PK"
    "low_shelf" -> "LS"
    "high_shelf" -> "HS"
    else -> null
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

private const val PYBLACKPEARL_IMPORT_PREAMP_MIN_DB = -16.0
private const val PYBLACKPEARL_IMPORT_PREAMP_MAX_DB = 6.0
