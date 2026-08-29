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
 * Product identity is resolved conservatively: normalized manufacturer/model equality is accepted,
 * and any broader equivalence must be supplied explicitly as a model alias by a qualified source.
 * Alternate product IDs remain resolvable so consolidating duplicate names does not break existing
 * managed-headphone state.
 *
 * The compatibility catalog can also contain multiple records for the exact same acoustic tuning.
 * Those records remain source data, but the app should not make a person choose between acoustically
 * identical rows. The rendered overlay therefore keeps one preferred visible record per exact
 * normalized filter set and strips machine-only labels from the compatibility details.
 */
fun overlayCanonicalCatalog(
    legacy: OpraCatalog,
    canonical: OpraCatalog,
): OpraCatalog {
    val vendors = linkedMapOf<String, OpraVendor>()
    legacy.vendors.forEach { vendors[it.id] = it }
    canonical.vendors.forEach { vendors[it.id] = it }

    val rawProducts = linkedMapOf<String, OpraProduct>()
    legacy.products.forEach { rawProducts[it.id] = it }
    canonical.products.forEach { rawProducts[it.id] = it }

    val aliasResolution = resolveProductAliases(legacy, canonical, vendors)
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
): ProductAliasResolution {
    val aliases = linkedMapOf<String, String>()
    val preferred = linkedMapOf<String, OpraProduct>()

    canonical.products.forEach { canonicalProduct ->
        val canonicalVendor = vendors[canonicalProduct.vendorId]?.name ?: return@forEach
        val canonicalVendorKey = normalizeIdentityText(canonicalVendor)
        val canonicalNameKey = normalizeIdentityText(canonicalProduct.name)
        val acceptedNameKeys = buildSet {
            add(canonicalNameKey)
            canonicalProduct.aliases.forEach { add(normalizeIdentityText(it)) }
        }.filter(String::isNotEmpty).toSet()
        if (acceptedNameKeys.isEmpty()) return@forEach

        val matchingLegacy = legacy.products.filter { legacyProduct ->
            val legacyVendor = vendors[legacyProduct.vendorId]?.name ?: return@filter false
            normalizeIdentityText(legacyVendor) == canonicalVendorKey &&
                normalizeIdentityText(legacyProduct.name) in acceptedNameKeys
        }
        if (matchingLegacy.isEmpty()) return@forEach

        // Prefer an existing product whose normalized display name exactly matches the canonical
        // model. This preserves a stable legacy ID whenever the catalog already has the best name.
        val retained = matchingLegacy.firstOrNull {
            normalizeIdentityText(it.name) == canonicalNameKey
        } ?: matchingLegacy.first()
        val retainedId = retained.id

        matchingLegacy.forEach { product ->
            if (product.id != retainedId) aliases[product.id] = retainedId
        }
        if (canonicalProduct.id != retainedId) aliases[canonicalProduct.id] = retainedId

        preferred[retainedId] = retained.copy(
            name = canonicalProduct.name,
            aliases = (
                canonicalProduct.aliases +
                    matchingLegacy.map(OpraProduct::name) +
                    matchingLegacy.flatMap(OpraProduct::aliases)
                )
                .filterNot { normalizeIdentityText(it) == canonicalNameKey }
                .distinctBy(::normalizeIdentityText),
        )
    }

    return ProductAliasResolution(aliases = aliases, preferredProducts = preferred)
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
