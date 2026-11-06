package com.weekssa.opraeqforuapp.data.library

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Proves the Room round trip the JVM suite cannot reach: export rows, wipe, import, compare. */
@RunWith(AndroidJUnit4::class)
class LibraryBundleDatabaseTest {
    private lateinit var database: OpraEqDatabase
    private lateinit var savedEqs: SavedEqRepository
    private lateinit var generalEqs: SavedGeneralEqRepository
    private val snapshotCodec = ManagedProfileSnapshotCodec()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OpraEqDatabase::class.java,
        ).allowMainThreadQueries().build()
        savedEqs = SavedEqRepository(database)
        generalEqs = SavedGeneralEqRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savedEqSurvivesExportWipeAndImport() = runBlocking {
        database.savedEqDao().upsert(favoriteEntity())
        val exported = savedEqs.exportBundleRows()
        assertEquals(1, exported.size)

        database.clearAllTables()
        assertEquals(0, savedEqs.exportBundleRows().size)

        val outcome = savedEqs.importBundleRows(exported)
        assertEquals(1, outcome.imported)

        val restored = database.savedEqDao().get("favorite:p1")
        assertNotNull(restored)
        assertEquals("favorite", restored!!.kind)
        assertEquals("AFUL", restored.manufacturer)
        assertEquals("Explorer — LoboNautics", restored.displayName)
        assertEquals(10L, restored.createdAtMillis)
        assertEquals(exported.single().profileJson, restored.profileJson)
        assertEquals(null, restored.canonicalSelectionJson)
    }

    @Test
    fun generalEqSurvivesExportWipeAndImport() = runBlocking {
        database.savedGeneralEqDao().upsert(
            SavedGeneralEqEntity(
                presetId = "gen-1",
                displayName = "Warm",
                category = "SOUND",
                profileJson = snapshotCodec.encode(sampleProfile("gen-1")),
                createdAtMillis = 50L,
                updatedAtMillis = 60L,
            ),
        )
        val exported = generalEqs.exportBundleRows()
        database.clearAllTables()

        val outcome = generalEqs.importBundleRows(exported)

        assertEquals(1, outcome.imported)
        val restored = database.savedGeneralEqDao().get("gen-1")
        assertNotNull(restored)
        assertEquals("Warm", restored!!.displayName)
        assertEquals("SOUND", restored.category)
    }

    @Test
    fun importingTheSameBackupTwiceChangesNothing() = runBlocking {
        database.savedEqDao().upsert(favoriteEntity())
        val exported = savedEqs.exportBundleRows()

        val second = savedEqs.importBundleRows(exported)

        assertEquals(0, second.imported)
        assertEquals(1, second.alreadyPresent)
        assertEquals("AFUL", database.savedEqDao().get("favorite:p1")!!.manufacturer)
    }

    @Test
    fun unreadableSelectionRestoresTheCurveAsPersonal() = runBlocking {
        database.savedEqDao().upsert(favoriteEntity())
        val exported = savedEqs.exportBundleRows().map { it.copy(canonicalSelectionJson = "not json") }
        database.clearAllTables()

        val outcome = savedEqs.importBundleRows(exported)

        assertEquals(1, outcome.imported)
        assertEquals(1, outcome.restoredAsPersonal)
        val restored = database.savedEqDao().get("favorite:p1")
        assertEquals("personal", restored!!.kind)
        assertEquals(exported.single().profileJson, restored.profileJson)
        assertEquals(null, restored.canonicalSelectionJson)
    }

    @Test
    fun damagedCurveIsReportedAndNotWritten() = runBlocking {
        val damaged = listOf(
            SavedEqTransfer(
                entryId = "favorite:broken",
                kind = "favorite",
                sourceProfileId = "broken",
                productId = "product-1",
                manufacturer = "AFUL",
                model = "Explorer",
                displayName = "Broken",
                profileJson = "{ this is not a profile",
                createdAtMillis = 1L,
                updatedAtMillis = 2L,
            ),
        )

        val outcome = savedEqs.importBundleRows(damaged)

        assertEquals(0, outcome.imported)
        assertEquals(1, outcome.failures.size)
        assertTrue(outcome.failures.single().contains("Broken"))
        assertEquals(0, database.savedEqDao().getAll().size)
    }

    private fun favoriteEntity(): SavedEqEntity = SavedEqEntity(
        entryId = "favorite:p1",
        kind = "favorite",
        sourceProfileId = "p1",
        productId = "product-1",
        manufacturer = "AFUL",
        model = "Explorer",
        displayName = "Explorer — LoboNautics",
        profileJson = snapshotCodec.encode(sampleProfile("p1")),
        createdAtMillis = 10L,
        updatedAtMillis = 20L,
        canonicalSelectionJson = null,
    )

    private fun sampleProfile(id: String): OpraEqProfile = OpraEqProfile(
        id = id,
        productId = "product-1",
        author = "Oratory1990",
        details = "Five bands",
        link = null,
        profileType = "parametric_eq",
        preampGainDb = -3.5,
        bands = listOf(
            OpraBand(type = "peak_dip", frequency = 1000.0, gainDb = 1.0, q = 0.7, slope = null),
        ),
    )
}

