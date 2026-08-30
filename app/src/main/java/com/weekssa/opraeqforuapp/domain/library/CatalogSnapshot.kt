package com.weekssa.opraeqforuapp.domain.library

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogSnapshot(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("source_registry_version") val sourceRegistryVersion: String,
    val profiles: List<CanonicalEqProfile>,
    val sources: List<SourceStatus> = emptyList(),
    @SerialName("headphone_aliases") val headphoneAliases: List<HeadphoneAliasGroup> = emptyList(),
)

@Serializable
data class HeadphoneAliasGroup(
    val manufacturer: String,
    @SerialName("canonical_model") val canonicalModel: String,
    val aliases: List<String> = emptyList(),
    val evidence: List<String> = emptyList(),
)

@Serializable
data class SourceStatus(
    @SerialName("source_id") val sourceId: String,
    val lifecycle: SourceLifecycle,
    @SerialName("last_successful_scan_at") val lastSuccessfulScanAt: String? = null,
    @SerialName("last_attempt_at") val lastAttemptAt: String? = null,
    val cursor: String? = null,
    @SerialName("parser_version") val parserVersion: String? = null,
    @SerialName("consecutive_failures") val consecutiveFailures: Int = 0,
    val redistribution: RedistributionMode = RedistributionMode.REVIEW_REQUIRED,
)

@Serializable
enum class SourceLifecycle {
    @SerialName("proposed") PROPOSED,
    @SerialName("reviewing") REVIEWING,
    @SerialName("active") ACTIVE,
    @SerialName("link-only") LINK_ONLY,
    @SerialName("paused") PAUSED,
    @SerialName("retired") RETIRED,
}

@Serializable
enum class RedistributionMode {
    @SerialName("allowed") ALLOWED,
    @SerialName("structured-data-only") STRUCTURED_DATA_ONLY,
    @SerialName("link-only") LINK_ONLY,
    @SerialName("review-required") REVIEW_REQUIRED,
}
