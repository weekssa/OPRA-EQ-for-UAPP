package com.weekssa.opraeqforuapp.data.catalog

import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.library.CanonicalEqSelection
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

    /** Returns complete source data only when the current catalog can prove this exact projection. */
    fun resolveCanonicalSelection(profile: OpraEqProfile): CanonicalEqSelection? = null

    /** Returns complete source data only when the current catalog can prove this exact preset. */
    fun resolveCanonicalSelection(preset: GeneralEqPreset): CanonicalEqSelection? = null
}
