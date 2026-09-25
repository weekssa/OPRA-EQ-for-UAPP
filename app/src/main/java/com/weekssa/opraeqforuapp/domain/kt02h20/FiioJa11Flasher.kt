package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import kotlin.math.abs
import kotlinx.coroutines.CancellationException

interface FiioJa11Transport {
    val deviceFingerprintKey: String?
        get() = null
    val usbProductId: Int?
        get() = null
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
    suspend fun sendReport(report: ByteArray): Boolean

    /**
     * Save is a device-specific lifecycle boundary. The default keeps deterministic fakes and
     * non-Android transports compatible; Android JA11 overrides it to await the documented
     * power-cycle/re-enumeration before final readback.
     */
    suspend fun saveToFlash(): Boolean = sendReport(FiioJa11Protocol.saveToFlashReport())
}

/**
 * Direct-Flash transaction for the normal FiiO JA11 run-mode PEQ protocol.
 *
 * The five editable coefficients live in User 1. A complete Flash therefore writes every User 1
 * band, writes the global EQ gain, explicitly selects User 1, applies, verifies the active program
 * and coefficients, saves, then verifies again. This prevents a successful write to an inactive
 * User 1 bank from being misreported as an audible EQ change while Vocal/Classic/Bass/Off is active.
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

        val baselineProgram = transport.readEqProgram()
        val baselineGlobalGainDb = transport.readGlobalGainDb()
        val baselineBands = (0 until FiioJa11Protocol.BAND_COUNT).map { index ->
            transport.readBand(index)
        }
        if (baselineProgram == null || baselineGlobalGainDb == null || baselineBands.any { it == null }) {
            return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t read the FiiO JA11 PEQ state. Reconnect the DAC and try again.",
            )
        }
        trace.baselineRead(
            program = baselineProgram,
            globalGainDb = baselineGlobalGainDb,
            bands = baselineBands.filterNotNull(),
        )

        trace.stage(FiioJa11OperationStage.WRITING)
        targetBands.forEachIndexed { index, band ->
            if (!transport.sendReport(FiioJa11Protocol.writeBandReport(index, band))) {
                return Kt02h20FlashResult.TransferFailed(
                    "FiiO JA11 stopped accepting PEQ data at band ${index + 1} of ${FiioJa11Protocol.BAND_COUNT}. The preset was not reported as applied.",
                )
            }
        }
        if (!transport.sendReport(FiioJa11Protocol.writeGlobalGainReport(representation.playbackGainDb))) {
            return Kt02h20FlashResult.TransferFailed(
                "FiiO JA11 did not accept the required global EQ gain. The preset was not reported as applied.",
            )
        }
        if (!transport.sendReport(FiioJa11Protocol.writeEqProgramReport(FiioJa11Protocol.EqProgram.USER_1))) {
            return Kt02h20FlashResult.TransferFailed(
                "FiiO JA11 did not accept selection of the User 1 PEQ program. The preset was not reported as applied.",
            )
        }
        if (!transport.sendReport(FiioJa11Protocol.applyReport())) {
            return Kt02h20FlashResult.TransferFailed(
                "FiiO JA11 did not accept the Apply command. Reconnect the DAC and try again.",
            )
        }
        trace.stage(FiioJa11OperationStage.APPLY_SENT)

        verifyTarget(targetBands, representation.playbackGainDb, trace, "VOLATILE_READBACK")?.let { reason ->
            return Kt02h20FlashResult.VerificationFailed(reason)
        }

        trace.saveSent()
        if (!transport.saveToFlash()) {
            return Kt02h20FlashResult.TransferFailed(
                "The PEQ was applied to the FiiO JA11, but the device did not complete the persistent Save/reconnect boundary.",
            )
        }

        verifyTarget(targetBands, representation.playbackGainDb, trace, "FINAL_READBACK")?.let { reason ->
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

    private suspend fun resetInternal(trace: FiioJa11OperationTraceBuilder): Kt02h20FlatResetResult {
        val baselineProgram = transport.readEqProgram()
        val baselineGlobalGainDb = transport.readGlobalGainDb()
        val baselineBands = (0 until FiioJa11Protocol.BAND_COUNT).map { index ->
            transport.readBand(index)
        }
        if (baselineProgram == null || baselineGlobalGainDb == null || baselineBands.any { it == null }) {
            return Kt02h20FlatResetResult.DeviceUnavailable(
                "Couldn’t read the FiiO JA11 PEQ state. Reconnect the DAC and try again.",
            )
        }
        trace.baselineRead(
            program = baselineProgram,
            globalGainDb = baselineGlobalGainDb,
            bands = baselineBands.filterNotNull(),
        )
        val flatBands = FiioJa11Protocol.completeBands(emptyList())
        trace.stage(FiioJa11OperationStage.WRITING)
        flatBands.forEachIndexed { index, band ->
            if (!transport.sendReport(FiioJa11Protocol.writeBandReport(index, band))) {
                return Kt02h20FlatResetResult.TransferFailed(
                    "FiiO JA11 stopped accepting the flat-EQ reset at band ${index + 1} of ${FiioJa11Protocol.BAND_COUNT}.",
                )
            }
        }
        if (!transport.sendReport(FiioJa11Protocol.writeGlobalGainReport(0.0))) {
            return Kt02h20FlatResetResult.TransferFailed(
                "FiiO JA11 did not accept the 0 dB global EQ gain for Reset.",
            )
        }
        if (!transport.sendReport(FiioJa11Protocol.writeEqProgramReport(FiioJa11Protocol.EqProgram.USER_1))) {
            return Kt02h20FlatResetResult.TransferFailed(
                "FiiO JA11 did not accept selection of the flat User 1 EQ for Reset.",
            )
        }
        if (!transport.sendReport(FiioJa11Protocol.applyReport())) {
            return Kt02h20FlatResetResult.TransferFailed("FiiO JA11 did not accept the flat-EQ Apply command.")
        }
        trace.stage(FiioJa11OperationStage.APPLY_SENT)
        verifyTarget(flatBands, 0.0, trace, "VOLATILE_READBACK")?.let { reason ->
            return Kt02h20FlatResetResult.VerificationFailed(reason)
        }
        trace.saveSent()
        if (!transport.saveToFlash()) {
            return Kt02h20FlatResetResult.TransferFailed(
                "The FiiO JA11 PEQ is flat in the current session, but the device did not complete the persistent Save/reconnect boundary.",
            )
        }
        verifyTarget(flatBands, 0.0, trace, "FINAL_READBACK")?.let { reason ->
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
        trace: FiioJa11OperationTraceBuilder? = null,
        phase: String = "VOLATILE_READBACK",
    ): String? {
        if (transport.readEqProgram() != FiioJa11Protocol.EqProgram.USER_1) {
            return "JA11 User 1 was not the active EQ program after Apply."
        }
        expectedBands.forEachIndexed { index, expected ->
            val actual = transport.readBand(index)
                ?: return "Couldn’t read back JA11 band ${index + 1}."
            if (!FiioJa11Protocol.nearlyMatches(expected, actual)) {
                return "JA11 band ${index + 1} readback did not match the intended value."
            }
        }
        val actualGain = transport.readGlobalGainDb()
            ?: run {
                trace?.compare(phase, null)
                return "Couldn’t read back the JA11 global EQ gain."
            }
        val wireExpected = FiioJa11Protocol.quantizedGlobalGainDb(expectedGlobalGainDb)
        trace?.compare(phase, actualGain)
        if (abs(actualGain - wireExpected) > GLOBAL_GAIN_READBACK_TOLERANCE_DB) {
            return "JA11 global EQ gain readback did not match the intended value."
        }
        return null
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
