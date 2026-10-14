package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValidation
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.validateForWrite
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol

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
    suspend fun readEqSnapshot(): HardwareEqSnapshotBundle? = null
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
    val previousDetachGeneration: Long,
    val previousDeviceSerial: String?,
    val baseline: FiioJa11DeviceSnapshot,
)

enum class FiioJa11DefaultsStage {
    BASELINE,
    REVIEW_CURRENTNESS,
    TARGET_WRITE,
    TARGET_VERIFY,
    MIC_TO_OPPOSITE,
    FIRST_REPLACEMENT,
    MIC_RESTORE,
    SECOND_REPLACEMENT,
    FINAL_VERIFY,
    COMPLETE,
}

data class FiioJa11DefaultsWriteCounts(
    val volumeWrites: Int = 0,
    val programWrites: Int = 0,
    val micWrites: Int = 0,
    val uacWrites: Int = 0,
    val eqBandWrites: Int = 0,
    val eqGainWrites: Int = 0,
    val eqApplyWrites: Int = 0,
    val eqSaveWrites: Int = 0,
) {
    init {
        require(volumeWrites in 0..1 && programWrites in 0..1 && micWrites in 0..2)
        require(uacWrites == 0 && eqBandWrites == 0 && eqGainWrites == 0 && eqApplyWrites == 0 && eqSaveWrites == 0)
    }
}

data class FiioJa11DefaultsOperationReport(
    val operationId: String,
    val stage: FiioJa11DefaultsStage,
    val originalMicEnabled: Boolean?,
    val requestedVolume: Int,
    val requestedProgram: FiioJa11Protocol.EqProgram,
    val counts: FiioJa11DefaultsWriteCounts,
    val lastVerifiedSnapshot: FiioJa11DeviceSnapshot?,
    val eqReadback: FiioJa11DefaultsEqReadback = FiioJa11DefaultsEqReadback.UNAVAILABLE,
    val detail: String? = null,
)

enum class FiioJa11DefaultsEqReadback { MATCHED, CHANGED, UNAVAILABLE }

sealed interface FiioJa11DefaultsResult {
    data class Verified(val snapshot: FiioJa11DeviceSnapshot, val report: FiioJa11DefaultsOperationReport) : FiioJa11DefaultsResult
    data class ReviewChanged(val snapshot: FiioJa11DeviceSnapshot) : FiioJa11DefaultsResult
    data class Failed(val report: FiioJa11DefaultsOperationReport) : FiioJa11DefaultsResult
}

data class FiioJa11DefaultsRequest(
    val reviewedSnapshot: FiioJa11DeviceSnapshot,
    val requestedVolume: Int,
    val requestedProgram: FiioJa11Protocol.EqProgram,
    val targetControlId: DacControlId,
) {
    init {
        require(requestedVolume in FiioJa11Protocol.MIN_OUTPUT_VOLUME..FiioJa11Protocol.MAX_OUTPUT_VOLUME)
        require(targetControlId == FiioJa11DeviceControls.OUTPUT_VOLUME ||
            targetControlId == FiioJa11DeviceControls.EQ_PROGRAM)
        require(
            if (targetControlId == FiioJa11DeviceControls.OUTPUT_VOLUME) {
                requestedProgram == reviewedSnapshot.eqProgram
            } else {
                requestedVolume == reviewedSnapshot.outputVolume
            },
        )
    }
}

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
    /** Runs one explicitly confirmed defaults operation while holding the shared JA11 gate. */
    suspend fun setDefaults(request: FiioJa11DefaultsRequest): FiioJa11DefaultsResult =
        operationGate.withExclusiveOperation {
            setDefaultsUnlocked(request)
        }

    private suspend fun setDefaultsUnlocked(request: FiioJa11DefaultsRequest): FiioJa11DefaultsResult {
        val operationId = java.util.UUID.randomUUID().toString()
        var stage = FiioJa11DefaultsStage.BASELINE
        var baseline: FiioJa11DeviceSnapshot? = null
        var originalMic: Boolean? = null
        var counts = FiioJa11DefaultsWriteCounts()

        fun failed(detail: String, snapshot: FiioJa11DeviceSnapshot? = baseline): FiioJa11DefaultsResult.Failed =
            FiioJa11DefaultsResult.Failed(
                FiioJa11DefaultsOperationReport(
                    operationId = operationId,
                    stage = stage,
                    originalMicEnabled = originalMic,
                    requestedVolume = request.requestedVolume,
                    requestedProgram = request.requestedProgram,
                    counts = counts,
                    lastVerifiedSnapshot = snapshot,
                    detail = detail,
                ),
            )

        stage = FiioJa11DefaultsStage.REVIEW_CURRENTNESS
        val current = when (val result = readSnapshotUnlocked()) {
            is FiioJa11ControlReadResult.Success -> result.snapshot
            FiioJa11ControlReadResult.NotConnected -> return failed("JA11 is not connected to one current supported session.", null)
            FiioJa11ControlReadResult.SessionChanged -> return failed("The JA11 session changed while checking the reviewed defaults.", null)
            is FiioJa11ControlReadResult.ReadFailed -> return failed("Could not read ${result.field} before saving defaults.", null)
        }
        if (current.sessionGeneration != request.reviewedSnapshot.sessionGeneration ||
            !sameRelevantState(current, request.reviewedSnapshot)
        ) return FiioJa11DefaultsResult.ReviewChanged(current)

        // Keep both optional EQ reads inside the same shared operation gate as the defaults
        // transaction. This makes any comparison attributable to this operation's reconnects.
        val initialEqSnapshot = if (current.eqProgram == FiioJa11Protocol.EqProgram.USER_1) {
            source.readEqSnapshot()?.takeIf {
                it.snapshot.sessionGeneration == current.sessionGeneration &&
                    it.snapshot.activeProgram == FiioJa11Protocol.EqProgram.USER_1
            }
        } else null
        baseline = current
        originalMic = current.headsetControlEnabled

        val originalVolume = current.outputVolume
        val originalProgram = current.eqProgram
        val originalMicValue = current.headsetControlEnabled
        stage = FiioJa11DefaultsStage.TARGET_WRITE
        if (request.requestedVolume != originalVolume) {
            counts = counts.copy(volumeWrites = 1)
            if (!source.writeOutputVolume(request.requestedVolume)) return failed("The requested volume write was not confirmed.")
        }
        if (request.requestedProgram != originalProgram) {
            if (!source.isSessionCurrent(current.sessionGeneration)) {
                return failed("The JA11 session changed before the requested program could be written.")
            }
            counts = counts.copy(programWrites = 1)
            if (!source.writeEqProgram(request.requestedProgram)) return failed("The requested EQ program write was not confirmed.")
        }

        stage = FiioJa11DefaultsStage.TARGET_VERIFY
        val afterTarget = when (val result = readSnapshotForGeneration(current.sessionGeneration)) {
            is FiioJa11ControlReadResult.Success -> result.snapshot
            else -> return failed("The runtime volume/program pair could not be verified before saving defaults.")
        }
        if (afterTarget.outputVolume != request.requestedVolume ||
            afterTarget.eqProgram != request.requestedProgram ||
            !sameCompanions(current, afterTarget)
        ) return failed("The runtime pair or a companion setting changed before persistence.", afterTarget)
        baseline = afterTarget

        val temporaryMic = !requireNotNull(originalMic)
        stage = FiioJa11DefaultsStage.MIC_TO_OPPOSITE
        val firstGeneration = source.sessionGeneration
        val firstDetach = source.detachGeneration
        val firstSerial = source.connectedDeviceSerial?.trim()?.takeIf(String::isNotEmpty)
        if (!source.isSessionCurrent(firstGeneration) || source.supportedCandidateCount != 1) {
            return failed("The JA11 session was no longer current before the microphone transition.")
        }
        counts = counts.copy(micWrites = 1)
        if (!source.writeHeadsetControlEnabled(temporaryMic)) return failed("The first microphone transition was not confirmed.")

        stage = FiioJa11DefaultsStage.FIRST_REPLACEMENT
        if (!awaitReplacement(firstGeneration, firstDetach, firstSerial)) {
            return failed("The first microphone reconnect could not be verified.")
        }
        val afterFirstRestart = when (val result = readSnapshotUnlocked()) {
            is FiioJa11ControlReadResult.Success -> result.snapshot
            else -> return failed("The first replacement session could not be read.")
        }
        if (afterFirstRestart.headsetControlEnabled != temporaryMic ||
            afterFirstRestart.outputVolume != request.requestedVolume ||
            afterFirstRestart.eqProgram != request.requestedProgram ||
            !sameCompanionsAfterRestart(current, afterFirstRestart)
        ) return failed("The first replacement session did not verify the requested pair and unchanged companions.", afterFirstRestart)
        baseline = afterFirstRestart

        stage = FiioJa11DefaultsStage.MIC_RESTORE
        val secondGeneration = source.sessionGeneration
        val secondDetach = source.detachGeneration
        val secondSerial = source.connectedDeviceSerial?.trim()?.takeIf(String::isNotEmpty)
        if (!source.isSessionCurrent(secondGeneration) || source.supportedCandidateCount != 1) {
            return failed("The first replacement session was no longer current before restoring the microphone.")
        }
        counts = counts.copy(micWrites = 2)
        if (!source.writeHeadsetControlEnabled(originalMicValue)) return failed("Microphone restoration was not confirmed.")

        stage = FiioJa11DefaultsStage.SECOND_REPLACEMENT
        if (!awaitReplacement(secondGeneration, secondDetach, secondSerial)) {
            return failed("The microphone restoration reconnect could not be verified.")
        }
        val finalSnapshot = when (val result = readSnapshotUnlocked()) {
            is FiioJa11ControlReadResult.Success -> result.snapshot
            else -> return failed("The final replacement session could not be read.")
        }
        stage = FiioJa11DefaultsStage.FINAL_VERIFY
        if (finalSnapshot.outputVolume != request.requestedVolume ||
            finalSnapshot.eqProgram != request.requestedProgram ||
            finalSnapshot.headsetControlEnabled != originalMic ||
            !sameCompanionsAfterRestart(current, finalSnapshot)
        ) return failed("Final defaults or original microphone state did not verify.", finalSnapshot)

        val finalEqSnapshot = if (finalSnapshot.eqProgram == FiioJa11Protocol.EqProgram.USER_1) {
            source.readEqSnapshot()?.takeIf {
                it.snapshot.sessionGeneration == finalSnapshot.sessionGeneration &&
                    it.snapshot.activeProgram == FiioJa11Protocol.EqProgram.USER_1
            }
        } else null
        val eqReadback = if (initialEqSnapshot != null && finalEqSnapshot != null) {
            if (initialEqSnapshot.fingerprint == finalEqSnapshot.fingerprint) {
                FiioJa11DefaultsEqReadback.MATCHED
            } else {
                FiioJa11DefaultsEqReadback.CHANGED
            }
        } else {
            FiioJa11DefaultsEqReadback.UNAVAILABLE
        }

        stage = FiioJa11DefaultsStage.COMPLETE
        val report = FiioJa11DefaultsOperationReport(
            operationId = operationId,
            stage = stage,
            originalMicEnabled = originalMic,
            requestedVolume = request.requestedVolume,
            requestedProgram = request.requestedProgram,
            counts = counts,
            lastVerifiedSnapshot = finalSnapshot,
            eqReadback = eqReadback,
        )
        return FiioJa11DefaultsResult.Verified(finalSnapshot, report)
    }

    private suspend fun awaitReplacement(generation: Long, detach: Long, serial: String?): Boolean =
        source.awaitExpectedReplacementSession(
            previousGeneration = generation,
            previousDetachGeneration = detach,
            previousSerial = serial,
            timeoutMillis = EXPECTED_REPLACEMENT_TIMEOUT_MILLIS,
        ) && source.sessionGeneration != generation &&
            source.detachGeneration != detach && source.supportedCandidateCount == 1 &&
            source.isSessionCurrent(source.sessionGeneration)

    private suspend fun readSnapshotForGeneration(generation: Long): FiioJa11ControlReadResult {
        if (!source.isSessionCurrent(generation) || source.sessionGeneration != generation) {
            return FiioJa11ControlReadResult.SessionChanged
        }
        val result = readSnapshotUnlocked()
        return if (result is FiioJa11ControlReadResult.Success && result.snapshot.sessionGeneration != generation) {
            FiioJa11ControlReadResult.SessionChanged
        } else result
    }

    private fun sameRelevantState(a: FiioJa11DeviceSnapshot, b: FiioJa11DeviceSnapshot): Boolean =
        a.usbProductId == b.usbProductId &&
            a.firmwareVersion == b.firmwareVersion &&
            a.outputVolume == b.outputVolume &&
            a.headsetControlEnabled == b.headsetControlEnabled &&
            a.eqProgram == b.eqProgram &&
            a.uacMode == b.uacMode

    private fun sameCompanions(before: FiioJa11DeviceSnapshot, after: FiioJa11DeviceSnapshot): Boolean =
        before.firmwareVersion == after.firmwareVersion &&
            before.headsetControlEnabled == after.headsetControlEnabled &&
            before.uacMode == after.uacMode &&
            before.usbProductId == after.usbProductId

    private fun sameCompanionsAfterRestart(before: FiioJa11DeviceSnapshot, after: FiioJa11DeviceSnapshot): Boolean =
        before.firmwareVersion == after.firmwareVersion &&
            before.uacMode == after.uacMode &&
            before.usbProductId == after.usbProductId

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
) : FiioJa11DeviceControlSource {
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
    override suspend fun readEqSnapshot(): HardwareEqSnapshotBundle? = sessions.readFiioJa11Snapshot()
    override suspend fun readUacMode(): FiioJa11Protocol.UacMode? = sessions.fiioJa11Transport.readUacMode()
    override suspend fun writeOutputVolume(level: Int): Boolean = sessions.fiioJa11Transport.writeOutputVolume(level)
    override suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean =
        sessions.fiioJa11Transport.writeHeadsetControlEnabled(enabled)
    override suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean =
        sessions.fiioJa11Transport.writeEqProgram(program)
    override suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean = sessions.fiioJa11Transport.writeUacMode(mode)
}
