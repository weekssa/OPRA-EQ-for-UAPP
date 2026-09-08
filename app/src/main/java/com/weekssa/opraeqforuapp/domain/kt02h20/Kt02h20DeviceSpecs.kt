package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs

/**
 * Compatibility names for the first JA11/JM12 implementation.
 *
 * The authoritative direct-hardware capability profiles now live in [HardwareEqDeviceSpecs] so new
 * DACs join the same acoustic adaptation framework instead of creating another optimizer path.
 */
object Kt02h20DeviceSpecs {
    val FIIO_JA11: FiveBandDeviceSpec = HardwareEqDeviceSpecs.FIIO_JA11
    val JCALLY_JM12_STOCK: FiveBandDeviceSpec = HardwareEqDeviceSpecs.JCALLY_JM12_STOCK
}
