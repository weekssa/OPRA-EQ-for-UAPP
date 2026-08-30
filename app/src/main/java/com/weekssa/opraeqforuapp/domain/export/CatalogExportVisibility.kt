package com.weekssa.opraeqforuapp.domain.export

import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences

data class CatalogExportVisibility(
    val catalog: OpraCatalog,
    val hiddenProfileCount: Int,
)

/**
 * Builds a presentation-only catalog view for the user's selected export targets.
 *
 * The source catalog is never mutated. When broader-library visibility is enabled (the default),
 * every canonical/runtime profile remains visible. When it is disabled, only profiles that can be
 * exported exactly or through an approved target-specific optimization remain in normal Browse.
 */
fun OpraCatalog.forExportTargetVisibility(
    preferences: ExportTargetPreferences,
): CatalogExportVisibility {
    if (preferences.showUnexportablePresets) {
        return CatalogExportVisibility(catalog = this, hiddenProfileCount = 0)
    }

    val visibleProfiles = profiles.filter { profile ->
        profile.isExportableToAny(preferences.selectedTargets)
    }
    val visibleProductIds = visibleProfiles
        .mapTo(mutableSetOf()) { profile -> canonicalProductId(profile.productId) }
    val visibleProducts = products.filter { product -> canonicalProductId(product.id) in visibleProductIds }
    val visibleVendorIds = visibleProducts.mapTo(mutableSetOf()) { it.vendorId }
    val visibleVendors = vendors.filter { it.id in visibleVendorIds }

    return CatalogExportVisibility(
        catalog = copy(
            vendors = visibleVendors,
            products = visibleProducts,
            profiles = visibleProfiles,
        ),
        hiddenProfileCount = profiles.size - visibleProfiles.size,
    )
}
