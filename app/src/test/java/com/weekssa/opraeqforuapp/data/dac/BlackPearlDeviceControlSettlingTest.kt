package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControls
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlDeviceControlSettlingTest {
    @Test
    fun halfDbPlaybackDeviceTargetIsRejectedBeforeUsbTraffic() = runBlocking {
        val source = SettlingSource()

        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
                requestedValue = DacControlValue.Numeric(1.5),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.InvalidRequest)
        assertEquals(0, source.readCount)
        assertEquals(0, source.writeCount)
    }

    @Test
    fun delayedAmpTopologyVisibilityVerifiesWithoutResendingWrite() = runBlocking {
        val source = SettlingSource(applyAmpOnReadbackAttempt = 3)

        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.AMP_TOPOLOGY,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.AMP_CLASS_H),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.Verified)
        result as BlackPearlDeviceControlWriteResult.Verified
        assertEquals(0, result.snapshot.ampTopologyCode)
        assertEquals(1, source.writeCount)
        assertEquals(3, source.postWriteReadbackAttempts)
    }

    @Test
    fun neverSettledAmpTopologyReturnsFinalMismatchWithoutResendingWrite() = runBlocking {
        val source = SettlingSource(applyAmpOnReadbackAttempt = null)

        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.AMP_TOPOLOGY,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.AMP_CLASS_H),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.ReadbackMismatch)
        assertEquals(1, source.writeCount)
        assertEquals(4, source.postWriteReadbackAttempts)
    }

    @Test
    fun sessionReplacementDuringSettlingStopsVerificationWithoutResendingWrite() = runBlocking {
        val source = SettlingSource(
            applyAmpOnReadbackAttempt = null,
            replaceSessionOnReadbackAttempt = 2,
        )

        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.AMP_TOPOLOGY,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.AMP_CLASS_H),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.StaleBaseline)
        assertEquals(1, source.writeCount)
        assertEquals(2, source.postWriteReadbackAttempts)
    }

    private class SettlingSource(
        private val applyAmpOnReadbackAttempt: Int? = 1,
        private val replaceSessionOnReadbackAttempt: Int? = null,
    ) : BlackPearlDeviceControlReadSource {
        override var sessionGeneration: Long = 7L
        var readCount: Int = 0
        var writeCount: Int = 0
        var postWriteReadbackAttempts: Int = 0

        private var writeIssued = false
        private var pendingAmpTopologyCode: Int? = null
        private var ampTopologyCode = 1
        private var gainModeCode = 1
        private var filterCode = 2
        private var micGainDb = 0
        private var leftBalanceDb = 0
        private var rightBalanceDb = 0
        private var playbackGainRaw = 512

        override fun isSessionCurrent(sessionGeneration: Long): Boolean =
            this.sessionGeneration == sessionGeneration

        override suspend fun readFirmwareVersion(): String? {
            beginSnapshotReadIfNeeded()
            return read { "0.6" }
        }

        override suspend fun readFilterCode(): Int? = read { filterCode }
        override suspend fun readGainModeCode(): Int? = read { gainModeCode }
        override suspend fun readAmpTopologyCode(): Int? = read { ampTopologyCode }
        override suspend fun readMicGainDb(): Int? = read { micGainDb }
        override suspend fun readLeftBalanceDb(): Int? = read { leftBalanceDb }
        override suspend fun readRightBalanceDb(): Int? = read { rightBalanceDb }
        override suspend fun readPlaybackGainRaw(): Int? = read { playbackGainRaw }

        override suspend fun writeFilterCode(value: Int): Boolean = write {
            filterCode = value
        }

        override suspend fun writeGainModeCode(value: Int): Boolean = write {
            gainModeCode = value
        }

        override suspend fun writeAmpTopologyCode(value: Int): Boolean {
            writeCount += 1
            writeIssued = true
            pendingAmpTopologyCode = value
            return true
        }

        override suspend fun writeMicGainDb(value: Int): Boolean = write {
            micGainDb = value
        }

        override suspend fun writeBalanceDb(value: Int): Boolean = write {
            leftBalanceDb = if (value < 0) value else 0
            rightBalanceDb = if (value > 0) value else 0
        }

        override suspend fun writePlaybackGainRaw(value: Int): Boolean = write {
            playbackGainRaw = value
        }

        private fun beginSnapshotReadIfNeeded() {
            if (!writeIssued) return
            postWriteReadbackAttempts += 1
            if (replaceSessionOnReadbackAttempt == postWriteReadbackAttempts) {
                sessionGeneration += 1L
                return
            }
            if (applyAmpOnReadbackAttempt == postWriteReadbackAttempts) {
                pendingAmpTopologyCode?.let { ampTopologyCode = it }
            }
        }

        private fun <T> read(block: () -> T): T {
            readCount += 1
            return block()
        }

        private fun write(block: () -> Unit): Boolean {
            writeCount += 1
            block()
            return true
        }
    }
}