package com.weekssa.opraeqforuapp.domain.ew300

/** Local state required to prevent an unqualified EW300 gain mapping from becoming active. */
interface Ew300GainStateStore {
    fun isGlobalGainQualified(deviceFingerprintKey: String): Boolean
    fun markGlobalGainQualified(deviceFingerprintKey: String, qualified: Boolean)
    fun readAppliedGainDeltaSteps(deviceFingerprintKey: String): Int
    fun writeAppliedGainDeltaSteps(deviceFingerprintKey: String, steps: Int)
    fun isPersistenceQualified(deviceFingerprintKey: String): Boolean = false
    fun markPersistenceQualified(deviceFingerprintKey: String, qualified: Boolean) = Unit
    fun readPersistencePending(deviceFingerprintKey: String): Ew300PersistencePending? = null
    fun writePersistencePending(deviceFingerprintKey: String, pending: Ew300PersistencePending?) = Unit
}

enum class Ew300PersistenceStage { TEMPORARY_COMMITTED, BASELINE_RESTORED, UNCERTAIN }

data class Ew300PersistencePending(
    val stage: Ew300PersistenceStage,
    val baseline: Map<Int, ByteArray>,
    val temporaryBandGain: ByteArray,
    val temporaryPlaybackGain: ByteArray,
    val powerCycleMarker: Long,
)
