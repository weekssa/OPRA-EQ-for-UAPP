package com.weekssa.opraeqforuapp.domain.ew300

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300QualifiedVolatileProtocolTest {
    @Test fun readGoldenVectorUsesThePhysicallyObservedElevenByteFraming() {
        assertArrayEquals(bytes(0x4B,0x26,0,0,0,0x52,0,0,0,0,0), Ew300QualifiedVolatileProtocol.readReport(0x26))
    }
    @Test fun temporaryWriteGoldenVectorUsesOnlyAQualifiedBandRegister() {
        assertArrayEquals(bytes(0x4B,0x26,0,0,0,0x57,0,0xF6,0xFF,0x64,0), Ew300QualifiedVolatileProtocol.temporaryWriteReport(0x26,bytes(0xF6,0xFF,0x64,0)))
    }
    @Test fun qualifiedWriteBoundaryExcludesSlotAndGlobalGain() {
        assertEquals((0x26..0x2F).toList(), Ew300QualifiedVolatileProtocol.qualifiedVolatileRegisterAddresses())
        assertFalse(Ew300QualifiedVolatileProtocol.isQualifiedVolatileRegister(0x24))
        assertFalse(Ew300QualifiedVolatileProtocol.isQualifiedVolatileRegister(0x66))
        assertTrue(Ew300QualifiedVolatileProtocol.isQualifiedVolatileRegister(0x2F))
        assertFails { Ew300QualifiedVolatileProtocol.temporaryWriteReport(0x24, bytes(0,0,0,0)) }
        assertFails { Ew300QualifiedVolatileProtocol.temporaryWriteReport(0x66, bytes(0,0,0,0)) }
    }
    @Test fun readbackRequiresExactReportAddressAndReadCommand() {
        val expected=bytes(0xF5,0xFF,0x64,0); val valid=bytes(0x4B,0x26,0,0,0,0x52,0,0xF5,0xFF,0x64,0)
        assertArrayEquals(expected,Ew300QualifiedVolatileProtocol.readbackData(0x26,valid))
        assertTrue(Ew300QualifiedVolatileProtocol.matchesExpectedReadback(0x26,expected,valid))
        assertFalse(Ew300QualifiedVolatileProtocol.matchesExpectedReadback(0x26,bytes(0xF6,0xFF,0x64,0),valid))
        assertNull(Ew300QualifiedVolatileProtocol.readbackData(0x26,valid.copyOfRange(0,10)))
        assertNull(Ew300QualifiedVolatileProtocol.readbackData(0x27,valid))
        assertNull(Ew300QualifiedVolatileProtocol.readbackData(0x26,bytes(0x4B,0x26,0,0,0,0x57,0,0,0,0,0)))
    }
    private fun assertFails(block:()->Unit) { try { block() } catch (_:IllegalArgumentException) { return }; throw AssertionError("Expected IllegalArgumentException") }
    private fun bytes(vararg values:Int):ByteArray=ByteArray(values.size){i->(values[i] and 0xFF).toByte()}
}
