package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControlReadCodec
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlUsbAudioMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BlackPearlUsbAudioModeRepositoryTest {
    @Test
    fun completeDeviceReadCarriesCurrentUacMode() = runTest {
        val result = DacControlRepository(FakeSource(BlackPearlUsbAudioMode.UAC_2_0))
            .readBlackPearlQualificationSnapshot()

        val success = assertIs<BlackPearlQualificationReadResult.Success>(result)
        assertEquals(BlackPearlUsbAudioMode.UAC_2_0, success.snapshot.usbAudioMode)
    }

    @Test
    fun unknownUacDescriptorDoesNotInvalidateOtherwiseCompleteDeviceRead() = runTest {
        val result = DacControlRepository(FakeSource(null))
            .readBlackPearlQualificationSnapshot()

        val success = assertIs<BlackPearlQualificationReadResult.Success>(result)
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
    }
}
