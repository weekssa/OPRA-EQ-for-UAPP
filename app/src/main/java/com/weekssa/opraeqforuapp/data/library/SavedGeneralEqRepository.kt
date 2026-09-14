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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SavedGeneralEqRepository(
    private val database: OpraEqDatabase,
    private val snapshotCodec: ManagedProfileSnapshotCodec = ManagedProfileSnapshotCodec(),
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
    suspend fun saveForOutput(outputId: String, preset: GeneralEqPreset): Boolean =
        withContext(ioDispatcher) {
            database.withTransaction {
                val existing = dao.get(preset.id)
                val now = nowMillis()
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
                existing == null
            }
        }

    @Suppress("UNUSED_PARAMETER")
    suspend fun toggleForOutput(outputId: String, preset: GeneralEqPreset): Boolean =
        withContext(ioDispatcher) {
            database.withTransaction {
                if (dao.get(preset.id) != null) {
                    dao.deleteAllSelections(preset.id)
                    dao.delete(preset.id)
                    return@withTransaction false
                }

                val now = nowMillis()
                dao.upsert(
                    SavedGeneralEqEntity(
                        presetId = preset.id,
                        displayName = preset.displayName,
                        category = preset.category.name,
                        profileJson = snapshotCodec.encode(preset.toExportProfile()),
                        createdAtMillis = now,
                        updatedAtMillis = now,
                    ),
                )
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
        canonicalProfileId = canonicalProfileId,
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
