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
 * The FiiO JA11 intentionally has two product IDs because switching UAC mode re-enumerates the same
 * physical model as 0x0101 (UAC 1.0) or 0x0102 (UAC 2.0). Both must resolve to FIIO_JA11 so the app
 * does not make the device appear unsupported merely because its USB-audio compatibility mode changed.
 *
 * JCALLY is deliberately absent from the supported registry. If legacy/research hardware is
 * connected, it follows the normal unsupported-device path and cannot surface hidden JCALLY flows.
 * Historical protocol research remains outside this current-product recognition boundary.
 */
val supportedDacUsbIdentities: List<SupportedDacUsbIdentity> = buildList {
    add(
        SupportedDacUsbIdentity(
            deviceId = DacDeviceId.TRN_BLACK_PEARL,
            vendorId = BlackPearlProtocol.VENDOR_ID,
            productId = BlackPearlProtocol.PRODUCT_ID,
        ),
    )
    FiioJa11Protocol.SUPPORTED_PRODUCT_IDS.sorted().forEach { productId ->
        add(
            SupportedDacUsbIdentity(
                deviceId = DacDeviceId.FIIO_JA11,
                vendorId = FiioJa11Protocol.VENDOR_ID,
                productId = productId,
            ),
        )
    }
}

fun supportedDacDeviceId(vendorId: Int, productId: Int): DacDeviceId? =
    supportedDacUsbIdentities.firstOrNull { identity ->
        identity.vendorId == vendorId && identity.productId == productId
    }?.deviceId
