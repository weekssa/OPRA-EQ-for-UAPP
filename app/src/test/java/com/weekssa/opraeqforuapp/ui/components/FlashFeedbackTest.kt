package com.weekssa.opraeqforuapp.ui.components

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashFeedbackTest {
    @Test
    fun onlyTerminalFlashResultsExpireAutomatically() {
        assertTrue(FlashFeedbackPhase.SENT.expiresAutomatically())
        assertTrue(FlashFeedbackPhase.COMPLETED.expiresAutomatically())
        assertFalse(FlashFeedbackPhase.STARTING.expiresAutomatically())
        assertFalse(FlashFeedbackPhase.VERIFYING.expiresAutomatically())
        assertFalse(FlashFeedbackPhase.UNCERTAIN.expiresAutomatically())
        assertFalse(FlashFeedbackPhase.FAILED.expiresAutomatically())
    }

    @Test
    fun blackPearlVerifiedSuccessUsesCompactVerifiedCopy() {
        val feedback = blackPearlFlashFeedback(
            BlackPearlFlashResult.Success(
                fidelity = DevicePresetFidelity.EXACT,
                appliedPlaybackGainDb = 0.0,
                warning = "A device limit was applied.",
            ),
        )

        assertEquals(FlashFeedbackPhase.COMPLETED, feedback.phase)
        assertTrue(feedback.verified)
        assertTrue(feedback.detail.orEmpty().contains("Flash successful"))
        assertTrue(feedback.detail.orEmpty().contains("saved and verified"))
        assertTrue(feedback.detail.orEmpty().contains("Final hardware readback matched"))
        assertTrue(feedback.detail.orEmpty().contains("A device limit was applied."))
    }

    @Test
    fun blackPearlVerificationFailureNeverLooksSuccessful() {
        val feedback = blackPearlFlashFeedback(
            BlackPearlFlashResult.VerificationFailed("band 4 gain expected -512 actual -511"),
        )

        assertEquals(FlashFeedbackPhase.UNCERTAIN, feedback.phase)
        assertFalse(feedback.verified)
        assertTrue(feedback.detail.orEmpty().contains("was not verified"))
        assertTrue(feedback.detail.orEmpty().contains("band 4 gain"))
        assertFalse(feedback.detail.orEmpty().contains("Flash successful"))
    }

    @Test
    fun blackPearlTransferFailureNeverLooksComplete() {
        val feedback = blackPearlFlashFeedback(
            BlackPearlFlashResult.TransferFailed("The transfer stopped."),
        )

        assertEquals(FlashFeedbackPhase.FAILED, feedback.phase)
        assertFalse(feedback.verified)
        assertEquals("The transfer stopped.", feedback.detail)
    }
}
