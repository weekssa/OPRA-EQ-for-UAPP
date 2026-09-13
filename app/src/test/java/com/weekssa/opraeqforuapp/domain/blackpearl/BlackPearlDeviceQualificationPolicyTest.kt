package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BlackPearlDeviceQualificationPolicyTest {
    @Test
    fun allApprovedNormalControlsHaveSoftwareSupportButOnlyFilterIsFirstWriteCandidate() {
        assertThat(BlackPearlDeviceQualificationPolicy.softwareImplementedControlIds).containsExactly(
            BlackPearlDeviceControls.DAC_FILTER,
            BlackPearlDeviceControls.BALANCE_DB,
            BlackPearlDeviceControls.MIC_GAIN_DB,
            BlackPearlDeviceControls.AMP_TOPOLOGY,
            BlackPearlDeviceControls.GAIN_MODE,
            BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
        )
        assertThat(BlackPearlDeviceQualificationPolicy.candidateWriteControlIds)
            .containsExactly(BlackPearlDeviceControls.DAC_FILTER)
        assertThat(BlackPearlDeviceQualificationPolicy.productionQualifiedWriteControlIds).isEmpty()
    }
}
