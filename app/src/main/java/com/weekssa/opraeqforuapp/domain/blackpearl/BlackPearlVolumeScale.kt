package com.weekssa.opraeqforuapp.domain.blackpearl

import kotlin.math.roundToInt

/**
 * Presentation mapping corroborated against the independent Black Pearl controller.
 *
 * The device stores playback/global gain as raw protocol units while its ordinary user-facing
 * controller displays that same range as 0..100. This helper is presentation-only: EQ Library does
 * not treat controller percent as dB and DEVICE writes continue to use the exact native raw value.
 */
object BlackPearlVolumeScale {
    fun percentFromRaw(raw: Int): Int {
        require(raw in BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW..BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW)
        val fraction = (raw - BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW).toDouble() /
            (BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW - BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW).toDouble()
        return (fraction * 100.0).roundToInt().coerceIn(0, 100)
    }
}
