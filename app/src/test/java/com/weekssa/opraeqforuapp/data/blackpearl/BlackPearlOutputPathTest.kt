package com.weekssa.opraeqforuapp.data.blackpearl

import android.hardware.usb.UsbConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlOutputPathTest {
    @Test
    fun usesInterruptOutWhenTheInterfaceExposesOne() {
        assertEquals(
            BlackPearlOutputPath.INTERRUPT_OUT,
            blackPearlOutputPath(hasInterruptOutEndpoint = true),
        )
    }

    @Test
    fun fallsBackToSetReportWhenNoInterruptOutExists() {
        assertEquals(
            BlackPearlOutputPath.CONTROL_SET_REPORT,
            blackPearlOutputPath(hasInterruptOutEndpoint = false),
        )
    }

    @Test
    fun onlyInterruptOutEndpointsAreEligibleForTheEndpointPath() {
        assertTrue(
            isBlackPearlInterruptOutEndpoint(
                direction = UsbConstants.USB_DIR_OUT,
                type = UsbConstants.USB_ENDPOINT_XFER_INT,
            ),
        )
        assertFalse(
            isBlackPearlInterruptOutEndpoint(
                direction = UsbConstants.USB_DIR_IN,
                type = UsbConstants.USB_ENDPOINT_XFER_INT,
            ),
        )
        assertFalse(
            isBlackPearlInterruptOutEndpoint(
                direction = UsbConstants.USB_DIR_OUT,
                type = UsbConstants.USB_ENDPOINT_XFER_BULK,
            ),
        )
    }

    @Test
    fun interruptOutRequiresTheCompleteReport() {
        assertTrue(
            blackPearlInterruptOutTransferSucceeded(
                transferredBytes = 64,
                expectedBytes = 64,
            ),
        )
        assertFalse(
            blackPearlInterruptOutTransferSucceeded(
                transferredBytes = 63,
                expectedBytes = 64,
            ),
        )
        assertFalse(
            blackPearlInterruptOutTransferSucceeded(
                transferredBytes = 0,
                expectedBytes = 64,
            ),
        )
        assertFalse(
            blackPearlInterruptOutTransferSucceeded(
                transferredBytes = -1,
                expectedBytes = 64,
            ),
        )
    }
}
