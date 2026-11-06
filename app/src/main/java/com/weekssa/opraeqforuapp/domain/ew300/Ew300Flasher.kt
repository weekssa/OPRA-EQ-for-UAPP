package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.validateForWrite
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandRepresentation
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface Ew300Transport {
    /** Strong identity for the currently open exact device/session. */
    val deviceFingerprintKey: String?
        get() = null
    val sessionGeneration: Long
        get() = 0L
    /** Monotonic physical detach evidence for power-removal qualification. */
    val detachGeneration: Long
        get() = 0L
    val permissionRequestCount: Long
        get() = 0L
    val registerWriteCount: Long
        get() = 0L
    val registerReadCount: Long
        get() = 0L
    val saveCommandCount: Long
        get() = 0L
    suspend fun readRegister(register: Int): ByteArray?
    suspend fun writeRegister(register: Int, data: ByteArray): Boolean
    suspend fun commit(): Boolean

    /** Confirms a successful no-Save runtime transaction so its session may reconnect safely. */
    fun markRuntimeVerified() = Unit

    /** Read-only UAC1 descriptor/control facts; null when the exact path is unavailable. */
    suspend fun readUacStatus(): Ew300UacStatus? = null

    /** Evidence-backed UAC microphone mute; false when no exact feature unit exists. */
    suspend fun setMicrophoneMuted(muted: Boolean): Boolean = false
}

enum class Ew300RuntimePersistenceState {
    UNKNOWN,
    RUNTIME_ONLY,
    SAVING,
    SAVED_THIS_SESSION,
    SAVE_NOT_VERIFIED,
}

sealed interface Ew300ShelfTestResult {
    data class Verified(val target: Ew300ShelfTestTarget) : Ew300ShelfTestResult
    data class NotSuitable(val reason: String) : Ew300ShelfTestResult
    data class DeviceUnavailable(val reason: String) : Ew300ShelfTestResult
    data class TransferFailed(val reason: String) : Ew300ShelfTestResult
    data class VerificationFailed(val reason: String) : Ew300ShelfTestResult
}

/** Guarded five-band EW300 transaction using the shared finite-target response optimizer. */
class Ew300Flasher(
    private val transport: Ew300Transport,
    private val gainStateStore: Ew300GainStateStore,
    private val mutationAuthorized: (String) -> Boolean = Ew300CapabilityProfile::authorizesMutation,
    private val traceStore: Ew300OperationTraceStore = Ew300OperationTraceStore(),
    private val sourceCommit: String = "unknown",
    private val appVersion: String = "unknown",
    private val signerVerified: Boolean = false,
    private val experimentalShelfEnabled: Boolean = false,
) {
    private val transactionCoordinator = Ew300TransactionCoordinator(transport)
    @Volatile
    private var lastSuccessfulFlashBaseline: Ew300RawBaseline? = null
    @Volatile
    private var pendingVerification: PendingVerification? = null

    val lastOperationTrace: StateFlow<Ew300OperationTrace?> = traceStore.lastTrace
    val operationStatus: StateFlow<Ew300OperationStatus> = traceStore.status
    private val mutableRuntimePersistence = MutableStateFlow(Ew300RuntimePersistenceState.UNKNOWN)
    val runtimePersistence: StateFlow<Ew300RuntimePersistenceState> = mutableRuntimePersistence
    @Volatile
    private var pendingRuntimeTarget: Ew300RuntimeTarget? = null

    suspend fun applyEditorWorkingCopy(
        workingCopy: com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
    ): Ew300EditorApplyResult = record("APPLY") { trace ->
        lastSuccessfulFlashBaseline = null
        trace.stage(Ew300OperationStage.AUTHORIZED_SESSION)
        val result = applyEditorWorkingCopyInternal(workingCopy, allowCautions, isSessionCurrent, trace)
        if (result == Ew300EditorApplyResult.Verified) {
            trace.stage(Ew300OperationStage.FINAL_READBACK)
        }
        result
    }

    private suspend fun applyEditorWorkingCopyInternal(
        workingCopy: com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
        trace: Ew300OperationTraceBuilder,
    ): Ew300EditorApplyResult {
        val deviceKey = transport.deviceFingerprintKey
            ?: return Ew300EditorApplyResult.DeviceUnavailable("The exact EW300 device fingerprint is unavailable.")
        if (!mutationAuthorized(deviceKey)) {
            return Ew300EditorApplyResult.InvalidPlan(
                "EW300 editor Apply is unavailable for this device identity or hardware profile.",
            )
        }
        trace.stage(Ew300OperationStage.BASELINE_CAPTURED)
        return Ew300EditorApplier(transport, experimentalShelfEnabled).apply(
            workingCopy = workingCopy,
            allowCautions = allowCautions,
            isSessionCurrent = isSessionCurrent,
            beforeFirstWrite = {
                trace.markBeforeFirstWrite(transport.permissionRequestCount)
                trace.stage(Ew300OperationStage.WRITING)
            },
            beforeSave = { trace.stage(Ew300OperationStage.VOLATILE_VERIFIED) },
            afterSave = { observation ->
                trace.stage(Ew300OperationStage.SAVE_SENT_ONCE)
                if (observation.replacementObserved) {
                    trace.stage(Ew300OperationStage.WAITING_FOR_REPLACEMENT)
                    if (observation.replacementIdentityMatched) {
                        trace.stage(Ew300OperationStage.REPLACEMENT_IDENTITY_VERIFIED)
                        trace.stage(Ew300OperationStage.REPLACEMENT_AUTHORIZED)
                    }
                } else if (observation.replacementIdentityMatched) {
                    trace.stage(Ew300OperationStage.SAME_SESSION_READBACK)
                }
            },
        )
    }

    /**
     * Experimental runtime-only editor Apply. Every write is read back in the same session and
     * zero Save commands are sent. The verified target remains pending for an explicit Save.
     */
    suspend fun applyEditorRuntime(
        workingCopy: com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
    ): Ew300RuntimeApplyResult = record("APPLY_RUNTIME") { trace ->
        lastSuccessfulFlashBaseline = null
        trace.stage(Ew300OperationStage.AUTHORIZED_SESSION)
        val deviceKey = transport.deviceFingerprintKey
            ?: return@record Ew300RuntimeApplyResult.DeviceUnavailable(
                "The exact EW300 device fingerprint is unavailable.",
            )
        if (!mutationAuthorized(deviceKey)) {
            return@record Ew300RuntimeApplyResult.InvalidPlan(
                "EW300 editor Apply is unavailable for this device identity or hardware profile.",
            )
        }
        trace.stage(Ew300OperationStage.BASELINE_CAPTURED)
        val result = Ew300EditorApplier(transport, experimentalShelfEnabled).applyRuntime(
            workingCopy = workingCopy,
            allowCautions = allowCautions,
            isSessionCurrent = isSessionCurrent,
            beforeFirstWrite = {
                trace.markBeforeFirstWrite(transport.permissionRequestCount)
                trace.stage(Ew300OperationStage.WRITING)
            },
        )
        when (result) {
            is Ew300RuntimeApplyResult.Verified -> {
                pendingRuntimeTarget = result.target
                mutableRuntimePersistence.value = Ew300RuntimePersistenceState.RUNTIME_ONLY
                trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
                trace.stage(Ew300OperationStage.FINAL_READBACK)
                transport.markRuntimeVerified()
            }
            else -> pendingRuntimeTarget = null
        }
        result
    }

    /**
     * Runtime-only absolute output-gain change. It never sends Save; the pending EQ/gain target is
     * offered to the explicit Save action. The tracked EQ delta is rebased so a later profile Flash
     * cannot subtract stale compensation twice.
     */
    suspend fun setPlaybackGainRuntime(gainDb: Double): Ew300PlaybackGainResult = record("SET_GAIN_RUNTIME") { trace ->
        trace.stage(Ew300OperationStage.AUTHORIZED_SESSION)
        val deviceKey = transport.deviceFingerprintKey
            ?: return@record Ew300PlaybackGainResult.DeviceUnavailable(
                "The exact EW300 device fingerprint is unavailable; no gain write was sent.",
            )
        if (!mutationAuthorized(deviceKey)) {
            return@record Ew300PlaybackGainResult.NotSuitable(
                "EW300 output gain is unavailable for this exact device identity.",
            )
        }
        val requestedSteps = runCatching {
            Ew300DeviceControls.playbackGainDescriptor.validateForWrite(
                com.weekssa.opraeqforuapp.domain.dac.DacControlValue.Numeric(gainDb),
            )
        }.getOrElse {
            return@record Ew300PlaybackGainResult.NotSuitable(
                "The requested EW300 output gain is not representable safely.",
            )
        }.let { validation ->
            if (validation !is com.weekssa.opraeqforuapp.domain.dac.DacControlValidation.Valid) {
                return@record Ew300PlaybackGainResult.NotSuitable(
                    "Choose an EW300 output gain from -64.0 dB through 0.0 dB in 0.5 dB steps.",
                )
            }
            Ew300Protocol.gainDbToSteps(gainDb)
        }
        val baseline = transactionCoordinator.captureBaseline()
            ?: return@record Ew300PlaybackGainResult.DeviceUnavailable(
                "Could not read the complete EW300 state before changing output gain.",
            )
        trace.recordBaseline(baseline)
        val currentGain = baseline.value(Ew300Protocol.GLOBAL_GAIN_REGISTER)
            ?: return@record Ew300PlaybackGainResult.DeviceUnavailable(
                "Could not read the EW300 output gain before changing it.",
            )
        val protocolFlags = baseline.value(Ew300Protocol.PROTOCOL_FLAGS_REGISTER)
            ?: return@record Ew300PlaybackGainResult.DeviceUnavailable(
                "Could not read the EW300 protocol layout before changing output gain.",
            )
        val currentSteps = Ew300Protocol.globalGainSteps(currentGain, protocolFlags)
            ?: return@record Ew300PlaybackGainResult.NotSuitable(
                "EW300 stereo output-gain channels are unequal. Refresh or reconnect before changing gain.",
            )
        val targetGain = Ew300Protocol.withGlobalGainSteps(currentGain, requestedSteps, protocolFlags)
        if (currentGain.contentEquals(targetGain)) {
            // A verified no-op absolute set still rebases the tracked EQ delta. Leaving a stale
            // compensation here would make a later profile Flash read the wrong user baseline.
            pendingRuntimeTarget = runtimeTargetFromBaseline(baseline, currentGain, requestedSteps)
            gainStateStore.writeAppliedGainDeltaSteps(deviceKey, 0)
            gainStateStore.writeUserBaselineGainSteps(deviceKey, requestedSteps)
            mutableRuntimePersistence.value = Ew300RuntimePersistenceState.RUNTIME_ONLY
            trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
            trace.stage(Ew300OperationStage.FINAL_READBACK)
            transport.markRuntimeVerified()
            return@record Ew300PlaybackGainResult.Success(requestedSteps / Ew300Protocol.GLOBAL_GAIN_STEPS_PER_DB)
        }
        trace.markBeforeFirstWrite(transport.permissionRequestCount)
        trace.stage(Ew300OperationStage.WRITING)
        if (!transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, targetGain)) {
            pendingRuntimeTarget = null
            gainStateStore.writeAppliedGainDeltaSteps(deviceKey, 0)
            gainStateStore.writeUserBaselineGainSteps(deviceKey, currentSteps)
            mutableRuntimePersistence.value = Ew300RuntimePersistenceState.UNKNOWN
            return@record Ew300PlaybackGainResult.TransferFailed(
                "EW300 did not accept the output-gain change. No Save command was sent.",
            )
        }
        val volatileGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        if (volatileGain == null || !volatileGain.contentEquals(targetGain)) {
            pendingRuntimeTarget = null
            gainStateStore.writeAppliedGainDeltaSteps(deviceKey, 0)
            gainStateStore.writeUserBaselineGainSteps(deviceKey, currentSteps)
            mutableRuntimePersistence.value = Ew300RuntimePersistenceState.UNKNOWN
            return@record Ew300PlaybackGainResult.VerificationFailed(
                "EW300 output-gain readback did not match the requested value.",
            )
        }
        pendingRuntimeTarget = runtimeTargetFromBaseline(baseline, targetGain, requestedSteps)
        gainStateStore.writeAppliedGainDeltaSteps(deviceKey, 0)
        gainStateStore.writeUserBaselineGainSteps(deviceKey, requestedSteps)
        mutableRuntimePersistence.value = Ew300RuntimePersistenceState.RUNTIME_ONLY
        trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
        trace.stage(Ew300OperationStage.FINAL_READBACK)
        transport.markRuntimeVerified()
        return@record Ew300PlaybackGainResult.Success(requestedSteps / Ew300Protocol.GLOBAL_GAIN_STEPS_PER_DB)
    }

    /**
     * Explicit one-Save persistence of the exact runtime target verified in this session.
     * A fresh comparison precedes the single Save; an uncertain Save is never replayed.
     */
    suspend fun saveRuntime(): Ew300RuntimeSaveResult = record("SAVE_RUNTIME") { trace ->
        trace.stage(Ew300OperationStage.AUTHORIZED_SESSION)
        val target = pendingRuntimeTarget
            ?: return@record Ew300RuntimeSaveResult.NotSuitable(
                "No verified runtime change is pending in this session. Read the DAC and reapply before saving.",
            )
        val deviceKey = transport.deviceFingerprintKey
        if (deviceKey == null || deviceKey != target.deviceFingerprintKey ||
            transport.sessionGeneration != target.sessionGeneration
        ) {
            pendingRuntimeTarget = null
            mutableRuntimePersistence.value = Ew300RuntimePersistenceState.UNKNOWN
            return@record Ew300RuntimeSaveResult.NotSuitable(
                "The runtime target belongs to an earlier USB session. Read the DAC again; no Save was sent.",
            )
        }
        val currentBands = readWireBands()
        val currentGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        val protocolFlags = transport.readRegister(Ew300Protocol.PROTOCOL_FLAGS_REGISTER)
        val expectedBands = expectedRuntimeBands(target)
        if (currentBands != expectedBands || currentGain == null || protocolFlags == null ||
            !Ew300Protocol.globalGainMatches(currentGain, target.gainSteps, protocolFlags)
        ) {
            pendingRuntimeTarget = null
            mutableRuntimePersistence.value = Ew300RuntimePersistenceState.RUNTIME_ONLY
            return@record Ew300RuntimeSaveResult.NotSuitable(
                "The live EW300 state changed after the runtime Apply. No Save was sent; read the DAC again.",
            )
        }
        trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
        mutableRuntimePersistence.value = Ew300RuntimePersistenceState.SAVING
        val sessionGenerationBeforeSave = transport.sessionGeneration
        val detachGenerationBeforeSave = transport.detachGeneration
        if (!transport.commit()) {
            pendingRuntimeTarget = null
            mutableRuntimePersistence.value = Ew300RuntimePersistenceState.SAVE_NOT_VERIFIED
            return@record Ew300RuntimeSaveResult.TransferFailed(
                "EW300 Save was not confirmed. It may have completed; reconnect and read the DAC before any retry.",
            )
        }
        trace.stage(Ew300OperationStage.SAVE_SENT_ONCE)
        if (!recordCommitBoundary(
                trace = trace,
                expectedFingerprint = deviceKey,
                sessionGenerationBeforeSave = sessionGenerationBeforeSave,
                detachGenerationBeforeSave = detachGenerationBeforeSave,
            )
        ) {
            pendingRuntimeTarget = null
            mutableRuntimePersistence.value = Ew300RuntimePersistenceState.SAVE_NOT_VERIFIED
            return@record Ew300RuntimeSaveResult.VerificationFailed(
                "EW300 Save returned on an unverified USB session. The Save may have completed; stop and read before any later write.",
            )
        }
        trace.stage(Ew300OperationStage.FINAL_READBACK)
        val finalBands = Ew300ReadbackRetry.read(
            read = { readWireBands() },
            matches = { it == expectedBands },
        )
        val finalGain = Ew300ReadbackRetry.read(
            read = { transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER) },
            matches = { gain -> gain != null && Ew300Protocol.globalGainMatches(gain, target.gainSteps, protocolFlags) },
        )
        if (finalBands != expectedBands || finalGain == null ||
            !Ew300Protocol.globalGainMatches(finalGain, target.gainSteps, protocolFlags)
        ) {
            pendingRuntimeTarget = null
            mutableRuntimePersistence.value = Ew300RuntimePersistenceState.SAVE_NOT_VERIFIED
            return@record Ew300RuntimeSaveResult.VerificationFailed(
                "EW300 Save could not be verified by final readback. Read the DAC before any later action.",
            )
        }
        pendingRuntimeTarget = null
        gainStateStore.writeAppliedGainDeltaSteps(deviceKey, 0)
        gainStateStore.writeUserBaselineGainSteps(deviceKey, target.gainSteps)
        mutableRuntimePersistence.value = Ew300RuntimePersistenceState.SAVED_THIS_SESSION
        return@record Ew300RuntimeSaveResult.Verified
    }

    /** Parity-only fixed A/B shelf state; zero Save and exact volatile readback. */
    suspend fun applyShelfTestTarget(target: Ew300ShelfTestTarget): Ew300ShelfTestResult =
        record("SHELF_TEST") { trace ->
            trace.stage(Ew300OperationStage.AUTHORIZED_SESSION)
            val deviceKey = transport.deviceFingerprintKey
                ?: return@record Ew300ShelfTestResult.DeviceUnavailable(
                    "The exact EW300 device fingerprint is unavailable.",
                )
            if (!experimentalShelfEnabled) {
                return@record Ew300ShelfTestResult.NotSuitable(
                    "Native shelf A/B states are available only in the parity package.",
                )
            }
            if (!mutationAuthorized(deviceKey)) {
                return@record Ew300ShelfTestResult.NotSuitable(
                    "EW300 shelf A/B states are unavailable for this exact device identity.",
                )
            }
            val baseline = transactionCoordinator.captureBaseline()
                ?: return@record Ew300ShelfTestResult.DeviceUnavailable(
                    "Could not read the complete EW300 state before the shelf A/B target.",
                )
            val targetBands = target.bands()
            val wires = targetBands.map { band ->
                runCatching {
                    Ew300Protocol.encodeBand(band, allowExperimentalShelf = true)
                }.getOrElse {
                    return@record Ew300ShelfTestResult.NotSuitable(
                        it.message ?: "The shelf A/B target is not representable.",
                    )
                }.let { (gain, q) -> Ew300WireBandPair(gain, q) }
            }
            trace.markBeforeFirstWrite(transport.permissionRequestCount)
            trace.stage(Ew300OperationStage.WRITING)
            wires.forEachIndexed { index, wire ->
                if (!transport.writeRegister(Ew300Protocol.bandRegister(index), wire.gain) ||
                    !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, wire.q)
                ) {
                    return@record if (transactionCoordinator.restoreVolatile(baseline)) {
                        Ew300ShelfTestResult.TransferFailed(
                            "EW300 stopped accepting the shelf A/B target; the original state was restored.",
                        )
                    } else {
                        Ew300ShelfTestResult.TransferFailed(
                            "EW300 stopped accepting the shelf A/B target and restoration failed; reconnect and read before any retry.",
                        )
                    }
                }
            }
            if (readWireBands() != wires) {
                return@record if (transactionCoordinator.restoreVolatile(baseline)) {
                    Ew300ShelfTestResult.VerificationFailed(
                        "EW300 shelf A/B readback did not match; the original state was restored.",
                    )
                } else {
                    Ew300ShelfTestResult.VerificationFailed(
                        "EW300 shelf A/B readback did not match and restoration failed; reconnect and read.",
                    )
                }
            }
            trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
            trace.stage(Ew300OperationStage.FINAL_READBACK)
            transport.markRuntimeVerified()
            Ew300ShelfTestResult.Verified(target)
        }

    suspend fun flash(profile: OpraEqProfile): Kt02h20FlashResult = record("FLASH") { trace ->
        lastSuccessfulFlashBaseline = null
        trace.stage(Ew300OperationStage.AUTHORIZED_SESSION)
        flashInternal(profile, trace)
    }

    /**
     * Restores the complete baseline captured by the last verified Flash.
     *
     * This is intentionally a separate transaction: Flash and restoration each have at most one
     * Save, their evidence remains truthful, and an uncertain restoration is never replayed.
     * The signed validation candidate is the only caller exposed to the owner; ordinary builds do
     * not surface this action.
     */
    suspend fun restoreLastFlashBaseline(): Ew300RestorationResult = record("RESTORE") { trace ->
        val baseline = lastSuccessfulFlashBaseline
        lastSuccessfulFlashBaseline = null
        trace.stage(Ew300OperationStage.AUTHORIZED_SESSION)
        if (baseline == null) {
            return@record Ew300RestorationResult.NoBaseline(
                "No verified EW300 Flash baseline is available for restoration. No operation was sent.",
            )
        }
        restoreExactBaselineInternal(baseline, trace)
    }

    private suspend fun flashInternal(
        profile: OpraEqProfile,
        trace: Ew300OperationTraceBuilder,
    ): Kt02h20FlashResult {
        val deviceKey = transport.deviceFingerprintKey
            ?: return Kt02h20FlashResult.DeviceUnavailable("The exact EW300 device fingerprint is unavailable.")
        if (!mutationAuthorized(deviceKey)) {
            return Kt02h20FlashResult.NotSuitable(
                "Persistent EW300 Flash is unavailable for this device identity or hardware profile.",
            )
        }
        val hasNativeShelf = experimentalShelfEnabled && profile.bands.orEmpty().any {
            it.type == "low_shelf" || it.type == "high_shelf"
        }
        // A shelf profile inside the prequalified envelope keeps its native type 3/4 words.
        // Anything else is fitted to the five Peak bands with the shared SIMGOT_EW300 optimizer,
        // so a library profile never fails only because its shelf sits outside that envelope.
        val nativeShelf = if (hasNativeShelf) nativeShelfRepresentation(profile) else null
        val representation = nativeShelf ?: when (
            val result = Kt02h20FiveBandOptimizer.optimize(profile, HardwareEqDeviceSpecs.SIMGOT_EW300)
        ) {
            is FiveBandOptimizationResult.NotSuitable -> return Kt02h20FlashResult.NotSuitable(result.reason)
            is FiveBandOptimizationResult.Ready -> result.representation
        }
        val target = completeBands(representation.bands)
        val baseline = transactionCoordinator.captureBaseline()
        val currentGain = baseline?.value(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        val baselineBands = baseline?.bands()
        if (baseline == null || currentGain == null || baselineBands == null || baselineBands.size != Ew300Protocol.BAND_COUNT) {
            return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t read the EW300 PEQ state. Reconnect the DAC and try again.",
            )
        }
        trace.recordBaseline(baseline)
        val protocolFlags = baseline.value(Ew300Protocol.PROTOCOL_FLAGS_REGISTER)
            ?: return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t read the EW300 protocol layout. Reconnect the DAC and try again.",
            )
        val currentSteps = Ew300Protocol.globalGainSteps(currentGain, protocolFlags)
            ?: return Kt02h20FlashResult.NotSuitable(
                "EW300 stereo playback-gain channels are unequal. No write was sent; refresh or reconnect the DAC before trying again.",
            )
        val baselineSteps = currentSteps - gainStateStore.readAppliedGainDeltaSteps(deviceKey)
        val requestedDeltaSteps = runCatching {
            Ew300Protocol.gainDbToSteps(representation.playbackGainDb)
        }.getOrElse {
            return Kt02h20FlashResult.NotSuitable(
                "The requested EW300 playback-gain adjustment is outside the qualified range.",
            )
        }
        val targetGainSteps = baselineSteps + requestedDeltaSteps
        if (targetGainSteps !in Ew300Protocol.GLOBAL_GAIN_MIN_STEPS..Ew300Protocol.GLOBAL_GAIN_MAX_STEPS) {
            return Kt02h20FlashResult.NotSuitable(
                "Applying ${formatDb(representation.playbackGainDb)} dB of EW300 playback gain would exceed the qualified device range.",
            )
        }
        val targetWires = target.map { band ->
            runCatching {
                Ew300Protocol.encodeBand(band, allowExperimentalShelf = experimentalShelfEnabled)
            }.getOrElse {
                return Kt02h20FlashResult.NotSuitable(it.message ?: "EW300 band is not representable.")
            }
        }
        val targetGain = Ew300Protocol.withGlobalGainSteps(currentGain, targetGainSteps, protocolFlags)
        trace.markBeforeFirstWrite(transport.permissionRequestCount)
        trace.stage(Ew300OperationStage.WRITING)
        if (!currentGain.contentEquals(targetGain) &&
            !transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, targetGain)
        ) {
            return restoreBeforeCommit(
                baseline = baseline,
                reason = "EW300 did not accept the required global-gain adjustment.",
                trace = trace,
            )
        }
        targetWires.forEachIndexed { index, (gain, q) ->
            if (!transport.writeRegister(Ew300Protocol.bandRegister(index), gain) ||
                !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, q)
            ) {
                return restoreBeforeCommit(
                    baseline = baseline,
                    reason = "EW300 stopped accepting PEQ data at band ${index + 1} of ${Ew300Protocol.BAND_COUNT}.",
                    trace = trace,
                )
            }
        }
        if (verify(target, allowExperimentalShelf = experimentalShelfEnabled) != null) {
            return restoreBeforeCommit(
                baseline = baseline,
                reason = "EW300 volatile PEQ readback did not match before Save.",
                trace = trace,
            )
        }
        trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
        val sessionGenerationBeforeSave = transport.sessionGeneration
        val detachBeforeSave = transport.detachGeneration
        pendingVerification = PendingVerification(
            operationId = trace.id,
            operation = "FLASH",
            deviceFingerprintKey = deviceKey,
            sessionGenerationBeforeSave = sessionGenerationBeforeSave,
            detachGenerationBeforeSave = detachBeforeSave,
            expectedRegisters = expectedRegisters(targetWires, targetGain),
            targetDiffersFromBaseline = targetWires.withIndex().any { (index, wire) ->
                val (gain, q) = wire
                baseline.value(Ew300Protocol.bandRegister(index))?.contentEquals(gain) != true ||
                    baseline.value(Ew300Protocol.bandRegister(index) + 1)?.contentEquals(q) != true
            } || !baseline.matches(Ew300Protocol.GLOBAL_GAIN_REGISTER, targetGain),
            appliedGainDeltaSteps = requestedDeltaSteps,
        )
        if (!transport.commit()) {
            return Kt02h20FlashResult.TransferFailed(
                "EW300 accepted the PEQ writes but did not accept the persistence command.",
            )
        }
        trace.stage(Ew300OperationStage.SAVE_SENT_ONCE)
        if (!recordCommitBoundary(
                trace = trace,
                expectedFingerprint = deviceKey,
                sessionGenerationBeforeSave = sessionGenerationBeforeSave,
                detachGenerationBeforeSave = detachBeforeSave,
            )
        ) {
            return Kt02h20FlashResult.VerificationFailed(
                "EW300 Save returned on a different or unverified USB session. No readback was accepted.",
            )
        }
        trace.stage(Ew300OperationStage.FINAL_READBACK)
        val finalReadback = reconcileFinalReadback(target, targetGain, experimentalShelfEnabled)
        if (finalReadback.bandFailure != null) {
            val verificationFailure = finalReadback.bandFailure
            return Kt02h20FlashResult.VerificationFailed(
                "EW300 final PEQ readback did not match band ${verificationFailure.band + 1}: " +
                    "expected ${verificationFailure.expected}, read ${verificationFailure.actual}.",
            )
        }
        if (finalReadback.gain == null || !finalReadback.gain.contentEquals(targetGain)) {
            return Kt02h20FlashResult.VerificationFailed(
                "EW300 final global-gain readback did not match the requested adjustment.",
            )
        }
        gainStateStore.writeAppliedGainDeltaSteps(deviceKey, requestedDeltaSteps)
        lastSuccessfulFlashBaseline = baseline
        pendingVerification = null
        return Kt02h20FlashResult.Success(
            representation = representation,
            explicitPersistenceCommandUsed = true,
        )
    }

    suspend fun resetToFlat(): Kt02h20FlatResetResult = record("RESET") { trace ->
        lastSuccessfulFlashBaseline = null
        trace.stage(Ew300OperationStage.AUTHORIZED_SESSION)
        resetToFlatInternal(trace)
    }

    /**
     * Changes only the EW300's absolute playback/global gain and verifies the persisted result.
     *
     * The requested value is the resulting hardware gain, not an EQ headroom delta. The existing
     * EQ delta is retained separately so a later Flash or Reset can restore the user's baseline
     * without double-applying or erasing this control change.
     */
    suspend fun setPlaybackGain(gainDb: Double): Ew300PlaybackGainResult = record("GAIN") { trace ->
        trace.stage(Ew300OperationStage.AUTHORIZED_SESSION)
        val deviceKey = transport.deviceFingerprintKey
            ?: return@record Ew300PlaybackGainResult.DeviceUnavailable(
                "The exact EW300 device fingerprint is unavailable; no gain write was sent.",
            )
        if (!mutationAuthorized(deviceKey)) {
            return@record Ew300PlaybackGainResult.NotSuitable(
                "EW300 playback gain is unavailable for this exact device identity.",
            )
        }
        val requestedSteps = runCatching {
            Ew300DeviceControls.playbackGainDescriptor.validateForWrite(
                com.weekssa.opraeqforuapp.domain.dac.DacControlValue.Numeric(gainDb),
            )
        }.getOrElse {
            return@record Ew300PlaybackGainResult.NotSuitable(
                "The requested EW300 playback gain is not representable safely.",
            )
        }.let { validation ->
            if (validation !is com.weekssa.opraeqforuapp.domain.dac.DacControlValidation.Valid) {
                return@record Ew300PlaybackGainResult.NotSuitable(
                    "Choose an EW300 playback gain from -64.0 dB through 0.0 dB in 0.5 dB steps.",
                )
            }
            Ew300Protocol.gainDbToSteps(gainDb)
        }
        val baseline = transactionCoordinator.captureBaseline()
            ?: return@record Ew300PlaybackGainResult.DeviceUnavailable(
                "Couldn’t read the complete EW300 state before changing playback gain.",
            )
        trace.recordBaseline(baseline)
        val currentGain = baseline.value(Ew300Protocol.GLOBAL_GAIN_REGISTER)
            ?: return@record Ew300PlaybackGainResult.DeviceUnavailable(
                "Couldn’t read the EW300 playback gain before changing it.",
            )
        val protocolFlags = baseline.value(Ew300Protocol.PROTOCOL_FLAGS_REGISTER)
            ?: return@record Ew300PlaybackGainResult.DeviceUnavailable(
                "Couldn’t read the EW300 protocol layout before changing playback gain.",
            )
        val currentSteps = Ew300Protocol.globalGainSteps(currentGain, protocolFlags)
            ?: return@record Ew300PlaybackGainResult.NotSuitable(
                "EW300 stereo playback-gain channels are unequal. Refresh or reconnect before changing gain.",
            )
        val appliedDeltaSteps = gainStateStore.readAppliedGainDeltaSteps(deviceKey)
        val userBaselineSteps = currentSteps - appliedDeltaSteps
        if (userBaselineSteps !in Ew300Protocol.GLOBAL_GAIN_MIN_STEPS..Ew300Protocol.GLOBAL_GAIN_MAX_STEPS) {
            return@record Ew300PlaybackGainResult.NotSuitable(
                "The current EW300 EQ/gain baseline is outside the safe control range; no write was sent.",
            )
        }
        val targetGain = Ew300Protocol.withGlobalGainSteps(currentGain, requestedSteps, protocolFlags)
        if (currentGain.contentEquals(targetGain)) {
            gainStateStore.writeUserBaselineGainSteps(deviceKey, requestedSteps - appliedDeltaSteps)
            trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
            trace.stage(Ew300OperationStage.FINAL_READBACK)
            return@record Ew300PlaybackGainResult.Success(requestedSteps / Ew300Protocol.GLOBAL_GAIN_STEPS_PER_DB)
        }

        trace.markBeforeFirstWrite(transport.permissionRequestCount)
        trace.stage(Ew300OperationStage.WRITING)
        if (!transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, targetGain)) {
            return@record Ew300PlaybackGainResult.TransferFailed(
                "EW300 did not accept the playback-gain change. No Save command was sent.",
            )
        }
        val volatileGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        if (volatileGain == null || !volatileGain.contentEquals(targetGain)) {
            return@record Ew300PlaybackGainResult.VerificationFailed(
                "EW300 playback-gain readback did not match before Save.",
            )
        }
        trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
        val sessionGenerationBeforeSave = transport.sessionGeneration
        val detachGenerationBeforeSave = transport.detachGeneration
        pendingVerification = PendingVerification(
            operationId = trace.id,
            operation = "GAIN",
            deviceFingerprintKey = deviceKey,
            sessionGenerationBeforeSave = sessionGenerationBeforeSave,
            detachGenerationBeforeSave = detachGenerationBeforeSave,
            expectedRegisters = mapOf(Ew300Protocol.GLOBAL_GAIN_REGISTER to targetGain.copyOf()),
            targetDiffersFromBaseline = true,
            appliedGainDeltaSteps = appliedDeltaSteps,
            userBaselineGainSteps = requestedSteps - appliedDeltaSteps,
        )
        if (!transport.commit()) {
            return@record Ew300PlaybackGainResult.TransferFailed(
                "EW300 accepted the playback-gain write but did not accept the persistence command. Stop and reconnect before retrying.",
            )
        }
        trace.stage(Ew300OperationStage.SAVE_SENT_ONCE)
        if (!recordCommitBoundary(
                trace = trace,
                expectedFingerprint = deviceKey,
                sessionGenerationBeforeSave = sessionGenerationBeforeSave,
                detachGenerationBeforeSave = detachGenerationBeforeSave,
            )
        ) {
            return@record Ew300PlaybackGainResult.VerificationFailed(
                "EW300 Save returned on an unverified USB session. No later write is safe until refresh or reconnect.",
            )
        }
        trace.stage(Ew300OperationStage.FINAL_READBACK)
        val finalGain = Ew300ReadbackRetry.read(
            read = { transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER) },
            matches = { it?.contentEquals(targetGain) == true },
        )
        if (finalGain == null || !finalGain.contentEquals(targetGain)) {
            return@record Ew300PlaybackGainResult.VerificationFailed(
                "EW300 final playback-gain readback did not match the requested setting.",
            )
        }
        gainStateStore.writeUserBaselineGainSteps(deviceKey, requestedSteps - appliedDeltaSteps)
        pendingVerification = null
        return@record Ew300PlaybackGainResult.Success(requestedSteps / Ew300Protocol.GLOBAL_GAIN_STEPS_PER_DB)
    }

    private suspend fun resetToFlatInternal(trace: Ew300OperationTraceBuilder): Kt02h20FlatResetResult {
        val flat = completeBands(emptyList())
        val deviceKey = transport.deviceFingerprintKey
            ?: return Kt02h20FlatResetResult.DeviceUnavailable("The exact EW300 device fingerprint is unavailable.")
        if (!mutationAuthorized(deviceKey)) {
            return Kt02h20FlatResetResult.NotSuitable(
                "EW300 Reset is unavailable for this device identity or hardware profile.",
            )
        }
        val baseline = transactionCoordinator.captureBaseline()
            ?: return Kt02h20FlatResetResult.DeviceUnavailable("Couldn’t read the complete EW300 state before reset.")
        val currentGain = baseline.value(Ew300Protocol.GLOBAL_GAIN_REGISTER)
            ?: return Kt02h20FlatResetResult.DeviceUnavailable("Couldn’t read the EW300 global gain before reset.")
        val protocolFlags = baseline.value(Ew300Protocol.PROTOCOL_FLAGS_REGISTER)
            ?: return Kt02h20FlatResetResult.DeviceUnavailable("Couldn’t read the EW300 protocol layout before reset.")
        val currentSteps = Ew300Protocol.globalGainSteps(currentGain, protocolFlags)
            ?: return Kt02h20FlatResetResult.NotSuitable(
                "EW300 stereo playback-gain channels are unequal. No write was sent; refresh or reconnect the DAC before trying again.",
            )
        val baselineSteps = currentSteps - gainStateStore.readAppliedGainDeltaSteps(deviceKey)
        if (baselineSteps !in Ew300Protocol.GLOBAL_GAIN_MIN_STEPS..Ew300Protocol.GLOBAL_GAIN_MAX_STEPS) {
            return Kt02h20FlatResetResult.NotSuitable("The EW300 baseline global gain is outside the qualified range.")
        }
        val baselineGain = Ew300Protocol.withGlobalGainSteps(currentGain, baselineSteps, protocolFlags)
        val baselineBands = baseline.bands()
        if (baselineBands.size != Ew300Protocol.BAND_COUNT) {
            return Kt02h20FlatResetResult.DeviceUnavailable("Couldn’t read the complete EW300 EQ before reset.")
        }
        trace.recordBaseline(baseline)
        trace.markBeforeFirstWrite(transport.permissionRequestCount)
        trace.stage(Ew300OperationStage.WRITING)
        flat.forEachIndexed { index, band ->
            val (gain, q) = Ew300Protocol.encodeBand(band)
            if (!transport.writeRegister(Ew300Protocol.bandRegister(index), gain) ||
                !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, q)
            ) {
                return restoreBeforeCommit(
                    baseline = baseline,
                    reason = "EW300 stopped accepting the flat-EQ reset at band ${index + 1}.",
                    trace = trace,
                ).toFlatResetResult()
            }
        }
        if (!currentGain.contentEquals(baselineGain) &&
            !transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, baselineGain)
        ) {
            return restoreBeforeCommit(
                baseline = baseline,
                reason = "EW300 flat EQ was prepared, but its prior gain could not be restored.",
                trace = trace,
            ).toFlatResetResult()
        }
        if (verify(flat) != null) {
            return restoreBeforeCommit(
                baseline = baseline,
                reason = "EW300 volatile flat-EQ readback did not match before Save.",
                trace = trace,
            ).toFlatResetResult()
        }
        trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
        val sessionGenerationBeforeSave = transport.sessionGeneration
        val detachBeforeSave = transport.detachGeneration
        pendingVerification = PendingVerification(
            operationId = trace.id,
            operation = "RESET",
            deviceFingerprintKey = deviceKey,
            sessionGenerationBeforeSave = sessionGenerationBeforeSave,
            detachGenerationBeforeSave = detachBeforeSave,
            expectedRegisters = expectedRegisters(
                flat.map { band -> Ew300Protocol.encodeBand(band) },
                baselineGain,
            ),
            targetDiffersFromBaseline = flat.withIndex().any { (index, band) ->
                val (gain, q) = Ew300Protocol.encodeBand(band)
                baseline.value(Ew300Protocol.bandRegister(index))?.contentEquals(gain) != true ||
                    baseline.value(Ew300Protocol.bandRegister(index) + 1)?.contentEquals(q) != true
            } || !baseline.matches(Ew300Protocol.GLOBAL_GAIN_REGISTER, baselineGain),
            appliedGainDeltaSteps = 0,
        )
        if (!transport.commit()) {
            return Kt02h20FlatResetResult.TransferFailed("EW300 did not accept the flat-EQ persistence command.")
        }
        trace.stage(Ew300OperationStage.SAVE_SENT_ONCE)
        if (!recordCommitBoundary(
                trace = trace,
                expectedFingerprint = deviceKey,
                sessionGenerationBeforeSave = sessionGenerationBeforeSave,
                detachGenerationBeforeSave = detachBeforeSave,
            )
        ) {
            return Kt02h20FlatResetResult.VerificationFailed(
                "EW300 Save returned on a different or unverified USB session. No readback was accepted.",
            )
        }
        trace.stage(Ew300OperationStage.FINAL_READBACK)
        val finalReadback = reconcileFinalReadback(flat, baselineGain)
        if (finalReadback.bandFailure != null) {
            return Kt02h20FlatResetResult.VerificationFailed("EW300 final flat-EQ readback did not match.")
        }
        if (finalReadback.gain == null || !finalReadback.gain.contentEquals(baselineGain)) {
            return Kt02h20FlatResetResult.VerificationFailed("EW300 final flat-EQ gain readback did not match.")
        }
        gainStateStore.writeAppliedGainDeltaSteps(deviceKey, 0)
        pendingVerification = null
        return Kt02h20FlatResetResult.Success(restoredPlaybackGainDb = 0.0, explicitPersistenceCommandUsed = true)
    }

    /**
     * Completes an operation whose Save/reconnect window ended uncertainly.
     *
     * This is deliberately read-only. It requires the exact identity, both a newer USB session
     * and a newer observed detach, then captures every EW300 snapshot register and compares the
     * encoded target bytes. It never retries a write or Save command.
     */
    suspend fun reconcilePendingVerification(): Boolean {
        val pending = pendingVerification ?: return false
        val trace = lastOperationTrace.value ?: return false
        if (trace.operationId != pending.operationId || trace.operation != pending.operation || trace.stateKnown) return false
        val currentFingerprint = transport.deviceFingerprintKey ?: return false
        val currentSession = transport.sessionGeneration
        val currentDetach = transport.detachGeneration
        if (currentFingerprint != pending.deviceFingerprintKey ||
            currentSession <= pending.sessionGenerationBeforeSave ||
            currentDetach <= pending.detachGenerationBeforeSave ||
            !pending.targetDiffersFromBaseline
        ) {
            return false
        }
        val readback = transactionCoordinator.captureBaseline() ?: return false
        if (readback.deviceFingerprintKey != pending.deviceFingerprintKey ||
            readback.sessionGeneration != currentSession ||
            readback.detachGeneration != currentDetach ||
            !pending.expectedRegisters.all { (register, expected) -> readback.matches(register, expected) }
        ) {
            return false
        }
        gainStateStore.writeAppliedGainDeltaSteps(
            pending.deviceFingerprintKey,
            pending.appliedGainDeltaSteps,
        )
        pending.userBaselineGainSteps?.let { steps ->
            gainStateStore.writeUserBaselineGainSteps(pending.deviceFingerprintKey, steps)
        }
        val reconciled = trace.copy(
            deviceFingerprintKey = currentFingerprint,
            sessionGeneration = currentSession,
            detachGeneration = currentDetach,
            replacementObserved = true,
            replacementIdentityMatched = true,
            finalReadbackMatched = true,
            stateKnown = true,
            outcome = Ew300OperationOutcome.SUCCESS,
            stages = (trace.stages + Ew300OperationStage.FINAL_READBACK +
                Ew300OperationStage.RECONCILED_AFTER_RECONNECT + Ew300OperationStage.VERIFIED).distinct(),
            failureReason = null,
        )
        if (!traceStore.reconcile(reconciled)) return false
        pendingVerification = null
        return true
    }

    private suspend fun restoreBeforeCommit(
        baseline: Ew300RawBaseline,
        reason: String,
        trace: Ew300OperationTraceBuilder,
    ): Kt02h20FlashResult {
        if (!transactionCoordinator.restoreVolatile(baseline)) {
            return Kt02h20FlashResult.TransferFailed("$reason Restoration also failed; reconnect and read before any retry.")
        }
        trace.restored()
        return Kt02h20FlashResult.TransferFailed("$reason The original EW300 state was restored and verified.")
    }

    private suspend fun restoreExactBaselineInternal(
        baseline: Ew300RawBaseline,
        trace: Ew300OperationTraceBuilder,
    ): Ew300RestorationResult {
        val deviceKey = transport.deviceFingerprintKey
            ?: return Ew300RestorationResult.DeviceUnavailable(
                "The exact EW300 fingerprint is unavailable; no restoration write was sent.",
            )
        if (!mutationAuthorized(deviceKey) || baseline.deviceFingerprintKey != deviceKey) {
            return Ew300RestorationResult.NotSuitable(
                "The saved EW300 baseline does not belong to the exact authorized device; no restoration write was sent.",
            )
        }
        if (!baseline.isComplete()) {
            return Ew300RestorationResult.DeviceUnavailable(
                "The saved EW300 baseline is incomplete; no restoration write was sent.",
            )
        }
        trace.recordBaseline(baseline)
        trace.markBeforeFirstWrite(transport.permissionRequestCount)
        trace.stage(Ew300OperationStage.WRITING)
        val writesAccepted = baseline.restorableRegisters().all { register ->
            baseline.value(register)?.let { value -> transport.writeRegister(register, value) } == true
        }
        if (!writesAccepted) {
            return Ew300RestorationResult.TransferFailed(
                "EW300 did not accept the complete exact-baseline restoration before Save. Stop; do not retry.",
            )
        }
        if (!transactionCoordinator.verifyExact(baseline)) {
            return Ew300RestorationResult.VerificationFailed(
                "EW300 exact-baseline volatile readback did not match before Save. Stop; do not retry.",
            )
        }
        trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
        val sessionGenerationBeforeSave = transport.sessionGeneration
        val detachGenerationBeforeSave = transport.detachGeneration
        if (!transport.commit()) {
            return Ew300RestorationResult.TransferFailed(
                "EW300 restoration Save returned uncertain. Stop; do not retry.",
            )
        }
        trace.stage(Ew300OperationStage.SAVE_SENT_ONCE)
        if (!recordCommitBoundary(
                trace = trace,
                expectedFingerprint = deviceKey,
                sessionGenerationBeforeSave = sessionGenerationBeforeSave,
                detachGenerationBeforeSave = detachGenerationBeforeSave,
            )
        ) {
            return Ew300RestorationResult.VerificationFailed(
                "EW300 restoration Save returned on a different or unverified USB session. Stop; do not retry.",
            )
        }
        trace.stage(Ew300OperationStage.FINAL_READBACK)
        val restored = Ew300ReadbackRetry.read(
            read = { transactionCoordinator.verifyExact(baseline) },
            matches = { it },
        )
        if (!restored) {
            return Ew300RestorationResult.VerificationFailed(
                "EW300 final exact-baseline readback did not match byte-for-byte. Stop; do not retry.",
            )
        }
        trace.restored()
        return Ew300RestorationResult.Verified
    }

    private fun recordCommitBoundary(
        trace: Ew300OperationTraceBuilder,
        expectedFingerprint: String,
        sessionGenerationBeforeSave: Long,
        detachGenerationBeforeSave: Long,
    ): Boolean {
        val replacementObserved = transport.detachGeneration != detachGenerationBeforeSave
        val identityMatched = transport.deviceFingerprintKey == expectedFingerprint &&
            (!replacementObserved || transport.sessionGeneration > sessionGenerationBeforeSave)
        if (replacementObserved) {
            trace.stage(Ew300OperationStage.WAITING_FOR_REPLACEMENT)
            if (identityMatched) {
                trace.stage(Ew300OperationStage.REPLACEMENT_IDENTITY_VERIFIED)
                trace.stage(Ew300OperationStage.REPLACEMENT_AUTHORIZED)
            }
        } else if (identityMatched) {
            trace.stage(Ew300OperationStage.SAME_SESSION_READBACK)
        }
        return identityMatched
    }

    private suspend fun <T> record(
        operation: String,
        block: suspend (Ew300OperationTraceBuilder) -> T,
    ): T {
        pendingVerification = null
        val operationId = traceStore.begin(operation)
        val trace = Ew300OperationTraceBuilder(
            operationId = operationId,
            operation = operation,
            sourceCommit = sourceCommit,
            appVersion = appVersion,
            signerVerified = signerVerified,
            deviceFingerprintKey = transport.deviceFingerprintKey,
            sessionGeneration = transport.sessionGeneration,
            detachGeneration = transport.detachGeneration,
            initialPermissionRequestCount = transport.permissionRequestCount,
            initialRegisterWriteCount = transport.registerWriteCount,
            initialSaveCommandCount = transport.saveCommandCount,
        )
        try {
            val result = block(trace)
            val known = when (result) {
                is Kt02h20FlashResult.Success,
                is Kt02h20FlatResetResult.Success,
                Ew300EditorApplyResult.Verified,
                Ew300RestorationResult.Verified,
                is Ew300PlaybackGainResult.Success,
                is Ew300RuntimeApplyResult.Verified,
                Ew300RuntimeSaveResult.Verified,
                is Ew300ShelfTestResult.Verified,
                -> true
                else -> false
            }
            trace.complete(
                result.traceOutcome(),
                known,
                result.failureReason(),
            )
            return result
        } catch (error: Throwable) {
            trace.complete(
                Ew300OperationOutcome.EXCEPTION,
                false,
                error.message ?: "The EW300 operation terminated with an exception.",
            )
            throw error
        } finally {
            traceStore.publish(
                trace.build(
                    permissionRequestCount = transport.permissionRequestCount,
                    registerWriteCount = transport.registerWriteCount,
                    saveCommandCount = transport.saveCommandCount,
                    sessionGeneration = transport.sessionGeneration,
                    detachGeneration = transport.detachGeneration,
                ),
            )
        }
    }

    private fun runtimeTargetFromBaseline(
        baseline: Ew300RawBaseline,
        gain: ByteArray,
        gainSteps: Int,
    ): Ew300RuntimeTarget {
        require(baseline.isComplete())
        val registers = buildMap {
            baseline.bands().forEachIndexed { index, (bandGain, q) ->
                put(Ew300Protocol.bandRegister(index), bandGain.copyOf())
                put(Ew300Protocol.bandRegister(index) + 1, q.copyOf())
            }
            put(Ew300Protocol.GLOBAL_GAIN_REGISTER, gain.copyOf())
        }
        return Ew300RuntimeTarget(
            deviceFingerprintKey = baseline.deviceFingerprintKey,
            sessionGeneration = transport.sessionGeneration,
            detachGeneration = transport.detachGeneration,
            registers = registers,
            gainSteps = gainSteps,
        )
    }

    private fun expectedRuntimeBands(target: Ew300RuntimeTarget): List<Ew300WireBandPair> = buildList {
        repeat(Ew300Protocol.BAND_COUNT) { index ->
            val gain = target.registers[Ew300Protocol.bandRegister(index)] ?: return emptyList()
            val q = target.registers[Ew300Protocol.bandRegister(index) + 1] ?: return emptyList()
            add(Ew300WireBandPair(gain.copyOf(), q.copyOf()))
        }
    }

    private suspend fun readWireBands(): List<Ew300WireBandPair>? = buildList {
        repeat(Ew300Protocol.BAND_COUNT) { index ->
            val gain = transport.readRegister(Ew300Protocol.bandRegister(index)) ?: return null
            val q = transport.readRegister(Ew300Protocol.bandRegister(index) + 1) ?: return null
            if (gain.size != 4 || q.size != 4) return null
            add(Ew300WireBandPair(gain, q))
        }
    }

    private data class Ew300WireBandPair(
        val gain: ByteArray,
        val q: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean = other is Ew300WireBandPair &&
            gain.contentEquals(other.gain) && q.contentEquals(other.q)

        override fun hashCode(): Int = 31 * gain.contentHashCode() + q.contentHashCode()
    }

    private fun expectedRegisters(
        wires: List<Pair<ByteArray, ByteArray>>,
        globalGain: ByteArray,
    ): Map<Int, ByteArray> = buildMap {
        wires.forEachIndexed { index, (gain, q) ->
            put(Ew300Protocol.bandRegister(index), gain.copyOf())
            put(Ew300Protocol.bandRegister(index) + 1, q.copyOf())
        }
        put(Ew300Protocol.GLOBAL_GAIN_REGISTER, globalGain.copyOf())
    }

    private data class PendingVerification(
        val operationId: String,
        val operation: String,
        val deviceFingerprintKey: String,
        val sessionGenerationBeforeSave: Long,
        val detachGenerationBeforeSave: Long,
        val expectedRegisters: Map<Int, ByteArray>,
        val targetDiffersFromBaseline: Boolean,
        val appliedGainDeltaSteps: Int,
        val userBaselineGainSteps: Int? = null,
    )

    private fun Kt02h20FlashResult.toFlatResetResult(): Kt02h20FlatResetResult = when (this) {
        is Kt02h20FlashResult.TransferFailed -> Kt02h20FlatResetResult.TransferFailed(reason)
        is Kt02h20FlashResult.VerificationFailed -> Kt02h20FlatResetResult.VerificationFailed(reason)
        else -> Kt02h20FlatResetResult.TransferFailed("EW300 reset stopped before verification.")
    }

    private fun Any?.failureReason(): String? = when (this) {
        is Kt02h20FlashResult.NotSuitable -> reason
        is Kt02h20FlashResult.DeviceUnavailable -> reason
        is Kt02h20FlashResult.TransferFailed -> reason
        is Kt02h20FlashResult.VerificationFailed -> reason
        is Kt02h20FlatResetResult.NotSuitable -> reason
        is Kt02h20FlatResetResult.DeviceUnavailable -> reason
        is Kt02h20FlatResetResult.TransferFailed -> reason
        is Kt02h20FlatResetResult.VerificationFailed -> reason
        is Ew300EditorApplyResult.InvalidPlan -> reason
        is Ew300EditorApplyResult.StaleBaseline -> reason
        is Ew300EditorApplyResult.DeviceUnavailable -> reason
        is Ew300EditorApplyResult.TransferFailed -> reason
        is Ew300EditorApplyResult.VerificationFailed -> reason
        is Ew300RestorationResult.NoBaseline -> reason
        is Ew300RestorationResult.DeviceUnavailable -> reason
        is Ew300RestorationResult.NotSuitable -> reason
        is Ew300RestorationResult.TransferFailed -> reason
        is Ew300RestorationResult.VerificationFailed -> reason
        is Ew300PlaybackGainResult.NotSuitable -> reason
        is Ew300PlaybackGainResult.DeviceUnavailable -> reason
        is Ew300PlaybackGainResult.TransferFailed -> reason
        is Ew300PlaybackGainResult.VerificationFailed -> reason
        is Ew300RuntimeApplyResult.InvalidPlan -> reason
        is Ew300RuntimeApplyResult.StaleBaseline -> reason
        is Ew300RuntimeApplyResult.DeviceUnavailable -> reason
        is Ew300RuntimeApplyResult.TransferFailed -> reason
        is Ew300RuntimeApplyResult.VerificationFailed -> reason
        is Ew300RuntimeApplyResult.ConfirmationRequired ->
            "The reviewed EQ has ${cautionCount} caution(s); confirm before applying."
        is Ew300RuntimeSaveResult.NotSuitable -> reason
        is Ew300RuntimeSaveResult.DeviceUnavailable -> reason
        is Ew300RuntimeSaveResult.TransferFailed -> reason
        is Ew300RuntimeSaveResult.VerificationFailed -> reason
        is Ew300ShelfTestResult.NotSuitable -> reason
        is Ew300ShelfTestResult.DeviceUnavailable -> reason
        is Ew300ShelfTestResult.TransferFailed -> reason
        is Ew300ShelfTestResult.VerificationFailed -> reason
        else -> null
    }

    private fun Any?.traceOutcome(): String = when (this) {
        is Kt02h20FlashResult.Success,
        is Kt02h20FlatResetResult.Success,
        Ew300EditorApplyResult.Verified,
        Ew300RestorationResult.Verified,
        is Ew300PlaybackGainResult.Success,
        is Ew300RuntimeApplyResult.Verified,
        Ew300RuntimeSaveResult.Verified,
        is Ew300ShelfTestResult.Verified -> Ew300OperationOutcome.SUCCESS
        is Kt02h20FlashResult.NotSuitable,
        is Kt02h20FlatResetResult.NotSuitable,
        is Ew300EditorApplyResult.InvalidPlan,
        is Ew300RestorationResult.NotSuitable,
        -> Ew300OperationOutcome.NOT_SUITABLE
        is Kt02h20FlashResult.DeviceUnavailable,
        is Kt02h20FlatResetResult.DeviceUnavailable,
        is Ew300EditorApplyResult.DeviceUnavailable,
        is Ew300RestorationResult.DeviceUnavailable,
        -> Ew300OperationOutcome.DEVICE_UNAVAILABLE
        is Kt02h20FlashResult.TransferFailed,
        is Kt02h20FlatResetResult.TransferFailed,
        is Ew300EditorApplyResult.TransferFailed,
        is Ew300RestorationResult.TransferFailed,
        -> Ew300OperationOutcome.TRANSFER_FAILED
        is Kt02h20FlashResult.VerificationFailed,
        is Kt02h20FlatResetResult.VerificationFailed,
        is Ew300EditorApplyResult.VerificationFailed,
        is Ew300RestorationResult.VerificationFailed,
        -> Ew300OperationOutcome.VERIFICATION_FAILED
        is Ew300EditorApplyResult.StaleBaseline -> Ew300OperationOutcome.STALE_BASELINE
        is Ew300EditorApplyResult.ConfirmationRequired -> Ew300OperationOutcome.CONFIRMATION_REQUIRED
        is Ew300RestorationResult.NoBaseline -> Ew300OperationOutcome.NO_BASELINE
        is Ew300PlaybackGainResult.NotSuitable -> Ew300OperationOutcome.NOT_SUITABLE
        is Ew300PlaybackGainResult.DeviceUnavailable -> Ew300OperationOutcome.DEVICE_UNAVAILABLE
        is Ew300PlaybackGainResult.TransferFailed -> Ew300OperationOutcome.TRANSFER_FAILED
        is Ew300PlaybackGainResult.VerificationFailed -> Ew300OperationOutcome.VERIFICATION_FAILED
        is Ew300RuntimeApplyResult.InvalidPlan,
        is Ew300RuntimeSaveResult.NotSuitable,
        -> Ew300OperationOutcome.NOT_SUITABLE
        is Ew300RuntimeApplyResult.DeviceUnavailable,
        is Ew300RuntimeSaveResult.DeviceUnavailable,
        -> Ew300OperationOutcome.DEVICE_UNAVAILABLE
        is Ew300RuntimeApplyResult.TransferFailed,
        is Ew300RuntimeSaveResult.TransferFailed,
        -> Ew300OperationOutcome.TRANSFER_FAILED
        is Ew300RuntimeApplyResult.VerificationFailed,
        is Ew300RuntimeSaveResult.VerificationFailed,
        -> Ew300OperationOutcome.VERIFICATION_FAILED
        is Ew300RuntimeApplyResult.StaleBaseline -> Ew300OperationOutcome.STALE_BASELINE
        is Ew300RuntimeApplyResult.ConfirmationRequired -> Ew300OperationOutcome.CONFIRMATION_REQUIRED
        is Ew300ShelfTestResult.NotSuitable -> Ew300OperationOutcome.NOT_SUITABLE
        is Ew300ShelfTestResult.DeviceUnavailable -> Ew300OperationOutcome.DEVICE_UNAVAILABLE
        is Ew300ShelfTestResult.TransferFailed -> Ew300OperationOutcome.TRANSFER_FAILED
        is Ew300ShelfTestResult.VerificationFailed -> Ew300OperationOutcome.VERIFICATION_FAILED
        else -> Ew300OperationOutcome.UNKNOWN
    }

    private suspend fun verify(
        expected: List<Kt02h20Band>,
        allowExperimentalShelf: Boolean = false,
    ): VerificationFailure? {
        expected.indices.forEach { index ->
            val gain = transport.readRegister(Ew300Protocol.bandRegister(index))
                ?: return VerificationFailure(index, expected[index], null)
            val q = transport.readRegister(Ew300Protocol.bandRegister(index) + 1)
                ?: return VerificationFailure(index, expected[index], null)
            val actual = Ew300Protocol.decodeBand(index, gain, q)
            // Verify the device's encoded fields, not the display-layer Double values. The
            // EW300 stores integer tenths/thousandths, so 0.7 and 0.7000000000000001 are the
            // same hardware value even though Kotlin data-class equality treats them as distinct.
            val expectedWire = runCatching {
                Ew300Protocol.encodeBand(expected[index], allowExperimentalShelf)
            }.getOrNull()
            val matchesWire = expectedWire != null &&
                expectedWire.first.contentEquals(gain) &&
                expectedWire.second.contentEquals(q)
            if (!matchesWire) return VerificationFailure(index, expected[index], actual)
        }
        return null
    }

    /**
     * Reconciles the complete post-Save state while the replacement HID session settles.
     *
     * A transient read failure or stale unsolicited report must not turn a successful Save into
     * a false warning when the exact authorized session becomes readable milliseconds later. The
     * retry is deliberately read-only and bounded; identity/session checks have already passed at
     * [recordCommitBoundary].
     */
    private suspend fun reconcileFinalReadback(
        expected: List<Kt02h20Band>,
        expectedGain: ByteArray,
        allowExperimentalShelf: Boolean = false,
    ): FinalReadback = Ew300ReadbackRetry.read(
        read = {
            FinalReadback(
                bandFailure = verify(expected, allowExperimentalShelf),
                gain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER),
            )
        },
        matches = { observation ->
            observation.bandFailure == null &&
                observation.gain?.contentEquals(expectedGain) == true
        },
    )

    private data class FinalReadback(
        val bandFailure: VerificationFailure?,
        val gain: ByteArray?,
    )

    private data class VerificationFailure(
        val band: Int,
        val expected: Kt02h20Band,
        val actual: Kt02h20Band?,
    )

    private fun completeBands(bands: List<Kt02h20Band>): List<Kt02h20Band> =
        bands + List(Ew300Protocol.BAND_COUNT - bands.size) { Kt02h20Band("peak_dip", 1000.0, 0.0, 1.0) }

    private fun nativeShelfRepresentation(
        profile: OpraEqProfile,
    ): FiveBandRepresentation? {
        val source = profile.bands.orEmpty()
        if (source.isEmpty() || source.size > Ew300Protocol.BAND_COUNT) return null
        val converted = source.map { band ->
            val type = band.type ?: return null
            if (type !in setOf("peak_dip", "low_shelf", "high_shelf")) return null
            Kt02h20Band(
                type = type,
                frequencyHz = band.frequency ?: return null,
                gainDb = band.gainDb ?: return null,
                q = band.q ?: return null,
            )
        }
        val target = completeBands(converted)
        target.forEach { band ->
            runCatching {
                Ew300Protocol.encodeBand(band, allowExperimentalShelf = true)
            }.getOrElse { return null }
        }
        return FiveBandRepresentation(
            bands = target,
            playbackGainDb = 0.0,
            fidelity = DevicePresetFidelity.OPTIMIZED,
            rmsErrorDb = 0.0,
            maxAbsoluteErrorDb = 0.0,
            usesGeneratedHeadroom = false,
            representationVersion = HardwareEqDeviceSpecs.SIMGOT_EW300.representationVersion,
            sourceBandCount = source.size,
            usedResponseFit = false,
            usesNativeQuantization = true,
        )
    }

    private fun formatDb(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
}
