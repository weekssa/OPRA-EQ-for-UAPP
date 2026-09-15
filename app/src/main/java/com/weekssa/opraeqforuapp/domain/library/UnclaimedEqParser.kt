package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.pow

data class UnclaimedEqParseResult(
    val format: UnclaimedEqFormat,
    val state: UnclaimedEqParseState,
    val message: String? = null,
    val content: UnclaimedEqParsedContent? = null,
)

/**
 * Conservative recovery parser for EQ Library-owned legacy artifacts.
 *
 * Recovery never guesses missing filter semantics. Equalizer APO / AutoEq parametric text uses the
 * same strict parser as Personal EQ import. UAPP/ToneBoosters XML is decoded only for the exact
 * deterministic ten-band structure written by EQ Library.
 */
object UnclaimedEqParser {
    private const val UAPP_VALUE_COUNT = 66
    private const val UAPP_BAND_COUNT = 10
    private const val VALUES_PER_BAND = 6

    private const val F_MIN = 16.0
    private const val F_MAX = 20_000.0
    private const val GAIN_MIN = -20.0
    private const val GAIN_MAX = 20.0
    private const val Q_MIN = 0.1
    private const val Q_MAX = 10.0

    private const val LOW_SHELF_TYPE = 0.071428575
    private const val PEAK_TYPE = 0.21428572
    private const val HIGH_SHELF_TYPE = 0.2857143

    fun parse(fileName: String, bytes: ByteArray): UnclaimedEqParseResult {
        if (bytes.isEmpty()) {
            return invalid(UnclaimedEqFormat.UNKNOWN, "The managed file is empty.")
        }
        val prefix = bytes.copyOfRange(0, minOf(bytes.size, 128))
            .toString(StandardCharsets.ISO_8859_1)
            .trimStart()
        val looksXml = fileName.endsWith(".xml", ignoreCase = true) || prefix.startsWith("<?xml")
        return if (looksXml) parseToneBoostersXml(bytes) else parseParametricText(bytes)
    }

    private fun parseParametricText(bytes: ByteArray): UnclaimedEqParseResult {
        val text = bytes.toString(StandardCharsets.UTF_8)
        if (text.contains("GraphicEQ:", ignoreCase = true)) {
            return UnclaimedEqParseResult(
                format = UnclaimedEqFormat.UNKNOWN,
                state = UnclaimedEqParseState.UNSUPPORTED,
                message = "GraphicEQ does not contain the parametric Q/filter data required for lossless recovery.",
            )
        }
        val parsed = ParametricEqTextParser.parseStrictPersonal(text)
        if (!parsed.isValid) {
            return invalid(
                UnclaimedEqFormat.PARAMETRIC_TEXT,
                parsed.errors.joinToString(" ").ifBlank { "The parametric EQ is incomplete." },
            )
        }
        val bands = parsed.parsedEq.filters.map { filter ->
            OpraBand(
                type = when (filter.type) {
                    EqFilterType.PEAK -> "peak_dip"
                    EqFilterType.LOW_SHELF -> "low_shelf"
                    EqFilterType.HIGH_SHELF -> "high_shelf"
                    else -> error("Strict Personal EQ parsing admitted an unsupported filter type.")
                },
                frequency = filter.frequencyHz,
                gainDb = requireNotNull(filter.gainDb),
                q = requireNotNull(filter.q),
                slope = filter.slope,
            )
        }
        return UnclaimedEqParseResult(
            format = UnclaimedEqFormat.PARAMETRIC_TEXT,
            state = UnclaimedEqParseState.RECOVERABLE,
            content = UnclaimedEqParsedContent(
                suggestedName = null,
                preampGainDb = parsed.parsedEq.preampGainDb,
                bands = bands,
            ),
        )
    }

    private fun parseToneBoostersXml(bytes: ByteArray): UnclaimedEqParseResult {
        val xml = bytes.toString(StandardCharsets.ISO_8859_1)
        if (!xml.contains("<PresetInfo") || !xml.contains("TenBand=\"1\"")) {
            return UnclaimedEqParseResult(
                format = UnclaimedEqFormat.TONEBOOSTERS_XML,
                state = UnclaimedEqParseState.UNSUPPORTED,
                message = "This XML is not the ten-band UAPP/ToneBoosters structure written by EQ Library.",
            )
        }
        val values = VALUE_REGEX.findAll(xml)
            .map { it.groupValues[1].trim().toDoubleOrNull() }
            .toList()
        if (values.size < UAPP_VALUE_COUNT || values.any { it == null || !it.isFinite() }) {
            return invalid(
                UnclaimedEqFormat.TONEBOOSTERS_XML,
                "The ToneBoosters value list is incomplete or malformed.",
            )
        }
        val finiteValues = values.map { requireNotNull(it) }
        val bands = mutableListOf<OpraBand>()
        repeat(UAPP_BAND_COUNT) { index ->
            val offset = index * VALUES_PER_BAND
            val enabled = finiteValues[offset + 2]
            if (enabled < 0.5) return@repeat
            val frequencyNorm = finiteValues[offset]
            val gainNorm = finiteValues[offset + 1]
            val qNorm = finiteValues[offset + 3]
            val typeNorm = finiteValues[offset + 4]
            if (frequencyNorm !in 0.0..1.0 || gainNorm !in 0.0..1.0 || qNorm !in 0.0..1.0) {
                return invalid(
                    UnclaimedEqFormat.TONEBOOSTERS_XML,
                    "Band ${index + 1} contains values outside the supported ToneBoosters range.",
                )
            }
            val type = when {
                typeNorm.near(LOW_SHELF_TYPE) -> "low_shelf"
                typeNorm.near(PEAK_TYPE) -> "peak_dip"
                typeNorm.near(HIGH_SHELF_TYPE) -> "high_shelf"
                else -> return UnclaimedEqParseResult(
                    format = UnclaimedEqFormat.TONEBOOSTERS_XML,
                    state = UnclaimedEqParseState.UNSUPPORTED,
                    message = "Band ${index + 1} uses a ToneBoosters filter type EQ Library cannot recover safely.",
                )
            }
            bands += OpraBand(
                type = type,
                frequency = F_MIN + frequencyNorm.pow(3.0) * (F_MAX - F_MIN),
                gainDb = GAIN_MIN + gainNorm * (GAIN_MAX - GAIN_MIN),
                q = Q_MIN + qNorm.pow(3.0) * (Q_MAX - Q_MIN),
                slope = null,
            )
        }
        if (bands.isEmpty()) {
            return invalid(UnclaimedEqFormat.TONEBOOSTERS_XML, "The managed preset contains no enabled EQ bands.")
        }
        val gainNorm = finiteValues[61]
        if (gainNorm !in 0.0..1.0) {
            return invalid(UnclaimedEqFormat.TONEBOOSTERS_XML, "The ToneBoosters preamp value is outside the supported range.")
        }
        val name = NAME_REGEX.find(xml)?.groupValues?.get(1)?.let(::decodeXmlAttribute)?.trim()?.takeIf(String::isNotEmpty)
        return UnclaimedEqParseResult(
            format = UnclaimedEqFormat.TONEBOOSTERS_XML,
            state = UnclaimedEqParseState.RECOVERABLE,
            content = UnclaimedEqParsedContent(
                suggestedName = name,
                preampGainDb = GAIN_MIN + gainNorm * (GAIN_MAX - GAIN_MIN),
                bands = bands,
            ),
        )
    }

    private fun invalid(format: UnclaimedEqFormat, message: String) = UnclaimedEqParseResult(
        format = format,
        state = UnclaimedEqParseState.INVALID,
        message = message,
    )

    private fun Double.near(expected: Double): Boolean = abs(this - expected) <= 0.00001

    private fun decodeXmlAttribute(value: String): String = value
        .replace("&quot;", "\"")
        .replace("&gt;", ">")
        .replace("&lt;", "<")
        .replace("&amp;", "&")

    private val VALUE_REGEX = Regex("<Value>([^<]+)</Value>", RegexOption.IGNORE_CASE)
    private val NAME_REGEX = Regex("\\bName=\"([^\"]*)\"", RegexOption.IGNORE_CASE)
}
