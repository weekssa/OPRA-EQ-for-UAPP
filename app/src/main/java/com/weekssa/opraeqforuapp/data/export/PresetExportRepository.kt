package com.weekssa.opraeqforuapp.data.export

import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.PresetExportCandidate
import com.weekssa.opraeqforuapp.domain.export.buildEqLibraryExportPlan
import com.weekssa.opraeqforuapp.domain.export.disambiguatedExportFileName
import com.weekssa.opraeqforuapp.domain.export.presetBytes
import com.weekssa.opraeqforuapp.domain.export.stableExportId
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface PresetExportItemResult {
    val candidate: PresetExportCandidate

    data class Created(override val candidate: PresetExportCandidate) : PresetExportItemResult
    data class Updated(override val candidate: PresetExportCandidate) : PresetExportItemResult
    data class Current(override val candidate: PresetExportCandidate) : PresetExportItemResult
    data class Conflict(
        override val candidate: PresetExportCandidate,
        val reason: String,
    ) : PresetExportItemResult
    data class Failed(
        override val candidate: PresetExportCandidate,
        val reason: String,
    ) : PresetExportItemResult
}

data class PresetExportSummary(
    val results: List<PresetExportItemResult>,
    val accessLost: Boolean = false,
) {
    val createdCount = results.count { it is PresetExportItemResult.Created }
    val updatedCount = results.count { it is PresetExportItemResult.Updated }
    val currentCount = results.count { it is PresetExportItemResult.Current }
    val conflictCount = results.count { it is PresetExportItemResult.Conflict }
    val failedCount = results.count { it is PresetExportItemResult.Failed }
    val successfulCount = createdCount + updatedCount + currentCount
    val devicesWritten: Set<String> = results
        .filter {
            it is PresetExportItemResult.Created ||
                it is PresetExportItemResult.Updated ||
                it is PresetExportItemResult.Current
        }
        .mapTo(linkedSetOf()) { it.candidate.deviceName }
}

data class ExportItemKey(
    val productId: String,
    val profileId: String,
)

data class ExportCurrentness(
    val exportableItems: Set<ExportItemKey> = emptySet(),
    val needsExportItems: Set<ExportItemKey> = emptySet(),
) {
    val hasAnythingToExport: Boolean get() = exportableItems.isNotEmpty()
    val hasPendingExport: Boolean get() = needsExportItems.isNotEmpty()

    fun isExportable(productId: String, profileId: String): Boolean =
        ExportItemKey(productId, profileId) in exportableItems

    fun needsExport(productId: String, profileId: String): Boolean =
        ExportItemKey(productId, profileId) in needsExportItems
}

class PresetExportRepository(
    database: OpraEqDatabase,
    private val documentStore: ExportDocumentStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val ownershipDao = database.exportOwnershipDao()

    /**
     * Evaluates the active output against the exact app-owned SAF document URI and generated
     * content, not against a provider-specific display-name assumption.
     */
    suspend fun evaluateCurrentness(
        treeUri: String?,
        headphones: List<ManagedHeadphoneRecord>,
        device: ExportDevice,
    ): ExportCurrentness = withContext(ioDispatcher) {
        val plan = buildEqLibraryExportPlan(headphones, device)
        val allCandidates = (plan.candidates + plan.duplicateConflicts).distinctBy {
            Triple(it.productId, it.profileId, it.generatedFingerprint)
        }
        val exportable = allCandidates.mapTo(linkedSetOf()) { candidate ->
            ExportItemKey(candidate.productId, candidate.profileId)
        }
        if (treeUri == null) {
            return@withContext ExportCurrentness(
                exportableItems = exportable,
                needsExportItems = exportable,
            )
        }

        val needs = linkedSetOf<ExportItemKey>()
        for (candidate in allCandidates) {
            val key = ExportItemKey(candidate.productId, candidate.profileId)
            val ownerships = ownershipDao.getForExportIdentity(
                profileId = candidate.profileId,
                productId = candidate.productId,
                treeUri = treeUri,
                relativeDirectory = candidate.relativeDirectory,
            )
            val current = ownerships.any { ownership ->
                ownership.exportedFingerprint == candidate.generatedFingerprint &&
                    ownership.exportedContentHash == candidate.contentHash &&
                    ownedDocumentIsCurrent(ownership, candidate)
            }
            if (!current) needs += key
        }
        ExportCurrentness(exportableItems = exportable, needsExportItems = needs)
    }

    suspend fun exportSelected(
        treeUri: String,
        headphones: List<ManagedHeadphoneRecord>,
        device: ExportDevice,
    ): PresetExportSummary = withContext(ioDispatcher) {
        val plan = buildEqLibraryExportPlan(headphones, device)
        val results = plan.duplicateConflicts.map { candidate ->
            PresetExportItemResult.Conflict(
                candidate,
                "EQ Library could not derive a unique stable export name for this preset.",
            )
        }.toMutableList<PresetExportItemResult>()

        val root = documentStore.openWritableTree(treeUri)
            ?: return@withContext PresetExportSummary(results = results, accessLost = true)

        for (candidate in plan.candidates) {
            results += exportOne(root, treeUri, candidate)
        }

        PresetExportSummary(results = results)
    }

    private fun ownedDocumentIsCurrent(
        ownership: ExportOwnershipEntity,
        candidate: PresetExportCandidate,
    ): Boolean {
        val document = ownedDocument(ownership) ?: return false
        return documentStore.contentHash(document) == candidate.contentHash
    }

    private suspend fun exportOne(
        root: ExportDirectoryHandle,
        treeUri: String,
        candidate: PresetExportCandidate,
    ): PresetExportItemResult {
        var targetDirectory = root
        for (segment in candidate.relativeDirectory.split('/').filter(String::isNotBlank)) {
            targetDirectory = ensureDirectory(targetDirectory, segment)
                ?: return PresetExportItemResult.Failed(
                    candidate,
                    "Couldn’t create or access ${candidate.relativeDirectory}.",
                )
        }

        val knownOwnerships = ownershipDao.getForExportIdentity(
            profileId = candidate.profileId,
            productId = candidate.productId,
            treeUri = treeUri,
            relativeDirectory = candidate.relativeDirectory,
        )
        for (ownership in knownOwnerships) {
            val document = ownedDocument(ownership)
            if (document == null) {
                ownershipDao.delete(ownership.documentUri)
                continue
            }
            return exportToOwnedDocument(document, ownership, treeUri, candidate)
        }

        val preferredExisting = documentStore.findFile(targetDirectory, candidate.fileName)
        val preferredOwnership = preferredExisting?.let { existing ->
            ownershipDao.getByDocumentUri(existing.uri)
        }
        if (
            preferredExisting != null &&
            preferredOwnership != null &&
            preferredOwnership.profileId == candidate.profileId &&
            preferredOwnership.productId == candidate.productId
        ) {
            return exportToOwnedDocument(preferredExisting, preferredOwnership, treeUri, candidate)
        }

        val stableId = stableExportId(candidate.productId, candidate.profileId)
        val firstRequestName = if (preferredExisting == null) {
            candidate.fileName
        } else {
            disambiguatedExportFileName(candidate.fileName, stableId)
        }
        val secondRequestName = disambiguatedExportFileName(
            candidate.fileName,
            "$stableId-eq-library",
        )

        val requestNames = linkedSetOf(firstRequestName, secondRequestName)
        var lastFailureReason = "The preset file could not be created."
        for (requestName in requestNames) {
            when (val result = createOwnedFile(targetDirectory, treeUri, candidate, requestName)) {
                is CreateOwnedFileResult.Success -> return result.result
                is CreateOwnedFileResult.RetryableFailure -> lastFailureReason = result.reason
                is CreateOwnedFileResult.UnsafeProviderBehavior -> {
                    return PresetExportItemResult.Conflict(candidate, result.reason)
                }
            }
        }
        return PresetExportItemResult.Failed(candidate, lastFailureReason)
    }

    private suspend fun exportToOwnedDocument(
        document: ExportDocumentHandle,
        ownership: ExportOwnershipEntity,
        treeUri: String,
        candidate: PresetExportCandidate,
    ): PresetExportItemResult {
        val actualName = persistedExportFileName(candidate.fileName, document.name)
        val currentHash = documentStore.contentHash(document)
        if (
            ownership.exportedFingerprint == candidate.generatedFingerprint &&
            ownership.exportedContentHash == candidate.contentHash &&
            currentHash == candidate.contentHash
        ) {
            ownershipDao.upsert(
                ownership.copy(
                    treeUri = treeUri,
                    relativeDirectory = candidate.relativeDirectory,
                    fileName = actualName,
                ),
            )
            return PresetExportItemResult.Current(candidate)
        }

        val expectedBytes = presetBytes(candidate)
        return if (replaceManagedFile(document, expectedBytes)) {
            ownershipDao.upsert(
                ownership.copy(
                    treeUri = treeUri,
                    relativeDirectory = candidate.relativeDirectory,
                    fileName = actualName,
                    exportedFingerprint = candidate.generatedFingerprint,
                    exportedContentHash = candidate.contentHash,
                    exportedAtMillis = nowMillis(),
                ),
            )
            PresetExportItemResult.Updated(candidate)
        } else {
            PresetExportItemResult.Failed(
                candidate,
                "The existing app-managed preset could not be updated.",
            )
        }
    }

    private suspend fun createOwnedFile(
        targetDirectory: ExportDirectoryHandle,
        treeUri: String,
        candidate: PresetExportCandidate,
        requestName: String,
    ): CreateOwnedFileResult {
        val preexisting = documentStore.findFile(targetDirectory, requestName)
        val preexistingOwnership = preexisting?.let { existing ->
            ownershipDao.getByDocumentUri(existing.uri)
        }
        if (
            preexisting != null &&
            preexistingOwnership != null &&
            preexistingOwnership.profileId == candidate.profileId &&
            preexistingOwnership.productId == candidate.productId
        ) {
            return CreateOwnedFileResult.Success(
                exportToOwnedDocument(preexisting, preexistingOwnership, treeUri, candidate),
            )
        }

        val created = documentStore.createFile(
            targetDirectory,
            candidate.mimeType,
            requestName,
        ) ?: return CreateOwnedFileResult.RetryableFailure(
            "The document provider did not create $requestName.",
        )

        if (preexisting != null && created.uri == preexisting.uri) {
            return CreateOwnedFileResult.UnsafeProviderBehavior(
                "The document provider returned an existing unowned file instead of creating a new preset. No file was changed.",
            )
        }

        val bytes = presetBytes(candidate)
        if (!documentStore.writeBytes(created, bytes)) {
            documentStore.delete(created)
            return CreateOwnedFileResult.RetryableFailure("The preset file could not be written.")
        }

        val actualName = persistedExportFileName(requestName, created.name)
        ownershipDao.upsert(
            ExportOwnershipEntity(
                documentUri = created.uri,
                treeUri = treeUri,
                relativeDirectory = candidate.relativeDirectory,
                profileId = candidate.profileId,
                productId = candidate.productId,
                fileName = actualName,
                exportedFingerprint = candidate.generatedFingerprint,
                exportedContentHash = candidate.contentHash,
                exportedAtMillis = nowMillis(),
            ),
        )
        return CreateOwnedFileResult.Success(PresetExportItemResult.Created(candidate))
    }

    private fun ownedDocument(ownership: ExportOwnershipEntity): ExportDocumentHandle? =
        documentStore.openDocument(ownership.documentUri)

    private fun ensureDirectory(
        parent: ExportDirectoryHandle,
        name: String,
    ): ExportDirectoryHandle? =
        documentStore.findDirectory(parent, name) ?: documentStore.createDirectory(parent, name)

    private fun replaceManagedFile(
        document: ExportDocumentHandle,
        newBytes: ByteArray,
    ): Boolean {
        val backup = documentStore.readBytes(document, MAX_BACKUP_BYTES)
        if (documentStore.writeBytes(document, newBytes)) return true
        if (backup != null) documentStore.writeBytes(document, backup)
        return false
    }

    private sealed interface CreateOwnedFileResult {
        data class Success(val result: PresetExportItemResult) : CreateOwnedFileResult
        data class RetryableFailure(val reason: String) : CreateOwnedFileResult
        data class UnsafeProviderBehavior(val reason: String) : CreateOwnedFileResult
    }

    companion object {
        private const val MAX_BACKUP_BYTES = 1024 * 1024
    }
}

/** The provider-returned display name is authoritative once its newly created URI is owned. */
internal fun persistedExportFileName(requestedName: String, providerName: String?): String =
    providerName?.takeIf(String::isNotBlank) ?: requestedName
