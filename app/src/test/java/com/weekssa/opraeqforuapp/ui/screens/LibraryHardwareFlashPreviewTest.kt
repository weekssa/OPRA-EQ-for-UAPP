package com.weekssa.opraeqforuapp.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryHardwareFlashPreviewTest {
    @Test
    fun oneConnectedBlackPearlIsSelected() {
        assertEquals(
            LibraryHardwareFlashDevice.BLACK_PEARL,
            connectedLibraryHardwareFlashDevice(
                blackPearlConnected = true,
                fiioJa11Connected = false,
            ),
        )
    }

    @Test
    fun oneConnectedFiioIsSelected() {
        assertEquals(
            LibraryHardwareFlashDevice.FIIO_JA11,
            connectedLibraryHardwareFlashDevice(
                blackPearlConnected = false,
                fiioJa11Connected = true,
            ),
        )
    }

    @Test
    fun oneConnectedEw300IsSelected() {
        assertEquals(
            LibraryHardwareFlashDevice.SIMGOT_EW300,
            connectedLibraryHardwareFlashDevice(
                blackPearlConnected = false,
                fiioJa11Connected = false,
                ew300Connected = true,
            ),
        )
    }

    @Test
    fun noConnectedDacDoesNotGuess() {
        assertNull(
            connectedLibraryHardwareFlashDevice(
                blackPearlConnected = false,
                fiioJa11Connected = false,
            ),
        )
    }

    @Test
    fun twoConnectedDacsDoNotGuess() {
        assertNull(
            connectedLibraryHardwareFlashDevice(
                blackPearlConnected = true,
                fiioJa11Connected = true,
            ),
        )
    }

    @Test
    fun ew300AndAnotherConnectedDacDoNotGuess() {
        assertNull(
            connectedLibraryHardwareFlashDevice(
                blackPearlConnected = true,
                fiioJa11Connected = false,
                ew300Connected = true,
            ),
        )
    }
}
