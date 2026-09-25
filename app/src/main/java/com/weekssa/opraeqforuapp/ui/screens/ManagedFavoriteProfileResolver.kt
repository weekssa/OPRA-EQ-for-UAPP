package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile

/**
 * Uses the current canonical projection when a managed row still contains an older snapshot.
 *
 * The persisted snapshot remains the safe fallback for removing an existing favorite or for
 * fail-closed error handling when the current catalog cannot resolve the profile.
 */
internal fun resolveManagedFavoriteProfile(
    profileId: String,
    lastKnownProfile: OpraEqProfile,
    currentProfiles: List<OpraEqProfile>,
): OpraEqProfile = currentProfiles.singleOrNull { it.id == profileId } ?: lastKnownProfile
