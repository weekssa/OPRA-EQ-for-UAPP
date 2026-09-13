package com.weekssa.opraeqforuapp.data.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlProtocol
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
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
            supportedDacDeviceId(FiioJa11Protocol.VENDOR_ID, FiioJa11Protocol.PRODUCT_ID),
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
            supportedDacDeviceId(BlackPearlProtocol.VENDOR_ID, BlackPearlProtocol.PRODUCT_ID + 1),
        ).isNull()
    }

    @Test
    fun sameProductWithWrongVendorDoesNotMatch() {
        assertThat(
            supportedDacDeviceId(FiioJa11Protocol.VENDOR_ID + 1, FiioJa11Protocol.PRODUCT_ID),
        ).isNull()
    }

    @Test
    fun registryContainsOnlyBlackPearlAndFiioCurrentTargets() {
        assertThat(supportedDacUsbIdentities.map { identity -> identity.deviceId })
            .containsExactly(
                DacDeviceId.TRN_BLACK_PEARL,
                DacDeviceId.FIIO_JA11,
            )
            .inOrder()
    }
}
