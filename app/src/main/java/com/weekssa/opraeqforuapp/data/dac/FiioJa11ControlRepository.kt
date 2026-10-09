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

interface FiioJa11DeviceControlSource {
    val sessionGeneration: Long
    val connectedProductId: Int?
    /** Stable physical-device key with re-enumeration-specific PID/HID interface removed. */
    val deviceIdentityKey: String?
        get() = null
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
    val controlId: DacControlId,
    val requestedValue: DacControlValue,
    val previousSessionGeneration: Long,
    val deviceIdentityKey: String,
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
    data class WriteUncertain(val controlId: DacControlId) : FiioJa11ControlWriteResult
    data class WrongDevice(val controlId: DacControlId) : FiioJa11ControlWriteResult
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
    IDENTITY_UNAVAILABLE,
    IDENTITY_MISMATCH,
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
    fun createPendingRestartWrite(
        controlId: DacControlId,
        requestedValue: DacControlValue,
        baseline: FiioJa11DeviceSnapshot,
    ): FiioJa11PendingRestartWrite? {
        if (!FiioJa11DeviceControls.requiresSessionRestart(controlId)) return null
        val deviceIdentityKey = source.deviceIdentityKey?.takeIf(String::isNotBlank) ?: return null
        if (baseline.sessionGeneration != source.sessionGeneration || !source.isSessionCurrent(baseline.sessionGeneration)) {
            return null
        }
        return FiioJa11PendingRestartWrite(
            controlId = controlId,
            requestedValue = requestedValue,
            previousSessionGeneration = baseline.sessionGeneration,
            deviceIdentityKey = deviceIdentityKey,
            baseline = baseline,
        )
    }

    fun isReplacementSessionCurrent(pending: FiioJa11PendingRestartWrite): Boolean {
        return replacementSessionStatus(pending) == FiioJa11ReplacementSessionStatus.READY
    }

    fun isSupportedReplacementSessionCurrent(pending: FiioJa11PendingRestartWrite): Boolean =
        replacementSessionStatus(pending) in setOf(
            FiioJa11ReplacementSessionStatus.IDENTITY_UNAVAILABLE,
            FiioJa11ReplacementSessionStatus.IDENTITY_MISMATCH,
            FiioJa11ReplacementSessionStatus.READY,
        )

    fun hasStableReplacementIdentity(pending: FiioJa11PendingRestartWrite): Boolean =
        replacementSessionStatus(pending) in setOf(
            FiioJa11ReplacementSessionStatus.IDENTITY_MISMATCH,
            FiioJa11ReplacementSessionStatus.READY,
        )

    internal fun replacementSessionStatus(
        pending: FiioJa11PendingRestartWrite,
    ): FiioJa11ReplacementSessionStatus = replacementSessionEvaluation(pending).status

    private fun replacementSessionEvaluation(
        pending: FiioJa11PendingRestartWrite,
    ): ReplacementSessionEvaluation {
        val first = captureReplacementSessionObservation()
        val second = captureReplacementSessionObservation()
        val status = when {
            first != second || !second.stable -> FiioJa11ReplacementSessionStatus.OBSERVATION_CHANGED
            !second.sessionCurrent ||
                second.generation <= 0L ||
                second.generation == pending.previousSessionGeneration ->
                FiioJa11ReplacementSessionStatus.NOT_CURRENT
            second.productId?.let(FiioJa11Protocol::supportsProductId) != true ->
                FiioJa11ReplacementSessionStatus.UNSUPPORTED_DEVICE
            second.identityKey.isNullOrBlank() -> FiioJa11ReplacementSessionStatus.IDENTITY_UNAVAILABLE
            second.identityKey == pending.deviceIdentityKey -> FiioJa11ReplacementSessionStatus.READY
            else -> FiioJa11ReplacementSessionStatus.IDENTITY_MISMATCH
        }
        return ReplacementSessionEvaluation(status, second)
    }

    private fun captureReplacementSessionObservation(): ReplacementSessionObservation {
        val generation = source.sessionGeneration
        val productId = source.connectedProductId
        val identityKey = source.deviceIdentityKey
        val current = source.isSessionCurrent(generation)
        val stable = generation == source.sessionGeneration &&
            productId == source.connectedProductId &&
            identityKey == source.deviceIdentityKey &&
            current &&
            source.isSessionCurrent(generation)
        return ReplacementSessionObservation(generation, productId, identityKey, current, stable)
    }

    private data class ReplacementSessionObservation(
        val generation: Long,
        val productId: Int?,
        val identityKey: String?,
        val sessionCurrent: Boolean,
        val stable: Boolean,
    )

    private data class ReplacementSessionEvaluation(
        val status: FiioJa11ReplacementSessionStatus,
        val observation: ReplacementSessionObservation,
    )

    private fun replacementIdentityFailure(
        pending: FiioJa11PendingRestartWrite,
    ): FiioJa11ControlWriteResult? {
        val currentIdentity = source.deviceIdentityKey?.takeIf(String::isNotBlank)
            ?: return FiioJa11ControlWriteResult.ReadFailed(pending.controlId, "USB identity")
        return if (currentIdentity == pending.deviceIdentityKey) {
            null
        } else {
            FiioJa11ControlWriteResult.WrongDevice(pending.controlId)
        }
    }

    suspend fun readSnapshot(): FiioJa11ControlReadResult = operationGate.withExclusiveOperation {
        val result = readSnapshotUnlocked()
        if (result is FiioJa11ControlReadResult.Success) {
            val identityKey = source.deviceIdentityKey
            val snapshotSessionStillCurrent =
                source.sessionGeneration == result.snapshot.sessionGeneration &&
                    source.isSessionCurrent(result.snapshot.sessionGeneration)
            Ja11DiagnosticLog.eventForDevice(
                "FiiO JA11",
                "RESTART_IDENTITY_AVAILABILITY",
                "identityAvailable" to (snapshotSessionStillCurrent && !identityKey.isNullOrBlank()),
                "sessionCurrent" to snapshotSessionStillCurrent,
                "sessionGeneration" to result.snapshot.sessionGeneration,
            )
        }
        result
    }

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
            if (generation <= 0L || !source.isSessionCurrent(generation)) {
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
            val requiresSessionRestart = FiioJa11DeviceControls.requiresSessionRestart(intent.controlId)
            val deviceIdentityKey = if (requiresSessionRestart) {
                source.deviceIdentityKey?.takeIf(String::isNotBlank)
                    ?: return@withExclusiveOperation FiioJa11ControlWriteResult.ReadFailed(intent.controlId, "USB identity")
            } else {
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

            val writeOutcome = writeTarget(intent.controlId, intent.requestedValue, generation)
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
                return@withExclusiveOperation FiioJa11ControlWriteResult.ReconnectRequired(
                    FiioJa11PendingRestartWrite(
                        controlId = intent.controlId,
                        requestedValue = intent.requestedValue,
                        previousSessionGeneration = generation,
                        deviceIdentityKey = checkNotNull(deviceIdentityKey),
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

    suspend fun verifyRestartedControl(pending: FiioJa11PendingRestartWrite): FiioJa11ControlWriteResult {
        val result = operationGate.withExclusiveOperation {
            val replacement = replacementSessionEvaluation(pending)
            val observation = replacement.observation
            val generation = observation.generation
            val sessionCurrent = observation.sessionCurrent
            Ja11DiagnosticLog.eventForDevice(
                "FiiO JA11",
                "RESTART_VERIFY_ATTEMPT",
                "expectedSessionGeneration" to pending.previousSessionGeneration,
                "actualSessionGeneration" to generation,
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
                FiioJa11ReplacementSessionStatus.IDENTITY_UNAVAILABLE ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.ReadFailed(
                        pending.controlId,
                        "USB identity",
                    )
                FiioJa11ReplacementSessionStatus.IDENTITY_MISMATCH ->
                    return@withExclusiveOperation FiioJa11ControlWriteResult.WrongDevice(pending.controlId)
                FiioJa11ReplacementSessionStatus.READY -> Unit
            }
            val identity = checkNotNull(observation.identityKey)
            Ja11DiagnosticLog.eventForDevice(
                "FiiO JA11",
                "RESTART_VERIFY_IDENTITY",
                "identityMatches" to (identity == pending.deviceIdentityKey),
            )
            if (source.sessionGeneration != generation || !source.isSessionCurrent(generation)) {
                replacementIdentityFailure(pending)?.let { return@withExclusiveOperation it }
                return@withExclusiveOperation FiioJa11ControlWriteResult.StaleBaseline(
                    pending.controlId,
                    generation,
                    source.sessionGeneration,
                )
            }
            val read = readSnapshotUnlocked(
                expectedSessionGeneration = generation,
                expectedDeviceIdentityKey = identity,
            )
            replacementIdentityFailure(pending)?.let { return@withExclusiveOperation it }
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
            verifyReadback(pending.controlId, pending.requestedValue, pending.baseline, readback, afterRestart = true)
        }
        Ja11DiagnosticLog.eventForDevice(
            "FiiO JA11",
            "RESTART_VERIFY_RESULT",
            "controlId" to pending.controlId.value,
            "result" to result.javaClass.simpleName,
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

    private suspend fun readSnapshotUnlocked(
        expectedSessionGeneration: Long? = null,
        expectedDeviceIdentityKey: String? = null,
    ): FiioJa11ControlReadResult {
        val generation = expectedSessionGeneration ?: source.sessionGeneration
        fun sessionMatches(): Boolean =
            source.sessionGeneration == generation &&
                source.isSessionCurrent(generation) &&
                (expectedDeviceIdentityKey == null || source.deviceIdentityKey == expectedDeviceIdentityKey)

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
        if (source.isSessionCurrent(generation)) FiioJa11ControlReadResult.ReadFailed(field)
        else FiioJa11ControlReadResult.SessionChanged
}

class SessionFiioJa11DeviceControlSource(
    private val sessions: DacSessionRepository,
) : FiioJa11DeviceControlSource {
    override val sessionGeneration: Long
        get() = sessions.fiioJa11Transport.sessionGeneration
    override val connectedProductId: Int?
        get() = sessions.fiioJa11Transport.connectedProductId
    override val deviceIdentityKey: String?
        get() = sessions.fiioJa11Transport.deviceIdentityKey

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
