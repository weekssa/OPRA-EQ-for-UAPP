package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditSpecs
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditor
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditorStartResult
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.ew300.Ew300UacStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.ui.Ew300UacUiState
import com.weekssa.opraeqforuapp.ui.MyDacEditorStage
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Ew300FullParityFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun snapshotState(gainDb: Double = -30.0): HardwareEqSnapshotState {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = listOf(
                    Kt02h20Band("peak_dip", 1_000.0, 0.0, 1.0),
                    Kt02h20Band("peak_dip", 2_000.0, 0.0, 1.0),
                    Kt02h20Band("peak_dip", 500.0, 0.0, 1.0),
                    Kt02h20Band("peak_dip", 4_000.0, 0.0, 1.0),
                    Kt02h20Band("peak_dip", 8_000.0, 0.0, 1.0),
                ),
                globalGainDb = gainDb,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1L,
            ),
        )
        return HardwareEqSnapshotState().publishCurrent(bundle)
    }

    @Test
    fun sixtyStepVolumeRowOpensAndInvokesItsLevelCallback() {
        var selectedLevel = -1
        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Ew300DeviceStatus(
                    report = null,
                    hardwareEqState = snapshotState(-30.0),
                    running = false,
                    onRun = {},
                    onShareReadable = {},
                    onShareJson = {},
                    onSetVolumeLevel = { selectedLevel = it },
                )
            }
        }

        composeRule.onNodeWithText("Device volume").performScrollTo().assertIsDisplayed()
        if (!BuildConfig.EW300_PARITY_EXPERIMENT_ENABLED) {
            // The parity surface is compiled off in this variant, so the row must not offer a
            // dialog it cannot honour.
            composeRule.onNodeWithText("Apply").assertDoesNotExist()
            return
        }
        composeRule.onNodeWithText("Device volume").performClick()
        composeRule.onNodeWithText("Device volume").assertIsDisplayed()
        composeRule.onNodeWithText("Apply").performClick()
        assertEquals(30, selectedLevel)
    }

    @Test
    fun uacRowsShowCurrentRateAndNoUnprovenMicrophoneToggle() {
        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Ew300DeviceStatus(
                    report = null,
                    hardwareEqState = snapshotState(),
                    running = false,
                    onRun = {},
                    onShareReadable = {},
                    onShareJson = {},
                    uacState = Ew300UacUiState(
                        status = Ew300UacStatus(
                            uacVersion = "UAC 1.0",
                            currentSampleRateHz = 48_000,
                            sampleRatePath = "UAC1 GET_CUR endpoint",
                            microphoneMuteSupported = true,
                            microphoneMuted = false,
                            headsetFeaturePresent = false,
                            uacModeSwitchSupported = false,
                            blockedReasons = listOf("One configuration exposes UAC 1.0; no exact UAC mode switch control was located."),
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Current sample rate").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("48,000 Hz").assertIsDisplayed()
        composeRule.onNodeWithText("Microphone").assertDoesNotExist()
        composeRule.onNodeWithText("Microphone input").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Microphone mute").assertDoesNotExist()
        composeRule.onNodeWithText("Headset remote").assertDoesNotExist()
        composeRule.onNodeWithText("Read USB audio").assertDoesNotExist()
    }

    @Test
    fun experimentalEditorExposesNativeShelfChips() {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = listOf(
                    Kt02h20Band("low_shelf", 250.0, -6.0, 0.707),
                    Kt02h20Band("peak_dip", 1_000.0, 0.0, 1.0),
                    Kt02h20Band("peak_dip", 2_000.0, 0.0, 1.0),
                    Kt02h20Band("peak_dip", 4_000.0, 0.0, 1.0),
                    Kt02h20Band("high_shelf", 4_000.0, -6.0, 0.707),
                ),
                globalGainDb = -6.0,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1L,
                allowExperimentalShelf = true,
            ),
        )
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = HardwareEqSnapshotState().publishCurrent(bundle),
            spec = HardwareEqEditSpecs.SIMGOT_EW300_PARITY,
        ) as HardwareEqEditorStartResult.Ready
        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DacEqEditorScreen(
                    state = MyDacEditorUiState(
                        stage = MyDacEditorStage.EDIT,
                        workingCopy = started.workingCopy,
                        selectedBandIndex = 0,
                    ),
                    onRetryOpen = {},
                    onClose = {},
                    onSelectBand = {},
                    onShowAllBands = {},
                    onShowReview = {},
                    onUpdateBand = { _, _, _, _, _ -> },
                    onUseSafeGain = {},
                    onResetEdits = {},
                    onApply = {},
                    dacLabel = "SIMGOT EW300 DSP",
                )
            }
        }

        if (BuildConfig.EW300_SHELF_EDITOR_ENABLED) {
            composeRule.onNodeWithText("Low shelf").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("High shelf").performScrollTo().assertIsDisplayed()
        } else {
            // v0.11 product decision: native shelf controls are engineering-only and absent from
            // the product editor. The same working copy still carries the shelf values.
            composeRule.onNodeWithText("Low shelf").assertDoesNotExist()
            composeRule.onNodeWithText("High shelf").assertDoesNotExist()
        }
    }

    @Test
    fun unknownMicReadbackIsNotShownAsLive() {
        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Ew300DeviceStatus(
                    report = null,
                    hardwareEqState = snapshotState(),
                    running = false,
                    onRun = {},
                    onShareReadable = {},
                    onShareJson = {},
                    uacState = Ew300UacUiState(
                        status = Ew300UacStatus(
                            uacVersion = "UAC 1.0",
                            currentSampleRateHz = null,
                            sampleRatePath = null,
                            microphoneMuteSupported = true,
                            microphoneMuted = null,
                            headsetFeaturePresent = false,
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Current sample rate").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Microphone").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Microphone mute").assertDoesNotExist()
    }
}
