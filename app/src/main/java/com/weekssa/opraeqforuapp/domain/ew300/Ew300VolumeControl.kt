package com.weekssa.opraeqforuapp.domain.ew300

import kotlin.math.roundToInt

/**
 * JA11-style 0-60 presentation mapped onto the exact EW300 0x66 output-gain register.
 * This is intentionally not JA11 command 0x02: 60 = 0 dB and 0 = -60 dB, one dB per step.
 */
object Ew300VolumeControl {
    const val MIN_LEVEL = 0
    const val MAX_LEVEL = 60
    const val MIN_GAIN_DB = -60.0
    const val MAX_GAIN_DB = 0.0

    fun requireLevel(level: Int): Int {
        require(level in MIN_LEVEL..MAX_LEVEL) { "EW300 volume level must be 0..60." }
        return level
    }

    fun levelToGainDb(level: Int): Double = requireLevel(level) - 60.0

    fun gainDbToLevel(gainDb: Double): Int {
        require(gainDb.isFinite()) { "EW300 gain must be finite." }
        return (gainDb + 60.0).roundToInt().coerceIn(MIN_LEVEL, MAX_LEVEL)
    }

    fun levelToSteps(level: Int): Int = (requireLevel(level) - 60) * 2

    fun isRepresentableLevel(level: Int): Boolean = level in MIN_LEVEL..MAX_LEVEL
}

