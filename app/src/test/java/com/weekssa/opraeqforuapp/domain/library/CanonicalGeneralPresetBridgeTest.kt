package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanonicalGeneralPresetBridgeTest {
    @Test
    fun generalPresetRemainsCanonicalButDoesNotBecomeFakeHeadphone() {
        val headphone = profile(
            id = "headphone-profile",
            headphone = HeadphoneIdentity("Sennheiser", "HD 650"),
            scope = EqProfileScope.HEADPHONE,
            purpose = EqPresetPurpose.CORRECTION_TUNING,
            tuningLabel = "Neutral",
        )
        val general = profile(
            id = "general-bass-boost",
            headphone = null,
            scope = EqProfileScope.GENERAL,
            purpose = EqPresetPurpose.EFFECT,
            tuningLabel = "Bass Boost",
        )
        val snapshot = CatalogSnapshot(
            schemaVersion = 1,
            generatedAt = "2026-08-30T00:00:00Z",
            sourceRegistryVersion = "test",
            profiles = listOf(headphone, general),
        )

        val legacy = CanonicalLegacyCatalogAdapter.adapt(snapshot)

        assertThat(snapshot.profiles).hasSize(2)
        assertThat(snapshot.profiles.single { it.scope == EqProfileScope.GENERAL }.tuningLabel)
            .isEqualTo("Bass Boost")
        assertThat(legacy.products).hasSize(1)
        assertThat(legacy.products.single().name).isEqualTo("HD 650")
        assertThat(legacy.profiles).hasSize(1)
        assertThat(legacy.profiles.single().details).doesNotContain("Bass Boost")
    }

    private fun profile(
        id: String,
        headphone: HeadphoneIdentity?,
        scope: EqProfileScope,
        purpose: EqPresetPurpose,
        tuningLabel: String,
    ) = CanonicalEqProfile(
        canonicalProfileId = id,
        headphone = headphone,
        scope = scope,
        purpose = purpose,
        creator = "Creator",
        target = EqTarget(null, EqTargetKind.UNKNOWN),
        tuningLabel = tuningLabel,
        revisions = listOf(
            EqRevision(
                revisionId = "$id-rev",
                acousticFingerprint = "$id-fingerprint",
                preampGainDb = -3.0,
                filters = listOf(EqFilter(EqFilterType.PEAK, 1000.0, -2.0, 1.0)),
                sourceReferences = listOf(
                    EqSourceReference(
                        sourceId = "community",
                        sourceKind = EqSourceKind.COMMUNITY,
                        sourceRecordId = id,
                        url = "https://example.com/$id",
                        creator = "Creator",
                        provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
                        redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
                        isPrimary = true,
                    ),
                ),
                isLatest = true,
            ),
        ),
    )
}
