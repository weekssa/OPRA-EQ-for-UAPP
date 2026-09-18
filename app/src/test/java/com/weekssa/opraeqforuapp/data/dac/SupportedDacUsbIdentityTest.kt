package com.weekssa.opraeqforuapp.data.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlProtocol
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Protocol
import org.junit.Test

class SupportedDacUsbIdentityTest {
    @Test
    fun exactCurrentProductVidPidPairsMapToTheirDeviceIds() {
        assertThat(
            supportedDacDeviceId(BlackPearlProtocol.VENDOR_ID, BlackPearlProtocol.PRODUCT_ID),
        ).isEqualTo(DacDeviceId.TRN_BLACK_PEARL)
        assertThat(
            supportedDacDeviceId(FiioJa11Protocol.VENDOR_ID, FiioJa11Protocol.PRODUCT_ID_UAC_1),
        ).isEqualTo(DacDeviceId.FIIO_JA11)
        assertThat(
            supportedDacDeviceId(FiioJa11Protocol.VENDOR_ID, FiioJa11Protocol.PRODUCT_ID_UAC_2),
        ).isEqualTo(DacDeviceId.FIIO_JA11)
    }

    @Test
    fun legacyJcallyIdentityFallsThroughToUnsupportedDeviceBehavior() {
        assertThat(
            supportedDacDeviceId(JcallyJm12Protocol.VENDOR_ID, JcallyJm12Protocol.PRODUCT_ID),
        ).isNull()
    }

    @Test
    fun sameVendorWithWrongProductDoesNotMatch() {
        assertThat(
            supportedDacDeviceId(FiioJa11Protocol.VENDOR_ID, 0x0103),
        ).isNull()
    }

    @Test
    fun sameProductWithWrongVendorDoesNotMatch() {
        assertThat(
            supportedDacDeviceId(FiioJa11Protocol.VENDOR_ID + 1, FiioJa11Protocol.PRODUCT_ID_UAC_2),
        ).isNull()
    }

    @Test
    fun registryContainsBlackPearlBothFiioEnumerationsAndEw300() {
        assertThat(supportedDacUsbIdentities).hasSize(4)
        assertThat(supportedDacUsbIdentities.count { it.deviceId == DacDeviceId.TRN_BLACK_PEARL }).isEqualTo(1)
        assertThat(supportedDacUsbIdentities.count { it.deviceId == DacDeviceId.FIIO_JA11 }).isEqualTo(2)
        assertThat(supportedDacUsbIdentities.count { it.deviceId == DacDeviceId.SIMGOT_EW300 }).isEqualTo(1)
        assertThat(supportedDacDeviceId(Ew300Protocol.VENDOR_ID, Ew300Protocol.PRODUCT_ID))
            .isEqualTo(DacDeviceId.SIMGOT_EW300)
        assertThat(supportedDacUsbIdentities.any { it.deviceId == DacDeviceId.JCALLY_JM12_STOCK }).isFalse()
    }
}
