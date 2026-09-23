package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.isUsableParametricSource
import com.weekssa.opraeqforuapp.domain.library.CanonicalEqSelection
import com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapter
import com.weekssa.opraeqforuapp.domain.library.LocalSavedEqAdapter
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import java.util.Locale

/** Canonical local snapshots take precedence; legacy rows remain readable without fabricated data. */
internal object SavedEqRecordMapper {
    fun toDomain(
        entity: SavedEqEntity,
        legacyCodec: ManagedProfileSnapshotCodec,
        canonicalCodec: SavedEqCanonicalSnapshotCodec,
        captureMetadataCodec: SavedEqCaptureMetadataCodec,
        selectionCodec: CanonicalEqSelectionCodec = CanonicalEqSelectionCodec(),
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
        val canonicalSelection = entity.canonicalSelectionJson?.let { encoded ->
            try {
                require(entity.kind == SavedEqRepository.KIND_FAVORITE) {
                    "Canonical catalog selections are valid only for Favorites"
                }
                require(canonical == null) { "Favorite row cannot also contain a local Personal EQ snapshot" }
                selectionCodec.decode(encoded).also { selection ->
                    require(selection.profile.isHeadphoneProfile) {
                        "Favorite must retain a canonical headphone profile"
                    }
                    require(selection.profile.headphone?.manufacturer == entity.manufacturer) {
                        "Favorite manufacturer no longer matches its canonical profile"
                    }
                    require(CanonicalLegacyCatalogAdapter.displayProductName(requireNotNull(selection.profile.headphone)) == entity.model) {
                        "Favorite model no longer matches its canonical profile"
                    }
                    val projected = CanonicalLegacyCatalogAdapter.projectSelection(selection, entity.productId)
                    require(projected.id == entity.sourceProfileId) {
                        "Favorite ID no longer matches its canonical revision"
                    }
                }
            } catch (_: IllegalArgumentException) {
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
        val projectedSelection = canonicalSelection?.let { selection ->
            runCatching {
                CanonicalLegacyCatalogAdapter.projectSelection(selection, entity.productId)
            }.getOrElse {
                savedEqDataInvalid = true
                null
            }
        }
        val profile = projectedSelection ?: projectedCanonical ?: try {
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
        if (profile.productId != entity.productId) savedEqDataInvalid = true
        if (
            entity.kind == SavedEqRepository.KIND_FAVORITE &&
            entity.sourceProfileId != null &&
            profile.id != entity.sourceProfileId
        ) {
            savedEqDataInvalid = true
        }
        if (!profile.hasValidSavedEqStructure()) savedEqDataInvalid = true
        val kind = when (entity.kind) {
            SavedEqRepository.KIND_FAVORITE -> SavedEqKind.Favorite
            SavedEqRepository.KIND_PERSONAL -> SavedEqKind.Personal
            else -> {
                savedEqDataInvalid = true
                SavedEqKind.Unreadable
            }
        }

        return SavedEqRecord(
            entryId = entity.entryId,
            kind = kind,
            sourceProfileId = entity.sourceProfileId,
            productId = entity.productId,
            manufacturer = canonical?.let { it.headphone?.manufacturer.orEmpty() }
                ?: canonicalSelection?.profile?.headphone?.manufacturer
                ?: entity.manufacturer,
            model = canonical?.let { it.headphone?.model.orEmpty() }
                ?: canonicalSelection?.profile?.headphone?.let(CanonicalLegacyCatalogAdapter::displayProductName)
                ?: entity.model,
            displayName = canonical?.displayName ?: entity.displayName,
            profile = profile,
            createdAtMillis = entity.createdAtMillis,
            updatedAtMillis = entity.updatedAtMillis,
            captureMetadata = captureMetadata,
            canonicalSnapshot = canonical,
            savedEqDataInvalid = savedEqDataInvalid,
            canonicalSelection = canonicalSelection,
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

    private fun OpraEqProfile.hasValidSavedEqStructure(): Boolean {
        if (id.isBlank() || productId.isBlank()) return false
        if (preampGainDb?.isFinite() == false) return false
        if (eqLibrarySafetyHeadroomDb?.isFinite() == false) return false
        val type = profileType?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty) ?: return false
        if (bands.orEmpty().any { band ->
                listOf(band.frequency, band.gainDb, band.q, band.slope)
                    .any { value -> value?.isFinite() == false }
            }
        ) {
            return false
        }
        // Non-parametric source profiles remain intact and are classified by each target adapter;
        // they are not corrupt merely because this app cannot currently transform them.
        return type != "parametric_eq" || isUsableParametricSource()
    }
}
