package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.ew300.Ew300RuntimePersistenceState
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.ui.Ew300RuntimeSaveUiState
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Ew300ParityFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun state(): HardwareEqSnapshotState {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = listOf(
                    Kt02h20Band("peak_dip", 1_000.0, 0.0, 1.0),
                    Kt02h20Band("peak_dip", 2_000.0, -2.0, 1.4),
                    Kt02h20Band("peak_dip", 500.0, 0.0, 1.0),
                    Kt02h20Band("peak_dip", 4_000.0, 0.0, 1.0),
                    Kt02h20Band("peak_dip", 8_000.0, 0.0, 1.0),
                ),
                globalGainDb = -6.0,
                sessionGeneration = 1L,
                verifiedAtEpochMillis = 1L,
            ),
        )
        return HardwareEqSnapshotState().publishCurrent(bundle)
    }

    @Test
    fun eqTabShowsFiveBandRowsAndRuntimeSaveState() {
        var saves = 0
        composeRule.setContent {
            Column(Modifier.height(2000.dp)) {
                Ew300MyDacContent(
                    connectionState = Kt02h20ConnectionState.Connected,
                    hardwareEqState = state(),
                    editorState = MyDacEditorUiState(),
                    operationTrace = null,
                    catalogState = CatalogState.Loading,
                    managedHeadphones = emptyList(),
                    savedEqs = emptyList(),
                    runtimePersistence = Ew300RuntimePersistenceState.RUNTIME_ONLY,
                    runtimeSaveState = Ew300RuntimeSaveUiState(),
                    onConnect = {},
                    onResetEq = { "ok" },
                    onRestoreBaseline = { "ok" },
                    onRunCapabilityBatch = { error("not used") },
                    onAdvancePersistenceQualification = { error("not used") },
                    onCaptureDacEq = { _, _ -> "ok" },
                    onOpenEditor = {},
                    onCloseEditor = {},
                    onSelectBand = {},
                    onShowAllBands = {},
                    onShowReview = {},
                    onUpdateBand = { _, _, _, _, _ -> },
                    onUseSafeGain = {},
                    onResetEdits = {},
                    onApply = {},
                    onSaveRuntime = { saves += 1 },
                    onMessage = {},
                )
            }
        }

        composeRule.onNodeWithText("Current EQ · Five-band bank").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1   Peak   1,000 Hz   +0.0 dB   Q 1.000").assertIsDisplayed()
        composeRule.onNodeWithText("Runtime only · EQ and output gain have not been saved to the DAC.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Save EQ and output gain").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, saves)
    }

    @Test
    fun resetRequiresExplicitPersistentConfirmation() {
        var resets = 0
        composeRule.setContent {
            Column(Modifier.height(2000.dp)) {
                Ew300MyDacContent(
                    connectionState = Kt02h20ConnectionState.Connected,
                    hardwareEqState = state(),
                    editorState = MyDacEditorUiState(),
                    operationTrace = null,
                    catalogState = CatalogState.Loading,
                    managedHeadphones = emptyList(),
                    savedEqs = emptyList(),
                    onConnect = {},
                    onResetEq = {
                        resets += 1
                        "ok"
                    },
                    onRestoreBaseline = { "ok" },
                    onRunCapabilityBatch = { error("not used") },
                    onAdvancePersistenceQualification = { error("not used") },
                    onCaptureDacEq = { _, _ -> "ok" },
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
        }

        composeRule.onNodeWithText("Reset to flat").performScrollTo().performClick()
        composeRule.onNodeWithText("Reset and save the DAC EQ to flat? This sends one Save and may briefly disconnect the DAC.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Reset and save").performClick()
        composeRule.waitForIdle()
        assertEquals(1, resets)
    }

    @Test
    fun deviceTabShowsTruthfulAboutAndDoesNotClaimUnknownControls() {
        composeRule.setContent {
            Column(Modifier.height(2000.dp)) {
                Ew300MyDacContent(
                    connectionState = Kt02h20ConnectionState.Connected,
                    hardwareEqState = state(),
                    editorState = MyDacEditorUiState(),
                    operationTrace = null,
                    catalogState = CatalogState.Loading,
                    managedHeadphones = emptyList(),
                    savedEqs = emptyList(),
                    onConnect = {},
                    onResetEq = { "ok" },
                    onRestoreBaseline = { "ok" },
                    onRunCapabilityBatch = { error("not used") },
                    onAdvancePersistenceQualification = { error("not used") },
                    onCaptureDacEq = { _, _ -> "ok" },
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
        }

        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithText("About").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Model").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Firmware").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("31B2:0111 · revision 1.01 · HID interface 3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "App controls for presets, microphone, UAC switching, and tuning have not been established for this EW300.",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun bandRowsExposeAccessibleValues() {
        composeRule.setContent {
            Column(Modifier.height(2000.dp)) {
                Ew300MyDacContent(
                    connectionState = Kt02h20ConnectionState.Connected,
                    hardwareEqState = state(),
                    editorState = MyDacEditorUiState(),
                    operationTrace = null,
                    catalogState = CatalogState.Loading,
                    managedHeadphones = emptyList(),
                    savedEqs = emptyList(),
                    onConnect = {},
                    onResetEq = { "ok" },
                    onRestoreBaseline = { "ok" },
                    onRunCapabilityBatch = { error("not used") },
                    onAdvancePersistenceQualification = { error("not used") },
                    onCaptureDacEq = { _, _ -> "ok" },
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
        }

        composeRule.onNodeWithText("2   Peak   2,000 Hz   -2.0 dB   Q 1.400")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Band 2", substring = true)
            .assertExists()
    }
}

