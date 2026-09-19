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
    suspend fun apply(
        workingCopy: HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
    ): Ew300EditorApplyResult {
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
        val baseline = ordered(workingCopy.baselineSnapshot.filters)
        val target = ordered(workingCopy.filters)
        if (baseline.map(HardwareEqFilter::index) != (0 until Ew300Protocol.BAND_COUNT).toList()) {
            return Ew300EditorApplyResult.InvalidPlan("The EW300 editor baseline does not contain all five bands.")
        }
        if (target.map(HardwareEqFilter::index) != baseline.map(HardwareEqFilter::index)) {
            return Ew300EditorApplyResult.InvalidPlan("The reviewed EQ changed the EW300 band layout.")
        }
        val expectedBaseline = baseline.map { encode(it) ?: return invalidBand(it.index) }
        val fresh = readBands() ?: return Ew300EditorApplyResult.DeviceUnavailable("Could not re-read all EW300 bands before Apply. No changes were written.")
        if (!isSessionCurrent(generation)) return stale()
        if (fresh != expectedBaseline) {
            return Ew300EditorApplyResult.StaleBaseline("The EW300 EQ changed after the editor was opened. No changes were written; read the DAC again.")
        }
        val baselineGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
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

        if (gainChanged && targetGainSteps < expectedGainSteps) {
            if (!writeGain(targetGainSteps)) return Ew300EditorApplyResult.TransferFailed("EW300 did not accept the safer global-gain adjustment. No EQ bands were written.")
        }
        if (bandsChanged) {
            target.forEachIndexed { index, filter ->
                val wire = targetBands[index]
                if (!isSessionCurrent(generation)) return stale()
                if (!transport.writeRegister(Ew300Protocol.bandRegister(index), wire.first) ||
                    !transport.writeRegister(Ew300Protocol.bandRegister(index) + 1, wire.second)
                ) return Ew300EditorApplyResult.TransferFailed("EW300 stopped accepting the reviewed EQ at band ${index + 1}. Reconnect and read before retrying.")
            }
            if (!transport.commit()) return Ew300EditorApplyResult.TransferFailed("EW300 accepted the reviewed bands but did not accept the persistence command.")
            if (readBands() != targetBands) return Ew300EditorApplyResult.VerificationFailed("EW300 final EQ readback did not match the reviewed values.")
        }
        if (gainChanged && targetGainSteps > expectedGainSteps && !writeGain(targetGainSteps)) {
            return Ew300EditorApplyResult.TransferFailed("The reviewed EQ is verified, but EW300 did not accept the final global-gain adjustment.")
        }
        if (!isSessionCurrent(generation)) return stale()
        val finalGain = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER)
        if (finalGain == null || Ew300Protocol.globalGainSteps(finalGain) != targetGainSteps) {
            return Ew300EditorApplyResult.VerificationFailed("EW300 final global-gain readback did not match the reviewed value.")
        }
        return Ew300EditorApplyResult.Verified
    }

    private suspend fun writeGain(steps: Int): Boolean {
        val current = transport.readRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER) ?: return false
        return transport.writeRegister(Ew300Protocol.GLOBAL_GAIN_REGISTER, Ew300Protocol.withGlobalGainSteps(current, steps))
    }

    private suspend fun readBands(): List<Pair<ByteArray, ByteArray>>? = buildList {
        repeat(Ew300Protocol.BAND_COUNT) { index ->
            val gain = transport.readRegister(Ew300Protocol.bandRegister(index)) ?: return null
            val q = transport.readRegister(Ew300Protocol.bandRegister(index) + 1) ?: return null
            if (gain.size != 4 || q.size != 4) return null
            add(gain to q)
        }
    }

    private fun encode(filter: HardwareEqFilter): Pair<ByteArray, ByteArray>? = runCatching {
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
        )
    }.getOrNull()

    private fun ordered(filters: List<HardwareEqFilter>) = filters.sortedBy(HardwareEqFilter::index)
    private fun invalidBand(index: Int) = Ew300EditorApplyResult.InvalidPlan("EW300 band ${index + 1} cannot be encoded exactly.")
    private fun invalidGain() = Ew300EditorApplyResult.InvalidPlan("The reviewed EW300 global gain is unavailable or outside the qualified range.")
    private fun stale() = Ew300EditorApplyResult.StaleBaseline("The EW300 USB session changed during Apply. Reconnect and read the DAC before retrying.")
}
