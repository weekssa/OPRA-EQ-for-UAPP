package com.weekssa.opraeqforuapp.data.managed

import androidx.room.withTransaction
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
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
                    val fingerprint = snapshotCodec.fingerprint(profile)
                    val generated = if (selection.selected) {
                        generateManagedPreset(
                            productName = product.name,
                            profile = profile,
                            fingerprint = fingerprint,
                            nowMillis = now,
                        )
                    } else {
                        null
                    }
                    ManagedProfileEntity(
                        profileId = profile.id,
                        productId = product.id,
                        selected = selection.selected,
                        explicitlyExcluded = selection.explicitlyExcluded,
                        snapshotJson = snapshotCodec.encode(profile),
                        fingerprint = fingerprint,
                        firstSeenAtMillis = existing?.firstSeenAtMillis ?: now,
                        lastSeenAtMillis = now,
                        isNewUnreviewed = existing?.isNewUnreviewed ?: false,
                        isUpdatedUnreviewed = existing?.isUpdatedUnreviewed ?: false,
                        noLongerAvailable = false,
                        generatedPresetName = generated?.presetName ?: existing?.generatedPresetName,
                        generatedXml = generated?.xml ?: existing?.generatedXml,
                        generatedFromFingerprint = generated?.fingerprint ?: existing?.generatedFromFingerprint,
                        generatedAtMillis = generated?.generatedAtMillis ?: existing?.generatedAtMillis,
                    )
                },
            )
        }
    }

    suspend fun reconcileCatalog(catalog: OpraCatalog): ManagedCatalogChangeSummary =
        database.withTransaction {
            val now = nowMillis()
            var summary = ManagedCatalogChangeSummary()

            dao.getHeadphones().forEach { headphone ->
                val product = catalog.product(headphone.productId)
                val vendor = product?.let { catalog.vendor(it.vendorId) }
                val currentProfiles = if (product != null) {
                    catalog.profilesForProduct(product.id)
                } else {
                    emptyList()
                }
                val reconciled = reconcileManagedProfiles(
                    productId = headphone.productId,
                    productName = product?.name ?: headphone.productName,
                    currentProfiles = currentProfiles,
                    existingProfiles = dao.getProfiles(headphone.productId),
                    autoIncludeNewProfiles = headphone.autoIncludeNewProfiles,
                    nowMillis = now,
                    snapshotCodec = snapshotCodec,
                )

                dao.upsertHeadphone(
                    headphone.copy(
                        vendorId = vendor?.id ?: headphone.vendorId,
                        vendorName = vendor?.name ?: headphone.vendorName,
                        productName = product?.name ?: headphone.productName,
                        updatedAtMillis = now,
                    ),
                )
                if (reconciled.profiles.isNotEmpty()) {
                    dao.upsertProfiles(reconciled.profiles)
                }
                summary += reconciled.changes
            }

            summary
        }

    suspend fun removeUnavailableProfile(productId: String, profileId: String) {
        database.withTransaction {
            val profile = dao.getProfiles(productId).firstOrNull { it.profileId == profileId }
                ?: return@withTransaction
            require(profile.noLongerAvailable) {
                "Only profiles no longer available in OPRA may be removed directly from retained state."
            }
            dao.deleteProfile(productId, profileId)
            if (dao.countSelectedProfiles(productId) == 0) {
                dao.deleteHeadphone(productId)
            }
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
    generatedPresetName = generatedPresetName,
    generatedXml = generatedXml,
    generatedFromFingerprint = generatedFromFingerprint,
    generatedAtMillis = generatedAtMillis,
)
