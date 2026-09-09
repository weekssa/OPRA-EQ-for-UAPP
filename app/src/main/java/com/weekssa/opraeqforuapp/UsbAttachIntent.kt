package com.weekssa.opraeqforuapp

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.weekssa.opraeqforuapp.data.dac.supportedDacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId

internal fun Intent.supportedAttachedDacDeviceId(): DacDeviceId? {
    if (action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return null
    val device = attachedUsbDevice() ?: return null
    return supportedDacDeviceId(device.vendorId, device.productId)
}

private fun Intent.attachedUsbDevice(): UsbDevice? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(UsbManager.EXTRA_DEVICE)
    }
