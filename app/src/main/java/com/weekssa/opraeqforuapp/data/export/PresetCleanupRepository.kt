package com.weekssa.opraeqforuapp.data.export

import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import kotlinx.coroutines.CoroutineDispatcher
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
    database: OpraEqDatabase,
    private val documentStore: ExportDocumentStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val ownershipDao = database.exportOwnershipDao()

    suspend fun deleteForProfiles(profileIds: Set<String>): PresetCleanupSummary =
        withContext(ioDispatcher) {
            deleteOwnerships(
                profileIds.flatMap { profileId -> ownershipDao.getForProfile(profileId) }
                    .distinctBy(ExportOwnershipEntity::documentUri),
            )
        }

    suspend fun deleteForProduct(productId: String): PresetCleanupSummary =
        withContext(ioDispatcher) {
            deleteOwnerships(ownershipDao.getForProduct(productId))
        }

    private suspend fun deleteOwnerships(
        ownerships: List<ExportOwnershipEntity>,
    ): PresetCleanupSummary {
        var removed = 0
        var failed = 0
        ownerships.forEach { ownership ->
            if (deleteOwnedDocument(ownership)) {
                ownershipDao.delete(ownership.documentUri)
                removed += 1
            } else {
                failed += 1
            }
        }
        return PresetCleanupSummary(
            requestedCount = ownerships.size,
            removedCount = removed,
            failedCount = failed,
        )
    }

    /**
     * Delete only the exact URI EQ Library owns. If direct provider deletion fails, traverse the
     * persisted app-owned tree/path and require the same exact child URI before deleting it.
     * Confirmed missing children are safe to forget; lookup/access failures retain ownership so a
     * later retry cannot accidentally orphan an app-managed document.
     */
    private fun deleteOwnedDocument(ownership: ExportOwnershipEntity): Boolean {
        if (documentStore.deleteByUri(ownership.documentUri)) return true
        return deleteOrConfirmMissingThroughTree(ownership)
    }

    private fun deleteOrConfirmMissingThroughTree(ownership: ExportOwnershipEntity): Boolean {
        var directory = documentStore.openWritableTree(ownership.treeUri) ?: return false
        val segments = ownership.relativeDirectory
            .split('/')
            .filter(String::isNotBlank)
        for (segment in segments) {
            directory = when (val lookup = documentStore.findDirectory(directory, segment)) {
                is ExportLookup.Found -> lookup.value
                ExportLookup.Missing -> return true
                ExportLookup.Unavailable -> return false
            }
        }

        return when (val lookup = documentStore.findFile(directory, ownership.fileName)) {
            is ExportLookup.Found -> {
                if (lookup.value.uri != ownership.documentUri) {
                    false
                } else {
                    documentStore.delete(lookup.value)
                }
            }
            ExportLookup.Missing -> true
            ExportLookup.Unavailable -> false
        }
    }
}
