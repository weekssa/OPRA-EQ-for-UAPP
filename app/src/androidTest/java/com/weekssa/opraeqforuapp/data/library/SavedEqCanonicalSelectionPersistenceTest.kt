package com.weekssa.opraeqforuapp.data.library

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weekssa.opraeqforuapp.data.catalog.AppCatalogRepository
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import com.weekssa.opraeqforuapp.domain.library.CanonicalEqProfile
import com.weekssa.opraeqforuapp.domain.library.CanonicalEqSelection
import com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapter
import com.weekssa.opraeqforuapp.domain.library.CatalogSnapshot
import com.weekssa.opraeqforuapp.domain.library.EqFilter
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.EqPresetPurpose
import com.weekssa.opraeqforuapp.domain.library.EqProfileScope
import com.weekssa.opraeqforuapp.domain.library.EqRevision
import com.weekssa.opraeqforuapp.domain.library.EqSourceKind
import com.weekssa.opraeqforuapp.domain.library.EqSourceReference
import com.weekssa.opraeqforuapp.domain.library.EqTarget
import com.weekssa.opraeqforuapp.domain.library.EqTargetKind
import com.weekssa.opraeqforuapp.domain.library.FavoriteToggleResult
import com.weekssa.opraeqforuapp.domain.library.HeadphoneIdentity
import com.weekssa.opraeqforuapp.domain.library.ProvenanceTier
import com.weekssa.opraeqforuapp.domain.library.RedistributionPolicy
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import com.weekssa.opraeqforuapp.ui.screens.resolveManagedFavoriteProfile
import java.nio.file.Files
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedEqCanonicalSelectionPersistenceTest {
    private lateinit var database: OpraEqDatabase
    private lateinit var savedEqRepository: SavedEqRepository
    private lateinit var savedGeneralEqRepository: SavedGeneralEqRepository
    private lateinit var snapshot: CatalogSnapshot

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OpraEqDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        savedEqRepository = SavedEqRepository(database, nowMillis = { 1_000L })
        savedGeneralEqRepository = SavedGeneralEqRepository(database, nowMillis = { 2_000L })
        snapshot = sampleSnapshot()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun favoritePersistsFullCanonicalProfileAndExactSelectedRevision() = runBlocking {
        val legacyProfile = CanonicalLegacyCatalogAdapter.adapt(snapshot).profiles
            .single { it.canonicalProfileId == "headphone-profile" && it.id == "eq-library:headphone-profile@headphone-new" }
        val selection = requireNotNull(CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, legacyProfile))

        val saved = savedEqRepository.toggleFavorite(
            outputId = "UAPP",
            profile = legacyProfile,
            manufacturer = "ignored display value",
            model = "ignored display value",
            canonicalSelection = selection,
        )

        assertEquals(FavoriteToggleResult.SAVED, saved)
        val entity = requireNotNull(database.savedEqDao().observeAll().first().singleOrNull())
        val stored = CanonicalEqSelectionCodec().decode(requireNotNull(entity.canonicalSelectionJson))
        assertEquals(selection, stored)
        assertEquals(listOf("headphone-old", "headphone-new"), stored.profile.revisions.map { it.revisionId })
        assertEquals("headphone-new", stored.selectedRevisionId)
        assertEquals(
            listOf("headphone-source-old", "headphone-source-new"),
            stored.profile.revisions.flatMap { it.sourceReferences }.mapNotNull { it.sourceRecordId },
        )

        val record = savedEqRepository.observeForOutput("UAPP").first().single()
        assertFalse(record.savedEqDataInvalid)
        assertEquals(selection, record.canonicalSelection)
        assertEquals(legacyProfile, record.actionProfileOrNull())
    }

    @Test
    fun aliasedCommunityFavoritePersistsCanonicalSelectionAndCanBeRemoved() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceSnapshot = afulAliasSnapshot()
        val filesDir = Files.createTempDirectory(context.cacheDir.toPath(), "aful-favorite-alias").toFile()
        try {
            val canonicalCatalogRepository = CanonicalCatalogRepository(
                filesDir = filesDir,
                source = { destination ->
                    destination.writeText(Json.encodeToString(sourceSnapshot), Charsets.UTF_8)
                },
            )
            val legacyCatalogRepository = FakeCatalogRepository(
                OpraCatalog(
                    vendors = listOf(OpraVendor(id = "aful", name = "AFUL")),
                    products = listOf(
                        OpraProduct(
                            id = LEGACY_AFUL_EXPLORER_PRODUCT_ID,
                            vendorId = "aful",
                            name = "Explorer",
                            type = "headphones",
                            subtype = "in_ear",
                        ),
                    ),
                    profiles = emptyList(),
                ),
            )
            val catalogRepository = CanonicalFirstCatalogRepository(
                canonicalCatalogRepository,
                legacyCatalogRepository,
            )
            catalogRepository.initialize()

            val effectiveCatalog = (catalogRepository.state.value as CatalogState.Ready).catalog
            val canonicalProductId = CanonicalLegacyCatalogAdapter.adapt(sourceSnapshot).products.single().id
            assertEquals(LEGACY_AFUL_EXPLORER_PRODUCT_ID, effectiveCatalog.productAliases[canonicalProductId])
            val displayedProfile = effectiveCatalog.profiles.single()
            assertEquals(LEGACY_AFUL_EXPLORER_PRODUCT_ID, displayedProfile.productId)
            val selection = requireNotNull(catalogRepository.resolveCanonicalSelection(displayedProfile))
            assertEquals(displayedProfile.productId, selection.compatibilityProductId)
            assertTrue(CanonicalLegacyCatalogAdapter.matchesSelection(selection, displayedProfile))

            val unrelatedProductProfile = displayedProfile.copy(productId = "unrelated::explorer")
            assertEquals(
                FavoriteToggleResult.CANONICAL_SOURCE_UNAVAILABLE,
                savedEqRepository.toggleFavorite(
                    outputId = "UAPP",
                    profile = unrelatedProductProfile,
                    manufacturer = "AFUL",
                    model = "Explorer",
                    canonicalSelection = selection,
                ),
            )
            assertTrue(database.savedEqDao().observeAll().first().isEmpty())

            assertEquals(
                FavoriteToggleResult.SAVED,
                savedEqRepository.toggleFavorite(
                    outputId = "UAPP",
                    profile = displayedProfile,
                    manufacturer = "ignored display value",
                    model = "ignored display value",
                    canonicalSelection = selection,
                ),
            )

            val saved = savedEqRepository.observeForOutput("UAPP").first().single()
            assertEquals(displayedProfile.productId, saved.productId)
            assertEquals(selection, saved.canonicalSelection)
            assertEquals(
                selection,
                CanonicalEqSelectionCodec().decode(
                    requireNotNull(database.savedEqDao().observeAll().first().single().canonicalSelectionJson),
                ),
            )
            assertEquals("community-96571b708868cdf52c109d5f", saved.canonicalSelection?.profile?.canonicalProfileId)
            assertEquals("rev-b2b639d1d1dfee12e6b87f97", saved.canonicalSelection?.selectedRevisionId)
            assertEquals(
                "b2b639d1d1dfee12e6b87f970fbe8e3d0d4e0a86493d1a3bcee1dfbe4783b5e9",
                saved.canonicalSelection?.selectedRevision?.acousticFingerprint,
            )
            assertEquals(displayedProfile.canonicalProfileId, saved.actionProfileOrNull()?.canonicalProfileId)
            assertEquals(displayedProfile.productId, saved.actionProfileOrNull()?.productId)
            assertEquals(
                "reddit-iems-1pi6g5d-lobonautics-explorer-basshead",
                saved.canonicalSelection?.selectedRevision?.sourceReferences?.single()?.sourceRecordId,
            )

            assertEquals(
                FavoriteToggleResult.REMOVED,
                savedEqRepository.toggleFavorite("UAPP", displayedProfile, "AFUL", "Explorer", selection),
            )
            assertTrue(database.savedEqDao().observeAll().first().isEmpty())
        } finally {
            filesDir.deleteRecursively()
        }
    }

    @Test
    fun staleManagedSnapshotResolvesCurrentProjectionBeforeFavoritePersistence() = runBlocking {
        val currentProfile = CanonicalLegacyCatalogAdapter.adapt(snapshot).profiles
            .single { it.canonicalProfileId == "headphone-profile" && it.id == "eq-library:headphone-profile@headphone-new" }
        val selection = requireNotNull(CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, currentProfile))
        val staleSnapshot = currentProfile.copy(
            details = "stale display projection",
            bands = currentProfile.bands!!.map { band ->
                band.copy(gainDb = requireNotNull(band.gainDb) + 0.5)
            },
        )
        val managed = ManagedProfileRecord(
            profileId = currentProfile.id,
            selected = true,
            explicitlyExcluded = false,
            lastKnownProfile = staleSnapshot,
            fingerprint = "stale",
            firstSeenAtMillis = 1L,
            lastSeenAtMillis = 1L,
            isNewUnreviewed = false,
            isUpdatedUnreviewed = false,
            noLongerAvailable = false,
            generatedPresetName = null,
            generatedXml = null,
            generatedFromFingerprint = null,
            generatedAtMillis = null,
        )

        val resolved = resolveManagedFavoriteProfile(
            profileId = managed.profileId,
            lastKnownProfile = managed.lastKnownProfile,
            currentProfiles = listOf(currentProfile),
        )
        assertEquals(currentProfile, resolved)
        assertEquals(
            FavoriteToggleResult.SAVED,
            savedEqRepository.toggleFavorite(
                outputId = "UAPP",
                profile = resolved,
                manufacturer = "Maker",
                model = "Headphone",
                canonicalSelection = selection,
            ),
        )

        val stored = CanonicalEqSelectionCodec().decode(
            requireNotNull(database.savedEqDao().observeAll().first().single().canonicalSelectionJson),
        )
        assertEquals(selection, stored)
        assertEquals(currentProfile, savedEqRepository.observeForOutput("UAPP").first().single().actionProfileOrNull())
    }

    @Test
    fun favoriteRejectsUnresolvedOrChangedProjectionAndCorruptSelectionCannotAct() = runBlocking {
        val legacyProfile = CanonicalLegacyCatalogAdapter.adapt(snapshot).profiles
            .single { it.canonicalProfileId == "headphone-profile" && it.id == "eq-library:headphone-profile@headphone-new" }
        val selection = requireNotNull(CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, legacyProfile))
        val altered = legacyProfile.copy(bands = legacyProfile.bands!!.map { it.copy(gainDb = requireNotNull(it.gainDb) + 1.0) })

        val rejected = savedEqRepository.toggleFavorite(
            "UAPP",
            altered,
            "Maker",
            "Headphone",
            selection,
        )
        assertEquals(FavoriteToggleResult.CANONICAL_SOURCE_UNAVAILABLE, rejected)
        assertTrue(database.savedEqDao().observeAll().first().isEmpty())

        assertEquals(
            FavoriteToggleResult.SAVED,
            savedEqRepository.toggleFavorite("UAPP", legacyProfile, "Maker", "Headphone", selection),
        )
        val saved = requireNotNull(database.savedEqDao().observeAll().first().singleOrNull())
        database.savedEqDao().upsert(saved.copy(canonicalSelectionJson = "{invalid-json"))

        val damaged = savedEqRepository.observeForOutput("UAPP").first().single()
        assertTrue(damaged.savedEqDataInvalid)
        assertNull(damaged.actionProfileOrNull())
        assertTrue(savedEqRepository.toManagedHeadphones(listOf(damaged)).isEmpty())
    }

    @Test
    fun generalBatchPersistsCanonicalSelectionsAtomicallyAndRejectsStaleBatch() = runBlocking {
        val presets = CanonicalLegacyCatalogAdapter.adapt(snapshot).generalPresets
        val savedPreset = presets.single { it.id.endsWith("@general-new") }
        val selection = requireNotNull(CanonicalLegacyCatalogAdapter.resolveSelection(snapshot, savedPreset))
        val stale = savedPreset.copy(bands = savedPreset.bands.map { it.copy(gainDb = (it.gainDb ?: 0.0) + 0.5) })

        assertFalse(
            savedGeneralEqRepository.saveAllForOutput(
                "UAPP",
                listOf(savedPreset to selection, stale to selection),
            ),
        )
        assertTrue(savedGeneralEqRepository.observeForOutput("UAPP").first().isEmpty())

        assertTrue(savedGeneralEqRepository.saveForOutput("UAPP", savedPreset, selection))
        val entity = requireNotNull(database.savedGeneralEqDao().observeAll().first().singleOrNull())
        val stored = CanonicalEqSelectionCodec().decode(requireNotNull(entity.canonicalSelectionJson))
        assertEquals(selection, stored)
        assertEquals(listOf("general-old", "general-new"), stored.profile.revisions.map { it.revisionId })

        val record = savedGeneralEqRepository.observeForOutput("UAPP").first().single()
        assertFalse(record.savedEqDataInvalid)
        assertEquals(
            CanonicalLegacyCatalogAdapter.projectGeneralSelection(selection, savedPreset.id),
            record.actionProfileOrNull(),
        )

        database.savedGeneralEqDao().upsert(entity.copy(canonicalSelectionJson = "{invalid-json"))
        val damaged = savedGeneralEqRepository.observeForOutput("UAPP").first().single()
        assertTrue(damaged.savedEqDataInvalid)
        assertNull(damaged.actionProfileOrNull())
    }

    @Test
    fun malformedGeneralProfileAndCategoryRemainVisibleButCannotAct() = runBlocking {
        database.savedGeneralEqDao().upsert(
            SavedGeneralEqEntity(
                presetId = "general:malformed",
                displayName = "Malformed general EQ",
                category = "not-a-category",
                profileJson = "{invalid-json",
                createdAtMillis = 1L,
                updatedAtMillis = 2L,
            ),
        )

        val record = savedGeneralEqRepository.observeForOutput("UAPP").first().single()

        assertTrue(record.savedEqDataInvalid)
        assertNull(record.actionProfileOrNull())
    }

    private fun afulAliasSnapshot() = CatalogSnapshot(
        schemaVersion = 1,
        generatedAt = "2026-09-30T00:00:00Z",
        sourceRegistryVersion = "favorite-alias-test",
        profiles = listOf(
            CanonicalEqProfile(
                canonicalProfileId = "community-96571b708868cdf52c109d5f",
                headphone = HeadphoneIdentity("AFUL", "Explorer"),
                creator = "LoboNautics",
                target = EqTarget(null, EqTargetKind.UNKNOWN),
                tuningLabel = "Basshead tuning",
                revisions = listOf(
                    EqRevision(
                        revisionId = "rev-b2b639d1d1dfee12e6b87f97",
                        acousticFingerprint = "b2b639d1d1dfee12e6b87f970fbe8e3d0d4e0a86493d1a3bcee1dfbe4783b5e9",
                        preampGainDb = -3.7,
                        filters = listOf(
                            EqFilter(EqFilterType.LOW_SHELF, 20.0, 2.0, 0.3),
                            EqFilter(EqFilterType.LOW_SHELF, 90.0, 3.0, 0.3),
                            EqFilter(EqFilterType.PEAK, 200.0, -1.0, 1.3),
                            EqFilter(EqFilterType.PEAK, 1500.0, -0.6, 1.2),
                            EqFilter(EqFilterType.PEAK, 2700.0, 1.0, 2.0),
                            EqFilter(EqFilterType.PEAK, 3500.0, 3.7, 1.1),
                            EqFilter(EqFilterType.PEAK, 3800.0, -2.0, 1.2),
                            EqFilter(EqFilterType.PEAK, 8000.0, 1.5, 0.3),
                        ),
                        sourceReferences = listOf(
                            EqSourceReference(
                                sourceId = "reddit-audio",
                                sourceKind = EqSourceKind.COMMUNITY,
                                sourceRecordId = "reddit-iems-1pi6g5d-lobonautics-explorer-basshead",
                                sourceVendorId = "AFUL",
                                sourceProductId = "Explorer",
                                url = "https://www.reddit.com/r/iems/comments/1pi6g5d/aful_explorer_basshead_tuning/",
                                creator = "LoboNautics",
                                provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
                                redistributionPolicy = RedistributionPolicy.STRUCTURED_DATA_ONLY,
                                isPrimary = true,
                            ),
                        ),
                        isLatest = true,
                    ),
                ),
            ),
        ),
    )

    private class FakeCatalogRepository(catalog: OpraCatalog) : AppCatalogRepository {
        override val state: StateFlow<CatalogState> = MutableStateFlow(
            CatalogState.Ready(catalog = catalog, lastSuccessfulRefreshMillis = 1L),
        )

        override suspend fun initialize() = Unit

        override suspend fun refresh(): CatalogRefreshResult = CatalogRefreshResult.Failure(
            reason = CatalogRefreshFailureReason.Network,
            usingSavedCatalog = true,
        )
    }

    private fun sampleSnapshot(): CatalogSnapshot {
        val headphone = CanonicalEqProfile(
            canonicalProfileId = "headphone-profile",
            headphone = HeadphoneIdentity("Maker", "Headphone", variant = "Revision Test"),
            creator = "Tester",
            target = EqTarget("Reference", EqTargetKind.EXPLICIT_TARGET),
            tuningLabel = "Headphone test",
            revisions = listOf(
                revision("headphone-old", 100.0, source("headphone-source-old"), latest = false),
                revision("headphone-new", 200.0, source("headphone-source-new"), latest = true),
            ),
        )
        val general = CanonicalEqProfile(
            canonicalProfileId = "general-profile",
            scope = EqProfileScope.GENERAL,
            purpose = EqPresetPurpose.EFFECT,
            creator = "Tester",
            target = EqTarget(null, EqTargetKind.UNKNOWN),
            tuningLabel = "General test",
            revisions = listOf(
                revision("general-old", 80.0, source("general-source-old"), latest = false),
                revision("general-new", 120.0, source("general-source-new"), latest = true),
            ),
        )
        return CatalogSnapshot(
            schemaVersion = 1,
            generatedAt = "2026-09-23T00:00:00Z",
            sourceRegistryVersion = "test",
            profiles = listOf(headphone, general),
        )
    }

    private fun revision(id: String, frequency: Double, reference: EqSourceReference, latest: Boolean) = EqRevision(
        revisionId = id,
        acousticFingerprint = "fingerprint-$id",
        preampGainDb = -1.0,
        filters = listOf(EqFilter(EqFilterType.PEAK, frequency, 1.0, 1.0)),
        sourceReferences = listOf(reference),
        isLatest = latest,
    )

    private fun source(id: String) = EqSourceReference(
        sourceId = "community",
        sourceKind = EqSourceKind.COMMUNITY,
        sourceRecordId = id,
        url = "https://example.com/$id",
        creator = "Tester",
        provenanceTier = ProvenanceTier.TRACEABLE_COMMUNITY,
        redistributionPolicy = RedistributionPolicy.LINK_ONLY,
        isPrimary = true,
    )

    private companion object {
        const val LEGACY_AFUL_EXPLORER_PRODUCT_ID = "aful::explorer"
    }
}
