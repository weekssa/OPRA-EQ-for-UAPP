package com.weekssa.opraeqforuapp.data.library

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedEqDao {
    @Query("SELECT * FROM saved_eqs ORDER BY updatedAtMillis DESC, entryId ASC")
    fun observeAll(): Flow<List<SavedEqEntity>>

    @Query("SELECT * FROM output_saved_eqs WHERE outputId = :outputId ORDER BY selectedAtMillis DESC, entryId ASC")
    fun observeOutputSelections(outputId: String): Flow<List<OutputSavedEqEntity>>

    @Query("SELECT * FROM saved_eqs WHERE entryId = :entryId LIMIT 1")
    suspend fun get(entryId: String): SavedEqEntity?

    @Query("SELECT * FROM output_saved_eqs WHERE outputId = :outputId AND entryId = :entryId LIMIT 1")
    suspend fun getSelection(outputId: String, entryId: String): OutputSavedEqEntity?

    @Query("SELECT COUNT(*) FROM output_saved_eqs WHERE entryId = :entryId")
    suspend fun selectionCount(entryId: String): Int

    @Upsert
    suspend fun upsert(entity: SavedEqEntity)

    @Upsert
    suspend fun upsertSelection(selection: OutputSavedEqEntity)

    @Query("DELETE FROM output_saved_eqs WHERE outputId = :outputId AND entryId = :entryId")
    suspend fun deleteSelection(outputId: String, entryId: String)

    @Query("DELETE FROM saved_eqs WHERE entryId = :entryId")
    suspend fun delete(entryId: String)
}
