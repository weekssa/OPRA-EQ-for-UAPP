package com.weekssa.opraeqforuapp.data.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.PresetExportCandidate
import com.weekssa.opraeqforuapp.domain.export.buildEqLibraryExportPlan
import com.weekssa.opraeqforuapp.domain.export.disambiguatedExportFileName
import com.weekssa.opraeqforuapp.domain.export.presetBytes
import com.weekssa.opraeqforuapp.domain.export.stableExportId
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import java.io.IOException
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
        .filter { it is PresetExportItemResult.Created || it is PresetExportItemResult.Updated || it is PresetExportItemResult.Current }
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
    context: Context,
    database: OpraEqDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val ownershipDao = database.exportOwnershipDao()

    /**
     * Evaluates the active output against the exact app-owned SAF document URI and generated
     * content, not against a provider-specific display-name assumption. A provider may normalize or
     * otherwise adjust a requested filename; once EQ Library owns that returned URI, the URI and
     * stable preset identity are authoritative for later currentness/update/cleanup operations.
     */
    suspend fun evaluateCurrentness(
        treeUri: Uri?,
        headphones: List<ManagedHeadphoneRecord>,
        device: ExportDevice,
    ): ExportCurrentness = withContext(Dispatchers.IO) {
        val plan = buildEqLibraryExportPlan(headphones, device)
        val allCandidates = (plan.candidates + plan.duplicateConflicts).distinctBy {
            Triple(it.productId, it.profileId, it.generatedFingerprint)
        }
        val exportable = allCandidates.mapTo(linkedSetOf()) { candidate ->
            ExportItemKey(candidate.productId, candidate.profileId)
        }
        if (treeUri == null) {
            return@withContext ExportCurrentness(exportableItems = exportable, needsExportItems = exportable)
        }

        val tree = treeUri.toString()
        val needs = linkedSetOf<ExportItemKey>()
        for (candidate in allCandidates) {
            val key = ExportItemKey(candidate.productId, candidate.profileId)
            val ownerships = ownershipDao.getForExportIdentity(
                profileId = candidate.profileId,
                productId = candidate.productId,
                treeUri = tree,
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
        treeUri: Uri,
        headphones: List<ManagedHeadphoneRecord>,
        device: ExportDevice,
    ): PresetExportSummary = withContext(Dispatchers.IO) {
        val plan = buildEqLibraryExportPlan(headphones, device)
        val results = plan.duplicateConflicts.map { candidate ->
            PresetExportItemResult.Conflict(
                candidate,
                "EQ Library could not derive a unique stable export name for this preset.",
            )
        }.toMutableList<PresetExportItemResult>()

        val root = try {
            DocumentFile.fromTreeUri(appContext, treeUri)
        } catch (_: SecurityException) {
            null
        }
        if (root == null || !root.exists() || !root.canWrite()) {
            return@withContext PresetExportSummary(results = results, accessLost = true)
        }

        try {
            for (candidate in plan.candidates) {
                results += exportOne(root, treeUri, candidate)
            }
        } catch (_: SecurityException) {
            return@withContext PresetExportSummary(results = results, accessLost = true)
        }

        PresetExportSummary(results = results)
    }

    private fun ownedDocumentIsCurrent(
        ownership: ExportOwnershipEntity,
        candidate: PresetExportCandidate,
    ): Boolean {
        val document = ownedDocument(ownership) ?: return false
        return readContentHash(document.uri) == candidate.contentHash
    }

    private suspend fun exportOne(
        root: DocumentFile,
        treeUri: Uri,
        candidate: PresetExportCandidate,
    ): PresetExportItemResult {
        var targetDirectory = root
        for (segment in candidate.relativeDirectory.split('/').filter(String::isNotBlank)) {
            targetDirectory = ensureDirectory(targetDirectory, segment)
                ?: return PresetExportItemResult.Failed(candidate, "Couldn’t create or access ${candidate.relativeDirectory}.")
        }

        val tree = treeUri.toString()

        // First follow the stable preset identity to any exact URI we previously created. This is
        // deliberately independent of the human-readable filename requested from the provider.
        val knownOwnerships = ownershipDao.getForExportIdentity(
            profileId = candidate.profileId,
            productId = candidate.productId,
            treeUri = tree,
            relativeDirectory = candidate.relativeDirectory,
        )
        for (ownership in knownOwnerships) {
            val document = ownedDocument(ownership)
            if (document == null) {
                // Metadata for a document that is already gone is safe to discard. A later creation
                // will establish a fresh exact URI without touching any unknown replacement file.
                ownershipDao.delete(ownership.documentUri)
                continue
            }
            return exportToOwnedDocument(document, ownership, treeUri, candidate)
        }

        // Legacy/repaired ownership may still be discoverable by the preferred URI even if older
        // metadata does not match the newer identity fields exactly.
        val preferredExisting = targetDirectory.findFile(candidate.fileName)
        val preferredOwnership = preferredExisting?.let { existing ->
            ownershipDao.getByDocumentUri(existing.uri.toString())
        }
        if (
            preferredExisting != null &&
            preferredOwnership != null &&
            preferredOwnership.profileId == candidate.profileId &&
            preferredOwnership.productId == candidate.productId
        ) {
            return exportToOwnedDocument(preferredExisting, preferredOwnership, treeUri, candidate)
        }

        // Never overwrite a same-name document that we cannot prove belongs to this preset. Choose
        // a stable app-derived fallback name and let the provider normalize that creation if needed.
        val stableId = stableExportId(candidate.productId, candidate.profileId)
        val preferredNameAvailable = preferredExisting == null
        val firstRequestName = if (preferredNameAvailable) {
            candidate.fileName
        } else {
            disambiguatedExportFileName(candidate.fileName, stableId)
        }
        val secondRequestName = disambiguatedExportFileName(candidate.fileName, "$stableId-eq-library")

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
        document: DocumentFile,
        ownership: ExportOwnershipEntity,
        treeUri: Uri,
        candidate: PresetExportCandidate,
    ): PresetExportItemResult {
        val actualName = persistedExportFileName(candidate.fileName, document.name)
        val currentHash = readContentHash(document.uri)
        if (
            ownership.exportedFingerprint == candidate.generatedFingerprint &&
            ownership.exportedContentHash == candidate.contentHash &&
            currentHash == candidate.contentHash
        ) {
            ownershipDao.upsert(
                ownership.copy(
                    treeUri = treeUri.toString(),
                    relativeDirectory = candidate.relativeDirectory,
                    fileName = actualName,
                ),
            )
            return PresetExportItemResult.Current(candidate)
        }

        val expectedBytes = presetBytes(candidate)
        return if (replaceManagedFile(document.uri, expectedBytes)) {
            ownershipDao.upsert(
                ownership.copy(
                    treeUri = treeUri.toString(),
                    relativeDirectory = candidate.relativeDirectory,
                    fileName = actualName,
                    exportedFingerprint = candidate.generatedFingerprint,
                    exportedContentHash = candidate.contentHash,
                    exportedAtMillis = nowMillis(),
                ),
            )
            PresetExportItemResult.Updated(candidate)
        } else {
            PresetExportItemResult.Failed(candidate, "The existing app-managed preset could not be updated.")
        }
    }

    private suspend fun createOwnedFile(
        targetDirectory: DocumentFile,
        treeUri: Uri,
        candidate: PresetExportCandidate,
        requestName: String,
    ): CreateOwnedFileResult {
        val preexisting = targetDirectory.findFile(requestName)
        val preexistingUri = preexisting?.uri?.toString()
        val preexistingOwnership = preexistingUri?.let { ownershipDao.getByDocumentUri(it) }
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

        val created = targetDirectory.createFile(candidate.mimeType, requestName)
            ?: return CreateOwnedFileResult.RetryableFailure("The document provider did not create $requestName.")

        // ACTION_OPEN_DOCUMENT_TREE providers are expected to create a new child document. Never
        // write if a broken provider instead hands back the exact URI of a pre-existing unowned file.
        if (preexistingUri != null && created.uri.toString() == preexistingUri) {
            return CreateOwnedFileResult.UnsafeProviderBehavior(
                "The document provider returned an existing unowned file instead of creating a new preset. No file was changed.",
            )
        }

        val bytes = presetBytes(candidate)
        if (!writeBytes(created.uri, bytes)) {
            runCatching { created.delete() }
            return CreateOwnedFileResult.RetryableFailure("The preset file could not be written.")
        }

        val actualName = persistedExportFileName(requestName, created.name)
        ownershipDao.upsert(
            ExportOwnershipEntity(
                documentUri = created.uri.toString(),
                treeUri = treeUri.toString(),
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

    private fun ownedDocument(ownership: ExportOwnershipEntity): DocumentFile? {
        val uri = runCatching { Uri.parse(ownership.documentUri) }.getOrNull() ?: return null
        val document = runCatching { DocumentFile.fromSingleUri(appContext, uri) }.getOrNull() ?: return null
        return document.takeIf { it.exists() && it.isFile }
    }

    private fun ensureDirectory(parent: DocumentFile, name: String): DocumentFile? {
        val existing = parent.findFile(name)
        if (existing != null) return existing.takeIf(DocumentFile::isDirectory)
        return parent.createDirectory(name)?.takeIf(DocumentFile::isDirectory)
    }

    private fun replaceManagedFile(uri: Uri, newBytes: ByteArray): Boolean {
        val backup = try {
            resolver.openInputStream(uri)?.use { input ->
                input.readBytes().takeIf { it.size <= MAX_BACKUP_BYTES }
            }
        } catch (_: Exception) {
            null
        }

        if (writeBytes(uri, newBytes)) return true
        if (backup != null) {
            writeBytes(uri, backup)
        }
        return false
    }

    private fun writeBytes(uri: Uri, bytes: ByteArray): Boolean = try {
        resolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(bytes)
            output.flush()
        } != null
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private fun readContentHash(uri: Uri): String? = try {
        resolver.openInputStream(uri)?.use { input ->
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }
    } catch (_: Exception) {
        null
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
