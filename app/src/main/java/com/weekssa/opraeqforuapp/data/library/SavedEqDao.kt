package com.weekssa.opraeqforuapp.data.library

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedEqDao {
    @Query("SELECT * FROM saved_eqs ORDER BY updatedAtMillis DESC, entryId ASC")
    fun observeAll(): Flow<List<SavedEqEntity>>

    @Query("SELECT * FROM saved_eqs WHERE entryId = :entryId LIMIT 1")
    suspend fun get(entryId: String): SavedEqEntity?

    @Query("SELECT * FROM saved_eqs WHERE kind = 'favorite' AND sourceProfileId = :profileId LIMIT 1")
    suspend fun getFavorite(profileId: String): SavedEqEntity?

    @Upsert
    suspend fun upsert(entity: SavedEqEntity)

    @Query("DELETE FROM saved_eqs WHERE entryId = :entryId")
    suspend fun delete(entryId: String)

    @Query("DELETE FROM saved_eqs WHERE kind = 'favorite' AND sourceProfileId = :profileId")
    suspend fun deleteFavorite(profileId: String)
}
