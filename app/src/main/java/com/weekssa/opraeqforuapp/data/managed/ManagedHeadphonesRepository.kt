package com.weekssa.opraeqforuapp.data.managed

import androidx.room.withTransaction
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipDao
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.assessUappCompatibility
import com.weekssa.opraeqforuapp.domain.catalog.isUsableParametricSource
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneSelection
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import com.weekssa.opraeqforuapp.domain.managed.selectionUpdatesForSave
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Owns the user's headphone/profile collection independently from an EQ destination.
 *
 * The outputId parameters remain temporarily source-compatible with older call sites and tests,
 * but they no longer select a different My EQs collection. The shared managed_* rows are the
 * durable headphone/profile ownership source. Legacy output_managed_* rows are retained only as
 * migration/history data so upgrades preserve the union of collections created by older releases.
 */
class ManagedHeadphonesRepository(
    private val database: OpraEqDatabase,
    private val snapshotCodec: ManagedProfileSnapshotCodec = ManagedProfileSnapshotCodec(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.managedHeadphonesDao()
    private val exportOwnershipDao: ExportOwnershipDao = database.exportOwnershipDao()

    @Suppress("UNUSED_PARAMETER")
    fun observeHeadphones(outputId: String = DEFAULT_OUTPUT_ID): Flow<List<ManagedHeadphoneRecord>> =
        combine(
            dao.observeHeadphones(),
            dao.observeAllProfiles(),
        ) { headphones, profiles ->
            val profilesByProduct = profiles.groupBy(ManagedProfileEntity::productId)
            headphones.map { headphone ->
                headphone.toDomain(
                    profiles = profilesByProduct[headphone.productId].orEmpty(),
                    snapshotCodec = snapshotCodec,
                )
            }
        }.flowOn(ioDispatcher)

    @Suppress("UNUSED_PARAMETER")
    fun observeHeadphone(
        productId: String,
        outputId: String = DEFAULT_OUTPUT_ID,
    ): Flow<ManagedHeadphoneRecord?> = combine(
        dao.observeHeadphone(productId),
        dao.observeProfiles(productId),
    ) { headphone, profiles ->
        headphone?.toDomain(profiles, snapshotCodec)
    }.flowOn(ioDispatcher)

    @Suppress("UNUSED_PARAMETER")
    suspend fun getHeadphone(
        productId: String,
        outputId: String = DEFAULT_OUTPUT_ID,
    ): ManagedHeadphoneRecord? = withContext(ioDispatcher) {
        val headphone = dao.getHeadphone(productId) ?: return@withContext null
        headphone.toDomain(dao.getProfiles(productId), snapshotCodec)
    }

    suspend fun getSelectionState(
        productId: String,
        outputId: String = DEFAULT_OUTPUT_ID,
    ): ManagedHeadphoneSelection? = getHeadphone(productId, outputId)?.toSelectionState()

    /**
     * Saves one global headphone/profile selection. Changing Default EQ target or connecting a DAC
     * cannot rewrite this selection; target compatibility is evaluated later when an action is
     * requested.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun saveSelection(
        catalog: OpraCatalog,
        productId: String,
        stagedSelectedProfileIds: Set<String>,
        autoIncludeNewProfiles: Boolean,
        outputId: String = DEFAULT_OUTPUT_ID,
    ) = withContext(ioDispatcher) {
        val canonicalProductId = catalog.canonicalProductId(productId)
        val product = requireNotNull(catalog.product(canonicalProductId)) {
            "Cannot manage unknown EQ Library product $productId."
        }
        val vendor = requireNotNull(catalog.vendor(product.vendorId)) {
            "Cannot manage EQ Library product $productId without its vendor."
        }
        val currentProfiles = catalog.profilesForProduct(canonicalProductId)
        val selectionUpdates = selectionUpdatesForSave(
            profiles = currentProfiles,
            stagedSelectedProfileIds = stagedSelectedProfileIds,
            autoIncludeNewProfiles = autoIncludeNewProfiles,
        )
        val now = nowMillis()

        database.withTransaction {
            if (canonicalProductId != productId) {
                migrateManagedHeadphoneAlias(catalog, productId, now)
            }
            val existingHeadphone = dao.getHeadphone(canonicalProductId)
            val existingProfiles = dao.getProfiles(canonicalProductId).associateBy(ManagedProfileEntity::profileId)

            dao.upsertHeadphone(
                ManagedHeadphoneEntity(
                    productId = canonicalProductId,
                    vendorId = vendor.id,
                    vendorName = vendor.name,
                    productName = product.name,
                    // Legacy column name retained for migration compatibility. It stores the
                    // headphone-level "Notify me about new EQs" preference.
                    autoIncludeNewProfiles = autoIncludeNewProfiles,
                    createdAtMillis = existingHeadphone?.createdAtMillis ?: now,
                    updatedAtMillis = now,
                ),
            )

            val sourceEntities = currentProfiles.map { profile ->
                val existing = existingProfiles[profile.id]
                val selection = requireNotNull(selectionUpdates[profile.id])
                val fingerprint = snapshotCodec.fingerprint(profile)
                val uappRepresentable =
                    profile.assessUappCompatibility().category != ProfileCompatibility.NotCompatible
                val generated = if (
                    selection.selected && (
                        existing?.generatedPresetName == null ||
                            existing.generatedFromFingerprint != fingerprint ||
                            (existing.generatedXml == null && uappRepresentable)
                        )
                ) {
                    generateManagedPreset(
                        productName = product.name,
                        profile = profile,
                        fingerprint = fingerprint,
                        nowMillis = now,
                    )
                } else {
                    null
                }
                val presetArtifacts = reconcileManagedPresetArtifacts(
                    existing = existing,
                    currentFingerprint = fingerprint,
                    sourceUsable = profile.isUsableParametricSource(),
                    uappRepresentable = uappRepresentable,
                    generated = generated,
                )
                ManagedProfileEntity(
                    profileId = profile.id,
                    productId = canonicalProductId,
                    selected = selection.selected,
                    explicitlyExcluded = selection.explicitlyExcluded,
                    snapshotJson = snapshotCodec.encode(profile),
                    fingerprint = fingerprint,
                    firstSeenAtMillis = existing?.firstSeenAtMillis ?: now,
                    lastSeenAtMillis = now,
                    isNewUnreviewed = existing?.isNewUnreviewed ?: false,
                    isUpdatedUnreviewed = existing?.isUpdatedUnreviewed ?: false,
                    noLongerAvailable = false,
                    generatedPresetName = presetArtifacts.presetName,
                    generatedXml = presetArtifacts.xml,
                    generatedFromFingerprint = presetArtifacts.fromFingerprint,
                    generatedAtMillis = presetArtifacts.generatedAtMillis,
                )
            }
            if (sourceEntities.isNotEmpty()) dao.upsertProfiles(sourceEntities)
        }
    }

    suspend fun reconcileCatalog(catalog: OpraCatalog): ManagedCatalogChangeSummary =
        withContext(ioDispatcher) {
            database.withTransaction {
                val now = nowMillis()
                migrateAllManagedHeadphoneAliases(catalog, now)
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
                    if (reconciled.profiles.isNotEmpty()) dao.upsertProfiles(reconciled.profiles)
                    reconciled.profileIdMigrations.forEach { (fromProfileId, toProfileId) ->
                        exportOwnershipDao.migrateProfile(
                            productId = headphone.productId,
                            fromProfileId = fromProfileId,
                            toProfileId = toProfileId,
                        )
                    }
                    reconciled.profileIdsToDelete.forEach { profileId ->
                        dao.deleteProfile(headphone.productId, profileId)
                    }
                    summary += reconciled.changes
                }

                summary
            }
        }

    private suspend fun migrateAllManagedHeadphoneAliases(catalog: OpraCatalog, now: Long) {
        dao.getHeadphones().forEach { headphone ->
            if (catalog.canonicalProductId(headphone.productId) != headphone.productId) {
                migrateManagedHeadphoneAlias(catalog, headphone.productId, now)
            }
        }
    }

    private suspend fun migrateManagedHeadphoneAlias(
        catalog: OpraCatalog,
        oldProductId: String,
        now: Long,
    ) {
        val canonicalProductId = catalog.canonicalProductId(oldProductId)
        if (canonicalProductId == oldProductId) return
        val oldHeadphone = dao.getHeadphone(oldProductId) ?: return
        val product = catalog.product(canonicalProductId) ?: return
        val vendor = catalog.vendor(product.vendorId) ?: return
        val canonicalHeadphone = dao.getHeadphone(canonicalProductId)
        val canonicalProfiles = dao.getProfiles(canonicalProductId).associateBy(ManagedProfileEntity::profileId)
        val oldProfiles = dao.getProfiles(oldProductId)

        dao.upsertHeadphone(
            ManagedHeadphoneEntity(
                productId = canonicalProductId,
                vendorId = vendor.id,
                vendorName = vendor.name,
                productName = product.name,
                autoIncludeNewProfiles = oldHeadphone.autoIncludeNewProfiles ||
                    (canonicalHeadphone?.autoIncludeNewProfiles ?: false),
                createdAtMillis = minOf(
                    oldHeadphone.createdAtMillis,
                    canonicalHeadphone?.createdAtMillis ?: oldHeadphone.createdAtMillis,
                ),
                updatedAtMillis = now,
            ),
        )
        if (oldProfiles.isNotEmpty()) {
            dao.upsertProfiles(
                oldProfiles.map { old ->
                    val canonical = canonicalProfiles[old.profileId]
                    old.copy(
                        productId = canonicalProductId,
                        selected = old.selected || (canonical?.selected ?: false),
                        explicitlyExcluded = if (old.selected || canonical?.selected == true) {
                            false
                        } else {
                            old.explicitlyExcluded || (canonical?.explicitlyExcluded ?: false)
                        },
                        firstSeenAtMillis = minOf(
                            old.firstSeenAtMillis,
                            canonical?.firstSeenAtMillis ?: old.firstSeenAtMillis,
                        ),
                        lastSeenAtMillis = maxOf(
                            old.lastSeenAtMillis,
                            canonical?.lastSeenAtMillis ?: old.lastSeenAtMillis,
                        ),
                        isNewUnreviewed = old.isNewUnreviewed || (canonical?.isNewUnreviewed ?: false),
                        isUpdatedUnreviewed = old.isUpdatedUnreviewed || (canonical?.isUpdatedUnreviewed ?: false),
                    )
                },
            )
        }
        dao.deleteHeadphone(oldProductId)
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun removeUnavailableProfile(
        productId: String,
        profileId: String,
        outputId: String = DEFAULT_OUTPUT_ID,
    ) = withContext(ioDispatcher) {
        database.withTransaction {
            val profile = dao.getProfiles(productId).firstOrNull { it.profileId == profileId }
                ?: return@withTransaction
            require(profile.noLongerAvailable) {
                "Only profiles no longer available in EQ Library may be removed directly from retained state."
            }
            dao.deleteProfile(productId, profileId)
            if (dao.getProfiles(productId).none(ManagedProfileEntity::selected)) {
                dao.deleteHeadphone(productId)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun removeHeadphone(
        productId: String,
        outputId: String = DEFAULT_OUTPUT_ID,
    ) = withContext(ioDispatcher) {
        dao.deleteHeadphone(productId)
    }

    suspend fun markReviewed(productId: String) = withContext(ioDispatcher) {
        dao.markReviewed(productId)
    }

    companion object {
        /** Legacy compatibility constant; My EQs ownership is no longer output-scoped. */
        const val DEFAULT_OUTPUT_ID = "UAPP"
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

private fun ManagedProfileEntity.toDomain(
    snapshotCodec: ManagedProfileSnapshotCodec,
) = ManagedProfileRecord(
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
