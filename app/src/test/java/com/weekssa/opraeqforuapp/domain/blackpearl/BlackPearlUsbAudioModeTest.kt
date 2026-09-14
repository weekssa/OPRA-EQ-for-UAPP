package com.weekssa.opraeqforuapp.domain.blackpearl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlackPearlUsbAudioModeTest {
    @Test
    fun parsesUac1AudioControlHeader() {
        assertEquals(
            BlackPearlUsbAudioMode.UAC_1_0,
            BlackPearlUsbAudioDescriptorParser.parse(descriptors(0x0100)),
        )
    }

    @Test
    fun parsesUac2AudioControlHeader() {
        assertEquals(
            BlackPearlUsbAudioMode.UAC_2_0,
            BlackPearlUsbAudioDescriptorParser.parse(descriptors(0x0200)),
        )
    }

    @Test
    fun ignoresClassSpecificHeaderOutsideAudioControlInterface() {
        val raw = byteArrayOf(
            9, 4, 0, 0, 0, 0x03, 0, 0, 0,
            5, 0x24, 1, 0, 2,
        )
        assertNull(BlackPearlUsbAudioDescriptorParser.parse(raw))
    }

    @Test
    fun rejectsMalformedDescriptorLength() {
        assertNull(
            BlackPearlUsbAudioDescriptorParser.parse(
                byteArrayOf(9, 4, 0, 0),
            ),
        )
    }

    @Test
    fun rejectsConflictingAudioControlRevisions() {
        val raw = descriptors(0x0100) + descriptors(0x0200)
        assertNull(BlackPearlUsbAudioDescriptorParser.parse(raw))
    }

    @Test
    fun unsupportedAudioControlRevisionIsNotGuessed() {
        assertNull(BlackPearlUsbAudioDescriptorParser.parse(descriptors(0x0300)))
    }

    private fun descriptors(bcdAdc: Int): ByteArray = byteArrayOf(
        18, 1, 0, 2, 0, 0, 0, 64, 0x02, 0x33, 0xE8.toByte(), 0x43, 0, 1, 1, 2, 3, 1,
        9, 4, 0, 0, 0, 1, 1, 0, 0,
        5, 0x24, 1, (bcdAdc and 0xFF).toByte(), ((bcdAdc ushr 8) and 0xFF).toByte(),
    )
}
