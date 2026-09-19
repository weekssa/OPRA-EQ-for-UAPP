package com.weekssa.opraeqforuapp.domain.ew300

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class Ew300CapabilityBatchTest {
    @Test
    fun defaultBatchReadsOnlyAllowlistedSnapshotAndExportsReport() = runBlocking {
        val transport = FakeTransport()
        val report = Ew300CapabilityBatch(transport).run()

        assertEquals(Ew300CapabilityCaseResult.Status.PASS, report.status)
        assertTrue(transport.writes.isEmpty())
        assertTrue(report.toReadableText().contains("Read-only EW300 state"))
        assertTrue(report.toJson().contains("identity-and-eq-snapshot"))
    }

    @Test
    fun failureStopsLaterCasesAndMarksStateUnknown() = runBlocking {
        val transport = FakeTransport(failRegister = 0x26)
        val plan = listOf(
            Ew300CapabilityTestCase("first", "First", "first", Ew300CapabilityTestKind.READ_ONLY_SNAPSHOT, 0x26),
            Ew300CapabilityTestCase("second", "Second", "second", Ew300CapabilityTestKind.READ_ONLY_SNAPSHOT, 0x27),
        )

        val report = Ew300CapabilityBatch(transport).run(plan)

        assertEquals(Ew300CapabilityCaseResult.Status.FAIL, report.status)
        assertTrue(!report.stateKnown)
        assertEquals(Ew300CapabilityCaseResult.Status.SKIPPED, report.cases[1].status)
        assertEquals(listOf(0x26), transport.reads)
    }

    private class FakeTransport(
        private val failRegister: Int? = null,
    ) : Ew300Transport {
        override val deviceFingerprintKey: String = "test-ew300|descriptor=fixture"
        val reads = mutableListOf<Int>()
        val writes = mutableListOf<Int>()

        override suspend fun readRegister(register: Int): ByteArray? {
            reads += register
            if (register == failRegister) return null
            return byteArrayOf(0, 0, 0, 0)
        }

        override suspend fun writeRegister(register: Int, data: ByteArray): Boolean {
            writes += register
            return true
        }

        override suspend fun commit(): Boolean = true
    }
}
