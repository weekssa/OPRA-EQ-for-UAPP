package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.DacControlId

/**
 * Separates software implementation from physical production qualification.
 *
 * All normal Black Pearl controls below have software transaction support, but only the next control
 * under hands-on qualification is allowed to become write-interactive in a candidate build. Advancing
 * this set requires the prior control's maintained physical checklist to pass.
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

    /** First physical write gate. Do not broaden until its Black Pearl checklist passes. */
    val candidateWriteControlIds: Set<DacControlId> = setOf(
        BlackPearlDeviceControls.DAC_FILTER,
    )

    val productionQualifiedWriteControlIds: Set<DacControlId> = emptySet()

    fun isSoftwareImplemented(controlId: DacControlId): Boolean =
        controlId in softwareImplementedControlIds

    fun isCandidateWriteEnabled(controlId: DacControlId): Boolean =
        controlId in candidateWriteControlIds

    fun isProductionWriteQualified(controlId: DacControlId): Boolean =
        controlId in productionQualifiedWriteControlIds
}
