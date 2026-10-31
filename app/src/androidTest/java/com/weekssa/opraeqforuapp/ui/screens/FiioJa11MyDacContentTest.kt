package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
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
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsRequest
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsEqReadback
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsMechanism
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsOperationReport
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsStage
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsWriteCounts
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStage
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationTrace
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiState
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import org.junit.Assert.assertEquals
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
        composeRule.onAllNodesWithText("Flash successful", substring = true).assertCountEquals(0)
    }

    @Test
    fun completedEditorApplyResultRestoresWithoutStartingAnotherMutation() {
        val restorationTester = StateRestorationTester(composeRule)
        var applyRequests = 0
        val trace = editorApplyTrace()
        setContentForTrace(
            trace = trace,
            restorationTester = restorationTester,
            onApply = { applyRequests += 1 },
        )

        composeRule.onNodeWithText("JA11 Apply verified").performScrollTo().assertIsDisplayed()
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("JA11 Apply verified").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { assertEquals("result restoration cannot reissue Apply", 0, applyRequests) }
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
        composeRule.onAllNodesWithText("Apply successful", substring = true).assertCountEquals(0)
    }

    @Test
    fun verifiedEditorApplyCanBeDismissedAndExpiresOnTestClock() {
        setContentForTrace(editorApplyTrace())
        composeRule.onNodeWithText("JA11 Apply verified").performScrollTo().assertIsDisplayed()
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(FIIO_JA11_TERMINAL_SUCCESS_EXPIRY_MILLIS + 1L)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("JA11 Apply verified").assertCountEquals(0)
    }

    @Test
    fun verifiedEditorApplyCanBeDismissedBeforeExpiry() {
        setContentForTrace(editorApplyTrace())

        composeRule.onNodeWithContentDescription("Dismiss JA11 Apply result")
            .performScrollTo()
            .performClick()
        composeRule.onAllNodesWithText("JA11 Apply verified").assertCountEquals(0)
    }

    @Test
    fun stagedVolumeCancelDoesNotWriteAndApplyWritesOnlyRuntimeValue() {
        var appliedVolume: Int? = null
        setContentForTrace(editorApplyTrace(), onVolume = { appliedVolume = it })
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithText("Device volume").performClick()
        composeRule.onNodeWithText("+1").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle { assertEquals(null, appliedVolume) }

        composeRule.onNodeWithText("Device volume").performClick()
        composeRule.onNodeWithText("+1").performClick()
        composeRule.onNodeWithText("Apply").performClick()
        composeRule.runOnIdle { assertEquals(21, appliedVolume) }
    }

    @Test
    fun userOneSetAsDefaultShowsNativePreservationConfirmationAndCapturesReviewedPair() {
        var request: FiioJa11DefaultsRequest? = null
        setContentForTrace(editorApplyTrace(), onDefaults = { request = it })
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithText("Device volume").performClick()
        composeRule.onNodeWithText("+1").performClick()
        composeRule.onNodeWithText("Set as Default").performClick()
        composeRule.onNodeWithText("Confirm and Set Default").assertIsDisplayed()
        composeRule.onNodeWithText("preserve and verify the existing User 1 EQ and microphone setting", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("disconnect briefly during Save", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("No EQ-band data is written", substring = true).assertCountEquals(0)
        composeRule.runOnIdle { assertEquals(null, request) }
        composeRule.onNodeWithText("Confirm and Set Default").performClick()
        composeRule.runOnIdle {
            assertEquals(21, request?.requestedVolume)
            assertEquals(FiioJa11Protocol.EqProgram.USER_1, request?.requestedProgram)
        }
    }

    @Test
    fun nonUserOneSetAsDefaultShowsReviewedMicTransitionsAndTwoReconnects() {
        var request: FiioJa11DefaultsRequest? = null
        setContentForTrace(
            editorApplyTrace(),
            deviceEqProgram = FiioJa11Protocol.EqProgram.CLASSIC,
            onDefaults = { request = it },
        )
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithText("EQ program").performClick()
        composeRule.onNodeWithText("Set as Default").performClick()

        composeRule.onNodeWithText("Reviewed microphone setting: Off", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("switch it to On, then back to Off", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("two USB audio reconnects", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("EQ Library will not write the User 1 EQ", substring = true).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(null, request) }
        composeRule.onNodeWithText("Confirm and Set Default").performClick()
        composeRule.runOnIdle {
            assertEquals(FiioJa11Protocol.EqProgram.CLASSIC, request?.requestedProgram)
        }
    }

    @Test
    fun changedDefaultsReadbackRendersAsFailureInsteadOfSuccess() {
        setContentForTrace(
            editorApplyTrace(),
            defaultsReport = FiioJa11DefaultsOperationReport(
                operationId = "defaults-failed",
                stage = FiioJa11DefaultsStage.NATIVE_FINAL_VERIFY,
                originalMicEnabled = false,
                requestedVolume = 20,
                requestedProgram = FiioJa11Protocol.EqProgram.USER_1,
                counts = FiioJa11DefaultsWriteCounts(
                    eqBandWrites = FiioJa11Protocol.BAND_COUNT,
                    eqGainWrites = 1,
                    eqApplyWrites = 1,
                    eqSaveWrites = 1,
                ),
                lastVerifiedSnapshot = deviceSnapshot(),
                mechanism = FiioJa11DefaultsMechanism.NATIVE_USER_1_REWRITE,
                eqReadback = FiioJa11DefaultsEqReadback.CHANGED,
            ),
        )
        composeRule.onNodeWithText("DEVICE").performClick()

        composeRule.onNodeWithText("Defaults were not verified", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Final User 1 EQ readback did not match", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("Defaults verified", substring = true).assertCountEquals(0)
    }

    @Test
    fun cancellingDefaultConfirmationReturnsToStagedVolumeWithoutWriting() {
        var applyCalls = 0
        var defaultCalls = 0
        setContentForTrace(
            editorApplyTrace(),
            onVolume = { applyCalls += 1 },
            onDefaults = { defaultCalls += 1 },
        )
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithText("Device volume").performClick()
        composeRule.onNodeWithText("+1").performClick()
        composeRule.onNodeWithText("Set as Default").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onAllNodesWithText("Device volume").assertCountEquals(2)
        composeRule.onNodeWithText("21 / 60").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(0, applyCalls)
            assertEquals(0, defaultCalls)
        }
    }

    @Test
    fun restoringDefaultsDialogStateDoesNotIssueAWrite() {
        val restorationTester = StateRestorationTester(composeRule)
        var defaultCalls = 0
        setContentForTrace(
            editorApplyTrace(),
            restorationTester = restorationTester,
            onDefaults = { defaultCalls += 1 },
        )
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithText("Device volume").performClick()
        composeRule.onNodeWithText("Set as Default").performClick()
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.runOnIdle { assertEquals(0, defaultCalls) }
    }

    @Test
    fun eqProgramSelectionStaysLocalUntilApply() {
        val appliedPrograms = mutableListOf<FiioJa11Protocol.EqProgram>()
        setContentForTrace(editorApplyTrace(), onProgram = { appliedPrograms += it })
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithText("EQ program").performClick()
        composeRule.onNodeWithText("Classic", substring = true).performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle { assertEquals(emptyList<FiioJa11Protocol.EqProgram>(), appliedPrograms) }

        composeRule.onNodeWithText("EQ program").performClick()
        composeRule.onNodeWithText("Classic", substring = true).performClick()
        composeRule.onNodeWithText("Apply").performClick()
        composeRule.runOnIdle { assertEquals(listOf(FiioJa11Protocol.EqProgram.CLASSIC), appliedPrograms) }
    }

    private fun setContentForTrace(
        trace: FiioJa11OperationTrace,
        restorationTester: StateRestorationTester? = null,
        onApply: () -> Unit = {},
        onVolume: (Int) -> Unit = {},
        onProgram: (FiioJa11Protocol.EqProgram) -> Unit = {},
        onDefaults: (FiioJa11DefaultsRequest) -> Unit = {},
        deviceEqProgram: FiioJa11Protocol.EqProgram = FiioJa11Protocol.EqProgram.USER_1,
        defaultsReport: FiioJa11DefaultsOperationReport? = null,
    ) {
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

        val content: @Composable () -> Unit = {
            FiioJa11MyDacContent(
                connectionState = Kt02h20ConnectionState.Connected,
                hardwareEqState = HardwareEqSnapshotState(
                    bundle = bundle,
                    freshness = DacStateFreshness.CURRENT,
                ),
                editorState = MyDacEditorUiState(),
                deviceState = FiioJa11DeviceUiState(
                    snapshot = deviceSnapshot(eqProgram = deviceEqProgram),
                    isCurrentSession = true,
                    defaultsReport = defaultsReport,
                ),
                onConnect = {},
                onReadDeviceControls = {},
                onSetOutputVolume = onVolume,
                onSetEqProgram = onProgram,
                onSetDefaults = onDefaults,
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
                onApply = { onApply() },
                operationTrace = trace,
                operationStatus = FiioJa11OperationStatus.Completed(trace),
                onMessage = {},
            )
        }
        if (restorationTester == null) {
            composeRule.setContent { content() }
        } else {
            restorationTester.setContent { content() }
        }
    }

    private fun deviceSnapshot(
        eqProgram: FiioJa11Protocol.EqProgram = FiioJa11Protocol.EqProgram.USER_1,
    ) = FiioJa11DeviceSnapshot(
        sessionGeneration = 1L,
        usbProductId = FiioJa11Protocol.PRODUCT_ID_UAC_2,
        firmwareVersion = "1.0",
        sampleRateLabel = "96 kHz",
        outputVolume = 20,
        headsetControlEnabled = false,
        eqProgram = eqProgram,
        uacMode = FiioJa11Protocol.UacMode.UAC_2,
    )

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
