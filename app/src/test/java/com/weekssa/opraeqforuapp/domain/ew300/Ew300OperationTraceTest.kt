package com.weekssa.opraeqforuapp.domain.ew300

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300OperationTraceTest {
    @Test
    fun reportContainsRequiredInvariantFieldsWithoutPrivateData() {
        val builder = Ew300OperationTraceBuilder(
            operation = "FLASH",
            sourceCommit = "abcdef123456",
            appVersion = "0.7.0",
            signerVerified = true,
            deviceFingerprintKey = "vid=31b2|pid=111|manufacturer=LE XIAN|product=SIMGOT EW300 DSP|serial=private-test-value|deviceRevision=1.01|interface=3",
            sessionGeneration = 4L,
            detachGeneration = 2L,
            initialPermissionRequestCount = 8L,
            initialRegisterWriteCount = 10L,
            initialSaveCommandCount = 1L,
        )
        builder.stage(Ew300OperationStage.AUTHORIZED_SESSION)
        builder.stage(Ew300OperationStage.BASELINE_CAPTURED)
        builder.markBeforeFirstWrite(8L)
        builder.stage(Ew300OperationStage.WRITING)
        builder.stage(Ew300OperationStage.VOLATILE_VERIFIED)
        builder.stage(Ew300OperationStage.SAVE_SENT_ONCE)
        builder.stage(Ew300OperationStage.FINAL_READBACK)
        builder.complete("Success", stateKnown = true)
        val report = builder.build(8L, 21L, 2L, 5L, 3L)

        assertTrue(report.toReadableText().contains("permissionRequestsBeforeFirstWrite=0"))
        assertTrue(report.toJson().contains("\"saveCommandCount\":1"))
        assertTrue(report.toReadableText().contains("mutationReplayCount=unmeasured"))
        assertTrue(report.toJson().contains("\"competingConnectionJobCount\":null"))
        assertTrue(report.toJson().contains("\"finalReadbackMatched\":true"))
        assertTrue(report.toReadableText().contains("serial=[redacted]"))
        assertFalse(report.toReadableText().contains("private-test-value"))
        assertFalse(report.toJson().contains("private-test-value"))
    }
}
