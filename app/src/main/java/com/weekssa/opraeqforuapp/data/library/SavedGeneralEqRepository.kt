package com.weekssa.opraeqforuapp.data.library

import androidx.room.withTransaction
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqCategory
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConverter
import com.weekssa.opraeqforuapp.domain.library.CanonicalEqSelection
import com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapter
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SavedGeneralEqRepository(
    private val database: OpraEqDatabase,
    private val snapshotCodec: ManagedProfileSnapshotCodec = ManagedProfileSnapshotCodec(),
    private val selectionCodec: CanonicalEqSelectionCodec = CanonicalEqSelectionCodec(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.savedGeneralEqDao()

    /** General EQs in My EQs are global; outputId is retained for source compatibility only. */
    @Suppress("UNUSED_PARAMETER")
    fun observeForOutput(outputId: String): Flow<List<SavedGeneralEqRecord>> =
        dao.observeAll()
            .map { saved -> saved.map(::toDomain) }
            .flowOn(ioDispatcher)

    @Suppress("UNUSED_PARAMETER")
    suspend fun getForOutput(outputId: String, presetId: String): SavedGeneralEqRecord? =
        withContext(ioDispatcher) { dao.get(presetId)?.let(::toDomain) }

    @Suppress("UNUSED_PARAMETER")
    suspend fun saveForOutput(
        outputId: String,
        preset: GeneralEqPreset,
        selection: CanonicalEqSelection,
    ): Boolean = saveAllForOutput(outputId, listOf(preset to selection))

    /** All-or-nothing save so a stale/missing canonical source cannot partially save a batch. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun saveAllForOutput(
        outputId: String,
        selections: List<Pair<GeneralEqPreset, CanonicalEqSelection>>,
    ): Boolean =
        withContext(ioDispatcher) {
            if (selections.isEmpty()) return@withContext true
            if (selections.any { (preset, selection) ->
                    !CanonicalLegacyCatalogAdapter.isSameGeneralSelection(selection, preset)
                }
            ) return@withContext false
            database.withTransaction {
                selections.forEach { (preset, selection) ->
                    val existing = dao.get(preset.id)
                    val now = nowMillis()
                    val canonicalProfile = CanonicalLegacyCatalogAdapter.projectGeneralSelection(
                        selection,
                        preset.id,
                    )
                    dao.upsert(
                        SavedGeneralEqEntity(
                            presetId = preset.id,
                            displayName = preset.displayName,
                            category = preset.category.name,
                            profileJson = snapshotCodec.encode(canonicalProfile),
                            createdAtMillis = existing?.createdAtMillis ?: now,
                            updatedAtMillis = now,
                            canonicalSelectionJson = selectionCodec.encode(selection),
                        ),
                    )
                }
                true
            }
        }

    @Suppress("UNUSED_PARAMETER")
    suspend fun removeFromOutput(outputId: String, presetId: String) = withContext(ioDispatcher) {
        database.withTransaction {
            dao.deleteAllSelections(presetId)
            dao.delete(presetId)
        }
    }

    fun toExportRecord(record: SavedGeneralEqRecord): ManagedHeadphoneRecord {
        val profile = requireNotNull(record.actionProfileOrNull()) {
            "This saved General EQ has invalid or incomplete source data and cannot be exported."
        }
        val fingerprint = snapshotCodec.fingerprint(profile)
        val presetName = ToneBoostersConverter.buildPresetName(
            modelLabel = record.displayName,
            creator = profile.author,
            details = null,
        )
        val uapp = runCatching { ToneBoostersConverter.convert(profile, presetName) }.getOrNull()
        return ManagedHeadphoneRecord(
            productId = "general-export:${record.presetId}",
            vendorId = "general-eqs",
            vendorName = "General EQs",
            productName = categoryLabel(record.category),
            autoIncludeNewProfiles = false,
            createdAtMillis = record.createdAtMillis,
            updatedAtMillis = record.updatedAtMillis,
            profiles = listOf(
                ManagedProfileRecord(
                    profileId = record.presetId,
                    selected = true,
                    explicitlyExcluded = false,
                    lastKnownProfile = profile,
                    fingerprint = fingerprint,
                    firstSeenAtMillis = record.createdAtMillis,
                    lastSeenAtMillis = record.updatedAtMillis,
                    isNewUnreviewed = false,
                    isUpdatedUnreviewed = false,
                    noLongerAvailable = false,
                    generatedPresetName = presetName,
                    generatedXml = uapp?.xml,
                    generatedFromFingerprint = fingerprint,
                    generatedAtMillis = record.updatedAtMillis,
                ),
            ),
        )
    }

    private fun toDomain(entity: SavedGeneralEqEntity): SavedGeneralEqRecord {
        val profile = snapshotCodec.decode(entity.profileJson)
        var invalid = false
        val selection = entity.canonicalSelectionJson?.let { encoded ->
            runCatching {
                selectionCodec.decode(encoded).also { decoded ->
                    require(decoded.profile.isGeneralPreset) {
                        "Saved General EQ selection has the wrong canonical scope"
                    }
                    val projectedPreset = CanonicalLegacyCatalogAdapter.projectGeneralPreset(decoded)
                    require(projectedPreset.id == entity.presetId) {
                        "Saved General EQ ID does not match its canonical profile/revision"
                    }
                    require(projectedPreset.displayName == entity.displayName) {
                        "Saved General EQ name does not match its canonical revision"
                    }
                    require(projectedPreset.category.name == entity.category) {
                        "Saved General EQ category does not match its canonical purpose"
                    }
                    require(CanonicalLegacyCatalogAdapter.matchesGeneralSelection(decoded, profile, entity.presetId)) {
                        "Saved General EQ projection does not match its canonical selection"
                    }
                }
            }.getOrElse {
                invalid = true
                null
            }
        }
        return SavedGeneralEqRecord(
            presetId = entity.presetId,
            displayName = entity.displayName,
            category = GeneralEqCategory.valueOf(entity.category),
            profile = profile,
            createdAtMillis = entity.createdAtMillis,
            updatedAtMillis = entity.updatedAtMillis,
            canonicalSelection = selection,
            savedEqDataInvalid = invalid,
        )
    }

    companion object {
        private fun categoryLabel(category: GeneralEqCategory): String = when (category) {
            GeneralEqCategory.SOUND -> "Sound"
            GeneralEqCategory.GENRE -> "Genre"
            GeneralEqCategory.UTILITY -> "Utility"
        }
    }
}
