package com.weekssa.opraeqforuapp.data.library

import androidx.room.withTransaction
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.blackpearl.buildBlackPearlCapturedEqDraft
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConverter
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.ParametricEqTextParser
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SavedEqRepository(
    private val database: OpraEqDatabase,
    private val snapshotCodec: ManagedProfileSnapshotCodec = ManagedProfileSnapshotCodec(),
    private val captureMetadataCodec: SavedEqCaptureMetadataCodec = SavedEqCaptureMetadataCodec(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.savedEqDao()

    fun observeForOutput(outputId: String): Flow<List<SavedEqRecord>> =
        combine(
            dao.observeAll(),
            dao.observeOutputSelections(outputId),
        ) { saved, selections ->
            val selectionById = selections.associateBy(OutputSavedEqEntity::entryId)
            saved.asSequence()
                .filter { it.entryId in selectionById }
                .map(::toDomain)
                .sortedWith(
                    compareByDescending<SavedEqRecord> { selectionById[it.entryId]?.selectedAtMillis ?: 0L }
                        .thenBy { it.entryId },
                )
                .toList()
        }.flowOn(ioDispatcher)

    suspend fun getForOutput(outputId: String, entryId: String): SavedEqRecord? =
        withContext(ioDispatcher) {
            if (dao.getSelection(outputId, entryId) == null) return@withContext null
            dao.get(entryId)?.let(::toDomain)
        }

    suspend fun toggleFavorite(
        outputId: String,
        profile: OpraEqProfile,
        manufacturer: String,
        model: String,
    ): Boolean = withContext(ioDispatcher) {
        database.withTransaction {
            val entryId = favoriteEntryId(profile.id)
            if (dao.getSelection(outputId, entryId) != null) {
                dao.deleteSelection(outputId, entryId)
                if (dao.selectionCount(entryId) == 0) dao.delete(entryId)
                return@withTransaction false
            }

            val existing = dao.get(entryId)
            val now = nowMillis()
            dao.upsert(
                SavedEqEntity(
                    entryId = entryId,
                    kind = KIND_FAVORITE,
                    sourceProfileId = profile.id,
                    productId = profile.productId,
                    manufacturer = manufacturer,
                    model = model,
                    displayName = favoriteDisplayName(profile),
                    profileJson = snapshotCodec.encode(profile),
                    createdAtMillis = existing?.createdAtMillis ?: now,
                    updatedAtMillis = now,
                ),
            )
            dao.upsertSelection(
                OutputSavedEqEntity(
                    outputId = outputId,
                    entryId = entryId,
                    selectedAtMillis = now,
                ),
            )
            true
        }
    }

    suspend fun importPersonal(
        outputId: String,
        manufacturer: String,
        model: String,
        displayName: String,
        target: String?,
        peqText: String,
    ): SavedEqRecord = withContext(ioDispatcher) {
        val maker = manufacturer.trim()
        val headphoneModel = model.trim()
        val name = displayName.trim()
        require(maker.isNotEmpty()) { "Manufacturer is required." }
        require(headphoneModel.isNotEmpty()) { "Model is required." }
        require(name.isNotEmpty()) { "EQ name is required." }

        val strict = ParametricEqTextParser.parseStrictPersonal(peqText)
        require(strict.errors.isEmpty()) { strict.errors.joinToString(" ") }
        val parsed = strict.parsedEq
        require(parsed.filters.isNotEmpty()) { "No supported enabled PEQ filters were found." }
        require(parsed.filters.all { it.type in SUPPORTED_PERSONAL_TYPES }) {
            "The import contains a filter type that EQ Library cannot export safely yet."
        }
        require(parsed.filters.all { it.q != null }) { "Every imported filter needs a Q value." }

        val id = UUID.randomUUID().toString()
        val productId = "personal-product:$id"
        val profileId = "personal-eq:$id"
        val details = buildList {
            add("Personal import")
            target?.trim()?.takeIf(String::isNotEmpty)?.let { add("Target: $it") }
        }.joinToString(" · ")
        val profile = OpraEqProfile(
            id = profileId,
            productId = productId,
            author = "Personal",
            details = details,
            link = null,
            profileType = "parametric_eq",
            preampGainDb = parsed.preampGainDb,
            bands = parsed.filters.map { filter ->
                OpraBand(
                    type = when (filter.type) {
                        EqFilterType.PEAK -> "peak_dip"
                        EqFilterType.LOW_SHELF -> "low_shelf"
                        EqFilterType.HIGH_SHELF -> "high_shelf"
                        else -> error("unsupported personal EQ filter")
                    },
                    frequency = filter.frequencyHz,
                    gainDb = filter.gainDb,
                    q = filter.q,
                    slope = filter.slope,
                )
            },
        )
        val now = nowMillis()
        val entity = SavedEqEntity(
            entryId = "personal:$id",
            kind = KIND_PERSONAL,
            sourceProfileId = null,
            productId = productId,
            manufacturer = maker,
            model = headphoneModel,
            displayName = name,
            profileJson = snapshotCodec.encode(profile),
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        database.withTransaction {
            dao.upsert(entity)
            dao.upsertSelection(
                OutputSavedEqEntity(
                    outputId = outputId,
                    entryId = entity.entryId,
                    selectedAtMillis = now,
                ),
            )
        }
        toDomain(entity)
    }

    /**
     * Saves a complete verified Black Pearl hardware EQ as a Personal EQ without treating ordinary
     * playback/global gain as source preamp. The exact native fingerprint is retained separately so
     * provenance survives canonical conversion and future matching can remain deterministic.
     */
    suspend fun captureBlackPearlEq(
        displayName: String,
        snapshotBundle: HardwareEqSnapshotBundle,
        association: SavedEqHeadphoneAssociation?,
    ): SavedEqRecord = withContext(ioDispatcher) {
        val name = displayName.trim()
        require(name.isNotEmpty()) { "EQ name is required." }

        val id = UUID.randomUUID().toString()
        val draft = buildBlackPearlCapturedEqDraft(
            captureId = id,
            snapshotBundle = snapshotBundle,
            association = association,
        )
        val now = nowMillis()
        val entity = SavedEqEntity(
            entryId = "personal:$id",
            kind = KIND_PERSONAL,
            sourceProfileId = null,
            productId = draft.productId,
            manufacturer = draft.manufacturer,
            model = draft.model,
            displayName = name,
            profileJson = snapshotCodec.encode(draft.profile),
            createdAtMillis = now,
            updatedAtMillis = now,
            captureMetadataJson = captureMetadataCodec.encode(draft.captureMetadata),
        )
        database.withTransaction {
            dao.upsert(entity)
            // A DAC capture belongs to that hardware output's My EQs, not the user's unrelated
            // active file-export selection.
            dao.upsertSelection(
                OutputSavedEqEntity(
                    outputId = ExportDevice.BLACK_PEARL.name,
                    entryId = entity.entryId,
                    selectedAtMillis = now,
                ),
            )
        }
        toDomain(entity)
    }

    suspend fun removeFromOutput(outputId: String, entryId: String) = withContext(ioDispatcher) {
        database.withTransaction {
            dao.deleteSelection(outputId, entryId)
            if (dao.selectionCount(entryId) == 0) dao.delete(entryId)
        }
    }

    fun toManagedHeadphone(record: SavedEqRecord): ManagedHeadphoneRecord {
        val fingerprint = snapshotCodec.fingerprint(record.profile)
        val modelLabel = record.model.ifBlank { record.displayName }
        val manufacturerLabel = record.manufacturer.ifBlank { "Personal EQ" }
        val presetName = ToneBoostersConverter.buildPresetName(
            modelLabel = modelLabel,
            creator = record.profile.author,
            details = record.displayName,
        )
        // Keep the source profile available to text-device formatters even when UAPP cannot
        // represent it. UAPP simply receives no XML candidate rather than preventing export to
        // another compatible target.
        val uapp = runCatching { ToneBoostersConverter.convert(record.profile, presetName) }.getOrNull()
        return ManagedHeadphoneRecord(
            productId = record.productId,
            vendorId = "saved-eq-vendor:${sha256(manufacturerLabel)}",
            vendorName = manufacturerLabel,
            productName = modelLabel,
            autoIncludeNewProfiles = false,
            createdAtMillis = record.createdAtMillis,
            updatedAtMillis = record.updatedAtMillis,
            profiles = listOf(
                ManagedProfileRecord(
                    profileId = record.profile.id,
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

    private fun toDomain(entity: SavedEqEntity) = SavedEqRecord(
        entryId = entity.entryId,
        kind = when (entity.kind) {
            KIND_FAVORITE -> SavedEqKind.Favorite
            KIND_PERSONAL -> SavedEqKind.Personal
            else -> error("Unknown saved EQ kind ${entity.kind}")
        },
        sourceProfileId = entity.sourceProfileId,
        productId = entity.productId,
        manufacturer = entity.manufacturer,
        model = entity.model,
        displayName = entity.displayName,
        profile = snapshotCodec.decode(entity.profileJson),
        createdAtMillis = entity.createdAtMillis,
        updatedAtMillis = entity.updatedAtMillis,
        captureMetadata = entity.captureMetadataJson?.let(captureMetadataCodec::decode),
    )

    companion object {
        private const val KIND_FAVORITE = "favorite"
        private const val KIND_PERSONAL = "personal"
        private val SUPPORTED_PERSONAL_TYPES = setOf(
            EqFilterType.PEAK,
            EqFilterType.LOW_SHELF,
            EqFilterType.HIGH_SHELF,
        )

        private fun favoriteEntryId(profileId: String) = "favorite:${sha256(profileId)}"

        private fun favoriteDisplayName(profile: OpraEqProfile): String = buildList {
            profile.author?.takeIf(String::isNotBlank)?.let(::add)
            profile.details?.takeIf(String::isNotBlank)?.let(::add)
        }.joinToString(" · ").ifBlank { "Saved EQ" }

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
                .take(24)
    }
}
