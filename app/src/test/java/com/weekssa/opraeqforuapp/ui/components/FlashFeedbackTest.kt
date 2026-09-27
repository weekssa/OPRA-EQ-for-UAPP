package com.weekssa.opraeqforuapp.ui.components

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashFeedbackTest {
    @Test
    fun blackPearlTransferSuccessDoesNotClaimFinalReadback() {
        val feedback = blackPearlFlashFeedback(
            BlackPearlFlashResult.Success(
                fidelity = DevicePresetFidelity.EXACT,
                appliedPlaybackGainDb = 0.0,
                warning = "A device limit was applied.",
            ),
        )

        assertEquals(FlashFeedbackPhase.SENT, feedback.phase)
        assertFalse(feedback.verified)
        assertTrue(feedback.detail.orEmpty().contains("not read back"))
        assertTrue(feedback.detail.orEmpty().contains("A device limit was applied."))
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
