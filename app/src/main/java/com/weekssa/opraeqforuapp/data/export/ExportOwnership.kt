package com.weekssa.opraeqforuapp.data.export

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity(
    tableName = "export_ownership",
    indices = [
        Index("profileId"),
        Index("productId"),
        Index("treeUri"),
    ],
)
data class ExportOwnershipEntity(
    @PrimaryKey val documentUri: String,
    val treeUri: String,
    val relativeDirectory: String,
    val profileId: String,
    val productId: String,
    /** Actual display name returned by the document provider for this owned URI. */
    val fileName: String,
    val exportedFingerprint: String,
    val exportedContentHash: String,
    val exportedAtMillis: Long,
)

@Dao
interface ExportOwnershipDao {
    @Query("SELECT * FROM export_ownership WHERE documentUri = :documentUri")
    suspend fun getByDocumentUri(documentUri: String): ExportOwnershipEntity?

    @Query("SELECT * FROM export_ownership WHERE profileId = :profileId")
    suspend fun getForProfile(profileId: String): List<ExportOwnershipEntity>

    @Query("SELECT * FROM export_ownership WHERE productId = :productId")
    suspend fun getForProduct(productId: String): List<ExportOwnershipEntity>

    @Query(
        """
        SELECT * FROM export_ownership
        WHERE profileId = :profileId
          AND productId = :productId
          AND treeUri = :treeUri
          AND relativeDirectory = :relativeDirectory
        ORDER BY exportedAtMillis DESC
        """,
    )
    suspend fun getForExportIdentity(
        profileId: String,
        productId: String,
        treeUri: String,
        relativeDirectory: String,
    ): List<ExportOwnershipEntity>

    @Upsert
    suspend fun upsert(ownership: ExportOwnershipEntity)

    @Query("DELETE FROM export_ownership WHERE documentUri = :documentUri")
    suspend fun delete(documentUri: String)
}
