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

interface FiioJa11DeviceControlSource {
    val sessionGeneration: Long
    val connectedProductId: Int?
    fun isSessionCurrent(sessionGeneration: Long): Boolean
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

            val baselineValue = FiioJa11DeviceControls.valueFromSnapshot(intent.controlId, baseline)
            if (baselineValue == intent.requestedValue) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.Verified(
                    intent.controlId,
                    intent.requestedValue,
                    baseline,
                    baseline,
                )
            }

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

    suspend fun verifyRestartedControl(pending: FiioJa11PendingRestartWrite): FiioJa11ControlWriteResult =
        operationGate.withExclusiveOperation {
            val generation = source.sessionGeneration
            if (generation <= 0L || !source.isSessionCurrent(generation)) {
                return@withExclusiveOperation FiioJa11ControlWriteResult.NotConnected(pending.controlId)
            }
            if (generation == pending.previousSessionGeneration) {
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
}

class SessionFiioJa11DeviceControlSource(
    private val sessions: DacSessionRepository,
) : FiioJa11DeviceControlSource {
    override val sessionGeneration: Long
        get() = sessions.fiioJa11Transport.sessionGeneration
    override val connectedProductId: Int?
        get() = sessions.fiioJa11Transport.connectedProductId

    override fun isSessionCurrent(sessionGeneration: Long): Boolean =
        sessions.fiioJa11ConnectionState.value is Kt02h20ConnectionState.Connected &&
            sessions.isFiioJa11SessionCurrent(sessionGeneration)

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
}
