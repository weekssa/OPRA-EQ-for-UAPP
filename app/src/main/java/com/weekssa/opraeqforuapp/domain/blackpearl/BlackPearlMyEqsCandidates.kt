package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.SavedHardwareEqIdentity
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord

/**
 * Builds the deterministic saved-EQ candidate set for a Black Pearl-specific My EQs collection.
 *
 * Managed canonical profiles take precedence over Favorites with the same source profile ID because
 * they carry the direct headphone association. Personal and General EQ identities remain local and
 * namespaced. Only selected managed profiles are part of My EQs; retained unavailable selections are
 * still eligible because My EQs intentionally preserves them until the user removes them.
 */
fun buildBlackPearlMyEqsCandidates(
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    savedGeneralEqs: List<SavedGeneralEqRecord>,
): List<BlackPearlSavedEqCandidate> {
    val managed = managedHeadphones
        .sortedBy { it.productId }
        .flatMap { headphone ->
            headphone.profiles
                .asSequence()
                .filter { it.selected }
                .sortedBy { it.profileId }
                .map { managedProfile ->
                    val profile = managedProfile.lastKnownProfile
                    BlackPearlSavedEqCandidate(
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
        .sortedBy { it.entryId }
        .map { record ->
            val key = record.sourceProfileId
                ?.let(::canonicalKey)
                ?: "saved:${record.entryId}"
            BlackPearlSavedEqCandidate(
                identity = SavedHardwareEqIdentity(
                    savedEqKey = key,
                    displayName = displayName(record.model, record.displayName),
                ),
                profile = record.profile,
            )
        }

    val general = savedGeneralEqs
        .sortedBy { it.presetId }
        .map { record ->
            BlackPearlSavedEqCandidate(
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
