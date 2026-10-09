package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditSpecs
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditor
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditorStartResult
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiioJa11FlasherTest {
    @Test
    fun editorApplyUsesFreshTokenThenExactFiveBandApplySaveAndFinalReadback() = runBlocking {
        val transport = FakeJa11Transport()
        val baseline = editorBaseline(transport)
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = HardwareEqSnapshotState().publishCurrent(baseline.snapshotBundle),
            spec = HardwareEqEditSpecs.FIIO_JA11,
        ) as HardwareEqEditorStartResult.Ready
        val edited = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.FIIO_JA11,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 80.0,
            gainDb = 1.0,
            q = 0.7,
        )
        assertTrue(transport.sentCommands.isEmpty())

        val result = FiioJa11Flasher(transport).applyEditorWorkingCopy(
            HardwareEqEditor.useSafeGain(edited, HardwareEqEditSpecs.FIIO_JA11),
            baseline,
        )

        assertTrue(result.toString(), result is FiioJa11EditorApplyResult.Verified)
        assertEquals(
            listOf(0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19),
            transport.sentCommands,
        )
        assertEquals(1, transport.saveCount)
        assertEquals(1.0, transport.bands[0].gainDb, 0.0)
        assertEquals(FiioJa11Protocol.EqProgram.USER_1, transport.program)
    }

    @Test
    fun editorApplyRejectsChangedCompleteBaselineBeforeAnyWrite() = runBlocking {
        val transport = FakeJa11Transport()
        val baseline = editorBaseline(transport)
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = HardwareEqSnapshotState().publishCurrent(baseline.snapshotBundle),
            spec = HardwareEqEditSpecs.FIIO_JA11,
        ) as HardwareEqEditorStartResult.Ready
        val edited = HardwareEqEditor.updateFilter(
            started.workingCopy,
            HardwareEqEditSpecs.FIIO_JA11,
            0,
            EqFilterType.PEAK,
            80.0,
            1.0,
            0.7,
        )
        transport.bands[1] = transport.bands[1].copy(gainDb = 0.5)

        val result = FiioJa11Flasher(transport).applyEditorWorkingCopy(
            HardwareEqEditor.useSafeGain(edited, HardwareEqEditSpecs.FIIO_JA11),
            baseline,
        )

        assertTrue(result.toString(), result is FiioJa11EditorApplyResult.StaleBaseline)
        assertTrue(transport.sentCommands.isEmpty())
        assertEquals(0, transport.saveCount)
    }

    @Test
    fun editorApplyStopsIfProgramChangesDuringFreshBaselineReadBeforeWrites() = runBlocking {
        val transport = FakeJa11Transport(
            changeProgramAfterInitialGlobalGainRead = FiioJa11Protocol.EqProgram.OFF,
        )
        val baseline = editorBaseline(transport)
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = HardwareEqSnapshotState().publishCurrent(baseline.snapshotBundle),
            spec = HardwareEqEditSpecs.FIIO_JA11,
        ) as HardwareEqEditorStartResult.Ready
        val edited = HardwareEqEditor.updateFilter(
            started.workingCopy,
            HardwareEqEditSpecs.FIIO_JA11,
            0,
            EqFilterType.PEAK,
            80.0,
            1.0,
            0.7,
        )

        val result = FiioJa11Flasher(transport).applyEditorWorkingCopy(
            HardwareEqEditor.useSafeGain(edited, HardwareEqEditSpecs.FIIO_JA11),
            baseline,
        )

        assertTrue(result.toString(), result is FiioJa11EditorApplyResult.StaleBaseline)
        assertEquals(FiioJa11Protocol.EqProgram.OFF, transport.program)
        assertTrue(transport.sentCommands.isEmpty())
        assertEquals(0, transport.saveCount)
    }

    @Test
    fun editorApplyFinalMismatchDoesNotRetrySave() = runBlocking {
        val transport = FakeJa11Transport(postSaveGlobalGainDb = -3.9)
        val baseline = editorBaseline(transport)
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = HardwareEqSnapshotState().publishCurrent(baseline.snapshotBundle),
            spec = HardwareEqEditSpecs.FIIO_JA11,
        ) as HardwareEqEditorStartResult.Ready
        val edited = HardwareEqEditor.updateFilter(
            started.workingCopy,
            HardwareEqEditSpecs.FIIO_JA11,
            0,
            EqFilterType.PEAK,
            80.0,
            1.0,
            0.7,
        )

        val result = FiioJa11Flasher(transport).applyEditorWorkingCopy(
            HardwareEqEditor.useSafeGain(edited, HardwareEqEditSpecs.FIIO_JA11),
            baseline,
        )

        assertTrue(result.toString(), result is FiioJa11EditorApplyResult.VerificationFailed)
        assertEquals(1, transport.saveCount)
        assertEquals(1, transport.sentCommands.count { it == 0x19 })
    }

    @Test
    fun flashSelectsAndVerifiesUserOneBeforeWritingFiveSlotsAndGain() = runBlocking {
        val transport = FakeJa11Transport(initialProgram = FiioJa11Protocol.EqProgram.VOCAL)
        val flasher = FiioJa11Flasher(transport)

        val result = flasher.flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.Success)
        result as Kt02h20FlashResult.Success
        assertTrue(result.explicitPersistenceCommandUsed)
        assertEquals(
            listOf(0x16, 0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19),
            transport.sentCommands,
        )
        val selectionWrite = transport.operationHistory.indexOf("write:16")
        val selectionReadback = transport.operationHistory
            .drop(selectionWrite + 1)
            .indexOf("readProgram:USER_1") + selectionWrite + 1
        val firstBandOrGainWrite = transport.operationHistory.indexOfFirst {
            it == "write:15" || it == "write:17"
        }
        assertTrue(selectionWrite >= 0)
        assertTrue(selectionReadback > selectionWrite)
        assertTrue(selectionReadback < firstBandOrGainWrite)
        assertEquals(FiioJa11Protocol.EqProgram.USER_1, transport.program)
        assertEquals(10, transport.bandReadsAfterWrites)
        assertEquals(2, transport.globalGainReadsAfterWrites)
        assertEquals(3, transport.programReadsAfterWrites)
        assertEquals(-4.0, transport.globalGainDb, 0.001)
        assertEquals(1, transport.saveCount)
        assertEquals(2.5, transport.bands[0].gainDb, 0.0)
        assertEquals(0.0, transport.bands[2].gainDb, 0.0)
    }

    @Test
    fun flashFromOffWritesTheUserOneBankAndLeavesTheOffBankUntouched() = runBlocking {
        val transport = FakeJa11Transport(initialProgram = FiioJa11Protocol.EqProgram.OFF)
        val originalOffBand = transport.bands[0]
        transport.bandsFor(FiioJa11Protocol.EqProgram.USER_1)[0] =
            FiioJa11Protocol.Band("peak_dip", 730.0, -2.0, 1.1)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.Success)
        assertEquals(FiioJa11Protocol.EqProgram.USER_1, transport.program)
        assertEquals(originalOffBand, transport.bandsFor(FiioJa11Protocol.EqProgram.OFF)[0])
        assertEquals(FiioJa11Protocol.Band("low_shelf", 100.0, 2.5, 0.7), transport.bands[0])
        assertEquals(
            listOf(0x16, 0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19),
            transport.sentCommands,
        )
        assertEquals(1, transport.saveCount)
    }

    @Test
    fun flashSkipsProgramSelectionWhenUserOneIsAlreadyActive() = runBlocking {
        val transport = FakeJa11Transport(initialProgram = FiioJa11Protocol.EqProgram.USER_1)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.Success)
        assertEquals(
            listOf(0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19),
            transport.sentCommands,
        )
        assertEquals(1, transport.saveCount)
    }

    @Test
    fun unconfirmedUserOneSelectionStopsBeforeBandOrGainWrites() = runBlocking {
        val transport = FakeJa11Transport(
            initialProgram = FiioJa11Protocol.EqProgram.OFF,
            ignoreProgramWrite = true,
        )
        val initialOffBands = transport.bandsFor(FiioJa11Protocol.EqProgram.OFF).toList()
        val initialUserOneBands = transport.bandsFor(FiioJa11Protocol.EqProgram.USER_1).toList()
        val initialGain = transport.globalGainDb

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.VerificationFailed)
        assertEquals(listOf(0x16), transport.sentCommands)
        assertEquals(FiioJa11Protocol.EqProgram.OFF, transport.program)
        assertEquals(initialOffBands, transport.bandsFor(FiioJa11Protocol.EqProgram.OFF))
        assertEquals(initialUserOneBands, transport.bandsFor(FiioJa11Protocol.EqProgram.USER_1))
        assertEquals(initialGain, transport.globalGainDb, 0.0)
        assertEquals(0, transport.saveCount)
    }

    @Test
    fun unavailableUserOneSelectionReadbackMarksStateUncertainAndStopsBeforeDataWrites() = runBlocking {
        val transport = FakeJa11Transport(
            initialProgram = FiioJa11Protocol.EqProgram.OFF,
            failProgramReadAfterCommandNumber = 1,
        )
        val flasher = FiioJa11Flasher(transport)

        val result = flasher.flash(exactProfile())
        val trace = requireNotNull(flasher.lastOperationTrace.value)

        assertTrue(result.toString(), result is Kt02h20FlashResult.TransferFailed)
        assertEquals(listOf(0x16), transport.sentCommands)
        assertEquals(0, transport.bandReadsAfterWrites)
        assertEquals(0, transport.globalGainReadsAfterWrites)
        assertEquals(0, transport.saveCount)
        assertFalse(trace.stateKnown)
        assertTrue((result as Kt02h20FlashResult.TransferFailed).reason.contains("may have switched to User 1"))
    }

    @Test
    fun oneFlashWorksFromUserOneVocalAndOffWithProgramSelectionBeforeDataWrites() = runBlocking {
        listOf(
            FiioJa11Protocol.EqProgram.USER_1 to emptyList(),
            FiioJa11Protocol.EqProgram.VOCAL to listOf(0x16),
            FiioJa11Protocol.EqProgram.OFF to listOf(0x16),
        ).forEach { (initialProgram, programSelectionCommands) ->
            val transport = FakeJa11Transport(initialProgram = initialProgram)

            val result = FiioJa11Flasher(transport).flash(exactProfile())

            assertTrue("$initialProgram: $result", result is Kt02h20FlashResult.Success)
            assertEquals(
                programSelectionCommands + listOf(0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19),
                transport.sentCommands,
            )
            assertEquals(1, transport.saveCount)
        }
    }

    @Test
    fun unexpectedSessionChangeAfterFirstBandStopsAllLaterCommandsAndDoesNotSave() = runBlocking {
        val transport = FakeJa11Transport(changeSessionAfterCommandNumber = 1)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.TransferFailed)
        assertEquals(listOf(0x15), transport.sentCommands)
        assertEquals(listOf(1L), transport.sentAtGenerations)
        assertEquals(0, transport.saveCount)
    }

    @Test
    fun unexpectedSessionChangeAfterApplyStopsBeforePreSaveReadsAndSave() = runBlocking {
        val transport = FakeJa11Transport(changeSessionAfterCommandNumber = 7)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.TransferFailed)
        assertEquals(listOf(0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18), transport.sentCommands)
        assertEquals(0, transport.bandReadsAfterWrites)
        assertEquals(0, transport.globalGainReadsAfterWrites)
        assertEquals(0, transport.programReadsAfterWrites)
        assertEquals(0, transport.saveCount)
    }

    @Test
    fun finalFlashVerificationUsesReplacementSessionAfterSaveRestart() = runBlocking {
        val transport = FakeJa11Transport(reconnectAfterSave = true)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.Success)
        assertEquals(1, transport.saveCount)
        assertEquals(1L, transport.sentAtGenerations.last())
        assertTrue(transport.readGenerations.takeLast(7).all { it == 2L })
    }

    @Test
    fun seriallessSoleJa11CanCompleteSaveReconnectAndFreshReadback() = runBlocking {
        val transport = FakeJa11Transport(serial = null, reconnectAfterSave = true)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.Success)
        assertEquals(1, transport.saveCount)
        assertEquals(1, transport.sentCommands.count { it == 0x19 })
        assertTrue(transport.readGenerations.takeLast(7).all { it == 2L })
    }

    @Test
    fun multipleInitialJa11CandidatesPreventFlashBeforeAnyWrite() = runBlocking {
        val transport = FakeJa11Transport(initialCandidateCount = 2)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.DeviceUnavailable)
        assertTrue(transport.sentCommands.isEmpty())
        assertEquals(0, transport.saveCount)
    }

    @Test
    fun multipleCandidatesAppearingAfterSavePreventFinalReadback() = runBlocking {
        val transport = FakeJa11Transport(reconnectAfterSave = true, candidateCountAfterSave = 2)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.VerificationFailed)
        assertEquals(1, transport.saveCount)
        assertEquals(1, transport.globalGainReadsAfterWrites)
    }

    @Test
    fun serialMismatchAfterSavePreventsFinalReadbackWithoutRetryingSave() = runBlocking {
        val transport = FakeJa11Transport(reconnectAfterSave = true, replacementSerial = "different-ja11")

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.VerificationFailed)
        assertEquals(1, transport.saveCount)
        assertEquals(1, transport.sentCommands.count { it == 0x19 })
        assertEquals(1, transport.globalGainReadsAfterWrites)
    }

    @Test
    fun unexpectedPidTransitionDuringFlashSaveFailsClosedBeforeFinalReadback() = runBlocking {
        val transport = FakeJa11Transport(
            reconnectAfterSave = true,
            replacementProductId = FiioJa11Protocol.PRODUCT_ID_UAC_1,
        )

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.VerificationFailed)
        assertEquals(1, transport.saveCount)
        assertEquals(1, transport.sentCommands.count { it == 0x19 })
        assertEquals(1, transport.globalGainReadsAfterWrites)
    }

    @Test
    fun saveThatDetachesDuringItsSettleUsesFreshSessionForFinalVerification() = runBlocking {
        val transport = FakeJa11Transport(changeSessionAfterCommandNumber = 8)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.Success)
        assertEquals(1, transport.saveCount)
        assertEquals(listOf(0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19), transport.sentCommands)
        assertEquals(2L, transport.sessionGeneration)
        assertTrue(transport.readGenerations.takeLast(7).all { it == 2L })
    }

    @Test
    fun applyFailureNeverAttemptsPersistentSave() = runBlocking {
        val transport = FakeJa11Transport(failCommand = 0x18)
        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.TransferFailed)
        assertFalse(0x19 in transport.sentCommands)
        assertEquals(0, transport.saveCount)
    }

    @Test
    fun preflightReadFailurePreventsAllWrites() = runBlocking {
        val transport = FakeJa11Transport(readable = false)
        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.DeviceUnavailable)
        assertTrue(transport.sentCommands.isEmpty())
    }

    @Test
    fun preflightSessionChangePreventsAllWrites() = runBlocking {
        val transport = FakeJa11Transport(changeSessionDuringBaselineRead = true)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.DeviceUnavailable)
        assertTrue(transport.sentCommands.isEmpty())
    }

    @Test
    fun programSelectionReadbackMismatchNeverReportsFlashSuccess() = runBlocking {
        val transport = FakeJa11Transport(
            initialProgram = FiioJa11Protocol.EqProgram.VOCAL,
            ignoreProgramWrite = true,
        )

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.VerificationFailed)
        assertFalse(0x19 in transport.sentCommands)
        assertEquals(FiioJa11Protocol.EqProgram.VOCAL, transport.program)
    }

    @Test
    fun staleGlobalGainReadbackNeverAttemptsPersistentSave() = runBlocking {
        val transport = FakeJa11Transport(ignoreGlobalGainWrite = true)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.VerificationFailed)
        assertFalse(0x19 in transport.sentCommands)
        assertEquals(0.0, transport.globalGainDb, 0.001)
        assertEquals(0, transport.saveCount)
    }

    @Test
    fun unexpectedGlobalGainReadbackFailsClosedBeforePersistentSave() = runBlocking {
        val transport = FakeJa11Transport(postWriteGlobalGainDb = -3.800390625)
        val flasher = FiioJa11Flasher(transport)

        val result = flasher.flash(exactProfile())
        val trace = requireNotNull(flasher.lastOperationTrace.value)

        assertTrue(result is Kt02h20FlashResult.VerificationFailed)
        assertFalse(0x19 in transport.sentCommands)
        assertEquals(0, transport.saveCount)
        assertEquals(-3.800390625, trace.readbackGlobalGainDb!!, 0.0)
    }

    @Test
    fun verificationFailurePublishesExactGainComparisonBeforeSave() = runBlocking {
        val transport = FakeJa11Transport(ignoreGlobalGainWrite = true)
        val flasher = FiioJa11Flasher(
            transport = transport,
            sourceCommit = "test-source",
            appVersion = "test-version",
            signerVerified = true,
        )

        val result = flasher.flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.VerificationFailed)
        val trace = requireNotNull(flasher.lastOperationTrace.value)
        assertEquals("FLASH", trace.operation)
        assertEquals("test-source", trace.sourceCommit)
        assertTrue(trace.signerVerified)
        assertEquals("ja11-flash", trace.sourceProfileId)
        assertEquals("2.20", trace.firmwareVersion)
        assertEquals(-4.0, trace.canonicalPreampGainDb!!, 0.0)
        assertEquals(-4.0, trace.generatedOrSelectedTargetGainDb!!, 0.0)
        assertEquals(-4.0, trace.quantizedWireTargetGainDb!!, 0.0)
        assertEquals(0.0, trace.readbackGlobalGainDb!!, 0.0)
        assertEquals("VOLATILE_READBACK", trace.comparisonPhase)
        assertEquals(0L, trace.saveCommandCount)
        assertTrue(trace.stateKnown)
        assertTrue(trace.stages.contains(FiioJa11OperationStage.FAILED))
        assertTrue(trace.events.any { it.command == "0x17" })
        assertEquals(FiioJa11Protocol.EqProgram.USER_1.name, trace.baselineProgram)
        assertEquals(0.0, trace.baselineGlobalGainDb!!, 0.0)
        assertEquals(FiioJa11Protocol.BAND_COUNT, trace.baselineBands.size)
    }

    @Test
    fun staleBandReadbackNeverAttemptsPersistentSave() = runBlocking {
        val transport = FakeJa11Transport(ignoreBandWriteIndex = 0)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.VerificationFailed)
        assertFalse(0x19 in transport.sentCommands)
        assertEquals(0, transport.saveCount)
    }

    @Test
    fun saveReconnectFailureNeverRunsFinalVerification() = runBlocking {
        val transport = FakeJa11Transport(saveReconnectAccepted = false)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.TransferFailed)
        assertEquals(1, transport.saveCount)
        assertEquals(1, transport.globalGainReadsAfterWrites)
    }

    @Test
    fun finalReadbackMismatchAfterSaveFailsWithoutRetryingSave() = runBlocking {
        val transport = FakeJa11Transport(postSaveGlobalGainDb = -3.9)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.VerificationFailed)
        assertEquals(1, transport.saveCount)
        assertEquals(2, transport.globalGainReadsAfterWrites)
        assertEquals(listOf(0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19), transport.sentCommands)
    }

    @Test
    fun resetWritesAllFiveFlatSlotsZeroGainSelectsUserOneApplyAndSave() = runBlocking {
        val transport = FakeJa11Transport(initialProgram = FiioJa11Protocol.EqProgram.BASS).apply {
            globalGainDb = -6.0
            bands[0] = FiioJa11Protocol.Band("peak_dip", 1_000.0, 6.0, 1.0)
        }

        val result = FiioJa11Flasher(transport).resetToFlat()

        assertTrue(result is Kt02h20FlatResetResult.Success)
        assertEquals(0.0, transport.globalGainDb, 0.001)
        assertTrue(transport.bands.all { kotlin.math.abs(it.gainDb) < 0.000_001 })
        assertEquals(
            FiioJa11Protocol.Band("peak_dip", 1_000.0, 6.0, 1.0),
            transport.bandsFor(FiioJa11Protocol.EqProgram.BASS)[0],
        )
        assertEquals(FiioJa11Protocol.EqProgram.USER_1, transport.program)
        assertEquals(
            listOf(0x16, 0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19),
            transport.sentCommands,
        )
    }

    @Test
    fun resetStopsAfterSessionChangesDuringUserOneSelectionBeforeBandWrites() = runBlocking {
        val transport = FakeJa11Transport(
            initialProgram = FiioJa11Protocol.EqProgram.BASS,
            changeSessionAfterCommandNumber = 1,
        )

        val result = FiioJa11Flasher(transport).resetToFlat()

        assertTrue(result.toString(), result is Kt02h20FlatResetResult.TransferFailed)
        assertEquals(listOf(0x16), transport.sentCommands)
        assertEquals(0, transport.saveCount)
    }

    @Test
    fun editorApplyStopsAfterSessionChangesDuringFirstBandWrite() = runBlocking {
        val transport = FakeJa11Transport(changeSessionAfterCommandNumber = 1)
        val baseline = editorBaseline(transport)
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = HardwareEqSnapshotState().publishCurrent(baseline.snapshotBundle),
            spec = HardwareEqEditSpecs.FIIO_JA11,
        ) as HardwareEqEditorStartResult.Ready
        val edited = HardwareEqEditor.updateFilter(
            started.workingCopy,
            HardwareEqEditSpecs.FIIO_JA11,
            0,
            EqFilterType.PEAK,
            80.0,
            1.0,
            0.7,
        )

        val result = FiioJa11Flasher(transport).applyEditorWorkingCopy(
            HardwareEqEditor.useSafeGain(edited, HardwareEqEditSpecs.FIIO_JA11),
            baseline,
        )

        assertTrue(result.toString(), result is FiioJa11EditorApplyResult.TransferFailed)
        assertEquals(listOf(0x15), transport.sentCommands)
        assertEquals(0, transport.saveCount)
    }

    private fun exactProfile(): OpraEqProfile = OpraEqProfile(
        id = "ja11-flash",
        productId = "product",
        author = "Test",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = -4.0,
        bands = listOf(
            OpraBand("low_shelf", 100.0, 2.5, 0.7, null),
            OpraBand("peak_dip", 1_000.0, -1.5, 1.2, null),
        ),
    )

    private fun editorBaseline(transport: FakeJa11Transport): FiioJa11EditorBaseline =
        FiioJa11EditorBaseline(
            deviceFingerprintKey = requireNotNull(transport.deviceFingerprintKey),
            usbProductId = requireNotNull(transport.usbProductId),
            snapshotBundle = requireNotNull(
                HardwareEqSnapshotFactory.fiioJa11(
                    nativeBands = transport.bands,
                    globalEqGainDb = transport.globalGainDb,
                    sessionGeneration = transport.sessionGeneration,
                    verifiedAtEpochMillis = 10L,
                    eqEnabled = true,
                    activeProgram = FiioJa11Protocol.EqProgram.USER_1,
                ),
            ),
        )

    private class FakeJa11Transport(
        private val failCommand: Int? = null,
        private val readable: Boolean = true,
        initialProgram: FiioJa11Protocol.EqProgram = FiioJa11Protocol.EqProgram.USER_1,
        private val ignoreProgramWrite: Boolean = false,
        private val failProgramReadAfterCommandNumber: Int? = null,
        private val changeProgramAfterInitialGlobalGainRead: FiioJa11Protocol.EqProgram? = null,
        private val ignoreGlobalGainWrite: Boolean = false,
        private val ignoreBandWriteIndex: Int? = null,
        private val saveReconnectAccepted: Boolean = true,
        private val postSaveGlobalGainDb: Double? = null,
        private val postWriteGlobalGainDb: Double? = null,
        private val changeSessionAfterCommandNumber: Int? = null,
        private val reconnectAfterSave: Boolean = false,
        private val changeSessionDuringBaselineRead: Boolean = false,
        private val serial: String? = "ja11-test",
        initialCandidateCount: Int = 1,
        private val candidateCountAfterSave: Int? = null,
        private val replacementSerial: String? = null,
        private val replacementProductId: Int? = null,
    ) : FiioJa11Transport {
        override var deviceFingerprintKey: String? = serial?.let { "serial=$it|vid=2972" } ?: "vid=2972"
        override val deviceSerialIdentity: String?
            get() = fiioJa11SerialIdentity(deviceFingerprintKey)
        override var usbProductId: Int = FiioJa11Protocol.PRODUCT_ID_UAC_2
        override var sessionGeneration: Long = 1L
        override var detachGeneration: Long = 0L
        override var supportedJa11CandidateCount: Int = initialCandidateCount
        private val bandsByProgram = FiioJa11Protocol.EqProgram.values().associateWith {
            FiioJa11Protocol.completeBands(emptyList()).toMutableList()
        }.toMutableMap()
        val bands: MutableList<FiioJa11Protocol.Band>
            get() = bandsFor(program)
        var globalGainDb: Double = 0.0
        var program: FiioJa11Protocol.EqProgram = initialProgram
        val sentCommands = mutableListOf<Int>()
        val operationHistory = mutableListOf<String>()
        val sentAtGenerations = mutableListOf<Long>()
        val readGenerations = mutableListOf<Long>()
        var saveCount = 0
        var writeStarted = false
        var bandReadsAfterWrites = 0
        var globalGainReadsAfterWrites = 0
        var programReadsAfterWrites = 0
        private var traceActive = false
        private val traceEvents = mutableListOf<FiioJa11TransportEvent>()
        private var programChangedDuringGainRead = false

        fun bandsFor(program: FiioJa11Protocol.EqProgram): MutableList<FiioJa11Protocol.Band> =
            requireNotNull(bandsByProgram[program])

        override fun beginTrace(operationId: String) {
            traceActive = true
            traceEvents.clear()
        }

        override fun endTrace(): List<FiioJa11TransportEvent> {
            traceActive = false
            return traceEvents.toList()
        }

        override suspend fun readBand(index: Int): FiioJa11Protocol.Band? {
            readGenerations += sessionGeneration
            operationHistory += "readBand:${program.name}:$index"
            if (!readable) return null
            if (writeStarted) bandReadsAfterWrites++
            return bands[index]
        }

        override suspend fun readBandInSession(
            index: Int,
            expected: FiioJa11SessionToken,
        ): FiioJa11Protocol.Band? = readPinned(expected) { readBand(index) }

        override suspend fun readGlobalGainDb(): Double? {
            readGenerations += sessionGeneration
            operationHistory += "readGain:${program.name}"
            if (!readable) return null
            if (writeStarted) globalGainReadsAfterWrites++
            val value = if (writeStarted) postWriteGlobalGainDb ?: globalGainDb else globalGainDb
            if (!writeStarted && !programChangedDuringGainRead) {
                changeProgramAfterInitialGlobalGainRead?.let { program = it }
                programChangedDuringGainRead = true
            }
            return value
        }

        override suspend fun readGlobalGainDbInSession(expected: FiioJa11SessionToken): Double? =
            readPinned(expected, ::readGlobalGainDb)

        override suspend fun readFirmwareVersion(): String? = "2.20"

        override suspend fun readFirmwareVersionInSession(expected: FiioJa11SessionToken): String? =
            readPinned(expected, ::readFirmwareVersion)

        override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? {
            readGenerations += sessionGeneration
            operationHistory += "readProgram:${program.name}"
            if (!readable) return null
            if (writeStarted) programReadsAfterWrites++
            if (failProgramReadAfterCommandNumber == sentCommands.size && sentCommands.isNotEmpty()) return null
            val value = program
            if (changeSessionDuringBaselineRead && !writeStarted) changeSession()
            return value
        }

        override suspend fun readEqProgramInSession(
            expected: FiioJa11SessionToken,
        ): FiioJa11Protocol.EqProgram? = readPinned(expected, ::readEqProgram)

        override suspend fun saveToFlash(expected: FiioJa11SessionToken): Boolean {
            val accepted = ja11RestartWriteWasAccepted(
                sendReportInSession(FiioJa11Protocol.saveToFlashReport(), expected),
            ) && saveReconnectAccepted
            if (accepted) {
                postSaveGlobalGainDb?.let { globalGainDb = it }
                candidateCountAfterSave?.let { supportedJa11CandidateCount = it }
                replacementSerial?.let { deviceFingerprintKey = "serial=$it|vid=2972" }
                replacementProductId?.let { usbProductId = it }
                if (reconnectAfterSave) changeSession()
            }
            return accepted
        }

        override suspend fun sendReport(report: ByteArray): Boolean {
            writeStarted = true
            val command = report[5].toInt() and 0xFF
            operationHistory += "write:%02x".format(command)
            sentCommands += command
            sentAtGenerations += sessionGeneration
            if (command == failCommand) {
                if (traceActive) traceEvents += traceEvent(report, succeeded = false)
                return false
            }
            when (command) {
                0x15 -> {
                    val index = report[7].toInt() and 0xFF
                    if (index != ignoreBandWriteIndex) {
                        val gainRawUnsigned = ((report[8].toInt() and 0xFF) shl 8) or (report[9].toInt() and 0xFF)
                        val gainRaw = if (gainRawUnsigned >= 0x8000) gainRawUnsigned - 0x10000 else gainRawUnsigned
                        val frequency = ((report[10].toInt() and 0xFF) shl 8) or (report[11].toInt() and 0xFF)
                        val qRaw = ((report[12].toInt() and 0xFF) shl 8) or (report[13].toInt() and 0xFF)
                        val type = when (report[14].toInt() and 0xFF) {
                            0 -> "peak_dip"
                            1 -> "low_shelf"
                            2 -> "high_shelf"
                            else -> error("unexpected test filter type")
                        }
                        bands[index] = FiioJa11Protocol.Band(type, frequency.toDouble(), gainRaw / 10.0, qRaw / 100.0)
                    }
                }
                0x16 -> if (!ignoreProgramWrite) {
                    program = FiioJa11Protocol.EqProgram.fromCode(report[7].toInt() and 0xFF)
                        ?: error("unexpected test EQ program")
                }
                0x17 -> {
                    if (!ignoreGlobalGainWrite) {
                        val rawUnsigned = ((report[7].toInt() and 0xFF) shl 8) or
                            (report[8].toInt() and 0xFF)
                        val raw = if (rawUnsigned >= 0x8000) rawUnsigned - 0x10000 else rawUnsigned
                        globalGainDb = raw / 10.0
                    }
                }
                0x19 -> saveCount++
            }
            if (traceActive) traceEvents += traceEvent(report, succeeded = true)
            if (sentCommands.size == changeSessionAfterCommandNumber) changeSession()
            return true
        }

        private suspend fun <T> readPinned(
            expected: FiioJa11SessionToken,
            read: suspend () -> T?,
        ): T? {
            if (!isCurrentSession(expected)) return null
            return read()?.takeIf { isCurrentSession(expected) }
        }

        private fun changeSession() {
            sessionGeneration++
            detachGeneration++
        }

        private fun traceEvent(report: ByteArray, succeeded: Boolean): FiioJa11TransportEvent =
            FiioJa11TransportEvent(
                sequence = traceEvents.size + 1,
                elapsedMillis = traceEvents.size.toLong(),
                direction = "WRITE",
                command = "0x%02x".format(report[5].toInt() and 0xFF),
                requestHex = report.toJa11TraceHex(),
                responseHex = null,
                sessionGeneration = 1L,
                detachGeneration = 0L,
                succeeded = succeeded,
            )
    }
}
