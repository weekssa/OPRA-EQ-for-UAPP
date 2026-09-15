package com.weekssa.opraeqforuapp.ui.screens

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControls
import org.junit.Test

class BlackPearlDeviceResetSectionTest {
    @Test
    fun stalePreWriteSnapshot_waitsInsteadOfFailing() {
        val result = blackPearlRestoreStepVerification(
            isBusy = false,
            writeGeneration = 7L,
            issuedFromWriteGeneration = 7L,
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
            writeGeneration = 8L,
            issuedFromWriteGeneration = 7L,
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
            writeGeneration = 8L,
            issuedFromWriteGeneration = 7L,
            lastVerifiedWriteControlId = BlackPearlDeviceControls.DAC_FILTER,
            expectedControlId = BlackPearlDeviceControls.DAC_FILTER,
            requestedValueSatisfied = true,
        )
        val mismatch = blackPearlRestoreStepVerification(
            isBusy = false,
            writeGeneration = 8L,
            issuedFromWriteGeneration = 7L,
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
            writeGeneration = 8L,
            issuedFromWriteGeneration = 7L,
            lastVerifiedWriteControlId = BlackPearlDeviceControls.GAIN_MODE,
            expectedControlId = BlackPearlDeviceControls.DAC_FILTER,
            requestedValueSatisfied = true,
        )

        assertThat(result).isEqualTo(BlackPearlRestoreStepVerification.MISMATCH)
    }

    @Test
    fun previousPlaybackVerification_cannotBeReusedForFinalPlaybackStep() {
        val staleSameControl = blackPearlRestoreStepVerification(
            isBusy = false,
            writeGeneration = 12L,
            issuedFromWriteGeneration = 12L,
            lastVerifiedWriteControlId = BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
            expectedControlId = BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
            requestedValueSatisfied = false,
        )
        val freshFinalPlayback = blackPearlRestoreStepVerification(
            isBusy = false,
            writeGeneration = 13L,
            issuedFromWriteGeneration = 12L,
            lastVerifiedWriteControlId = BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
            expectedControlId = BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
            requestedValueSatisfied = true,
        )

        assertThat(staleSameControl).isEqualTo(BlackPearlRestoreStepVerification.WAITING)
        assertThat(freshFinalPlayback).isEqualTo(BlackPearlRestoreStepVerification.SATISFIED)
    }
}
