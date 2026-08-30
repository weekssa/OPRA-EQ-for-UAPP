package com.weekssa.opraeqforuapp.domain.library

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EqPresetClassificationTest {
    @Test
    fun defaultHeadphoneCorrectionUsesExistingLineageAlgorithm() {
        val headphone = HeadphoneIdentity("Sennheiser", "HD 650")
        val target = EqTarget("Harman", EqTargetKind.EXPLICIT_TARGET)

        val legacy = EqFingerprint.lineage(headphone, "Creator", target, "Neutral")
        val classified = EqFingerprint.lineage(
            scope = EqProfileScope.HEADPHONE,
            purpose = EqPresetPurpose.CORRECTION_TUNING,
            headphone = headphone,
            creator = "Creator",
            target = target,
            tuningLabel = "Neutral",
        )

        assertEquals(legacy, classified)
    }

    @Test
    fun effectAndGenreWithSameAcousticsRemainDistinctProfiles() {
        val effect = candidate(EqPresetPurpose.EFFECT)
        val genre = candidate(EqPresetPurpose.GENRE)

        val profiles = EqCatalogBuilder().build(listOf(effect, genre))

        assertThat(profiles).hasSize(2)
        assertThat(profiles.map { it.purpose })
            .containsExactly(EqPresetPurpose.EFFECT, EqPresetPurpose.GENRE)
        assertThat(profiles.map { it.canonicalProfileId }.distinct()).hasSize(2)
    }

    @Test
    fun invalidGeneralPersonalCommunityCandidateIsRejected() {
        val invalid = candidate(EqPresetPurpose.EFFECT).copy(
            purpose = EqPresetPurpose.PERSONAL_COMMUNITY,
        )

        assertThrows(IllegalArgumentException::class.java) {
            EqCatalogBuilder().build(listOf(invalid))
        }
    }

    private fun candidate(purpose: EqPresetPurpose) = EqCandidate(
        scope = EqProfileScope.GENERAL,
        purpose = purpose,
        creator = "Creator",
        target = EqTarget(null, EqTargetKind.UNKNOWN),
        tuningLabel = "Same label",
        preampGainDb = -3.0,
        filters = listOf(EqFilter(EqFilterType.LOW_SHELF, 100.0, 3.0, 0.7)),
        sourceReference = EqSourceReference(
            sourceId = "community",
            sourceKind = EqSourceKind.COMMUNITY,
            sourceRecordId = purpose.name,
            url = "https://example.com/${purpose.name.lowercase()}",
            creator = "Creator",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
            isPrimary = true,
        ),
    )
}
