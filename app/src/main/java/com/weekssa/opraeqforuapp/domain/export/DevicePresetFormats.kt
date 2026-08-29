package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class ExportDevice(
    val folderName: String,
    val extension: String,
    val mimeType: String,
) {
    UAPP("UAPP", "xml", "application/xml"),
    BLACK_PEARL("TRN Black Pearl", "txt", "text/plain"),
    TOPPING_DX5_II("Topping DX5 II", "txt", "text/plain"),
    TOPPING_DX1_II("Topping DX1 II", "txt", "text/plain"),
}

data class DevicePresetVariant(
    val device: ExportDevice,
    val content: String,
    val transformation: String,
)

fun buildTextDeviceVariants(profile: OpraEqProfile): List<DevicePresetVariant> {
    val topping = formatToppingTunePreset(profile)
    val blackPearl = formatBlackPearlPreset(profile)
    return buildList {
        if (blackPearl != null) {
            add(
                DevicePresetVariant(
                    device = ExportDevice.BLACK_PEARL,
                    content = blackPearl,
                    transformation = "Optimized to a maximum of 10 peaking filters for Black Pearl compatibility.",
                ),
            )
        }
        if (topping != null) {
            add(
                DevicePresetVariant(
                    device = ExportDevice.TOPPING_DX5_II,
                    content = topping,
                    transformation = "Converted to TOPPING Tune / Equalizer APO parameter text.",
                ),
            )
            add(
                DevicePresetVariant(
                    device = ExportDevice.TOPPING_DX1_II,
                    content = topping,
                    transformation = "Converted to TOPPING Tune / Equalizer APO parameter text.",
                ),
            )
        }
    }
}

private fun formatToppingTunePreset(profile: OpraEqProfile): String? {
    val mapped = profile.bands.orEmpty().mapNotNull(::mapToppingBand).take(MAX_BANDS)
    if (mapped.isEmpty()) return null
    val preamp = profile.preampGainDb?.takeIf(Double::isFinite)?.coerceIn(-12.0, 12.0) ?: 0.0
    return buildString {
        appendLine("Preamp: ${db(preamp)} dB")
        mapped.forEachIndexed { index, band ->
            appendLine(
                "Filter ${index + 1}: ON ${band.type} Fc ${hz(band.frequency)} Hz Gain ${db(band.gainDb)} dB Q ${q(band.q)}",
            )
        }
    }.trimEnd()
}

private data class TextBand(
    val type: String,
    val frequency: Double,
    val gainDb: Double,
    val q: Double,
)

private fun mapToppingBand(band: OpraBand): TextBand? {
    val type = when (band.type) {
        "peak_dip" -> "PK"
        "low_shelf" -> "LSC"
        "high_shelf" -> "HSC"
        else -> return null
    }
    val frequency = band.frequency?.takeIf(Double::isFinite)?.coerceIn(20.0, 20_000.0) ?: return null
    val gain = band.gainDb?.takeIf(Double::isFinite)?.coerceIn(-12.0, 12.0) ?: return null
    val bandQ = band.q?.takeIf(Double::isFinite)?.coerceIn(0.1, 15.0) ?: 0.707
    return TextBand(type, frequency, gain, bandQ)
}

private data class BlackPearlCandidate(
    val order: Int,
    val frequency: Double,
    val gainDb: Double,
    val q: Double,
    val score: Double,
)

private fun formatBlackPearlPreset(profile: OpraEqProfile): String? {
    val candidates = mutableListOf<BlackPearlCandidate>()
    profile.bands.orEmpty().forEachIndexed { index, source ->
        val frequency = source.frequency?.takeIf(Double::isFinite)?.coerceIn(20.0, 20_000.0) ?: return@forEachIndexed
        val gain = source.gainDb?.takeIf(Double::isFinite)?.coerceIn(-10.0, 10.0) ?: return@forEachIndexed
        val bandQ = source.q?.takeIf(Double::isFinite)?.coerceIn(0.1, 10.0) ?: 0.707
        when (source.type) {
            "peak_dip" -> candidates += BlackPearlCandidate(
                order = index * 10,
                frequency = frequency,
                gainDb = gain,
                q = bandQ,
                score = abs(gain) * 1.25,
            )
            "low_shelf" -> {
                candidates += BlackPearlCandidate(
                    order = index * 10,
                    frequency = max(20.0, frequency / 3.0),
                    gainDb = (gain * 0.85).coerceIn(-10.0, 10.0),
                    q = 0.35,
                    score = abs(gain) * 0.95,
                )
                candidates += BlackPearlCandidate(
                    order = index * 10 + 1,
                    frequency = max(20.0, frequency / 1.35),
                    gainDb = (gain * 0.55).coerceIn(-10.0, 10.0),
                    q = 0.55,
                    score = abs(gain) * 0.7,
                )
            }
            "high_shelf" -> {
                candidates += BlackPearlCandidate(
                    order = index * 10,
                    frequency = min(20_000.0, frequency * 1.35),
                    gainDb = (gain * 0.55).coerceIn(-10.0, 10.0),
                    q = 0.55,
                    score = abs(gain) * 0.7,
                )
                candidates += BlackPearlCandidate(
                    order = index * 10 + 1,
                    frequency = min(20_000.0, frequency * 3.0),
                    gainDb = (gain * 0.85).coerceIn(-10.0, 10.0),
                    q = 0.35,
                    score = abs(gain) * 0.95,
                )
            }
        }
    }
    if (candidates.isEmpty()) return null

    val chosen = candidates
        .sortedByDescending(BlackPearlCandidate::score)
        .take(MAX_BANDS)
        .sortedBy(BlackPearlCandidate::order)
    val preamp = profile.preampGainDb?.takeIf(Double::isFinite) ?: 0.0

    return buildString {
        appendLine("Preamp: ${db(preamp)} dB")
        chosen.forEachIndexed { index, band ->
            appendLine(
                "Filter ${index + 1}: ON PK Fc ${hz(band.frequency)} Hz Gain ${db(band.gainDb)} dB Q ${q(band.q)}",
            )
        }
    }.trimEnd()
}

private fun hz(value: Double): String =
    if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value) else String.format(Locale.US, "%.1f", value)

private fun db(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun q(value: Double): String = String.format(Locale.US, "%.3f", value)

private const val MAX_BANDS = 10
