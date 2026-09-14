package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue

/**
 * Project-owner selected Black Pearl restore preset.
 *
 * This is intentionally not described as a TRN factory-default contract. Public evidence does not
 * establish the complete Black Pearl factory state. These values are the explicit EQ Library reset
 * targets approved by the project owner for v0.6.
 *
 * Microphone gain is explicitly restored to 0 dB. EQ reset is separately optional and continues to
 * use the independently qualified Reset EQ to flat transaction.
 */
data class BlackPearlDeviceDefaultStep(
    val controlId: DacControlId,
    val requestedValue: DacControlValue,
)

object BlackPearlDeviceDefaults {
    /** Minimum qualified whole-dB DEVICE level, used only as a fail-safe intermediate step. */
    const val SAFETY_VOLUME_DB = -37.0

    /** Whole-dB native value that presents as 50% in the Black Pearl percentage UI. */
    const val RESTORED_VOLUME_DB = -6.0

    /**
     * Safe restore ordering: lower listening level first, restore non-volume settings, then establish
     * the requested 50% presentation last. Every step still goes through the existing fresh-read,
     * one-write, persist, and verified-readback DEVICE transaction when a change is required.
     */
    val restoreSteps: List<BlackPearlDeviceDefaultStep> = listOf(
        BlackPearlDeviceDefaultStep(
            BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
            DacControlValue.Numeric(SAFETY_VOLUME_DB),
        ),
        BlackPearlDeviceDefaultStep(
            BlackPearlDeviceControls.DAC_FILTER,
            DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_FAST_LL),
        ),
        BlackPearlDeviceDefaultStep(
            BlackPearlDeviceControls.BALANCE_DB,
            DacControlValue.Numeric(0.0),
        ),
        BlackPearlDeviceDefaultStep(
            BlackPearlDeviceControls.MIC_GAIN_DB,
            DacControlValue.Numeric(0.0),
        ),
        BlackPearlDeviceDefaultStep(
            BlackPearlDeviceControls.AMP_TOPOLOGY,
            DacControlValue.Discrete(BlackPearlDeviceControls.AMP_CLASS_AB),
        ),
        BlackPearlDeviceDefaultStep(
            BlackPearlDeviceControls.GAIN_MODE,
            DacControlValue.Discrete(BlackPearlDeviceControls.GAIN_HIGH),
        ),
        BlackPearlDeviceDefaultStep(
            BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
            DacControlValue.Numeric(RESTORED_VOLUME_DB),
        ),
    )

    val finalTargets: Map<DacControlId, DacControlValue> = linkedMapOf(
        BlackPearlDeviceControls.PLAYBACK_GAIN_DB to DacControlValue.Numeric(RESTORED_VOLUME_DB),
        BlackPearlDeviceControls.DAC_FILTER to DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_FAST_LL),
        BlackPearlDeviceControls.GAIN_MODE to DacControlValue.Discrete(BlackPearlDeviceControls.GAIN_HIGH),
        BlackPearlDeviceControls.AMP_TOPOLOGY to DacControlValue.Discrete(BlackPearlDeviceControls.AMP_CLASS_AB),
        BlackPearlDeviceControls.BALANCE_DB to DacControlValue.Numeric(0.0),
        BlackPearlDeviceControls.MIC_GAIN_DB to DacControlValue.Numeric(0.0),
    )

    fun isStepSatisfied(
        step: BlackPearlDeviceDefaultStep,
        snapshot: BlackPearlDeviceQualificationSnapshot,
    ): Boolean = BlackPearlDeviceControls.valueFromSnapshot(step.controlId, snapshot) == step.requestedValue
}
