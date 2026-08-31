package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity

sealed interface BlackPearlFlashPlan {
    data class Ready(
        val reports: List<ByteArray>,
        val requiredPlaybackGainDb: Double,
        val fidelity: DevicePresetFidelity,
        val omittedBandCount: Int,
        val warning: String? = null,
    ) : BlackPearlFlashPlan

    data class NotRepresentable(val reason: String) : BlackPearlFlashPlan
}

/**
 * Builds the Black Pearl PEQ portion of a direct-Flash transaction. Global playback-gain
 * application is handled by [BlackPearlFlasher] after it reads the current hardware state, because
 * representability depends on the current gain and the previous EQ Library-applied adjustment.
 */
fun buildBlackPearlFlashPlan(
    profile: OpraEqProfile,
    activeSlot: Byte,
): BlackPearlFlashPlan {
    if (profile.profileType != "parametric_eq") {
        return BlackPearlFlashPlan.NotRepresentable("Direct Flash requires a parametric EQ profile.")
    }

    val playbackGainDb = profile.effectivePlaybackPreampDb()
        ?.takeIf(Double::isFinite)
        ?: return BlackPearlFlashPlan.NotRepresentable(
            "This EQ has no source preamp or generated safety headroom, so direct Flash cannot determine a safe playback-gain adjustment.",
        )

    val sourceBands = profile.bands.orEmpty()
    if (sourceBands.isEmpty()) {
        return BlackPearlFlashPlan.NotRepresentable("This EQ has no flashable parametric bands.")
    }

    val selectedBands = sourceBands.take(BlackPearlProtocol.BAND_COUNT)
    val prepared = selectedBands.mapIndexed { index, band ->
        val type = band.type
            ?: return BlackPearlFlashPlan.NotRepresentable("Band ${index + 1} is missing its filter type.")
        val frequency = band.frequency
            ?: return BlackPearlFlashPlan.NotRepresentable("Band ${index + 1} is missing its frequency.")
        val gain = band.gainDb
            ?: return BlackPearlFlashPlan.NotRepresentable("Band ${index + 1} is missing its gain.")
        val q = band.q
            ?: return BlackPearlFlashPlan.NotRepresentable("Band ${index + 1} is missing its Q value.")
        BlackPearlProtocol.Band(type, frequency, gain, q).also { preparedBand ->
            val failure = runCatching {
                BlackPearlProtocol.writeBandReport(index, preparedBand, activeSlot)
            }.exceptionOrNull()
            if (failure != null) {
                return BlackPearlFlashPlan.NotRepresentable(
                    failure.message ?: "Band ${index + 1} is outside the validated Black Pearl EQ capability profile.",
                )
            }
        }
    }

    val omitted = (sourceBands.size - prepared.size).coerceAtLeast(0)
    return BlackPearlFlashPlan.Ready(
        reports = BlackPearlProtocol.flashSequence(prepared, activeSlot),
        requiredPlaybackGainDb = playbackGainDb,
        fidelity = if (omitted == 0) DevicePresetFidelity.EXACT else DevicePresetFidelity.OPTIMIZED,
        omittedBandCount = omitted,
        warning = if (omitted > 0) {
            "Black Pearl supports 10 EQ bands. The first 10 source-priority bands will be flashed; $omitted lower-priority ${if (omitted == 1) "band" else "bands"} will be omitted."
        } else {
            null
        },
    )
}
