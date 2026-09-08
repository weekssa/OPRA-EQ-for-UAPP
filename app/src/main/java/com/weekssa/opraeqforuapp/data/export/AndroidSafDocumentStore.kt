package com.weekssa.opraeqforuapp.data.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest

class AndroidSafDocumentStore(context: Context) : ExportDocumentStore {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    override fun openWritableTree(treeUri: String): ExportDirectoryHandle? {
        val uri = treeUri.toUriOrNull() ?: return null
        val document = runCatching { DocumentFile.fromTreeUri(appContext, uri) }.getOrNull() ?: return null
        return document
            .takeIf { it.exists() && it.isDirectory && it.canWrite() }
            ?.let(::AndroidDirectoryHandle)
    }

    override fun openDocument(documentUri: String): ExportDocumentHandle? {
        val uri = documentUri.toUriOrNull() ?: return null
        val document = runCatching { DocumentFile.fromSingleUri(appContext, uri) }.getOrNull() ?: return null
        return document.takeIf { it.exists() && it.isFile }?.let(::AndroidDocumentHandle)
    }

    override fun findDirectory(
        parent: ExportDirectoryHandle,
        name: String,
    ): ExportDirectoryHandle? = parent.androidDirectoryOrNull()
        ?.findFileSafely(name)
        ?.takeIf(DocumentFile::isDirectory)
        ?.let(::AndroidDirectoryHandle)

    override fun createDirectory(
        parent: ExportDirectoryHandle,
        name: String,
    ): ExportDirectoryHandle? = runCatching {
        parent.androidDirectoryOrNull()?.createDirectory(name)
    }.getOrNull()
        ?.takeIf(DocumentFile::isDirectory)
        ?.let(::AndroidDirectoryHandle)

    override fun findFile(
        parent: ExportDirectoryHandle,
        name: String,
    ): ExportDocumentHandle? = parent.androidDirectoryOrNull()
        ?.findFileSafely(name)
        ?.takeIf(DocumentFile::isFile)
        ?.let(::AndroidDocumentHandle)

    override fun createFile(
        parent: ExportDirectoryHandle,
        mimeType: String,
        displayName: String,
    ): ExportDocumentHandle? = runCatching {
        parent.androidDirectoryOrNull()?.createFile(mimeType, displayName)
    }.getOrNull()
        ?.takeIf(DocumentFile::isFile)
        ?.let(::AndroidDocumentHandle)

    override fun contentHash(document: ExportDocumentHandle): String? {
        val uri = document.androidDocumentOrNull()?.uri ?: return null
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
        val uri = document.androidDocumentOrNull()?.uri ?: return null
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
        val uri = document.androidDocumentOrNull()?.uri ?: return false
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

    override fun delete(document: ExportDocumentHandle): Boolean =
        runCatching { document.androidDocumentOrNull()?.delete() == true }.getOrDefault(false)

    override fun deleteByUri(documentUri: String): Boolean {
        val uri = documentUri.toUriOrNull() ?: return false
        return try {
            DocumentsContract.deleteDocument(resolver, uri)
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun ExportDirectoryHandle.androidDirectoryOrNull(): DocumentFile? =
        (this as? AndroidDirectoryHandle)?.document

    private fun ExportDocumentHandle.androidDocumentOrNull(): DocumentFile? =
        (this as? AndroidDocumentHandle)?.document

    private fun DocumentFile.findFileSafely(name: String): DocumentFile? =
        runCatching { findFile(name) }.getOrNull()

    private fun String.toUriOrNull(): Uri? = runCatching(Uri::parse).getOrNull()

    private class AndroidDirectoryHandle(val document: DocumentFile) : ExportDirectoryHandle {
        override val uri: String = document.uri.toString()
    }

    private class AndroidDocumentHandle(val document: DocumentFile) : ExportDocumentHandle {
        override val uri: String = document.uri.toString()
        override val name: String? = document.name
    }
}
