package com.weekssa.opraeqforuapp.domain.catalog

import java.util.Locale

data class OpraVendor(
    val id: String,
    val name: String,
)

data class OpraProduct(
    val id: String,
    val vendorId: String,
    val name: String,
    val type: String,
    val subtype: String,
    val aliases: List<String> = emptyList(),
)

data class OpraBand(
    val type: String?,
    val frequency: Double?,
    val gainDb: Double?,
    val q: Double?,
    val slope: Double?,
)

data class OpraEqProfile(
    val id: String,
    val productId: String,
    val author: String?,
    val details: String?,
    val link: String?,
    val profileType: String?,
    val preampGainDb: Double?,
    val bands: List<OpraBand>?,
    /**
     * Derived playback headroom supplied by EQ Library only when the source omitted preamp.
     * Source-authentic preampGainDb is never overwritten with this value.
     */
    val eqLibrarySafetyHeadroomDb: Double? = null,
    /**
     * Publication trust state. Legacy OPRA/v0.2 profiles default to verified so the absence of the
     * v0.3 field never downgrades existing catalog data. Unverified profiles remain manually
     * selectable/exportable but are excluded from silent automatic inclusion.
     */
    val isVerified: Boolean = true,
) {
    fun effectivePlaybackPreampDb(): Double? = preampGainDb ?: eqLibrarySafetyHeadroomDb

    fun usesEqLibrarySafetyHeadroom(): Boolean =
        preampGainDb == null && eqLibrarySafetyHeadroomDb != null
}

enum class GeneralEqCategory {
    SOUND,
    GENRE,
    UTILITY,
}

/**
 * User-facing projection of a canonical general preset. It deliberately has no headphone/product
 * identity, preventing Effect/Genre presets from being smuggled through the headphone hierarchy.
 */
data class GeneralEqPreset(
    val id: String,
    val displayName: String,
    val category: GeneralEqCategory,
    val creator: String?,
    val soundImpactSummary: String?,
    val sourceUrl: String?,
    val preampGainDb: Double?,
    val bands: List<OpraBand>,
    val eqLibrarySafetyHeadroomDb: Double? = null,
    val isVerified: Boolean = true,
    val isLatestRevision: Boolean = true,
)

data class OpraProductSearchResult(
    val vendor: OpraVendor,
    val product: OpraProduct,
    val profileCount: Int,
)

data class OpraCatalog(
    val vendors: List<OpraVendor>,
    val products: List<OpraProduct>,
    val profiles: List<OpraEqProfile>,
    val ignoredEntryCount: Int = 0,
    /**
     * Maps alternate/legacy product IDs to the single visible product ID.
     *
     * This lets catalog overlays consolidate duplicate model names from different sources without
     * breaking managed-headphone records that still reference an older product ID.
     */
    val productAliases: Map<String, String> = emptyMap(),
    val generalPresets: List<GeneralEqPreset> = emptyList(),
) {
    private val vendorById = vendors.associateBy(OpraVendor::id)
    private val productById = products.associateBy(OpraProduct::id)
    private val visibleProducts = products.filter { canonicalProductId(it.id) == it.id }
    private val productsByVendor = visibleProducts.groupBy(OpraProduct::vendorId)
    private val profilesByProduct = profiles.groupBy { canonicalProductId(it.productId) }

    fun vendor(vendorId: String): OpraVendor? = vendorById[vendorId]

    fun canonicalProductId(productId: String): String = resolveProductId(productId)

    fun product(productId: String): OpraProduct? = productById[canonicalProductId(productId)]

    fun productsForVendor(vendorId: String): List<OpraProduct> =
        productsByVendor[vendorId]
            .orEmpty()
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    fun profilesForProduct(productId: String): List<OpraEqProfile> =
        profilesByProduct[canonicalProductId(productId)]
            .orEmpty()
            .sortedWith(
                compareBy<OpraEqProfile> { it.author.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.details.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.id },
            )

    fun profileCount(productId: String): Int =
        profilesByProduct[canonicalProductId(productId)]?.size ?: 0

    fun searchProducts(query: String): List<OpraProductSearchResult> {
        val tokens = query
            .lowercase(Locale.ROOT)
            .split(SEARCH_SEPARATOR)
            .map(::normalizeSearchText)
            .filter(String::isNotEmpty)

        if (tokens.isEmpty()) return emptyList()

        return visibleProducts.mapNotNull { product ->
            val vendor = vendorById[product.vendorId] ?: return@mapNotNull null
            val haystack = normalizeSearchText(
                buildString {
                    append(vendor.name)
                    append(' ')
                    append(product.name)
                    product.aliases.forEach { alias ->
                        append(' ')
                        append(alias)
                    }
                },
            )
            if (tokens.all(haystack::contains)) {
                OpraProductSearchResult(
                    vendor = vendor,
                    product = product,
                    profileCount = profileCount(product.id),
                )
            } else {
                null
            }
        }.sortedWith(
            compareBy<OpraProductSearchResult> { it.product.name.lowercase(Locale.ROOT) }
                .thenBy { it.vendor.name.lowercase(Locale.ROOT) }
                .thenBy { it.product.id },
        )
    }

    fun searchGeneralPresets(query: String): List<GeneralEqPreset> {
        val tokens = query
            .lowercase(Locale.ROOT)
            .split(SEARCH_SEPARATOR)
            .map(::normalizeSearchText)
            .filter(String::isNotEmpty)
        if (tokens.isEmpty()) return generalPresets
        return generalPresets.filter { preset ->
            val haystack = normalizeSearchText(
                listOfNotNull(
                    preset.displayName,
                    preset.creator,
                    preset.soundImpactSummary,
                    preset.category.name,
                ).joinToString(" "),
            )
            tokens.all(haystack::contains)
        }
    }

    private fun resolveProductId(productId: String): String {
        var current = productId
        val visited = mutableSetOf<String>()
        while (visited.add(current)) {
            val next = productAliases[current] ?: return current
            if (next == current) return current
            current = next
        }
        return productId
    }

    companion object {
        private val SEARCH_SEPARATOR = Regex("[^\\p{L}\\p{N}]+")

        private fun normalizeSearchText(value: String): String =
            value.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)
    }
}
