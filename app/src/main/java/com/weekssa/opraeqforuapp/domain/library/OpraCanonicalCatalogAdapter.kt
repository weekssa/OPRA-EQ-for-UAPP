package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog

object OpraCanonicalCatalogAdapter {
    fun adapt(
        catalog: OpraCatalog,
        generatedAt: String,
        sourceRegistryVersion: String,
        discoveredAtEpochSeconds: Long? = null,
    ): CatalogSnapshot {
        val profiles = catalog.products.asSequence()
            .flatMap { product ->
                val vendor = catalog.vendor(product.vendorId) ?: return@flatMap emptySequence()
                catalog.profilesForProduct(product.id).asSequence().mapNotNull { profile ->
                    OpraProfileAdapter.adapt(
                        vendor = vendor,
                        product = product,
                        profile = profile,
                        discoveredAtEpochSeconds = discoveredAtEpochSeconds,
                    )
                }
            }
            .distinctBy { profile ->
                val revision = profile.latestRevision
                listOf(profile.headphone.normalizedKey, revision.acousticFingerprint)
            }
            .sortedWith(
                compareBy<CanonicalEqProfile> { it.headphone.manufacturer.lowercase() }
                    .thenBy { it.headphone.model.lowercase() }
                    .thenBy { it.creator.orEmpty().lowercase() }
                    .thenBy { it.tuningLabel.orEmpty().lowercase() },
            )
            .toList()

        return CatalogSnapshot(
            schemaVersion = 1,
            generatedAt = generatedAt,
            sourceRegistryVersion = sourceRegistryVersion,
            profiles = profiles,
            sources = listOf(
                SourceStatus(
                    sourceId = "opra",
                    lifecycle = SourceLifecycle.ACTIVE,
                    lastSuccessfulScanAt = generatedAt,
                    lastAttemptAt = generatedAt,
                    parserVersion = "1",
                    redistribution = RedistributionMode.STRUCTURED_DATA_ONLY,
                ),
            ),
        )
    }
}
