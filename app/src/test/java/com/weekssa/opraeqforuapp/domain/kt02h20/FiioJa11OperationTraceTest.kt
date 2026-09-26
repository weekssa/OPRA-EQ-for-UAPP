package com.weekssa.opraeqforuapp.domain.kt02h20

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiioJa11OperationTraceTest {
    @Test
    fun exportedReportsKeepRawExchangesAndExactComparisonInBothFormats() {
        val trace = FiioJa11OperationTrace(
            operationId = "op-1",
            operation = "FLASH",
            sourceCommit = "0123456789abcdef",
            appVersion = "0.7.0",
            signerVerified = true,
            deviceFingerprintKey = "vid=2972|pid=0102|serial=private",
            usbProductId = 0x0102,
            sessionGeneration = 4L,
            detachGeneration = 5L,
            permissionRequestCount = 0L,
            sourceProfileId = "Jaytiss",
            canonicalPreampGainDb = -3.9,
            generatedOrSelectedTargetGainDb = -3.9,
            quantizedWireTargetGainDb = -3.9,
            readbackGlobalGainDb = 0.0,
            globalGainToleranceDb = 0.001,
            comparisonPhase = "VOLATILE_READBACK",
            sourceBandCount = 9,
            targetBandCount = 5,
            fidelity = "OPTIMIZED",
            usesGeneratedHeadroom = false,
            usedResponseFit = true,
            targetBands = listOf(FiioJa11TraceBand("peak_dip", 1_000.0, 2.0, 1.0)),
            saveCommandCount = 0L,
            stateKnown = true,
            outcome = "VerificationFailed",
            stages = listOf(FiioJa11OperationStage.VOLATILE_READBACK, FiioJa11OperationStage.FAILED),
            events = listOf(
                FiioJa11TransportEvent(
                    sequence = 1,
                    elapsedMillis = 8L,
                    direction = "WRITE",
                    command = "0x17",
                    requestHex = "02 aa 0a 00 00 17 02 ff d9 00 ee",
                    responseHex = null,
                    sessionGeneration = 4L,
                    detachGeneration = 5L,
                    succeeded = true,
                ),
            ),
            failureReason = "JA11 global EQ gain readback did not match the intended value.",
            firmwareVersion = "2.20",
        )

        val readable = trace.toReadableText()
        val json = trace.toJson()

        assertTrue(readable.contains("quantizedWireTargetGainDb=-3.9"))
        assertTrue(readable.contains("firmwareVersion=2.20"))
        assertTrue(readable.contains("request=02 aa 0a 00 00 17 02 ff d9 00 ee"))
        assertTrue(json.contains("\"comparisonPhase\":\"VOLATILE_READBACK\""))
        assertTrue(json.contains("\"firmwareVersion\":\"2.20\""))
        assertTrue(json.contains("\"requestHex\":\"02 aa 0a 00 00 17 02 ff d9 00 ee\""))
        val parsed = Json.parseToJsonElement(json).jsonObject
        assertEquals(1, parsed.getValue("targetBands").jsonArray.size)
        assertEquals(1, parsed.getValue("events").jsonArray.size)
        assertFalse(json.contains("account"))
    }
}
