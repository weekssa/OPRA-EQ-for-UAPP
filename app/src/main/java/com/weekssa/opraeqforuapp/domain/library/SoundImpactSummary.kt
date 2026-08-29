package com.weekssa.opraeqforuapp.domain.library

import kotlin.math.abs

/**
 * Generates deliberately conservative, filter-action summaries.
 *
 * These summaries describe what the EQ filters do. They do not claim a measured
 * stock-vs-EQ acoustic change unless a future caller supplies verified stock-response data.
 */
object SoundImpactSummary {
    fun fromFilters(filters: List<EqFilter>): String? {
        val tonal = filters.mapNotNull(::classify).filter { abs(it.weight) >= 0.75 }
        if (tonal.isEmpty()) return null

        val grouped = tonal.groupBy { it.region }
            .mapValues { (_, values) -> values.sumOf { it.weight } }
            .filterValues { abs(it) >= 1.0 }

        if (grouped.isEmpty()) return null

        val ordered = Region.entries.mapNotNull { region ->
            grouped[region]?.let { region to it }
        }

        return ordered.take(2).joinToString(" and ") { (region, weight) ->
            phrase(region, weight)
        }.replaceFirstChar { it.uppercase() } + "."
    }

    private fun classify(filter: EqFilter): Contribution? {
        val gain = filter.gainDb ?: return null
        if (abs(gain) < 0.5) return null
        val region = when {
            filter.frequencyHz < 60.0 -> Region.SUB_BASS
            filter.frequencyHz < 250.0 -> Region.BASS
            filter.frequencyHz < 1_000.0 -> Region.LOWER_MIDS
            filter.frequencyHz < 3_000.0 -> Region.MIDS
            filter.frequencyHz < 6_000.0 -> Region.UPPER_MIDS
            else -> Region.TREBLE
        }
        val widthWeight = when (filter.type) {
            EqFilterType.LOW_SHELF, EqFilterType.HIGH_SHELF -> 1.4
            EqFilterType.PEAK -> when {
                filter.q == null -> 1.0
                filter.q < 0.7 -> 1.25
                filter.q > 4.0 -> 0.55
                else -> 1.0
            }
            else -> 0.65
        }
        return Contribution(region, gain * widthWeight)
    }

    private fun phrase(region: Region, weight: Double): String {
        val verb = if (weight > 0) "adds" else "reduces"
        val magnitude = abs(weight)
        return when {
            magnitude >= 6.0 -> "noticeably $verb ${region.label}"
            magnitude >= 3.0 -> "$verb ${region.label}"
            weight < 0 -> "slightly $verb ${region.label}"
            else -> "$verb slightly ${region.label}"
        }
    }

    private data class Contribution(val region: Region, val weight: Double)

    private enum class Region(val label: String) {
        SUB_BASS("sub-bass"),
        BASS("bass"),
        LOWER_MIDS("lower-mid energy"),
        MIDS("midrange energy"),
        UPPER_MIDS("upper-mid energy"),
        TREBLE("treble energy"),
    }
}
