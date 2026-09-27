package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity

interface BlackPearlTransport {
    suspend fun readActiveSlot(): Byte?
    suspend fun readGlobalGainRaw(): Int?
    suspend fun readNativeBand(index: Int): BlackPearlReadCodec.NativeBand? = null
    suspend fun sendReport(report: ByteArray): Boolean
}

sealed interface BlackPearlFlashResult {
    data class Success(
        val fidelity: DevicePresetFidelity,
        val appliedPlaybackGainDb: Double,
        val warning: String?,
    ) : BlackPearlFlashResult

    data class NotRepresentable(val reason: String) : BlackPearlFlashResult
    data class DeviceUnavailable(val reason: String) : BlackPearlFlashResult
    data class TransferFailed(val reason: String) : BlackPearlFlashResult
    data class VerificationFailed(val reason: String) : BlackPearlFlashResult
}

sealed interface BlackPearlFlatResetResult {
    data class Success(
        val restoredPlaybackGainDb: Double,
    ) : BlackPearlFlatResetResult

    data class NotRepresentable(val reason: String) : BlackPearlFlatResetResult
    data class DeviceUnavailable(val reason: String) : BlackPearlFlatResetResult
    data class TransferFailed(val reason: String) : BlackPearlFlatResetResult
}

class BlackPearlFlasher(
    private val transport: BlackPearlTransport,
    private val gainStateStore: BlackPearlGainStateStore,
) {
    /**
     * Returns the app-owned relative playback-gain delta already tracked by the qualified Flash/Reset
     * path. This is local persisted state, not a USB read and not the DAC's absolute playback volume.
     */
    fun readTrackedAppliedPlaybackGainDb(): Double? =
        gainStateStore.readAppliedGainDeltaRaw()?.let(BlackPearlProtocol::rawDeltaToGainDb)

    /**
     * A complete fresh native snapshot is the recovery boundary after an unverified mutation. The
     * observed absolute gain is now the user's new baseline; do not invent an app-owned delta from
     * an earlier uncertain transaction.
     */
    fun establishFreshGainBaselineAfterVerifiedSnapshot() {
        if (gainStateStore.readAppliedGainDeltaRaw() == null) {
            gainStateStore.writeAppliedGainDeltaRaw(0)
        }
    }

    suspend fun applyEditorWorkingCopy(
        workingCopy: HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
    ): BlackPearlEditorApplyResult = BlackPearlEditorApplier(
        transport = transport,
        gainStateStore = gainStateStore,
    ).apply(
        workingCopy = workingCopy,
        allowCautions = allowCautions,
        isSessionCurrent = isSessionCurrent,
    )

    suspend fun flash(
        profile: OpraEqProfile,
        expectedSessionGeneration: Long,
        isSessionCurrent: (Long) -> Boolean,
    ): BlackPearlFlashResult {
        val activeSlot = transport.readActiveSlot()
            ?: return BlackPearlFlashResult.DeviceUnavailable(
                "Couldn’t read the Black Pearl active EQ slot. Reconnect the DAC and try again.",
            )

        val plan = when (val candidate = buildBlackPearlFlashPlan(profile, activeSlot)) {
            is BlackPearlFlashPlan.NotRepresentable -> return BlackPearlFlashResult.NotRepresentable(candidate.reason)
            is BlackPearlFlashPlan.Ready -> candidate
        }

        val currentGainRaw = transport.readGlobalGainRaw()
            ?: return BlackPearlFlashResult.DeviceUnavailable(
                "Couldn’t read the Black Pearl playback gain. Reconnect the DAC and try again.",
            )
        val previousEqDeltaRaw = gainStateStore.readAppliedGainDeltaRaw()
            ?: return BlackPearlFlashResult.VerificationFailed(
                "The Black Pearl tracked playback-gain baseline is unknown. Read the complete DAC state before any later write.",
            )
        val baselineGainRaw = currentGainRaw - previousEqDeltaRaw
        val requestedDeltaRaw = BlackPearlProtocol.gainDbToRawDelta(plan.requiredPlaybackGainDb)
        val targetGainRaw = baselineGainRaw + requestedDeltaRaw
        if (targetGainRaw !in BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW..BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW) {
            return BlackPearlFlashResult.NotRepresentable(
                "Applying ${formatDb(plan.requiredPlaybackGainDb)} dB of playback gain would exceed the Black Pearl's validated volume range. Adjust the DAC volume and try again.",
            )
        }

        val expectedBands = expectedNativeBands(plan, activeSlot)
            ?: return BlackPearlFlashResult.VerificationFailed(
                "The planned Black Pearl EQ could not be decoded for final hardware verification.",
            )
        if (!isSessionCurrent(expectedSessionGeneration)) {
            return BlackPearlFlashResult.VerificationFailed(
                "The Black Pearl USB session changed before Flash began. Read the DAC again before any later write.",
            )
        }

        if (targetGainRaw != currentGainRaw) {
            if (!isSessionCurrent(expectedSessionGeneration)) {
                return sessionChangedDuringFlash()
            }
            if (!transport.sendReport(BlackPearlProtocol.writeGlobalGainReport(targetGainRaw))) {
                return BlackPearlFlashResult.TransferFailed(
                    "Black Pearl did not accept the required playback-gain adjustment. No EQ bands were written.",
                )
            }
        }
        // Persist immediately after the hardware gain is known to be in the requested state. If a
        // later PEQ transfer fails, a retry must replace this adjustment rather than stack it.
        gainStateStore.writeAppliedGainDeltaRaw(requestedDeltaRaw)

        plan.reports.forEachIndexed { index, report ->
            if (!isSessionCurrent(expectedSessionGeneration)) {
                return sessionChangedDuringFlash()
            }
            if (!transport.sendReport(report)) {
                return BlackPearlFlashResult.TransferFailed(
                    "Black Pearl stopped accepting EQ data during Flash at step ${index + 1} of ${plan.reports.size}. The playback-gain state is retained so a retry will not apply it twice.",
                )
            }
        }

        if (!isSessionCurrent(expectedSessionGeneration)) {
            return sessionChangedDuringFlash()
        }
        val actualBands = buildList {
            repeat(BlackPearlProtocol.BAND_COUNT) { index ->
                val actual = transport.readNativeBand(index)
                if (actual == null) {
                    gainStateStore.markAppliedGainDeltaUnknown()
                    return BlackPearlFlashResult.VerificationFailed(
                        "Final Black Pearl EQ readback was unavailable at band ${index + 1}.",
                    )
                }
                add(actual)
            }
        }
        if (!isSessionCurrent(expectedSessionGeneration)) {
            return sessionChangedDuringFlash()
        }
        expectedBands.zip(actualBands).forEachIndexed { index, (expected, actual) ->
            nativeBandMismatchReason(expected, actual)?.let { mismatch ->
                gainStateStore.markAppliedGainDeltaUnknown()
                return BlackPearlFlashResult.VerificationFailed(
                    "Final Black Pearl EQ readback mismatch at band ${index + 1}: $mismatch.",
                )
            }
        }

        val finalGainRaw = transport.readGlobalGainRaw()
            ?: run {
                gainStateStore.markAppliedGainDeltaUnknown()
                return BlackPearlFlashResult.VerificationFailed(
                    "Final Black Pearl playback-gain readback was unavailable.",
                )
            }
        if (!isSessionCurrent(expectedSessionGeneration)) {
            return sessionChangedDuringFlash()
        }
        if (finalGainRaw != targetGainRaw) {
            // The raw readback is authoritative for the observed hardware state. Reconcile the
            // anti-stacking delta to it rather than retaining the requested delta as false truth.
            gainStateStore.writeAppliedGainDeltaRaw(finalGainRaw - baselineGainRaw)
            return BlackPearlFlashResult.VerificationFailed(
                "Final Black Pearl playback-gain readback did not match: expected raw $targetGainRaw, actual raw $finalGainRaw.",
            )
        }

        return BlackPearlFlashResult.Success(
            fidelity = plan.fidelity,
            appliedPlaybackGainDb = BlackPearlProtocol.rawDeltaToGainDb(requestedDeltaRaw),
            warning = plan.warning,
        )
    }

    private fun expectedNativeBands(
        plan: BlackPearlFlashPlan.Ready,
        activeSlot: Byte,
    ): List<BlackPearlReadCodec.NativeBand>? {
        if (plan.reports.size < BlackPearlProtocol.BAND_COUNT) return null
        return plan.reports
            .take(BlackPearlProtocol.BAND_COUNT)
            .map { report -> BlackPearlReadCodec.bandFromWriteReport(report) ?: return null }
            .takeIf { bands -> bands.all { it.activeSlot == (activeSlot.toInt() and 0xFF) } }
    }

    private fun nativeBandMismatchReason(
        expected: BlackPearlReadCodec.NativeBand,
        actual: BlackPearlReadCodec.NativeBand,
    ): String? = when {
        expected.index != actual.index -> "index expected ${expected.index} actual ${actual.index}"
        expected.activeSlot != actual.activeSlot -> "active slot expected ${expected.activeSlot} actual ${actual.activeSlot}"
        expected.type != actual.type -> "filter type expected ${expected.type} actual ${actual.type}"
        expected.frequencyRawHz != actual.frequencyRawHz ->
            "frequency raw expected ${expected.frequencyRawHz} actual ${actual.frequencyRawHz}"
        expected.gainRaw256 != actual.gainRaw256 ->
            "gain raw expected ${expected.gainRaw256} actual ${actual.gainRaw256}"
        expected.qRaw256 != actual.qRaw256 -> "Q raw expected ${expected.qRaw256} actual ${actual.qRaw256}"
        else -> null
    }

    private fun sessionChangedDuringFlash(): BlackPearlFlashResult.VerificationFailed {
        gainStateStore.markAppliedGainDeltaUnknown()
        return BlackPearlFlashResult.VerificationFailed(
            "The Black Pearl USB session changed during Flash. Final hardware state is uncertain; reconnect and read the DAC before any later write.",
        )
    }

    suspend fun resetToFlat(): BlackPearlFlatResetResult {
        val activeSlot = transport.readActiveSlot()
            ?: return BlackPearlFlatResetResult.DeviceUnavailable(
                "Couldn’t read the Black Pearl active EQ slot. Reconnect the DAC and try again.",
            )
        val currentGainRaw = transport.readGlobalGainRaw()
            ?: return BlackPearlFlatResetResult.DeviceUnavailable(
                "Couldn’t read the Black Pearl playback gain. Reconnect the DAC and try again.",
            )

        val previousEqDeltaRaw = gainStateStore.readAppliedGainDeltaRaw()
            ?: return BlackPearlFlatResetResult.DeviceUnavailable(
                "The Black Pearl tracked playback-gain baseline is unknown. Read the complete DAC state before Reset.",
            )
        val baselineGainRaw = currentGainRaw - previousEqDeltaRaw
        if (baselineGainRaw !in BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW..BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW) {
            return BlackPearlFlatResetResult.NotRepresentable(
                "Restoring the playback gain that existed before EQ Library's adjustment would exceed the Black Pearl's validated volume range. Adjust the DAC volume and try again.",
            )
        }

        // Flatten, latch, and persist the EQ slot before removing EQ Library's playback attenuation.
        // If USB transfer fails, the device keeps the safer pre-reset playback gain instead of
        // exposing a partially reset/old boosted EQ at a louder level.
        val reports = BlackPearlProtocol.flashSequence(emptyList(), activeSlot)
        reports.forEachIndexed { index, report ->
            if (!transport.sendReport(report)) {
                return BlackPearlFlatResetResult.TransferFailed(
                    "Black Pearl stopped accepting the flat-EQ reset at step ${index + 1} of ${reports.size}. Playback gain was not restored; reconnect and try again.",
                )
            }
        }

        if (baselineGainRaw != currentGainRaw) {
            if (!transport.sendReport(BlackPearlProtocol.writeGlobalGainReport(baselineGainRaw))) {
                return BlackPearlFlatResetResult.TransferFailed(
                    "The Black Pearl EQ slot is flat, but its playback gain could not be restored. The previous EQ Library gain adjustment is still tracked so a retry can finish safely.",
                )
            }
        }
        // Clear the tracked delta only after the slot is flat and the hardware gain is confirmed at
        // baseline. A failed gain write therefore remains safely retryable without losing state.
        gainStateStore.writeAppliedGainDeltaRaw(0)

        return BlackPearlFlatResetResult.Success(
            restoredPlaybackGainDb = BlackPearlProtocol.rawDeltaToGainDb(baselineGainRaw - currentGainRaw),
        )
    }
}

private fun formatDb(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
