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

        val migration = OpraEqDatabase.MIGRATION_2_3
        assertEquals(2, migration.startVersion)
        assertEquals(3, migration.endVersion)

        migration.migrate(recordingDatabase)

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

        val destructiveSql = Regex("\\b(DROP|DELETE|TRUNCATE|REPLACE)\\b", RegexOption.IGNORE_CASE)
        assertFalse(
            "v0.2 -> v0.3 migration must not destroy existing user data: $executedSql",
            executedSql.any { destructiveSql.containsMatchIn(it) },
        )
    }
}
