package com.weekssa.opraeqforuapp.ui.screens

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import org.junit.Test

class DeviceOperationStatusTest {
    private val volume = DacControlId("device.volume")

    @Test
    fun activeWriteShowsApplyingInsteadOfStaleState() {
        val presentation = deviceOperationStatusPresentation(
            isReading = false,
            isWriting = true,
            activeWriteControlId = volume,
            pendingVerificationControlId = null,
            hasSnapshot = true,
            isCurrentSession = false,
            controlName = { "volume" },
        )

        assertThat(presentation.heading).isEqualTo("Applying volume…")
        assertThat(presentation.staleMessage).isNull()
    }

    @Test
    fun restartWriteStaysApplyingUntilReplacementSessionIsVerified() {
        val presentation = deviceOperationStatusPresentation(
            isReading = false,
            isWriting = false,
            activeWriteControlId = null,
            pendingVerificationControlId = volume,
            hasSnapshot = true,
            isCurrentSession = false,
            controlName = { "volume" },
        )

        assertThat(presentation.heading).isEqualTo("Applying volume…")
        assertThat(presentation.pendingMessage).contains("Reconnect")
        assertThat(presentation.staleMessage).isNull()
    }

    @Test
    fun idleStaleSnapshotUsesLastReadMessage() {
        val presentation = deviceOperationStatusPresentation(
            isReading = false,
            isWriting = false,
            activeWriteControlId = null,
            pendingVerificationControlId = null,
            hasSnapshot = true,
            isCurrentSession = false,
            controlName = { "volume" },
        )

        assertThat(presentation.heading).isEqualTo("Last read")
        assertThat(presentation.staleMessage).isNotNull()
    }

    @Test
    fun controlLabelUsesCapabilityIdAsStableFallback() {
        assertThat(deviceOperationControlLabel(DacControlId("fiio.uac_mode")))
            .isEqualTo("Uac Mode")
    }

    @Test
    fun currentIdleSnapshotReportsCurrentDeviceState() {
        val presentation = deviceOperationStatusPresentation(
            isReading = false,
            isWriting = false,
            activeWriteControlId = null,
            pendingVerificationControlId = null,
            hasSnapshot = true,
            isCurrentSession = true,
            controlName = { "volume" },
        )

        assertThat(presentation.heading).isEqualTo("Current device state")
        assertThat(presentation.staleMessage).isNull()
    }
}
