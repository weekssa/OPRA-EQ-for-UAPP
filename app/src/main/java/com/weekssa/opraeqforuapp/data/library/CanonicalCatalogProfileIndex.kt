package com.weekssa.opraeqforuapp.data.library

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.PushbackInputStream
import java.nio.charset.StandardCharsets
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal data class JsonProfileLocation(
    val offset: Long,
    val length: Int,
)

/** Builds a compact on-disk lookup index without decoding the entire catalog a second time. */
internal class CanonicalCatalogProfileIndexReader(
    private val file: File,
    private val json: Json,
) {
    private var offset = 0L
    private lateinit var input: PushbackInputStream

    fun read(): Map<String, JsonProfileLocation> {
        if (!file.isFile) throw IOException("Canonical catalog cache is missing.")
        BufferedInputStream(file.inputStream(), BUFFER_SIZE).let { buffered ->
            input = PushbackInputStream(buffered, 1)
            input.use {
                if (nextToken() != OBJECT_START) throw IOException("Canonical catalog root is not an object.")
                while (true) {
                    val keyStart = nextToken()
                    when (keyStart) {
                        OBJECT_END -> return emptyMap()
                        COMMA -> continue
                        STRING_START -> Unit
                        else -> throw IOException("Canonical catalog root has an invalid property.")
                    }
                    val key = readStringValue()
                    if (nextToken() != COLON) throw IOException("Canonical catalog root is missing a colon.")
                    val valueStart = nextToken()
                    if (key == PROFILES_KEY) return readProfilesArray(valueStart)

                    skipValue(valueStart)
                    when (nextToken()) {
                        COMMA -> Unit
                        OBJECT_END -> return emptyMap()
                        else -> throw IOException("Canonical catalog root has an invalid separator.")
                    }
                }
            }
        }
    }

    private fun readProfilesArray(first: Int): Map<String, JsonProfileLocation> {
        if (first != ARRAY_START) throw IOException("Canonical catalog profiles are not an array.")
        val locations = HashMap<String, JsonProfileLocation>()
        while (true) {
            val valueStart = nextToken()
            when (valueStart) {
                ARRAY_END -> return locations
                COMMA -> continue
                else -> Unit
            }

            val valueOffset = offset - 1
            val profileId = if (valueStart == OBJECT_START) {
                readProfileObjectId()
            } else {
                skipValue(valueStart)
                null
            }
            val length = (offset - valueOffset).toInt()
            if (profileId != null && length > 0) {
                locations[profileId] = JsonProfileLocation(valueOffset, length)
            }

            when (nextToken()) {
                COMMA -> Unit
                ARRAY_END -> return locations
                else -> throw IOException("Canonical catalog profiles have an invalid separator.")
            }
        }
    }

    /** The source profile ID is decoded while every other field is skipped as a JSON value. */
    private fun readProfileObjectId(): String? {
        var profileId: String? = null
        while (true) {
            val keyStart = nextToken()
            when (keyStart) {
                OBJECT_END -> return profileId
                STRING_START -> Unit
                else -> throw IOException("Canonical profile has an invalid property.")
            }
            val key = readStringValue()
            if (nextToken() != COLON) throw IOException("Canonical profile is missing a colon.")
            val valueStart = nextToken()
            if (key == PROFILE_ID_KEY && valueStart == STRING_START) {
                profileId = readStringValue()
            } else {
                skipValue(valueStart)
            }

            when (nextToken()) {
                COMMA -> Unit
                OBJECT_END -> return profileId
                else -> throw IOException("Canonical profile has an invalid separator.")
            }
        }
    }

    private fun skipValue(first: Int) {
        when (first) {
            STRING_START -> skipStringValue()
            OBJECT_START -> skipObjectValue()
            ARRAY_START -> skipArrayValue()
            else -> skipPrimitiveValue()
        }
    }

    private fun skipObjectValue() {
        while (true) {
            val keyStart = nextToken()
            when (keyStart) {
                OBJECT_END -> return
                STRING_START -> skipStringValue()
                else -> throw IOException("JSON object has an invalid property.")
            }
            if (nextToken() != COLON) throw IOException("JSON object is missing a colon.")
            skipValue(nextToken())
            when (nextToken()) {
                COMMA -> Unit
                OBJECT_END -> return
                else -> throw IOException("JSON object has an invalid separator.")
            }
        }
    }

    private fun skipArrayValue() {
        while (true) {
            val valueStart = nextToken()
            when (valueStart) {
                ARRAY_END -> return
                COMMA -> Unit
                else -> {
                    skipValue(valueStart)
                    when (nextToken()) {
                        COMMA -> Unit
                        ARRAY_END -> return
                        else -> throw IOException("JSON array has an invalid separator.")
                    }
                }
            }
        }
    }

    private fun skipPrimitiveValue() {
        while (true) {
            val value = readByte()
            if (value < 0) return
            if (value <= SPACE || value == COMMA || value == ARRAY_END || value == OBJECT_END) {
                unreadByte(value)
                return
            }
        }
    }

    /** Called after the opening quote has already been consumed. */
    private fun readStringValue(): String {
        val encoded = ByteArrayOutputStream()
        encoded.write(STRING_START)
        var escaped = false
        while (true) {
            val value = readByte()
            if (value < 0) throw IOException("JSON string is incomplete.")
            encoded.write(value)
            when {
                escaped -> escaped = false
                value == BACKSLASH -> escaped = true
                value == STRING_START -> return json.decodeFromString(
                    String(encoded.toByteArray(), StandardCharsets.UTF_8),
                )
            }
        }
    }

    /** Called after the opening quote has already been consumed. */
    private fun skipStringValue() {
        var escaped = false
        while (true) {
            val value = readByte()
            if (value < 0) throw IOException("JSON string is incomplete.")
            when {
                escaped -> escaped = false
                value == BACKSLASH -> escaped = true
                value == STRING_START -> return
            }
        }
    }

    private fun nextToken(): Int {
        while (true) {
            val value = readByte()
            if (value < 0 || value > SPACE) return value
        }
    }

    private fun readByte(): Int {
        val value = input.read()
        if (value >= 0) offset += 1
        return value
    }

    private fun unreadByte(value: Int) {
        input.unread(value)
        offset -= 1
    }

    private companion object {
        const val BUFFER_SIZE = 32 * 1024
        const val BACKSLASH = '\\'.code
        const val COMMA = ','.code
        const val COLON = ':'.code
        const val OBJECT_START = '{'.code
        const val OBJECT_END = '}'.code
        const val ARRAY_START = '['.code
        const val ARRAY_END = ']'.code
        const val STRING_START = '"'.code
        const val SPACE = ' '.code
        const val PROFILES_KEY = "profiles"
        const val PROFILE_ID_KEY = "canonical_profile_id"
    }
}
