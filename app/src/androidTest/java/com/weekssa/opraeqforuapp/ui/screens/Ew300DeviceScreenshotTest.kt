package com.weekssa.opraeqforuapp.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.ew300.Ew300UacStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.ui.Ew300UacUiState
import java.io.File
import org.junit.Rule
import org.junit.Test

/**
 * Captures the v0.11 DEVICE page for the design-language record.
 *
 * The page is short, so a 360x640 viewport shows it without scrolling. The bottom capture uses a
 * 360x360 viewport so it actually shows the scrolled end of the page rather than repeating the top.
 */
class Ew300DeviceScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun render(viewportHeight: Int) {
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
        composeRule.setContent {
            Box(
                Modifier
                    .size(width = 360.dp, height = viewportHeight.dp)
                    .background(Color.White)
                    .testTag("device-page"),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Ew300DeviceStatus(
                        report = null,
                        hardwareEqState = HardwareEqSnapshotState(
                            bundle = bundle,
                            freshness = DacStateFreshness.CURRENT,
                        ),
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
        }
    }

    private fun dir(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File(context.getExternalFilesDir(null), "v011-screens").apply { mkdirs() }
    }

    private fun capture(name: String) {
        val bitmap = composeRule.onNodeWithTag("device-page").captureToImage().asAndroidBitmap()
        File(dir(), name).outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    @Test
    fun captureTopAboutAndLimitations() {
        render(viewportHeight = 640)
        composeRule.waitForIdle()
        capture("device-top.png")
        composeRule.onNodeWithText("About this DAC").performClick()
        composeRule.waitForIdle()
        capture("about-expanded.png")
        val limitations = composeRule.onNodeWithText("Device limitations")
            .captureToImage()
            .asAndroidBitmap()
        File(dir(), "limitations.png").outputStream().use { stream ->
            limitations.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    @Test
    fun captureScrolledBottom() {
        render(viewportHeight = 360)
        composeRule.onNodeWithText("About this DAC").performScrollTo().performClick()
        composeRule.onNodeWithText("Device limitations").performScrollTo()
        composeRule.waitForIdle()
        capture("device-bottom.png")
    }
}

