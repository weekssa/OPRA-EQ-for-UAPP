package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.domain.library.LocalSavedEqSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Persists the complete source-neutral local EQ model with explicit defaults and nulls. */
class SavedEqCanonicalSnapshotCodec(
    private val json: Json = Json {
        encodeDefaults = true
        explicitNulls = true
    },
) {
    fun encode(snapshot: LocalSavedEqSnapshot): String = json.encodeToString(snapshot)

    fun decode(encoded: String): LocalSavedEqSnapshot = json.decodeFromString(encoded)
}
