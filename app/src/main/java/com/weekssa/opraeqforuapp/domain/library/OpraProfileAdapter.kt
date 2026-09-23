package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraFilterTypeNormalizer
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
        if (profile.profileType?.trim()?.lowercase(Locale.ROOT) != "parametric_eq") return null
        if (profile.preampGainDb?.isFinite() == false) return null
        val sourceBands = profile.bands?.takeIf { it.isNotEmpty() } ?: return null
        val filters = sourceBands.map { band -> adaptBand(band) ?: return null }

        val fingerprint = AcousticFingerprint.of(profile.preampGainDb, filters)
        val sourcePriority = AcousticFingerprint.sourcePriority(filters)
        val creator = profile.author?.trim()?.takeIf(String::isNotEmpty)
        val details = profile.details?.trim()?.takeIf(String::isNotEmpty)
        val targetName = inferExplicitTarget(details)
        val target = EqTarget(
            name = targetName,
            kind = if (targetName != null) EqTargetKind.EXPLICIT_TARGET else EqTargetKind.UNKNOWN,
        )
        val canonicalId = canonicalId(
            vendor.name,
            product.name,
            creator,
            targetName,
            fingerprint,
            sourcePriority,
        )
        val revisionId = "$canonicalId-${fingerprint.take(12)}-priority-${sourcePriority.take(12)}"
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

    private fun adaptBand(band: com.weekssa.opraeqforuapp.domain.catalog.OpraBand): EqFilter? {
        val rawType = band.type?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val normalizedType = OpraFilterTypeNormalizer.normalize(rawType) ?: return null
        val type = parseFilterType(normalizedType)
        val frequency = band.frequency?.takeIf { it.isFinite() && it > 0.0 } ?: return null
        // OPRA's eq_info schema documents a zero default for per-band gain_db.
        val gain = band.gainDb ?: 0.0
        if (!gain.isFinite()) return null

        val q = band.q
        if (q != null && (!q.isFinite() || q < MIN_OPRA_Q)) return null
        // The OPRA eq_info schema defines 12 dB/octave as the default for low/high-pass filters.
        val slope = band.slope ?: if (OpraFilterTypeNormalizer.requiresSlope(normalizedType)) {
            DEFAULT_OPRA_SLOPE
        } else {
            null
        }
        if (slope != null && (!slope.isFinite() || slope !in OPRA_SLOPES)) return null

        when {
            OpraFilterTypeNormalizer.requiresQ(normalizedType) -> if (q == null) return null
        }

        return EqFilter(
            type = type,
            frequencyHz = frequency,
            gainDb = gain,
            q = q,
            slope = slope,
            sourceType = rawType.takeIf { type == EqFilterType.OTHER },
        )
    }

    private fun parseFilterType(value: String): EqFilterType = when (value) {
        "peak_dip" -> EqFilterType.PEAK
        "low_shelf" -> EqFilterType.LOW_SHELF
        "high_shelf" -> EqFilterType.HIGH_SHELF
        "low_pass" -> EqFilterType.LOW_PASS
        "high_pass" -> EqFilterType.HIGH_PASS
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
        sourcePriority: String,
    ): String = listOf(
        slug(manufacturer),
        slug(model),
        slug(creator ?: "unknown"),
        slug(target ?: "custom"),
        fingerprint.take(16),
        "priority-$sourcePriority",
    ).joinToString(":")

    private fun slug(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
        .trim('-')

    private const val MIN_OPRA_Q = 0.1
    private const val DEFAULT_OPRA_SLOPE = 12.0
    private val OPRA_SLOPES = setOf(6.0, 12.0, 18.0, 24.0, 30.0, 36.0)
}
