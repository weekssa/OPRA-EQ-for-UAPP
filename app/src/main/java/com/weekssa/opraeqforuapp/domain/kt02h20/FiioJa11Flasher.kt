package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacHeadroomStatus
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqFilter
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException

interface FiioJa11Transport {
    val deviceFingerprintKey: String?
        get() = null
    /** A serial is optional continuity evidence, not a prerequisite for JA11 support. */
    val deviceSerialIdentity: String?
        get() = fiioJa11SerialIdentity(deviceFingerprintKey)
    val usbProductId: Int?
        get() = null
    /** Exact JA11 VID/PID candidates currently attached through the approved transport matcher. */
    val supportedJa11CandidateCount: Int
        get() = 1
    val sessionGeneration: Long
        get() = 0L
    val detachGeneration: Long
        get() = 0L
    val permissionRequestCount: Long
        get() = 0L

    /** Starts bounded raw protocol capture for one operation; ordinary background reads are excluded. */
    fun beginTrace(operationId: String) = Unit

    /** Ends bounded raw protocol capture and returns only this operation's exchanges. */
    fun endTrace(): List<FiioJa11TransportEvent> = emptyList()

    suspend fun readBand(index: Int): FiioJa11Protocol.Band?
    suspend fun readGlobalGainDb(): Double?
    suspend fun readEqProgram(): FiioJa11Protocol.EqProgram?
    /** Optional read-only firmware metadata used to identify protocol-semantic differences. */
    suspend fun readFirmwareVersion(): String? = null

    /** Fail closed unless the transport can pin this read to the transaction's exact USB session. */
    suspend fun readBandInSession(index: Int, expected: FiioJa11SessionToken): FiioJa11Protocol.Band? = null
    suspend fun readGlobalGainDbInSession(expected: FiioJa11SessionToken): Double? = null
    suspend fun readEqProgramInSession(expected: FiioJa11SessionToken): FiioJa11Protocol.EqProgram? = null
    suspend fun readFirmwareVersionInSession(expected: FiioJa11SessionToken): String? = null

    suspend fun sendReport(report: ByteArray): Boolean

    fun isCurrentSession(expected: FiioJa11SessionToken): Boolean =
        supportedJa11CandidateCount == 1 &&
            deviceFingerprintKey == expected.deviceFingerprintKey &&
            deviceSerialIdentity == expected.deviceSerialIdentity &&
            usbProductId == expected.usbProductId &&
            sessionGeneration == expected.sessionGeneration &&
            detachGeneration == expected.detachGeneration

    /** Sends only if the transaction's exact USB session is still current. */
    suspend fun sendReportInSession(
        report: ByteArray,
        expected: FiioJa11SessionToken,
    ): FiioJa11ReportWriteOutcome {
        if (!isCurrentSession(expected)) return FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND
        if (!sendReport(report)) return FiioJa11ReportWriteOutcome.INCOMPLETE_OR_UNKNOWN
        return if (isCurrentSession(expected)) {
            FiioJa11ReportWriteOutcome.COMPLETED
        } else {
            FiioJa11ReportWriteOutcome.COMPLETED_WITH_SESSION_CHANGE
        }
    }

    /**
     * Save is a device-specific lifecycle boundary. The default keeps deterministic fakes and
     * non-Android transports compatible; Android JA11 overrides it to await the documented
     * power-cycle/re-enumeration before final readback.
     */
    suspend fun saveToFlash(expected: FiioJa11SessionToken): Boolean =
        ja11RestartWriteWasAccepted(sendReportInSession(FiioJa11Protocol.saveToFlashReport(), expected))
}

/**
 * Direct-Flash transaction for the normal FiiO JA11 run-mode PEQ protocol.
 *
 * The five editable coefficients live in User 1. A complete Flash therefore writes every User 1
 * band and global EQ gain only after User 1 is the freshly verified active program, applies,
 * verifies the active program and coefficients, saves, then verifies again. This prevents writes
 * from landing in the active Off/built-in program bank before a later switch to User 1.
 */
class FiioJa11Flasher(
    private val transport: FiioJa11Transport,
    private val traceStore: FiioJa11OperationTraceStore = FiioJa11OperationTraceStore(),
    private val sourceCommit: String = "unknown",
    private val appVersion: String = "unknown",
    private val signerVerified: Boolean = false,
) {
    val lastOperationTrace: kotlinx.coroutines.flow.StateFlow<FiioJa11OperationTrace?> = traceStore.lastTrace
    val operationStatus: kotlinx.coroutines.flow.StateFlow<FiioJa11OperationStatus> = traceStore.status

    suspend fun flash(profile: OpraEqProfile): Kt02h20FlashResult {
        val operationId = traceStore.begin("FLASH")
        transport.beginTrace(operationId)
        val trace = FiioJa11OperationTraceBuilder(
            operationId = operationId,
            operation = "FLASH",
            sourceCommit = sourceCommit,
            appVersion = appVersion,
            signerVerified = signerVerified,
            deviceFingerprintKey = transport.deviceFingerprintKey,
            usbProductId = transport.usbProductId,
            sessionGeneration = transport.sessionGeneration,
            detachGeneration = transport.detachGeneration,
            permissionRequestCount = transport.permissionRequestCount,
        )
        val result = try {
            flashInternal(profile, trace)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            Kt02h20FlashResult.TransferFailed(
                "FiiO JA11 operation stopped unexpectedly: ${error.message ?: "unknown transport error"}.",
            )
        }
        trace.complete(
            outcome = result.outcomeName(),
            stateKnown = result.stateKnownForTrace(),
            failureReason = result.failureReason(),
        )
        trace.addEvents(transport.endTrace())
        traceStore.publish(trace.build())
        return result
    }

    private suspend fun flashInternal(
        profile: OpraEqProfile,
        trace: FiioJa11OperationTraceBuilder,
    ): Kt02h20FlashResult {
        val representation = when (val optimized = Kt02h20FiveBandOptimizer.optimize(profile, Kt02h20DeviceSpecs.FIIO_JA11)) {
            is FiveBandOptimizationResult.NotSuitable -> return Kt02h20FlashResult.NotSuitable(optimized.reason)
            is FiveBandOptimizationResult.Ready -> optimized.representation
        }
        val targetBands = FiioJa11Protocol.completeBands(representation.bands)
        trace.target(profile, representation, targetBands)

        val transactionSession = captureCurrentSessionToken()
            ?: return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t confirm the current FiiO JA11 USB identity and session. No EQ changes were written.",
            )

        val baselineProgram = readInSession(transactionSession) { transport.readEqProgramInSession(transactionSession) }
        val baselineGlobalGainDb = readInSession(transactionSession) {
            transport.readGlobalGainDbInSession(transactionSession)
        }
        val baselineBands = (0 until FiioJa11Protocol.BAND_COUNT).map { index ->
            readInSession(transactionSession) { transport.readBandInSession(index, transactionSession) }
        }
        val firmwareVersion = readInSession(transactionSession) {
            transport.readFirmwareVersionInSession(transactionSession)
        }
        if (!transport.isCurrentSession(transactionSession) ||
            baselineProgram == null || baselineGlobalGainDb == null || baselineBands.any { it == null }
        ) {
            return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t read one complete FiiO JA11 PEQ baseline from the same USB session. No EQ changes were written.",
            )
        }
        trace.baselineRead(
            program = baselineProgram,
            globalGainDb = baselineGlobalGainDb,
            bands = baselineBands.filterNotNull(),
            firmwareVersion = firmwareVersion,
        )

        trace.stage(FiioJa11OperationStage.WRITING)
        when (ensureUserOneActive(transactionSession)) {
            UserOneActivationResult.ACTIVE -> Unit
            UserOneActivationResult.READ_UNAVAILABLE -> return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t confirm FiiO JA11 User 1 before the EQ write. No bands or global gain were written.",
            )
            UserOneActivationResult.SELECTION_FAILED -> return Kt02h20FlashResult.TransferFailed(
                "The FiiO JA11 User 1 selection did not complete in the authorized session. User 1 may now be active; no bands or global gain were written. Refresh before any later write.",
            )
            UserOneActivationResult.SELECTION_READ_UNAVAILABLE -> return Kt02h20FlashResult.TransferFailed(
                "FiiO JA11 may have switched to User 1, but the same-session readback did not complete. No bands, global gain, Apply, or Save were sent. Refresh before any later write.",
            )
            UserOneActivationResult.SELECTION_NOT_CONFIRMED -> return Kt02h20FlashResult.VerificationFailed(
                "FiiO JA11 did not confirm User 1 active after selection. No bands or global gain were written.",
            )
        }
        targetBands.forEachIndexed { index, band ->
            if (!sendPreSaveReport(FiioJa11Protocol.writeBandReport(index, band), transactionSession)) {
                return Kt02h20FlashResult.TransferFailed(
                    "The FiiO JA11 USB session changed or stopped confirming reports at band ${index + 1} of ${FiioJa11Protocol.BAND_COUNT}. No later commands were sent and the preset was not reported as applied.",
                )
            }
        }
        if (!sendPreSaveReport(FiioJa11Protocol.writeGlobalGainReport(representation.playbackGainDb), transactionSession)) {
            return Kt02h20FlashResult.TransferFailed(
                "The FiiO JA11 USB session changed or stopped confirming the global EQ gain report. No later commands were sent.",
            )
        }
        if (!sendPreSaveReport(FiioJa11Protocol.applyReport(), transactionSession)) {
            return Kt02h20FlashResult.TransferFailed(
                "The FiiO JA11 USB session changed or stopped confirming Apply. No Save was sent.",
            )
        }
        trace.stage(FiioJa11OperationStage.APPLY_SENT)

        verifyTarget(targetBands, representation.playbackGainDb, transactionSession, trace, "VOLATILE_READBACK")?.let { reason ->
            return Kt02h20FlashResult.VerificationFailed(reason)
        }

        trace.saveSent()
        if (!transport.saveToFlash(transactionSession)) {
            return Kt02h20FlashResult.TransferFailed(
                "The PEQ was applied to the FiiO JA11, but the device did not complete the persistent Save/reconnect boundary.",
            )
        }

        val finalSession = captureCurrentSessionToken()
            ?.takeIf { acceptablePostSaveSession(transactionSession, it) }
            ?: return Kt02h20FlashResult.VerificationFailed(
                "FiiO JA11 completed Save, but a single supported session could not be confirmed for final readback.",
            )

        verifyTarget(targetBands, representation.playbackGainDb, finalSession, trace, "FINAL_READBACK")?.let { reason ->
            return Kt02h20FlashResult.VerificationFailed(
                "FiiO JA11 accepted Save, but the final readback did not match the intended PEQ. $reason",
            )
        }

        return Kt02h20FlashResult.Success(
            representation = representation,
            explicitPersistenceCommandUsed = true,
        )
    }

    suspend fun resetToFlat(): Kt02h20FlatResetResult = resetWithTrace()

    /**
     * Applies the shared local editor's already-reviewed User 1 working copy. The read immediately
     * before the first write compares one coherent identity/session/program/band/gain token, then
     * reuses the exact five-band -> global gain -> Apply -> one Save -> final readback transaction
     * used by direct JA11 Flash. User 1 is already active and freshly verified before any edit.
     */
    suspend fun applyEditorWorkingCopy(
        workingCopy: HardwareEqEditWorkingCopy,
        baseline: FiioJa11EditorBaseline,
    ): FiioJa11EditorApplyResult {
        val operationId = traceStore.begin("EDITOR_APPLY")
        transport.beginTrace(operationId)
        val trace = FiioJa11OperationTraceBuilder(
            operationId = operationId,
            operation = "EDITOR_APPLY",
            sourceCommit = sourceCommit,
            appVersion = appVersion,
            signerVerified = signerVerified,
            deviceFingerprintKey = transport.deviceFingerprintKey,
            usbProductId = transport.usbProductId,
            sessionGeneration = transport.sessionGeneration,
            detachGeneration = transport.detachGeneration,
            permissionRequestCount = transport.permissionRequestCount,
        )
        val result = try {
            applyEditorInternal(workingCopy, baseline, trace)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            FiioJa11EditorApplyResult.TransferFailed(
                "FiiO JA11 editor Apply stopped unexpectedly: ${error.message ?: "unknown transport error"}.",
            )
        }
        trace.complete(
            outcome = result.editorOutcomeName(),
            stateKnown = result.editorStateKnownForTrace(),
            failureReason = result.editorFailureReason(),
        )
        trace.addEvents(transport.endTrace())
        traceStore.publish(trace.build())
        return result
    }

    private suspend fun applyEditorInternal(
        workingCopy: HardwareEqEditWorkingCopy,
        baseline: FiioJa11EditorBaseline,
        trace: FiioJa11OperationTraceBuilder,
    ): FiioJa11EditorApplyResult {
        if (workingCopy.baselineSnapshot.deviceId != DacDeviceId.FIIO_JA11) {
            return FiioJa11EditorApplyResult.InvalidPlan("This editor working copy does not belong to a FiiO JA11.")
        }
        if (workingCopy.baselineSnapshot.activeProgram != FiioJa11Protocol.EqProgram.USER_1) {
            return FiioJa11EditorApplyResult.InvalidPlan("Only the verified active JA11 User 1 program can be edited.")
        }
        if (!workingCopy.hasChanges) {
            return FiioJa11EditorApplyResult.InvalidPlan("There are no reviewed JA11 hardware changes to apply.")
        }
        if (workingCopy.hasBlockingIssues) {
            return FiioJa11EditorApplyResult.InvalidPlan("Fix the blocking JA11 EQ values before applying.")
        }
        if (workingCopy.headroomAssessment?.status != DacHeadroomStatus.SAFE) {
            return FiioJa11EditorApplyResult.InvalidPlan("The reviewed JA11 EQ does not yet have a safe global-gain plan.")
        }
        if (workingCopy.filters.size != FiioJa11Protocol.BAND_COUNT ||
            workingCopy.filters.map(HardwareEqFilter::index) != (0 until FiioJa11Protocol.BAND_COUNT).toList()
        ) {
            return FiioJa11EditorApplyResult.InvalidPlan("The reviewed JA11 editor does not contain the complete five-band layout.")
        }

        if (!sameAuthorizedIdentity(baseline)) {
            return FiioJa11EditorApplyResult.StaleBaseline(
                "The FiiO JA11 identity or USB session changed after the editor was opened. No editor changes were written.",
            )
        }
        val transactionSession = baseline.sessionToken()
        trace.stage(FiioJa11OperationStage.AUTHORIZED_SESSION)

        // This is the mandatory immediate pre-write token comparison, even when the generation has
        // not changed. A background DEVICE operation or external state change must invalidate Apply.
        val currentProgram = readInSession(transactionSession) {
            transport.readEqProgramInSession(transactionSession)
        }
            ?: return FiioJa11EditorApplyResult.DeviceUnavailable(
                "Couldn’t re-read the active FiiO JA11 program before Apply. No editor changes were written.",
            )
        if (currentProgram != FiioJa11Protocol.EqProgram.USER_1) {
            return FiioJa11EditorApplyResult.StaleBaseline(
                "FiiO JA11 is no longer on User 1. No editor changes were written; read the current EQ again.",
            )
        }
        val currentBands = (0 until FiioJa11Protocol.BAND_COUNT).map { index ->
            readInSession(transactionSession) { transport.readBandInSession(index, transactionSession) }
        }
        val currentGlobalGainDb = readInSession(transactionSession) {
            transport.readGlobalGainDbInSession(transactionSession)
        }
        if (currentBands.any { it == null } || currentGlobalGainDb == null) {
            return FiioJa11EditorApplyResult.DeviceUnavailable(
                "Couldn’t complete the fresh five-band JA11 read before Apply. No editor changes were written.",
            )
        }
        if (!sameAuthorizedIdentity(baseline)) {
            return FiioJa11EditorApplyResult.StaleBaseline(
                "The FiiO JA11 identity or USB session changed while the editor baseline was being verified. No editor changes were written.",
            )
        }
        trace.baselineRead(
            program = currentProgram,
            globalGainDb = currentGlobalGainDb,
            bands = currentBands.filterNotNull(),
            firmwareVersion = null,
        )
        val currentBundle = HardwareEqSnapshotFactory.fiioJa11(
            nativeBands = currentBands.filterNotNull(),
            globalEqGainDb = currentGlobalGainDb,
            sessionGeneration = transport.sessionGeneration,
            verifiedAtEpochMillis = System.currentTimeMillis(),
            eqEnabled = true,
            activeProgram = currentProgram,
        ) ?: return FiioJa11EditorApplyResult.DeviceUnavailable(
            "The fresh JA11 read was not representable as a complete User 1 snapshot. No editor changes were written.",
        )
        if (currentBundle.fingerprint != baseline.snapshotBundle.fingerprint ||
            currentBundle.snapshot.activeProgram != baseline.snapshotBundle.snapshot.activeProgram
        ) {
            return FiioJa11EditorApplyResult.StaleBaseline(
                "The FiiO JA11 User 1 EQ changed after the editor was opened. No editor changes were written; read the DAC again.",
            )
        }

        val targetBands = workingCopy.filters.sortedBy(HardwareEqFilter::index).map { filter ->
            runCatching { filter.toJa11Band() }.getOrElse { error ->
                return FiioJa11EditorApplyResult.InvalidPlan(
                    error.message ?: "A reviewed JA11 band could not be represented exactly.",
                )
            }
        }
        val targetGlobalGainDb = workingCopy.plannedHeadroomGainDb
            ?: return FiioJa11EditorApplyResult.InvalidPlan("The reviewed JA11 global EQ gain is unavailable.")
        val quantizedGlobalGainDb = runCatching {
            FiioJa11Protocol.quantizedGlobalGainDb(targetGlobalGainDb)
        }.getOrElse { error ->
            return FiioJa11EditorApplyResult.InvalidPlan(
                error.message ?: "The reviewed JA11 global EQ gain is outside the device range.",
            )
        }
        trace.targetManualEdit(targetBands, quantizedGlobalGainDb)

        val programBeforeWrite = readInSession(transactionSession) {
            transport.readEqProgramInSession(transactionSession)
        } ?: return FiioJa11EditorApplyResult.DeviceUnavailable(
            "Couldn’t confirm active JA11 User 1 immediately before editor writes. No editor changes were written.",
        )
        if (programBeforeWrite != FiioJa11Protocol.EqProgram.USER_1) {
            return FiioJa11EditorApplyResult.StaleBaseline(
                "FiiO JA11 is no longer on User 1 immediately before editor writes. No editor changes were written; read the current EQ again.",
            )
        }

        trace.stage(FiioJa11OperationStage.WRITING)
        targetBands.forEachIndexed { index, band ->
            if (!sendPreSaveReport(FiioJa11Protocol.writeBandReport(index, band), transactionSession)) {
                return FiioJa11EditorApplyResult.TransferFailed(
                    "The FiiO JA11 USB session changed or stopped confirming the reviewed editor value at band ${index + 1}. No later commands were sent.",
                )
            }
        }
        if (!sendPreSaveReport(FiioJa11Protocol.writeGlobalGainReport(quantizedGlobalGainDb), transactionSession)) {
            return FiioJa11EditorApplyResult.TransferFailed(
                "The FiiO JA11 USB session changed or stopped confirming the reviewed global EQ gain. No later commands were sent.",
            )
        }
        if (!sendPreSaveReport(FiioJa11Protocol.applyReport(), transactionSession)) {
            return FiioJa11EditorApplyResult.TransferFailed(
                "The FiiO JA11 USB session changed or stopped confirming Apply. No Save was sent.",
            )
        }
        trace.stage(FiioJa11OperationStage.APPLY_SENT)

        verifyTarget(targetBands, quantizedGlobalGainDb, transactionSession, trace, "VOLATILE_READBACK")?.let { reason ->
            return FiioJa11EditorApplyResult.VerificationFailed(
                "FiiO JA11 editor Apply was not verified. $reason",
            )
        }
        trace.saveSent()
        if (!transport.saveToFlash(transactionSession)) {
            return FiioJa11EditorApplyResult.TransferFailed(
                "FiiO JA11 applied the reviewed EQ, but the one Save/reconnect boundary did not complete. The requested state was not verified.",
            )
        }
        val finalSession = captureCurrentSessionToken()
            ?.takeIf { acceptablePostSaveSession(transactionSession, it) }
            ?: return FiioJa11EditorApplyResult.VerificationFailed(
                "FiiO JA11 completed Save, but a single supported session could not be confirmed for final readback.",
            )
        verifyTarget(targetBands, quantizedGlobalGainDb, finalSession, trace, "FINAL_READBACK")?.let { reason ->
            return FiioJa11EditorApplyResult.VerificationFailed(
                "FiiO JA11 editor Apply was not verified after Save. $reason",
            )
        }
        return FiioJa11EditorApplyResult.Verified
    }

    private fun sameAuthorizedIdentity(baseline: FiioJa11EditorBaseline): Boolean =
        transport.deviceFingerprintKey == baseline.deviceFingerprintKey &&
            transport.usbProductId == baseline.usbProductId &&
            transport.sessionGeneration == baseline.sessionGeneration &&
            transport.detachGeneration == baseline.detachGeneration

    private fun HardwareEqFilter.toJa11Band(): FiioJa11Protocol.Band = FiioJa11Protocol.Band(
        type = when (type) {
            EqFilterType.PEAK -> "peak_dip"
            EqFilterType.LOW_SHELF -> "low_shelf"
            EqFilterType.HIGH_SHELF -> "high_shelf"
            else -> error("Unsupported FiiO JA11 editor filter type: $type")
        },
        frequencyHz = frequencyHz.roundToInt().toDouble(),
        gainDb = (gainDb * 10.0).roundToInt() / 10.0,
        q = (q * 100.0).roundToInt() / 100.0,
    )

    private suspend fun resetInternal(trace: FiioJa11OperationTraceBuilder): Kt02h20FlatResetResult {
        val transactionSession = captureCurrentSessionToken()
            ?: return Kt02h20FlatResetResult.DeviceUnavailable(
                "Couldn’t confirm the current FiiO JA11 USB identity and session. No EQ changes were written.",
            )
        val baselineProgram = readInSession(transactionSession) { transport.readEqProgramInSession(transactionSession) }
        val baselineGlobalGainDb = readInSession(transactionSession) {
            transport.readGlobalGainDbInSession(transactionSession)
        }
        val baselineBands = (0 until FiioJa11Protocol.BAND_COUNT).map { index ->
            readInSession(transactionSession) { transport.readBandInSession(index, transactionSession) }
        }
        val firmwareVersion = readInSession(transactionSession) {
            transport.readFirmwareVersionInSession(transactionSession)
        }
        if (!transport.isCurrentSession(transactionSession) ||
            baselineProgram == null || baselineGlobalGainDb == null || baselineBands.any { it == null }
        ) {
            return Kt02h20FlatResetResult.DeviceUnavailable(
                "Couldn’t read one complete FiiO JA11 PEQ baseline from the same USB session. No EQ changes were written.",
            )
        }
        trace.baselineRead(
            program = baselineProgram,
            globalGainDb = baselineGlobalGainDb,
            bands = baselineBands.filterNotNull(),
            firmwareVersion = firmwareVersion,
        )
        val flatBands = FiioJa11Protocol.completeBands(emptyList())
        trace.stage(FiioJa11OperationStage.WRITING)
        when (ensureUserOneActive(transactionSession)) {
            UserOneActivationResult.ACTIVE -> Unit
            UserOneActivationResult.READ_UNAVAILABLE -> return Kt02h20FlatResetResult.DeviceUnavailable(
                "Couldn’t confirm FiiO JA11 User 1 before the flat-EQ reset. No bands or global gain were written.",
            )
            UserOneActivationResult.SELECTION_FAILED -> return Kt02h20FlatResetResult.TransferFailed(
                "The FiiO JA11 User 1 selection did not complete in the authorized session. User 1 may now be active; no bands or global gain were written. Refresh before any later write.",
            )
            UserOneActivationResult.SELECTION_READ_UNAVAILABLE -> return Kt02h20FlatResetResult.TransferFailed(
                "FiiO JA11 may have switched to User 1, but the same-session readback did not complete. No bands, global gain, Apply, or Save were sent. Refresh before any later write.",
            )
            UserOneActivationResult.SELECTION_NOT_CONFIRMED -> return Kt02h20FlatResetResult.VerificationFailed(
                "FiiO JA11 did not confirm User 1 active after selection. No bands or global gain were written.",
            )
        }
        flatBands.forEachIndexed { index, band ->
            if (!sendPreSaveReport(FiioJa11Protocol.writeBandReport(index, band), transactionSession)) {
                return Kt02h20FlatResetResult.TransferFailed(
                    "The FiiO JA11 USB session changed or stopped confirming the flat-EQ reset at band ${index + 1} of ${FiioJa11Protocol.BAND_COUNT}. No later commands were sent.",
                )
            }
        }
        if (!sendPreSaveReport(FiioJa11Protocol.writeGlobalGainReport(0.0), transactionSession)) {
            return Kt02h20FlatResetResult.TransferFailed(
                "The FiiO JA11 USB session changed or stopped confirming 0 dB global EQ gain. No later commands were sent.",
            )
        }
        if (!sendPreSaveReport(FiioJa11Protocol.applyReport(), transactionSession)) {
            return Kt02h20FlatResetResult.TransferFailed(
                "The FiiO JA11 USB session changed or stopped confirming Reset Apply. No Save was sent.",
            )
        }
        trace.stage(FiioJa11OperationStage.APPLY_SENT)
        verifyTarget(flatBands, 0.0, transactionSession, trace, "VOLATILE_READBACK")?.let { reason ->
            return Kt02h20FlatResetResult.VerificationFailed(reason)
        }
        trace.saveSent()
        if (!transport.saveToFlash(transactionSession)) {
            return Kt02h20FlatResetResult.TransferFailed(
                "The FiiO JA11 PEQ is flat in the current session, but the device did not complete the persistent Save/reconnect boundary.",
            )
        }
        val finalSession = captureCurrentSessionToken()
            ?.takeIf { acceptablePostSaveSession(transactionSession, it) }
            ?: return Kt02h20FlatResetResult.VerificationFailed(
                "FiiO JA11 completed Save, but a single supported session could not be confirmed for final readback.",
            )
        verifyTarget(flatBands, 0.0, finalSession, trace, "FINAL_READBACK")?.let { reason ->
            return Kt02h20FlatResetResult.VerificationFailed(
                "FiiO JA11 accepted Save, but the final flat-EQ readback did not match. $reason",
            )
        }
        return Kt02h20FlatResetResult.Success(
            restoredPlaybackGainDb = 0.0,
            explicitPersistenceCommandUsed = true,
        )
    }

    private suspend fun verifyTarget(
        expectedBands: List<FiioJa11Protocol.Band>,
        expectedGlobalGainDb: Double,
        expectedSession: FiioJa11SessionToken,
        trace: FiioJa11OperationTraceBuilder? = null,
        phase: String = "VOLATILE_READBACK",
    ): String? {
        if (!transport.isCurrentSession(expectedSession)) {
            return "The authorized FiiO JA11 USB session changed before readback."
        }
        val activeProgram = readInSession(expectedSession) { transport.readEqProgramInSession(expectedSession) }
            ?: return "Couldn’t verify the active JA11 EQ program because the authorized USB session stopped responding."
        if (activeProgram != FiioJa11Protocol.EqProgram.USER_1) {
            return "JA11 User 1 was not the active EQ program after Apply."
        }
        expectedBands.forEachIndexed { index, expected ->
            val actual = readInSession(expectedSession) { transport.readBandInSession(index, expectedSession) }
                ?: return if (transport.isCurrentSession(expectedSession)) {
                    "Couldn’t read back JA11 band ${index + 1}."
                } else {
                    "The authorized FiiO JA11 USB session changed before band ${index + 1} readback."
                }
            if (!FiioJa11Protocol.nearlyMatches(expected, actual)) {
                return "JA11 band ${index + 1} readback did not match the intended value."
            }
        }
        val actualGain = readInSession(expectedSession) { transport.readGlobalGainDbInSession(expectedSession) }
            ?: run {
                trace?.compare(phase, null)
                return if (transport.isCurrentSession(expectedSession)) {
                    "Couldn’t read back the JA11 global EQ gain."
                } else {
                    "The authorized FiiO JA11 USB session changed before global-gain readback."
                }
            }
        val wireExpected = FiioJa11Protocol.quantizedGlobalGainDb(expectedGlobalGainDb)
        trace?.compare(phase, actualGain)
        if (abs(actualGain - wireExpected) > GLOBAL_GAIN_READBACK_TOLERANCE_DB) {
            return "JA11 global EQ gain readback did not match the intended value."
        }
        return null
    }

    private fun captureCurrentSessionToken(): FiioJa11SessionToken? {
        if (transport.supportedJa11CandidateCount != 1) return null
        val fingerprint = transport.deviceFingerprintKey ?: return null
        val productId = transport.usbProductId?.takeIf(FiioJa11Protocol::supportsProductId) ?: return null
        return runCatching {
            FiioJa11SessionToken(
                deviceFingerprintKey = fingerprint,
                usbProductId = productId,
                sessionGeneration = transport.sessionGeneration,
                detachGeneration = transport.detachGeneration,
                deviceSerialIdentity = transport.deviceSerialIdentity,
            )
        }.getOrNull()?.takeIf(transport::isCurrentSession)
    }

    private suspend fun <T> readInSession(
        expected: FiioJa11SessionToken,
        read: suspend () -> T?,
    ): T? {
        if (!transport.isCurrentSession(expected)) return null
        val value = read()
        return value?.takeIf { transport.isCurrentSession(expected) }
    }

    private suspend fun sendPreSaveReport(
        report: ByteArray,
        expected: FiioJa11SessionToken,
    ): Boolean = transport.sendReportInSession(report, expected) == FiioJa11ReportWriteOutcome.COMPLETED

    private suspend fun ensureUserOneActive(
        expected: FiioJa11SessionToken,
    ): UserOneActivationResult {
        val activeProgram = readInSession(expected) { transport.readEqProgramInSession(expected) }
            ?: return UserOneActivationResult.READ_UNAVAILABLE
        if (activeProgram == FiioJa11Protocol.EqProgram.USER_1) return UserOneActivationResult.ACTIVE
        if (!sendPreSaveReport(FiioJa11Protocol.writeEqProgramReport(FiioJa11Protocol.EqProgram.USER_1), expected)) {
            return UserOneActivationResult.SELECTION_FAILED
        }
        val selectedProgram = readInSession(expected) { transport.readEqProgramInSession(expected) }
            ?: return UserOneActivationResult.SELECTION_READ_UNAVAILABLE
        return if (selectedProgram == FiioJa11Protocol.EqProgram.USER_1) {
            UserOneActivationResult.ACTIVE
        } else {
            UserOneActivationResult.SELECTION_NOT_CONFIRMED
        }
    }

    private fun acceptablePostSaveSession(
        expected: FiioJa11SessionToken,
        actual: FiioJa11SessionToken,
    ): Boolean {
        val candidateCount = transport.supportedJa11CandidateCount
        if (candidateCount != 1) return false
        if (fiioJa11RestartContinuityFromSerials(
            originalSerialIdentity = expected.deviceSerialIdentity,
            replacementSerialIdentity = transport.deviceSerialIdentity,
            supportedCandidateCount = candidateCount,
        ) == null) return false
        val unchangedSession = actual.sessionGeneration == expected.sessionGeneration &&
            actual.detachGeneration == expected.detachGeneration &&
            transport.isCurrentSession(expected)
        if (unchangedSession) return actual.usbProductId == expected.usbProductId
        return actual.sessionGeneration > expected.sessionGeneration &&
            actual.detachGeneration > expected.detachGeneration &&
            actual.usbProductId == expected.usbProductId &&
            actual.usbProductId.let(FiioJa11Protocol::supportsProductId)
    }

    private suspend fun resetWithTrace(): Kt02h20FlatResetResult {
        val operationId = traceStore.begin("RESET")
        transport.beginTrace(operationId)
        val trace = FiioJa11OperationTraceBuilder(
            operationId = operationId,
            operation = "RESET",
            sourceCommit = sourceCommit,
            appVersion = appVersion,
            signerVerified = signerVerified,
            deviceFingerprintKey = transport.deviceFingerprintKey,
            usbProductId = transport.usbProductId,
            sessionGeneration = transport.sessionGeneration,
            detachGeneration = transport.detachGeneration,
            permissionRequestCount = transport.permissionRequestCount,
        )
        val result = try {
            resetInternal(trace)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            Kt02h20FlatResetResult.TransferFailed(
                "FiiO JA11 reset stopped unexpectedly: ${error.message ?: "unknown transport error"}.",
            )
        }
        trace.complete(
            outcome = result.outcomeName(),
            stateKnown = result.stateKnownForTrace(),
            failureReason = result.failureReason(),
        )
        trace.addEvents(transport.endTrace())
        traceStore.publish(trace.build())
        return result
    }

    private companion object {
        const val GLOBAL_GAIN_READBACK_TOLERANCE_DB = 0.001
    }

    private enum class UserOneActivationResult {
        ACTIVE,
        READ_UNAVAILABLE,
        SELECTION_FAILED,
        SELECTION_READ_UNAVAILABLE,
        SELECTION_NOT_CONFIRMED,
    }
}

private fun Kt02h20FlashResult.outcomeName(): String = when (this) {
    is Kt02h20FlashResult.Success -> "Success"
    is Kt02h20FlashResult.NotSuitable -> "NotSuitable"
    is Kt02h20FlashResult.DeviceUnavailable -> "DeviceUnavailable"
    is Kt02h20FlashResult.TransferFailed -> "TransferFailed"
    is Kt02h20FlashResult.VerificationFailed -> "VerificationFailed"
}

private fun Kt02h20FlashResult.stateKnownForTrace(): Boolean = when (this) {
    is Kt02h20FlashResult.Success,
    is Kt02h20FlashResult.NotSuitable,
    is Kt02h20FlashResult.DeviceUnavailable,
    is Kt02h20FlashResult.VerificationFailed,
    -> true
    is Kt02h20FlashResult.TransferFailed -> false
}

private fun Kt02h20FlashResult.failureReason(): String? = when (this) {
    is Kt02h20FlashResult.Success -> null
    is Kt02h20FlashResult.NotSuitable -> reason
    is Kt02h20FlashResult.DeviceUnavailable -> reason
    is Kt02h20FlashResult.TransferFailed -> reason
    is Kt02h20FlashResult.VerificationFailed -> reason
}

private fun FiioJa11EditorApplyResult.editorOutcomeName(): String = when (this) {
    FiioJa11EditorApplyResult.Verified -> "Success"
    is FiioJa11EditorApplyResult.InvalidPlan -> "InvalidPlan"
    is FiioJa11EditorApplyResult.StaleBaseline -> "StaleBaseline"
    is FiioJa11EditorApplyResult.DeviceUnavailable -> "DeviceUnavailable"
    is FiioJa11EditorApplyResult.TransferFailed -> "TransferFailed"
    is FiioJa11EditorApplyResult.VerificationFailed -> "VerificationFailed"
}

private fun FiioJa11EditorApplyResult.editorStateKnownForTrace(): Boolean = when (this) {
    FiioJa11EditorApplyResult.Verified,
    is FiioJa11EditorApplyResult.InvalidPlan,
    is FiioJa11EditorApplyResult.StaleBaseline,
    is FiioJa11EditorApplyResult.DeviceUnavailable,
    is FiioJa11EditorApplyResult.VerificationFailed,
    -> true
    is FiioJa11EditorApplyResult.TransferFailed -> false
}

private fun FiioJa11EditorApplyResult.editorFailureReason(): String? = when (this) {
    FiioJa11EditorApplyResult.Verified -> null
    is FiioJa11EditorApplyResult.InvalidPlan -> reason
    is FiioJa11EditorApplyResult.StaleBaseline -> reason
    is FiioJa11EditorApplyResult.DeviceUnavailable -> reason
    is FiioJa11EditorApplyResult.TransferFailed -> reason
    is FiioJa11EditorApplyResult.VerificationFailed -> reason
}

private fun Kt02h20FlatResetResult.outcomeName(): String = when (this) {
    is Kt02h20FlatResetResult.Success -> "Success"
    is Kt02h20FlatResetResult.NotSuitable -> "NotSuitable"
    is Kt02h20FlatResetResult.DeviceUnavailable -> "DeviceUnavailable"
    is Kt02h20FlatResetResult.TransferFailed -> "TransferFailed"
    is Kt02h20FlatResetResult.VerificationFailed -> "VerificationFailed"
}

private fun Kt02h20FlatResetResult.stateKnownForTrace(): Boolean = when (this) {
    is Kt02h20FlatResetResult.Success,
    is Kt02h20FlatResetResult.NotSuitable,
    is Kt02h20FlatResetResult.DeviceUnavailable,
    is Kt02h20FlatResetResult.VerificationFailed,
    -> true
    is Kt02h20FlatResetResult.TransferFailed -> false
}

private fun Kt02h20FlatResetResult.failureReason(): String? = when (this) {
    is Kt02h20FlatResetResult.Success -> null
    is Kt02h20FlatResetResult.NotSuitable -> reason
    is Kt02h20FlatResetResult.DeviceUnavailable -> reason
    is Kt02h20FlatResetResult.TransferFailed -> reason
    is Kt02h20FlatResetResult.VerificationFailed -> reason
}
