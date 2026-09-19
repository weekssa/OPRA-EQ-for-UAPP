package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityCaseResult
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityReport
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
                )
            }
        }

        composeRule.onNodeWithText("It never writes, saves, resets, or retries a mutation.", substring = true)
            .assertIsDisplayed()
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
}
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
