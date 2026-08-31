package com.weekssa.opraeqforuapp.data.managed

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipDao
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipEntity
import com.weekssa.opraeqforuapp.data.library.SavedEqDao
import com.weekssa.opraeqforuapp.data.library.SavedEqEntity

@Database(
    entities = [
        ManagedHeadphoneEntity::class,
        ManagedProfileEntity::class,
        OutputManagedHeadphoneEntity::class,
        OutputManagedProfileEntity::class,
        ExportOwnershipEntity::class,
        SavedEqEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class OpraEqDatabase : RoomDatabase() {
    abstract fun managedHeadphonesDao(): ManagedHeadphonesDao
    abstract fun exportOwnershipDao(): ExportOwnershipDao
    abstract fun savedEqDao(): SavedEqDao

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

        /**
         * Moves the pre-output-context saved selection into UAPP so upgrades preserve exactly what
         * the user already had. Other outputs intentionally start empty until the user saves EQs
         * while that output is active.
         */
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

        fun create(context: Context): OpraEqDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                OpraEqDatabase::class.java,
                "opra_eq_for_uapp.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
