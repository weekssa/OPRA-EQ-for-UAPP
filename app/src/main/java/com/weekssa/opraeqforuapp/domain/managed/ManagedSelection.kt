package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.catalog.isUsableParametricSource

const val DEFAULT_AUTO_INCLUDE_NEW_PROFILES = true

data class StoredProfileSelection(
    val selected: Boolean,
    val explicitlyExcluded: Boolean,
)

data class ManagedHeadphoneSelection(
    val productId: String,
    val autoIncludeNewProfiles: Boolean,
    val profileSelections: Map<String, StoredProfileSelection>,
) {
    fun isSelected(profile: OpraEqProfile): Boolean {
        if (!profile.isUsableParametricSource()) return false
        val stored = profileSelections[profile.id]
        return stored?.selected ?: (
            autoIncludeNewProfiles &&
                profile.isVerified &&
                !profile.isHistoricalRevision()
            )
    }
}

/**
 * First-use defaults silently select only verified current usable canonical PEQ sources.
 * Unverified profiles remain manually selectable, including through an explicit Select all action,
 * but never enter a user's library merely because the automatic-new-profile setting defaults ON.
 * Output capability is deliberately not a selection gate.
 */
fun defaultStagedSelectedProfileIds(profiles: List<OpraEqProfile>): Set<String> =
    selectableProfileIds(profiles, includeHistorical = false, verifiedOnly = true)

fun managedSelectionCommitEnabled(
    isManaged: Boolean,
    stagedSelectedProfileIds: Set<String>,
    baselineSelectedProfileIds: Set<String>,
    autoIncludeNewProfiles: Boolean,
    baselineAutoIncludeNewProfiles: Boolean,
): Boolean {
    if (!isManaged) return stagedSelectedProfileIds.isNotEmpty()
    return stagedSelectedProfileIds != baselineSelectedProfileIds ||
        autoIncludeNewProfiles != baselineAutoIncludeNewProfiles
}

fun selectionUpdatesForSave(
    profiles: List<OpraEqProfile>,
    stagedSelectedProfileIds: Set<String>,
    autoIncludeNewProfiles: Boolean,
): Map<String, StoredProfileSelection> {
    val profileById = profiles.associateBy(OpraEqProfile::id)
    val invalidSelectedIds = stagedSelectedProfileIds.filter { profileId ->
        val profile = profileById[profileId]
        profile == null || !profile.isUsableParametricSource()
    }
    require(invalidSelectedIds.isEmpty()) {
        "Selection contains unknown or unusable source profile IDs: ${invalidSelectedIds.joinToString()}"
    }

    return profiles.associate { profile ->
        val selectable = profile.isUsableParametricSource()
        val selected = selectable && profile.id in stagedSelectedProfileIds
        profile.id to StoredProfileSelection(
            selected = selected,
            // An Unverified profile is already suppressed from silent inclusion by trust state.
            // Leaving it unchecked must not become a permanent explicit exclusion merely because
            // the user saved the headphone before that profile was independently verified.
            explicitlyExcluded = selectable &&
                profile.isVerified &&
                autoIncludeNewProfiles &&
                !profile.isHistoricalRevision() &&
                !selected,
        )
    }
}

fun selectableProfileIds(
    profiles: List<OpraEqProfile>,
    includeHistorical: Boolean = false,
    verifiedOnly: Boolean = false,
): Set<String> = profiles.asSequence()
    .filter(OpraEqProfile::isUsableParametricSource)
    .filter { includeHistorical || !it.isHistoricalRevision() }
    .filter { !verifiedOnly || it.isVerified }
    .map(OpraEqProfile::id)
    .toSet()
