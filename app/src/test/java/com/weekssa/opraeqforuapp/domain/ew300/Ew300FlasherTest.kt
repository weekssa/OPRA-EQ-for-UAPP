package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300FlasherTest {
    @Test
    fun persistentFlashIsBlockedForAnUnrecognizedDeviceIdentity() = runBlocking {
        val transport = FakeTransport()
        val result = Ew300Flasher(transport, QualifiedGainStore()).flash(profile(preamp = null))

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.NotSuitable)
        assertEquals(0, transport.writes.size)
        assertEquals(0, transport.commitCount)
    }

    @Test
    fun incompleteStrictBaselinePreventsEveryWrite() = runBlocking {
        val transport = FakeTransport(missingRegister = 0x24)
        val result = Ew300Flasher(transport, QualifiedGainStore(), mutationAuthorized = { true })
            .flash(profile(preamp = null))

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.DeviceUnavailable)
        assertTrue(transport.writes.isEmpty())
        assertEquals(0, transport.commitCount)
    }

    @Test
    fun flashWritesAllFiveBandsCommitsAndVerifiesReadback() = runBlocking {
        val transport = FakeTransport()
        val store = QualifiedGainStore()
        val result = Ew300Flasher(transport, store, mutationAuthorized = { true }).flash(profile(preamp = null))

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.Success)
        assertEquals(10, transport.writes.size)
        assertEquals(1, transport.commitCount)
        assertEquals(0, store.delta)
        assertEquals(21, transport.readsAfterWrites)
    }

    @Test
    fun sourcePreampIsAppliedThroughGlobalGainRegister() = runBlocking {
        val transport = FakeTransport()
        val result = Ew300Flasher(transport, QualifiedGainStore(), mutationAuthorized = { true }).flash(profile(preamp = -4.0))

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.Success)
        assertEquals(-8, Ew300Protocol.globalGainSteps(transport.state.getValue(Ew300Protocol.GLOBAL_GAIN_REGISTER)))
        assertEquals(11, transport.writes.size)
        assertEquals(1, transport.commitCount)
    }

    @Test
    fun resetWritesFlatStateAndVerifiesIt() = runBlocking {
        val transport = FakeTransport()
        val result = Ew300Flasher(transport, QualifiedGainStore(), mutationAuthorized = { true }).resetToFlat()

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult.Success)
        assertEquals(10, transport.writes.size)
        assertEquals(1, transport.commitCount)
        assertTrue(transport.state.values.any { it.contentEquals(bytes(0, 0, 0xE8, 0x03)) })
    }

    @Test
    fun commitFailureStopsBeforeReadbackVerification() = runBlocking {
        val transport = FakeTransport(commitSucceeds = false)
        val store = QualifiedGainStore()

        val result = Ew300Flasher(transport, store, mutationAuthorized = { true }).flash(profile(preamp = null))

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.TransferFailed)
        assertEquals(10, transport.readsAfterWrites)
        assertEquals(0, store.delta)
    }

    @Test
    fun flashRejectsAnUnverifiedReplacementSessionBeforeFinalReadback() = runBlocking {
        val transport = FakeTransport(replacementFingerprint = "other-ew300")

        val result = Ew300Flasher(
            transport,
            QualifiedGainStore(),
            mutationAuthorized = { true },
        ).flash(profile(preamp = null))

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.VerificationFailed)
        assertEquals(1, transport.commitCount)
        assertEquals(0, transport.readsAfterCommit)
    }

    @Test
    fun partialBandFailureRestoresTheCapturedVolatileStateBeforeStopping() = runBlocking {
        val transport = FakeTransport(failWriteRegister = Ew300Protocol.bandRegister(0) + 1)
        val result = Ew300Flasher(
            transport,
            QualifiedGainStore(),
            mutationAuthorized = { true },
        ).flash(profile(preamp = null))

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.TransferFailed)
        assertTrue(transport.state.values.all { it.contentEquals(bytes(0, 0, 0, 0)) })
        assertEquals(0, transport.commitCount)
    }

    @Test
    fun verifiedFlashBaselineCanBeRestoredExactlyAfterAReplacementSession() = runBlocking {
        val transport = FakeTransport(detachOnCommitNumber = 2)
        val original = transport.state.mapValues { it.value.copyOf() }
        val flasher = Ew300Flasher(transport, QualifiedGainStore(), mutationAuthorized = { true })

        val flash = flasher.flash(profile(preamp = -4.0))
        assertTrue(flash is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.Success)
        assertEquals(1, transport.commitCount)

        val restoration = flasher.restoreLastFlashBaseline()
        assertTrue(restoration is Ew300RestorationResult.Verified)
        assertEquals(2, transport.commitCount)
        assertEquals(21, transport.writes.size)
        original.forEach { (register, value) ->
            assertTrue(transport.state.getValue(register).contentEquals(value))
        }

        val trace = requireNotNull(flasher.lastOperationTrace.value)
        assertEquals("RESTORE", trace.operation)
        assertEquals(11L, trace.registerWriteCount)
        assertEquals(1L, trace.saveCommandCount)
        assertEquals(0L, trace.permissionRequestsBeforeFirstWrite)
        assertTrue(trace.replacementObserved)
        assertTrue(trace.replacementIdentityMatched)
        assertTrue(trace.finalReadbackMatched)
        assertTrue(trace.restorationVerified)
        assertTrue(trace.stateKnown)
        assertNull(trace.failureReason)
        assertEquals(transport.deviceFingerprintKey, trace.baselineFingerprintKey)
        assertEquals(1L, trace.baselineSessionGeneration)
    }

    @Test
    fun restorationWithoutAVerifiedFlashBaselineSendsNoWrites() = runBlocking {
        val transport = FakeTransport()
        val flasher = Ew300Flasher(transport, QualifiedGainStore(), mutationAuthorized = { true })
        val result = flasher.restoreLastFlashBaseline()

        assertTrue(result is Ew300RestorationResult.NoBaseline)
        assertTrue(transport.writes.isEmpty())
        assertEquals(0, transport.commitCount)
        val trace = requireNotNull(flasher.lastOperationTrace.value)
        assertFalse(trace.stateKnown)
        assertFalse(trace.restorationVerified)
    }

    @Test
    fun restorationRejectsWrongReplacementFingerprintWithoutFinalReadback() = runBlocking {
        val transport = FakeTransport(detachOnCommitNumber = 2)
        val flasher = Ew300Flasher(transport, QualifiedGainStore(), mutationAuthorized = { true })
        assertTrue(flasher.flash(profile(preamp = null)) is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.Success)
        transport.deviceFingerprintKey = "other-ew300"

        val result = flasher.restoreLastFlashBaseline()
        val trace = requireNotNull(flasher.lastOperationTrace.value)

        assertTrue(result is Ew300RestorationResult.NotSuitable)
        assertEquals(0, transport.commitCount - 1)
        assertFalse(trace.finalReadbackMatched)
        assertFalse(trace.restorationVerified)
        assertFalse(trace.stateKnown)
    }

    @Test
    fun restorationRejectsStaleReplacementGeneration() = runBlocking {
        val transport = FakeTransport(detachOnCommitNumber = 2, replacementGeneration = 1L)
        val flasher = Ew300Flasher(transport, QualifiedGainStore(), mutationAuthorized = { true })
        assertTrue(flasher.flash(profile(preamp = null)) is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.Success)
        transport.sessionGeneration = 2L

        val result = flasher.restoreLastFlashBaseline()
        val trace = requireNotNull(flasher.lastOperationTrace.value)

        assertTrue(result is Ew300RestorationResult.VerificationFailed)
        assertEquals(2, transport.commitCount)
        assertFalse(trace.finalReadbackMatched)
        assertFalse(trace.stateKnown)
    }

    @Test
    fun restorationFinalReadbackMismatchIsDurableUncertainty() = runBlocking {
        val transport = FakeTransport(
            detachOnCommitNumber = 2,
            corruptOnCommitNumber = 2,
            corruptRegister = Ew300Protocol.bandRegister(0),
        )
        val flasher = Ew300Flasher(transport, QualifiedGainStore(), mutationAuthorized = { true })
        assertTrue(flasher.flash(profile(preamp = null)) is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.Success)

        val result = flasher.restoreLastFlashBaseline()
        val trace = requireNotNull(flasher.lastOperationTrace.value)

        assertTrue(result is Ew300RestorationResult.VerificationFailed)
        assertTrue(Ew300OperationStage.FINAL_READBACK in trace.stages)
        assertFalse(trace.finalReadbackMatched)
        assertFalse(trace.restorationVerified)
        assertFalse(trace.stateKnown)
    }

    @Test
    fun exceptionAfterRestorationSaveRemainsDurableUncertainty() = runBlocking {
        val transport = FakeTransport(throwOnCommitNumber = 2)
        val flasher = Ew300Flasher(transport, QualifiedGainStore(), mutationAuthorized = { true })
        assertTrue(flasher.flash(profile(preamp = null)) is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.Success)

        try {
            flasher.restoreLastFlashBaseline()
            assertTrue("restoration should throw after the Save boundary", false)
        } catch (_: IllegalStateException) {
            // The durable operation trace is the result of the uncertain transaction.
        }
        val trace = requireNotNull(flasher.lastOperationTrace.value)
        assertEquals("EXCEPTION:IllegalStateException", trace.outcome)
        assertFalse(trace.stateKnown)
        assertFalse(trace.restorationVerified)
        assertTrue(trace.failureReason?.contains("after Save") == true)
    }

    private fun profile(preamp: Double?): OpraEqProfile = OpraEqProfile(
        id = "ew300-test",
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

    private inner class FakeTransport(
        private val commitSucceeds: Boolean = true,
        private val failWriteRegister: Int? = null,
        private val missingRegister: Int? = null,
        private val replacementFingerprint: String? = null,
        private val detachOnCommitNumber: Int? = null,
        private val replacementGeneration: Long = 2L,
        private val corruptOnCommitNumber: Int? = null,
        private val corruptRegister: Int? = null,
        private val throwOnCommitNumber: Int? = null,
    ) : Ew300Transport {
        override var deviceFingerprintKey: String = "test-ew300"
        override var sessionGeneration: Long = 1L
        override var detachGeneration: Long = 0L
        override val registerWriteCount: Long
            get() = writes.size.toLong()
        override val saveCommandCount: Long
            get() = commitCount.toLong()
        val state = (0 until Ew300Protocol.BAND_COUNT)
            .flatMap { index ->
                val register = Ew300Protocol.bandRegister(index)
                listOf(register, register + 1)
            }
            .associateWith { bytes(0, 0, 0, 0) }
            .toMutableMap()
            .also {
                it[0x24] = bytes(0, 0, 0, 0)
                it[Ew300Protocol.GLOBAL_GAIN_REGISTER] = bytes(0, 0, 0, 0)
            }
        val writes = mutableListOf<Int>()
        var commitCount = 0
        var readsAfterWrites = 0
        var readsAfterCommit = 0
        override suspend fun readRegister(register: Int): ByteArray? {
            if (writes.isNotEmpty()) readsAfterWrites++
            if (commitCount > 0) readsAfterCommit++
            if (register == missingRegister) return null
            if (commitCount >= (corruptOnCommitNumber ?: Int.MAX_VALUE) && register == corruptRegister) {
                return bytes(0x7F, 0, 0, 0)
            }
            return state[register]
        }

        override suspend fun writeRegister(register: Int, data: ByteArray): Boolean {
            writes += register
            if (register == failWriteRegister) return false
            state[register] = data.copyOf()
            return true
        }

        override suspend fun commit(): Boolean {
            commitCount++
            if (detachOnCommitNumber == commitCount || (detachOnCommitNumber == null && replacementFingerprint != null)) {
                deviceFingerprintKey = replacementFingerprint ?: deviceFingerprintKey
                sessionGeneration = replacementGeneration
                detachGeneration = 1L
            }
            if (throwOnCommitNumber == commitCount) {
                throw IllegalStateException("transport failed after Save")
            }
            return commitSucceeds
        }
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
        (values[index] and 0xFF).toByte()
    }

    private class QualifiedGainStore : Ew300GainStateStore {
        var delta = 0
        override fun isGlobalGainQualified(deviceFingerprintKey: String): Boolean = true
        override fun markGlobalGainQualified(deviceFingerprintKey: String, qualified: Boolean) = Unit
        override fun readAppliedGainDeltaSteps(deviceFingerprintKey: String): Int = delta
        override fun writeAppliedGainDeltaSteps(deviceFingerprintKey: String, steps: Int) { delta = steps }
    }
}
