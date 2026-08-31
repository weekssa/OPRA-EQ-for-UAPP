package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqCategory
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile

data class SavedGeneralEqRecord(
    val presetId: String,
    val displayName: String,
    val category: GeneralEqCategory,
    val profile: OpraEqProfile,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
