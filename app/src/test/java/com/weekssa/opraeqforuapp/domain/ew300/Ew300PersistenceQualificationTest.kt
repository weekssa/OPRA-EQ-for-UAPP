package com.weekssa.opraeqforuapp.domain.ew300

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class Ew300PersistenceQualificationTest {
    @Test
    fun unauthorizedBuildSendsNoReadWriteOrCommit() = runBlocking {
        val transport = FakeTransport()
        val store = FakeStore()
        val result = Ew300PersistenceQualifier(transport, store).advance()

        assertTrue(result is Ew300PersistenceQualificationResult.Failed)
        assertTrue((result as Ew300PersistenceQualificationResult.Failed).stateKnown)
        assertEquals(0, transport.readCount)
        assertEquals(0, transport.writeCount)
        assertEquals(0, transport.commitCount)
        assertNull(store.pending)
    }

    @Test
    fun unqualifiedHardwareRevisionSendsNoReadWriteOrCommit() = runBlocking {
        val transport = FakeTransport()
        val store = FakeStore()
        val result = Ew300PersistenceQualifier(
            transport = transport,
            stateStore = store,
            authorizationGate = { true },
        ).advance()

        assertTrue(result is Ew300PersistenceQualificationResult.Failed)
        assertTrue((result as Ew300PersistenceQualificationResult.Failed).stateKnown)
        assertEquals(0, transport.readCount)
        assertEquals(0, transport.writeCount)
        assertEquals(0, transport.commitCount)
        assertNull(store.pending)
    }

    @Test
    fun shelfCodedFirstBandStopsBeforeAnyWriteOrSave() = runBlocking {
        val transport = FakeTransport(firstBandType = 3)
        val store = FakeStore()
        val result = qualifier(transport, store).advance()

        assertTrue(result is Ew300PersistenceQualificationResult.Failed)
        assertTrue((result as Ew300PersistenceQualificationResult.Failed).stateKnown)
        assertEquals(0, transport.writeCount)
        assertEquals(0, transport.commitCount)
        assertNull(store.pending)
    }

    @Test
    fun unequalStereoGainStopsQualificationBeforeAnyMutation() = runBlocking {
        val transport = FakeTransport(initialGlobalGain = byteArrayOf(0x96.toByte(), 0xF8.toByte(), 0, 0))
        val store = FakeStore()

        val result = qualifier(transport, store).advance()

        assertTrue(result is Ew300PersistenceQualificationResult.Failed)
        assertFalse((result as Ew300PersistenceQualificationResult.Failed).stateKnown)
        assertEquals(0, transport.writeCount)
        assertEquals(0, transport.commitCount)
        assertNull(store.pending)
    }

    @Test
    fun twoPowerCycleFlowQualifiesPersistenceAndRestoresExactBaseline() = runBlocking {
        val transport = FakeTransport()
        val store = FakeStore()
        val qualifier = qualifier(transport, store)

        assertTrue(qualifier.advance() is Ew300PersistenceQualificationResult.AwaitingPowerCycle)
        assertEquals(1, transport.commitCount)
        assertFalse(transport.state.deepEquals(transport.baseline))

        transport.detachGeneration += 1
        assertTrue(qualifier.advance() is Ew300PersistenceQualificationResult.AwaitingPowerCycle)
        assertEquals(2, transport.commitCount)
        assertTrue(transport.state.deepEquals(transport.baseline))

        transport.detachGeneration += 1
        assertTrue(qualifier.advance() is Ew300PersistenceQualificationResult.Verified)
        assertTrue(store.persistenceQualified)
        assertTrue(store.gainQualified)
        assertNull(store.pending)
        assertEquals(2, transport.commitCount)
    }

    @Test
    fun temporaryValuesLostAfterPowerCycleKeepPersistentFlashLocked() = runBlocking {
        val transport = FakeTransport()
        val store = FakeStore()
        val qualifier = qualifier(transport, store)

        assertTrue(qualifier.advance() is Ew300PersistenceQualificationResult.AwaitingPowerCycle)
        transport.state = transport.baseline.deepCopy()
        transport.detachGeneration += 1
        val result = qualifier.advance()

        assertTrue(result is Ew300PersistenceQualificationResult.NotPersistent)
        assertFalse(store.persistenceQualified)
        assertFalse(store.gainQualified)
        assertNull(store.pending)
        assertEquals(1, transport.commitCount)
    }

    @Test
    fun continueWithoutObservedPhysicalDetachSendsNoAdditionalOperation() = runBlocking {
        val transport = FakeTransport()
        val store = FakeStore()
        val qualifier = qualifier(transport, store)

        assertTrue(qualifier.advance() is Ew300PersistenceQualificationResult.AwaitingPowerCycle)
        val writesAfterBegin = transport.writeCount
        val result = qualifier.advance()

        assertTrue(result is Ew300PersistenceQualificationResult.AwaitingPowerCycle)
        assertEquals(1, transport.commitCount)
        assertEquals(writesAfterBegin, transport.writeCount)
    }

    @Test
    fun uncertainCommitStopsWithoutAutomaticRetryOrRestorationMutation() = runBlocking {
        val transport = FakeTransport(commitAccepted = false)
        val store = FakeStore()
        val qualifier = qualifier(transport, store)
        val result = qualifier.advance()

        assertTrue(result is Ew300PersistenceQualificationResult.Failed)
        assertFalse((result as Ew300PersistenceQualificationResult.Failed).stateKnown)
        assertEquals(1, transport.commitCount)
        assertTrue(store.pending != null)
        assertFalse(store.persistenceQualified)
        assertTrue(qualifier.advance() is Ew300PersistenceQualificationResult.Failed)
        assertEquals(1, transport.commitCount)
    }

    @Test
    fun rejectedFirstWriteWithExactBaselineReadbackStopsKnownWithoutCommit() = runBlocking {
        val transport = FakeTransport(failWriteAt = setOf(1))
        val store = FakeStore()
        val result = qualifier(transport, store).advance()

        assertTrue(result is Ew300PersistenceQualificationResult.Failed)
        assertTrue((result as Ew300PersistenceQualificationResult.Failed).stateKnown)
        assertNull(store.pending)
        assertEquals(0, transport.commitCount)
        assertEquals(1, transport.writeCount)
    }

    @Test
    fun ambiguousFirstWriteStopsTerminalWithoutAnyFollowUpMutation() = runBlocking {
        val transport = FakeTransport(failWriteAt = setOf(1), mutateOnFailedWrite = true)
        val store = FakeStore()
        val qualifier = qualifier(transport, store)
        val result = qualifier.advance()

        assertTrue(result is Ew300PersistenceQualificationResult.Failed)
        assertFalse((result as Ew300PersistenceQualificationResult.Failed).stateKnown)
        assertEquals(Ew300PersistenceStage.UNCERTAIN, store.pending?.stage)
        assertEquals(0, transport.commitCount)
        assertEquals(1, transport.writeCount)

        assertTrue(qualifier.advance() is Ew300PersistenceQualificationResult.Failed)
        assertEquals(1, transport.writeCount)
    }

    @Test
    fun failedRestorationWriteBecomesTerminalAndIsNeverRetried() = runBlocking {
        val transport = FakeTransport(failWriteAt = setOf(3))
        val store = FakeStore()
        val qualifier = qualifier(transport, store)

        assertTrue(qualifier.advance() is Ew300PersistenceQualificationResult.AwaitingPowerCycle)
        transport.detachGeneration += 1
        val result = qualifier.advance()

        assertTrue(result is Ew300PersistenceQualificationResult.Failed)
        assertEquals(Ew300PersistenceStage.UNCERTAIN, store.pending?.stage)
        val writesAtStop = transport.writeCount
        assertTrue(qualifier.advance() is Ew300PersistenceQualificationResult.Failed)
        assertEquals(writesAtStop, transport.writeCount)
    }

    private class FakeStore : Ew300GainStateStore {
        var gainQualified = false
        var persistenceQualified = false
        var pending: Ew300PersistencePending? = null
        var delta = 0

        override fun isGlobalGainQualified(deviceFingerprintKey: String) = gainQualified
        override fun markGlobalGainQualified(deviceFingerprintKey: String, qualified: Boolean) {
            gainQualified = qualified
        }
        override fun readAppliedGainDeltaSteps(deviceFingerprintKey: String) = delta
        override fun writeAppliedGainDeltaSteps(deviceFingerprintKey: String, steps: Int) {
            delta = steps
        }
        override fun isPersistenceQualified(deviceFingerprintKey: String) = persistenceQualified
        override fun markPersistenceQualified(deviceFingerprintKey: String, qualified: Boolean) {
            persistenceQualified = qualified
        }
        override fun readPersistencePending(deviceFingerprintKey: String) = pending
        override fun writePersistencePending(deviceFingerprintKey: String, pending: Ew300PersistencePending?) {
            this.pending = pending
        }
    }

    private fun qualifier(transport: Ew300Transport, store: Ew300GainStateStore) =
        Ew300PersistenceQualifier(
            transport = transport,
            stateStore = store,
            authorizationGate = { true },
            mutationAuthorized = { it == "exact-ew300-test" },
        )

    private class FakeTransport(
        private val commitAccepted: Boolean = true,
        private val failWriteAt: Set<Int> = emptySet(),
        private val mutateOnFailedWrite: Boolean = false,
        private val firstBandType: Int = 0,
        private val initialGlobalGain: ByteArray = byteArrayOf(0xF8.toByte(), 0xF8.toByte(), 0, 0),
    ) : Ew300Transport {
        override val deviceFingerprintKey = "exact-ew300-test"
        override var detachGeneration = 0L
        val baseline = Ew300CapabilityBatch.snapshotRegisters().associateWith { register ->
            when (register) {
                Ew300Protocol.FIRST_BAND_REGISTER -> byteArrayOf(0xF5.toByte(), 0xFF.toByte(), 0x64, 0)
                Ew300Protocol.FIRST_BAND_REGISTER + 1 -> byteArrayOf(0, 0, firstBandType.toByte(), 0)
                Ew300Protocol.GLOBAL_GAIN_REGISTER -> initialGlobalGain.copyOf()
                else -> byteArrayOf(0, 0, 0, 0)
            }
        }
        var state = baseline.mapValues { it.value.copyOf() }
        var commitCount = 0
        var writeCount = 0
        var readCount = 0

        override suspend fun readRegister(register: Int): ByteArray? {
            readCount += 1
            return state[register]?.copyOf()
        }
        override suspend fun writeRegister(register: Int, data: ByteArray): Boolean {
            writeCount += 1
            if (writeCount in failWriteAt) {
                if (mutateOnFailedWrite) {
                    state = state.toMutableMap().also { it[register] = data.copyOf() }
                }
                return false
            }
            state = state.toMutableMap().also { it[register] = data.copyOf() }
            return true
        }
        override suspend fun commit(): Boolean {
            commitCount += 1
            return commitAccepted
        }
    }

    private fun Map<Int, ByteArray>.deepCopy() = mapValues { it.value.copyOf() }
    private fun Map<Int, ByteArray>.deepEquals(other: Map<Int, ByteArray>): Boolean =
        keys == other.keys && keys.all { key -> getValue(key).contentEquals(other.getValue(key)) }
}
