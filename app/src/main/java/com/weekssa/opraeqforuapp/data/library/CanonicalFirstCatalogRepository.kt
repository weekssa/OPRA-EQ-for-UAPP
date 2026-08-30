package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.data.catalog.AppCatalogRepository
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.library.CanonicalCatalogFailureReason
import com.weekssa.opraeqforuapp.data.library.CanonicalCatalogRefreshResult
import com.weekssa.opraeqforuapp.data.library.CanonicalCatalogRepository
import com.weekssa.opraeqforuapp.data.library.CanonicalCatalogState
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapter
import com.weekssa.opraeqforuapp.domain.library.CatalogSnapshot
import com.weekssa.opraeqforuapp.domain.library.overlayCanonicalCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * v0.3 multi-source catalog bridge.
 *
 * The complete OPRA catalog remains the compatibility/base layer while canonical records are
 * overlaid on top. This prevents a small canonical snapshot from shrinking the v0.2 library:
 * matching OPRA IDs gain canonical provenance/revision metadata, canonical-only sources and
 * historical revisions are added, and all other OPRA records remain available.
 */
class CanonicalFirstCatalogRepository(
    private val canonicalRepository: CanonicalCatalogRepository,
    private val legacyFallback: AppCatalogRepository,
) : AppCatalogRepository {
    private val mutableState = MutableStateFlow<CatalogState>(CatalogState.Loading)

    override val state: StateFlow<CatalogState> = mutableState.asStateFlow()

    override suspend fun initialize() {
        canonicalRepository.initialize()
        legacyFallback.initialize()
        renderAvailableCatalog()
    }

    override suspend fun refresh(): CatalogRefreshResult {
        val previous = mutableState.value as? CatalogState.Ready
        if (previous != null) mutableState.value = previous.copy(isRefreshing = true)

        val canonicalResult = canonicalRepository.refresh()
        val legacyResult = legacyFallback.refresh()
        val ready = renderAvailableCatalog()

        if (ready != null) {
            val eitherSucceeded = canonicalResult is CanonicalCatalogRefreshResult.Success ||
                legacyResult is CatalogRefreshResult.Success
            if (eitherSucceeded) {
                return CatalogRefreshResult.Success(
                    catalog = ready.catalog,
                    refreshedAtMillis = ready.lastSuccessfulRefreshMillis,
                )
            }
            return CatalogRefreshResult.Failure(
                reason = when (canonicalResult) {
                    is CanonicalCatalogRefreshResult.Failure -> canonicalResult.reason.toLegacyReason()
                    is CanonicalCatalogRefreshResult.Success -> CatalogRefreshFailureReason.Network
                },
                usingSavedCatalog = true,
            )
        }

        return when {
            canonicalResult is CanonicalCatalogRefreshResult.Failure -> CatalogRefreshResult.Failure(
                reason = canonicalResult.reason.toLegacyReason(),
                usingSavedCatalog = false,
            )
            legacyResult is CatalogRefreshResult.Failure -> legacyResult
            else -> CatalogRefreshResult.Failure(CatalogRefreshFailureReason.InvalidCatalog, usingSavedCatalog = false)
        }
    }

    private fun renderAvailableCatalog(): CatalogState.Ready? {
        val canonicalReady = canonicalRepository.state.value as? CanonicalCatalogState.Ready
        val legacyReady = legacyFallback.state.value as? CatalogState.Ready
        val canonicalCatalog = canonicalReady
            ?.takeIf { it.snapshot.isUsable() }
            ?.let { CanonicalLegacyCatalogAdapter.adapt(it.snapshot) }

        val catalog: OpraCatalog = when {
            legacyReady != null && canonicalCatalog != null ->
                overlayCanonicalCatalog(
                    legacy = legacyReady.catalog,
                    canonical = canonicalCatalog,
                    headphoneAliases = canonicalReady.snapshot.headphoneAliases,
                )
            canonicalCatalog != null -> canonicalCatalog
            legacyReady != null -> legacyReady.catalog
            else -> {
                mutableState.value = unavailableState()
                return null
            }
        }

        val refreshedAt = maxOf(
            legacyReady?.lastSuccessfulRefreshMillis ?: Long.MIN_VALUE,
            canonicalReady?.refreshedAtMillis ?: Long.MIN_VALUE,
        ).takeIf { it != Long.MIN_VALUE } ?: System.currentTimeMillis()

        return CatalogState.Ready(
            catalog = catalog,
            lastSuccessfulRefreshMillis = refreshedAt,
        ).also { mutableState.value = it }
    }

    private fun unavailableState(): CatalogState.Unavailable {
        val canonicalReason = (canonicalRepository.state.value as? CanonicalCatalogState.Unavailable)?.reason
        val legacyReason = (legacyFallback.state.value as? CatalogState.Unavailable)?.reason
        return CatalogState.Unavailable(
            reason = canonicalReason?.toLegacyReason() ?: legacyReason ?: CatalogRefreshFailureReason.Network,
        )
    }

    private fun CatalogSnapshot.isUsable(): Boolean = profiles.isNotEmpty()

    private fun CanonicalCatalogFailureReason.toLegacyReason(): CatalogRefreshFailureReason = when (this) {
        CanonicalCatalogFailureReason.Network -> CatalogRefreshFailureReason.Network
        CanonicalCatalogFailureReason.InvalidCatalog -> CatalogRefreshFailureReason.InvalidCatalog
        CanonicalCatalogFailureReason.Storage -> CatalogRefreshFailureReason.Storage
    }
}
