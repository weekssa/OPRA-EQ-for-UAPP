package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.ew300.Ew300RuntimePersistenceState
import com.weekssa.opraeqforuapp.domain.ew300.Ew300UacStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.ui.Ew300UacUiState
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The v0.11 product device surface: JA11 layout, proven EW300 controls only, no microphone
 * section while the mute request has no measured effect on captured audio.
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
        return HardwareEqSnapshotState(bundle = bundle, freshness = DacStateFreshness.CURRENT)
    }

    internal fun renderDevice(
        runtimePersistence: Ew300RuntimePersistenceState = Ew300RuntimePersistenceState.UNKNOWN,
        onSaveRuntime: () -> Unit = {},
    ) {
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
                    runtimePersistence = runtimePersistence,
                    onSaveRuntime = onSaveRuntime,
                    running = false,
                    onRun = {},
                    onShareReadable = {},
                    onShareJson = {},
                )
            }
        }
    }

    @Test
    fun deviceSurfaceUsesAudioUsbAndAboutInThatOrder() {
        renderDevice()

        val audio = composeRule.onNodeWithText("Audio").fetchSemanticsNode().boundsInRoot.top
        val usb = composeRule.onNodeWithText("USB & System").fetchSemanticsNode().boundsInRoot.top
        val about = composeRule.onNodeWithText("About").fetchSemanticsNode().boundsInRoot.top

        assertTrue("Audio must precede USB & System", audio < usb)
        assertTrue("USB & System must precede About", usb < about)
        composeRule.onNodeWithText("Microphone").assertDoesNotExist()
    }

    @Test
    fun deviceSurfaceKeepsProvenRowsAndDropsTheRest() {
        renderDevice()

        composeRule.onNodeWithText("Device volume").performScrollTo().assertIsDisplayed()
        // Startup settings is hidden while the saved state is unknown.
        composeRule.onNodeWithText("Startup settings").assertDoesNotExist()
        composeRule.onNodeWithText("Current sample rate").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("96,000 Hz").assertIsDisplayed()
        // The USB audio class is fixed for this device, so it lives in About.
        composeRule.onNodeWithText("USB audio class").assertDoesNotExist()

        composeRule.onNodeWithText("Output gain").assertDoesNotExist()
        composeRule.onNodeWithText("Equalizer").assertDoesNotExist()
        composeRule.onNodeWithText("Connection").assertDoesNotExist()
        composeRule.onNodeWithText("Headset remote").assertDoesNotExist()
        composeRule.onNodeWithText("Read USB audio").assertDoesNotExist()
        composeRule.onNodeWithText("Experimental features").assertDoesNotExist()
        composeRule.onNodeWithText("Microphone input").assertDoesNotExist()
        composeRule.onNodeWithText("Microphone mute").assertDoesNotExist()
        composeRule.onNodeWithText("Native shelf engineering controls").assertDoesNotExist()
        composeRule.onNodeWithText("Set Low Shelf").assertDoesNotExist()
    }

    @Test
    fun startupSettingsOffersSaveWhileTheStateIsNotSaved() {
        var saves = 0
        renderDevice(
            runtimePersistence = Ew300RuntimePersistenceState.RUNTIME_ONLY,
            onSaveRuntime = { saves += 1 },
        )

        composeRule.onNodeWithText("Startup settings").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Not saved").assertIsDisplayed()
        composeRule.onNodeWithText("Save").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun aboutListsIdentityAndTheOneLineLimitations() {
        renderDevice()

        composeRule.onNodeWithText("About this DAC").performScrollTo().performClick()
        composeRule.onNodeWithText("SIMGOT EW300 DSP").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("31B2:0111").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("USB audio class").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("UAC 1.0").assertIsDisplayed()
        composeRule.onNodeWithText("Device limitations").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "Headset-remote disable and UAC mode switching are chip-limited; native shelf acoustics are unmeasured; the device accepts a microphone mute request, but no effect on captured audio has been measured.",
        ).assertIsDisplayed()
    }
}

