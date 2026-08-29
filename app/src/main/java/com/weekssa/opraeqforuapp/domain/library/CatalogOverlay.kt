package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor

/**
 * Adds canonical v0.3 records on top of the complete legacy OPRA catalog.
 *
 * Matching IDs are intentionally replaced by canonical records so the latest OPRA-backed
 * revision can carry v0.3 provenance/revision metadata, while every OPRA product/profile not
 * represented in the canonical snapshot remains available. Canonical-only sources and historical
 * revisions are appended with their stable synthetic IDs.
 */
fun overlayCanonicalCatalog(
    legacy: OpraCatalog,
    canonical: OpraCatalog,
): OpraCatalog {
    val vendors = linkedMapOf<String, OpraVendor>()
    legacy.vendors.forEach { vendors[it.id] = it }
    canonical.vendors.forEach { vendors[it.id] = it }

    val products = linkedMapOf<String, OpraProduct>()
    legacy.products.forEach { products[it.id] = it }
    canonical.products.forEach { products[it.id] = it }

    val profiles = linkedMapOf<String, OpraEqProfile>()
    legacy.profiles.forEach { profiles[it.id] = it }
    canonical.profiles.forEach { profiles[it.id] = it }

    return OpraCatalog(
        vendors = vendors.values.toList(),
        products = products.values.toList(),
        profiles = profiles.values.toList(),
        ignoredEntryCount = legacy.ignoredEntryCount + canonical.ignoredEntryCount,
    )
}
