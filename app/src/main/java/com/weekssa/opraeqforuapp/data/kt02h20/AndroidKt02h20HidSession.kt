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
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11ReportWriteOutcome
import com.weekssa.opraeqforuapp.domain.kt02h20.classifyJa11ReportWriteOutcome
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface Kt02h20ConnectionState {
    data object Disconnected : Kt02h20ConnectionState
    data object Connecting : Kt02h20ConnectionState
    data object Connected : Kt02h20ConnectionState
    data class Error(val message: String) : Kt02h20ConnectionState
    data class PermissionRequired(val message: String) : Kt02h20ConnectionState
}

/**
 * Narrow Android USB-host session for one exact approved hardware model.
 *
 * A model may have more than one exact approved PID when a verified mode switch re-enumerates the
 * same physical device (for example JA11 UAC 1.0/2.0). This class still accepts only the explicit
 * closed set supplied by the caller; it never broad-matches a chipset family.
 *
 * This class has no device commands of its own and cannot enter firmware-update mode.
 */
internal class AndroidKt02h20HidSession(
    context: Context,
    private val vendorId: Int,
    private val productIds: Set<Int>,
    private val deviceLabel: String,
    permissionSuffix: String,
    private val deviceIdentityMatcher: (UsbDevice) -> Boolean = { true },
    private val hidInterfaceMatcher: (UsbInterface) -> Boolean = { true },
    private val additionalFingerprintFields: (UsbDevice) -> List<Pair<String, String>> = { emptyList() },
) : Closeable {
    init {
        require(productIds.isNotEmpty()) { "At least one approved USB PID is required." }
        require(productIds.all { it in 0..0xFFFF }) { "USB PIDs must be 16-bit values." }
    }

    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<Kt02h20ConnectionState>(Kt02h20ConnectionState.Disconnected)
    val state: StateFlow<Kt02h20ConnectionState> = mutableState.asStateFlow()
    private val mutablePresent = MutableStateFlow(false)
    val present: StateFlow<Boolean> = mutablePresent.asStateFlow()

    @Volatile
    private var session: UsbSession? = null
    @Volatile
    private var currentSessionGeneration: Long = 0L
    @Volatile
    private var detachSequence: Long = 0L
    @Volatile
    private var permissionRequests: Long = 0L
    private var lastSessionGeneration: Long = 0L
    private var receiverRegistered = false
    private val permissionAction = "${appContext.packageName}.$permissionSuffix.USB_PERMISSION"

    val sessionGeneration: Long
        get() = currentSessionGeneration

    val detachGeneration: Long
        get() = detachSequence

    val connectedProductId: Int?
        get() = session?.productId

    val deviceFingerprintKey: String?
        get() = session?.fingerprintKey

    val permissionRequestCount: Long
        get() = permissionRequests

    /**
     * A read or ordinary mutation may report success only if the physical session stayed the
     * same for the whole exchange. Save is the deliberate exception because it is itself allowed
     * to detach and replace the USB session; its caller owns the reconnect boundary.
     */
    fun isCurrentSession(expectedGeneration: Long, expectedDetachGeneration: Long): Boolean =
        expectedGeneration > 0L &&
            currentSessionGeneration == expectedGeneration &&
            detachSequence == expectedDetachGeneration &&
            session?.generation == expectedGeneration &&
            session?.detachGeneration == expectedDetachGeneration &&
            state.value is Kt02h20ConnectionState.Connected

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.usbDevice() ?: return
            if (!device.matchesTarget()) return
            when (intent.action) {
                permissionAction -> {
                    val granted = usbManager.hasPermission(device)
                    Ja11DiagnosticLog.eventForDevice(
                        deviceLabel,
                        "USB_PERMISSION_RESULT",
                        "pid" to device.productId,
                        "granted" to granted,
                    )
                    if (granted) {
                        openAsync(device)
                    } else {
                        mutableState.value = Kt02h20ConnectionState.PermissionRequired(
                            "Android USB permission is required for $deviceLabel. Approve the prompt, then tap Connect to verify the DAC.",
                        )
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val currentSession = session
                    if (currentSession == null || !currentSession.matchesDetach(device.productId, device.deviceName)) {
                        // A delayed detach broadcast for the old UAC PID (or a second matching
                        // device) must not invalidate a newer session that is already open.
                        Ja11DiagnosticLog.eventForDevice(
                            deviceLabel,
                            "USB_DETACH_IGNORED",
                            "pid" to device.productId,
                            "currentPid" to currentSession?.productId,
                            "currentSessionGeneration" to currentSession?.generation,
                            "currentDetachGeneration" to currentSession?.detachGeneration,
                        )
                        mutablePresent.value = findDevice() != null
                        return
                    }
                    // A device reset can emit DETACHED/ATTACHED during a mutating operation.
                    // Publish the physical absence immediately so the reconnect policy cannot
                    // open a new handle against the old UsbDevice while the reset is in flight.
                    // The actual close still runs under the session mutex before any replacement
                    // session can be opened.
                    val detachedSession = currentSession
                    Ja11DiagnosticLog.eventForDevice(
                        deviceLabel,
                        "USB_DETACH",
                        "pid" to device.productId,
                        "sessionGeneration" to detachedSession.generation,
                        "detachGenerationBefore" to detachSequence,
                    )
                    detachSequence = nextSessionGeneration(detachSequence)
                    val detachAtEvent = detachSequence
                    mutablePresent.value = false
                    mutableState.value = Kt02h20ConnectionState.Disconnected
                    scope.launch {
                        mutex.withLock {
                            if (session?.generation == detachedSession.generation &&
                                session?.detachGeneration == detachedSession.detachGeneration &&
                                detachSequence == detachAtEvent
                            ) {
                                closeSessionLocked()
                            }
                        }
                        mutablePresent.value = findDevice() != null
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val hasPermission = usbManager.hasPermission(device)
                    Ja11DiagnosticLog.eventForDevice(
                        deviceLabel,
                        "USB_ATTACH",
                        "pid" to device.productId,
                        "permissionGranted" to hasPermission,
                        "sessionGeneration" to currentSessionGeneration,
                        "detachGeneration" to detachSequence,
                    )
                    mutablePresent.value = true
                    if (mutableState.value is Kt02h20ConnectionState.Connecting && hasPermission) {
                        openAsync(device)
                    }
                }
            }
        }
    }

    init {
        registerReceiver()
        mutablePresent.value = findDevice() != null
    }

    @Synchronized
    fun connect() {
        val device = findDevice()
        if (device == null) {
            Ja11DiagnosticLog.eventForDevice(deviceLabel, "CONNECT_NO_DEVICE")
            mutablePresent.value = false
            mutableState.value = Kt02h20ConnectionState.Error(
                "$deviceLabel not detected. Connect the DAC by USB and try again.",
            )
            return
        }
        Ja11DiagnosticLog.eventForDevice(
            deviceLabel,
            "CONNECT_REQUEST",
            "pid" to device.productId,
            "permissionGranted" to usbManager.hasPermission(device),
            "sessionGeneration" to currentSessionGeneration,
            "detachGeneration" to detachSequence,
        )
        mutablePresent.value = true
        // Connect is intentionally idempotent. During an EW300 commit the USB function can
        // disappear and re-enumerate; the permission callback, attach callback, and reconnect
        // policy may all observe that transition. Do not queue duplicate permission requests or
        // competing open jobs while one connection attempt is already in flight.
        val current = session
        if (current != null &&
            current.detachGeneration == detachSequence &&
            state.value is Kt02h20ConnectionState.Connected
        ) {
            mutableState.value = Kt02h20ConnectionState.Connected
            return
        }
        if (mutableState.value is Kt02h20ConnectionState.Connecting) return
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
            permissionRequests += 1L
            Ja11DiagnosticLog.eventForDevice(
                deviceLabel,
                "USB_PERMISSION_REQUESTED",
                "pid" to device.productId,
                "requestCount" to permissionRequests,
            )
            usbManager.requestPermission(device, permissionIntent)
            startPermissionFallback()
        }
    }

    suspend fun send(report: ByteArray, settleMillis: Long = 8L): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = session ?: return@withLock false
            val written = runCatching {
                current.connection.bulkTransfer(
                    current.endpointOut,
                    report,
                    report.size,
                    TRANSFER_TIMEOUT_MILLIS,
                )
            }.getOrDefault(-1)
            if (written != report.size) return@withLock false
            if (settleMillis > 0) delay(settleMillis)
            true
        }
    }

    /**
     * Sends one JA11 report only through the session captured by its caller. A complete report is
     * kept distinct from a later session change during settling so restart-capable controls can
     * verify the requested value on a replacement session without retrying the report.
     */
    suspend fun sendJa11Report(
        report: ByteArray,
        expectedGeneration: Long,
        expectedDetachGeneration: Long,
        settleMillis: Long,
    ): FiioJa11ReportWriteOutcome = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = session
            if (current == null ||
                currentSessionGeneration != expectedGeneration ||
                current.generation != expectedGeneration ||
                detachSequence != expectedDetachGeneration ||
                current.detachGeneration != expectedDetachGeneration ||
                state.value !is Kt02h20ConnectionState.Connected
            ) {
                Ja11DiagnosticLog.eventForDevice(
                    deviceLabel,
                    "JA11_REPORT_REJECTED_STALE",
                    "command" to report.getOrNull(5)?.toInt()?.and(0xFF),
                    "bytesExpected" to report.size,
                    "actualPid" to current?.productId,
                    "approvedPids" to productIds.sorted().joinToString(","),
                    "expectedSessionGeneration" to expectedGeneration,
                    "actualSessionGeneration" to current?.generation,
                    "expectedDetachGeneration" to expectedDetachGeneration,
                    "actualDetachGeneration" to detachSequence,
                )
                return@withLock classifyJa11ReportWriteOutcome(
                    reportWasComplete = false,
                    sessionCurrentBeforeSend = false,
                    sessionCurrentAfterSettle = false,
                )
            }
            Ja11DiagnosticLog.eventForDevice(
                deviceLabel,
                "JA11_REPORT_SEND_STARTED",
                "command" to report.getOrNull(5)?.toInt()?.and(0xFF),
                "bytesExpected" to report.size,
                "pid" to current.productId,
                "sessionGeneration" to current.generation,
                "detachGeneration" to current.detachGeneration,
            )
            val written = runCatching {
                current.connection.bulkTransfer(
                    current.endpointOut,
                    report,
                    report.size,
                    TRANSFER_TIMEOUT_MILLIS,
                )
            }.getOrDefault(-1)
            if (written != report.size) {
                val outcome = classifyJa11ReportWriteOutcome(
                    reportWasComplete = false,
                    sessionCurrentBeforeSend = true,
                    sessionCurrentAfterSettle = isCurrentSession(expectedGeneration, expectedDetachGeneration),
                )
                Ja11DiagnosticLog.eventForDevice(
                    deviceLabel,
                    "JA11_REPORT_SEND_RESULT",
                    "command" to report.getOrNull(5)?.toInt()?.and(0xFF),
                    "bytesExpected" to report.size,
                    "bytesWritten" to written,
                    "pid" to current.productId,
                    "sessionGeneration" to current.generation,
                    "detachGeneration" to current.detachGeneration,
                    "outcome" to outcome.name,
                )
                return@withLock outcome
            }
            if (settleMillis > 0) delay(settleMillis)
            val outcome = classifyJa11ReportWriteOutcome(
                reportWasComplete = true,
                sessionCurrentBeforeSend = true,
                sessionCurrentAfterSettle = isCurrentSession(expectedGeneration, expectedDetachGeneration),
            )
            Ja11DiagnosticLog.eventForDevice(
                deviceLabel,
                "JA11_REPORT_SEND_RESULT",
                "command" to report.getOrNull(5)?.toInt()?.and(0xFF),
                "bytesExpected" to report.size,
                "bytesWritten" to written,
                "pid" to current.productId,
                "sessionGeneration" to current.generation,
                "detachGeneration" to current.detachGeneration,
                "settleMillis" to settleMillis,
                "currentAfterSettle" to isCurrentSession(expectedGeneration, expectedDetachGeneration),
                "outcome" to outcome.name,
            )
            outcome
        }
    }

    suspend fun exchange(
        report: ByteArray,
        minResponseBytes: Int,
        timeoutMillis: Long = RESPONSE_TIMEOUT_MILLIS,
        acceptResponse: (ByteArray) -> Boolean = { true },
    ): ByteArray? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = session ?: return@withLock null
            drainInput(current)
            val written = runCatching {
                current.connection.bulkTransfer(
                    current.endpointOut,
                    report,
                    report.size,
                    TRANSFER_TIMEOUT_MILLIS,
                )
            }.getOrDefault(-1)
            if (written != report.size) return@withLock null
            val deadline = System.currentTimeMillis() + timeoutMillis
            while (System.currentTimeMillis() < deadline) {
                val response = ByteArray(maxOf(current.endpointIn.maxPacketSize, 64))
                val read = runCatching {
                    current.connection.bulkTransfer(
                        current.endpointIn,
                        response,
                        response.size,
                        READ_POLL_MILLIS,
                    )
                }.getOrDefault(-1)
                if (read >= minResponseBytes) {
                    val candidate = response.copyOf(read)
                    if (acceptResponse(candidate)) return@withLock candidate
                }
                delay(READ_RETRY_DELAY_MILLIS)
            }
            null
        }
    }

    /** JA11-only read exchange pinned to the session captured before the baseline field read. */
    suspend fun exchangeJa11(
        report: ByteArray,
        expectedGeneration: Long,
        expectedDetachGeneration: Long,
        minResponseBytes: Int,
        timeoutMillis: Long = RESPONSE_TIMEOUT_MILLIS,
        acceptResponse: (ByteArray) -> Boolean = { true },
    ): ByteArray? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = session
            if (current == null ||
                currentSessionGeneration != expectedGeneration ||
                current.generation != expectedGeneration ||
                detachSequence != expectedDetachGeneration ||
                current.detachGeneration != expectedDetachGeneration ||
                state.value !is Kt02h20ConnectionState.Connected
            ) {
                return@withLock null
            }
            drainInput(current)
            val written = runCatching {
                current.connection.bulkTransfer(
                    current.endpointOut,
                    report,
                    report.size,
                    TRANSFER_TIMEOUT_MILLIS,
                )
            }.getOrDefault(-1)
            if (written != report.size) return@withLock null
            val deadline = System.currentTimeMillis() + timeoutMillis
            while (System.currentTimeMillis() < deadline) {
                val response = ByteArray(maxOf(current.endpointIn.maxPacketSize, 64))
                val read = runCatching {
                    current.connection.bulkTransfer(
                        current.endpointIn,
                        response,
                        response.size,
                        READ_POLL_MILLIS,
                    )
                }.getOrDefault(-1)
                if (read >= minResponseBytes) {
                    val candidate = response.copyOf(read)
                    if (acceptResponse(candidate)) {
                        return@withLock candidate.takeIf {
                            isCurrentSession(expectedGeneration, expectedDetachGeneration)
                        }
                    }
                }
                delay(READ_RETRY_DELAY_MILLIS)
            }
            null
        }
    }

    /**
     * Allows a mutating command that may reset the USB function to finish its reconnect boundary.
     *
     * EW300's persistence command can re-enumerate the same VID/PID. The old UsbDeviceConnection
     * is no longer valid after that point, so callers must not immediately issue a readback on it.
     * This waits for the observed detach and a fresh session generation (including the Android
     * permission prompt when the platform requires it); a timeout is reported as a failed mutation
     * rather than risking a readback against the old handle.
     */
    suspend fun awaitReconnectAfterMutation(
        previousGeneration: Long,
        previousDetachGeneration: Long,
        observationMillis: Long = REENUMERATION_OBSERVATION_MILLIS,
        timeoutMillis: Long = RECONNECT_TIMEOUT_MILLIS,
    ): Boolean {
        if (previousGeneration <= 0L) return false
        delay(observationMillis)
        return withTimeoutOrNull(timeoutMillis) {
            state.first {
                it is Kt02h20ConnectionState.Connected &&
                    currentSessionGeneration != 0L &&
                    currentSessionGeneration != previousGeneration &&
                    detachSequence != previousDetachGeneration
            }
            true
        } ?: false
    }

    /**
     * Some exact-device Save implementations re-enumerate while others keep the HID session.
     * Treat an unchanged healthy session as accepted; if detach was observed, require a fresh
     * generation. The caller still owns value readback and power-removal verification.
     */
    suspend fun awaitOptionalReconnectAfterMutation(
        previousGeneration: Long,
        previousDetachGeneration: Long,
        observationMillis: Long = REENUMERATION_OBSERVATION_MILLIS,
        timeoutMillis: Long = RECONNECT_TIMEOUT_MILLIS,
    ): Boolean {
        if (previousGeneration <= 0L) return false
        delay(observationMillis)
        if (detachSequence == previousDetachGeneration) {
            return currentSessionGeneration == previousGeneration &&
                state.value is Kt02h20ConnectionState.Connected
        }
        return withTimeoutOrNull(timeoutMillis) {
            state.first {
                it is Kt02h20ConnectionState.Connected &&
                    currentSessionGeneration != 0L &&
                    currentSessionGeneration != previousGeneration
            }
            true
        } ?: false
    }

    private fun drainInput(current: UsbSession) {
        val buffer = ByteArray(maxOf(current.endpointIn.maxPacketSize, 64))
        repeat(8) {
            val read = runCatching {
                current.connection.bulkTransfer(current.endpointIn, buffer, buffer.size, 2)
            }.getOrDefault(-1)
            if (read <= 0) return
        }
    }

    private fun openAsync(device: UsbDevice) {
        Ja11DiagnosticLog.eventForDevice(
            deviceLabel,
            "USB_OPEN_REQUEST",
            "pid" to device.productId,
            "sessionGeneration" to currentSessionGeneration,
            "detachGeneration" to detachSequence,
        )
        scope.launch {
            mutex.withLock {
                // Permission and attach broadcasts can both request an open for the same
                // UsbDevice. The first successful opener owns the session; later jobs must leave
                // it alone instead of closing and replacing a live handle.
                val current = session
                if (current != null &&
                    current.detachGeneration == detachSequence &&
                    state.value is Kt02h20ConnectionState.Connected
                ) {
                    mutableState.value = Kt02h20ConnectionState.Connected
                    return@withLock
                }
                closeSessionLocked()
                val connection = runCatching { usbManager.openDevice(device) }.getOrNull()
                if (connection == null) {
                    Ja11DiagnosticLog.eventForDevice(
                        deviceLabel,
                        "USB_OPEN_FAILED",
                        "pid" to device.productId,
                        "reason" to "open_device_null",
                    )
                    mutableState.value = Kt02h20ConnectionState.Error(
                        "Android could not open the $deviceLabel USB device.",
                    )
                    return@withLock
                }
                val descriptor = runCatching { findHidInterface(device) }.getOrNull()
                val claimed = descriptor != null && runCatching {
                    connection.claimInterface(descriptor.usbInterface, true)
                }.getOrDefault(false)
                if (!claimed) {
                    connection.close()
                    Ja11DiagnosticLog.eventForDevice(
                        deviceLabel,
                        "USB_OPEN_FAILED",
                        "pid" to device.productId,
                        "reason" to "hid_claim_failed",
                    )
                    mutableState.value = Kt02h20ConnectionState.Error(
                        "Android could not claim the $deviceLabel PEQ HID interface.",
                    )
                    return@withLock
                }
                lastSessionGeneration = nextSessionGeneration(lastSessionGeneration)
                session = UsbSession(
                    connection = connection,
                    usbInterface = descriptor.usbInterface,
                    endpointIn = descriptor.endpointIn,
                    endpointOut = descriptor.endpointOut,
                    productId = device.productId,
                    fingerprintKey = fingerprintKey(device, descriptor.usbInterface),
                    deviceName = device.deviceName,
                    generation = lastSessionGeneration,
                    detachGeneration = detachSequence,
                )
                currentSessionGeneration = lastSessionGeneration
                mutableState.value = Kt02h20ConnectionState.Connected
                Ja11DiagnosticLog.eventForDevice(
                    deviceLabel,
                    "USB_SESSION_OPENED",
                    "pid" to device.productId,
                    "sessionGeneration" to lastSessionGeneration,
                    "detachGeneration" to detachSequence,
                )
            }
        }
    }

    private fun findHidInterface(device: UsbDevice): HidInterface? =
        (0 until device.interfaceCount)
            .map(device::getInterface)
            .filter { it.interfaceClass == UsbConstants.USB_CLASS_HID && hidInterfaceMatcher(it) }
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
                device == null -> {
                    mutablePresent.value = false
                    mutableState.value = Kt02h20ConnectionState.Error(
                        "$deviceLabel disconnected while Android was requesting USB permission.",
                    )
                }
                usbManager.hasPermission(device) -> openAsync(device)
                else -> mutableState.value = Kt02h20ConnectionState.PermissionRequired(
                    "Android USB permission is required for $deviceLabel. Approve it, then tap Connect to verify the DAC.",
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
        Ja11DiagnosticLog.eventForDevice(
            deviceLabel,
            "USB_SESSION_CLOSED",
            "pid" to current.productId,
            "sessionGeneration" to current.generation,
            "detachGeneration" to current.detachGeneration,
        )
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

    private fun UsbDevice.matchesTarget(): Boolean =
        this.vendorId == vendorId && this.productId in productIds && deviceIdentityMatcher(this)

    private fun fingerprintKey(device: UsbDevice, usbInterface: UsbInterface): String {
        val manufacturer = runCatching { device.manufacturerName }.getOrNull().orEmpty()
        val product = runCatching { device.productName }.getOrNull().orEmpty()
        val serial = runCatching { device.serialNumber }.getOrNull().orEmpty()
        return (listOf(
            "vid=${device.vendorId.toString(16)}",
            "pid=${device.productId.toString(16)}",
            "manufacturer=${manufacturer.trim()}",
            "product=${product.trim()}",
            "serial=${serial.trim()}",
        ) + additionalFingerprintFields(device).map { (key, value) -> "$key=${value.trim()}" } +
            "interface=${usbInterface.id}").joinToString("|")
    }

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
        val productId: Int,
        val fingerprintKey: String,
        val deviceName: String,
        val generation: Long,
        val detachGeneration: Long,
    ) {
        fun matchesDetach(detachedProductId: Int, detachedDeviceName: String): Boolean =
            productId == detachedProductId && deviceName == detachedDeviceName
    }

    private companion object {
        const val TRANSFER_TIMEOUT_MILLIS = 300
        const val RESPONSE_TIMEOUT_MILLIS = 700L
        const val READ_POLL_MILLIS = 80
        const val READ_RETRY_DELAY_MILLIS = 5L
        const val PERMISSION_RESPONSE_TIMEOUT_MILLIS = 10_000L
        const val REENUMERATION_OBSERVATION_MILLIS = 350L
        const val RECONNECT_TIMEOUT_MILLIS = 20_000L

        fun nextSessionGeneration(previous: Long): Long =
            if (previous == Long.MAX_VALUE) 1L else previous + 1L
    }
}
