package com.weekssa.opraeqforuapp.data.managed

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import java.security.MessageDigest
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
        val bytes = MessageDigest.getInstance("SHA-256").digest(encode(profile).toByteArray(Charsets.UTF_8))
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
    bands = bands?.map { band ->
        StoredBandSnapshot(
            type = band.type,
            frequency = band.frequency,
            gainDb = band.gainDb,
            q = band.q,
            slope = band.slope,
        )
    },
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
)
