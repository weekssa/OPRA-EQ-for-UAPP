package com.weekssa.opraeqforuapp.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.ui.EqLibraryApp
import com.weekssa.opraeqforuapp.ui.EqLibraryUiState
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Captures the shipped context bar states for the design record. */
class ContextBarScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun state(
        present: Boolean,
        connection: Kt02h20ConnectionState,
        directFlash: Boolean,
    ) = EqLibraryUiState(
        catalogState = CatalogState.Loading,
        appPreferences = AppPreferences(
            exportTargets = ExportTargetPreferences().withActiveTarget(ExportDevice.FIIO_JA11),
            directFiioJa11FlashEnabled = directFlash,
        ),
        dacRecognitionState = DacRecognitionState(
            presentDeviceIds = if (present) setOf(DacDeviceId.FIIO_JA11) else emptySet(),
            recognizedDeviceIds = setOf(DacDeviceId.FIIO_JA11),
        ),
        fiioJa11ConnectionState = connection,
        fiioJa11HardwareEqState = contextBarFixtureHardwareState(),
    )

    private fun render(state: EqLibraryUiState, narrow: Boolean = false) {
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                if (narrow) {
                    Box(Modifier.width(360.dp).fillMaxHeight().testTag("bar-shot")) {
                        EqLibraryApp(state = state, actions = contextBarFixtureActions())
                    }
                } else {
                    EqLibraryApp(state = state, actions = contextBarFixtureActions())
                }
            }
        }
    }

    private fun capture(name: String, narrow: Boolean = false) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.getExternalFilesDir(null), "ctx-bar").apply { mkdirs() }
        val bitmap = if (narrow) {
            composeRule.onNodeWithTag("bar-shot").captureToImage().asAndroidBitmap()
        } else {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun captureConnected() {
        render(state(present = true, connection = Kt02h20ConnectionState.Connected, directFlash = true))
        composeRule.waitForIdle()
        capture("context-bar-connected.png")
    }

    @Test
    fun captureNotConnected() {
        render(state(present = true, connection = Kt02h20ConnectionState.Disconnected, directFlash = true))
        composeRule.waitForIdle()
        capture("context-bar-not-connected.png")
    }

    @Test
    fun captureDirectFlashOff() {
        render(state(present = true, connection = Kt02h20ConnectionState.Disconnected, directFlash = false))
        composeRule.waitForIdle()
        capture("context-bar-direct-flash-off.png")
    }

    @Test
    fun captureNoDacAttached() {
        render(state(present = false, connection = Kt02h20ConnectionState.Disconnected, directFlash = true))
        composeRule.waitForIdle()
        capture("context-bar-no-dac.png")
    }

    @Test
    fun captureEqLibrary() {
        render(state(present = true, connection = Kt02h20ConnectionState.Connected, directFlash = true))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("EQ Library").performClick()
        composeRule.waitForIdle()
        capture("context-bar-eq-library.png")
    }

    @Test
    fun captureNarrowLargeText() {
        render(
            state(present = true, connection = Kt02h20ConnectionState.Disconnected, directFlash = true),
            narrow = true,
        )
        composeRule.waitForIdle()
        capture("context-bar-360dp-font200.png", narrow = true)
    }
}

