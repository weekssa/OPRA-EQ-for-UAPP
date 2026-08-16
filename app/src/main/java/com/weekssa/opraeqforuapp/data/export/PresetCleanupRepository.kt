package com.weekssa.opraeqforuapp.data.export

import android.content.Context
import android.net.Uri
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
            val uri = Uri.parse(ownership.documentUri)
            val succeeded = try {
                val document = DocumentFile.fromSingleUri(appContext, uri)
                when {
                    document == null -> true
                    !document.exists() -> true
                    else -> document.delete()
                }
            } catch (_: SecurityException) {
                false
            } catch (_: Exception) {
                false
            }
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
}
