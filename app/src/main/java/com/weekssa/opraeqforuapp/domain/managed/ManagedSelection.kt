package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility

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
        return stored?.selected ?: autoIncludeNewProfiles
    }
}

fun defaultStagedSelectedProfileIds(profiles: List<OpraEqProfile>): Set<String> =
    selectableProfileIds(profiles)

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
            explicitlyExcluded = selectable && autoIncludeNewProfiles && !selected,
        )
    }
}

fun selectableProfileIds(profiles: List<OpraEqProfile>): Set<String> =
    profiles.asSequence()
        .filter { it.assessCompatibility().category.isSelectable }
        .map(OpraEqProfile::id)
        .toSet()
