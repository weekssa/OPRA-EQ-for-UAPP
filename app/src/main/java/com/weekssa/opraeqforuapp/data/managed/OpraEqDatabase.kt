package com.weekssa.opraeqforuapp.data.managed

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipDao
import com.weekssa.opraeqforuapp.data.export.ExportOwnershipEntity

@Database(
    entities = [
        ManagedHeadphoneEntity::class,
        ManagedProfileEntity::class,
        ExportOwnershipEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class OpraEqDatabase : RoomDatabase() {
    abstract fun managedHeadphonesDao(): ManagedHeadphonesDao
    abstract fun exportOwnershipDao(): ExportOwnershipDao

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

        fun create(context: Context): OpraEqDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                OpraEqDatabase::class.java,
                "opra_eq_for_uapp.db",
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
