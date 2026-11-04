package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningApplyOutcome
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningLevel
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningPersistence
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningSnapshot
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Ja11TuningUiStateTest {
    private val snapshot = Ja11TuningSnapshot(
        sessionGeneration = 3L,
        values = Ja11TuningValues(Ja11TuningLevel.WARMER, noiseGateEnabled = true),
        persistence = Ja11TuningPersistence.SESSION_ONLY,
    )

    @Test
    fun unknownStateIsNeverRenderedAsOffOrStock() {
        val state = Ja11TuningUiState()

        assertNull(state.level)
        assertNull(state.noiseGateEnabled)
        assertEquals("Not readable · simulated", ja11TuningRowValue(state))
    }

    @Test
    fun staleValuesArePrefixedAsLastRead() {
        val state = Ja11TuningUiState().read(snapshot).markStale()

        assertFalse(state.isCurrent)
        assertTrue(ja11TuningRowValue(state).startsWith("Last read · "))
        assertEquals(Ja11TuningPersistence.UNKNOWN, state.persistence)
    }

    @Test
    fun verifiedApplyBecomesCurrentWithGeneration() {
        val state = Ja11TuningUiState().beginApply(snapshot.values!!).applyOutcome(
            Ja11TuningApplyOutcome.Verified(snapshot),
        )

        assertEquals(Ja11TuningPhase.VERIFIED, state.phase)
        assertTrue(state.isCurrent)
        assertEquals(3L, state.sessionGeneration)
        assertEquals("Warmer · gate on", ja11TuningRowValue(state))
    }

    @Test
    fun verificationFailureKeepsTheErrorAndDropsCurrentness() {
        val state = Ja11TuningUiState(isCurrent = true, sessionGeneration = 3L).applyOutcome(
            Ja11TuningApplyOutcome.VerificationFailed("Readback did not match.", null),
        )

        assertEquals(Ja11TuningPhase.FAILED, state.phase)
        assertFalse(state.isCurrent)
        assertEquals("Readback did not match.", state.error)
        assertNull(state.sessionGeneration.takeIf { state.isCurrent })
    }

    @Test
    fun uncertainDisconnectSaysCompletionIsUnknown() {
        val state = Ja11TuningUiState().beginApply(snapshot.values!!).applyOutcome(
            Ja11TuningApplyOutcome.Disconnected("Cable removed.", uncertain = true),
        )

        assertEquals(Ja11TuningPhase.DISCONNECTED, state.phase)
        assertEquals(Ja11TuningPersistence.UNKNOWN, state.persistence)
        assertTrue(state.error!!.contains("Completion is unknown"))
        assertTrue(state.error!!.contains("Read the current values"))
    }

    @Test
    fun unsupportedStateHidesAnyValuesAndSaysNothingWasWritten() {
        val state = Ja11TuningUiState().read(snapshot).applyOutcome(Ja11TuningApplyOutcome.Unsupported)

        assertEquals(Ja11TuningPhase.UNSUPPORTED, state.phase)
        assertFalse(state.isCurrent)
    }

    @Test
    fun persistenceWordingDistinguishesSessionOnlySavedAndUnknown() {
        val sessionOnly = Ja11TuningUiState().read(snapshot)
        val saved = Ja11TuningUiState().read(snapshot.copy(persistence = Ja11TuningPersistence.SAVED))
        val unknown = Ja11TuningUiState()

        assertTrue(ja11TuningPersistenceText(sessionOnly).contains("session-only"))
        assertTrue(ja11TuningPersistenceText(saved).contains("startup save verified"))
        assertTrue(ja11TuningPersistenceText(unknown).contains("unknown"))
    }

    @Test
    fun currentStateRequiresAVerifiedGeneration() {
        val failure = runCatching {
            Ja11TuningUiState(isCurrent = true, sessionGeneration = null)
        }

        assertTrue(failure.isFailure)
    }
}
