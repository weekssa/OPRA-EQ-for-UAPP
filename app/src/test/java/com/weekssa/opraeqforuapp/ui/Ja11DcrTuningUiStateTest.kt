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
        assertFalse(Ja11DcrTuningUiState().read(Ja11DcrTuning.STOCK).tuningEnabled)
    }

    @Test
    fun capturedDeviceBaselineIsReportedAsStockNotEllyn() {
        val state = Ja11DcrTuningUiState(baseline = 0).read(0)
        assertEquals("Stock · off (device value)", ja11DcrRowValue(state))
        assertFalse(state.tuningEnabled)
    }

    @Test
    fun unknownAndNullAreNotRenderedAsEllyn() {
        assertEquals(Ja11DcrTuningPhase.UNKNOWN, Ja11DcrTuningUiState().read(null).phase)
        assertEquals("Unknown", ja11DcrRowValue(Ja11DcrTuningUiState().read(null)))
    }

    @Test
    fun failedWriteDoesNotClaimSuccess() {
        val state = Ja11DcrTuningUiState().beginApply().failed("no ack")
        assertEquals(Ja11DcrTuningPhase.ERROR, state.phase)
        assertFalse(state.tuningEnabled)
    }
}
