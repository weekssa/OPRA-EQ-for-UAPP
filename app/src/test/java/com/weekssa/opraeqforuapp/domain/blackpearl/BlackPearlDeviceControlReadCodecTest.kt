package com.weekssa.opraeqforuapp.domain.blackpearl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BlackPearlDeviceControlReadCodecTest {
    @Test
    fun candidateReadRequestsUseObservedReadHeadersAndParameters() {
        assertReadRequest(BlackPearlDeviceControlReadCodec.firmwareVersionRequest(), 0x0C, 0x00, 0x00, 0x00)
        assertReadRequest(BlackPearlDeviceControlReadCodec.filterRequest(), 0x11, 0x00, 0x00, 0x00)
        assertReadRequest(BlackPearlDeviceControlReadCodec.gainModeRequest(), 0x19, 0x00, 0x00, 0x00)
        assertReadRequest(BlackPearlDeviceControlReadCodec.ampTopologyRequest(), 0x1D, 0x00, 0x00, 0x00)
        assertReadRequest(BlackPearlDeviceControlReadCodec.micGainRequest(), 0x02, 0x02, 0x02, 0x00)
        assertReadRequest(BlackPearlDeviceControlReadCodec.balanceLeftRequest(), 0x16, 0x04, 0x01, 0x00)
        assertReadRequest(BlackPearlDeviceControlReadCodec.balanceRightRequest(), 0x16, 0x04, 0x00, 0x00)
    }

    @Test
    fun candidateWritePacketsMatchCorroboratedControlSemantics() {
        assertWriteRequest(
            BlackPearlDeviceControlReadCodec.filterWriteReport(BlackPearlDeviceControlReadCodec.FILTER_FAST_PC),
            command = 0x11,
            p1 = 0x01,
            p2 = 0x02,
        )
        assertWriteRequest(
            BlackPearlDeviceControlReadCodec.gainModeWriteReport(BlackPearlDeviceControlReadCodec.GAIN_MODE_HIGH),
            command = 0x19,
            p1 = 0x01,
            p2 = 0x01,
        )
        assertWriteRequest(
            BlackPearlDeviceControlReadCodec.ampTopologyWriteReport(BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_AB),
            command = 0x1D,
            p1 = 0x01,
            p2 = 0x01,
        )
        assertWriteRequest(
            BlackPearlDeviceControlReadCodec.micGainWriteReport(-7),
            command = 0x02,
            p1 = 0x02,
            p2 = 0x80,
            p3 = 0xF9,
        )
    }

    @Test
    fun balanceWriteAlwaysOwnsBothSidesAndClearsOppositeAttenuation() {
        val left = BlackPearlDeviceControlReadCodec.balanceWriteReports(-5)
        assertEquals(2, left.size)
        assertWriteRequest(left[0], command = 0x16, p1 = 0x04, p2 = 0x01, p3 = 0x00, p4 = 251)
        assertWriteRequest(left[1], command = 0x16, p1 = 0x04, p2 = 0x00, p3 = 0x00, p4 = 0)

        val right = BlackPearlDeviceControlReadCodec.balanceWriteReports(7)
        assertWriteRequest(right[0], command = 0x16, p1 = 0x04, p2 = 0x01, p3 = 0x00, p4 = 0)
        assertWriteRequest(right[1], command = 0x16, p1 = 0x04, p2 = 0x00, p3 = 0x00, p4 = 249)

        val centered = BlackPearlDeviceControlReadCodec.balanceWriteReports(0)
        assertWriteRequest(centered[0], command = 0x16, p1 = 0x04, p2 = 0x01, p3 = 0x00, p4 = 0)
        assertWriteRequest(centered[1], command = 0x16, p1 = 0x04, p2 = 0x00, p3 = 0x00, p4 = 0)
    }

    @Test
    fun candidateWriteBuildersRejectUnknownOrOutOfRangeValues() {
        assertThrows(IllegalArgumentException::class.java) {
            BlackPearlDeviceControlReadCodec.filterWriteReport(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BlackPearlDeviceControlReadCodec.gainModeWriteReport(2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BlackPearlDeviceControlReadCodec.ampTopologyWriteReport(2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BlackPearlDeviceControlReadCodec.micGainWriteReport(16)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BlackPearlDeviceControlReadCodec.balanceWriteReports(-16)
        }
    }

    @Test
    fun decodesObservedFirmwareAsciiPayload() {
        val response = response(0x0C).apply {
            "BP-1.2.3".encodeToByteArray().copyInto(this, destinationOffset = 4)
        }

        assertEquals("BP-1.2.3", BlackPearlDeviceControlReadCodec.firmwareVersionFromResponse(response))
    }

    @Test
    fun firmwareDecodeRejectsWrongHeaderEmptyAndNonPrintablePayload() {
        assertNull(BlackPearlDeviceControlReadCodec.firmwareVersionFromResponse(response(0x0C)))
        assertNull(
            BlackPearlDeviceControlReadCodec.firmwareVersionFromResponse(
                response(0x0C).apply {
                    this[4] = '1'.code.toByte()
                    this[5] = 0x01
                },
            ),
        )
        assertNull(
            BlackPearlDeviceControlReadCodec.firmwareVersionFromResponse(
                response(0x0C).apply {
                    "1.0".encodeToByteArray().copyInto(this, destinationOffset = 4)
                    this[1] = 0x01
                },
            ),
        )
    }

    @Test
    fun decodesObservedDiscreteControlBytes() {
        assertEquals(2, BlackPearlDeviceControlReadCodec.filterFromResponse(response(0x11, byte4 = 2)))
        assertEquals(1, BlackPearlDeviceControlReadCodec.gainModeFromResponse(response(0x19, byte4 = 1)))
        assertEquals(1, BlackPearlDeviceControlReadCodec.ampTopologyFromResponse(response(0x1D, byte4 = 1)))
    }

    @Test
    fun decodesSignedMicGainAndSeparateBalanceSides() {
        val mic = response(0x02).apply { this[5] = (-7).toByte() }
        val left = response(0x16).apply { this[6] = 251.toByte() }
        val right = response(0x16).apply { this[6] = 249.toByte() }

        assertEquals(-7, BlackPearlDeviceControlReadCodec.micGainDbFromResponse(mic))
        assertEquals(-5, BlackPearlDeviceControlReadCodec.leftBalanceDbFromResponse(left))
        assertEquals(7, BlackPearlDeviceControlReadCodec.rightBalanceDbFromResponse(right))
    }

    @Test
    fun rejectsWrongHeadersAndOutOfRangeCandidateValues() {
        assertNull(BlackPearlDeviceControlReadCodec.filterFromResponse(response(0x11, byte4 = 0)))
        assertNull(BlackPearlDeviceControlReadCodec.gainModeFromResponse(response(0x19, byte4 = 3)))
        assertNull(BlackPearlDeviceControlReadCodec.ampTopologyFromResponse(response(0x1D, byte4 = 2)))
        assertNull(
            BlackPearlDeviceControlReadCodec.micGainDbFromResponse(
                response(0x02).apply { this[5] = 16 },
            ),
        )
        assertNull(
            BlackPearlDeviceControlReadCodec.leftBalanceDbFromResponse(
                response(0x16).apply { this[6] = 200.toByte() },
            ),
        )
        assertNull(
            BlackPearlDeviceControlReadCodec.filterFromResponse(
                response(0x11, byte4 = 1).apply { this[1] = 0x01 },
            ),
        )
    }

    @Test
    fun qualificationSnapshotRetainsFirmwareAndBothBalanceSidesWithoutInventingCombinedState() {
        val normal = BlackPearlDeviceQualificationSnapshot(
            sessionGeneration = 3,
            firmwareVersion = "BP-1.2.3",
            filterCode = 2,
            gainModeCode = 1,
            ampTopologyCode = 0,
            micGainDb = 4,
            leftBalanceDb = 0,
            rightBalanceDb = 6,
            playbackGainRaw = -1024,
        )
        assertEquals("BP-1.2.3", normal.firmwareVersion)
        assertEquals(6, normal.signedBalanceDb)
        assertEquals(-4.0, normal.playbackGainDb, 0.0)

        val inconsistent = normal.copy(leftBalanceDb = -4, rightBalanceDb = 6)
        assertNull(inconsistent.signedBalanceDb)
    }

    private fun assertReadRequest(request: ByteArray, command: Int, p1: Int, p2: Int, p3: Int) {
        assertEquals(64, request.size)
        assertEquals(0x4B, request[0].toInt() and 0xFF)
        assertEquals(0x80, request[1].toInt() and 0xFF)
        assertEquals(command, request[2].toInt() and 0xFF)
        assertEquals(p1, request[3].toInt() and 0xFF)
        assertEquals(p2, request[4].toInt() and 0xFF)
        assertEquals(p3, request[5].toInt() and 0xFF)
    }

    private fun assertWriteRequest(
        request: ByteArray,
        command: Int,
        p1: Int,
        p2: Int,
        p3: Int = 0,
        p4: Int = 0,
    ) {
        assertEquals(64, request.size)
        assertEquals(0x4B, request[0].toInt() and 0xFF)
        assertEquals(0x01, request[1].toInt() and 0xFF)
        assertEquals(command, request[2].toInt() and 0xFF)
        assertEquals(p1, request[3].toInt() and 0xFF)
        assertEquals(p2, request[4].toInt() and 0xFF)
        assertEquals(p3, request[5].toInt() and 0xFF)
        assertEquals(p4, request[6].toInt() and 0xFF)
        assertEquals(0, request.drop(7).sumOf { it.toInt() })
    }

    private fun response(command: Int, byte4: Int = 0): ByteArray = ByteArray(64).apply {
        this[0] = 0x4B
        this[1] = 0x80.toByte()
        this[2] = command.toByte()
        this[4] = byte4.toByte()
    }
}
