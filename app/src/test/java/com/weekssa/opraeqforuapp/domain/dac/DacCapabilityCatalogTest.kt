package com.weekssa.opraeqforuapp.domain.dac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DacCapabilityCatalogTest {
    @Test
    fun blackPearlIdentityIsQualifiedButAdditionalControlsRemainHidden() {
        val capabilities = DacCapabilityCatalog.forDevice(DacDeviceId.TRN_BLACK_PEARL)

        assertEquals(DacValidationStatus.HARDWARE_QUALIFIED, capabilities.identity.validationStatus)
        assertEquals(0x3302, capabilities.identity.usbVendorId)
        assertEquals(0x43E8, capabilities.identity.usbProductId)
        assertTrue(capabilities.exposedControls.isEmpty())
    }

    @Test
    fun ja11AndStockJm12RemainHardwareValidationPendingWithNoDeviceControls() {
        listOf(DacDeviceId.FIIO_JA11, DacDeviceId.JCALLY_JM12_STOCK).forEach { deviceId ->
            val capabilities = DacCapabilityCatalog.forDevice(deviceId)
            assertEquals(DacValidationStatus.HARDWARE_VALIDATION_PENDING, capabilities.identity.validationStatus)
            assertTrue(capabilities.exposedControls.isEmpty())
        }
    }

    @Test
    fun unsupportedSectionsDisappearNaturallyWhenNoControlsAreExposed() {
        val capabilities = DacCapabilityCatalog.forDevice(DacDeviceId.TRN_BLACK_PEARL)

        DacControlSection.entries.forEach { section ->
            assertTrue(capabilities.controlsIn(section).isEmpty())
        }
    }
}
