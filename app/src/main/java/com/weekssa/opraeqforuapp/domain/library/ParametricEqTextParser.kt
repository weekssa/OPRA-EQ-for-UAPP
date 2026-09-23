package com.weekssa.opraeqforuapp.domain.library

import java.util.Locale

/**
 * Parses the de-facto Equalizer APO / AutoEq parametric text format used by
 * AutoEq and many community presets. Unknown prose is ignored, but malformed
 * preamp or active filter lines fail the whole parse closed so a source EQ can
 * never silently become a partial EQ.
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

    private val frequencyRegex = Regex(
        pattern = """\bFc\s+([+-]?\d+(?:\.\d+)?)\s*Hz\b""",
        option = RegexOption.IGNORE_CASE,
    )

    private val gainRegex = Regex(
        pattern = """\bGain\s+([+-]?\d+(?:\.\d+)?)\s*dB\b""",
        option = RegexOption.IGNORE_CASE,
    )

    private val qRegex = Regex(
        pattern = """\bQ\s+([+-]?\d+(?:\.\d+)?)\b""",
        option = RegexOption.IGNORE_CASE,
    )

    fun parse(text: String): ParsedEq {
        var preamp: Double? = null
        val filters = mutableListOf<EqFilter>()
        var invalidEqLine = false

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            if (line.startsWith("Preamp", ignoreCase = true)) {
                val match = preampRegex.matchEntire(line)
                val parsedPreamp = match?.groupValues?.get(1)?.toDoubleOrNull()
                if (parsedPreamp == null || !parsedPreamp.isFinite()) {
                    invalidEqLine = true
                } else {
                    preamp = parsedPreamp
                }
                return@forEach
            }

            if (!line.startsWith("Filter", ignoreCase = true)) return@forEach
            val filterMatch = filterPrefixRegex.matchEntire(line)
            if (filterMatch == null) {
                invalidEqLine = true
                return@forEach
            }
            if (!filterMatch.groupValues[1].equals("ON", ignoreCase = true)) return@forEach

            val type = parseType(filterMatch.groupValues[2])
            if (type == EqFilterType.OTHER) {
                invalidEqLine = true
                return@forEach
            }

            val body = filterMatch.groupValues[3]
            val frequency = frequencyRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
            if (frequency == null || !frequency.isFinite() || frequency <= 0.0) {
                invalidEqLine = true
                return@forEach
            }

            val gain = gainRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
            val q = qRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
            if (gain != null && !gain.isFinite()) {
                invalidEqLine = true
                return@forEach
            }
            if (q != null && (!q.isFinite() || q <= 0.0)) {
                invalidEqLine = true
                return@forEach
            }

            when (type) {
                EqFilterType.PEAK,
                EqFilterType.LOW_SHELF,
                EqFilterType.HIGH_SHELF,
                -> if (gain == null || q == null) {
                    invalidEqLine = true
                    return@forEach
                }

                EqFilterType.LOW_PASS,
                EqFilterType.HIGH_PASS,
                -> if (q == null) {
                    invalidEqLine = true
                    return@forEach
                }

                EqFilterType.OTHER -> error("handled above")
            }

            filters += EqFilter(
                type = type,
                frequencyHz = frequency,
                gainDb = gain,
                q = q,
            )
        }

        return ParsedEq(
            preampGainDb = preamp.takeUnless { invalidEqLine },
            filters = if (invalidEqLine) emptyList() else filters,
        )
    }

    data class StrictParseResult(
        val parsedEq: ParsedEq,
        val errors: List<String>,
    ) {
        val isValid: Boolean get() = errors.isEmpty() && parsedEq.filters.isNotEmpty()
    }

    /**
     * User-facing personal import is intentionally stricter than source discovery. Unknown prose
     * may be ignored, but malformed Filter/Preamp lines and unsupported enabled filter types are
     * blocking so an intended 10-band EQ can never silently become a 9-band import.
     */
    fun parseStrictPersonal(text: String): StrictParseResult {
        var preamp: Double? = null
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
                } else {
                    val parsedPreamp = match.groupValues[1].toDoubleOrNull()
                    if (parsedPreamp == null || !parsedPreamp.isFinite()) {
                        errors += "Line $lineNumber: invalid Preamp value."
                    } else {
                        preamp = parsedPreamp
                    }
                }
                return@forEachIndexed
            }

            if (!line.startsWith("Filter", ignoreCase = true)) return@forEachIndexed
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
            if (type !in STRICT_PERSONAL_TYPES) {
                errors += "Line $lineNumber: unsupported active filter type $rawType."
                return@forEachIndexed
            }

            val body = filterMatch.groupValues[3]
            val frequency = frequencyRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
            if (frequency == null || !frequency.isFinite() || frequency <= 0.0) {
                errors += "Line $lineNumber: filter frequency must be a positive number."
                return@forEachIndexed
            }
            val gain = gainRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
            if (gain == null || !gain.isFinite()) {
                errors += "Line $lineNumber: filter Gain is required."
                return@forEachIndexed
            }
            val q = qRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
            if (q == null || !q.isFinite() || q <= 0.0) {
                errors += "Line $lineNumber: filter Q must be a positive number."
                return@forEachIndexed
            }

            filters += EqFilter(
                type = type,
                frequencyHz = frequency,
                gainDb = gain,
                q = q,
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
}
