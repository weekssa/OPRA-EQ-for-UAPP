package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.domain.library.CanonicalEqSelection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Stores full catalog provenance/revisions together with the exact revision saved to My EQs. */
class CanonicalEqSelectionCodec(
    private val json: Json = Json {
        encodeDefaults = true
        explicitNulls = true
    },
) {
    fun encode(selection: CanonicalEqSelection): String = json.encodeToString(selection)

    fun decode(encoded: String): CanonicalEqSelection = json.decodeFromString(encoded)
}
