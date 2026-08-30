package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanonicalCatalogQueryTest {
    @Test
    fun search_matchesHeadphoneCreatorTargetAndSource() {
        val snapshot = snapshot()

        assertThat(CanonicalCatalogQuery.search(snapshot, CanonicalCatalogFilters(query = "edition xs")))
            .hasSize(1)
        assertThat(CanonicalCatalogQuery.search(snapshot, CanonicalCatalogFilters(query = "oratory")))
            .hasSize(1)
        assertThat(CanonicalCatalogQuery.search(snapshot, CanonicalCatalogFilters(query = "harman")))
            .hasSize(1)
        assertThat(
            CanonicalCatalogQuery.search(
                snapshot,
                CanonicalCatalogFilters(sourceKinds = setOf(EqSourceKind.COMMUNITY)),
            ),
        ).isEmpty()
    }

    @Test
    fun search_canReturnHistoricalRevisionMatches() {
        val snapshot = snapshot()

        val latestOnly = CanonicalCatalogQuery.search(
            snapshot,
            CanonicalCatalogFilters(query = "legacy", latestOnly = true),
        )
        assertThat(latestOnly).isEmpty()

        val allRevisions = CanonicalCatalogQuery.search(
            snapshot,
            CanonicalCatalogFilters(query = "legacy", latestOnly = false),
        )
        assertThat(allRevisions).hasSize(1)
        assertThat(allRevisions.single().matchingRevisionIds).containsExactly("rev-old")
    }

    @Test
    fun generalPresetIsSearchableWithoutHeadphoneIdentity() {
        val general = generalProfile()
        val snapshot = snapshot().copy(profiles = snapshot().profiles + general)

        assertThat(CanonicalCatalogQuery.search(snapshot, CanonicalCatalogFilters(query = "bass boost")))
            .extracting { it.profile.canonicalProfileId }
            .containsExactly("general-bass-boost")
        assertThat(
            CanonicalCatalogQuery.search(
                snapshot,
                CanonicalCatalogFilters(
                    scopes = setOf(EqProfileScope.GENERAL),
                    purposes = setOf(EqPresetPurpose.EFFECT),
                ),
            ),
        ).extracting { it.profile.canonicalProfileId }
            .containsExactly("general-bass-boost")
        assertThat(
            CanonicalCatalogQuery.search(
                snapshot,
                CanonicalCatalogFilters(scopes = setOf(EqProfileScope.HEADPHONE)),
            ),
        ).extracting { it.profile.canonicalProfileId }
            .containsExactly("hifiman-edition-xs-oratory-harman")
    }

    @Test
    fun facetsIncludeScopeAndPurpose() {
        val base = snapshot().profiles.single()
        val duplicateCase = base.copy(canonicalProfileId = "second", creator = "ORATORY1990")
        val general = generalProfile()
        val snapshot = snapshot().copy(profiles = listOf(base, duplicateCase, general))

        assertThat(CanonicalCatalogQuery.availableCreators(snapshot))
            .containsExactly("oratory1990", "EQ Library Community")
        assertThat(CanonicalCatalogQuery.availableTargets(snapshot)).containsExactly("Harman 2018")
        assertThat(CanonicalCatalogQuery.availableSourceKinds(snapshot))
            .containsExactly(EqSourceKind.CREATOR, EqSourceKind.COMMUNITY)
        assertThat(CanonicalCatalogQuery.availableScopes(snapshot))
            .containsExactly(EqProfileScope.HEADPHONE, EqProfileScope.GENERAL)
        assertThat(CanonicalCatalogQuery.availablePurposes(snapshot))
            .containsExactly(EqPresetPurpose.CORRECTION_TUNING, EqPresetPurpose.EFFECT)
    }

    private fun snapshot(): CatalogSnapshot {
        val source = EqSourceReference(
            sourceId = "oratory1990",
            sourceKind = EqSourceKind.CREATOR,
            sourceRecordId = "edition-xs",
            url = "https://example.com/edition-xs",
            creator = "oratory1990",
            provenanceTier = ProvenanceTier.AUTHORITATIVE,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            isPrimary = true,
        )
        val old = EqRevision(
            revisionId = "rev-old",
            acousticFingerprint = "old",
            preampGainDb = -5.0,
            filters = listOf(EqFilter(EqFilterType.PEAK, 100.0, 2.0, 1.0)),
            sourceReferences = listOf(source),
            sourceVersionLabel = "Legacy preset",
            isLatest = false,
        )
        val latest = EqRevision(
            revisionId = "rev-new",
            acousticFingerprint = "new",
            preampGainDb = -5.0,
            filters = listOf(EqFilter(EqFilterType.PEAK, 110.0, 2.0, 1.0)),
            sourceReferences = listOf(source),
            soundImpactSummary = "Adds sub-bass while smoothing upper mids.",
            isLatest = true,
        )
        return CatalogSnapshot(
            schemaVersion = 1,
            generatedAt = "2026-08-29T00:00:00Z",
            sourceRegistryVersion = "test",
            profiles = listOf(
                CanonicalEqProfile(
                    canonicalProfileId = "hifiman-edition-xs-oratory-harman",
                    headphone = HeadphoneIdentity("HIFIMAN", "Edition XS"),
                    creator = "oratory1990",
                    target = EqTarget("Harman 2018", EqTargetKind.CREATOR_TARGET),
                    tuningLabel = "Default",
                    revisions = listOf(old, latest),
                ),
            ),
        )
    }

    private fun generalProfile(): CanonicalEqProfile {
        val source = EqSourceReference(
            sourceId = "community",
            sourceKind = EqSourceKind.COMMUNITY,
            sourceRecordId = "bass-boost",
            url = "https://example.com/bass-boost",
            creator = "EQ Library Community",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            isPrimary = true,
        )
        return CanonicalEqProfile(
            canonicalProfileId = "general-bass-boost",
            scope = EqProfileScope.GENERAL,
            purpose = EqPresetPurpose.EFFECT,
            creator = "EQ Library Community",
            target = EqTarget(null, EqTargetKind.UNKNOWN),
            tuningLabel = "Bass Boost",
            revisions = listOf(
                EqRevision(
                    revisionId = "general-rev",
                    acousticFingerprint = "general-fingerprint",
                    preampGainDb = -3.0,
                    filters = listOf(EqFilter(EqFilterType.LOW_SHELF, 100.0, 3.0, 0.7)),
                    sourceReferences = listOf(source),
                    isLatest = true,
                ),
            ),
        )
    }
}
