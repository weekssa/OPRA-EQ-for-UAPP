package com.weekssa.opraeqforuapp.domain.catalog

import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

/** Source-order authority retained only when an adapter verifies the source provenance. */
@Serializable
enum class EqBandOrderProvenance {
    /** OPRA catalog order is the documented priority order for constrained UAPP exports. */
    @SerialName("opra_source_priority")
    OPRA_SOURCE_PRIORITY,
}

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
    /** Stable canonical tuning lineage identity used by local Hide/Unhide. */
    val canonicalProfileId: String = id,
    val isVerified: Boolean = true,
    /** Null unless a trusted source adapter verified the band ordering provenance. */
    val bandOrderProvenance: EqBandOrderProvenance? = null,
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
    /** Stable canonical tuning lineage identity used by local Hide/Unhide. */
    val canonicalProfileId: String = id,
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
    private val lookupIndexes by lazy {
        val productById = products.associateBy(OpraProduct::id)
        val visibleProducts = products.filter { canonicalProductId(it.id) == it.id }
        CatalogLookupIndexes(
            vendorById = vendors.associateBy(OpraVendor::id),
            productById = productById,
            visibleProducts = visibleProducts,
            productsByVendor = visibleProducts.groupBy(OpraProduct::vendorId),
            profilesByProduct = profiles.groupBy { canonicalProductId(it.productId) },
        )
    }

    fun vendor(vendorId: String): OpraVendor? = lookupIndexes.vendorById[vendorId]

    fun canonicalProductId(productId: String): String = resolveProductId(productId)

    fun product(productId: String): OpraProduct? = lookupIndexes.productById[canonicalProductId(productId)]

    fun productsForVendor(vendorId: String): List<OpraProduct> =
        lookupIndexes.productsByVendor[vendorId]
            .orEmpty()
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    fun profilesForProduct(productId: String): List<OpraEqProfile> =
        lookupIndexes.profilesByProduct[canonicalProductId(productId)]
            .orEmpty()
            .sortedWith(
                compareBy<OpraEqProfile> { it.author.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.details.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.id },
            )

    fun profileCount(productId: String): Int =
        lookupIndexes.profilesByProduct[canonicalProductId(productId)]?.size ?: 0

    fun searchProducts(query: String): List<OpraProductSearchResult> {
        val tokens = query
            .lowercase(Locale.ROOT)
            .split(SEARCH_SEPARATOR)
            .map(::normalizeSearchText)
            .filter(String::isNotEmpty)

        if (tokens.isEmpty()) return emptyList()

        return lookupIndexes.visibleProducts.mapNotNull { product ->
            val vendor = lookupIndexes.vendorById[product.vendorId] ?: return@mapNotNull null
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

    private data class CatalogLookupIndexes(
        val vendorById: Map<String, OpraVendor>,
        val productById: Map<String, OpraProduct>,
        val visibleProducts: List<OpraProduct>,
        val productsByVendor: Map<String, List<OpraProduct>>,
        val profilesByProduct: Map<String, List<OpraEqProfile>>,
    )

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

    /**
     * Returns the ordinary browse/search projection after applying the user's global local Hide
     * preferences. Canonical data is never removed; callers that manage My EQs keep the original
     * unfiltered catalog. Hiding one lineage hides all of its genuine revisions.
     */
    fun excludingHiddenCanonicalProfiles(hiddenCanonicalProfileIds: Set<String>): OpraCatalog {
        if (hiddenCanonicalProfileIds.isEmpty()) return this

        val visibleProfiles = profiles.filterNot { it.canonicalProfileId in hiddenCanonicalProfileIds }
        val visibleGeneralPresets = generalPresets.filterNot {
            it.canonicalProfileId in hiddenCanonicalProfileIds
        }
        val visibleProductIds = visibleProfiles
            .mapTo(mutableSetOf()) { canonicalProductId(it.productId) }
        val visibleProducts = products.filter { canonicalProductId(it.id) in visibleProductIds }
        val visibleVendorIds = visibleProducts.mapTo(mutableSetOf(), OpraProduct::vendorId)

        return copy(
            vendors = vendors.filter { it.id in visibleVendorIds },
            products = visibleProducts,
            profiles = visibleProfiles,
            generalPresets = visibleGeneralPresets,
        )
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
