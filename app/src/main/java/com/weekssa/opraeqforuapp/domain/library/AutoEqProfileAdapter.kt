package com.weekssa.opraeqforuapp.domain.library

import java.util.Locale

/**
 * Converts an AutoEq-style ParametricEQ.txt payload into the canonical EQ model.
 * Measurement provenance is intentionally distinct from authored creator presets.
 */
object AutoEqProfileAdapter {
    data class Metadata(
        val manufacturer: String,
        val model: String,
        val variant: String? = null,
        val padsOrMode: String? = null,
        val sourceRecordId: String,
        val sourceUrl: String?,
        val measurementSource: String?,
        val targetName: String?,
        val sourceVersionLabel: String? = null,
        val sourceUpdatedAtEpochSeconds: Long? = null,
        val discoveredAtEpochSeconds: Long? = null,
    )

    fun adapt(metadata: Metadata, parametricEqText: String): CanonicalEqProfile? {
        val parsed = ParametricEqTextParser.parse(parametricEqText)
        if (parsed.filters.isEmpty()) return null

        val fingerprint = AcousticFingerprint.of(parsed.preampGainDb, parsed.filters)
        val creator = "AutoEq"
        val target = metadata.targetName?.trim()?.takeIf(String::isNotEmpty)
        val measurement = metadata.measurementSource?.trim()?.takeIf(String::isNotEmpty)
        val canonicalId = canonicalId(
            manufacturer = metadata.manufacturer,
            model = metadata.model,
            variant = metadata.variant,
            measurementSource = measurement,
            target = target,
            fingerprint = fingerprint,
        )
        val sourceReference = EqSourceReference(
            sourceId = "autoeq",
            sourceKind = EqSourceKind.MEASUREMENT_DERIVED,
            sourceRecordId = metadata.sourceRecordId,
            url = metadata.sourceUrl,
            creator = creator,
            provenanceTier = ProvenanceTier.MEASUREMENT_DERIVED,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            updatedAtEpochSeconds = metadata.sourceUpdatedAtEpochSeconds,
            discoveredAtEpochSeconds = metadata.discoveredAtEpochSeconds,
            lastVerifiedAtEpochSeconds = metadata.discoveredAtEpochSeconds,
            isPrimary = true,
        )

        return CanonicalEqProfile(
            canonicalProfileId = canonicalId,
            headphone = HeadphoneIdentity(
                manufacturer = metadata.manufacturer.trim(),
                model = metadata.model.trim(),
                variant = metadata.variant?.trim()?.takeIf(String::isNotEmpty),
                padsOrMode = metadata.padsOrMode?.trim()?.takeIf(String::isNotEmpty),
            ),
            creator = creator,
            target = EqTarget(
                name = target,
                kind = if (target != null) EqTargetKind.EXPLICIT_TARGET else EqTargetKind.UNKNOWN,
            ),
            tuningLabel = buildTuningLabel(measurement, target),
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

    private fun buildTuningLabel(measurementSource: String?, target: String?): String? = when {
        measurementSource != null && target != null -> "$target from $measurementSource measurement"
        target != null -> target
        measurementSource != null -> "From $measurementSource measurement"
        else -> null
    }

    private fun canonicalId(
        manufacturer: String,
        model: String,
        variant: String?,
        measurementSource: String?,
        target: String?,
        fingerprint: String,
    ): String = listOf(
        "autoeq",
        slug(manufacturer),
        slug(model),
        slug(variant ?: "default"),
        slug(measurementSource ?: "unknown-measurement"),
        slug(target ?: "unknown-target"),
        fingerprint.take(16),
    ).joinToString(":")

    private fun slug(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
        .trim('-')
}
