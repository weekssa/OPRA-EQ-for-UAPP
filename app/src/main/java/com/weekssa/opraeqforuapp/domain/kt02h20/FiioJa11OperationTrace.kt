package com.weekssa.opraeqforuapp.domain.kt02h20

import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stages used to distinguish a volatile Apply readback from the post-Save readback. */
enum class FiioJa11OperationStage {
    IDLE,
    AUTHORIZED_SESSION,
    OPTIMIZED_TARGET,
    BASELINE_READ,
    WRITING,
    APPLY_SENT,
    VOLATILE_READBACK,
    SAVE_SENT_ONCE,
    FINAL_READBACK,
    VERIFIED,
    FAILED,
    STATE_UNCERTAIN,
}

data class FiioJa11TraceBand(
    val type: String,
    val frequencyHz: Double,
    val gainDb: Double,
    val q: Double,
)

/** One privacy-safe raw HID exchange captured only while an operation trace is active. */
data class FiioJa11TransportEvent(
    val sequence: Int,
    val elapsedMillis: Long,
    val direction: String,
    val command: String,
    val requestHex: String,
    val responseHex: String?,
    val sessionGeneration: Long,
    val detachGeneration: Long,
    val succeeded: Boolean,
)

/**
 * Evidence for one JA11 Flash or Reset transaction.
 *
 * The report intentionally keeps the raw request/response bytes and exact comparison values
 * together. It is exported only after an operation, not persisted as a background device log.
 */
data class FiioJa11OperationTrace(
    val operationId: String,
    val operation: String,
    val sourceCommit: String,
    val appVersion: String,
    val signerVerified: Boolean,
    val deviceFingerprintKey: String?,
    val usbProductId: Int?,
    val sessionGeneration: Long,
    val detachGeneration: Long,
    val permissionRequestCount: Long,
    val sourceProfileId: String?,
    val canonicalPreampGainDb: Double?,
    val generatedOrSelectedTargetGainDb: Double?,
    val quantizedWireTargetGainDb: Double?,
    val readbackGlobalGainDb: Double?,
    val globalGainToleranceDb: Double,
    val comparisonPhase: String?,
    val sourceBandCount: Int?,
    val targetBandCount: Int?,
    val fidelity: String?,
    val usesGeneratedHeadroom: Boolean?,
    val usedResponseFit: Boolean?,
    val targetBands: List<FiioJa11TraceBand>,
    val saveCommandCount: Long,
    val stateKnown: Boolean,
    val outcome: String,
    val stages: List<FiioJa11OperationStage>,
    val events: List<FiioJa11TransportEvent>,
    val failureReason: String?,
    val baselineProgram: String? = null,
    val baselineGlobalGainDb: Double? = null,
    val baselineBands: List<FiioJa11TraceBand> = emptyList(),
) {
    fun toReadableText(): String = buildString {
        appendLine("FiiO JA11 operation report")
        appendLine("operation=$operation")
        appendLine("operationId=$operationId")
        appendLine("sourceCommit=$sourceCommit")
        appendLine("appVersion=$appVersion")
        appendLine("signerVerified=$signerVerified")
        appendLine("deviceFingerprintKey=${deviceFingerprintKey.redactedForJa11Report() ?: "unavailable"}")
        appendLine("usbProductId=${usbProductId ?: "unavailable"}")
        appendLine("sessionGeneration=$sessionGeneration")
        appendLine("detachGeneration=$detachGeneration")
        appendLine("permissionRequestCount=$permissionRequestCount")
        appendLine("sourceProfileId=${sourceProfileId ?: "unavailable"}")
        appendLine("canonicalPreampGainDb=${canonicalPreampGainDb ?: "unavailable"}")
        appendLine("generatedOrSelectedTargetGainDb=${generatedOrSelectedTargetGainDb ?: "unavailable"}")
        appendLine("quantizedWireTargetGainDb=${quantizedWireTargetGainDb ?: "unavailable"}")
        appendLine("readbackGlobalGainDb=${readbackGlobalGainDb ?: "unavailable"}")
        appendLine("baselineProgram=${baselineProgram ?: "unavailable"}")
        appendLine("baselineGlobalGainDb=${baselineGlobalGainDb ?: "unavailable"}")
        appendLine("baselineBands=${baselineBands.joinToString(";") { it.asText() }}")
        appendLine("globalGainToleranceDb=$globalGainToleranceDb")
        appendLine("comparisonPhase=${comparisonPhase ?: "unavailable"}")
        appendLine("sourceBandCount=${sourceBandCount ?: "unavailable"}")
        appendLine("targetBandCount=${targetBandCount ?: "unavailable"}")
        appendLine("fidelity=${fidelity ?: "unavailable"}")
        appendLine("usesGeneratedHeadroom=${usesGeneratedHeadroom ?: "unavailable"}")
        appendLine("usedResponseFit=${usedResponseFit ?: "unavailable"}")
        appendLine("saveCommandCount=$saveCommandCount")
        appendLine("stateKnown=$stateKnown")
        appendLine("outcome=$outcome")
        appendLine("stages=${stages.joinToString(",")}")
        appendLine("targetBands=${targetBands.joinToString(";") { it.asText() }}")
        appendLine("failureReason=${failureReason.redactedForJa11Report(deviceFingerprintKey) ?: "none"}")
        appendLine("transportEvents=${events.size}")
        events.forEach { event ->
            appendLine(
                "event#${event.sequence} elapsedMs=${event.elapsedMillis} direction=${event.direction} " +
                    "command=${event.command} request=${event.requestHex} response=${event.responseHex ?: "none"} " +
                    "session=${event.sessionGeneration} detach=${event.detachGeneration} success=${event.succeeded}",
            )
        }
        appendLine("privacyNote=USB protocol evidence only; no account or private phone data is included.")
    }

    fun toJson(): String = buildString {
        append('{')
        field("operation", operation)
        field("operationId", operationId)
        field("sourceCommit", sourceCommit)
        field("appVersion", appVersion)
        boolField("signerVerified", signerVerified)
        field("deviceFingerprintKey", deviceFingerprintKey.redactedForJa11Report())
        numberField("usbProductId", usbProductId?.toLong())
        numberField("sessionGeneration", sessionGeneration)
        numberField("detachGeneration", detachGeneration)
        numberField("permissionRequestCount", permissionRequestCount)
        field("sourceProfileId", sourceProfileId)
        decimalField("canonicalPreampGainDb", canonicalPreampGainDb)
        decimalField("generatedOrSelectedTargetGainDb", generatedOrSelectedTargetGainDb)
        decimalField("quantizedWireTargetGainDb", quantizedWireTargetGainDb)
        decimalField("readbackGlobalGainDb", readbackGlobalGainDb)
        field("baselineProgram", baselineProgram)
        decimalField("baselineGlobalGainDb", baselineGlobalGainDb)
        decimalField("globalGainToleranceDb", globalGainToleranceDb)
        field("comparisonPhase", comparisonPhase)
        numberField("sourceBandCount", sourceBandCount?.toLong())
        numberField("targetBandCount", targetBandCount?.toLong())
        field("fidelity", fidelity)
        boolField("usesGeneratedHeadroom", usesGeneratedHeadroom)
        boolField("usedResponseFit", usedResponseFit)
        numberField("saveCommandCount", saveCommandCount)
        boolField("stateKnown", stateKnown)
        field("outcome", outcome)
        field("failureReason", failureReason.redactedForJa11Report(deviceFingerprintKey))
        append(",\"stages\":[")
        stages.forEachIndexed { index, stage ->
            if (index > 0) append(',')
            append('"').append(stage.name).append('"')
        }
        append("]")
        append(",\"targetBands\":[")
        targetBands.forEachIndexed { index, band ->
            if (index > 0) append(',')
            append(band.toJsonObject())
        }
        append("]")
        append(",\"baselineBands\":[")
        baselineBands.forEachIndexed { index, band ->
            if (index > 0) append(',')
            append(band.toJsonObject())
        }
        append("]")
        append(",\"events\":[")
        events.forEachIndexed { index, event ->
            if (index > 0) append(',')
            append(event.toJsonObject())
        }
        append("]}")
    }

    private fun FiioJa11TraceBand.toJsonObject(): String = buildString {
        append('{')
        field("type", type)
        decimalField("frequencyHz", frequencyHz)
        decimalField("gainDb", gainDb)
        decimalField("q", q)
        append('}')
    }

    private fun FiioJa11TransportEvent.toJsonObject(): String = buildString {
        append('{')
        numberField("sequence", sequence.toLong())
        numberField("elapsedMillis", elapsedMillis)
        field("direction", direction)
        field("command", command)
        field("requestHex", requestHex)
        field("responseHex", responseHex)
        numberField("sessionGeneration", sessionGeneration)
        numberField("detachGeneration", detachGeneration)
        boolField("succeeded", succeeded)
        append('}')
    }

    private fun FiioJa11TraceBand.asText(): String =
        "$type ${"%.3f".format(frequencyHz)}Hz ${"%+.4f".format(gainDb)}dB Q${"%.4f".format(q)}"

    private fun StringBuilder.field(name: String, value: String?) {
        if (length > 1) append(',')
        append('"').append(name).append("\":")
        if (value == null) append("null") else append('"').append(value.jsonEscaped()).append('"')
    }

    private fun StringBuilder.boolField(name: String, value: Boolean?) {
        if (length > 1) append(',')
        append('"').append(name).append("\":").append(value ?: "null")
    }

    private fun StringBuilder.numberField(name: String, value: Long?) {
        if (length > 1) append(',')
        append('"').append(name).append("\":").append(value ?: "null")
    }

    private fun StringBuilder.decimalField(name: String, value: Double?) {
        if (length > 1) append(',')
        append('"').append(name).append("\":")
        append(value?.takeIf { it.isFinite() } ?: "null")
    }

    private fun String.jsonEscaped(): String =
        replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}

sealed interface FiioJa11OperationStatus {
    data object Idle : FiioJa11OperationStatus
    data class Running(val operationId: String, val operation: String) : FiioJa11OperationStatus
    data class Completed(val trace: FiioJa11OperationTrace) : FiioJa11OperationStatus
}

class FiioJa11OperationTraceStore {
    private val mutableLastTrace = MutableStateFlow<FiioJa11OperationTrace?>(null)
    val lastTrace: StateFlow<FiioJa11OperationTrace?> = mutableLastTrace.asStateFlow()

    private val mutableStatus = MutableStateFlow<FiioJa11OperationStatus>(FiioJa11OperationStatus.Idle)
    val status: StateFlow<FiioJa11OperationStatus> = mutableStatus.asStateFlow()

    fun begin(operation: String): String {
        val operationId = UUID.randomUUID().toString()
        mutableStatus.value = FiioJa11OperationStatus.Running(operationId, operation)
        return operationId
    }

    fun publish(trace: FiioJa11OperationTrace) {
        val current = mutableStatus.value
        if (current is FiioJa11OperationStatus.Running && current.operationId != trace.operationId) return
        if (current is FiioJa11OperationStatus.Completed && current.trace.operationId != trace.operationId) return
        mutableLastTrace.value = trace
        mutableStatus.value = FiioJa11OperationStatus.Completed(trace)
    }
}

internal class FiioJa11OperationTraceBuilder(
    val operationId: String,
    private val operation: String,
    private val sourceCommit: String,
    private val appVersion: String,
    private val signerVerified: Boolean,
    private val deviceFingerprintKey: String?,
    private val usbProductId: Int?,
    private val sessionGeneration: Long,
    private val detachGeneration: Long,
    private val permissionRequestCount: Long,
) {
    private val stages = mutableListOf(FiioJa11OperationStage.IDLE)
    private val events = mutableListOf<FiioJa11TransportEvent>()
    private var sourceProfileId: String? = null
    private var canonicalPreampGainDb: Double? = null
    private var generatedOrSelectedTargetGainDb: Double? = null
    private var quantizedWireTargetGainDb: Double? = null
    private var readbackGlobalGainDb: Double? = null
    private var comparisonPhase: String? = null
    private var sourceBandCount: Int? = null
    private var targetBandCount: Int? = null
    private var fidelity: String? = null
    private var usesGeneratedHeadroom: Boolean? = null
    private var usedResponseFit: Boolean? = null
    private var targetBands: List<FiioJa11TraceBand> = emptyList()
    private var saveCommandCount = 0L
    private var stateKnown = false
    private var outcome = "NOT_COMPLETED"
    private var failureReason: String? = null
    private var baselineProgram: String? = null
    private var baselineGlobalGainDb: Double? = null
    private var baselineBands: List<FiioJa11TraceBand> = emptyList()

    fun stage(stage: FiioJa11OperationStage) {
        if (stages.lastOrNull() != stage) stages += stage
    }

    fun target(profile: com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile, representation: FiveBandRepresentation, bands: List<FiioJa11Protocol.Band>) {
        sourceProfileId = profile.id
        canonicalPreampGainDb = profile.preampGainDb
        generatedOrSelectedTargetGainDb = representation.playbackGainDb
        quantizedWireTargetGainDb = FiioJa11Protocol.quantizedGlobalGainDb(representation.playbackGainDb)
        sourceBandCount = representation.sourceBandCount
        targetBandCount = bands.size
        fidelity = representation.fidelity.name
        usesGeneratedHeadroom = representation.usesGeneratedHeadroom
        usedResponseFit = representation.usedResponseFit
        targetBands = bands.map { FiioJa11TraceBand(it.type, it.frequencyHz, it.gainDb, it.q) }
        stage(FiioJa11OperationStage.OPTIMIZED_TARGET)
    }

    fun baselineRead(
        program: FiioJa11Protocol.EqProgram,
        globalGainDb: Double,
        bands: List<FiioJa11Protocol.Band>,
    ) {
        baselineProgram = program.name
        baselineGlobalGainDb = globalGainDb
        baselineBands = bands.map { FiioJa11TraceBand(it.type, it.frequencyHz, it.gainDb, it.q) }
        stage(FiioJa11OperationStage.BASELINE_READ)
    }

    fun compare(phase: String, actual: Double?) {
        comparisonPhase = phase
        readbackGlobalGainDb = actual
        stage(if (phase == "FINAL_READBACK") FiioJa11OperationStage.FINAL_READBACK else FiioJa11OperationStage.VOLATILE_READBACK)
    }

    fun addEvents(newEvents: List<FiioJa11TransportEvent>) {
        events += newEvents
    }

    fun saveSent() {
        saveCommandCount += 1L
        stage(FiioJa11OperationStage.SAVE_SENT_ONCE)
    }

    fun complete(outcome: String, stateKnown: Boolean, failureReason: String?) {
        this.outcome = outcome
        this.stateKnown = stateKnown
        this.failureReason = failureReason
        stage(
            when {
                outcome == "Success" -> FiioJa11OperationStage.VERIFIED
                stateKnown -> FiioJa11OperationStage.FAILED
                else -> FiioJa11OperationStage.STATE_UNCERTAIN
            },
        )
    }

    fun build(): FiioJa11OperationTrace = FiioJa11OperationTrace(
        operationId = operationId,
        operation = operation,
        sourceCommit = sourceCommit,
        appVersion = appVersion,
        signerVerified = signerVerified,
        deviceFingerprintKey = deviceFingerprintKey,
        usbProductId = usbProductId,
        sessionGeneration = sessionGeneration,
        detachGeneration = detachGeneration,
        permissionRequestCount = permissionRequestCount,
        sourceProfileId = sourceProfileId,
        canonicalPreampGainDb = canonicalPreampGainDb,
        generatedOrSelectedTargetGainDb = generatedOrSelectedTargetGainDb,
        quantizedWireTargetGainDb = quantizedWireTargetGainDb,
        readbackGlobalGainDb = readbackGlobalGainDb,
        globalGainToleranceDb = 0.001,
        comparisonPhase = comparisonPhase,
        sourceBandCount = sourceBandCount,
        targetBandCount = targetBandCount,
        fidelity = fidelity,
        usesGeneratedHeadroom = usesGeneratedHeadroom,
        usedResponseFit = usedResponseFit,
        targetBands = targetBands,
        saveCommandCount = saveCommandCount,
        stateKnown = stateKnown,
        outcome = outcome,
        stages = stages.toList(),
        events = events.toList(),
        failureReason = failureReason,
        baselineProgram = baselineProgram,
        baselineGlobalGainDb = baselineGlobalGainDb,
        baselineBands = baselineBands,
    )
}

internal fun ByteArray.toJa11TraceHex(): String = joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }

/** Removes the unit-unique USB serial from owner-shareable JA11 reports. */
private fun String?.redactedForJa11Report(fingerprintKey: String? = null): String? {
    if (this == null) return null
    val serial = fingerprintKey
        ?.split('|')
        ?.firstOrNull { it.substringBefore('=').equals("serial", ignoreCase = true) }
        ?.substringAfter('=', "")
        ?.takeIf { it.isNotBlank() }
    val valueWithoutKnownSerial = if (serial == null) this else replace(serial, "[redacted]")
    val serialAssignment = Regex("(?i)(serial\\s*[=:]\\s*)[^|,;\\s]+")
    return serialAssignment.replace(valueWithoutKnownSerial) { match ->
        "${match.groupValues[1]}[redacted]"
    }
}
