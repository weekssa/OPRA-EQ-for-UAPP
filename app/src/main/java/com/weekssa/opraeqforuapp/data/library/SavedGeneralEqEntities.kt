package com.weekssa.opraeqforuapp.data.library

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_general_eqs",
    indices = [Index("category")],
)
data class SavedGeneralEqEntity(
    @PrimaryKey val presetId: String,
    val displayName: String,
    val category: String,
    val profileJson: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "output_general_eqs",
    primaryKeys = ["outputId", "presetId"],
    indices = [Index("presetId")],
)
data class OutputGeneralEqEntity(
    val outputId: String,
    val presetId: String,
    val selectedAtMillis: Long,
)
