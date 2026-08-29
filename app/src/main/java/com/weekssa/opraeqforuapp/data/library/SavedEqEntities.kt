package com.weekssa.opraeqforuapp.data.library

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_eqs",
    indices = [Index("kind"), Index("sourceProfileId")],
)
data class SavedEqEntity(
    @PrimaryKey val entryId: String,
    val kind: String,
    val sourceProfileId: String?,
    val productId: String,
    val manufacturer: String,
    val model: String,
    val displayName: String,
    val profileJson: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
