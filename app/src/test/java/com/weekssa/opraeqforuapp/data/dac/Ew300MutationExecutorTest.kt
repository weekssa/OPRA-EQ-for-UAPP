package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Flasher
import com.weekssa.opraeqforuapp.domain.ew300.Ew300GainStateStore
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationStage
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Transport
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300MutationExecutorTest {
    @Test
    fun cancellingUiCallerDuringDetachDoesNotCancelOwnedFlashBeforeFinalVerification() = runBlocking {
        val ownerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val reconnectGate = Ew300ReconnectGate()
        val executor = Ew300MutationExecutor(ownerScope, Mutex(), reconnectGate)
        val transport = FakeTransport(
            reconnectGate = reconnectGate,
            detachOnCommit = true,
            pauseAfterDetach = true,
            replacementFingerprint = QUALIFIED_TEST_FINGERPRINT,
            replacementGeneration = 2L,
        )
        val flasher = Ew300Flasher(
            transport = transport,
            gainStateStore = QualifiedGainStore(),
            mutationAuthorized = { true },
        )

        try {
            val uiCaller = launch {
                executor.execute { flasher.flash(profile(preamp = -4.0)) }
            }

            withTimeout(2_000L) { transport.detachObserved.await() }
            assertTrue(reconnectGate.canAutomaticReconnect())

            // Simulate the Compose caller being disposed while the replacement USB session is
            // pending. The session-owned transaction must continue independently.
            uiCaller.cancelAndJoin()
            assertTrue(uiCaller.isCancelled)

            transport.allowCommitToReturn.complete(Unit)
            val trace = withTimeout(2_000L) {
                flasher.lastOperationTrace.first { it?.stateKnown == true }
            }!!

            assertEquals(11L, trace.registerWriteCount)
            assertEquals(1L, trace.saveCommandCount)
            assertEquals(0L, trace.permissionRequestsBeforeFirstWrite)
            assertTrue(trace.replacementObserved)
            assertTrue(trace.replacementIdentityMatched)
            assertTrue(trace.finalReadbackMatched)
            assertTrue(trace.stateKnown)
            assertTrue(Ew300OperationStage.FINAL_READBACK in trace.stages)
            assertTrue(Ew300OperationStage.VERIFIED in trace.stages)
            assertNull(trace.mutationReplayCount)
            assertNull(trace.competingConnectionJobCount)

            withTimeout(2_000L) {
                while (reconnectGate.snapshot().mutationDepth != 0) delay(10L)
            }
        } finally {
            ownerScope.cancel()
        }
    }

    @Test
    fun cancellingUiCallerDuringDetachDoesNotCancelOwnedRestoration() = runBlocking {
        val ownerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val reconnectGate = Ew300ReconnectGate()
        val executor = Ew300MutationExecutor(ownerScope, Mutex(), reconnectGate)
        val transport = FakeTransport(
            reconnectGate = reconnectGate,
            detachOnCommit = true,
            pauseAtCommit = 2,
            replacementFingerprint = QUALIFIED_TEST_FINGERPRINT,
            replacementGeneration = 2L,
        )
        val flasher = Ew300Flasher(
            transport = transport,
            gainStateStore = QualifiedGainStore(),
            mutationAuthorized = { true },
        )

        try {
            val flash = executor.execute { flasher.flash(profile(preamp = -4.0)) }
            assertTrue(flash is Kt02h20FlashResult.Success)

            val uiCaller = launch {
                executor.execute { flasher.restoreLastFlashBaseline() }
            }

            withTimeout(2_000L) { transport.secondDetachObserved.await() }
            assertTrue(reconnectGate.canAutomaticReconnect())

            uiCaller.cancelAndJoin()
            assertTrue(uiCaller.isCancelled)

            transport.allowCommitToReturn.complete(Unit)
            val trace = withTimeout(2_000L) {
                flasher.lastOperationTrace.first { it?.let { trace -> trace.operation == "RESTORE" && trace.stateKnown } == true }
            }!!

            assertEquals(11L, trace.registerWriteCount)
            assertEquals(1L, trace.saveCommandCount)
            assertTrue(trace.replacementObserved)
            assertTrue(trace.replacementIdentityMatched)
            assertTrue(trace.finalReadbackMatched)
            assertTrue(trace.restorationVerified)
            assertTrue(trace.stateKnown)
            assertNull(trace.mutationReplayCount)
            assertNull(trace.competingConnectionJobCount)
        } finally {
            ownerScope.cancel()
        }
    }

    @Test
    fun detachedReplacementMustAdvanceSessionGenerationBeforeFinalReadback() = runBlocking {
        val transport = FakeTransport(
            detachOnCommit = true,
            replacementFingerprint = QUALIFIED_TEST_FINGERPRINT,
            replacementGeneration = 1L,
        )
        val flasher = Ew300Flasher(
            transport = transport,
            gainStateStore = QualifiedGainStore(),
            mutationAuthorized = { true },
        )

        val result = flasher.flash(profile(preamp = null))
        val trace = requireNotNull(flasher.lastOperationTrace.value)

        assertTrue(result is Kt02h20FlashResult.VerificationFailed)
        assertEquals(1, transport.saveCount)
        assertEquals(0, transport.readsAfterCommit)
        assertTrue(trace.replacementObserved)
        assertFalse(trace.replacementIdentityMatched)
        assertFalse(trace.finalReadbackMatched)
        assertFalse(trace.stateKnown)
    }

    @Test
    fun detachedReplacementMustMatchFingerprintBeforeFinalReadback() = runBlocking {
        val transport = FakeTransport(
            detachOnCommit = true,
            replacementFingerprint = "other-ew300",
            replacementGeneration = 2L,
        )
        val flasher = Ew300Flasher(
            transport = transport,
            gainStateStore = QualifiedGainStore(),
            mutationAuthorized = { true },
        )

        val result = flasher.flash(profile(preamp = null))
        val trace = requireNotNull(flasher.lastOperationTrace.value)

        assertTrue(result is Kt02h20FlashResult.VerificationFailed)
        assertEquals(1, transport.saveCount)
        assertEquals(0, transport.readsAfterCommit)
        assertTrue(trace.replacementObserved)
        assertFalse(trace.replacementIdentityMatched)
        assertFalse(trace.finalReadbackMatched)
        assertFalse(trace.stateKnown)
    }

    @Test
    fun failedFinalReadbackIsReportedAsUncertainNotMatched() = runBlocking {
        val transport = FakeTransport(
            detachOnCommit = false,
            corruptFinalRegister = Ew300Protocol.bandRegister(0),
        )
        val flasher = Ew300Flasher(
            transport = transport,
            gainStateStore = QualifiedGainStore(),
            mutationAuthorized = { true },
        )

        val result = flasher.flash(profile(preamp = null))
        val trace = requireNotNull(flasher.lastOperationTrace.value)

        assertTrue(result is Kt02h20FlashResult.VerificationFailed)
        assertEquals(1, transport.saveCount)
        assertTrue(Ew300OperationStage.FINAL_READBACK in trace.stages)
        assertTrue(Ew300OperationStage.STATE_UNCERTAIN in trace.stages)
        assertFalse(trace.finalReadbackMatched)
        assertFalse(trace.stateKnown)
    }

    private fun profile(preamp: Double?): OpraEqProfile = OpraEqProfile(
        id = "ew300-lifecycle-test",
        productId = "product",
        author = "Test",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = preamp,
        bands = listOf(
            OpraBand("peak_dip", 1_000.0, 0.0, 1.0, null),
            OpraBand("peak_dip", 2_000.0, 0.0, 1.0, null),
        ),
    )

    private class FakeTransport(
        private val reconnectGate: Ew300ReconnectGate? = null,
        private val detachOnCommit: Boolean,
        private val pauseAfterDetach: Boolean = false,
        private val pauseAtCommit: Int? = null,
        private val replacementFingerprint: String = QUALIFIED_TEST_FINGERPRINT,
        private val replacementGeneration: Long = 2L,
        private val corruptFinalRegister: Int? = null,
    ) : Ew300Transport {
        override var deviceFingerprintKey: String = QUALIFIED_TEST_FINGERPRINT
        override var sessionGeneration: Long = 1L
        override var detachGeneration: Long = 0L
        override val permissionRequestCount: Long = 0L
        override val registerWriteCount: Long
            get() = writeCount.toLong()
        override val saveCommandCount: Long
            get() = saveCount.toLong()

        val detachObserved = CompletableDeferred<Unit>()
        val secondDetachObserved = CompletableDeferred<Unit>()
        val allowCommitToReturn = CompletableDeferred<Unit>()
        var writeCount = 0
        var saveCount = 0
        var readsAfterCommit = 0

        private val state = (0 until Ew300Protocol.BAND_COUNT)
            .flatMap { index ->
                val register = Ew300Protocol.bandRegister(index)
                listOf(register, register + 1)
            }
            .associateWith { bytes(0, 0, 0, 0) }
            .toMutableMap()
            .also {
                it[Ew300Protocol.PROTOCOL_FLAGS_REGISTER] = bytes(0, 0, 0, 0)
                it[0x24] = bytes(0, 0, 0, 0)
                it[Ew300Protocol.GLOBAL_GAIN_REGISTER] = bytes(0, 0, 0, 0)
            }

        override suspend fun readRegister(register: Int): ByteArray? {
            if (saveCount > 0) {
                readsAfterCommit += 1
                if (register == corruptFinalRegister) return bytes(0x7F, 0, 0, 0)
            }
            return state[register]?.copyOf()
        }

        override suspend fun writeRegister(register: Int, data: ByteArray): Boolean {
            writeCount += 1
            state[register] = data.copyOf()
            return true
        }

        override suspend fun commit(): Boolean {
            saveCount += 1
            reconnectGate?.markSaveSent()
            if (detachOnCommit) {
                detachGeneration += 1L
                sessionGeneration = if (saveCount == 1) replacementGeneration else sessionGeneration + 1L
                deviceFingerprintKey = replacementFingerprint
                if (saveCount == 1) detachObserved.complete(Unit) else secondDetachObserved.complete(Unit)
                if (pauseAtCommit == saveCount || (pauseAfterDetach && saveCount == 1)) {
                    allowCommitToReturn.await()
                }
            }
            return true
        }
    }

    private class QualifiedGainStore : Ew300GainStateStore {
        private var delta = 0
        override fun isGlobalGainQualified(deviceFingerprintKey: String): Boolean = true
        override fun markGlobalGainQualified(deviceFingerprintKey: String, qualified: Boolean) = Unit
        override fun readAppliedGainDeltaSteps(deviceFingerprintKey: String): Int = delta
        override fun writeAppliedGainDeltaSteps(deviceFingerprintKey: String, steps: Int) {
            delta = steps
        }
    }

    companion object {
        private const val QUALIFIED_TEST_FINGERPRINT =
            "vid=31b2|pid=111|manufacturer=LE XIAN|product=SIMGOT EW300 DSP|serial=2024-07-03-0000-0000-0000|interface=3"

        private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
            (values[index] and 0xFF).toByte()
        }
    }
}
