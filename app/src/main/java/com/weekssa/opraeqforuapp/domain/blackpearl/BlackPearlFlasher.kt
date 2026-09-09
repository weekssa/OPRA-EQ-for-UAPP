package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity

interface BlackPearlTransport {
    suspend fun readActiveSlot(): Byte?
    suspend fun readGlobalGainRaw(): Int?
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
    fun readTrackedAppliedPlaybackGainDb(): Double =
        BlackPearlProtocol.rawDeltaToGainDb(gainStateStore.readAppliedGainDeltaRaw())

    suspend fun flash(profile: OpraEqProfile): BlackPearlFlashResult {
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
        val baselineGainRaw = currentGainRaw - previousEqDeltaRaw
        val requestedDeltaRaw = BlackPearlProtocol.gainDbToRawDelta(plan.requiredPlaybackGainDb)
        val targetGainRaw = baselineGainRaw + requestedDeltaRaw
        if (targetGainRaw !in BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW..BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW) {
            return BlackPearlFlashResult.NotRepresentable(
                "Applying ${formatDb(plan.requiredPlaybackGainDb)} dB of playback gain would exceed the Black Pearl's validated volume range. Adjust the DAC volume and try again.",
            )
        }

        if (targetGainRaw != currentGainRaw) {
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
            if (!transport.sendReport(report)) {
                return BlackPearlFlashResult.TransferFailed(
                    "Black Pearl stopped accepting EQ data during Flash at step ${index + 1} of ${plan.reports.size}. The playback-gain state is retained so a retry will not apply it twice.",
                )
            }
        }
        return BlackPearlFlashResult.Success(
            fidelity = plan.fidelity,
            appliedPlaybackGainDb = BlackPearlProtocol.rawDeltaToGainDb(requestedDeltaRaw),
            warning = plan.warning,
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