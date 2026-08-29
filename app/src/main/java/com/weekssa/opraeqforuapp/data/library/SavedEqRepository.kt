package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConverter
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.ParametricEqTextParser
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedEqRepository(
    database: OpraEqDatabase,
    private val snapshotCodec: ManagedProfileSnapshotCodec = ManagedProfileSnapshotCodec(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.savedEqDao()

    fun observeAll(): Flow<List<SavedEqRecord>> =
        dao.observeAll().map { entities -> entities.map(::toDomain) }

    suspend fun get(entryId: String): SavedEqRecord? = dao.get(entryId)?.let(::toDomain)

    suspend fun toggleFavorite(
        profile: OpraEqProfile,
        manufacturer: String,
        model: String,
    ): Boolean {
        val existing = dao.getFavorite(profile.id)
        if (existing != null) {
            dao.deleteFavorite(profile.id)
            return false
        }
        val now = nowMillis()
        dao.upsert(
            SavedEqEntity(
                entryId = favoriteEntryId(profile.id),
                kind = KIND_FAVORITE,
                sourceProfileId = profile.id,
                productId = profile.productId,
                manufacturer = manufacturer,
                model = model,
                displayName = favoriteDisplayName(profile),
                profileJson = snapshotCodec.encode(profile),
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        return true
    }

    suspend fun importPersonal(
        manufacturer: String,
        model: String,
        displayName: String,
        target: String?,
        peqText: String,
    ): SavedEqRecord {
        val maker = manufacturer.trim()
        val headphoneModel = model.trim()
        val name = displayName.trim()
        require(maker.isNotEmpty()) { "Manufacturer is required." }
        require(headphoneModel.isNotEmpty()) { "Model is required." }
        require(name.isNotEmpty()) { "EQ name is required." }

        val parsed = ParametricEqTextParser.parse(peqText)
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
            preampGainDb = parsed.preampGainDb ?: 0.0,
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
        dao.upsert(entity)
        return toDomain(entity)
    }

    suspend fun delete(entryId: String) {
        dao.delete(entryId)
    }

    fun toManagedHeadphone(record: SavedEqRecord): ManagedHeadphoneRecord {
        val fingerprint = snapshotCodec.fingerprint(record.profile)
        val presetName = ToneBoostersConverter.buildPresetName(
            modelLabel = record.model,
            creator = record.profile.author,
            details = record.displayName,
        )
        // Keep the source profile available to text-device formatters even when UAPP cannot
        // represent it. UAPP simply receives no XML candidate rather than preventing export to
        // another compatible target.
        val uapp = runCatching { ToneBoostersConverter.convert(record.profile, presetName) }.getOrNull()
        return ManagedHeadphoneRecord(
            productId = record.productId,
            vendorId = "saved-eq-vendor:${sha256(record.manufacturer)}",
            vendorName = record.manufacturer,
            productName = record.model,
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
