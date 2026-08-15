package com.weekssa.opraeqforuapp.domain.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile

data class ManagedProfileRecord(
    val profileId: String,
    val selected: Boolean,
    val explicitlyExcluded: Boolean,
    val lastKnownProfile: OpraEqProfile,
    val fingerprint: String,
    val firstSeenAtMillis: Long,
    val lastSeenAtMillis: Long,
    val isNewUnreviewed: Boolean,
    val isUpdatedUnreviewed: Boolean,
    val noLongerAvailable: Boolean,
)

data class ManagedHeadphoneRecord(
    val productId: String,
    val vendorId: String,
    val vendorName: String,
    val productName: String,
    val autoIncludeNewProfiles: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val profiles: List<ManagedProfileRecord>,
) {
    fun toSelectionState(): ManagedHeadphoneSelection = ManagedHeadphoneSelection(
        productId = productId,
        autoIncludeNewProfiles = autoIncludeNewProfiles,
        profileSelections = profiles.associate { profile ->
            profile.profileId to StoredProfileSelection(
                selected = profile.selected,
                explicitlyExcluded = profile.explicitlyExcluded,
            )
        },
    )

    val selectedProfileCount: Int
        get() = profiles.count { it.selected }
}
