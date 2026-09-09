package com.weekssa.opraeqforuapp.data.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlReadCodec
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Transport
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Transport
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HardwareEqSnapshotReadersTest {
    @Test
    fun blackPearlReaderRequiresEveryBandAndReadsGainWithoutWriting() = runBlocking {
        val source = FakeBlackPearlSource(blackPearlBands(), globalGainRaw = -2_560)
        val bundle = BlackPearlSnapshotReader(source) { 99L }.read(sessionGeneration = 3)

        assertThat(bundle).isNotNull()
        assertThat(source.bandReads).containsExactlyElementsIn((0 until 10).toList()).inOrder()
        assertThat(source.globalGainReads).isEqualTo(1)
        assertThat(bundle!!.snapshot.playbackGainDb).isEqualTo(-10.0)
        assertThat(bundle.snapshot.verifiedAtEpochMillis).isEqualTo(99L)
    }

    @Test
    fun blackPearlReaderFailsClosedOnMissingBand() = runBlocking {
        val source = FakeBlackPearlSource(
            bands = blackPearlBands().associateBy { it.index } - 6,
            globalGainRaw = 0,
        )

        val bundle = BlackPearlSnapshotReader(source).read(sessionGeneration = 1)

        assertThat(bundle).isNull()
        assertThat(source.globalGainReads).isEqualTo(0)
    }

    @Test
    fun ja11ReaderReadsFiveBandsAndGlobalEqGainOnly() = runBlocking {
        val transport = FakeJa11Transport()
        val bundle = FiioJa11SnapshotReader(transport) { 55L }.read(sessionGeneration = 2)

        assertThat(bundle).isNotNull()
        assertThat(transport.bandReads).containsExactly(0, 1, 2, 3, 4).inOrder()
        assertThat(transport.globalGainReads).isEqualTo(1)
        assertThat(transport.sendCount).isEqualTo(0)
        assertThat(bundle!!.snapshot.dedicatedEqPreampDb).isEqualTo(-3.0)
        assertThat(bundle.snapshot.verifiedAtEpochMillis).isEqualTo(55L)
    }

    @Test
    fun jm12ReaderHandshakesAndReadsRegistersWithoutAnyWrite() = runBlocking {
        val transport = FakeJm12Transport()
        val bundle = JcallyJm12SnapshotReader(transport) { 77L }.read(sessionGeneration = 4)

        assertThat(bundle).isNotNull()
        assertThat(transport.handshakeCount).isEqualTo(1)
        assertThat(transport.writeCount).isEqualTo(0)
        assertThat(bundle!!.snapshot.playbackGainDb).isEqualTo(-6.0)
        assertThat(bundle.fingerprint.eqEnabled).isTrue()
        assertThat(bundle.snapshot.verifiedAtEpochMillis).isEqualTo(77L)
        assertThat(transport.readAddresses).contains(JcallyJm12Protocol.REG_PROTOCOL_FLAGS)
        assertThat(transport.readAddresses).contains(JcallyJm12Protocol.REG_EQ_DAC_ENABLE)
        assertThat(transport.readAddresses).contains(JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN)
    }

    @Test
    fun jm12ReaderFailedHandshakePerformsNoRegisterReadOrWrite() = runBlocking {
        val transport = FakeJm12Transport(handshakeOk = false)

        val bundle = JcallyJm12SnapshotReader(transport).read(sessionGeneration = 1)

        assertThat(bundle).isNull()
        assertThat(transport.readAddresses).isEmpty()
        assertThat(transport.writeCount).isEqualTo(0)
    }

    private class FakeBlackPearlSource(
        bands: List<BlackPearlReadCodec.NativeBand>,
        private val globalGainRaw: Int?,
    ) : BlackPearlSnapshotSource {
        constructor(
            bands: Map<Int, BlackPearlReadCodec.NativeBand>,
            globalGainRaw: Int?,
        ) : this(bands.values.toList(), globalGainRaw)

        private val byIndex = bands.associateBy { it.index }
        val bandReads = mutableListOf<Int>()
        var globalGainReads = 0

        override suspend fun readNativeBand(index: Int): BlackPearlReadCodec.NativeBand? {
            bandReads += index
            return byIndex[index]
        }

        override suspend fun readGlobalGainRaw(): Int? {
            globalGainReads += 1
            return globalGainRaw
        }
    }

    private class FakeJa11Transport : FiioJa11Transport {
        val bandReads = mutableListOf<Int>()
        var globalGainReads = 0
        var sendCount = 0

        override suspend fun readBand(index: Int): FiioJa11Protocol.Band? {
            bandReads += index
            return FiioJa11Protocol.Band(
                type = "peak_dip",
                frequencyHz = listOf(80.0, 250.0, 1_000.0, 4_000.0, 12_000.0)[index],
                gainDb = if (index == 0) 2.0 else 0.0,
                q = 0.7,
            )
        }

        override suspend fun readGlobalGainDb(): Double? {
            globalGainReads += 1
            return -3.0
        }

        override suspend fun sendReport(report: ByteArray): Boolean {
            sendCount += 1
            return true
        }
    }

    private class FakeJm12Transport(
        private val handshakeOk: Boolean = true,
    ) : JcallyJm12Transport {
        val registers = mutableMapOf<Int, Int>()
        val readAddresses = mutableListOf<Int>()
        var handshakeCount = 0
        var writeCount = 0

        init {
            registers[JcallyJm12Protocol.REG_PROTOCOL_FLAGS] = 0x0200
            registers[JcallyJm12Protocol.REG_EQ_DAC_ENABLE] = 0x01
            registers[JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN] = 0xF4
            List(5) { index ->
                Kt02h20Band(
                    type = "peak_dip",
                    frequencyHz = listOf(80.0, 250.0, 1_000.0, 4_000.0, 12_000.0)[index],
                    gainDb = if (index == 0) 1.5 else 0.0,
                    q = 0.7,
                )
            }.forEachIndexed { index, band ->
                val encoded = JcallyJm12Protocol.encodeBand(band)
                val address = JcallyJm12Protocol.bandRegisterAddress(index)
                registers[address] = encoded.a
                registers[address + 1] = encoded.b
            }
        }

        override suspend fun handshake(): Boolean {
            handshakeCount += 1
            return handshakeOk
        }

        override suspend fun readRegister(address: Int): Int? {
            readAddresses += address
            return registers[address]
        }

        override suspend fun writeRegister(address: Int, value: Int): Boolean {
            writeCount += 1
            registers[address] = value
            return true
        }
    }

    private fun blackPearlBands(): List<BlackPearlReadCodec.NativeBand> = List(10) { index ->
        BlackPearlReadCodec.NativeBand(
            index = index,
            type = EqFilterType.PEAK,
            frequencyRawHz = listOf(31, 63, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)[index],
            gainRaw256 = if (index == 0) 256 else 0,
            qRaw256 = 256,
            activeSlot = 2,
        )
    }
}
