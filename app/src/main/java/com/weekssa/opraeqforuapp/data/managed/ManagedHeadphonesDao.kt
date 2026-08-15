package com.weekssa.opraeqforuapp.data.managed

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedHeadphonesDao {
    @Query(
        """
        SELECT * FROM managed_headphones
        ORDER BY vendorName COLLATE NOCASE, productName COLLATE NOCASE, productId
        """,
    )
    fun observeHeadphones(): Flow<List<ManagedHeadphoneEntity>>

    @Query("SELECT * FROM managed_headphones WHERE productId = :productId")
    fun observeHeadphone(productId: String): Flow<ManagedHeadphoneEntity?>

    @Query("SELECT * FROM managed_profiles WHERE productId = :productId ORDER BY profileId")
    fun observeProfiles(productId: String): Flow<List<ManagedProfileEntity>>

    @Query("SELECT * FROM managed_headphones WHERE productId = :productId")
    suspend fun getHeadphone(productId: String): ManagedHeadphoneEntity?

    @Query("SELECT * FROM managed_profiles WHERE productId = :productId ORDER BY profileId")
    suspend fun getProfiles(productId: String): List<ManagedProfileEntity>

    @Upsert
    suspend fun upsertHeadphone(headphone: ManagedHeadphoneEntity)

    @Upsert
    suspend fun upsertProfiles(profiles: List<ManagedProfileEntity>)

    @Query("DELETE FROM managed_headphones WHERE productId = :productId")
    suspend fun deleteHeadphone(productId: String)

    @Query("UPDATE managed_profiles SET isNewUnreviewed = 0, isUpdatedUnreviewed = 0 WHERE productId = :productId")
    suspend fun markReviewed(productId: String)
}
