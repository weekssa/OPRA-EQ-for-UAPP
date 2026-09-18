package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashPlan
import com.weekssa.opraeqforuapp.domain.blackpearl.buildBlackPearlFlashPlan
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20DeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.adaptationSummary
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs

internal enum class LibraryHardwareFlashDevice(val displayName: String) {
    BLACK_PEARL("TRN Black Pearl"),
    FIIO_JA11("FiiO JA11"),
    SIMGOT_EW300("SIMGOT EW300 DSP"),
}

internal sealed interface LibraryHardwareFlashPreview {
    val device: LibraryHardwareFlashDevice

    data class Ready(
        override val device: LibraryHardwareFlashDevice,
        val fidelity: DevicePresetFidelity,
        val adaptationSummary: String,
        val gainDb: Double,
        val warning: String? = null,
    ) : LibraryHardwareFlashPreview

    data class NotSuitable(
        override val device: LibraryHardwareFlashDevice,
        val reason: String,
    ) : LibraryHardwareFlashPreview
}

/**
 * EQ Library may offer one direct Flash action only when exactly one supported DAC session is open.
 * If both are connected, the app deliberately does not guess which physical DAC the user meant.
 */
internal fun connectedLibraryHardwareFlashDevice(
    blackPearlConnected: Boolean,
    fiioJa11Connected: Boolean,
    ew300Connected: Boolean = false,
): LibraryHardwareFlashDevice? = when {
    listOf(blackPearlConnected, fiioJa11Connected, ew300Connected).count { it } != 1 -> null
    blackPearlConnected -> LibraryHardwareFlashDevice.BLACK_PEARL
    fiioJa11Connected -> LibraryHardwareFlashDevice.FIIO_JA11
    ew300Connected -> LibraryHardwareFlashDevice.SIMGOT_EW300
    else -> null
}

internal fun libraryHardwareFlashPreview(
    profile: OpraEqProfile,
    device: LibraryHardwareFlashDevice,
): LibraryHardwareFlashPreview = when (device) {
    LibraryHardwareFlashDevice.BLACK_PEARL -> when (
        val plan = buildBlackPearlFlashPlan(profile, activeSlot = 0x00)
    ) {
        is BlackPearlFlashPlan.Ready -> LibraryHardwareFlashPreview.Ready(
            device = device,
            fidelity = plan.fidelity,
            adaptationSummary = plan.adaptationSummary,
            gainDb = plan.requiredPlaybackGainDb,
            warning = plan.warning,
        )
        is BlackPearlFlashPlan.NotRepresentable -> LibraryHardwareFlashPreview.NotSuitable(
            device = device,
            reason = plan.reason,
        )
    }

    LibraryHardwareFlashDevice.FIIO_JA11 -> when (
        val result = Kt02h20FiveBandOptimizer.optimize(profile, Kt02h20DeviceSpecs.FIIO_JA11)
    ) {
        is FiveBandOptimizationResult.Ready -> LibraryHardwareFlashPreview.Ready(
            device = device,
            fidelity = result.representation.fidelity,
            adaptationSummary = result.representation.adaptationSummary(),
            gainDb = result.representation.playbackGainDb,
        )
        is FiveBandOptimizationResult.NotSuitable -> LibraryHardwareFlashPreview.NotSuitable(
            device = device,
            reason = result.reason,
        )
    }

    LibraryHardwareFlashDevice.SIMGOT_EW300 -> when {
        profile.preampGainDb != null -> LibraryHardwareFlashPreview.NotSuitable(
            device = device,
            reason = "This EW300 production path currently requires a profile without source preamp; its global-gain mapping is not enabled yet.",
        )
        else -> when (
            val result = Kt02h20FiveBandOptimizer.optimize(profile, HardwareEqDeviceSpecs.SIMGOT_EW300)
        ) {
            is FiveBandOptimizationResult.Ready -> if (kotlin.math.abs(result.representation.playbackGainDb) > 0.000_001) {
                LibraryHardwareFlashPreview.NotSuitable(
                    device = device,
                    reason = "This EW300 production path cannot apply the profile's required playback headroom until the global-gain mapping is verified.",
                )
            } else {
                LibraryHardwareFlashPreview.Ready(
                    device = device,
                    fidelity = result.representation.fidelity,
                    adaptationSummary = result.representation.adaptationSummary(),
                    gainDb = result.representation.playbackGainDb,
                )
            }
            is FiveBandOptimizationResult.NotSuitable -> LibraryHardwareFlashPreview.NotSuitable(
                device = device,
                reason = result.reason,
            )
        }
    }
}
