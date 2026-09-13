package com.weekssa.opraeqforuapp.domain.settings

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.export.ExportDevice

data class EffectiveOutputResolution(
    val output: ExportDevice,
    val automaticDevice: DacDeviceId? = null,
    val multipleSupportedDacsPresent: Boolean = false,
) {
    val isUsingAutomaticHardware: Boolean
        get() = automaticDevice != null
}

/**
 * Pure resolver for the user-friendly output policy.
 *
 * Automatic mode uses one physically present current-product DAC without requiring the user to
 * pre-enable/select that hardware output. Zero or multiple supported DACs never cause guessing and
 * fall back to the user's saved manual output. My DAC remains independently bound to actual hardware.
 */
object EffectiveOutputResolver {
    fun resolve(
        behavior: OutputBehavior,
        manualFallback: ExportDevice,
        presentDeviceIds: Set<DacDeviceId>,
    ): EffectiveOutputResolution {
        if (behavior == OutputBehavior.Manual) {
            return EffectiveOutputResolution(output = manualFallback)
        }

        val candidates = presentDeviceIds
            .mapNotNull { deviceId -> deviceId.toCurrentProductOutput()?.let { deviceId to it } }
            .distinctBy { (_, output) -> output }

        return when (candidates.size) {
            1 -> EffectiveOutputResolution(
                output = candidates.single().second,
                automaticDevice = candidates.single().first,
            )
            0 -> EffectiveOutputResolution(output = manualFallback)
            else -> EffectiveOutputResolution(
                output = manualFallback,
                multipleSupportedDacsPresent = true,
            )
        }
    }

    private fun DacDeviceId.toCurrentProductOutput(): ExportDevice? = when (this) {
        DacDeviceId.TRN_BLACK_PEARL -> ExportDevice.BLACK_PEARL
        DacDeviceId.FIIO_JA11 -> ExportDevice.FIIO_JA11
        DacDeviceId.JCALLY_JM12_STOCK -> null
    }
}
