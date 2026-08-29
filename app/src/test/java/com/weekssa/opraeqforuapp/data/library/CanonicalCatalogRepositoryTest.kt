package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.domain.library.CanonicalEqProfile
import com.weekssa.opraeqforuapp.domain.library.CatalogSnapshot
import com.weekssa.opraeqforuapp.domain.library.EqFilter
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.EqRevision
import com.weekssa.opraeqforuapp.domain.library.EqSourceKind
import com.weekssa.opraeqforuapp.domain.library.EqSourceReference
import com.weekssa.opraeqforuapp.domain.library.EqTarget
import com.weekssa.opraeqforuapp.domain.library.EqTargetKind
import com.weekssa.opraeqforuapp.domain.library.HeadphoneIdentity
import com.weekssa.opraeqforuapp.domain.library.ProvenanceTier
import com.weekssa.opraeqforuapp.domain.library.RedistributionPolicy
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalCatalogRepositoryTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun validDownloadIsPromotedAndLoaded() = runBlocking {
        val root = createTempDirectory(prefix = "canonical-catalog-").toFile()
        try {
            val snapshot = sampleSnapshot("rev-1")
            val source = CanonicalCatalogSource { destination ->
                destination.writeText(json.encodeToString(snapshot))
            }
            val repository = CanonicalCatalogRepository(root, source, nowMillis = { 1234L })

            val result = repository.refresh()

            assertTrue(result is CanonicalCatalogRefreshResult.Success)
            val ready = repository.state.value as CanonicalCatalogState.Ready
            assertEquals("rev-1", ready.snapshot.profiles.single().latestRevision.revisionId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun invalidRefreshKeepsLastKnownGoodSnapshot() = runBlocking {
        val root = createTempDirectory(prefix = "canonical-catalog-").toFile()
        try {
            var fail = false
            val source = CanonicalCatalogSource { destination ->
                if (fail) throw IOException("offline")
                destination.writeText(json.encodeToString(sampleSnapshot("rev-1")))
            }
            val repository = CanonicalCatalogRepository(root, source, nowMillis = { 1234L })
            assertTrue(repository.refresh() is CanonicalCatalogRefreshResult.Success)

            fail = true
            val failed = repository.refresh() as CanonicalCatalogRefreshResult.Failure

            assertTrue(failed.usingLastKnownGood)
            val ready = repository.state.value as CanonicalCatalogState.Ready
            assertEquals("rev-1", ready.snapshot.profiles.single().latestRevision.revisionId)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun sampleSnapshot(revisionId: String) = CatalogSnapshot(
        schemaVersion = 1,
        generatedAt = "2026-08-29T00:00:00Z",
        sourceRegistryVersion = "0.3.0-test",
        profiles = listOf(
            CanonicalEqProfile(
                canonicalProfileId = "sennheiser-hd650:test",
                headphone = HeadphoneIdentity("Sennheiser", "HD 650"),
                creator = "Tester",
                target = EqTarget("Test Target", EqTargetKind.EXPLICIT_TARGET),
                tuningLabel = "Reference",
                revisions = listOf(
                    EqRevision(
                        revisionId = revisionId,
                        acousticFingerprint = "fingerprint-$revisionId",
                        preampGainDb = -5.0,
                        filters = listOf(EqFilter(EqFilterType.PEAK, 1000.0, -2.0, 1.0)),
                        sourceReferences = listOf(
                            EqSourceReference(
                                sourceId = "test",
                                sourceKind = EqSourceKind.STRUCTURED_CATALOG,
                                sourceRecordId = "record-1",
                                url = "https://example.com/eq",
                                creator = "Tester",
                                provenanceTier = ProvenanceTier.AUTHORITATIVE,
                                redistributionPolicy = RedistributionPolicy.ALLOWED,
                                isPrimary = true,
                            ),
                        ),
                        isLatest = true,
                    ),
                ),
            ),
        ),
    )
}
