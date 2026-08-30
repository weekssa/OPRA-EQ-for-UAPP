package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
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
        val compatibility = profile.assessCompatibility().category
        if (compatibility == ProfileCompatibility.NotCompatible) {
            DeviceExportability.NOT_REPRESENTABLE
        } else {
            when (determineDeviceFidelity(profile, requireNotNull(device.eqCapabilities))) {
                DevicePresetFidelity.EXACT -> DeviceExportability.EXACT
                DevicePresetFidelity.OPTIMIZED -> DeviceExportability.OPTIMIZED
            }
        }
    }
    else -> buildTextDeviceVariant(profile, device)?.let { variant ->
        when (variant.fidelity) {
            DevicePresetFidelity.EXACT -> DeviceExportability.EXACT
            DevicePresetFidelity.OPTIMIZED -> DeviceExportability.OPTIMIZED
        }
    } ?: DeviceExportability.NOT_REPRESENTABLE
}

fun OpraEqProfile.isExportableToAny(devices: Set<ExportDevice>): Boolean =
    devices.any { device -> assessDeviceExportability(this, device) != DeviceExportability.NOT_REPRESENTABLE }
