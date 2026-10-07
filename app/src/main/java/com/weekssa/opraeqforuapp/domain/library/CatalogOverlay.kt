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
 * Product identity is resolved conservatively. Exact normalized manufacturer/model/subtype matches
 * are always safe to collapse, including redundant manufacturer text in a model label. Broader
 * equivalence must come from an explicit catalog/source alias. Alternate product IDs remain
 * resolvable so cleanup does not break existing managed state.
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
    val canonicalProfileIds = if (legacy.profiles.isEmpty()) {
        emptySet()
    } else {
        canonical.profiles.mapTo(HashSet(canonical.profiles.size), OpraEqProfile::id)
    }
    legacy.profiles.forEach { profile ->
        profiles[profile.id] = if (profile.id in canonicalProfileIds) {
            profile
        } else {
            profile.toUserFacingProfile(defaultSource = "OPRA")
        }
    }
    canonical.profiles.forEach { profiles[it.id] = it }
    profiles.replaceAll { _, profile ->
        val productId = resolveProductId(profile.productId, aliasResolution.aliases)
        if (productId == profile.productId) profile else profile.copy(productId = productId)
    }
    val deduplicatedProfiles = deduplicateAcoustically(profiles)

    return OpraCatalog(
        vendors = vendors.values.filter { it.id in visibleVendorIds },
        products = visibleProducts,
        profiles = deduplicatedProfiles,
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
    val sourceProductByRoot = linkedMapOf<String, OpraProduct>()
    val legacyIds = legacy.products.mapTo(mutableSetOf(), OpraProduct::id)

    fun vendorName(product: OpraProduct): String? = vendors[product.vendorId]?.name

    val productsByVendorModel = linkedMapOf<Pair<String, String>, MutableList<OpraProduct>>()
    fun indexProduct(product: OpraProduct) {
        val manufacturer = vendorName(product).orEmpty()
        val vendorKey = normalizeIdentityText(manufacturer)
        val modelKey = normalizeIdentityModel(product.name, manufacturer)
        if (vendorKey.isNotEmpty() && modelKey.isNotEmpty()) {
            productsByVendorModel.getOrPut(vendorKey to modelKey) { mutableListOf() }.add(product)
        }
    }
    legacy.products.forEach(::indexProduct)
    canonical.products.forEach { if (it.id !in legacyIds) indexProduct(it) }

    // Avoid retaining a second full product-ID map during startup.

    fun applyIdentityGroup(
        manufacturer: String,
        canonicalModel: String,
        modelAliases: Collection<String>,
        requiredSubtype: String? = null,
    ) {
        val manufacturerKey = normalizeIdentityText(manufacturer)
        val canonicalKey = normalizeIdentityModel(canonicalModel, manufacturer)
        if (manufacturerKey.isEmpty() || canonicalKey.isEmpty()) return
        val acceptedKeys = (modelAliases + canonicalModel)
            .map { normalizeIdentityModel(it, manufacturer) }
            .filter(String::isNotEmpty)
            .toSet()
        val requiredSubtypeKey = requiredSubtype?.let(::normalizeIdentityText)
        val matches = acceptedKeys.asSequence()
            .flatMap { modelKey -> productsByVendorModel[manufacturerKey to modelKey].orEmpty().asSequence() }
            .filter { product -> requiredSubtypeKey == null || normalizeIdentityText(product.subtype) == requiredSubtypeKey }
            .distinctBy(OpraProduct::id)
            .toList()
        if (matches.isEmpty()) return

        val retained = matches.firstOrNull {
            it.id in legacyIds && normalizeIdentityModel(it.name, manufacturer) == canonicalKey
        } ?: matches.firstOrNull { it.id in legacyIds }
            ?: matches.firstOrNull { normalizeIdentityModel(it.name, manufacturer) == canonicalKey }
            ?: matches.first()
        val retainedRoot = resolveProductId(retained.id, aliases)

        matches.forEach { product ->
            val root = resolveProductId(product.id, aliases)
            if (root != retainedRoot) aliases[root] = retainedRoot
            if (product.id != retainedRoot) aliases[product.id] = retainedRoot
        }

        val retainedProduct = sourceProductByRoot[retainedRoot] ?: retained
        sourceProductByRoot.putIfAbsent(retainedRoot, retainedProduct)
        preferred[retainedRoot] = retainedProduct.copy(
            name = canonicalModel,
            aliases = (
                modelAliases +
                    matches.map(OpraProduct::name) +
                    matches.flatMap(OpraProduct::aliases)
                )
                .filterNot { normalizeIdentityModel(it, manufacturer) == canonicalKey }
                .distinctBy(::normalizeIdentityText),
        )
    }

    // Global safe cleanup: punctuation/casing/spacing variants, plus a redundant manufacturer token,
    // collapse even when no canonical source touches that headphone. Subtype remains part of the key.
    productsByVendorModel.values.forEach { vendorModelGroup ->
        if (vendorModelGroup.size > 1) {
            vendorModelGroup.groupBy { normalizeIdentityText(it.subtype) }.values.forEach { group ->
                if (group.size > 1 && group.first().name.isNotBlank()) {
                    val representative = group.firstOrNull { it.id in legacyIds } ?: group.first()
                    applyIdentityGroup(
                        manufacturer = vendorName(representative).orEmpty(),
                        canonicalModel = representative.name,
                        modelAliases = emptyList(),
                        requiredSubtype = representative.subtype,
                    )
                }
            }
        }
    }

    // Canonical profiles can carry qualified aliases from their source manifest.
    canonical.products.forEach { canonicalProduct ->
        val manufacturer = vendorName(canonicalProduct) ?: return@forEach
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
        val rootProduct = sourceProductByRoot[root] ?: product
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

private fun normalizeIdentityModel(value: String, manufacturer: String): String {
    val modelKey = normalizeIdentityText(value)
    val manufacturerKey = normalizeIdentityText(manufacturer)
    if (modelKey.isEmpty() || manufacturerKey.isEmpty()) return modelKey
    return when {
        modelKey.startsWith(manufacturerKey) && modelKey.length >= manufacturerKey.length + 3 ->
            modelKey.removePrefix(manufacturerKey)
        modelKey.endsWith(manufacturerKey) && modelKey.length >= manufacturerKey.length + 3 ->
            modelKey.removeSuffix(manufacturerKey)
        else -> modelKey
    }
}

private fun deduplicateAcoustically(
    profiles: LinkedHashMap<String, OpraEqProfile>,
): List<OpraEqProfile> {
    val retainedIds = HashMap<AcousticFingerprintKey, String>()
    val fingerprintCollisions = linkedMapOf<AcousticFingerprintKey, MutableList<OpraEqProfile>>()
    val discardedProfileIds = HashSet<String>()
    profiles.forEach { (profileId, profile) ->
        val fingerprint = profile.legacyAcousticFingerprintOrNull() ?: return@forEach
        val key = AcousticFingerprintKey(profile.productId, fingerprint)
        val retainedId = retainedIds[key]
        if (retainedId == null) {
            retainedIds[key] = profileId
            return@forEach
        }
        val signature = profile.legacyAcousticSignature() ?: return@forEach
        val previous = profiles.getValue(retainedId)

        if (previous.legacyAcousticSignature() == signature) {
            if (profile.preferenceScore() > previous.preferenceScore()) profiles[retainedId] = profile
            discardedProfileIds += profileId
            return@forEach
        }

        val collisions = fingerprintCollisions.getOrPut(key) { mutableListOf() }
        val collisionIndex = collisions.indexOfFirst { it.legacyAcousticSignature() == signature }
        if (collisionIndex < 0) {
            collisions += profile
        } else if (profile.preferenceScore() > collisions[collisionIndex].preferenceScore()) {
            collisions[collisionIndex] = profile
        }
        discardedProfileIds += profileId
    }

    // Profile IDs are unique in this ordered map. Removing duplicate and fingerprint-collision
    // rows here preserves the first-occurrence order without holding a second map of every profile.
    discardedProfileIds.forEach(profiles::remove)
    retainedIds.clear()
    val collisionCount = fingerprintCollisions.values.sumOf { it.size }
    val deduplicatedProfiles = ArrayList<OpraEqProfile>(profiles.size + collisionCount)
    deduplicatedProfiles.addAll(profiles.values)
    fingerprintCollisions.values.forEach(deduplicatedProfiles::addAll)
    profiles.clear()
    fingerprintCollisions.clear()
    return deduplicatedProfiles
}

private data class AcousticFingerprintKey(val productId: String, val fingerprint: Long)

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
    val generatedSoundSummary = existingSoundSummary
        ?: soundImpactFromLegacyBands()
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

    val normalizedDetails = compactDetails.takeIf(String::isNotBlank)
    return if (normalizedDetails == details) this else copy(details = normalizedDetails)
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

internal fun humanizeCanonicalTarget(value: String?): String? = humanizeTarget(value)

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
            sourceType = band.type
                ?.takeIf { it.toEqFilterType() == EqFilterType.OTHER }
                ?.trim(),
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
