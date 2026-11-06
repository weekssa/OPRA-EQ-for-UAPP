package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.data.export.ExportDirectoryHandle
import com.weekssa.opraeqforuapp.data.export.ExportDocumentHandle
import com.weekssa.opraeqforuapp.data.export.ExportDocumentStore
import com.weekssa.opraeqforuapp.data.export.ExportLookup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqLibraryBundleImporterTest {
    private val backupUri = "content://docs/backup.json"

    @Test
    fun exportWritesEveryRowAndImportRestoresThemUnchanged() = runBlocking {
        val store = FilesDocumentStore()
        val source = FakeSavedRows(listOf(favoriteRow, personalRow))
        val general = FakeGeneralRows(listOf(generalRow))

        val exported = bundleStore(store, source, general).exportTo(backupUri)

        assertEquals(2, exported.savedEqs)
        assertEquals(1, exported.generalEqs)
        assertEquals("Exported 2 saved EQs and 1 General EQ.", exported.summary)

        val target = FakeSavedRows(emptyList())
        val targetGeneral = FakeGeneralRows(emptyList())
        val imported = bundleStore(store, target, targetGeneral).importFrom(backupUri)

        assertEquals(listOf(favoriteRow, personalRow), target.rows)
        assertEquals(listOf(generalRow), targetGeneral.rows)
        assertEquals(2, imported.savedEqsImported)
        assertEquals(1, imported.generalEqsImported)
        assertEquals("Imported 3 EQs.", imported.summary)
    }

    @Test
    fun importReportsEntriesThatWereAlreadyHere() = runBlocking {
        val store = FilesDocumentStore()
        bundleStore(store, FakeSavedRows(listOf(favoriteRow)), FakeGeneralRows())
            .exportTo(backupUri)
        val target = FakeSavedRows(alreadyPresent = 1)

        val imported = bundleStore(store, target, FakeGeneralRows()).importFrom(backupUri)

        assertEquals(0, imported.savedEqsImported)
        assertEquals(1, imported.savedEqsAlreadyPresent)
        assertEquals("1 already here.", imported.summary)
    }

    @Test
    fun importExplainsAFavoriteRestoredAsPersonal() = runBlocking {
        val store = FilesDocumentStore()
        bundleStore(store, FakeSavedRows(listOf(favoriteRow)), FakeGeneralRows())
            .exportTo(backupUri)
        val target = FakeSavedRows(restoredAsPersonal = 1)

        val imported = bundleStore(store, target, FakeGeneralRows()).importFrom(backupUri)

        assertEquals(1, imported.savedEqsRestoredAsPersonal)
        assertTrue(imported.summary.contains("restored as Personal"))
    }

    @Test
    fun foreignJsonIsRefusedWithoutTouchingTheLibrary() = runBlocking {
        val store = FilesDocumentStore()
        store.put(backupUri, """{"bands":[]}""")
        val target = FakeSavedRows()

        val imported = bundleStore(store, target, FakeGeneralRows()).importFrom(backupUri)

        assertEquals("This file is not an EQ Library backup.", imported.summary)
        assertTrue(target.rows.isEmpty())
    }

    @Test
    fun missingFileSaysNothingHappened() = runBlocking {
        val imported = bundleStore(FilesDocumentStore(), FakeSavedRows(), FakeGeneralRows())
            .importFrom("content://docs/missing.json")
        assertEquals("That file could not be opened. Nothing was imported.", imported.summary)
    }

    @Test
    fun emptyLibraryExportsAsNothingToDo() = runBlocking {
        val result = bundleStore(FilesDocumentStore(), FakeSavedRows(), FakeGeneralRows())
            .exportTo(backupUri)
        assertEquals("There is nothing to export yet.", result.summary)
    }

    private fun bundleStore(
        documentStore: ExportDocumentStore,
        saved: BundleRowSource<SavedEqTransfer>,
        general: BundleRowSource<GeneralEqTransfer>,
    ) = EqLibraryBundleStore(
        documentStore = documentStore,
        savedEqs = saved,
        generalEqs = general,
        sourcePackage = "com.weekssa.opraeqforuapp",
        sourceVersionName = "0.10.0",
        sourceVersionCode = 22,
        nowMillis = { 1_700_000_000_000L },
    )

    private class FakeSavedRows(
        initial: List<SavedEqTransfer> = emptyList(),
        private val alreadyPresent: Int = 0,
        private val restoredAsPersonal: Int = 0,
    ) : BundleRowSource<SavedEqTransfer> {
        val rows: MutableList<SavedEqTransfer> = initial.toMutableList()

        override suspend fun exportBundleRows(): List<SavedEqTransfer> = rows.toList()

        override suspend fun importBundleRows(rows: List<SavedEqTransfer>): BundleImportOutcome {
            if (alreadyPresent > 0) {
                return BundleImportOutcome(alreadyPresent = alreadyPresent)
            }
            this.rows += rows
            return BundleImportOutcome(
                imported = rows.size,
                alreadyPresent = alreadyPresent,
                restoredAsPersonal = restoredAsPersonal,
            )
        }
    }

    private class FakeGeneralRows(
        initial: List<GeneralEqTransfer> = emptyList(),
    ) : BundleRowSource<GeneralEqTransfer> {
        val rows: MutableList<GeneralEqTransfer> = initial.toMutableList()

        override suspend fun exportBundleRows(): List<GeneralEqTransfer> = rows.toList()

        override suspend fun importBundleRows(rows: List<GeneralEqTransfer>): BundleImportOutcome {
            this.rows += rows
            return BundleImportOutcome(imported = rows.size)
        }
    }

    private class FilesDocumentStore : ExportDocumentStore {
        private val files = mutableMapOf<String, ByteArray>()

        fun put(uri: String, text: String) {
            files[uri] = text.toByteArray(Charsets.UTF_8)
        }

        override fun openWritableTree(treeUri: String): ExportDirectoryHandle? = null

        /** A document the picker returned exists even before anything is written to it. */
        override fun openDocument(documentUri: String): ExportLookup<ExportDocumentHandle> =
            if (documentUri.endsWith("missing.json")) {
                ExportLookup.Missing
            } else {
                ExportLookup.Found(DocumentHandle(documentUri))
            }

        override fun findDirectory(
            parent: ExportDirectoryHandle,
            name: String,
        ): ExportLookup<ExportDirectoryHandle> = ExportLookup.Unavailable

        override fun createDirectory(
            parent: ExportDirectoryHandle,
            name: String,
        ): ExportDirectoryHandle? = null

        override fun findFile(
            parent: ExportDirectoryHandle,
            name: String,
        ): ExportLookup<ExportDocumentHandle> = ExportLookup.Unavailable

        override fun createFile(
            parent: ExportDirectoryHandle,
            mimeType: String,
            displayName: String,
        ): ExportDocumentHandle? = null

        override fun contentHash(document: ExportDocumentHandle): String? = null

        override fun readBytes(document: ExportDocumentHandle, maxBytes: Int): ByteArray? =
            files[document.uri]?.takeIf { it.size <= maxBytes }

        override fun writeBytes(document: ExportDocumentHandle, bytes: ByteArray): Boolean {
            files[document.uri] = bytes
            return true
        }

        override fun delete(document: ExportDocumentHandle): Boolean =
            files.remove(document.uri) != null

        override fun deleteByUri(documentUri: String): Boolean = files.remove(documentUri) != null

        private data class DocumentHandle(override val uri: String) : ExportDocumentHandle {
            override val name: String? = uri.substringAfterLast('/')
        }
    }

    private companion object {
        val favoriteRow = SavedEqTransfer(
            entryId = "favorite:p1",
            kind = "favorite",
            sourceProfileId = "p1",
            productId = "product-1",
            manufacturer = "AFUL",
            model = "Explorer",
            displayName = "Explorer — LoboNautics",
            profileJson = """{"id":"p1"}""",
            createdAtMillis = 10L,
            updatedAtMillis = 20L,
            canonicalSelectionJson = """{"profile":{}}""",
        )
        val personalRow = SavedEqTransfer(
            entryId = "personal:x",
            kind = "personal",
            productId = "personal-product:x",
            manufacturer = "Sennheiser",
            model = "HD 600",
            displayName = "My curve",
            profileJson = """{"id":"personal-eq:x"}""",
            createdAtMillis = 30L,
            updatedAtMillis = 40L,
        )
        val generalRow = GeneralEqTransfer(
            presetId = "gen-1",
            displayName = "Warm",
            category = "SOUND",
            profileJson = """{"id":"gen-1"}""",
            createdAtMillis = 50L,
            updatedAtMillis = 60L,
        )
    }
}

