package com.weekssa.opraeqforuapp.domain.catalog

import java.util.Locale

/** Shared source-token normalization for the OPRA adapter and its UAPP export gate. */
object OpraFilterTypeNormalizer {
    fun normalize(value: String?): String? = value
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf(String::isNotEmpty)
        ?.let { raw ->
            when (raw) {
                "peak", "pk", "peq", "peaking" -> "peak_dip"
                "ls", "lsc", "lowshelf" -> "low_shelf"
                "hs", "hsc", "highshelf" -> "high_shelf"
                "lp", "lpf", "lowpass" -> "low_pass"
                "hp", "hpf", "highpass" -> "high_pass"
                else -> raw
            }
        }

    fun requiresQ(normalizedType: String): Boolean = normalizedType in Q_REQUIRED_TYPES

    fun requiresSlope(normalizedType: String): Boolean = normalizedType in SLOPE_REQUIRED_TYPES

    private val Q_REQUIRED_TYPES = setOf(
        "peak_dip", "low_shelf", "high_shelf", "band_pass", "band_stop",
    )
    private val SLOPE_REQUIRED_TYPES = setOf("low_pass", "high_pass")
}
