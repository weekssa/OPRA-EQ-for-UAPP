package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlProtocol
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlReadCodec
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Transport

/** Narrow read-only surface needed to build a Black Pearl native EQ snapshot. */
interface BlackPearlSnapshotSource {
    suspend fun readNativeBand(index: Int): BlackPearlReadCodec.NativeBand?
    suspend fun readGlobalGainRaw(): Int?
}

class BlackPearlSnapshotReader(
    private val source: BlackPearlSnapshotSource,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun read(sessionGeneration: Long): HardwareEqSnapshotBundle? {
        if (sessionGeneration <= 0) return null
        val bands = buildList {
            repeat(BlackPearlProtocol.BAND_COUNT) { index ->
                add(source.readNativeBand(index) ?: return null)
            }
        }
        val globalGainRaw = source.readGlobalGainRaw() ?: return null
        return HardwareEqSnapshotFactory.blackPearl(
            nativeBands = bands,
            globalGainRaw = globalGainRaw,
            sessionGeneration = sessionGeneration,
            verifiedAtEpochMillis = nowEpochMillis(),
        )
    }
}

/**
 * Read surface for the JA11 EQ tab.
 *
 * Active EQ program is read explicitly so stored User 1 coefficients are never misrepresented as
 * the current acoustic EQ while the device is Off or running a built-in Vocal/Classic/Bass program.
 */
interface FiioJa11SnapshotSource {
    suspend fun readEqProgram(): FiioJa11Protocol.EqProgram?
    suspend fun readBand(index: Int): FiioJa11Protocol.Band?
    suspend fun readGlobalGainDb(): Double?
}

class FiioJa11SnapshotReader(
    private val source: FiioJa11SnapshotSource,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun read(sessionGeneration: Long): HardwareEqSnapshotBundle? {
        if (sessionGeneration <= 0) return null
        val program = source.readEqProgram() ?: return null

        // Built-in programs are real current hardware state, but their native coefficient set is not
        // established by the current protocol evidence. Returning no PEQ bundle is safer than drawing
        // the stored User 1 bank as though it were Vocal/Classic/Bass. DEVICE state still reports the
        // exact active program separately.
        if (program != FiioJa11Protocol.EqProgram.USER_1 && program != FiioJa11Protocol.EqProgram.OFF) {
            return null
        }

        val bands = buildList {
            repeat(FiioJa11Protocol.BAND_COUNT) { index ->
                add(source.readBand(index) ?: return null)
            }
        }
        val globalEqGainDb = source.readGlobalGainDb() ?: return null
        return HardwareEqSnapshotFactory.fiioJa11(
            nativeBands = bands,
            globalEqGainDb = globalEqGainDb,
            sessionGeneration = sessionGeneration,
            verifiedAtEpochMillis = nowEpochMillis(),
            eqEnabled = program == FiioJa11Protocol.EqProgram.USER_1,
        )
    }
}

class JcallyJm12SnapshotReader(
    private val transport: JcallyJm12Transport,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun read(sessionGeneration: Long): HardwareEqSnapshotBundle? {
        if (sessionGeneration <= 0) return null
        if (!transport.handshake()) return null

        val protocolFlags = transport.readRegister(JcallyJm12Protocol.REG_PROTOCOL_FLAGS) ?: return null
        val eqEnableRegister = transport.readRegister(JcallyJm12Protocol.REG_EQ_DAC_ENABLE) ?: return null
        val digitalGainRegister = transport.readRegister(JcallyJm12Protocol.REG_DIGITAL_DAC_GAIN) ?: return null
        val digitalGainSteps = JcallyJm12Protocol.decodeDigitalGainSteps(
            digitalGainRegister,
            protocolFlags,
        )

        val bands = buildList {
            repeat(JcallyJm12Protocol.BAND_COUNT) { index ->
                val address = JcallyJm12Protocol.bandRegisterAddress(index)
                val a = transport.readRegister(address) ?: return null
                val b = transport.readRegister(address + 1) ?: return null
                add(JcallyJm12Protocol.decodeBand(a, b) ?: return null)
            }
        }

        return HardwareEqSnapshotFactory.jcallyJm12Stock(
            nativeBands = bands,
            eqEnabled = JcallyJm12Protocol.isEqEnabled(eqEnableRegister),
            digitalGainSteps = digitalGainSteps,
            sessionGeneration = sessionGeneration,
            verifiedAtEpochMillis = nowEpochMillis(),
        )
    }
}
