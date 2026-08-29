package com.weekssa.opraeqforuapp.domain.library

data class CanonicalCatalogFilters(
    val query: String = "",
    val creators: Set<String> = emptySet(),
    val sourceKinds: Set<EqSourceKind> = emptySet(),
    val targets: Set<String> = emptySet(),
    val latestOnly: Boolean = true,
)

data class CanonicalProfileSearchResult(
    val profile: CanonicalEqProfile,
    val matchingRevisionIds: List<String>,
)

object CanonicalCatalogQuery {
    fun search(
        snapshot: CatalogSnapshot,
        filters: CanonicalCatalogFilters = CanonicalCatalogFilters(),
    ): List<CanonicalProfileSearchResult> {
        val normalizedQuery = normalize(filters.query)
        val normalizedCreators = filters.creators.mapTo(mutableSetOf()) { normalize(it) }
        val normalizedTargets = filters.targets.mapTo(mutableSetOf()) { normalize(it) }

        return snapshot.profiles.mapNotNull { profile ->
            if (normalizedCreators.isNotEmpty() && normalize(profile.creator.orEmpty()) !in normalizedCreators) {
                return@mapNotNull null
            }
            if (normalizedTargets.isNotEmpty() && normalize(profile.target.name.orEmpty()) !in normalizedTargets) {
                return@mapNotNull null
            }

            val revisions = if (filters.latestOnly) listOf(profile.latestRevision) else profile.revisions
            val matchingRevisions = revisions.filter { revision ->
                if (filters.sourceKinds.isNotEmpty() && revision.sourceReferences.none { it.sourceKind in filters.sourceKinds }) {
                    return@filter false
                }
                normalizedQuery.isEmpty() || matchesQuery(profile, revision, normalizedQuery)
            }
            if (matchingRevisions.isEmpty()) null else CanonicalProfileSearchResult(
                profile = profile,
                matchingRevisionIds = matchingRevisions.map { it.revisionId },
            )
        }.sortedWith(
            compareBy<CanonicalProfileSearchResult>(
                { normalize(it.profile.headphone.manufacturer) },
                { normalize(it.profile.headphone.model) },
                { normalize(it.profile.headphone.variant.orEmpty()) },
                { normalize(it.profile.creator.orEmpty()) },
                { normalize(it.profile.tuningLabel.orEmpty()) },
            ),
        )
    }

    fun availableCreators(snapshot: CatalogSnapshot): List<String> =
        snapshot.profiles.mapNotNull { it.creator }
            .filter { it.isNotBlank() }
            .distinctBy { normalize(it) }
            .sortedBy { normalize(it) }

    fun availableTargets(snapshot: CatalogSnapshot): List<String> =
        snapshot.profiles.mapNotNull { it.target.name }
            .filter { it.isNotBlank() }
            .distinctBy { normalize(it) }
            .sortedBy { normalize(it) }

    fun availableSourceKinds(snapshot: CatalogSnapshot): Set<EqSourceKind> =
        snapshot.profiles.asSequence()
            .flatMap { it.revisions.asSequence() }
            .flatMap { it.sourceReferences.asSequence() }
            .map { it.sourceKind }
            .toSet()

    private fun matchesQuery(profile: CanonicalEqProfile, revision: EqRevision, query: String): Boolean {
        val values = buildList {
            add(profile.headphone.manufacturer)
            add(profile.headphone.model)
            profile.headphone.variant?.let { add(it) }
            profile.headphone.padsOrMode?.let { add(it) }
            profile.creator?.let { add(it) }
            profile.target.name?.let { add(it) }
            profile.tuningLabel?.let { add(it) }
            revision.sourceVersionLabel?.let { add(it) }
            revision.soundImpactSummary?.let { add(it) }
            revision.sourceReferences.forEach { source ->
                add(source.sourceId)
                source.creator?.let { add(it) }
            }
        }
        return values.any { normalize(it).contains(query) }
    }

    private fun normalize(value: String): String =
        value.lowercase().trim().replace(Regex("\\s+"), " ")
}
