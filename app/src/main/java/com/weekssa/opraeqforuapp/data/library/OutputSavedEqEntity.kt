package com.weekssa.opraeqforuapp.data.library

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "output_saved_eqs",
    primaryKeys = ["outputId", "entryId"],
    indices = [Index("entryId")],
)
data class OutputSavedEqEntity(
    val outputId: String,
    val entryId: String,
    val selectedAtMillis: Long,
)
