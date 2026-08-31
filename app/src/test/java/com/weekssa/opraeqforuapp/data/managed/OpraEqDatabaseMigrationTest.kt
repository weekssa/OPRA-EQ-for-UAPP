package com.weekssa.opraeqforuapp.data.managed

import androidx.sqlite.db.SupportSQLiteDatabase
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpraEqDatabaseMigrationTest {
    @Test
    fun migration2To3AddsSavedEqsWithoutDestructiveSql() {
        val executedSql = recordMigration(OpraEqDatabase.MIGRATION_2_3)

        assertEquals(3, executedSql.size)
        val tableSql = executedSql[0].replace(Regex("\\s+"), " ").trim()
        assertTrue(tableSql.startsWith("CREATE TABLE IF NOT EXISTS saved_eqs ("))
        listOf(
            "entryId TEXT NOT NULL PRIMARY KEY",
            "kind TEXT NOT NULL",
            "sourceProfileId TEXT",
            "productId TEXT NOT NULL",
            "manufacturer TEXT NOT NULL",
            "model TEXT NOT NULL",
            "displayName TEXT NOT NULL",
            "profileJson TEXT NOT NULL",
            "createdAtMillis INTEGER NOT NULL",
            "updatedAtMillis INTEGER NOT NULL",
        ).forEach { requiredColumn ->
            assertTrue("Missing migrated column: $requiredColumn", tableSql.contains(requiredColumn))
        }

        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_saved_eqs_kind ON saved_eqs(kind)",
            executedSql[1],
        )
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_saved_eqs_sourceProfileId ON saved_eqs(sourceProfileId)",
            executedSql[2],
        )
        assertNoDestructiveSql(executedSql)
    }

    @Test
    fun migration3To4CreatesOutputSelectionsAndSeedsExistingLibraryIntoUapp() {
        val migration = OpraEqDatabase.MIGRATION_3_4
        assertEquals(3, migration.startVersion)
        assertEquals(4, migration.endVersion)

        val executedSql = recordMigration(migration)

        assertEquals(7, executedSql.size)
        val headphoneTable = executedSql[0].replace(Regex("\\s+"), " ").trim()
        val profileTable = executedSql[2].replace(Regex("\\s+"), " ").trim()
        assertTrue(headphoneTable.contains("CREATE TABLE IF NOT EXISTS output_managed_headphones"))
        assertTrue(headphoneTable.contains("PRIMARY KEY(outputId, productId)"))
        assertTrue(profileTable.contains("CREATE TABLE IF NOT EXISTS output_managed_profiles"))
        assertTrue(profileTable.contains("PRIMARY KEY(outputId, profileId)"))

        val headphoneSeed = executedSql[5].replace(Regex("\\s+"), " ").trim()
        val profileSeed = executedSql[6].replace(Regex("\\s+"), " ").trim()
        assertTrue(headphoneSeed.contains("SELECT 'UAPP', productId, autoIncludeNewProfiles"))
        assertTrue(headphoneSeed.contains("FROM managed_headphones"))
        assertTrue(profileSeed.contains("SELECT 'UAPP', productId, profileId, selected, explicitlyExcluded"))
        assertTrue(profileSeed.contains("FROM managed_profiles"))
        assertNoDestructiveSql(executedSql)
    }

    @Test
    fun migration4To5AddsGeneralEqsWithoutTouchingHeadphoneSelections() {
        val migration = OpraEqDatabase.MIGRATION_4_5
        assertEquals(4, migration.startVersion)
        assertEquals(5, migration.endVersion)

        val executedSql = recordMigration(migration)

        assertEquals(4, executedSql.size)
        val savedTable = executedSql[0].replace(Regex("\\s+"), " ").trim()
        val outputTable = executedSql[2].replace(Regex("\\s+"), " ").trim()
        assertTrue(savedTable.contains("CREATE TABLE IF NOT EXISTS saved_general_eqs"))
        assertTrue(savedTable.contains("presetId TEXT NOT NULL PRIMARY KEY"))
        assertTrue(savedTable.contains("profileJson TEXT NOT NULL"))
        assertTrue(outputTable.contains("CREATE TABLE IF NOT EXISTS output_general_eqs"))
        assertTrue(outputTable.contains("PRIMARY KEY(outputId, presetId)"))
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_saved_general_eqs_category ON saved_general_eqs(category)",
            executedSql[1],
        )
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_output_general_eqs_presetId ON output_general_eqs(presetId)",
            executedSql[3],
        )
        assertNoDestructiveSql(executedSql)
    }

    private fun recordMigration(migration: androidx.room.migration.Migration): List<String> {
        val executedSql = mutableListOf<String>()
        val recordingDatabase = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name != "execSQL") {
                throw AssertionError("Unexpected database call during migration: ${method.name}")
            }
            executedSql += args?.firstOrNull() as? String
                ?: throw AssertionError("execSQL did not receive SQL text")
            null
        } as SupportSQLiteDatabase
        migration.migrate(recordingDatabase)
        return executedSql
    }

    private fun assertNoDestructiveSql(executedSql: List<String>) {
        val destructiveSql = Regex("\\b(DROP|DELETE|TRUNCATE|REPLACE)\\b", RegexOption.IGNORE_CASE)
        assertFalse(
            "Migration must not destroy existing user data: $executedSql",
            executedSql.any { destructiveSql.containsMatchIn(it) },
        )
    }
}
