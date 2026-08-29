package com.weekssa.opraeqforuapp.data.catalog

import kotlinx.coroutines.flow.StateFlow

/**
 * Catalog contract consumed by the existing managed-headphone and export engine.
 *
 * v0.3 can satisfy this contract from the canonical multi-source catalog while keeping the
 * v0.2 OPRA implementation available as a compatibility/failure fallback during migration.
 */
interface AppCatalogRepository {
    val state: StateFlow<CatalogState>

    suspend fun initialize()

    suspend fun refresh(): CatalogRefreshResult
}
