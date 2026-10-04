package com.weekssa.opraeqforuapp.ui.screens

import java.nio.file.Files
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalEqInputDraftStoreTest {
    @Test
    fun smallTextIsStoredInline() {
        val root = Files.createTempDirectory("personal-eq-draft-small").toFile()
        val store = PersonalEqInputDraftStore(root)
        val draftId = UUID.randomUUID().toString()
        val source = "Filter 1: ON PK Fc 100 Hz Gain 1 dB Q 1.0"

        val saved = store.encode(draftId, source)

        assertTrue(saved.startsWith(PersonalEqInputDraftStore.INLINE_PREFIX))
        assertEquals(source, store.decode(draftId, saved))
        assertFalse(store.backingFile(draftId).exists())
        root.deleteRecursively()
    }

    @Test
    fun largeTextIsStoredOutsideSavedStateAndRestoredFromPrivateFile() {
        val root = Files.createTempDirectory("personal-eq-draft-large").toFile()
        val store = PersonalEqInputDraftStore(root)
        val draftId = UUID.randomUUID().toString()
        val source = buildString {
            repeat(30_000) { index -> append("Filter $index: ON PK Fc 100 Hz Gain 1 dB Q 1.0\n") }
        }

        val saved = store.encode(draftId, source)

        assertTrue(saved.length < 64)
        assertFalse(saved.contains(source))
        assertTrue(store.backingFile(draftId).isFile)
        assertEquals(source, store.decode(draftId, saved))
        assertFalse(store.backingFile(draftId).exists())
        root.deleteRecursively()
    }

    @Test
    fun aMissingLargeDraftRestoresAsAnActionableFailureMarker() {
        val root = Files.createTempDirectory("personal-eq-draft-missing").toFile()
        val store = PersonalEqInputDraftStore(root)
        val draftId = UUID.randomUUID().toString()

        assertEquals("\u0000peq-restore-failed", store.decode(draftId, "\u0000peq-file:$draftId"))
        root.deleteRecursively()
    }
}
