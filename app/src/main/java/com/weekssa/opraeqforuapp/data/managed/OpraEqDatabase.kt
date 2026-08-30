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
        ExportOwnershipEntity::class,
        SavedEqEntity::class,
    ],
    version = 3,
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

        fun create(context: Context): OpraEqDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                OpraEqDatabase::class.java,
                "opra_eq_for_uapp.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
