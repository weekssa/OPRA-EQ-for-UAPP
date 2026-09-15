package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BlackPearlVolumeScaleTest {
    @Test
    fun nativeRangeMapsToControllerEndpoints() {
        assertThat(BlackPearlVolumeScale.percentFromRaw(BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW)).isEqualTo(0)
        assertThat(BlackPearlVolumeScale.percentFromRaw(BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW)).isEqualTo(100)
    }

    @Test
    fun qualificationBaselineRaw512MatchesIndependentController63Percent() {
        assertThat(BlackPearlVolumeScale.percentFromRaw(512)).isEqualTo(63)
    }

    @Test
    fun outOfRangeRawIsRejectedRatherThanClamped() {
        val failure = runCatching {
            BlackPearlVolumeScale.percentFromRaw(BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW + 1)
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
    }
}
