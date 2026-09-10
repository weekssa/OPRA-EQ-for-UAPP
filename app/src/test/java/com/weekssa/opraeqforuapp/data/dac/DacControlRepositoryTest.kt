package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlProtocol
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
        assertEquals(2, snapshot.filterCode)
        assertEquals(1, snapshot.gainModeCode)
        assertEquals(0, snapshot.ampTopologyCode)
        assertEquals(-4, snapshot.micGainDb)
        assertEquals(-3, snapshot.leftBalanceDb)
        assertEquals(0, snapshot.rightBalanceDb)
        assertEquals(-1024, snapshot.playbackGainRaw)
        assertEquals(7, source.readCount)
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
        assertEquals(4, source.readCount)
    }

    private class FakeSource(
        private var current: Boolean = true,
        private val changeSessionAfterRead: Int? = null,
        private val failField: String? = null,
    ) : BlackPearlDeviceControlReadSource {
        override var sessionGeneration: Long = 7L
        var readCount: Int = 0

        override fun isSessionCurrent(sessionGeneration: Long): Boolean =
            current && this.sessionGeneration == sessionGeneration

        override suspend fun readFilterCode(): Int? = read("filter") { 2 }
        override suspend fun readGainModeCode(): Int? = read("gain") { 1 }
        override suspend fun readAmpTopologyCode(): Int? = read("topology") { 0 }
        override suspend fun readMicGainDb(): Int? = read("mic") { -4 }
        override suspend fun readLeftBalanceDb(): Int? = read("left") { -3 }
        override suspend fun readRightBalanceDb(): Int? = read("right") { 0 }
        override suspend fun readPlaybackGainRaw(): Int? = read("playback") { -1024 }

        private fun <T> read(field: String, value: () -> T): T? {
            readCount += 1
            if (changeSessionAfterRead == readCount) {
                sessionGeneration += 1L
            }
            if (failField == field) return null
            return value()
        }
    }
}
