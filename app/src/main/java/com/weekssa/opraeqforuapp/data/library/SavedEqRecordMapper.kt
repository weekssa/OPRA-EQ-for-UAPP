package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.library.LocalSavedEqAdapter
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord

/** Canonical local snapshots take precedence; legacy rows remain readable without fabricated data. */
internal object SavedEqRecordMapper {
    fun toDomain(
        entity: SavedEqEntity,
        legacyCodec: ManagedProfileSnapshotCodec,
        canonicalCodec: SavedEqCanonicalSnapshotCodec,
        captureMetadataCodec: SavedEqCaptureMetadataCodec,
    ): SavedEqRecord {
        var savedEqDataInvalid = false
        val canonical = entity.canonicalSnapshotJson?.let { encoded ->
            try {
                require(entity.kind == SavedEqRepository.KIND_PERSONAL) {
                    "Canonical local EQ data is valid only for Personal EQ records"
                }
                canonicalCodec.decode(encoded)
            } catch (_: IllegalArgumentException) {
                // Keep the row visible using its legacy projection, but mark it unusable for actions.
                savedEqDataInvalid = true
                null
            }
        }
        val projectedCanonical = canonical?.let { snapshot ->
            try {
                LocalSavedEqAdapter.projectToLegacy(snapshot, entity.productId)
            } catch (_: IllegalArgumentException) {
                savedEqDataInvalid = true
                null
            }
        }
        val profile = projectedCanonical ?: try {
            legacyCodec.decode(entity.profileJson)
        } catch (_: IllegalArgumentException) {
            savedEqDataInvalid = true
            displayOnlyProfile(entity)
        }
        val captureMetadata = entity.captureMetadataJson?.let { encoded ->
            try {
                captureMetadataCodec.decode(encoded)
            } catch (_: IllegalArgumentException) {
                savedEqDataInvalid = true
                null
            }
        }

        return SavedEqRecord(
            entryId = entity.entryId,
            kind = when (entity.kind) {
                SavedEqRepository.KIND_FAVORITE -> SavedEqKind.Favorite
                SavedEqRepository.KIND_PERSONAL -> SavedEqKind.Personal
                else -> error("Unknown saved EQ kind ${entity.kind}")
            },
            sourceProfileId = entity.sourceProfileId,
            productId = entity.productId,
            manufacturer = canonical?.let { it.headphone?.manufacturer.orEmpty() } ?: entity.manufacturer,
            model = canonical?.let { it.headphone?.model.orEmpty() } ?: entity.model,
            displayName = canonical?.displayName ?: entity.displayName,
            profile = profile,
            createdAtMillis = entity.createdAtMillis,
            updatedAtMillis = entity.updatedAtMillis,
            captureMetadata = captureMetadata,
            canonicalSnapshot = canonical,
            savedEqDataInvalid = savedEqDataInvalid,
        )
    }

    private fun displayOnlyProfile(entity: SavedEqEntity) = OpraEqProfile(
        id = entity.sourceProfileId ?: entity.entryId,
        productId = entity.productId,
        author = null,
        details = null,
        link = null,
        profileType = null,
        preampGainDb = null,
        bands = emptyList(),
        isVerified = false,
    )
}
