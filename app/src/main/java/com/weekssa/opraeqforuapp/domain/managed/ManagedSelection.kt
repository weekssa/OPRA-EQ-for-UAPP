package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.catalog.isUsableParametricSource

/** New managed headphones notify about newly discovered EQs by default. */
const val DEFAULT_NOTIFY_NEW_PROFILES = true

/**
 * Legacy source/storage name retained through v0.3 migrations. It no longer means silent
 * selection; it is the per-headphone "Notify me about new EQs" preference.
 */
@Deprecated("Use DEFAULT_NOTIFY_NEW_PROFILES")
const val DEFAULT_AUTO_INCLUDE_NEW_PROFILES = DEFAULT_NOTIFY_NEW_PROFILES

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
        return profileSelections[profile.id]?.selected == true
    }
}

/** A never-managed headphone starts with an explicit empty selection. */
fun defaultStagedSelectedProfileIds(@Suppress("UNUSED_PARAMETER") profiles: List<OpraEqProfile>): Set<String> =
    emptySet()

fun managedSelectionCommitEnabled(
    isManaged: Boolean,
    stagedSelectedProfileIds: Set<String>,
    baselineSelectedProfileIds: Set<String>,
    @Suppress("UNUSED_PARAMETER") autoIncludeNewProfiles: Boolean,
    @Suppress("UNUSED_PARAMETER") baselineAutoIncludeNewProfiles: Boolean,
): Boolean {
    if (!isManaged) return stagedSelectedProfileIds.isNotEmpty()
    return stagedSelectedProfileIds != baselineSelectedProfileIds
}

fun selectionUpdatesForSave(
    profiles: List<OpraEqProfile>,
    stagedSelectedProfileIds: Set<String>,
    @Suppress("UNUSED_PARAMETER") autoIncludeNewProfiles: Boolean,
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
        val selected = profile.isUsableParametricSource() && profile.id in stagedSelectedProfileIds
        profile.id to StoredProfileSelection(
            selected = selected,
            explicitlyExcluded = false,
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
