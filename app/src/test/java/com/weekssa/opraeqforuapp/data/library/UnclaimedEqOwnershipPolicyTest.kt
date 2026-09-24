package com.weekssa.opraeqforuapp.data.library

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipEntity
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileEntity
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
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
    fun `stale UAPP export remains visible without making another target recoverable`() {
        val profileId = "profile:claimed-but-not-uapp-representable"
        val productId = "product:exact"
        val staleUapp = ownership(
            documentUri = "content://eq-library/stale-uapp",
            profileId = profileId,
            productId = productId,
            relativeDirectory = "${ExportDevice.UAPP.folderName}/Maker/Model",
        )
        val otherTarget = staleUapp.copy(
            documentUri = "content://eq-library/other-target",
            relativeDirectory = "TRN Black Pearl/Maker/Model",
        )
        val anotherProduct = staleUapp.copy(
            documentUri = "content://eq-library/other-product",
            productId = "product:other",
        )

        val unresolved = unresolvedOwnedArtifacts(
            ownerships = listOf(staleUapp, otherTarget, anotherProduct),
            claimedProfileIds = setOf(profileId),
            unrepresentableUappProfiles = setOf(ManagedUappExportIdentity(productId, profileId)),
        )

        assertThat(unresolved).containsExactly(staleUapp)
    }

    @Test
    fun `only current unsupported undecodable or identity-mismatched snapshots flag stale exports`() {
        val codec = ManagedProfileSnapshotCodec()
        val unsupported = profile("unsupported", productId = "product:current", overUappLimit = true)
        val usable = profile("usable", productId = "product:current", overUappLimit = false)
        val removed = profile("removed", productId = "product:current", overUappLimit = true)
        val undecodable = managedProfile(usable, codec).copy(
            profileId = "undecodable",
            snapshotJson = "{malformed snapshot",
        )
        val mismatchedProfileId = managedProfile(usable, codec).copy(
            profileId = "stored-profile-id",
        )
        val mismatchedProductId = managedProfile(usable, codec).copy(
            productId = "stored-product-id",
        )

        val identities = unrepresentableManagedUappProfiles(
            profiles = listOf(
                managedProfile(unsupported, codec),
                managedProfile(usable, codec),
                managedProfile(removed, codec).copy(noLongerAvailable = true),
                undecodable,
                mismatchedProfileId,
                mismatchedProductId,
            ),
            snapshotCodec = codec,
        )

        assertThat(identities).containsExactly(
            ManagedUappExportIdentity("product:current", "unsupported"),
            ManagedUappExportIdentity("product:current", "undecodable"),
            ManagedUappExportIdentity("product:current", "stored-profile-id"),
            ManagedUappExportIdentity("stored-product-id", "usable"),
        )
    }

    @Test
    fun `recovery is blocked only for exact UAPP artifacts with current unrepresentable source`() {
        val codec = ManagedProfileSnapshotCodec()
        val usable = profile("usable", productId = "product:current", overUappLimit = false)
        val unsupported = profile("unsupported", productId = "product:current", overUappLimit = true)
        val currentUnsupported = managedProfile(unsupported, codec)
        val oldProfile = currentUnsupported.copy(noLongerAvailable = true)
        val uapp = ownership(
            documentUri = "content://eq-library/uapp",
            profileId = "unsupported",
            productId = "product:current",
            relativeDirectory = "${ExportDevice.UAPP.folderName}/Maker/Model",
        )
        val otherTarget = uapp.copy(relativeDirectory = "TRN Black Pearl/Maker/Model")

        assertThat(shouldBlockUappExportRecovery(uapp, currentUnsupported, codec)).isTrue()
        assertThat(shouldBlockUappExportRecovery(otherTarget, currentUnsupported, codec)).isFalse()
        assertThat(shouldBlockUappExportRecovery(uapp, null, codec)).isFalse()
        assertThat(shouldBlockUappExportRecovery(uapp, oldProfile, codec)).isFalse()
        assertThat(shouldBlockUappExportRecovery(uapp, managedProfile(usable, codec), codec)).isFalse()
        assertThat(
            shouldBlockUappExportRecovery(
                uapp,
                managedProfile(usable, codec).copy(snapshotJson = "{malformed snapshot"),
                codec,
            ),
        ).isTrue()
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
        relativeDirectory: String = "Manufacturer/Model",
        exportedAtMillis: Long = 1,
    ) = ExportOwnershipEntity(
        documentUri = documentUri,
        treeUri = "content://eq-library/tree",
        relativeDirectory = relativeDirectory,
        profileId = profileId,
        productId = productId,
        fileName = fileName,
        exportedFingerprint = "fingerprint",
        exportedContentHash = "hash",
        exportedAtMillis = exportedAtMillis,
    )

    private fun profile(id: String, productId: String, overUappLimit: Boolean) = OpraEqProfile(
        id = id,
        productId = productId,
        author = "Creator",
        details = null,
        link = null,
        profileType = "parametric_eq",
        preampGainDb = 0.0,
        bands = (1..(if (overUappLimit) 11 else 1)).map { index ->
            OpraBand("peak_dip", 100.0 * index, 0.0, 1.0, null)
        },
    )

    private fun managedProfile(
        profile: OpraEqProfile,
        codec: ManagedProfileSnapshotCodec,
    ) = ManagedProfileEntity(
        profileId = profile.id,
        productId = profile.productId,
        selected = true,
        explicitlyExcluded = false,
        snapshotJson = codec.encode(profile),
        fingerprint = codec.fingerprint(profile),
        firstSeenAtMillis = 1L,
        lastSeenAtMillis = 1L,
        isNewUnreviewed = false,
        isUpdatedUnreviewed = false,
        noLongerAvailable = false,
        generatedPresetName = "${profile.id} preset",
        generatedXml = null,
        generatedFromFingerprint = codec.fingerprint(profile),
        generatedAtMillis = null,
    )
}
