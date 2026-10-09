package com.weekssa.opraeqforuapp.data.kt02h20

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Test

class Kt02h20ConnectAttemptTrackerTest {
    @Test
    fun grantedPermissionMovesOnlyTheMatchingAttemptToOpeningOnce() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val waiting = tracker.begin(
            deviceName = "/dev/bus/usb/001/003",
            productId = 0x0102,
            detachGeneration = 4,
            permissionRequired = true,
        )

        val resolution = tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            granted = true,
        )

        assertThat(resolution).isEqualTo(
            Kt02h20PermissionResolution.Granted(waiting.copy(phase = Kt02h20ConnectAttempt.Phase.OPENING)),
        )
        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
    }

    @Test
    fun delayedPermissionRemainsPendingAndAResultAfterTheFallbackOpensOnce() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val waiting = tracker.begin("usb-delayed", 0x0102, 5, permissionRequired = true)

        assertThat(tracker.resolvePermissionFallback(
            attemptId = waiting.id,
            currentDeviceName = waiting.deviceName,
            currentProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            permissionGranted = false,
            retainPendingPrompt = true,
        )).isEqualTo(Kt02h20PermissionFallbackResolution.StillPending)
        assertThat(tracker.currentAttempt()).isEqualTo(waiting)

        val resolution = tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            granted = true,
        )
        assertThat(resolution).isEqualTo(
            Kt02h20PermissionResolution.Granted(waiting.copy(phase = Kt02h20ConnectAttempt.Phase.OPENING)),
        )
        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
    }

    @Test
    fun delayedPromptHasATerminalBoundAndRejectsAGrantAfterExpiry() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val waiting = tracker.begin("usb-delayed", 0x0102, 51, permissionRequired = true)

        assertThat(tracker.resolvePermissionFallback(
            attemptId = waiting.id,
            currentDeviceName = waiting.deviceName,
            currentProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            permissionGranted = false,
            retainPendingPrompt = true,
        )).isEqualTo(Kt02h20PermissionFallbackResolution.StillPending)
        assertThat(tracker.currentAttempt()).isEqualTo(waiting)

        assertThat(tracker.resolvePermissionFallback(
            attemptId = waiting.id,
            currentDeviceName = waiting.deviceName,
            currentProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            permissionGranted = false,
            retainPendingPrompt = false,
        )).isEqualTo(Kt02h20PermissionFallbackResolution.Retryable)
        assertThat(tracker.currentAttempt()).isNull()
        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
    }

    @Test
    fun permissionGrantedAtFallbackOpensFreshMatchingAttempt() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val waiting = tracker.begin("usb-current", 0x0102, 6, permissionRequired = true)

        assertThat(tracker.resolvePermissionFallback(
            attemptId = waiting.id,
            currentDeviceName = waiting.deviceName,
            currentProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            permissionGranted = true,
            retainPendingPrompt = false,
        )).isEqualTo(Kt02h20PermissionFallbackResolution.Open(
            waiting.copy(phase = Kt02h20ConnectAttempt.Phase.OPENING),
        ))
    }

    @Test
    fun ordinaryRetryFallbackEndsTheAttemptAndLateGrantIsStale() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val waiting = tracker.begin("usb-retry", 0x0102, 7, permissionRequired = true)

        assertThat(tracker.resolvePermissionFallback(
            attemptId = waiting.id,
            currentDeviceName = waiting.deviceName,
            currentProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            permissionGranted = false,
            retainPendingPrompt = false,
        )).isEqualTo(Kt02h20PermissionFallbackResolution.Retryable)
        assertThat(tracker.currentAttempt()).isNull()
        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
    }

    @Test
    fun fallbackRejectsDisconnectedAndDifferentCurrentEndpoints() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val disconnected = tracker.begin("usb-gone", 0x0102, 8, permissionRequired = true)

        assertThat(tracker.resolvePermissionFallback(
            attemptId = disconnected.id,
            currentDeviceName = null,
            currentProductId = null,
            currentDetachGeneration = disconnected.detachGeneration,
            permissionGranted = false,
            retainPendingPrompt = true,
        )).isEqualTo(Kt02h20PermissionFallbackResolution.Disconnected)

        val changed = tracker.begin("usb-old", 0x0102, 9, permissionRequired = true)
        assertThat(tracker.resolvePermissionFallback(
            attemptId = changed.id,
            currentDeviceName = "usb-new",
            currentProductId = 0x0101,
            currentDetachGeneration = changed.detachGeneration,
            permissionGranted = true,
            retainPendingPrompt = true,
        )).isEqualTo(Kt02h20PermissionFallbackResolution.DeviceChanged)
        assertThat(tracker.currentAttempt()).isNull()
    }

    @Test
    fun deniedPermissionConsumesTheAttemptAndLateGrantCannotReopenIt() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val waiting = tracker.begin("usb-current", 0x0102, 8, permissionRequired = true)

        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            granted = false,
        )).isEqualTo(Kt02h20PermissionResolution.Denied)
        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = waiting.detachGeneration,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
        assertThat(tracker.currentAttempt()).isNull()
    }

    @Test
    fun detachInvalidatesPendingPermissionAndItsDelayedCallback() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val waiting = tracker.begin("usb-detached", 0x0102, 11, permissionRequired = true)

        assertThat(tracker.invalidateForDetach("usb-detached", 0x0102, 11)).isTrue()
        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = 12,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
        assertThat(tracker.currentAttempt()).isNull()
    }

    @Test
    fun aNewDetachGenerationRetiresAnAlreadyQueuedReplacementOpen() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val opening = tracker.begin("usb-replacement", 0x0102, 12, permissionRequired = false)

        assertThat(tracker.invalidateStaleAttempt(currentDetachGeneration = 13)).isEqualTo(opening)
        assertThat(tracker.currentAttempt()).isNull()
        assertThat(tracker.invalidateStaleAttempt(currentDetachGeneration = 13)).isNull()
    }

    @Test
    fun obsoleteRequestDeviceGenerationAndPidCannotAcquireCurrentSession() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val waiting = tracker.begin("usb-003", 0x0102, 12, permissionRequired = true)

        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id - 1,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = 12,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = "usb-obsolete",
            callbackProductId = waiting.productId,
            currentDetachGeneration = 12,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = 0x0101,
            currentDetachGeneration = 12,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
        assertThat(tracker.resolvePermissionCallback(
            requestId = waiting.id,
            callbackDeviceName = waiting.deviceName,
            callbackProductId = waiting.productId,
            currentDetachGeneration = 13,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
    }

    @Test
    fun canceledOpeningAttemptRejectsACompletionFromItsObsoleteSession() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val opening = tracker.begin("usb-004", 0x0102, 18, permissionRequired = false)

        assertThat(tracker.isCurrent(opening)).isTrue()
        assertThat(tracker.cancel(opening.id)).isTrue()
        assertThat(tracker.isCurrent(opening)).isFalse()
        assertThat(tracker.finish(opening.id)).isFalse()
    }

    @Test
    fun replacementSessionMustResolveToTheCurrentPermissionedEndpoint() {
        val attempt = Kt02h20ConnectAttempt(
            id = 3,
            deviceName = "usb-current",
            productId = 0x0102,
            detachGeneration = 21,
            phase = Kt02h20ConnectAttempt.Phase.OPENING,
        )

        assertThat(attempt.matchesCurrentPermissionedDevice("usb-current", 0x0102, 21, true)).isTrue()
        assertThat(attempt.matchesCurrentPermissionedDevice("usb-obsolete", 0x0102, 21, true)).isFalse()
        assertThat(attempt.matchesCurrentPermissionedDevice("usb-current", 0x0101, 21, true)).isFalse()
        assertThat(attempt.matchesCurrentPermissionedDevice("usb-current", 0x0102, 22, true)).isFalse()
        assertThat(attempt.matchesCurrentPermissionedDevice("usb-current", 0x0102, 21, false)).isFalse()
    }

    @Test
    fun canceledPermissionAttemptCannotAcceptAQueuedCallbackAfterAnotherAttemptBegins() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val oldAttempt = tracker.begin("usb-old", 0x0102, 31, permissionRequired = true)
        assertThat(tracker.cancel(oldAttempt.id)).isTrue()
        val currentAttempt = tracker.begin("usb-current", 0x0102, 32, permissionRequired = true)

        assertThat(tracker.resolvePermissionCallback(
            requestId = oldAttempt.id,
            callbackDeviceName = oldAttempt.deviceName,
            callbackProductId = oldAttempt.productId,
            currentDetachGeneration = 32,
            granted = true,
        )).isEqualTo(Kt02h20PermissionResolution.Stale)
        assertThat(tracker.currentAttempt()).isEqualTo(currentAttempt)
    }

    @Test
    fun lifecycleGateSerializesSessionPublicationAgainstConcurrentCancellation() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val attempt = tracker.begin("usb-current", 0x0102, 41, permissionRequired = false)
        val gate = Kt02h20SessionLifecycleGate()
        val commitEntered = CountDownLatch(1)
        val allowCommit = CountDownLatch(1)
        val published = AtomicBoolean(false)
        val committed = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)
        val cancelInvocationStarted = CountDownLatch(1)
        val threadFailure = AtomicReference<Throwable?>(null)

        val commitThread = Thread {
            runCatching {
                committed.set(
                    gate.commitAttempt(
                        tracker = tracker,
                        attempt = attempt,
                        canCommit = { true },
                        publishSession = {
                            commitEntered.countDown()
                            check(allowCommit.await(3, TimeUnit.SECONDS))
                            published.set(true)
                        },
                    ),
                )
            }.onFailure(threadFailure::set)
        }
        commitThread.start()
        assertThat(commitEntered.await(3, TimeUnit.SECONDS)).isTrue()

        val cancelThread = Thread {
            runCatching {
                cancelInvocationStarted.countDown()
                cancelled.set(gate.cancelAttempt(tracker, attempt.id))
            }.onFailure(threadFailure::set)
        }
        cancelThread.start()
        assertThat(cancelInvocationStarted.await(3, TimeUnit.SECONDS)).isTrue()
        val blockedDeadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (cancelThread.state != Thread.State.BLOCKED && System.nanoTime() < blockedDeadlineNanos) {
            Thread.yield()
        }
        assertThat(cancelThread.state).isEqualTo(Thread.State.BLOCKED)
        allowCommit.countDown()
        commitThread.join(3_000)
        cancelThread.join(3_000)

        assertThat(commitThread.isAlive).isFalse()
        assertThat(cancelThread.isAlive).isFalse()
        assertThat(threadFailure.get()).isNull()
        assertThat(committed.get()).isTrue()
        assertThat(cancelled.get()).isFalse()
        assertThat(published.get()).isTrue()
        assertThat(tracker.currentAttempt()).isNull()
    }

    @Test
    fun cancellationThatWinsBeforeCommitPreventsSessionPublication() {
        val tracker = Kt02h20ConnectAttemptTracker()
        val attempt = tracker.begin("usb-current", 0x0102, 42, permissionRequired = false)
        val gate = Kt02h20SessionLifecycleGate()
        var published = false

        assertThat(gate.cancelAttempt(tracker, attempt.id)).isTrue()
        val committed = gate.commitAttempt(
            tracker = tracker,
            attempt = attempt,
            canCommit = { true },
            publishSession = { published = true },
        )

        assertThat(committed).isFalse()
        assertThat(published).isFalse()
        assertThat(tracker.currentAttempt()).isNull()
    }
}
