package com.weekssa.opraeqforuapp.data.blackpearl

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlProtocol
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlTransport
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface BlackPearlConnectionState {
    data object Disconnected : BlackPearlConnectionState
    data object Connecting : BlackPearlConnectionState
    data object Connected : BlackPearlConnectionState
    data class Error(val message: String) : BlackPearlConnectionState
}

/** Android USB-host transport for the EQ-only Black Pearl protocol. */
class AndroidBlackPearlUsbTransport(
    context: Context,
) : BlackPearlTransport, Closeable {
    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usbMutex = Mutex()
    private val mutableState = MutableStateFlow<BlackPearlConnectionState>(BlackPearlConnectionState.Disconnected)
    val state: StateFlow<BlackPearlConnectionState> = mutableState.asStateFlow()

    @Volatile
    private var session: UsbSession? = null
    private var receiverRegistered = false

    private val permissionAction = "${appContext.packageName}.BLACK_PEARL_USB_PERMISSION"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.usbDevice() ?: return
            if (!device.isBlackPearl()) return
            when (intent.action) {
                permissionAction -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAsync(device)
                    } else {
                        mutableState.value = BlackPearlConnectionState.Error("USB permission was not granted for the Black Pearl.")
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    closeSession()
                    mutableState.value = BlackPearlConnectionState.Disconnected
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    if (mutableState.value is BlackPearlConnectionState.Connecting && usbManager.hasPermission(device)) {
                        openAsync(device)
                    }
                }
            }
        }
    }

    init {
        registerReceiver()
    }

    fun connect() {
        val device = findDevice()
        if (device == null) {
            mutableState.value = BlackPearlConnectionState.Error("TRN Black Pearl not detected. Connect the DAC by USB and try again.")
            return
        }
        if (session != null) {
            mutableState.value = BlackPearlConnectionState.Connected
            return
        }

        mutableState.value = BlackPearlConnectionState.Connecting
        if (usbManager.hasPermission(device)) {
            openAsync(device)
        } else {
            val permissionIntent = PendingIntent.getBroadcast(
                appContext,
                0,
                Intent(permissionAction).setPackage(appContext.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    override suspend fun readActiveSlot(): Byte? = usbMutex.withLock {
        val current = session ?: return@withLock null
        val endpoint = current.endpointIn ?: return@withLock null
        val request = BlackPearlProtocol.readBandReport(0)

        // Drain stale interrupt responses before issuing the targeted read.
        val drain = ByteArray(BlackPearlProtocol.REPORT_SIZE)
        repeat(8) {
            if (current.connection.bulkTransfer(endpoint, drain, drain.size, 2) <= 0) return@repeat
        }

        if (!sendControlReport(current, request)) return@withLock null
        val deadline = System.currentTimeMillis() + READ_TIMEOUT_MILLIS
        val response = ByteArray(BlackPearlProtocol.REPORT_SIZE)
        while (System.currentTimeMillis() < deadline) {
            val read = current.connection.bulkTransfer(endpoint, response, response.size, READ_POLL_MILLIS)
            if (read > 0) {
                BlackPearlProtocol.activeSlotFromBandResponse(response)?.let { return@withLock it }
            }
            delay(READ_RETRY_DELAY_MILLIS)
        }
        null
    }

    override suspend fun sendReport(report: ByteArray): Boolean = usbMutex.withLock {
        require(report.size == BlackPearlProtocol.REPORT_SIZE) { "Black Pearl HID reports must be 64 bytes." }
        val current = session ?: return@withLock false
        val sent = sendControlReport(current, report)
        if (sent) {
            when (report[2].toInt() and 0xFF) {
                0x09 -> delay(PEQ_WRITE_SETTLE_MILLIS)
                0x01 -> delay(FLASH_SETTLE_MILLIS)
                else -> delay(COMMAND_SETTLE_MILLIS)
            }
        }
        sent
    }

    private fun sendControlReport(current: UsbSession, report: ByteArray): Boolean {
        val result = current.connection.controlTransfer(
            HID_CLASS_INTERFACE_OUT,
            HID_SET_REPORT,
            HID_OUTPUT_REPORT_VALUE_BASE or (report[0].toInt() and 0xFF),
            current.usbInterface.id,
            report,
            report.size,
            CONTROL_TIMEOUT_MILLIS,
        )
        return result >= 0
    }

    private fun openAsync(device: UsbDevice) {
        scope.launch {
            usbMutex.withLock {
                closeSessionLocked()
                val connection = usbManager.openDevice(device)
                if (connection == null) {
                    mutableState.value = BlackPearlConnectionState.Error("Android could not open the Black Pearl USB device.")
                    return@withLock
                }
                val usbInterface = findControlInterface(device)
                if (usbInterface == null || !connection.claimInterface(usbInterface, true)) {
                    connection.close()
                    mutableState.value = BlackPearlConnectionState.Error("Android could not claim the Black Pearl EQ interface.")
                    return@withLock
                }
                val endpointIn = findInterruptInEndpoint(usbInterface)
                if (endpointIn == null) {
                    connection.releaseInterface(usbInterface)
                    connection.close()
                    mutableState.value = BlackPearlConnectionState.Error("Black Pearl EQ response endpoint was not found.")
                    return@withLock
                }
                session = UsbSession(connection, usbInterface, endpointIn)
                mutableState.value = BlackPearlConnectionState.Connected
            }
        }
    }

    private fun findDevice(): UsbDevice? = usbManager.deviceList.values.firstOrNull(UsbDevice::isBlackPearl)

    private fun findControlInterface(device: UsbDevice): UsbInterface? {
        val interfaces = (0 until device.interfaceCount).map(device::getInterface)
        return interfaces.firstOrNull { intf ->
            intf.interfaceClass == UsbConstants.USB_CLASS_HID && findInterruptInEndpoint(intf) != null
        } ?: interfaces.firstOrNull { intf ->
            intf.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC && findInterruptInEndpoint(intf) != null
        } ?: interfaces.firstOrNull { findInterruptInEndpoint(it) != null }
    }

    private fun findInterruptInEndpoint(usbInterface: UsbInterface): UsbEndpoint? =
        (0 until usbInterface.endpointCount)
            .map(usbInterface::getEndpoint)
            .firstOrNull { endpoint ->
                endpoint.direction == UsbConstants.USB_DIR_IN &&
                    endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT
            }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(permissionAction).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun closeSession() {
        scope.launch {
            usbMutex.withLock { closeSessionLocked() }
        }
    }

    private fun closeSessionLocked() {
        val current = session ?: return
        session = null
        runCatching { current.connection.releaseInterface(current.usbInterface) }
        runCatching { current.connection.close() }
    }

    override fun close() {
        if (receiverRegistered) {
            runCatching { appContext.unregisterReceiver(receiver) }
            receiverRegistered = false
        }
        closeSession()
        mutableState.value = BlackPearlConnectionState.Disconnected
    }

    private fun UsbDevice.isBlackPearl(): Boolean =
        vendorId == BlackPearlProtocol.VENDOR_ID && productId == BlackPearlProtocol.PRODUCT_ID

    @Suppress("DEPRECATION")
    private fun Intent.usbDevice(): UsbDevice? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
    } else {
        getParcelableExtra(UsbManager.EXTRA_DEVICE)
    }

    private data class UsbSession(
        val connection: UsbDeviceConnection,
        val usbInterface: UsbInterface,
        val endpointIn: UsbEndpoint?,
    )

    companion object {
        private const val HID_CLASS_INTERFACE_OUT = 0x21
        private const val HID_SET_REPORT = 0x09
        private const val HID_OUTPUT_REPORT_VALUE_BASE = 0x0200
        private const val CONTROL_TIMEOUT_MILLIS = 250
        private const val READ_TIMEOUT_MILLIS = 500L
        private const val READ_POLL_MILLIS = 60
        private const val READ_RETRY_DELAY_MILLIS = 5L
        private const val PEQ_WRITE_SETTLE_MILLIS = 100L
        private const val FLASH_SETTLE_MILLIS = 300L
        private const val COMMAND_SETTLE_MILLIS = 20L
    }
}
