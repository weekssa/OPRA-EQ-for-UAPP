package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlProtocol
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol

data class SupportedDacUsbIdentity(
    val deviceId: DacDeviceId,
    val vendorId: Int,
    val productId: Int,
)

/**
 * Current product USB identities only.
 *
 * JCALLY is deliberately absent from the supported registry. If legacy/research hardware is
 * connected, it follows the normal unsupported-device path and cannot surface hidden JCALLY flows.
 * Historical protocol research remains outside this current-product recognition boundary.
 */
val supportedDacUsbIdentities: List<SupportedDacUsbIdentity> = listOf(
    SupportedDacUsbIdentity(
        deviceId = DacDeviceId.TRN_BLACK_PEARL,
        vendorId = BlackPearlProtocol.VENDOR_ID,
        productId = BlackPearlProtocol.PRODUCT_ID,
    ),
    SupportedDacUsbIdentity(
        deviceId = DacDeviceId.FIIO_JA11,
        vendorId = FiioJa11Protocol.VENDOR_ID,
        productId = FiioJa11Protocol.PRODUCT_ID,
    ),
)

fun supportedDacDeviceId(vendorId: Int, productId: Int): DacDeviceId? =
    supportedDacUsbIdentities.firstOrNull { identity ->
        identity.vendorId == vendorId && identity.productId == productId
    }?.deviceId
