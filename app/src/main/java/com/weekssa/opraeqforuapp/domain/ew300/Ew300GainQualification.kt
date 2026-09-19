package com.weekssa.opraeqforuapp.domain.ew300

/** Local state required to prevent an unqualified EW300 gain mapping from becoming active. */
interface Ew300GainStateStore {
    fun isGlobalGainQualified(deviceFingerprintKey: String): Boolean
    fun markGlobalGainQualified(deviceFingerprintKey: String, qualified: Boolean)
    fun readAppliedGainDeltaSteps(deviceFingerprintKey: String): Int
    fun writeAppliedGainDeltaSteps(deviceFingerprintKey: String, steps: Int)
}

sealed interface Ew300GainQualificationResult {
    data object Verified : Ew300GainQualificationResult
    data class Failed(val reason: String) : Ew300GainQualificationResult
}

/**
 * One reversible, exact-device gate for the provisional global-gain mapping.
 *
 * It changes only the qualified gain byte by one 0.5 dB step, persists it, verifies the new
 * value after the expected USB re-enumeration, restores the captured four-byte field, persists
 * that restoration, and requires a final exact full-register readback. No band field is changed.
 */
class Ew300GainQualifier(
    private val transport: Ew300Transport,
    private val stateStore: Ew300GainStateStore,
) {
    suspend fun qualify(): Ew300GainQualificationResult {
        val deviceKey = transport.deviceFingerprintKey ?: return fail("The exact EW300 device fingerprint is unavailable.")
        val baseline = readSnapshot() ?: return fail("Couldn’t read the complete EW300 state before qualification.")
        val currentGain = baseline.getValue(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        val currentSteps = Ew300Protocol.globalGainSteps(currentGain)
        val temporarySteps = when {
            currentSteps < Ew300Protocol.GLOBAL_GAIN_MAX_STEPS -> currentSteps + 1
            currentSteps > Ew300Protocol.GLOBAL_GAIN_MIN_STEPS -> currentSteps - 1
            else -> return fail("The EW300 global-gain field is at an unsupported edge value.")
        }
        val temporary = Ew300Protocol.withGlobalGainSteps(currentGain, temporarySteps)

        stateStore.markGlobalGainQualified(deviceKey, false)
        var temporaryWriteAccepted = false
        var temporaryCommitConfirmed = false
        var restorationAttempted = false
        try {
            temporaryWriteAccepted = transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, temporary)
            if (!temporaryWriteAccepted) {
                return fail("The EW300 did not accept the temporary global-gain qualification write.")
            }
            if (!transport.commit()) {
                return fail("The EW300 temporary qualification commit was not confirmed; reconnect and read before retrying.")
            }
            temporaryCommitConfirmed = true

            val temporaryRead = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
            if (temporaryRead == null || !temporaryRead.contentEquals(temporary)) {
                return fail("The EW300 global-gain temporary readback did not match.")
            }

            restorationAttempted = true
            if (!transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, currentGain) ||
                !transport.commit()
            ) return fail("The EW300 temporary gain was changed, but restoration was not confirmed; reconnect and read before retrying.")

            val restored = readSnapshot()
            if (restored == null || !matchesSnapshot(baseline, restored)) {
                return fail("The EW300 final gain restoration did not match the preserved state.")
            }

            stateStore.markGlobalGainQualified(deviceKey, true)
            return Ew300GainQualificationResult.Verified
        } finally {
            // A failed write cannot have changed the device. If it did change, make a best-effort
            // restoration before leaving the gate locked; never report qualification after this.
            // A failed/uncertain commit must never trigger an automatic second mutation. Once the
            // temporary commit is confirmed, restoration is the bounded safety action; if that
            // restoration itself is uncertain, leave the device for a fresh read and diagnosis.
            if (temporaryWriteAccepted && temporaryCommitConfirmed && !restorationAttempted &&
                !stateStore.isGlobalGainQualified(deviceKey)
            ) {
                restore(currentGain)
            }
        }
    }

    private suspend fun restore(gain: ByteArray) {
        if (transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, gain) && transport.commit()) {
            readSnapshot()
        }
    }

    private suspend fun readSnapshot(): Map<Int, ByteArray>? {
        val registers = buildList {
            add(0x24)
            repeat(Ew300Protocol.BAND_COUNT * 2) { add(Ew300Protocol.FIRST_BAND_REGISTER + it) }
            add(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        }
        return registers.associateWith { register -> transport.readRegister(register) ?: return null }
    }

    private fun matchesSnapshot(expected: Map<Int, ByteArray>, actual: Map<Int, ByteArray>): Boolean =
        expected.keys.all { register -> actual[register]?.contentEquals(expected.getValue(register)) == true }

    private fun fail(reason: String): Ew300GainQualificationResult.Failed {
        transport.deviceFingerprintKey?.let { key -> stateStore.markGlobalGainQualified(key, false) }
        return Ew300GainQualificationResult.Failed(reason)
    }
}
