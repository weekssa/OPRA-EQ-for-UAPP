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
 *
 * The +/-10 dB per-band gain range is currently a validated/recommended hardware range, not a text
 * file encoding limit. File export therefore preserves any finite source gain exactly rather than
 * rejecting or clamping it. Direct Flash applies a separate caution for protocol-encodable values
 * outside that validated range.
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

    val fidelity = blackPearlFileFidelity(profile, sourceBands, capabilities)
    val content = buildString {
        appendLine("Preamp: ${formatDb(effectivePreamp)} dB")
        mapped.forEachIndexed { index, band ->
            appendLine(
                "Filter ${index + 1}: ON ${band.type} Fc ${formatHz(band.frequency)} Hz " +
                    "Gain ${formatDb(band.gainDb)} dB Q ${formatQ(band.q)}",
            )
        }
    }.trimEnd()

    val baseTransformation = when (fidelity) {
        DevicePresetFidelity.EXACT ->
            "Source EQ bands and source preamp are preserved in Black Pearl import text."
        DevicePresetFidelity.OPTIMIZED ->
            "EQ Library optimized Black Pearl file export: effective playback headroom is preserved in the Preamp line and only the first ${capabilities.maxBands ?: mapped.size} source-priority bands are included when required by the device limit."
    }
    val outsideValidatedGainRange = mapped.mapIndexedNotNull { index, band ->
        band.gainDb.takeIf { gain -> gain !in capabilities.minGainDb..capabilities.maxGainDb }?.let { gain ->
            "Band ${index + 1} ${formatSignedDb(gain)} dB"
        }
    }
    val transformation = if (outsideValidatedGainRange.isEmpty()) {
        baseTransformation
    } else {
        "$baseTransformation Caution: ${outsideValidatedGainRange.joinToString()} is outside the currently validated Black Pearl filter-gain range; the source value is preserved unchanged and is not clamped."
    }

    return DevicePresetVariant(
        device = ExportDevice.BLACK_PEARL,
        content = content,
        transformation = transformation,
        fidelity = fidelity,
    )
}

private fun blackPearlFileFidelity(
    profile: OpraEqProfile,
    sourceBands: List<OpraBand>,
    capabilities: DeviceEqCapabilities,
): DevicePresetFidelity {
    val exceedsBandCount = capabilities.maxBands?.let { sourceBands.size > it } ?: false
    val usesGeneratedHeadroom = profile.preampGainDb?.takeIf(Double::isFinite) == null
    return if (exceedsBandCount || usesGeneratedHeadroom) {
        DevicePresetFidelity.OPTIMIZED
    } else {
        DevicePresetFidelity.EXACT
    }
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
private fun formatSignedDb(value: Double): String = String.format(Locale.US, "%+.2f", value)
private fun formatQ(value: Double): String = String.format(Locale.US, "%.3f", value)
