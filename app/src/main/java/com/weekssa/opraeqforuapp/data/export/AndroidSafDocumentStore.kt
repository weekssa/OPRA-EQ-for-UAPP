package com.weekssa.opraeqforuapp.data.export

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest

class AndroidSafDocumentStore(context: Context) : ExportDocumentStore {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    override fun openWritableTree(treeUri: String): ExportDirectoryHandle? {
        val tree = treeUri.toUriOrNull() ?: return null
        return try {
            val rootUri = DocumentsContract.buildDocumentUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree),
            )
            when (val lookup = queryDocument(rootUri)) {
                is ExportLookup.Found -> lookup.value
                    .takeIf { row -> row.isDirectory && row.canWrite() }
                    ?.let { AndroidDirectoryHandle(rootUri) }
                ExportLookup.Missing,
                ExportLookup.Unavailable,
                -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun openDocument(documentUri: String): ExportLookup<ExportDocumentHandle> {
        val uri = documentUri.toUriOrNull() ?: return ExportLookup.Unavailable
        return when (val lookup = queryDocument(uri)) {
            is ExportLookup.Found -> {
                if (lookup.value.isDirectory) {
                    ExportLookup.Unavailable
                } else {
                    ExportLookup.Found(
                        AndroidDocumentHandle(
                            androidUri = uri,
                            name = lookup.value.name,
                        ),
                    )
                }
            }
            ExportLookup.Missing -> ExportLookup.Missing
            ExportLookup.Unavailable -> ExportLookup.Unavailable
        }
    }

    override fun findDirectory(
        parent: ExportDirectoryHandle,
        name: String,
    ): ExportLookup<ExportDirectoryHandle> {
        val parentUri = parent.androidDirectoryUriOrNull() ?: return ExportLookup.Unavailable
        return when (val lookup = findChild(parentUri, name)) {
            is ExportLookup.Found -> {
                if (!lookup.value.isDirectory) {
                    ExportLookup.Unavailable
                } else {
                    ExportLookup.Found(AndroidDirectoryHandle(lookup.value.uri))
                }
            }
            ExportLookup.Missing -> ExportLookup.Missing
            ExportLookup.Unavailable -> ExportLookup.Unavailable
        }
    }

    override fun createDirectory(
        parent: ExportDirectoryHandle,
        name: String,
    ): ExportDirectoryHandle? {
        val parentUri = parent.androidDirectoryUriOrNull() ?: return null
        return try {
            DocumentsContract.createDocument(
                resolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                name,
            )?.let(::AndroidDirectoryHandle)
        } catch (_: Exception) {
            null
        }
    }

    override fun findFile(
        parent: ExportDirectoryHandle,
        name: String,
    ): ExportLookup<ExportDocumentHandle> {
        val parentUri = parent.androidDirectoryUriOrNull() ?: return ExportLookup.Unavailable
        return when (val lookup = findChild(parentUri, name)) {
            is ExportLookup.Found -> {
                if (lookup.value.isDirectory) {
                    ExportLookup.Unavailable
                } else {
                    ExportLookup.Found(
                        AndroidDocumentHandle(
                            androidUri = lookup.value.uri,
                            name = lookup.value.name,
                        ),
                    )
                }
            }
            ExportLookup.Missing -> ExportLookup.Missing
            ExportLookup.Unavailable -> ExportLookup.Unavailable
        }
    }

    override fun createFile(
        parent: ExportDirectoryHandle,
        mimeType: String,
        displayName: String,
    ): ExportDocumentHandle? {
        val parentUri = parent.androidDirectoryUriOrNull() ?: return null
        val createdUri = try {
            DocumentsContract.createDocument(resolver, parentUri, mimeType, displayName)
        } catch (_: Exception) {
            null
        } ?: return null

        val actualName = when (val lookup = queryDocument(createdUri)) {
            is ExportLookup.Found -> lookup.value.name
            ExportLookup.Missing,
            ExportLookup.Unavailable,
            -> null
        }
        return AndroidDocumentHandle(createdUri, actualName)
    }

    override fun contentHash(document: ExportDocumentHandle): String? {
        val uri = document.androidDocumentUriOrNull() ?: return null
        return try {
            resolver.openInputStream(uri)?.use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
                digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun readBytes(document: ExportDocumentHandle, maxBytes: Int): ByteArray? {
        val uri = document.androidDocumentUriOrNull() ?: return null
        return try {
            resolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun writeBytes(document: ExportDocumentHandle, bytes: ByteArray): Boolean {
        val uri = document.androidDocumentUriOrNull() ?: return false
        return try {
            resolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(bytes)
                output.flush()
            } != null
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    override fun delete(document: ExportDocumentHandle): Boolean {
        val uri = document.androidDocumentUriOrNull() ?: return false
        return deleteUri(uri)
    }

    override fun deleteByUri(documentUri: String): Boolean {
        val uri = documentUri.toUriOrNull() ?: return false
        return deleteUri(uri)
    }

    private fun findChild(parentUri: Uri, name: String): ExportLookup<DocumentRow> {
        val childrenUri = try {
            DocumentsContract.buildChildDocumentsUriUsingTree(
                parentUri,
                DocumentsContract.getDocumentId(parentUri),
            )
        } catch (_: Exception) {
            return ExportLookup.Unavailable
        }

        return try {
            resolver.query(childrenUri, CHILD_PROJECTION, null, null, null)?.use { cursor ->
                val idIndex = cursor.requireColumn(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    ?: return@use ExportLookup.Unavailable
                val nameIndex = cursor.requireColumn(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    ?: return@use ExportLookup.Unavailable
                val mimeIndex = cursor.requireColumn(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    ?: return@use ExportLookup.Unavailable
                val flagsIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)

                var match: DocumentRow? = null
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) != name) continue
                    if (match != null) return@use ExportLookup.Unavailable
                    val documentId = cursor.getString(idIndex) ?: return@use ExportLookup.Unavailable
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, documentId)
                    match = DocumentRow(
                        uri = childUri,
                        name = cursor.getString(nameIndex),
                        mimeType = cursor.getString(mimeIndex),
                        flags = cursor.longOrZero(flagsIndex),
                    )
                }
                match?.let { row -> ExportLookup.Found(row) } ?: ExportLookup.Missing
            } ?: ExportLookup.Unavailable
        } catch (_: Exception) {
            ExportLookup.Unavailable
        }
    }

    private fun queryDocument(uri: Uri): ExportLookup<DocumentRow> = try {
        resolver.query(uri, DOCUMENT_PROJECTION, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use ExportLookup.Missing
            val nameIndex = cursor.requireColumn(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                ?: return@use ExportLookup.Unavailable
            val mimeIndex = cursor.requireColumn(DocumentsContract.Document.COLUMN_MIME_TYPE)
                ?: return@use ExportLookup.Unavailable
            val flagsIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)
            ExportLookup.Found(
                DocumentRow(
                    uri = uri,
                    name = cursor.getString(nameIndex),
                    mimeType = cursor.getString(mimeIndex),
                    flags = cursor.longOrZero(flagsIndex),
                ),
            )
        } ?: ExportLookup.Unavailable
    } catch (_: Exception) {
        ExportLookup.Unavailable
    }

    private fun DocumentRow.canWrite(): Boolean {
        if (
            appContext.checkCallingOrSelfUriPermission(
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (mimeType.isNullOrBlank()) return false
        return if (isDirectory) {
            (flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE.toLong()) != 0L
        } else {
            (flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE.toLong()) != 0L
        }
    }

    private fun deleteUri(uri: Uri): Boolean = try {
        DocumentsContract.deleteDocument(resolver, uri)
    } catch (_: Exception) {
        false
    }

    private fun Cursor.requireColumn(name: String): Int? =
        getColumnIndex(name).takeIf { it >= 0 }

    private fun Cursor.longOrZero(index: Int): Long =
        if (index >= 0 && !isNull(index)) getLong(index) else 0L

    private fun ExportDirectoryHandle.androidDirectoryUriOrNull(): Uri? =
        (this as? AndroidDirectoryHandle)?.androidUri

    private fun ExportDocumentHandle.androidDocumentUriOrNull(): Uri? =
        (this as? AndroidDocumentHandle)?.androidUri

    private fun String.toUriOrNull(): Uri? = runCatching(Uri::parse).getOrNull()

    private data class DocumentRow(
        val uri: Uri,
        val name: String?,
        val mimeType: String?,
        val flags: Long,
    ) {
        val isDirectory: Boolean
            get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    }

    private class AndroidDirectoryHandle(val androidUri: Uri) : ExportDirectoryHandle {
        override val uri: String = androidUri.toString()
    }

    private class AndroidDocumentHandle(
        val androidUri: Uri,
        override val name: String?,
    ) : ExportDocumentHandle {
        override val uri: String = androidUri.toString()
    }

    companion object {
        private val DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
        )
        private val CHILD_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
        )
    }
}