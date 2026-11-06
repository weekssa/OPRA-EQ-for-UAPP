package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.ew300.Ew300UacStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.ui.Ew300UacUiState
import org.junit.Rule
import org.junit.Test

/**
 * The v0.11 prototype device surface: proven EW300 controls only, no engineering panel.
 */
class Ew300ProductSurfaceTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun flatState(): HardwareEqSnapshotState {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = List(Ew300Protocol.BAND_COUNT) {
                    Kt02h20Band("peak_dip", 1_000.0, 0.0, 1.0)
                },
                globalGainDb = 0.0,
                sessionGeneration = 7,
                verifiedAtEpochMillis = 1,
            ),
        )
        return HardwareEqSnapshotState(
            bundle = bundle,
            freshness = com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness.CURRENT,
        )
    }

    @Test
    fun deviceSurfaceShowsProvenControlsAndNoEngineeringPanel() {
        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Ew300DeviceStatus(
                    report = null,
                    hardwareEqState = flatState(),
                    uacState = Ew300UacUiState(
                        status = Ew300UacStatus(
                            uacVersion = "UAC 1.0",
                            currentSampleRateHz = 96_000,
                            sampleRatePath = "UAC1 GET_CUR",
                            microphoneMuteSupported = true,
                            microphoneMuted = false,
                            headsetFeaturePresent = false,
                        ),
                    ),
                    running = false,
                    onRun = {},
                    onShareReadable = {},
                    onShareJson = {},
                )
            }
        }

        composeRule.onNodeWithText("Device volume (0–60)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Microphone mute").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Current sample rate").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Native shelf engineering controls").assertDoesNotExist()
        composeRule.onNodeWithText("Set Low Shelf").assertDoesNotExist()
    }
}

