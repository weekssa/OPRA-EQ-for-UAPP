package com.weekssa.opraeqforuapp.data.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.export.PresetExportCandidate
import com.weekssa.opraeqforuapp.domain.export.buildPresetExportPlan
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
}

class PresetExportRepository(
    context: Context,
    database: OpraEqDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val ownershipDao = database.exportOwnershipDao()

    suspend fun exportSelected(
        treeUri: Uri,
        headphones: List<ManagedHeadphoneRecord>,
    ): PresetExportSummary = withContext(Dispatchers.IO) {
        val plan = buildPresetExportPlan(headphones)
        val results = plan.duplicateConflicts.map { candidate ->
            PresetExportItemResult.Conflict(
                candidate,
                "Two selected OPRA profiles resolve to the same deterministic filename. No file was written.",
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

    private suspend fun exportOne(
        root: DocumentFile,
        treeUri: Uri,
        candidate: PresetExportCandidate,
    ): PresetExportItemResult {
        val manufacturerDirectory = ensureDirectory(root, candidate.manufacturerName)
            ?: return PresetExportItemResult.Failed(candidate, "Couldn’t create or access the manufacturer folder.")
        val modelDirectory = ensureDirectory(manufacturerDirectory, candidate.modelName)
            ?: return PresetExportItemResult.Failed(candidate, "Couldn’t create or access the model folder.")

        val existing = modelDirectory.findFile(candidate.fileName)
        if (existing != null) {
            val ownership = ownershipDao.getByDocumentUri(existing.uri.toString())
                ?: return PresetExportItemResult.Conflict(
                    candidate,
                    "A file with this deterministic name already exists and is not known to be managed by OPRA EQ for UAPP.",
                )
            if (ownership.profileId != candidate.profileId || ownership.productId != candidate.productId) {
                return PresetExportItemResult.Conflict(
                    candidate,
                    "The deterministic filename is already owned by a different app-managed preset.",
                )
            }

            val expectedBytes = candidate.xml.toByteArray(Charsets.ISO_8859_1)
            val currentHash = readContentHash(existing.uri)
            if (
                ownership.exportedFingerprint == candidate.generatedFingerprint &&
                ownership.exportedContentHash == candidate.contentHash &&
                currentHash == candidate.contentHash
            ) {
                return PresetExportItemResult.Current(candidate)
            }

            return if (replaceManagedFile(existing.uri, expectedBytes)) {
                ownershipDao.upsert(
                    ownership.copy(
                        treeUri = treeUri.toString(),
                        relativeDirectory = candidate.relativeDirectory,
                        fileName = candidate.fileName,
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

        val created = modelDirectory.createFile(XML_MIME_TYPE, candidate.fileName)
            ?: return PresetExportItemResult.Failed(candidate, "The preset file could not be created.")
        if (created.name != candidate.fileName) {
            runCatching { created.delete() }
            return PresetExportItemResult.Conflict(
                candidate,
                "The selected document provider changed the deterministic filename, so the file was not kept.",
            )
        }

        val bytes = candidate.xml.toByteArray(Charsets.ISO_8859_1)
        if (!writeBytes(created.uri, bytes)) {
            runCatching { created.delete() }
            return PresetExportItemResult.Failed(candidate, "The preset file could not be written.")
        }

        ownershipDao.upsert(
            ExportOwnershipEntity(
                documentUri = created.uri.toString(),
                treeUri = treeUri.toString(),
                relativeDirectory = candidate.relativeDirectory,
                profileId = candidate.profileId,
                productId = candidate.productId,
                fileName = candidate.fileName,
                exportedFingerprint = candidate.generatedFingerprint,
                exportedContentHash = candidate.contentHash,
                exportedAtMillis = nowMillis(),
            ),
        )
        return PresetExportItemResult.Created(candidate)
    }

    private fun ensureDirectory(parent: DocumentFile, name: String): DocumentFile? {
        val existing = parent.findFile(name)
        if (existing != null) return existing.takeIf(DocumentFile::isDirectory)
        return parent.createDirectory(name)?.takeIf { it.name == name }
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

    companion object {
        private const val XML_MIME_TYPE = "application/xml"
        private const val MAX_BACKUP_BYTES = 1024 * 1024
    }
}
