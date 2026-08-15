package com.weekssa.opraeqforuapp.data.sync

import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.OpraCatalogRepository
import com.weekssa.opraeqforuapp.data.managed.ManagedCatalogChangeSummary
import com.weekssa.opraeqforuapp.data.managed.ManagedHeadphonesRepository

data class CatalogSyncOutcome(
    val catalogResult: CatalogRefreshResult,
    val managedChanges: ManagedCatalogChangeSummary? = null,
)

class CatalogSyncCoordinator(
    private val catalogRepository: OpraCatalogRepository,
    private val managedHeadphonesRepository: ManagedHeadphonesRepository,
) {
    suspend fun refresh(): CatalogSyncOutcome {
        val result = catalogRepository.refresh()
        val changes = if (result is CatalogRefreshResult.Success) {
            managedHeadphonesRepository.reconcileCatalog(result.catalog)
        } else {
            null
        }
        return CatalogSyncOutcome(
            catalogResult = result,
            managedChanges = changes,
        )
    }
}
