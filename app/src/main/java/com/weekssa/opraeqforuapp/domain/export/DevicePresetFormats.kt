package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandDeviceSpec
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandQuantization
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

enum class OutputCategory(val sortOrder: Int, val heading: String) {
    HARDWARE_DAC(0, "Hardware DACs"),
    APP(1, "Apps"),
    UNIVERSAL_FORMAT(2, "Universal formats"),
}

enum class OutputFormatKind {
    HARDWARE_ONLY,
    BLACK_PEARL_TEXT,
    TONEBOOSTERS_XML,
    PARAMETRIC_TEXT,
    GRAPHIC_EQ_127,
}

private val UAPP_CURRENT_CAPABILITIES = DeviceEqCapabilities(
    maxBands = 10,
    supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
    minFrequencyHz = 16.0,
    maxFrequencyHz = 20_000.0,
    minGainDb = -20.0,
    maxGainDb = 20.0,
    minQ = 0.1,
    maxQ = 10.0,
    minPreampDb = -20.0,
    maxPreampDb = 20.0,
)

private val GENERIC_PARAMETRIC_CAPABILITIES = DeviceEqCapabilities(
    maxBands = null,
    supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
    minFrequencyHz = 10.0,
    maxFrequencyHz = 40_000.0,
    minGainDb = -60.0,
    maxGainDb = 60.0,
    minQ = 0.01,
    maxQ = 100.0,
    minPreampDb = -60.0,
    maxPreampDb = 24.0,
)

private val EASY_EFFECTS_CURRENT_CAPABILITIES = GENERIC_PARAMETRIC_CAPABILITIES.copy(maxBands = 32)
private val POWERAMP_CURRENT_CAPABILITIES = GENERIC_PARAMETRIC_CAPABILITIES.copy(maxBands = 64)

private val TOPPING_CURRENT_CAPABILITIES = DeviceEqCapabilities(
    maxBands = 10,
    supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
    minFrequencyHz = 20.0,
    maxFrequencyHz = 20_000.0,
    minGainDb = -12.0,
    maxGainDb = 12.0,
    minQ = 0.1,
    maxQ = 15.0,
    minPreampDb = -12.0,
    maxPreampDb = 12.0,
)

private val BLACK_PEARL_CURRENT_CAPABILITIES = DeviceEqCapabilities(
    maxBands = 10,
    supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
    minFrequencyHz = 20.0,
    maxFrequencyHz = 20_000.0,
    minGainDb = -128.0,
    maxGainDb = 127.99609375,
    minQ = 0.1,
    maxQ = 10.0,
)

private val FIIO_JA11_CURRENT_CAPABILITIES = DeviceEqCapabilities(
    maxBands = 5,
    supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
    minFrequencyHz = 20.0,
    maxFrequencyHz = 20_000.0,
    minGainDb = -24.0,
    maxGainDb = 12.0,
    minQ = 0.1,
    maxQ = 10.0,
    minPreampDb = -12.0,
    maxPreampDb = 12.0,
)

private val JCALLY_JM12_CURRENT_CAPABILITIES = DeviceEqCapabilities(
    maxBands = 5,
    supportedBandTypes = setOf("peak_dip", "low_shelf", "high_shelf"),
    minFrequencyHz = 20.0,
    maxFrequencyHz = 20_000.0,
    minGainDb = -30.0,
    maxGainDb = 30.0,
    minQ = 0.1,
    maxQ = 20.0,
    minPreampDb = -24.0,
    maxPreampDb = 12.0,
)

/**
 * One registry drives Settings, the output selector, format generation, capability checks, and
 * expansion. Existing enum names are intentionally stable because they are persisted in DataStore.
 */
enum class ExportDevice(
    val folderName: String,
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val category: OutputCategory,
    val settingsSubtitle: String,
    val formatKind: OutputFormatKind,
    val validationStatus: String? = null,
    val eqCapabilities: DeviceEqCapabilities? = null,
    val selectableInV03: Boolean = true,
    val supportsFileExport: Boolean = true,
) {
    UAPP(
        folderName = "USB Audio Player PRO - ToneBoosters",
        extension = "xml",
        mimeType = "application/xml",
        displayName = "USB Audio Player PRO / ToneBoosters",
        category = OutputCategory.APP,
        settingsSubtitle = ".xml · ToneBoosters preset · 10 bands",
        formatKind = OutputFormatKind.TONEBOOSTERS_XML,
        eqCapabilities = UAPP_CURRENT_CAPABILITIES,
    ),
    BLACK_PEARL(
        folderName = "TRN Black Pearl",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "TRN Black Pearl",
        category = OutputCategory.HARDWARE_DAC,
        settingsSubtitle = ".txt · AutoEq preset · 10-band PEQ",
        formatKind = OutputFormatKind.BLACK_PEARL_TEXT,
        eqCapabilities = BLACK_PEARL_CURRENT_CAPABILITIES,
    ),
    FIIO_JA11(
        folderName = "FiiO JA11",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "FiiO JA11",
        category = OutputCategory.HARDWARE_DAC,
        settingsSubtitle = "5-band PEQ · Direct Flash · no verified import file",
        formatKind = OutputFormatKind.HARDWARE_ONLY,
        validationStatus = "Hardware validation pending",
        eqCapabilities = FIIO_JA11_CURRENT_CAPABILITIES,
        supportsFileExport = false,
    ),
    JCALLY_JM12(
        folderName = "JCALLY JM12",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "JCALLY JM12",
        category = OutputCategory.HARDWARE_DAC,
        settingsSubtitle = "5-band PEQ · Direct Flash · no verified import file",
        formatKind = OutputFormatKind.HARDWARE_ONLY,
        validationStatus = "Hardware validation pending · persistence pending",
        eqCapabilities = JCALLY_JM12_CURRENT_CAPABILITIES,
        supportsFileExport = false,
    ),
    UNIVERSAL_PARAMETRIC(
        folderName = "AutoEq - Equalizer APO Parametric",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "AutoEq / Equalizer APO Parametric",
        category = OutputCategory.UNIVERSAL_FORMAT,
        settingsSubtitle = ".txt · Portable parametric EQ",
        formatKind = OutputFormatKind.PARAMETRIC_TEXT,
        eqCapabilities = GENERIC_PARAMETRIC_CAPABILITIES,
    ),
    POWERAMP(
        folderName = "Poweramp - Poweramp Equalizer",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "Poweramp / Poweramp Equalizer",
        category = OutputCategory.APP,
        settingsSubtitle = ".txt · AutoEq parametric · up to 64 bands",
        formatKind = OutputFormatKind.PARAMETRIC_TEXT,
        eqCapabilities = POWERAMP_CURRENT_CAPABILITIES,
    ),
    TOPPING_TUNE(
        folderName = "TOPPING Tune",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "TOPPING Tune",
        category = OutputCategory.APP,
        settingsSubtitle = ".txt · AutoEq parametric · 10 bands",
        formatKind = OutputFormatKind.PARAMETRIC_TEXT,
        validationStatus = "Official AutoEq import path",
        eqCapabilities = TOPPING_CURRENT_CAPABILITIES,
    ),
    WAVELET(
        folderName = "Wavelet",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "Wavelet",
        category = OutputCategory.APP,
        settingsSubtitle = ".txt · GraphicEQ · 127 points",
        formatKind = OutputFormatKind.GRAPHIC_EQ_127,
    ),
    EASY_EFFECTS(
        folderName = "EasyEffects",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "EasyEffects",
        category = OutputCategory.APP,
        settingsSubtitle = ".txt · Equalizer APO parametric · up to 32 bands",
        formatKind = OutputFormatKind.PARAMETRIC_TEXT,
        eqCapabilities = EASY_EFFECTS_CURRENT_CAPABILITIES,
    ),
    EQUALIZER_APO(
        folderName = "Equalizer APO",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "Equalizer APO",
        category = OutputCategory.APP,
        settingsSubtitle = ".txt · Parametric EQ",
        formatKind = OutputFormatKind.PARAMETRIC_TEXT,
        eqCapabilities = GENERIC_PARAMETRIC_CAPABILITIES,
    ),
    UNIVERSAL_GRAPHIC_EQ(
        folderName = "AutoEq GraphicEQ",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "AutoEq GraphicEQ",
        category = OutputCategory.UNIVERSAL_FORMAT,
        settingsSubtitle = ".txt · Portable 127-point GraphicEQ",
        formatKind = OutputFormatKind.GRAPHIC_EQ_127,
    ),
    TOPPING_DX5_II(
        folderName = "Topping DX5 II",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "TOPPING DX5 II",
        category = OutputCategory.HARDWARE_DAC,
        settingsSubtitle = "Hardware validation pending",
        formatKind = OutputFormatKind.PARAMETRIC_TEXT,
        validationStatus = "Untested",
        eqCapabilities = TOPPING_CURRENT_CAPABILITIES,
        selectableInV03 = false,
    ),
    TOPPING_DX1_II(
        folderName = "Topping DX1 II",
        extension = "txt",
        mimeType = "text/plain",
        displayName = "TOPPING DX1 II",
        category = OutputCategory.HARDWARE_DAC,
        settingsSubtitle = "Hardware validation pending",
        formatKind = OutputFormatKind.PARAMETRIC_TEXT,
        validationStatus = "Untested",
        eqCapabilities = TOPPING_CURRENT_CAPABILITIES,
        selectableInV03 = false,
    );

    val isHardwareOutput: Boolean get() = category == OutputCategory.HARDWARE_DAC

    companion object {
        val selectableOutputs: List<ExportDevice> = entries
            .filter(ExportDevice::selectableInV03)
            .sortedWith(
                compareBy<ExportDevice> { it.category.sortOrder }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName },
            )
    }
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
    val representationVersion: Int = 1,
)

fun buildTextDeviceVariant(
    profile: OpraEqProfile,
    device: ExportDevice,
): DevicePresetVariant? = when (device) {
    ExportDevice.UAPP,
    ExportDevice.BLACK_PEARL,
    ExportDevice.FIIO_JA11,
    ExportDevice.JCALLY_JM12 -> null

    ExportDevice.UNIVERSAL_PARAMETRIC,
    ExportDevice.EQUALIZER_APO -> buildParametricVariant(profile, device)

    ExportDevice.POWERAMP,
    ExportDevice.EASY_EFFECTS,
    ExportDevice.TOPPING_TUNE -> buildLimitedParametricVariant(profile, device)

    ExportDevice.WAVELET -> formatWaveletGraphicEq(profile)?.let { content ->
        DevicePresetVariant(
            device = device,
            content = content,
            fidelity = DevicePresetFidelity.OPTIMIZED,
            transformation = "EQ Library rendered the parametric source response to Wavelet's fixed 127-point GraphicEQ import grid. Wavelet normalizes imported GraphicEQ data, so source preamp is not represented as an independent control.",
        )
    }

    ExportDevice.UNIVERSAL_GRAPHIC_EQ -> formatWaveletGraphicEq(profile)?.let { content ->
        DevicePresetVariant(
            device = device,
            content = content,
            fidelity = DevicePresetFidelity.OPTIMIZED,
            transformation = "EQ Library rendered the complete parametric response to the portable 127-point AutoEq GraphicEQ text grid.",
        )
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

private fun buildParametricVariant(
    profile: OpraEqProfile,
    device: ExportDevice,
): DevicePresetVariant? {
    val capabilities = requireNotNull(device.eqCapabilities)
    return formatParametricText(profile, capabilities)?.let { content ->
        val fidelity = determineDeviceFidelity(profile, capabilities)
        val exactDescription = when (device) {
            ExportDevice.EQUALIZER_APO -> "Source parametric EQ preserved in Equalizer APO configuration text."
            else -> "Source parametric EQ preserved in standard AutoEq/Equalizer APO-style text."
        }
        DevicePresetVariant(
            device = device,
            content = content,
            fidelity = fidelity,
            transformation = fidelityDescription(
                fidelity,
                exactDescription,
                "EQ Library normalized the source to ${device.displayName}'s parametric text capability profile.",
            ),
        )
    }
}

private fun buildLimitedParametricVariant(
    profile: OpraEqProfile,
    device: ExportDevice,
): DevicePresetVariant? {
    val capabilities = requireNotNull(device.eqCapabilities)
    val maxBands = requireNotNull(capabilities.maxBands)
    val quantization = when (device) {
        ExportDevice.TOPPING_TUNE -> FiveBandQuantization(
            frequencyStepHz = 1.0,
            gainStepDb = 0.1,
            qStep = 0.001,
            preampStepDb = 0.1,
        )
        else -> FiveBandQuantization(
            frequencyStepHz = 0.1,
            gainStepDb = 0.01,
            qStep = 0.001,
            preampStepDb = 0.01,
        )
    }
    val spec = FiveBandDeviceSpec(
        stableId = "app-${device.name.lowercase(Locale.US)}",
        displayName = device.displayName,
        capabilities = capabilities,
        quantization = quantization,
        representationVersion = if (device == ExportDevice.TOPPING_TUNE) 1 else 2,
    )
    val representation = when (val result = Kt02h20FiveBandOptimizer.optimize(profile, spec)) {
        is FiveBandOptimizationResult.NotSuitable -> return null
        is FiveBandOptimizationResult.Ready -> result.representation
    }
    val bands = representation.bands.map { band ->
        TextBand(
            type = parametricType(band.type) ?: return null,
            frequency = band.frequencyHz,
            gainDb = band.gainDb,
            q = band.q,
        )
    }
    val content = renderParametricText(representation.playbackGainDb, bands)
    val exactText = when (device) {
        ExportDevice.EASY_EFFECTS -> "Source EQ is directly representable in EasyEffects' Equalizer APO parametric import profile."
        ExportDevice.POWERAMP -> "Source EQ is directly representable in AutoEq parametric text accepted by Poweramp/Poweramp Equalizer."
        ExportDevice.TOPPING_TUNE -> "Source EQ is directly representable in TOPPING Tune's documented AutoEq import profile."
        else -> "Source EQ is directly representable in ${device.displayName}."
    }
    val optimizedText = when {
        representation.usedResponseFit ->
            "EQ Library fitted the complete source response to ${device.displayName}'s $maxBands-band target (RMS ${metricDb(representation.rmsErrorDb)} dB, max ${metricDb(representation.maxAbsoluteErrorDb)} dB); no source bands were silently truncated."
        representation.usesGeneratedHeadroom && representation.usesNativeQuantization ->
            "EQ Library preserved the source filter structure, rounded values to ${device.displayName}'s import precision, and generated ${db(representation.playbackGainDb)} dB of target-specific safety headroom because the source has no preamp."
        representation.usesGeneratedHeadroom ->
            "EQ Library preserved the source filter structure and generated ${db(representation.playbackGainDb)} dB of target-specific safety headroom because the source has no preamp."
        representation.usesNativeQuantization ->
            "EQ Library preserved the source filter structure and rounded values to ${device.displayName}'s supported import precision."
        else -> "EQ Library produced a deterministic target-specific representation for ${device.displayName}."
    }
    return DevicePresetVariant(
        device = device,
        content = content,
        fidelity = representation.fidelity,
        transformation = fidelityDescription(representation.fidelity, exactText, optimizedText),
        representationVersion = representation.representationVersion,
    )
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

/**
 * Legacy nonselectable TOPPING-device reference formatter. Product-facing TOPPING Tune export uses
 * the shared finite-target adapter above and never uses this clamping/first-N reference path.
 */
internal fun formatToppingTunePreset(
    profile: OpraEqProfile,
    capabilities: DeviceEqCapabilities,
): String? {
    val bands = profile.bands.orEmpty()
    if (bands.isEmpty() || bands.any { it.type !in capabilities.supportedBandTypes }) return null
    val mapped = bands.map { band -> mapParametricBand(band, capabilities) ?: return null }
    val limited = applyBandLimit(mapped, capabilities.maxBands)
    if (limited.isEmpty()) return null
    val preamp = coercePreamp(profile.effectivePlaybackPreampDb(), capabilities)
    return renderParametricText(preamp, limited)
}

internal fun formatParametricText(
    profile: OpraEqProfile,
    capabilities: DeviceEqCapabilities,
): String? {
    val sourceBands = profile.bands.orEmpty()
    if (sourceBands.isEmpty()) return null
    if (capabilities.maxBands?.let { sourceBands.size > it } == true) return null
    val mapped = sourceBands.map { band ->
        if (band.type !in capabilities.supportedBandTypes) return null
        mapParametricBandExact(band, capabilities) ?: return null
    }
    val preamp = profile.effectivePlaybackPreampDb()?.takeIf(Double::isFinite) ?: 0.0
    val minPreamp = capabilities.minPreampDb
    val maxPreamp = capabilities.maxPreampDb
    if (minPreamp != null && maxPreamp != null && preamp !in minPreamp..maxPreamp) return null
    return renderParametricText(preamp, mapped)
}

private data class TextBand(
    val type: String,
    val frequency: Double,
    val gainDb: Double,
    val q: Double,
)

private fun mapParametricBandExact(
    band: OpraBand,
    capabilities: DeviceEqCapabilities,
): TextBand? {
    val type = parametricType(band.type) ?: return null
    val frequency = band.frequency?.takeIf(Double::isFinite) ?: return null
    val gain = band.gainDb?.takeIf(Double::isFinite) ?: return null
    val bandQ = band.q?.takeIf(Double::isFinite) ?: return null
    if (frequency !in capabilities.minFrequencyHz..capabilities.maxFrequencyHz) return null
    if (gain !in capabilities.minGainDb..capabilities.maxGainDb) return null
    if (bandQ !in capabilities.minQ..capabilities.maxQ) return null
    return TextBand(type, frequency, gain, bandQ)
}

private fun mapParametricBand(
    band: OpraBand,
    capabilities: DeviceEqCapabilities,
): TextBand? {
    val type = parametricType(band.type) ?: return null
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
        ?: return null
    return TextBand(type, frequency, gain, bandQ)
}

private fun renderParametricText(preamp: Double, bands: List<TextBand>): String = buildString {
    appendLine("Preamp: ${db(preamp)} dB")
    bands.forEachIndexed { index, band ->
        appendLine(
            "Filter ${index + 1}: ON ${band.type} Fc ${hz(band.frequency)} Hz Gain ${db(band.gainDb)} dB Q ${q(band.q)}",
        )
    }
}.trimEnd()

internal fun formatBlackPearlPreset(
    profile: OpraEqProfile,
    capabilities: DeviceEqCapabilities,
): String? {
    val effectivePreamp = profile.effectivePlaybackPreampDb()
        ?.takeIf(Double::isFinite)
        ?: return null
    if (kotlin.math.abs(effectivePreamp) > BLACK_PEARL_PREAMP_ZERO_TOLERANCE_DB) return null

    val sourceBands = profile.bands.orEmpty()
    if (sourceBands.isEmpty()) return null
    val selectedBands = applyBandLimit(sourceBands, capabilities.maxBands)
    val mapped = selectedBands.map { band ->
        if (band.type !in capabilities.supportedBandTypes) return null
        mapParametricBandExact(band, capabilities) ?: return null
    }
    if (mapped.isEmpty()) return null
    return renderParametricText(0.0, mapped)
}

internal fun formatWaveletGraphicEq(profile: OpraEqProfile): String? {
    val bands = profile.bands.orEmpty()
    if (bands.isEmpty()) return null
    if (bands.any { it.type !in setOf("peak_dip", "low_shelf", "high_shelf") }) return null
    if (bands.any { it.frequency?.isFinite() != true || it.gainDb?.isFinite() != true || it.q?.isFinite() != true }) {
        return null
    }
    val samples = WAVELET_FREQUENCIES.map { frequency ->
        val gain = responseDb(bands, frequency) ?: return null
        "$frequency ${graphicDb(gain)}"
    }
    return "GraphicEQ: ${samples.joinToString("; ")}"
}

private data class Biquad(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double,
)

private fun responseDb(bands: List<OpraBand>, frequency: Int, sampleRate: Double = 96_000.0): Double? {
    val omega = 2.0 * PI * frequency / sampleRate
    val c1 = cos(omega)
    val s1 = sin(omega)
    val c2 = cos(2.0 * omega)
    val s2 = sin(2.0 * omega)
    var real = 1.0
    var imag = 0.0
    for (band in bands) {
        val biquad = rbjBiquad(band, sampleRate) ?: return null
        val numeratorReal = biquad.b0 + biquad.b1 * c1 + biquad.b2 * c2
        val numeratorImag = -(biquad.b1 * s1 + biquad.b2 * s2)
        val denominatorReal = 1.0 + biquad.a1 * c1 + biquad.a2 * c2
        val denominatorImag = -(biquad.a1 * s1 + biquad.a2 * s2)
        val denom = denominatorReal * denominatorReal + denominatorImag * denominatorImag
        if (denom <= 0.0 || !denom.isFinite()) return null
        val hReal = (numeratorReal * denominatorReal + numeratorImag * denominatorImag) / denom
        val hImag = (numeratorImag * denominatorReal - numeratorReal * denominatorImag) / denom
        val nextReal = real * hReal - imag * hImag
        val nextImag = real * hImag + imag * hReal
        real = nextReal
        imag = nextImag
    }
    val magnitudeSquared = real * real + imag * imag
    if (magnitudeSquared <= 0.0 || !magnitudeSquared.isFinite()) return null
    return 10.0 * ln(magnitudeSquared) / ln(10.0)
}

private fun rbjBiquad(band: OpraBand, sampleRate: Double): Biquad? {
    val frequency = band.frequency?.takeIf(Double::isFinite) ?: return null
    val gain = band.gainDb?.takeIf(Double::isFinite) ?: return null
    val q = band.q?.takeIf(Double::isFinite)?.takeIf { it > 0.0 } ?: return null
    if (frequency <= 0.0 || frequency >= sampleRate / 2.0) return null
    val a = 10.0.pow(gain / 40.0)
    val w0 = 2.0 * PI * frequency / sampleRate
    val cw = cos(w0)
    val sw = sin(w0)
    val alpha = sw / (2.0 * q)

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
    return Biquad(
        b0 = values[0] / a0,
        b1 = values[1] / a0,
        b2 = values[2] / a0,
        a1 = values[4] / a0,
        a2 = values[5] / a0,
    )
}

private fun <T> applyBandLimit(items: List<T>, maxBands: Int?): List<T> =
    maxBands?.let(items::take) ?: items

private fun coercePreamp(
    playbackPreamp: Double?,
    capabilities: DeviceEqCapabilities,
): Double {
    val value = playbackPreamp?.takeIf(Double::isFinite) ?: 0.0
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
private fun graphicDb(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun metricDb(value: Double): String = String.format(Locale.US, "%.2f", value)

private const val BLACK_PEARL_PREAMP_ZERO_TOLERANCE_DB = 0.000_001

private val WAVELET_FREQUENCIES = listOf(
    20, 21, 22, 23, 24, 26, 27, 29, 30, 32, 34, 36, 38, 40, 43, 45, 48, 50, 53, 56, 59, 63,
    66, 70, 74, 78, 83, 87, 92, 97, 103, 109, 115, 121, 128, 136, 143, 151, 160, 169, 178, 188,
    199, 210, 222, 235, 248, 262, 277, 292, 309, 326, 345, 364, 385, 406, 429, 453, 479, 506, 534,
    565, 596, 630, 665, 703, 743, 784, 829, 875, 924, 977, 1032, 1090, 1151, 1216, 1284, 1357, 1433,
    1514, 1599, 1689, 1784, 1885, 1991, 2103, 2221, 2347, 2479, 2618, 2766, 2921, 3086, 3260, 3443,
    3637, 3842, 4058, 4287, 4528, 4783, 5052, 5337, 5637, 5955, 6290, 6644, 7018, 7414, 7831, 8272,
    8738, 9230, 9749, 10298, 10878, 11490, 12137, 12821, 13543, 14305, 15110, 15961, 16860, 17809,
    18812, 19871,
)
