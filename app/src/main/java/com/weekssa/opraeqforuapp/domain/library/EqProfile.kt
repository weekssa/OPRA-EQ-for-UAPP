package com.weekssa.opraeqforuapp.domain.library

enum class EqSourceKind {
    STRUCTURED_CATALOG,
    CREATOR,
    MEASUREMENT_DERIVED,
    COMMUNITY,
    PERSONAL,
}

enum class ProvenanceTier {
    AUTHORITATIVE,
    MEASUREMENT_DERIVED,
    TRACEABLE_COMMUNITY,
    MIRROR,
    AMBIGUOUS,
}

data class HeadphoneIdentity(
    val manufacturer: String,
    val model: String,
    val variant: String? = null,
)

data class EqFilter(
    val type: String,
    val frequencyHz: Double,
    val gainDb: Double,
    val q: Double? = null,
    val slope: Double? = null,
)

data class SourceReference(
    val sourceId: String,
    val url: String?,
    val creator: String?,
    val sourceUpdatedAt: String? = null,
    val discoveredAt: String? = null,
    val lastVerifiedAt: String? = null,
    val tier: ProvenanceTier,
    val isPrimary: Boolean = false,
)

data class EqRevision(
    val revisionId: String,
    val acousticFingerprint: String,
    val preampDb: Double?,
    val filters: List<EqFilter>,
    val target: String? = null,
    val sourcePublishedAt: String? = null,
    val sourceUpdatedAt: String? = null,
    val firstSeenAt: String? = null,
    val changeSummary: String? = null,
)

data class EqProfile(
    val canonicalId: String,
    val headphone: HeadphoneIdentity,
    val creator: String?,
    val sourceKind: EqSourceKind,
    val provenanceTier: ProvenanceTier,
    val target: String?,
    val sourceReferences: List<SourceReference>,
    val revisions: List<EqRevision>,
    val latestRevisionId: String,
    val soundImpactSummary: String? = null,
) {
    init {
        require(revisions.isNotEmpty()) { "EQ profile must contain at least one revision" }
        require(revisions.any { it.revisionId == latestRevisionId }) {
            "latestRevisionId must reference an existing revision"
        }
    }

    val latestRevision: EqRevision
        get() = revisions.first { it.revisionId == latestRevisionId }
}
