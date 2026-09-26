package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStage
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationTrace
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11TransportEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiioJa11OperationPresentationTest {
    @Test
    fun resetControlIsDisabledOnlyDuringAnActiveJa11Operation() {
        assertFalse(
            fiioJa11OperationControlsEnabled(
                FiioJa11OperationStatus.Running("operation-1", "FLASH"),
            ),
        )
        assertTrue(fiioJa11OperationControlsEnabled(FiioJa11OperationStatus.Idle))
    }

    @Test
    fun successfulFlashReportsFinalReadbackWithoutInventingReconnectPersistence() {
        val presentation = fiioJa11OperationStatusPresentation(trace())

        assertTrue(presentation.verified)
        assertTrue(presentation.message.contains("saved and verified"))
        assertTrue(presentation.message.contains("Final hardware readback matched"))
        assertTrue(presentation.message.contains("Reconnect persistence was not tested"))
    }

    @Test
    fun observedSessionChangeIsIncludedOnlyWhenTraceContainsOne() {
        val presentation = fiioJa11OperationStatusPresentation(
            trace(
                events = listOf(
                    FiioJa11TransportEvent(
                        sequence = 1,
                        elapsedMillis = 10L,
                        direction = "READ",
                        command = "0x17",
                        requestHex = "request",
                        responseHex = "response",
                        sessionGeneration = 2L,
                        detachGeneration = 1L,
                        succeeded = true,
                    ),
                ),
            ),
        )

        assertTrue(presentation.verified)
        assertTrue(presentation.message.contains("session change was observed"))
    }

    @Test
    fun failedOperationRemainsFailClosedAndPreservesReportRecovery() {
        val presentation = fiioJa11OperationStatusPresentation(
            trace(
                outcome = "VerificationFailed",
                stateKnown = true,
                stages = listOf(FiioJa11OperationStage.FINAL_READBACK, FiioJa11OperationStage.FAILED),
                failureReason = "final readback mismatch",
            ),
        )

        assertFalse(presentation.verified)
        assertTrue(presentation.message.contains("was not verified"))
        assertTrue(presentation.message.contains("Do not retry"))
        assertTrue(presentation.message.contains("stop and reconnect or refresh"))
        assertFalse(presentation.message.contains("share the operation report"))
        assertTrue(presentation.message.contains("final readback mismatch"))
    }

    @Test
    fun failureBeforeSaveDoesNotSuggestThatPersistenceWasAttempted() {
        val presentation = fiioJa11OperationStatusPresentation(
            trace(
                outcome = "VerificationFailed",
                stateKnown = true,
                stages = listOf(FiioJa11OperationStage.VOLATILE_READBACK, FiioJa11OperationStage.FAILED),
                comparisonPhase = "VOLATILE_READBACK",
                saveCommandCount = 0L,
                failureReason = "volatile readback mismatch",
            ),
        )

        assertFalse(presentation.verified)
        assertTrue(presentation.message.contains("before persistent Save"))
        assertFalse(presentation.message.contains("Save was sent"))
    }

    @Test
    fun reportControlsPreserveReadableAndTechnicalEvidence() {
        assertTrue(FIIO_JA11_READABLE_REPORT_LABEL.contains("report"))
        assertTrue(FIIO_JA11_TECHNICAL_REPORT_LABEL.contains("JSON"))
        assertTrue(fiioJa11OperationReportDescription(trace()).contains("technical report"))
    }

    private fun trace(
        outcome: String = "Success",
        stateKnown: Boolean = true,
        stages: List<FiioJa11OperationStage> = listOf(
            FiioJa11OperationStage.FINAL_READBACK,
            FiioJa11OperationStage.VERIFIED,
        ),
        failureReason: String? = null,
        events: List<FiioJa11TransportEvent> = emptyList(),
        comparisonPhase: String = "FINAL_READBACK",
        saveCommandCount: Long = 1L,
    ) = FiioJa11OperationTrace(
        operationId = "operation-1",
        operation = "FLASH",
        sourceCommit = "candidate",
        appVersion = "0.7.0",
        signerVerified = true,
        deviceFingerprintKey = "vid=2972|pid=0102|interface=3",
        usbProductId = 0x0102,
        sessionGeneration = 1L,
        detachGeneration = 0L,
        permissionRequestCount = 0L,
        sourceProfileId = "Jaytiss",
        canonicalPreampGainDb = -3.9,
        generatedOrSelectedTargetGainDb = -3.9,
        quantizedWireTargetGainDb = -3.9,
        readbackGlobalGainDb = -3.9,
        globalGainToleranceDb = 0.001,
        comparisonPhase = comparisonPhase,
        sourceBandCount = 9,
        targetBandCount = 5,
        fidelity = "OPTIMIZED",
        usesGeneratedHeadroom = false,
        usedResponseFit = true,
        targetBands = emptyList(),
        saveCommandCount = saveCommandCount,
        stateKnown = stateKnown,
        outcome = outcome,
        stages = stages,
        events = events,
        failureReason = failureReason,
    )
}
