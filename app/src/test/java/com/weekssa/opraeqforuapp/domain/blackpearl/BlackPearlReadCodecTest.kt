package com.weekssa.opraeqforuapp.domain.blackpearl

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import org.junit.Test

class BlackPearlReadCodecTest {
    @Test
    fun decodesSignedNativePeakBandAndActiveSlot() {
        val response = response(
            index = 4,
            frequencyHz = 1234,
            qRaw256 = 320,
            gainRaw256 = -640,
            typeCode = 0x02,
            activeSlot = 3,
        )

        val band = BlackPearlReadCodec.bandFromResponse(response)

        assertThat(band).isNotNull()
        assertThat(band!!.index).isEqualTo(4)
        assertThat(band.type).isEqualTo(EqFilterType.PEAK)
        assertThat(band.frequencyRawHz).isEqualTo(1234)
        assertThat(band.frequencyHz).isEqualTo(1234.0)
        assertThat(band.qRaw256).isEqualTo(320)
        assertThat(band.q).isEqualTo(1.25)
        assertThat(band.gainRaw256).isEqualTo(-640)
        assertThat(band.gainDb).isEqualTo(-2.5)
        assertThat(band.activeSlot).isEqualTo(3)
    }

    @Test
    fun decodesShelfTypeCodes() {
        assertThat(
            BlackPearlReadCodec.bandFromResponse(response(typeCode = 0x03))?.type,
        ).isEqualTo(EqFilterType.LOW_SHELF)
        assertThat(
            BlackPearlReadCodec.bandFromResponse(response(typeCode = 0x04))?.type,
        ).isEqualTo(EqFilterType.HIGH_SHELF)
    }

    @Test
    fun preservesProtocolEncodableGainOutsideNormalListeningRange() {
        val raw = (-11.9 * 256.0).toInt()
        val band = BlackPearlReadCodec.bandFromResponse(response(gainRaw256 = raw))

        assertThat(band).isNotNull()
        assertThat(band!!.gainRaw256).isEqualTo(raw)
        assertThat(band.gainDb).isWithin(1e-12).of(raw.toDouble() / 256.0)
    }

    @Test
    fun rejectsWrongCommandUnsupportedTypeAndInvalidNativeRanges() {
        val wrongCommand = response().apply { this[2] = 0x11 }
        val unsupportedType = response(typeCode = 0x01)
        val badFrequency = response(frequencyHz = 19)
        val badQ = response(qRaw256 = 1)

        assertThat(BlackPearlReadCodec.bandFromResponse(wrongCommand)).isNull()
        assertThat(BlackPearlReadCodec.bandFromResponse(unsupportedType)).isNull()
        assertThat(BlackPearlReadCodec.bandFromResponse(badFrequency)).isNull()
        assertThat(BlackPearlReadCodec.bandFromResponse(badQ)).isNull()
    }

    @Test
    fun rejectsTruncatedResponseAndOutOfRangeBandIndex() {
        assertThat(BlackPearlReadCodec.bandFromResponse(ByteArray(36))).isNull()
        assertThat(BlackPearlReadCodec.bandFromResponse(response(index = 10))).isNull()
    }

    private fun response(
        index: Int = 0,
        frequencyHz: Int = 1000,
        qRaw256: Int = 256,
        gainRaw256: Int = 0,
        typeCode: Int = 0x02,
        activeSlot: Int = 2,
    ): ByteArray = BlackPearlProtocol.readBandReport(0).apply {
        this[5] = index.toByte()
        putLe16(28, frequencyHz)
        putLe16(30, qRaw256)
        putLe16(32, gainRaw256)
        this[34] = typeCode.toByte()
        this[36] = activeSlot.toByte()
    }

    private fun ByteArray.putLe16(offset: Int, value: Int) {
        val raw = value and 0xffff
        this[offset] = (raw and 0xff).toByte()
        this[offset + 1] = ((raw ushr 8) and 0xff).toByte()
    }
}
