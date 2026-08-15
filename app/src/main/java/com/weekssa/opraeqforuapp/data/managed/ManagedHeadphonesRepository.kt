package com.weekssa.opraeqforuapp.data.managed

import androidx.room.withTransaction
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneSelection
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import com.weekssa.opraeqforuapp.domain.managed.selectionUpdatesForSave
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ManagedHeadphonesRepository(
    private val database: OpraEqDatabase,
    private val snapshotCodec: ManagedProfileSnapshotCodec = ManagedProfileSnapshotCodec(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.managedHeadphonesDao()

    fun observeHeadphones(): Flow<List<ManagedHeadphoneRecord>> =
        dao.observeHeadphones().map { headphones ->
            headphones.map { headphone ->
                val profiles = dao.getProfiles(headphone.productId)
                headphone.toDomain(profiles, snapshotCodec)
            }
        }

    fun observeHeadphone(productId: String): Flow<ManagedHeadphoneRecord?> =
        combine(
            dao.observeHeadphone(productId),
            dao.observeProfiles(productId),
        ) { headphone, profiles ->
            headphone?.toDomain(profiles, snapshotCodec)
        }

    suspend fun getHeadphone(productId: String): ManagedHeadphoneRecord? {
        val headphone = dao.getHeadphone(productId) ?: return null
        return headphone.toDomain(dao.getProfiles(productId), snapshotCodec)
    }

    suspend fun getSelectionState(productId: String): ManagedHeadphoneSelection? =
        getHeadphone(productId)?.toSelectionState()

    suspend fun saveSelection(
        catalog: OpraCatalog,
        productId: String,
        stagedSelectedProfileIds: Set<String>,
        autoIncludeNewProfiles: Boolean,
    ) {
        val product = requireNotNull(catalog.product(productId)) {
            "Cannot manage unknown OPRA product $productId."
        }
        val vendor = requireNotNull(catalog.vendor(product.vendorId)) {
            "Cannot manage OPRA product $productId without its vendor."
        }
        val currentProfiles = catalog.profilesForProduct(productId)
        val selectionUpdates = selectionUpdatesForSave(
            profiles = currentProfiles,
            stagedSelectedProfileIds = stagedSelectedProfileIds,
            autoIncludeNewProfiles = autoIncludeNewProfiles,
        )
        val now = nowMillis()

        database.withTransaction {
            val existingHeadphone = dao.getHeadphone(productId)
            val existingProfiles = dao.getProfiles(productId).associateBy(ManagedProfileEntity::profileId)
            dao.upsertHeadphone(
                ManagedHeadphoneEntity(
                    productId = product.id,
                    vendorId = vendor.id,
                    vendorName = vendor.name,
                    productName = product.name,
                    autoIncludeNewProfiles = autoIncludeNewProfiles,
                    createdAtMillis = existingHeadphone?.createdAtMillis ?: now,
                    updatedAtMillis = now,
                ),
            )
            dao.upsertProfiles(
                currentProfiles.map { profile ->
                    val existing = existingProfiles[profile.id]
                    val selection = requireNotNull(selectionUpdates[profile.id])
                    ManagedProfileEntity(
                        profileId = profile.id,
                        productId = product.id,
                        selected = selection.selected,
                        explicitlyExcluded = selection.explicitlyExcluded,
                        snapshotJson = snapshotCodec.encode(profile),
                        fingerprint = snapshotCodec.fingerprint(profile),
                        firstSeenAtMillis = existing?.firstSeenAtMillis ?: now,
                        lastSeenAtMillis = now,
                        isNewUnreviewed = existing?.isNewUnreviewed ?: false,
                        isUpdatedUnreviewed = existing?.isUpdatedUnreviewed ?: false,
                        noLongerAvailable = false,
                    )
                },
            )
        }
    }

    suspend fun removeHeadphone(productId: String) {
        dao.deleteHeadphone(productId)
    }

    suspend fun markReviewed(productId: String) {
        dao.markReviewed(productId)
    }
}

private fun ManagedHeadphoneEntity.toDomain(
    profiles: List<ManagedProfileEntity>,
    snapshotCodec: ManagedProfileSnapshotCodec,
) = ManagedHeadphoneRecord(
    productId = productId,
    vendorId = vendorId,
    vendorName = vendorName,
    productName = productName,
    autoIncludeNewProfiles = autoIncludeNewProfiles,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    profiles = profiles.map { profile -> profile.toDomain(snapshotCodec) },
)

private fun ManagedProfileEntity.toDomain(snapshotCodec: ManagedProfileSnapshotCodec) = ManagedProfileRecord(
    profileId = profileId,
    selected = selected,
    explicitlyExcluded = explicitlyExcluded,
    lastKnownProfile = snapshotCodec.decode(snapshotJson),
    fingerprint = fingerprint,
    firstSeenAtMillis = firstSeenAtMillis,
    lastSeenAtMillis = lastSeenAtMillis,
    isNewUnreviewed = isNewUnreviewed,
    isUpdatedUnreviewed = isUpdatedUnreviewed,
    noLongerAvailable = noLongerAvailable,
)
