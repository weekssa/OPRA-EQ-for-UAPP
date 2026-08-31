package com.weekssa.opraeqforuapp.data.library

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGeneralEqDao {
    @Query("SELECT * FROM saved_general_eqs ORDER BY category, displayName COLLATE NOCASE, presetId")
    fun observeAll(): Flow<List<SavedGeneralEqEntity>>

    @Query("SELECT * FROM output_general_eqs WHERE outputId = :outputId ORDER BY selectedAtMillis, presetId")
    fun observeOutputSelections(outputId: String): Flow<List<OutputGeneralEqEntity>>

    @Query("SELECT * FROM saved_general_eqs WHERE presetId = :presetId")
    suspend fun get(presetId: String): SavedGeneralEqEntity?

    @Query("SELECT * FROM output_general_eqs WHERE outputId = :outputId AND presetId = :presetId")
    suspend fun getSelection(outputId: String, presetId: String): OutputGeneralEqEntity?

    @Query("SELECT COUNT(*) FROM output_general_eqs WHERE presetId = :presetId")
    suspend fun selectionCount(presetId: String): Int

    @Upsert
    suspend fun upsert(eq: SavedGeneralEqEntity)

    @Upsert
    suspend fun upsertSelection(selection: OutputGeneralEqEntity)

    @Query("DELETE FROM output_general_eqs WHERE outputId = :outputId AND presetId = :presetId")
    suspend fun deleteSelection(outputId: String, presetId: String)

    @Query("DELETE FROM saved_general_eqs WHERE presetId = :presetId")
    suspend fun delete(presetId: String)
}
