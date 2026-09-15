package com.weekssa.opraeqforuapp.ui.screens

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import org.junit.Test

class MyDacConnectionPresentationTest {
    @Test
    fun presentDisconnectedDeviceOffersConnect() {
        val recognition = recognitionState(DacDeviceId.TRN_BLACK_PEARL)

        assertThat(
            shouldOfferMyDacConnect(
                deviceId = DacDeviceId.TRN_BLACK_PEARL,
                recognitionState = recognition,
                blackPearl = BlackPearlConnectionState.Disconnected,
                fiioJa11 = Kt02h20ConnectionState.Disconnected,
                jcallyJm12 = Kt02h20ConnectionState.Disconnected,
            ),
        ).isTrue()
    }

    @Test
    fun presentConnectionErrorOffersRetry() {
        val recognition = recognitionState(DacDeviceId.FIIO_JA11)
        val fiioError = Kt02h20ConnectionState.Error("permission denied")

        assertThat(
            shouldOfferMyDacConnect(
                deviceId = DacDeviceId.FIIO_JA11,
                recognitionState = recognition,
                blackPearl = BlackPearlConnectionState.Disconnected,
                fiioJa11 = fiioError,
                jcallyJm12 = Kt02h20ConnectionState.Disconnected,
            ),
        ).isTrue()
        assertThat(
            hasMyDacConnectionError(
                deviceId = DacDeviceId.FIIO_JA11,
                blackPearl = BlackPearlConnectionState.Disconnected,
                fiioJa11 = fiioError,
                jcallyJm12 = Kt02h20ConnectionState.Disconnected,
            ),
        ).isTrue()
    }

    @Test
    fun connectedOrConnectingDeviceDoesNotOfferConnect() {
        val recognition = recognitionState(DacDeviceId.JCALLY_JM12_STOCK)

        assertThat(
            shouldOfferMyDacConnect(
                deviceId = DacDeviceId.JCALLY_JM12_STOCK,
                recognitionState = recognition,
                blackPearl = BlackPearlConnectionState.Disconnected,
                fiioJa11 = Kt02h20ConnectionState.Disconnected,
                jcallyJm12 = Kt02h20ConnectionState.Connecting,
            ),
        ).isFalse()
        assertThat(
            shouldOfferMyDacConnect(
                deviceId = DacDeviceId.JCALLY_JM12_STOCK,
                recognitionState = recognition,
                blackPearl = BlackPearlConnectionState.Disconnected,
                fiioJa11 = Kt02h20ConnectionState.Disconnected,
                jcallyJm12 = Kt02h20ConnectionState.Connected,
            ),
        ).isFalse()
    }

    @Test
    fun sessionStickyButDetachedDeviceDoesNotOfferConnect() {
        val recognition = DacRecognitionState(
            presentDeviceIds = emptySet(),
            recognizedDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL),
        )

        assertThat(
            shouldOfferMyDacConnect(
                deviceId = DacDeviceId.TRN_BLACK_PEARL,
                recognitionState = recognition,
                blackPearl = BlackPearlConnectionState.Disconnected,
                fiioJa11 = Kt02h20ConnectionState.Disconnected,
                jcallyJm12 = Kt02h20ConnectionState.Disconnected,
            ),
        ).isFalse()
    }

    private fun recognitionState(deviceId: DacDeviceId): DacRecognitionState = DacRecognitionState(
        presentDeviceIds = setOf(deviceId),
        recognizedDeviceIds = setOf(deviceId),
    )
}
