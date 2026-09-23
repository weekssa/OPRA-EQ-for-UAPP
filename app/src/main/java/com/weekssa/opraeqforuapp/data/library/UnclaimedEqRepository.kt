package com.weekssa.opraeqforuapp.data.library

import androidx.room.withTransaction
import com.weekssa.opraeqforuapp.data.export.ExportDocumentStore
import com.weekssa.opraeqforuapp.data.export.ExportLookup
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipEntity
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileEntity
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
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
        )
    }.mapLatest { index ->
        withContext(ioDispatcher) {
            unresolvedOwnedArtifacts(index.ownerships, index.claimedProfileIds)
                .mapNotNull { inspectOwnership(it) }
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
        database.withTransaction {
            savedEqDao.upsert(entity)
            // Retain the exact owned file and make its recovered Personal EQ association explicit.
            ownershipDao.upsert(
                ownership.copy(
                    profileId = profileId,
                    productId = productId,
                ),
            )
        }
        SavedEqRecordMapper.toDomain(
            entity = entity,
            legacyCodec = snapshotCodec,
            canonicalCodec = canonicalSnapshotCodec,
            captureMetadataCodec = SavedEqCaptureMetadataCodec(),
        )
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

    private suspend fun inspectOwnership(ownership: ExportOwnershipEntity): UnclaimedEqRecord? {
        val reason = "This app-managed EQ file no longer matches an item currently claimed by My EQs."
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

    private data class RecoveryIndex(
        val ownerships: List<ExportOwnershipEntity>,
        val claimedProfileIds: Set<String>,
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
): List<ExportOwnershipEntity> = ownerships
    .distinctBy(ExportOwnershipEntity::documentUri)
    .filter { it.profileId !in claimedProfileIds }

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
