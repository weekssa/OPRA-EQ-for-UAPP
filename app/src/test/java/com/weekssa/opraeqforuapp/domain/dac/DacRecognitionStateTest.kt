package com.weekssa.opraeqforuapp.domain.dac

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class DacRecognitionStateTest {
    @Test
    fun presentDeviceBecomesRecognizedAndRemainsRecognizedAfterDisconnect() {
        val connected = DacRecognitionState().withPresentDevices(setOf(DacDeviceId.TRN_BLACK_PEARL))
        val disconnected = connected.withPresentDevices(emptySet())

        assertThat(connected.presentDeviceIds).containsExactly(DacDeviceId.TRN_BLACK_PEARL)
        assertThat(connected.recognizedDeviceIds).containsExactly(DacDeviceId.TRN_BLACK_PEARL)
        assertThat(disconnected.presentDeviceIds).isEmpty()
        assertThat(disconnected.recognizedDeviceIds).containsExactly(DacDeviceId.TRN_BLACK_PEARL)
        assertThat(disconnected.hasRecognizedDevice).isTrue()
    }

    @Test
    fun multipleSupportedDevicesRemainExplicitInsteadOfSelectingOneArbitrarily() {
        val state = DacRecognitionState()
            .withPresentDevices(setOf(DacDeviceId.TRN_BLACK_PEARL, DacDeviceId.FIIO_JA11))

        assertThat(state.presentDeviceIds)
            .containsExactly(DacDeviceId.TRN_BLACK_PEARL, DacDeviceId.FIIO_JA11)
        assertThat(state.recognizedDeviceIds)
            .containsExactly(DacDeviceId.TRN_BLACK_PEARL, DacDeviceId.FIIO_JA11)
    }

    @Test
    fun recognitionOnlyGrowsWithinSession() {
        val state = DacRecognitionState()
            .withPresentDevices(setOf(DacDeviceId.FIIO_JA11))
            .withPresentDevices(emptySet())
            .withPresentDevices(setOf(DacDeviceId.JCALLY_JM12_STOCK))

        assertThat(state.presentDeviceIds).containsExactly(DacDeviceId.JCALLY_JM12_STOCK)
        assertThat(state.recognizedDeviceIds)
            .containsExactly(DacDeviceId.FIIO_JA11, DacDeviceId.JCALLY_JM12_STOCK)
    }

    @Test
    fun invalidStateCannotCallPresentDeviceUnrecognized() {
        assertThrows(IllegalArgumentException::class.java) {
            DacRecognitionState(
                presentDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL),
                recognizedDeviceIds = emptySet(),
            )
        }
    }
}
