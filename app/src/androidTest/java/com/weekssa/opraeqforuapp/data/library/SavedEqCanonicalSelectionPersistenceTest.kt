package com.weekssa.opraeqforuapp.data.library

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
}
