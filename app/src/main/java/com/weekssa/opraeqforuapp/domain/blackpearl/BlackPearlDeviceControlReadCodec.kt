package com.weekssa.opraeqforuapp.domain.blackpearl

/**
 * Read-only codec for Black Pearl DEVICE qualification candidates.
 *
 * These commands are intentionally separate from the already-qualified EQ protocol. Their presence
 * here does not make them production-exposed controls; each must pass EQ Library's own physical
 * qualification before normal DEVICE exposure or any write implementation is added.
 */
object BlackPearlDeviceControlReadCodec {
    const val FILTER_FAST_LL = 1
    const val FILTER_FAST_PC = 2
    const val FILTER_SLOW_LL = 3
    const val FILTER_SLOW_PC = 4
    const val FILTER_NOS = 5

    const val GAIN_MODE_LOW = 0
    const val GAIN_MODE_HIGH = 1

    const val AMP_TOPOLOGY_CLASS_H = 0
    const val AMP_TOPOLOGY_CLASS_AB = 1

    private const val REPORT_ID = 0x4B
    private const val READ = 0x80
    private const val CMD_MIC_GAIN = 0x02
    private const val CMD_VERSION = 0x0C
    private const val CMD_FILTER = 0x11
    private const val CMD_BALANCE = 0x16
    private const val CMD_GAIN_MODE = 0x19
    private const val CMD_AMP_TOPOLOGY = 0x1D

    fun firmwareVersionRequest(): ByteArray = readRequest(CMD_VERSION)
    fun filterRequest(): ByteArray = readRequest(CMD_FILTER)
    fun gainModeRequest(): ByteArray = readRequest(CMD_GAIN_MODE)
    fun ampTopologyRequest(): ByteArray = readRequest(CMD_AMP_TOPOLOGY)
    fun micGainRequest(): ByteArray = readRequest(CMD_MIC_GAIN, p1 = 0x02, p2 = 0x02)
    fun balanceLeftRequest(): ByteArray = readRequest(CMD_BALANCE, p1 = 0x04, p2 = 0x01)
    fun balanceRightRequest(): ByteArray = readRequest(CMD_BALANCE, p1 = 0x04, p2 = 0x00)

    /**
     * The corroborated firmware response stores a NUL-terminated ASCII version string beginning at
     * byte 4. Qualification is deliberately conservative: malformed/non-printable payloads are not
     * guessed or partially displayed as a version.
     */
    fun firmwareVersionFromResponse(report: ByteArray): String? {
        if (!validHeader(report, CMD_VERSION) || report.size <= 4) return null
        val payloadEnd = (4 until report.size).firstOrNull { index -> report[index] == 0.toByte() }
            ?: report.size
        if (payloadEnd <= 4) return null
        val payload = report.copyOfRange(4, payloadEnd)
        if (payload.any { byte -> byte.u8() !in 0x20..0x7E }) return null
        return payload.toString(Charsets.US_ASCII).trim().takeIf(String::isNotEmpty)
    }

    fun filterFromResponse(report: ByteArray): Int? =
        byte4Value(report, CMD_FILTER)?.takeIf { it in FILTER_FAST_LL..FILTER_NOS }

    fun gainModeFromResponse(report: ByteArray): Int? =
        byte4Value(report, CMD_GAIN_MODE)?.takeIf { it in GAIN_MODE_LOW..GAIN_MODE_HIGH }

    fun ampTopologyFromResponse(report: ByteArray): Int? =
        byte4Value(report, CMD_AMP_TOPOLOGY)?.takeIf { it in AMP_TOPOLOGY_CLASS_H..AMP_TOPOLOGY_CLASS_AB }

    fun micGainDbFromResponse(report: ByteArray): Int? {
        if (!validHeader(report, CMD_MIC_GAIN) || report.size <= 5) return null
        return report[5].toInt().takeIf { it in -15..15 }
    }

    fun leftBalanceDbFromResponse(report: ByteArray): Int? =
        balanceMagnitude(report)?.let { raw ->
            val value = if (raw == 0) 0 else raw - 256
            value.takeIf { it in -15..0 }
        }

    fun rightBalanceDbFromResponse(report: ByteArray): Int? =
        balanceMagnitude(report)?.let { raw ->
            val value = if (raw == 0) 0 else 256 - raw
            value.takeIf { it in 0..15 }
        }

    private fun readRequest(command: Int, p1: Int = 0, p2: Int = 0, p3: Int = 0): ByteArray =
        ByteArray(BlackPearlProtocol.REPORT_SIZE).apply {
            this[0] = REPORT_ID.toByte()
            this[1] = READ.toByte()
            this[2] = command.toByte()
            this[3] = p1.toByte()
            this[4] = p2.toByte()
            this[5] = p3.toByte()
        }

    private fun byte4Value(report: ByteArray, command: Int): Int? {
        if (!validHeader(report, command) || report.size <= 4) return null
        return report[4].u8()
    }

    private fun balanceMagnitude(report: ByteArray): Int? {
        if (!validHeader(report, CMD_BALANCE) || report.size <= 6) return null
        return report[6].u8()
    }

    private fun validHeader(report: ByteArray, command: Int): Boolean =
        report.size >= 3 &&
            report[0].u8() == REPORT_ID &&
            report[1].u8() == READ &&
            report[2].u8() == command

    private fun Byte.u8(): Int = toInt() and 0xFF
}

data class BlackPearlDeviceQualificationSnapshot(
    val sessionGeneration: Long,
    val firmwareVersion: String,
    val filterCode: Int,
    val gainModeCode: Int,
    val ampTopologyCode: Int,
    val micGainDb: Int,
    val leftBalanceDb: Int,
    val rightBalanceDb: Int,
    val playbackGainRaw: Int,
) {
    init {
        require(sessionGeneration > 0)
        require(firmwareVersion.isNotBlank())
        require(firmwareVersion.all { character -> character.code in 0x20..0x7E })
        require(filterCode in 1..5)
        require(gainModeCode in 0..1)
        require(ampTopologyCode in 0..1)
        require(micGainDb in -15..15)
        require(leftBalanceDb in -15..0)
        require(rightBalanceDb in 0..15)
        require(playbackGainRaw in BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW..BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW)
    }

    val playbackGainDb: Double
        get() = BlackPearlProtocol.rawDeltaToGainDb(playbackGainRaw)

    /** Null means the two raw balance sides are inconsistent with the observed one-sided model. */
    val signedBalanceDb: Int?
        get() = when {
            leftBalanceDb == 0 -> rightBalanceDb
            rightBalanceDb == 0 -> leftBalanceDb
            else -> null
        }
}
