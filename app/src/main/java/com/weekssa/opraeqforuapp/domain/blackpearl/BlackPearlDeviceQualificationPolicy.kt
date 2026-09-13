package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.DacControlId

/**
 * Separates software implementation, the current physical candidates, and production-qualified writes.
 *
 * All normal Black Pearl controls below have software transaction support. DAC reconstruction filter
 * is already hardware-qualified. The project owner approved one consolidated hands-on qualification
 * round for the remaining normal controls, so those controls are admitted together as physical
 * candidates while retaining independent per-control pass/fail records.
 */
object BlackPearlDeviceQualificationPolicy {
    val softwareImplementedControlIds: Set<DacControlId> = setOf(
        BlackPearlDeviceControls.DAC_FILTER,
        BlackPearlDeviceControls.BALANCE_DB,
        BlackPearlDeviceControls.MIC_GAIN_DB,
        BlackPearlDeviceControls.AMP_TOPOLOGY,
        BlackPearlDeviceControls.GAIN_MODE,
        BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
    )

    /** Consolidated physical qualification batch after the qualified DAC reconstruction filter. */
    val candidateWriteControlIds: Set<DacControlId> = setOf(
        BlackPearlDeviceControls.BALANCE_DB,
        BlackPearlDeviceControls.MIC_GAIN_DB,
        BlackPearlDeviceControls.AMP_TOPOLOGY,
        BlackPearlDeviceControls.GAIN_MODE,
        BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
    )

    val productionQualifiedWriteControlIds: Set<DacControlId> = setOf(
        BlackPearlDeviceControls.DAC_FILTER,
    )

    fun isSoftwareImplemented(controlId: DacControlId): Boolean =
        controlId in softwareImplementedControlIds

    fun isCandidateWriteEnabled(controlId: DacControlId): Boolean =
        controlId in candidateWriteControlIds

    fun isProductionWriteQualified(controlId: DacControlId): Boolean =
        controlId in productionQualifiedWriteControlIds

    fun isWriteInteractive(controlId: DacControlId): Boolean =
        isProductionWriteQualified(controlId) || isCandidateWriteEnabled(controlId)
}
