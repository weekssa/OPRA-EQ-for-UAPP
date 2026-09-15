package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.data.dac.FiioJa11PendingRestartWrite
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiioJa11DeviceUiStateTest {
    @Test
    fun pendingRestartVerificationRemainsBusyUntilReplacementSessionIsVerified() {
        val baseline = snapshot(sessionGeneration = 4L)
        val pending = FiioJa11PendingRestartWrite(
            controlId = FiioJa11DeviceControls.UAC_MODE,
            requestedValue = DacControlValue.Discrete(FiioJa11Protocol.UacMode.UAC_1.name.lowercase()),
            previousSessionGeneration = baseline.sessionGeneration,
            baseline = baseline,
        )

        val awaitingReconnect = FiioJa11DeviceUiState(snapshot = baseline, isCurrentSession = true)
            .reconnectRequired(pending)

        assertTrue(awaitingReconnect.isBusy)
        assertFalse(awaitingReconnect.isReading)
        assertFalse(awaitingReconnect.isWriting)
        assertFalse(awaitingReconnect.isCurrentSession)
        assertTrue(awaitingReconnect.pendingRestartWrite === pending)
    }

    @Test
    fun verifiedReplacementSessionClearsBusyRestartState() {
        val baseline = snapshot(sessionGeneration = 7L)
        val pending = FiioJa11PendingRestartWrite(
            controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
            requestedValue = DacControlValue.Toggle(false),
            previousSessionGeneration = baseline.sessionGeneration,
            baseline = baseline,
        )
        val replacement = snapshot(sessionGeneration = 8L).copy(headsetControlEnabled = false)

        val verified = FiioJa11DeviceUiState(snapshot = baseline)
            .reconnectRequired(pending)
            .verified(FiioJa11DeviceControls.HEADSET_CONTROL, replacement)

        assertFalse(verified.isBusy)
        assertTrue(verified.isCurrentSession)
        assertTrue(verified.pendingRestartWrite == null)
    }

    private fun snapshot(sessionGeneration: Long): FiioJa11DeviceSnapshot = FiioJa11DeviceSnapshot(
        sessionGeneration = sessionGeneration,
        usbProductId = FiioJa11Protocol.PRODUCT_ID_UAC_2,
        firmwareVersion = "2.20",
        sampleRateLabel = "48 kHz",
        outputVolume = 30,
        headsetControlEnabled = true,
        eqProgram = FiioJa11Protocol.EqProgram.USER_1,
        uacMode = FiioJa11Protocol.UacMode.UAC_2,
    )
}
