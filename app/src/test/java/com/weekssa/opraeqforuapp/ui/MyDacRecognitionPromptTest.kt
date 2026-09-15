package com.weekssa.opraeqforuapp.ui

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import org.junit.Test

class MyDacRecognitionPromptTest {
    @Test
    fun newlyAttachedSupportedDacIsDetected() {
        assertThat(
            newlyPresentDac(
                previousPresentDeviceNames = emptyList(),
                currentPresentDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL),
            ),
        ).isEqualTo(DacDeviceId.TRN_BLACK_PEARL)
    }

    @Test
    fun unchangedPresentSetDoesNotRepeatDetection() {
        assertThat(
            newlyPresentDac(
                previousPresentDeviceNames = listOf(DacDeviceId.TRN_BLACK_PEARL.name),
                currentPresentDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL),
            ),
        ).isNull()
    }

    @Test
    fun detachThenReattachCanProduceANewDetection() {
        val afterDetach = emptyList<String>()

        assertThat(
            newlyPresentDac(
                previousPresentDeviceNames = afterDetach,
                currentPresentDeviceIds = setOf(DacDeviceId.FIIO_JA11),
            ),
        ).isEqualTo(DacDeviceId.FIIO_JA11)
    }

    @Test
    fun simultaneousNewDevicesUseStableDeviceOrder() {
        assertThat(
            newlyPresentDac(
                previousPresentDeviceNames = emptyList(),
                currentPresentDeviceIds = setOf(
                    DacDeviceId.JCALLY_JM12_STOCK,
                    DacDeviceId.TRN_BLACK_PEARL,
                ),
            ),
        ).isEqualTo(DacDeviceId.TRN_BLACK_PEARL)
    }
}
