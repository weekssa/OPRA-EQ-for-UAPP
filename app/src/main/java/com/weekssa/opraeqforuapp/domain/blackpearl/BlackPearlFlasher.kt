package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity

interface BlackPearlTransport {
    suspend fun readActiveSlot(): Byte?
    suspend fun sendReport(report: ByteArray): Boolean
}

sealed interface BlackPearlFlashResult {
    data class Success(
        val fidelity: DevicePresetFidelity,
        val warning: String?,
    ) : BlackPearlFlashResult

    data class NotRepresentable(val reason: String) : BlackPearlFlashResult
    data class DeviceUnavailable(val reason: String) : BlackPearlFlashResult
    data class TransferFailed(val reason: String) : BlackPearlFlashResult
}

class BlackPearlFlasher(
    private val transport: BlackPearlTransport,
) {
    suspend fun flash(profile: OpraEqProfile): BlackPearlFlashResult {
        val activeSlot = transport.readActiveSlot()
            ?: return BlackPearlFlashResult.DeviceUnavailable(
                "Couldn’t read the Black Pearl active EQ slot. Reconnect the DAC and try again.",
            )

        return when (val plan = buildBlackPearlFlashPlan(profile, activeSlot)) {
            is BlackPearlFlashPlan.NotRepresentable -> BlackPearlFlashResult.NotRepresentable(plan.reason)
            is BlackPearlFlashPlan.Ready -> {
                plan.reports.forEachIndexed { index, report ->
                    if (!transport.sendReport(report)) {
                        return BlackPearlFlashResult.TransferFailed(
                            "Black Pearl stopped accepting EQ data during Flash at step ${index + 1} of ${plan.reports.size}. The app did not send any non-EQ DAC controls.",
                        )
                    }
                }
                BlackPearlFlashResult.Success(
                    fidelity = plan.fidelity,
                    warning = plan.warning,
                )
            }
        }
    }
}
