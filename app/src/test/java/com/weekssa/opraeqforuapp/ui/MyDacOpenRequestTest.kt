package com.weekssa.opraeqforuapp.ui

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import org.junit.Test

class MyDacOpenRequestTest {
    @Test
    fun recognizedRequestedDeviceCanOpenMyDac() {
        val request = MyDacOpenRequest(1L, DacDeviceId.TRN_BLACK_PEARL)
        val recognition = DacRecognitionState(
            presentDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL),
            recognizedDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL),
        )

        assertThat(canFulfillMyDacOpenRequest(request, recognition)).isTrue()
    }

    @Test
    fun differentRecognizedDeviceCannotFulfillRequest() {
        val request = MyDacOpenRequest(2L, DacDeviceId.FIIO_JA11)
        val recognition = DacRecognitionState(
            presentDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL),
            recognizedDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL),
        )

        assertThat(canFulfillMyDacOpenRequest(request, recognition)).isFalse()
    }

    @Test
    fun missingRequestCannotOpenMyDac() {
        assertThat(canFulfillMyDacOpenRequest(null, DacRecognitionState())).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun requestIdMustBePositive() {
        MyDacOpenRequest(0L, DacDeviceId.JCALLY_JM12_STOCK)
    }
}
