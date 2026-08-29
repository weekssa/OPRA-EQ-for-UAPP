package com.weekssa.opraeqforuapp.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.HttpOpraCatalogSource
import com.weekssa.opraeqforuapp.data.catalog.OpraCatalogRepository
import com.weekssa.opraeqforuapp.data.library.CanonicalCatalogRepository
import com.weekssa.opraeqforuapp.data.library.CanonicalFirstCatalogRepository
import com.weekssa.opraeqforuapp.data.library.HttpCanonicalCatalogSource
import com.weekssa.opraeqforuapp.data.managed.ManagedHeadphonesRepository
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase

class OpraCatalogSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val database = OpraEqDatabase.create(applicationContext)
        return try {
            val catalogRepository = CanonicalFirstCatalogRepository(
                canonicalRepository = CanonicalCatalogRepository(
                    filesDir = applicationContext.filesDir,
                    source = HttpCanonicalCatalogSource(
                        userAgent = "EQ Library/${BuildConfig.VERSION_NAME}",
                    ),
                ),
                legacyFallback = OpraCatalogRepository(
                    filesDir = applicationContext.filesDir,
                    source = HttpOpraCatalogSource(
                        userAgent = "EQ Library/${BuildConfig.VERSION_NAME}",
                    ),
                ),
            )
            val managedRepository = ManagedHeadphonesRepository(database)
            val outcome = CatalogSyncCoordinator(
                catalogRepository = catalogRepository,
                managedHeadphonesRepository = managedRepository,
            ).refresh()

            when (val catalogResult = outcome.catalogResult) {
                is CatalogRefreshResult.Success -> {
                    val changes = outcome.managedChanges
                    Result.success(
                        workDataOf(
                            KEY_AFFECTED_HEADPHONES to (changes?.affectedProductIds?.size ?: 0),
                            KEY_NEW_PROFILES to (changes?.newProfileCount ?: 0),
                            KEY_UPDATED_PROFILES to (changes?.updatedSelectedProfileCount ?: 0),
                            KEY_REMOVED_PROFILES to (changes?.removedSelectedProfileCount ?: 0),
                            KEY_BECAME_INCOMPATIBLE to (changes?.becameNotCompatibleSelectedProfileCount ?: 0),
                        ),
                    )
                }
                is CatalogRefreshResult.Failure -> when (catalogResult.reason) {
                    CatalogRefreshFailureReason.Network -> Result.retry()
                    CatalogRefreshFailureReason.InvalidCatalog,
                    CatalogRefreshFailureReason.Storage,
                    -> Result.success()
                }
            }
        } finally {
            database.close()
        }
    }

    companion object {
        const val KEY_AFFECTED_HEADPHONES = "affected_headphones"
        const val KEY_NEW_PROFILES = "new_profiles"
        const val KEY_UPDATED_PROFILES = "updated_profiles"
        const val KEY_REMOVED_PROFILES = "removed_profiles"
        const val KEY_BECAME_INCOMPATIBLE = "became_incompatible"
    }
}
