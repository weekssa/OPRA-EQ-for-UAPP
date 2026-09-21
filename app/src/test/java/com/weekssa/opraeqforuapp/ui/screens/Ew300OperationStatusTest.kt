package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationStage
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationTrace
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300OperationStatusTest {
    @Test
    fun verifiedReplacementReadbackIsShownAsSuccessful() {
        val presentation = ew300OperationStatusPresentation(trace(outcome = "Verified"))

        assertTrue(presentation.verified)
        assertTrue(presentation.message.contains("reconnected"))
        assertTrue(presentation.message.contains("final hardware readback matched"))
    }

    @Test
    fun unverifiedReplacementIsNotPresentedAsSuccess() {
        val presentation = ew300OperationStatusPresentation(
            trace(outcome = "VerificationFailed", finalReadbackMatched = false),
        )

        assertFalse(presentation.verified)
        assertTrue(presentation.message.contains("not verified"))
        assertFalse(presentation.message.contains("final hardware readback matched"))
    }

    private fun trace(
        outcome: String,
        finalReadbackMatched: Boolean = true,
    ) = Ew300OperationTrace(
        operationId = "operation-1",
        operation = "FLASH",
        sourceCommit = "candidate",
        appVersion = "0.7.0",
        signerVerified = true,
        deviceFingerprintKey = "vid=31b2|pid=111|interface=3",
        sessionGeneration = 2L,
        detachGeneration = 1L,
        permissionRequestCount = 1L,
        permissionRequestsBeforeFirstWrite = 0L,
        registerWriteCount = 10L,
        saveCommandCount = 1L,
        mutationReplayCount = null,
        competingConnectionJobCount = null,
        replacementObserved = true,
        replacementIdentityMatched = true,
        baselineCaptured = true,
        volatileReadbackMatched = true,
        finalReadbackMatched = finalReadbackMatched,
        restorationVerified = false,
        stateKnown = outcome == "Verified",
        outcome = outcome,
        stages = listOf(Ew300OperationStage.FINAL_READBACK),
    )
}
