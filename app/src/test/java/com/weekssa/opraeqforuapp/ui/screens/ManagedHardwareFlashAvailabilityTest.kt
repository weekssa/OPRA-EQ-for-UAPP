package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedHardwareFlashAvailabilityTest {
    @Test
    fun ew300FlashRequiresDirectToggleAndExactConnectedSession() {
        assertFalse(
            isManagedHardwareFlashEnabled(
                activeOutput = ExportDevice.SIMGOT_EW300,
                directBlackPearlFlashEnabled = false,
                blackPearlConnected = false,
                directFiioJa11FlashEnabled = false,
                fiioJa11Connected = false,
                directJcallyJm12FlashEnabled = false,
                jcallyJm12Connected = false,
                directEw300FlashEnabled = false,
                ew300Connected = true,
            ),
        )
        assertTrue(
            isManagedHardwareFlashEnabled(
                activeOutput = ExportDevice.SIMGOT_EW300,
                directBlackPearlFlashEnabled = false,
                blackPearlConnected = false,
                directFiioJa11FlashEnabled = false,
                fiioJa11Connected = false,
                directJcallyJm12FlashEnabled = false,
                jcallyJm12Connected = false,
                directEw300FlashEnabled = true,
                ew300Connected = true,
            ),
        )
    }

    @Test
    fun inactiveOutputNeverEnablesEw300Flash() {
        assertFalse(
            isManagedHardwareFlashEnabled(
                activeOutput = ExportDevice.BLACK_PEARL,
                directBlackPearlFlashEnabled = false,
                blackPearlConnected = false,
                directFiioJa11FlashEnabled = false,
                fiioJa11Connected = false,
                directJcallyJm12FlashEnabled = false,
                jcallyJm12Connected = false,
                directEw300FlashEnabled = true,
                ew300Connected = true,
            ),
        )
    }
}
