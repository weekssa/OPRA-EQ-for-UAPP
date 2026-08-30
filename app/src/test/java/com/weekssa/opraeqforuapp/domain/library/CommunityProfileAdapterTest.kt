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
    fun arbitraryCommunityFilterCountAndMissingPreampRemainSourceAuthentic() {
        val filters = (1..15).joinToString("\n") { index ->
            "Filter $index: ON PK Fc ${80 + index * 20} Hz Gain ${index / 10.0} dB Q 1.0"
        }
        val profile = CommunityProfileAdapter.adapt(
            CommunityProfileAdapter.Metadata(
                sourceId = "headphones-community",
                sourceRecordId = "post-15-band",
                sourceUrl = "https://example.com/post/15-band",
                manufacturer = "HiFiMAN",
                model = "Edition XS",
                creator = "example-user",
            ),
            filters,
        )!!

        assertNull(profile.latestRevision.preampGainDb)
        assertEquals(15, profile.latestRevision.filters.size)
        assertEquals(100.0, profile.latestRevision.filters.first().frequencyHz, 0.0001)
        assertEquals(380.0, profile.latestRevision.filters.last().frequencyHz, 0.0001)
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
