package com.weekssa.opraeqforuapp.domain.library

import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConversionException
import com.weekssa.opraeqforuapp.domain.conversion.ToneBoostersConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    @Test
    fun `primary provenance matches the candidate that supplied revision filters`() {
        val candidates = listOf(
            candidate(VerificationStatus.VERIFIED, "a-reference-sorts-first").copy(creator = null),
            candidate(VerificationStatus.VERIFIED, "z-filter-source"),
        )

        val revision = EqCatalogBuilder().build(candidates).single().latestRevision

        assertEquals("z-filter-source", revision.sourceReferences.single { it.isPrimary }.sourceId)
    }

    @Test
    fun `deduplicated source reference retains the chosen primary candidate metadata`() {
        val earlierReference = candidate(VerificationStatus.VERIFIED, "same-source").copy(
            sourceReference = candidate(VerificationStatus.VERIFIED, "same-source").sourceReference.copy(
                sourceRecordId = "same-record",
                url = "https://example.com/same-record",
                sourceDataset = "stale-dataset-label",
                isPrimary = false,
            ),
        )
        val selectedPrimary = earlierReference.copy(
            sourceReference = earlierReference.sourceReference.copy(
                sourceDataset = "selected-primary-dataset",
                isPrimary = true,
            ),
        )

        val references = EqCatalogBuilder().build(listOf(earlierReference, selectedPrimary))
            .single().latestRevision.sourceReferences

        assertEquals(1, references.size)
        assertEquals("selected-primary-dataset", references.single().sourceDataset)
        assertTrue(references.single().isPrimary)
    }

    @Test
    fun `secondary OPRA reference cannot authorize truncation of another source filter order`() {
        val bands = (1..11).map { index ->
            EqFilter(EqFilterType.PEAK, 100.0 + index, 0.0, 1.0)
        }
        val nonOpra = candidate(VerificationStatus.VERIFIED, "a-structured-source").copy(
            filters = bands,
            sourceReference = EqSourceReference(
                sourceId = "a-structured-source",
                sourceKind = EqSourceKind.STRUCTURED_CATALOG,
                sourceRecordId = "source-record",
                sourceVendorId = "other-vendor",
                sourceProductId = "other-product",
                url = "https://example.com/source",
                creator = "Creator",
                provenanceTier = ProvenanceTier.AUTHORITATIVE,
                redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
                isPrimary = true,
            ),
        )
        val opra = candidate(VerificationStatus.VERIFIED, "opra").copy(
            filters = bands,
            sourceReference = EqSourceReference(
                sourceId = "opra",
                sourceKind = EqSourceKind.STRUCTURED_CATALOG,
                sourceRecordId = "opra-record",
                sourceVendorId = "opra-vendor",
                sourceProductId = "opra-product",
                url = "https://example.com/opra",
                creator = "Creator",
                provenanceTier = ProvenanceTier.AUTHORITATIVE,
                redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
                isPrimary = true,
            ),
        )

        val canonical = EqCatalogBuilder().build(listOf(nonOpra, opra)).single()
        val projected = CanonicalLegacyCatalogAdapter.adapt(
            CatalogSnapshot(1, "2026-09-23T00:00:00Z", "test", listOf(canonical)),
        ).profiles.single()

        assertEquals("a-structured-source", canonical.latestRevision.sourceReferences.single { it.isPrimary }.sourceId)
        assertNull(projected.bandOrderProvenance)
        assertThrows(ToneBoostersConversionException::class.java) {
            ToneBoostersConverter.convert(projected, "Mixed-source over-budget profile")
        }
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
