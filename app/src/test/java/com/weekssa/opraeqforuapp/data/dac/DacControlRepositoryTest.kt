package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControls
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceDefaults
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.screens.BlackPearlRestoreStepVerification
import com.weekssa.opraeqforuapp.ui.screens.blackPearlRestoreStepVerification
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DacControlRepositoryTest {
    @Test
    fun restoreWithDelayedUiDeliveryAndVerifiedNoOpsReachesFinalVolumeWithoutRepeatingWrites() = runBlocking {
        val source = FakeSource(filterCode = 1, gainModeCode = 1, ampTopologyCode = 1,
            micGainDb = 0, leftBalanceDb = 0, rightBalanceDb = 0, playbackGainRaw = -1536)
        val repository = DacControlRepository(source)
        val initial = repository.readBlackPearlQualificationSnapshot() as BlackPearlQualificationReadResult.Success
        var state = BlackPearlQualificationUiState().success(initial.snapshot)

        for (step in BlackPearlDeviceDefaults.restoreSteps) {
            val issuedFrom = state.writeGeneration
            // Compose can deliver this old state repeatedly after a callback has issued a step.
            // It may even match the target (verified no-op), but it is not completion evidence.
            repeat(3) {
                assertEquals(BlackPearlRestoreStepVerification.WAITING, blackPearlRestoreStepVerification(
                    state.isBusy, state.writeGeneration, issuedFrom, state.lastVerifiedWriteControlId,
                    step.controlId, BlackPearlDeviceDefaults.isStepSatisfied(step, state.snapshot!!),
                ))
            }
            state = state.beginWrite(step.controlId)
            val result = repository.writeBlackPearlControl(DacWriteIntent(
                step.controlId, step.requestedValue, initial.snapshot.sessionGeneration,
            ))
            assertTrue(result is BlackPearlDeviceControlWriteResult.Verified)
            result as BlackPearlDeviceControlWriteResult.Verified
            // A StateFlow collector may skip the busy emission; the completed cycle still counts.
            state = state.writeVerified(step.controlId, result.snapshot)
            assertEquals(BlackPearlRestoreStepVerification.SATISFIED, blackPearlRestoreStepVerification(
                state.isBusy, state.writeGeneration, issuedFrom, state.lastVerifiedWriteControlId,
                step.controlId, BlackPearlDeviceDefaults.isStepSatisfied(step, state.snapshot!!),
            ))
        }
        assertEquals(-1536, source.playbackGainRaw)
        assertEquals(2, source.writeCount) // Safety volume and final volume; five verified no-ops.
        assertEquals(2, source.persistCount)
        assertEquals(7L, state.writeGeneration)
    }

    @Test
    fun realFailureAfterSafetyVolumeHasNoAutomaticRetryOrVolumeIncrease() = runBlocking {
        val source = FakeSource(failOnWrite = 2)
        val repository = DacControlRepository(source)
        val safety = BlackPearlDeviceDefaults.restoreSteps.first()
        val floor = repository.writeBlackPearlControl(DacWriteIntent(safety.controlId, safety.requestedValue, 7L))
        assertTrue(floor is BlackPearlDeviceControlWriteResult.Verified)
        val filter = BlackPearlDeviceDefaults.restoreSteps[1]
        val failure = repository.writeBlackPearlControl(DacWriteIntent(filter.controlId, filter.requestedValue, 7L))
        assertTrue(failure is BlackPearlDeviceControlWriteResult.TransferFailed)
        assertEquals(-9472, source.playbackGainRaw)
        assertEquals(2, source.writeCount)
        assertEquals(1, source.persistCount)
    }

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
    fun filterWriteRequiresFreshBaselineThenVerifiesPersistsAndVerifiesAgain() = runBlocking {
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
        assertEquals(1, source.persistCount)
        assertEquals(listOf("write", "persist"), source.operationLog)
        assertEquals(24, source.readCount)
    }

    @Test
    fun alreadyCurrentRequestedValueDoesNotIssueRedundantWriteOrPersistenceSave() = runBlocking {
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
        assertEquals(0, source.persistCount)
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
        assertEquals(0, source.persistCount)
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
        assertEquals(0, source.persistCount)
    }

    @Test
    fun failedTargetTransferIsReportedWithoutPersistenceOrReadbackSuccess() = runBlocking {
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
        assertEquals(0, source.persistCount)
        assertEquals(8, source.readCount)
    }

    @Test
    fun failedPersistenceIsReportedAfterLiveVerificationWithoutSaveRetry() = runBlocking {
        val source = FakeSource(failPersistence = true)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.DAC_FILTER,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_NOS),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.TransferFailed)
        assertEquals(1, source.writeCount)
        assertEquals(1, source.persistCount)
        assertEquals(listOf("write", "persist"), source.operationLog)
        assertEquals(16, source.readCount)
    }

    @Test
    fun writeReadbackMismatchIsNeverPersistedOrReportedAsSuccess() = runBlocking {
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
        assertEquals(0, source.persistCount)
        assertEquals(16, source.readCount)
    }

    @Test
    fun unrelatedStateMutationIsNeverPersistedEvenWhenRequestedValueMatches() = runBlocking {
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
        assertEquals(0, source.persistCount)
    }

    @Test
    fun stateThatRevertsDuringPersistenceFailsFinalVerification() = runBlocking {
        val source = FakeSource(revertFilterOnPersistTo = 2)
        val requested = DacControlValue.Discrete(BlackPearlDeviceControls.FILTER_SLOW_LL)
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
        assertEquals(1, source.writeCount)
        assertEquals(1, source.persistCount)
        assertEquals(24, source.readCount)
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
        assertEquals(0, source.persistCount)
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
        assertEquals(1, source.persistCount)
        assertEquals(24, source.readCount)
    }

    @Test
    fun microphoneGainWriteVerifiesReadbackAndPreservesOtherControls() = runBlocking {
        val source = FakeSource(micGainDb = 0, leftBalanceDb = 0, rightBalanceDb = 0, playbackGainRaw = 512)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.MIC_GAIN_DB,
                requestedValue = DacControlValue.Numeric(-1.0),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.Verified)
        result as BlackPearlDeviceControlWriteResult.Verified
        assertEquals(-1, result.snapshot.micGainDb)
        assertEquals(2, result.snapshot.filterCode)
        assertEquals(1, result.snapshot.gainModeCode)
        assertEquals(0, result.snapshot.ampTopologyCode)
        assertEquals(0, result.snapshot.signedBalanceDb)
        assertEquals(512, result.snapshot.playbackGainRaw)
        assertEquals(1, source.persistCount)
        assertEquals(24, source.readCount)
    }

    @Test
    fun amplifierTopologyWriteVerifiesRequestedDiscreteValue() = runBlocking {
        val source = FakeSource(ampTopologyCode = 0, leftBalanceDb = 0, rightBalanceDb = 0)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.AMP_TOPOLOGY,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.AMP_CLASS_AB),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.Verified)
        result as BlackPearlDeviceControlWriteResult.Verified
        assertEquals(1, result.snapshot.ampTopologyCode)
        assertEquals(1, source.writeCount)
        assertEquals(1, source.persistCount)
        assertEquals(24, source.readCount)
    }

    @Test
    fun gainModeWriteVerifiesRequestedDiscreteValue() = runBlocking {
        val source = FakeSource(gainModeCode = 1, leftBalanceDb = 0, rightBalanceDb = 0)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.GAIN_MODE,
                requestedValue = DacControlValue.Discrete(BlackPearlDeviceControls.GAIN_LOW),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.Verified)
        result as BlackPearlDeviceControlWriteResult.Verified
        assertEquals(0, result.snapshot.gainModeCode)
        assertEquals(1, source.writeCount)
        assertEquals(1, source.persistCount)
        assertEquals(24, source.readCount)
    }

    @Test
    fun playbackWriteUsesExactlyRepresentableGainVerifiesPersistsAndVerifiesRawReadback() = runBlocking {
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
        assertEquals(1, source.persistCount)
        assertEquals(listOf("write", "persist"), source.operationLog)
        assertEquals(24, source.readCount)
    }

    @Test
    fun playbackWriteRejectsNonNativeGridValueBeforeUsbTraffic() = runBlocking {
        val source = FakeSource(playbackGainRaw = 512)
        val result = DacControlRepository(source).writeBlackPearlControl(
            DacWriteIntent(
                controlId = BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
                requestedValue = DacControlValue.Numeric(1.001),
                expectedSessionGeneration = 7L,
            ),
        )

        assertTrue(result is BlackPearlDeviceControlWriteResult.InvalidRequest)
        assertEquals(0, source.readCount)
        assertEquals(0, source.writeCount)
        assertEquals(0, source.persistCount)
    }

    private class FakeSource(
        private var current: Boolean = true,
        private val changeSessionAfterRead: Int? = null,
        private val failField: String? = null,
        private val failWrites: Boolean = false,
        private val failOnWrite: Int? = null,
        private val failPersistence: Boolean = false,
        private val ignoreWrites: Boolean = false,
        private val mutateUnrelatedOnWrite: Boolean = false,
        private val revertFilterOnPersistTo: Int? = null,
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
        var persistCount: Int = 0
        val operationLog = mutableListOf<String>()

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

        override suspend fun persistDeviceSettings(): Boolean {
            persistCount += 1
            operationLog += "persist"
            if (failPersistence) return false
            revertFilterOnPersistTo?.let { filterCode = it }
            return true
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
            operationLog += "write"
            if (failWrites || writeCount == failOnWrite) return false
            if (!ignoreWrites) block()
            if (mutateUnrelatedOnWrite) gainModeCode = 0
            return true
        }
    }
}
