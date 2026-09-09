package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiioJa11FlasherTest {
    @Test
    fun flashWritesFiveSlotsThenGainApplyVerifySaveAndFinalVerify() = runBlocking {
        val transport = FakeJa11Transport()
        val flasher = FiioJa11Flasher(transport)

        val result = flasher.flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.Success)
        result as Kt02h20FlashResult.Success
        assertTrue(result.explicitPersistenceCommandUsed)
        assertEquals(listOf(0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19), transport.sentCommands)
        assertEquals(10, transport.bandReadsAfterWrites)
        assertEquals(2, transport.globalGainReadsAfterWrites)
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
    fun resetWritesAllFiveFlatSlotsZeroGainApplyAndSave() = runBlocking {
        val transport = FakeJa11Transport().apply {
            globalGainDb = -6.0
            bands[0] = FiioJa11Protocol.Band("peak_dip", 1_000.0, 6.0, 1.0)
        }

        val result = FiioJa11Flasher(transport).resetToFlat()

        assertTrue(result is Kt02h20FlatResetResult.Success)
        assertEquals(0.0, transport.globalGainDb, 0.001)
        assertTrue(transport.bands.all { kotlin.math.abs(it.gainDb) < 0.000_001 })
        assertEquals(listOf(0x15, 0x15, 0x15, 0x15, 0x15, 0x17, 0x18, 0x19), transport.sentCommands)
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

    private class FakeJa11Transport(
        private val failCommand: Int? = null,
        private val readable: Boolean = true,
    ) : FiioJa11Transport {
        val bands = FiioJa11Protocol.completeBands(emptyList()).toMutableList()
        var globalGainDb: Double = 0.0
        val sentCommands = mutableListOf<Int>()
        var saveCount = 0
        var writeStarted = false
        var bandReadsAfterWrites = 0
        var globalGainReadsAfterWrites = 0

        override suspend fun readBand(index: Int): FiioJa11Protocol.Band? {
            if (!readable) return null
            if (writeStarted) bandReadsAfterWrites++
            return bands[index]
        }

        override suspend fun readGlobalGainDb(): Double? {
            if (!readable) return null
            if (writeStarted) globalGainReadsAfterWrites++
            return globalGainDb
        }

        override suspend fun sendReport(report: ByteArray): Boolean {
            writeStarted = true
            val command = report[5].toInt() and 0xFF
            sentCommands += command
            if (command == failCommand) return false
            when (command) {
                0x15 -> {
                    val index = report[7].toInt() and 0xFF
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
                0x17 -> {
                    val rawUnsigned = (report[7].toInt() and 0xFF) or ((report[8].toInt() and 0xFF) shl 8)
                    val raw = if (rawUnsigned >= 0x8000) rawUnsigned - 0x10000 else rawUnsigned
                    globalGainDb = raw / 2560.0
                }
                0x19 -> saveCount++
            }
            return true
        }
    }
}
