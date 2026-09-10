package com.weekssa.opraeqforuapp.domain.blackpearl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlackPearlDeviceControlReadCodecTest {
    @Test
    fun candidateReadRequestsUseObservedReadHeadersAndParameters() {
        assertRequest(BlackPearlDeviceControlReadCodec.filterRequest(), 0x11, 0x00, 0x00, 0x00)
        assertRequest(BlackPearlDeviceControlReadCodec.gainModeRequest(), 0x19, 0x00, 0x00, 0x00)
        assertRequest(BlackPearlDeviceControlReadCodec.ampTopologyRequest(), 0x1D, 0x00, 0x00, 0x00)
        assertRequest(BlackPearlDeviceControlReadCodec.micGainRequest(), 0x02, 0x02, 0x02, 0x00)
        assertRequest(BlackPearlDeviceControlReadCodec.balanceLeftRequest(), 0x16, 0x04, 0x01, 0x00)
        assertRequest(BlackPearlDeviceControlReadCodec.balanceRightRequest(), 0x16, 0x04, 0x00, 0x00)
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
    fun qualificationSnapshotRetainsBothBalanceSidesWithoutInventingCombinedState() {
        val normal = BlackPearlDeviceQualificationSnapshot(
            sessionGeneration = 3,
            filterCode = 2,
            gainModeCode = 1,
            ampTopologyCode = 0,
            micGainDb = 4,
            leftBalanceDb = 0,
            rightBalanceDb = 6,
            playbackGainRaw = -1024,
        )
        assertEquals(6, normal.signedBalanceDb)
        assertEquals(-4.0, normal.playbackGainDb, 0.0)

        val inconsistent = normal.copy(leftBalanceDb = -4, rightBalanceDb = 6)
        assertNull(inconsistent.signedBalanceDb)
    }

    private fun assertRequest(request: ByteArray, command: Int, p1: Int, p2: Int, p3: Int) {
        assertEquals(64, request.size)
        assertEquals(0x4B, request[0].toInt() and 0xFF)
        assertEquals(0x80, request[1].toInt() and 0xFF)
        assertEquals(command, request[2].toInt() and 0xFF)
        assertEquals(p1, request[3].toInt() and 0xFF)
        assertEquals(p2, request[4].toInt() and 0xFF)
        assertEquals(p3, request[5].toInt() and 0xFF)
    }

    private fun response(command: Int, byte4: Int = 0): ByteArray = ByteArray(64).apply {
        this[0] = 0x4B
        this[1] = 0x80.toByte()
        this[2] = command.toByte()
        this[4] = byte4.toByte()
    }
}
