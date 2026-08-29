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
        val normalizedCreators = filters.creators.mapTo(mutableSetOf())(::normalize)
        val normalizedTargets = filters.targets.mapTo(mutableSetOf())(::normalize)

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
                matchingRevisionIds = matchingRevisions.map(EqRevision::revisionId),
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
        snapshot.profiles.mapNotNull(CanonicalEqProfile::creator)
            .filter(String::isNotBlank)
            .distinctBy(::normalize)
            .sortedBy(::normalize)

    fun availableTargets(snapshot: CatalogSnapshot): List<String> =
        snapshot.profiles.mapNotNull { it.target.name }
            .filter(String::isNotBlank)
            .distinctBy(::normalize)
            .sortedBy(::normalize)

    fun availableSourceKinds(snapshot: CatalogSnapshot): Set<EqSourceKind> =
        snapshot.profiles.asSequence()
            .flatMap { it.revisions.asSequence() }
            .flatMap { it.sourceReferences.asSequence() }
            .map(EqSourceReference::sourceKind)
            .toSet()

    private fun matchesQuery(profile: CanonicalEqProfile, revision: EqRevision, query: String): Boolean {
        val values = buildList {
            add(profile.headphone.manufacturer)
            add(profile.headphone.model)
            profile.headphone.variant?.let(::add)
            profile.headphone.padsOrMode?.let(::add)
            profile.creator?.let(::add)
            profile.target.name?.let(::add)
            profile.tuningLabel?.let(::add)
            revision.sourceVersionLabel?.let(::add)
            revision.soundImpactSummary?.let(::add)
            revision.sourceReferences.forEach { source ->
                add(source.sourceId)
                source.creator?.let(::add)
            }
        }
        return values.any { normalize(it).contains(query) }
    }

    private fun normalize(value: String): String =
        value.lowercase().trim().replace(Regex("\\s+"), " ")
}
