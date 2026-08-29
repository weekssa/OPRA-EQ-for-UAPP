package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SourceDisplayLabelTest {
    @Test
    fun qualifiedGithubSourceDoesNotExposeInternalSlug() {
        val source = EqSourceReference(
            sourceId = "mrchillstorm-headphone-target",
            sourceKind = EqSourceKind.REPOSITORY,
            sourceRecordId = "repo:zero2",
            url = "https://github.com/MrChillStorm/Headphone_Target",
            creator = "MrChillStorm",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            isPrimary = true,
        )
        val profile = CanonicalEqProfile(
            canonicalProfileId = "zero2-iso226",
            headphone = HeadphoneIdentity("7Hz", "Zero:2"),
            creator = "MrChillStorm",
            target = EqTarget("ISO 226:2023 85 phon (author-defined)", EqTargetKind.EXPLICIT_TARGET),
            tuningLabel = "ISO 226:2023 85 phon",
            revisions = listOf(
                EqRevision(
                    revisionId = "rev-1",
                    acousticFingerprint = "fingerprint-1",
                    preampGainDb = -7.2,
                    filters = listOf(EqFilter(EqFilterType.PEAK, 1000.0, -2.0, 1.0)),
                    sourceReferences = listOf(source),
                    soundImpactSummary = "Reduces midrange energy.",
                    isLatest = true,
                ),
            ),
        )

        val legacy = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-08-29T22:00:00Z", "test", listOf(profile)),
        )
        val details = legacy.profiles.single().details.orEmpty()

        assertThat(details).contains("Source: MrChillStorm")
        assertThat(details).doesNotContain("mrchillstorm-headphone-target")
    }
}
