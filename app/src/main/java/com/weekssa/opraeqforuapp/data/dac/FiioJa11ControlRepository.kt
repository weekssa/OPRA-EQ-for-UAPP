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
import kotlin.math.abs
import kotlin.math.roundToInt

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
    suspend fun readEqBand(index: Int): FiioJa11Protocol.Band? = null
    suspend fun readEqGlobalGainDb(): Double? = null
    suspend fun readDigitalFilterMode(): FiioJa11Protocol.DigitalFilterMode? = null
    suspend fun readUacMode(): FiioJa11Protocol.UacMode?
    suspend fun writeOutputVolume(level: Int): Boolean
    suspend fun writeDigitalFilterMode(mode: FiioJa11Protocol.DigitalFilterMode): Boolean = false
    suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean
    suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean
    suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean
    suspend fun writeEqBand(index: Int, band: FiioJa11Protocol.Band): Boolean = false
    suspend fun writeEqGlobalGain(gainDb: Double): Boolean = false
    suspend fun applyEqChanges(): Boolean = false
    suspend fun saveEqToFlash(program: FiioJa11Protocol.EqProgram): Boolean = false
    suspend fun awaitFinalReadbackReconnect(): Boolean = false
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
    NATIVE_TARGET_WRITE,
    NATIVE_SNAPSHOT,
    NATIVE_STAGE,
    NATIVE_APPLY,
    NATIVE_VOLATILE_VERIFY,
    NATIVE_SAVE,
    NATIVE_FINAL_VERIFY,
    COMPLETE,
}

enum class FiioJa11DefaultsMechanism {
    /** Native reference-derived staging/Apply/Save path. Never writes the microphone. */
    NATIVE_PROGRAM_REWRITE,
    MIC_RECONNECT,
    ;

    companion object {
        /**
         * Volume/program defaults always use the native path. The microphone round trip is a
         * separately built fallback and is never selected automatically.
         */
        fun forRequestedProgram(@Suppress("UNUSED_PARAMETER") program: FiioJa11Protocol.EqProgram): FiioJa11DefaultsMechanism =
            NATIVE_PROGRAM_REWRITE
    }
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
        require(uacWrites == 0)
        require(eqBandWrites in 0..FiioJa11Protocol.BAND_COUNT)
        require(eqGainWrites in 0..1 && eqApplyWrites in 0..1 && eqSaveWrites in 0..1)
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
    val mechanism: FiioJa11DefaultsMechanism,
    val eqReadback: FiioJa11DefaultsEqReadback = FiioJa11DefaultsEqReadback.UNAVAILABLE,
    val detail: String? = null,
) {
    init {
        require(mechanism != FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE || counts.micWrites == 0)
        require(
            mechanism != FiioJa11DefaultsMechanism.MIC_RECONNECT ||
                (counts.eqBandWrites == 0 && counts.eqGainWrites == 0 &&
                    counts.eqApplyWrites == 0 && counts.eqSaveWrites == 0),
        )
    }
}

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
    /**
     * Candidate B switch. The shipped candidate always uses the native path; the explicit
     * microphone round trip is a separately identified fallback that is never selected at runtime.
     */
    private val allowMicRoundTripFallback: Boolean = false,
) {
    /** Runs one explicitly confirmed defaults operation while holding the shared JA11 gate. */
    suspend fun setDefaults(request: FiioJa11DefaultsRequest): FiioJa11DefaultsResult =
        operationGate.withExclusiveOperation {
            setDefaultsUnlocked(request)
        }

    private suspend fun setDefaultsUnlocked(request: FiioJa11DefaultsRequest): FiioJa11DefaultsResult {
        val operationId = java.util.UUID.randomUUID().toString()
        // FiiO Control sends `0x19` with the currently selected preset; a constant 3 targets User 1
        // regardless of the active preset and damaged the bank in J034. The save payload is now the
        // requested program, which keeps the write in the slot the working curve came from.
        val mechanism = if (allowMicRoundTripFallback &&
            request.requestedProgram != FiioJa11Protocol.EqProgram.USER_1
        ) {
            FiioJa11DefaultsMechanism.MIC_RECONNECT
        } else {
            FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE
        }
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
                    mechanism = mechanism,
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

        if (mechanism == FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE) {
            return setProgramVolumeDefaultsNatively(
                request = request,
                reviewedCurrent = current,
                operationId = operationId,
            )
        }

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

        stage = if (eqReadback == FiioJa11DefaultsEqReadback.CHANGED) {
            FiioJa11DefaultsStage.FINAL_VERIFY
        } else {
            FiioJa11DefaultsStage.COMPLETE
        }
        val report = FiioJa11DefaultsOperationReport(
            operationId = operationId,
            stage = stage,
            originalMicEnabled = originalMic,
            requestedVolume = request.requestedVolume,
            requestedProgram = request.requestedProgram,
            counts = counts,
            lastVerifiedSnapshot = finalSnapshot,
            mechanism = mechanism,
            eqReadback = eqReadback,
            detail = if (eqReadback == FiioJa11DefaultsEqReadback.CHANGED) {
                "User 1 EQ readback changed across the defaults reconnect."
            } else {
                null
            },
        )
        return if (stage == FiioJa11DefaultsStage.FINAL_VERIFY) {
            FiioJa11DefaultsResult.Failed(report)
        } else {
            FiioJa11DefaultsResult.Verified(finalSnapshot, report)
        }
    }

    private data class ActiveProgramSnapshot(
        val device: FiioJa11DeviceSnapshot,
        val bands: List<FiioJa11Protocol.Band>,
        val globalGainDb: Double,
    )

    private data class NativeFinalRead(
        val snapshot: ActiveProgramSnapshot?,
        val detail: String?,
    )

    /**
     * Native JA11 program/volume persistence.
     *
     * The active program's working EQ is read fresh, re-staged exactly, applied once, verified in the
     * same session, and committed with the single reference Save(3). The microphone, UAC mode and
     * built-in coefficient meanings are never inferred; any failure stops the transaction and leaves
     * only read-only recovery.
     */
    private suspend fun setProgramVolumeDefaultsNatively(
        request: FiioJa11DefaultsRequest,
        reviewedCurrent: FiioJa11DeviceSnapshot,
        operationId: String,
    ): FiioJa11DefaultsResult {
        val reviewedGeneration = reviewedCurrent.sessionGeneration
        var stage = FiioJa11DefaultsStage.NATIVE_TARGET_WRITE
        var current = reviewedCurrent
        var lastVerifiedSnapshot: FiioJa11DeviceSnapshot? = reviewedCurrent
        var counts = FiioJa11DefaultsWriteCounts()

        fun reviewedSessionIsCurrent(): Boolean =
            source.sessionGeneration == reviewedGeneration &&
                source.isSessionCurrent(reviewedGeneration) &&
                source.supportedCandidateCount == 1

        fun failed(
            detail: String,
            eqReadback: FiioJa11DefaultsEqReadback = FiioJa11DefaultsEqReadback.UNAVAILABLE,
            snapshot: FiioJa11DeviceSnapshot? = lastVerifiedSnapshot,
        ): FiioJa11DefaultsResult.Failed = FiioJa11DefaultsResult.Failed(
            FiioJa11DefaultsOperationReport(
                operationId = operationId,
                stage = stage,
                originalMicEnabled = reviewedCurrent.headsetControlEnabled,
                requestedVolume = request.requestedVolume,
                requestedProgram = request.requestedProgram,
                counts = counts,
                lastVerifiedSnapshot = snapshot,
                mechanism = FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE,
                eqReadback = eqReadback,
                detail = detail,
            ),
        )

        if (current.eqProgram != request.requestedProgram) {
            if (!reviewedSessionIsCurrent()) {
                return failed("The JA11 session changed before the requested EQ program could be written.")
            }
            counts = counts.copy(programWrites = 1)
            if (!source.writeEqProgram(request.requestedProgram)) {
                return failed("The JA11 did not accept the requested EQ program.")
            }
            current = when (val result = readSnapshotForGeneration(reviewedGeneration)) {
                is FiioJa11ControlReadResult.Success -> result.snapshot
                else -> return failed("The requested EQ program could not be verified.")
            }
            if (current.eqProgram != request.requestedProgram ||
                !sameCompanions(reviewedCurrent, current)
            ) {
                return failed("The requested EQ program or a companion setting did not verify.")
            }
            lastVerifiedSnapshot = current
        }

        if (current.outputVolume != request.requestedVolume) {
            if (!reviewedSessionIsCurrent()) {
                return failed("The JA11 session changed before the requested volume could be written.")
            }
            counts = counts.copy(volumeWrites = 1)
            if (!source.writeOutputVolume(request.requestedVolume)) {
                return failed("The requested volume write was not confirmed.")
            }
            current = when (val result = readSnapshotForGeneration(reviewedGeneration)) {
                is FiioJa11ControlReadResult.Success -> result.snapshot
                else -> return failed("The requested volume could not be verified.")
            }
            if (current.outputVolume != request.requestedVolume ||
                current.eqProgram != request.requestedProgram ||
                !sameCompanions(reviewedCurrent, current)
            ) {
                return failed("The requested volume or a companion setting did not verify.")
            }
            lastVerifiedSnapshot = current
        }

        stage = FiioJa11DefaultsStage.NATIVE_SNAPSHOT
        val activeBefore = readCompleteActiveProgramSnapshot(reviewedGeneration)
            ?: return failed("A complete fresh EQ readback could not be read. No EQ data was staged.")
        lastVerifiedSnapshot = activeBefore.device
        defaultsMismatch(
            read = activeBefore,
            request = request,
            reviewedCurrent = reviewedCurrent,
            expectedBands = null,
            expectedGain = null,
        )?.let { reason -> return failed(reason) }

        stage = FiioJa11DefaultsStage.NATIVE_STAGE
        activeBefore.bands.forEachIndexed { index, band ->
            if (!band.isExactlyRepresentable()) {
                return failed("Band ${index + 1} cannot be re-staged exactly. No EQ data was staged.")
            }
        }
        if (!activeBefore.globalGainDb.isExactlyRepresentable(10.0) ||
            activeBefore.globalGainDb !in FiioJa11Protocol.MIN_GLOBAL_GAIN_DB..FiioJa11Protocol.MAX_GLOBAL_GAIN_DB
        ) {
            return failed("The active global EQ gain cannot be re-staged exactly. No EQ data was staged.")
        }
        try {
            activeBefore.bands.forEachIndexed { index, band ->
                FiioJa11Protocol.writeBandReport(index, band)
            }
            FiioJa11Protocol.writeGlobalGainReport(activeBefore.globalGainDb)
        } catch (_: IllegalArgumentException) {
            return failed("The active EQ is outside the exact JA11 staging range. No EQ data was staged.")
        }

        activeBefore.bands.forEachIndexed { index, band ->
            if (!reviewedSessionIsCurrent()) {
                return failed("The JA11 session changed before band ${index + 1} could be staged.")
            }
            counts = counts.copy(eqBandWrites = index + 1)
            if (!source.writeEqBand(index, band)) {
                return failed("The JA11 did not accept unchanged band ${index + 1}.")
            }
        }
        if (!reviewedSessionIsCurrent()) {
            return failed("The JA11 session changed before the unchanged global EQ gain could be staged.")
        }
        counts = counts.copy(eqGainWrites = 1)
        if (!source.writeEqGlobalGain(activeBefore.globalGainDb)) {
            return failed("The JA11 did not accept the unchanged global EQ gain.")
        }

        stage = FiioJa11DefaultsStage.NATIVE_APPLY
        if (!reviewedSessionIsCurrent()) {
            return failed("The JA11 session changed before Apply. The EQ bank was not saved.")
        }
        counts = counts.copy(eqApplyWrites = 1)
        if (!source.applyEqChanges()) {
            return failed("The JA11 did not accept Apply for the preserved EQ bank.")
        }

        stage = FiioJa11DefaultsStage.NATIVE_VOLATILE_VERIFY
        val volatile = readCompleteActiveProgramSnapshot(reviewedGeneration)
            ?: return failed("The applied EQ could not be read back in the same session.")
        lastVerifiedSnapshot = volatile.device
        defaultsMismatch(
            read = volatile,
            request = request,
            reviewedCurrent = reviewedCurrent,
            expectedBands = activeBefore.bands,
            expectedGain = activeBefore.globalGainDb,
        )?.let { reason ->
            return failed(reason, FiioJa11DefaultsEqReadback.CHANGED)
        }

        stage = FiioJa11DefaultsStage.NATIVE_SAVE
        if (!reviewedSessionIsCurrent()) {
            return failed("The JA11 session changed after volatile verification. Save was not sent.")
        }
        counts = counts.copy(eqSaveWrites = 1)
        if (!source.saveEqToFlash(request.requestedProgram)) {
            return failed("The JA11 applied the preserved EQ, but the one Save boundary did not complete.")
        }

        stage = FiioJa11DefaultsStage.NATIVE_FINAL_VERIFY
        val finalRead = readNativeFinalState(
            request = request,
            reviewedCurrent = reviewedCurrent,
            expected = activeBefore,
        )
        finalRead.snapshot?.let {
            current = it.device
            lastVerifiedSnapshot = current
        }
        if (finalRead.detail != null) {
            val readback = when {
                finalRead.snapshot == null -> FiioJa11DefaultsEqReadback.UNAVAILABLE
                bandsAndGainMatch(finalRead.snapshot, activeBefore) -> FiioJa11DefaultsEqReadback.MATCHED
                else -> FiioJa11DefaultsEqReadback.CHANGED
            }
            return failed(finalRead.detail, readback)
        }

        stage = FiioJa11DefaultsStage.COMPLETE
        val report = FiioJa11DefaultsOperationReport(
            operationId = operationId,
            stage = stage,
            originalMicEnabled = reviewedCurrent.headsetControlEnabled,
            requestedVolume = request.requestedVolume,
            requestedProgram = request.requestedProgram,
            counts = counts,
            lastVerifiedSnapshot = current,
            mechanism = FiioJa11DefaultsMechanism.NATIVE_PROGRAM_REWRITE,
            eqReadback = FiioJa11DefaultsEqReadback.MATCHED,
        )
        return FiioJa11DefaultsResult.Verified(current, report)
    }

    private suspend fun readCompleteActiveProgramSnapshot(
        requiredGeneration: Long? = null,
    ): ActiveProgramSnapshot? {
        if (requiredGeneration != null &&
            (source.sessionGeneration != requiredGeneration || !source.isSessionCurrent(requiredGeneration))
        ) {
            return null
        }
        val device = when (val result = readSnapshotUnlocked()) {
            is FiioJa11ControlReadResult.Success -> result.snapshot
            else -> return null
        }
        if (requiredGeneration != null && device.sessionGeneration != requiredGeneration) return null
        val bands = buildList {
            repeat(FiioJa11Protocol.BAND_COUNT) { index ->
                add(source.readEqBand(index) ?: return null)
            }
        }
        val gain = source.readEqGlobalGainDb() ?: return null
        if (source.sessionGeneration != device.sessionGeneration ||
            !source.isSessionCurrent(device.sessionGeneration)
        ) {
            return null
        }
        return ActiveProgramSnapshot(device, bands, gain)
    }

    private fun defaultsMismatch(
        read: ActiveProgramSnapshot,
        request: FiioJa11DefaultsRequest,
        reviewedCurrent: FiioJa11DeviceSnapshot,
        expectedBands: List<FiioJa11Protocol.Band>?,
        expectedGain: Double?,
    ): String? {
        if (read.device.outputVolume != request.requestedVolume) {
            return "The requested volume did not verify with the active EQ program."
        }
        if (read.device.eqProgram != request.requestedProgram) {
            return "The requested EQ program was not active during verification."
        }
        if (read.device.headsetControlEnabled != reviewedCurrent.headsetControlEnabled) {
            return "The microphone state changed during the native defaults path."
        }
        if (!sameCompanionsAfterRestart(reviewedCurrent, read.device)) {
            return "A companion JA11 setting changed during the native defaults path."
        }
        if (expectedBands != null) {
            expectedBands.forEachIndexed { index, expected ->
                val actual = read.bands.getOrNull(index)
                    ?: return "Band ${index + 1} is missing after Apply."
                if (!FiioJa11Protocol.nearlyMatches(expected, actual)) {
                    return "Band ${index + 1} changed after Apply."
                }
            }
        }
        if (expectedGain != null && abs(read.globalGainDb - expectedGain) > 0.051) {
            return "The global EQ gain changed after Apply."
        }
        return null
    }

    private fun bandsAndGainMatch(read: ActiveProgramSnapshot, expected: ActiveProgramSnapshot): Boolean =
        read.bands.size == expected.bands.size &&
            read.bands.zip(expected.bands).all { (actual, wanted) ->
                FiioJa11Protocol.nearlyMatches(wanted, actual)
            } &&
            abs(read.globalGainDb - expected.globalGainDb) <= 0.051

    private suspend fun readNativeFinalState(
        request: FiioJa11DefaultsRequest,
        reviewedCurrent: FiioJa11DeviceSnapshot,
        expected: ActiveProgramSnapshot,
    ): NativeFinalRead {
        val firstGeneration = source.sessionGeneration
        val firstDetachGeneration = source.detachGeneration
        val first = readCompleteActiveProgramSnapshot()
        val firstDetail = if (first == null) {
            "The fresh final readback could not complete after Save."
        } else {
            defaultsMismatch(
                read = first,
                request = request,
                reviewedCurrent = reviewedCurrent,
                expectedBands = expected.bands,
                expectedGain = expected.globalGainDb,
            )
        }
        val firstStable = source.sessionGeneration == firstGeneration &&
            source.detachGeneration == firstDetachGeneration
        if (firstDetail == null && firstStable) return NativeFinalRead(first, null)
        if (first != null && firstDetail != null && firstStable) {
            return NativeFinalRead(first, firstDetail)
        }

        if (!source.awaitFinalReadbackReconnect()) {
            return NativeFinalRead(first, firstDetail ?: "The JA11 USB session changed during final verification.")
        }

        val retryGeneration = source.sessionGeneration
        val retryDetachGeneration = source.detachGeneration
        val retry = readCompleteActiveProgramSnapshot()
            ?: return NativeFinalRead(null, "The replacement-session readback could not complete after Save.")
        defaultsMismatch(
            read = retry,
            request = request,
            reviewedCurrent = reviewedCurrent,
            expectedBands = expected.bands,
            expectedGain = expected.globalGainDb,
        )?.let { reason -> return NativeFinalRead(retry, reason) }
        if (source.sessionGeneration != retryGeneration || source.detachGeneration != retryDetachGeneration) {
            return NativeFinalRead(retry, "The JA11 USB session changed again during final verification.")
        }
        return NativeFinalRead(retry, null)
    }

    private fun FiioJa11Protocol.Band.isExactlyRepresentable(): Boolean =
        frequencyHz.isExactlyRepresentable(1.0) &&
            gainDb.isExactlyRepresentable(10.0) &&
            q.isExactlyRepresentable(100.0) &&
            frequencyHz in FiioJa11Protocol.MIN_FREQUENCY_HZ..FiioJa11Protocol.MAX_FREQUENCY_HZ &&
            gainDb in FiioJa11Protocol.MIN_BAND_GAIN_DB..FiioJa11Protocol.MAX_BAND_GAIN_DB &&
            q in FiioJa11Protocol.MIN_Q..FiioJa11Protocol.MAX_Q

    private fun Double.isExactlyRepresentable(scale: Double): Boolean {
        if (!isFinite()) return false
        val scaled = this * scale
        return scaled.isFinite() && abs(scaled - scaled.roundToInt()) <= 1e-7
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
            (a.digitalFilterMode == null || b.digitalFilterMode == null ||
                a.digitalFilterMode == b.digitalFilterMode) &&
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
            if (intent.controlId == FiioJa11DeviceControls.DIGITAL_FILTER && baseline.digitalFilterMode == null) {
                // No validated reply established the control on this JA11; never guess a value.
                return@withExclusiveOperation FiioJa11ControlWriteResult.InvalidRequest(intent.controlId, null)
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
        // Optional capability probe: a missing or unparsable `0x04` reply disables the control
        // instead of failing the snapshot, because no JA11 filter value is assumed.
        val digitalFilter = if (source.isSessionCurrent(generation)) {
            source.readDigitalFilterMode()
        } else {
            null
        }
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
                digitalFilterMode = digitalFilter,
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
        FiioJa11DeviceControls.DIGITAL_FILTER -> {
            val valueId = (value as? DacControlValue.Discrete)?.valueId ?: return false
            source.writeDigitalFilterMode(FiioJa11DeviceControls.digitalFilterMode(valueId) ?: return false)
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
        if (controlId != FiioJa11DeviceControls.DIGITAL_FILTER &&
            before.digitalFilterMode != null &&
            after.digitalFilterMode != null &&
            before.digitalFilterMode != after.digitalFilterMode
        ) {
            add("digital filter")
        }
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
    override suspend fun readEqBand(index: Int): FiioJa11Protocol.Band? =
        sessions.fiioJa11Transport.readBand(index)
    override suspend fun readEqGlobalGainDb(): Double? = sessions.fiioJa11Transport.readGlobalGainDb()
    override suspend fun readDigitalFilterMode(): FiioJa11Protocol.DigitalFilterMode? =
        sessions.fiioJa11Transport.readDigitalFilterMode()
    override suspend fun readUacMode(): FiioJa11Protocol.UacMode? = sessions.fiioJa11Transport.readUacMode()
    override suspend fun writeOutputVolume(level: Int): Boolean = sessions.fiioJa11Transport.writeOutputVolume(level)
    override suspend fun writeDigitalFilterMode(mode: FiioJa11Protocol.DigitalFilterMode): Boolean =
        sessions.fiioJa11Transport.writeDigitalFilterMode(mode)
    override suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean =
        sessions.fiioJa11Transport.writeHeadsetControlEnabled(enabled)
    override suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean =
        sessions.fiioJa11Transport.writeEqProgram(program)
    override suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean = sessions.fiioJa11Transport.writeUacMode(mode)
    override suspend fun writeEqBand(index: Int, band: FiioJa11Protocol.Band): Boolean =
        sessions.fiioJa11Transport.sendReport(FiioJa11Protocol.writeBandReport(index, band))
    override suspend fun writeEqGlobalGain(gainDb: Double): Boolean =
        sessions.fiioJa11Transport.sendReport(FiioJa11Protocol.writeGlobalGainReport(gainDb))
    override suspend fun applyEqChanges(): Boolean =
        sessions.fiioJa11Transport.sendReport(FiioJa11Protocol.applyReport())
    override suspend fun saveEqToFlash(program: FiioJa11Protocol.EqProgram): Boolean =
        sessions.fiioJa11Transport.saveToFlash(program)
    override suspend fun awaitFinalReadbackReconnect(): Boolean =
        sessions.fiioJa11Transport.awaitFinalReadbackReconnect()
}
