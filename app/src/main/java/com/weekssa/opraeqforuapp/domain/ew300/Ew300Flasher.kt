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
    suspend fun readRegister(register: Int): ByteArray?
    suspend fun writeRegister(register: Int, data: ByteArray): Boolean
    suspend fun commit(): Boolean
}

/** Five-band EW300 transaction using the shared finite-target response optimizer. */
class Ew300Flasher(
    private val transport: Ew300Transport,
    private val gainStateStore: Ew300GainStateStore,
) {
    suspend fun applyEditorWorkingCopy(
        workingCopy: com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
    ): Ew300EditorApplyResult = Ew300EditorApplier(transport).apply(
        workingCopy = workingCopy,
        allowCautions = allowCautions,
        isSessionCurrent = isSessionCurrent,
    )

    suspend fun flash(profile: OpraEqProfile): Kt02h20FlashResult {
        if (!gainStateStore.isGlobalGainQualified()) {
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
        if (currentGain == null || !readAndDecodeCurrent()) {
            return Kt02h20FlashResult.DeviceUnavailable(
                "Couldn’t read the EW300 PEQ state. Reconnect the DAC and try again.",
            )
        }
        val currentSteps = Ew300Protocol.globalGainSteps(currentGain)
        val baselineSteps = currentSteps - gainStateStore.readAppliedGainDeltaSteps()
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
        val targetGain = Ew300Protocol.withGlobalGainSteps(currentGain, targetGainSteps)
        if (!currentGain.contentEquals(targetGain) &&
            !transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, targetGain)
        ) {
            return Kt02h20FlashResult.TransferFailed(
                "EW300 did not accept the required global-gain adjustment. No EQ bands were written.",
            )
        }
        if (!currentGain.contentEquals(targetGain)) {
            gainStateStore.writeAppliedGainDeltaSteps(requestedDeltaSteps)
        }
        target.forEachIndexed { index, band ->
            val (gain, q) = runCatching { Ew300Protocol.encodeBand(band) }.getOrElse {
                return Kt02h20FlashResult.NotSuitable(it.message ?: "EW300 band is not representable.")
            }
            if (!transport.writeRegister(Ew300Protocol.bandRegister(index), gain) ||
                !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, q)
            ) {
                return Kt02h20FlashResult.TransferFailed(
                    "EW300 stopped accepting PEQ data at band ${index + 1} of ${Ew300Protocol.BAND_COUNT}.",
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
        return Kt02h20FlashResult.Success(
            representation = representation,
            explicitPersistenceCommandUsed = true,
        )
    }

    suspend fun resetToFlat(): Kt02h20FlatResetResult {
        val flat = completeBands(emptyList())
        if (!gainStateStore.isGlobalGainQualified()) {
            return Kt02h20FlatResetResult.NotSuitable(
                "Run the one-time EW300 global-gain qualification from My DAC before resetting its EQ.",
            )
        }
        val currentGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
            ?: return Kt02h20FlatResetResult.DeviceUnavailable("Couldn’t read the EW300 global gain before reset.")
        val baselineSteps = Ew300Protocol.globalGainSteps(currentGain) - gainStateStore.readAppliedGainDeltaSteps()
        if (baselineSteps !in Ew300Protocol.GLOBAL_GAIN_MIN_STEPS..Ew300Protocol.GLOBAL_GAIN_MAX_STEPS) {
            return Kt02h20FlatResetResult.NotSuitable("The EW300 baseline global gain is outside the qualified range.")
        }
        val baselineGain = Ew300Protocol.withGlobalGainSteps(currentGain, baselineSteps)
        flat.forEachIndexed { index, band ->
            val (gain, q) = Ew300Protocol.encodeBand(band)
            if (!transport.writeRegister(Ew300Protocol.bandRegister(index), gain) ||
                !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, q)
            ) {
                return Kt02h20FlatResetResult.TransferFailed("EW300 stopped accepting the flat-EQ reset at band ${index + 1}.")
            }
        }
        if (!currentGain.contentEquals(baselineGain) &&
            !transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, baselineGain)
        ) {
            return Kt02h20FlatResetResult.TransferFailed("EW300 flat EQ was prepared, but its prior gain could not be restored.")
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
        gainStateStore.writeAppliedGainDeltaSteps(0)
        return Kt02h20FlatResetResult.Success(restoredPlaybackGainDb = 0.0, explicitPersistenceCommandUsed = true)
    }

    private suspend fun readAndDecodeCurrent(): Boolean = (0 until Ew300Protocol.BAND_COUNT).all { index ->
        val gain = transport.readRegister(Ew300Protocol.bandRegister(index))
        val q = transport.readRegister(Ew300Protocol.bandRegister(index) + 1)
        gain?.size == 4 && q?.size == 4
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

    private fun completeBands(bands: List<Kt02h20Band>): List<Kt02h20Band> =
        bands + List(Ew300Protocol.BAND_COUNT - bands.size) { Kt02h20Band("peak_dip", 1000.0, 0.0, 1.0) }

    private fun formatDb(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
}
