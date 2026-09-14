package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import org.junit.Test

class BlackPearlDeviceDefaultsTest {
    @Test
    fun `restore preset uses owner approved hybrid values and leaves microphone untouched`() {
        assertThat(BlackPearlDeviceDefaults.finalTargets[BlackPearlDeviceControls.PLAYBACK_GAIN_DB])
            .isEqualTo(DacControlValue.Numeric(BlackPearlDeviceDefaults.RESTORED_VOLUME_DB))
        assertThat(BlackPearlDeviceDefaults.finalTargets[BlackPearlDeviceControls.DAC_FILTER])
            .isEqualTo(DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_FAST_LL))
        assertThat(BlackPearlDeviceDefaults.finalTargets[BlackPearlDeviceControls.GAIN_MODE])
            .isEqualTo(DacControlValue.Discrete(BlackPearlDeviceControls.GAIN_HIGH))
        assertThat(BlackPearlDeviceDefaults.finalTargets[BlackPearlDeviceControls.AMP_TOPOLOGY])
            .isEqualTo(DacControlValue.Discrete(BlackPearlDeviceControls.AMP_CLASS_AB))
        assertThat(BlackPearlDeviceDefaults.finalTargets[BlackPearlDeviceControls.BALANCE_DB])
            .isEqualTo(DacControlValue.Numeric(0.0))
        assertThat(BlackPearlDeviceDefaults.finalTargets)
            .doesNotContainKey(BlackPearlDeviceControls.MIC_GAIN_DB)
    }

    @Test
    fun `restore sequence lowers volume before level sensitive changes and establishes 50 percent last`() {
        val steps = BlackPearlDeviceDefaults.restoreSteps
        assertThat(steps.first().requestedValue)
            .isEqualTo(DacControlValue.Numeric(BlackPearlDeviceDefaults.SAFETY_VOLUME_DB))
        assertThat(steps.first().controlId).isEqualTo(BlackPearlDeviceControls.PLAYBACK_GAIN_DB)
        assertThat(steps.last().controlId).isEqualTo(BlackPearlDeviceControls.PLAYBACK_GAIN_DB)
        assertThat(steps.last().requestedValue)
            .isEqualTo(DacControlValue.Numeric(BlackPearlDeviceDefaults.RESTORED_VOLUME_DB))

        val restoredRaw = requireNotNull(
            BlackPearlDeviceControls.playbackGainRaw(BlackPearlDeviceDefaults.RESTORED_VOLUME_DB),
        )
        assertThat(BlackPearlVolumeScale.percentFromRaw(restoredRaw)).isEqualTo(50)

        val ampIndex = steps.indexOfFirst { it.controlId == BlackPearlDeviceControls.AMP_TOPOLOGY }
        val gainIndex = steps.indexOfFirst { it.controlId == BlackPearlDeviceControls.GAIN_MODE }
        assertThat(ampIndex).isIn(1 until steps.lastIndex)
        assertThat(gainIndex).isIn(1 until steps.lastIndex)
    }
}
