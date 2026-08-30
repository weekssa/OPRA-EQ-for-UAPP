package com.weekssa.opraeqforuapp.domain.library

import org.junit.Assert.assertEquals
import org.junit.Test

class EqCatalogVerificationTest {
    @Test
    fun `community candidate remains unverified when it is the only matching source`() {
        val profiles = EqCatalogBuilder().build(
            listOf(candidate(VerificationStatus.UNVERIFIED, "reddit-audio")),
        )

        assertEquals(1, profiles.size)
        assertEquals(VerificationStatus.UNVERIFIED, profiles.single().latestRevision.verificationStatus)
    }

    @Test
    fun `verified same-fingerprint evidence promotes the shared acoustic revision`() {
        val profiles = EqCatalogBuilder().build(
            listOf(
                candidate(VerificationStatus.UNVERIFIED, "reddit-audio"),
                candidate(VerificationStatus.VERIFIED, "reviewed-source"),
            ),
        )

        val revision = profiles.single().latestRevision
        assertEquals(1, profiles.single().revisions.size)
        assertEquals(VerificationStatus.VERIFIED, revision.verificationStatus)
        assertEquals(2, revision.sourceReferences.size)
    }

    private fun candidate(
        verificationStatus: VerificationStatus,
        sourceId: String,
    ) = EqCandidate(
        headphone = HeadphoneIdentity("Example", "Headphone"),
        creator = "Creator",
        target = EqTarget(name = null, kind = EqTargetKind.UNKNOWN),
        tuningLabel = "Community tuning",
        preampGainDb = -3.0,
        filters = listOf(
            EqFilter(
                type = EqFilterType.PEAK,
                frequencyHz = 100.0,
                gainDb = 3.0,
                q = 0.7,
            ),
        ),
        sourceReference = EqSourceReference(
            sourceId = sourceId,
            sourceKind = EqSourceKind.COMMUNITY,
            sourceRecordId = "post-$sourceId",
            url = "https://example.com/$sourceId",
            creator = "Creator",
            provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
            redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
        ),
        verificationStatus = verificationStatus,
    )
}
