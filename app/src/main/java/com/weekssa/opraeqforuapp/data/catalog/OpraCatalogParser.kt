package com.weekssa.opraeqforuapp.data.catalog

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import java.io.BufferedReader
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

class CatalogParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

class OpraCatalogParser(
    private val json: Json = Json,
) {
    fun parse(file: File): OpraCatalog = file.bufferedReader(Charsets.UTF_8).use(::parse)

    fun parse(reader: BufferedReader): OpraCatalog {
        val vendors = linkedMapOf<String, OpraVendor>()
        val products = linkedMapOf<String, OpraProduct>()
        val profiles = linkedMapOf<String, OpraEqProfile>()
        var ignoredEntries = 0
        var lineNumber = 0
        var parsedEntries = 0

        while (true) {
            val line = reader.readLine() ?: break
            lineNumber += 1
            if (line.isBlank()) continue
            parsedEntries += 1
            if (parsedEntries > MAX_ENTRIES) {
                throw CatalogParseException("OPRA catalog exceeds the supported $MAX_ENTRIES-entry safety limit.")
            }

            try {
                val root = json.parseToJsonElement(line) as? JsonObject
                    ?: throw CatalogParseException("Line $lineNumber is not a JSON object.")
                val entryType = root.string("type")
                    ?: throw CatalogParseException("Line $lineNumber is missing an entry type.")
                val id = root.string("id")?.takeIf(String::isNotBlank)
                    ?: throw CatalogParseException("Line $lineNumber is missing a valid entry id.")
                val data = root["data"] as? JsonObject
                    ?: throw CatalogParseException("Line $lineNumber is missing its data object.")

                when (entryType) {
                    "vendor" -> {
                        ensureUnique(vendors, id, entryType, lineNumber)
                        val name = data.string("name")?.takeIf(String::isNotBlank)
                            ?: throw CatalogParseException("Vendor $id is missing its display name.")
                        vendors[id] = OpraVendor(id = id, name = name)
                    }
                    "product" -> {
                        ensureUnique(products, id, entryType, lineNumber)
                        val vendorId = data.string("vendor_id")?.takeIf(String::isNotBlank)
                            ?: throw CatalogParseException("Product $id is missing vendor_id.")
                        val name = data.string("name")?.takeIf(String::isNotBlank)
                            ?: throw CatalogParseException("Product $id is missing its display name.")
                        val type = data.string("type")?.takeIf(String::isNotBlank)
                            ?: throw CatalogParseException("Product $id is missing its type.")
                        if (type != "headphones") {
                            throw CatalogParseException("Product $id has unsupported product type $type.")
                        }
                        val subtype = data.string("subtype")?.takeIf(String::isNotBlank)
                            ?: throw CatalogParseException("Product $id is missing its subtype.")
                        products[id] = OpraProduct(
                            id = id,
                            vendorId = vendorId,
                            name = name,
                            type = type,
                            subtype = subtype,
                        )
                    }
                    "eq" -> {
                        ensureUnique(profiles, id, entryType, lineNumber)
                        val productId = data.string("product_id")?.takeIf(String::isNotBlank)
                            ?: throw CatalogParseException("EQ $id is missing product_id.")
                        profiles[id] = parseProfile(id, productId, data)
                    }
                    else -> ignoredEntries += 1
                }
            } catch (exception: CatalogParseException) {
                throw exception
            } catch (exception: Exception) {
                throw CatalogParseException("Could not parse OPRA catalog line $lineNumber.", exception)
            }
        }

        if (vendors.isEmpty() || products.isEmpty() || profiles.isEmpty()) {
            throw CatalogParseException("OPRA catalog is missing vendors, products, or EQ profiles.")
        }

        products.values.forEach { product ->
            if (product.vendorId !in vendors) {
                throw CatalogParseException("Product ${product.id} references missing vendor ${product.vendorId}.")
            }
        }
        profiles.values.forEach { profile ->
            if (profile.productId !in products) {
                throw CatalogParseException("EQ ${profile.id} references missing product ${profile.productId}.")
            }
        }

        return OpraCatalog(
            vendors = vendors.values.toList(),
            products = products.values.toList(),
            profiles = profiles.values.toList(),
            ignoredEntryCount = ignoredEntries,
        )
    }

    private fun parseProfile(id: String, productId: String, data: JsonObject): OpraEqProfile {
        val parameters = data["parameters"] as? JsonObject
        val bands = when (val bandsElement = parameters?.get("bands")) {
            is JsonArray -> bandsElement.map { bandElement ->
                val band = bandElement as? JsonObject
                if (band == null) {
                    OpraBand(type = null, frequency = null, gainDb = null, q = null, slope = null)
                } else {
                    OpraBand(
                        type = band.string("type"),
                        frequency = band.number("frequency"),
                        gainDb = band.number("gain_db"),
                        q = band.number("q"),
                        slope = band.number("slope"),
                    )
                }
            }
            else -> null
        }

        return OpraEqProfile(
            id = id,
            productId = productId,
            author = data.string("author")?.takeIf(String::isNotBlank),
            details = data.string("details")?.takeIf(String::isNotBlank),
            link = data.string("link")?.takeIf(String::isNotBlank),
            profileType = data.string("type"),
            preampGainDb = parameters?.number("gain_db"),
            bands = bands,
        )
    }

    private fun <T> ensureUnique(
        entries: Map<String, T>,
        id: String,
        type: String,
        lineNumber: Int,
    ) {
        if (id in entries) {
            throw CatalogParseException("Duplicate $type id $id on line $lineNumber.")
        }
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull

    private fun JsonObject.number(name: String): Double? =
        (this[name] as? JsonPrimitive)?.takeUnless(JsonPrimitive::isString)?.doubleOrNull

    companion object {
        private const val MAX_ENTRIES = 100_000
    }
}
