package com.weekssa.opraeqforuapp.data.library

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import kotlin.io.path.createTempDirectory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalCatalogProfileIndexTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun readsMultipleProfileSpansWithEscapedKeysAndNestedUnknownValues() {
        val root = createTempDirectory(prefix = "canonical-profile-index-").toFile()
        try {
            val catalog = File(root, "catalog.json")
            catalog.writeText(
                """{"metadata":{"values":["brace } comma ,",{"escaped":"quote: \" slash: \\"}]},""" +
                    """"pro\u0066iles":[""" +
                    """{"canonical_profile_id":"first\/profile","other":{"items":[true,null,{"text":"end ] , }"}]}},""" +
                    """{"canonical_profile_id":"second\u002Fprofile","other":[1,2,3]}]}""",
            )

            val locations = CanonicalCatalogProfileIndexReader(catalog, json).read()

            assertEquals(setOf("first/profile", "second/profile"), locations.keys)
            locations.forEach { (expectedId, location) ->
                val profileBytes = ByteArray(location.length)
                RandomAccessFile(catalog, "r").use { file ->
                    file.seek(location.offset)
                    file.readFully(profileBytes)
                }
                val profile = json.parseToJsonElement(
                    String(profileBytes, StandardCharsets.UTF_8),
                ).jsonObject
                assertEquals(expectedId, profile.getValue("canonical_profile_id").jsonPrimitive.content)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsMalformedProfileObjectsInsteadOfReturningPartialIndex() {
        val root = createTempDirectory(prefix = "canonical-profile-index-invalid-").toFile()
        try {
            val catalog = File(root, "catalog.json")
            catalog.writeText("""{"profiles":[{"canonical_profile_id" "broken"}]}""")

            val failure = runCatching { CanonicalCatalogProfileIndexReader(catalog, json).read() }
                .exceptionOrNull()

            assertTrue(failure is IOException)
        } finally {
            root.deleteRecursively()
        }
    }
}
