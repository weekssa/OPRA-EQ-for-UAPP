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

/** Lifecycle state for the most recent EW300 mutation or editor operation. */
sealed interface Ew300OperationStatus {
    data object Idle : Ew300OperationStatus

    data class Running(
        val operationId: String,
        val operation: String,
    ) : Ew300OperationStatus

    data class Completed(
        val trace: Ew300OperationTrace,
    ) : Ew300OperationStatus
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
    /** Null means the operation stopped before a first-write boundary was observed. */
    val permissionRequestsBeforeFirstWrite: Long?,
    val registerWriteCount: Long,
    val saveCommandCount: Long,
    /** Null means this build did not instrument mutation replay attempts. */
    val mutationReplayCount: Long?,
    /** Null means this build did not instrument competing connection jobs. */
    val competingConnectionJobCount: Long?,
    val replacementObserved: Boolean,
    val replacementIdentityMatched: Boolean,
    val baselineCaptured: Boolean,
    val volatileReadbackMatched: Boolean,
    val finalReadbackMatched: Boolean,
    val restorationVerified: Boolean,
    val stateKnown: Boolean,
    val outcome: String,
    val stages: List<Ew300OperationStage>,
    val baselineFingerprintKey: String? = null,
    val baselineSessionGeneration: Long? = null,
    val baselineDetachGeneration: Long? = null,
    val failureReason: String? = null,
) {
    fun toReadableText(): String = buildString {
        appendLine("EW300 operation report")
        appendLine("operation=$operation")
        appendLine("operationId=$operationId")
        appendLine("sourceCommit=$sourceCommit")
        appendLine("appVersion=$appVersion")
        appendLine("signerVerified=$signerVerified")
        appendLine("deviceFingerprintKey=${deviceFingerprintKey.redactedForEw300Report() ?: "unavailable"}")
        appendLine("sessionGeneration=$sessionGeneration")
        appendLine("detachGeneration=$detachGeneration")
        appendLine("permissionRequestCount=$permissionRequestCount")
        appendLine("permissionRequestsBeforeFirstWrite=${permissionRequestsBeforeFirstWrite ?: "unmeasured"}")
        appendLine("registerWriteCount=$registerWriteCount")
        appendLine("saveCommandCount=$saveCommandCount")
        appendLine("mutationReplayCount=${mutationReplayCount ?: "unmeasured"}")
        appendLine("competingConnectionJobCount=${competingConnectionJobCount ?: "unmeasured"}")
        appendLine("replacementObserved=$replacementObserved")
        appendLine("replacementIdentityMatched=$replacementIdentityMatched")
        appendLine("baselineCaptured=$baselineCaptured")
        appendLine("volatileReadbackMatched=$volatileReadbackMatched")
        appendLine("finalReadbackMatched=$finalReadbackMatched")
        appendLine("restorationVerified=$restorationVerified")
        appendLine("stateKnown=$stateKnown")
        appendLine("outcome=$outcome")
        appendLine("baselineFingerprintKey=${baselineFingerprintKey.redactedForEw300Report() ?: "unavailable"}")
        appendLine("baselineSessionGeneration=${baselineSessionGeneration ?: "unavailable"}")
        appendLine("baselineDetachGeneration=${baselineDetachGeneration ?: "unavailable"}")
        appendLine("failureReason=${failureReason.redactedForEw300Report(deviceFingerprintKey ?: baselineFingerprintKey) ?: "none"}")
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
        field("deviceFingerprintKey", deviceFingerprintKey.redactedForEw300Report())
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
        field("baselineFingerprintKey", baselineFingerprintKey.redactedForEw300Report())
        numberField("baselineSessionGeneration", baselineSessionGeneration)
        numberField("baselineDetachGeneration", baselineDetachGeneration)
        field("failureReason", failureReason.redactedForEw300Report(deviceFingerprintKey ?: baselineFingerprintKey))
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

    private fun StringBuilder.numberField(name: String, value: Long?) {
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
    private val operationId: String = UUID.randomUUID().toString(),
) {
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
    private var baselineFingerprintKey: String? = null
    private var baselineSessionGeneration: Long? = null
    private var baselineDetachGeneration: Long? = null
    private var failureReason: String? = null

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
        complete(outcome, stateKnown, null)
    }

    fun complete(outcome: String, stateKnown: Boolean, failureReason: String?) {
        this.outcome = outcome
        this.stateKnown = stateKnown
        this.failureReason = failureReason
        stage(if (stateKnown) Ew300OperationStage.VERIFIED else Ew300OperationStage.STATE_UNCERTAIN)
    }

    fun restored() { restorationVerified = true }

    fun recordBaseline(baseline: Ew300RawBaseline) {
        baselineFingerprintKey = baseline.deviceFingerprintKey
        baselineSessionGeneration = baseline.sessionGeneration
        baselineDetachGeneration = baseline.detachGeneration
        stage(Ew300OperationStage.BASELINE_CAPTURED)
    }

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
        permissionRequestsBeforeFirstWrite = permissionCountBeforeFirstWrite?.let {
            it - initialPermissionRequestCount
        },
        registerWriteCount = registerWriteCount - initialRegisterWriteCount,
        saveCommandCount = saveCommandCount - initialSaveCommandCount,
        mutationReplayCount = null,
        competingConnectionJobCount = null,
        replacementObserved = replacementObserved,
        replacementIdentityMatched = replacementIdentityMatched,
        baselineCaptured = baselineCaptured,
        volatileReadbackMatched = volatileReadbackMatched,
        // FINAL_READBACK means verification was attempted. A match is truthful only when the
        // operation also completed with a known verified state.
        finalReadbackMatched = finalReadbackMatched && stateKnown,
        restorationVerified = restorationVerified,
        stateKnown = stateKnown,
        outcome = outcome,
        stages = stages.toList(),
        baselineFingerprintKey = baselineFingerprintKey,
        baselineSessionGeneration = baselineSessionGeneration,
        baselineDetachGeneration = baselineDetachGeneration,
        failureReason = failureReason,
    )
}

class Ew300OperationTraceStore {
    private val mutableLastTrace = MutableStateFlow<Ew300OperationTrace?>(null)
    val lastTrace: StateFlow<Ew300OperationTrace?> = mutableLastTrace.asStateFlow()

    private val mutableStatus = MutableStateFlow<Ew300OperationStatus>(Ew300OperationStatus.Idle)
    val status: StateFlow<Ew300OperationStatus> = mutableStatus.asStateFlow()

    fun begin(operation: String): String {
        val operationId = UUID.randomUUID().toString()
        mutableStatus.value = Ew300OperationStatus.Running(operationId, operation)
        return operationId
    }

    fun publish(trace: Ew300OperationTrace) {
        val current = mutableStatus.value
        when (current) {
            is Ew300OperationStatus.Running -> if (current.operationId != trace.operationId) {
                // A late completion from an older operation must not replace the newer
                // operation's terminal state or make its warning/success presentation stale.
                return
            }
            is Ew300OperationStatus.Completed -> if (current.trace.operationId != trace.operationId) {
                return
            }
            Ew300OperationStatus.Idle -> Unit
        }
        mutableLastTrace.value = trace
        mutableStatus.value = Ew300OperationStatus.Completed(trace)
    }
}
