package com.weekssa.opraeqforuapp.data.blackpearl

import android.hardware.usb.UsbConstants

/** Output path selected by the Black Pearl USB transport for an unchanged HID report. */
internal enum class BlackPearlOutputPath {
    INTERRUPT_OUT,
    CONTROL_SET_REPORT,
}

internal fun blackPearlOutputPath(hasInterruptOutEndpoint: Boolean): BlackPearlOutputPath =
    if (hasInterruptOutEndpoint) {
        BlackPearlOutputPath.INTERRUPT_OUT
    } else {
        BlackPearlOutputPath.CONTROL_SET_REPORT
    }

internal fun isBlackPearlInterruptOutEndpoint(
    direction: Int,
    type: Int,
): Boolean = direction == UsbConstants.USB_DIR_OUT && type == UsbConstants.USB_ENDPOINT_XFER_INT

internal fun blackPearlInterruptOutTransferSucceeded(
    transferredBytes: Int,
    expectedBytes: Int,
): Boolean = expectedBytes > 0 && transferredBytes == expectedBytes
