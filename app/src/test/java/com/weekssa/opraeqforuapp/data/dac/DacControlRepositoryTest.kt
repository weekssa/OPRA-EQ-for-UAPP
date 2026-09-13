package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControls
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DacControlRepositoryTest {
    @Test
    fun readOnlyQualificationReturnsCompleteSnapshotFromOneCurrentSession() = runBlocking {
        val source = FakeSource()
        val result = DacControlRepository(source).readBlackPearlQualificationSnapshot()

        assertTrue(result is BlackPearlQualificationReadResult.Success)
        val snapshot = (result as BlackPearlQualificationReadResult.Success).snapshot
        assertEquals(7L, snapshot.sessionGeneration)
        assertEquals("BP-1.2.3", snapshot.firmwareVersion)
        assertEquals(2, snapshot.filterCode)
        assertEquals(1, snapshot.gainModeCode)
        assertEquals(0, snapshot.ampTopologyCode)
        assertEquals(-4, snapshot.micGainDb)
        assertEquals(-3, snapshot.leftBalanceDb)
        assertEquals(0, snapshot.rightBalanceDb)
        assertEquals(-1024, snapshot.playbackGainRaw)
        assertEquals(8, source.readCount)
    }

    @Test
    fun disconnectBeforeReadReturnsNotConnectedWithoutTransfers() = runBlocking {
        val source = FakeSource(current = false)
        val result = DacControlRepository(source).readBlackPearlQualificationSnapshot()

        assertTrue(result is BlackPearlQualificationReadResult.NotConnected)
        assertEquals(0, source.readCount)
    }

    @Test
    fun sessionChangeDuringSequenceIsNotMisreportedAsFieldFailure() = runBlocking {
        val source = FakeSource(changeSessionAfterRead = 2)
        val result = DacControlRepository(source).readBlackPearlQualificationSnapshot()

        assertTrue(result is BlackPearlQualificationReadResult.SessionChanged)
        assertEquals(2, source.readCount)
    }

    @Test
    fun malformedSingleFieldStopsSequenceAndNamesFailedRead() = runBlocking {
        val source = FakeSource(failField = "mic")
        val result = DacControlRepository(source).readBlackPearlQualificationSnapshot()

        assertEquals(BlackPearlQualificationReadResult.ReadFailed("mic gain"), result)
        assertEquals(5, source.readCount)
    }

    @Test
    fun malformedFirmwareStopsBeforeOtherCandidateReads() = runBlocking {
        val source = FakeSource(failField = "firmware")
        val result = DacControlRepository(source).readBlackPearlQualificationSnapshot()

        assertEquals(BlackPearlQualificationReadResult.ReadFailed("firmware version"), result)
        assertEquals(1, source.readCount)
    }

    @Test
    fun filterWriteRequiresFreshBaselineThenVerifiesCompleteReadback() = runBlocking {
        val source = FakeSource()
        val repository = DacControlRepository(source)
        val result = repository.writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.DAC_FILTER,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_SLOW_LL),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.Verified)
        result as BlackPearlDeviceControlWriteResult.Verified
        assertEquals(2, result.baseline.filterCode)
        assertEquals(3, result.snapshot.filterCode)
        assertEquals(1, source.writeCount)
        assertEquals(16, source.readCount)
    }

    @Test
    fun alreadyCurrentRequestedValueDoesNotIssueRedundantWrite() = runBlocking {
        val source = FakeSource()
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.DAC_FILTER,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_FAST_PC),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.Verified)
        assertEquals(0, source.writeCount)
        assertEquals(8, source.readCount)
    }

    @Test
    fun staleExpectedGenerationCannotWrite() = runBlocking {
        val source = FakeSource()
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.DAC_FILTER,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_NOS),
                expectedSessionGeneration = 6L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.StaleBaseline)
        assertEquals(0, source.writeCount)
        assertEquals(0, source.readCount)
    }

    @Test
    fun sessionReplacementDuringFreshBaselinePreventsWrite() = runBlocking {
        val source = FakeSource(changeSessionAfterRead = 8)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.DAC_FILTER,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_NOS),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.StaleBaseline)
        assertEquals(0, source.writeCount)
    }

    @Test
    fun failedTargetTransferIsReportedWithoutReadbackSuccess() = runBlocking {
        val source = FakeSource(failWrites = true)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.DAC_FILTER,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_NOS),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.TransferFailed)
        assertEquals(1, source.writeCount)
        assertEquals(8, source.readCount)
    }

    @Test
    fun writeReadbackMismatchIsNeverReportedAsSuccess() = runBlocking {
        val source = FakeSource(ignoreWrites = true)
        val requested = DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_NOS)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.DAC_FILTER,
                requestedValue = requested,
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.ReadbackMismatch)
        result as BlackPearlDeviceControlWriteResult.ReadbackMismatch
        assertEquals(requested, result.requestedValue)
        assertEquals(DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_FAST_PC), result.actualValue)
    }

    @Test
    fun unrelatedStateMutationFailsVerificationEvenWhenRequestedValueMatches() = runBlocking {
        val source = FakeSource(mutateUnrelatedOnWrite = true)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.DAC_FILTER,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_SLOW_PC),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.UnrelatedStateChanged)
        result as BlackPearlDeviceControlWriteResult.UnrelatedStateChanged
        assertEquals(listOf("gain mode"), result.changedFields)
    }

    @Test
    fun invalidControlValueIsRejectedBeforeAnyUsbTraffic() = runBlocking {
        val source = FakeSource()
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.DAC_FILTER,
                requestedValue = DacControlValue.Discrete("unknown-filter"),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.InvalidRequest)
        assertEquals(0, source.readCount)
        assertEquals(0, source.writeCount)
    }

    @Test
    fun balanceWriteVerifiesSignedOneSidedReadbackAndPreservesOtherControls() = runBlocking {
        val source = FakeSource(leftBalanceDb = 0, rightBalanceDb = 0)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.BALANCE_DB,
                requestedValue = DacControlValue.Numeric(-5.0),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.Verified)
        result as BlackPearlDeviceControlWriteResult.Verified
        assertEquals(-5, result.snapshot.signedBalanceDb)
        assertEquals(1, source.writeCount)
    }

    @Test
    fun playbackWriteUsesExactlyRepresentableGainAndVerifiesRawReadback() = runBlocking {
        val source = FakeSource(playbackGainRaw = 512)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
                requestedValue = DacControlValue.Numeric(1.0),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.Verified)
        result as BlackPearlDeviceControlWriteResult.Verified
        assertEquals(256, result.snapshot.playbackGainRaw)
    }

    private class FakeSource(
        private var current: Boolean = true,
        private val changeSessionAfterRead: Int? = null,
        private val failField: String? = null,
        private val failWrites: Boolean = false,
        private val ignoreWrites: Boolean = false,
        private val mutateUnrelatedOnWrite: Boolean = false,
        var firmwareVersion: String = "BP-1.2.3",
        var filterCode: Int = 2,
        var gainModeCode: Int = 1,
        var ampTopologyCode: Int = 0,
        var micGainDb: Int = -4,
        var leftBalanceDb: Int = -3,
        var rightBalanceDb: Int = 0,
        var playbackGainRaw: Int = -1024,
    ) : BlackPearlDeviceControlReadSource {
        override var sessionGeneration: Long = 7L
        var readCount: Int = 0
        var writeCount: Int = 0

        override fun isSessionCurrent(sessionGeneration: Long): Boolean =
            current && this.sessionGeneration == sessionGeneration

        override suspend fun readFirmwareVersion(): String? = read("firmware") { firmwareVersion }
        override suspend fun readFilterCode(): Int? = read("filter") { filterCode }
        override suspend fun readGainModeCode(): Int? = read("gain") { gainModeCode }
        override suspend fun readAmpTopologyCode(): Int? = read("topology") { ampTopologyCode }
        override suspend fun readMicGainDb(): Int? = read("mic") { micGainDb }
        override suspend fun readLeftBalanceDb(): Int? = read("left") { leftBalanceDb }
        override suspend fun readRightBalanceDb(): Int? = read("right") { rightBalanceDb }
        override suspend fun readPlaybackGainRaw(): Int? = read("playback") { playbackGainRaw }

        override suspend fun writeFilterCode(value: Int): Boolean = write {
            filterCode = value
        }

        override suspend fun writeGainModeCode(value: Int): Boolean = write {
            gainModeCode = value
        }

        override suspend fun writeAmpTopologyCode(value: Int): Boolean = write {
            ampTopologyCode = value
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

        private fun <T> read(field: String, value: () -> T): T? {
            readCount += 1
            if (changeSessionAfterRead == readCount) {
                sessionGeneration += 1L
            }
            if (failField == field) return null
            return value()
        }

        private fun write(block: () -> Unit): Boolean {
            writeCount += 1
            if (failWrites) return false
            if (!ignoreWrites) block()
            if (mutateUnrelatedOnWrite) gainModeCode = 0
            return true
        }
    }
}
