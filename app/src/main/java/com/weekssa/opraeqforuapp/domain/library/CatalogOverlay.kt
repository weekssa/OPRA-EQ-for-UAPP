package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import java.util.Locale

/**
 * Adds canonical v0.3 records on top of the complete legacy OPRA catalog.
 *
 * Matching IDs are intentionally replaced by canonical records so the latest OPRA-backed
 * revision can carry v0.3 provenance/revision metadata, while every OPRA product/profile not
 * represented in the canonical snapshot remains available. Canonical-only sources and historical
 * revisions are appended with their stable synthetic IDs.
 *
 * Product identity is resolved conservatively. Exact normalized manufacturer/model matches are
 * always safe to collapse. Broader equivalence must come from an explicit catalog/source alias.
 * Alternate product IDs remain resolvable so cleanup does not break existing managed state.
 */
fun overlayCanonicalCatalog(
    legacy: OpraCatalog,
    canonical: OpraCatalog,
    headphoneAliases: List<HeadphoneAliasGroup> = emptyList(),
): OpraCatalog {
    val vendors = linkedMapOf<String, OpraVendor>()
    legacy.vendors.forEach { vendors[it.id] = it }
    canonical.vendors.forEach { vendors[it.id] = it }

    val rawProducts = linkedMapOf<String, OpraProduct>()
    legacy.products.forEach { rawProducts[it.id] = it }
    canonical.products.forEach { rawProducts[it.id] = it }

    val aliasResolution = resolveProductAliases(
        legacy = legacy,
        canonical = canonical,
        vendors = vendors,
        headphoneAliases = headphoneAliases,
    )
    val visibleProducts = rawProducts.values
        .filter { resolveProductId(it.id, aliasResolution.aliases) == it.id }
        .map { product -> aliasResolution.preferredProducts[product.id] ?: product }

    val visibleVendorIds = visibleProducts.map(OpraProduct::vendorId).toSet()

    val profiles = linkedMapOf<String, OpraEqProfile>()
    legacy.profiles.forEach { profiles[it.id] = it.toUserFacingProfile(defaultSource = "OPRA") }
    canonical.profiles.forEach { profiles[it.id] = it.toUserFacingProfile(defaultSource = null) }
    val resolvedProfiles = profiles.values.map { profile ->
        profile.copy(productId = resolveProductId(profile.productId, aliasResolution.aliases))
    }

    return OpraCatalog(
        vendors = vendors.values.filter { it.id in visibleVendorIds },
        products = visibleProducts,
        profiles = deduplicateAcoustically(resolvedProfiles),
        ignoredEntryCount = legacy.ignoredEntryCount + canonical.ignoredEntryCount,
        productAliases = aliasResolution.aliases,
    )
}

private data class ProductAliasResolution(
    val aliases: Map<String, String>,
    val preferredProducts: Map<String, OpraProduct>,
)

private fun resolveProductAliases(
    legacy: OpraCatalog,
    canonical: OpraCatalog,
    vendors: Map<String, OpraVendor>,
    headphoneAliases: List<HeadphoneAliasGroup>,
): ProductAliasResolution {
    val aliases = linkedMapOf<String, String>()
    val preferred = linkedMapOf<String, OpraProduct>()
    val allProducts = (legacy.products + canonical.products).distinctBy(OpraProduct::id)
    val legacyIds = legacy.products.map(OpraProduct::id).toSet()

    fun vendorKey(product: OpraProduct): String? =
        vendors[product.vendorId]?.name?.let(::normalizeIdentityText)?.takeIf(String::isNotEmpty)

    fun applyIdentityGroup(
        manufacturer: String,
        canonicalModel: String,
        modelAliases: Collection<String>,
    ) {
        val manufacturerKey = normalizeIdentityText(manufacturer)
        val canonicalKey = normalizeIdentityText(canonicalModel)
        if (manufacturerKey.isEmpty() || canonicalKey.isEmpty()) return
        val acceptedKeys = (modelAliases + canonicalModel)
            .map(::normalizeIdentityText)
            .filter(String::isNotEmpty)
            .toSet()
        val matches = allProducts.filter { product ->
            vendorKey(product) == manufacturerKey && normalizeIdentityText(product.name) in acceptedKeys
        }
        if (matches.isEmpty()) return

        val retained = matches.firstOrNull {
            it.id in legacyIds && normalizeIdentityText(it.name) == canonicalKey
        } ?: matches.firstOrNull { it.id in legacyIds }
            ?: matches.firstOrNull { normalizeIdentityText(it.name) == canonicalKey }
            ?: matches.first()
        val retainedRoot = resolveProductId(retained.id, aliases)

        matches.forEach { product ->
            val root = resolveProductId(product.id, aliases)
            if (root != retainedRoot) aliases[root] = retainedRoot
            if (product.id != retainedRoot) aliases[product.id] = retainedRoot
        }

        val retainedProduct = allProducts.firstOrNull { it.id == retainedRoot } ?: retained
        preferred[retainedRoot] = retainedProduct.copy(
            name = canonicalModel,
            aliases = (
                modelAliases +
                    matches.map(OpraProduct::name) +
                    matches.flatMap(OpraProduct::aliases)
                )
                .filterNot { normalizeIdentityText(it) == canonicalKey }
                .distinctBy(::normalizeIdentityText),
        )
    }

    // Global safe cleanup: punctuation, casing and spacing variants of the same manufacturer/model
    // are equivalent even when no canonical source happens to touch that headphone yet.
    allProducts
        .groupBy { product -> vendorKey(product) to normalizeIdentityText(product.name) }
        .values
        .filter { group -> group.size > 1 && group.first().name.isNotBlank() }
        .forEach { group ->
            val representative = group.firstOrNull { it.id in legacyIds } ?: group.first()
            val manufacturer = vendors[representative.vendorId]?.name ?: return@forEach
            applyIdentityGroup(manufacturer, representative.name, emptyList())
        }

    // Canonical profiles can carry qualified aliases from their source manifest.
    canonical.products.forEach { canonicalProduct ->
        val manufacturer = vendors[canonicalProduct.vendorId]?.name ?: return@forEach
        applyIdentityGroup(manufacturer, canonicalProduct.name, canonicalProduct.aliases)
    }

    // Catalog-level aliases let the audit/curation pipeline clean OPRA-only products without an APK
    // release. These groups are intentionally explicit rather than inferred by fuzzy matching.
    headphoneAliases.forEach { group ->
        applyIdentityGroup(group.manufacturer, group.canonicalModel, group.aliases)
    }

    // A later explicit group can connect roots created by an earlier exact-normalized group. Normalize
    // both the alias map and preferred display records to their final retained IDs.
    val flattenedAliases = aliases.keys.associateWith { key -> resolveProductId(key, aliases) }
        .filterValues { value -> value.isNotBlank() }
        .filter { (key, value) -> key != value }
    val normalizedPreferred = linkedMapOf<String, OpraProduct>()
    preferred.forEach { (id, product) ->
        val root = resolveProductId(id, flattenedAliases)
        val rootProduct = allProducts.firstOrNull { it.id == root } ?: product
        normalizedPreferred[root] = rootProduct.copy(
            name = product.name,
            aliases = (rootProduct.aliases + product.aliases)
                .distinctBy(::normalizeIdentityText),
        )
    }

    return ProductAliasResolution(
        aliases = flattenedAliases,
        preferredProducts = normalizedPreferred,
    )
}

private fun resolveProductId(productId: String, aliases: Map<String, String>): String {
    var current = productId
    val visited = mutableSetOf<String>()
    while (visited.add(current)) {
        val next = aliases[current] ?: return current
        if (next == current) return current
        current = next
    }
    return productId
}

private fun normalizeIdentityText(value: String): String =
    value.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

private fun deduplicateAcoustically(profiles: List<OpraEqProfile>): List<OpraEqProfile> {
    val retained = linkedMapOf<String, OpraEqProfile>()
    profiles.forEach { profile ->
        val acousticKey = profile.legacyAcousticSignature()
        if (acousticKey == null) {
            retained["id:${profile.id}"] = profile
            return@forEach
        }
        val key = "${profile.productId}|$acousticKey"
        val previous = retained[key]
        if (previous == null || profile.preferenceScore() > previous.preferenceScore()) {
            retained[key] = profile
        }
    }
    return retained.values.toList()
}

private fun OpraEqProfile.preferenceScore(): Int {
    var score = 0
    val detailText = details.orEmpty()
    if (detailText.contains("Latest", ignoreCase = true)) score += 30
    if (id.startsWith("eq-library:", ignoreCase = true)) score += 20
    if (detailText.contains("Measurement:", ignoreCase = true)) score += 10
    if (!link.isNullOrBlank()) score += 5
    if (!author.isNullOrBlank()) score += 1
    return score
}

private fun OpraEqProfile.toUserFacingProfile(defaultSource: String?): OpraEqProfile {
    val originalParts = details.orEmpty()
        .split(" · ")
        .map(String::trim)
        .filter(String::isNotEmpty)

    val status = originalParts.firstOrNull {
        it.equals("Latest", ignoreCase = true) || it.equals("Previous revision", ignoreCase = true)
    }
    val revisionDate = originalParts.firstOrNull { it.startsWith("Revision date:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.takeIf(String::isNotEmpty)
    val explicitTarget = originalParts.firstOrNull { it.startsWith("Target:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.takeIf(String::isNotEmpty)
    val rawTarget = originalParts.firstNotNullOfOrNull(::targetFromRawLabel)
    val target = humanizeTarget(explicitTarget ?: rawTarget)
    val explicitSource = originalParts.firstOrNull { it.startsWith("Source:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.takeIf(String::isNotEmpty)
    val source = humanizeSource(explicitSource ?: defaultSource)
    val measurement = originalParts.firstNotNullOfOrNull(::measurementFromLabel)
    val context = originalParts.mapNotNull { part ->
        humanReadableContext(
            value = part,
            status = status,
            target = explicitTarget,
        )
    }
    val existingSoundSummary = originalParts.firstOrNull(::looksLikeSoundSummary)
    val hasBands = bands.orEmpty().isNotEmpty()
    val generatedSoundSummary = soundImpactFromLegacyBands()
        ?: if (hasBands) "Makes small frequency-response adjustments." else null

    val compactDetails = buildList {
        status?.let(::add)
        if (status.equals("Previous revision", ignoreCase = true)) {
            revisionDate?.let { add("Revision: $it") }
        }
        measurement?.let { add("Measurement: $it") }
        context.forEach(::add)
        target?.let { add("Target: $it") }
        source?.let { add("Source: $it") }
        (existingSoundSummary ?: generatedSoundSummary)?.let(::add)
    }.distinct().joinToString(" · ")

    return copy(details = compactDetails.takeIf(String::isNotBlank))
}

private fun humanReadableContext(value: String, status: String?, target: String?): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.equals(status, ignoreCase = true)) return null
    if (trimmed.startsWith("Revision date:", ignoreCase = true)) return null
    if (trimmed.startsWith("Target:", ignoreCase = true)) return null
    if (trimmed.startsWith("Source:", ignoreCase = true)) return null
    if (trimmed.startsWith("Provenance:", ignoreCase = true)) return null
    if (trimmed.startsWith("Version:", ignoreCase = true)) return null
    if (targetFromRawLabel(trimmed) != null) return null
    if (measurementFromLabel(trimmed) != null) return null
    if (looksLikeSoundSummary(trimmed)) return null
    if (trimmed.equals("Consolidated", ignoreCase = true)) return null
    if (target != null && trimmed.equals("$target Target", ignoreCase = true)) return null
    return trimmed
}

private fun targetFromRawLabel(value: String): String? = when {
    value.startsWith("Target_", ignoreCase = true) -> value.substringAfter('_')
    value.equals("HRTF_5128_Diffuse_Field", ignoreCase = true) -> value
    else -> null
}

private fun measurementFromLabel(value: String): String? {
    val match = Regex("^AutoEq \\((.+) measurement\\)$", RegexOption.IGNORE_CASE).matchEntire(value)
        ?: return null
    return match.groupValues[1].trim().takeIf(String::isNotEmpty)
}

private fun humanizeTarget(value: String?): String? {
    val cleaned = value
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.replace('_', ' ')
        ?: return null
    return when (cleaned.lowercase(Locale.ROOT).replace(" ", "")) {
        "rtingscom" -> "RTINGS.com"
        "senselabaizu" -> "SenseLab Aizu"
        "hrtf5128diffusefield", "5128diffusefield" -> "B&K 5128 Diffuse Field (reference)"
        else -> cleaned.replace(Regex("\\s+"), " ")
    }
}

private fun humanizeSource(value: String?): String? = when (value?.trim()?.lowercase(Locale.ROOT)) {
    null, "" -> null
    "opra" -> "OPRA"
    "autoeq" -> "AutoEQ"
    else -> value.trim()
}

private fun looksLikeSoundSummary(value: String): Boolean {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return normalized.endsWith('.') && listOf(
        "adds ",
        "reduces ",
        "slightly adds ",
        "slightly reduces ",
        "noticeably adds ",
        "noticeably reduces ",
        "makes small ",
    ).any(normalized::startsWith)
}

private fun OpraEqProfile.soundImpactFromLegacyBands(): String? {
    val filters = bands.orEmpty().mapNotNull { band ->
        val frequency = band.frequency ?: return@mapNotNull null
        EqFilter(
            type = band.type.toEqFilterType(),
            frequencyHz = frequency,
            gainDb = band.gainDb,
            q = band.q,
            slope = band.slope,
        )
    }
    if (filters.isEmpty()) return null
    return SoundImpactSummary.fromFilters(filters)
}

private fun String?.toEqFilterType(): EqFilterType = when (this?.trim()?.lowercase(Locale.ROOT)) {
    "peak_dip", "peak", "pk", "peq" -> EqFilterType.PEAK
    "low_shelf", "ls", "lsc" -> EqFilterType.LOW_SHELF
    "high_shelf", "hs", "hsc" -> EqFilterType.HIGH_SHELF
    "low_pass", "lp" -> EqFilterType.LOW_PASS
    "high_pass", "hp" -> EqFilterType.HIGH_PASS
    else -> EqFilterType.OTHER
}
