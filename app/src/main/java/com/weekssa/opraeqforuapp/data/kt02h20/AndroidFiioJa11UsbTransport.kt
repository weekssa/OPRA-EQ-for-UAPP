package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Transport
import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

class AndroidFiioJa11UsbTransport(
    context: Context,
) : FiioJa11Transport, Closeable {
    private val hid = AndroidKt02h20HidSession(
        context = context,
        vendorId = FiioJa11Protocol.VENDOR_ID,
        productId = FiioJa11Protocol.PRODUCT_ID,
        deviceLabel = "FiiO JA11",
        permissionSuffix = "FIIO_JA11",
    )

    val state: StateFlow<Kt02h20ConnectionState> = hid.state

    fun connect() = hid.connect()

    override suspend fun readBand(index: Int): FiioJa11Protocol.Band? {
        val response = hid.exchange(
            report = FiioJa11Protocol.readBandReport(index),
            minResponseBytes = 15,
        ) ?: return null
        val parsed = FiioJa11Protocol.bandFromResponse(response) ?: return null
        return parsed.second.takeIf { parsed.first == index }
    }

    override suspend fun readGlobalGainDb(): Double? {
        val response = hid.exchange(
            report = FiioJa11Protocol.readGlobalGainReport(),
            minResponseBytes = 8,
        ) ?: return null
        return FiioJa11Protocol.globalGainFromResponse(response)
    }

    override suspend fun sendReport(report: ByteArray): Boolean = hid.send(
        report = report,
        settleMillis = when {
            report.size > 6 && (report[5].toInt() and 0xFF) == 0x15 -> 25L
            report.size > 6 && (report[5].toInt() and 0xFF) == 0x19 -> 80L
            else -> 15L
        },
    )

    override fun close() = hid.close()
}
