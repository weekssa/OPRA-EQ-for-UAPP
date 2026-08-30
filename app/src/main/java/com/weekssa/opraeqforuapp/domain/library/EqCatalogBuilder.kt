package com.weekssa.opraeqforuapp.domain.library

class EqCatalogBuilder {
    fun build(candidates: List<EqCandidate>): List<CanonicalEqProfile> {
        if (candidates.isEmpty()) return emptyList()
        candidates.forEach { candidate ->
            require(candidate.hasValidClassification()) {
                "Invalid EQ preset classification: ${candidate.scope}/${candidate.purpose}"
            }
        }

        return candidates
            .groupBy(::candidateContextKey)
            .values
            .flatMap(::buildForContext)
            .sortedWith(
                compareBy<CanonicalEqProfile> { it.scope.name }
                    .thenBy { it.purpose.name }
                    .thenBy { it.headphone?.manufacturer.orEmpty().lowercase() }
                    .thenBy { it.headphone?.model.orEmpty().lowercase() }
                    .thenBy { it.creator.orEmpty().lowercase() }
                    .thenBy { it.target.name.orEmpty().lowercase() }
                    .thenBy { it.tuningLabel.orEmpty().lowercase() },
            )
    }

    private fun candidateContextKey(candidate: EqCandidate): String = listOf(
        candidate.scope.name,
        candidate.purpose.name,
        candidate.headphone?.normalizedKey.orEmpty(),
    ).joinToString("|")

    private fun buildForContext(candidates: List<EqCandidate>): List<CanonicalEqProfile> {
        val acousticClusters = candidates
            .groupBy { candidate -> EqFingerprint.acoustic(candidate.preampGainDb, candidate.filters) }
            .map { (acousticFingerprint, clusterCandidates) ->
                buildRevisionCluster(acousticFingerprint, clusterCandidates)
            }

        return acousticClusters
            .groupBy { cluster -> lineageFingerprint(cluster.primary) }
            .map { (lineageFingerprint, clusters) -> buildProfile(lineageFingerprint, clusters) }
    }

    private fun buildRevisionCluster(
        acousticFingerprint: String,
        candidates: List<EqCandidate>,
    ): RevisionCluster {
        val primary = candidates.minWithOrNull(primaryCandidateComparator)
            ?: error("Revision cluster cannot be empty")
        val references = candidates
            .map(EqCandidate::sourceReference)
            .distinctBy { reference -> listOf(reference.sourceId, reference.sourceRecordId, reference.url) }
            .sortedWith(
                compareBy<EqSourceReference> { provenanceRank(it.provenanceTier) }
                    .thenByDescending(EqSourceReference::isPrimary)
                    .thenBy { it.sourceId },
            )
            .mapIndexed { index, reference -> reference.copy(isPrimary = index == 0) }
        val verificationStatus = if (candidates.any { it.verificationStatus == VerificationStatus.VERIFIED }) {
            VerificationStatus.VERIFIED
        } else {
            VerificationStatus.UNVERIFIED
        }

        return RevisionCluster(
            acousticFingerprint = acousticFingerprint,
            primary = primary,
            sourceReferences = references,
            verificationStatus = verificationStatus,
        )
    }

    private fun buildProfile(
        lineageFingerprint: String,
        clusters: List<RevisionCluster>,
    ): CanonicalEqProfile {
        val profilePrimary = clusters.minWithOrNull(
            compareBy<RevisionCluster> { provenanceRank(it.primary.sourceReference.provenanceTier) }
                .thenByDescending { it.primary.sourceReference.isPrimary }
                .thenBy { it.primary.sourceReference.sourceId },
        ) ?: error("Profile cluster cannot be empty")

        val ordered = clusters.sortedWith(
            compareBy<RevisionCluster> { revisionTimestamp(it) ?: Long.MIN_VALUE }
                .thenBy(RevisionCluster::acousticFingerprint),
        )
        val latestFingerprint = ordered.last().acousticFingerprint
        val revisions = ordered.map { cluster ->
            EqRevision(
                revisionId = EqFingerprint.revisionId(lineageFingerprint, cluster.acousticFingerprint),
                acousticFingerprint = cluster.acousticFingerprint,
                preampGainDb = cluster.primary.preampGainDb,
                filters = cluster.primary.filters,
                sourceReferences = cluster.sourceReferences,
                sourceVersionLabel = cluster.primary.sourceVersionLabel,
                soundImpactSummary = cluster.primary.soundImpactSummary,
                verificationStatus = cluster.verificationStatus,
                firstSeenAtEpochSeconds = cluster.sourceReferences.mapNotNull(EqSourceReference::discoveredAtEpochSeconds).minOrNull(),
                sourceUpdatedAtEpochSeconds = cluster.sourceReferences.mapNotNull { it.updatedAtEpochSeconds ?: it.publishedAtEpochSeconds }.maxOrNull(),
                isLatest = cluster.acousticFingerprint == latestFingerprint,
            )
        }

        return CanonicalEqProfile(
            canonicalProfileId = lineageFingerprint,
            headphone = profilePrimary.primary.headphone,
            scope = profilePrimary.primary.scope,
            purpose = profilePrimary.primary.purpose,
            creator = profilePrimary.primary.creator,
            target = profilePrimary.primary.target,
            tuningLabel = profilePrimary.primary.tuningLabel,
            revisions = revisions,
        )
    }

    private fun lineageFingerprint(candidate: EqCandidate): String {
        val hasLineageMetadata = !candidate.creator.isNullOrBlank() ||
            !candidate.target.name.isNullOrBlank() ||
            !candidate.tuningLabel.isNullOrBlank()
        if (hasLineageMetadata) {
            return EqFingerprint.lineage(
                scope = candidate.scope,
                purpose = candidate.purpose,
                headphone = candidate.headphone,
                creator = candidate.creator,
                target = candidate.target,
                tuningLabel = candidate.tuningLabel,
            )
        }

        val sourceFallback = candidate.sourceReference.sourceRecordId
            ?: candidate.sourceReference.url
            ?: candidate.sourceReference.sourceId
        return EqFingerprint.lineage(
            scope = candidate.scope,
            purpose = candidate.purpose,
            headphone = candidate.headphone,
            creator = "unknown:${candidate.sourceReference.sourceId}:$sourceFallback",
            target = candidate.target,
            tuningLabel = candidate.tuningLabel,
        )
    }

    private fun revisionTimestamp(cluster: RevisionCluster): Long? = cluster.sourceReferences
        .mapNotNull { it.updatedAtEpochSeconds ?: it.publishedAtEpochSeconds ?: it.discoveredAtEpochSeconds }
        .maxOrNull()

    private val primaryCandidateComparator =
        compareBy<EqCandidate> { provenanceRank(it.sourceReference.provenanceTier) }
            .thenByDescending { it.sourceReference.isPrimary }
            .thenByDescending { !it.creator.isNullOrBlank() }
            .thenBy { it.sourceReference.sourceId }

    private fun provenanceRank(tier: ProvenanceTier): Int = when (tier) {
        ProvenanceTier.AUTHORITATIVE -> 0
        ProvenanceTier.MEASUREMENT_DERIVED -> 1
        ProvenanceTier.TRACEABLE_COMMUNITY -> 2
        ProvenanceTier.MIRROR -> 3
        ProvenanceTier.NEEDS_REVIEW -> 4
    }

    private data class RevisionCluster(
        val acousticFingerprint: String,
        val primary: EqCandidate,
        val sourceReferences: List<EqSourceReference>,
        val verificationStatus: VerificationStatus,
    )
}
