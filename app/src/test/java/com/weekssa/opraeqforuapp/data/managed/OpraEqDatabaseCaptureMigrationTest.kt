package com.weekssa.opraeqforuapp.data.managed

import androidx.sqlite.db.SupportSQLiteDatabase
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OpraEqDatabaseCaptureMigrationTest {
    @Test
    fun migration6To7AddsOnlyNullableCaptureMetadata() {
        val migration = OpraEqDatabase.MIGRATION_6_7
        assertEquals(6, migration.startVersion)
        assertEquals(7, migration.endVersion)

        val executedSql = mutableListOf<String>()
        val database = Proxy.newProxyInstance(
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

        migration.migrate(database)

        assertEquals(listOf("ALTER TABLE saved_eqs ADD COLUMN captureMetadataJson TEXT"), executedSql)
        val destructiveSql = Regex("\\b(DROP|DELETE|TRUNCATE|REPLACE)\\b", RegexOption.IGNORE_CASE)
        assertFalse(executedSql.any { destructiveSql.containsMatchIn(it) })
    }
}
