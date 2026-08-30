package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class DeviceEqCapabilities(
    val maxBands: Int?,
    val supportedBandTypes: Set<String>,
    val minFrequencyHz: Double = 20.0,
    val maxFrequencyHz: Double = 20_000.0,
    val minGainDb: Double = -12.0,
    val maxGainDb: Double = 12.0,
    val minQ: Double = 0.1,
    val maxQ: Double = 15.0,
    val minPreampDb: Double? = null,
    val maxPreampDb: Double? = null,
) {
    init {
        require(maxBands == null || maxBands > 0) { "maxBands must be positive when specified" }
        require(minFrequencyHz < maxFrequencyHz) { "frequency range must be increasing" }
        require(minGainDb <= maxGainDb) { "gain range must be increasing" }
        require(minQ < maxQ) { "Q range must be increasing" }
        require((minPreampDb == null) == (maxPreampDb == null)) {
            "preamp limits must either both be set or both be null"
        }
        if (minPreampDb != null && maxPreampDb != null) {
            require(minPreampDb <= maxPreampDb) { "preamp range must be increasing" }
        }
    }
}

private val TOPPING_CURRENT_CAPABILITIES = DeviceEqCapabilities(
    maxBands = 10,
    supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
    minGainDb = -12.0,
    maxGainDb = 12.0,
    minQ = 0.1,
    maxQ = 15.0,
    minPreampDb = -12.0,
    maxPreampDb = 12.0,
)

private val BLACK_PEARL_CURRENT_CAPABILITIES = DeviceEqCapabilities(
    maxBands = 10,
    supportedBandTypes = setOf("peak_dip"),
    minGainDb = -10.0,
    maxGainDb = 10.0,
    minQ = 0.1,
    maxQ = 10.0,
)

enum class ExportDevice(
    val folderName: String,
    val extension: String,
    val mimeType: String,
    val validationStatus: String? = null,
    val eqCapabilities: DeviceEqCapabilities? = null,
) {
    UAPP("UAPP", "xml", "application/xml"),
    BLACK_PEARL(
        "TRN Black Pearl",
        "txt",
        "text/plain",
        eqCapabilities = BLACK_PEARL_CURRENT_CAPABILITIES,
    ),
    TOPPING_DX5_II(
        "Topping DX5 II",
        "txt",
        "text/plain",
        validationStatus = "Untested",
        eqCapabilities = TOPPING_CURRENT_CAPABILITIES,
    ),
    TOPPING_DX1_II(
        "Topping DX1 II",
        "txt",
        "text/plain",
        validationStatus = "Untested",
        eqCapabilities = TOPPING_CURRENT_CAPABILITIES,
    ),
}

enum class DevicePresetFidelity {
    EXACT,
    OPTIMIZED,
}

data class DevicePresetVariant(
    val device: ExportDevice,
    val content: String,
    val transformation: String,
    val fidelity: DevicePresetFidelity,
)

fun buildTextDeviceVariant(
    profile: OpraEqProfile,
    device: ExportDevice,
): DevicePresetVariant? = when (device) {
    ExportDevice.UAPP -> null
    ExportDevice.BLACK_PEARL -> {
        val capabilities = requireNotNull(device.eqCapabilities)
        formatBlackPearlPreset(profile, capabilities)?.let { content ->
            val fidelity = determineDeviceFidelity(profile, capabilities)
            DevicePresetVariant(
                device = device,
                content = content,
                fidelity = fidelity,
                transformation = fidelityDescription(
                    fidelity = fidelity,
                    exactDescription = "Source EQ preserved within the Black Pearl device capability profile (${bandLimitLabel(capabilities)}).",
                    optimizedDescription = "EQ Library optimized conversion for Black Pearl using the device capability profile (${bandLimitLabel(capabilities)}; peaking filters only).",
                ),
            )
        }
    }
    ExportDevice.TOPPING_DX5_II,
    ExportDevice.TOPPING_DX1_II -> {
        val capabilities = requireNotNull(device.eqCapabilities)
        formatToppingTunePreset(profile, capabilities)?.let { content ->
            val fidelity = determineDeviceFidelity(profile, capabilities)
            DevicePresetVariant(
                device = device,
                content = content,
                fidelity = fidelity,
                transformation = fidelityDescription(
                    fidelity = fidelity,
                    exactDescription = "Source EQ preserved; only the file syntax was converted to TOPPING Tune / Equalizer APO parameter text (${bandLimitLabel(capabilities)}).",
                    optimizedDescription = "EQ Library optimized conversion to TOPPING Tune / Equalizer APO parameter text using the device capability profile (${bandLimitLabel(capabilities)}).",
                ),
            )
        }
    }
}

fun buildTextDeviceVariants(profile: OpraEqProfile): List<DevicePresetVariant> =
    ExportDevice.entries.mapNotNull { device -> buildTextDeviceVariant(profile, device) }

internal fun determineDeviceFidelity(
    profile: OpraEqProfile,
    capabilities: DeviceEqCapabilities,
): DevicePresetFidelity {
    val bands = profile.bands.orEmpty()
    val exceedsBandCount = capabilities.maxBands?.let { bands.size > it } ?: false
    if (bands.isEmpty() || exceedsBandCount) return DevicePresetFidelity.OPTIMIZED
    if (bands.any { !bandFitsExactly(it, capabilities) }) return DevicePresetFidelity.OPTIMIZED
    if (!preampFitsExactly(profile.preampGainDb, capabilities)) return DevicePresetFidelity.OPTIMIZED
    return DevicePresetFidelity.EXACT
}

private fun bandFitsExactly(
    band: OpraBand,
    capabilities: DeviceEqCapabilities,
): Boolean {
    if (band.type !in capabilities.supportedBandTypes) return false
    val frequency = band.frequency?.takeIf(Double::isFinite) ?: return false
    val gain = band.gainDb?.takeIf(Double::isFinite) ?: return false
    val bandQ = band.q?.takeIf(Double::isFinite) ?: return false
    return frequency in capabilities.minFrequencyHz..capabilities.maxFrequencyHz &&
        gain in capabilities.minGainDb..capabilities.maxGainDb &&
        bandQ in capabilities.minQ..capabilities.maxQ
}

private fun preampFitsExactly(
    sourcePreamp: Double?,
    capabilities: DeviceEqCapabilities,
): Boolean {
    val value = sourcePreamp?.takeIf(Double::isFinite) ?: return false
    val minPreamp = capabilities.minPreampDb
    val maxPreamp = capabilities.maxPreampDb
    return if (minPreamp != null && maxPreamp != null) value in minPreamp..maxPreamp else true
}

internal fun formatToppingTunePreset(
    profile: OpraEqProfile,
    capabilities: DeviceEqCapabilities,
): String? {
    val mapped = profile.bands.orEmpty().mapNotNull { band -> mapToppingBand(band, capabilities) }
    val limited = applyBandLimit(mapped, capabilities.maxBands)
    if (limited.isEmpty()) return null
    val preamp = coercePreamp(profile.preampGainDb, capabilities)
    return buildString {
        appendLine("Preamp: ${db(preamp)} dB")
        limited.forEachIndexed { index, band ->
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

private fun mapToppingBand(
    band: OpraBand,
    capabilities: DeviceEqCapabilities,
): TextBand? {
    if (band.type !in capabilities.supportedBandTypes) return null
    val type = when (band.type) {
        "peak_dip" -> "PK"
        "low_shelf" -> "LSC"
        "high_shelf" -> "HSC"
        else -> return null
    }
    val frequency = band.frequency
        ?.takeIf(Double::isFinite)
        ?.coerceIn(capabilities.minFrequencyHz, capabilities.maxFrequencyHz)
        ?: return null
    val gain = band.gainDb
        ?.takeIf(Double::isFinite)
        ?.coerceIn(capabilities.minGainDb, capabilities.maxGainDb)
        ?: return null
    val bandQ = band.q
        ?.takeIf(Double::isFinite)
        ?.coerceIn(capabilities.minQ, capabilities.maxQ)
        ?: 0.707.coerceIn(capabilities.minQ, capabilities.maxQ)
    return TextBand(type, frequency, gain, bandQ)
}

private data class BlackPearlCandidate(
    val order: Int,
    val frequency: Double,
    val gainDb: Double,
    val q: Double,
    val score: Double,
)

internal fun formatBlackPearlPreset(
    profile: OpraEqProfile,
    capabilities: DeviceEqCapabilities,
): String? {
    val candidates = mutableListOf<BlackPearlCandidate>()
    profile.bands.orEmpty().forEachIndexed { index, source ->
        val frequency = source.frequency
            ?.takeIf(Double::isFinite)
            ?.coerceIn(capabilities.minFrequencyHz, capabilities.maxFrequencyHz)
            ?: return@forEachIndexed
        val gain = source.gainDb
            ?.takeIf(Double::isFinite)
            ?.coerceIn(capabilities.minGainDb, capabilities.maxGainDb)
            ?: return@forEachIndexed
        val bandQ = source.q
            ?.takeIf(Double::isFinite)
            ?.coerceIn(capabilities.minQ, capabilities.maxQ)
            ?: 0.707.coerceIn(capabilities.minQ, capabilities.maxQ)
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
                    frequency = max(capabilities.minFrequencyHz, frequency / 3.0),
                    gainDb = (gain * 0.85).coerceIn(capabilities.minGainDb, capabilities.maxGainDb),
                    q = 0.35.coerceIn(capabilities.minQ, capabilities.maxQ),
                    score = abs(gain) * 0.95,
                )
                candidates += BlackPearlCandidate(
                    order = index * 10 + 1,
                    frequency = max(capabilities.minFrequencyHz, frequency / 1.35),
                    gainDb = (gain * 0.55).coerceIn(capabilities.minGainDb, capabilities.maxGainDb),
                    q = 0.55.coerceIn(capabilities.minQ, capabilities.maxQ),
                    score = abs(gain) * 0.7,
                )
            }
            "high_shelf" -> {
                candidates += BlackPearlCandidate(
                    order = index * 10,
                    frequency = min(capabilities.maxFrequencyHz, frequency * 1.35),
                    gainDb = (gain * 0.55).coerceIn(capabilities.minGainDb, capabilities.maxGainDb),
                    q = 0.55.coerceIn(capabilities.minQ, capabilities.maxQ),
                    score = abs(gain) * 0.7,
                )
                candidates += BlackPearlCandidate(
                    order = index * 10 + 1,
                    frequency = min(capabilities.maxFrequencyHz, frequency * 3.0),
                    gainDb = (gain * 0.85).coerceIn(capabilities.minGainDb, capabilities.maxGainDb),
                    q = 0.35.coerceIn(capabilities.minQ, capabilities.maxQ),
                    score = abs(gain) * 0.95,
                )
            }
        }
    }
    if (candidates.isEmpty()) return null

    val chosen = applyBandLimit(
        candidates.sortedByDescending(BlackPearlCandidate::score),
        capabilities.maxBands,
    ).sortedBy(BlackPearlCandidate::order)
    val preamp = coercePreamp(profile.preampGainDb, capabilities)

    return buildString {
        appendLine("Preamp: ${db(preamp)} dB")
        chosen.forEachIndexed { index, band ->
            appendLine(
                "Filter ${index + 1}: ON PK Fc ${hz(band.frequency)} Hz Gain ${db(band.gainDb)} dB Q ${q(band.q)}",
            )
        }
    }.trimEnd()
}

private fun <T> applyBandLimit(items: List<T>, maxBands: Int?): List<T> =
    maxBands?.let(items::take) ?: items

private fun coercePreamp(
    sourcePreamp: Double?,
    capabilities: DeviceEqCapabilities,
): Double {
    val value = sourcePreamp?.takeIf(Double::isFinite) ?: 0.0
    val minPreamp = capabilities.minPreampDb
    val maxPreamp = capabilities.maxPreampDb
    return if (minPreamp != null && maxPreamp != null) value.coerceIn(minPreamp, maxPreamp) else value
}

private fun fidelityDescription(
    fidelity: DevicePresetFidelity,
    exactDescription: String,
    optimizedDescription: String,
): String = when (fidelity) {
    DevicePresetFidelity.EXACT -> exactDescription
    DevicePresetFidelity.OPTIMIZED -> optimizedDescription
}

private fun bandLimitLabel(capabilities: DeviceEqCapabilities): String =
    capabilities.maxBands?.let { "up to $it filters" } ?: "no fixed filter-count limit"

private fun hz(value: Double): String =
    if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value) else String.format(Locale.US, "%.1f", value)

private fun db(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun q(value: Double): String = String.format(Locale.US, "%.3f", value)
