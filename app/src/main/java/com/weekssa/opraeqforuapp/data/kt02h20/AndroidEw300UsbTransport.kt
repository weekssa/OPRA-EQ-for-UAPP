package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Transport
import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

/** Android USB-host transport for the qualified SIMGOT EW300 DSP cable. */
class AndroidEw300UsbTransport(context: Context) : Ew300Transport, Closeable {
    private val hid = AndroidKt02h20HidSession(
        context = context,
        vendorId = Ew300Protocol.VENDOR_ID,
        productIds = setOf(Ew300Protocol.PRODUCT_ID),
        deviceLabel = "SIMGOT EW300 DSP",
        permissionSuffix = "SIMGOT_EW300",
    )

    val state: StateFlow<Kt02h20ConnectionState> = hid.state
    val present: StateFlow<Boolean> = hid.present
    val sessionGeneration: Long get() = hid.sessionGeneration

    fun connect() = hid.connect()

    override suspend fun readRegister(register: Int): ByteArray? =
        hid.exchange(
            report = Ew300Protocol.readRegisterReport(register),
            minResponseBytes = Ew300Protocol.REPORT_SIZE,
            // The EW300 can expose unsolicited HID input after a write. Do not let a valid-sized
            // non-read response satisfy this transaction; wait for the exact register read echo.
            acceptResponse = { response -> Ew300Protocol.decodeRead(register, response) != null },
        )
            ?.let { Ew300Protocol.decodeRead(register, it) }

    override suspend fun writeRegister(register: Int, data: ByteArray): Boolean =
        hid.send(Ew300Protocol.writeRegisterReport(register, data), settleMillis = 20L)

    override suspend fun commit(): Boolean =
        hid.send(Ew300Protocol.commitReport(), settleMillis = 100L)

    override fun close() = hid.close()
}
