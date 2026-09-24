package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandRepresentation
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult
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
) {
    private val transactionCoordinator = Ew300TransactionCoordinator(transport)
    @Volatile
    private var lastSuccessfulFlashBaseline: Ew300RawBaseline? = null

    val lastOperationTrace: StateFlow<Ew300OperationTrace?> = traceStore.lastTrace

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
        return Ew300EditorApplier(transport).apply(
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
        val representation = when (val result = Kt02h20FiveBandOptimizer.optimize(profile, HardwareEqDeviceSpecs.SIMGOT_EW300)) {
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
        val currentSteps = Ew300Protocol.globalGainSteps(currentGain)
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
            runCatching { Ew300Protocol.encodeBand(band) }.getOrElse {
                return Kt02h20FlashResult.NotSuitable(it.message ?: "EW300 band is not representable.")
            }
        }
        val targetGain = Ew300Protocol.withGlobalGainSteps(currentGain, targetGainSteps)
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
        if (verify(target) != null) {
            return restoreBeforeCommit(
                baseline = baseline,
                reason = "EW300 volatile PEQ readback did not match before Save.",
                trace = trace,
            )
        }
        trace.stage(Ew300OperationStage.VOLATILE_VERIFIED)
        val sessionGenerationBeforeSave = transport.sessionGeneration
        val detachBeforeSave = transport.detachGeneration
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
        val verificationFailure = verify(target)
        if (verificationFailure != null) {
            return Kt02h20FlashResult.VerificationFailed(
                "EW300 final PEQ readback did not match band ${verificationFailure.band + 1}: " +
                    "expected ${verificationFailure.expected}, read ${verificationFailure.actual}.",
            )
        }
        val finalGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        if (finalGain == null || !finalGain.contentEquals(targetGain)) {
            return Kt02h20FlashResult.VerificationFailed(
                "EW300 final global-gain readback did not match the requested adjustment.",
            )
        }
        gainStateStore.writeAppliedGainDeltaSteps(deviceKey, requestedDeltaSteps)
        lastSuccessfulFlashBaseline = baseline
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
        val baselineSteps = Ew300Protocol.globalGainSteps(currentGain) - gainStateStore.readAppliedGainDeltaSteps(deviceKey)
        if (baselineSteps !in Ew300Protocol.GLOBAL_GAIN_MIN_STEPS..Ew300Protocol.GLOBAL_GAIN_MAX_STEPS) {
            return Kt02h20FlatResetResult.NotSuitable("The EW300 baseline global gain is outside the qualified range.")
        }
        val baselineGain = Ew300Protocol.withGlobalGainSteps(currentGain, baselineSteps)
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
        val verificationFailure = verify(flat)
        if (verificationFailure != null) {
            return Kt02h20FlatResetResult.VerificationFailed("EW300 final flat-EQ readback did not match.")
        }
        val finalGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        if (finalGain == null || !finalGain.contentEquals(baselineGain)) {
            return Kt02h20FlatResetResult.VerificationFailed("EW300 final flat-EQ gain readback did not match.")
        }
        gainStateStore.writeAppliedGainDeltaSteps(deviceKey, 0)
        return Kt02h20FlatResetResult.Success(restoredPlaybackGainDb = 0.0, explicitPersistenceCommandUsed = true)
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
        if (!transactionCoordinator.verifyExact(baseline)) {
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
        val trace = Ew300OperationTraceBuilder(
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
                is Kt02h20FlashResult.TransferFailed,
                is Kt02h20FlashResult.VerificationFailed,
                is Kt02h20FlatResetResult.TransferFailed,
                is Kt02h20FlatResetResult.VerificationFailed,
                is Ew300EditorApplyResult.TransferFailed,
                is Ew300EditorApplyResult.VerificationFailed,
                is Ew300RestorationResult.NoBaseline,
                is Ew300RestorationResult.DeviceUnavailable,
                is Ew300RestorationResult.NotSuitable,
                is Ew300RestorationResult.TransferFailed,
                is Ew300RestorationResult.VerificationFailed,
                -> false
                else -> true
            }
            trace.complete(
                result!!::class.simpleName ?: "COMPLETED",
                known,
                result.failureReason(),
            )
            return result
        } catch (error: Throwable) {
            trace.complete(
                "EXCEPTION:${error::class.simpleName}",
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
        else -> null
    }

    private suspend fun verify(expected: List<Kt02h20Band>): VerificationFailure? {
        expected.indices.forEach { index ->
            val gain = transport.readRegister(Ew300Protocol.bandRegister(index))
                ?: return VerificationFailure(index, expected[index], null)
            val q = transport.readRegister(Ew300Protocol.bandRegister(index) + 1)
                ?: return VerificationFailure(index, expected[index], null)
            val actual = Ew300Protocol.decodeBand(index, gain, q)
            // Verify the device's encoded fields, not the display-layer Double values. The
            // EW300 stores integer tenths/thousandths, so 0.7 and 0.7000000000000001 are the
            // same hardware value even though Kotlin data-class equality treats them as distinct.
            val expectedWire = runCatching { Ew300Protocol.encodeBand(expected[index]) }.getOrNull()
            val matchesWire = expectedWire != null &&
                expectedWire.first.contentEquals(gain) &&
                expectedWire.second.contentEquals(q)
            if (!matchesWire) return VerificationFailure(index, expected[index], actual)
        }
        return null
    }

    private data class VerificationFailure(
        val band: Int,
        val expected: Kt02h20Band,
        val actual: Kt02h20Band?,
    )

    private fun completeBands(bands: List<Kt02h20Band>): List<Kt02h20Band> =
        bands + List(Ew300Protocol.BAND_COUNT - bands.size) { Kt02h20Band("peak_dip", 1000.0, 0.0, 1.0) }

    private fun formatDb(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
}
