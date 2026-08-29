package com.weekssa.opraeqforuapp.domain.library

import kotlinx.serialization.Serializable

@Serializable
enum class EqSourceKind {
    STRUCTURED_CATALOG,
    CREATOR,
    MEASUREMENT_DERIVED,
    COMMUNITY,
    REPOSITORY,
    DEVICE_COMMUNITY,
    USER_SUBMISSION,
    PERSONAL_IMPORT,
}

@Serializable
enum class ProvenanceTier {
    AUTHORITATIVE,
    MEASUREMENT_DERIVED,
    TRACEABLE_COMMUNITY,
    MIRROR,
    NEEDS_REVIEW,
}

@Serializable
enum class RedistributionPolicy {
    ALLOWED,
    STRUCTURED_DATA_ONLY,
    LINK_ONLY,
    UNKNOWN_REVIEW,
}

@Serializable
enum class EqFilterType {
    PEAK,
    LOW_SHELF,
    HIGH_SHELF,
    LOW_PASS,
    HIGH_PASS,
    OTHER,
}

@Serializable
enum class EqTargetKind {
    EXPLICIT_TARGET,
    CREATOR_TARGET,
    CUSTOM_USER,
    UNKNOWN,
}

@Serializable
data class HeadphoneIdentity(
    val manufacturer: String,
    val model: String,
    val variant: String? = null,
    val padsOrMode: String? = null,
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
    val frequencyHz: Double,
    val gainDb: Double? = null,
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
    val sourceId: String,
    val sourceKind: EqSourceKind,
    val sourceRecordId: String?,
    val url: String?,
    val creator: String?,
    val provenanceTier: ProvenanceTier,
    val redistributionPolicy: RedistributionPolicy,
    val publishedAtEpochSeconds: Long? = null,
    val updatedAtEpochSeconds: Long? = null,
    val discoveredAtEpochSeconds: Long? = null,
    val lastVerifiedAtEpochSeconds: Long? = null,
    val isPrimary: Boolean = false,
)

@Serializable
data class EqRevision(
    val revisionId: String,
    val acousticFingerprint: String,
    val preampGainDb: Double?,
    val filters: List<EqFilter>,
    val sourceReferences: List<EqSourceReference>,
    val sourceVersionLabel: String? = null,
    val soundImpactSummary: String? = null,
    val firstSeenAtEpochSeconds: Long? = null,
    val sourceUpdatedAtEpochSeconds: Long? = null,
    val isLatest: Boolean = false,
)

@Serializable
data class CanonicalEqProfile(
    val canonicalProfileId: String,
    val headphone: HeadphoneIdentity,
    val creator: String?,
    val target: EqTarget,
    val tuningLabel: String?,
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
)
