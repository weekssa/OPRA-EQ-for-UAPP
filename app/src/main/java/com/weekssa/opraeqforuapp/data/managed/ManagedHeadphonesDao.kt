package com.weekssa.opraeqforuapp.data.managed

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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

    @Query(
        """
        SELECT * FROM output_managed_headphones
        WHERE outputId = :outputId
        ORDER BY productId
        """,
    )
    fun observeOutputHeadphones(outputId: String): Flow<List<OutputManagedHeadphoneEntity>>

    @Query(
        """
        SELECT * FROM output_managed_profiles
        WHERE outputId = :outputId
        ORDER BY productId, profileId
        """,
    )
    fun observeOutputProfiles(outputId: String): Flow<List<OutputManagedProfileEntity>>

    @Query(
        """
        SELECT * FROM managed_headphones
        ORDER BY vendorName COLLATE NOCASE, productName COLLATE NOCASE, productId
        """,
    )
    suspend fun getHeadphones(): List<ManagedHeadphoneEntity>

    @Query("SELECT * FROM managed_headphones WHERE productId = :productId")
    suspend fun getHeadphone(productId: String): ManagedHeadphoneEntity?

    @Query("SELECT * FROM managed_profiles WHERE productId = :productId ORDER BY profileId")
    suspend fun getProfiles(productId: String): List<ManagedProfileEntity>

    @Query("SELECT * FROM output_managed_headphones ORDER BY outputId, productId")
    suspend fun getAllOutputHeadphones(): List<OutputManagedHeadphoneEntity>

    @Query("SELECT * FROM output_managed_headphones WHERE outputId = :outputId ORDER BY productId")
    suspend fun getOutputHeadphones(outputId: String): List<OutputManagedHeadphoneEntity>

    @Query("SELECT * FROM output_managed_headphones WHERE outputId = :outputId AND productId = :productId")
    suspend fun getOutputHeadphone(outputId: String, productId: String): OutputManagedHeadphoneEntity?

    @Query("SELECT * FROM output_managed_profiles WHERE outputId = :outputId AND productId = :productId ORDER BY profileId")
    suspend fun getOutputProfiles(outputId: String, productId: String): List<OutputManagedProfileEntity>

    @Query("SELECT * FROM output_managed_profiles WHERE productId = :productId ORDER BY outputId, profileId")
    suspend fun getOutputProfilesForProduct(productId: String): List<OutputManagedProfileEntity>

    @Query("SELECT * FROM output_managed_profiles WHERE profileId = :profileId ORDER BY outputId")
    suspend fun getOutputProfilesForProfile(profileId: String): List<OutputManagedProfileEntity>

    @Upsert
    suspend fun upsertHeadphone(headphone: ManagedHeadphoneEntity)

    @Upsert
    suspend fun upsertProfiles(profiles: List<ManagedProfileEntity>)

    @Upsert
    suspend fun upsertOutputHeadphone(headphone: OutputManagedHeadphoneEntity)

    @Upsert
    suspend fun upsertOutputProfiles(profiles: List<OutputManagedProfileEntity>)

    @Query("DELETE FROM managed_profiles WHERE profileId = :profileId AND productId = :productId")
    suspend fun deleteProfile(productId: String, profileId: String)

    @Query("DELETE FROM output_managed_profiles WHERE outputId = :outputId AND productId = :productId")
    suspend fun deleteOutputProfiles(outputId: String, productId: String)

    @Query("DELETE FROM output_managed_profiles WHERE outputId = :outputId AND productId = :productId AND profileId = :profileId")
    suspend fun deleteOutputProfile(outputId: String, productId: String, profileId: String)

    @Query("DELETE FROM output_managed_profiles WHERE outputId = :outputId AND profileId = :profileId")
    suspend fun deleteOutputProfileEverywhere(outputId: String, profileId: String)

    @Query("SELECT COUNT(*) FROM managed_profiles WHERE productId = :productId")
    suspend fun countProfiles(productId: String): Int

    @Query("SELECT COUNT(*) FROM output_managed_headphones WHERE productId = :productId")
    suspend fun countOutputsForProduct(productId: String): Int

    @Query("DELETE FROM managed_headphones WHERE productId = :productId")
    suspend fun deleteHeadphone(productId: String)

    @Query("DELETE FROM output_managed_headphones WHERE outputId = :outputId AND productId = :productId")
    suspend fun deleteOutputHeadphone(outputId: String, productId: String)

    @Query("UPDATE managed_profiles SET isNewUnreviewed = 0, isUpdatedUnreviewed = 0 WHERE productId = :productId")
    suspend fun clearReviewFlags(productId: String)

    @Query("UPDATE managed_headphones SET updatedAtMillis = :updatedAtMillis WHERE productId = :productId")
    suspend fun touchHeadphone(productId: String, updatedAtMillis: Long)

    @Transaction
    suspend fun markReviewed(productId: String) {
        clearReviewFlags(productId)
        touchHeadphone(productId, System.currentTimeMillis())
    }
}
