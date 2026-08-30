package com.weekssa.opraeqforuapp.domain.conversion

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

class ToneBoostersConversionException(message: String) : IllegalArgumentException(message)

data class ToneBoostersConversionResult(
    val presetName: String,
    val xml: String,
    val warnings: List<String>,
    val sourceBandCount: Int,
    val convertedBandCount: Int,
)

object ToneBoostersConverter {
    const val MISSING_CREATOR_LABEL = "Creator information missing"

    private const val F_MIN = 16.0
    private const val F_MAX = 20_000.0
    private const val GAIN_MIN = -20.0
    private const val GAIN_MAX = 20.0
    private const val Q_MIN = 0.1
    private const val Q_MAX = 10.0
    private const val MAX_BANDS = 10

    private val filterTypes = mapOf(
        "low_shelf" to 0.071428575,
        "peak_dip" to 0.21428572,
        "high_shelf" to 0.2857143,
    )

    private val disabledFilter = NormalizedFilter(
        frequency = 0.9282573,
        gain = 0.5,
        enabled = 0,
        q = 0.39434525,
        type = filterTypes.getValue("peak_dip"),
    )

    fun convert(profile: OpraEqProfile, presetName: String): ToneBoostersConversionResult {
        val compatibility = profile.assessCompatibility()
        if (compatibility.category == ProfileCompatibility.NotCompatible) {
            throw ToneBoostersConversionException(
                compatibility.reason ?: "This profile is not compatible with the established conversion.",
            )
        }
        val gainDb = profile.effectivePlaybackPreampDb()
            ?: throw ToneBoostersConversionException(
                "This profile has no source preamp and no EQ Library-generated safety headroom.",
            )
        val bands = profile.bands
            ?: throw ToneBoostersConversionException("This profile is missing its parametric EQ band list.")
        val converted = buildXml(presetName = presetName, gainDb = gainDb, bands = bands)
        return if (profile.usesEqLibrarySafetyHeadroom()) {
            converted.copy(
                warnings = listOf(
                    "Source omitted preamp; this export uses EQ Library-generated safety headroom of ${gainDb.toDisplayNumber()} dB.",
                ) + converted.warnings,
            )
        } else {
            converted
        }
    }

    fun buildXml(
        presetName: String,
        gainDb: Double,
        bands: List<OpraBand>,
    ): ToneBoostersConversionResult {
        val warnings = if (bands.size > MAX_BANDS) {
            listOf(
                "Source has ${bands.size} bands; the current UAPP/ToneBoosters target supports 10, so only the first 10 priority-sorted bands were used.",
            )
        } else {
            emptyList()
        }
        val convertedBands = bands.take(MAX_BANDS)
        val filters = convertedBands.map(::normalizeFilter).toMutableList()
        while (filters.size < MAX_BANDS) filters += disabledFilter

        val safePresetName = uappSafeName(presetName)
        val values = buildList {
            filters.forEach { filter ->
                add(format(filter.frequency))
                add(format(filter.gain))
                add(filter.enabled.toString())
                add(format(filter.q))
                add(format(filter.type))
                add("0")
            }
            add("0")
            add(format(normalizeGain(gainDb)))
            add("1")
            add(format(0.33333334))
            add(format(0.05))
            add("0")
        }

        val xml = buildString {
            append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n")
            append("<Preset><PresetInfo Name=\"")
            append(escapeXmlAttribute(safePresetName))
            append("\" TenBand=\"1\">")
            values.forEach { value ->
                append("<Value>")
                append(value)
                append("</Value>")
            }
            append("</PresetInfo></Preset>\n")
        }

        return ToneBoostersConversionResult(
            presetName = safePresetName,
            xml = xml,
            warnings = warnings,
            sourceBandCount = bands.size,
            convertedBandCount = convertedBands.size,
        )
    }

    fun normalizeFrequency(frequency: Double): Double {
        if (frequency !in F_MIN..F_MAX) {
            throw ToneBoostersConversionException(
                "frequency ${frequency.toDisplayNumber()} Hz is outside UAPP/ToneBoosters range 16-20000 Hz",
            )
        }
        return ((frequency - F_MIN) / (F_MAX - F_MIN)).pow(1.0 / 3.0)
    }

    fun normalizeGain(gainDb: Double): Double {
        if (gainDb !in GAIN_MIN..GAIN_MAX) {
            throw ToneBoostersConversionException(
                "gain ${gainDb.toDisplayNumber()} dB is outside UAPP/ToneBoosters range -20 to +20 dB",
            )
        }
        return (gainDb - GAIN_MIN) / (GAIN_MAX - GAIN_MIN)
    }

    fun normalizeQ(q: Double): Double {
        if (q !in Q_MIN..Q_MAX) {
            throw ToneBoostersConversionException(
                "Q ${q.toDisplayNumber()} is outside UAPP/ToneBoosters range 0.1-10",
            )
        }
        return ((q - Q_MIN) / (Q_MAX - Q_MIN)).pow(1.0 / 3.0)
    }

    fun uappSafeName(value: String): String {
        val source = value.replace('•', '-')
        val latin1 = buildString {
            var index = 0
            while (index < source.length) {
                val codePoint = Character.codePointAt(source, index)
                append(if (codePoint <= 0xff) codePoint.toChar() else '?')
                index += Character.charCount(codePoint)
            }
        }
        val sanitized = latin1
            .replace(INVALID_FILENAME_CHARS, "-")
            .replace(WHITESPACE, " ")
            .trim()
            .trim('.')
            .trim()
        return sanitized.ifEmpty { "Preset" }
    }

    fun buildPresetName(
        modelLabel: String,
        creator: String?,
        details: String?,
        verifiedVariantLabel: String? = null,
    ): String {
        val creatorValue = creator?.trim().orEmpty().ifEmpty { MISSING_CREATOR_LABEL }
        val compactDetails = compactDetails(details, verifiedVariantLabel)
        return uappSafeName(
            buildList {
                add(modelLabel.trim())
                add(creatorValue)
                if (compactDetails.isNotEmpty()) add(compactDetails)
            }.joinToString(" - "),
        )
    }

    private fun compactDetails(details: String?, verifiedVariantLabel: String?): String {
        var value = details.orEmpty().trim()
        if (value.isEmpty()) return ""
        value = value.replaceFirst(Regex("^Measured\\s+by\\s+", RegexOption.IGNORE_CASE), "").trim()
        val variant = verifiedVariantLabel?.trim().orEmpty()
        if (variant.isNotEmpty()) {
            value = value.replaceFirst(
                Regex("\\s*\\(\\s*${Regex.escape(variant)}\\s*\\)\\s*$", RegexOption.IGNORE_CASE),
                "",
            ).trim()
        }
        return value
    }

    private fun normalizeFilter(band: OpraBand): NormalizedFilter {
        val typeName = band.type
            ?: throw ToneBoostersConversionException("filter is missing its OPRA type")
        val type = filterTypes[typeName]
            ?: throw ToneBoostersConversionException(
                "unsupported OPRA filter type for UAPP/ToneBoosters: $typeName",
            )
        val frequency = band.frequency
            ?: throw ToneBoostersConversionException("filter type $typeName is missing frequency")
        val q = band.q
            ?: throw ToneBoostersConversionException("filter type $typeName is missing Q")
        return NormalizedFilter(
            frequency = normalizeFrequency(frequency),
            gain = normalizeGain(band.gainDb ?: 0.0),
            enabled = 1,
            q = normalizeQ(q),
            type = type,
        )
    }

    private fun format(value: Double): String =
        BigDecimal(value)
            .setScale(8, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .toPlainString()

    private fun escapeXmlAttribute(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun Double.toDisplayNumber(): String =
        BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

    private data class NormalizedFilter(
        val frequency: Double,
        val gain: Double,
        val enabled: Int,
        val q: Double,
        val type: Double,
    )

    private val INVALID_FILENAME_CHARS = Regex("[\\\\/:*?\"<>|]")
    private val WHITESPACE = Regex("\\s+")
}
