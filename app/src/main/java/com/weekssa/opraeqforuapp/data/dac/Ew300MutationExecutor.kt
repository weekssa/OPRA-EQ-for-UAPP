package com.weekssa.opraeqforuapp.data.dac

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns EW300 hardware mutations in the authoritative DAC-session scope.
 *
 * The initiating UI coroutine may disappear while Save re-enumerates the USB function. Launching
 * the transaction from [scope] makes that caller only an initiator/waiter: cancelling the caller
 * stops its wait, but does not cancel the already-started hardware transaction. The session owner
 * still cancels the operation when the owning ViewModel/repository is closed.
 */
internal class Ew300MutationExecutor(
    private val scope: CoroutineScope,
    private val operationMutex: Mutex,
    private val reconnectGate: Ew300ReconnectGate,
) {
    suspend fun <T> execute(block: suspend () -> T): T {
        val operation = scope.async {
            operationMutex.withLock {
                reconnectGate.beginMutation()
                try {
                    block()
                } finally {
                    reconnectGate.endMutation()
                }
            }
        }
        return operation.await()
    }
}
