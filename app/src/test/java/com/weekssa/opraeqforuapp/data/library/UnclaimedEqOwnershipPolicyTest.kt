package com.weekssa.opraeqforuapp.data.library

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipEntity
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.library.UnclaimedEqParsedContent
import org.junit.Test

class UnclaimedEqOwnershipPolicyTest {
    @Test
    fun `owned artifact becomes unclaimed only when its profile is not globally claimed`() {
        val claimed = ownership(
            documentUri = "content://eq-library/claimed",
            profileId = "profile:claimed",
        )
        val legacy = ownership(
            documentUri = "content://eq-library/legacy",
            profileId = "profile:legacy",
            fileName = "Old naming - creator.xml",
            productId = "old-product-id",
        )

        val unresolved = unresolvedOwnedArtifacts(
            ownerships = listOf(claimed, legacy),
            claimedProfileIds = setOf("profile:claimed"),
        )

        assertThat(unresolved).containsExactly(legacy)
    }

    @Test
    fun `destination changes cannot create false unclaimed items`() {
        val artifact = ownership(
            documentUri = "content://eq-library/stable",
            profileId = "profile:stable",
        )
        val globalMyEqsProfileIds = setOf("profile:stable")

        // There is intentionally no destination argument in the ownership policy. Whether UAPP,
        // Black Pearl, FiiO, or another target is active cannot change this result.
        assertThat(unresolvedOwnedArtifacts(listOf(artifact), globalMyEqsProfileIds)).isEmpty()
        assertThat(unresolvedOwnedArtifacts(listOf(artifact), globalMyEqsProfileIds)).isEmpty()
    }

    @Test
    fun `one exact owned document produces at most one unresolved row`() {
        val first = ownership(
            documentUri = "content://eq-library/same-document",
            profileId = "profile:old-a",
            exportedAtMillis = 10,
        )
        val duplicateHistoricalObservation = first.copy(
            profileId = "profile:old-b",
            exportedAtMillis = 20,
        )

        val unresolved = unresolvedOwnedArtifacts(
            ownerships = listOf(first, duplicateHistoricalObservation),
            claimedProfileIds = emptySet(),
        )

        assertThat(unresolved).hasSize(1)
        assertThat(unresolved.single().documentUri).isEqualTo(first.documentUri)
    }

    @Test
    fun `distinct unclaimed app-owned documents remain independently discoverable`() {
        val a = ownership(documentUri = "content://eq-library/a", profileId = "profile:a")
        val b = ownership(documentUri = "content://eq-library/b", profileId = "profile:b")

        assertThat(unresolvedOwnedArtifacts(listOf(a, b), emptySet())).containsExactly(a, b).inOrder()
    }

    @Test
    fun `recovered Personal EQ preserves preamp and every parsed band value`() {
        val bands = listOf(
            OpraBand(type = "peak_dip", frequency = 123.456, gainDb = -3.21, q = 1.234, slope = null),
            OpraBand(type = "low_shelf", frequency = 55.5, gainDb = 2.75, q = 0.707, slope = 0.9),
        )
        val content = UnclaimedEqParsedContent(
            suggestedName = "Legacy Name",
            preampGainDb = -6.125,
            bands = bands,
        )

        val recovered = buildRecoveredPersonalProfile(
            captureId = "fixed-id",
            originalFileName = "legacy.xml",
            content = content,
        )

        assertThat(recovered.id).isEqualTo("personal-eq:fixed-id")
        assertThat(recovered.productId).isEqualTo("personal-product:fixed-id")
        assertThat(recovered.preampGainDb).isEqualTo(-6.125)
        assertThat(recovered.bands).containsExactlyElementsIn(bands).inOrder()
        assertThat(recovered.details).contains("Recovered legacy EQ")
        assertThat(recovered.details).contains("legacy.xml")
        assertThat(recovered.author).isEqualTo("Personal")
    }

    private fun ownership(
        documentUri: String,
        profileId: String,
        fileName: String = "Preset.xml",
        productId: String = "product:1",
        exportedAtMillis: Long = 1,
    ) = ExportOwnershipEntity(
        documentUri = documentUri,
        treeUri = "content://eq-library/tree",
        relativeDirectory = "Manufacturer/Model",
        profileId = profileId,
        productId = productId,
        fileName = fileName,
        exportedFingerprint = "fingerprint",
        exportedContentHash = "hash",
        exportedAtMillis = exportedAtMillis,
    )
}
