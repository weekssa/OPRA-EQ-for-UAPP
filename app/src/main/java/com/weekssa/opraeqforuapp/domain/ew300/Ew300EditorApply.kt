package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacHeadroomStatus
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqFilter
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band

sealed interface Ew300EditorApplyResult {
    data object Verified : Ew300EditorApplyResult
    data class ConfirmationRequired(val cautionCount: Int) : Ew300EditorApplyResult
    data class InvalidPlan(val reason: String) : Ew300EditorApplyResult
    data class StaleBaseline(val reason: String) : Ew300EditorApplyResult
    data class DeviceUnavailable(val reason: String) : Ew300EditorApplyResult
    data class TransferFailed(val reason: String) : Ew300EditorApplyResult
    data class VerificationFailed(val reason: String) : Ew300EditorApplyResult
}

/** Immutable, session-scoped live target a later explicit Save may persist. */
data class Ew300RuntimeTarget(
    val deviceFingerprintKey: String,
    val sessionGeneration: Long,
    val detachGeneration: Long,
    val registers: Map<Int, ByteArray>,
    val gainSteps: Int,
) {
    init {
        require(deviceFingerprintKey.isNotBlank())
        require(sessionGeneration > 0L)
        require(detachGeneration >= 0L)
        require(registers.isNotEmpty())
    }
}

sealed interface Ew300RuntimeApplyResult {
    data class Verified(val target: Ew300RuntimeTarget) : Ew300RuntimeApplyResult
    data class ConfirmationRequired(val cautionCount: Int) : Ew300RuntimeApplyResult
    data class InvalidPlan(val reason: String) : Ew300RuntimeApplyResult
    data class StaleBaseline(val reason: String) : Ew300RuntimeApplyResult
    data class DeviceUnavailable(val reason: String) : Ew300RuntimeApplyResult
    data class TransferFailed(val reason: String) : Ew300RuntimeApplyResult
    data class VerificationFailed(val reason: String) : Ew300RuntimeApplyResult
}

sealed interface Ew300RuntimeSaveResult {
    data object Verified : Ew300RuntimeSaveResult
    data class NotSuitable(val reason: String) : Ew300RuntimeSaveResult
    data class DeviceUnavailable(val reason: String) : Ew300RuntimeSaveResult
    data class TransferFailed(val reason: String) : Ew300RuntimeSaveResult
    data class VerificationFailed(val reason: String) : Ew300RuntimeSaveResult
}

data class Ew300CommitObservation(
    val replacementObserved: Boolean,
    val replacementIdentityMatched: Boolean,
)

/** Applies the shared My DAC editor working copy through the guarded EW300 transaction. */
class Ew300EditorApplier(
    private val transport: Ew300Transport,
    private val allowExperimentalShelf: Boolean = false,
) {
    private val transactionCoordinator = Ew300TransactionCoordinator(transport)

    suspend fun apply(
        workingCopy: HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
        beforeFirstWrite: () -> Unit = {},
        beforeSave: () -> Unit = {},
        afterSave: (Ew300CommitObservation) -> Unit = {},
    ): Ew300EditorApplyResult {
        val expectedFingerprint = transport.deviceFingerprintKey
        if (expectedFingerprint == null) {
            return Ew300EditorApplyResult.DeviceUnavailable("The exact EW300 device fingerprint is unavailable.")
        }
        if (workingCopy.baselineSnapshot.deviceId != DacDeviceId.SIMGOT_EW300) {
            return Ew300EditorApplyResult.InvalidPlan("This editor working copy does not belong to SIMGOT EW300 DSP.")
        }
        if (!workingCopy.hasChanges) return Ew300EditorApplyResult.InvalidPlan("There are no reviewed hardware changes to apply.")
        if (workingCopy.hasBlockingIssues) return Ew300EditorApplyResult.InvalidPlan("Fix the blocking EQ values before applying.")
        val headroom = workingCopy.headroomAssessment
            ?: return Ew300EditorApplyResult.InvalidPlan("The reviewed EQ headroom could not be assessed safely.")
        if (headroom.status != DacHeadroomStatus.SAFE) {
            return Ew300EditorApplyResult.InvalidPlan("The reviewed EQ does not yet have a safe headroom plan. Use safe gain or revise the EQ before applying.")
        }
        if (workingCopy.cautions.isNotEmpty() && !allowCautions) {
            return Ew300EditorApplyResult.ConfirmationRequired(workingCopy.cautions.size)
        }
        val generation = workingCopy.baselineSnapshot.sessionGeneration
        if (!isSessionCurrent(generation)) return stale()
        val baselineFilters = ordered(workingCopy.baselineSnapshot.filters)
        val target = ordered(workingCopy.filters)
        if (baselineFilters.map(HardwareEqFilter::index) != (0 until Ew300Protocol.BAND_COUNT).toList()) {
            return Ew300EditorApplyResult.InvalidPlan("The EW300 editor baseline does not contain all five bands.")
        }
        if (target.map(HardwareEqFilter::index) != baselineFilters.map(HardwareEqFilter::index)) {
            return Ew300EditorApplyResult.InvalidPlan("The reviewed EQ changed the EW300 band layout.")
        }
        val expectedBaseline = baselineFilters.map { encode(it) ?: return invalidBand(it.index) }
        val rawBaseline = transactionCoordinator.captureBaseline()
            ?: return Ew300EditorApplyResult.DeviceUnavailable("Could not re-read the complete EW300 state before Apply. No changes were written.")
        if (!isSessionCurrent(generation)) return stale()
        val fresh = rawBaseline.bands().map { (gain, q) -> WireBandPair(gain, q) }
        if (fresh != expectedBaseline) {
            return Ew300EditorApplyResult.StaleBaseline("The EW300 EQ changed after the editor was opened. No changes were written; read the DAC again.")
        }
        val baselineGain = rawBaseline.value(Ew300Protocol.GLOBAL_GAIN_REGISTER)
            ?: return Ew300EditorApplyResult.DeviceUnavailable("Could not re-read EW300 global gain before Apply. No changes were written.")
        val protocolFlags = rawBaseline.value(Ew300Protocol.PROTOCOL_FLAGS_REGISTER)
            ?: return Ew300EditorApplyResult.DeviceUnavailable("Could not re-read EW300 protocol layout before Apply. No changes were written.")
        val expectedGainSteps = Ew300Protocol.gainDbToSteps(workingCopy.baselineHeadroomGainDb ?: return invalidGain())
        val actualBaselineGainSteps = Ew300Protocol.globalGainSteps(baselineGain, protocolFlags)
            ?: return Ew300EditorApplyResult.StaleBaseline(
                "The EW300 stereo playback-gain channels differ. No changes were written; refresh the DAC before applying.",
            )
        if (actualBaselineGainSteps != expectedGainSteps) {
            return Ew300EditorApplyResult.StaleBaseline("The EW300 global gain changed after the editor was opened. No changes were written; read the DAC again.")
        }
        val targetBands = target.map { encode(it) ?: return invalidBand(it.index) }
        val targetGainSteps = Ew300Protocol.gainDbToSteps(workingCopy.plannedHeadroomGainDb ?: return invalidGain())
        val bandsChanged = targetBands != expectedBaseline
        val gainChanged = targetGainSteps != expectedGainSteps
        if (!bandsChanged && !gainChanged) return Ew300EditorApplyResult.InvalidPlan("There are no reviewed hardware changes to apply.")

        beforeFirstWrite()
        if (gainChanged && targetGainSteps < expectedGainSteps) {
            if (!writeGain(targetGainSteps, protocolFlags)) {
                return restoreAfterFailure(
                    baseline = rawBaseline,
                    reason = "EW300 did not accept the safer global-gain adjustment.",
                )
            }
        }
        if (bandsChanged) {
            target.forEachIndexed { index, filter ->
                val wire = targetBands[index]
                if (!isSessionCurrent(generation)) return stale()
                if (!transport.writeRegister(Ew300Protocol.bandRegister(index), wire.gain) ||
                    !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, wire.q)
                ) {
                    return restoreAfterFailure(
                        baseline = rawBaseline,
                        reason = "EW300 stopped accepting the reviewed EQ at band ${index + 1}.",
                    )
                }
            }
            if (readBands() != targetBands) {
                return restoreAfterFailure(
                    baseline = rawBaseline,
                    reason = "EW300 final EQ readback did not match the reviewed values.",
                )
            }
        }
        if (gainChanged && targetGainSteps > expectedGainSteps && !writeGain(targetGainSteps, protocolFlags)) {
            return restoreAfterFailure(
                baseline = rawBaseline,
                reason = "EW300 did not accept the final global-gain adjustment.",
            )
        }

        // Apply is a persistent product transaction. The Save command may re-enumerate the USB
        // function, so the old session generation is intentionally not reused after commit.
        beforeSave()
        val sessionGenerationBeforeSave = transport.sessionGeneration
        val detachGenerationBeforeSave = transport.detachGeneration
        if (!transport.commit()) {
            return Ew300EditorApplyResult.TransferFailed(
                "EW300 Save was not confirmed. Reconnect and read the DAC before any retry; the app will not retry automatically.",
            )
        }
        val replacementObserved = transport.detachGeneration != detachGenerationBeforeSave
        val replacementIdentityMatched = transport.deviceFingerprintKey == expectedFingerprint &&
            (!replacementObserved || transport.sessionGeneration > sessionGenerationBeforeSave)
        afterSave(Ew300CommitObservation(replacementObserved, replacementIdentityMatched))
        if (!replacementIdentityMatched) {
            return Ew300EditorApplyResult.VerificationFailed(
                "EW300 Save returned on a different or unverified USB session. No readback was accepted.",
            )
        }
        val finalReadback = Ew300ReadbackRetry.read(
            read = {
                FinalReadback(
                    bands = readBands(),
                    gain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER),
                )
            },
            matches = { observation ->
                observation.bands == targetBands &&
                    observation.gain?.let { Ew300Protocol.globalGainMatches(it, targetGainSteps, protocolFlags) } == true
            },
        )
        if (finalReadback.bands != targetBands ||
            finalReadback.gain == null ||
            !Ew300Protocol.globalGainMatches(finalReadback.gain, targetGainSteps, protocolFlags)
        ) {
            return Ew300EditorApplyResult.VerificationFailed(
                "EW300 final readback did not match the reviewed values after Save.",
            )
        }
        return Ew300EditorApplyResult.Verified
    }

    /**
     * Runtime-only Apply: identical validation, safer-gain ordering and readback, but zero Save.
     * The returned target is valid only for the exact session that produced it.
     */
    suspend fun applyRuntime(
        workingCopy: HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
        beforeFirstWrite: () -> Unit = {},
    ): Ew300RuntimeApplyResult {
        val expectedFingerprint = transport.deviceFingerprintKey
            ?: return Ew300RuntimeApplyResult.DeviceUnavailable("The exact EW300 device fingerprint is unavailable.")
        if (workingCopy.baselineSnapshot.deviceId != DacDeviceId.SIMGOT_EW300) {
            return Ew300RuntimeApplyResult.InvalidPlan("This editor working copy does not belong to SIMGOT EW300 DSP.")
        }
        if (!workingCopy.hasChanges) {
            return Ew300RuntimeApplyResult.InvalidPlan("There are no reviewed hardware changes to apply.")
        }
        if (workingCopy.hasBlockingIssues) {
            return Ew300RuntimeApplyResult.InvalidPlan("Fix the blocking EQ values before applying.")
        }
        val headroom = workingCopy.headroomAssessment
            ?: return Ew300RuntimeApplyResult.InvalidPlan("The reviewed EQ headroom could not be assessed safely.")
        if (headroom.status != DacHeadroomStatus.SAFE) {
            return Ew300RuntimeApplyResult.InvalidPlan(
                "The reviewed EQ does not yet have a safe headroom plan. Use safe gain or revise the EQ before applying.",
            )
        }
        if (workingCopy.cautions.isNotEmpty() && !allowCautions) {
            return Ew300RuntimeApplyResult.ConfirmationRequired(workingCopy.cautions.size)
        }
        val generation = workingCopy.baselineSnapshot.sessionGeneration
        if (!isSessionCurrent(generation)) return runtimeStale()
        val baselineFilters = ordered(workingCopy.baselineSnapshot.filters)
        val target = ordered(workingCopy.filters)
        if (baselineFilters.map(HardwareEqFilter::index) != (0 until Ew300Protocol.BAND_COUNT).toList()) {
            return Ew300RuntimeApplyResult.InvalidPlan("The EW300 editor baseline does not contain all five bands.")
        }
        if (target.map(HardwareEqFilter::index) != baselineFilters.map(HardwareEqFilter::index)) {
            return Ew300RuntimeApplyResult.InvalidPlan("The reviewed EQ changed the EW300 band layout.")
        }
        val expectedBaseline = baselineFilters.map {
            encode(it) ?: return runtimeInvalidBand(it.index)
        }
        val rawBaseline = transactionCoordinator.captureBaseline()
            ?: return Ew300RuntimeApplyResult.DeviceUnavailable(
                "Could not re-read the complete EW300 state before Apply. No changes were written.",
            )
        if (!isSessionCurrent(generation)) return runtimeStale()
        val fresh = rawBaseline.bands().map { (gain, q) -> WireBandPair(gain, q) }
        if (fresh != expectedBaseline) {
            return Ew300RuntimeApplyResult.StaleBaseline(
                "The EW300 EQ changed after the editor was opened. No changes were written; read the DAC again.",
            )
        }
        val baselineGain = rawBaseline.value(Ew300Protocol.GLOBAL_GAIN_REGISTER)
            ?: return Ew300RuntimeApplyResult.DeviceUnavailable(
                "Could not re-read EW300 output gain before Apply. No changes were written.",
            )
        val protocolFlags = rawBaseline.value(Ew300Protocol.PROTOCOL_FLAGS_REGISTER)
            ?: return Ew300RuntimeApplyResult.DeviceUnavailable(
                "Could not re-read EW300 protocol layout before Apply. No changes were written.",
            )
        val expectedGainSteps = Ew300Protocol.gainDbToSteps(
            workingCopy.baselineHeadroomGainDb ?: return runtimeInvalidGain(),
        )
        val actualBaselineGainSteps = Ew300Protocol.globalGainSteps(baselineGain, protocolFlags)
            ?: return Ew300RuntimeApplyResult.StaleBaseline(
                "The EW300 stereo output-gain channels differ. No changes were written; refresh the DAC before applying.",
            )
        if (actualBaselineGainSteps != expectedGainSteps) {
            return Ew300RuntimeApplyResult.StaleBaseline(
                "The EW300 output gain changed after the editor was opened. No changes were written; read the DAC again.",
            )
        }
        val targetBands = target.map { encode(it) ?: return runtimeInvalidBand(it.index) }
        val targetGainSteps = Ew300Protocol.gainDbToSteps(
            workingCopy.plannedHeadroomGainDb ?: return runtimeInvalidGain(),
        )
        val bandsChanged = targetBands != expectedBaseline
        val gainChanged = targetGainSteps != expectedGainSteps
        if (!bandsChanged && !gainChanged) {
            return Ew300RuntimeApplyResult.InvalidPlan("There are no reviewed hardware changes to apply.")
        }

        beforeFirstWrite()
        if (gainChanged && targetGainSteps < expectedGainSteps) {
            if (!writeGain(targetGainSteps, protocolFlags)) {
                return runtimeRestoreAfterFailure(
                    baseline = rawBaseline,
                    reason = "EW300 did not accept the safer output-gain adjustment.",
                )
            }
        }
        if (bandsChanged) {
            target.forEachIndexed { index, _ ->
                val wire = targetBands[index]
                if (!isSessionCurrent(generation)) return runtimeStale()
                if (!transport.writeRegister(Ew300Protocol.bandRegister(index), wire.gain) ||
                    !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, wire.q)
                ) {
                    return runtimeRestoreAfterFailure(
                        baseline = rawBaseline,
                        reason = "EW300 stopped accepting the reviewed EQ at band ${index + 1}.",
                    )
                }
            }
        }
        if (gainChanged && targetGainSteps > expectedGainSteps && !writeGain(targetGainSteps, protocolFlags)) {
            return runtimeRestoreAfterFailure(
                baseline = rawBaseline,
                reason = "EW300 did not accept the final output-gain adjustment.",
            )
        }
        if (!isSessionCurrent(generation) || transport.deviceFingerprintKey != expectedFingerprint) {
            return runtimeStale()
        }
        val finalBands = readBands()
        val finalGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        if (finalBands != targetBands || finalGain == null ||
            !Ew300Protocol.globalGainMatches(finalGain, targetGainSteps, protocolFlags)
        ) {
            return runtimeRestoreAfterFailure(
                baseline = rawBaseline,
                reason = "EW300 runtime readback did not match the reviewed values.",
            )
        }
        val registers = buildMap {
            targetBands.forEachIndexed { index, (gain, q) ->
                put(Ew300Protocol.bandRegister(index), gain.copyOf())
                put(Ew300Protocol.bandRegister(index) + 1, q.copyOf())
            }
            put(Ew300Protocol.GLOBAL_GAIN_REGISTER, finalGain.copyOf())
        }
        return Ew300RuntimeApplyResult.Verified(
            Ew300RuntimeTarget(
                deviceFingerprintKey = expectedFingerprint,
                sessionGeneration = transport.sessionGeneration,
                detachGeneration = transport.detachGeneration,
                registers = registers,
                gainSteps = targetGainSteps,
            ),
        )
    }

    private suspend fun runtimeRestoreAfterFailure(
        baseline: Ew300RawBaseline,
        reason: String,
    ): Ew300RuntimeApplyResult {
        if (!transactionCoordinator.restoreVolatile(baseline)) {
            return Ew300RuntimeApplyResult.TransferFailed(
                "$reason Restoration of the original EW300 state also failed; reconnect and read before any retry.",
            )
        }
        return Ew300RuntimeApplyResult.TransferFailed("$reason The original EW300 state was restored and verified.")
    }

    private suspend fun writeGain(steps: Int, protocolFlags: ByteArray): Boolean {
        val current = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER) ?: return false
        val target = Ew300Protocol.withGlobalGainSteps(current, steps, protocolFlags)
        return transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, target) &&
            transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)?.contentEquals(target) == true
    }

    private suspend fun readBands(): List<WireBandPair>? = buildList {
        repeat(Ew300Protocol.BAND_COUNT) { index ->
            val gain = transport.readRegister(Ew300Protocol.bandRegister(index)) ?: return null
            val q = transport.readRegister(Ew300Protocol.bandRegister(index) + 1) ?: return null
            if (gain.size != 4 || q.size != 4) return null
            add(WireBandPair(gain, q))
        }
    }

    private fun encode(filter: HardwareEqFilter): WireBandPair? {
        val type = when (filter.type) {
            EqFilterType.PEAK -> "peak_dip"
            EqFilterType.LOW_SHELF -> "low_shelf"
            EqFilterType.HIGH_SHELF -> "high_shelf"
            else -> return null
        }
        if (type != "peak_dip" && !allowExperimentalShelf) return null
        return runCatching {
            Ew300Protocol.encodeBand(
                Kt02h20Band(
                    type = type,
                    frequencyHz = filter.frequencyHz,
                    gainDb = filter.gainDb,
                    q = filter.q,
                ),
                allowExperimentalShelf = allowExperimentalShelf,
            ).let { (gain, q) -> WireBandPair(gain, q) }
        }.getOrNull()
    }

    private suspend fun restoreAfterFailure(
        baseline: Ew300RawBaseline,
        reason: String,
    ): Ew300EditorApplyResult {
        if (!transactionCoordinator.restoreVolatile(baseline)) {
            return Ew300EditorApplyResult.TransferFailed("$reason Restoration of the original EW300 state also failed; reconnect and read before any retry.")
        }
        return Ew300EditorApplyResult.TransferFailed("$reason The original EW300 state was restored and verified.")
    }

    private data class WireBandPair(
        val gain: ByteArray,
        val q: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean = other is WireBandPair &&
            gain.contentEquals(other.gain) && q.contentEquals(other.q)

        override fun hashCode(): Int = 31 * gain.contentHashCode() + q.contentHashCode()
    }

    private data class FinalReadback(
        val bands: List<WireBandPair>?,
        val gain: ByteArray?,
    )

    private fun ordered(filters: List<HardwareEqFilter>) = filters.sortedBy(HardwareEqFilter::index)
    private fun invalidBand(index: Int) = Ew300EditorApplyResult.InvalidPlan("EW300 band ${index + 1} cannot be encoded exactly.")
    private fun invalidGain() = Ew300EditorApplyResult.InvalidPlan("The reviewed EW300 global gain is unavailable or outside the qualified range.")
    private fun stale() = Ew300EditorApplyResult.StaleBaseline("The EW300 USB session changed during Apply. Reconnect and read the DAC before retrying.")
    private fun runtimeInvalidBand(index: Int) = Ew300RuntimeApplyResult.InvalidPlan("EW300 band ${index + 1} cannot be encoded exactly.")
    private fun runtimeInvalidGain() = Ew300RuntimeApplyResult.InvalidPlan(
        "The reviewed EW300 output gain is unavailable or outside the qualified range.",
    )
    private fun runtimeStale() = Ew300RuntimeApplyResult.StaleBaseline(
        "The EW300 USB session changed during Apply. Reconnect and read the DAC before retrying.",
    )
}
