package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.data.catalog.AppCatalogRepository
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapter
import com.weekssa.opraeqforuapp.domain.library.CatalogSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Canonical multi-source catalog is the v0.3 source of truth.
 *
 * The legacy OPRA repository remains a temporary compatibility/failure fallback so an upgrade
 * from v0.2 does not strand saved headphones when the canonical endpoint is unavailable or has
 * not yet published its first usable snapshot.
 */
class CanonicalFirstCatalogRepository(
    private val canonicalRepository: CanonicalCatalogRepository,
    private val legacyFallback: AppCatalogRepository,
) : AppCatalogRepository {
    private val mutableState = MutableStateFlow<CatalogState>(CatalogState.Loading)

    override val state: StateFlow<CatalogState> = mutableState.asStateFlow()

    override suspend fun initialize() {
        canonicalRepository.initialize()
        val canonicalReady = canonicalRepository.state.value as? CanonicalCatalogState.Ready
        if (canonicalReady != null && canonicalReady.snapshot.isUsable()) {
            applyCanonical(canonicalReady.snapshot, canonicalReady.refreshedAtMillis)
            return
        }

        legacyFallback.initialize()
        mutableState.value = legacyFallback.state.value
    }

    override suspend fun refresh(): CatalogRefreshResult {
        val previous = mutableState.value as? CatalogState.Ready
        if (previous != null) mutableState.value = previous.copy(isRefreshing = true)

        return when (val canonicalResult = canonicalRepository.refresh()) {
            is CanonicalCatalogRefreshResult.Success -> {
                if (canonicalResult.snapshot.isUsable()) {
                    val mapped = CanonicalLegacyCatalogAdapter.adapt(canonicalResult.snapshot)
                    mutableState.value = CatalogState.Ready(
                        catalog = mapped,
                        lastSuccessfulRefreshMillis = canonicalResult.refreshedAtMillis,
                    )
                    CatalogRefreshResult.Success(mapped, canonicalResult.refreshedAtMillis)
                } else {
                    refreshFallback()
                }
            }
            is CanonicalCatalogRefreshResult.Failure -> {
                val cachedCanonical = canonicalRepository.state.value as? CanonicalCatalogState.Ready
                if (cachedCanonical != null && cachedCanonical.snapshot.isUsable()) {
                    applyCanonical(cachedCanonical.snapshot, cachedCanonical.refreshedAtMillis)
                    CatalogRefreshResult.Failure(
                        reason = canonicalResult.reason.toLegacyReason(),
                        usingSavedCatalog = true,
                    )
                } else {
                    refreshFallback()
                }
            }
        }
    }

    private suspend fun refreshFallback(): CatalogRefreshResult {
        val result = legacyFallback.refresh()
        mutableState.value = legacyFallback.state.value
        return result
    }

    private fun applyCanonical(snapshot: CatalogSnapshot, refreshedAtMillis: Long) {
        mutableState.value = CatalogState.Ready(
            catalog = CanonicalLegacyCatalogAdapter.adapt(snapshot),
            lastSuccessfulRefreshMillis = refreshedAtMillis,
        )
    }

    private fun CatalogSnapshot.isUsable(): Boolean = profiles.isNotEmpty()

    private fun CanonicalCatalogFailureReason.toLegacyReason(): CatalogRefreshFailureReason = when (this) {
        CanonicalCatalogFailureReason.Network -> CatalogRefreshFailureReason.Network
        CanonicalCatalogFailureReason.InvalidCatalog -> CatalogRefreshFailureReason.InvalidCatalog
        CanonicalCatalogFailureReason.Storage -> CatalogRefreshFailureReason.Storage
    }
}
