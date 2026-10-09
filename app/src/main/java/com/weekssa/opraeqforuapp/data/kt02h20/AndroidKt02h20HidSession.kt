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
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11SessionIdentityContinuity
import com.weekssa.opraeqforuapp.domain.kt02h20.classifyJa11ReportWriteOutcome
import com.weekssa.opraeqforuapp.domain.kt02h20.fiioJa11SessionIdentityContinuity
import java.io.Closeable
import java.util.UUID
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
    data class PermissionRequired(
        val message: String,
        /** False only while the JA11 restart flow is waiting for its still-open permission prompt. */
        val retryAvailable: Boolean = true,
    ) : Kt02h20ConnectionState
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
    private val blockRetryWhilePermissionPending: Boolean = false,
    private val permissionRequester: ((UsbDevice, PendingIntent) -> Unit)? = null,
    private val permissionResponseTimeoutMillis: Long = PERMISSION_RESPONSE_TIMEOUT_MILLIS,
    /** Null preserves the shared transport's eligible callback until explicit invalidation. */
    private val permissionPromptMaxDurationMillis: Long? = null,
    /** Only JA11 opts into the opened-connection serial reader; other shared transports stay unchanged. */
    private val allowConnectionSerialFallback: Boolean = false,
    private val deviceSerialReader: (UsbDevice) -> String? = { it.serialNumber },
    private val connectionSerialReader: (UsbDeviceConnection) -> String? = { it.serial },
    /** JA11 refuses arbitrary selection when multiple supported units are attached. */
    private val requireUniqueTarget: Boolean = false,
) : Closeable {
    init {
        require(productIds.isNotEmpty()) { "At least one approved USB PID is required." }
        require(productIds.all { it in 0..0xFFFF }) { "USB PIDs must be 16-bit values." }
        require(permissionResponseTimeoutMillis >= 0L) { "Permission response timeout cannot be negative." }
        require(permissionPromptMaxDurationMillis == null ||
            permissionPromptMaxDurationMillis >= permissionResponseTimeoutMillis
        ) {
            "Permission prompt lifetime must be at least as long as its first fallback."
        }
    }

    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val sessionLifecycleGate = Kt02h20SessionLifecycleGate()
    private val connectAttempts = Kt02h20ConnectAttemptTracker()
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
    /** Invalidates an open that began before a JA11 attach/replacement was observed. */
    @Volatile
    private var targetAttachSequence: Long = 0L
    @Volatile
    private var permissionRequests: Long = 0L
    @Volatile
    private var expectedRestartTransactionToken: String? = null
    private var lastSessionGeneration: Long = 0L
    private var previousSessionIdentity: PreviousSessionIdentity? = null
    private var receiverRegistered = false
    // PendingIntent identity survives this session object's lifetime. A unique action prevents a
    // queued result from an obsolete instance colliding with a new tracker whose IDs restart at 1.
    private val permissionAction = "${appContext.packageName}.$permissionSuffix.USB_PERMISSION.${UUID.randomUUID()}"

    val sessionGeneration: Long
        get() = currentSessionGeneration

    val detachGeneration: Long
        get() = detachSequence

    val connectedProductId: Int?
        get() = session?.productId

    val deviceFingerprintKey: String?
        get() = session?.fingerprintKey

    /** Optional serial read from the same freshly opened USB descriptor/connection. */
    val deviceSerialIdentity: String?
        get() = session?.serialIdentity

    val permissionRequestCount: Long
        get() = permissionRequests

    /** Current count of all attached devices accepted by this exact transport matcher. */
    val targetDeviceCount: Int
        get() = currentTargetDevices().size

    /** Binds automatic post-reset permission/open attempts to the owning Mic/UAC transaction. */
    fun setExpectedRestartTransactionToken(token: String?): Boolean = sessionLifecycleGate.withLock {
        val normalized = token?.takeIf(String::isNotBlank)
        if (normalized == null) {
            expectedRestartTransactionToken = null
            true
        } else if (expectedRestartTransactionToken == null || expectedRestartTransactionToken == normalized) {
            val activeAttempt = connectAttempts.currentAttempt()
            if (activeAttempt != null && activeAttempt.transactionToken != normalized) {
                false
            } else {
                expectedRestartTransactionToken = normalized
                true
            }
        } else {
            false
        }
    }

    fun clearExpectedRestartTransactionToken(token: String) {
        sessionLifecycleGate.withLock {
            if (expectedRestartTransactionToken != token) return@withLock
            expectedRestartTransactionToken = null
            connectAttempts.currentAttempt()
                ?.takeIf { it.transactionToken == token }
                ?.let { attempt ->
                    if (connectAttempts.cancel(attempt.id) && session == null &&
                        state.value !is Kt02h20ConnectionState.Connected
                    ) {
                        mutableState.value = Kt02h20ConnectionState.Disconnected
                    }
                }
        }
    }

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
            state.value is Kt02h20ConnectionState.Connected &&
            (!requireUniqueTarget || (
                session?.targetAttachSequence == targetAttachSequence &&
                    session?.let(::matchesCurrentSessionDescriptor) == true
                ))

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.usbDevice() ?: return
            if (!device.matchesTarget()) return
            when (intent.action) {
                permissionAction -> {
                    val requestId = intent.getLongExtra(CONNECT_ATTEMPT_ID_EXTRA, INVALID_CONNECT_ATTEMPT_ID)
                    val granted = if (intent.hasExtra(UsbManager.EXTRA_PERMISSION_GRANTED)) {
                        intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    } else {
                        usbManager.hasPermission(device)
                    }
                    val resolution = sessionLifecycleGate.withLock {
                        val attemptBeforeResolution = connectAttempts.currentAttempt()
                        connectAttempts.resolvePermissionCallback(
                            requestId = requestId,
                            callbackDeviceName = device.deviceName,
                            callbackProductId = device.productId,
                            currentDetachGeneration = detachSequence,
                            granted = granted,
                        ).also { result ->
                            if (result is Kt02h20PermissionResolution.Denied &&
                                attemptBeforeResolution?.let { attempt ->
                                    attempt.id == requestId &&
                                        attempt.transactionToken == expectedRestartTransactionToken
                                } == true
                            ) {
                                mutableState.value = Kt02h20ConnectionState.PermissionRequired(
                                    "Android USB permission was denied for $deviceLabel. Grant access, then tap Retry connect.",
                                )
                            }
                        }
                    }
                    Ja11DiagnosticLog.eventForDevice(
                        deviceLabel,
                        "USB_PERMISSION_RESULT",
                        "pid" to device.productId,
                        "requestId" to requestId,
                        "transactionToken" to (resolution as? Kt02h20PermissionResolution.Granted)
                            ?.attempt?.transactionToken,
                        "granted" to granted,
                        "attemptCurrent" to (resolution !is Kt02h20PermissionResolution.Stale),
                    )
                    when (resolution) {
                        is Kt02h20PermissionResolution.Granted -> {
                            val currentDevice = findCurrentDevice(
                                resolution.attempt.deviceName,
                                resolution.attempt.productId,
                            )
                            val currentPermissionGranted = currentDevice?.let(usbManager::hasPermission) == true
                            val currentPermissionedDevice = currentDevice?.takeIf { currentPermissionGranted }
                            Ja11DiagnosticLog.eventForDevice(
                                deviceLabel,
                                "USB_PERMISSION_DEVICE_REFRESH",
                                "pid" to device.productId,
                                "currentDeviceFound" to (currentDevice != null),
                                "currentPermissionGranted" to currentPermissionGranted,
                            )
                            val anyTargetPresent = targetDeviceCount > 0
                            val shouldOpen = sessionLifecycleGate.withLock {
                                if (!connectAttempts.isCurrent(resolution.attempt)) {
                                    false
                                } else if (resolution.attempt.transactionToken != expectedRestartTransactionToken) {
                                    connectAttempts.cancel(resolution.attempt.id)
                                    false
                                } else if (resolution.attempt.detachGeneration != detachSequence) {
                                    connectAttempts.cancel(resolution.attempt.id)
                                    false
                                } else if (currentPermissionedDevice == null) {
                                    connectAttempts.cancel(resolution.attempt.id)
                                    mutablePresent.value = anyTargetPresent
                                    mutableState.value = if (requireUniqueTarget && targetDeviceCount > 1) {
                                        Kt02h20ConnectionState.Error(multipleTargetsMessage())
                                    } else if (anyTargetPresent) {
                                        Kt02h20ConnectionState.PermissionRequired(
                                            "The USB device changed while permission was being granted. Reconnect it, then try again.",
                                        )
                                    } else {
                                        Kt02h20ConnectionState.Disconnected
                                    }
                                    false
                                } else {
                                    mutableState.value = Kt02h20ConnectionState.Connecting
                                    true
                                }
                            }
                            if (shouldOpen) openAsync(resolution.attempt)
                        }
                        Kt02h20PermissionResolution.Denied -> Unit
                        Kt02h20PermissionResolution.Stale -> {
                            Ja11DiagnosticLog.eventForDevice(
                                deviceLabel,
                                "USB_PERMISSION_RESULT_IGNORED",
                                "pid" to device.productId,
                                "requestId" to requestId,
                                "reason" to "obsolete_attempt",
                            )
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    var detachedSession: UsbSession? = null
                    var permissionAttemptDetached = false
                    val detachGenerationBefore = sessionLifecycleGate.withLock {
                        val generationBeforeDetach = detachSequence
                        val currentSession = session
                        val indistinguishableCurrentDeviceStillAttached = requireUniqueTarget &&
                            currentTargetDevices().any { attached ->
                                attached.deviceName == device.deviceName &&
                                    attached.productId == device.productId
                            }
                        when {
                            indistinguishableCurrentDeviceStillAttached -> {
                                // The detached broadcast cannot be attributed to this serialless
                                // same-path/PID session while an indistinguishable candidate is
                                // still present. Do not treat it as expected-reset evidence.
                                Unit
                            }
                            currentSession != null && currentSession.matchesDetach(device.productId, device.deviceName) -> {
                                // Publish absence before closing so no operation can continue on the old handle.
                                detachedSession = currentSession
                                detachSequence = nextSessionGeneration(detachSequence)
                                connectAttempts.invalidateStaleAttempt(detachSequence)?.let { staleAttempt ->
                                    Ja11DiagnosticLog.eventForDevice(
                                        deviceLabel,
                                        "USB_CONNECT_ATTEMPT_INVALIDATED_ON_DETACH",
                                        "requestId" to staleAttempt.id,
                                        "attemptDetachGeneration" to staleAttempt.detachGeneration,
                                        "detachGeneration" to detachSequence,
                                    )
                                }
                                mutablePresent.value = false
                                mutableState.value = Kt02h20ConnectionState.Disconnected
                            }
                            currentSession == null && connectAttempts.invalidateForDetach(
                                deviceName = device.deviceName,
                                productId = device.productId,
                                currentDetachGeneration = detachSequence,
                            ) -> {
                                // A detach before the first open invalidates the permission/open attempt too.
                                permissionAttemptDetached = true
                                detachSequence = nextSessionGeneration(detachSequence)
                                connectAttempts.invalidateStaleAttempt(detachSequence)?.let { staleAttempt ->
                                    Ja11DiagnosticLog.eventForDevice(
                                        deviceLabel,
                                        "USB_CONNECT_ATTEMPT_INVALIDATED_ON_DETACH",
                                        "requestId" to staleAttempt.id,
                                        "attemptDetachGeneration" to staleAttempt.detachGeneration,
                                        "detachGeneration" to detachSequence,
                                    )
                                }
                                mutablePresent.value = false
                                mutableState.value = Kt02h20ConnectionState.Disconnected
                            }
                            else -> Unit
                        }
                        generationBeforeDetach
                    }
                    if (detachedSession != null) {
                        val detached = checkNotNull(detachedSession)
                        val detachAtEvent = detachSequence
                        Ja11DiagnosticLog.eventForDevice(
                            deviceLabel,
                            "USB_DETACH",
                            "pid" to device.productId,
                            "sessionGeneration" to detached.generation,
                            "detachGenerationBefore" to detachGenerationBefore,
                        )
                        scope.launch {
                            mutex.withLock {
                                if (session?.generation == detached.generation &&
                                    session?.detachGeneration == detached.detachGeneration &&
                                    detachSequence == detachAtEvent
                                ) {
                                    closeSessionLocked()
                                }
                            }
                            mutablePresent.value = targetDeviceCount > 0
                        }
                    } else if (permissionAttemptDetached) {
                        Ja11DiagnosticLog.eventForDevice(
                            deviceLabel,
                            "USB_DETACH_PERMISSION_PENDING",
                            "pid" to device.productId,
                            "detachGenerationBefore" to detachGenerationBefore,
                            "detachGenerationAfter" to detachSequence,
                        )
                    } else {
                        // A delayed detach for the old UAC PID must not invalidate a replacement session.
                        Ja11DiagnosticLog.eventForDevice(
                            deviceLabel,
                            "USB_DETACH_IGNORED",
                            "pid" to device.productId,
                            "currentPid" to session?.productId,
                            "currentSessionGeneration" to session?.generation,
                            "currentDetachGeneration" to detachSequence,
                            "reason" to if (requireUniqueTarget && currentTargetDevices().any { attached ->
                                attached.deviceName == device.deviceName && attached.productId == device.productId
                            }) "same_path_pid_still_attached" else "no_matching_live_session",
                        )
                        mutablePresent.value = targetDeviceCount > 0
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val currentDevice = findCurrentDevice(device.deviceName, device.productId)
                    val hasPermission = currentDevice?.let(usbManager::hasPermission) == true
                    val currentCandidateCount = targetDeviceCount
                    mutablePresent.value = targetDeviceCount > 0
                    val openingAttempt = sessionLifecycleGate.withLock {
                        if (requireUniqueTarget && currentDevice != null) {
                            targetAttachSequence = nextSessionGeneration(targetAttachSequence)
                        }
                        val candidateCount = targetDeviceCount
                        mutablePresent.value = candidateCount > 0
                        val currentSession = session?.takeIf {
                            it.detachGeneration == detachSequence &&
                                state.value is Kt02h20ConnectionState.Connected
                        }
                        if (requireUniqueTarget && currentSession != null) {
                            connectAttempts.cancel()
                            mutableState.value = Kt02h20ConnectionState.Error(
                                if (candidateCount > 1) multipleTargetsMessage()
                                else unexpectedAttachMessage(),
                            )
                            closeSession()
                            return@withLock null
                        }
                        if (requireUniqueTarget && candidateCount > 1) {
                            connectAttempts.cancel()
                            mutableState.value = Kt02h20ConnectionState.Error(multipleTargetsMessage())
                            closeSession()
                            return@withLock null
                        }
                        val attempt = connectAttempts.currentAttempt() ?: return@withLock null
                        if (attempt.deviceName != device.deviceName ||
                            attempt.productId != device.productId ||
                            attempt.detachGeneration != detachSequence ||
                            attempt.transactionToken != expectedRestartTransactionToken ||
                            !hasPermission
                        ) {
                            if (requireUniqueTarget && connectAttempts.isCurrent(attempt)) {
                                connectAttempts.cancel(attempt.id)
                                mutableState.value = Kt02h20ConnectionState.Error(
                                    "$deviceLabel changed while the USB session was opening. Disconnect and reconnect before trying again.",
                                )
                            }
                            return@withLock null
                        }
                        val opening = if (attempt.phase == Kt02h20ConnectAttempt.Phase.WAITING_FOR_PERMISSION) {
                            connectAttempts.observePermissionGranted(attempt.id, detachSequence)
                        } else {
                            attempt
                        }
                        if (opening != null) mutableState.value = Kt02h20ConnectionState.Connecting
                        opening
                    }
                    Ja11DiagnosticLog.eventForDevice(
                        deviceLabel,
                        "USB_ATTACH",
                        "pid" to device.productId,
                        "supportedCandidateCount" to currentCandidateCount,
                        "permissionGranted" to hasPermission,
                        "sessionGeneration" to currentSessionGeneration,
                        "detachGeneration" to detachSequence,
                        "attachSequence" to targetAttachSequence,
                        "transactionToken" to expectedRestartTransactionToken,
                    )
                    openingAttempt?.let(::openAsync)
                }
            }
        }
    }

    init {
        registerReceiver()
        mutablePresent.value = targetDeviceCount > 0
    }

    fun connect() = connect(automaticReconnect = false)

    /** Revalidates the observed disconnected state after a reconnect callback is dequeued. */
    fun connectAutomatically() = connect(automaticReconnect = true)

    private fun connect(automaticReconnect: Boolean) {
        sessionLifecycleGate.withLock {
            if (automaticReconnect && mutableState.value !is Kt02h20ConnectionState.Disconnected) {
                return@withLock
            }
            val candidateCount = targetDeviceCount
            if (requireUniqueTarget && candidateCount > 1) {
                mutablePresent.value = true
                mutableState.value = Kt02h20ConnectionState.Error(multipleTargetsMessage())
                return@withLock
            }
            val device = findDevice()
            if (device == null) {
                Ja11DiagnosticLog.eventForDevice(deviceLabel, "CONNECT_NO_DEVICE")
                mutablePresent.value = candidateCount > 0
                mutableState.value = Kt02h20ConnectionState.Error(
                    if (requireUniqueTarget && candidateCount > 1) multipleTargetsMessage()
                    else "$deviceLabel not detected. Connect the DAC by USB and try again.",
                )
                return@withLock
            }
            val hasPermission = usbManager.hasPermission(device)
            Ja11DiagnosticLog.eventForDevice(
                deviceLabel,
                "CONNECT_REQUEST",
                "pid" to device.productId,
                "permissionGranted" to hasPermission,
                "sessionGeneration" to currentSessionGeneration,
                "detachGeneration" to detachSequence,
            )
            mutablePresent.value = true
            // Connect is intentionally idempotent. During a USB re-enumeration the permission
            // callback, attach callback, and reconnect policy may all observe the transition.
            // Do not queue duplicate requests or competing open jobs while one attempt is active.
            val current = session
            if (current != null &&
                current.detachGeneration == detachSequence &&
                state.value is Kt02h20ConnectionState.Connected
            ) {
                mutableState.value = Kt02h20ConnectionState.Connected
                return@withLock
            }
            val connectionState = mutableState.value
            val activeAttempt = connectAttempts.currentAttempt()
            val waitingForJa11Permission =
                (connectionState as? Kt02h20ConnectionState.PermissionRequired)?.retryAvailable == false
            val retryingLatePermissionPrompt =
                (connectionState as? Kt02h20ConnectionState.PermissionRequired)?.retryAvailable == true &&
                    activeAttempt?.phase == Kt02h20ConnectAttempt.Phase.WAITING_FOR_PERMISSION
            if (connectionState is Kt02h20ConnectionState.Connecting ||
                waitingForJa11Permission ||
                (activeAttempt != null && !retryingLatePermissionPrompt)
            ) {
                return@withLock
            }
            if (retryingLatePermissionPrompt) {
                val superseded = checkNotNull(activeAttempt)
                connectAttempts.cancel(superseded.id)
                Ja11DiagnosticLog.eventForDevice(
                    deviceLabel,
                    "USB_PERMISSION_ATTEMPT_SUPERSEDED_BY_RETRY",
                    "pid" to superseded.productId,
                    "requestId" to superseded.id,
                )
            }
            mutableState.value = Kt02h20ConnectionState.Connecting
            val attempt = connectAttempts.begin(
                deviceName = device.deviceName,
                productId = device.productId,
                detachGeneration = detachSequence,
                permissionRequired = !hasPermission,
                transactionToken = expectedRestartTransactionToken,
            )
            if (hasPermission) {
                openAsync(attempt)
                return@withLock
            }
            val mutabilityFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val permissionIntent = PendingIntent.getBroadcast(
                appContext,
                attempt.id.toInt(),
                Intent(permissionAction)
                    .setPackage(appContext.packageName)
                    .putExtra(CONNECT_ATTEMPT_ID_EXTRA, attempt.id),
                PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag,
            )
            permissionRequests += 1L
            Ja11DiagnosticLog.eventForDevice(
                deviceLabel,
                "USB_PERMISSION_REQUESTED",
                "pid" to device.productId,
                "requestCount" to permissionRequests,
                "requestId" to attempt.id,
                "detachGeneration" to attempt.detachGeneration,
                "transactionToken" to attempt.transactionToken,
            )
            val requested = runCatching {
                val requester = permissionRequester
                if (requester == null) usbManager.requestPermission(device, permissionIntent)
                else requester(device, permissionIntent)
            }.isSuccess
            if (requested) {
                startPermissionFallback(attempt)
            } else {
                connectAttempts.finish(attempt.id)
                mutableState.value = Kt02h20ConnectionState.PermissionRequired(
                    "Android could not request USB permission for $deviceLabel. Try Connect again.",
                )
            }
        }
    }

    /** Invalidates an outstanding permission/open attempt after its owning operation terminates. */
    fun cancelPendingConnectAttempt(terminalErrorMessage: String? = null) {
        val attempt = sessionLifecycleGate.withLock {
            val currentAttempt = connectAttempts.currentAttempt()
            if (currentAttempt == null) {
                if (terminalErrorMessage != null && mutableState.value !is Kt02h20ConnectionState.Connected) {
                    mutableState.value = Kt02h20ConnectionState.Error(terminalErrorMessage)
                }
                return@withLock null
            }
            val cancelled = sessionLifecycleGate.cancelAttempt(connectAttempts, currentAttempt.id) {
                if (mutableState.value !is Kt02h20ConnectionState.Connected) {
                    when {
                        terminalErrorMessage != null ->
                            mutableState.value = Kt02h20ConnectionState.Error(terminalErrorMessage)
                        session == null -> mutableState.value = Kt02h20ConnectionState.Disconnected
                    }
                }
            }
            currentAttempt.takeIf { cancelled }
        }
        if (attempt == null) return
        Ja11DiagnosticLog.eventForDevice(
            deviceLabel,
            "USB_CONNECT_ATTEMPT_CANCELLED",
            "requestId" to attempt.id,
            "detachGeneration" to attempt.detachGeneration,
        )
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
            if (current == null || !isCurrentSession(expectedGeneration, expectedDetachGeneration)) {
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
            // Recheck immediately at the USB boundary in case another JA11 appeared after the
            // caller captured this session token but before the queued transfer started.
            if (!isCurrentSession(expectedGeneration, expectedDetachGeneration)) {
                return@withLock classifyJa11ReportWriteOutcome(
                    reportWasComplete = false,
                    sessionCurrentBeforeSend = false,
                    sessionCurrentAfterSettle = false,
                )
            }
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
            if (current == null || !isCurrentSession(expectedGeneration, expectedDetachGeneration)) {
                return@withLock null
            }
            if (!drainInput(current) { isCurrentSession(expectedGeneration, expectedDetachGeneration) }) {
                return@withLock null
            }
            if (!isCurrentSession(expectedGeneration, expectedDetachGeneration)) return@withLock null
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
                if (!isCurrentSession(expectedGeneration, expectedDetachGeneration)) return@withLock null
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

    private fun drainInput(current: UsbSession, canTransfer: () -> Boolean = { true }): Boolean {
        val buffer = ByteArray(maxOf(current.endpointIn.maxPacketSize, 64))
        repeat(8) {
            if (!canTransfer()) return false
            val read = runCatching {
                current.connection.bulkTransfer(current.endpointIn, buffer, buffer.size, 2)
            }.getOrDefault(-1)
            if (read <= 0) return true
        }
        return true
    }

    private fun openAsync(attempt: Kt02h20ConnectAttempt) {
        val attachSequenceAtRequest = targetAttachSequence
        Ja11DiagnosticLog.eventForDevice(
            deviceLabel,
            "USB_OPEN_REQUEST",
            "pid" to attempt.productId,
            "requestId" to attempt.id,
            "sessionGeneration" to currentSessionGeneration,
            "detachGeneration" to attempt.detachGeneration,
            "attachSequence" to attachSequenceAtRequest,
            "transactionToken" to attempt.transactionToken,
        )
        scope.launch {
            mutex.withLock {
                val shouldOpen = sessionLifecycleGate.withLock {
                    if (!connectAttempts.isCurrent(attempt)) {
                        false
                    } else if (attempt.transactionToken != expectedRestartTransactionToken) {
                        connectAttempts.cancel(attempt.id)
                        false
                    } else if (attempt.detachGeneration != detachSequence) {
                        connectAttempts.cancel(attempt.id)
                        if (mutableState.value is Kt02h20ConnectionState.Connecting) {
                            mutableState.value = Kt02h20ConnectionState.Disconnected
                        }
                        false
                    } else if (requireUniqueTarget && attachSequenceAtRequest != targetAttachSequence) {
                        false
                    } else {
                        // Permission and attach broadcasts can both request an open for the same
                        // UsbDevice. The first successful opener owns the session; later jobs leave
                        // it alone instead of closing and replacing a live handle.
                        val current = session
                        if (current != null &&
                            current.detachGeneration == detachSequence &&
                            current.deviceName == attempt.deviceName &&
                            current.productId == attempt.productId &&
                            state.value is Kt02h20ConnectionState.Connected
                        ) {
                            connectAttempts.finish(attempt.id)
                            mutableState.value = Kt02h20ConnectionState.Connected
                            false
                        } else {
                            true
                        }
                    }
                }
                if (!shouldOpen) return@withLock
                // Permission and attach broadcasts can both request an open for the same
                // attempt. A fresh UsbDevice descriptor is resolved for each open.
                val device = findCurrentPermissionedDevice(attempt)
                if (device == null) {
                    failConnectAttempt(
                        attempt,
                        "$deviceLabel changed or Android USB permission was unavailable before the session opened.",
                    )
                    return@withLock
                }
                closeSessionLocked()
                val connection = runCatching { usbManager.openDevice(device) }.getOrNull()
                if (connection == null) {
                    Ja11DiagnosticLog.eventForDevice(
                        deviceLabel,
                        "USB_OPEN_FAILED",
                        "pid" to device.productId,
                        "requestId" to attempt.id,
                        "reason" to "open_device_null",
                    )
                    failConnectAttempt(
                        attempt,
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
                        "requestId" to attempt.id,
                        "reason" to "hid_claim_failed",
                    )
                    failConnectAttempt(
                        attempt,
                        "Android could not claim the $deviceLabel PEQ HID interface.",
                    )
                    return@withLock
                }
                // Read the identity from the same descriptor/connection we just opened, before
                // taking the lifecycle gate's monitor. A serial string descriptor read is USB I/O.
                val openedIdentity = fingerprintKey(device, descriptor.usbInterface, connection)
                var latestDevice: UsbDevice? = null
                var openedSession: UsbSession? = null
                val committed = sessionLifecycleGate.commitAttempt(
                    tracker = connectAttempts,
                    attempt = attempt,
                    canCommit = {
                        if (requireUniqueTarget && attachSequenceAtRequest != targetAttachSequence) {
                            false
                        } else if (attempt.transactionToken != expectedRestartTransactionToken) {
                            connectAttempts.cancel(attempt.id)
                            false
                        } else if (attempt.detachGeneration != detachSequence) {
                            false
                        } else {
                            val currentDevice = findCurrentPermissionedDevice(attempt)
                            latestDevice = currentDevice
                            currentDevice != null && matchesOpenedDescriptor(
                                opened = descriptor,
                                openedSerial = openedIdentity.serialIdentity,
                                currentDevice = currentDevice,
                            )
                        }
                    },
                    publishSession = {
                        val committedDevice = checkNotNull(latestDevice)
                        lastSessionGeneration = nextSessionGeneration(lastSessionGeneration)
                        openedSession = UsbSession(
                            connection = connection,
                            usbInterface = descriptor.usbInterface,
                            endpointIn = descriptor.endpointIn,
                            endpointOut = descriptor.endpointOut,
                            productId = committedDevice.productId,
                            fingerprintKey = openedIdentity.fingerprintKey,
                            serialIdentity = openedIdentity.serialIdentity,
                            serialSource = openedIdentity.serialSource,
                            connectionSerialStatus = openedIdentity.connectionSerialStatus,
                            deviceName = committedDevice.deviceName,
                            generation = lastSessionGeneration,
                            detachGeneration = detachSequence,
                            targetAttachSequence = attachSequenceAtRequest,
                        )
                        session = openedSession
                        currentSessionGeneration = lastSessionGeneration
                        mutableState.value = Kt02h20ConnectionState.Connected
                    },
                )
                if (!committed) {
                    runCatching { connection.releaseInterface(descriptor.usbInterface) }
                    runCatching { connection.close() }
                    if (hasNewerPermissionedOpenForCurrentTarget(attempt, attachSequenceAtRequest)) {
                        Ja11DiagnosticLog.eventForDevice(
                            deviceLabel,
                            "USB_OPEN_REJECTED_STALE_ATTACH",
                            "pid" to attempt.productId,
                            "requestId" to attempt.id,
                            "requestAttachSequence" to attachSequenceAtRequest,
                            "currentAttachSequence" to targetAttachSequence,
                        )
                    } else {
                        failConnectAttempt(
                            attempt,
                            "$deviceLabel changed or the connection was cancelled before its fresh USB session could be published.",
                        )
                    }
                    return@withLock
                }
                val established = checkNotNull(openedSession)
                Ja11DiagnosticLog.eventForDevice(
                    deviceLabel,
                    "USB_SESSION_OPENED",
                    "pid" to established.productId,
                    "supportedCandidateCount" to targetDeviceCount,
                    "requestId" to attempt.id,
                    "sessionGeneration" to established.generation,
                    "detachGeneration" to established.detachGeneration,
                    "transactionToken" to attempt.transactionToken,
                )
                if (allowConnectionSerialFallback) {
                    val previousIdentity = previousSessionIdentity
                    val continuity = fiioJa11SessionIdentityContinuity(
                        hasPreviousSession = previousIdentity != null,
                        previousSerialIdentity = previousIdentity?.serialIdentity,
                        currentSerialIdentity = established.serialIdentity,
                    )
                    Ja11DiagnosticLog.eventForDevice(
                        deviceLabel,
                        "USB_SESSION_IDENTITY_CONTINUITY",
                        "previousSessionGeneration" to previousIdentity?.generation,
                        "sessionGeneration" to established.generation,
                        "previousSerialSource" to previousIdentity?.serialSource,
                        "serialSource" to established.serialSource,
                        "previousConnectionSerialStatus" to previousIdentity?.connectionSerialStatus,
                        "connectionSerialStatus" to established.connectionSerialStatus,
                        "serialAvailable" to (established.serialIdentity != null),
                        "serialMatches" to (continuity ==
                            FiioJa11SessionIdentityContinuity.SAME_DEVICE_SERIAL_MATCHED),
                        "supportedCandidateCount" to targetDeviceCount,
                        "continuity" to continuity.name,
                    )
                    previousSessionIdentity = null
                }
            }
        }
    }

    private fun failConnectAttempt(attempt: Kt02h20ConnectAttempt, message: String) {
        val failed = sessionLifecycleGate.withLock {
            if (!connectAttempts.isCurrent(attempt)) {
                false
            } else if (attempt.transactionToken != expectedRestartTransactionToken) {
                connectAttempts.cancel(attempt.id)
                false
            } else if (!connectAttempts.finish(attempt.id)) {
                false
            } else {
                mutableState.value = Kt02h20ConnectionState.Error(message)
                true
            }
        }
        if (!failed) return
        Ja11DiagnosticLog.eventForDevice(
            deviceLabel,
            "USB_OPEN_FAILED",
            "pid" to attempt.productId,
            "requestId" to attempt.id,
            "transactionToken" to attempt.transactionToken,
            "reason" to "open_attempt_failed",
        )
    }

    private fun findCurrentDevice(deviceName: String, productId: Int): UsbDevice? =
        currentTargetDevices().let { candidates ->
            if (requireUniqueTarget && candidates.size != 1) return null
            candidates.firstOrNull { device ->
                device.deviceName == deviceName &&
                    device.productId == productId &&
                    device.matchesTarget()
            }
        }

    private fun findHidInterface(device: UsbDevice): HidInterface? {
        val validInterfaces = (0 until device.interfaceCount)
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
                val input = if (requireUniqueTarget) inputEndpoints.singleOrNull() else inputEndpoints.firstOrNull()
                val output = if (requireUniqueTarget) outputEndpoints.singleOrNull() else outputEndpoints.firstOrNull()
                if (input != null && output != null) HidInterface(intf, input, output) else null
            }
        return if (requireUniqueTarget) validInterfaces.singleOrNull() else validInterfaces.firstOrNull()
    }

    private fun findCurrentPermissionedDevice(attempt: Kt02h20ConnectAttempt): UsbDevice? =
        findCurrentDevice(attempt.deviceName, attempt.productId)?.takeIf { device ->
            attempt.matchesCurrentPermissionedDevice(
                currentDeviceName = device.deviceName,
                currentProductId = device.productId,
                currentDetachGeneration = detachSequence,
                permissionGranted = usbManager.hasPermission(device),
            )
        }

    /**
     * Keeps an opened JA11 handle bound to the sole current permissioned descriptor. Device name
     * here is a live-session binding only; it is never treated as proof of same-unit continuity.
     */
    private fun matchesCurrentSessionDescriptor(current: UsbSession): Boolean {
        if (requireUniqueTarget && current.targetAttachSequence != targetAttachSequence) return false
        val candidate = currentTargetDevices().singleOrNull() ?: return false
        if (candidate.deviceName != current.deviceName ||
            candidate.productId != current.productId ||
            !usbManager.hasPermission(candidate)
        ) {
            return false
        }
        val candidateSerial = readUsableDeviceSerial(candidate)
        if (current.serialIdentity != null && candidateSerial != null && current.serialIdentity != candidateSerial) {
            return false
        }
        val currentDescriptor = findHidInterface(candidate) ?: return false
        return currentDescriptor.matches(current)
    }

    /** Validates session path, selected HID endpoints, and optional serial continuity. */
    private fun matchesOpenedDescriptor(
        opened: HidInterface,
        openedSerial: String?,
        currentDevice: UsbDevice,
    ): Boolean {
        val currentDescriptor = findHidInterface(currentDevice) ?: return false
        if (!opened.matches(currentDescriptor)) return false
        val currentSerial = readUsableDeviceSerial(currentDevice)
        return openedSerial == null || currentSerial == null || openedSerial == currentSerial
    }

    /** Keeps an attach-triggered fresh open alive while retiring its older in-flight opener. */
    private fun hasNewerPermissionedOpenForCurrentTarget(
        attempt: Kt02h20ConnectAttempt,
        requestAttachSequence: Long,
    ): Boolean {
        if (!requireUniqueTarget || requestAttachSequence == targetAttachSequence ||
            !connectAttempts.isCurrent(attempt)
        ) {
            return false
        }
        val candidate = currentTargetDevices().singleOrNull() ?: return false
        return candidate.deviceName == attempt.deviceName &&
            candidate.productId == attempt.productId &&
            usbManager.hasPermission(candidate)
    }

    private fun HidInterface.matches(current: UsbSession): Boolean =
        usbInterface.id == current.usbInterface.id &&
            endpointIn.address == current.endpointIn.address &&
            endpointOut.address == current.endpointOut.address

    private fun HidInterface.matches(other: HidInterface): Boolean =
        usbInterface.id == other.usbInterface.id &&
            endpointIn.address == other.endpointIn.address &&
            endpointOut.address == other.endpointOut.address

    private fun readUsableDeviceSerial(device: UsbDevice): String? =
        runCatching { deviceSerialReader(device) }
            .getOrNull()
            ?.trim()
            ?.takeIf(String::isNotBlank)

    private fun startPermissionFallback(attempt: Kt02h20ConnectAttempt) {
        scope.launch {
            delay(permissionResponseTimeoutMillis)
            val firstFallback = resolvePermissionFallback(
                attempt = attempt,
                retainPendingPrompt = true,
            )
            if (firstFallback != Kt02h20PermissionFallbackResolution.StillPending) return@launch

            // JA11 bounds the prompt to its restart-verification deadline. Other shared-session
            // transports retain the old request until grant, explicit Retry, detach, cancellation,
            // or session close so a late Android callback remains compatible with prior behavior.
            val hardDeadline = permissionPromptMaxDurationMillis ?: return@launch
            delay(hardDeadline - permissionResponseTimeoutMillis)
            resolvePermissionFallback(attempt = attempt, retainPendingPrompt = false)
        }
    }

    private fun resolvePermissionFallback(
        attempt: Kt02h20ConnectAttempt,
        retainPendingPrompt: Boolean,
    ): Kt02h20PermissionFallbackResolution {
        val fallback = sessionLifecycleGate.withLock {
            val currentDevice = findCurrentDevice(attempt.deviceName, attempt.productId)
            connectAttempts.resolvePermissionFallback(
                attemptId = attempt.id,
                currentDeviceName = currentDevice?.deviceName,
                currentProductId = currentDevice?.productId,
                currentDetachGeneration = detachSequence,
                permissionGranted = currentDevice?.let(usbManager::hasPermission) == true,
                retainPendingPrompt = retainPendingPrompt,
            ).also { result ->
                when (result) {
                    Kt02h20PermissionFallbackResolution.Stale -> Unit
                    Kt02h20PermissionFallbackResolution.Disconnected -> {
                        mutablePresent.value = targetDeviceCount > 0
                        mutableState.value = Kt02h20ConnectionState.Error(
                            "$deviceLabel disconnected while Android was requesting USB permission.",
                        )
                    }
                    Kt02h20PermissionFallbackResolution.DeviceChanged -> {
                        mutablePresent.value = targetDeviceCount > 0
                        mutableState.value = Kt02h20ConnectionState.PermissionRequired(
                            "The USB device changed while Android was requesting permission. Reconnect it, then try again.",
                        )
                    }
                    is Kt02h20PermissionFallbackResolution.Open ->
                        mutableState.value = Kt02h20ConnectionState.Connecting
                    Kt02h20PermissionFallbackResolution.StillPending ->
                        mutableState.value = Kt02h20ConnectionState.PermissionRequired(
                            if (blockRetryWhilePermissionPending) {
                                "Waiting for Android USB permission for $deviceLabel. Approve the system prompt to continue."
                            } else {
                                "Android USB permission is taking longer than expected for $deviceLabel. Approve the system prompt or tap Connect to retry."
                            },
                            retryAvailable = !blockRetryWhilePermissionPending,
                        )
                    Kt02h20PermissionFallbackResolution.Retryable ->
                        mutableState.value = Kt02h20ConnectionState.PermissionRequired(
                            "Android USB permission is required for $deviceLabel. Approve it, then tap Connect to try again.",
                        )
                }
            }
        }
        if (fallback is Kt02h20PermissionFallbackResolution.Open) openAsync(fallback.attempt)
        return fallback
    }

    private fun currentTargetDevices(): List<UsbDevice> = usbManager.deviceList.values.filter { it.matchesTarget() }

    private fun findDevice(): UsbDevice? {
        val candidates = currentTargetDevices()
        return if (requireUniqueTarget) candidates.singleOrNull() else candidates.firstOrNull()
    }

    private fun multipleTargetsMessage(): String =
        "More than one supported $deviceLabel is connected. Disconnect the extra device, then try again."

    private fun unexpectedAttachMessage(): String =
        "A new $deviceLabel appeared before the current USB session disconnected. Disconnect and reconnect before trying again."

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
        if (allowConnectionSerialFallback) {
            previousSessionIdentity = PreviousSessionIdentity(
                generation = current.generation,
                serialIdentity = current.serialIdentity,
                serialSource = current.serialSource,
                connectionSerialStatus = current.connectionSerialStatus,
            )
        }
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
        sessionLifecycleGate.withLock {
            connectAttempts.cancel()
            mutableState.value = Kt02h20ConnectionState.Disconnected
        }
        closeSession()
    }

    private fun UsbDevice.matchesTarget(): Boolean =
        this.vendorId == vendorId && this.productId in productIds && deviceIdentityMatcher(this)

    private fun fingerprintKey(
        device: UsbDevice,
        usbInterface: UsbInterface,
        connection: UsbDeviceConnection,
    ): UsbIdentityFingerprint {
        val manufacturer = runCatching { device.manufacturerName }.getOrNull().orEmpty()
        val product = runCatching { device.productName }.getOrNull().orEmpty()
        var serialStatus = "OTHER_EXCEPTION"
        val serial = try {
            deviceSerialReader(device).also { value ->
                serialStatus = when {
                    value == null -> "READABLE_NULL"
                    value.isBlank() -> "READABLE_BLANK"
                    else -> "READABLE_NONBLANK"
                }
            }
        } catch (_: SecurityException) {
            serialStatus = "SECURITY_EXCEPTION"
            null
        } catch (_: RuntimeException) {
            serialStatus = "OTHER_EXCEPTION"
            null
        }
        var connectionSerialStatus = "NOT_CHECKED"
        val connectionSerial = if (allowConnectionSerialFallback && serialStatus == "READABLE_NULL") {
            try {
                connectionSerialReader(connection).also { value ->
                    connectionSerialStatus = when {
                        value == null -> "READABLE_NULL"
                        value.isBlank() -> "READABLE_BLANK"
                        else -> "READABLE_NONBLANK"
                    }
                }
            } catch (_: SecurityException) {
                connectionSerialStatus = "SECURITY_EXCEPTION"
                null
            } catch (_: RuntimeException) {
                connectionSerialStatus = "OTHER_EXCEPTION"
                null
            }
        } else {
            null
        }
        val serialSource = when {
            serialStatus == "READABLE_NONBLANK" -> "USB_DEVICE"
            connectionSerialStatus == "READABLE_NONBLANK" -> "USB_CONNECTION"
            else -> "NONE"
        }
        val effectiveSerial = when (serialSource) {
            "USB_DEVICE" -> serial
            "USB_CONNECTION" -> connectionSerial
            else -> null
        }
        Ja11DiagnosticLog.eventForDevice(
            deviceLabel,
            "USB_IDENTITY_DESCRIPTOR_STATUS",
            "permissionGranted" to usbManager.hasPermission(device),
            "serialStatus" to serialStatus,
            "connectionSerialStatus" to connectionSerialStatus,
            "serialSource" to serialSource,
            "productId" to device.productId,
        )
        val fingerprintKey = (listOf(
            "vid=${device.vendorId.toString(16)}",
            "pid=${device.productId.toString(16)}",
            "manufacturer=${manufacturer.trim()}",
            "product=${product.trim()}",
            "serial=${effectiveSerial.orEmpty().trim()}",
        ) + additionalFingerprintFields(device).map { (key, value) -> "$key=${value.trim()}" } +
            "interface=${usbInterface.id}").joinToString("|")
        return UsbIdentityFingerprint(
            fingerprintKey = fingerprintKey,
            serialIdentity = effectiveSerial?.trim()?.takeIf(String::isNotBlank),
            serialSource = serialSource,
            connectionSerialStatus = connectionSerialStatus,
        )
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

    private data class UsbIdentityFingerprint(
        val fingerprintKey: String,
        val serialIdentity: String?,
        val serialSource: String,
        val connectionSerialStatus: String,
    )

    private data class PreviousSessionIdentity(
        val generation: Long,
        val serialIdentity: String?,
        val serialSource: String,
        val connectionSerialStatus: String,
    )

    private data class UsbSession(
        val connection: UsbDeviceConnection,
        val usbInterface: UsbInterface,
        val endpointIn: UsbEndpoint,
        val endpointOut: UsbEndpoint,
        val productId: Int,
        val fingerprintKey: String,
        val serialIdentity: String?,
        val serialSource: String,
        val connectionSerialStatus: String,
        val deviceName: String,
        val generation: Long,
        val detachGeneration: Long,
        val targetAttachSequence: Long,
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
        const val CONNECT_ATTEMPT_ID_EXTRA = "connectAttemptId"
        const val INVALID_CONNECT_ATTEMPT_ID = -1L

        fun nextSessionGeneration(previous: Long): Long =
            if (previous == Long.MAX_VALUE) 1L else previous + 1L
    }
}
