package com.weekssa.opraeqforuapp.domain.dac

import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DacCapabilityCatalogTest {
    @Test
    fun blackPearlIdentityIsQualifiedButAdditionalControlsRemainHardwareGated() {
        val capabilities = DacCapabilityCatalog.forDevice(DacDeviceId.TRN_BLACK_PEARL)

        assertEquals(DacValidationStatus.HARDWARE_QUALIFIED, capabilities.identity.validationStatus)
        assertEquals(0x3302, capabilities.identity.usbVendorId)
        assertEquals(0x43E8, capabilities.identity.usbProductId)
        assertTrue(capabilities.exposedControls.isEmpty())
    }

    @Test
    fun ja11ExposesSoftwareEstablishedControlsWhilePhysicalValidationRemainsPending() {
        val capabilities = DacCapabilityCatalog.forDevice(DacDeviceId.FIIO_JA11)

        assertEquals(DacValidationStatus.HARDWARE_VALIDATION_PENDING, capabilities.identity.validationStatus)
        assertEquals(0x2972, capabilities.identity.usbVendorId)
        assertEquals(0x0102, capabilities.identity.usbProductId)
        assertEquals(FiioJa11DeviceControls.descriptors, capabilities.exposedControls)
        assertTrue(capabilities.exposedControls.isNotEmpty())
    }

    @Test
    fun legacyJcallyIdentityHasNoCurrentProductControls() {
        val capabilities = DacCapabilityCatalog.forDevice(DacDeviceId.JCALLY_JM12_STOCK)

        assertEquals(DacValidationStatus.HARDWARE_VALIDATION_PENDING, capabilities.identity.validationStatus)
        assertTrue(capabilities.exposedControls.isEmpty())
    }

    @Test
    fun blackPearlUnsupportedSectionsDisappearNaturallyWhenNoControlsAreExposed() {
        val capabilities = DacCapabilityCatalog.forDevice(DacDeviceId.TRN_BLACK_PEARL)

        DacControlSection.entries.forEach { section ->
            assertTrue(capabilities.controlsIn(section).isEmpty())
        }
    }
}
