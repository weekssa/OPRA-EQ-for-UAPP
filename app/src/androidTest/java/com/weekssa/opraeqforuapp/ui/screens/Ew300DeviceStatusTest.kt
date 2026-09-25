package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityCaseResult
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityReport
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationStage
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationTrace
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Ew300DeviceStatusTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readOnlyReportExplainsSafetyAndExposesBothExportFormats() {
        var runCount = 0
        var readableShareCount = 0
        var jsonShareCount = 0
        val report = Ew300CapabilityReport(
            planVersion = "test-plan",
            deviceFingerprintKey = "test-device",
            cases = listOf(
                Ew300CapabilityCaseResult(
                    caseId = "snapshot",
                    status = Ew300CapabilityCaseResult.Status.PASS,
                    message = "Read-only snapshot passed.",
                ),
            ),
            stateKnown = true,
            stoppedAfterFailure = false,
        )

        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Ew300DeviceStatus(
                    report = report,
                    running = false,
                    onRun = { runCount += 1 },
                    onShareReadable = { readableShareCount += 1 },
                    onShareJson = { jsonShareCount += 1 },
                    validationEvidenceEnabled = true,
                )
            }
        }

        composeRule.onNodeWithText("It never writes, saves, resets, or retries a mutation.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Flash, persistence, and Reset remain hardware-validation pending.", substring = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("Start Save qualification", substring = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("Run read-only report").performScrollTo().assertIsEnabled().performClick()
        composeRule.onNodeWithText("Result: PASS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Share readable report").performScrollTo().performClick()
        composeRule.onNodeWithText("Share technical report").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, runCount)
            assertEquals(1, readableShareCount)
            assertEquals(1, jsonShareCount)
        }
    }

    @Test
    fun publicDeviceStatusIsCompactAndRefreshesThroughItsSharedCallback() {
        var refreshCount = 0
        val report = Ew300CapabilityReport(
            planVersion = "test-plan",
            deviceFingerprintKey = "test-device",
            cases = emptyList(),
            stateKnown = true,
            stoppedAfterFailure = false,
        )

        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Ew300DeviceStatus(
                    report = report,
                    running = false,
                    onRefresh = { refreshCount += 1 },
                    onRun = {},
                    onShareReadable = {},
                    onShareJson = {},
                    operationTrace = Ew300OperationTrace(
                        operationId = "operation-1",
                        operation = "FLASH",
                        sourceCommit = "candidate",
                        appVersion = "0.7.0",
                        signerVerified = true,
                        deviceFingerprintKey = "test-device",
                        sessionGeneration = 2L,
                        detachGeneration = 1L,
                        permissionRequestCount = 1L,
                        permissionRequestsBeforeFirstWrite = 0L,
                        registerWriteCount = 11L,
                        saveCommandCount = 1L,
                        mutationReplayCount = null,
                        competingConnectionJobCount = null,
                        replacementObserved = true,
                        replacementIdentityMatched = true,
                        baselineCaptured = true,
                        volatileReadbackMatched = true,
                        finalReadbackMatched = true,
                        restorationVerified = false,
                        stateKnown = true,
                        outcome = "Success",
                        stages = listOf(Ew300OperationStage.FINAL_READBACK),
                    ),
                    validationEvidenceEnabled = false,
                )
            }
        }

        composeRule.onNodeWithText("Output gain").assertIsDisplayed()
        composeRule.onNodeWithText("Equalizer").assertIsDisplayed()
        composeRule.onNodeWithText("Connection").assertIsDisplayed()
        composeRule.onNodeWithText("Refresh").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Validation capability report").assertDoesNotExist()
        composeRule.onNodeWithText("Run read-only report").assertDoesNotExist()
        composeRule.onNodeWithText("Share readable report").assertDoesNotExist()
        composeRule.onNodeWithText("Share technical report").assertDoesNotExist()
        composeRule.onNodeWithText("Last operation report").assertDoesNotExist()
        composeRule.onNodeWithText("Restore exact pre-test baseline").assertDoesNotExist()
        composeRule.onNodeWithText("Only verified EW300 functions are shown", substring = true).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, refreshCount) }
    }

    @Test
    fun eqOverviewShowsFlatGraphAccessibilityAndManualRefresh() {
        var refreshCount = 0
        val flatBands = List(Ew300Protocol.BAND_COUNT) {
            Kt02h20Band("peak_dip", 1_000.0, 0.0, 1.0)
        }
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = flatBands,
                globalGainDb = 0.0,
                sessionGeneration = 3,
                verifiedAtEpochMillis = 10,
            ),
        )

        composeRule.setContent {
            Ew300MyDacContent(
                connectionState = Kt02h20ConnectionState.Connected,
                hardwareEqState = HardwareEqSnapshotState(
                    bundle = bundle,
                    freshness = DacStateFreshness.CURRENT,
                ),
                editorState = MyDacEditorUiState(),
                operationTrace = null,
                catalogState = CatalogState.Loading,
                managedHeadphones = emptyList(),
                savedEqs = emptyList(),
                savedGeneralEqs = emptyList(),
                onConnect = { refreshCount += 1 },
                onResetEq = {},
                onRestoreBaseline = { "" },
                onRunCapabilityBatch = { error("not invoked") },
                onAdvancePersistenceQualification = { error("not invoked") },
                onCaptureDacEq = { _, _ -> "" },
                onOpenEditor = {},
                onCloseEditor = {},
                onSelectBand = {},
                onShowAllBands = {},
                onShowReview = {},
                onUpdateBand = { _, _, _, _, _ -> },
                onUseSafeGain = {},
                onResetEdits = {},
                onApply = {},
                onMessage = {},
            )
        }

        composeRule.onNodeWithText("Flat").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("EQ response").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "EW300 EQ response graph from 20 hertz to 20 kilohertz with 0 active Peak bands. Response ranges from 0.0 to 0.0 decibels, with a zero-decibel reference line.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Refresh").performScrollTo().assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, refreshCount) }
    }

    @Test
    fun deviceStatusDoesNotPresentFailedCachedReadAsCurrent() {
        val flatBands = List(Ew300Protocol.BAND_COUNT) {
            Kt02h20Band("peak_dip", 1_000.0, 0.0, 1.0)
        }
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = flatBands,
                globalGainDb = 0.0,
                sessionGeneration = 3,
                verifiedAtEpochMillis = 10,
            ),
        )

        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Ew300DeviceStatus(
                    report = null,
                    hardwareEqState = HardwareEqSnapshotState(
                        bundle = bundle,
                        freshness = DacStateFreshness.LAST_READ_STALE,
                        readFailed = true,
                    ),
                    running = false,
                    onRun = {},
                    onShareReadable = null,
                    onShareJson = null,
                )
            }
        }

        composeRule.onNodeWithText("Last read device state").assertIsDisplayed()
        composeRule.onNodeWithText("cached values are not current", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Current device state").assertDoesNotExist()
    }

    @Test
    fun ew300EditorSubtitleUsesTheProvidedDeviceName() {
        composeRule.setContent {
            Text(stringResource(R.string.my_dac_editor_subtitle, "SIMGOT EW300 DSP", 0))
        }

        composeRule.onNodeWithText("SIMGOT EW300 DSP · Slot 0").assertIsDisplayed()
        composeRule.onNodeWithText("TRN Black Pearl", substring = true).assertDoesNotExist()
    }
}
