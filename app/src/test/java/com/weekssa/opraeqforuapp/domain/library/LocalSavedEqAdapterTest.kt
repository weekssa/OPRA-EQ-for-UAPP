package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocalSavedEqAdapterTest {
    @Test
    fun prioritySequenceChangesRevisionIdButNotAcousticFingerprint() {
        val first = profile(listOf(
            OpraBand("peak_dip", 200.0, 1.0, 1.0, null),
            OpraBand("peak_dip", 2_000.0, -1.0, 1.0, null),
        ))
        val reversed = profile(first.bands!!.reversed())

        val firstSnapshot = adapt(first)
        val reversedSnapshot = adapt(reversed)

        assertEquals(firstSnapshot.revision.acousticFingerprint, reversedSnapshot.revision.acousticFingerprint)
        assertEquals(first.bands!!.map { it.frequency }, firstSnapshot.revision.filters.map { it.frequencyHz })
        assertEquals(reversed.bands!!.map { it.frequency }, reversedSnapshot.revision.filters.map { it.frequencyHz })
        org.junit.Assert.assertNotEquals(firstSnapshot.revision.revisionId, reversedSnapshot.revision.revisionId)
    }

    @Test
    fun preservesFullOrderedFilterSemanticsNullsAndSeparateDerivedHeadroom() {
        val profile = OpraEqProfile(
            id = "personal-eq:1",
            productId = "personal-product:1",
            author = "Personal",
            details = "Captured from a DAC · Slot 2",
            link = null,
            profileType = "parametric_eq",
            preampGainDb = null,
            bands = listOf(
                OpraBand("Odd Filter", 800.0, null, null, null),
                OpraBand("low_shelf", 90.0, 2.5, 0.7, null),
            ),
            eqLibrarySafetyHeadroomDb = -3.25,
        )
        val source = EqSourceReference(
            sourceId = "device_capture",
            sourceKind = EqSourceKind.DEVICE_CAPTURE,
            sourceRecordId = "capture-1",
            url = null,
            creator = null,
            provenanceTier = ProvenanceTier.AUTHORITATIVE,
            redistributionPolicy = RedistributionPolicy.UNKNOWN_REVIEW,
            lastVerifiedAtEpochSeconds = 123L,
            isPrimary = true,
        )

        val snapshot = LocalSavedEqAdapter.adapt(
            profile = profile,
            displayName = "Measured at home",
            headphone = null,
            target = EqTarget(name = null, kind = EqTargetKind.UNKNOWN),
            sourceReference = source,
            verificationStatus = VerificationStatus.VERIFIED,
            observedAtEpochSeconds = 123L,
        )

        assertNotNull(snapshot)
        val revision = snapshot!!.revision
        assertNull(revision.preampGainDb)
        assertEquals(-3.25, revision.eqLibrarySafetyHeadroomDb!!, 0.0)
        assertEquals(listOf(EqFilterType.OTHER, EqFilterType.LOW_SHELF), revision.filters.map { it.type })
        assertEquals("Odd Filter", revision.filters.first().sourceType)
        assertNull(revision.filters.first().gainDb)
        assertNull(revision.filters.first().q)
        assertNull(revision.filters.first().slope)
        assertEquals(90.0, revision.filters.last().frequencyHz, 0.0)
        assertNull(snapshot.tuningLabel)
        assertNull(revision.sourceUpdatedAtEpochSeconds)
    }

    @Test
    fun rejectsMalformedActiveBandInsteadOfDroppingIt() {
        val snapshot = LocalSavedEqAdapter.adapt(
            profile = OpraEqProfile(
                id = "bad",
                productId = "personal-product:bad",
                author = null,
                details = null,
                link = null,
                profileType = "parametric_eq",
                preampGainDb = 0.0,
                bands = listOf(OpraBand("peak_dip", Double.NaN, 1.0, 1.0, null)),
            ),
            displayName = "Malformed",
            headphone = null,
            target = EqTarget(name = null, kind = EqTargetKind.UNKNOWN),
            sourceReference = EqSourceReference(
                sourceId = "personal_import",
                sourceKind = EqSourceKind.PERSONAL_IMPORT,
                sourceRecordId = "bad",
                url = null,
                creator = null,
                provenanceTier = ProvenanceTier.NEEDS_REVIEW,
                redistributionPolicy = RedistributionPolicy.UNKNOWN_REVIEW,
            ),
            verificationStatus = VerificationStatus.UNVERIFIED,
            observedAtEpochSeconds = 456L,
        )

        assertNull(snapshot)
    }

    private fun adapt(profile: OpraEqProfile) = requireNotNull(
        LocalSavedEqAdapter.adapt(
            profile = profile,
            displayName = "Profile",
            headphone = null,
            target = EqTarget(name = null, kind = EqTargetKind.UNKNOWN),
            sourceReference = EqSourceReference(
                sourceId = "personal_import",
                sourceKind = EqSourceKind.PERSONAL_IMPORT,
                sourceRecordId = profile.id,
                url = null,
                creator = null,
                provenanceTier = ProvenanceTier.NEEDS_REVIEW,
                redistributionPolicy = RedistributionPolicy.UNKNOWN_REVIEW,
            ),
            verificationStatus = VerificationStatus.UNVERIFIED,
            observedAtEpochSeconds = 789L,
        ),
    )

    private fun profile(bands: List<OpraBand>) = OpraEqProfile(
        id = "ordered-profile",
        productId = "personal-product:ordered",
        author = null,
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = -2.0,
        bands = bands,
    )
}
