package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.DacControlId

/**
 * Separates software implementation, the next physical candidate, and production-qualified writes.
 *
 * All normal Black Pearl controls below have software transaction support. A control becomes normal
 * write-interactive only after its maintained physical checklist passes. Exactly one later control is
 * admitted as the next qualification candidate so hardware exposure advances deliberately.
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

    /** Next physical write gate after the qualified DAC reconstruction filter. */
    val candidateWriteControlIds: Set<DacControlId> = setOf(
        BlackPearlDeviceControls.BALANCE_DB,
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
