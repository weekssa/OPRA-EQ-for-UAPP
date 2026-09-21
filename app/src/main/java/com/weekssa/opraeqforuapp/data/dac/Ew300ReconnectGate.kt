package com.weekssa.opraeqforuapp.data.dac

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared mutation boundary for the EW300 session owner and its automatic reconnect observer.
 *
 * Android may report the USB function as detached and attached while Save is re-enumerating the
 * DSP. Before Save has been accepted, reconnecting is unsafe: a replacement UsbDevice may not
 * yet have permission and connect() would legitimately ask Android for it. Once Save has been
 * sent, reconnect is allowed so the normal replacement-session path can continue to read back.
 */
class Ew300ReconnectGate {
    private val mutableAutomaticReconnectAllowed = MutableStateFlow(true)
    val automaticReconnectAllowed: StateFlow<Boolean> = mutableAutomaticReconnectAllowed.asStateFlow()
    private var mutationDepth = 0
    private var replacementReconnectReleased = false

    @Synchronized
    fun beginMutation() {
        mutationDepth += 1
        replacementReconnectReleased = false
        mutableAutomaticReconnectAllowed.value = false
    }

    @Synchronized
    fun markSaveSent() {
        if (mutationDepth > 0) {
            replacementReconnectReleased = true
            mutableAutomaticReconnectAllowed.value = true
        }
    }

    @Synchronized
    fun canAutomaticReconnect(): Boolean = mutationDepth == 0 || replacementReconnectReleased

    @Synchronized
    fun endMutation() {
        mutationDepth = (mutationDepth - 1).coerceAtLeast(0)
        if (mutationDepth == 0) {
            replacementReconnectReleased = false
            mutableAutomaticReconnectAllowed.value = true
        }
    }

    @Synchronized
    internal fun snapshot(): Snapshot = Snapshot(mutationDepth, replacementReconnectReleased)

    internal data class Snapshot(
        val mutationDepth: Int,
        val replacementReconnectReleased: Boolean,
    )
}
