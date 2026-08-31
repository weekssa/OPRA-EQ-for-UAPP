package com.weekssa.opraeqforuapp.data.library

import androidx.room.withTransaction
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqCategory
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConverter
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SavedGeneralEqRepository(
    private val database: OpraEqDatabase,
    private val snapshotCodec: ManagedProfileSnapshotCodec = ManagedProfileSnapshotCodec(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.savedGeneralEqDao()

    fun observeForOutput(outputId: String): Flow<List<SavedGeneralEqRecord>> =
        combine(
            dao.observeAll(),
            dao.observeOutputSelections(outputId),
        ) { saved, selections ->
            val selectedIds = selections.mapTo(mutableSetOf(), OutputGeneralEqEntity::presetId)
            saved.asSequence()
                .filter { it.presetId in selectedIds }
                .map(::toDomain)
                .sortedWith(
                    compareBy<SavedGeneralEqRecord> { it.category.ordinal }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
                        .thenBy { it.presetId },
                )
                .toList()
        }

    suspend fun getForOutput(outputId: String, presetId: String): SavedGeneralEqRecord? {
        if (dao.getSelection(outputId, presetId) == null) return null
        return dao.get(presetId)?.let(::toDomain)
    }

    suspend fun toggleForOutput(outputId: String, preset: GeneralEqPreset): Boolean =
        database.withTransaction {
            if (dao.getSelection(outputId, preset.id) != null) {
                dao.deleteSelection(outputId, preset.id)
                if (dao.selectionCount(preset.id) == 0) dao.delete(preset.id)
                return@withTransaction false
            }

            val now = nowMillis()
            val existing = dao.get(preset.id)
            dao.upsert(
                SavedGeneralEqEntity(
                    presetId = preset.id,
                    displayName = preset.displayName,
                    category = preset.category.name,
                    profileJson = snapshotCodec.encode(preset.toExportProfile()),
                    createdAtMillis = existing?.createdAtMillis ?: now,
                    updatedAtMillis = now,
                ),
            )
            dao.upsertSelection(
                OutputGeneralEqEntity(
                    outputId = outputId,
                    presetId = preset.id,
                    selectedAtMillis = now,
                ),
            )
            true
        }

    suspend fun removeFromOutput(outputId: String, presetId: String) {
        database.withTransaction {
            dao.deleteSelection(outputId, presetId)
            if (dao.selectionCount(presetId) == 0) dao.delete(presetId)
        }
    }

    fun toExportRecord(record: SavedGeneralEqRecord): ManagedHeadphoneRecord {
        val fingerprint = snapshotCodec.fingerprint(record.profile)
        val presetName = ToneBoostersConverter.buildPresetName(
            modelLabel = record.displayName,
            creator = record.profile.author,
            details = null,
        )
        val uapp = runCatching { ToneBoostersConverter.convert(record.profile, presetName) }.getOrNull()
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
                    lastKnownProfile = record.profile,
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

    private fun toDomain(entity: SavedGeneralEqEntity): SavedGeneralEqRecord = SavedGeneralEqRecord(
        presetId = entity.presetId,
        displayName = entity.displayName,
        category = GeneralEqCategory.valueOf(entity.category),
        profile = snapshotCodec.decode(entity.profileJson),
        createdAtMillis = entity.createdAtMillis,
        updatedAtMillis = entity.updatedAtMillis,
    )

    private fun GeneralEqPreset.toExportProfile(): OpraEqProfile = OpraEqProfile(
        id = id,
        productId = INTERNAL_GENERAL_PRODUCT_ID,
        author = creator,
        details = soundImpactSummary,
        link = sourceUrl,
        profileType = "parametric_eq",
        preampGainDb = preampGainDb,
        bands = bands,
        eqLibrarySafetyHeadroomDb = eqLibrarySafetyHeadroomDb,
        isVerified = isVerified,
    )

    companion object {
        private const val INTERNAL_GENERAL_PRODUCT_ID = "eq-library-general"

        private fun categoryLabel(category: GeneralEqCategory): String = when (category) {
            GeneralEqCategory.SOUND -> "Sound"
            GeneralEqCategory.GENRE -> "Genre"
            GeneralEqCategory.UTILITY -> "Utility"
        }
    }
}
