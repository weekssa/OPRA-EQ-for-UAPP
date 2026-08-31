package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import java.util.Locale

/**
 * Builds the file representation for an external Black Pearl preset importer.
 *
 * File export and direct USB Flash remain independent delivery paths. Both preserve the effective
 * source preamp / EQ Library safety headroom; direct Flash applies that value through the approved
 * Black Pearl playback-gain command while file export preserves it as a Preamp line.
 */
internal fun buildFileExportDeviceVariant(
    profile: OpraEqProfile,
    device: ExportDevice,
): DevicePresetVariant? = when (device) {
    ExportDevice.BLACK_PEARL -> buildBlackPearlFileExportVariant(profile)
    else -> buildTextDeviceVariant(profile, device)
}

private fun buildBlackPearlFileExportVariant(profile: OpraEqProfile): DevicePresetVariant? {
    val capabilities = requireNotNull(ExportDevice.BLACK_PEARL.eqCapabilities)
    val effectivePreamp = profile.effectivePlaybackPreampDb()
        ?.takeIf(Double::isFinite)
        ?: return null
    val sourceBands = profile.bands.orEmpty()
    if (sourceBands.isEmpty()) return null

    val selectedBands = capabilities.maxBands?.let(sourceBands::take) ?: sourceBands
    val mapped = selectedBands.map { band ->
        mapBlackPearlFileBand(band, capabilities) ?: return null
    }
    if (mapped.isEmpty()) return null

    val fidelity = determineDeviceFidelity(profile, capabilities)
    val content = buildString {
        appendLine("Preamp: ${formatDb(effectivePreamp)} dB")
        mapped.forEachIndexed { index, band ->
            appendLine(
                "Filter ${index + 1}: ON ${band.type} Fc ${formatHz(band.frequency)} Hz " +
                    "Gain ${formatDb(band.gainDb)} dB Q ${formatQ(band.q)}",
            )
        }
    }.trimEnd()

    val transformation = when (fidelity) {
        DevicePresetFidelity.EXACT ->
            "Source EQ bands and source preamp are preserved in Black Pearl import text."
        DevicePresetFidelity.OPTIMIZED ->
            "EQ Library optimized Black Pearl file export: effective playback headroom is preserved in the Preamp line and only the first ${capabilities.maxBands ?: mapped.size} source-priority bands are included when required by the device limit."
    }

    return DevicePresetVariant(
        device = ExportDevice.BLACK_PEARL,
        content = content,
        transformation = transformation,
        fidelity = fidelity,
    )
}

private data class BlackPearlFileBand(
    val type: String,
    val frequency: Double,
    val gainDb: Double,
    val q: Double,
)

private fun mapBlackPearlFileBand(
    band: OpraBand,
    capabilities: DeviceEqCapabilities,
): BlackPearlFileBand? {
    if (band.type !in capabilities.supportedBandTypes) return null
    val type = parametricType(band.type) ?: return null
    val frequency = band.frequency?.takeIf(Double::isFinite) ?: return null
    val gain = band.gainDb?.takeIf(Double::isFinite) ?: return null
    val q = band.q?.takeIf(Double::isFinite) ?: return null
    if (frequency !in capabilities.minFrequencyHz..capabilities.maxFrequencyHz) return null
    if (gain !in capabilities.minGainDb..capabilities.maxGainDb) return null
    if (q !in capabilities.minQ..capabilities.maxQ) return null
    return BlackPearlFileBand(type, frequency, gain, q)
}

private fun formatHz(value: Double): String =
    if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.1f", value)
    }

private fun formatDb(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun formatQ(value: Double): String = String.format(Locale.US, "%.3f", value)
