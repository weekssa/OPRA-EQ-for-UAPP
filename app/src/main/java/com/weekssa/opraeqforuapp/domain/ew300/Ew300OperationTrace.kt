package com.weekssa.opraeqforuapp.domain.ew300

import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Ew300OperationStage {
    IDLE, AUTHORIZED_SESSION, BASELINE_CAPTURED, WRITING, VOLATILE_VERIFIED,
    SAVE_SENT_ONCE, SAME_SESSION_READBACK, WAITING_FOR_REPLACEMENT,
    REPLACEMENT_IDENTITY_VERIFIED, REPLACEMENT_AUTHORIZED, FINAL_READBACK,
    VERIFIED, FAILED, STATE_UNCERTAIN,
}

/** Privacy-safe, shareable evidence for one EW300 Apply, Flash, or Reset operation. */
data class Ew300OperationTrace(
    val operationId: String,
    val operation: String,
    val sourceCommit: String,
    val appVersion: String,
    val signerVerified: Boolean,
    val deviceFingerprintKey: String?,
    val sessionGeneration: Long,
    val detachGeneration: Long,
    val permissionRequestCount: Long,
    val permissionRequestsBeforeFirstWrite: Long,
    val registerWriteCount: Long,
    val saveCommandCount: Long,
    val mutationReplayCount: Long,
    val competingConnectionJobCount: Long,
    val replacementObserved: Boolean,
    val replacementIdentityMatched: Boolean,
    val baselineCaptured: Boolean,
    val volatileReadbackMatched: Boolean,
    val finalReadbackMatched: Boolean,
    val restorationVerified: Boolean,
    val stateKnown: Boolean,
    val outcome: String,
    val stages: List<Ew300OperationStage>,
) {
    fun toReadableText(): String = buildString {
        appendLine("EW300 operation report")
        appendLine("operation=$operation")
        appendLine("operationId=$operationId")
        appendLine("sourceCommit=$sourceCommit")
        appendLine("appVersion=$appVersion")
        appendLine("signerVerified=$signerVerified")
        appendLine("deviceFingerprintKey=${deviceFingerprintKey ?: "unavailable"}")
        appendLine("sessionGeneration=$sessionGeneration")
        appendLine("detachGeneration=$detachGeneration")
        appendLine("permissionRequestCount=$permissionRequestCount")
        appendLine("permissionRequestsBeforeFirstWrite=$permissionRequestsBeforeFirstWrite")
        appendLine("registerWriteCount=$registerWriteCount")
        appendLine("saveCommandCount=$saveCommandCount")
        appendLine("mutationReplayCount=$mutationReplayCount")
        appendLine("competingConnectionJobCount=$competingConnectionJobCount")
        appendLine("replacementObserved=$replacementObserved")
        appendLine("replacementIdentityMatched=$replacementIdentityMatched")
        appendLine("baselineCaptured=$baselineCaptured")
        appendLine("volatileReadbackMatched=$volatileReadbackMatched")
        appendLine("finalReadbackMatched=$finalReadbackMatched")
        appendLine("restorationVerified=$restorationVerified")
        appendLine("stateKnown=$stateKnown")
        appendLine("outcome=$outcome")
        appendLine("stages=${stages.joinToString(",")}")
        appendLine("privacyNote=No account, phone, or private device data is included.")
    }

    fun toJson(): String = buildString {
        append('{')
        field("operation", operation)
        field("operationId", operationId)
        field("sourceCommit", sourceCommit)
        field("appVersion", appVersion)
        boolField("signerVerified", signerVerified)
        field("deviceFingerprintKey", deviceFingerprintKey)
        numberField("sessionGeneration", sessionGeneration)
        numberField("detachGeneration", detachGeneration)
        numberField("permissionRequestCount", permissionRequestCount)
        numberField("permissionRequestsBeforeFirstWrite", permissionRequestsBeforeFirstWrite)
        numberField("registerWriteCount", registerWriteCount)
        numberField("saveCommandCount", saveCommandCount)
        numberField("mutationReplayCount", mutationReplayCount)
        numberField("competingConnectionJobCount", competingConnectionJobCount)
        boolField("replacementObserved", replacementObserved)
        boolField("replacementIdentityMatched", replacementIdentityMatched)
        boolField("baselineCaptured", baselineCaptured)
        boolField("volatileReadbackMatched", volatileReadbackMatched)
        boolField("finalReadbackMatched", finalReadbackMatched)
        boolField("restorationVerified", restorationVerified)
        boolField("stateKnown", stateKnown)
        field("outcome", outcome)
        append(",\"stages\":[")
        stages.forEachIndexed { index, stage ->
            if (index > 0) append(',')
            append('"').append(stage.name).append('"')
        }
        append("]}")
    }

    private fun StringBuilder.field(name: String, value: String?) {
        if (length > 1) append(',')
        append('"').append(name).append("\":")
        if (value == null) append("null") else append('"').append(value.jsonEscaped()).append('"')
    }

    private fun StringBuilder.boolField(name: String, value: Boolean) {
        if (length > 1) append(',')
        append('"').append(name).append("\":").append(value)
    }

    private fun StringBuilder.numberField(name: String, value: Long) {
        if (length > 1) append(',')
        append('"').append(name).append("\":").append(value)
    }

    private fun String.jsonEscaped(): String = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}

class Ew300OperationTraceBuilder(
    private val operation: String,
    private val sourceCommit: String,
    private val appVersion: String,
    private val signerVerified: Boolean,
    private val deviceFingerprintKey: String?,
    private val sessionGeneration: Long,
    private val detachGeneration: Long,
    private val initialPermissionRequestCount: Long,
    private val initialRegisterWriteCount: Long,
    private val initialSaveCommandCount: Long,
) {
    private val operationId = UUID.randomUUID().toString()
    private val stages = mutableListOf(Ew300OperationStage.IDLE)
    private var baselineCaptured = false
    private var volatileReadbackMatched = false
    private var finalReadbackMatched = false
    private var restorationVerified = false
    private var replacementObserved = false
    private var replacementIdentityMatched = false
    private var outcome = "NOT_COMPLETED"
    private var stateKnown = false
    private var permissionCountBeforeFirstWrite: Long? = null

    fun stage(stage: Ew300OperationStage) {
        if (stages.lastOrNull() != stage) stages += stage
        when (stage) {
            Ew300OperationStage.BASELINE_CAPTURED -> baselineCaptured = true
            Ew300OperationStage.VOLATILE_VERIFIED -> volatileReadbackMatched = true
            Ew300OperationStage.FINAL_READBACK -> finalReadbackMatched = true
            Ew300OperationStage.REPLACEMENT_IDENTITY_VERIFIED -> replacementIdentityMatched = true
            Ew300OperationStage.WAITING_FOR_REPLACEMENT -> replacementObserved = true
            else -> Unit
        }
    }

    fun complete(outcome: String, stateKnown: Boolean) {
        this.outcome = outcome
        this.stateKnown = stateKnown
        stage(if (stateKnown) Ew300OperationStage.VERIFIED else Ew300OperationStage.STATE_UNCERTAIN)
    }

    fun restored() { restorationVerified = true }

    fun markBeforeFirstWrite(permissionRequestCount: Long) {
        if (permissionCountBeforeFirstWrite == null) {
            this.permissionCountBeforeFirstWrite = permissionRequestCount
        }
    }

    fun build(
        permissionRequestCount: Long,
        registerWriteCount: Long,
        saveCommandCount: Long,
        sessionGeneration: Long,
        detachGeneration: Long,
    ): Ew300OperationTrace = Ew300OperationTrace(
        operationId = operationId,
        operation = operation,
        sourceCommit = sourceCommit,
        appVersion = appVersion,
        signerVerified = signerVerified,
        deviceFingerprintKey = deviceFingerprintKey,
        sessionGeneration = sessionGeneration,
        detachGeneration = detachGeneration,
        permissionRequestCount = permissionRequestCount - initialPermissionRequestCount,
        permissionRequestsBeforeFirstWrite =
            (permissionCountBeforeFirstWrite ?: permissionRequestCount) - initialPermissionRequestCount,
        registerWriteCount = registerWriteCount - initialRegisterWriteCount,
        saveCommandCount = saveCommandCount - initialSaveCommandCount,
        mutationReplayCount = 0L,
        competingConnectionJobCount = 0L,
        replacementObserved = replacementObserved,
        replacementIdentityMatched = replacementIdentityMatched,
        baselineCaptured = baselineCaptured,
        volatileReadbackMatched = volatileReadbackMatched,
        finalReadbackMatched = finalReadbackMatched,
        restorationVerified = restorationVerified,
        stateKnown = stateKnown,
        outcome = outcome,
        stages = stages.toList(),
    )
}

class Ew300OperationTraceStore {
    private val mutableLastTrace = MutableStateFlow<Ew300OperationTrace?>(null)
    val lastTrace: StateFlow<Ew300OperationTrace?> = mutableLastTrace.asStateFlow()

    fun publish(trace: Ew300OperationTrace) { mutableLastTrace.value = trace }
}
