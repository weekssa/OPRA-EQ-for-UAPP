package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValidation
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import com.weekssa.opraeqforuapp.domain.dac.validateForWrite
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonPrimitive

interface FiioJa11DeviceControlSource {
    val sessionGeneration: Long
    val detachGeneration: Long
    val connectedDeviceSerial: String?
    val supportedCandidateCount: Int
    val connectedProductId: Int?
    fun isSessionCurrent(sessionGeneration: Long): Boolean
    suspend fun awaitExpectedReplacementSession(
        previousGeneration: Long,
        previousDetachGeneration: Long,
        previousSerial: String?,
        timeoutMillis: Long,
    ): Boolean
    suspend fun readOutputVolume(): Int?
    suspend fun readSampleRateLabel(): String?
    suspend fun readFirmwareVersion(): String?
    suspend fun readHeadsetControlEnabled(): Boolean?
    suspend fun readEqProgram(): FiioJa11Protocol.EqProgram?
    suspend fun readUacMode(): FiioJa11Protocol.UacMode?
    suspend fun writeOutputVolume(level: Int): Boolean
    suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean
    suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean
    suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean
}

data class FiioJa11EqParameterSnapshot(
    val program: FiioJa11Protocol.EqProgram,
    val bands: List<FiioJa11Protocol.Band>,
    val globalGainDb: Double,
) {
    val bankIdentity: String
        get() = if (program == FiioJa11Protocol.EqProgram.USER_1) "USER_1_STORED_BANK" else "ACTIVE_PROGRAM_RAW_ONLY"
}

interface FiioJa11SettingsPersistenceSource {
    suspend fun readEqParameters(program: FiioJa11Protocol.EqProgram): FiioJa11EqParameterSnapshot?
    suspend fun saveCurrentSettings(program: FiioJa11Protocol.EqProgram): Boolean
    suspend fun awaitFinalReadbackReconnect(): Boolean
    fun beginSettingsTrace(operationId: String)
    fun endSettingsTrace(): List<String>
}

data class FiioJa11SettingsSaveReport(
    val operationId: String,
    val sourceCommit: String,
    val action: String,
    val requestedVolume: Int?,
    val requestedProgram: String?,
    val baseline: FiioJa11DeviceSnapshot?,
    val finalState: FiioJa11DeviceSnapshot?,
    val saveProgram: String?,
    val targetedWriteCount: Int,
    val saveCount: Int,
    val sessionGenerations: List<Long>,
    val detachGenerations: List<Long>,
    val readbackStages: List<String>,
    val boundedReadOnlyRetryCount: Int,
    val transportEvents: List<String>,
    val baselineEq: FiioJa11EqParameterSnapshot?,
    val preSaveEq: FiioJa11EqParameterSnapshot?,
    val finalEq: FiioJa11EqParameterSnapshot?,
    val eqComparison: String,
    val outcome: String,
    val stateKnown: Boolean,
    val finalStateCurrent: Boolean,
    val failureReason: String?,
    val latestAutomaticRefresh: FiioJa11DeviceSnapshot? = null,
) {
    fun toReadableText(): String = buildString {
        appendLine("FiiO JA11 settings Save report")
        appendLine("operationId=$operationId")
        appendLine("sourceCommit=$sourceCommit")
        appendLine("action=$action")
        appendLine("requestedVolume=${requestedVolume ?: "not requested"}")
        appendLine("requestedProgram=${requestedProgram ?: "not requested"}")
        appendLine("baseline=${baseline.asReportValue()}")
        appendLine("finalState=${finalState.asReportValue()}")
        appendLine("saveProgram=${saveProgram ?: "unavailable"}")
        appendLine("softwareRecordedTargetWriteCount=$targetedWriteCount")
        appendLine("softwareRecordedSaveCount=$saveCount")
        appendLine("sessionGenerations=${sessionGenerations.joinToString(",")}")
        appendLine("detachGenerations=${detachGenerations.joinToString(",")}")
        appendLine("readbackStages=${readbackStages.joinToString(",")}")
        appendLine("boundedReadOnlyRetryCount=$boundedReadOnlyRetryCount")
        transportEvents.forEach { appendLine("transportEvent=$it") }
        appendLine("baselineEq=${baselineEq.asReportValue()}")
        appendLine("preSaveEq=${preSaveEq.asReportValue()}")
        appendLine("finalEq=${finalEq.asReportValue()}")
        appendLine("eqComparison=$eqComparison")
        appendLine("latestAutomaticRefresh=${latestAutomaticRefresh.asReportValue()}")
        appendLine("outcome=$outcome")
        appendLine("stateKnown=$stateKnown")
        appendLine("finalStateCurrent=$finalStateCurrent")
        appendLine("failureReason=${failureReason ?: "none"}")
        appendLine("countNote=Transport counts are software recorded and do not prove firmware reboot or USB bus counts.")
    }

    fun toJson(): String {
        fun quote(value: String?): String = value?.let(::JsonPrimitive)?.toString() ?: "null"
        val eventsJson = transportEvents.joinToString(prefix = "[", postfix = "]") { quote(it) }
        return """{"operationId":${quote(operationId)},"sourceCommit":${quote(sourceCommit)},"action":${quote(action)},"requestedVolume":${requestedVolume ?: "null"},"requestedProgram":${quote(requestedProgram)},"baseline":${quote(baseline.asReportValue())},"finalState":${quote(finalState.asReportValue())},"saveProgram":${quote(saveProgram)},"softwareRecordedTargetWriteCount":$targetedWriteCount,"softwareRecordedSaveCount":$saveCount,"sessionGenerations":${sessionGenerations.joinToString(prefix = "[", postfix = "]")},"detachGenerations":${detachGenerations.joinToString(prefix = "[", postfix = "]")},"readbackStages":${quote(readbackStages.joinToString(","))},"boundedReadOnlyRetryCount":$boundedReadOnlyRetryCount,"transportEvents":$eventsJson,"baselineEq":${quote(baselineEq.asReportValue())},"preSaveEq":${quote(preSaveEq.asReportValue())},"finalEq":${quote(finalEq.asReportValue())},"eqComparison":${quote(eqComparison)},"latestAutomaticRefresh":${quote(latestAutomaticRefresh.asReportValue())},"outcome":${quote(outcome)},"stateKnown":$stateKnown,"finalStateCurrent":$finalStateCurrent,"failureReason":${quote(failureReason)},"countNote":"software-recorded counts are not firmware reboot or USB bus counts"}"""
    }
}

private fun FiioJa11DeviceSnapshot?.asReportValue(): String = this?.let {
    "volume=${it.outputVolume};program=${it.eqProgram.name};mic=${it.headsetControlEnabled};uac=${it.uacMode.name};pid=${it.usbProductId};firmware=${it.firmwareVersion};generation=${it.sessionGeneration}"
} ?: "unavailable"

private fun FiioJa11EqParameterSnapshot?.asReportValue(): String = this?.let {
    "program=${it.program.name};bankIdentity=${it.bankIdentity};gainDb=${it.globalGainDb};bands=" +
        it.bands.joinToString("|") { band -> "${band.type}:${band.frequencyHz}:${band.gainDb}:${band.q}" }
} ?: "unavailable"

sealed interface FiioJa11ControlReadResult {
    data class Success(val snapshot: FiioJa11DeviceSnapshot) : FiioJa11ControlReadResult
    data object NotConnected : FiioJa11ControlReadResult
    data object SessionChanged : FiioJa11ControlReadResult
    data class ReadFailed(val field: String) : FiioJa11ControlReadResult
}

data class FiioJa11PendingRestartWrite(
    val controlId: DacControlId,
    val requestedValue: DacControlValue,
    val previousSessionGeneration: Long,
    val previousDetachGeneration: Long,
    val previousDeviceSerial: String?,
    val baseline: FiioJa11DeviceSnapshot,
)

sealed interface FiioJa11ControlWriteResult {
    data class Verified(
        val controlId: DacControlId,
        val requestedValue: DacControlValue,
        val baseline: FiioJa11DeviceSnapshot,
        val snapshot: FiioJa11DeviceSnapshot,
    ) : FiioJa11ControlWriteResult

    data class ReconnectRequired(val pending: FiioJa11PendingRestartWrite) : FiioJa11ControlWriteResult
    data class InvalidRequest(val controlId: DacControlId, val validation: DacControlValidation?) : FiioJa11ControlWriteResult
    data class NotConnected(val controlId: DacControlId) : FiioJa11ControlWriteResult
    data class StaleBaseline(
        val controlId: DacControlId,
        val expectedSessionGeneration: Long,
        val actualSessionGeneration: Long,
    ) : FiioJa11ControlWriteResult
    data class ReadFailed(val controlId: DacControlId, val field: String) : FiioJa11ControlWriteResult
    data class TransferFailed(val controlId: DacControlId) : FiioJa11ControlWriteResult
    data class ReadbackMismatch(
        val controlId: DacControlId,
        val requestedValue: DacControlValue,
        val actualValue: DacControlValue?,
        val snapshot: FiioJa11DeviceSnapshot,
    ) : FiioJa11ControlWriteResult
    data class UnrelatedStateChanged(
        val controlId: DacControlId,
        val changedFields: List<String>,
        val snapshot: FiioJa11DeviceSnapshot,
    ) : FiioJa11ControlWriteResult
}

/**
 * Software-established JA11 DEVICE transaction boundary.
 *
 * Production injects the physical JA11 session gate so DEVICE and EQ transactions serialize through
 * one owner. Normal writes require a complete fresh baseline, one targeted write, complete same-
 * session readback, exact requested-value verification, and unrelated-state verification. Controls
 * established to restart/re-enumerate USB return ReconnectRequired until a fresh replacement-session
 * read verifies the requested state.
 */
class FiioJa11ControlRepository(
    private val source: FiioJa11DeviceControlSource,
    private val operationGate: DacOperationGate = MutexDacOperationGate(),
    private val sourceCommit: String = "unknown",
) {
    suspend fun readSnapshot(): FiioJa11ControlReadResult =
        operationGate.withExclusiveOperation { readSnapshotUnlocked() }

    suspend fun writeControl(intent: DacWriteIntent): FiioJa11ControlWriteResult =
        operationGate.withExclusiveOperation {
            val descriptor = FiioJa11DeviceControls.descriptor(intent.controlId)
                ?: return@withExclusiveOperation FiioJa11ControlWriteResult.InvalidRequest(intent.controlId, null)
            if (intent.controlId !in FiioJa11DeviceControls.softwareImplementedWriteControlIds) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.InvalidRequest(intent.controlId, null)
            }
            val validation = descriptor.validateForWrite(intent.requestedValue)
            if (validation !is DacControlValidation.Valid) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.InvalidRequest(intent.controlId, validation)
            }

            val generation = source.sessionGeneration
            if (generation <= 0L || source.supportedCandidateCount != 1 || !source.isSessionCurrent(generation)) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.NotConnected(intent.controlId)
            }
            if (generation != intent.expectedSessionGeneration) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    intent.controlId,
                    intent.expectedSessionGeneration,
                    generation,
                )
            }

            val baseline = when (val read = readSnapshotUnlocked()) {
                is FiioJa11ControlReadResult.Success -> read.snapshot
                FiioJa11ControlReadResult.NotConnected ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.NotConnected(intent.controlId)
                FiioJa11ControlReadResult.SessionChanged ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                        intent.controlId,
                        intent.expectedSessionGeneration,
                        source.sessionGeneration,
                    )
                is FiioJa11ControlReadResult.ReadFailed ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.ReadFailed(intent.controlId, read.field)
            }
            if (baseline.sessionGeneration != intent.expectedSessionGeneration) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    intent.controlId,
                    intent.expectedSessionGeneration,
                    baseline.sessionGeneration,
                )
            }

            val baselineValue = FiioJa11DeviceControls.valueFromSnapshot(intent.controlId, baseline)
            if (baselineValue == intent.requestedValue) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.Verified(
                    intent.controlId,
                    intent.requestedValue,
                    baseline,
                    baseline,
                )
            }

            val previousDetachGeneration = source.detachGeneration
            val previousDeviceSerial = source.connectedDeviceSerial?.trim()?.takeIf(String::isNotEmpty)
            if (!writeTarget(intent.controlId, intent.requestedValue)) {
                return@withExclusiveOperation if (source.isSessionCurrent(generation)) {
                    FiioJa11ControlWriteResult.TransferFailed(intent.controlId)
                } else {
                    FiioJa11ControlWriteResult.StaleBaseline(intent.controlId, generation, source.sessionGeneration)
                }
            }

            if (FiioJa11DeviceControls.requiresSessionRestart(intent.controlId)) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.ReconnectRequired(
                    FiioJa11PendingRestartWrite(
                        controlId = intent.controlId,
                        requestedValue = intent.requestedValue,
                        previousSessionGeneration = generation,
                        previousDetachGeneration = previousDetachGeneration,
                        previousDeviceSerial = previousDeviceSerial,
                        baseline = baseline,
                    ),
                )
            }

            if (!source.isSessionCurrent(generation)) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    intent.controlId,
                    generation,
                    source.sessionGeneration,
                )
            }
            val readback = when (val read = readSnapshotUnlocked()) {
                is FiioJa11ControlReadResult.Success -> read.snapshot
                FiioJa11ControlReadResult.NotConnected ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.NotConnected(intent.controlId)
                FiioJa11ControlReadResult.SessionChanged ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                        intent.controlId,
                        generation,
                        source.sessionGeneration,
                    )
                is FiioJa11ControlReadResult.ReadFailed ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.ReadFailed(intent.controlId, read.field)
            }
            verifyReadback(intent.controlId, intent.requestedValue, baseline, readback)
        }

    suspend fun writeControlAndSave(intent: DacWriteIntent): FiioJa11SettingsSaveReport =
        operationGate.withExclusiveOperation {
            val operationId = UUID.randomUUID().toString()
            val action = when (intent.controlId) {
                FiioJa11DeviceControls.OUTPUT_VOLUME -> "OUTPUT_VOLUME"
                FiioJa11DeviceControls.EQ_PROGRAM -> "EQ_PROGRAM"
                else -> "UNSUPPORTED"
            }
            val requestedVolume = (intent.requestedValue as? DacControlValue.Numeric)?.value
                ?.takeIf { it % 1.0 == 0.0 }?.toInt()
            val requestedProgram = (intent.requestedValue as? DacControlValue.Discrete)?.valueId
                ?.let(FiioJa11DeviceControls::eqProgram)
            var baseline: FiioJa11DeviceSnapshot? = null
            var finalState: FiioJa11DeviceSnapshot? = null
            var baselineEq: FiioJa11EqParameterSnapshot? = null
            var preSaveEq: FiioJa11EqParameterSnapshot? = null
            var finalEq: FiioJa11EqParameterSnapshot? = null
            var saveProgram: FiioJa11Protocol.EqProgram? = null
            var targetWriteCount = 0
            var saveCount = 0
            var readOnlyRetryCount = 0
            val generations = linkedSetOf<Long>()
            val detachGenerations = linkedSetOf<Long>()
            val stages = mutableListOf<String>()
            var eqComparison = "INSUFFICIENT_EVIDENCE"
            var startedTrace = false
            var transportEvents: List<String> = emptyList()
            val persistence = source as? FiioJa11SettingsPersistenceSource

            fun report(outcome: String, reason: String?, known: Boolean = false): FiioJa11SettingsSaveReport {
                if (startedTrace) {
                    transportEvents = persistence?.endSettingsTrace().orEmpty()
                    startedTrace = false
                }
                return FiioJa11SettingsSaveReport(
                operationId = operationId,
                sourceCommit = sourceCommit,
                action = action,
                requestedVolume = requestedVolume,
                requestedProgram = requestedProgram?.name,
                baseline = baseline,
                finalState = finalState,
                saveProgram = saveProgram?.name,
                targetedWriteCount = targetWriteCount,
                saveCount = saveCount,
                sessionGenerations = generations.toList(),
                detachGenerations = detachGenerations.toList(),
                readbackStages = stages.toList(),
                boundedReadOnlyRetryCount = readOnlyRetryCount,
                transportEvents = transportEvents,
                baselineEq = baselineEq,
                preSaveEq = preSaveEq,
                finalEq = finalEq,
                eqComparison = eqComparison,
                outcome = outcome,
                stateKnown = known,
                finalStateCurrent = finalState?.let { source.isSessionCurrent(it.sessionGeneration) } == true,
                failureReason = reason,
            )
            }

            try {
                if (persistence == null) return@withExclusiveOperation report("UNSUPPORTED", "Settings persistence is unavailable.")
                if (action == "UNSUPPORTED") return@withExclusiveOperation report("INVALID_REQUEST", "Only volume and EQ program can be saved.")
                val descriptor = FiioJa11DeviceControls.descriptor(intent.controlId)
                    ?: return@withExclusiveOperation report("INVALID_REQUEST", "Unsupported setting.")
                val validation = descriptor.validateForWrite(intent.requestedValue)
                if (validation !is DacControlValidation.Valid) {
                    return@withExclusiveOperation report("INVALID_REQUEST", "The requested setting is outside the supported range.")
                }
                if ((intent.controlId == FiioJa11DeviceControls.OUTPUT_VOLUME && requestedVolume == null) ||
                    (intent.controlId == FiioJa11DeviceControls.EQ_PROGRAM && requestedProgram == null)
                ) return@withExclusiveOperation report("INVALID_REQUEST", "The requested setting is invalid.")

                val generation = source.sessionGeneration
                generations += generation
                detachGenerations += source.detachGeneration
                if (generation <= 0L || source.supportedCandidateCount != 1 || !source.isSessionCurrent(generation)) {
                    return@withExclusiveOperation report("NOT_CONNECTED", "No unique current JA11 session is available.")
                }
                if (generation != intent.expectedSessionGeneration) {
                    return@withExclusiveOperation report("STALE_BASELINE", "The JA11 session changed before the operation started.")
                }

                persistence.beginSettingsTrace(operationId)
                startedTrace = true
                stages += "BASELINE_READ"
                baseline = when (val read = readSnapshotUnlocked()) {
                    is FiioJa11ControlReadResult.Success -> read.snapshot
                    FiioJa11ControlReadResult.NotConnected -> return@withExclusiveOperation report("NOT_CONNECTED", "The JA11 disconnected before the baseline was complete.")
                    FiioJa11ControlReadResult.SessionChanged -> return@withExclusiveOperation report("STALE_BASELINE", "The JA11 session changed during the baseline read.")
                    is FiioJa11ControlReadResult.ReadFailed -> return@withExclusiveOperation report("BASELINE_READ_FAILED", "Could not read ${read.field}.")
                }
                generations += baseline!!.sessionGeneration
                if (baseline!!.sessionGeneration != intent.expectedSessionGeneration) {
                    return@withExclusiveOperation report("STALE_BASELINE", "The JA11 session changed during the baseline read.")
                }
                val initial = baseline!!
                if (FiioJa11DeviceControls.valueFromSnapshot(intent.controlId, initial) == intent.requestedValue) {
                    stages += "UNCHANGED_NO_WRITE"
                    finalState = initial
                    return@withExclusiveOperation report("UNCHANGED", null, known = true)
                }
                baselineEq = persistence.readEqParameters(initial.eqProgram)
                if ((initial.eqProgram == FiioJa11Protocol.EqProgram.USER_1 && baselineEq == null) ||
                    !source.isSessionCurrent(generation)
                ) {
                    return@withExclusiveOperation report("BASELINE_EQ_READ_FAILED", "Could not read the active program parameters in the current session.")
                }
                if (!source.isSessionCurrent(generation) || source.sessionGeneration != generation ||
                    source.supportedCandidateCount != 1 || !FiioJa11Protocol.supportsProductId(source.connectedProductId ?: -1)
                ) return@withExclusiveOperation report("STALE_BASELINE", "The JA11 identity or session changed before the write.")

                stages += "TARGET_WRITE"
                targetWriteCount = 1
                if (!writeTarget(intent.controlId, intent.requestedValue)) {
                    return@withExclusiveOperation report("TARGET_WRITE_FAILED", "The target setting transfer was not confirmed.")
                }
                if (!source.isSessionCurrent(generation)) {
                    return@withExclusiveOperation report("TARGET_WRITE_UNCERTAIN", "The JA11 session changed during the setting write.")
                }

                stages += "VOLATILE_READBACK"
                val verifiedBeforeSave = when (val read = readSnapshotUnlocked()) {
                    is FiioJa11ControlReadResult.Success -> read.snapshot
                    else -> return@withExclusiveOperation report("VOLATILE_READBACK_FAILED", "Could not verify the setting before Save.")
                }
                finalState = verifiedBeforeSave
                generations += verifiedBeforeSave.sessionGeneration
                if (verifiedBeforeSave.sessionGeneration != generation) {
                    return@withExclusiveOperation report("VOLATILE_READBACK_FAILED", "The JA11 session changed before Save.")
                }
                val targetValue = FiioJa11DeviceControls.valueFromSnapshot(intent.controlId, verifiedBeforeSave)
                if (targetValue != intent.requestedValue) {
                    return@withExclusiveOperation report("VOLATILE_READBACK_MISMATCH", "The setting did not match before Save.", known = true)
                }
                val changed = unrelatedChangedFields(intent.controlId, initial, verifiedBeforeSave, afterRestart = false)
                if (changed.isNotEmpty()) return@withExclusiveOperation report("UNRELATED_STATE_CHANGED", "Unexpected state changed: ${changed.joinToString()}.", known = true)
                saveProgram = verifiedBeforeSave.eqProgram
                preSaveEq = persistence.readEqParameters(saveProgram!!)
                if ((saveProgram == FiioJa11Protocol.EqProgram.USER_1 && preSaveEq == null) ||
                    !source.isSessionCurrent(generation)
                ) {
                    return@withExclusiveOperation report("PRESAVE_EQ_READ_FAILED", "Could not complete the pre-Save parameter read.")
                }
                if (initial.eqProgram == saveProgram && baselineEq != null && preSaveEq != null && baselineEq != preSaveEq) {
                    return@withExclusiveOperation report("EQ_CHANGED_BEFORE_SAVE", "JA11 EQ parameters changed before Save.", known = true)
                }

                stages += "SAVE_SENT_ONCE"
                saveCount = 1
                if (!persistence.saveCurrentSettings(saveProgram!!)) {
                    return@withExclusiveOperation report("SAVE_UNCONFIRMED", "Could not confirm that the settings were saved.")
                }
                stages += "POST_SAVE_READBACK"
                var after: FiioJa11DeviceSnapshot
                while (true) {
                    val postSaveRead = readSnapshotUnlocked()
                    after = when (postSaveRead) {
                        is FiioJa11ControlReadResult.Success -> postSaveRead.snapshot
                        else -> {
                            if (readOnlyRetryCount > 0) {
                                return@withExclusiveOperation report("FINAL_READBACK_FAILED", "Could not confirm that the settings were saved.")
                            }
                            readOnlyRetryCount = 1
                            stages += "READ_ONLY_RECONNECT_RECOVERY"
                            if (!persistence.awaitFinalReadbackReconnect()) {
                                return@withExclusiveOperation report("FINAL_READBACK_FAILED", "Could not confirm that the settings were saved.")
                            }
                            continue
                        }
                    }
                    generations += after.sessionGeneration
                    detachGenerations += source.detachGeneration
                    stages += "FINAL_READBACK"
                    if (!source.isSessionCurrent(after.sessionGeneration) || source.supportedCandidateCount != 1) {
                        if (readOnlyRetryCount > 0) {
                            return@withExclusiveOperation report("FINAL_SESSION_UNSTABLE", "The final JA11 session is not current.")
                        }
                        readOnlyRetryCount = 1
                        stages += "READ_ONLY_RECONNECT_RECOVERY"
                        if (!persistence.awaitFinalReadbackReconnect()) {
                            return@withExclusiveOperation report("FINAL_SESSION_UNSTABLE", "The final JA11 session is not current.")
                        }
                        continue
                    }
                    if (FiioJa11DeviceControls.valueFromSnapshot(intent.controlId, after) != intent.requestedValue) {
                        return@withExclusiveOperation report("FINAL_TARGET_MISMATCH", "Final settings did not match the request.", known = true)
                    }
                    if (unrelatedChangedFields(intent.controlId, initial, after, afterRestart = true).isNotEmpty()) {
                        return@withExclusiveOperation report("FINAL_COMPANION_MISMATCH", "An unrelated DEVICE setting changed.", known = true)
                    }
                    if (after.eqProgram != saveProgram) {
                        return@withExclusiveOperation report("FINAL_PROGRAM_MISMATCH", "The active EQ program changed after Save.", known = true)
                    }
                    val readEq = persistence.readEqParameters(after.eqProgram)
                    if ((after.eqProgram == FiioJa11Protocol.EqProgram.USER_1 && readEq == null) ||
                        !source.isSessionCurrent(after.sessionGeneration)
                    ) {
                        if (readOnlyRetryCount > 0) {
                            return@withExclusiveOperation report("FINAL_EQ_READ_FAILED", "Could not complete final EQ parameter readback.")
                        }
                        readOnlyRetryCount = 1
                        stages += "READ_ONLY_RECONNECT_RECOVERY"
                        if (!persistence.awaitFinalReadbackReconnect()) {
                            return@withExclusiveOperation report("FINAL_EQ_READ_FAILED", "Could not complete final EQ parameter readback.")
                        }
                        continue
                    }
                    finalEq = readEq
                    finalState = after
                    break
                }
                eqComparison = when {
                    preSaveEq?.program == FiioJa11Protocol.EqProgram.USER_1 &&
                        finalEq?.program == FiioJa11Protocol.EqProgram.USER_1 -> "USER_1_STORED_BANK_" +
                        if (preSaveEq == finalEq) "VERIFIED" else "MISMATCH"
                    preSaveEq != null && finalEq != null && preSaveEq.program == finalEq.program -> "SAME_PROGRAM_RAW_" +
                        if (preSaveEq == finalEq) "VERIFIED" else "MISMATCH"
                    else -> "INSUFFICIENT_EVIDENCE"
                }
                if (eqComparison.endsWith("MISMATCH")) {
                    return@withExclusiveOperation report("FINAL_EQ_MISMATCH", "Final EQ parameters changed across Save.", known = true)
                }
                stages += "VERIFIED"
                report("SUCCESS", null, known = true)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                report("FAILED_UNEXPECTEDLY", error.message ?: "unknown transport failure")
            } finally {
                if (startedTrace) persistence?.endSettingsTrace()
            }
        }

    suspend fun verifyRestartedControl(pending: FiioJa11PendingRestartWrite): FiioJa11ControlWriteResult =
        operationGate.withExclusiveOperation {
            if (!source.awaitExpectedReplacementSession(
                    previousGeneration = pending.previousSessionGeneration,
                    previousDetachGeneration = pending.previousDetachGeneration,
                    previousSerial = pending.previousDeviceSerial,
                    timeoutMillis = EXPECTED_REPLACEMENT_TIMEOUT_MILLIS,
                )
            ) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    pending.controlId,
                    pending.previousSessionGeneration,
                    source.sessionGeneration,
                )
            }
            val generation = source.sessionGeneration
            if (generation <= 0L || source.supportedCandidateCount != 1 || !source.isSessionCurrent(generation)) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.NotConnected(pending.controlId)
            }
            if (generation == pending.previousSessionGeneration ||
                source.detachGeneration == pending.previousDetachGeneration
            ) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    pending.controlId,
                    pending.previousSessionGeneration,
                    generation,
                )
            }
            val readback = when (val read = readSnapshotUnlocked()) {
                is FiioJa11ControlReadResult.Success -> read.snapshot
                FiioJa11ControlReadResult.NotConnected ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.NotConnected(pending.controlId)
                FiioJa11ControlReadResult.SessionChanged ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                        pending.controlId,
                        pending.previousSessionGeneration,
                        source.sessionGeneration,
                    )
                is FiioJa11ControlReadResult.ReadFailed ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.ReadFailed(pending.controlId, read.field)
            }
            verifyReadback(pending.controlId, pending.requestedValue, pending.baseline, readback, afterRestart = true)
        }

    private fun verifyReadback(
        controlId: DacControlId,
        requestedValue: DacControlValue,
        baseline: FiioJa11DeviceSnapshot,
        readback: FiioJa11DeviceSnapshot,
        afterRestart: Boolean = false,
    ): FiioJa11ControlWriteResult {
        val actualValue = FiioJa11DeviceControls.valueFromSnapshot(controlId, readback)
        if (actualValue != requestedValue) {
            return FiioJa11ControlWriteResult.ReadbackMismatch(controlId, requestedValue, actualValue, readback)
        }
        val changes = unrelatedChangedFields(controlId, baseline, readback, afterRestart)
        if (changes.isNotEmpty()) {
            return FiioJa11ControlWriteResult.UnrelatedStateChanged(controlId, changes, readback)
        }
        return FiioJa11ControlWriteResult.Verified(controlId, requestedValue, baseline, readback)
    }

    private suspend fun readSnapshotUnlocked(): FiioJa11ControlReadResult {
        val generation = source.sessionGeneration
        if (generation <= 0L || !source.isSessionCurrent(generation)) return FiioJa11ControlReadResult.NotConnected

        suspend fun <T> field(name: String, read: suspend () -> T?): T? {
            if (!source.isSessionCurrent(generation)) return null
            return read()?.takeIf { source.isSessionCurrent(generation) }
        }

        if (source.supportedCandidateCount != 1) return FiioJa11ControlReadResult.NotConnected
        val productId = source.connectedProductId?.takeIf(FiioJa11Protocol::supportsProductId)
            ?: return failedOrChanged(generation, "USB identity")
        val firmware = field("firmware", source::readFirmwareVersion)
            ?: return failedOrChanged(generation, "firmware")
        val sampleRate = field("sample rate", source::readSampleRateLabel)
            ?: return failedOrChanged(generation, "sample rate")
        val volume = field("output volume", source::readOutputVolume)
            ?: return failedOrChanged(generation, "output volume")
        val headset = field("headset control", source::readHeadsetControlEnabled)
            ?: return failedOrChanged(generation, "headset control")
        val program = field("EQ program", source::readEqProgram)
            ?: return failedOrChanged(generation, "EQ program")
        val uac = field("UAC mode", source::readUacMode)
            ?: return failedOrChanged(generation, "UAC mode")
        if (uac.productId != productId) return FiioJa11ControlReadResult.ReadFailed("UAC mode / USB identity")
        if (!source.isSessionCurrent(generation)) return FiioJa11ControlReadResult.SessionChanged

        return FiioJa11ControlReadResult.Success(
            FiioJa11DeviceSnapshot(
                sessionGeneration = generation,
                usbProductId = productId,
                firmwareVersion = firmware,
                sampleRateLabel = sampleRate,
                outputVolume = volume,
                headsetControlEnabled = headset,
                eqProgram = program,
                uacMode = uac,
            ),
        )
    }

    private suspend fun writeTarget(controlId: DacControlId, value: DacControlValue): Boolean = when (controlId) {
        FiioJa11DeviceControls.OUTPUT_VOLUME -> {
            val requested = (value as? DacControlValue.Numeric)?.value ?: return false
            if (requested % 1.0 != 0.0) return false
            source.writeOutputVolume(requested.toInt())
        }
        FiioJa11DeviceControls.EQ_PROGRAM -> {
            val valueId = (value as? DacControlValue.Discrete)?.valueId ?: return false
            source.writeEqProgram(FiioJa11DeviceControls.eqProgram(valueId) ?: return false)
        }
        FiioJa11DeviceControls.HEADSET_CONTROL -> {
            val enabled = (value as? DacControlValue.Toggle)?.enabled ?: return false
            source.writeHeadsetControlEnabled(enabled)
        }
        FiioJa11DeviceControls.UAC_MODE -> {
            val valueId = (value as? DacControlValue.Discrete)?.valueId ?: return false
            source.writeUacMode(FiioJa11DeviceControls.uacMode(valueId) ?: return false)
        }
        else -> false
    }

    private fun unrelatedChangedFields(
        controlId: DacControlId,
        before: FiioJa11DeviceSnapshot,
        after: FiioJa11DeviceSnapshot,
        afterRestart: Boolean,
    ): List<String> = buildList {
        if (before.firmwareVersion != after.firmwareVersion) add("firmware")
        if (controlId != FiioJa11DeviceControls.OUTPUT_VOLUME && before.outputVolume != after.outputVolume) add("output volume")
        if (controlId != FiioJa11DeviceControls.HEADSET_CONTROL && before.headsetControlEnabled != after.headsetControlEnabled) {
            add("headset control")
        }
        if (controlId != FiioJa11DeviceControls.EQ_PROGRAM && before.eqProgram != after.eqProgram) add("EQ program")
        if (controlId != FiioJa11DeviceControls.UAC_MODE && before.uacMode != after.uacMode) add("UAC mode")
        if (controlId != FiioJa11DeviceControls.UAC_MODE && before.usbProductId != after.usbProductId) add("USB identity")
        // Stream/sample-rate may legitimately change independently while audio is playing. It is read
        // for information but is not an owned setting changed by this transaction.
        if (!afterRestart && before.sessionGeneration != after.sessionGeneration) add("USB session")
    }

    private fun failedOrChanged(generation: Long, field: String): FiioJa11ControlReadResult =
        if (source.isSessionCurrent(generation)) FiioJa11ControlReadResult.ReadFailed(field)
        else FiioJa11ControlReadResult.SessionChanged

    private companion object {
        const val EXPECTED_REPLACEMENT_TIMEOUT_MILLIS = 45_000L
    }
}

class SessionFiioJa11DeviceControlSource(
    private val sessions: DacSessionRepository,
) : FiioJa11DeviceControlSource, FiioJa11SettingsPersistenceSource {
    override val sessionGeneration: Long
        get() = sessions.fiioJa11Transport.sessionGeneration
    override val detachGeneration: Long
        get() = sessions.fiioJa11Transport.detachGeneration
    override val connectedDeviceSerial: String?
        get() = sessions.fiioJa11Transport.connectedDeviceSerial
    override val supportedCandidateCount: Int
        get() = sessions.fiioJa11Transport.supportedCandidateCount
    override val connectedProductId: Int?
        get() = sessions.fiioJa11Transport.connectedProductId

    override fun isSessionCurrent(sessionGeneration: Long): Boolean =
        sessions.fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected &&
            sessions.isFiioJa11SessionCurrent(sessionGeneration)

    override suspend fun awaitExpectedReplacementSession(
        previousGeneration: Long,
        previousDetachGeneration: Long,
        previousSerial: String?,
        timeoutMillis: Long,
    ): Boolean = sessions.fiioJa11Transport.awaitExpectedReplacementSession(
        previousGeneration = previousGeneration,
        previousDetachGeneration = previousDetachGeneration,
        previousSerial = previousSerial,
        timeoutMillis = timeoutMillis,
    )

    override suspend fun readOutputVolume(): Int? = sessions.fiioJa11Transport.readOutputVolume()
    override suspend fun readSampleRateLabel(): String? = sessions.fiioJa11Transport.readSampleRateLabel()
    override suspend fun readFirmwareVersion(): String? = sessions.fiioJa11Transport.readFirmwareVersion()
    override suspend fun readHeadsetControlEnabled(): Boolean? = sessions.fiioJa11Transport.readHeadsetControlEnabled()
    override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? = sessions.fiioJa11Transport.readEqProgram()
    override suspend fun readUacMode(): FiioJa11Protocol.UacMode? = sessions.fiioJa11Transport.readUacMode()
    override suspend fun writeOutputVolume(level: Int): Boolean = sessions.fiioJa11Transport.writeOutputVolume(level)
    override suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean =
        sessions.fiioJa11Transport.writeHeadsetControlEnabled(enabled)
    override suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean =
        sessions.fiioJa11Transport.writeEqProgram(program)
    override suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean = sessions.fiioJa11Transport.writeUacMode(mode)

    override suspend fun readEqParameters(program: FiioJa11Protocol.EqProgram): FiioJa11EqParameterSnapshot? {
        val generation = sessionGeneration
        if (!isSessionCurrent(generation) || readEqProgram() != program) return null
        val bands = (0 until FiioJa11Protocol.BAND_COUNT).map { sessions.fiioJa11Transport.readBand(it) }
        val gain = sessions.fiioJa11Transport.readGlobalGainDb()
        if (!isSessionCurrent(generation) || readEqProgram() != program || bands.any { it == null } || gain == null) return null
        return FiioJa11EqParameterSnapshot(program, bands.filterNotNull(), gain)
    }

    override suspend fun saveCurrentSettings(program: FiioJa11Protocol.EqProgram): Boolean =
        sessions.fiioJa11Transport.saveCurrentSettings(program)

    override suspend fun awaitFinalReadbackReconnect(): Boolean =
        sessions.fiioJa11Transport.awaitFinalReadbackReconnect()

    override fun beginSettingsTrace(operationId: String) = sessions.fiioJa11Transport.beginTrace(operationId)
    override fun endSettingsTrace(): List<String> = sessions.fiioJa11Transport.endTrace().map { event ->
        "${event.sequence}:${event.direction}:${event.command}:${event.sessionGeneration}:${event.detachGeneration}:${event.succeeded}"
    }
}
