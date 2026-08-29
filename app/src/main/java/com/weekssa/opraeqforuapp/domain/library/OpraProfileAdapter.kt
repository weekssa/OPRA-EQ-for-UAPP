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
        discoveredAtEpochSeconds: Long? = null,
    ): CanonicalEqProfile? {
        val filters = profile.bands
            .orEmpty()
            .mapNotNull { band ->
                val type = parseFilterType(band.type) ?: return@mapNotNull null
                val frequency = band.frequency ?: return@mapNotNull null
                EqFilter(
                    type = type,
                    frequencyHz = frequency,
                    gainDb = band.gainDb,
                    q = band.q,
                    slope = band.slope,
                )
            }
        if (filters.isEmpty()) return null

        val fingerprint = AcousticFingerprint.of(profile.preampGainDb, filters)
        val creator = profile.author?.trim()?.takeIf(String::isNotEmpty)
        val details = profile.details?.trim()?.takeIf(String::isNotEmpty)
        val targetName = inferExplicitTarget(details)
        val target = EqTarget(
            name = targetName,
            kind = if (targetName != null) EqTargetKind.EXPLICIT_TARGET else EqTargetKind.UNKNOWN,
        )
        val canonicalId = canonicalId(vendor.name, product.name, creator, targetName, fingerprint)
        val revisionId = "$canonicalId-${fingerprint.take(12)}"
        val sourceReference = EqSourceReference(
            sourceId = "opra",
            sourceKind = EqSourceKind.STRUCTURED_CATALOG,
            sourceRecordId = profile.id,
            sourceVendorId = vendor.id,
            sourceProductId = product.id,
            url = profile.link,
            creator = creator,
            provenanceTier = ProvenanceTier.AUTHORITATIVE,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            discoveredAtEpochSeconds = discoveredAtEpochSeconds,
            lastVerifiedAtEpochSeconds = discoveredAtEpochSeconds,
            isPrimary = true,
        )

        return CanonicalEqProfile(
            canonicalProfileId = canonicalId,
            headphone = HeadphoneIdentity(
                manufacturer = vendor.name.trim(),
                model = product.name.trim(),
            ),
            creator = creator,
            target = target,
            tuningLabel = details,
            revisions = listOf(
                EqRevision(
                    revisionId = revisionId,
                    acousticFingerprint = fingerprint,
                    preampGainDb = profile.preampGainDb,
                    filters = filters,
                    sourceReferences = listOf(sourceReference),
                    soundImpactSummary = SoundImpactSummary.fromFilters(filters),
                    firstSeenAtEpochSeconds = discoveredAtEpochSeconds,
                    isLatest = true,
                ),
            ),
        )
    }

    private fun parseFilterType(value: String?): EqFilterType? = when (value?.trim()?.uppercase(Locale.ROOT)) {
        "PK", "PEQ", "PEAK", "PEAKING" -> EqFilterType.PEAK
        "LS", "LSC", "LOW_SHELF", "LOWSHELF" -> EqFilterType.LOW_SHELF
        "HS", "HSC", "HIGH_SHELF", "HIGHSHELF" -> EqFilterType.HIGH_SHELF
        "LP", "LPF", "LOW_PASS", "LOWPASS" -> EqFilterType.LOW_PASS
        "HP", "HPF", "HIGH_PASS", "HIGHPASS" -> EqFilterType.HIGH_PASS
        null, "" -> null
        else -> EqFilterType.OTHER
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
