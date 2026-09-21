package com.weekssa.opraeqforuapp.domain.ew300

/**
 * Shared strict-read and volatile-restore boundary for every mutating EW300 transaction.
 *
 * A baseline is complete only when every register in the read-only plan returns exactly four
 * bytes. Writes are never retried. Restoration is intentionally volatile and never commits.
 */
data class Ew300RawBaseline(
    val registers: Map<Int, ByteArray>,
) {
    fun value(register: Int): ByteArray? = registers[register]?.copyOf()

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
        val values = linkedMapOf<Int, ByteArray>()
        for (register in Ew300CapabilityBatch.snapshotRegisters()) {
            val value = transport.readRegister(register)
            if (value == null || value.size != 4) return null
            values[register] = value.copyOf()
        }
        return Ew300RawBaseline(values)
    }

    suspend fun restoreVolatile(baseline: Ew300RawBaseline): Boolean {
        val writesAccepted = baseline.bands().withIndex().all { (index, pair) ->
            transport.writeRegister(Ew300Protocol.bandRegister(index), pair.first) &&
                transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, pair.second)
        } && baseline.value(Ew300Protocol.GLOBAL_GAIN_REGISTER)?.let {
            transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, it)
        } == true
        if (!writesAccepted) return false
        return baseline.bands().withIndex().all { (index, pair) ->
            baseline.matches(Ew300Protocol.bandRegister(index), transport.readRegister(Ew300Protocol.bandRegister(index))) &&
                baseline.matches(Ew300Protocol.bandRegister(index) + 1, transport.readRegister(Ew300Protocol.bandRegister(index) + 1))
        } && baseline.matches(
            Ew300Protocol.GLOBAL_GAIN_REGISTER,
            transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER),
        )
    }
}
