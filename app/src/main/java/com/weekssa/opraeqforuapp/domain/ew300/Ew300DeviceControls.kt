package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.dac.DacControlDescriptor
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlSafetyClass
import com.weekssa.opraeqforuapp.domain.dac.DacControlSection
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacNumericRange
import com.weekssa.opraeqforuapp.domain.dac.DacNumericUnit

/**
 * Software contract for the one EW300 device setting that has an exact register mapping.
 *
 * The UI deliberately exposes a conservative non-boosting envelope. The wire register accepts a
 * wider signed range, but that fact is not a safe listening recommendation and is not exposed as
 * a user control.
 */
object Ew300DeviceControls {
    val PLAYBACK_GAIN_DB = DacControlId("simgot_ew300.playback_gain_db")

    const val MIN_PLAYBACK_GAIN_DB = -64.0
    const val MAX_PLAYBACK_GAIN_DB = 0.0
    const val PLAYBACK_GAIN_STEP_DB = 0.5

    val playbackGainDescriptor = DacControlDescriptor.Numeric(
        id = PLAYBACK_GAIN_DB,
        section = DacControlSection.PLAYBACK,
        safetyClass = DacControlSafetyClass.LEVEL_SENSITIVE,
        unit = DacNumericUnit.DB,
        absoluteRange = DacNumericRange(MIN_PLAYBACK_GAIN_DB, MAX_PLAYBACK_GAIN_DB),
        step = PLAYBACK_GAIN_STEP_DB,
    )

    val descriptors: List<DacControlDescriptor> = listOf(playbackGainDescriptor)

    fun value(gainDb: Double): DacControlValue = DacControlValue.Numeric(gainDb)
}
