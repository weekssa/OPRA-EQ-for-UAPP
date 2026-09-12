package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot

interface BlackPearlDeviceControlReadSource {
    val sessionGeneration: Long
    fun isSessionCurrent(sessionGeneration: Long): Boolean
    suspend fun readFirmwareVersion(): String?
    suspend fun readFilterCode(): Int?
    suspend fun readGainModeCode(): Int?
    suspend fun readAmpTopologyCode(): Int?
    suspend fun readMicGainDb(): Int?
    suspend fun readLeftBalanceDb(): Int?
    suspend fun readRightBalanceDb(): Int?
    suspend fun readPlaybackGainRaw(): Int?
}

sealed interface BlackPearlQualificationReadResult {
    data class Success(
        val snapshot: BlackPearlDeviceQualificationSnapshot,
    ) : BlackPearlQualificationReadResult

    data object NotConnected : BlackPearlQualificationReadResult

    data object SessionChanged : BlackPearlQualificationReadResult

    data class ReadFailed(
        val field: String,
    ) : BlackPearlQualificationReadResult
}

/**
 * Device-control boundary kept separate from HardwareEqRepository.
 *
 * Increment 12 supports reads only. No candidate DEVICE write is implemented here; qualification
 * must complete on physical hardware before any write path or production capability exposure exists.
 */
class DacControlRepository(
    private val blackPearlSource: BlackPearlDeviceControlReadSource,
) {
    suspend fun readBlackPearlQualificationSnapshot(): BlackPearlQualificationReadResult {
        val generation = blackPearlSource.sessionGeneration
        if (generation <= 0L || !blackPearlSource.isSessionCurrent(generation)) {
            return BlackPearlQualificationReadResult.NotConnected
        }

        suspend fun <T> read(field: String, block: suspend () -> T?): T? {
            if (!blackPearlSource.isSessionCurrent(generation)) return null
            return block()?.takeIf { blackPearlSource.isSessionCurrent(generation) }
        }

        val firmwareVersion = read("firmware version", blackPearlSource::readFirmwareVersion)
            ?: return failedOrChanged(generation, "firmware version")
        val filter = read("DAC filter", blackPearlSource::readFilterCode)
            ?: return failedOrChanged(generation, "DAC filter")
        val gainMode = read("gain mode", blackPearlSource::readGainModeCode)
            ?: return failedOrChanged(generation, "gain mode")
        val topology = read("amp topology", blackPearlSource::readAmpTopologyCode)
            ?: return failedOrChanged(generation, "amp topology")
        val micGain = read("mic gain", blackPearlSource::readMicGainDb)
            ?: return failedOrChanged(generation, "mic gain")
        val leftBalance = read("left balance", blackPearlSource::readLeftBalanceDb)
            ?: return failedOrChanged(generation, "left balance")
        val rightBalance = read("right balance", blackPearlSource::readRightBalanceDb)
            ?: return failedOrChanged(generation, "right balance")
        val playbackGain = read("playback gain", blackPearlSource::readPlaybackGainRaw)
            ?: return failedOrChanged(generation, "playback gain")

        if (!blackPearlSource.isSessionCurrent(generation)) {
            return BlackPearlQualificationReadResult.SessionChanged
        }
        return BlackPearlQualificationReadResult.Success(
            BlackPearlDeviceQualificationSnapshot(
                sessionGeneration = generation,
                firmwareVersion = firmwareVersion,
                filterCode = filter,
                gainModeCode = gainMode,
                ampTopologyCode = topology,
                micGainDb = micGain,
                leftBalanceDb = leftBalance,
                rightBalanceDb = rightBalance,
                playbackGainRaw = playbackGain,
            ),
        )
    }

    private fun failedOrChanged(
        generation: Long,
        field: String,
    ): BlackPearlQualificationReadResult =
        if (blackPearlSource.isSessionCurrent(generation)) {
            BlackPearlQualificationReadResult.ReadFailed(field)
        } else {
            BlackPearlQualificationReadResult.SessionChanged
        }
}

class SessionBlackPearlDeviceControlReadSource(
    private val sessions: DacSessionRepository,
) : BlackPearlDeviceControlReadSource {
    override val sessionGeneration: Long
        get() = sessions.blackPearlTransport.sessionGeneration

    override fun isSessionCurrent(sessionGeneration: Long): Boolean =
        sessions.blackPearlConnectionState.value is BlackPearlConnectionState.Connected &&
            sessions.isBlackPearlSessionCurrent(sessionGeneration)

    override suspend fun readFirmwareVersion(): String? =
        sessions.blackPearlTransport.readDeviceFirmwareVersion()

    override suspend fun readFilterCode(): Int? = sessions.blackPearlTransport.readDeviceFilterCode()
    override suspend fun readGainModeCode(): Int? = sessions.blackPearlTransport.readDeviceGainModeCode()
    override suspend fun readAmpTopologyCode(): Int? = sessions.blackPearlTransport.readDeviceAmpTopologyCode()
    override suspend fun readMicGainDb(): Int? = sessions.blackPearlTransport.readDeviceMicGainDb()
    override suspend fun readLeftBalanceDb(): Int? = sessions.blackPearlTransport.readDeviceLeftBalanceDb()
    override suspend fun readRightBalanceDb(): Int? = sessions.blackPearlTransport.readDeviceRightBalanceDb()
    override suspend fun readPlaybackGainRaw(): Int? = sessions.blackPearlTransport.readGlobalGainRaw()
}
