package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
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
        val canonical = entity.canonicalSnapshotJson?.let { encoded ->
            require(entity.kind == SavedEqRepository.KIND_PERSONAL) {
                "Canonical local EQ data is valid only for Personal EQ records"
            }
            canonicalCodec.decode(encoded)
        }
        val profile = canonical?.let { LocalSavedEqAdapter.projectToLegacy(it, entity.productId) }
            ?: legacyCodec.decode(entity.profileJson)

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
            captureMetadata = entity.captureMetadataJson?.let(captureMetadataCodec::decode),
            canonicalSnapshot = canonical,
        )
    }
}
