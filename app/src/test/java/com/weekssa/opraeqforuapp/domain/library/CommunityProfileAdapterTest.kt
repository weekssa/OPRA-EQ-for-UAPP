package com.weekssa.opraeqforuapp.domain.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityProfileAdapterTest {
    private val eq = """
        Preamp: -5.5 dB
        Filter 1: ON PK Fc 105 Hz Gain 2.5 dB Q 0.70
        Filter 2: ON PK Fc 3100 Hz Gain -2.0 dB Q 1.20
    """.trimIndent()

    @Test
    fun traceableCommunityPresetPreservesAttributionAndLinkOnlyPolicy() {
        val profile = CommunityProfileAdapter.adapt(
            CommunityProfileAdapter.Metadata(
                sourceId = "head-fi",
                sourceRecordId = "post-123",
                sourceUrl = "https://example.com/post/123",
                manufacturer = "Sennheiser",
                model = "HD 650",
                creator = "example-user",
                discoveredAtEpochSeconds = 1234L,
            ),
            eq,
        )

        assertNotNull(profile)
        profile!!
        assertEquals(EqTargetKind.CUSTOM_USER, profile.target.kind)
        assertEquals("Community tuning", profile.tuningLabel)
        assertEquals(2, profile.latestRevision.filters.size)
        val source = profile.latestRevision.sourceReferences.single()
        assertEquals("head-fi", source.sourceId)
        assertEquals("example-user", source.creator)
        assertEquals(ProvenanceTier.TRACEABLE_COMMUNITY, source.provenanceTier)
        assertEquals(RedistributionPolicy.LINK_ONLY, source.redistributionPolicy)
        assertTrue(source.isPrimary)
    }

    @Test
    fun explicitTargetIsKeptWithoutGuessing() {
        val profile = CommunityProfileAdapter.adapt(
            CommunityProfileAdapter.Metadata(
                sourceId = "reddit-audio",
                sourceRecordId = "comment-99",
                sourceUrl = "https://example.com/comment/99",
                manufacturer = "HiFiMAN",
                model = "Edition XS",
                creator = "example-user",
                targetName = "Harman 2018",
            ),
            eq,
        )!!

        assertEquals(EqTargetKind.EXPLICIT_TARGET, profile.target.kind)
        assertEquals("Harman 2018", profile.target.name)
    }

    @Test
    fun missingTraceabilityIsRejected() {
        val profile = CommunityProfileAdapter.adapt(
            CommunityProfileAdapter.Metadata(
                sourceId = "head-fi",
                sourceRecordId = "post-123",
                sourceUrl = "",
                manufacturer = "Sennheiser",
                model = "HD 650",
                creator = "example-user",
            ),
            eq,
        )

        assertNull(profile)
    }
}
