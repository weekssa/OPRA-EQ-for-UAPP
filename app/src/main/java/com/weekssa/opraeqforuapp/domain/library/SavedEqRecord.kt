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
)
