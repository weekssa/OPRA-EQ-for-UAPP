package com.weekssa.opraeqforuapp.data.managed

import androidx.room.withTransaction
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.catalog.isUsableParametricSource
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneSelection
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import com.weekssa.opraeqforuapp.domain.managed.StoredProfileSelection
import com.weekssa.opraeqforuapp.domain.managed.selectionUpdatesForSave
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ManagedHeadphonesRepository(
    private val database: OpraEqDatabase,
    private val snapshotCodec: ManagedProfileSnapshotCodec = ManagedProfileSnapshotCodec(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.managedHeadphonesDao()

    /**
     * Returns only the headphones saved for one output context. Canonical/local source snapshots
     * remain shared, while selected flags are projected from the output-scoped tables. The legacy
     * autoIncludeNewProfiles field is the headphone-level new-EQ review preference.
     */
    fun observeHeadphones(outputId: String = DEFAULT_OUTPUT_ID): Flow<List<ManagedHeadphoneRecord>> =
        combine(
            dao.observeHeadphones(),
            dao.observeOutputHeadphones(outputId),
            dao.observeOutputProfiles(outputId),
        ) { headphones, outputHeadphones, outputProfiles ->
            val outputByProduct = outputHeadphones.associateBy(OutputManagedHeadphoneEntity::productId)
            val outputProfilesByProduct = outputProfiles.groupBy(OutputManagedProfileEntity::productId)
            headphones.mapNotNull { headphone ->
                val output = outputByProduct[headphone.productId] ?: return@mapNotNull null
                val selectionById = outputProfilesByProduct[headphone.productId]
                    .orEmpty()
                    .associateBy(OutputManagedProfileEntity::profileId)
                headphone.toDomain(
                    profiles = dao.getProfiles(headphone.productId),
                    snapshotCodec = snapshotCodec,
                    output = output,
                    outputSelections = selectionById,
                )
            }
        }

    fun observeHeadphone(
        productId: String,
        outputId: String = DEFAULT_OUTPUT_ID,
    ): Flow<ManagedHeadphoneRecord?> = combine(
        dao.observeHeadphone(productId),
        dao.observeProfiles(productId),
        dao.observeOutputHeadphones(outputId),
        dao.observeOutputProfiles(outputId),
    ) { headphone, profiles, outputHeadphones, outputProfiles ->
        val output = outputHeadphones.firstOrNull { it.productId == productId }
        if (headphone == null || output == null) {
            null
        } else {
            headphone.toDomain(
                profiles = profiles,
                snapshotCodec = snapshotCodec,
                output = output,
                outputSelections = outputProfiles
                    .filter { it.productId == productId }
                    .associateBy(OutputManagedProfileEntity::profileId),
            )
        }
    }

    suspend fun getHeadphone(
        productId: String,
        outputId: String = DEFAULT_OUTPUT_ID,
    ): ManagedHeadphoneRecord? {
        val headphone = dao.getHeadphone(productId) ?: return null
        val output = dao.getOutputHeadphone(outputId, productId) ?: return null
        val selections = dao.getOutputProfiles(outputId, productId)
            .associateBy(OutputManagedProfileEntity::profileId)
        return headphone.toDomain(
            profiles = dao.getProfiles(productId),
            snapshotCodec = snapshotCodec,
            output = output,
            outputSelections = selections,
        )
    }

    suspend fun getSelectionState(
        productId: String,
        outputId: String = DEFAULT_OUTPUT_ID,
    ): ManagedHeadphoneSelection? = getHeadphone(productId, outputId)?.toSelectionState()

    suspend fun saveSelection(
        catalog: OpraCatalog,
        productId: String,
        stagedSelectedProfileIds: Set<String>,
        autoIncludeNewProfiles: Boolean,
        outputId: String = DEFAULT_OUTPUT_ID,
    ) {
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
            val existingOutput = dao.getOutputHeadphone(outputId, canonicalProductId)
            val existingProfiles = dao.getProfiles(canonicalProductId).associateBy(ManagedProfileEntity::profileId)

            dao.upsertHeadphone(
                ManagedHeadphoneEntity(
                    productId = canonicalProductId,
                    vendorId = vendor.id,
                    vendorName = vendor.name,
                    productName = product.name,
                    // Legacy column name retained for migration compatibility. It now stores the
                    // headphone-level "Notify me about new EQs" preference.
                    autoIncludeNewProfiles = autoIncludeNewProfiles,
                    createdAtMillis = existingHeadphone?.createdAtMillis ?: now,
                    updatedAtMillis = now,
                ),
            )
            dao.upsertOutputHeadphone(
                OutputManagedHeadphoneEntity(
                    outputId = outputId,
                    productId = canonicalProductId,
                    autoIncludeNewProfiles = autoIncludeNewProfiles,
                    createdAtMillis = existingOutput?.createdAtMillis ?: now,
                    updatedAtMillis = now,
                ),
            )
            dao.upsertOutputProfiles(
                currentProfiles.map { profile ->
                    val selection = requireNotNull(selectionUpdates[profile.id])
                    OutputManagedProfileEntity(
                        outputId = outputId,
                        productId = canonicalProductId,
                        profileId = profile.id,
                        selected = selection.selected,
                        explicitlyExcluded = selection.explicitlyExcluded,
                    )
                },
            )

            val sourceEntities = currentProfiles.map { profile ->
                val existing = existingProfiles[profile.id]
                val fingerprint = snapshotCodec.fingerprint(profile)
                val generated = generateManagedPreset(
                    productName = product.name,
                    profile = profile,
                    fingerprint = fingerprint,
                    nowMillis = now,
                )
                ManagedProfileEntity(
                    profileId = profile.id,
                    productId = canonicalProductId,
                    selected = false,
                    explicitlyExcluded = false,
                    snapshotJson = snapshotCodec.encode(profile),
                    fingerprint = fingerprint,
                    firstSeenAtMillis = existing?.firstSeenAtMillis ?: now,
                    lastSeenAtMillis = now,
                    isNewUnreviewed = existing?.isNewUnreviewed ?: false,
                    isUpdatedUnreviewed = existing?.isUpdatedUnreviewed ?: false,
                    noLongerAvailable = false,
                    generatedPresetName = generated.presetName,
                    generatedXml = generated.xml,
                    generatedFromFingerprint = generated.fingerprint,
                    generatedAtMillis = generated.generatedAtMillis,
                )
            }
            dao.upsertProfiles(sourceEntities)
            syncSharedSelectionUnion(canonicalProductId)
        }
    }

    suspend fun reconcileCatalog(catalog: OpraCatalog): ManagedCatalogChangeSummary =
        database.withTransaction {
            val now = nowMillis()
            migrateAllManagedHeadphoneAliases(catalog, now)
            var summary = ManagedCatalogChangeSummary()

            dao.getHeadphones().forEach { headphone ->
                val product = catalog.product(headphone.productId)
                val vendor = product?.let { catalog.vendor(it.vendorId) }
                val currentProfiles = if (product != null) catalog.profilesForProduct(product.id) else emptyList()
                val reconciled = reconcileManagedProfiles(
                    productId = headphone.productId,
                    productName = product?.name ?: headphone.productName,
                    currentProfiles = currentProfiles,
                    existingProfiles = dao.getProfiles(headphone.productId),
                    // Legacy parameter name: controls whether new/changed profiles become review items.
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
                reconciled.profileIdsToDelete.forEach { profileId -> dao.deleteProfile(headphone.productId, profileId) }
                summary += reconciled.changes

                dao.getAllOutputHeadphones()
                    .filter { it.productId == headphone.productId }
                    .forEach { output ->
                        reconcileOutputSelection(
                            output = output,
                            currentProfiles = currentProfiles,
                        )
                    }
                syncSharedSelectionUnion(headphone.productId)
            }

            summary
        }

    private suspend fun reconcileOutputSelection(
        output: OutputManagedHeadphoneEntity,
        currentProfiles: List<com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile>,
    ) {
        val existingById = dao.getOutputProfiles(output.outputId, output.productId)
            .associateBy(OutputManagedProfileEntity::profileId)
        val updates = currentProfiles.map { profile ->
            val existing = existingById[profile.id]
            val selected = profile.isUsableParametricSource() && existing?.selected == true
            OutputManagedProfileEntity(
                outputId = output.outputId,
                productId = output.productId,
                profileId = profile.id,
                selected = selected,
                explicitlyExcluded = false,
            )
        }
        if (updates.isNotEmpty()) dao.upsertOutputProfiles(updates)
    }

    private suspend fun syncSharedSelectionUnion(productId: String) {
        val outputSelections = dao.getOutputProfilesForProduct(productId)
            .groupBy(OutputManagedProfileEntity::profileId)
        val sourceProfiles = dao.getProfiles(productId)
        if (sourceProfiles.isEmpty()) return
        dao.upsertProfiles(
            sourceProfiles.map { source ->
                val selections = outputSelections[source.profileId].orEmpty()
                source.copy(
                    selected = selections.any(OutputManagedProfileEntity::selected),
                    explicitlyExcluded = selections.any(OutputManagedProfileEntity::explicitlyExcluded),
                )
            },
        )
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
        if (oldProfiles.isNotEmpty()) dao.upsertProfiles(oldProfiles.map { it.copy(productId = canonicalProductId) })

        dao.getAllOutputHeadphones()
            .filter { it.productId == oldProductId }
            .forEach { output ->
                val canonicalOutput = dao.getOutputHeadphone(output.outputId, canonicalProductId)
                dao.upsertOutputHeadphone(
                    output.copy(
                        productId = canonicalProductId,
                        autoIncludeNewProfiles = output.autoIncludeNewProfiles ||
                            (canonicalOutput?.autoIncludeNewProfiles ?: false),
                        createdAtMillis = minOf(
                            output.createdAtMillis,
                            canonicalOutput?.createdAtMillis ?: output.createdAtMillis,
                        ),
                        updatedAtMillis = now,
                    ),
                )
                val outputProfiles = dao.getOutputProfiles(output.outputId, oldProductId)
                if (outputProfiles.isNotEmpty()) {
                    dao.upsertOutputProfiles(outputProfiles.map { it.copy(productId = canonicalProductId) })
                }
                dao.deleteOutputHeadphone(output.outputId, oldProductId)
            }
        dao.deleteHeadphone(oldProductId)
    }

    suspend fun removeUnavailableProfile(
        productId: String,
        profileId: String,
        outputId: String = DEFAULT_OUTPUT_ID,
    ) {
        database.withTransaction {
            val profile = dao.getProfiles(productId).firstOrNull { it.profileId == profileId }
                ?: return@withTransaction
            require(profile.noLongerAvailable) {
                "Only profiles no longer available in EQ Library may be removed directly from retained state."
            }
            dao.deleteOutputProfile(outputId, productId, profileId)
            syncSharedSelectionUnion(productId)
            if (dao.getOutputProfilesForProfile(profileId).none(OutputManagedProfileEntity::selected)) {
                dao.deleteProfile(productId, profileId)
            }
            if (dao.getOutputProfiles(outputId, productId).none(OutputManagedProfileEntity::selected)) {
                dao.deleteOutputHeadphone(outputId, productId)
            }
            if (dao.countOutputsForProduct(productId) == 0) dao.deleteHeadphone(productId)
        }
    }

    suspend fun removeHeadphone(
        productId: String,
        outputId: String = DEFAULT_OUTPUT_ID,
    ) {
        database.withTransaction {
            dao.deleteOutputProfiles(outputId, productId)
            dao.deleteOutputHeadphone(outputId, productId)
            if (dao.countOutputsForProduct(productId) == 0) {
                dao.deleteHeadphone(productId)
            } else {
                syncSharedSelectionUnion(productId)
            }
        }
    }

    suspend fun markReviewed(productId: String) {
        dao.markReviewed(productId)
    }

    companion object {
        const val DEFAULT_OUTPUT_ID = "UAPP"
    }
}

private fun ManagedHeadphoneEntity.toDomain(
    profiles: List<ManagedProfileEntity>,
    snapshotCodec: ManagedProfileSnapshotCodec,
    output: OutputManagedHeadphoneEntity,
    outputSelections: Map<String, OutputManagedProfileEntity>,
) = ManagedHeadphoneRecord(
    productId = productId,
    vendorId = vendorId,
    vendorName = vendorName,
    productName = productName,
    autoIncludeNewProfiles = autoIncludeNewProfiles,
    createdAtMillis = output.createdAtMillis,
    updatedAtMillis = output.updatedAtMillis,
    profiles = profiles.map { profile ->
        val selection = outputSelections[profile.profileId]
        profile.toDomain(
            snapshotCodec = snapshotCodec,
            selection = StoredProfileSelection(
                selected = selection?.selected ?: false,
                explicitlyExcluded = selection?.explicitlyExcluded ?: false,
            ),
        )
    },
)

private fun ManagedProfileEntity.toDomain(
    snapshotCodec: ManagedProfileSnapshotCodec,
    selection: StoredProfileSelection,
) = ManagedProfileRecord(
    profileId = profileId,
    selected = selection.selected,
    explicitlyExcluded = selection.explicitlyExcluded,
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
