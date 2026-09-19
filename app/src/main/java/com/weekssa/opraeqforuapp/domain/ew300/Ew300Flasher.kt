package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandRepresentation
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlatResetResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FlashResult

interface Ew300Transport {
    /** Strong identity for the currently open exact device/session. */
    val deviceFingerprintKey: String?
        get() = null
    /** Monotonic physical detach evidence for power-removal qualification. */
    val detachGeneration: Long
        get() = 0L
    suspend fun readRegister(register: Int): ByteArray?
    suspend fun writeRegister(register: Int, data: ByteArray): Boolean
    suspend fun commit(): Boolean
}

/** Guarded five-band EW300 transaction using the shared finite-target response optimizer. */
class Ew300Flasher(
    private val transport: Ew300Transport,
    private val gainStateStore: Ew300GainStateStore,
    private val persistenceQualified: () -> Boolean = { false },
) {
    suspend fun applyEditorWorkingCopy(
        workingCopy: com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
    ): Ew300EditorApplyResult {
        val deviceKey = transport.deviceFingerprintKey
            ?: return Ew300EditorApplyResult.DeviceUnavailable("The exact EW300 device fingerprint is unavailable.")
        if (!gainStateStore.isGlobalGainQualified(deviceKey)) {
            return Ew300EditorApplyResult.InvalidPlan(
                "EW300 editor Apply is locked until the exact device's gain capability is qualified.",
            )
        }
        return Ew300EditorApplier(transport).apply(
            workingCopy = workingCopy,
            allowCautions = allowCautions,
            isSessionCurrent = isSessionCurrent,
        )
    }

    suspend fun flash(profile: OpraEqProfile): Kt02h20FlashResult {
        if (!persistenceQualified()) {
            return Kt02h20FlashResult.NotSuitable(
                "Persistent EW300 Flash is not enabled until the exact save command passes the capability and power-cycle gate.",
            )
        }
        val deviceKey = transport.deviceFingerprintKey
            ?: return Kt02h20FlashResult.DeviceUnavailable("The exact EW300 device fingerprint is unavailable.")
        if (!gainStateStore.isGlobalGainQualified(deviceKey)) {
            return Kt02h20FlashResult.NotSuitable(
                "Run the one-time EW300 global-gain qualification from My DAC before flashing library EQs.",
            )
        }
        val representation = when (val result = Kt02h20FiveBandOptimizer.optimize(profile, HardwareEqDeviceSpecs.SIMGOT_EW300)) {
            is FiveBandOptimizationResult.NotSuitable -> return Kt02h20FlashResult.NotSuitable(result.reason)
            is FiveBandOptimizationResult.Ready -> result.representation
        }
        val target = completeBands(representation.bands)
        val currentGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        val baselineBands = readRawBands()
        if (currentGain == null || baselineBands == null) {
            return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t read the EW300 PEQ state. Reconnect the DAC and try again.",
            )
        }
        val currentSteps = Ew300Protocol.globalGainSteps(currentGain)
        val baselineSteps = currentSteps - gainStateStore.readAppliedGainDeltaSteps(deviceKey)
        val requestedDeltaSteps = runCatching {
            Ew300Protocol.gainDbToSteps(representation.playbackGainDb)
        }.getOrElse {
            return Kt02h20FlashResult.NotSuitable(
                "The requested EW300 playback-gain adjustment is outside the qualified range.",
            )
        }
        val targetGainSteps = baselineSteps + requestedDeltaSteps
        if (targetGainSteps !in Ew300Protocol.GLOBAL_GAIN_MIN_STEPS..Ew300Protocol.GLOBAL_GAIN_MAX_STEPS) {
            return Kt02h20FlashResult.NotSuitable(
                "Applying ${formatDb(representation.playbackGainDb)} dB of EW300 playback gain would exceed the qualified device range.",
            )
        }
        val targetWires = target.map { band ->
            runCatching { Ew300Protocol.encodeBand(band) }.getOrElse {
                return Kt02h20FlashResult.NotSuitable(it.message ?: "EW300 band is not representable.")
            }
        }
        val targetGain = Ew300Protocol.withGlobalGainSteps(currentGain, targetGainSteps)
        if (!currentGain.contentEquals(targetGain) &&
            !transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, targetGain)
        ) {
            return restoreBeforeCommit(
                baselineBands = baselineBands,
                baselineGain = currentGain,
                reason = "EW300 did not accept the required global-gain adjustment.",
            )
        }
        targetWires.forEachIndexed { index, (gain, q) ->
            if (!transport.writeRegister(Ew300Protocol.bandRegister(index), gain) ||
                !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, q)
            ) {
                return restoreBeforeCommit(
                    baselineBands = baselineBands,
                    baselineGain = currentGain,
                    reason = "EW300 stopped accepting PEQ data at band ${index + 1} of ${Ew300Protocol.BAND_COUNT}.",
                )
            }
        }
        if (!transport.commit()) {
            return Kt02h20FlashResult.TransferFailed(
                "EW300 accepted the PEQ writes but did not accept the persistence command.",
            )
        }
        val verificationFailure = verify(target)
        if (verificationFailure != null) {
            return Kt02h20FlashResult.VerificationFailed(
                "EW300 final PEQ readback did not match band ${verificationFailure.band + 1}: " +
                    "expected ${verificationFailure.expected}, read ${verificationFailure.actual}.",
            )
        }
        val finalGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        if (finalGain == null || !finalGain.contentEquals(targetGain)) {
            return Kt02h20FlashResult.VerificationFailed(
                "EW300 final global-gain readback did not match the requested adjustment.",
            )
        }
        gainStateStore.writeAppliedGainDeltaSteps(deviceKey, requestedDeltaSteps)
        return Kt02h20FlashResult.Success(
            representation = representation,
            explicitPersistenceCommandUsed = true,
        )
    }

    suspend fun resetToFlat(): Kt02h20FlatResetResult {
        if (!persistenceQualified()) {
            return Kt02h20FlatResetResult.NotSuitable(
                "EW300 Reset is not enabled until persistent save and reset semantics are qualified on this exact device.",
            )
        }
        val flat = completeBands(emptyList())
        val deviceKey = transport.deviceFingerprintKey
            ?: return Kt02h20FlatResetResult.DeviceUnavailable("The exact EW300 device fingerprint is unavailable.")
        if (!gainStateStore.isGlobalGainQualified(deviceKey)) {
            return Kt02h20FlatResetResult.NotSuitable(
                "Run the one-time EW300 global-gain qualification from My DAC before resetting its EQ.",
            )
        }
        val currentGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
            ?: return Kt02h20FlatResetResult.DeviceUnavailable("Couldn’t read the EW300 global gain before reset.")
        val baselineSteps = Ew300Protocol.globalGainSteps(currentGain) - gainStateStore.readAppliedGainDeltaSteps(deviceKey)
        if (baselineSteps !in Ew300Protocol.GLOBAL_GAIN_MIN_STEPS..Ew300Protocol.GLOBAL_GAIN_MAX_STEPS) {
            return Kt02h20FlatResetResult.NotSuitable("The EW300 baseline global gain is outside the qualified range.")
        }
        val baselineGain = Ew300Protocol.withGlobalGainSteps(currentGain, baselineSteps)
        val baselineBands = readRawBands()
            ?: return Kt02h20FlatResetResult.DeviceUnavailable("Couldn’t read the complete EW300 EQ before reset.")
        flat.forEachIndexed { index, band ->
            val (gain, q) = Ew300Protocol.encodeBand(band)
            if (!transport.writeRegister(Ew300Protocol.bandRegister(index), gain) ||
                !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, q)
            ) {
                return restoreBeforeCommit(
                    baselineBands = baselineBands,
                    baselineGain = currentGain,
                    reason = "EW300 stopped accepting the flat-EQ reset at band ${index + 1}.",
                ).toFlatResetResult()
            }
        }
        if (!currentGain.contentEquals(baselineGain) &&
            !transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, baselineGain)
        ) {
            return restoreBeforeCommit(
                baselineBands = baselineBands,
                baselineGain = currentGain,
                reason = "EW300 flat EQ was prepared, but its prior gain could not be restored.",
            ).toFlatResetResult()
        }
        if (!transport.commit()) {
            return Kt02h20FlatResetResult.TransferFailed("EW300 did not accept the flat-EQ persistence command.")
        }
        val verificationFailure = verify(flat)
        if (verificationFailure != null) {
            return Kt02h20FlatResetResult.VerificationFailed("EW300 final flat-EQ readback did not match.")
        }
        val finalGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        if (finalGain == null || !finalGain.contentEquals(baselineGain)) {
            return Kt02h20FlatResetResult.VerificationFailed("EW300 final flat-EQ gain readback did not match.")
        }
        gainStateStore.writeAppliedGainDeltaSteps(deviceKey, 0)
        return Kt02h20FlatResetResult.Success(restoredPlaybackGainDb = 0.0, explicitPersistenceCommandUsed = true)
    }

    private suspend fun readRawBands(): List<RawBand>? = buildList {
        repeat(Ew300Protocol.BAND_COUNT) { index ->
            val gain = transport.readRegister(Ew300Protocol.bandRegister(index))
            val q = transport.readRegister(Ew300Protocol.bandRegister(index) + 1)
            if (gain == null || q == null || gain.size != 4 || q.size != 4) return null
            add(RawBand(gain, q))
        }
    }

    private suspend fun restoreBeforeCommit(
        baselineBands: List<RawBand>,
        baselineGain: ByteArray,
        reason: String,
    ): Kt02h20FlashResult {
        val restored = baselineBands.withIndex().all { (index, raw) ->
            transport.writeRegister(Ew300Protocol.bandRegister(index), raw.gain) &&
                transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, raw.q)
        } && transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, baselineGain)
        if (!restored) {
            return Kt02h20FlashResult.TransferFailed("$reason Restoration also failed; reconnect and read before any retry.")
        }
        val restoredBands = readRawBands() == baselineBands
        val restoredGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
            ?.contentEquals(baselineGain) == true
        return if (restoredBands && restoredGain) {
            Kt02h20FlashResult.TransferFailed("$reason The original EW300 state was restored and verified.")
        } else {
            Kt02h20FlashResult.VerificationFailed("$reason The original EW300 state could not be verified after restoration.")
        }
    }

    private fun Kt02h20FlashResult.toFlatResetResult(): Kt02h20FlatResetResult = when (this) {
        is Kt02h20FlashResult.TransferFailed -> Kt02h20FlatResetResult.TransferFailed(reason)
        is Kt02h20FlashResult.VerificationFailed -> Kt02h20FlatResetResult.VerificationFailed(reason)
        else -> Kt02h20FlatResetResult.TransferFailed("EW300 reset stopped before verification.")
    }

    private suspend fun verify(expected: List<Kt02h20Band>): VerificationFailure? {
        expected.indices.forEach { index ->
            val gain = transport.readRegister(Ew300Protocol.bandRegister(index))
                ?: return VerificationFailure(index, expected[index], null)
            val q = transport.readRegister(Ew300Protocol.bandRegister(index) + 1)
                ?: return VerificationFailure(index, expected[index], null)
            val actual = Ew300Protocol.decodeBand(index, gain, q)
            // Verify the device's encoded fields, not the display-layer Double values. The
            // EW300 stores integer tenths/thousandths, so 0.7 and 0.7000000000000001 are the
            // same hardware value even though Kotlin data-class equality treats them as distinct.
            val expectedWire = runCatching { Ew300Protocol.encodeBand(expected[index]) }.getOrNull()
            val matchesWire = expectedWire != null &&
                expectedWire.first.contentEquals(gain) &&
                expectedWire.second.contentEquals(q)
            if (!matchesWire) return VerificationFailure(index, expected[index], actual)
        }
        return null
    }

    private data class VerificationFailure(
        val band: Int,
        val expected: Kt02h20Band,
        val actual: Kt02h20Band?,
    )

    private data class RawBand(
        val gain: ByteArray,
        val q: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean = other is RawBand &&
            gain.contentEquals(other.gain) && q.contentEquals(other.q)

        override fun hashCode(): Int = 31 * gain.contentHashCode() + q.contentHashCode()
    }

    private fun completeBands(bands: List<Kt02h20Band>): List<Kt02h20Band> =
        bands + List(Ew300Protocol.BAND_COUNT - bands.size) { Kt02h20Band("peak_dip", 1000.0, 0.0, 1.0) }

    private fun formatDb(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
}
