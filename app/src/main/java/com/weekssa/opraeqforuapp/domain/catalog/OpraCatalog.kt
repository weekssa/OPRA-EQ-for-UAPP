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
) {
    private val vendorById = vendors.associateBy(OpraVendor::id)
    private val productsByVendor = products.groupBy(OpraProduct::vendorId)
    private val profilesByProduct = profiles.groupBy(OpraEqProfile::productId)

    fun vendor(vendorId: String): OpraVendor? = vendorById[vendorId]

    fun product(productId: String): OpraProduct? = products.firstOrNull { it.id == productId }

    fun productsForVendor(vendorId: String): List<OpraProduct> =
        productsByVendor[vendorId]
            .orEmpty()
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    fun profilesForProduct(productId: String): List<OpraEqProfile> =
        profilesByProduct[productId]
            .orEmpty()
            .sortedWith(
                compareBy<OpraEqProfile> { it.author.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.details.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.id },
            )

    fun profileCount(productId: String): Int = profilesByProduct[productId]?.size ?: 0

    fun searchProducts(query: String): List<OpraProductSearchResult> {
        val tokens = query
            .lowercase(Locale.ROOT)
            .split(SEARCH_SEPARATOR)
            .map(::normalizeSearchText)
            .filter(String::isNotEmpty)

        if (tokens.isEmpty()) return emptyList()

        return products.mapNotNull { product ->
            val vendor = vendorById[product.vendorId] ?: return@mapNotNull null
            val haystack = normalizeSearchText("${vendor.name} ${product.name}")
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

    companion object {
        private val SEARCH_SEPARATOR = Regex("[^\\p{L}\\p{N}]+")

        private fun normalizeSearchText(value: String): String =
            value.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)
    }
}
