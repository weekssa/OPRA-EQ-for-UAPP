package com.weekssa.opraeqforuapp.ui

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class FiioJa11RestartVerificationWatchdogTest {
    @Test
    fun permissionPromptPastFallbackRemainsPendingUntilFreshSessionCanBeVerified() = runBlocking {
        val connection = MutableStateFlow<Kt02h20ConnectionState>(Kt02h20ConnectionState.Disconnected)
        var pending = true
        var replacementCurrent = false
        var permissionFailureCalled = false
        var verifyCount = 0

        val watchdog = async {
            runFiioJa11RestartVerificationWatchdog(
                connectionStates = connection,
                timeoutMillis = 2_000,
                isPending = { pending },
                isReplacementSessionCurrent = { replacementCurrent },
                verifyReplacementSession = { verifyCount++; pending = false; true },
                onPermissionRequired = { permissionFailureCalled = true; pending = false },
                onConnectionError = { pending = false },
                onTimeout = { pending = false },
            )
        }

        connection.value = Kt02h20ConnectionState.PermissionRequired(
            "Waiting for Android USB permission.",
            retryAvailable = false,
        )
        yield()
        assertThat(pending).isTrue()
        assertThat(permissionFailureCalled).isFalse()
        assertThat(verifyCount).isEqualTo(0)

        replacementCurrent = true
        connection.value = Kt02h20ConnectionState.Connected
        withTimeout(1_000) { watchdog.await() }

        assertThat(verifyCount).isEqualTo(1)
        assertThat(permissionFailureCalled).isFalse()
        assertThat(pending).isFalse()
    }

    @Test
    fun explicitPermissionDenialIsTerminal() = runBlocking {
        val connection = MutableStateFlow<Kt02h20ConnectionState>(
            Kt02h20ConnectionState.PermissionRequired(
                "USB permission was denied.",
                retryAvailable = true,
            ),
        )
        var pending = true
        var permissionFailureCalled = false
        var verifyCount = 0

        runFiioJa11RestartVerificationWatchdog(
            connectionStates = connection,
            timeoutMillis = 2_000,
            isPending = { pending },
            isReplacementSessionCurrent = { true },
            verifyReplacementSession = { verifyCount++; true },
            onPermissionRequired = { permissionFailureCalled = true; pending = false },
            onConnectionError = { pending = false },
            onTimeout = { pending = false },
        )

        assertThat(permissionFailureCalled).isTrue()
        assertThat(verifyCount).isEqualTo(0)
        assertThat(pending).isFalse()
    }

    @Test
    fun writeReturningBeforeReconnectStillStartsVerificationAndStateChangesDoNotCancelIt() = runBlocking {
        val connection = MutableStateFlow<Kt02h20ConnectionState>(Kt02h20ConnectionState.Disconnected)
        val verificationStarted = CompletableDeferred<Unit>()
        val allowVerificationToFinish = CompletableDeferred<Boolean>()
        var pending = true
        var replacementCurrent = false
        var timeoutCalled = false

        val watchdog = async {
            runFiioJa11RestartVerificationWatchdog(
                connectionStates = connection,
                timeoutMillis = 2_000,
                isPending = { pending },
                isReplacementSessionCurrent = { replacementCurrent },
                verifyReplacementSession = {
                    verificationStarted.complete(Unit)
                    allowVerificationToFinish.await().also { pending = false }
                },
                onPermissionRequired = { pending = false },
                onConnectionError = { pending = false },
                onTimeout = { timeoutCalled = true; pending = false },
            )
        }

        replacementCurrent = true
        connection.value = Kt02h20ConnectionState.Connected
        withTimeout(1_000) { verificationStarted.await() }
        connection.value = Kt02h20ConnectionState.Connecting
        allowVerificationToFinish.complete(true)
        withTimeout(1_000) { watchdog.await() }

        assertThat(timeoutCalled).isFalse()
        assertThat(pending).isFalse()
    }

    @Test
    fun waitsForTheOriginalReplacementIdentityBeforeStartingReadback() = runBlocking {
        val connection = MutableStateFlow<Kt02h20ConnectionState>(Kt02h20ConnectionState.Connected)
        var replacementCurrent = false
        var pending = true
        var verifyCount = 0

        val watchdog = async {
            runFiioJa11RestartVerificationWatchdog(
                connectionStates = connection,
                timeoutMillis = 2_000,
                isPending = { pending },
                isReplacementSessionCurrent = { replacementCurrent },
                verifyReplacementSession = { verifyCount++; pending = false; true },
                onPermissionRequired = { pending = false },
                onConnectionError = { pending = false },
                onTimeout = { pending = false },
            )
        }

        connection.value = Kt02h20ConnectionState.Disconnected
        replacementCurrent = true
        connection.value = Kt02h20ConnectionState.Connected
        withTimeout(1_000) { watchdog.await() }

        assertThat(verifyCount).isEqualTo(1)
    }

    @Test
    fun timeoutRunsTerminalCallbackAndClearsPendingOperation() = runBlocking {
        val connection = MutableStateFlow<Kt02h20ConnectionState>(Kt02h20ConnectionState.Disconnected)
        var pending = true
        var timeoutCalled = false

        runFiioJa11RestartVerificationWatchdog(
            connectionStates = connection,
            timeoutMillis = 20,
            isPending = { pending },
            isReplacementSessionCurrent = { false },
            verifyReplacementSession = { error("No replacement session should be read") },
            onPermissionRequired = { error("Permission was not requested") },
            onConnectionError = { error("Connection did not fail") },
            onTimeout = { timeoutCalled = true; pending = false },
        )

        assertThat(timeoutCalled).isTrue()
        assertThat(pending).isFalse()
    }

    @Test
    fun connectedSessionWithoutReplacementIdentityDoesNotVerifyAndTimesOut() = runBlocking {
        val connection = MutableStateFlow<Kt02h20ConnectionState>(Kt02h20ConnectionState.Connected)
        var pending = true
        var verifyCount = 0
        var timeoutCalled = false

        runFiioJa11RestartVerificationWatchdog(
            connectionStates = connection,
            timeoutMillis = 20,
            isPending = { pending },
            isReplacementSessionCurrent = { false },
            verifyReplacementSession = { verifyCount++; true },
            onPermissionRequired = { error("No permission prompt was active") },
            onConnectionError = { error("The replacement session remained connected") },
            onTimeout = { timeoutCalled = true; pending = false },
        )

        assertThat(verifyCount).isEqualTo(0)
        assertThat(timeoutCalled).isTrue()
        assertThat(pending).isFalse()
    }

    @Test
    fun canceledRestartDoesNotVerifyAReplacementThatConnectsLater() = runBlocking {
        val connection = MutableStateFlow<Kt02h20ConnectionState>(Kt02h20ConnectionState.Disconnected)
        var pending = true
        var verifyCount = 0
        val watchdog = async {
            runFiioJa11RestartVerificationWatchdog(
                connectionStates = connection,
                timeoutMillis = 2_000,
                isPending = { pending },
                isReplacementSessionCurrent = { true },
                verifyReplacementSession = { verifyCount++; true },
                onPermissionRequired = { error("Permission was not denied") },
                onConnectionError = { error("The session did not fail") },
                onTimeout = { error("Canceled work must not time out") },
            )
        }

        yield()
        pending = false
        connection.value = Kt02h20ConnectionState.Connected
        withTimeout(1_000) { watchdog.await() }

        assertThat(verifyCount).isEqualTo(0)
    }
}
