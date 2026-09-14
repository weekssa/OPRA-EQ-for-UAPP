package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControlReadCodec
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlUsbAudioMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlUsbAudioModeRepositoryTest {
    @Test
    fun completeDeviceReadCarriesCurrentUacMode() = runBlocking {
        val result = DacControlRepository(FakeSource(BlackPearlUsbAudioMode.UAC_2_0))
            .readBlackPearlQualificationSnapshot()

        assertTrue(result is BlackPearlQualificationReadResult.Success)
        val success = result as BlackPearlQualificationReadResult.Success
        assertEquals(BlackPearlUsbAudioMode.UAC_2_0, success.snapshot.usbAudioMode)
    }

    @Test
    fun unknownUacDescriptorDoesNotInvalidateOtherwiseCompleteDeviceRead() = runBlocking {
        val result = DacControlRepository(FakeSource(null))
            .readBlackPearlQualificationSnapshot()

        assertTrue(result is BlackPearlQualificationReadResult.Success)
        val success = result as BlackPearlQualificationReadResult.Success
        assertNull(success.snapshot.usbAudioMode)
    }

    private class FakeSource(
        private val mode: BlackPearlUsbAudioMode?,
    ) : BlackPearlDeviceControlReadSource {
        override val sessionGeneration: Long = 1L
        override fun isSessionCurrent(sessionGeneration: Long): Boolean = sessionGeneration == 1L
        override suspend fun readFirmwareVersion(): String = "0.6"
        override suspend fun readFilterCode(): Int = BlackPearlDeviceControlReadCodec.FILTER_FAST_PC
        override suspend fun readGainModeCode(): Int = BlackPearlDeviceControlReadCodec.GAIN_MODE_HIGH
        override suspend fun readAmpTopologyCode(): Int = BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_AB
        override suspend fun readMicGainDb(): Int = 0
        override suspend fun readLeftBalanceDb(): Int = 0
        override suspend fun readRightBalanceDb(): Int = 0
        override suspend fun readPlaybackGainRaw(): Int = 512
        override suspend fun readUsbAudioMode(): BlackPearlUsbAudioMode? = mode
        override suspend fun writeFilterCode(value: Int): Boolean = true
        override suspend fun writeGainModeCode(value: Int): Boolean = true
        override suspend fun writeAmpTopologyCode(value: Int): Boolean = true
        override suspend fun writeMicGainDb(value: Int): Boolean = true
        override suspend fun writeBalanceDb(value: Int): Boolean = true
        override suspend fun writePlaybackGainRaw(value: Int): Boolean = true
        override suspend fun persistDeviceSettings(): Boolean = true
    }
}
