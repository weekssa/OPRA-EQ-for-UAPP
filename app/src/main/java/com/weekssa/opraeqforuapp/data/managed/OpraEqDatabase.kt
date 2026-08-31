package com.weekssa.opraeqforuapp.data.managed

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipDao
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipEntity
import com.weekssa.opraeqforuapp.data.library.OutputGeneralEqEntity
import com.weekssa.opraeqforuapp.data.library.OutputSavedEqEntity
import com.weekssa.opraeqforuapp.data.library.SavedEqDao
import com.weekssa.opraeqforuapp.data.library.SavedEqEntity
import com.weekssa.opraeqforuapp.data.library.SavedGeneralEqDao
import com.weekssa.opraeqforuapp.data.library.SavedGeneralEqEntity

@Database(
    entities = [
        ManagedHeadphoneEntity::class,
        ManagedProfileEntity::class,
        OutputManagedHeadphoneEntity::class,
        OutputManagedProfileEntity::class,
        ExportOwnershipEntity::class,
        SavedEqEntity::class,
        OutputSavedEqEntity::class,
        SavedGeneralEqEntity::class,
        OutputGeneralEqEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class OpraEqDatabase : RoomDatabase() {
    abstract fun managedHeadphonesDao(): ManagedHeadphonesDao
    abstract fun exportOwnershipDao(): ExportOwnershipDao
    abstract fun savedEqDao(): SavedEqDao
    abstract fun savedGeneralEqDao(): SavedGeneralEqDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE managed_profiles ADD COLUMN generatedPresetName TEXT")
                db.execSQL("ALTER TABLE managed_profiles ADD COLUMN generatedXml TEXT")
                db.execSQL("ALTER TABLE managed_profiles ADD COLUMN generatedFromFingerprint TEXT")
                db.execSQL("ALTER TABLE managed_profiles ADD COLUMN generatedAtMillis INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS export_ownership (
                        documentUri TEXT NOT NULL PRIMARY KEY,
                        treeUri TEXT NOT NULL,
                        relativeDirectory TEXT NOT NULL,
                        profileId TEXT NOT NULL,
                        productId TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        exportedFingerprint TEXT NOT NULL,
                        exportedContentHash TEXT NOT NULL,
                        exportedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_export_ownership_profileId ON export_ownership(profileId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_export_ownership_productId ON export_ownership(productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_export_ownership_treeUri ON export_ownership(treeUri)")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_eqs (
                        entryId TEXT NOT NULL PRIMARY KEY,
                        kind TEXT NOT NULL,
                        sourceProfileId TEXT,
                        productId TEXT NOT NULL,
                        manufacturer TEXT NOT NULL,
                        model TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        profileJson TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_eqs_kind ON saved_eqs(kind)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_eqs_sourceProfileId ON saved_eqs(sourceProfileId)")
            }
        }

        /** Existing v0.2/v0.3 saved selections become UAPP selections on upgrade. */
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS output_managed_headphones (
                        outputId TEXT NOT NULL,
                        productId TEXT NOT NULL,
                        autoIncludeNewProfiles INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(outputId, productId)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_output_managed_headphones_productId " +
                        "ON output_managed_headphones(productId)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS output_managed_profiles (
                        outputId TEXT NOT NULL,
                        productId TEXT NOT NULL,
                        profileId TEXT NOT NULL,
                        selected INTEGER NOT NULL,
                        explicitlyExcluded INTEGER NOT NULL,
                        PRIMARY KEY(outputId, profileId)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_output_managed_profiles_productId " +
                        "ON output_managed_profiles(productId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_output_managed_profiles_outputId_productId " +
                        "ON output_managed_profiles(outputId, productId)",
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO output_managed_headphones(
                        outputId, productId, autoIncludeNewProfiles, createdAtMillis, updatedAtMillis
                    )
                    SELECT 'UAPP', productId, autoIncludeNewProfiles, createdAtMillis, updatedAtMillis
                    FROM managed_headphones
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO output_managed_profiles(
                        outputId, productId, profileId, selected, explicitlyExcluded
                    )
                    SELECT 'UAPP', productId, profileId, selected, explicitlyExcluded
                    FROM managed_profiles
                    """.trimIndent(),
                )
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_general_eqs (
                        presetId TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        category TEXT NOT NULL,
                        profileJson TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_general_eqs_category ON saved_general_eqs(category)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS output_general_eqs (
                        outputId TEXT NOT NULL,
                        presetId TEXT NOT NULL,
                        selectedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(outputId, presetId)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_output_general_eqs_presetId ON output_general_eqs(presetId)")
            }
        }

        /** Existing favorites/personal imports become UAPP My EQs selections on upgrade. */
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS output_saved_eqs (
                        outputId TEXT NOT NULL,
                        entryId TEXT NOT NULL,
                        selectedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(outputId, entryId)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_output_saved_eqs_entryId ON output_saved_eqs(entryId)")
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO output_saved_eqs(outputId, entryId, selectedAtMillis)
                    SELECT 'UAPP', entryId, updatedAtMillis
                    FROM saved_eqs
                    """.trimIndent(),
                )
            }
        }

        fun create(context: Context): OpraEqDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                OpraEqDatabase::class.java,
                "opra_eq_for_uapp.db",
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                )
                .build()
    }
}
