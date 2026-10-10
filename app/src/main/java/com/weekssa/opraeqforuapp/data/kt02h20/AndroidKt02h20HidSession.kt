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
    private val requireUniqueCandidate: Boolean = false,
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

    val connectedDeviceSerial: String?
        get() = session?.serial

    val supportedCandidateCount: Int
        get() = matchingDevices().size

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
            session != null &&
            state.value is Kt02h20ConnectionState.Connected &&
            (!requireUniqueCandidate || matchingDevices().singleOrNull()?.deviceName == session?.deviceName)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.usbDevice() ?: return
            if (!device.matchesTarget()) return
            when (intent.action) {
                permissionAction -> {
                    val currentDevice = currentDeviceForOpen(device)
                    if (currentDevice != null && usbManager.hasPermission(currentDevice)) {
                        openAsync(currentDevice)
                    } else {
                        if (requireUniqueCandidate) mutablePresent.value = supportedCandidateCount > 0
                        mutableState.value = candidateResolutionError()
                            ?: Kt02h20ConnectionState.PermissionRequired(
                                "Android USB permission is required for $deviceLabel. Approve the prompt, then tap Connect to verify the DAC.",
                            )
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    // A device reset can emit DETACHED/ATTACHED during a mutating operation.
                    // Publish the physical absence immediately so the reconnect policy cannot
                    // open a new handle against the old UsbDevice while the reset is in flight.
                    // The actual close still runs under the session mutex before any replacement
                    // session can be opened.
                    detachSequence = nextSessionGeneration(detachSequence)
                    mutablePresent.value = false
                    mutableState.value = Kt02h20ConnectionState.Disconnected
                    scope.launch {
                        mutex.withLock { closeSessionLocked() }
                        mutablePresent.value = findDevice() != null
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val currentDevice = currentDeviceForOpen(device)
                    mutablePresent.value = if (requireUniqueCandidate) supportedCandidateCount > 0 else true
                    if (requireUniqueCandidate && currentDevice == null && supportedCandidateCount > 1) {
                        failClosedForAmbiguousCandidates()
                    } else if (mutableState.value is Kt02h20ConnectionState.Connecting &&
                        currentDevice != null && usbManager.hasPermission(currentDevice)
                    ) {
                        openAsync(currentDevice)
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
        val candidates = matchingDevices()
        val device = if (requireUniqueCandidate) candidates.singleOrNull() else candidates.firstOrNull()
        if (device == null) {
            mutablePresent.value = candidates.isNotEmpty()
            mutableState.value = candidateResolutionError()
                ?: Kt02h20ConnectionState.Error("$deviceLabel not detected. Connect the DAC by USB and try again.")
            return
        }
        mutablePresent.value = true
        // Connect is intentionally idempotent. During an EW300 commit the USB function can
        // disappear and re-enumerate; the permission callback, attach callback, and reconnect
        // policy may all observe that transition. Do not queue duplicate permission requests or
        // competing open jobs while one connection attempt is already in flight.
        if (session != null &&
            (!requireUniqueCandidate || isCurrentSession(currentSessionGeneration, detachSequence))
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
            usbManager.requestPermission(device, permissionIntent)
            startPermissionFallback()
        }
    }

    suspend fun send(report: ByteArray, settleMillis: Long = 8L): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = session ?: return@withLock false
            if (requireUniqueCandidate && !isCurrentSession(currentSessionGeneration, detachSequence)) {
                return@withLock false
            }
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

    suspend fun exchange(
        report: ByteArray,
        minResponseBytes: Int,
        timeoutMillis: Long = RESPONSE_TIMEOUT_MILLIS,
        acceptResponse: (ByteArray) -> Boolean = { true },
    ): ByteArray? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = session ?: return@withLock null
            if (requireUniqueCandidate && !isCurrentSession(currentSessionGeneration, detachSequence)) {
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
                    if (acceptResponse(candidate)) return@withLock candidate
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

    /**
     * JA11 control writes that are known to restart USB must be followed by a physical detach,
     * a fresh uniquely resolved/opened session, and optional serial continuity when both serials
     * are available. Serial absence does not prevent the fresh session from being verified.
     */
    suspend fun awaitExpectedReplacementSession(
        previousGeneration: Long,
        previousDetachGeneration: Long,
        previousSerial: String?,
        timeoutMillis: Long = RECONNECT_TIMEOUT_MILLIS,
    ): Boolean {
        if (previousGeneration <= 0L) return false
        return withTimeoutOrNull(timeoutMillis) {
            state.first {
                detachSequence != previousDetachGeneration ||
                    supportedCandidateCount > 1 ||
                    it is Kt02h20ConnectionState.Error
            }
            if (detachSequence == previousDetachGeneration || supportedCandidateCount > 1 ||
                state.value is Kt02h20ConnectionState.Error
            ) return@withTimeoutOrNull false

            present.first { it && supportedCandidateCount > 0 }
            if (supportedCandidateCount != 1 || state.value is Kt02h20ConnectionState.Error) {
                return@withTimeoutOrNull false
            }
            if (state.value !is Kt02h20ConnectionState.Connected) connect()

            val replacementState = state.first {
                it is Kt02h20ConnectionState.Error || supportedCandidateCount > 1 ||
                    (it is Kt02h20ConnectionState.Connected &&
                    currentSessionGeneration != 0L &&
                    currentSessionGeneration != previousGeneration &&
                    detachSequence != previousDetachGeneration &&
                    supportedCandidateCount == 1 &&
                    isCurrentSession(currentSessionGeneration, detachSequence))
            }
            if (replacementState !is Kt02h20ConnectionState.Connected ||
                supportedCandidateCount != 1 ||
                !isCurrentSession(currentSessionGeneration, detachSequence)
            ) return@withTimeoutOrNull false
            val replacementSerial = connectedDeviceSerial?.trim()?.takeIf(String::isNotEmpty)
            val originalSerial = previousSerial?.trim()?.takeIf(String::isNotEmpty)
            originalSerial == null || replacementSerial == null || originalSerial == replacementSerial
        } ?: false
    }

    /**
     * JA11 Save has a late physical detach on qualified firmware. Observe USB state changes
     * until the bounded window expires instead of assuming a fixed delay means the old handle is
     * still valid. A detach requires a fresh replacement session before readback.
     */
    suspend fun awaitJa11SaveBoundary(
        previousGeneration: Long,
        previousDetachGeneration: Long,
        previousSerial: String?,
        observationMillis: Long = SAVE_DETACH_OBSERVATION_MILLIS,
    ): Boolean {
        if (previousGeneration <= 0L) return false
        withTimeoutOrNull(observationMillis) {
            state.first {
                detachSequence != previousDetachGeneration ||
                    currentSessionGeneration != previousGeneration ||
                    supportedCandidateCount != 1 ||
                    state.value !is Kt02h20ConnectionState.Connected
            }
        }
        return if (detachSequence != previousDetachGeneration) {
            awaitExpectedReplacementSession(
                previousGeneration = previousGeneration,
                previousDetachGeneration = previousDetachGeneration,
                previousSerial = previousSerial,
            )
        } else {
            isCurrentSession(previousGeneration, previousDetachGeneration)
        }
    }

    /** Waits for a late Save detach, then opens a fresh session for readback only. */
    suspend fun awaitLateJa11SaveReconnect(
        previousGeneration: Long,
        previousDetachGeneration: Long,
        previousSerial: String?,
        observationMillis: Long = LATE_SAVE_DETACH_OBSERVATION_MILLIS,
    ): Boolean {
        if (previousGeneration <= 0L || detachSequence != previousDetachGeneration) {
            return awaitExpectedReplacementSession(
                previousGeneration,
                previousDetachGeneration,
                previousSerial,
            )
        }
        val detached = withTimeoutOrNull(observationMillis) {
            state.first {
                detachSequence != previousDetachGeneration ||
                    supportedCandidateCount > 1 ||
                    it is Kt02h20ConnectionState.Error
            }
            detachSequence != previousDetachGeneration
        } ?: false
        if (!detached) return false
        return awaitExpectedReplacementSession(
            previousGeneration,
            previousDetachGeneration,
            previousSerial,
        )
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
        scope.launch {
            mutex.withLock {
                // Permission and attach broadcasts can both request an open for the same
                // UsbDevice. The first successful opener owns the session; later jobs must leave
                // it alone instead of closing and replacing a live handle.
                if (session != null) {
                    if (!requireUniqueCandidate || isCurrentSession(currentSessionGeneration, detachSequence)) {
                        mutableState.value = Kt02h20ConnectionState.Connected
                        return@withLock
                    }
                    closeSessionLocked()
                }
                val currentDevice = if (requireUniqueCandidate) currentDeviceForOpen(device) else device
                if (currentDevice == null) {
                    mutablePresent.value = supportedCandidateCount > 0
                    mutableState.value = candidateResolutionError()
                        ?: Kt02h20ConnectionState.Error("$deviceLabel is no longer available for USB open.")
                    return@withLock
                }
                if (requireUniqueCandidate && !usbManager.hasPermission(currentDevice)) {
                    mutableState.value = Kt02h20ConnectionState.PermissionRequired(
                        "Android USB permission is required for $deviceLabel. Approve it, then tap Connect to verify the DAC.",
                    )
                    return@withLock
                }
                val connection = runCatching { usbManager.openDevice(currentDevice) }.getOrNull()
                if (connection == null) {
                    mutableState.value = Kt02h20ConnectionState.Error(
                        "Android could not open the $deviceLabel USB device.",
                    )
                    return@withLock
                }
                val descriptor = runCatching { findHidInterface(currentDevice) }.getOrNull()
                val claimed = descriptor != null && runCatching {
                    connection.claimInterface(descriptor.usbInterface, true)
                }.getOrDefault(false)
                if (!claimed) {
                    connection.close()
                    mutableState.value = Kt02h20ConnectionState.Error(
                        "Android could not claim the $deviceLabel PEQ HID interface.",
                    )
                    return@withLock
                }
                val serial = sessionSerial(currentDevice, connection)
                session = UsbSession(
                    connection = connection,
                    usbInterface = descriptor.usbInterface,
                    endpointIn = descriptor.endpointIn,
                    endpointOut = descriptor.endpointOut,
                    productId = currentDevice.productId,
                    deviceName = currentDevice.deviceName,
                    serial = serial,
                    fingerprintKey = fingerprintKey(currentDevice, descriptor.usbInterface, serial),
                )
                lastSessionGeneration = nextSessionGeneration(lastSessionGeneration)
                currentSessionGeneration = lastSessionGeneration
                mutableState.value = Kt02h20ConnectionState.Connected
            }
        }
    }

    private fun findHidInterface(device: UsbDevice): HidInterface? {
        val interfaces = (0 until device.interfaceCount)
            .map(device::getInterface)
            .filter { it.interfaceClass == UsbConstants.USB_CLASS_HID && hidInterfaceMatcher(it) }
            .mapNotNull { intf ->
                val endpoints = (0 until intf.endpointCount).map(intf::getEndpoint)
                val inputEndpoints = endpoints.filter { endpoint ->
                    endpoint.direction == UsbConstants.USB_DIR_IN &&
                        endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT
                }
                val outputEndpoints = endpoints.filter { endpoint ->
                    endpoint.direction == UsbConstants.USB_DIR_OUT &&
                        endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT
                }
                if (requireUniqueCandidate) {
                    if (inputEndpoints.size == 1 && outputEndpoints.size == 1) {
                        HidInterface(intf, inputEndpoints.single(), outputEndpoints.single())
                    } else null
                } else if (inputEndpoints.isNotEmpty() && outputEndpoints.isNotEmpty()) {
                    HidInterface(intf, inputEndpoints.first(), outputEndpoints.first())
                } else null
            }
        return if (requireUniqueCandidate) interfaces.singleOrNull() else interfaces.firstOrNull()
    }

    private fun startPermissionFallback() {
        scope.launch {
            delay(PERMISSION_RESPONSE_TIMEOUT_MILLIS)
            if (mutableState.value !is Kt02h20ConnectionState.Connecting) return@launch
            val device = findDevice()
            when {
                device == null -> {
                    mutablePresent.value = supportedCandidateCount > 0
                    mutableState.value = candidateResolutionError()
                        ?: Kt02h20ConnectionState.Error("$deviceLabel disconnected while Android was requesting USB permission.")
                }
                usbManager.hasPermission(device) -> openAsync(device)
                else -> mutableState.value = Kt02h20ConnectionState.PermissionRequired(
                    "Android USB permission is required for $deviceLabel. Approve it, then tap Connect to verify the DAC.",
                )
            }
        }
    }

    private fun matchingDevices(): List<UsbDevice> = usbManager.deviceList.values.filter { it.matchesTarget() }

    private fun findDevice(): UsbDevice? {
        val candidates = matchingDevices()
        return if (requireUniqueCandidate) candidates.singleOrNull() else candidates.firstOrNull()
    }

    private fun currentDeviceForOpen(requested: UsbDevice): UsbDevice? {
        if (!requireUniqueCandidate) return requested.takeIf { it.matchesTarget() }
        return matchingDevices().singleOrNull()?.takeIf { it.deviceName == requested.deviceName }
    }

    private fun candidateResolutionError(): Kt02h20ConnectionState.Error? =
        if (requireUniqueCandidate && supportedCandidateCount > 1) {
            Kt02h20ConnectionState.Error(
                "More than one supported $deviceLabel is connected. Disconnect the extra device before connecting.",
            )
        } else null

    private fun failClosedForAmbiguousCandidates() {
        val message = candidateResolutionError()?.message ?: return
        scope.launch {
            mutex.withLock {
                closeSessionLocked()
                mutableState.value = Kt02h20ConnectionState.Error(message)
            }
        }
    }

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

    private fun UsbDevice.matchesTarget(): Boolean =
        this.vendorId == vendorId && this.productId in productIds && deviceIdentityMatcher(this)

    private fun sessionSerial(device: UsbDevice, connection: UsbDeviceConnection): String? {
        val deviceSerial = runCatching { device.serialNumber }.getOrNull()?.trim()?.takeIf(String::isNotEmpty)
        val connectionSerial = if (requireUniqueCandidate) {
            runCatching { connection.serial }.getOrNull()?.trim()?.takeIf(String::isNotEmpty)
        } else null
        return deviceSerial ?: connectionSerial
    }

    private fun fingerprintKey(device: UsbDevice, usbInterface: UsbInterface, serial: String?): String {
        val manufacturer = runCatching { device.manufacturerName }.getOrNull().orEmpty()
        val product = runCatching { device.productName }.getOrNull().orEmpty()
        return (listOf(
            "vid=${device.vendorId.toString(16)}",
            "pid=${device.productId.toString(16)}",
            "manufacturer=${manufacturer.trim()}",
            "product=${product.trim()}",
            "serial=${serial.orEmpty().trim()}",
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
        val deviceName: String,
        val serial: String?,
        val fingerprintKey: String,
    )

    private companion object {
        const val TRANSFER_TIMEOUT_MILLIS = 300
        const val RESPONSE_TIMEOUT_MILLIS = 700L
        const val READ_POLL_MILLIS = 80
        const val READ_RETRY_DELAY_MILLIS = 5L
        const val PERMISSION_RESPONSE_TIMEOUT_MILLIS = 10_000L
        const val REENUMERATION_OBSERVATION_MILLIS = 350L
        const val RECONNECT_TIMEOUT_MILLIS = 20_000L
        const val SAVE_DETACH_OBSERVATION_MILLIS = 1_500L
        const val LATE_SAVE_DETACH_OBSERVATION_MILLIS = 1_000L

        fun nextSessionGeneration(previous: Long): Long =
            if (previous == Long.MAX_VALUE) 1L else previous + 1L
    }
}
