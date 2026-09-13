package com.weekssa.opraeqforuapp.ui

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControls
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot
import org.junit.Test

class BlackPearlQualificationUiStateTest {
    @Test
    fun writeLifecycleNeverTreatsRequestedLocalValueAsCurrentBeforeVerification() {
        val baseline = snapshot(filterCode = 2)
        val initial = BlackPearlQualificationUiState().success(baseline)

        val writing = initial.beginWrite(BlackPearlDeviceControls.DAC_FILTER)
        assertThat(writing.isWriting).isTrue()
        assertThat(writing.snapshot).isEqualTo(baseline)
        assertThat(writing.lastVerifiedWriteControlId).isNull()

        val verified = writing.writeVerified(
            BlackPearlDeviceControls.DAC_FILTER,
            snapshot(filterCode = 3),
        )
        assertThat(verified.isWriting).isFalse()
        assertThat(verified.isCurrentSession).isTrue()
        assertThat(verified.snapshot?.filterCode).isEqualTo(3)
        assertThat(verified.lastVerifiedWriteControlId).isEqualTo(BlackPearlDeviceControls.DAC_FILTER)
    }

    @Test
    fun failedWriteClearsBusyStateAndDefaultsHardwareStateToNotCurrent() {
        val state = BlackPearlQualificationUiState()
            .success(snapshot(filterCode = 2))
            .beginWrite(BlackPearlDeviceControls.DAC_FILTER)
            .writeFailure("Verification failed")

        assertThat(state.isBusy).isFalse()
        assertThat(state.isCurrentSession).isFalse()
        assertThat(state.error).isEqualTo("Verification failed")
    }

    @Test
    fun currentSessionProjectionPreservesActiveWriteAndDoesNotPresentBaselineAsCurrent() {
        val writing = BlackPearlQualificationUiState()
            .success(snapshot(filterCode = 2))
            .beginWrite(BlackPearlDeviceControls.DAC_FILTER)

        val projected = writing.withSessionCurrent(current = true)

        assertThat(projected.isBusy).isTrue()
        assertThat(projected.isWriting).isTrue()
        assertThat(projected.activeWriteControlId).isEqualTo(BlackPearlDeviceControls.DAC_FILTER)
        assertThat(projected.isCurrentSession).isFalse()
    }

    @Test
    fun currentSessionProjectionPreservesReadBusyStateWithoutPresentingOldSnapshotAsCurrent() {
        val reading = BlackPearlQualificationUiState()
            .success(snapshot(filterCode = 2))
            .beginRead()

        val projected = reading.withSessionCurrent(current = true)

        assertThat(projected.isBusy).isTrue()
        assertThat(projected.isReading).isTrue()
        assertThat(projected.isCurrentSession).isFalse()
    }

    @Test
    fun staleSessionProjectionKeepsActiveTransactionBusyUntilRepositoryCompletes() {
        val writing = BlackPearlQualificationUiState()
            .success(snapshot(filterCode = 2))
            .beginWrite(BlackPearlDeviceControls.DAC_FILTER)

        val projected = writing.withSessionCurrent(current = false)

        assertThat(projected.isBusy).isTrue()
        assertThat(projected.isWriting).isTrue()
        assertThat(projected.activeWriteControlId).isEqualTo(BlackPearlDeviceControls.DAC_FILTER)
        assertThat(projected.isCurrentSession).isFalse()
        assertThat(projected.snapshot).isNotNull()
    }

    private fun snapshot(filterCode: Int) = BlackPearlDeviceQualificationSnapshot(
        sessionGeneration = 7L,
        firmwareVersion = "0.6",
        filterCode = filterCode,
        gainModeCode = 1,
        ampTopologyCode = 1,
        micGainDb = 0,
        leftBalanceDb = 0,
        rightBalanceDb = 0,
        playbackGainRaw = 512,
    )
}
