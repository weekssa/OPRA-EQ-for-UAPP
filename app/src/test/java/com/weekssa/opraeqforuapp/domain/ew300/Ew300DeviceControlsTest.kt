package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.dac.DacControlValidation
import com.weekssa.opraeqforuapp.domain.dac.validateForWrite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300DeviceControlsTest {
    @Test
    fun descriptorExposesOnlyTheConservativeNonBoostingRange() {
        val descriptor = Ew300DeviceControls.playbackGainDescriptor

        assertEquals(-64.0, descriptor.absoluteRange.minimum, 0.0)
        assertEquals(0.0, descriptor.absoluteRange.maximum, 0.0)
        assertEquals(0.5, descriptor.step, 0.0)
        assertEquals(
            DacControlValidation.Valid,
            descriptor.validateForWrite(Ew300DeviceControls.value(-40.0)),
        )
        assertTrue(
            descriptor.validateForWrite(Ew300DeviceControls.value(0.5)) is DacControlValidation.OutOfRange,
        )
    }
}
