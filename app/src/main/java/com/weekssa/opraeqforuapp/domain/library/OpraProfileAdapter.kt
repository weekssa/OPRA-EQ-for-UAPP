package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import java.util.Locale

object OpraProfileAdapter {
    fun adapt(
        vendor: OpraVendor,
        product: OpraProduct,
        profile: OpraEqProfile,
        discoveredAt: String? = null,
    ): EqProfile? {
        val filters = profile.bands
            .orEmpty()
            .mapNotNull { band ->
                val type = band.type?.trim().orEmpty()
                val frequency = band.frequency
                val gain = band.gainDb
                if (type.isBlank() || frequency == null || gain == null) return@mapNotNull null
                EqFilter(
                    type = type,
                    frequencyHz = frequency,
                    gainDb = gain,
                    q = band.q,
                    slope = band.slope,
                )
            }
        if (filters.isEmpty()) return null

        val fingerprint = AcousticFingerprint.of(profile.preampGainDb, filters)
        val creator = profile.author?.trim()?.takeIf(String::isNotEmpty)
        val details = profile.details?.trim()?.takeIf(String::isNotEmpty)
        val target = inferExplicitTarget(details)
        val canonicalId = canonicalId(vendor.name, product.name, creator, target, fingerprint)
        val revisionId = "$canonicalId-${fingerprint.take(12)}"

        return EqProfile(
            canonicalId = canonicalId,
            headphone = HeadphoneIdentity(
                manufacturer = vendor.name.trim(),
                model = product.name.trim(),
            ),
            creator = creator,
            sourceKind = EqSourceKind.STRUCTURED_CATALOG,
            provenanceTier = ProvenanceTier.AUTHORITATIVE,
            target = target,
            sourceReferences = listOf(
                SourceReference(
                    sourceId = "opra:${profile.id}",
                    url = profile.link,
                    creator = creator,
                    discoveredAt = discoveredAt,
                    lastVerifiedAt = discoveredAt,
                    tier = ProvenanceTier.AUTHORITATIVE,
                    isPrimary = true,
                ),
            ),
            revisions = listOf(
                EqRevision(
                    revisionId = revisionId,
                    acousticFingerprint = fingerprint,
                    preampDb = profile.preampGainDb,
                    filters = filters,
                    target = target,
                    firstSeenAt = discoveredAt,
                ),
            ),
            latestRevisionId = revisionId,
        )
    }

    private fun inferExplicitTarget(details: String?): String? {
        if (details == null) return null
        val normalized = details.lowercase(Locale.ROOT)
        return when {
            "harman" in normalized -> "Harman"
            "diffuse field" in normalized || "diffuse-field" in normalized -> "Diffuse Field"
            else -> null
        }
    }

    private fun canonicalId(
        manufacturer: String,
        model: String,
        creator: String?,
        target: String?,
        fingerprint: String,
    ): String = listOf(
        slug(manufacturer),
        slug(model),
        slug(creator ?: "unknown"),
        slug(target ?: "custom"),
        fingerprint.take(16),
    ).joinToString(":")

    private fun slug(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
        .trim('-')
}
