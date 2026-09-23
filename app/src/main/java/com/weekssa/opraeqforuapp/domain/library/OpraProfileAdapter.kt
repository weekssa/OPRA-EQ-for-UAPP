package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
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
        if (profile.preampGainDb?.isFinite() == false) return null
        val sourceBands = profile.bands ?: return null
        if (sourceBands.isEmpty()) return null

        val filters = sourceBands.map { band ->
            parseBand(band) ?: return null
        }

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

    /**
     * OPRA bands are source data, not optional hints. Any unsupported type, missing required field,
     * or non-finite value rejects the whole profile so canonical ingestion cannot silently drop or
     * partially reinterpret an active source filter.
     */
    private fun parseBand(band: OpraBand): EqFilter? {
        val type = parseFilterType(band.type) ?: return null
        val frequency = band.frequency?.takeIf { it.isFinite() && it > 0.0 } ?: return null
        val gain = band.gainDb?.takeIf { it.isFinite() }
        val q = band.q?.takeIf { it.isFinite() && it > 0.0 }
        val slope = band.slope?.takeIf { it.isFinite() }

        if (band.gainDb != null && gain == null) return null
        if (band.q != null && q == null) return null
        if (band.slope != null && slope == null) return null

        when (type) {
            EqFilterType.PEAK,
            EqFilterType.LOW_SHELF,
            EqFilterType.HIGH_SHELF,
            -> if (gain == null || q == null) return null

            EqFilterType.LOW_PASS,
            EqFilterType.HIGH_PASS,
            -> if (slope == null) return null

            EqFilterType.OTHER -> return null
        }

        return EqFilter(
            type = type,
            frequencyHz = frequency,
            gainDb = gain,
            q = q,
            slope = slope,
        )
    }

    private fun parseFilterType(value: String?): EqFilterType? = when (value?.trim()?.uppercase(Locale.ROOT)) {
        "PK", "PEQ", "PEAK", "PEAKING", "PEAK_DIP" -> EqFilterType.PEAK
        "LS", "LSC", "LOW_SHELF", "LOWSHELF" -> EqFilterType.LOW_SHELF
        "HS", "HSC", "HIGH_SHELF", "HIGHSHELF" -> EqFilterType.HIGH_SHELF
        "LP", "LPF", "LOW_PASS", "LOWPASS" -> EqFilterType.LOW_PASS
        "HP", "HPF", "HIGH_PASS", "HIGHPASS" -> EqFilterType.HIGH_PASS
        null, "" -> null
        else -> null
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
