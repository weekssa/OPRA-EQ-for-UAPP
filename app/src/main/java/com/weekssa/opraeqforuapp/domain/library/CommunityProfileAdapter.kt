package com.weekssa.opraeqforuapp.domain.library

import java.util.Locale

/**
 * Converts traceable public/community PEQ text into a canonical candidate profile.
 *
 * This adapter deliberately requires the caller to supply provenance metadata. It does not infer
 * a target or creator from forum prose. Link-only sources may still contribute normalized filters
 * and provenance while retaining the original source URL as the authoritative reference.
 */
object CommunityProfileAdapter {
    data class Metadata(
        val sourceId: String,
        val sourceKind: EqSourceKind = EqSourceKind.COMMUNITY,
        val sourceRecordId: String,
        val sourceUrl: String,
        val manufacturer: String,
        val model: String,
        val variant: String? = null,
        val padsOrMode: String? = null,
        val creator: String,
        val targetName: String? = null,
        val tuningLabel: String? = null,
        val sourceVersionLabel: String? = null,
        val sourcePublishedAtEpochSeconds: Long? = null,
        val sourceUpdatedAtEpochSeconds: Long? = null,
        val discoveredAtEpochSeconds: Long? = null,
        val redistributionPolicy: RedistributionPolicy = RedistributionPolicy.LINK_ONLY,
    )

    fun adapt(metadata: Metadata, parametricEqText: String): CanonicalEqProfile? {
        val sourceId = metadata.sourceId.trim()
        val sourceRecordId = metadata.sourceRecordId.trim()
        val sourceUrl = metadata.sourceUrl.trim()
        val manufacturer = metadata.manufacturer.trim()
        val model = metadata.model.trim()
        val creator = metadata.creator.trim()
        if (
            sourceId.isEmpty() || sourceRecordId.isEmpty() || sourceUrl.isEmpty() ||
            manufacturer.isEmpty() || model.isEmpty() || creator.isEmpty()
        ) return null

        val parsed = ParametricEqTextParser.parse(parametricEqText)
        if (parsed.filters.isEmpty()) return null

        val fingerprint = AcousticFingerprint.of(parsed.preampGainDb, parsed.filters)
        val target = metadata.targetName?.trim()?.takeIf(String::isNotEmpty)
        val canonicalId = canonicalId(
            manufacturer = manufacturer,
            model = model,
            variant = metadata.variant,
            creator = creator,
            target = target,
            tuningLabel = metadata.tuningLabel,
            fingerprint = fingerprint,
        )
        val sourceReference = EqSourceReference(
            sourceId = sourceId,
            sourceKind = metadata.sourceKind,
            sourceRecordId = sourceRecordId,
            url = sourceUrl,
            creator = creator,
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = metadata.redistributionPolicy,
            publishedAtEpochSeconds = metadata.sourcePublishedAtEpochSeconds,
            updatedAtEpochSeconds = metadata.sourceUpdatedAtEpochSeconds,
            discoveredAtEpochSeconds = metadata.discoveredAtEpochSeconds,
            lastVerifiedAtEpochSeconds = metadata.discoveredAtEpochSeconds,
            isPrimary = true,
        )

        return CanonicalEqProfile(
            canonicalProfileId = canonicalId,
            headphone = HeadphoneIdentity(
                manufacturer = manufacturer,
                model = model,
                variant = metadata.variant?.trim()?.takeIf(String::isNotEmpty),
                padsOrMode = metadata.padsOrMode?.trim()?.takeIf(String::isNotEmpty),
            ),
            creator = creator,
            target = EqTarget(
                name = target,
                kind = if (target != null) EqTargetKind.EXPLICIT_TARGET else EqTargetKind.CUSTOM_USER,
            ),
            tuningLabel = metadata.tuningLabel?.trim()?.takeIf(String::isNotEmpty)
                ?: target
                ?: "Community tuning",
            revisions = listOf(
                EqRevision(
                    revisionId = "$canonicalId-${fingerprint.take(12)}",
                    acousticFingerprint = fingerprint,
                    preampGainDb = parsed.preampGainDb,
                    filters = parsed.filters,
                    sourceReferences = listOf(sourceReference),
                    sourceVersionLabel = metadata.sourceVersionLabel,
                    soundImpactSummary = SoundImpactSummary.fromFilters(parsed.filters),
                    firstSeenAtEpochSeconds = metadata.discoveredAtEpochSeconds,
                    sourceUpdatedAtEpochSeconds = metadata.sourceUpdatedAtEpochSeconds,
                    isLatest = true,
                ),
            ),
        )
    }

    private fun canonicalId(
        manufacturer: String,
        model: String,
        variant: String?,
        creator: String,
        target: String?,
        tuningLabel: String?,
        fingerprint: String,
    ): String = listOf(
        "community",
        slug(manufacturer),
        slug(model),
        slug(variant ?: "default"),
        slug(creator),
        slug(target ?: tuningLabel ?: "custom"),
        fingerprint.take(16),
    ).joinToString(":")

    private fun slug(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
        .trim('-')
}
