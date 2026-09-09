package com.weekssa.opraeqforuapp.data.export

/** Opaque writable directory owned by an [ExportDocumentStore]. */
interface ExportDirectoryHandle {
    val uri: String
}

/** Opaque document owned by an [ExportDocumentStore]. */
interface ExportDocumentHandle {
    val uri: String
    val name: String?
}

/**
 * Result of a SAF lookup.
 *
 * Missing is materially different from Unavailable: ownership may be forgotten only when a
 * recorded document/path is confirmed missing, never when access or provider querying failed.
 */
sealed interface ExportLookup<out T> {
    data class Found<T>(val value: T) : ExportLookup<T>
    data object Missing : ExportLookup<Nothing>
    data object Unavailable : ExportLookup<Nothing>
}

/**
 * Platform boundary for Storage Access Framework operations used by preset export and cleanup.
 *
 * Repository code depends on this contract rather than Android Context, ContentResolver, Uri, or
 * DocumentFile. Implementations are invoked from repository IO dispatchers.
 */
interface ExportDocumentStore {
    fun openWritableTree(treeUri: String): ExportDirectoryHandle?

    fun openDocument(documentUri: String): ExportLookup<ExportDocumentHandle>

    fun findDirectory(parent: ExportDirectoryHandle, name: String): ExportLookup<ExportDirectoryHandle>

    fun createDirectory(parent: ExportDirectoryHandle, name: String): ExportDirectoryHandle?

    fun findFile(parent: ExportDirectoryHandle, name: String): ExportLookup<ExportDocumentHandle>

    fun createFile(
        parent: ExportDirectoryHandle,
        mimeType: String,
        displayName: String,
    ): ExportDocumentHandle?

    fun contentHash(document: ExportDocumentHandle): String?

    fun readBytes(document: ExportDocumentHandle, maxBytes: Int): ByteArray?

    fun writeBytes(document: ExportDocumentHandle, bytes: ByteArray): Boolean

    fun delete(document: ExportDocumentHandle): Boolean

    fun deleteByUri(documentUri: String): Boolean
}
