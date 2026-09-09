package com.weekssa.opraeqforuapp.domain.kt02h20

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JcallyJm12FlasherTest {
    @Test
    fun flashBypassesEqWritesAllFiveBandsTracksGainVerifiesAndReenables() = runBlocking {
        val transport = FakeJm12Transport()
        val store = FakeGainStore()
        val result = JcallyJm12Flasher(transport, store).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.Success)
        result as Kt02h20FlashResult.Success
        assertFalse(result.explicitPersistenceCommandUsed)
        assertTrue(JcallyJm12Protocol.isEqEnabled(transport.registers.getValue(JcallyJm12Protocol.REG_EQ_DAC_ENABLE)))
        assertEquals(-6, store.steps)
        assertEquals(
            -6,
            JcallyJm12Protocol.decodeDigitalGainSteps(
                transport.registers.getValue(JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN),
                transport.registers.getValue(JcallyJm12Protocol.REG_PROTOCOL_FLAGS),
            ).single(),
        )
        val bandWrites = transport.writeLog.count { address ->
            address in JcallyJm12Protocol.REG_EQ_DAC_BASE..(JcallyJm12Protocol.REG_EQ_DAC_BASE + 9)
        }
        assertEquals(10, bandWrites)
        assertEquals(JcallyJm12Protocol.REG_EQ_DAC_ENABLE, transport.writeLog.first())
        assertEquals(JcallyJm12Protocol.REG_EQ_DAC_ENABLE, transport.writeLog.last())
    }

    @Test
    fun retryReplacesTrackedGainInsteadOfStackingIt() = runBlocking {
        val transport = FakeJm12Transport()
        val store = FakeGainStore()
        val flasher = JcallyJm12Flasher(transport, store)

        assertTrue(flasher.flash(exactProfile()) is Kt02h20FlashResult.Success)
        val firstGain = JcallyJm12Protocol.decodeDigitalGainSteps(
            transport.registers.getValue(JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN),
            transport.registers.getValue(JcallyJm12Protocol.REG_PROTOCOL_FLAGS),
        ).single()
        assertTrue(flasher.flash(exactProfile()) is Kt02h20FlashResult.Success)
        val secondGain = JcallyJm12Protocol.decodeDigitalGainSteps(
            transport.registers.getValue(JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN),
            transport.registers.getValue(JcallyJm12Protocol.REG_PROTOCOL_FLAGS),
        ).single()

        assertEquals(-6, firstGain)
        assertEquals(firstGain, secondGain)
        assertEquals(-6, store.steps)
    }

    @Test
    fun bandTransferFailureLeavesEqSafelyBypassed() = runBlocking {
        val transport = FakeJm12Transport(failWriteAddress = JcallyJm12Protocol.REG_EQ_DAC_BASE + 2)
        val result = JcallyJm12Flasher(transport, FakeGainStore()).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.TransferFailed)
        assertFalse(JcallyJm12Protocol.isEqEnabled(transport.registers.getValue(JcallyJm12Protocol.REG_EQ_DAC_ENABLE)))
    }

    @Test
    fun failedHandshakePreventsEveryWrite() = runBlocking {
        val transport = FakeJm12Transport(handshakeOk = false)
        val result = JcallyJm12Flasher(transport, FakeGainStore()).flash(exactProfile())

        assertTrue(result is Kt02h20FlashResult.DeviceUnavailable)
        assertTrue(transport.writeLog.isEmpty())
    }

    @Test
    fun resetFlattensAllBandsRestoresBaselineGainAndClearsTrackedDelta() = runBlocking {
        val transport = FakeJm12Transport().apply {
            registers[JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN] = 0xFA // -3 dB / -6 steps
            val boosted = JcallyJm12Protocol.encodeBand(Kt02h20Band("peak_dip", 1_000.0, 5.0, 1.0))
            registers[JcallyJm12Protocol.REG_EQ_DAC_BASE] = boosted.a
            registers[JcallyJm12Protocol.REG_EQ_DAC_BASE + 1] = boosted.b
        }
        val store = FakeGainStore(steps = -6)

        val result = JcallyJm12Flasher(transport, store).resetToFlat()

        assertTrue(result is Kt02h20FlatResetResult.Success)
        assertEquals(0, store.steps)
        assertEquals(
            0,
            JcallyJm12Protocol.decodeDigitalGainSteps(
                transport.registers.getValue(JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN),
                transport.registers.getValue(JcallyJm12Protocol.REG_PROTOCOL_FLAGS),
            ).single(),
        )
        assertTrue(JcallyJm12Protocol.isEqEnabled(transport.registers.getValue(JcallyJm12Protocol.REG_EQ_DAC_ENABLE)))
        repeat(5) { index ->
            val address = JcallyJm12Protocol.bandRegisterAddress(index)
            val band = JcallyJm12Protocol.decodeBand(
                transport.registers.getValue(address),
                transport.registers.getValue(address + 1),
            )!!
            assertEquals(0.0, band.gainDb, 0.0)
        }
    }

    private fun exactProfile(): OpraEqProfile = OpraEqProfile(
        id = "jm12-flash",
        productId = "product",
        author = "Test",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = -3.0,
        bands = listOf(
            OpraBand("low_shelf", 100.0, 2.5, 0.7, null),
            OpraBand("peak_dip", 1_000.0, -1.5, 1.2, null),
        ),
    )

    private class FakeGainStore(
        var steps: Int = 0,
    ) : JcallyJm12GainStateStore {
        override fun readAppliedGainDeltaSteps(): Int = steps
        override fun writeAppliedGainDeltaSteps(steps: Int) {
            this.steps = steps
        }
    }

    private class FakeJm12Transport(
        private val handshakeOk: Boolean = true,
        private val failWriteAddress: Int? = null,
    ) : JcallyJm12Transport {
        val registers = mutableMapOf<Int, Int>()
        val writeLog = mutableListOf<Int>()

        init {
            registers[JcallyJm12Protocol.REG_PROTOCOL_FLAGS] = 0x0200
            registers[JcallyJm12Protocol.REG_EQ_DAC_ENABLE] = 0x01
            registers[JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN] = 0x00
            JcallyJm12Protocol.completeBands(emptyList()).forEachIndexed { index, band ->
                val encoded = JcallyJm12Protocol.encodeBand(band)
                val address = JcallyJm12Protocol.bandRegisterAddress(index)
                registers[address] = encoded.a
                registers[address + 1] = encoded.b
            }
        }

        override suspend fun handshake(): Boolean = handshakeOk

        override suspend fun readRegister(address: Int): Int? = registers[address]

        override suspend fun writeRegister(address: Int, value: Int): Boolean {
            writeLog += address
            if (address == failWriteAddress) return false
            registers[address] = value
            return true
        }
    }
}
