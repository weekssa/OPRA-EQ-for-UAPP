package com.weekssa.opraeqforuapp.domain.library

import java.util.Locale

/**
 * Parses the de-facto Equalizer APO / AutoEq parametric text format used by
 * AutoEq and many community presets. Unknown lines are ignored; malformed
 * filter lines are rejected rather than partially interpreted.
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

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            preampRegex.matchEntire(line)?.let { match ->
                preamp = match.groupValues[1].toDoubleOrNull()
                return@forEach
            }

            val filterMatch = filterPrefixRegex.matchEntire(line) ?: return@forEach
            if (!filterMatch.groupValues[1].equals("ON", ignoreCase = true)) return@forEach

            val type = parseType(filterMatch.groupValues[2])
            val body = filterMatch.groupValues[3]
            val frequency = frequencyRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
                ?: return@forEach
            if (frequency <= 0.0) return@forEach

            val gain = gainRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
            val q = qRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()

            when (type) {
                EqFilterType.PEAK,
                EqFilterType.LOW_SHELF,
                EqFilterType.HIGH_SHELF -> if (gain == null) return@forEach

                else -> Unit
            }

            filters += EqFilter(
                type = type,
                frequencyHz = frequency,
                gainDb = gain,
                q = q,
            )
        }

        return ParsedEq(
            preampGainDb = preamp,
            filters = filters,
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
}
