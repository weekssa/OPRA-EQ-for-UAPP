package com.weekssa.opraeqforuapp.data.dac

import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControlReadCodec
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControls
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlProtocol
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValidation
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacWriteIntent
import com.weekssa.opraeqforuapp.domain.dac.validateForWrite
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    suspend fun writeFilterCode(value: Int): Boolean
    suspend fun writeGainModeCode(value: Int): Boolean
    suspend fun writeAmpTopologyCode(value: Int): Boolean
    suspend fun writeMicGainDb(value: Int): Boolean
    suspend fun writeBalanceDb(value: Int): Boolean
    suspend fun writePlaybackGainRaw(value: Int): Boolean
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

sealed interface BlackPearlDeviceControlWriteResult {
    data class Verified(
        val controlId: DacControlId,
        val requestedValue: DacControlValue,
        val baseline: BlackPearlDeviceQualificationSnapshot,
        val snapshot: BlackPearlDeviceQualificationSnapshot,
    ) : BlackPearlDeviceControlWriteResult

    data class InvalidRequest(
        val controlId: DacControlId,
        val validation: DacControlValidation?,
    ) : BlackPearlDeviceControlWriteResult

    data class NotConnected(
        val controlId: DacControlId,
    ) : BlackPearlDeviceControlWriteResult

    data class StaleBaseline(
        val controlId: DacControlId,
        val expectedSessionGeneration: Long,
        val actualSessionGeneration: Long,
    ) : BlackPearlDeviceControlWriteResult

    data class ReadFailed(
        val controlId: DacControlId,
        val field: String,
    ) : BlackPearlDeviceControlWriteResult

    data class TransferFailed(
        val controlId: DacControlId,
    ) : BlackPearlDeviceControlWriteResult

    data class ReadbackMismatch(
        val controlId: DacControlId,
        val requestedValue: DacControlValue,
        val actualValue: DacControlValue?,
        val snapshot: BlackPearlDeviceQualificationSnapshot,
    ) : BlackPearlDeviceControlWriteResult

    data class UnrelatedStateChanged(
        val controlId: DacControlId,
        val changedFields: List<String>,
        val snapshot: BlackPearlDeviceQualificationSnapshot,
    ) : BlackPearlDeviceControlWriteResult
}

/**
 * Device-control boundary kept separate from HardwareEqRepository.
 *
 * Reads are already physically qualified. Candidate writes use the strict transaction model:
 * fresh complete baseline -> one targeted write -> complete readback -> requested-value verification
 * -> unrelated-state verification. No generic flash/save command is used here; persistence remains a
 * separate hardware qualification question.
 */
class DacControlRepository(
    private val blackPearlSource: BlackPearlDeviceControlReadSource,
) {
    private val operationMutex = Mutex()

    suspend fun readBlackPearlQualificationSnapshot(): BlackPearlQualificationReadResult =
        operationMutex.withLock { readBlackPearlQualificationSnapshotUnlocked() }

    suspend fun writeBlackPearlControl(intent: DacWriteIntent): BlackPearlDeviceControlWriteResult =
        operationMutex.withLock {
            val descriptor = BlackPearlDeviceControls.descriptor(intent.controlId)
                ?: return@withLock BlackPearlDeviceControlWriteResult.InvalidRequest(
                    controlId = intent.controlId,
                    validation = null,
                )
            val validation = descriptor.validateForWrite(intent.requestedValue)
            if (validation !is DacControlValidation.Valid &&
                validation !is DacControlValidation.CautionOutsideNormalRange
            ) {
                return@withLock BlackPearlDeviceControlWriteResult.InvalidRequest(intent.controlId, validation)
            }

            val generation = blackPearlSource.sessionGeneration
            if (generation <= 0L || !blackPearlSource.isSessionCurrent(generation)) {
                return@withLock BlackPearlDeviceControlWriteResult.NotConnected(intent.controlId)
            }
            if (generation != intent.expectedSessionGeneration) {
                return@withLock BlackPearlDeviceControlWriteResult.StaleBaseline(
                    controlId = intent.controlId,
                    expectedSessionGeneration = intent.expectedSessionGeneration,
                    actualSessionGeneration = generation,
                )
            }

            val baseline = when (val read = readBlackPearlQualificationSnapshotUnlocked()) {
                is BlackPearlQualificationReadResult.Success -> read.snapshot
                is BlackPearlQualificationReadResult.NotConnected ->
                    return@withLock BlackPearlDeviceControlWriteResult.NotConnected(intent.controlId)
                is BlackPearlQualificationReadResult.SessionChanged ->
                    return@withLock BlackPearlDeviceControlWriteResult.StaleBaseline(
                        intent.controlId,
                        intent.expectedSessionGeneration,
                        blackPearlSource.sessionGeneration,
                    )
                is BlackPearlQualificationReadResult.ReadFailed ->
                    return@withLock BlackPearlDeviceControlWriteResult.ReadFailed(intent.controlId, read.field)
            }

            if (baseline.sessionGeneration != intent.expectedSessionGeneration) {
                return@withLock BlackPearlDeviceControlWriteResult.StaleBaseline(
                    intent.controlId,
                    intent.expectedSessionGeneration,
                    baseline.sessionGeneration,
                )
            }

            val baselineValue = BlackPearlDeviceControls.valueFromSnapshot(intent.controlId, baseline)
            if (baselineValue == intent.requestedValue) {
                return@withLock BlackPearlDeviceControlWriteResult.Verified(
                    intent.controlId,
                    intent.requestedValue,
                    baseline,
                    baseline,
                )
            }

            if (!writeTarget(intent.controlId, intent.requestedValue)) {
                return@withLock if (blackPearlSource.isSessionCurrent(generation)) {
                    BlackPearlDeviceControlWriteResult.TransferFailed(intent.controlId)
                } else {
                    BlackPearlDeviceControlWriteResult.StaleBaseline(
                        intent.controlId,
                        intent.expectedSessionGeneration,
                        blackPearlSource.sessionGeneration,
                    )
                }
            }
            if (!blackPearlSource.isSessionCurrent(generation)) {
                return@withLock BlackPearlDeviceControlWriteResult.StaleBaseline(
                    intent.controlId,
                    intent.expectedSessionGeneration,
                    blackPearlSource.sessionGeneration,
                )
            }

            val readback = when (val read = readBlackPearlQualificationSnapshotUnlocked()) {
                is BlackPearlQualificationReadResult.Success -> read.snapshot
                is BlackPearlQualificationReadResult.NotConnected ->
                    return@withLock BlackPearlDeviceControlWriteResult.NotConnected(intent.controlId)
                is BlackPearlQualificationReadResult.SessionChanged ->
                    return@withLock BlackPearlDeviceControlWriteResult.StaleBaseline(
                        intent.controlId,
                        intent.expectedSessionGeneration,
                        blackPearlSource.sessionGeneration,
                    )
                is BlackPearlQualificationReadResult.ReadFailed ->
                    return@withLock BlackPearlDeviceControlWriteResult.ReadFailed(intent.controlId, read.field)
            }

            val actualValue = BlackPearlDeviceControls.valueFromSnapshot(intent.controlId, readback)
            if (actualValue != intent.requestedValue) {
                return@withLock BlackPearlDeviceControlWriteResult.ReadbackMismatch(
                    controlId = intent.controlId,
                    requestedValue = intent.requestedValue,
                    actualValue = actualValue,
                    snapshot = readback,
                )
            }

            val unrelatedChanges = unrelatedChangedFields(intent.controlId, baseline, readback)
            if (unrelatedChanges.isNotEmpty()) {
                return@withLock BlackPearlDeviceControlWriteResult.UnrelatedStateChanged(
                    controlId = intent.controlId,
                    changedFields = unrelatedChanges,
                    snapshot = readback,
                )
            }

            BlackPearlDeviceControlWriteResult.Verified(
                controlId = intent.controlId,
                requestedValue = intent.requestedValue,
                baseline = baseline,
                snapshot = readback,
            )
        }

    private suspend fun readBlackPearlQualificationSnapshotUnlocked(): BlackPearlQualificationReadResult {
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

    private suspend fun writeTarget(controlId: DacControlId, value: DacControlValue): Boolean = when (controlId) {
        BlackPearlDeviceControls.DAC_FILTER -> {
            val valueId = (value as? DacControlValue.Discrete)?.valueId ?: return false
            blackPearlSource.writeFilterCode(BlackPearlDeviceControls.filterCode(valueId) ?: return false)
        }
        BlackPearlDeviceControls.GAIN_MODE -> {
            val valueId = (value as? DacControlValue.Discrete)?.valueId ?: return false
            blackPearlSource.writeGainModeCode(BlackPearlDeviceControls.gainModeCode(valueId) ?: return false)
        }
        BlackPearlDeviceControls.AMP_TOPOLOGY -> {
            val valueId = (value as? DacControlValue.Discrete)?.valueId ?: return false
            blackPearlSource.writeAmpTopologyCode(BlackPearlDeviceControls.ampTopologyCode(valueId) ?: return false)
        }
        BlackPearlDeviceControls.BALANCE_DB -> {
            val requested = (value as? DacControlValue.Numeric)?.value ?: return false
            blackPearlSource.writeBalanceDb(requested.toInt())
        }
        BlackPearlDeviceControls.MIC_GAIN_DB -> {
            val requested = (value as? DacControlValue.Numeric)?.value ?: return false
            blackPearlSource.writeMicGainDb(requested.toInt())
        }
        BlackPearlDeviceControls.PLAYBACK_GAIN_DB -> {
            val requested = (value as? DacControlValue.Numeric)?.value ?: return false
            blackPearlSource.writePlaybackGainRaw(BlackPearlDeviceControls.playbackGainRaw(requested) ?: return false)
        }
        else -> false
    }

    private fun unrelatedChangedFields(
        controlId: DacControlId,
        before: BlackPearlDeviceQualificationSnapshot,
        after: BlackPearlDeviceQualificationSnapshot,
    ): List<String> = buildList {
        if (before.firmwareVersion != after.firmwareVersion) add("firmware version")
        if (controlId != BlackPearlDeviceControls.DAC_FILTER && before.filterCode != after.filterCode) add("DAC filter")
        if (controlId != BlackPearlDeviceControls.GAIN_MODE && before.gainModeCode != after.gainModeCode) add("gain mode")
        if (controlId != BlackPearlDeviceControls.AMP_TOPOLOGY && before.ampTopologyCode != after.ampTopologyCode) add("amp topology")
        if (controlId != BlackPearlDeviceControls.MIC_GAIN_DB && before.micGainDb != after.micGainDb) add("mic gain")
        if (controlId != BlackPearlDeviceControls.BALANCE_DB &&
            (before.leftBalanceDb != after.leftBalanceDb || before.rightBalanceDb != after.rightBalanceDb)
        ) add("balance")
        if (controlId != BlackPearlDeviceControls.PLAYBACK_GAIN_DB && before.playbackGainRaw != after.playbackGainRaw) {
            add("playback gain")
        }
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

    override suspend fun writeFilterCode(value: Int): Boolean =
        sessions.blackPearlTransport.sendReport(BlackPearlDeviceControlReadCodec.filterWriteReport(value))

    override suspend fun writeGainModeCode(value: Int): Boolean =
        sessions.blackPearlTransport.sendReport(BlackPearlDeviceControlReadCodec.gainModeWriteReport(value))

    override suspend fun writeAmpTopologyCode(value: Int): Boolean =
        sessions.blackPearlTransport.sendReport(BlackPearlDeviceControlReadCodec.ampTopologyWriteReport(value))

    override suspend fun writeMicGainDb(value: Int): Boolean =
        sessions.blackPearlTransport.sendReport(BlackPearlDeviceControlReadCodec.micGainWriteReport(value)) &&
            sessions.blackPearlTransport.sendReport(BlackPearlProtocol.latchReport())

    override suspend fun writeBalanceDb(value: Int): Boolean {
        val reports = BlackPearlDeviceControlReadCodec.balanceWriteReports(value)
        if (!sessions.blackPearlTransport.sendReport(reports[0])) return false
        delay(BALANCE_SIDE_SETTLE_MILLIS)
        if (!sessions.blackPearlTransport.sendReport(reports[1])) return false
        return sessions.blackPearlTransport.sendReport(BlackPearlProtocol.latchReport())
    }

    override suspend fun writePlaybackGainRaw(value: Int): Boolean =
        sessions.blackPearlTransport.sendReport(BlackPearlProtocol.writeGlobalGainReport(value)) &&
            sessions.blackPearlTransport.sendReport(BlackPearlProtocol.latchReport())

    private companion object {
        const val BALANCE_SIDE_SETTLE_MILLIS = 50L
    }
}
