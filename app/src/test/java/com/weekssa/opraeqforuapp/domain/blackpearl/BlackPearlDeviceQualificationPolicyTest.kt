package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BlackPearlDeviceQualificationPolicyTest {
    @Test
    fun qualifiedFilterRemainsInteractiveWhileBalanceIsNextCandidate() {
        assertThat(BlackPearlDeviceQualificationPolicy.softwareImplementedControlIds).containsExactly(
            BlackPearlDeviceControls.DAC_FILTER,
            BlackPearlDeviceControls.BALANCE_DB,
            BlackPearlDeviceControls.MIC_GAIN_DB,
            BlackPearlDeviceControls.AMP_TOPOLOGY,
            BlackPearlDeviceControls.GAIN_MODE,
            BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
        )
        assertThat(BlackPearlDeviceQualificationPolicy.productionQualifiedWriteControlIds)
            .containsExactly(BlackPearlDeviceControls.DAC_FILTER)
        assertThat(BlackPearlDeviceQualificationPolicy.candidateWriteControlIds)
            .containsExactly(BlackPearlDeviceControls.BALANCE_DB)
        assertThat(BlackPearlDeviceQualificationPolicy.isWriteInteractive(BlackPearlDeviceControls.DAC_FILTER))
            .isTrue()
        assertThat(BlackPearlDeviceQualificationPolicy.isWriteInteractive(BlackPearlDeviceControls.BALANCE_DB))
            .isTrue()
        assertThat(BlackPearlDeviceQualificationPolicy.isWriteInteractive(BlackPearlDeviceControls.MIC_GAIN_DB))
            .isFalse()
    }
}
