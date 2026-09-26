package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareFlashMessagingTest {
    @Test
    fun everySupportedFlashUsesTheSameInProgressContract() {
        val devices = listOf(
            ExportDevice.BLACK_PEARL,
            ExportDevice.FIIO_JA11,
            ExportDevice.SIMGOT_EW300,
            ExportDevice.JCALLY_JM12,
        )

        devices.forEach { device ->
            val message = hardwareFlashStartedMessage(device)

            assertTrue(message.startsWith("Flashing "))
            assertTrue(message.contains("Keep it connected"))
            assertTrue(message.contains("final hardware state is verified"))
        }
    }
}
