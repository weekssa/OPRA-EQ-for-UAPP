package com.weekssa.opraeqforuapp.data.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PresetCleanupSummary(
    val requestedCount: Int,
    val removedCount: Int,
    val failedCount: Int,
) {
    val allRemoved: Boolean get() = failedCount == 0
}

class PresetCleanupRepository(
    context: Context,
    database: OpraEqDatabase,
) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val ownershipDao = database.exportOwnershipDao()

    suspend fun deleteForProfiles(profileIds: Set<String>): PresetCleanupSummary =
        deleteOwnerships(
            profileIds.flatMap { profileId -> ownershipDao.getForProfile(profileId) }
                .distinctBy(ExportOwnershipEntity::documentUri),
        )

    suspend fun deleteForProduct(productId: String): PresetCleanupSummary =
        deleteOwnerships(ownershipDao.getForProduct(productId))

    private suspend fun deleteOwnerships(
        ownerships: List<ExportOwnershipEntity>,
    ): PresetCleanupSummary = withContext(Dispatchers.IO) {
        var removed = 0
        var failed = 0
        ownerships.forEach { ownership ->
            val succeeded = deleteOwnedDocument(ownership)
            if (succeeded) {
                ownershipDao.delete(ownership.documentUri)
                removed += 1
            } else {
                failed += 1
            }
        }
        PresetCleanupSummary(
            requestedCount = ownerships.size,
            removedCount = removed,
            failedCount = failed,
        )
    }

    /**
     * Exported files are descendants of a user-granted ACTION_OPEN_DOCUMENT_TREE URI.
     * Keep deletion anchored to that tree permission instead of treating the child URI
     * as though it came from ACTION_OPEN_DOCUMENT/ACTION_CREATE_DOCUMENT.
     */
    private fun deleteOwnedDocument(ownership: ExportOwnershipEntity): Boolean {
        val documentUri = runCatching { Uri.parse(ownership.documentUri) }.getOrNull() ?: return false

        val directlyDeleted = try {
            DocumentsContract.deleteDocument(resolver, documentUri)
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
        if (directlyDeleted) return true

        return deleteOrConfirmMissingThroughTree(ownership, documentUri)
    }

    /**
     * If direct deletion fails, traverse only the persisted app-owned tree/path. A missing
     * recorded path means the exported file is already gone. If a same-name replacement has
     * a different URI, do not delete it because it is no longer the exact app-owned document.
     */
    private fun deleteOrConfirmMissingThroughTree(
        ownership: ExportOwnershipEntity,
        expectedDocumentUri: Uri,
    ): Boolean {
        val treeUri = runCatching { Uri.parse(ownership.treeUri) }.getOrNull() ?: return false
        val root = try {
            DocumentFile.fromTreeUri(appContext, treeUri)
        } catch (_: SecurityException) {
            null
        } ?: return false

        var directory = root
        val segments = ownership.relativeDirectory
            .split('/')
            .filter(String::isNotBlank)
        for (segment in segments) {
            val child = try {
                directory.findFile(segment)
            } catch (_: SecurityException) {
                return false
            } ?: return true
            if (!child.isDirectory) return false
            directory = child
        }

        val target = try {
            directory.findFile(ownership.fileName)
        } catch (_: SecurityException) {
            return false
        } ?: return true

        if (target.uri != expectedDocumentUri) return false
        return try {
            target.delete()
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
