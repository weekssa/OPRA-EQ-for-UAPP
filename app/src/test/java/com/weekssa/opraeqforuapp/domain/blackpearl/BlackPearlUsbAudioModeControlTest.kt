package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.DacControlDescriptor
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlUsbAudioModeControlTest {
    @Test
    fun usbAudioModeIsTypedButNotWritableWithoutEstablishedCommand() {
        val descriptor = BlackPearlDeviceControls.descriptor(BlackPearlDeviceControls.USB_AUDIO_MODE)
        assertTrue(descriptor is DacControlDescriptor.Discrete)
        descriptor as DacControlDescriptor.Discrete

        assertFalse(descriptor.writable)
        assertEquals(
            listOf(BlackPearlDeviceControls.USB_UAC_1, BlackPearlDeviceControls.USB_UAC_2),
            descriptor.options.map { it.valueId },
        )
    }

    @Test
    fun currentUacModeMapsFromVerifiedSnapshot() {
        val snapshot = BlackPearlDeviceQualificationSnapshot(
            sessionGeneration = 1L,
            firmwareVersion = "0.6",
            filterCode = BlackPearlDeviceControlReadCodec.FILTER_FAST_PC,
            gainModeCode = BlackPearlDeviceControlReadCodec.GAIN_MODE_HIGH,
            ampTopologyCode = BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_AB,
            micGainDb = 0,
            leftBalanceDb = 0,
            rightBalanceDb = 0,
            playbackGainRaw = 512,
            usbAudioMode = BlackPearlUsbAudioMode.UAC_1_0,
        )

        assertEquals(
            DacControlValue.Discrete(BlackPearlDeviceControls.USB_UAC_1),
            BlackPearlDeviceControls.valueFromSnapshot(BlackPearlDeviceControls.USB_AUDIO_MODE, snapshot),
        )
    }
}
