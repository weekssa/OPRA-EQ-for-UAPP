package com.weekssa.opraeqforuapp.domain.fiio

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ja11TuningControllerTest {
    private val requested = Ja11TuningValues(Ja11TuningLevel.WARMER, noiseGateEnabled = true)

    private suspend fun generationOf(controller: Ja11TuningController): Long {
        val read = controller.read()
        assertTrue("expected a readable simulated state", read is Ja11TuningReadOutcome.Read)
        return (read as Ja11TuningReadOutcome.Read).snapshot.sessionGeneration
    }

    @Test
    fun applySendsOneWriteAndReportsVerifiedReadback() = runBlocking {
        val transport = SimulatedJa11TuningTransport()
        val controller = Ja11TuningController(transport)
        val generation = generationOf(controller)

        val outcome = controller.apply(Ja11TuningApplyRequest(requested, generation))

        assertTrue(outcome is Ja11TuningApplyOutcome.Verified)
        val verified = outcome as Ja11TuningApplyOutcome.Verified
        assertEquals(requested, verified.snapshot.values)
        assertEquals(1, transport.writeAttempts)
        assertEquals(1, transport.committedWrites)
        assertEquals(Ja11TuningPersistence.SESSION_ONLY, verified.snapshot.persistence)
    }

    @Test
    fun staleGenerationSendsNoWriteAtAll() = runBlocking {
        val transport = SimulatedJa11TuningTransport()
        val controller = Ja11TuningController(transport)
        val generation = generationOf(controller)

        val outcome = controller.apply(Ja11TuningApplyRequest(requested, generation + 5))

        assertTrue(outcome is Ja11TuningApplyOutcome.StaleSession)
        assertEquals(0, transport.writeAttempts)
        assertEquals(0, transport.committedWrites)
    }

    @Test
    fun permissionDeniedOnBaselineReadBlocksTheWrite() = runBlocking {
        val transport = SimulatedJa11TuningTransport(fault = SimulatedJa11TuningFault.PERMISSION_DENIED)
        val controller = Ja11TuningController(transport)

        val outcome = controller.apply(Ja11TuningApplyRequest(requested, 1L))

        assertEquals(Ja11TuningApplyOutcome.PermissionDenied, outcome)
        assertEquals(0, transport.writeAttempts)
    }

    @Test
    fun readbackMismatchIsNeverReportedAsSuccess() = runBlocking {
        val transport = SimulatedJa11TuningTransport(fault = SimulatedJa11TuningFault.READBACK_MISMATCH)
        val controller = Ja11TuningController(transport)
        val generation = generationOf(controller)

        val outcome = controller.apply(Ja11TuningApplyRequest(requested, generation))

        assertTrue(outcome is Ja11TuningApplyOutcome.VerificationFailed)
        assertEquals(1, transport.writeAttempts)
        assertEquals(1, transport.committedWrites)
    }

    @Test
    fun detachBeforeWriteIsCertainAndNotRetried() = runBlocking {
        val transport = SimulatedJa11TuningTransport(
            fault = SimulatedJa11TuningFault.DISCONNECT_BEFORE_WRITE,
        )
        val controller = Ja11TuningController(transport)
        val generation = generationOf(controller)

        val outcome = controller.apply(Ja11TuningApplyRequest(requested, generation))

        assertTrue(outcome is Ja11TuningApplyOutcome.Disconnected)
        assertFalse((outcome as Ja11TuningApplyOutcome.Disconnected).uncertain)
        assertEquals(1, transport.writeAttempts)
        assertEquals(0, transport.committedWrites)
    }

    @Test
    fun detachDuringWriteIsReportedUncertainWithNoReplay() = runBlocking {
        val transport = SimulatedJa11TuningTransport(
            fault = SimulatedJa11TuningFault.DISCONNECT_DURING_WRITE,
        )
        val controller = Ja11TuningController(transport)
        val generation = generationOf(controller)

        val outcome = controller.apply(Ja11TuningApplyRequest(requested, generation))

        assertTrue(outcome is Ja11TuningApplyOutcome.Disconnected)
        assertTrue((outcome as Ja11TuningApplyOutcome.Disconnected).uncertain)
        assertEquals(1, transport.writeAttempts)
    }

    @Test
    fun readbackTimeoutAfterWriteFailsClosedWithoutAnotherWrite() = runBlocking {
        val transport = SimulatedJa11TuningTransport(
            fault = SimulatedJa11TuningFault.DISCONNECT_AFTER_WRITE_BEFORE_READBACK,
        )
        val controller = Ja11TuningController(transport)
        val generation = generationOf(controller)

        val outcome = controller.apply(Ja11TuningApplyRequest(requested, generation))

        assertTrue(outcome is Ja11TuningApplyOutcome.VerificationFailed)
        assertEquals(1, transport.writeAttempts)
        assertEquals(1, transport.committedWrites)
    }

    @Test
    fun hardwareTransportWithoutEstablishedCapabilityRefusesEveryWrite() = runBlocking {
        val hardware = object : Ja11TuningTransport {
            var writes = 0
            override val isSimulated: Boolean = false

            override suspend fun read(): Ja11TuningReadResult =
                Ja11TuningReadResult.Success(
                    Ja11TuningSnapshot(
                        sessionGeneration = 1L,
                        values = Ja11TuningValues(Ja11TuningLevel.STOCK, noiseGateEnabled = false),
                        persistence = Ja11TuningPersistence.UNKNOWN,
                    ),
                )

            override suspend fun write(values: Ja11TuningValues): Ja11TuningWriteResult {
                writes += 1
                return Ja11TuningWriteResult.Accepted(1L)
            }
        }
        val controller = Ja11TuningController(hardware)

        assertEquals(Ja11TuningApplyOutcome.Unsupported, controller.apply(Ja11TuningApplyRequest(requested, 1L)))
        assertEquals(Ja11TuningReadOutcome.Unsupported, controller.read())
        assertEquals(0, hardware.writes)
    }

    @Test
    fun simulatedStartupSaveSurvivesReconnectAndUnsavedValuesDoNot() = runBlocking {
        val transport = SimulatedJa11TuningTransport()
        val controller = Ja11TuningController(transport)
        var generation = generationOf(controller)

        controller.apply(Ja11TuningApplyRequest(requested, generation))
        val saved = controller.saveStartupDefaults()
        assertTrue(saved is Ja11TuningSaveOutcome.Saved)
        assertEquals(Ja11TuningPersistence.SAVED, (saved as Ja11TuningSaveOutcome.Saved).snapshot.persistence)

        // Unsaved change first, then a simulated unplug: the saved startup value must win.
        val unsaved = Ja11TuningValues(Ja11TuningLevel.WARMEST, noiseGateEnabled = false)
        generation = (controller.read() as Ja11TuningReadOutcome.Read).snapshot.sessionGeneration
        controller.apply(Ja11TuningApplyRequest(unsaved, generation))

        val afterReconnect = controller.simulateReconnect() as Ja11TuningReadOutcome.Read
        assertEquals(requested, afterReconnect.snapshot.values)
        assertEquals(Ja11TuningPersistence.SAVED, afterReconnect.snapshot.persistence)
    }

    @Test
    fun sessionOnlyValueIsLostOnSimulatedUnplug() = runBlocking {
        val transport = SimulatedJa11TuningTransport()
        val controller = Ja11TuningController(transport)
        val generation = generationOf(controller)
        controller.apply(Ja11TuningApplyRequest(requested, generation))

        val afterReconnect = controller.simulateReconnect() as Ja11TuningReadOutcome.Read

        assertEquals(Ja11TuningLevel.STOCK, afterReconnect.snapshot.values?.level)
        assertEquals(Ja11TuningPersistence.UNKNOWN, afterReconnect.snapshot.persistence)
        assertTrue(afterReconnect.snapshot.sessionGeneration > generation)
    }

    @Test
    fun competingAppliesAreSerializedByTheExclusiveGate() = runBlocking {
        val transport = OverlapDetectingTransport()
        val controller = Ja11TuningController(transport)

        coroutineScope {
            launch {
                controller.apply(
                    Ja11TuningApplyRequest(Ja11TuningValues(Ja11TuningLevel.WARMER, true), 1L),
                )
            }
            launch {
                controller.apply(
                    Ja11TuningApplyRequest(Ja11TuningValues(Ja11TuningLevel.WARMEST, false), 1L),
                )
            }
        }

        assertFalse("tuning operations must not interleave", transport.overlap)
        assertEquals(2, transport.writes)
    }

    private class OverlapDetectingTransport : Ja11TuningTransport {
        override val isSimulated: Boolean = true
        private var inFlight = false
        private var generation = 1L
        private var values = Ja11TuningValues(Ja11TuningLevel.STOCK, noiseGateEnabled = false)
        var overlap = false
        var writes = 0

        override suspend fun read(): Ja11TuningReadResult {
            enter()
            yield()
            leave()
            return Ja11TuningReadResult.Success(
                Ja11TuningSnapshot(generation, values, Ja11TuningPersistence.UNKNOWN),
            )
        }

        override suspend fun write(values: Ja11TuningValues): Ja11TuningWriteResult {
            enter()
            yield()
            writes += 1
            this.values = values
            leave()
            return Ja11TuningWriteResult.Accepted(generation)
        }

        private fun enter() {
            if (inFlight) overlap = true
            inFlight = true
        }

        private fun leave() {
            inFlight = false
        }
    }
}
