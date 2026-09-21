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

/** Applies the shared My DAC editor working copy through the guarded EW300 transaction. */
class Ew300EditorApplier(
    private val transport: Ew300Transport,
) {
    private val transactionCoordinator = Ew300TransactionCoordinator(transport)

    suspend fun apply(
        workingCopy: HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
        beforeFirstWrite: () -> Unit = {},
    ): Ew300EditorApplyResult {
        if (transport.deviceFingerprintKey == null) {
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
        val expectedGainSteps = Ew300Protocol.gainDbToSteps(workingCopy.baselineHeadroomGainDb ?: return invalidGain())
        if (Ew300Protocol.globalGainSteps(baselineGain) != expectedGainSteps) {
            return Ew300EditorApplyResult.StaleBaseline("The EW300 global gain changed after the editor was opened. No changes were written; read the DAC again.")
        }
        val targetBands = target.map { encode(it) ?: return invalidBand(it.index) }
        val targetGainSteps = Ew300Protocol.gainDbToSteps(workingCopy.plannedHeadroomGainDb ?: return invalidGain())
        val bandsChanged = targetBands != expectedBaseline
        val gainChanged = targetGainSteps != expectedGainSteps
        if (!bandsChanged && !gainChanged) return Ew300EditorApplyResult.InvalidPlan("There are no reviewed hardware changes to apply.")

        beforeFirstWrite()
        if (gainChanged && targetGainSteps < expectedGainSteps) {
            if (!writeGain(targetGainSteps)) {
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
        if (gainChanged && targetGainSteps > expectedGainSteps && !writeGain(targetGainSteps)) {
            return restoreAfterFailure(
                baseline = rawBaseline,
                reason = "EW300 did not accept the final global-gain adjustment.",
            )
        }

        // Apply is a persistent product transaction. The Save command may re-enumerate the USB
        // function, so the old session generation is intentionally not reused after commit.
        if (!transport.commit()) {
            return Ew300EditorApplyResult.TransferFailed(
                "EW300 Save was not confirmed. Reconnect and read the DAC before any retry; the app will not retry automatically.",
            )
        }
        val finalBands = readBands()
        val finalGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        if (finalBands != targetBands || finalGain == null || Ew300Protocol.globalGainSteps(finalGain) != targetGainSteps) {
            return Ew300EditorApplyResult.VerificationFailed(
                "EW300 final readback did not match the reviewed values after Save.",
            )
        }
        return Ew300EditorApplyResult.Verified
    }

    private suspend fun writeGain(steps: Int): Boolean {
        val current = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER) ?: return false
        val target = Ew300Protocol.withGlobalGainSteps(current, steps)
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

    private fun encode(filter: HardwareEqFilter): WireBandPair? = runCatching {
        Ew300Protocol.encodeBand(
            Kt02h20Band(
                type = when (filter.type) {
                    EqFilterType.PEAK -> "peak_dip"
                    EqFilterType.LOW_SHELF -> "low_shelf"
                    EqFilterType.HIGH_SHELF -> "high_shelf"
                    else -> error("Unsupported EW300 editor filter type: ${filter.type}")
                },
                frequencyHz = filter.frequencyHz,
                gainDb = filter.gainDb,
                q = filter.q,
            ),
        ).let { (gain, q) -> WireBandPair(gain, q) }
    }.getOrNull()

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

    private fun ordered(filters: List<HardwareEqFilter>) = filters.sortedBy(HardwareEqFilter::index)
    private fun invalidBand(index: Int) = Ew300EditorApplyResult.InvalidPlan("EW300 band ${index + 1} cannot be encoded exactly.")
    private fun invalidGain() = Ew300EditorApplyResult.InvalidPlan("The reviewed EW300 global gain is unavailable or outside the qualified range.")
    private fun stale() = Ew300EditorApplyResult.StaleBaseline("The EW300 USB session changed during Apply. Reconnect and read the DAC before retrying.")
}
