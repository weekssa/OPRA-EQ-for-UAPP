package com.weekssa.opraeqforuapp.data.library

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.library.EqSourceKind
import com.weekssa.opraeqforuapp.domain.library.EqTargetKind
import com.weekssa.opraeqforuapp.domain.library.VerificationStatus
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedEqCanonicalSnapshotMigrationTest {
    @Test
    fun importsAndVerifiedCapturesPersistCanonicalSourceAndDerivedLegacyView() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, OpraEqDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = SavedEqRepository(database, nowMillis = { 123_000L })
            val imported = repository.importPersonal(
                outputId = "UAPP",
                manufacturer = "Acme",
                model = "Headphone",
                displayName = "Imported curve",
                target = "Custom target",
                peqText = "Filter 1: ON PK Fc 1000 Hz Gain 2 dB Q 1.0",
            )
            val importedEntity = requireNotNull(database.savedEqDao().get(imported.entryId))
            val importedCanonical = SavedEqCanonicalSnapshotCodec().decode(
                requireNotNull(importedEntity.canonicalSnapshotJson),
            )
            assertEquals("Imported curve", importedCanonical.displayName)
            assertEquals("Acme", importedCanonical.headphone?.manufacturer)
            assertEquals("Custom target", importedCanonical.target.name)
            assertEquals(EqTargetKind.CUSTOM_USER, importedCanonical.target.kind)
            assertEquals(EqSourceKind.PERSONAL_IMPORT, importedCanonical.revision.sourceReferences.single().sourceKind)
            assertEquals(VerificationStatus.UNVERIFIED, importedCanonical.revision.verificationStatus)
            val importedLegacyProjection = ManagedProfileSnapshotCodec().decode(importedEntity.profileJson)
            assertEquals(importedCanonical.profileId, importedLegacyProjection.id)
            assertEquals("Custom target", importedLegacyProjection.details)
            assertEquals(1_000.0, importedLegacyProjection.bands!!.single().frequency!!, 0.0)
            assertEquals(importedLegacyProjection, imported.profile)

            val snapshot = requireNotNull(
                HardwareEqSnapshotFactory.ew300(
                    nativeBands = listOf(
                        Kt02h20Band("peak_dip", 100.0, -1.1, 0.8),
                        Kt02h20Band("peak_dip", 200.0, -0.9, 0.8),
                        Kt02h20Band("peak_dip", 300.0, -0.4, 1.0),
                        Kt02h20Band("peak_dip", 8_000.0, -4.8, 1.5),
                        Kt02h20Band("peak_dip", 7_000.0, 0.5, 0.5),
                    ),
                    globalGainDb = -4.0,
                    sessionGeneration = 7L,
                    verifiedAtEpochMillis = 1234L,
                ),
            )
            val captured = repository.captureEw300Eq("Captured curve", snapshot, association = null)
            val capturedEntity = requireNotNull(database.savedEqDao().get(captured.entryId))
            val capturedCanonical = SavedEqCanonicalSnapshotCodec().decode(
                requireNotNull(capturedEntity.canonicalSnapshotJson),
            )
            assertEquals(EqSourceKind.DEVICE_CAPTURE, capturedCanonical.revision.sourceReferences.single().sourceKind)
            assertEquals(VerificationStatus.VERIFIED, capturedCanonical.revision.verificationStatus)
            assertNull(capturedCanonical.headphone)
            assertNull(capturedCanonical.revision.preampGainDb)
            assertEquals(5, capturedCanonical.revision.filters.size)
            assertNotNull(capturedEntity.captureMetadataJson)
            assertEquals(
                snapshot.fingerprint,
                SavedEqCaptureMetadataCodec().decode(capturedEntity.captureMetadataJson!!).nativeFingerprint,
            )
            assertNull(captured.profile.preampGainDb)
            assertEquals("", captured.manufacturer)
            assertEquals("", captured.model)
        } finally {
            database.close()
        }
    }

    @Test
    fun roomOpensVersion7RowsAfterAdditiveVersion8Migration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "saved-eq-migration-${UUID.randomUUID()}.db"
        val original = SavedEqEntity(
            entryId = "personal:before-v8",
            kind = "personal",
            sourceProfileId = null,
            productId = "personal-product:before-v8",
            manufacturer = "",
            model = "",
            displayName = "Legacy local EQ",
            profileJson = "{\"legacy\":\"profile-json\"}",
            createdAtMillis = 1L,
            updatedAtMillis = 2L,
            captureMetadataJson = "{\"legacy\":\"capture-metadata\"}",
        )

        try {
            Room.databaseBuilder(context, OpraEqDatabase::class.java, databaseName)
                .allowMainThreadQueries()
                .build()
                .use { version8 -> version8.savedEqDao().upsert(original) }

            val databasePath = context.getDatabasePath(databaseName).absolutePath
            SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
                raw.execSQL("ALTER TABLE saved_eqs DROP COLUMN canonicalSnapshotJson")
                raw.execSQL("PRAGMA user_version=7")
            }

            Room.databaseBuilder(context, OpraEqDatabase::class.java, databaseName)
                .addMigrations(OpraEqDatabase.MIGRATION_7_8)
                .allowMainThreadQueries()
                .build()
                .use { migrated ->
                    val restored = migrated.savedEqDao().get(original.entryId)
                    assertEquals(original, restored)
                    assertNull(restored?.canonicalSnapshotJson)
                }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }
}
