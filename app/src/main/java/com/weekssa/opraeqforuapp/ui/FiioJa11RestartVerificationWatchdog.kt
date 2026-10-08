package com.weekssa.opraeqforuapp.ui

import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/** Keeps restart verification owned until it succeeds, fails safely, or reaches its deadline. */
internal suspend fun runFiioJa11RestartVerificationWatchdog(
    connectionStates: StateFlow<Kt02h20ConnectionState>,
    timeoutMillis: Long,
    isPending: () -> Boolean,
    isReplacementSessionCurrent: () -> Boolean,
    verifyReplacementSession: suspend () -> Boolean,
    onPermissionRequired: () -> Unit,
    onConnectionError: () -> Unit,
    onTimeout: () -> Unit,
) {
    val completed = withTimeoutOrNull<Boolean>(timeoutMillis) {
        while (isPending()) {
            val state = connectionStates.first { connection ->
                connection is Kt02h20ConnectionState.PermissionRequired ||
                    connection is Kt02h20ConnectionState.Error ||
                    (connection === Kt02h20ConnectionState.Connected && isReplacementSessionCurrent())
            }
            if (!isPending()) return@withTimeoutOrNull true

            when (state) {
                is Kt02h20ConnectionState.PermissionRequired -> {
                    onPermissionRequired()
                    return@withTimeoutOrNull true
                }
                is Kt02h20ConnectionState.Error -> {
                    onConnectionError()
                    return@withTimeoutOrNull true
                }
                Kt02h20ConnectionState.Connected -> {
                    if (verifyReplacementSession()) return@withTimeoutOrNull true
                    if (isPending()) delay(RESTART_VERIFICATION_RETRY_DELAY_MILLIS)
                }
                else -> Unit
            }
        }
        true
    }

    if (completed == null && isPending()) onTimeout()
}

private const val RESTART_VERIFICATION_RETRY_DELAY_MILLIS = 150L
