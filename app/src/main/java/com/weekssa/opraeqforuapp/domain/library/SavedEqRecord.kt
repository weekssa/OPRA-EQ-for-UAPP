package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile

enum class SavedEqKind {
    Favorite,
    Personal,
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
) {
    val hasHeadphoneAssociation: Boolean
        get() = manufacturer.isNotBlank() && model.isNotBlank()
}
