package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MyDacSelectionTest {
    @Test
    fun attachedDacReplacesAStaleSelection() {
        assertEquals(
            DacDeviceId.SIMGOT_EW300,
            nextMyDacSelection(
                current = DacDeviceId.TRN_BLACK_PEARL,
                present = setOf(DacDeviceId.SIMGOT_EW300),
                recognized = setOf(DacDeviceId.TRN_BLACK_PEARL, DacDeviceId.SIMGOT_EW300),
            ),
        )
    }

    @Test
    fun currentSelectionIsKeptWhileItStaysAttached() {
        assertEquals(
            DacDeviceId.TRN_BLACK_PEARL,
            nextMyDacSelection(
                current = DacDeviceId.TRN_BLACK_PEARL,
                present = setOf(DacDeviceId.TRN_BLACK_PEARL),
                recognized = setOf(DacDeviceId.TRN_BLACK_PEARL, DacDeviceId.SIMGOT_EW300),
            ),
        )
    }

    @Test
    fun singleRecognizedDacKeepsItsPageWhenNothingIsAttached() {
        assertEquals(
            DacDeviceId.FIIO_JA11,
            nextMyDacSelection(
                current = null,
                present = emptySet(),
                recognized = setOf(DacDeviceId.FIIO_JA11),
            ),
        )
    }

    @Test
    fun severalRecognizedDacsWithoutAConnectionFallBackToTheChooser() {
        assertNull(
            nextMyDacSelection(
                current = null,
                present = emptySet(),
                recognized = setOf(DacDeviceId.TRN_BLACK_PEARL, DacDeviceId.SIMGOT_EW300),
            ),
        )
    }

    @Test
    fun twoAttachedDacsWithoutACurrentChoiceFallBackToTheChooser() {
        assertNull(
            nextMyDacSelection(
                current = null,
                present = setOf(DacDeviceId.TRN_BLACK_PEARL, DacDeviceId.FIIO_JA11),
                recognized = setOf(DacDeviceId.TRN_BLACK_PEARL, DacDeviceId.FIIO_JA11),
            ),
        )
    }
}

