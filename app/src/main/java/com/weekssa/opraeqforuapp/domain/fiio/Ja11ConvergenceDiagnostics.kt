package com.weekssa.opraeqforuapp.domain.fiio

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class Ja11ConvergenceComponent { ACTIVITY, VIEW_MODEL, HARDWARE_REPOSITORY, SESSION_REPOSITORY }

@Serializable
enum class Ja11ConvergenceOwner { NONE, CONNECTED_OBSERVER, FLASH_COMPLETION, EXPLICIT_READ, EQ_PROGRAM_WRITE, RESTART_VERIFICATION, EDITOR_BASELINE, SCHEDULED_REFRESH }

@Serializable
enum class Ja11ConvergencePhase {
    CREATED, RESUMED, PAUSED, STOPPED, DESTROYED, CLEARED, CLOSED,
    CONNECTED, NOT_CONNECTED, FLASH_STARTED, MUTATION_RETURNED, FLASH_RETURNED,
    REFRESH_REQUESTED, REFRESH_ENTERED, ATTEMPT_STARTED, GUARD_REJECTED,
    PROGRAM_MISSING, PROGRAM_UNSUPPORTED, BAND_MISSING, GAIN_MISSING, FACTORY_REJECTED,
    GENERATION_CHANGED, READ_NO_BUNDLE, READ_BUNDLE, PUBLISHED, READ_FAILED, STALE,
    CANCELLED, THREW, REFRESH_ENDED, UI_ADOPTION,
}

/** Only structural state is accepted; no device identity, coefficients or arbitrary error text. */
@Serializable
data class Ja11ConvergenceEvent(
    val component: Ja11ConvergenceComponent,
    val instanceId: Long,
    val phase: Ja11ConvergencePhase,
    val relatedInstanceId: Long? = null,
    val owner: Ja11ConvergenceOwner = Ja11ConvergenceOwner.NONE,
    val acquisitionId: Long? = null,
    val operationId: String? = null,
    val attempt: Int? = null,
    val generation: Long? = null,
    val bundleGeneration: Long? = null,
    val connected: Boolean? = null,
    val sessionCurrent: Boolean? = null,
    val bundleCurrent: Boolean? = null,
    val bundlePresent: Boolean? = null,
    val reading: Boolean? = null,
    val readFailed: Boolean? = null,
    val deviceCurrent: Boolean? = null,
    val fieldIndex: Int? = null,
)

@Serializable
data class Ja11ConvergenceRecord(val sequence: Long, val event: Ja11ConvergenceEvent)

@Serializable
data class Ja11ConvergenceSnapshot(
    val schemaVersion: Int = 1,
    val droppedRecords: Long,
    val records: List<Ja11ConvergenceRecord>,
)

/** Bounded, process-local recorder. It owns no coroutines, transport, files or callbacks. */
class Ja11ConvergenceDiagnostics(private val capacity: Int = 2_048) {
    init { require(capacity > 0) }
    private var nextId = 0L
    private var sequence = 0L
    private var dropped = 0L
    private val records = ArrayDeque<Ja11ConvergenceRecord>()
    private val operationIdPattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    @Synchronized
    fun allocateId(): Long = ++nextId

    @Synchronized
    fun record(event: Ja11ConvergenceEvent) {
        if (records.size == capacity) { records.removeFirst(); dropped += 1 }
        records.addLast(Ja11ConvergenceRecord(++sequence, event.copy(
            operationId = event.operationId?.takeIf(operationIdPattern::matches),
        )))
    }

    @Synchronized
    fun snapshot(): Ja11ConvergenceSnapshot = Ja11ConvergenceSnapshot(
        droppedRecords = dropped,
        records = records.toList(),
    )

    fun toJson(): String = Json { prettyPrint = true }.encodeToString(snapshot())
}

data class Ja11ConvergenceReadContext(
    val acquisitionId: Long,
    val repositoryId: Long,
    val owner: Ja11ConvergenceOwner,
    val operationId: String?,
    val attempt: Int,
)
