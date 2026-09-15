package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeBandFingerprint
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeFingerprint
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqCaptureMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedEqCaptureMetadataCodecTest {
    private val codec = SavedEqCaptureMetadataCodec()

    @Test
    fun roundTripPreservesExactBlackPearlNativeUnitsAndProvenance() {
        val metadata = SavedEqCaptureMetadata(
            deviceId = DacDeviceId.TRN_BLACK_PEARL,
            activeSlot = 2,
            verifiedAtEpochMillis = 123456789L,
            nativeFingerprint = HardwareEqNativeFingerprint(
                deviceId = DacDeviceId.TRN_BLACK_PEARL,
                eqEnabled = true,
                bands = listOf(
                    HardwareEqNativeBandFingerprint(
                        index = 0,
                        enabled = true,
                        type = EqFilterType.PEAK,
                        frequencyUnits = 3200L,
                        gainUnits = -640L,
                        qUnits = 307L,
                    ),
                    HardwareEqNativeBandFingerprint(
                        index = 1,
                        enabled = true,
                        type = EqFilterType.LOW_SHELF,
                        frequencyUnits = 105L,
                        gainUnits = 384L,
                        qUnits = 181L,
                    ),
                ),
                dedicatedEqPreampUnits = null,
            ),
        )

        assertEquals(metadata, codec.decode(codec.encode(metadata)))
    }
}
