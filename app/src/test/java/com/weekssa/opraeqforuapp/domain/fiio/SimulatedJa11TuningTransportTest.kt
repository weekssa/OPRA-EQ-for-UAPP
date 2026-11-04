package com.weekssa.opraeqforuapp.domain.fiio

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedJa11TuningTransportTest {
    @Test
    fun transportIsAlwaysMarkedSimulated() {
        assertTrue(SimulatedJa11TuningTransport().isSimulated)
    }

    @Test
    fun unreadableInitialStateStaysUnknownInsteadOfOff() = runBlocking {
        val transport = SimulatedJa11TuningTransport(
            initialValues = null,
            initialPersistence = Ja11TuningPersistence.UNKNOWN,
        )

        val read = transport.read() as Ja11TuningReadResult.Success

        assertEquals(null, read.snapshot.values)
        assertEquals(Ja11TuningPersistence.UNKNOWN, read.snapshot.persistence)
    }

    @Test
    fun writeMarksSessionOnlyAndCountsExactlyOneAttempt() = runBlocking {
        val transport = SimulatedJa11TuningTransport()
        val values = Ja11TuningValues(Ja11TuningLevel.WARMER, noiseGateEnabled = true)

        val result = transport.write(values)

        assertTrue(result is Ja11TuningWriteResult.Accepted)
        assertEquals(1, transport.writeAttempts)
        assertEquals(1, transport.committedWrites)
        val read = transport.read() as Ja11TuningReadResult.Success
        assertEquals(values, read.snapshot.values)
        assertEquals(Ja11TuningPersistence.SESSION_ONLY, read.snapshot.persistence)
    }

    @Test
    fun startupSaveAndReconnectChangeGeneration() = runBlocking {
        val transport = SimulatedJa11TuningTransport()
        val values = Ja11TuningValues(Ja11TuningLevel.WARMEST, noiseGateEnabled = true)
        transport.write(values)
        val before = (transport.read() as Ja11TuningReadResult.Success).snapshot.sessionGeneration

        assertTrue(transport.saveStartupDefaults())
        transport.simulateReconnect()
        val after = (transport.read() as Ja11TuningReadResult.Success).snapshot

        assertNotEquals(before, after.sessionGeneration)
        assertEquals(values, after.values)
        assertEquals(Ja11TuningPersistence.SAVED, after.persistence)
    }

    @Test
    fun reconnectWithoutSaveReturnsToBaselineAndUnknownPersistence() = runBlocking {
        val transport = SimulatedJa11TuningTransport()
        transport.write(Ja11TuningValues(Ja11TuningLevel.WARMER, noiseGateEnabled = true))

        transport.simulateReconnect()
        val after = (transport.read() as Ja11TuningReadResult.Success).snapshot

        assertEquals(Ja11TuningValues(Ja11TuningLevel.STOCK, noiseGateEnabled = false), after.values)
        assertEquals(Ja11TuningPersistence.UNKNOWN, after.persistence)
    }

    @Test
    fun permissionFaultBlocksReadsAndWritesWithoutCommitting() = runBlocking {
        val transport = SimulatedJa11TuningTransport(fault = SimulatedJa11TuningFault.PERMISSION_DENIED)

        assertEquals(Ja11TuningReadResult.PermissionDenied, transport.read())
        assertEquals(
            Ja11TuningWriteResult.PermissionDenied,
            transport.write(Ja11TuningValues(Ja11TuningLevel.WARMER, true)),
        )
        assertEquals(0, transport.committedWrites)
    }

    @Test
    fun timeoutMakesTheIntermediateStateUnreadableOnce() = runBlocking {
        val transport = SimulatedJa11TuningTransport(fault = SimulatedJa11TuningFault.TIMEOUT)

        val write = transport.write(Ja11TuningValues(Ja11TuningLevel.WARMER, true))
        assertTrue(write is Ja11TuningWriteResult.TimedOut)
        assertTrue(transport.read() is Ja11TuningReadResult.Offline)
        assertTrue(transport.read() is Ja11TuningReadResult.Success)
    }

    @Test
    fun readbackMismatchCommitsAWrongValueSoVerificationCanRejectIt() = runBlocking {
        val transport = SimulatedJa11TuningTransport(fault = SimulatedJa11TuningFault.READBACK_MISMATCH)

        transport.write(Ja11TuningValues(Ja11TuningLevel.WARMER, noiseGateEnabled = true))
        val read = transport.read() as Ja11TuningReadResult.Success

        assertEquals(Ja11TuningLevel.STOCK, read.snapshot.values?.level)
        assertFalse(read.snapshot.values?.noiseGateEnabled == false)
    }
}
