package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagedFavoriteProfileResolverTest {
    @Test
    fun prefersCurrentCanonicalProjectionWhenManagedSnapshotIsStale() {
        val persisted = profile(id = "profile", details = "old revision")
        val current = profile(id = "profile", details = "current revision")

        assertEquals(
            current,
            resolveManagedFavoriteProfile(
                profileId = "profile",
                lastKnownProfile = persisted,
                currentProfiles = listOf(current),
            ),
        )
    }

    @Test
    fun fallsBackToPersistedSnapshotWhenCurrentCatalogCannotResolveProfile() {
        val persisted = profile(id = "profile", details = "persisted")

        assertEquals(
            persisted,
            resolveManagedFavoriteProfile(
                profileId = "profile",
                lastKnownProfile = persisted,
                currentProfiles = emptyList(),
            ),
        )
    }

    private fun profile(id: String, details: String) = OpraEqProfile(
        id = id,
        productId = "product",
        author = "Test",
        details = details,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = emptyList(),
    )
}
