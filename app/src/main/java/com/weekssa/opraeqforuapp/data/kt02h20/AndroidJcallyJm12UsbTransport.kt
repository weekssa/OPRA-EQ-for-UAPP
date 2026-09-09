package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12Transport
import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

class AndroidJcallyJm12UsbTransport(
    context: Context,
) : JcallyJm12Transport, Closeable {
    private val hid = AndroidKt02h20HidSession(
        context = context,
        vendorId = JcallyJm12Protocol.VENDOR_ID,
        productId = JcallyJm12Protocol.PRODUCT_ID,
        deviceLabel = "JCALLY JM12",
        permissionSuffix = "JCALLY_JM12",
    )

    val state: StateFlow<Kt02h20ConnectionState> = hid.state
    val sessionGeneration: Long
        get() = hid.sessionGeneration

    fun connect() = hid.connect()

    override suspend fun handshake(): Boolean {
        val response = hid.exchange(
            report = JcallyJm12Protocol.handshakeReport(),
            minResponseBytes = JcallyJm12Protocol.REPORT_SIZE,
        ) ?: return false
        return JcallyJm12Protocol.handshakeAccepted(response)
    }

    override suspend fun readRegister(address: Int): Int? {
        val response = hid.exchange(
            report = JcallyJm12Protocol.readRegisterReport(address),
            minResponseBytes = JcallyJm12Protocol.REPORT_SIZE,
        ) ?: return null
        return JcallyJm12Protocol.readRegisterValue(address, response)
    }

    override suspend fun writeRegister(address: Int, value: Int): Boolean {
        val response = hid.exchange(
            report = JcallyJm12Protocol.writeRegisterReport(address, value),
            minResponseBytes = JcallyJm12Protocol.REPORT_SIZE,
        ) ?: return false
        return JcallyJm12Protocol.writeAcknowledged(address, response)
    }

    override fun close() = hid.close()
}
