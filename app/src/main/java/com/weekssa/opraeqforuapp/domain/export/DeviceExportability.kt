package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessUappCompatibility
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandDeviceSpec
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.adaptationSummary
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility

enum class DeviceExportability {
    EXACT,
    OPTIMIZED,
    NOT_REPRESENTABLE,
}

fun assessDeviceExportability(
    profile: OpraEqProfile,
    device: ExportDevice,
): DeviceExportability = when (device) {
    ExportDevice.UAPP -> {
        val compatibility = profile.assessUappCompatibility().category
        if (compatibility == ProfileCompatibility.NotCompatible) {
            DeviceExportability.NOT_REPRESENTABLE
        } else {
            when (determineDeviceFidelity(profile, requireNotNull(device.eqCapabilities))) {
                DevicePresetFidelity.EXACT -> DeviceExportability.EXACT
                DevicePresetFidelity.OPTIMIZED -> DeviceExportability.OPTIMIZED
            }
        }
    }
    ExportDevice.BLACK_PEARL -> assessHardware(profile, HardwareEqDeviceSpecs.TRN_BLACK_PEARL)
    ExportDevice.FIIO_JA11 -> assessHardware(profile, HardwareEqDeviceSpecs.FIIO_JA11)
    ExportDevice.JCALLY_JM12 -> assessHardware(profile, HardwareEqDeviceSpecs.JCALLY_JM12_STOCK)
    else -> buildFileExportDeviceVariant(profile, device)?.let { variant ->
        when (variant.fidelity) {
            DevicePresetFidelity.EXACT -> DeviceExportability.EXACT
            DevicePresetFidelity.OPTIMIZED -> DeviceExportability.OPTIMIZED
        }
    } ?: DeviceExportability.NOT_REPRESENTABLE
}

/**
 * Gives finite-hardware rows a concise reason for Exact/Optimized without mixing target adaptation
 * with source/catalog description text.
 */
fun deviceAdaptationSummary(
    profile: OpraEqProfile,
    device: ExportDevice,
): String? = when (device) {
    ExportDevice.BLACK_PEARL -> hardwareSummary(profile, HardwareEqDeviceSpecs.TRN_BLACK_PEARL)
    ExportDevice.FIIO_JA11 -> hardwareSummary(profile, HardwareEqDeviceSpecs.FIIO_JA11)
    ExportDevice.JCALLY_JM12 -> hardwareSummary(profile, HardwareEqDeviceSpecs.JCALLY_JM12_STOCK)
    else -> null
}

private fun assessHardware(
    profile: OpraEqProfile,
    spec: FiveBandDeviceSpec,
): DeviceExportability = when (val result = Kt02h20FiveBandOptimizer.optimize(profile, spec)) {
    is FiveBandOptimizationResult.NotSuitable -> DeviceExportability.NOT_REPRESENTABLE
    is FiveBandOptimizationResult.Ready -> when (result.representation.fidelity) {
        DevicePresetFidelity.EXACT -> DeviceExportability.EXACT
        DevicePresetFidelity.OPTIMIZED -> DeviceExportability.OPTIMIZED
    }
}

private fun hardwareSummary(
    profile: OpraEqProfile,
    spec: FiveBandDeviceSpec,
): String? = when (val result = Kt02h20FiveBandOptimizer.optimize(profile, spec)) {
    is FiveBandOptimizationResult.NotSuitable -> null
    is FiveBandOptimizationResult.Ready -> result.representation.adaptationSummary()
}

fun OpraEqProfile.isExportableToAny(devices: Set<ExportDevice>): Boolean =
    devices.any { device -> assessDeviceExportability(this, device) != DeviceExportability.NOT_REPRESENTABLE }
