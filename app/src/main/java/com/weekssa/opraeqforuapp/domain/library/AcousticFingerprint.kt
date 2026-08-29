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

    private fun normalize(filter: EqFilter): String = listOf(
        normalizeType(filter.type),
        format(filter.frequencyHz, 3),
        format(filter.gainDb, 3),
        format(filter.q ?: 0.0, 4),
        format(filter.slope ?: 0.0, 4),
    ).joinToString("|")

    private fun normalizeType(value: String): String = when (value.trim().uppercase(Locale.ROOT)) {
        "PK", "PEQ", "PEAK", "PEAKING" -> "PK"
        "LS", "LSC", "LOW_SHELF", "LOWSHELF" -> "LS"
        "HS", "HSC", "HIGH_SHELF", "HIGHSHELF" -> "HS"
        else -> value.trim().uppercase(Locale.ROOT)
    }

    private fun format(value: Double, decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", value)

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
