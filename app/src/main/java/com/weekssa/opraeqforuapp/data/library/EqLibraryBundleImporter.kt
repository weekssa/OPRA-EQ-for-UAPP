package com.weekssa.opraeqforuapp.data.library

import com.weekssa.opraeqforuapp.data.export.ExportDocumentStore
import com.weekssa.opraeqforuapp.data.export.ExportLookup
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Outcome of restoring one bundle. The counts are what the owner is told; nothing is silent. */
data class EqLibraryImportResult(
    val savedEqsImported: Int = 0,
    val savedEqsAlreadyPresent: Int = 0,
    val savedEqsRestoredAsPersonal: Int = 0,
    val generalEqsImported: Int = 0,
    val generalEqsAlreadyPresent: Int = 0,
    val skippedCount: Int = 0,
    val failures: List<String> = emptyList(),
    val summary: String = "",
)

data class EqLibraryExportResult(
    val savedEqs: Int,
    val generalEqs: Int,
    val skipped: Int,
    val summary: String,
)

/**
 * Reads and writes a [EqLibraryBundle] through a document the owner picked, then hands the rows to
 * the repositories that own them. It never merges with the catalog and never touches hardware.
 */
class EqLibraryBundleStore(
    private val documentStore: ExportDocumentStore,
    private val savedEqs: BundleRowSource<SavedEqTransfer>,
    private val generalEqs: BundleRowSource<GeneralEqTransfer>,
    private val sourcePackage: String,
    private val sourceVersionName: String,
    private val sourceVersionCode: Int,
    private val codec: EqLibraryBundleCodec = EqLibraryBundleCodec(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun exportTo(documentUri: String): EqLibraryExportResult = withContext(ioDispatcher) {
        val document = when (val lookup = documentStore.openDocument(documentUri)) {
            is ExportLookup.Found -> lookup.value
            ExportLookup.Missing, ExportLookup.Unavailable ->
                return@withContext EqLibraryExportResult(0, 0, 0, "That file could not be opened. Nothing was written.")
        }
        val saved = savedEqs.exportBundleRows()
        val general = generalEqs.exportBundleRows()
        val bundle = EqLibraryBundle(
            format = EqLibraryBundle.FORMAT_ID,
            exportedAtMillis = nowMillis(),
            sourcePackage = sourcePackage,
            sourceVersionName = sourceVersionName,
            sourceVersionCode = sourceVersionCode,
            savedEqs = saved,
            generalEqs = general,
        )
        val bytes = codec.encode(bundle).toByteArray(Charsets.UTF_8)
        if (!documentStore.writeBytes(document, bytes)) {
            return@withContext EqLibraryExportResult(0, 0, 0, "The file could not be written. Nothing was exported.")
        }
        EqLibraryExportResult(
            savedEqs = saved.size,
            generalEqs = general.size,
            skipped = 0,
            summary = exportSummary(saved.size, general.size),
        )
    }

    suspend fun importFrom(documentUri: String): EqLibraryImportResult = withContext(ioDispatcher) {
        val document = when (val lookup = documentStore.openDocument(documentUri)) {
            is ExportLookup.Found -> lookup.value
            ExportLookup.Missing, ExportLookup.Unavailable ->
                return@withContext EqLibraryImportResult(summary = "That file could not be opened. Nothing was imported.")
        }
        val raw = documentStore.readBytes(document, MAX_BUNDLE_BYTES)
            ?: return@withContext EqLibraryImportResult(summary = "That file could not be read. Nothing was imported.")
        val bundle = try {
            codec.decode(raw.toString(Charsets.UTF_8))
        } catch (error: IllegalArgumentException) {
            return@withContext EqLibraryImportResult(
                summary = error.message ?: "This file is not an EQ Library backup. Nothing was imported.",
            )
        }
        if (bundle.savedEqs.isEmpty() && bundle.generalEqs.isEmpty()) {
            return@withContext EqLibraryImportResult(summary = "That backup is empty. Nothing was imported.")
        }

        val savedOutcome = savedEqs.importBundleRows(bundle.savedEqs)
        val generalOutcome = generalEqs.importBundleRows(bundle.generalEqs)
        val failures = (savedOutcome.failures + generalOutcome.failures).take(MAX_REPORTED_FAILURES)
        EqLibraryImportResult(
            savedEqsImported = savedOutcome.imported,
            savedEqsAlreadyPresent = savedOutcome.alreadyPresent,
            savedEqsRestoredAsPersonal = savedOutcome.restoredAsPersonal,
            generalEqsImported = generalOutcome.imported,
            generalEqsAlreadyPresent = generalOutcome.alreadyPresent,
            skippedCount = savedOutcome.skippedKinds + generalOutcome.skippedKinds,
            failures = failures,
            summary = importSummary(
                savedImported = savedOutcome.imported,
                generalImported = generalOutcome.imported,
                alreadyPresent = savedOutcome.alreadyPresent + generalOutcome.alreadyPresent,
                restoredAsPersonal = savedOutcome.restoredAsPersonal,
                failures = failures,
            ),
        )
    }

    private fun exportSummary(savedCount: Int, generalCount: Int): String {
        if (savedCount == 0 && generalCount == 0) {
            return "There is nothing to export yet."
        }
        val parts = buildList {
            if (savedCount > 0) add(plural(savedCount, "saved EQ"))
            if (generalCount > 0) add(plural(generalCount, "General EQ"))
        }
        return "Exported " + parts.joinToString(" and ") + "."
    }

    private fun importSummary(
        savedImported: Int,
        generalImported: Int,
        alreadyPresent: Int,
        restoredAsPersonal: Int,
        failures: List<String>,
    ): String {
        val total = savedImported + generalImported
        if (total == 0 && alreadyPresent == 0 && failures.isEmpty()) {
            return "Nothing was imported."
        }
        val parts = buildList {
            if (total > 0) add("Imported " + plural(total, "EQ"))
            if (alreadyPresent > 0) add("$alreadyPresent already here")
            if (restoredAsPersonal > 0) {
                add("$restoredAsPersonal restored as Personal because the catalog profile is not in this version")
            }
            if (failures.isNotEmpty()) add("${failures.size} could not be read")
        }
        return parts.joinToString(". ", postfix = ".")
    }

    private fun plural(count: Int, noun: String): String =
        if (count == 1) "1 $noun" else "$count ${noun}s"

    companion object {
        /** A backup larger than this is not one we wrote; refuse it rather than risk the heap. */
        const val MAX_BUNDLE_BYTES: Int = 4 * 1024 * 1024
        private const val MAX_REPORTED_FAILURES = 3
    }
}

