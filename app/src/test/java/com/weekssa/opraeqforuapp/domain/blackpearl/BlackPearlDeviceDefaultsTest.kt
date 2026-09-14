package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlackPearlDeviceDefaultsTest {
    @Test
    fun `restore preset uses owner approved hybrid values and leaves microphone untouched`() {
        assertEquals(
            DacControlValue.Numeric(BlackPearlDeviceDefaults.RESTORED_VOLUME_DB),
            BlackPearlDeviceDefaults.finalTargets[BlackPearlDeviceControls.PLAYBACK_GAIN_DB],
        )
        assertEquals(
            DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_FAST_LL),
            BlackPearlDeviceDefaults.finalTargets[BlackPearlDeviceControls.DAC_FILTER],
        )
        assertEquals(
            DacControlValue.Discrete(BlackPearlDeviceControls.GAIN_HIGH),
            BlackPearlDeviceDefaults.finalTargets[BlackPearlDeviceControls.GAIN_MODE],
        )
        assertEquals(
            DacControlValue.Discrete(BlackPearlDeviceControls.AMP_CLASS_AB),
            BlackPearlDeviceDefaults.finalTargets[BlackPearlDeviceControls.AMP_TOPOLOGY],
        )
        assertEquals(
            DacControlValue.Numeric(0.0),
            BlackPearlDeviceDefaults.finalTargets[BlackPearlDeviceControls.BALANCE_DB],
        )
        assertFalse(BlackPearlDeviceDefaults.finalTargets.containsKey(BlackPearlDeviceControls.MIC_GAIN_DB))
    }

    @Test
    fun `restore sequence lowers volume before level sensitive changes and establishes 50 percent last`() {
        val steps = BlackPearlDeviceDefaults.restoreSteps
        assertEquals(
            DacControlValue.Numeric(BlackPearlDeviceDefaults.SAFETY_VOLUME_DB),
            steps.first().requestedValue,
        )
        assertEquals(BlackPearlDeviceControls.PLAYBACK_GAIN_DB, steps.first().controlId)
        assertEquals(BlackPearlDeviceControls.PLAYBACK_GAIN_DB, steps.last().controlId)
        assertEquals(
            DacControlValue.Numeric(BlackPearlDeviceDefaults.RESTORED_VOLUME_DB),
            steps.last().requestedValue,
        )

        val restoredRaw = requireNotNull(
            BlackPearlDeviceControls.playbackGainRaw(BlackPearlDeviceDefaults.RESTORED_VOLUME_DB),
        )
        assertEquals(50, BlackPearlVolumeScale.percentFromRaw(restoredRaw))

        val ampIndex = steps.indexOfFirst { it.controlId == BlackPearlDeviceControls.AMP_TOPOLOGY }
        val gainIndex = steps.indexOfFirst { it.controlId == BlackPearlDeviceControls.GAIN_MODE }
        assertTrue(ampIndex in 1 until steps.lastIndex)
        assertTrue(gainIndex in 1 until steps.lastIndex)
    }
}
