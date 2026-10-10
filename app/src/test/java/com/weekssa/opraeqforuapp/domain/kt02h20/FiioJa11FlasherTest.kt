package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditSpecs
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditor
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditorStartResult
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlinx.coroutines.delay
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
            listOf(0x16, 0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19),
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
    fun flashFromOffSelectsAndVerifiesUserOneBeforeWritingEqData() = runBlocking {
        val transport = FakeJa11Transport(initialProgram = FiioJa11Protocol.EqProgram.OFF)
        val flasher = FiioJa11Flasher(transport)

        val result = flasher.flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.Success)
        result as Kt02h20FlashResult.Success
        assertTrue(result.explicitPersistenceCommandUsed)
        assertEquals(
            listOf(0x16, 0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19),
            transport.sentCommands,
        )
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
    fun programSelectionReadbackMismatchNeverReportsFlashSuccess() = runBlocking {
        val transport = FakeJa11Transport(
            initialProgram = FiioJa11Protocol.EqProgram.VOCAL,
            ignoreProgramWrite = true,
        )

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.VerificationFailed)
        assertFalse(0x19 in transport.sentCommands)
        assertFalse(transport.sentCommands.any { it == 0x15 || it == 0x17 })
        assertEquals(FiioJa11Protocol.EqProgram.VOCAL, transport.program)
    }

    @Test
    fun delayedDetachAfterSaveUsesFreshSessionForFinalReadback() = runBlocking {
        val transport = FakeJa11Transport(saveDetachDelayMillis = 677L)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.Success)
        assertEquals(1, transport.saveCount)
        assertTrue(transport.finalReadSessionGenerations.isNotEmpty())
        assertTrue(transport.finalReadSessionGenerations.all { it == 2L })
        assertEquals(1, transport.sentCommands.count { it == 0x19 })
    }

    @Test
    fun lateDetachDuringFinalReadbackRetriesReadbackOnlyOnReplacementSession() = runBlocking {
        val transport = FakeJa11Transport(detachDuringFinalReadback = true)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.Success)
        assertEquals(1, transport.saveCount)
        assertEquals(1, transport.sentCommands.count { it == 0x19 })
        assertTrue(transport.finalReadSessionGenerations.any { it == 2L })
        assertEquals(1, transport.sentCommands.count { it == 0x18 })
        assertEquals(5, transport.sentCommands.count { it == 0x15 })
        assertEquals(1, transport.sentCommands.count { it == 0x17 })
    }

    @Test
    fun detachAfterLastFinalValueStillRequiresReplacementReadback() = runBlocking {
        val transport = FakeJa11Transport(detachAfterFinalGainRead = true)

        val result = FiioJa11Flasher(transport).flash(exactProfile())

        assertTrue(result.toString(), result is Kt02h20FlashResult.Success)
        assertEquals(1, transport.saveCount)
        assertEquals(1, transport.sentCommands.count { it == 0x19 })
        assertTrue(transport.finalReadSessionGenerations.contains(1L))
        assertTrue(transport.finalReadSessionGenerations.contains(2L))
        assertEquals(5, transport.sentCommands.count { it == 0x15 })
        assertEquals(1, transport.sentCommands.count { it == 0x17 })
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
        assertEquals(listOf(0x16, 0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19), transport.sentCommands)
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
        assertEquals(FiioJa11Protocol.EqProgram.USER_1, transport.program)
        assertEquals(
            listOf(0x16, 0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19),
            transport.sentCommands,
        )
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
        private val ignoreGlobalGainWrite: Boolean = false,
        private val ignoreBandWriteIndex: Int? = null,
        private val saveReconnectAccepted: Boolean = true,
        private val postSaveGlobalGainDb: Double? = null,
        private val postWriteGlobalGainDb: Double? = null,
        private val saveDetachDelayMillis: Long = 0L,
        private val detachDuringFinalReadback: Boolean = false,
        private val detachAfterFinalGainRead: Boolean = false,
    ) : FiioJa11Transport {
        override val deviceFingerprintKey: String = "serial=ja11-test|vid=2972"
        override val usbProductId: Int = FiioJa11Protocol.PRODUCT_ID_UAC_2
        override var sessionGeneration: Long = 1L
        override var detachGeneration: Long = 0L
        val bands = FiioJa11Protocol.completeBands(emptyList()).toMutableList()
        var globalGainDb: Double = 0.0
        var program: FiioJa11Protocol.EqProgram = initialProgram
        val sentCommands = mutableListOf<Int>()
        var saveCount = 0
        var writeStarted = false
        var bandReadsAfterWrites = 0
        var globalGainReadsAfterWrites = 0
        var programReadsAfterWrites = 0
        val finalReadSessionGenerations = mutableListOf<Long>()
        private var lateDetachTriggered = false
        private var traceActive = false
        private val traceEvents = mutableListOf<FiioJa11TransportEvent>()

        override fun beginTrace(operationId: String) {
            traceActive = true
            traceEvents.clear()
        }

        override fun endTrace(): List<FiioJa11TransportEvent> {
            traceActive = false
            return traceEvents.toList()
        }

        override suspend fun readBand(index: Int): FiioJa11Protocol.Band? {
            if (!readable) return null
            if (writeStarted) bandReadsAfterWrites++
            if (saveCount > 0) finalReadSessionGenerations += sessionGeneration
            if (sessionGeneration == 0L) return null
            return bands[index]
        }

        override suspend fun readGlobalGainDb(): Double? {
            if (!readable) return null
            if (writeStarted) globalGainReadsAfterWrites++
            if (saveCount > 0) finalReadSessionGenerations += sessionGeneration
            if (sessionGeneration == 0L) return null
            val value = if (writeStarted) postWriteGlobalGainDb ?: globalGainDb else globalGainDb
            if (saveCount > 0 && detachAfterFinalGainRead && !lateDetachTriggered) {
                lateDetachTriggered = true
                detachGeneration++
                sessionGeneration = 0L
            }
            return value
        }

        override suspend fun readFirmwareVersion(): String? = "2.20"

        override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? {
            if (!readable) return null
            if (writeStarted) programReadsAfterWrites++
            if (saveCount > 0) finalReadSessionGenerations += sessionGeneration
            if (saveCount > 0 && detachDuringFinalReadback && !lateDetachTriggered) {
                lateDetachTriggered = true
                detachGeneration++
                sessionGeneration = 0L
                return null
            }
            if (sessionGeneration == 0L) return null
            return program
        }

        override suspend fun saveToFlash(): Boolean {
            val accepted = sendReport(FiioJa11Protocol.saveToFlashReport()) && saveReconnectAccepted
            if (accepted) {
                postSaveGlobalGainDb?.let { globalGainDb = it }
                if (saveDetachDelayMillis > 0L) {
                    delay(saveDetachDelayMillis)
                    detachGeneration++
                    sessionGeneration += 1L
                }
            }
            return accepted
        }

        override suspend fun awaitFinalReadbackReconnect(): Boolean {
            if ((!detachDuringFinalReadback && !detachAfterFinalGainRead) || sessionGeneration != 0L) return false
            sessionGeneration = 2L
            return true
        }

        override suspend fun sendReport(report: ByteArray): Boolean {
            writeStarted = true
            val command = report[5].toInt() and 0xFF
            sentCommands += command
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
            return true
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
