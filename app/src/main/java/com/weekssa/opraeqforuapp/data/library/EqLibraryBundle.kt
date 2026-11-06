package com.weekssa.opraeqforuapp.data.library

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Portable copy of the parts of My EQs that belong to the owner rather than the catalog:
 * saved EQs (Favorites and Personal imports) and saved General EQs.
 *
 * The rows carry the stored representations verbatim, exactly as the source app wrote them, so a
 * restore re-creates the same records instead of a re-derived approximation. Nothing here is
 * hardware identity: the EW300 target is a device, not a field on a saved EQ.
 */
@Serializable
data class EqLibraryBundle(
    val format: String = FORMAT_ID,
    val version: Int = FORMAT_VERSION,
    val exportedAtMillis: Long = 0L,
    val sourcePackage: String = "",
    val sourceVersionName: String = "",
    val sourceVersionCode: Int = 0,
    val savedEqs: List<SavedEqTransfer> = emptyList(),
    val generalEqs: List<GeneralEqTransfer> = emptyList(),
    /** Rows the exporter could not carry, with the reason, so the count is never silent. */
    val skipped: List<SkippedTransfer> = emptyList(),
) {
    val isEmpty: Boolean
        get() = savedEqs.isEmpty() && generalEqs.isEmpty()

    companion object {
        const val FORMAT_ID = "eq-library-bundle"
        const val FORMAT_VERSION = 1
    }
}

/** One My EQs row. Favorite keeps its canonical selection; Personal keeps the imported curve. */
@Serializable
data class SavedEqTransfer(
    val entryId: String,
    val kind: String,
    val sourceProfileId: String? = null,
    val productId: String,
    val manufacturer: String,
    val model: String,
    val displayName: String,
    val profileJson: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val captureMetadataJson: String? = null,
    val canonicalSnapshotJson: String? = null,
    val canonicalSelectionJson: String? = null,
)

/** One saved General EQ row. */
@Serializable
data class GeneralEqTransfer(
    val presetId: String,
    val displayName: String,
    val category: String,
    val profileJson: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val canonicalSelectionJson: String? = null,
)

@Serializable
data class SkippedTransfer(
    val displayName: String,
    val reason: String,
)

/**
 * JSON codec for [EqLibraryBundle]. Unknown keys are ignored so a newer writer cannot break an
 * older reader; a different format or a newer major version is rejected with a readable message
 * instead of a partial import.
 */
class EqLibraryBundleCodec(
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    },
) {
    fun encode(bundle: EqLibraryBundle): String = json.encodeToString(bundle)

    fun decode(text: String): EqLibraryBundle {
        val bundle = try {
            json.decodeFromString<EqLibraryBundle>(text)
        } catch (error: Exception) {
            throw IllegalArgumentException("This file is not an EQ Library backup.", error)
        }
        require(bundle.format == EqLibraryBundle.FORMAT_ID) {
            "This file is not an EQ Library backup."
        }
        require(bundle.version <= EqLibraryBundle.FORMAT_VERSION) {
            "This backup was written by a newer EQ Library and cannot be read here."
        }
        return bundle
    }
}
