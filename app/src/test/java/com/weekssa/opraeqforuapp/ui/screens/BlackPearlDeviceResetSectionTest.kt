package com.weekssa.opraeqforuapp.ui.screens

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControls
import org.junit.Test

class BlackPearlDeviceResetSectionTest {
    @Test
    fun stalePreWriteSnapshot_waitsInsteadOfFailing() {
        val result = blackPearlRestoreStepVerification(
            isBusy = false,
            lastVerifiedWriteControlId = null,
            expectedControlId = BlackPearlDeviceControls.DAC_FILTER,
            requestedValueSatisfied = false,
        )

        assertThat(result).isEqualTo(BlackPearlRestoreStepVerification.WAITING)
    }

    @Test
    fun activeWrite_waitsForVerifiedReadback() {
        val result = blackPearlRestoreStepVerification(
            isBusy = true,
            lastVerifiedWriteControlId = null,
            expectedControlId = BlackPearlDeviceControls.DAC_FILTER,
            requestedValueSatisfied = false,
        )

        assertThat(result).isEqualTo(BlackPearlRestoreStepVerification.WAITING)
    }

    @Test
    fun matchingVerifiedControl_advancesOnlyWhenRequestedValueIsPresent() {
        val satisfied = blackPearlRestoreStepVerification(
            isBusy = false,
            lastVerifiedWriteControlId = BlackPearlDeviceControls.DAC_FILTER,
            expectedControlId = BlackPearlDeviceControls.DAC_FILTER,
            requestedValueSatisfied = true,
        )
        val mismatch = blackPearlRestoreStepVerification(
            isBusy = false,
            lastVerifiedWriteControlId = BlackPearlDeviceControls.DAC_FILTER,
            expectedControlId = BlackPearlDeviceControls.DAC_FILTER,
            requestedValueSatisfied = false,
        )

        assertThat(satisfied).isEqualTo(BlackPearlRestoreStepVerification.SATISFIED)
        assertThat(mismatch).isEqualTo(BlackPearlRestoreStepVerification.MISMATCH)
    }

    @Test
    fun previousControlsVerification_cannotAdvanceNextStep() {
        val result = blackPearlRestoreStepVerification(
            isBusy = false,
            lastVerifiedWriteControlId = BlackPearlDeviceControls.GAIN_MODE,
            expectedControlId = BlackPearlDeviceControls.DAC_FILTER,
            requestedValueSatisfied = true,
        )

        assertThat(result).isEqualTo(BlackPearlRestoreStepVerification.WAITING)
    }
}
