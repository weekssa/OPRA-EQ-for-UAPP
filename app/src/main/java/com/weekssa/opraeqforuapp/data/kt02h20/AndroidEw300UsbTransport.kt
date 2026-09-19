package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Transport
import java.io.Closeable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/** Android USB-host transport for the exact SIMGOT EW300 DSP beta identity. */
class AndroidEw300UsbTransport(context: Context) : Ew300Transport, Closeable {
    private val hid = AndroidKt02h20HidSession(
        context = context,
        vendorId = Ew300Protocol.VENDOR_ID,
        productIds = setOf(Ew300Protocol.PRODUCT_ID),
        deviceLabel = "SIMGOT EW300 DSP",
        permissionSuffix = "SIMGOT_EW300",
        deviceIdentityMatcher = Ew300UsbIdentity::matches,
        hidInterfaceMatcher = Ew300UsbIdentity::matchesHidInterface,
    )

    val state: StateFlow<Kt02h20ConnectionState> = hid.state
    val present: StateFlow<Boolean> = hid.present
    val sessionGeneration: Long get() = hid.sessionGeneration
    override val deviceFingerprintKey: String?
        get() = hid.deviceFingerprintKey
    override val detachGeneration: Long
        get() = hid.detachGeneration

    fun connect() = hid.connect()

    override suspend fun readRegister(register: Int): ByteArray? {
        // A connected EW300 can occasionally drop one HID input report while Android is
        // draining an unsolicited status report. Reads are idempotent, so retry only the
        // exact register read; mutating writes remain deliberately non-retried.
        repeat(3) { attempt ->
            val value = hid.exchange(
                report = Ew300Protocol.readRegisterReport(register),
                minResponseBytes = Ew300Protocol.REPORT_SIZE,
                // The EW300 can expose unsolicited HID input after a write. Do not let a valid-sized
                // non-read response satisfy this transaction; wait for the exact register read echo.
                acceptResponse = { response -> Ew300Protocol.decodeRead(register, response) != null },
            )?.let { Ew300Protocol.decodeRead(register, it) }
            if (value != null) return value
            if (attempt < 2) delay(100L)
        }
        return null
    }

    override suspend fun writeRegister(register: Int, data: ByteArray): Boolean =
        // Match the delay used by the physical qualification: the EW300 may emit an
        // unsolicited input report before the register is ready for the next transaction.
        hid.send(Ew300Protocol.writeRegisterReport(register, data), settleMillis = 200L)

    override suspend fun commit(): Boolean {
        // Persistence may reset the EW300 USB function. Keep the operation alive across that
        // expected re-enumeration and only let the flasher read back after a fresh HID handle is
        // available. This also prevents the shared auto-reconnect policy from racing a stale
        // connection while the old handle is being closed.
        val previousGeneration = hid.sessionGeneration
        val previousDetachGeneration = hid.detachGeneration
        if (!hid.send(Ew300Protocol.commitReport(), settleMillis = 1_000L)) return false
        return hid.awaitOptionalReconnectAfterMutation(previousGeneration, previousDetachGeneration)
    }

    override fun close() = hid.close()
}

/** Exact EW300 fingerprint boundary; VID/PID alone is intentionally insufficient. */
private object Ew300UsbIdentity {
    private const val EXPECTED_MANUFACTURER = "LE XIAN"
    private const val EXPECTED_PRODUCT = "SIMGOT EW300 DSP"
    private const val EXPECTED_INTERFACE_ID = 3
    private const val EXPECTED_PACKET_SIZE = 16

    fun matches(device: UsbDevice): Boolean {
        val manufacturer = runCatching { device.manufacturerName }.getOrNull()
        val product = runCatching { device.productName }.getOrNull()
        return manufacturer.equals(EXPECTED_MANUFACTURER, ignoreCase = true) &&
            product.equals(EXPECTED_PRODUCT, ignoreCase = true)
    }

    fun matchesHidInterface(usbInterface: UsbInterface): Boolean {
        if (usbInterface.id != EXPECTED_INTERFACE_ID) return false
        return (0 until usbInterface.endpointCount)
            .map(usbInterface::getEndpoint)
            .filter { it.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT }
            .any { it.direction == android.hardware.usb.UsbConstants.USB_DIR_IN && it.maxPacketSize == EXPECTED_PACKET_SIZE } &&
            (0 until usbInterface.endpointCount)
                .map(usbInterface::getEndpoint)
                .filter { it.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT }
                .any { it.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT && it.maxPacketSize == EXPECTED_PACKET_SIZE }
    }
}
