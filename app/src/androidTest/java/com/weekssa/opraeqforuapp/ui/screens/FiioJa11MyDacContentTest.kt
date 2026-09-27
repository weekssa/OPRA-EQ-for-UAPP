package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStage
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationTrace
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiState
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import org.junit.Rule
import org.junit.Test

class FiioJa11MyDacContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun completedEditorApplyShowsOneTruthfulTerminalResultAndBothReports() {
        val trace = editorApplyTrace()
        setContentForTrace(trace)

        composeRule.onAllNodesWithText("JA11 Apply verified").assertCountEquals(1)
        composeRule.onNodeWithText("Apply successful", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Final hardware readback matched", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(FIIO_JA11_READABLE_REPORT_LABEL).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(FIIO_JA11_TECHNICAL_REPORT_LABEL).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dismiss JA11 Apply result").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Flash successful", substring = true).assertDoesNotExist()
    }

    @Test
    fun failedEditorApplyRemainsTruthfulAndActionable() {
        val trace = editorApplyTrace(
            outcome = "VerificationFailed",
            stages = listOf(FiioJa11OperationStage.FINAL_READBACK, FiioJa11OperationStage.FAILED),
            failureReason = "final readback mismatch",
        )
        setContentForTrace(trace)

        composeRule.onNodeWithText("JA11 Apply not verified").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("was not verified", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Do not retry", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("final readback mismatch", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Apply successful", substring = true).assertDoesNotExist()
    }

    @Test
    fun verifiedEditorApplyCanBeDismissedAndExpiresOnTestClock() {
        setContentForTrace(editorApplyTrace())
        composeRule.onNodeWithText("JA11 Apply verified").performScrollTo().assertIsDisplayed()
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(FIIO_JA11_TERMINAL_SUCCESS_EXPIRY_MILLIS + 1L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("JA11 Apply verified").assertDoesNotExist()
    }

    @Test
    fun verifiedEditorApplyCanBeDismissedBeforeExpiry() {
        setContentForTrace(editorApplyTrace())

        composeRule.onNodeWithContentDescription("Dismiss JA11 Apply result")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("JA11 Apply verified").assertDoesNotExist()
    }

    private fun setContentForTrace(trace: FiioJa11OperationTrace) {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.fiioJa11(
                nativeBands = List(FiioJa11Protocol.BAND_COUNT) { index ->
                    FiioJa11Protocol.Band("peak_dip", 100.0 * (index + 1), 0.0, 1.0)
                },
                globalEqGainDb = 0.0,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1L,
            ),
        )

        composeRule.setContent {
            FiioJa11MyDacContent(
                connectionState = Kt02h20ConnectionState.Connected,
                hardwareEqState = HardwareEqSnapshotState(
                    bundle = bundle,
                    freshness = DacStateFreshness.CURRENT,
                ),
                editorState = MyDacEditorUiState(),
                deviceState = FiioJa11DeviceUiState(
                    snapshot = FiioJa11DeviceSnapshot(
                        sessionGeneration = 1L,
                        usbProductId = FiioJa11Protocol.PRODUCT_ID_UAC_2,
                        firmwareVersion = "1.0",
                        sampleRateLabel = "96 kHz",
                        outputVolume = 20,
                        headsetControlEnabled = false,
                        eqProgram = FiioJa11Protocol.EqProgram.USER_1,
                        uacMode = FiioJa11Protocol.UacMode.UAC_2,
                    ),
                    isCurrentSession = true,
                ),
                onConnect = {},
                onReadDeviceControls = {},
                onSetOutputVolume = {},
                onSetEqProgram = {},
                onSetHeadsetControl = {},
                onSetUacMode = {},
                onResetEq = { "" },
                onOpenEditor = {},
                onCloseEditor = {},
                onSelectBand = {},
                onShowAllBands = {},
                onShowReview = {},
                onUpdateBand = { _, _, _, _, _ -> },
                onUseSafeGain = {},
                onResetEdits = {},
                onApply = {},
                operationTrace = trace,
                operationStatus = FiioJa11OperationStatus.Completed(trace),
                onMessage = {},
            )
        }
    }

    private fun editorApplyTrace(
        outcome: String = "Success",
        stateKnown: Boolean = true,
        stages: List<FiioJa11OperationStage> = listOf(
            FiioJa11OperationStage.FINAL_READBACK,
            FiioJa11OperationStage.VERIFIED,
        ),
        failureReason: String? = null,
    ) = FiioJa11OperationTrace(
        operationId = "operation-1",
        operation = "EDITOR_APPLY",
        sourceCommit = "candidate",
        appVersion = "0.7.0",
        signerVerified = true,
        deviceFingerprintKey = "vid=2972|pid=0102|interface=3",
        usbProductId = 0x0102,
        sessionGeneration = 1L,
        detachGeneration = 0L,
        permissionRequestCount = 0L,
        sourceProfileId = null,
        canonicalPreampGainDb = null,
        generatedOrSelectedTargetGainDb = null,
        quantizedWireTargetGainDb = 0.0,
        readbackGlobalGainDb = 0.0,
        globalGainToleranceDb = 0.001,
        comparisonPhase = "FINAL_READBACK",
        sourceBandCount = null,
        targetBandCount = FiioJa11Protocol.BAND_COUNT,
        fidelity = null,
        usesGeneratedHeadroom = null,
        usedResponseFit = null,
        targetBands = emptyList(),
        saveCommandCount = 1L,
        stateKnown = stateKnown,
        outcome = outcome,
        stages = stages,
        events = emptyList(),
        failureReason = failureReason,
    )
}
