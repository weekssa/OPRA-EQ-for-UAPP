package com.weekssa.opraeqforuapp.data.library

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weekssa.opraeqforuapp.data.managed.ManagedProfileSnapshotCodec
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlProtocol
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlReadCodec
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.EqSourceKind
import com.weekssa.opraeqforuapp.domain.library.EqTargetKind
import com.weekssa.opraeqforuapp.domain.library.VerificationStatus
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

            val blackPearlBundle = requireNotNull(
                HardwareEqSnapshotFactory.blackPearl(
                    nativeBands = (0 until BlackPearlProtocol.BAND_COUNT).map { index ->
                        BlackPearlReadCodec.NativeBand(
                            index = index,
                            type = when (index) {
                                1 -> EqFilterType.LOW_SHELF
                                8 -> EqFilterType.HIGH_SHELF
                                else -> EqFilterType.PEAK
                            },
                            frequencyRawHz = 100 + index * 700,
                            gainRaw256 = if (index == 0) 2 * 256 else 0,
                            qRaw256 = 256,
                            activeSlot = 2,
                        )
                    },
                    globalGainRaw = -18 * 256,
                    sessionGeneration = 8L,
                    verifiedAtEpochMillis = 1_235L,
                ),
            )
            val blackPearlCapture = repository.captureBlackPearlEq(
                displayName = "Black Pearl capture",
                snapshotBundle = blackPearlBundle,
                association = null,
            )
            val blackPearlEntity = requireNotNull(database.savedEqDao().get(blackPearlCapture.entryId))
            val blackPearlCanonical = SavedEqCanonicalSnapshotCodec().decode(
                requireNotNull(blackPearlEntity.canonicalSnapshotJson),
            )
            assertEquals(10, blackPearlCanonical.revision.filters.size)
            assertEquals(
                listOf(EqFilterType.PEAK, EqFilterType.LOW_SHELF, EqFilterType.HIGH_SHELF),
                blackPearlCanonical.revision.filters.map { it.type }.distinct(),
            )
            assertEquals(
                EqSourceKind.DEVICE_CAPTURE,
                blackPearlCanonical.revision.sourceReferences.single().sourceKind,
            )
            assertEquals(
                blackPearlBundle.fingerprint,
                SavedEqCaptureMetadataCodec().decode(blackPearlEntity.captureMetadataJson!!).nativeFingerprint,
            )
            assertEquals(10, blackPearlCapture.profile.bands!!.size)
        } finally {
            database.close()
        }
    }

    @Test
    fun corruptCanonicalRowDoesNotBreakMyEqsListAndCannotBeAdaptedForExport() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, OpraEqDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = SavedEqRepository(database, nowMillis = { 456_000L })
            val valid = repository.importPersonal(
                outputId = "UAPP",
                manufacturer = "Acme",
                model = "Headphone",
                displayName = "Valid row",
                target = null,
                peqText = "Filter 1: ON PK Fc 1000 Hz Gain 2 dB Q 1.0",
            )
            val legacyProfile = OpraEqProfile(
                id = "legacy-damaged-row",
                productId = "personal-product:damaged",
                author = "Personal",
                details = null,
                link = null,
                profileType = "parametric_eq",
                preampGainDb = null,
                bands = listOf(OpraBand("peak_dip", 2_000.0, 1.0, 1.0, null)),
            )
            database.savedEqDao().upsert(
                SavedEqEntity(
                    entryId = "personal:damaged",
                    kind = SavedEqRepository.KIND_PERSONAL,
                    sourceProfileId = null,
                    productId = legacyProfile.productId,
                    manufacturer = "Acme",
                    model = "Headphone",
                    displayName = "Damaged canonical row",
                    profileJson = ManagedProfileSnapshotCodec().encode(legacyProfile),
                    createdAtMillis = 1L,
                    updatedAtMillis = 2L,
                    canonicalSnapshotJson = "{not-json}",
                ),
            )

            val records = repository.observeForOutput("UAPP").first()
            assertEquals(2, records.size)
            val validRow = requireNotNull(records.singleOrNull { it.entryId == valid.entryId })
            val damagedRow = requireNotNull(records.singleOrNull { it.entryId == "personal:damaged" })
            assertFalse(validRow.canonicalSnapshotInvalid)
            assertTrue(damagedRow.canonicalSnapshotInvalid)
            assertEquals("legacy-damaged-row", damagedRow.profile.id)
            assertThrows(IllegalStateException::class.java) {
                repository.toManagedHeadphone(damagedRow)
            }
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
            val version8 = Room.databaseBuilder(context, OpraEqDatabase::class.java, databaseName)
                .allowMainThreadQueries()
                .build()
            try {
                runBlocking { version8.savedEqDao().upsert(original) }
            } finally {
                version8.close()
            }

            val databasePath = context.getDatabasePath(databaseName).absolutePath
            SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
                raw.execSQL("ALTER TABLE saved_eqs DROP COLUMN canonicalSnapshotJson")
                raw.execSQL("PRAGMA user_version=7")
            }

            val migrated = Room.databaseBuilder(context, OpraEqDatabase::class.java, databaseName)
                .addMigrations(OpraEqDatabase.MIGRATION_7_8)
                .allowMainThreadQueries()
                .build()
            try {
                val restored = runBlocking { migrated.savedEqDao().get(original.entryId) }
                assertEquals(original, restored)
                assertNull(restored?.canonicalSnapshotJson)
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }
}
