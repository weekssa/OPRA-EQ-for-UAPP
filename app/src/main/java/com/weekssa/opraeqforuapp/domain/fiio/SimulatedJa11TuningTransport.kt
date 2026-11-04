package com.weekssa.opraeqforuapp.domain.fiio

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serialization boundary for tuning operations.
 *
 * The shape mirrors the repository's device operation gate so a future hardware transport can be
 * wrapped by the same exclusive-operation contract instead of inventing a second concurrency model.
 */
interface Ja11TuningExclusivity {
    suspend fun <T> withExclusiveOperation(block: suspend () -> T): T
}

class MutexJa11TuningExclusivity : Ja11TuningExclusivity {
    private val mutex = Mutex()

    override suspend fun <T> withExclusiveOperation(block: suspend () -> T): T =
        mutex.withLock { block() }
}

sealed interface Ja11TuningReadResult {
    data class Success(val snapshot: Ja11TuningSnapshot) : Ja11TuningReadResult
    data object PermissionDenied : Ja11TuningReadResult
    data class Offline(val reason: String) : Ja11TuningReadResult
    data class Failure(val reason: String) : Ja11TuningReadResult
}

sealed interface Ja11TuningWriteResult {
    data class Accepted(val sessionGeneration: Long) : Ja11TuningWriteResult
    data object PermissionDenied : Ja11TuningWriteResult
    data class DisconnectedBeforeWrite(val reason: String) : Ja11TuningWriteResult
    data class DisconnectedDuringWrite(val reason: String) : Ja11TuningWriteResult
    data class TimedOut(val reason: String) : Ja11TuningWriteResult
    data class Rejected(val reason: String) : Ja11TuningWriteResult
}

/**
 * Transport boundary for tuning state. [isSimulated] must be true for every non-hardware
 * implementation; the controller refuses hardware writes unless the capability model establishes
 * both a read and a write path.
 */
interface Ja11TuningTransport {
    val isSimulated: Boolean

    suspend fun read(): Ja11TuningReadResult

    suspend fun write(values: Ja11TuningValues): Ja11TuningWriteResult
}

/**
 * Operations that exist only in the simulated prototype: committing a simulated startup value and
 * simulating unplug/replug. A hardware transport must not implement this interface until the
 * device's own tuning persistence and reconnect semantics are independently established.
 */
interface Ja11TuningPrototypeControls {
    fun saveStartupDefaults(): Boolean
    fun simulateReconnect()
}

sealed interface Ja11TuningSaveOutcome {
    data class Saved(val snapshot: Ja11TuningSnapshot) : Ja11TuningSaveOutcome
    data object Unsupported : Ja11TuningSaveOutcome
    data object PermissionDenied : Ja11TuningSaveOutcome
    data class Failed(val reason: String) : Ja11TuningSaveOutcome
}

/** Deterministic injected failures for the simulated transport. */
enum class SimulatedJa11TuningFault {
    NONE,
    PERMISSION_DENIED,
    OFFLINE,
    DISCONNECT_BEFORE_WRITE,
    DISCONNECT_DURING_WRITE,
    DISCONNECT_AFTER_WRITE_BEFORE_READBACK,
    READBACK_MISMATCH,
    TIMEOUT,
}

/**
 * In-memory tuning transport used by the private prototype.
 *
 * It performs no USB I/O and has no knowledge of JA11 frames. Its purpose is to exercise the real
 * transaction shape (fresh baseline, session fence, verified readback, no replay) and the honest
 * state model (unknown, session-only, saved, lost on power cycle) before any protocol exists.
 */
class SimulatedJa11TuningTransport(
    private val fault: SimulatedJa11TuningFault = SimulatedJa11TuningFault.NONE,
    initialValues: Ja11TuningValues? = Ja11TuningValues(Ja11TuningLevel.STOCK, noiseGateEnabled = false),
    initialPersistence: Ja11TuningPersistence = Ja11TuningPersistence.UNKNOWN,
) : Ja11TuningTransport, Ja11TuningPrototypeControls {
    override val isSimulated: Boolean = true

    private var generation: Long = 1L
    private var workingValues: Ja11TuningValues? = initialValues
    private var savedValues: Ja11TuningValues? = null
    private var persistence: Ja11TuningPersistence = initialPersistence
    private var failNextReadAfterWrite = false
    private var disconnectingMidWrite = false

    var writeAttempts: Int = 0
        private set
    var readAttempts: Int = 0
        private set

    /** Number of values actually committed by simulated writes. */
    var committedWrites: Int = 0
        private set

    override suspend fun read(): Ja11TuningReadResult {
        readAttempts += 1
        if (disconnectingMidWrite) {
            disconnectingMidWrite = false
            generation += 1
            return Ja11TuningReadResult.Offline("Simulated USB session was replaced during the write.")
        }
        if (failNextReadAfterWrite) {
            failNextReadAfterWrite = false
            return Ja11TuningReadResult.Failure("Simulated readback timed out.")
        }
        return when (fault) {
            SimulatedJa11TuningFault.PERMISSION_DENIED -> Ja11TuningReadResult.PermissionDenied
            SimulatedJa11TuningFault.OFFLINE -> Ja11TuningReadResult.Offline("Simulated device is not attached.")
            else -> Ja11TuningReadResult.Success(snapshot())
        }
    }

    override suspend fun write(values: Ja11TuningValues): Ja11TuningWriteResult {
        writeAttempts += 1
        return when (fault) {
            SimulatedJa11TuningFault.PERMISSION_DENIED -> Ja11TuningWriteResult.PermissionDenied
            SimulatedJa11TuningFault.OFFLINE ->
                Ja11TuningWriteResult.DisconnectedBeforeWrite("Simulated device is not attached.")
            SimulatedJa11TuningFault.DISCONNECT_BEFORE_WRITE ->
                Ja11TuningWriteResult.DisconnectedBeforeWrite("Simulated detach before the first transfer.")
            SimulatedJa11TuningFault.DISCONNECT_DURING_WRITE -> {
                // The transfer may or may not have been committed; the session is gone either way.
                workingValues = values
                committedWrites += 1
                generation += 1
                persistence = Ja11TuningPersistence.UNKNOWN
                Ja11TuningWriteResult.DisconnectedDuringWrite("Simulated detach during the transfer.")
            }
            SimulatedJa11TuningFault.DISCONNECT_AFTER_WRITE_BEFORE_READBACK -> {
                workingValues = values
                committedWrites += 1
                persistence = Ja11TuningPersistence.SESSION_ONLY
                failNextReadAfterWrite = true
                Ja11TuningWriteResult.Accepted(generation)
            }
            SimulatedJa11TuningFault.TIMEOUT -> {
                disconnectingMidWrite = true
                Ja11TuningWriteResult.TimedOut("Simulated transfer timed out.")
            }
            SimulatedJa11TuningFault.READBACK_MISMATCH -> {
                // The simulated device silently refuses part of the request.
                workingValues = values.copy(level = Ja11TuningLevel.STOCK)
                committedWrites += 1
                persistence = Ja11TuningPersistence.SESSION_ONLY
                Ja11TuningWriteResult.Accepted(generation)
            }
            SimulatedJa11TuningFault.NONE -> {
                workingValues = values
                committedWrites += 1
                persistence = Ja11TuningPersistence.SESSION_ONLY
                Ja11TuningWriteResult.Accepted(generation)
            }
        }
    }

    /**
     * Simulates committing the current working values as startup defaults. This is not the JA11 EQ
     * Save (`0x19`) sequence and proves nothing about firmware persistence.
     */
    override fun saveStartupDefaults(): Boolean {
        val values = workingValues ?: return false
        savedValues = values
        persistence = Ja11TuningPersistence.SAVED
        return true
    }

    /**
     * Simulates unplug/replug: the session generation advances and unsaved working values are
     * replaced by saved startup values (or the baseline when nothing was saved).
     */
    override fun simulateReconnect() {
        generation += 1
        workingValues = savedValues ?: Ja11TuningValues(Ja11TuningLevel.STOCK, noiseGateEnabled = false)
        persistence = if (savedValues == null) {
            Ja11TuningPersistence.UNKNOWN
        } else {
            Ja11TuningPersistence.SAVED
        }
    }

    fun currentGeneration(): Long = generation

    private fun snapshot() = Ja11TuningSnapshot(
        sessionGeneration = generation,
        values = workingValues,
        persistence = persistence,
    )
}
