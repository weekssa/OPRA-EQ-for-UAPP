package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.data.kt02h20.Ja11DiagnosticLog
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValidation
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import com.weekssa.opraeqforuapp.domain.dac.validateForWrite
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11ReportWriteOutcome
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11RestartContinuity
import com.weekssa.opraeqforuapp.domain.kt02h20.fiioJa11RestartContinuityFromSerials
import com.weekssa.opraeqforuapp.domain.kt02h20.ja11RestartWriteWasAccepted
import java.util.UUID

interface FiioJa11DeviceControlSource {
    val sessionGeneration: Long
    val connectedProductId: Int?
    /** Optional usable USB serial; it is evidence for unit continuity, never a support prerequisite. */
    val deviceSerialIdentity: String?
        get() = null
    val detachGeneration: Long
        get() = 0L
    /** Number of currently attached devices accepted by the exact JA11 VID/PID matcher. */
    val supportedJa11CandidateCount: Int
        get() = 1
    fun bindRestartTransaction(transactionToken: String): Boolean = true
    fun clearRestartTransaction(transactionToken: String) = Unit
    fun isSessionCurrent(sessionGeneration: Long): Boolean
    suspend fun readOutputVolume(): Int?
    suspend fun readSampleRateLabel(): String?
    suspend fun readFirmwareVersion(): String?
    suspend fun readHeadsetControlEnabled(): Boolean?
    suspend fun readEqProgram(): FiioJa11Protocol.EqProgram?
    suspend fun readUacMode(): FiioJa11Protocol.UacMode?
    suspend fun writeOutputVolume(level: Int, expectedSessionGeneration: Long): FiioJa11ReportWriteOutcome
    suspend fun writeHeadsetControlEnabled(
        enabled: Boolean,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome
    suspend fun writeEqProgram(
        program: FiioJa11Protocol.EqProgram,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome
    suspend fun writeUacMode(
        mode: FiioJa11Protocol.UacMode,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome
}

sealed interface FiioJa11ControlReadResult {
    data class Success(val snapshot: FiioJa11DeviceSnapshot) : FiioJa11ControlReadResult
    data object NotConnected : FiioJa11ControlReadResult
    data object SessionChanged : FiioJa11ControlReadResult
    data class ReadFailed(val field: String) : FiioJa11ControlReadResult
}

data class FiioJa11PendingRestartWrite(
    val transactionToken: String = UUID.randomUUID().toString(),
    val controlId: DacControlId,
    val requestedValue: DacControlValue,
    val previousSessionGeneration: Long,
    val previousDetachGeneration: Long = 0L,
    val deviceSerialIdentity: String? = null,
    val baseline: FiioJa11DeviceSnapshot,
    /** Null while the pre-write operation is being handed to the single owned USB write. */
    val writeOutcome: FiioJa11ReportWriteOutcome? = null,
)

enum class FiioJa11RestartVerificationEvidence {
    SAME_DEVICE_SERIAL_MATCHED,
    SOLE_RETURNING_JA11_STATE_VERIFIED,
}

sealed interface FiioJa11ControlWriteResult {
    data class Verified(
        val controlId: DacControlId,
        val requestedValue: DacControlValue,
        val baseline: FiioJa11DeviceSnapshot,
        val snapshot: FiioJa11DeviceSnapshot,
        val restartEvidence: FiioJa11RestartVerificationEvidence? = null,
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
    data class WriteUncertain(val controlId: DacControlId) : FiioJa11ControlWriteResult
    data class WrongDevice(val controlId: DacControlId) : FiioJa11ControlWriteResult
    data class AmbiguousCandidates(val controlId: DacControlId, val candidateCount: Int) : FiioJa11ControlWriteResult
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

internal enum class FiioJa11ReplacementSessionStatus {
    NOT_CURRENT,
    OBSERVATION_CHANGED,
    UNSUPPORTED_DEVICE,
    IDENTITY_MISMATCH,
    EXPECTED_DETACH_NOT_OBSERVED,
    AMBIGUOUS_CANDIDATES,
    NO_CANDIDATE,
    WRITE_NOT_ACCEPTED,
    READY,
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
) {
    val supportedJa11CandidateCount: Int
        get() = source.supportedJa11CandidateCount

    fun createPendingRestartWrite(
        controlId: DacControlId,
        requestedValue: DacControlValue,
        baseline: FiioJa11DeviceSnapshot,
    ): FiioJa11PendingRestartWrite? {
        if (!FiioJa11DeviceControls.requiresSessionRestart(controlId)) return null
        if (source.supportedJa11CandidateCount != 1 ||
            baseline.sessionGeneration != source.sessionGeneration ||
            !source.isSessionCurrent(baseline.sessionGeneration)
        ) {
            return null
        }
        return FiioJa11PendingRestartWrite(
            controlId = controlId,
            requestedValue = requestedValue,
            previousSessionGeneration = baseline.sessionGeneration,
            previousDetachGeneration = source.detachGeneration,
            deviceSerialIdentity = source.deviceSerialIdentity,
            baseline = baseline,
        )
    }

    fun isReplacementSessionCurrent(pending: FiioJa11PendingRestartWrite): Boolean {
        return replacementSessionStatus(pending) == FiioJa11ReplacementSessionStatus.READY
    }

    fun isSupportedReplacementSessionCurrent(pending: FiioJa11PendingRestartWrite): Boolean =
        replacementSessionStatus(pending) == FiioJa11ReplacementSessionStatus.READY

    fun hasStableReplacementIdentity(pending: FiioJa11PendingRestartWrite): Boolean =
        pending.deviceSerialIdentity != null &&
            source.deviceSerialIdentity == pending.deviceSerialIdentity &&
            replacementSessionStatus(pending) == FiioJa11ReplacementSessionStatus.READY

    fun releaseRestartTransaction(pending: FiioJa11PendingRestartWrite) {
        source.clearRestartTransaction(pending.transactionToken)
    }

    internal fun replacementSessionStatus(
        pending: FiioJa11PendingRestartWrite,
    ): FiioJa11ReplacementSessionStatus = replacementSessionEvaluation(pending).status

    private fun replacementSessionEvaluation(
        pending: FiioJa11PendingRestartWrite,
    ): ReplacementSessionEvaluation {
        val first = captureReplacementSessionObservation()
        val second = captureReplacementSessionObservation()
        val status = when {
            pending.writeOutcome?.let(::ja11RestartWriteWasAccepted) != true ->
                FiioJa11ReplacementSessionStatus.WRITE_NOT_ACCEPTED
            second.supportedCandidateCount > 1 -> FiioJa11ReplacementSessionStatus.AMBIGUOUS_CANDIDATES
            second.supportedCandidateCount != 1 -> FiioJa11ReplacementSessionStatus.NO_CANDIDATE
            first != second || !second.stable -> FiioJa11ReplacementSessionStatus.OBSERVATION_CHANGED
            second.detachGeneration <= pending.previousDetachGeneration ->
                FiioJa11ReplacementSessionStatus.EXPECTED_DETACH_NOT_OBSERVED
            !second.sessionCurrent ||
                second.generation <= 0L ||
                second.generation == pending.previousSessionGeneration ->
                FiioJa11ReplacementSessionStatus.NOT_CURRENT
            second.productId?.let(FiioJa11Protocol::supportsProductId) != true ->
                FiioJa11ReplacementSessionStatus.UNSUPPORTED_DEVICE
            pending.deviceSerialIdentity != null && second.serialIdentity != null &&
                pending.deviceSerialIdentity != second.serialIdentity ->
                FiioJa11ReplacementSessionStatus.IDENTITY_MISMATCH
            else -> FiioJa11ReplacementSessionStatus.READY
        }
        return ReplacementSessionEvaluation(status, second)
    }

    private fun captureReplacementSessionObservation(): ReplacementSessionObservation {
        val generation = source.sessionGeneration
        val productId = source.connectedProductId
        val serialIdentity = source.deviceSerialIdentity
        val detachGeneration = source.detachGeneration
        val supportedCandidateCount = source.supportedJa11CandidateCount
        val current = source.isSessionCurrent(generation)
        val stable = generation == source.sessionGeneration &&
            productId == source.connectedProductId &&
            serialIdentity == source.deviceSerialIdentity &&
            detachGeneration == source.detachGeneration &&
            supportedCandidateCount == source.supportedJa11CandidateCount &&
            current &&
            source.isSessionCurrent(generation)
        return ReplacementSessionObservation(
            generation = generation,
            productId = productId,
            serialIdentity = serialIdentity,
            detachGeneration = detachGeneration,
            supportedCandidateCount = supportedCandidateCount,
            sessionCurrent = current,
            stable = stable,
        )
    }

    private data class ReplacementSessionObservation(
        val generation: Long,
        val productId: Int?,
        val serialIdentity: String?,
        val detachGeneration: Long,
        val supportedCandidateCount: Int,
        val sessionCurrent: Boolean,
        val stable: Boolean,
    )

    private data class ReplacementSessionEvaluation(
        val status: FiioJa11ReplacementSessionStatus,
        val observation: ReplacementSessionObservation,
    )

    private fun replacementContinuityFailure(
        pending: FiioJa11PendingRestartWrite,
        expectedObservation: ReplacementSessionObservation,
    ): FiioJa11ControlWriteResult? {
        val actual = captureReplacementSessionObservation()
        if (actual.supportedCandidateCount > 1) {
            return FiioJa11ControlWriteResult.AmbiguousCandidates(
                pending.controlId,
                actual.supportedCandidateCount,
            )
        }
        if (actual.supportedCandidateCount != 1) return FiioJa11ControlWriteResult.NotConnected(pending.controlId)
        if (!actual.stable || actual != expectedObservation) {
            return FiioJa11ControlWriteResult.StaleBaseline(
                pending.controlId,
                expectedObservation.generation,
                actual.generation,
            )
        }
        return if (pending.deviceSerialIdentity != null && actual.serialIdentity != null &&
            pending.deviceSerialIdentity != actual.serialIdentity
        ) {
            FiioJa11ControlWriteResult.WrongDevice(pending.controlId)
        } else {
            null
        }
    }

    suspend fun readSnapshot(): FiioJa11ControlReadResult = operationGate.withExclusiveOperation {
        val result = readSnapshotUnlocked()
        if (result is FiioJa11ControlReadResult.Success) {
            val serialIdentity = source.deviceSerialIdentity
            val snapshotSessionStillCurrent =
                source.sessionGeneration == result.snapshot.sessionGeneration &&
                    source.isSessionCurrent(result.snapshot.sessionGeneration)
            Ja11DiagnosticLog.eventForDevice(
                "FiiO JA11",
                "RESTART_IDENTITY_AVAILABILITY",
                "serialAvailable" to (snapshotSessionStillCurrent && !serialIdentity.isNullOrBlank()),
                "supportedCandidateCount" to source.supportedJa11CandidateCount,
                "sessionCurrent" to snapshotSessionStillCurrent,
                "sessionGeneration" to result.snapshot.sessionGeneration,
            )
        }
        result
    }

    suspend fun writeControl(
        intent: DacWriteIntent,
        preparedRestartWrite: FiioJa11PendingRestartWrite? = null,
    ): FiioJa11ControlWriteResult =
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
            if (generation <= 0L) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.NotConnected(intent.controlId)
            }
            val initialCandidateCount = source.supportedJa11CandidateCount
            if (initialCandidateCount != 1) {
                return@withExclusiveOperation if (initialCandidateCount <= 0) {
                    FiioJa11ControlWriteResult.NotConnected(intent.controlId)
                } else {
                    FiioJa11ControlWriteResult.AmbiguousCandidates(intent.controlId, initialCandidateCount)
                }
            }
            if (!source.isSessionCurrent(generation)) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.NotConnected(intent.controlId)
            }
            if (generation != intent.expectedSessionGeneration) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    intent.controlId,
                    intent.expectedSessionGeneration,
                    generation,
                )
            }
            val requiresSessionRestart = FiioJa11DeviceControls.requiresSessionRestart(intent.controlId)
            val preBaselineCandidateCount = source.supportedJa11CandidateCount
            if (requiresSessionRestart && preBaselineCandidateCount != 1) {
                val candidateCount = preBaselineCandidateCount
                return@withExclusiveOperation if (candidateCount <= 0) {
                    FiioJa11ControlWriteResult.NotConnected(intent.controlId)
                } else {
                    FiioJa11ControlWriteResult.AmbiguousCandidates(intent.controlId, candidateCount)
                }
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
            val pendingRestartWrite = if (requiresSessionRestart) {
                val pending = preparedRestartWrite ?: createPendingRestartWrite(
                    controlId = intent.controlId,
                    requestedValue = intent.requestedValue,
                    baseline = baseline,
                )
                if (pending == null) {
                    val candidateCount = source.supportedJa11CandidateCount
                    return@withExclusiveOperation if (candidateCount <= 0) {
                        FiioJa11ControlWriteResult.NotConnected(intent.controlId)
                    } else {
                        FiioJa11ControlWriteResult.AmbiguousCandidates(intent.controlId, candidateCount)
                    }
                }
                if (pending.controlId != intent.controlId ||
                    pending.requestedValue != intent.requestedValue ||
                    pending.previousSessionGeneration != generation ||
                    pending.previousDetachGeneration != source.detachGeneration ||
                    pending.deviceSerialIdentity != source.deviceSerialIdentity ||
                    pending.baseline != baseline ||
                    pending.writeOutcome != null
                ) {
                    return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                        intent.controlId,
                        intent.expectedSessionGeneration,
                        source.sessionGeneration,
                    )
                }
                pending
            } else {
                if (preparedRestartWrite != null) {
                    return@withExclusiveOperation FiioJa11ControlWriteResult.InvalidRequest(intent.controlId, null)
                }
                null
            }
            if (!source.isSessionCurrent(generation)) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    intent.controlId,
                    generation,
                    source.sessionGeneration,
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

            val preWriteCandidateCount = source.supportedJa11CandidateCount
            if (requiresSessionRestart && preWriteCandidateCount != 1) {
                val candidateCount = preWriteCandidateCount
                return@withExclusiveOperation if (candidateCount <= 0) {
                    FiioJa11ControlWriteResult.NotConnected(intent.controlId)
                } else {
                    FiioJa11ControlWriteResult.AmbiguousCandidates(intent.controlId, candidateCount)
                }
            }
            val transaction = pendingRestartWrite
            if (transaction != null && !source.bindRestartTransaction(transaction.transactionToken)) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    intent.controlId,
                    generation,
                    source.sessionGeneration,
                )
            }
            val writeOutcome = try {
                writeTarget(intent.controlId, intent.requestedValue, generation)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                transaction?.let { source.clearRestartTransaction(it.transactionToken) }
                throw cancelled
            } catch (_: Exception) {
                transaction?.let { source.clearRestartTransaction(it.transactionToken) }
                return@withExclusiveOperation FiioJa11ControlWriteResult.WriteUncertain(intent.controlId)
            }
            if (transaction != null && !ja11RestartWriteWasAccepted(writeOutcome)) {
                source.clearRestartTransaction(transaction.transactionToken)
            }
            when (writeOutcome) {
                FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                        intent.controlId,
                        generation,
                        source.sessionGeneration,
                    )
                FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.WriteUncertain(intent.controlId)
                FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE -> {
                    if (requiresSessionRestart) {
                        // The transport knows the complete report finished before the expected
                        // restart. Preserve that logical target for replacement-session readback.
                    } else {
                        return@withExclusiveOperation FiioJa11ControlWriteResult.WriteUncertain(intent.controlId)
                    }
                }
                FiioJa11ReportWriteOutcome.COMPLETED -> Unit
            }

            if (requiresSessionRestart) {
                val pending = checkNotNull(pendingRestartWrite).copy(writeOutcome = writeOutcome)
                Ja11DiagnosticLog.eventForDevice(
                    "FiiO JA11",
                    "RESTART_WRITE_ACCEPTED",
                    "transactionToken" to pending.transactionToken,
                    "controlId" to pending.controlId.value,
                    "writeOutcome" to writeOutcome.name,
                    "previousSessionGeneration" to pending.previousSessionGeneration,
                    "previousDetachGeneration" to pending.previousDetachGeneration,
                )
                return@withExclusiveOperation FiioJa11ControlWriteResult.ReconnectRequired(
                    pending,
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

    suspend fun verifyRestartedControl(pending: FiioJa11PendingRestartWrite): FiioJa11ControlWriteResult {
        val result = operationGate.withExclusiveOperation {
            val replacement = replacementSessionEvaluation(pending)
            val observation = replacement.observation
            val generation = observation.generation
            val sessionCurrent = observation.sessionCurrent
            Ja11DiagnosticLog.eventForDevice(
                "FiiO JA11",
                "RESTART_VERIFY_ATTEMPT",
                "transactionToken" to pending.transactionToken,
                "expectedSessionGeneration" to pending.previousSessionGeneration,
                "actualSessionGeneration" to generation,
                "expectedDetachGeneration" to pending.previousDetachGeneration,
                "actualDetachGeneration" to observation.detachGeneration,
                "supportedCandidateCount" to observation.supportedCandidateCount,
                "sessionCurrent" to sessionCurrent,
            )
            val productId = observation.productId
            val supportedProductId = productId?.let(FiioJa11Protocol::supportsProductId) == true
            Ja11DiagnosticLog.eventForDevice(
                "FiiO JA11",
                "RESTART_VERIFY_DEVICE",
                "productId" to productId,
                "supportedProductId" to supportedProductId,
            )
            when (replacement.status) {
                FiioJa11ReplacementSessionStatus.NOT_CURRENT -> {
                    return@withExclusiveOperation if (sessionCurrent &&
                        generation == pending.previousSessionGeneration
                    ) {
                        FiioJa11ControlWriteResult.StaleBaseline(
                            pending.controlId,
                            pending.previousSessionGeneration,
                            generation,
                        )
                    } else {
                        FiioJa11ControlWriteResult.NotConnected(pending.controlId)
                    }
                }
                FiioJa11ReplacementSessionStatus.OBSERVATION_CHANGED ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                        pending.controlId,
                        pending.previousSessionGeneration,
                        generation,
                    )
                FiioJa11ReplacementSessionStatus.UNSUPPORTED_DEVICE ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.NotConnected(pending.controlId)
                FiioJa11ReplacementSessionStatus.IDENTITY_MISMATCH ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.WrongDevice(pending.controlId)
                FiioJa11ReplacementSessionStatus.EXPECTED_DETACH_NOT_OBSERVED ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                        pending.controlId,
                        pending.previousSessionGeneration,
                        generation,
                    )
                FiioJa11ReplacementSessionStatus.AMBIGUOUS_CANDIDATES ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.AmbiguousCandidates(
                        pending.controlId,
                        observation.supportedCandidateCount,
                    )
                FiioJa11ReplacementSessionStatus.NO_CANDIDATE ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.NotConnected(pending.controlId)
                FiioJa11ReplacementSessionStatus.WRITE_NOT_ACCEPTED ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.WriteUncertain(pending.controlId)
                FiioJa11ReplacementSessionStatus.READY -> Unit
            }
            val serialIdentity = observation.serialIdentity
            val restartEvidence = fiioJa11RestartContinuityFromSerials(
                originalSerialIdentity = pending.deviceSerialIdentity,
                replacementSerialIdentity = serialIdentity,
                supportedCandidateCount = observation.supportedCandidateCount,
            ) ?: return@withExclusiveOperation FiioJa11ControlWriteResult.WrongDevice(pending.controlId)
            Ja11DiagnosticLog.eventForDevice(
                "FiiO JA11",
                "RESTART_VERIFY_IDENTITY",
                "transactionToken" to pending.transactionToken,
                "serialMatches" to (pending.deviceSerialIdentity != null &&
                    pending.deviceSerialIdentity == serialIdentity),
                "serialAvailable" to (serialIdentity != null),
                "supportedCandidateCount" to observation.supportedCandidateCount,
                "continuity" to restartEvidence.name,
            )
            if (source.sessionGeneration != generation || !source.isSessionCurrent(generation)) {
                replacementContinuityFailure(pending, observation)?.let { return@withExclusiveOperation it }
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    pending.controlId,
                    generation,
                    source.sessionGeneration,
                )
            }
            val read = readSnapshotUnlocked(
                expectedSessionGeneration = generation,
                expectedSerialIdentity = serialIdentity,
                compareExpectedSerialIdentity = true,
            )
            replacementContinuityFailure(pending, observation)?.let { return@withExclusiveOperation it }
            if (source.sessionGeneration != generation || !source.isSessionCurrent(generation)) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    pending.controlId,
                    generation,
                    source.sessionGeneration,
                )
            }
            val readback = when (read) {
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
            if (readback.sessionGeneration != generation) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    pending.controlId,
                    generation,
                    readback.sessionGeneration,
                )
            }
            verifyReadback(
                pending.controlId,
                pending.requestedValue,
                pending.baseline,
                readback,
                afterRestart = true,
                restartEvidence = if (restartEvidence == FiioJa11RestartContinuity.SAME_DEVICE_SERIAL_MATCHED) {
                    FiioJa11RestartVerificationEvidence.SAME_DEVICE_SERIAL_MATCHED
                } else {
                    FiioJa11RestartVerificationEvidence.SOLE_RETURNING_JA11_STATE_VERIFIED
                },
            )
        }
        Ja11DiagnosticLog.eventForDevice(
            "FiiO JA11",
            "RESTART_VERIFY_RESULT",
            "transactionToken" to pending.transactionToken,
            "controlId" to pending.controlId.value,
            "result" to result.javaClass.simpleName,
            "restartEvidence" to (result as? FiioJa11ControlWriteResult.Verified)?.restartEvidence?.name,
            "sessionGeneration" to source.sessionGeneration,
        )
        return result
    }

    private fun verifyReadback(
        controlId: DacControlId,
        requestedValue: DacControlValue,
        baseline: FiioJa11DeviceSnapshot,
        readback: FiioJa11DeviceSnapshot,
        afterRestart: Boolean = false,
        restartEvidence: FiioJa11RestartVerificationEvidence? = null,
    ): FiioJa11ControlWriteResult {
        val actualValue = FiioJa11DeviceControls.valueFromSnapshot(controlId, readback)
        if (actualValue != requestedValue) {
            return FiioJa11ControlWriteResult.ReadbackMismatch(controlId, requestedValue, actualValue, readback)
        }
        val changes = unrelatedChangedFields(controlId, baseline, readback, afterRestart)
        if (changes.isNotEmpty()) {
            return FiioJa11ControlWriteResult.UnrelatedStateChanged(controlId, changes, readback)
        }
        return FiioJa11ControlWriteResult.Verified(
            controlId = controlId,
            requestedValue = requestedValue,
            baseline = baseline,
            snapshot = readback,
            restartEvidence = restartEvidence,
        )
    }

    private suspend fun readSnapshotUnlocked(
        expectedSessionGeneration: Long? = null,
        expectedSerialIdentity: String? = null,
        compareExpectedSerialIdentity: Boolean = false,
    ): FiioJa11ControlReadResult {
        val generation = expectedSessionGeneration ?: source.sessionGeneration
        fun sessionMatches(): Boolean =
            source.supportedJa11CandidateCount == 1 &&
                source.sessionGeneration == generation &&
                source.isSessionCurrent(generation) &&
                (!compareExpectedSerialIdentity || source.deviceSerialIdentity == expectedSerialIdentity)

        if (generation <= 0L || !sessionMatches()) return FiioJa11ControlReadResult.NotConnected

        suspend fun <T> field(name: String, read: suspend () -> T?): T? {
            if (!sessionMatches()) return null
            return read()?.takeIf { sessionMatches() }
        }

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
        if (!sessionMatches()) return FiioJa11ControlReadResult.SessionChanged

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

    private suspend fun writeTarget(
        controlId: DacControlId,
        value: DacControlValue,
        expectedGeneration: Long,
    ): FiioJa11ReportWriteOutcome = when (controlId) {
        FiioJa11DeviceControls.OUTPUT_VOLUME -> {
            val requested = (value as? DacControlValue.Numeric)?.value
                ?: return FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
            if (requested % 1.0 != 0.0) return FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
            writeOrdinaryTarget(expectedGeneration) { source.writeOutputVolume(requested.toInt(), expectedGeneration) }
        }
        FiioJa11DeviceControls.EQ_PROGRAM -> {
            val valueId = (value as? DacControlValue.Discrete)?.valueId
                ?: return FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
            val program = FiioJa11DeviceControls.eqProgram(valueId)
                ?: return FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
            writeOrdinaryTarget(expectedGeneration) { source.writeEqProgram(program, expectedGeneration) }
        }
        FiioJa11DeviceControls.HEADSET_CONTROL -> {
            val enabled = (value as? DacControlValue.Toggle)?.enabled
                ?: return FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
            if (!source.isSessionCurrent(expectedGeneration)) return FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
            source.writeHeadsetControlEnabled(enabled, expectedGeneration)
        }
        FiioJa11DeviceControls.UAC_MODE -> {
            val valueId = (value as? DacControlValue.Discrete)?.valueId
                ?: return FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
            val mode = FiioJa11DeviceControls.uacMode(valueId)
                ?: return FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
            if (!source.isSessionCurrent(expectedGeneration)) return FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
            source.writeUacMode(mode, expectedGeneration)
        }
        else -> FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
    }

    private suspend fun writeOrdinaryTarget(
        expectedGeneration: Long,
        write: suspend () -> FiioJa11ReportWriteOutcome,
    ): FiioJa11ReportWriteOutcome {
        if (!source.isSessionCurrent(expectedGeneration)) return FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
        return when (write()) {
            FiioJa11ReportWriteOutcome.COMPLETED -> if (source.isSessionCurrent(expectedGeneration)) {
                FiioJa11ReportWriteOutcome.COMPLETED
            } else {
                FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
            }
            FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE,
            FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN,
            -> FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
            FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND -> FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
        }
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
        if (source.supportedJa11CandidateCount == 1 && source.isSessionCurrent(generation)) {
            FiioJa11ControlReadResult.ReadFailed(field)
        }
        else FiioJa11ControlReadResult.SessionChanged
}

class SessionFiioJa11DeviceControlSource(
    private val sessions: DacSessionRepository,
) : FiioJa11DeviceControlSource {
    override val sessionGeneration: Long
        get() = sessions.fiioJa11Transport.sessionGeneration
    override val connectedProductId: Int?
        get() = sessions.fiioJa11Transport.connectedProductId
    override val deviceSerialIdentity: String?
        get() = sessions.fiioJa11Transport.deviceSerialIdentity
    override val detachGeneration: Long
        get() = sessions.fiioJa11Transport.detachGeneration
    override val supportedJa11CandidateCount: Int
        get() = sessions.fiioJa11Transport.supportedJa11CandidateCount
    override fun bindRestartTransaction(transactionToken: String): Boolean =
        sessions.fiioJa11Transport.setExpectedRestartTransactionToken(transactionToken)
    override fun clearRestartTransaction(transactionToken: String) =
        sessions.fiioJa11Transport.clearExpectedRestartTransactionToken(transactionToken)

    override fun isSessionCurrent(sessionGeneration: Long): Boolean =
        sessions.fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected &&
            sessions.isFiioJa11SessionCurrent(sessionGeneration)

    override suspend fun readOutputVolume(): Int? = sessions.fiioJa11Transport.readOutputVolume()
    override suspend fun readSampleRateLabel(): String? = sessions.fiioJa11Transport.readSampleRateLabel()
    override suspend fun readFirmwareVersion(): String? = sessions.fiioJa11Transport.readFirmwareVersion()
    override suspend fun readHeadsetControlEnabled(): Boolean? = sessions.fiioJa11Transport.readHeadsetControlEnabled()
    override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? = sessions.fiioJa11Transport.readEqProgram()
    override suspend fun readUacMode(): FiioJa11Protocol.UacMode? = sessions.fiioJa11Transport.readUacMode()
    override suspend fun writeOutputVolume(
        level: Int,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome =
        sessions.fiioJa11Transport.writeOutputVolume(level, expectedSessionGeneration)
    override suspend fun writeHeadsetControlEnabled(
        enabled: Boolean,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome =
        sessions.fiioJa11Transport.writeHeadsetControlEnabled(enabled, expectedSessionGeneration)
    override suspend fun writeEqProgram(
        program: FiioJa11Protocol.EqProgram,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome = sessions.fiioJa11Transport.writeEqProgram(program, expectedSessionGeneration)
    override suspend fun writeUacMode(
        mode: FiioJa11Protocol.UacMode,
        expectedSessionGeneration: Long,
    ): FiioJa11ReportWriteOutcome =
        sessions.fiioJa11Transport.writeUacMode(mode, expectedSessionGeneration)
}
