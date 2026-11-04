package com.weekssa.opraeqforuapp.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningTransport
import com.weekssa.opraeqforuapp.domain.fiio.SimulatedJa11TuningFault
import com.weekssa.opraeqforuapp.domain.fiio.SimulatedJa11TuningTransport
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiState
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class Ja11TuningPrototypeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tuningRowIsLabelledSimulatedAndOpensThePrototypeDialog() {
        setContent(SimulatedJa11TuningTransport())
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithText("Tuning").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Prototype · simulated. No command is sent to JA11.")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag(JA11_TUNING_ROW_TAG).performScrollTo().performClick()

        composeRule.onNodeWithText("JA11 tuning (simulated)").assertIsDisplayed()
        composeRule.onNodeWithText("sends nothing to the DAC", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Apply (simulated)").assertIsDisplayed()
    }

    @Test
    fun applyReportsVerifiedReadbackAndUpdatesTheRow() {
        setContent(SimulatedJa11TuningTransport())
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithTag(JA11_TUNING_ROW_TAG).performScrollTo().performClick()
        composeRule.onNodeWithText("Warmer", substring = true).performClick()
        composeRule.onNodeWithText("Apply (simulated)").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Simulated apply verified by readback.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Simulated apply verified by readback.").assertIsDisplayed()
        composeRule.onNodeWithText("session-only", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithContentDescription("Tuning, prototype, simulated. Warmer · gate off")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun startupSaveAndSimulatedUnplugAreDistinguishedFromEqSave() {
        setContent(SimulatedJa11TuningTransport())
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithTag(JA11_TUNING_ROW_TAG).performScrollTo().performClick()
        composeRule.onNodeWithText("Warmest", substring = true).performClick()
        composeRule.onNodeWithText("Apply (simulated)").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Simulated apply verified by readback.").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(JA11_TUNING_SAVE_TAG).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("startup save verified", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(JA11_TUNING_UNPLUG_TAG).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Tuning, prototype, simulated. Warmest · gate off")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Simulated apply verified by readback.").fetchSemanticsNodes()
    }

    @Test
    fun permissionDeniedShowsNoValuesAndNoSuccess() {
        setContent(SimulatedJa11TuningTransport(fault = SimulatedJa11TuningFault.PERMISSION_DENIED))
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithTag(JA11_TUNING_ROW_TAG).performScrollTo().performClick()

        composeRule.onNodeWithText("Android USB permission is required", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("Simulated apply verified by readback.").fetchSemanticsNodes().let {
            assertEquals(0, it.size)
        }
        composeRule.onNodeWithTag(JA11_TUNING_APPLY_TAG).assertIsNotEnabled()
    }

    @Test
    fun largeFontScaleKeepsRowAndDialogReadable() {
        setContent(
            transport = SimulatedJa11TuningTransport(),
            fontScale = 1.5f,
        )
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithTag(JA11_TUNING_ROW_TAG).performScrollTo().performClick()

        composeRule.onNodeWithText("JA11 tuning (simulated)").assertIsDisplayed()
        composeRule.onNodeWithText("Noise gate (simulated)").assertIsDisplayed()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getExternalFilesDir("ja11-tuning-screenshots")?.let { dir ->
            save(captureDialog(), File(dir, "dialog-large-text.png"))
        }
    }

    @Test
    fun capturePrototypeScreenshotsForOwnerReview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = context.getExternalFilesDir("ja11-tuning-screenshots") ?: return

        setContent(SimulatedJa11TuningTransport())
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithTag(JA11_TUNING_ROW_TAG).performScrollTo()
        composeRule.waitForIdle()
        save(captureRoot(), File(dir, "device-light.png"))

        composeRule.onNodeWithTag(JA11_TUNING_ROW_TAG).performClick()
        composeRule.onNodeWithText("Warmer", substring = true).performClick()
        save(captureDialog(), File(dir, "dialog-light.png"))
        composeRule.onNodeWithText("Apply (simulated)").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Simulated apply verified by readback.").fetchSemanticsNodes().isNotEmpty()
        }
        save(captureDialog(), File(dir, "dialog-verified-light.png"))
    }

    @Test
    fun captureDarkThemeScreenshotForOwnerReview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = context.getExternalFilesDir("ja11-tuning-screenshots") ?: return

        setContent(SimulatedJa11TuningTransport(), dark = true)
        composeRule.onNodeWithText("DEVICE").performClick()
        composeRule.onNodeWithTag(JA11_TUNING_ROW_TAG).performScrollTo().performClick()
        save(captureDialog(), File(dir, "dialog-dark.png"))
    }

    private fun captureRoot(): ImageBitmap {
        composeRule.waitForIdle()
        return composeRule.onRoot().captureToImage()
    }

    private fun captureDialog(): ImageBitmap {
        composeRule.waitForIdle()
        return composeRule.onNodeWithTag(JA11_TUNING_DIALOG_TAG).captureToImage()
    }

    private fun save(image: ImageBitmap, file: File) {
        file.outputStream().use { out ->
            require(image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)) {
                "could not write " + file.name
            }
        }
    }

    private fun setContent(
        transport: Ja11TuningTransport,
        fontScale: Float = 1f,
        dark: Boolean = false,
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
            CompositionLocalProvider(
                LocalDensity provides Density(density = 2.75f, fontScale = fontScale),
            ) {
                OpraEqTheme(themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light) {
                    FiioJa11MyDacContent(
                    connectionState = Kt02h20ConnectionState.Connected,
                    hardwareEqState = HardwareEqSnapshotState(
                        bundle = bundle,
                        freshness = DacStateFreshness.CURRENT,
                    ),
                    editorState = MyDacEditorUiState(),
                    deviceState = FiioJa11DeviceUiState(
                        snapshot = deviceSnapshot(),
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
                    onMessage = {},
                        ja11TuningTransportFactory = { transport },
                    )
                }
            }
        }
        composeRule.setContent { content() }
    }

    private fun deviceSnapshot() = FiioJa11DeviceSnapshot(
        sessionGeneration = 1L,
        usbProductId = FiioJa11Protocol.PRODUCT_ID_UAC_2,
        firmwareVersion = "2.20",
        sampleRateLabel = "96 kHz",
        outputVolume = 20,
        headsetControlEnabled = false,
        eqProgram = FiioJa11Protocol.EqProgram.USER_1,
        digitalFilterMode = FiioJa11Protocol.DigitalFilterMode.FAST_LL,
        uacMode = FiioJa11Protocol.UacMode.UAC_2,
    )
}
