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
            deviceSerialIdentity = "ja11-test",
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
            deviceSerialIdentity = "ja11-test",
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

    @Test
    fun wrongDeviceKeepsOriginalIntentPendingUntilReconnectOrTimeout() {
        val baseline = snapshot(sessionGeneration = 8L)
        val pending = FiioJa11PendingRestartWrite(
            controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
            requestedValue = DacControlValue.Toggle(false),
            previousSessionGeneration = baseline.sessionGeneration,
            deviceSerialIdentity = "original-ja11",
            baseline = baseline,
        )

        val waiting = FiioJa11DeviceUiState(snapshot = baseline)
            .beginRestartWrite(pending)
            .reconnectRequired(pending)
            .awaitingReconnect("Waiting for the original JA11")

        assertTrue(waiting.isBusy)
        assertTrue(waiting.pendingRestartWrite === pending)
        assertFalse(waiting.isCurrentSession)
        assertTrue(waiting.error == "Waiting for the original JA11")
    }

    @Test
    fun terminalRestartFailuresClearBusyStateForAllFailureClasses() {
        val baseline = snapshot(sessionGeneration = 9L)
        val pending = FiioJa11PendingRestartWrite(
            controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
            requestedValue = DacControlValue.Toggle(false),
            previousSessionGeneration = baseline.sessionGeneration,
            deviceSerialIdentity = "ja11-test",
            baseline = baseline,
        )

        listOf(
            "Value mismatch",
            "Verification timed out",
            "Android USB permission was not granted",
            "Replacement read failed",
            "A different USB device returned",
            "Reconnect failed",
        ).forEach { message ->
            val writing = FiioJa11DeviceUiState(snapshot = baseline, isCurrentSession = true)
                .beginRestartWrite(pending)

            assertTrue(writing.isBusy)
            assertTrue(writing.isWriting)
            assertTrue(writing.pendingRestartWrite === pending)

            val reconnecting = writing.awaitingReconnect("Reconnecting")
            assertTrue(reconnecting.isBusy)
            assertTrue(reconnecting.pendingRestartWrite === pending)

            val terminal = reconnecting.failure(message)
            assertFalse("$message must clear the busy state", terminal.isBusy)
            assertTrue("$message must clear pending state", terminal.pendingRestartWrite == null)
            assertTrue("$message must preserve terminal race identity", terminal.terminalRestartWrite === pending)
            assertFalse("$message must not treat the previous session as current", terminal.isCurrentSession)
        }
    }

    @Test
    fun writeResultCannotRecreatePendingStateAfterReplacementSessionAlreadyVerified() {
        val baseline = snapshot(sessionGeneration = 12L)
        val pending = FiioJa11PendingRestartWrite(
            controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
            requestedValue = DacControlValue.Toggle(false),
            previousSessionGeneration = baseline.sessionGeneration,
            deviceSerialIdentity = "ja11-test",
            baseline = baseline,
        )
        val replacement = snapshot(sessionGeneration = 13L).copy(headsetControlEnabled = false)
        val verified = FiioJa11DeviceUiState(snapshot = baseline)
            .beginRestartWrite(pending)
            .verified(FiioJa11DeviceControls.HEADSET_CONTROL, replacement)

        val lateWriteResult = verified.reconnectRequired(pending)

        assertFalse(lateWriteResult.isBusy)
        assertTrue(lateWriteResult.pendingRestartWrite == null)
        assertTrue(lateWriteResult.isCurrentSession)
        assertTrue(lateWriteResult.lastVerifiedWriteControlId == FiioJa11DeviceControls.HEADSET_CONTROL)
    }

    @Test
    fun staleWriteResultCannotReplaceANewerPendingRestartWrite() {
        val firstBaseline = snapshot(sessionGeneration = 20L)
        val firstPending = FiioJa11PendingRestartWrite(
            controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
            requestedValue = DacControlValue.Toggle(false),
            previousSessionGeneration = firstBaseline.sessionGeneration,
            deviceSerialIdentity = "ja11-test",
            baseline = firstBaseline,
        )
        val secondBaseline = snapshot(sessionGeneration = 21L).copy(headsetControlEnabled = false)
        val secondPending = FiioJa11PendingRestartWrite(
            controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
            requestedValue = DacControlValue.Toggle(true),
            previousSessionGeneration = secondBaseline.sessionGeneration,
            deviceSerialIdentity = "ja11-test",
            baseline = secondBaseline,
        )
        val newerWriteState = FiioJa11DeviceUiState(snapshot = secondBaseline)
            .beginRestartWrite(secondPending)

        val afterLateResult = newerWriteState.reconnectRequired(firstPending)

        assertTrue(afterLateResult.pendingRestartWrite === secondPending)
        assertTrue(afterLateResult.isWriting)
        assertTrue(afterLateResult.activeWriteControlId == FiioJa11DeviceControls.HEADSET_CONTROL)
    }

    @Test
    fun lateWriteResultCannotRecreatePendingStateAfterTerminalVerificationFailure() {
        val baseline = snapshot(sessionGeneration = 15L)
        val pending = FiioJa11PendingRestartWrite(
            controlId = FiioJa11DeviceControls.UAC_MODE,
            requestedValue = DacControlValue.Discrete("uac_1"),
            previousSessionGeneration = baseline.sessionGeneration,
            deviceSerialIdentity = "ja11-test",
            baseline = baseline,
        )
        val terminal = FiioJa11DeviceUiState(snapshot = baseline)
            .beginRestartWrite(pending)
            .failure("The replacement session could not verify the change")
        val pendingFromWriteResult = pending.copy(baseline = baseline.copy(sampleRateLabel = "44.1 kHz"))

        val lateResultState = terminal.reconnectRequired(pendingFromWriteResult)

        assertFalse(lateResultState.isBusy)
        assertTrue(lateResultState.pendingRestartWrite == null)
        assertTrue(lateResultState.error == "The replacement session could not verify the change")
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
