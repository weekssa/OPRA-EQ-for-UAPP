package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import java.util.Locale

/**
 * Stable exact acoustic identity for legacy compatibility records.
 *
 * Record IDs, author names, source links, and display metadata are deliberately excluded. Filter
 * order and equivalent legacy filter aliases are normalized so mirrors of the same preset resolve
 * to the same identity.
 */
internal fun OpraEqProfile.legacyAcousticSignature(): String? {
    val normalizedBands = bands.orEmpty().mapNotNull(OpraBand::legacyAcousticKey).sorted()
    if (normalizedBands.isEmpty()) return null
    return buildString {
        append("preamp=")
        append(legacyAcousticFormat(preampGainDb ?: 0.0, 3))
        normalizedBands.forEach {
            append(';')
            append(it)
        }
    }
}

private fun OpraBand.legacyAcousticKey(): String? {
    val frequencyValue = frequency ?: return null
    return listOf(
        normalizedLegacyFilterType(type),
        legacyAcousticFormat(frequencyValue, 3),
        legacyAcousticFormat(gainDb ?: 0.0, 3),
        legacyAcousticFormat(q ?: 0.0, 4),
        legacyAcousticFormat(slope ?: 0.0, 4),
    ).joinToString("|")
}

private fun normalizedLegacyFilterType(value: String?): String = when (value?.trim()?.lowercase(Locale.ROOT)) {
    "peak_dip", "peak", "pk", "peq" -> "PK"
    "low_shelf", "ls", "lsc" -> "LS"
    "high_shelf", "hs", "hsc" -> "HS"
    "low_pass", "lp" -> "LP"
    "high_pass", "hp" -> "HP"
    else -> value.orEmpty().trim().uppercase(Locale.ROOT)
}

private fun legacyAcousticFormat(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value)
