package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision

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
        if (!profile.assessCompatibility().category.isSelectable) return false
        val stored = profileSelections[profile.id]
        return stored?.selected ?: (autoIncludeNewProfiles && !profile.isHistoricalRevision())
    }
}

fun defaultStagedSelectedProfileIds(profiles: List<OpraEqProfile>): Set<String> =
    selectableProfileIds(profiles, includeHistorical = false)

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
        profile == null || !profile.assessCompatibility().category.isSelectable
    }
    require(invalidSelectedIds.isEmpty()) {
        "Selection contains unknown or Not-compatible profile IDs: ${invalidSelectedIds.joinToString()}"
    }

    return profiles.associate { profile ->
        val selectable = profile.assessCompatibility().category.isSelectable
        val selected = selectable && profile.id in stagedSelectedProfileIds
        profile.id to StoredProfileSelection(
            selected = selected,
            explicitlyExcluded = selectable &&
                autoIncludeNewProfiles &&
                !profile.isHistoricalRevision() &&
                !selected,
        )
    }
}

fun selectableProfileIds(
    profiles: List<OpraEqProfile>,
    includeHistorical: Boolean = false,
): Set<String> = profiles.asSequence()
    .filter { it.assessCompatibility().category.isSelectable }
    .filter { includeHistorical || !it.isHistoricalRevision() }
    .map(OpraEqProfile::id)
    .toSet()
