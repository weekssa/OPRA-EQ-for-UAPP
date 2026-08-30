package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import java.security.MessageDigest
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ManagedProfileSnapshotCodec(
    private val json: Json = Json {
        encodeDefaults = true
        explicitNulls = true
    },
) {
    fun encode(profile: OpraEqProfile): String = json.encodeToString(profile.toStoredSnapshot())

    fun decode(encoded: String): OpraEqProfile = json.decodeFromString<StoredProfileSnapshot>(encoded).toDomain()

    fun fingerprint(profile: OpraEqProfile): String {
        val semanticPayload = json.encodeToString(profile.toSemanticFingerprintSnapshot())
        val bytes = MessageDigest.getInstance("SHA-256").digest(semanticPayload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}

@Serializable
private data class StoredProfileSnapshot(
    val id: String,
    val productId: String,
    val author: String?,
    val details: String?,
    val link: String?,
    val profileType: String?,
    val preampGainDb: Double?,
    val bands: List<StoredBandSnapshot>?,
    /** Derived playback metadata; never substitutes for the source-authentic preamp field. */
    val eqLibrarySafetyHeadroomDb: Double? = null,
    /** Publication trust metadata; old snapshots default to verified for v0.2 compatibility. */
    val isVerified: Boolean = true,
)

@Serializable
private data class StoredProfileFingerprintSnapshot(
    val author: String,
    val details: String,
    val profileType: String?,
    val preampGainDb: Double?,
    val bands: List<StoredBandSnapshot>?,
)

@Serializable
private data class StoredBandSnapshot(
    val type: String?,
    val frequency: Double?,
    val gainDb: Double?,
    val q: Double?,
    val slope: Double?,
)

private fun OpraEqProfile.toStoredSnapshot() = StoredProfileSnapshot(
    id = id,
    productId = productId,
    author = author,
    details = details,
    link = link,
    profileType = profileType,
    preampGainDb = preampGainDb,
    bands = bands?.map(OpraBand::toStoredSnapshot),
    eqLibrarySafetyHeadroomDb = eqLibrarySafetyHeadroomDb,
    isVerified = isVerified,
)

private fun OpraEqProfile.toSemanticFingerprintSnapshot() = StoredProfileFingerprintSnapshot(
    author = author.orEmpty().lowercase(Locale.ROOT),
    details = details.orEmpty().lowercase(Locale.ROOT),
    profileType = profileType,
    preampGainDb = preampGainDb,
    bands = bands?.map(OpraBand::toStoredSnapshot),
)

private fun OpraBand.toStoredSnapshot() = StoredBandSnapshot(
    type = type,
    frequency = frequency,
    gainDb = gainDb,
    q = q,
    slope = slope,
)

private fun StoredProfileSnapshot.toDomain() = OpraEqProfile(
    id = id,
    productId = productId,
    author = author,
    details = details,
    link = link,
    profileType = profileType,
    preampGainDb = preampGainDb,
    bands = bands?.map { band ->
        OpraBand(
            type = band.type,
            frequency = band.frequency,
            gainDb = band.gainDb,
            q = band.q,
            slope = band.slope,
        )
    },
    eqLibrarySafetyHeadroomDb = eqLibrarySafetyHeadroomDb,
    isVerified = isVerified,
)
