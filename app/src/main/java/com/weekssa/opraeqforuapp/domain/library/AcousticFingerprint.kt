package com.weekssa.opraeqforuapp.domain.library

import java.security.MessageDigest
import java.util.Locale

object AcousticFingerprint {
    fun of(preampDb: Double?, filters: List<EqFilter>): String {
        val normalized = buildString {
            append("preamp=")
            append(format(preampDb ?: 0.0, 3))
            append(';')
            filters
                .map(::normalize)
                .sorted()
                .forEach {
                    append(it)
                    append(';')
                }
        }
        return sha256(normalized)
    }

    /** Identity of the source's filter-priority sequence, separate from acoustic equivalence. */
    fun sourcePriority(filters: List<EqFilter>): String = sha256(
        filters.joinToString(separator = ";") { normalize(it) },
    )

    private fun normalize(filter: EqFilter): String = listOf(
        normalizeType(filter),
        format(filter.frequencyHz, 3),
        format(filter.gainDb ?: 0.0, 3),
        format(filter.q ?: 0.0, 4),
        format(filter.slope ?: 0.0, 4),
    ).joinToString("|")

    private fun normalizeType(filter: EqFilter): String = when (filter.type) {
        EqFilterType.PEAK -> "PK"
        EqFilterType.LOW_SHELF -> "LS"
        EqFilterType.HIGH_SHELF -> "HS"
        EqFilterType.LOW_PASS -> "LP"
        EqFilterType.HIGH_PASS -> "HP"
        EqFilterType.OTHER -> filter.sourceType
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { "OTHER:${it.lowercase(Locale.ROOT)}" }
            ?: "OTHER"
    }

    private fun format(value: Double, decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", value)

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
