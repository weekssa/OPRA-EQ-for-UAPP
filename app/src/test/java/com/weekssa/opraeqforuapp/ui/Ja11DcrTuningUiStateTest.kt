package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.domain.kt02h20.Ja11DcrTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ja11DcrTuningUiStateTest {
    @Test
    fun stockAndEllynAreDistinguished() {
        assertEquals(Ja11DcrTuningPhase.STOCK, Ja11DcrTuningUiState().read(Ja11DcrTuning.STOCK).phase)
        assertEquals(Ja11DcrTuningPhase.ELLYN, Ja11DcrTuningUiState().read(Ja11DcrTuning.ELLYN).phase)
        assertTrue(Ja11DcrTuningUiState().read(Ja11DcrTuning.ELLYN).tuningEnabled)
        assertEquals("On", ja11DcrToggleValue(Ja11DcrTuningUiState().read(Ja11DcrTuning.ELLYN)))
        assertEquals("Off", ja11DcrToggleValue(Ja11DcrTuningUiState().read(Ja11DcrTuning.STOCK)))
        assertFalse(Ja11DcrTuningUiState().read(Ja11DcrTuning.STOCK).tuningEnabled)
    }

    @Test
    fun stockRowIsNotReportedAsEllyn() {
        val state = Ja11DcrTuningUiState().read(Ja11DcrTuning.STOCK)
        assertEquals("Stock · off", ja11DcrRowValue(state))
        assertFalse(state.tuningEnabled)
    }

    @Test
    fun usbIdentityRegisterCannotBeWritten() {
        assertFalse(Ja11DcrTuning.canWriteRegister(Ja11DcrTuning.USB_IDENTITY_REGISTER))
        assertTrue(Ja11DcrTuning.canWriteRegister(0x3A))
        assertTrue(Ja11DcrTuning.canWriteRegister(0x3B))
    }

    @Test
    fun usbIdentityRegisterIsNeverATuningParameter() {
        assertFalse(Ja11DcrTuning.parameters.any { it.register == Ja11DcrTuning.USB_IDENTITY_REGISTER })
    }

    @Test
    fun unknownAndNullAreNotRenderedAsEllyn() {
        assertEquals(Ja11DcrTuningPhase.UNKNOWN, Ja11DcrTuningUiState().read(null).phase)
        assertEquals("Tap Refresh to read", ja11DcrRowValue(Ja11DcrTuningUiState().read(null)))
    }

    @Test
    fun failedWriteDoesNotClaimSuccess() {
        val state = Ja11DcrTuningUiState().beginApply().failed("no ack")
        assertEquals(Ja11DcrTuningPhase.ERROR, state.phase)
        assertFalse(state.tuningEnabled)
    }
}
