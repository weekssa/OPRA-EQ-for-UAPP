package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog

object OpraCanonicalCatalogAdapter {
    data class QuarantinedProfile(
        val profileId: String,
        val productId: String,
        val reason: String,
    )

    data class AdaptationResult(
        val snapshot: CatalogSnapshot,
        val quarantinedProfiles: List<QuarantinedProfile>,
    )

    fun adapt(
        catalog: OpraCatalog,
        generatedAt: String,
        sourceRegistryVersion: String,
        discoveredAtEpochSeconds: Long? = null,
    ): AdaptationResult {
        val profiles = mutableListOf<CanonicalEqProfile>()
        val quarantined = mutableListOf<QuarantinedProfile>()
        val processedProfileIds = mutableSetOf<String>()
        catalog.products.forEach { product ->
            val sourceProfiles = catalog.profilesForProduct(product.id)
            val vendor = catalog.vendor(product.vendorId)
            if (vendor == null) {
                sourceProfiles.forEach { profile ->
                    processedProfileIds += profile.id
                    quarantined += QuarantinedProfile(
                        profileId = profile.id,
                        productId = profile.productId,
                        reason = "The OPRA product has no matching vendor record.",
                    )
                }
                return@forEach
            }
            sourceProfiles.forEach { profile ->
                processedProfileIds += profile.id
                val adapted = OpraProfileAdapter.adapt(
                    vendor = vendor,
                    product = product,
                    profile = profile,
                    discoveredAtEpochSeconds = discoveredAtEpochSeconds,
                )
                if (adapted == null) {
                    quarantined += QuarantinedProfile(
                        profileId = profile.id,
                        productId = profile.productId,
                        reason = "The OPRA profile is incomplete or contains invalid numeric fields.",
                    )
                } else {
                    profiles += adapted
                }
            }
        }
        catalog.profiles.filterNot { it.id in processedProfileIds }.forEach { profile ->
            quarantined += QuarantinedProfile(
                profileId = profile.id,
                productId = profile.productId,
                reason = "The OPRA profile has no matching product record.",
            )
        }

        // AcousticFingerprint is intentionally order-independent. OPRA order, however, is export
        // priority for a band-limited target. Deduplicate only when both the acoustic content and
        // the ordered source representation match; otherwise the lower-priority tail could change.
        val priorityDistinctProfiles = profiles.distinctBy { profile ->
            val revision = profile.latestRevision
            listOf(
                requireNotNull(profile.headphone).normalizedKey,
                revision.acousticFingerprint,
                AcousticFingerprint.sourcePriority(revision.filters),
            )
        }
        val snapshotProfiles = priorityDistinctProfiles
            .sortedWith(
                compareBy<CanonicalEqProfile> { requireNotNull(it.headphone).manufacturer.lowercase() }
                    .thenBy { requireNotNull(it.headphone).model.lowercase() }
                    .thenBy { it.creator.orEmpty().lowercase() }
                    .thenBy { it.tuningLabel.orEmpty().lowercase() },
            )
            .toList()

        return AdaptationResult(
            snapshot = CatalogSnapshot(
                schemaVersion = 1,
                generatedAt = generatedAt,
                sourceRegistryVersion = sourceRegistryVersion,
                profiles = snapshotProfiles,
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
            ),
            quarantinedProfiles = quarantined.distinctBy { it.profileId },
        )
    }
}
