package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackPearlFlasherTest {
    @Test
    fun successfulZeroPreampFlashReadsSlotAndGainThenSendsCompleteEqSequence() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x06, globalGainRaw = -2_000)
        val store = FakeGainStore()
        val result = flash(transport, store, profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.Success)
        result as BlackPearlFlashResult.Success
        assertEquals(DevicePresetFidelity.EXACT, result.fidelity)
        assertEquals(0.0, result.appliedPlaybackGainDb, 0.0)
        assertEquals(12, transport.sent.size)
        assertTrue(transport.sent.none { it[2].u8() == 0x03 })
        assertTrue(transport.sent.take(10).all { it[36].u8() == 0x06 })
    }

    @Test
    fun finalNativeReadbackAndGainMatchAreRequiredForSuccess() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x06, globalGainRaw = -2_000)

        val result = flash(transport, FakeGainStore(), profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.Success)
        assertEquals(10, transport.nativeBandReadCount)
        assertEquals(2, transport.globalGainReadCount)
    }

    @Test
    fun missingFinalNativeReadbackIsNotVerifiedWithoutRetry() = runBlocking {
        val transport = FakeTransport(
            activeSlot = 0x06,
            globalGainRaw = -2_000,
            missingBandIndex = 4,
        )

        val result = flash(transport, FakeGainStore(), profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.VerificationFailed)
        assertFalse(result is BlackPearlFlashResult.Success)
        assertEquals(12, transport.sent.size)
        assertTrue((result as BlackPearlFlashResult.VerificationFailed).reason.contains("band 5"))
    }

    @Test
    fun oneMismatchedBandFieldIsNotVerifiedWithoutAutomaticRewrite() = runBlocking {
        val transport = FakeTransport(
            activeSlot = 0x06,
            globalGainRaw = -2_000,
            mismatchedBandIndex = 3,
        )

        val result = flash(transport, FakeGainStore(), profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.VerificationFailed)
        assertFalse(result is BlackPearlFlashResult.Success)
        assertEquals(12, transport.sent.size)
        assertTrue((result as BlackPearlFlashResult.VerificationFailed).reason.contains("band 4"))
        assertTrue((result as BlackPearlFlashResult.VerificationFailed).reason.contains("gain"))
    }

    @Test
    fun mismatchedFinalGainIsNotVerifiedWithoutAutomaticRewrite() = runBlocking {
        val transport = FakeTransport(
            activeSlot = 0x06,
            globalGainRaw = -2_000,
            finalGlobalGainRawOverride = -1_999,
        )

        val result = flash(transport, FakeGainStore(), profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.VerificationFailed)
        assertFalse(result is BlackPearlFlashResult.Success)
        assertEquals(12, transport.sent.size)
        assertTrue((result as BlackPearlFlashResult.VerificationFailed).reason.contains("playback-gain"))
    }

    @Test
    fun missingFinalGainReadbackIsNotVerifiedWithoutAutomaticRewrite() = runBlocking {
        val transport = FakeTransport(
            activeSlot = 0x06,
            globalGainRaw = -2_000,
            missingFinalGlobalGainRead = true,
        )

        val result = flash(transport, FakeGainStore(), profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.VerificationFailed)
        assertFalse(result is BlackPearlFlashResult.Success)
        assertEquals(12, transport.sent.size)
        assertTrue((result as BlackPearlFlashResult.VerificationFailed).reason.contains("playback-gain"))
    }

    @Test
    fun sessionChangeDuringFinalVerificationIsNotVerifiedAndDoesNotMutateAgain() = runBlocking {
        val transport = FakeTransport(
            activeSlot = 0x06,
            globalGainRaw = -2_000,
            invalidateSessionAfterFlash = true,
        )

        val result = flash(transport, FakeGainStore(), profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.VerificationFailed)
        assertFalse(result is BlackPearlFlashResult.Success)
        assertEquals(12, transport.sent.size)
        assertTrue((result as BlackPearlFlashResult.VerificationFailed).reason.contains("session"))
    }

    @Test
    fun negativePreampAdjustsGlobalGainBeforeEqWrites() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x02, globalGainRaw = -2_000)
        val store = FakeGainStore()

        val result = flash(transport, store, profile(preamp = -4.5))

        assertTrue(result is BlackPearlFlashResult.Success)
        assertEquals(13, transport.sent.size)
        assertEquals(0x03, transport.sent.first()[2].u8())
        assertEquals(-2_000 + BlackPearlProtocol.gainDbToRawDelta(-4.5), gainRaw(transport.sent.first()))
        assertEquals(BlackPearlProtocol.gainDbToRawDelta(-4.5), store.appliedRaw)
    }

    @Test
    fun laterFlashReplacesPriorEqGainInsteadOfAccumulatingIt() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x01, globalGainRaw = -2_000)
        val store = FakeGainStore()

        flash(transport, store, profile(preamp = -4.0))
        assertEquals(-3_024, transport.globalGainRaw)

        transport.sent.clear()
        flash(transport, store, profile(preamp = -6.0))

        assertEquals(-3_536, transport.globalGainRaw)
        assertEquals(-1_536, store.appliedRaw)
        assertEquals(-3_536, gainRaw(transport.sent.first()))
    }

    @Test
    fun zeroPreampRestoresPriorEqLibraryAttenuation() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x01, globalGainRaw = -2_000)
        val store = FakeGainStore()

        flash(transport, store, profile(preamp = -4.0))
        transport.sent.clear()
        flash(transport, store, profile(preamp = 0.0))

        assertEquals(-2_000, transport.globalGainRaw)
        assertEquals(0, store.appliedRaw)
        assertEquals(-2_000, gainRaw(transport.sent.first()))
    }

    @Test
    fun flatResetFlattensActiveSlotBeforeRestoringTrackedGain() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x05, globalGainRaw = -3_024)
        val store = FakeGainStore(appliedRaw = -1_024)

        val result = BlackPearlFlasher(transport, store).resetToFlat()

        assertTrue(result is BlackPearlFlatResetResult.Success)
        result as BlackPearlFlatResetResult.Success
        assertEquals(4.0, result.restoredPlaybackGainDb, 0.0)
        assertEquals(0, store.appliedRaw)
        assertEquals(-2_000, transport.globalGainRaw)
        assertEquals(13, transport.sent.size)
        transport.sent.take(10).forEachIndexed { index, report ->
            assertEquals(0x09, report[2].u8())
            assertEquals(index, report[5].u8())
            assertEquals(0, bandGainRaw(report))
            assertEquals(0x05, report[36].u8())
        }
        assertEquals(0x0A, transport.sent[10][2].u8())
        assertEquals(0x01, transport.sent[11][2].u8())
        assertEquals(0x03, transport.sent[12][2].u8())
        assertEquals(-2_000, gainRaw(transport.sent[12]))
        assertTrue(transport.sent.take(12).none { it[2].u8() == 0x03 })
    }

    @Test
    fun flatResetWithoutTrackedGainSkipsGlobalGainWriteButStillClearsEqBands() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x03, globalGainRaw = -2_000)
        val store = FakeGainStore()

        val result = BlackPearlFlasher(transport, store).resetToFlat()

        assertTrue(result is BlackPearlFlatResetResult.Success)
        assertEquals(12, transport.sent.size)
        assertTrue(transport.sent.none { it[2].u8() == 0x03 })
        assertTrue(transport.sent.take(10).all { bandGainRaw(it) == 0 && it[36].u8() == 0x03 })
        assertEquals(0, store.appliedRaw)
    }

    @Test
    fun flatResetRejectsUnsafeBaselineRestorationWithoutWrites() = runBlocking {
        val transport = FakeTransport(
            activeSlot = 0x00,
            globalGainRaw = BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW - 100,
        )
        val store = FakeGainStore(appliedRaw = -500)

        val result = BlackPearlFlasher(transport, store).resetToFlat()

        assertTrue(result is BlackPearlFlatResetResult.NotRepresentable)
        assertTrue(transport.sent.isEmpty())
        assertEquals(-500, store.appliedRaw)
    }

    @Test
    fun flatResetPeqFailureLeavesPlaybackGainAndTrackedDeltaUnchanged() = runBlocking {
        val transport = FakeTransport(
            activeSlot = 0x01,
            globalGainRaw = -3_024,
            failAtSend = 3,
        )
        val store = FakeGainStore(appliedRaw = -1_024)

        val result = BlackPearlFlasher(transport, store).resetToFlat()

        assertTrue(result is BlackPearlFlatResetResult.TransferFailed)
        assertEquals(-3_024, transport.globalGainRaw)
        assertEquals(-1_024, store.appliedRaw)
        assertEquals(3, transport.sent.size)
        assertTrue(transport.sent.none { it[2].u8() == 0x03 })
    }

    @Test
    fun flatResetGainRestoreFailureKeepsTrackedDeltaForSafeRetry() = runBlocking {
        val transport = FakeTransport(
            activeSlot = 0x01,
            globalGainRaw = -3_024,
            failAtSend = 13,
        )
        val store = FakeGainStore(appliedRaw = -1_024)

        val result = BlackPearlFlasher(transport, store).resetToFlat()

        assertTrue(result is BlackPearlFlatResetResult.TransferFailed)
        assertEquals(-3_024, transport.globalGainRaw)
        assertEquals(-1_024, store.appliedRaw)
        assertEquals(13, transport.sent.size)
        assertEquals(0x03, transport.sent.last()[2].u8())
        assertTrue(transport.sent.take(10).all { bandGainRaw(it) == 0 })
    }

    @Test
    fun missingActiveSlotFailsBeforeAnyWrite() = runBlocking {
        val transport = FakeTransport(activeSlot = null, globalGainRaw = -2_000)
        val result = flash(transport, FakeGainStore(), profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.DeviceUnavailable)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun flatResetMissingActiveSlotFailsBeforeAnyWrite() = runBlocking {
        val transport = FakeTransport(activeSlot = null, globalGainRaw = -2_000)
        val result = BlackPearlFlasher(transport, FakeGainStore()).resetToFlat()

        assertTrue(result is BlackPearlFlatResetResult.DeviceUnavailable)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun missingGlobalGainFailsBeforeAnyWrite() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x00, globalGainRaw = null)
        val result = flash(transport, FakeGainStore(), profile(preamp = -5.0))

        assertTrue(result is BlackPearlFlashResult.DeviceUnavailable)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun unrepresentableTargetGainFailsWithoutClampingOrWrites() = runBlocking {
        val transport = FakeTransport(
            activeSlot = 0x00,
            globalGainRaw = BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW + 100,
        )
        val result = flash(transport, FakeGainStore(), profile(preamp = -5.0))

        assertTrue(result is BlackPearlFlashResult.NotRepresentable)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun transportFailureStopsSequenceImmediately() = runBlocking {
        val transport = FakeTransport(activeSlot = 0x01, globalGainRaw = -2_000, failAtSend = 3)
        val result = flash(transport, FakeGainStore(), profile(preamp = 0.0))

        assertTrue(result is BlackPearlFlashResult.TransferFailed)
        assertEquals(3, transport.sent.size)
    }

    private fun profile(preamp: Double?) = OpraEqProfile(
        id = "p",
        productId = "product",
        author = "Tester",
        details = "Target",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = preamp,
        bands = listOf(
            OpraBand("peak_dip", 1_000.0, -2.0, 1.0, null),
            OpraBand("low_shelf", 105.0, 3.0, 0.75, null),
        ),
    )

    private class FakeGainStore(
        var appliedRaw: Int = 0,
    ) : BlackPearlGainStateStore {
        override fun readAppliedGainDeltaRaw(): Int = appliedRaw
        override fun writeAppliedGainDeltaRaw(rawDelta: Int) {
            appliedRaw = rawDelta
        }
    }

    private class FakeTransport(
        private val activeSlot: Byte?,
        globalGainRaw: Int?,
        private val failAtSend: Int? = null,
        private val missingBandIndex: Int? = null,
        private val mismatchedBandIndex: Int? = null,
        private val finalGlobalGainRawOverride: Int? = null,
        private val missingFinalGlobalGainRead: Boolean = false,
        private val invalidateSessionAfterFlash: Boolean = false,
    ) : BlackPearlTransport {
        val sent = mutableListOf<ByteArray>()
        var nativeBandReadCount = 0
        var globalGainReadCount = 0
        var sessionCurrent = true
        private val nativeBands = mutableMapOf<Int, BlackPearlReadCodec.NativeBand>()
        var globalGainRaw: Int? = globalGainRaw

        override suspend fun readActiveSlot(): Byte? = activeSlot

        override suspend fun readGlobalGainRaw(): Int? {
            globalGainReadCount++
            if (globalGainReadCount > 1 && missingFinalGlobalGainRead) return null
            return if (globalGainReadCount == 1) globalGainRaw else finalGlobalGainRawOverride ?: globalGainRaw
        }

        override suspend fun readNativeBand(index: Int): BlackPearlReadCodec.NativeBand? {
            nativeBandReadCount++
            if (index == missingBandIndex) return null
            return nativeBands[index]
        }

        override suspend fun sendReport(report: ByteArray): Boolean {
            sent += report.copyOf()
            val succeeds = failAtSend == null || sent.size != failAtSend
            if (succeeds && report[2].u8() == 0x03) {
                globalGainRaw = gainRaw(report)
            }
            if (succeeds && report[2].u8() == 0x09) {
                BlackPearlReadCodec.bandFromWriteReport(report)?.let { band ->
                    nativeBands[band.index] = if (
                        report[2].u8() == 0x09 && band.index == mismatchedBandIndex
                    ) {
                        band.copy(gainRaw256 = band.gainRaw256 + 1)
                    } else {
                        band
                    }
                }
            }
            if (succeeds && report[2].u8() == 0x01 && invalidateSessionAfterFlash) {
                sessionCurrent = false
            }
            return succeeds
        }
    }

    private suspend fun flash(
        transport: FakeTransport,
        store: FakeGainStore,
        profile: OpraEqProfile,
    ): BlackPearlFlashResult = BlackPearlFlasher(transport, store).flash(
        profile = profile,
        expectedSessionGeneration = 1L,
        isSessionCurrent = { transport.sessionCurrent },
    )
}

private fun gainRaw(report: ByteArray): Int = ByteBuffer.wrap(report, 4, 2)
    .order(ByteOrder.LITTLE_ENDIAN)
    .short
    .toInt()

private fun bandGainRaw(report: ByteArray): Int = ByteBuffer.wrap(report, 32, 2)
    .order(ByteOrder.LITTLE_ENDIAN)
    .short
    .toInt()

private fun Byte.u8(): Int = toInt() and 0xFF
