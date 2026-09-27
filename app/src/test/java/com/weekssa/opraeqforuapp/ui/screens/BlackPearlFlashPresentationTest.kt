package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlFlashPresentationTest {
    @Test
    fun verifiedFlashUsesCompactSavedAndVerifiedReadbackWording() {
        val presentation = blackPearlFlashPresentation(
            BlackPearlFlashResult.Success(
                fidelity = DevicePresetFidelity.EXACT,
                appliedPlaybackGainDb = -3.0,
                warning = null,
            ),
        )

        assertTrue(presentation.verified)
        assertTrue(presentation.message.contains("Flash successful"))
        assertTrue(presentation.message.contains("saved and verified"))
        assertTrue(presentation.message.contains("Final hardware readback matched"))
        assertTrue(presentation.message.length < 220)
    }

    @Test
    fun finalVerificationFailureNeverUsesSuccessWording() {
        val presentation = blackPearlFlashPresentation(
            BlackPearlFlashResult.VerificationFailed("band 4 gain expected -512 actual -511"),
        )

        assertFalse(presentation.verified)
        assertTrue(presentation.message.contains("was not verified"))
        assertTrue(presentation.message.contains("Final hardware readback did not confirm"))
        assertTrue(presentation.message.contains("band 4 gain"))
        assertFalse(presentation.message.contains("Flash successful"))
        assertFalse(presentation.message.contains("saved and verified"))
    }

    @Test
    fun transferFailurePreservesUncertainRecoveryGuidance() {
        val presentation = blackPearlFlashPresentation(
            BlackPearlFlashResult.TransferFailed("the transfer stopped after the latch command"),
        )

        assertFalse(presentation.verified)
        assertTrue(presentation.message.contains("transfer did not complete"))
        assertTrue(presentation.message.contains("Stop and reconnect or refresh before any later write"))
        assertTrue(presentation.message.contains("transfer stopped"))
        assertFalse(presentation.message.contains("Flash successful"))
    }
}
