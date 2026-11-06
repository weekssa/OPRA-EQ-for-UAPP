package com.weekssa.opraeqforuapp.domain.ew300

import kotlinx.coroutines.delay

/**
 * Shared strict-read and volatile-restore boundary for every mutating EW300 transaction.
 *
 * A baseline is complete only when every register in the read-only plan returns exactly four
 * bytes. Writes are never retried. Restoration is intentionally volatile and never commits.
 */
data class Ew300RawBaseline(
    val registers: Map<Int, ByteArray>,
    val deviceFingerprintKey: String,
    val sessionGeneration: Long,
    val detachGeneration: Long,
) {
    init {
        require(deviceFingerprintKey.isNotBlank()) { "EW300 baseline fingerprint must not be blank" }
        require(sessionGeneration > 0L) { "EW300 baseline session generation must be positive" }
        require(detachGeneration >= 0L) { "EW300 baseline detach generation must not be negative" }
    }

    fun value(register: Int): ByteArray? = registers[register]?.copyOf()

    fun isComplete(): Boolean =
        registers.keys == Ew300CapabilityBatch.snapshotRegisters().toSet() &&
            registers.values.all { it.size == 4 }

    /** Only these registers have qualified restoration semantics; the identity/current-slot read is read-only. */
    fun restorableRegisters(): List<Int> = buildList {
        repeat(Ew300Protocol.BAND_COUNT) { index ->
            add(Ew300Protocol.bandRegister(index))
            add(Ew300Protocol.bandRegister(index) + 1)
        }
        add(Ew300Protocol.GLOBAL_GAIN_REGISTER)
    }

    fun bands(): List<Pair<ByteArray, ByteArray>> {
        val result = mutableListOf<Pair<ByteArray, ByteArray>>()
        repeat(Ew300Protocol.BAND_COUNT) { index ->
            val gain = value(Ew300Protocol.bandRegister(index)) ?: return emptyList()
            val q = value(Ew300Protocol.bandRegister(index) + 1) ?: return emptyList()
            result += gain to q
        }
        return result
    }

    fun matches(register: Int, actual: ByteArray?): Boolean =
        actual != null && value(register)?.contentEquals(actual) == true
}

class Ew300TransactionCoordinator(
    private val transport: Ew300Transport,
    private val readRetryDelayMillis: Long = 120L,
) {
    suspend fun captureBaseline(): Ew300RawBaseline? {
        val fingerprint = transport.deviceFingerprintKey ?: return null
        val sessionGeneration = transport.sessionGeneration
        if (sessionGeneration <= 0L) return null
        val values = linkedMapOf<Int, ByteArray>()
        for (register in Ew300CapabilityBatch.snapshotRegisters()) {
            val value = readSnapshotRegister(register) ?: return null
            values[register] = value
        }
        return Ew300RawBaseline(
            registers = values,
            deviceFingerprintKey = fingerprint,
            sessionGeneration = sessionGeneration,
            detachGeneration = transport.detachGeneration,
        )
    }

    /**
     * A snapshot read is idempotent, so one re-read is allowed when a register comes back empty or
     * short. Writes are never retried: a failed write leaves the device in an unknown state.
     */
    private suspend fun readSnapshotRegister(register: Int): ByteArray? {
        transport.readRegister(register)?.takeIf { it.size == 4 }?.let { return it.copyOf() }
        if (readRetryDelayMillis > 0L) delay(readRetryDelayMillis)
        return transport.readRegister(register)?.takeIf { it.size == 4 }?.copyOf()
    }

    suspend fun restoreVolatile(baseline: Ew300RawBaseline): Boolean {
        if (!baseline.isComplete()) return false
        val writesAccepted = baseline.restorableRegisters().all { register ->
            baseline.value(register)?.let { value -> transport.writeRegister(register, value) } == true
        }
        if (!writesAccepted) return false
        return verifyExact(baseline)
    }

    suspend fun verifyExact(baseline: Ew300RawBaseline): Boolean {
        if (!baseline.isComplete()) return false
        return baseline.registers.keys.all { register ->
            baseline.matches(register, transport.readRegister(register))
        }
    }
}
