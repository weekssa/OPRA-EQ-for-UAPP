package com.weekssa.opraeqforuapp.data.library

import androidx.room.withTransaction
import com.weekssa.opraeqforuapp.data.export.ExportDocumentStore
import com.weekssa.opraeqforuapp.data.export.ExportLookup
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipEntity
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileEntity
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessUappCompatibility
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.library.EqSourceKind
import com.weekssa.opraeqforuapp.domain.library.EqSourceReference
import com.weekssa.opraeqforuapp.domain.library.EqTarget
import com.weekssa.opraeqforuapp.domain.library.EqTargetKind
import com.weekssa.opraeqforuapp.domain.library.HeadphoneIdentity
import com.weekssa.opraeqforuapp.domain.library.LocalSavedEqAdapter
import com.weekssa.opraeqforuapp.domain.library.ProvenanceTier
import com.weekssa.opraeqforuapp.domain.library.RedistributionPolicy
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.VerificationStatus
import com.weekssa.opraeqforuapp.domain.library.UnclaimedEqFormat
import com.weekssa.opraeqforuapp.domain.library.UnclaimedEqParseState
import com.weekssa.opraeqforuapp.domain.library.UnclaimedEqParsedContent
import com.weekssa.opraeqforuapp.domain.library.UnclaimedEqParser
import com.weekssa.opraeqforuapp.domain.library.UnclaimedEqRecord
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

/**
 * Reconciles app-owned SAF artifacts against the destination-agnostic My EQs collection.
 *
 * This deliberately does not crawl arbitrary files in a user-selected tree. Only exact document
 * URIs already recorded in export_ownership are eligible, preserving the app's existing ownership
 * and delete-safety boundary.
 */
class UnclaimedEqRepository(
    private val database: OpraEqDatabase,
    private val documentStore: ExportDocumentStore,
    private val snapshotCodec: ManagedProfileSnapshotCodec = ManagedProfileSnapshotCodec(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val canonicalSnapshotCodec: SavedEqCanonicalSnapshotCodec = SavedEqCanonicalSnapshotCodec(),
    private val canonicalSelectionCodec: CanonicalEqSelectionCodec = CanonicalEqSelectionCodec(),
) {
    private val ownershipDao = database.exportOwnershipDao()
    private val managedDao = database.managedHeadphonesDao()
    private val savedEqDao = database.savedEqDao()
    private val savedGeneralEqDao = database.savedGeneralEqDao()

    fun observeUnclaimed(): Flow<List<UnclaimedEqRecord>> = combine(
        ownershipDao.observeAll(),
        managedDao.observeAllProfiles(),
        savedEqDao.observeAll(),
        savedGeneralEqDao.observeAll(),
    ) { ownerships, managedProfiles, savedEqs, generalEqs ->
        RecoveryIndex(
            ownerships = ownerships,
            claimedProfileIds = buildSet {
                managedProfiles.filter(ManagedProfileEntity::selected).forEach { add(it.profileId) }
                savedEqs.forEach { saved ->
                    runCatching { snapshotCodec.decode(saved.profileJson).id }.getOrNull()?.let(::add)
                }
                generalEqs.forEach { add(it.presetId) }
            },
            unrepresentableUappProfiles = unrepresentableManagedUappProfiles(managedProfiles, snapshotCodec),
        )
    }.mapLatest { index ->
        withContext(ioDispatcher) {
            unresolvedOwnedArtifacts(
                ownerships = index.ownerships,
                claimedProfileIds = index.claimedProfileIds,
                unrepresentableUappProfiles = index.unrepresentableUappProfiles,
            ).mapNotNull { ownership ->
                val staleUappExport = ownership.isUappExport() &&
                    ManagedUappExportIdentity(ownership.productId, ownership.profileId) in index.unrepresentableUappProfiles
                inspectOwnership(ownership, sourceNoLongerRepresentable = staleUappExport)
            }
                .sortedWith(
                    compareByDescending<UnclaimedEqRecord> { it.exportedAtMillis }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.originalFileName },
                )
        }
    }.flowOn(ioDispatcher)

    suspend fun recoverToPersonal(
        documentUri: String,
        manufacturer: String,
        model: String,
        displayName: String,
    ): SavedEqRecord = withContext(ioDispatcher) {
        val maker = manufacturer.trim()
        val headphoneModel = model.trim()
        val name = displayName.trim()
        require(maker.isNotEmpty()) { "Manufacturer is required." }
        require(headphoneModel.isNotEmpty()) { "Model is required." }
        require(name.isNotEmpty()) { "EQ name is required." }

        val ownership = requireNotNull(ownershipDao.getByDocumentUri(documentUri)) {
            "This managed file is no longer available for recovery."
        }
        previouslyRecoveredPersonalEntity(ownership)?.let { existing ->
            return@withContext toSavedEqRecord(existing)
        }
        val currentManagedProfile = managedDao.getProfiles(ownership.productId)
            .firstOrNull { it.profileId == ownership.profileId }
        require(!shouldBlockUappExportRecovery(ownership, currentManagedProfile, snapshotCodec)) {
            "This older UAPP EQ may not match the current complete source filters, so it cannot be " +
                "recovered as a complete EQ. You may keep or delete the file."
        }
        val document = when (val lookup = documentStore.openDocument(documentUri)) {
            is ExportLookup.Found -> lookup.value
            ExportLookup.Missing -> {
                ownershipDao.delete(documentUri)
                error("The managed file is no longer present.")
            }
            ExportLookup.Unavailable -> error("The managed file cannot be accessed with the current folder permission.")
        }
        val bytes = documentStore.readBytes(document, MAX_RECOVERY_BYTES)
            ?: error("The managed file could not be read safely.")
        val parsed = UnclaimedEqParser.parse(ownership.fileName, bytes)
        val content = parsed.content
        require(parsed.state == UnclaimedEqParseState.RECOVERABLE && content != null) {
            parsed.message ?: "This managed file cannot be recovered safely."
        }

        val id = UUID.randomUUID().toString()
        val profile = buildRecoveredPersonalProfile(
            captureId = id,
            originalFileName = ownership.fileName,
            content = content,
        )
        val productId = profile.productId
        val profileId = profile.id
        val now = nowMillis()
        val observedAtEpochSeconds = now / 1_000L
        val canonicalSnapshot = requireNotNull(
            LocalSavedEqAdapter.adapt(
                profile = profile,
                displayName = name,
                headphone = HeadphoneIdentity(manufacturer = maker, model = headphoneModel),
                target = EqTarget(name = null, kind = EqTargetKind.UNKNOWN),
                sourceReference = EqSourceReference(
                    sourceId = "personal_import",
                    sourceKind = EqSourceKind.PERSONAL_IMPORT,
                    sourceRecordId = ownership.fileName,
                    url = null,
                    creator = null,
                    provenanceTier = ProvenanceTier.NEEDS_REVIEW,
                    redistributionPolicy = RedistributionPolicy.UNKNOWN_REVIEW,
                    discoveredAtEpochSeconds = observedAtEpochSeconds,
                    isPrimary = true,
                ),
                verificationStatus = VerificationStatus.UNVERIFIED,
                observedAtEpochSeconds = observedAtEpochSeconds,
            ),
        ) { "Recovered EQ could not be represented without dropping filter data." }
        val legacyProjection = LocalSavedEqAdapter.projectToLegacy(canonicalSnapshot, productId)
        val entity = SavedEqEntity(
            entryId = "personal:$id",
            kind = SavedEqRepository.KIND_PERSONAL,
            sourceProfileId = null,
            productId = productId,
            manufacturer = maker,
            model = headphoneModel,
            displayName = name,
            profileJson = snapshotCodec.encode(legacyProjection),
            createdAtMillis = now,
            updatedAtMillis = now,
            captureMetadataJson = null,
            canonicalSnapshotJson = canonicalSnapshotCodec.encode(canonicalSnapshot),
        )
        val savedEntity = database.withTransaction {
            val ownershipAtCommit = requireNotNull(ownershipDao.getByDocumentUri(documentUri)) {
                "This managed file is no longer available for recovery."
            }
            previouslyRecoveredPersonalEntity(ownershipAtCommit)?.let { existing ->
                return@withTransaction existing
            }
            require(ownershipAtCommit == ownership) {
                "This managed file's ownership changed while it was being recovered. Refresh My EQs and review it again."
            }
            // Recheck in the write transaction so catalog reconciliation cannot make the export
            // stale between the initial read and creation of a recovered Personal EQ.
            val currentManagedProfileAtCommit = managedDao.getProfiles(ownershipAtCommit.productId)
                .firstOrNull { it.profileId == ownershipAtCommit.profileId }
            require(!shouldBlockUappExportRecovery(ownershipAtCommit, currentManagedProfileAtCommit, snapshotCodec)) {
                "This older UAPP EQ may no longer match the current complete source filters, so it cannot be " +
                    "recovered as a complete EQ. You may keep or delete the file."
            }
            savedEqDao.upsert(entity)
            // Retain the exact owned file and make its recovered Personal EQ association explicit.
            ownershipDao.upsert(
                ownershipAtCommit.copy(
                    profileId = profileId,
                    productId = productId,
                ),
            )
            entity
        }
        toSavedEqRecord(savedEntity)
    }

    /**
     * Deletes only an exact document URI already owned by EQ Library. Provider/access uncertainty
     * fails closed and leaves both the file and unresolved ownership row intact.
     */
    suspend fun deleteUnclaimed(documentUri: String): Boolean = withContext(ioDispatcher) {
        val ownership = ownershipDao.getByDocumentUri(documentUri) ?: return@withContext true
        when (val lookup = documentStore.openDocument(ownership.documentUri)) {
            is ExportLookup.Found -> {
                if (!documentStore.delete(lookup.value)) return@withContext false
                ownershipDao.delete(documentUri)
                true
            }
            ExportLookup.Missing -> {
                ownershipDao.delete(documentUri)
                true
            }
            ExportLookup.Unavailable -> false
        }
    }

    private suspend fun inspectOwnership(
        ownership: ExportOwnershipEntity,
        sourceNoLongerRepresentable: Boolean,
    ): UnclaimedEqRecord? {
        val reason = if (sourceNoLongerRepresentable) {
            "This previously exported UAPP EQ may not represent the complete current source filters. " +
                "It is kept for review; don't use or recover it as a complete EQ."
        } else {
            "This app-managed EQ file no longer matches an item currently claimed by My EQs."
        }
        return when (val lookup = documentStore.openDocument(ownership.documentUri)) {
            ExportLookup.Missing -> {
                // Existing storage policy permits forgetting ownership only after confirmed absence.
                ownershipDao.delete(ownership.documentUri)
                null
            }
            ExportLookup.Unavailable -> UnclaimedEqRecord(
                documentUri = ownership.documentUri,
                originalFileName = ownership.fileName,
                relativeDirectory = ownership.relativeDirectory,
                previousProfileId = ownership.profileId,
                previousProductId = ownership.productId,
                format = UnclaimedEqFormat.UNKNOWN,
                parseState = UnclaimedEqParseState.ACCESS_UNAVAILABLE,
                reason = reason,
                parseMessage = "The file is still tracked, but its document provider is unavailable or permission was lost.",
                parsedContent = null,
                exportedAtMillis = ownership.exportedAtMillis,
            )
            is ExportLookup.Found -> {
                if (sourceNoLongerRepresentable) {
                    return UnclaimedEqRecord(
                        documentUri = ownership.documentUri,
                        originalFileName = ownership.fileName,
                        relativeDirectory = ownership.relativeDirectory,
                        previousProfileId = ownership.profileId,
                        previousProductId = ownership.productId,
                        format = UnclaimedEqFormat.TONEBOOSTERS_XML,
                        parseState = UnclaimedEqParseState.SOURCE_UNVERIFIED,
                        reason = reason,
                        parseMessage = reason,
                        parsedContent = null,
                        exportedAtMillis = ownership.exportedAtMillis,
                    )
                }
                val bytes = documentStore.readBytes(lookup.value, MAX_RECOVERY_BYTES)
                val parsed = if (bytes == null) null else UnclaimedEqParser.parse(ownership.fileName, bytes)
                UnclaimedEqRecord(
                    documentUri = ownership.documentUri,
                    originalFileName = ownership.fileName,
                    relativeDirectory = ownership.relativeDirectory,
                    previousProfileId = ownership.profileId,
                    previousProductId = ownership.productId,
                    format = parsed?.format ?: UnclaimedEqFormat.UNKNOWN,
                    parseState = parsed?.state ?: UnclaimedEqParseState.INVALID,
                    reason = reason,
                    parseMessage = parsed?.message ?: "The file could not be read within the safe recovery limit.",
                    parsedContent = parsed?.content,
                    exportedAtMillis = ownership.exportedAtMillis,
                )
            }
        }
    }

    /**
     * A completed recovery is linked to its exact owned URI by the ownership row's generated
     * Personal profile identity. Return that row rather than creating a duplicate when a tap is
     * repeated or two recovery requests race before either reaches the write transaction.
     */
    private suspend fun previouslyRecoveredPersonalEntity(
        ownership: ExportOwnershipEntity,
    ): SavedEqEntity? {
        val personalPrefix = "personal-eq:"
        if (!ownership.profileId.startsWith(personalPrefix)) return null
        val recoveryId = ownership.profileId.removePrefix(personalPrefix)
        val canonicalUuid = runCatching { UUID.fromString(recoveryId).toString() == recoveryId }.getOrDefault(false)
        require(canonicalUuid) {
            "This managed file has a malformed Personal EQ association and cannot be recovered safely."
        }
        val entryId = "personal:$recoveryId"
        // A canonical dangling UUID may be left by an intentional Personal EQ deletion. This
        // helper runs only inside an explicit owner-initiated recovery action, so the owned file
        // may be re-imported as a new Personal EQ instead of being silently restored.
        val entity = savedEqDao.get(entryId) ?: return null
        require(entity.kind == SavedEqRepository.KIND_PERSONAL && entity.productId == ownership.productId) {
            "This managed file is already associated with a Personal EQ that cannot be verified safely."
        }
        val profile = runCatching { snapshotCodec.decode(entity.profileJson) }.getOrNull()
        require(profile != null && profile.id == ownership.profileId && profile.productId == ownership.productId) {
            "This managed file is already associated with a Personal EQ that cannot be verified safely."
        }
        return entity
    }

    private fun toSavedEqRecord(entity: SavedEqEntity): SavedEqRecord = SavedEqRecordMapper.toDomain(
        entity = entity,
        legacyCodec = snapshotCodec,
        canonicalCodec = canonicalSnapshotCodec,
        selectionCodec = canonicalSelectionCodec,
        captureMetadataCodec = SavedEqCaptureMetadataCodec(),
    )

    private data class RecoveryIndex(
        val ownerships: List<ExportOwnershipEntity>,
        val claimedProfileIds: Set<String>,
        val unrepresentableUappProfiles: Set<ManagedUappExportIdentity>,
    )

    companion object {
        private const val MAX_RECOVERY_BYTES = 1024 * 1024
    }
}

/**
 * Destination/device state is deliberately absent from this policy. A file is unresolved only when
 * the profile identity recorded for that exact app-owned URI is no longer claimed by the global
 * My EQs collection. distinctBy is defensive; export_ownership also has documentUri as its PK.
 */
internal fun unresolvedOwnedArtifacts(
    ownerships: List<ExportOwnershipEntity>,
    claimedProfileIds: Set<String>,
    unrepresentableUappProfiles: Set<ManagedUappExportIdentity> = emptySet(),
): List<ExportOwnershipEntity> = ownerships
    .distinctBy(ExportOwnershipEntity::documentUri)
    .filter { ownership ->
        ownership.profileId !in claimedProfileIds || (
            ownership.isUappExport() &&
                ManagedUappExportIdentity(ownership.productId, ownership.profileId) in unrepresentableUappProfiles
            )
    }

internal data class ManagedUappExportIdentity(
    val productId: String,
    val profileId: String,
)

internal fun unrepresentableManagedUappProfiles(
    profiles: List<ManagedProfileEntity>,
    snapshotCodec: ManagedProfileSnapshotCodec,
): Set<ManagedUappExportIdentity> = profiles.asSequence()
    .filterNot(ManagedProfileEntity::noLongerAvailable)
    // A current identity whose snapshot cannot be decoded or identity-checked is not safe to
    // treat as UAPP-representable. Keep its exact owned export visible for review.
    .filterNot { it.isUappSourceRepresentable(snapshotCodec) }
    .map { ManagedUappExportIdentity(it.productId, it.profileId) }
    .toSet()

internal fun shouldBlockUappExportRecovery(
    ownership: ExportOwnershipEntity,
    currentManagedProfile: ManagedProfileEntity?,
    snapshotCodec: ManagedProfileSnapshotCodec,
): Boolean = ownership.isUappExport() &&
    currentManagedProfile != null &&
    !currentManagedProfile.noLongerAvailable &&
    !currentManagedProfile.isUappSourceRepresentable(snapshotCodec)

private fun ManagedProfileEntity.isUappSourceRepresentable(
    snapshotCodec: ManagedProfileSnapshotCodec,
): Boolean {
    val profile = runCatching { snapshotCodec.decode(snapshotJson) }.getOrNull() ?: return false
    return profile.id == profileId &&
        profile.productId == productId &&
        profile.assessUappCompatibility().category != ProfileCompatibility.NotCompatible
}

private fun ExportOwnershipEntity.isUappExport(): Boolean =
    relativeDirectory == ExportDevice.UAPP.folderName ||
        relativeDirectory.startsWith("${ExportDevice.UAPP.folderName}/")

/** Builds an ordinary Personal EQ while preserving every parsed filter/preamp value verbatim. */
internal fun buildRecoveredPersonalProfile(
    captureId: String,
    originalFileName: String,
    content: UnclaimedEqParsedContent,
): OpraEqProfile = OpraEqProfile(
    id = "personal-eq:$captureId",
    productId = "personal-product:$captureId",
    author = "Personal",
    details = "Recovered legacy EQ · Original file: $originalFileName",
    link = null,
    profileType = "parametric_eq",
    preampGainDb = content.preampGainDb,
    bands = content.bands,
)
