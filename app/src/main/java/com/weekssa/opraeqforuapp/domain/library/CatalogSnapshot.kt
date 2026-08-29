package com.weekssa.opraeqforuapp.domain.library

import kotlinx.serialization.Serializable

@Serializable
data class CatalogSnapshot(
    val schemaVersion: Int,
    val generatedAt: String,
    val sourceRegistryVersion: String,
    val profiles: List<CanonicalEqProfile>,
    val sources: List<SourceStatus> = emptyList(),
)

@Serializable
data class SourceStatus(
    val sourceId: String,
    val lifecycle: SourceLifecycle,
    val lastSuccessfulScanAt: String? = null,
    val lastAttemptAt: String? = null,
    val cursor: String? = null,
    val parserVersion: String? = null,
    val consecutiveFailures: Int = 0,
    val redistribution: RedistributionMode = RedistributionMode.REVIEW_REQUIRED,
)

@Serializable
enum class SourceLifecycle {
    PROPOSED,
    REVIEWING,
    ACTIVE,
    LINK_ONLY,
    PAUSED,
    RETIRED,
}

@Serializable
enum class RedistributionMode {
    ALLOWED,
    STRUCTURED_DATA_ONLY,
    LINK_ONLY,
    REVIEW_REQUIRED,
}
