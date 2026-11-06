package com.weekssa.opraeqforuapp.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EqLibraryBundleTest {
    private val codec = EqLibraryBundleCodec()

    @Test
    fun roundTripKeepsEveryStoredField() {
        val bundle = EqLibraryBundle(
            format = EqLibraryBundle.FORMAT_ID,
            exportedAtMillis = 1_700_000_000_000L,
            sourcePackage = "com.weekssa.opraeqforuapp",
            sourceVersionName = "0.10.0",
            sourceVersionCode = 22,
            savedEqs = listOf(
                SavedEqTransfer(
                    entryId = "favorite:p1",
                    kind = "favorite",
                    sourceProfileId = "p1",
                    productId = "product-1",
                    manufacturer = "AFUL",
                    model = "Explorer",
                    displayName = "Explorer — LoboNautics",
                    profileJson = """{"id":"p1","bands":[]}""",
                    createdAtMillis = 10L,
                    updatedAtMillis = 20L,
                    canonicalSelectionJson = """{"profile":{}}""",
                ),
                SavedEqTransfer(
                    entryId = "personal:x",
                    kind = "personal",
                    productId = "personal-product:x",
                    manufacturer = "Sennheiser",
                    model = "HD 600",
                    displayName = "My curve",
                    profileJson = """{"id":"personal-eq:x"}""",
                    createdAtMillis = 30L,
                    updatedAtMillis = 40L,
                ),
            ),
            generalEqs = listOf(
                GeneralEqTransfer(
                    presetId = "gen-1",
                    displayName = "Warm",
                    category = "SOUND",
                    profileJson = """{"id":"gen-1"}""",
                    createdAtMillis = 50L,
                    updatedAtMillis = 60L,
                ),
            ),
        )

        val restored = codec.decode(codec.encode(bundle))

        assertEquals(bundle, restored)
        assertEquals(2, restored.savedEqs.size)
        assertEquals("favorite", restored.savedEqs.first().kind)
        assertEquals("Warm", restored.generalEqs.single().displayName)
        assertEquals("com.weekssa.opraeqforuapp", restored.sourcePackage)
    }

    @Test
    fun plainJsonIsNotAcceptedAsABackup() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            codec.decode("""{"bands":[{"frequency":1000}]}""")
        }
        assertEquals("This file is not an EQ Library backup.", error.message)
    }

    @Test
    fun newerFormatVersionIsRefused() {
        val future = """{"format":"eq-library-bundle","version":9,"savedEqs":[]}"""
        val error = assertThrows(IllegalArgumentException::class.java) { codec.decode(future) }
        assertTrue(error.message.orEmpty().contains("newer EQ Library"))
    }

    @Test
    fun unknownKeysAreIgnoredSoAnOlderReaderNeverBreaks() {
        val text = """{"format":"eq-library-bundle","version":1,"futureField":true,"savedEqs":[]}"""
        val bundle = codec.decode(text)
        assertTrue(bundle.isEmpty)
    }
}

