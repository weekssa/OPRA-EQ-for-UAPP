package com.weekssa.opraeqforuapp.ui

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.data.dac.FiioJa11PendingRestartWrite
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsEqReadback
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsMechanism
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsOperationReport
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsRequest
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsStage
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsWriteCounts
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
            previousDetachGeneration = 0L,
            previousDeviceSerial = null,
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
            previousDetachGeneration = 0L,
            previousDeviceSerial = null,
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
    fun failedRestartVerificationClearsPendingAuthorization() {
        val baseline = snapshot(sessionGeneration = 7L)
        val pending = FiioJa11PendingRestartWrite(
            controlId = FiioJa11DeviceControls.HEADSET_CONTROL,
            requestedValue = DacControlValue.Toggle(false),
            previousSessionGeneration = baseline.sessionGeneration,
            previousDetachGeneration = 0L,
            previousDeviceSerial = null,
            baseline = baseline,
        )

        val failed = FiioJa11DeviceUiState(snapshot = baseline)
            .reconnectRequired(pending)
            .failure("FiiO JA11 did not reconnect before verification timed out.")

        assertFalse(failed.isBusy)
        assertTrue(failed.pendingRestartWrite == null)
        assertFalse(failed.isCurrentSession)
    }

    @Test
    fun changedBankCannotRenderAsVerifiedEvenIfStageAccidentallySaysComplete() {
        val report = defaultsReport(
            stage = FiioJa11DefaultsStage.COMPLETE,
            mechanism = FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE,
            eqReadback = FiioJa11DefaultsEqReadback.CHANGED,
        )

        assertFalse(fiioJa11DefaultsReportIsVerified(report))
        assertTrue(fiioJa11DefaultsReportText(report).startsWith("Defaults were not verified"))
        assertFalse(fiioJa11DefaultsReportText(report).contains("Defaults verified"))

        val finalVerifyReport = report.copy(stage = FiioJa11DefaultsStage.NATIVE_FINAL_VERIFY)
        assertFalse(fiioJa11DefaultsReportIsVerified(finalVerifyReport))
        assertFalse(fiioJa11DefaultsReportText(finalVerifyReport).contains("NATIVE_FINAL_VERIFY"))
        assertFalse(fiioJa11DefaultsReportText(finalVerifyReport).contains("bands/gain"))
    }

    @Test
    fun nativeDefaultsCopyDescribesUnchangedRestagingWithoutClaimingNoEqWrites() {
        val report = defaultsReport(
            stage = FiioJa11DefaultsStage.COMPLETE,
            mechanism = FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE,
            eqReadback = FiioJa11DefaultsEqReadback.MATCHED,
            counts = FiioJa11DefaultsWriteCounts(
                volumeWrites = 1,
                eqBandWrites = FiioJa11Protocol.BAND_COUNT,
                eqGainWrites = 1,
                eqApplyWrites = 1,
                eqSaveWrites = 1,
            ),
        )
        val text = fiioJa11DefaultsReportText(report)

        assertTrue(text.contains("Saved to JA11. Volume 30 and User 1 are set."))
        assertTrue(text.contains("active EQ and microphone setting were preserved"))
        assertFalse(text.contains("bands/gain"))
        assertThat(requireNotNull(fiioJa11DefaultsEqReadbackText(report)))
            .isEqualTo("Final EQ readback matched.")
    }

    @Test
    fun microphoneFallbackUnavailableCopyDoesNotClaimTheEqWasCompared() {
        val report = defaultsReport(
            stage = FiioJa11DefaultsStage.COMPLETE,
            mechanism = FiioJa11DefaultsMechanism.MIC_RECONNECT,
            eqReadback = FiioJa11DefaultsEqReadback.UNAVAILABLE,
            counts = FiioJa11DefaultsWriteCounts(micWrites = 2),
        )
        val text = fiioJa11DefaultsReportText(report)

        assertTrue(text.contains("existing User 1 EQ was not written, but was not compared"))
        assertFalse(text.contains("matched on readback"))
        assertFalse(text.contains("bands/gain"))
    }

    @Test
    fun microphoneFallbackMatchedCopyClaimsAComparedUnchangedEq() {
        val report = defaultsReport(
            stage = FiioJa11DefaultsStage.COMPLETE,
            mechanism = FiioJa11DefaultsMechanism.MIC_RECONNECT,
            eqReadback = FiioJa11DefaultsEqReadback.MATCHED,
            counts = FiioJa11DefaultsWriteCounts(micWrites = 2),
        )

        assertTrue(fiioJa11DefaultsReportText(report)
            .contains("existing User 1 EQ was not changed and matched on readback"))
    }

    @Test
    fun confirmationCopyIsMechanismAwareBeforeAnyWrite() {
        val userOne = snapshot(7L)
        val nativeText = fiioJa11DefaultsConfirmationText(
            FiioJa11DefaultsRequest(
                reviewedSnapshot = userOne,
                requestedVolume = userOne.outputVolume,
                requestedProgram = FiioJa11Protocol.EqProgram.USER_1,
                targetControlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
            ),
        )
        assertTrue(nativeText.contains("startup defaults"))
        assertTrue(nativeText.contains("keeps the active EQ and microphone setting"))
        assertTrue(nativeText.contains("JA11 may disconnect briefly"))
        assertFalse(nativeText.contains("Apply"))
        assertFalse(nativeText.contains("bands/gain"))

        for (reviewedMic in listOf(false, true)) {
            val reviewed = if (reviewedMic) "On" else "Off"
            val switched = if (reviewedMic) "Off" else "On"
            val classic = userOne.copy(
                eqProgram = FiioJa11Protocol.EqProgram.CLASSIC,
                headsetControlEnabled = reviewedMic,
            )
            val microphoneText = fiioJa11DefaultsConfirmationText(
                FiioJa11DefaultsRequest(
                    reviewedSnapshot = classic,
                    requestedVolume = classic.outputVolume,
                    requestedProgram = FiioJa11Protocol.EqProgram.CLASSIC,
                    targetControlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                ),
                mechanism = FiioJa11DefaultsMechanism.MIC_RECONNECT,
            )
            assertTrue(microphoneText.contains("EQ Library will not write the User 1 EQ"))
            assertTrue(microphoneText.contains("Reviewed microphone setting: $reviewed"))
            assertTrue(microphoneText.contains("switch it to $switched, then back to $reviewed"))
            assertTrue(microphoneText.contains("two USB audio reconnects"))
        }
    }

    private fun defaultsReport(
        stage: FiioJa11DefaultsStage,
        mechanism: FiioJa11DefaultsMechanism,
        eqReadback: FiioJa11DefaultsEqReadback,
        counts: FiioJa11DefaultsWriteCounts = FiioJa11DefaultsWriteCounts(),
    ) = FiioJa11DefaultsOperationReport(
        operationId = "defaults-test",
        stage = stage,
        originalMicEnabled = true,
        requestedVolume = 30,
        requestedProgram = FiioJa11Protocol.EqProgram.USER_1,
        counts = counts,
        lastVerifiedSnapshot = snapshot(8L),
        mechanism = mechanism,
        eqReadback = eqReadback,
    )

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
