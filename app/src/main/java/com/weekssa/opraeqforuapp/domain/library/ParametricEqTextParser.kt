package com.weekssa.opraeqforuapp.domain.library

import java.util.Locale

/**
 * Parses the de-facto Equalizer APO / AutoEq parametric text format used by
 * AutoEq and many community presets. Source ingestion is fail-closed: an active malformed or
 * unsupported filter invalidates the whole candidate rather than silently producing a partial EQ.
 */
object ParametricEqTextParser {
    data class ParsedEq(
        val preampGainDb: Double?,
        val filters: List<EqFilter>,
    )

    private val preampRegex = Regex(
        pattern = """^\s*Preamp\s*:\s*([+-]?\d+(?:\.\d+)?)\s*dB\s*$""",
        option = RegexOption.IGNORE_CASE,
    )

    private val filterPrefixRegex = Regex(
        pattern = """^\s*Filter\s+\d+\s*:\s*(ON|OFF)\s+([A-Za-z]+)\s+(.+)$""",
        option = RegexOption.IGNORE_CASE,
    )

    private val parameterRegex = Regex(
        pattern = """\b(Fc|Gain|Q)\s+([+-]?\d+(?:\.\d+)?)(?:\s*(Hz|dB))?\b""",
        option = RegexOption.IGNORE_CASE,
    )

    fun parse(text: String): ParsedEq {
        val result = parseStrictSource(text)
        require(result.isValid) { result.errors.joinToString(" ") }
        return result.parsedEq
    }

    data class StrictParseResult(
        val parsedEq: ParsedEq,
        val errors: List<String>,
    ) {
        val isValid: Boolean get() = errors.isEmpty() && parsedEq.filters.isNotEmpty()
    }

    /** Personal import only admits filter families that its save/export path preserves. */
    fun parseStrictPersonal(text: String): StrictParseResult =
        parseStrict(text, STRICT_PERSONAL_TYPES)

    /** Source ingestion preserves every canonical filter family and rejects partial candidates. */
    fun parseStrictSource(text: String): StrictParseResult =
        parseStrict(text, STRICT_SOURCE_TYPES)

    private fun parseStrict(
        text: String,
        allowedTypes: Set<EqFilterType>,
    ): StrictParseResult {
        var preamp: Double? = null
        var preampSeen = false
        val filters = mutableListOf<EqFilter>()
        val errors = mutableListOf<String>()

        text.lineSequence().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed

            if (line.startsWith("Preamp", ignoreCase = true)) {
                val match = preampRegex.matchEntire(line)
                if (match == null) {
                    errors += "Line $lineNumber: malformed Preamp line."
                } else if (preampSeen) {
                    errors += "Line $lineNumber: duplicate Preamp line."
                } else {
                    preamp = match.groupValues[1].toDoubleOrNull()
                    preampSeen = true
                    if (preamp == null || !preamp!!.isFinite()) {
                        errors += "Line $lineNumber: invalid Preamp value."
                    }
                }
                return@forEachIndexed
            }

            if (!line.startsWith("Filter", ignoreCase = true)) {
                errors += "Line $lineNumber: unsupported EQ directive."
                return@forEachIndexed
            }
            val filterMatch = filterPrefixRegex.matchEntire(line)
            if (filterMatch == null) {
                errors += "Line $lineNumber: malformed Filter line."
                return@forEachIndexed
            }
            if (filterMatch.groupValues[1].equals("OFF", ignoreCase = true)) {
                return@forEachIndexed
            }

            val rawType = filterMatch.groupValues[2]
            val type = parseType(rawType)
            if (type !in allowedTypes) {
                errors += "Line $lineNumber: unsupported active filter type $rawType."
                return@forEachIndexed
            }

            val body = filterMatch.groupValues[3]
            val parameters = parameterRegex.findAll(body).toList()
            val fields = parameters.map { it.groupValues[1].lowercase(Locale.ROOT) }
            val onlyWhitespaceOutsideValues = parameters.withIndex().all { (index, match) ->
                val gapStart = if (index == 0) 0 else parameters[index - 1].range.last + 1
                body.substring(gapStart, match.range.first).isBlank()
            } && body.substring((parameters.lastOrNull()?.range?.last ?: -1) + 1).isBlank()
            if (
                parameters.size != 3 ||
                fields.toSet() != REQUIRED_FIELDS ||
                fields.size != fields.toSet().size ||
                !onlyWhitespaceOutsideValues ||
                parameters.any { match ->
                    when (match.groupValues[1].lowercase(Locale.ROOT)) {
                        "fc" -> !match.groupValues[3].equals("Hz", ignoreCase = true)
                        "gain" -> !match.groupValues[3].equals("dB", ignoreCase = true)
                        "q" -> match.groupValues[3].isNotEmpty()
                        else -> true
                    }
                }
            ) {
                errors += "Line $lineNumber: each active filter must contain exactly one Fc, Gain, and Q value."
                return@forEachIndexed
            }
            val valueByField = parameters.associate { match ->
                match.groupValues[1].lowercase(Locale.ROOT) to match.groupValues[2].toDoubleOrNull()
            }
            val frequency = valueByField["fc"]
            if (frequency == null || !frequency.isFinite() || frequency <= 0.0) {
                errors += "Line $lineNumber: filter frequency must be a positive number."
                return@forEachIndexed
            }
            val gain = valueByField["gain"]
            if (gain == null || !gain.isFinite()) {
                errors += "Line $lineNumber: filter Gain is required."
                return@forEachIndexed
            }
            val q = valueByField["q"]
            if (q == null || !q.isFinite() || q <= 0.0) {
                errors += "Line $lineNumber: filter Q must be a positive number."
                return@forEachIndexed
            }

            filters += EqFilter(
                type = type,
                frequencyHz = frequency,
                gainDb = gain,
                q = q,
                sourceType = rawType.takeIf { type == EqFilterType.OTHER },
            )
        }

        if (filters.isEmpty() && errors.isEmpty()) {
            errors += "This EQ format isn't supported yet. Use Equalizer APO / AutoEq parametric text."
        }
        return StrictParseResult(
            parsedEq = ParsedEq(preampGainDb = preamp, filters = filters),
            errors = errors,
        )
    }

    private fun parseType(raw: String): EqFilterType = when (raw.uppercase(Locale.ROOT)) {
        "PK", "PEQ", "PEAK", "PEAKING" -> EqFilterType.PEAK
        "LS", "LSC", "LOWSHELF" -> EqFilterType.LOW_SHELF
        "HS", "HSC", "HIGHSHELF" -> EqFilterType.HIGH_SHELF
        "LP", "LPF", "LOWPASS" -> EqFilterType.LOW_PASS
        "HP", "HPF", "HIGHPASS" -> EqFilterType.HIGH_PASS
        else -> EqFilterType.OTHER
    }

    private val STRICT_PERSONAL_TYPES = setOf(
        EqFilterType.PEAK,
        EqFilterType.LOW_SHELF,
        EqFilterType.HIGH_SHELF,
    )

    private val STRICT_SOURCE_TYPES = STRICT_PERSONAL_TYPES + setOf(
        EqFilterType.LOW_PASS,
        EqFilterType.HIGH_PASS,
        EqFilterType.OTHER,
    )

    private val REQUIRED_FIELDS = setOf("fc", "gain", "q")
}
