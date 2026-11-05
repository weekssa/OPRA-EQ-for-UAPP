package com.weekssa.opraeqforuapp.domain.ew300

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Ew300VolumeControlTest {
    @Test
    fun boundariesMapToTheDocumentedSixtyToZeroDecibelScale() {
        assertEquals(-60.0, Ew300VolumeControl.levelToGainDb(0), 0.0)
        assertEquals(-59.0, Ew300VolumeControl.levelToGainDb(1), 0.0)
        assertEquals(-30.0, Ew300VolumeControl.levelToGainDb(30), 0.0)
        assertEquals(-1.0, Ew300VolumeControl.levelToGainDb(59), 0.0)
        assertEquals(0.0, Ew300VolumeControl.levelToGainDb(60), 0.0)
    }

    @Test
    fun reverseMappingRoundsToTheNearestLevelAndClampsOutsideTheEnvelope() {
        assertEquals(0, Ew300VolumeControl.gainDbToLevel(-60.0))
        assertEquals(30, Ew300VolumeControl.gainDbToLevel(-30.0))
        assertEquals(60, Ew300VolumeControl.gainDbToLevel(0.0))
        assertEquals(0, Ew300VolumeControl.gainDbToLevel(-64.0))
        assertEquals(60, Ew300VolumeControl.gainDbToLevel(0.5))
    }

    @Test
    fun levelStepsUseBothStereoHalfDecibelSteps() {
        assertEquals(-120, Ew300VolumeControl.levelToSteps(0))
        assertEquals(-60, Ew300VolumeControl.levelToSteps(30))
        assertEquals(0, Ew300VolumeControl.levelToSteps(60))
        assertThrows(IllegalArgumentException::class.java) { Ew300VolumeControl.levelToGainDb(61) }
    }
}

