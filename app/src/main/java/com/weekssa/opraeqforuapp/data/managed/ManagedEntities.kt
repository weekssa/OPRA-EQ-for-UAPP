package com.weekssa.opraeqforuapp.data.managed

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "managed_headphones")
data class ManagedHeadphoneEntity(
    @PrimaryKey val productId: String,
    val vendorId: String,
    val vendorName: String,
    val productName: String,
    val autoIncludeNewProfiles: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "managed_profiles",
    foreignKeys = [
        ForeignKey(
            entity = ManagedHeadphoneEntity::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("productId")],
)
data class ManagedProfileEntity(
    @PrimaryKey val profileId: String,
    val productId: String,
    val selected: Boolean,
    val explicitlyExcluded: Boolean,
    val snapshotJson: String,
    val fingerprint: String,
    val firstSeenAtMillis: Long,
    val lastSeenAtMillis: Long,
    val isNewUnreviewed: Boolean,
    val isUpdatedUnreviewed: Boolean,
    val noLongerAvailable: Boolean,
    val generatedPresetName: String?,
    val generatedXml: String?,
    val generatedFromFingerprint: String?,
    val generatedAtMillis: Long?,
)
