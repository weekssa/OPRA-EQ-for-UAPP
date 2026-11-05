package com.weekssa.opraeqforuapp.domain.ew300

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Ew300UacDescriptorTest {
    private fun descriptor(): ByteArray = bytes(
        // Configuration
        0x09, 0x02, 0x4F, 0x00, 0x02, 0x01, 0x00, 0x80, 0x32,
        // AudioControl interface 0
        0x09, 0x04, 0x00, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00,
        // Input terminal: microphone 0x0201
        0x0C, 0x24, 0x02, 0x01, 0x01, 0x02, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
        // Feature unit 2 -> source 1, master mute+volume
        0x09, 0x24, 0x06, 0x02, 0x01, 0x01, 0x03, 0x00, 0x00,
        // AudioStreaming interface 1
        0x09, 0x04, 0x01, 0x00, 0x01, 0x01, 0x02, 0x00, 0x00,
        // AS general: terminal link 1, format tag 1
        0x07, 0x24, 0x01, 0x01, 0x01, 0x01, 0x00,
        // Format type: 1 channel, 2-byte, 16-bit, 3 discrete rates
        0x11, 0x24, 0x02, 0x01, 0x01, 0x02, 0x10, 0x03,
        0x44, 0xAC, 0x00, 0x80, 0xBB, 0x00, 0x00, 0x77, 0x01,
        // Isochronous IN endpoint 0x82
        0x07, 0x05, 0x82, 0x01, 0x40, 0x00, 0x01,
    )

    @Test
    fun parsesUac1MicrophoneFeatureAndStreamingRates() {
        val result = requireNotNull(Ew300UacDescriptorParser.parse(descriptor()))
        assertEquals("UAC 1.0", result.uacVersion)
        assertEquals(0, result.audioControlInterface)
        assertEquals(1, result.streams.size)
        val stream = result.streams.single()
        assertEquals(1, stream.interfaceNumber)
        assertEquals(0x82, stream.endpointAddress)
        assertEquals(listOf(44_100, 48_000, 96_000), stream.sampleRates)
        val mic = requireNotNull(result.microphoneFeature)
        assertEquals(2, mic.unitId)
        assertTrue(mic.muteSupported)
        assertEquals(Ew300UacDescriptorParser.TERMINAL_MICROPHONE, mic.sourceTerminalType)
    }

    @Test
    fun buildsTheExactUac1ControlRequests() {
        val parsed = requireNotNull(Ew300UacDescriptorParser.parse(descriptor()))
        val stream = parsed.streams.single()
        val endpoint = requireNotNull(Ew300UacRequests.currentSampleRateEndpoint(stream))
        assertEquals(0xA2, endpoint.requestType)
        assertEquals(0x81, endpoint.request)
        assertEquals(0x0100, endpoint.value)
        assertEquals(0x8201, endpoint.index)
        assertEquals(3, endpoint.length)

        val mic = requireNotNull(parsed.microphoneFeature)
        val muteGet = Ew300UacRequests.microphoneMuteGet(mic.unitId, 0)
        assertEquals(0xA1, muteGet.requestType)
        assertEquals(0x0100, muteGet.value)
        assertEquals(0x0200, muteGet.index)
        val muteSet = Ew300UacRequests.microphoneMuteSet(mic.unitId, 0, muted = true)
        assertEquals(0x21, muteSet.requestType)
        assertEquals(0x01, muteSet.request)
        assertEquals(1, muteSet.data?.single()?.toInt())
    }

    @Test
    fun exactEw300DescriptorExposesMicMuteAndPlaybackRatePath() {
        val raw = hex("09022F01040100A0320904000000010100000A240100015B000201020C2402010102000101000000092406020101010200072405030102000924030401010003000C24020501010002030000000D2404060205050203000000000A240607060101020200092403080103000700090401000001020000090401010101020000072401040101000E2402010102100244AC0080BB000905810560000100000725010100000009040200000102000009040201010102000007240105010100112402010202100344AC0080BB000077010905010980010100000725010100000009040202010102000007240105010100112402010203180344AC0080BB0000770109050109400201000007250101000000090403000203000000092110012101224A000705820310000107050203100001")
        val parsed = requireNotNull(Ew300UacDescriptorParser.parse(raw))
        assertEquals("UAC 1.0", parsed.uacVersion)
        assertEquals(0, parsed.audioControlInterface)
        assertTrue(parsed.streams.any { it.interfaceNumber == 2 && it.endpointAddress == 0x01 })
        assertTrue(parsed.streams.any { it.interfaceNumber == 1 && it.endpointAddress == 0x81 })
        val mic = requireNotNull(parsed.microphoneFeature)
        assertEquals(2, mic.unitId)
        assertTrue(mic.muteSupported)
        assertEquals(Ew300UacDescriptorParser.TERMINAL_MICROPHONE, mic.sourceTerminalType)
    }
}

private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

private fun hex(value: String): ByteArray =
    value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
