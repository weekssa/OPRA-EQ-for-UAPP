package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile

enum class SavedEqKind {
    Favorite,
    Personal,
    Unreadable,
}

enum class FavoriteToggleResult {
    SAVED,
    REMOVED,
    CANONICAL_SOURCE_UNAVAILABLE,
}

data class SavedEqRecord(
    val entryId: String,
    val kind: SavedEqKind,
    val sourceProfileId: String?,
    val productId: String,
    val manufacturer: String,
    val model: String,
    val displayName: String,
    val profile: OpraEqProfile,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val captureMetadata: SavedEqCaptureMetadata? = null,
    val canonicalSnapshot: LocalSavedEqSnapshot? = null,
    /** Persisted canonical, legacy, or capture data failed validation; record is display-only. */
    val savedEqDataInvalid: Boolean = false,
    /** Complete immutable catalog profile and exact selected revision for canonical Favorites. */
    val canonicalSelection: CanonicalEqSelection? = null,
) {
    val hasHeadphoneAssociation: Boolean
        get() = manufacturer.isNotBlank() && model.isNotBlank()

    /**
     * Validated compatibility view for an action boundary. Source-neutral local snapshots remain
     * authoritative when present; legacy-only rows keep their existing read/action path.
     */
    fun actionProfileOrNull(): OpraEqProfile? {
        if (savedEqDataInvalid) return null
        canonicalSelection?.let { selection ->
            return runCatching {
                CanonicalLegacyCatalogAdapter.projectSelection(selection, productId)
            }.getOrNull()
        }
        val snapshot = canonicalSnapshot ?: return profile
        return runCatching { LocalSavedEqAdapter.projectToLegacy(snapshot, productId) }.getOrNull()
    }
}
