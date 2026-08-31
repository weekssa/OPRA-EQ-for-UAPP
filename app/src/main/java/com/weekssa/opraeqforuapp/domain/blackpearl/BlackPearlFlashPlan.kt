package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.effectivePlaybackPreampDb
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import kotlin.math.abs

sealed interface BlackPearlFlashPlan {
    data class Ready(
        val reports: List<ByteArray>,
        val fidelity: DevicePresetFidelity,
        val omittedBandCount: Int,
        val warning: String? = null,
    ) : BlackPearlFlashPlan

    data class NotRepresentable(val reason: String) : BlackPearlFlashPlan
}

/**
 * Builds the EQ-only direct-Flash transaction without changing any other DAC setting.
 *
 * The observed Black Pearl PEQ packet has no independently corroborated preamp field. Public
 * reference controllers implement imported preamp by changing global DAC volume, which is outside
 * EQ Library's approved scope. Any profile requiring non-zero attenuation is therefore rejected for
 * direct Flash rather than silently changing volume or dropping the preamp.
 */
fun buildBlackPearlFlashPlan(
    profile: OpraEqProfile,
    activeSlot: Byte,
): BlackPearlFlashPlan {
    if (profile.profileType != "parametric_eq") {
        return BlackPearlFlashPlan.NotRepresentable("Direct Flash requires a parametric EQ profile.")
    }

    val preamp = profile.effectivePlaybackPreampDb()
        ?: return BlackPearlFlashPlan.NotRepresentable(
            "This EQ has no source preamp or generated safety headroom, so direct Flash cannot prove safe playback without changing DAC volume.",
        )
    if (abs(preamp) > PREAMP_ZERO_TOLERANCE_DB) {
        return BlackPearlFlashPlan.NotRepresentable(
            "This EQ requires ${formatDb(preamp)} dB preamp/headroom. The Black Pearl PEQ protocol has no independently verified per-EQ preamp field, and EQ Library will not change global DAC volume.",
        )
    }

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
        fidelity = if (omitted == 0) DevicePresetFidelity.EXACT else DevicePresetFidelity.OPTIMIZED,
        omittedBandCount = omitted,
        warning = if (omitted > 0) {
            "Black Pearl supports 10 EQ bands. The first 10 source-priority bands will be flashed; $omitted lower-priority ${if (omitted == 1) "band" else "bands"} will be omitted."
        } else {
            null
        },
    )
}

private fun formatDb(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
private const val PREAMP_ZERO_TOLERANCE_DB = 0.000_001
