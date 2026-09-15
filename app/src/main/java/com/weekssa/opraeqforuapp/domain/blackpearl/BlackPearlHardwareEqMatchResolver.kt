package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatchResolution
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatcher
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeFingerprint
import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqIdentity

/** One My EQs profile that may have a deterministic Black Pearl target representation. */
data class BlackPearlSavedEqCandidate(
    val identity: SavedHardwareEqIdentity,
    val profile: OpraEqProfile,
)

/**
 * Pure resolver from an actual Black Pearl native fingerprint to deterministic saved My EQs state.
 *
 * Candidate identities are de-duplicated before target derivation so the same canonical EQ appearing
 * both as a managed profile and a Favorite cannot create false ambiguity. Profiles that cannot be
 * represented on Black Pearl are simply absent from the device-native match set.
 */
object BlackPearlHardwareEqMatchResolver {
    fun resolve(
        actual: HardwareEqNativeFingerprint,
        candidates: List<BlackPearlSavedEqCandidate>,
    ): HardwareEqMatchResolution {
        val representations = candidates
            .distinctBy { it.identity.savedEqKey }
            .mapNotNull { candidate ->
                when (val derived = BlackPearlSavedEqRepresentationDeriver.derive(candidate.profile)) {
                    is BlackPearlSavedEqRepresentationResult.NotRepresentable -> null
                    is BlackPearlSavedEqRepresentationResult.Ready ->
                        derived.representation.asSavedRepresentation(candidate.identity)
                }
            }
        return HardwareEqMatcher.resolve(actual, representations)
    }
}
