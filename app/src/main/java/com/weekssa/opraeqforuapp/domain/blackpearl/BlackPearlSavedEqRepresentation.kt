package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeBandFingerprint
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeFingerprint
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqFingerprint
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqIdentity
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqRepresentation
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity

/**
 * Deterministic Black Pearl target representation for one saved canonical/local EQ.
 *
 * The native fingerprint is derived by decoding the exact PEQ write reports produced by the existing
 * qualified Flash planner. Ordinary Black Pearl playback/global gain is deliberately retained only
 * as context and is not part of native EQ identity because it also represents user device volume.
 */
data class BlackPearlSavedEqRepresentation(
    val fingerprint: HardwareEqNativeFingerprint,
    val fidelity: DevicePresetFidelity,
    val adaptationSummary: String,
    val requiredPlaybackGainDb: Double,
    val representationVersion: Int,
    val warning: String?,
) {
    init {
        require(fingerprint.deviceId == DacDeviceId.TRN_BLACK_PEARL)
        require(adaptationSummary.isNotBlank())
        require(requiredPlaybackGainDb.isFinite())
        require(representationVersion > 0)
    }

    fun asSavedFingerprint(identity: SavedHardwareEqIdentity): SavedHardwareEqFingerprint =
        SavedHardwareEqFingerprint(identity = identity, fingerprint = fingerprint)

    fun asSavedRepresentation(identity: SavedHardwareEqIdentity): SavedHardwareEqRepresentation =
        SavedHardwareEqRepresentation(
            identity = identity,
            fingerprint = fingerprint,
            fidelity = fidelity,
            adaptationSummary = adaptationSummary,
            representationVersion = representationVersion,
        )
}

sealed interface BlackPearlSavedEqRepresentationResult {
    data class Ready(
        val representation: BlackPearlSavedEqRepresentation,
    ) : BlackPearlSavedEqRepresentationResult

    data class NotRepresentable(
        val reason: String,
    ) : BlackPearlSavedEqRepresentationResult
}

/** Reuses the exact current Black Pearl Flash plan as the authority for saved native EQ identity. */
object BlackPearlSavedEqRepresentationDeriver {
    fun derive(profile: OpraEqProfile): BlackPearlSavedEqRepresentationResult {
        val plan = when (val candidate = buildBlackPearlFlashPlan(profile, activeSlot = 0)) {
            is BlackPearlFlashPlan.NotRepresentable ->
                return BlackPearlSavedEqRepresentationResult.NotRepresentable(candidate.reason)
            is BlackPearlFlashPlan.Ready -> candidate
        }

        val bandReports = plan.reports.take(BlackPearlProtocol.BAND_COUNT)
        if (bandReports.size != BlackPearlProtocol.BAND_COUNT) {
            return BlackPearlSavedEqRepresentationResult.NotRepresentable(
                "The derived Black Pearl representation did not contain all native EQ bands.",
            )
        }

        val bands = bandReports.mapIndexed { expectedIndex, report ->
            val native = BlackPearlReadCodec.bandFromWriteReport(report)
                ?: return BlackPearlSavedEqRepresentationResult.NotRepresentable(
                    "The derived Black Pearl EQ band ${expectedIndex + 1} could not be decoded safely.",
                )
            if (native.index != expectedIndex) {
                return BlackPearlSavedEqRepresentationResult.NotRepresentable(
                    "The derived Black Pearl EQ band order was inconsistent.",
                )
            }
            HardwareEqNativeBandFingerprint(
                index = native.index,
                enabled = true,
                type = native.type,
                frequencyUnits = native.frequencyRawHz.toLong(),
                gainUnits = native.gainRaw256.toLong(),
                qUnits = native.qRaw256.toLong(),
            )
        }

        return BlackPearlSavedEqRepresentationResult.Ready(
            BlackPearlSavedEqRepresentation(
                fingerprint = HardwareEqNativeFingerprint(
                    deviceId = DacDeviceId.TRN_BLACK_PEARL,
                    eqEnabled = true,
                    bands = bands,
                    // Command 0x03 is ordinary playback/global gain, not a dedicated EQ preamp.
                    dedicatedEqPreampUnits = null,
                ),
                fidelity = plan.fidelity,
                adaptationSummary = plan.adaptationSummary,
                requiredPlaybackGainDb = plan.requiredPlaybackGainDb,
                representationVersion = plan.representationVersion,
                warning = plan.warning,
            ),
        )
    }
}
