package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlProtocol
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Protocol

data class SupportedDacUsbIdentity(
    val deviceId: DacDeviceId,
    val vendorId: Int,
    val productId: Int,
)

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
    SupportedDacUsbIdentity(
        deviceId = DacDeviceId.JCALLY_JM12_STOCK,
        vendorId = JcallyJm12Protocol.VENDOR_ID,
        productId = JcallyJm12Protocol.PRODUCT_ID,
    ),
)

fun supportedDacDeviceId(vendorId: Int, productId: Int): DacDeviceId? =
    supportedDacUsbIdentities.firstOrNull { identity ->
        identity.vendorId == vendorId && identity.productId == productId
    }?.deviceId
