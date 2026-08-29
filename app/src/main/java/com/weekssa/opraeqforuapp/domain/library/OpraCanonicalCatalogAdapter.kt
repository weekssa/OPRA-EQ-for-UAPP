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
            .toList()

        val deduplicated = EqCatalogBuilder.mergeProfiles(profiles)
        return CatalogSnapshot(
            schemaVersion = 1,
            generatedAt = generatedAt,
            sourceRegistryVersion = sourceRegistryVersion,
            profiles = deduplicated,
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
