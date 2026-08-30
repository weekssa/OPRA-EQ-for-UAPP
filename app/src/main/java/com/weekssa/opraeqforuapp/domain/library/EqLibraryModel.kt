package com.weekssa.opraeqforuapp.domain.library

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EqSourceKind {
    @SerialName("structured_catalog") STRUCTURED_CATALOG,
    @SerialName("creator") CREATOR,
    @SerialName("measurement_derived") MEASUREMENT_DERIVED,
    @SerialName("community") COMMUNITY,
    @SerialName("repository") REPOSITORY,
    @SerialName("device_community") DEVICE_COMMUNITY,
    @SerialName("user_submission") USER_SUBMISSION,
    @SerialName("personal_import") PERSONAL_IMPORT,
}

@Serializable
enum class ProvenanceTier {
    @SerialName("authoritative") AUTHORITATIVE,
    @SerialName("measurement_derived") MEASUREMENT_DERIVED,
    @SerialName("traceable_community") TRACEABLE_COMMUNITY,
    @SerialName("mirror") MIRROR,
    @SerialName("needs_review") NEEDS_REVIEW,
}

@Serializable
enum class RedistributionPolicy {
    @SerialName("allowed") ALLOWED,
    @SerialName("structured-data-only") STRUCTURED_DATA_ONLY,
    @SerialName("link-only") LINK_ONLY,
    @SerialName("unknown-review") UNKNOWN_REVIEW,
}

@Serializable
enum class VerificationStatus {
    @SerialName("verified") VERIFIED,
    @SerialName("unverified") UNVERIFIED,
}

@Serializable
enum class EqFilterType {
    @SerialName("peak") PEAK,
    @SerialName("low_shelf") LOW_SHELF,
    @SerialName("high_shelf") HIGH_SHELF,
    @SerialName("low_pass") LOW_PASS,
    @SerialName("high_pass") HIGH_PASS,
    @SerialName("other") OTHER,
}

@Serializable
enum class EqTargetKind {
    @SerialName("explicit_target") EXPLICIT_TARGET,
    @SerialName("creator_target") CREATOR_TARGET,
    @SerialName("custom_user") CUSTOM_USER,
    @SerialName("unknown") UNKNOWN,
}

@Serializable
data class HeadphoneIdentity(
    val manufacturer: String,
    val model: String,
    val variant: String? = null,
    @SerialName("pads_or_mode") val padsOrMode: String? = null,
    /** Explicit source-qualified names known to refer to this same physical product. */
    @SerialName("model_aliases") val modelAliases: List<String> = emptyList(),
) {
    val normalizedKey: String
        get() = listOf(manufacturer, model, variant.orEmpty(), padsOrMode.orEmpty())
            .joinToString("|") { normalizeIdentityText(it) }

    companion object {
        private fun normalizeIdentityText(value: String): String =
            value.lowercase().filter(Char::isLetterOrDigit)
    }
}

@Serializable
data class EqFilter(
    val type: EqFilterType,
    @SerialName("frequency_hz") val frequencyHz: Double,
    @SerialName("gain_db") val gainDb: Double? = null,
    val q: Double? = null,
    val slope: Double? = null,
)

@Serializable
data class EqTarget(
    val name: String?,
    val kind: EqTargetKind,
)

@Serializable
data class EqSourceReference(
    @SerialName("source_id") val sourceId: String,
    @SerialName("source_kind") val sourceKind: EqSourceKind,
    @SerialName("source_record_id") val sourceRecordId: String?,
    /**
     * Upstream dataset/database that supplied the measurement or structured tuning input.
     *
     * This is intentionally separate from sourceId. For example, an AutoEq-generated tuning can
     * have sourceId=autoeq while sourceDataset=HypetheSonics. Keeping both prevents the UI from
     * incorrectly attributing authorship to a measurement database while still making the full
     * multi-database catalog visible and filterable.
     */
    @SerialName("source_dataset") val sourceDataset: String? = null,
    @SerialName("source_vendor_id") val sourceVendorId: String? = null,
    @SerialName("source_product_id") val sourceProductId: String? = null,
    val url: String?,
    val creator: String?,
    @SerialName("provenance_tier") val provenanceTier: ProvenanceTier,
    @SerialName("redistribution_policy") val redistributionPolicy: RedistributionPolicy,
    @SerialName("published_at_epoch_seconds") val publishedAtEpochSeconds: Long? = null,
    @SerialName("updated_at_epoch_seconds") val updatedAtEpochSeconds: Long? = null,
    @SerialName("discovered_at_epoch_seconds") val discoveredAtEpochSeconds: Long? = null,
    @SerialName("last_verified_at_epoch_seconds") val lastVerifiedAtEpochSeconds: Long? = null,
    @SerialName("is_primary") val isPrimary: Boolean = false,
)

@Serializable
data class EqRevision(
    @SerialName("revision_id") val revisionId: String,
    @SerialName("acoustic_fingerprint") val acousticFingerprint: String,
    @SerialName("preamp_gain_db") val preampGainDb: Double?,
    val filters: List<EqFilter>,
    @SerialName("source_references") val sourceReferences: List<EqSourceReference>,
    @SerialName("source_version_label") val sourceVersionLabel: String? = null,
    @SerialName("sound_impact_summary") val soundImpactSummary: String? = null,
    @SerialName("verification_status") val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    @SerialName("first_seen_at_epoch_seconds") val firstSeenAtEpochSeconds: Long? = null,
    @SerialName("source_updated_at_epoch_seconds") val sourceUpdatedAtEpochSeconds: Long? = null,
    @SerialName("is_latest") val isLatest: Boolean = false,
    /**
     * Playback headroom calculated by EQ Library only when the source omitted preamp.
     * This is derived metadata, never a replacement for source-authentic preampGainDb.
     */
    @SerialName("eq_library_safety_headroom_db") val eqLibrarySafetyHeadroomDb: Double? = null,
)

@Serializable
data class CanonicalEqProfile(
    @SerialName("canonical_profile_id") val canonicalProfileId: String,
    val headphone: HeadphoneIdentity,
    val creator: String?,
    val target: EqTarget,
    @SerialName("tuning_label") val tuningLabel: String?,
    val revisions: List<EqRevision>,
) {
    init {
        require(revisions.isNotEmpty()) { "EQ profile must contain at least one revision" }
        require(revisions.count(EqRevision::isLatest) == 1) { "EQ profile must contain exactly one latest revision" }
    }

    val latestRevision: EqRevision
        get() = revisions.first(EqRevision::isLatest)
}

data class EqCandidate(
    val headphone: HeadphoneIdentity,
    val creator: String?,
    val target: EqTarget,
    val tuningLabel: String?,
    val preampGainDb: Double?,
    val filters: List<EqFilter>,
    val sourceReference: EqSourceReference,
    val sourceVersionLabel: String? = null,
    val soundImpactSummary: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
)
