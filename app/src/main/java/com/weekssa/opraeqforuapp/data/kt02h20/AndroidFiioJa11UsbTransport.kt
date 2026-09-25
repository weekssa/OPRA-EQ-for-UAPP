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
        val response = hid.exchange(
            report = FiioJa11Protocol.readFirmwareVersionReport(),
            minResponseBytes = 8,
            acceptResponse = { candidate -> FiioJa11Protocol.firmwareVersionFromResponse(candidate) != null },
        ) ?: return null
        return FiioJa11Protocol.firmwareVersionFromResponse(response)
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
        val response = hid.exchange(
            report = FiioJa11Protocol.readBandReport(index),
            minResponseBytes = 15,
            acceptResponse = { candidate -> FiioJa11Protocol.bandFromResponse(candidate)?.first == index },
        ) ?: return null
        val parsed = FiioJa11Protocol.bandFromResponse(response) ?: return null
        return parsed.second.takeIf { parsed.first == index }
    }

    override suspend fun readGlobalGainDb(): Double? {
        val response = hid.exchange(
            report = FiioJa11Protocol.readGlobalGainReport(),
            minResponseBytes = 8,
            acceptResponse = { candidate -> FiioJa11Protocol.globalGainFromResponse(candidate) != null },
        ) ?: return null
        return FiioJa11Protocol.globalGainFromResponse(response)
    }

    override suspend fun sendReport(report: ByteArray): Boolean = hid.send(
        report = report,
        settleMillis = FiioJa11Timing.settleMillisForMutation(report),
    )

    private suspend fun <T> exchangeOneByte(
        request: ByteArray,
        decoder: (ByteArray) -> T?,
    ): T? {
        val response = hid.exchange(
            report = request,
            minResponseBytes = 7,
            acceptResponse = { candidate -> decoder(candidate) != null },
        ) ?: return null
        return decoder(response)
    }

    override fun close() = hid.close()
}
