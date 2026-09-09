package com.weekssa.opraeqforuapp.data.kt02h20

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

sealed interface Kt02h20ConnectionState {
    data object Disconnected : Kt02h20ConnectionState
    data object Connecting : Kt02h20ConnectionState
    data object Connected : Kt02h20ConnectionState
    data class Error(val message: String) : Kt02h20ConnectionState
}

/**
 * Narrow Android USB-host session for one exact VID/PID and one HID interrupt interface.
 *
 * This class has no device commands of its own. It cannot enter firmware-update mode or address an
 * arbitrary KT02H20 dongle; the caller supplies the exact approved USB identity at construction.
 */
internal class AndroidKt02h20HidSession(
    context: Context,
    private val vendorId: Int,
    private val productId: Int,
    private val deviceLabel: String,
    permissionSuffix: String,
) : Closeable {
    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<Kt02h20ConnectionState>(Kt02h20ConnectionState.Disconnected)
    val state: StateFlow<Kt02h20ConnectionState> = mutableState.asStateFlow()

    @Volatile
    private var session: UsbSession? = null
    @Volatile
    private var currentSessionGeneration: Long = 0L
    private var lastSessionGeneration: Long = 0L
    private var receiverRegistered = false
    private val permissionAction = "${appContext.packageName}.$permissionSuffix.USB_PERMISSION"

    val sessionGeneration: Long
        get() = currentSessionGeneration

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.usbDevice() ?: return
            if (!device.matchesTarget()) return
            when (intent.action) {
                permissionAction -> {
                    if (usbManager.hasPermission(device)) {
                        openAsync(device)
                    } else {
                        mutableState.value = Kt02h20ConnectionState.Error(
                            "USB permission was not granted for $deviceLabel.",
                        )
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    closeSession()
                    mutableState.value = Kt02h20ConnectionState.Disconnected
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    if (mutableState.value is Kt02h20ConnectionState.Connecting && usbManager.hasPermission(device)) {
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
            mutableState.value = Kt02h20ConnectionState.Error(
                "$deviceLabel not detected. Connect the DAC by USB and try again.",
            )
            return
        }
        if (session != null) {
            mutableState.value = Kt02h20ConnectionState.Connected
            return
        }
        mutableState.value = Kt02h20ConnectionState.Connecting
        if (usbManager.hasPermission(device)) {
            openAsync(device)
        } else {
            val mutabilityFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val permissionIntent = PendingIntent.getBroadcast(
                appContext,
                permissionAction.hashCode(),
                Intent(permissionAction).setPackage(appContext.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag,
            )
            usbManager.requestPermission(device, permissionIntent)
            startPermissionFallback()
        }
    }

    suspend fun send(report: ByteArray, settleMillis: Long = 8L): Boolean = mutex.withLock {
        val current = session ?: return@withLock false
        val written = current.connection.bulkTransfer(
            current.endpointOut,
            report,
            report.size,
            TRANSFER_TIMEOUT_MILLIS,
        )
        if (written != report.size) return@withLock false
        if (settleMillis > 0) delay(settleMillis)
        true
    }

    suspend fun exchange(
        report: ByteArray,
        minResponseBytes: Int,
        timeoutMillis: Long = RESPONSE_TIMEOUT_MILLIS,
    ): ByteArray? = mutex.withLock {
        val current = session ?: return@withLock null
        drainInput(current)
        val written = current.connection.bulkTransfer(
            current.endpointOut,
            report,
            report.size,
            TRANSFER_TIMEOUT_MILLIS,
        )
        if (written != report.size) return@withLock null
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            val response = ByteArray(maxOf(current.endpointIn.maxPacketSize, 64))
            val read = current.connection.bulkTransfer(
                current.endpointIn,
                response,
                response.size,
                READ_POLL_MILLIS,
            )
            if (read >= minResponseBytes) return@withLock response.copyOf(read)
            delay(READ_RETRY_DELAY_MILLIS)
        }
        null
    }

    private fun drainInput(current: UsbSession) {
        val buffer = ByteArray(maxOf(current.endpointIn.maxPacketSize, 64))
        repeat(8) {
            if (current.connection.bulkTransfer(current.endpointIn, buffer, buffer.size, 2) <= 0) return
        }
    }

    private fun openAsync(device: UsbDevice) {
        scope.launch {
            mutex.withLock {
                closeSessionLocked()
                val connection = usbManager.openDevice(device)
                if (connection == null) {
                    mutableState.value = Kt02h20ConnectionState.Error(
                        "Android could not open the $deviceLabel USB device.",
                    )
                    return@withLock
                }
                val descriptor = findHidInterface(device)
                if (descriptor == null || !connection.claimInterface(descriptor.usbInterface, true)) {
                    connection.close()
                    mutableState.value = Kt02h20ConnectionState.Error(
                        "Android could not claim the $deviceLabel PEQ HID interface.",
                    )
                    return@withLock
                }
                session = UsbSession(
                    connection = connection,
                    usbInterface = descriptor.usbInterface,
                    endpointIn = descriptor.endpointIn,
                    endpointOut = descriptor.endpointOut,
                )
                lastSessionGeneration = nextSessionGeneration(lastSessionGeneration)
                currentSessionGeneration = lastSessionGeneration
                mutableState.value = Kt02h20ConnectionState.Connected
            }
        }
    }

    private fun findHidInterface(device: UsbDevice): HidInterface? =
        (0 until device.interfaceCount)
            .map(device::getInterface)
            .filter { it.interfaceClass == UsbConstants.USB_CLASS_HID }
            .mapNotNull { intf ->
                val endpoints = (0 until intf.endpointCount).map(intf::getEndpoint)
                val input = endpoints.firstOrNull { endpoint ->
                    endpoint.direction == UsbConstants.USB_DIR_IN &&
                        endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT
                }
                val output = endpoints.firstOrNull { endpoint ->
                    endpoint.direction == UsbConstants.USB_DIR_OUT &&
                        endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT
                }
                if (input != null && output != null) HidInterface(intf, input, output) else null
            }
            .firstOrNull()

    private fun startPermissionFallback() {
        scope.launch {
            delay(PERMISSION_RESPONSE_TIMEOUT_MILLIS)
            if (mutableState.value !is Kt02h20ConnectionState.Connecting) return@launch
            val device = findDevice()
            when {
                device == null -> mutableState.value = Kt02h20ConnectionState.Error(
                    "$deviceLabel disconnected while Android was requesting USB permission.",
                )
                usbManager.hasPermission(device) -> openAsync(device)
                else -> mutableState.value = Kt02h20ConnectionState.Error(
                    "USB permission request timed out. Disconnect and reconnect $deviceLabel, then try again.",
                )
            }
        }
    }

    private fun findDevice(): UsbDevice? = usbManager.deviceList.values.firstOrNull { it.matchesTarget() }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(permissionAction).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true
    }

    private fun closeSession() {
        scope.launch { mutex.withLock { closeSessionLocked() } }
    }

    private fun closeSessionLocked() {
        val current = session ?: return
        session = null
        currentSessionGeneration = 0L
        runCatching { current.connection.releaseInterface(current.usbInterface) }
        runCatching { current.connection.close() }
    }

    override fun close() {
        if (receiverRegistered) {
            runCatching { appContext.unregisterReceiver(receiver) }
            receiverRegistered = false
        }
        closeSession()
        mutableState.value = Kt02h20ConnectionState.Disconnected
    }

    private fun UsbDevice.matchesTarget(): Boolean = this.vendorId == vendorId && this.productId == productId

    @Suppress("DEPRECATION")
    private fun Intent.usbDevice(): UsbDevice? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
    } else {
        getParcelableExtra(UsbManager.EXTRA_DEVICE)
    }

    private data class HidInterface(
        val usbInterface: UsbInterface,
        val endpointIn: UsbEndpoint,
        val endpointOut: UsbEndpoint,
    )

    private data class UsbSession(
        val connection: UsbDeviceConnection,
        val usbInterface: UsbInterface,
        val endpointIn: UsbEndpoint,
        val endpointOut: UsbEndpoint,
    )

    private companion object {
        const val TRANSFER_TIMEOUT_MILLIS = 300
        const val RESPONSE_TIMEOUT_MILLIS = 700L
        const val READ_POLL_MILLIS = 80
        const val READ_RETRY_DELAY_MILLIS = 5L
        const val PERMISSION_RESPONSE_TIMEOUT_MILLIS = 10_000L

        fun nextSessionGeneration(previous: Long): Long =
            if (previous == Long.MAX_VALUE) 1L else previous + 1L
    }
}
