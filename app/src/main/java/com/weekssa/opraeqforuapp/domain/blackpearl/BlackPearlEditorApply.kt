package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacHeadroomStatus
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqFilter
import com.weekssa.opraeqforuapp.domain.library.EqFilterType

sealed interface BlackPearlEditorApplyResult {
    data class Verified(
        val appliedTrackedGainDb: Double,
    ) : BlackPearlEditorApplyResult

    data class ConfirmationRequired(
        val cautionCount: Int,
    ) : BlackPearlEditorApplyResult

    data class InvalidPlan(val reason: String) : BlackPearlEditorApplyResult
    data class StaleBaseline(val reason: String) : BlackPearlEditorApplyResult
    data class DeviceUnavailable(val reason: String) : BlackPearlEditorApplyResult
    data class TransferFailed(val reason: String) : BlackPearlEditorApplyResult
    data class VerificationFailed(val reason: String) : BlackPearlEditorApplyResult
}

/**
 * Applies one reviewed local Black Pearl editor working copy using only the already-qualified PEQ,
 * latch/save, and tracked playback-gain commands.
 *
 * The transaction refuses to start if the hardware EQ or EQ Library's tracked gain baseline changed
 * since editor entry. More attenuation is applied before changed EQ; any louder gain is deferred until
 * the changed EQ has been persisted and read back exactly. Success is returned only after final native
 * band and global-gain readback verification.
 */
internal class BlackPearlEditorApplier(
    private val transport: BlackPearlTransport,
    private val gainStateStore: BlackPearlGainStateStore,
) {
    suspend fun apply(
        workingCopy: HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
        isSessionCurrent: (Long) -> Boolean,
    ): BlackPearlEditorApplyResult {
        preflight(workingCopy, allowCautions)?.let { return it }

        val expectedGeneration = workingCopy.baselineSnapshot.sessionGeneration
        if (!isSessionCurrent(expectedGeneration)) {
            return BlackPearlEditorApplyResult.StaleBaseline(
                "The Black Pearl USB session changed after the editor was opened. Read the DAC again before applying.",
            )
        }

        val activeSlot = workingCopy.baselineSnapshot.activeSlot
            ?: return BlackPearlEditorApplyResult.InvalidPlan("The editor baseline has no active Black Pearl EQ slot.")
        if (activeSlot !in 0..0xff) {
            return BlackPearlEditorApplyResult.InvalidPlan("The editor baseline has an invalid Black Pearl EQ slot.")
        }
        val slotByte = activeSlot.toByte()
        val orderedBaseline = workingCopy.baselineSnapshot.filters.sortedBy(HardwareEqFilter::index)
        val orderedWorking = workingCopy.filters.sortedBy(HardwareEqFilter::index)
        if (orderedBaseline.map(HardwareEqFilter::index) != (0 until BlackPearlProtocol.BAND_COUNT).toList()) {
            return BlackPearlEditorApplyResult.InvalidPlan("The editor baseline does not contain the complete Black Pearl band layout.")
        }

        val expectedBaseline = orderedBaseline.map { filter ->
            expectedNativeBand(filter, slotByte)
                ?: return BlackPearlEditorApplyResult.InvalidPlan("The editor baseline cannot be encoded exactly for Black Pearl.")
        }
        val freshBands = readBandsOrNull()
            ?: return BlackPearlEditorApplyResult.DeviceUnavailable(
                "Couldn’t re-read all Black Pearl EQ bands before Apply. No editor changes were written.",
            )
        if (!isSessionCurrent(expectedGeneration)) {
            return BlackPearlEditorApplyResult.StaleBaseline(
                "The Black Pearl USB session changed while the editor baseline was being verified. No editor changes were written.",
            )
        }
        if (freshBands != expectedBaseline) {
            return BlackPearlEditorApplyResult.StaleBaseline(
                "The Black Pearl EQ changed after the editor was opened. No editor changes were written; read the DAC again.",
            )
        }

        val baselineTrackedGainDb = workingCopy.baselineHeadroomGainDb
            ?: return BlackPearlEditorApplyResult.InvalidPlan("The Black Pearl tracked EQ gain baseline is unavailable.")
        val plannedTrackedGainDb = workingCopy.plannedHeadroomGainDb
            ?: return BlackPearlEditorApplyResult.InvalidPlan("The Black Pearl planned EQ safety gain is unavailable.")
        val baselineTrackedRaw = BlackPearlProtocol.gainDbToRawDelta(baselineTrackedGainDb)
        val currentTrackedRaw = gainStateStore.readAppliedGainDeltaRaw()
        if (currentTrackedRaw != baselineTrackedRaw) {
            return BlackPearlEditorApplyResult.StaleBaseline(
                "EQ Library’s tracked Black Pearl gain changed after the editor was opened. No editor changes were written.",
            )
        }

        val currentGainRaw = transport.readGlobalGainRaw()
            ?: return BlackPearlEditorApplyResult.DeviceUnavailable(
                "Couldn’t re-read Black Pearl playback gain before Apply. No editor changes were written.",
            )
        if (!isSessionCurrent(expectedGeneration)) {
            return BlackPearlEditorApplyResult.StaleBaseline(
                "The Black Pearl USB session changed before Apply. No editor changes were written.",
            )
        }

        val userVolumeBaselineRaw = currentGainRaw - currentTrackedRaw
        val plannedTrackedRaw = BlackPearlProtocol.gainDbToRawDelta(plannedTrackedGainDb)
        val targetGainRaw = userVolumeBaselineRaw + plannedTrackedRaw
        if (targetGainRaw !in BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW..BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW) {
            return BlackPearlEditorApplyResult.InvalidPlan(
                "The reviewed EQ safety adjustment would exceed the Black Pearl’s validated playback-gain range. Adjust playback volume and review again.",
            )
        }

        val filtersChanged = workingCopy.differences.isNotEmpty()
        val gainChanged = targetGainRaw != currentGainRaw
        if (!filtersChanged && !gainChanged) {
            return BlackPearlEditorApplyResult.InvalidPlan("There are no reviewed hardware changes to apply.")
        }

        val targetBands = orderedWorking.map { filter ->
            expectedNativeBand(filter, slotByte)
                ?: return BlackPearlEditorApplyResult.InvalidPlan("A reviewed EQ band cannot be encoded exactly for Black Pearl.")
        }
        val eqReports = if (filtersChanged) {
            runCatching {
                BlackPearlProtocol.flashSequence(
                    bands = orderedWorking.map(HardwareEqFilter::toProtocolBand),
                    activeSlot = slotByte,
                )
            }.getOrElse { error ->
                return BlackPearlEditorApplyResult.InvalidPlan(
                    error.message ?: "The reviewed Black Pearl EQ cannot be represented.",
                )
            }
        } else {
            emptyList()
        }

        val gainBecomesQuieter = gainChanged && targetGainRaw < currentGainRaw
        val gainBecomesLouder = gainChanged && targetGainRaw > currentGainRaw

        if (gainBecomesQuieter) {
            if (!isSessionCurrent(expectedGeneration)) return sessionChangedDuringApply()
            if (!transport.sendReport(BlackPearlProtocol.writeGlobalGainReport(targetGainRaw))) {
                return BlackPearlEditorApplyResult.TransferFailed(
                    "Black Pearl did not accept the safer playback-gain adjustment. No EQ bands were written.",
                )
            }
            // Keep anti-stacking state aligned as soon as the accepted hardware gain may have changed.
            gainStateStore.writeAppliedGainDeltaRaw(plannedTrackedRaw)
        }

        if (filtersChanged) {
            eqReports.forEachIndexed { index, report ->
                if (!isSessionCurrent(expectedGeneration)) return sessionChangedDuringApply()
                if (!transport.sendReport(report)) {
                    return BlackPearlEditorApplyResult.TransferFailed(
                        "Black Pearl stopped accepting the reviewed EQ at step ${index + 1} of ${eqReports.size}. A safer gain adjustment, if already applied, remains tracked for retry.",
                    )
                }
            }

            val verifiedBands = readBandsOrNull()
                ?: return BlackPearlEditorApplyResult.VerificationFailed(
                    "The reviewed EQ was sent, but Black Pearl band readback could not be completed. Playback gain was not raised.",
                )
            if (!isSessionCurrent(expectedGeneration)) return sessionChangedDuringApply()
            if (verifiedBands != targetBands) {
                return BlackPearlEditorApplyResult.VerificationFailed(
                    "Black Pearl EQ readback did not match the reviewed values. Playback gain was not raised.",
                )
            }
        }

        if (gainBecomesLouder) {
            // Raising gain is last, after changed EQ has already been persisted and verified.
            if (!isSessionCurrent(expectedGeneration)) return sessionChangedDuringApply()
            if (!transport.sendReport(BlackPearlProtocol.writeGlobalGainReport(targetGainRaw))) {
                return BlackPearlEditorApplyResult.TransferFailed(
                    "The reviewed EQ is verified, but Black Pearl did not accept the final playback-gain adjustment. The previous safer gain remains tracked.",
                )
            }
            gainStateStore.writeAppliedGainDeltaRaw(plannedTrackedRaw)
        }

        val finalGainRaw = transport.readGlobalGainRaw()
            ?: return BlackPearlEditorApplyResult.VerificationFailed(
                "Couldn’t verify Black Pearl playback gain after Apply.",
            )
        if (!isSessionCurrent(expectedGeneration)) return sessionChangedDuringApply()
        if (finalGainRaw != targetGainRaw) {
            // Reconcile local anti-stacking state to the actual verified hardware result when possible.
            gainStateStore.writeAppliedGainDeltaRaw(finalGainRaw - userVolumeBaselineRaw)
            return BlackPearlEditorApplyResult.VerificationFailed(
                "Black Pearl playback-gain readback did not match the reviewed value.",
            )
        }

        if (!gainChanged && currentTrackedRaw != plannedTrackedRaw) {
            // Defensive consistency; target equality normally implies identical tracked delta.
            gainStateStore.writeAppliedGainDeltaRaw(plannedTrackedRaw)
        }

        return BlackPearlEditorApplyResult.Verified(
            appliedTrackedGainDb = BlackPearlProtocol.rawDeltaToGainDb(plannedTrackedRaw),
        )
    }

    private fun preflight(
        workingCopy: HardwareEqEditWorkingCopy,
        allowCautions: Boolean,
    ): BlackPearlEditorApplyResult? {
        if (workingCopy.baselineSnapshot.deviceId != DacDeviceId.TRN_BLACK_PEARL) {
            return BlackPearlEditorApplyResult.InvalidPlan("This editor working copy does not belong to a TRN Black Pearl.")
        }
        if (!workingCopy.hasChanges) {
            return BlackPearlEditorApplyResult.InvalidPlan("There are no reviewed hardware changes to apply.")
        }
        if (workingCopy.hasBlockingIssues) {
            return BlackPearlEditorApplyResult.InvalidPlan("Fix the blocking EQ values before applying.")
        }
        val headroom = workingCopy.headroomAssessment
            ?: return BlackPearlEditorApplyResult.InvalidPlan("The reviewed EQ headroom could not be assessed safely.")
        if (headroom.status != DacHeadroomStatus.SAFE) {
            return BlackPearlEditorApplyResult.InvalidPlan(
                "The reviewed EQ does not yet have a safe headroom plan. Use safe gain or revise the EQ before applying.",
            )
        }
        if (workingCopy.cautions.isNotEmpty() && !allowCautions) {
            return BlackPearlEditorApplyResult.ConfirmationRequired(workingCopy.cautions.size)
        }
        return null
    }

    private suspend fun readBandsOrNull(): List<BlackPearlReadCodec.NativeBand>? = buildList {
        repeat(BlackPearlProtocol.BAND_COUNT) { index ->
            add(transport.readNativeBand(index) ?: return null)
        }
    }

    private fun expectedNativeBand(
        filter: HardwareEqFilter,
        activeSlot: Byte,
    ): BlackPearlReadCodec.NativeBand? = runCatching {
        BlackPearlReadCodec.bandFromWriteReport(
            BlackPearlProtocol.writeBandReport(
                index = filter.index,
                band = filter.toProtocolBand(),
                activeSlot = activeSlot,
            ),
        )
    }.getOrNull()

    private fun HardwareEqFilter.toProtocolBand(): BlackPearlProtocol.Band = BlackPearlProtocol.Band(
        type = when (type) {
            EqFilterType.PEAK -> "peak_dip"
            EqFilterType.LOW_SHELF -> "low_shelf"
            EqFilterType.HIGH_SHELF -> "high_shelf"
            else -> error("Unsupported Black Pearl editor filter type: $type")
        },
        frequencyHz = frequencyHz,
        gainDb = gainDb,
        q = q,
    )

    private fun sessionChangedDuringApply(): BlackPearlEditorApplyResult.TransferFailed =
        BlackPearlEditorApplyResult.TransferFailed(
            "The Black Pearl disconnected or its USB session changed during Apply. Reconnect and read the DAC before retrying.",
        )
}
