package com.weekssa.opraeqforuapp.domain.ew300

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatchResolution
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatcher
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeBandFingerprint
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeFingerprint
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqIdentity
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqRepresentation
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.adaptationSummary
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import kotlin.math.roundToLong

data class Ew300SavedEqCandidate(
    val identity: SavedHardwareEqIdentity,
    val profile: OpraEqProfile,
)

data class Ew300SavedEqRepresentation(
    val fingerprint: HardwareEqNativeFingerprint,
    val fidelity: DevicePresetFidelity,
    val adaptationSummary: String,
    val representationVersion: Int,
) {
    init {
        require(fingerprint.deviceId == DacDeviceId.SIMGOT_EW300)
        require(adaptationSummary.isNotBlank())
        require(representationVersion > 0)
    }

    fun asSavedRepresentation(identity: SavedHardwareEqIdentity): SavedHardwareEqRepresentation =
        SavedHardwareEqRepresentation(
            identity = identity,
            fingerprint = fingerprint,
            fidelity = fidelity,
            adaptationSummary = adaptationSummary,
            representationVersion = representationVersion,
        )
}

sealed interface Ew300SavedEqRepresentationResult {
    data class Ready(val representation: Ew300SavedEqRepresentation) : Ew300SavedEqRepresentationResult
    data class NotRepresentable(val reason: String) : Ew300SavedEqRepresentationResult
}

/**
 * Derives the exact five-band native identity from the same optimizer and wire codec used by EW300
 * Flash. Playback/global gain is intentionally excluded because it is device state, not EQ identity.
 */
object Ew300SavedEqRepresentationDeriver {
    fun derive(profile: OpraEqProfile): Ew300SavedEqRepresentationResult {
        val representation = when (
            val result = Kt02h20FiveBandOptimizer.optimize(profile, HardwareEqDeviceSpecs.SIMGOT_EW300)
        ) {
            is FiveBandOptimizationResult.NotSuitable ->
                return Ew300SavedEqRepresentationResult.NotRepresentable(result.reason)
            is FiveBandOptimizationResult.Ready -> result.representation
        }

        if (representation.bands.size > Ew300Protocol.BAND_COUNT) {
            return Ew300SavedEqRepresentationResult.NotRepresentable(
                "The derived EW300 representation exceeds the five-band hardware budget.",
            )
        }
        val complete = representation.bands + List(Ew300Protocol.BAND_COUNT - representation.bands.size) {
            Kt02h20Band("peak_dip", 1_000.0, 0.0, 1.0)
        }
        if (complete.any { it.type != "peak_dip" }) {
            return Ew300SavedEqRepresentationResult.NotRepresentable(
                "The derived EW300 representation contains an unqualified native filter type.",
            )
        }

        val native = complete.mapIndexed { index, band ->
            val encoded = runCatching { Ew300Protocol.encodeBand(band) }.getOrElse {
                return Ew300SavedEqRepresentationResult.NotRepresentable(
                    it.message ?: "The derived EW300 band could not be encoded safely.",
                )
            }
            val decoded = Ew300Protocol.decodeBand(index, encoded.first, encoded.second)
                ?: return Ew300SavedEqRepresentationResult.NotRepresentable(
                    "The derived EW300 band could not be decoded back to native identity.",
                )
            HardwareEqNativeBandFingerprint(
                index = index,
                enabled = true,
                type = com.weekssa.opraeqforuapp.domain.library.EqFilterType.PEAK,
                frequencyUnits = decoded.frequencyHz.roundToLong(),
                gainUnits = (decoded.gainDb * 10.0).roundToLong(),
                qUnits = (decoded.q * 1000.0).roundToLong(),
            )
        }

        return Ew300SavedEqRepresentationResult.Ready(
            Ew300SavedEqRepresentation(
                fingerprint = HardwareEqNativeFingerprint(
                    deviceId = DacDeviceId.SIMGOT_EW300,
                    eqEnabled = true,
                    bands = native,
                    dedicatedEqPreampUnits = null,
                ),
                fidelity = representation.fidelity,
                adaptationSummary = representation.adaptationSummary(),
                representationVersion = representation.representationVersion,
            ),
        )
    }
}

fun resolveEw300HardwareEq(
    bundle: HardwareEqSnapshotBundle,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    savedGeneralEqs: List<SavedGeneralEqRecord>,
): HardwareEqMatchResolution {
    val representations = buildEw300MyEqsCandidates(managedHeadphones, savedEqs, savedGeneralEqs)
        .mapNotNull { candidate ->
            when (val result = Ew300SavedEqRepresentationDeriver.derive(candidate.profile)) {
                is Ew300SavedEqRepresentationResult.Ready ->
                    result.representation.asSavedRepresentation(candidate.identity)
                is Ew300SavedEqRepresentationResult.NotRepresentable -> null
            }
        }
    return HardwareEqMatcher.resolve(bundle.fingerprint, representations)
}

fun buildEw300MyEqsCandidates(
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    savedGeneralEqs: List<SavedGeneralEqRecord>,
): List<Ew300SavedEqCandidate> {
    val managed = managedHeadphones
        .sortedBy { it.productId }
        .flatMap { headphone ->
            headphone.profiles
                .asSequence()
                .filter { it.selected }
                .sortedBy { it.profileId }
                .map { managedProfile ->
                    val profile = managedProfile.lastKnownProfile
                    Ew300SavedEqCandidate(
                        identity = SavedHardwareEqIdentity(
                            savedEqKey = canonicalKey(profile.id),
                            displayName = displayName(
                                headphone.productName,
                                profile.author,
                                profile.details,
                            ),
                        ),
                        profile = profile,
                    )
                }
                .toList()
        }

    val saved = savedEqs
        .filterNot { it.savedEqDataInvalid }
        .sortedBy { it.entryId }
        .map { record ->
            Ew300SavedEqCandidate(
                identity = SavedHardwareEqIdentity(
                    savedEqKey = record.sourceProfileId?.let(::canonicalKey) ?: "saved:${record.entryId}",
                    displayName = displayName(record.model, record.displayName),
                ),
                profile = record.profile,
            )
        }

    val general = savedGeneralEqs
        .sortedBy { it.presetId }
        .map { record ->
            Ew300SavedEqCandidate(
                identity = SavedHardwareEqIdentity(
                    savedEqKey = "general:${record.presetId}",
                    displayName = record.displayName,
                ),
                profile = record.profile,
            )
        }

    return (managed + saved + general).distinctBy { it.identity.savedEqKey }
}

private fun canonicalKey(profileId: String): String = "canonical:$profileId"

private fun displayName(vararg parts: String?): String =
    parts.asSequence()
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        .distinct()
        .joinToString(" · ")
        .ifBlank { "Saved EQ" }
