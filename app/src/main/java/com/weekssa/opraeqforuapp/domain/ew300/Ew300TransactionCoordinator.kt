package com.weekssa.opraeqforuapp.domain.ew300

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
) {
    suspend fun captureBaseline(): Ew300RawBaseline? {
        val fingerprint = transport.deviceFingerprintKey ?: return null
        val sessionGeneration = transport.sessionGeneration
        if (sessionGeneration <= 0L) return null
        val values = linkedMapOf<Int, ByteArray>()
        for (register in Ew300CapabilityBatch.snapshotRegisters()) {
            val value = transport.readRegister(register)
            if (value == null || value.size != 4) return null
            values[register] = value.copyOf()
        }
        return Ew300RawBaseline(
            registers = values,
            deviceFingerprintKey = fingerprint,
            sessionGeneration = sessionGeneration,
            detachGeneration = transport.detachGeneration,
        )
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
