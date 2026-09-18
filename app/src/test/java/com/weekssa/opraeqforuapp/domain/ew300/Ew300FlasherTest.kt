package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300FlasherTest {
    @Test
    fun flashWritesAllFiveBandsCommitsAndVerifiesReadback() = runBlocking {
        val transport = FakeTransport()
        val result = Ew300Flasher(transport).flash(profile(preamp = null))

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.Success)
        assertEquals(10, transport.writes.size)
        assertEquals(1, transport.commitCount)
        assertEquals(10, transport.readsAfterWrites)
    }

    @Test
    fun sourcePreampIsRejectedBeforeAnyDeviceWrite() = runBlocking {
        val transport = FakeTransport()
        val result = Ew300Flasher(transport).flash(profile(preamp = -4.0))

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.NotSuitable)
        assertTrue(transport.writes.isEmpty())
        assertEquals(0, transport.commitCount)
    }

    @Test
    fun resetWritesFlatStateAndVerifiesIt() = runBlocking {
        val transport = FakeTransport()
        val result = Ew300Flasher(transport).resetToFlat()

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult.Success)
        assertEquals(10, transport.writes.size)
        assertEquals(1, transport.commitCount)
        assertTrue(transport.state.values.any { it.contentEquals(bytes(0, 0, 0xE8, 0x03)) })
    }

    @Test
    fun commitFailureStopsBeforeReadbackVerification() = runBlocking {
        val transport = FakeTransport(commitSucceeds = false)

        val result = Ew300Flasher(transport).flash(profile(preamp = null))

        assertTrue(result is com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult.TransferFailed)
        assertEquals(0, transport.readsAfterWrites)
    }

    private fun profile(preamp: Double?): OpraEqProfile = OpraEqProfile(
        id = "ew300-test",
        productId = "product",
        author = "Test",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = preamp,
        bands = listOf(
            OpraBand("peak_dip", 1_000.0, 0.0, 1.0, null),
            OpraBand("peak_dip", 2_000.0, 0.0, 1.0, null),
        ),
    )

    private inner class FakeTransport(
        private val commitSucceeds: Boolean = true,
    ) : Ew300Transport {
        val state = (0 until Ew300Protocol.BAND_COUNT)
            .flatMap { index ->
                val register = Ew300Protocol.bandRegister(index)
                listOf(register, register + 1)
            }
            .associateWith { bytes(0, 0, 0, 0) }
            .toMutableMap()
        val writes = mutableListOf<Int>()
        var commitCount = 0
        var readsAfterWrites = 0

        override suspend fun readRegister(register: Int): ByteArray? {
            if (writes.isNotEmpty()) readsAfterWrites++
            return state[register]
        }

        override suspend fun writeRegister(register: Int, data: ByteArray): Boolean {
            writes += register
            state[register] = data.copyOf()
            return true
        }

        override suspend fun commit(): Boolean {
            commitCount++
            return commitSucceeds
        }
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { index ->
        (values[index] and 0xFF).toByte()
    }
}
