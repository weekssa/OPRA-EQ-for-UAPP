package com.weekssa.opraeqforuapp

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationTrace
import com.weekssa.opraeqforuapp.ui.EqLibraryUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class Ja11FlashReportDiagnosticTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<Ja11FlashReportActivity>()

    @Test
    fun onlyCurrentCompletedFlashTraceIsExposed() {
        val flash = trace("flash-1", "FLASH")
        val edit = trace("edit-1", "EDITOR_APPLY")

        assertSame(
            flash,
            completedJa11FlashTrace(
                EqLibraryUiState(
                    fiioJa11OperationTrace = flash,
                    fiioJa11OperationStatus = FiioJa11OperationStatus.Completed(flash),
                ),
            ),
        )
        assertNull(
            completedJa11FlashTrace(
                EqLibraryUiState(
                    fiioJa11OperationTrace = flash,
                    fiioJa11OperationStatus = FiioJa11OperationStatus.Completed(edit),
                ),
            ),
        )
        assertNull(
            completedJa11FlashTrace(
                EqLibraryUiState(
                    fiioJa11OperationTrace = flash,
                    fiioJa11OperationStatus = FiioJa11OperationStatus.Running("flash-2", "FLASH"),
                ),
            ),
        )
        assertNull(
            completedJa11FlashTrace(
                EqLibraryUiState(
                    fiioJa11OperationTrace = edit,
                    fiioJa11OperationStatus = FiioJa11OperationStatus.Completed(edit),
                ),
            ),
        )
    }

    @Test
    fun completedFlashReportStaysVisibleUntilDismissedAndExposesBothExports() {
        val diagnosticApplication = targetDiagnosticApplication()
        val flash = trace("flash-2", "FLASH")
        diagnosticApplication.retainedFlashReport.value?.let {
            diagnosticApplication.dismissCompletedFlash(it.operationId)
        }
        diagnosticApplication.retainCompletedFlash(flash)

        composeRule.onNodeWithText("FiiO JA11 Flash report").assertIsDisplayed()
        composeRule.onNodeWithText("Operation ID: flash-2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Share operation report").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Share technical report (JSON)").performScrollTo().assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(60_000L)
        composeRule.onNodeWithText("Operation ID: flash-2").performScrollTo().assertIsDisplayed()
        assertSame(flash, diagnosticApplication.retainedFlashReport.value)

        composeRule.onNodeWithText("Dismiss report").performScrollTo().performClick()
        assertNull(diagnosticApplication.retainedFlashReport.value)
    }

    @Test
    fun readableAndJsonShareIntentsUseExistingTraceSerializers() {
        val flash = trace("flash-3", "FLASH")

        val readable = ja11FlashShareIntent(flash, Ja11FlashReportFormat.READABLE)
        assertEquals(Intent.ACTION_SEND, readable.action)
        assertEquals("text/plain", readable.type)
        assertEquals(flash.toReadableText(), readable.getStringExtra(Intent.EXTRA_TEXT))

        val json = ja11FlashShareIntent(flash, Ja11FlashReportFormat.TECHNICAL_JSON)
        assertEquals(Intent.ACTION_SEND, json.action)
        assertEquals("application/json", json.type)
        assertEquals(flash.toJson(), json.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun retainedFlashReportStaysUntilDismissedAndCannotBeReplacedEarly() {
        val diagnosticApplication = targetDiagnosticApplication()
        val firstFlash = trace("retained-flash-1", "FLASH")
        val laterFlash = trace("retained-flash-2", "FLASH")

        diagnosticApplication.retainCompletedFlash(firstFlash)
        diagnosticApplication.retainCompletedFlash(laterFlash)
        assertSame(firstFlash, diagnosticApplication.retainedFlashReport.value)

        diagnosticApplication.dismissCompletedFlash(firstFlash.operationId)
        assertNull(diagnosticApplication.retainedFlashReport.value)
        diagnosticApplication.retainCompletedFlash(firstFlash)
        assertNull(diagnosticApplication.retainedFlashReport.value)

        diagnosticApplication.retainCompletedFlash(laterFlash)
        assertSame(laterFlash, diagnosticApplication.retainedFlashReport.value)
        diagnosticApplication.dismissCompletedFlash(laterFlash.operationId)
        assertNull(diagnosticApplication.retainedFlashReport.value)
    }

    private fun trace(operationId: String, operation: String) = FiioJa11OperationTrace(
        operationId = operationId,
        operation = operation,
        sourceCommit = "candidate-sha",
        appVersion = "0.8.1-ja11diag",
        signerVerified = true,
        deviceFingerprintKey = null,
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
        targetBandCount = null,
        fidelity = null,
        usesGeneratedHeadroom = null,
        usedResponseFit = null,
        targetBands = emptyList(),
        saveCommandCount = 1L,
        stateKnown = true,
        outcome = "Success",
        stages = emptyList(),
        events = emptyList(),
        failureReason = null,
    )

    private fun targetDiagnosticApplication() = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as Ja11DiagnosticApplication
}
