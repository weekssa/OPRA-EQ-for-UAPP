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

class BlackPearlFlasher(
    private val transport: BlackPearlTransport,
    private val gainStateStore: BlackPearlGainStateStore,
) {
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
}

private fun formatDb(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
