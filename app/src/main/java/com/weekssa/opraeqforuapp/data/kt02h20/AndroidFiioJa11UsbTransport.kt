package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Timing
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Transport
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
    )

    val state: StateFlow<Kt02h20ConnectionState> = hid.state
    val present: StateFlow<Boolean> = hid.present
    val sessionGeneration: Long
        get() = hid.sessionGeneration
    val connectedProductId: Int?
        get() = hid.connectedProductId

    fun connect() = hid.connect()

    suspend fun readOutputVolume(): Int? = exchangeOneByte(
        request = FiioJa11Protocol.readOutputVolumeReport(),
        decoder = FiioJa11Protocol::outputVolumeFromResponse,
    )

    suspend fun readSampleRateLabel(): String? = exchangeOneByte(
        request = FiioJa11Protocol.readSampleRateReport(),
        decoder = FiioJa11Protocol::sampleRateLabelFromResponse,
    )

    suspend fun readFirmwareVersion(): String? {
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
        if (!sendReport(FiioJa11Protocol.saveToFlashReport())) return false

        // FiiO documents Save as a chip power-cycle/restart boundary. Final readback must use a
        // fresh session when Android observed detach/attach, while unchanged healthy sessions
        // remain accepted for firmware variants that persist without re-enumerating.
        return hid.awaitOptionalReconnectAfterMutation(
            previousGeneration = previousGeneration,
            previousDetachGeneration = previousDetachGeneration,
        )
    }

    override suspend fun sendReport(report: ByteArray): Boolean {
        val generation = hid.sessionGeneration
        val detachGeneration = hid.detachGeneration
        val sent = hid.send(
            report = report,
            settleMillis = FiioJa11Timing.settleMillisForMutation(report),
        )
        val command = report.getOrNull(5)?.toInt()?.and(0xFF)
        return sent && (command == 0x19 || hid.isCurrentSession(generation, detachGeneration))
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
        if (!hid.isCurrentSession(generation, detachGeneration)) return null
        val response = hid.exchange(
            report = request,
            minResponseBytes = minResponseBytes,
            acceptResponse = acceptResponse,
        ) ?: return null
        if (!hid.isCurrentSession(generation, detachGeneration)) return null
        return decoder(response)
    }

    override fun close() = hid.close()
}
