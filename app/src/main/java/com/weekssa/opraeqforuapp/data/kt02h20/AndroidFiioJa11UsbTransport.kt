package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11TransportEvent
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Timing
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Transport
import com.weekssa.opraeqforuapp.domain.kt02h20.toJa11TraceHex
import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

class AndroidFiioJa11UsbTransport(
    context: Context,
) : FiioJa11Transport, Closeable {
    private val hid = AndroidKt02h20HidSession(
        context = context,
        vendorId = FiioJa11Protocol.VENDOR_ID,
        productIds = FiioJa11Protocol.SUPPORTED_PRODUCT_IDS,
        deviceLabel = "FiiO JA11",
        permissionSuffix = "FIIO_JA11",
        requireUniqueCandidate = true,
    )

    private var savePreviousGeneration = 0L
    private var savePreviousDetachGeneration = 0L
    private var savePreviousSerial: String? = null
    private var awaitingLateSaveDetach = false

    val state: StateFlow<Kt02h20ConnectionState> = hid.state
    val present: StateFlow<Boolean> = hid.present
    val connectedProductId: Int?
        get() = hid.connectedProductId
    val connectedDeviceSerial: String?
        get() = hid.connectedDeviceSerial
    val supportedCandidateCount: Int
        get() = hid.supportedCandidateCount
    override val deviceFingerprintKey: String?
        get() = hid.deviceFingerprintKey
    override val usbProductId: Int?
        get() = hid.connectedProductId
    override val sessionGeneration: Long
        get() = hid.sessionGeneration
    override val detachGeneration: Long
        get() = hid.detachGeneration
    override val permissionRequestCount: Long
        get() = hid.permissionRequestCount

    private val traceLock = Any()
    private var traceStartMillis: Long? = null
    private val traceEvents = mutableListOf<FiioJa11TransportEvent>()

    override fun beginTrace(operationId: String) {
        synchronized(traceLock) {
            traceStartMillis = System.currentTimeMillis()
            traceEvents.clear()
        }
    }

    override fun endTrace(): List<FiioJa11TransportEvent> =
        synchronized(traceLock) {
            val result = traceEvents.toList()
            traceStartMillis = null
            traceEvents.clear()
            result
        }

    fun connect() = hid.connect()

    suspend fun readOutputVolume(): Int? = exchangeOneByte(
        request = FiioJa11Protocol.readOutputVolumeReport(),
        decoder = FiioJa11Protocol::outputVolumeFromResponse,
    )

    suspend fun readSampleRateLabel(): String? = exchangeOneByte(
        request = FiioJa11Protocol.readSampleRateReport(),
        decoder = FiioJa11Protocol::sampleRateLabelFromResponse,
    )

    override suspend fun readFirmwareVersion(): String? {
        return exchangeOnStableSession(
            request = FiioJa11Protocol.readFirmwareVersionReport(),
            minResponseBytes = 8,
            acceptResponse = { candidate -> FiioJa11Protocol.firmwareVersionFromResponse(candidate) != null },
            decoder = FiioJa11Protocol::firmwareVersionFromResponse,
        )
    }

    suspend fun readHeadsetControlEnabled(): Boolean? = exchangeOneByte(
        request = FiioJa11Protocol.readHeadsetControlReport(),
        decoder = FiioJa11Protocol::headsetControlFromResponse,
    )

    override suspend fun readEqProgram(): FiioJa11Protocol.EqProgram? = exchangeOneByte(
        request = FiioJa11Protocol.readEqProgramReport(),
        decoder = FiioJa11Protocol::eqProgramFromResponse,
    )

    suspend fun readUacMode(): FiioJa11Protocol.UacMode? = exchangeOneByte(
        request = FiioJa11Protocol.readUacModeReport(),
        decoder = FiioJa11Protocol::uacModeFromResponse,
    )

    suspend fun writeOutputVolume(level: Int): Boolean =
        sendReport(FiioJa11Protocol.writeOutputVolumeReport(level))

    suspend fun writeHeadsetControlEnabled(enabled: Boolean): Boolean =
        sendReport(FiioJa11Protocol.writeHeadsetControlReport(enabled))

    suspend fun writeEqProgram(program: FiioJa11Protocol.EqProgram): Boolean =
        sendReport(FiioJa11Protocol.writeEqProgramReport(program))

    suspend fun writeUacMode(mode: FiioJa11Protocol.UacMode): Boolean =
        sendReport(FiioJa11Protocol.writeUacModeReport(mode))

    override suspend fun readBand(index: Int): FiioJa11Protocol.Band? {
        return exchangeOnStableSession(
            request = FiioJa11Protocol.readBandReport(index),
            minResponseBytes = 15,
            acceptResponse = { candidate -> FiioJa11Protocol.bandFromResponse(candidate)?.first == index },
            decoder = { candidate ->
                FiioJa11Protocol.bandFromResponse(candidate)?.takeIf { it.first == index }?.second
            },
        )
    }

    override suspend fun readGlobalGainDb(): Double? {
        return exchangeOnStableSession(
            request = FiioJa11Protocol.readGlobalGainReport(),
            minResponseBytes = 8,
            acceptResponse = { candidate -> FiioJa11Protocol.globalGainFromResponse(candidate) != null },
            decoder = FiioJa11Protocol::globalGainFromResponse,
        )
    }

    override suspend fun saveToFlash(): Boolean {
        val previousGeneration = hid.sessionGeneration
        val previousDetachGeneration = hid.detachGeneration
        val previousSerial = hid.connectedDeviceSerial
        savePreviousGeneration = previousGeneration
        savePreviousDetachGeneration = previousDetachGeneration
        savePreviousSerial = previousSerial
        awaitingLateSaveDetach = false
        if (!sendReport(FiioJa11Protocol.saveToFlashReport())) return false

        // Observe USB state changes through the bounded post-Save window. A detected detach must
        // complete on a fresh unique session before final readback; a stable session is retained
        // only after that window expires without a detach.
        val completed = hid.awaitJa11SaveBoundary(
            previousGeneration = previousGeneration,
            previousDetachGeneration = previousDetachGeneration,
            previousSerial = previousSerial,
        )
        if (!completed) return false
        savePreviousGeneration = hid.sessionGeneration
        savePreviousDetachGeneration = hid.detachGeneration
        savePreviousSerial = hid.connectedDeviceSerial
        awaitingLateSaveDetach = true
        return true
    }

    override suspend fun awaitFinalReadbackReconnect(): Boolean {
        if (!awaitingLateSaveDetach) return false
        awaitingLateSaveDetach = false
        return hid.awaitLateJa11SaveReconnect(
            previousGeneration = savePreviousGeneration,
            previousDetachGeneration = savePreviousDetachGeneration,
            previousSerial = savePreviousSerial,
        )
    }

    suspend fun awaitExpectedReplacementSession(
        previousGeneration: Long,
        previousDetachGeneration: Long,
        previousSerial: String?,
        timeoutMillis: Long,
    ): Boolean = hid.awaitExpectedReplacementSession(
        previousGeneration = previousGeneration,
        previousDetachGeneration = previousDetachGeneration,
        previousSerial = previousSerial,
        timeoutMillis = timeoutMillis,
    )

    override suspend fun sendReport(report: ByteArray): Boolean {
        val generation = hid.sessionGeneration
        val detachGeneration = hid.detachGeneration
        val sent = hid.send(
            report = report,
            settleMillis = FiioJa11Timing.settleMillisForMutation(report),
        )
        val command = report.getOrNull(5)?.toInt()?.and(0xFF)
        val accepted = sent && (
            command == 0x12 || command == 0x19 || command == 0x20 ||
                hid.isCurrentSession(generation, detachGeneration)
            )
        recordTrace(
            direction = "WRITE",
            request = report,
            response = null,
            sessionGeneration = generation,
            detachGeneration = detachGeneration,
            succeeded = accepted,
        )
        return accepted
    }

    private suspend fun <T> exchangeOneByte(
        request: ByteArray,
        decoder: (ByteArray) -> T?,
    ): T? = exchangeOnStableSession(
        request = request,
        minResponseBytes = 7,
        acceptResponse = { candidate -> decoder(candidate) != null },
        decoder = decoder,
    )

    private suspend fun <T> exchangeOnStableSession(
        request: ByteArray,
        minResponseBytes: Int,
        acceptResponse: (ByteArray) -> Boolean,
        decoder: (ByteArray) -> T?,
    ): T? {
        val generation = hid.sessionGeneration
        val detachGeneration = hid.detachGeneration
        if (!hid.isCurrentSession(generation, detachGeneration)) {
            recordTrace(
                direction = "READ",
                request = request,
                response = null,
                sessionGeneration = generation,
                detachGeneration = detachGeneration,
                succeeded = false,
            )
            return null
        }
        val response = hid.exchange(
            report = request,
            minResponseBytes = minResponseBytes,
            acceptResponse = acceptResponse,
        )
        val stable = response != null && hid.isCurrentSession(generation, detachGeneration)
        recordTrace(
            direction = "READ",
            request = request,
            response = response,
            sessionGeneration = generation,
            detachGeneration = detachGeneration,
            succeeded = stable,
        )
        if (!stable) return null
        return decoder(requireNotNull(response))
    }

    private fun recordTrace(
        direction: String,
        request: ByteArray,
        response: ByteArray?,
        sessionGeneration: Long,
        detachGeneration: Long,
        succeeded: Boolean,
    ) {
        synchronized(traceLock) {
            val start = traceStartMillis ?: return
            traceEvents += FiioJa11TransportEvent(
                sequence = traceEvents.size + 1,
                elapsedMillis = (System.currentTimeMillis() - start).coerceAtLeast(0L),
                direction = direction,
                command = request.getOrNull(5)?.let { "0x%02x".format(it.toInt() and 0xFF) } ?: "unknown",
                requestHex = request.toJa11TraceHex(),
                responseHex = response?.toJa11TraceHex(),
                sessionGeneration = sessionGeneration,
                detachGeneration = detachGeneration,
                succeeded = succeeded,
            )
        }
    }

    override fun close() = hid.close()
}
