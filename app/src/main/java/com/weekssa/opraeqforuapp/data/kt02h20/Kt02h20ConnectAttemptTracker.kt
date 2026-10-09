package com.weekssa.opraeqforuapp.data.kt02h20

/** One generation-fenced USB open or permission request owned by a single HID session. */
internal data class Kt02h20ConnectAttempt(
    val id: Long,
    val deviceName: String,
    val productId: Int,
    val detachGeneration: Long,
    val phase: Phase,
) {
    enum class Phase {
        WAITING_FOR_PERMISSION,
        OPENING,
    }

    fun matchesCurrentPermissionedDevice(
        currentDeviceName: String,
        currentProductId: Int,
        currentDetachGeneration: Long,
        permissionGranted: Boolean,
    ): Boolean =
        phase == Phase.OPENING &&
            deviceName == currentDeviceName &&
            productId == currentProductId &&
            detachGeneration == currentDetachGeneration &&
            permissionGranted
}

internal sealed interface Kt02h20PermissionResolution {
    data class Granted(val attempt: Kt02h20ConnectAttempt) : Kt02h20PermissionResolution
    data object Denied : Kt02h20PermissionResolution
    data object Stale : Kt02h20PermissionResolution
}

internal sealed interface Kt02h20PermissionFallbackResolution {
    data class Open(val attempt: Kt02h20ConnectAttempt) : Kt02h20PermissionFallbackResolution
    data object StillPending : Kt02h20PermissionFallbackResolution
    data object Retryable : Kt02h20PermissionFallbackResolution
    data object Disconnected : Kt02h20PermissionFallbackResolution
    data object DeviceChanged : Kt02h20PermissionFallbackResolution
    data object Stale : Kt02h20PermissionFallbackResolution
}

/** Serializes session publication against permission-attempt cancellation and detach handling. */
internal class Kt02h20SessionLifecycleGate {
    private val lock = Any()

    fun <T> withLock(block: () -> T): T = synchronized(lock, block)

    fun cancelAttempt(
        tracker: Kt02h20ConnectAttemptTracker,
        attemptId: Long,
        onCancelled: () -> Unit = {},
    ): Boolean = withLock {
        if (!tracker.cancel(attemptId)) return@withLock false
        onCancelled()
        true
    }

    fun commitAttempt(
        tracker: Kt02h20ConnectAttemptTracker,
        attempt: Kt02h20ConnectAttempt,
        canCommit: () -> Boolean,
        publishSession: () -> Unit,
    ): Boolean = withLock {
        if (!tracker.isCurrent(attempt) || !canCommit()) return@withLock false
        if (!tracker.finish(attempt.id)) return@withLock false
        publishSession()
        true
    }
}

/**
 * Fences delayed and duplicate Android permission callbacks from detached or superseded opens.
 * Device objects themselves are always reacquired from UsbManager after permission is granted.
 */
internal class Kt02h20ConnectAttemptTracker {
    private var nextId = 0L
    private var current: Kt02h20ConnectAttempt? = null

    @Synchronized
    fun begin(
        deviceName: String,
        productId: Int,
        detachGeneration: Long,
        permissionRequired: Boolean,
    ): Kt02h20ConnectAttempt {
        nextId = if (nextId == Long.MAX_VALUE) 1L else nextId + 1L
        return Kt02h20ConnectAttempt(
            id = nextId,
            deviceName = deviceName,
            productId = productId,
            detachGeneration = detachGeneration,
            phase = if (permissionRequired) {
                Kt02h20ConnectAttempt.Phase.WAITING_FOR_PERMISSION
            } else {
                Kt02h20ConnectAttempt.Phase.OPENING
            },
        ).also { current = it }
    }

    @Synchronized
    fun currentAttempt(): Kt02h20ConnectAttempt? = current

    @Synchronized
    fun isCurrent(attempt: Kt02h20ConnectAttempt): Boolean = current == attempt

    /** Resolves the ten-second UI fallback without racing a grant or accepting a stale endpoint. */
    @Synchronized
    fun resolvePermissionFallback(
        attemptId: Long,
        currentDeviceName: String?,
        currentProductId: Int?,
        currentDetachGeneration: Long,
        permissionGranted: Boolean,
        retainPendingPrompt: Boolean,
    ): Kt02h20PermissionFallbackResolution {
        val active = current ?: return Kt02h20PermissionFallbackResolution.Stale
        if (active.id != attemptId ||
            active.phase != Kt02h20ConnectAttempt.Phase.WAITING_FOR_PERMISSION ||
            active.detachGeneration != currentDetachGeneration
        ) {
            return Kt02h20PermissionFallbackResolution.Stale
        }
        if (currentDeviceName == null || currentProductId == null) {
            current = null
            return Kt02h20PermissionFallbackResolution.Disconnected
        }
        if (active.deviceName != currentDeviceName || active.productId != currentProductId) {
            current = null
            return Kt02h20PermissionFallbackResolution.DeviceChanged
        }
        if (permissionGranted) {
            return active.copy(phase = Kt02h20ConnectAttempt.Phase.OPENING)
                .also { current = it }
                .let(Kt02h20PermissionFallbackResolution::Open)
        }
        if (retainPendingPrompt) return Kt02h20PermissionFallbackResolution.StillPending
        current = null
        return Kt02h20PermissionFallbackResolution.Retryable
    }

    @Synchronized
    fun resolvePermissionCallback(
        requestId: Long,
        callbackDeviceName: String,
        callbackProductId: Int,
        currentDetachGeneration: Long,
        granted: Boolean,
    ): Kt02h20PermissionResolution {
        val active = current ?: return Kt02h20PermissionResolution.Stale
        if (active.id != requestId ||
            active.phase != Kt02h20ConnectAttempt.Phase.WAITING_FOR_PERMISSION ||
            active.deviceName != callbackDeviceName ||
            active.productId != callbackProductId ||
            active.detachGeneration != currentDetachGeneration
        ) {
            return Kt02h20PermissionResolution.Stale
        }
        if (!granted) {
            current = null
            return Kt02h20PermissionResolution.Denied
        }
        return active.copy(phase = Kt02h20ConnectAttempt.Phase.OPENING)
            .also { current = it }
            .let(Kt02h20PermissionResolution::Granted)
    }

    @Synchronized
    fun observePermissionGranted(
        attemptId: Long,
        currentDetachGeneration: Long,
    ): Kt02h20ConnectAttempt? {
        val active = current ?: return null
        if (active.id != attemptId ||
            active.phase != Kt02h20ConnectAttempt.Phase.WAITING_FOR_PERMISSION ||
            active.detachGeneration != currentDetachGeneration
        ) {
            return null
        }
        return active.copy(phase = Kt02h20ConnectAttempt.Phase.OPENING)
            .also { current = it }
    }

    @Synchronized
    fun invalidateForDetach(
        deviceName: String,
        productId: Int,
        currentDetachGeneration: Long,
    ): Boolean {
        val active = current ?: return false
        if (active.deviceName != deviceName ||
            active.productId != productId ||
            active.detachGeneration != currentDetachGeneration
        ) {
            return false
        }
        current = null
        return true
    }

    /** Retires any open or permission attempt made obsolete by a newer detach generation. */
    @Synchronized
    fun invalidateStaleAttempt(currentDetachGeneration: Long): Kt02h20ConnectAttempt? {
        val active = current ?: return null
        if (active.detachGeneration == currentDetachGeneration) return null
        current = null
        return active
    }

    @Synchronized
    fun cancel(attemptId: Long? = null): Boolean {
        val active = current ?: return false
        if (attemptId != null && active.id != attemptId) return false
        current = null
        return true
    }

    @Synchronized
    fun finish(attemptId: Long): Boolean {
        val active = current ?: return false
        if (active.id != attemptId) return false
        current = null
        return true
    }
}
