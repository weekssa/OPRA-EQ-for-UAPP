package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.adaptationSummary
import java.util.Locale

sealed interface BlackPearlFlashPlan {
    data class Ready(
        val reports: List<ByteArray>,
        val requiredPlaybackGainDb: Double,
        val fidelity: DevicePresetFidelity,
        /** Legacy diagnostic count; optimized profiles are response-fitted, not first-N truncated. */
        val omittedBandCount: Int,
        val warning: String? = null,
        val representationVersion: Int = 1,
        val rmsErrorDb: Double = 0.0,
        val maxAbsoluteErrorDb: Double = 0.0,
    ) : BlackPearlFlashPlan

    data class NotRepresentable(val reason: String) : BlackPearlFlashPlan
}

/**
 * Builds the Black Pearl PEQ portion of a direct-Flash transaction from the shared hardware
 * adaptation plan. Global playback-gain application is handled by [BlackPearlFlasher] after it reads
 * current hardware state, because final absolute representability depends on that baseline and the
 * previous EQ Library-applied adjustment.
 */
fun buildBlackPearlFlashPlan(
    profile: OpraEqProfile,
    activeSlot: Byte,
): BlackPearlFlashPlan {
    if (profile.profileType != "parametric_eq") {
        return BlackPearlFlashPlan.NotRepresentable("Direct Flash requires a parametric EQ profile.")
    }

    val representation = when (
        val result = Kt02h20FiveBandOptimizer.optimize(profile, HardwareEqDeviceSpecs.TRN_BLACK_PEARL)
    ) {
        is FiveBandOptimizationResult.NotSuitable -> return BlackPearlFlashPlan.NotRepresentable(result.reason)
        is FiveBandOptimizationResult.Ready -> result.representation
    }

    val prepared = representation.bands.mapIndexed { index, band ->
        BlackPearlProtocol.Band(
            type = band.type,
            frequencyHz = band.frequencyHz,
            gainDb = band.gainDb,
            q = band.q,
        ).also { preparedBand ->
            val failure = runCatching {
                BlackPearlProtocol.writeBandReport(index, preparedBand, activeSlot)
            }.exceptionOrNull()
            if (failure != null) {
                return BlackPearlFlashPlan.NotRepresentable(
                    failure.message ?: "Band ${index + 1} is outside the representable Black Pearl EQ capability profile.",
                )
            }
        }
    }

    val warnings = buildList {
        if (representation.fidelity == DevicePresetFidelity.OPTIMIZED) {
            val metrics = if (representation.usedResponseFit) {
                " RMS ${formatMetric(representation.rmsErrorDb)} dB, max ${formatMetric(representation.maxAbsoluteErrorDb)} dB."
            } else {
                ""
            }
            add("Black Pearl: Optimized · ${representation.adaptationSummary()}.$metrics".trim())
        }

        val outsideValidatedGainRange = prepared.mapIndexedNotNull { index, band ->
            band.gainDb.takeUnless(BlackPearlProtocol::isBandGainWithinValidatedRange)?.let { gain ->
                "Band ${index + 1} ${String.format(Locale.US, "%+.2f", gain)} dB"
            }
        }
        if (outsideValidatedGainRange.isNotEmpty()) {
            add(
                "Caution: ${outsideValidatedGainRange.joinToString()} is outside EQ Library's currently validated " +
                    "Black Pearl filter-gain range of -10 dB..+10 dB. The exact ${if (outsideValidatedGainRange.size == 1) "value" else "values"} " +
                    "will be sent unchanged and will not be clamped. The protocol can encode ${if (outsideValidatedGainRange.size == 1) "this value" else "these values"}, " +
                    "but physical-hardware behavior outside the validated range has not yet been confirmed.",
            )
        }
    }

    return BlackPearlFlashPlan.Ready(
        reports = BlackPearlProtocol.flashSequence(prepared, activeSlot),
        requiredPlaybackGainDb = representation.playbackGainDb,
        fidelity = representation.fidelity,
        omittedBandCount = (profile.bands.orEmpty().size - prepared.size).coerceAtLeast(0),
        warning = warnings.takeIf(List<String>::isNotEmpty)?.joinToString("\n\n"),
        representationVersion = representation.representationVersion,
        rmsErrorDb = representation.rmsErrorDb,
        maxAbsoluteErrorDb = representation.maxAbsoluteErrorDb,
    )
}

private fun formatMetric(value: Double): String = String.format(Locale.US, "%.2f", value)
