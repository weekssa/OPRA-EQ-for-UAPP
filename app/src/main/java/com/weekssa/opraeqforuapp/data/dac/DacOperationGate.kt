package com.weekssa.opraeqforuapp.data.dac

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serialization boundary for every multi-step operation that owns one physical DAC session.
 *
 * Repositories receive this narrow gate instead of owning independent mutexes. Production gates
 * delegate to [DacSessionRepository], the ViewModel-scoped physical-session owner, so EQ and DEVICE
 * operations for the same DAC cannot interleave. Unit tests may use the local mutex-backed default.
 */
interface DacOperationGate {
    suspend fun <T> withExclusiveOperation(block: suspend () -> T): T
}

class MutexDacOperationGate : DacOperationGate {
    private val mutex = Mutex()

    override suspend fun <T> withExclusiveOperation(block: suspend () -> T): T =
        mutex.withLock { block() }
}

class BlackPearlSessionOperationGate(
    private val sessions: DacSessionRepository,
) : DacOperationGate {
    override suspend fun <T> withExclusiveOperation(block: suspend () -> T): T =
        sessions.withExclusiveBlackPearlOperation(block)
}

class FiioJa11SessionOperationGate(
    private val sessions: DacSessionRepository,
) : DacOperationGate {
    override suspend fun <T> withExclusiveOperation(block: suspend () -> T): T =
        sessions.withExclusiveFiioJa11Operation(block)
}
