package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.runtime.saveable.Saver
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/** Keeps large editor text out of Activity saved-state Bundles. */
internal class PersonalEqInputDraftStore(
    private val filesDirectory: File,
) {
    fun saver(draftId: String): Saver<String, String> = Saver(
        save = { text -> encode(draftId, text) },
        restore = { saved -> decode(draftId, saved) },
    )

    internal fun encode(draftId: String, text: String): String {
        val file = draftFile(draftId)
        if (text.length <= MAX_INLINE_SAVED_CHARACTERS) {
            file.delete()
            return INLINE_PREFIX + text
        }

        return runCatching {
            val parent = requireNotNull(file.parentFile)
            check(parent.isDirectory || parent.mkdirs()) { "Couldn't create the local draft folder." }
            val temporary = File(parent, "${file.name}.tmp")
            try {
                temporary.writeText(text, Charsets.UTF_8)
                try {
                    Files.move(
                        temporary.toPath(),
                        file.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                temporary.delete()
            }
            FILE_PREFIX + draftId
        }.getOrElse {
            RESTORE_FAILURE_MARKER
        }
    }

    internal fun decode(draftId: String, saved: String): String = when {
        saved == RESTORE_FAILURE_MARKER -> RESTORE_FAILURE_MARKER
        saved.startsWith(INLINE_PREFIX) -> saved.removePrefix(INLINE_PREFIX)
        saved == FILE_PREFIX + draftId -> {
            val file = draftFile(draftId)
            runCatching { file.readText(Charsets.UTF_8) }
                .onSuccess { file.delete() }
                .getOrElse { RESTORE_FAILURE_MARKER }
        }
        saved.startsWith(FILE_PREFIX) -> RESTORE_FAILURE_MARKER
        else -> saved // Accept a small draft saved by a prior app version.
    }

    internal fun backingFile(draftId: String): File = draftFile(draftId)

    internal fun delete(draftId: String) {
        val file = draftFile(draftId)
        file.delete()
        File(requireNotNull(file.parentFile), "${file.name}.tmp").delete()
    }

    private fun draftFile(draftId: String): File {
        val validId = runCatching { UUID.fromString(draftId) }.getOrNull()
            ?: error("Invalid Personal EQ draft key.")
        return File(File(filesDirectory, DRAFT_DIRECTORY), "$validId.txt")
    }

    internal companion object {
        const val DRAFT_DIRECTORY = "personal-eq-import-drafts"
        const val INLINE_PREFIX = "\u0000peq-inline:"
        const val FILE_PREFIX = "\u0000peq-file:"
        const val MAX_INLINE_SAVED_CHARACTERS = 8_192
        const val RESTORE_FAILURE_MARKER = "\u0000peq-restore-failed"
    }
}
