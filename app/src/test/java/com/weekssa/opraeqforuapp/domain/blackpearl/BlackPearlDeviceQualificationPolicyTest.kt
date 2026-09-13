package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BlackPearlDeviceQualificationPolicyTest {
    @Test
    fun qualifiedFilterAndRemainingBatchAreInteractiveWithoutFirmwareWrite() {
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
        assertThat(BlackPearlDeviceQualificationPolicy.candidateWriteControlIds).containsExactly(
            BlackPearlDeviceControls.BALANCE_DB,
            BlackPearlDeviceControls.MIC_GAIN_DB,
            BlackPearlDeviceControls.AMP_TOPOLOGY,
            BlackPearlDeviceControls.GAIN_MODE,
            BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
        )
        assertThat(BlackPearlDeviceQualificationPolicy.isWriteInteractive(BlackPearlDeviceControls.DAC_FILTER))
            .isTrue()
        assertThat(BlackPearlDeviceQualificationPolicy.isWriteInteractive(BlackPearlDeviceControls.BALANCE_DB))
            .isTrue()
        assertThat(BlackPearlDeviceQualificationPolicy.isWriteInteractive(BlackPearlDeviceControls.MIC_GAIN_DB))
            .isTrue()
        assertThat(BlackPearlDeviceQualificationPolicy.isWriteInteractive(BlackPearlDeviceControls.AMP_TOPOLOGY))
            .isTrue()
        assertThat(BlackPearlDeviceQualificationPolicy.isWriteInteractive(BlackPearlDeviceControls.GAIN_MODE))
            .isTrue()
        assertThat(BlackPearlDeviceQualificationPolicy.isWriteInteractive(BlackPearlDeviceControls.PLAYBACK_GAIN_DB))
            .isTrue()
        assertThat(BlackPearlDeviceQualificationPolicy.isWriteInteractive(BlackPearlDeviceControls.FIRMWARE))
            .isFalse()
    }
}
