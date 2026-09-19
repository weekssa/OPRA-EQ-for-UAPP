package com.weekssa.opraeqforuapp.domain.ew300

sealed interface Ew300PersistenceQualificationResult {
    data class AwaitingPowerCycle(val message: String) : Ew300PersistenceQualificationResult
    data class Verified(val message: String) : Ew300PersistenceQualificationResult
    data class NotPersistent(val message: String) : Ew300PersistenceQualificationResult
    data class Failed(val message: String, val stateKnown: Boolean) : Ew300PersistenceQualificationResult
}

/**
 * Exact-device, baseline-preserving qualification for 0x53 persistence and 0x66 playback gain.
 *
 * The complete snapshot is captured before mutation. One Peak gain moves by 0.1 dB and playback
 * gain moves 0.5 dB downward, then both are committed together. Two explicit physical power cycles
 * verify temporary persistence and final restoration. No failed or uncertain commit is retried.
 */
class Ew300PersistenceQualifier(
    private val transport: Ew300Transport,
    private val stateStore: Ew300GainStateStore,
    private val authorizationGate: () -> Boolean = { false },
) {
    suspend fun advance(): Ew300PersistenceQualificationResult {
        if (!authorizationGate()) {
            return failed("This installed build is not authorized for EW300 Save qualification. No operation was sent.", true)
        }
        val key = transport.deviceFingerprintKey
            ?: return failed("The exact EW300 fingerprint is unavailable; no operation was sent.", true)
        return when (val pending = stateStore.readPersistencePending(key)) {
            null -> begin(key)
            else -> when (pending.stage) {
                Ew300PersistenceStage.TEMPORARY_COMMITTED -> verifyTemporaryAndRestore(key, pending)
                Ew300PersistenceStage.BASELINE_RESTORED -> verifyFinalRestoration(key, pending)
                Ew300PersistenceStage.UNCERTAIN -> failed(
                    "A prior Save or restoration result is uncertain. No operation was sent; share the report before any recovery action.",
                    false,
                )
            }
        }
    }

    private suspend fun begin(key: String): Ew300PersistenceQualificationResult {
        stateStore.markPersistenceQualified(key, false)
        stateStore.markGlobalGainQualified(key, false)
        val baseline = readSnapshot()
            ?: return failed("Could not read the complete EW300 baseline. No write was sent.", true)
        val bandRegister = Ew300Protocol.FIRST_BAND_REGISTER
        val baselineBand = baseline.getValue(bandRegister)
        val baselineGain = baseline.getValue(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        val temporaryBand = adjustSigned16(baselineBand, -1)
            ?: return failed("The first Peak gain is at an unsupported edge value. No write was sent.", true)
        val currentPlaybackSteps = Ew300Protocol.globalGainSteps(baselineGain)
        if (currentPlaybackSteps <= Ew300Protocol.GLOBAL_GAIN_MIN_STEPS) {
            return failed("Playback gain is at its lower edge. No write was sent.", true)
        }
        val temporaryPlayback = Ew300Protocol.withGlobalGainSteps(baselineGain, currentPlaybackSteps - 1)
        val pending = Ew300PersistencePending(
            stage = Ew300PersistenceStage.TEMPORARY_COMMITTED,
            baseline = baseline,
            temporaryBandGain = temporaryBand,
            temporaryPlaybackGain = temporaryPlayback,
            powerCycleMarker = transport.detachGeneration,
        )
        // Store the recovery record before the first mutation.
        stateStore.writePersistencePending(key, pending)

        if (!transport.writeRegister(bandRegister, temporaryBand)) {
            val observed = readSnapshot()
            if (observed != null && matchesSnapshot(baseline, observed)) {
                stateStore.writePersistencePending(key, null)
                return failed("The EW300 rejected the temporary 0.1 dB Peak change. Exact readback confirms the baseline is unchanged.", true)
            }
            stateStore.writePersistencePending(key, pending.copy(stage = Ew300PersistenceStage.UNCERTAIN))
            return failed(
                "The temporary Peak write was not confirmed and exact baseline readback failed. Stop and share the report; no further operation was sent.",
                false,
            )
        }
        if (!transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, temporaryPlayback)) {
            return restoreBeforeCommitOrFail(key, pending, "The EW300 rejected the temporary safer playback-gain change.")
        }
        val changedBand = transport.readRegister(bandRegister)
        val changedPlayback = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        if (changedBand?.contentEquals(temporaryBand) != true ||
            changedPlayback?.contentEquals(temporaryPlayback) != true
        ) {
            return restoreBeforeCommitOrFail(key, pending, "Temporary readback did not match before Save.")
        }
        if (!transport.commit()) {
            stateStore.writePersistencePending(key, pending.copy(stage = Ew300PersistenceStage.UNCERTAIN))
            return failed(
                "The Save result is uncertain. Stop, leave the cable connected, and share the report; the app will not retry automatically.",
                false,
            )
        }
        val immediate = readSnapshot()
        if (immediate == null || !matchesTemporary(pending, immediate)) {
            stateStore.writePersistencePending(key, pending.copy(stage = Ew300PersistenceStage.UNCERTAIN))
            return failed(
                "Save was sent, but immediate readback is uncertain. Stop and share the report; do not repeat the action.",
                false,
            )
        }
        stateStore.writePersistencePending(
            key,
            pending.copy(powerCycleMarker = transport.detachGeneration),
        )
        return Ew300PersistenceQualificationResult.AwaitingPowerCycle(
            "Temporary values were saved and verified. Unplug the EW300 completely for 10 seconds, reconnect it, then tap Continue qualification.",
        )
    }

    private suspend fun verifyTemporaryAndRestore(
        key: String,
        pending: Ew300PersistencePending,
    ): Ew300PersistenceQualificationResult {
        if (transport.detachGeneration <= pending.powerCycleMarker) {
            return Ew300PersistenceQualificationResult.AwaitingPowerCycle(
                "A complete unplug/reconnect has not been detected yet. Unplug the EW300 for 10 seconds, reconnect it, then continue.",
            )
        }
        val afterPowerCycle = readSnapshot()
            ?: return failed("Could not read the EW300 after power removal. No write was sent.", false)
        if (!matchesTemporary(pending, afterPowerCycle)) {
            if (matchesSnapshot(pending.baseline, afterPowerCycle)) {
                stateStore.writePersistencePending(key, null)
                return Ew300PersistenceQualificationResult.NotPersistent(
                    "The temporary values did not survive full power removal. Persistent Flash remains unavailable; the original baseline is intact.",
                )
            }
            return failed(
                "The post-power-cycle state matched neither the temporary values nor the baseline. Stop and share the report.",
                false,
            )
        }

        val bandRegister = Ew300Protocol.FIRST_BAND_REGISTER
        if (!transport.writeRegister(bandRegister, pending.baseline.getValue(bandRegister)) ||
            !transport.writeRegister(
                Ew300Protocol.GLOBAL_GAIN_REGISTER,
                pending.baseline.getValue(Ew300Protocol.GLOBAL_GAIN_REGISTER),
            )
        ) {
            stateStore.writePersistencePending(key, pending.copy(stage = Ew300PersistenceStage.UNCERTAIN))
            return failed("The temporary values persisted, but baseline restoration was not accepted. Stop and share the report.", false)
        }
        if (!transport.commit()) {
            stateStore.writePersistencePending(key, pending.copy(stage = Ew300PersistenceStage.UNCERTAIN))
            return failed("The baseline restore Save is uncertain. Stop and share the report; do not retry.", false)
        }
        val restored = readSnapshot()
        if (restored == null || !matchesSnapshot(pending.baseline, restored)) {
            stateStore.writePersistencePending(key, pending.copy(stage = Ew300PersistenceStage.UNCERTAIN))
            return failed("The baseline restoration could not be verified. Stop and share the report.", false)
        }
        stateStore.writePersistencePending(
            key,
            pending.copy(
                stage = Ew300PersistenceStage.BASELINE_RESTORED,
                powerCycleMarker = transport.detachGeneration,
            ),
        )
        return Ew300PersistenceQualificationResult.AwaitingPowerCycle(
            "The original values were restored and saved. Unplug the EW300 completely for 10 seconds again, reconnect it, then tap Finish qualification.",
        )
    }

    private suspend fun verifyFinalRestoration(
        key: String,
        pending: Ew300PersistencePending,
    ): Ew300PersistenceQualificationResult {
        if (transport.detachGeneration <= pending.powerCycleMarker) {
            return Ew300PersistenceQualificationResult.AwaitingPowerCycle(
                "The final complete unplug/reconnect has not been detected yet. Unplug the EW300 for 10 seconds, reconnect it, then finish.",
            )
        }
        val final = readSnapshot()
            ?: return failed("Could not read the final EW300 state after power removal. No write was sent.", false)
        if (!matchesSnapshot(pending.baseline, final)) {
            return failed("The final power-cycle state does not match the preserved baseline. Stop and share the report.", false)
        }
        stateStore.markGlobalGainQualified(key, true)
        stateStore.markPersistenceQualified(key, true)
        stateStore.writeAppliedGainDeltaSteps(key, 0)
        stateStore.writePersistencePending(key, null)
        return Ew300PersistenceQualificationResult.Verified(
            "EW300 Save, Peak persistence, playback-gain persistence, and exact baseline restoration passed both power-removal checks.",
        )
    }

    private suspend fun restoreBeforeCommitOrFail(
        key: String,
        pending: Ew300PersistencePending,
        reason: String,
    ): Ew300PersistenceQualificationResult {
        val bandRegister = Ew300Protocol.FIRST_BAND_REGISTER
        val restored = transport.writeRegister(bandRegister, pending.baseline.getValue(bandRegister)) &&
            transport.writeRegister(
                Ew300Protocol.GLOBAL_GAIN_REGISTER,
                pending.baseline.getValue(Ew300Protocol.GLOBAL_GAIN_REGISTER),
            )
        val verified = restored && readSnapshot()?.let { matchesSnapshot(pending.baseline, it) } == true
        if (verified) {
            stateStore.writePersistencePending(key, null)
        } else {
            stateStore.writePersistencePending(key, pending.copy(stage = Ew300PersistenceStage.UNCERTAIN))
        }
        return failed(
            if (verified) "$reason The original volatile state was restored and verified; Save was not sent."
            else "$reason The original volatile state could not be verified. Stop and share the report.",
            verified,
        )
    }

    private suspend fun readSnapshot(): Map<Int, ByteArray>? =
        Ew300CapabilityBatch.snapshotRegisters().associateWith { register ->
            transport.readRegister(register)?.takeIf { it.size == 4 }?.copyOf() ?: return null
        }

    private fun matchesTemporary(pending: Ew300PersistencePending, actual: Map<Int, ByteArray>): Boolean =
        pending.baseline.keys.all { register ->
            val expected = when (register) {
                Ew300Protocol.FIRST_BAND_REGISTER -> pending.temporaryBandGain
                Ew300Protocol.GLOBAL_GAIN_REGISTER -> pending.temporaryPlaybackGain
                else -> pending.baseline.getValue(register)
            }
            actual[register]?.contentEquals(expected) == true
        }

    private fun matchesSnapshot(expected: Map<Int, ByteArray>, actual: Map<Int, ByteArray>): Boolean =
        expected.keys == actual.keys && expected.keys.all { register ->
            actual[register]?.contentEquals(expected.getValue(register)) == true
        }

    private fun adjustSigned16(source: ByteArray, delta: Int): ByteArray? {
        if (source.size != 4) return null
        val raw = (source[0].toInt() and 0xFF) or ((source[1].toInt() and 0xFF) shl 8)
        val signed = if (raw >= 0x8000) raw - 0x10000 else raw
        val changed = signed + delta
        if (changed !in Short.MIN_VALUE..Short.MAX_VALUE) return null
        return source.copyOf().also {
            it[0] = (changed and 0xFF).toByte()
            it[1] = ((changed ushr 8) and 0xFF).toByte()
        }
    }

    private fun failed(message: String, stateKnown: Boolean) =
        Ew300PersistenceQualificationResult.Failed(message, stateKnown)
}
