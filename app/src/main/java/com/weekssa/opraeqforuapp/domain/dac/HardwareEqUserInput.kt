package com.weekssa.opraeqforuapp.domain.dac

import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlin.math.round

data class HardwareEqUserInputValues(
    val frequencyHz: Double,
    val gainDb: Double,
    val q: Double,
)

/**
 * Converts ordinary decimal editor input into the exact native gain/Q representation before the
 * strict hardware editor validator sees it.
 *
 * Black Pearl gain and Q are encoded in 1/256 units. Requiring a user to type binary-fraction
 * decimals such as 1.69921875 is not useful, and the Compose editor intentionally presents shorter
 * decimal values. Values that are already inside the verified range are therefore snapped to the
 * nearest native gain/Q step. Out-of-range values are never clamped, and frequency remains strict
 * because its native 1 Hz step is directly understandable in the editor.
 */
internal fun normalizeHardwareEqUserInput(
    spec: HardwareEqEditSpec,
    frequencyHz: Double,
    gainDb: Double,
    q: Double,
): HardwareEqUserInputValues = HardwareEqUserInputValues(
    frequencyHz = frequencyHz,
    gainDb = gainDb.snapToNativeStepWhenInRange(spec.gainRangeDb, spec.gainStepDb),
    q = q.snapToNativeStepWhenInRange(spec.qRange, spec.qStep),
)

internal fun updateHardwareEqFilterFromUserInput(
    workingCopy: HardwareEqEditWorkingCopy,
    spec: HardwareEqEditSpec,
    bandIndex: Int,
    type: EqFilterType,
    frequencyHz: Double,
    gainDb: Double,
    q: Double,
): HardwareEqEditWorkingCopy {
    val normalized = normalizeHardwareEqUserInput(spec, frequencyHz, gainDb, q)
    return HardwareEqEditor.updateFilter(
        workingCopy = workingCopy,
        spec = spec,
        bandIndex = bandIndex,
        type = type,
        frequencyHz = normalized.frequencyHz,
        gainDb = normalized.gainDb,
        q = normalized.q,
    )
}

private fun Double.snapToNativeStepWhenInRange(
    range: DacNumericRange,
    step: Double,
): Double {
    if (this !in range) return this
    return round(this / step) * step
}
