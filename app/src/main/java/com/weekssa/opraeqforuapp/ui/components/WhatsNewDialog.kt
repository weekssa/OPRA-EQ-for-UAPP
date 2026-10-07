package com.weekssa.opraeqforuapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R

@Composable
fun WhatsNewDialog(
    version: String,
    notes: String,
    onDismiss: () -> Unit,
) {
    val unavailableNotes = stringResource(R.string.whats_new_notes_unavailable)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_new_title, version)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                ReleaseNoteContent(notes.ifBlank { unavailableNotes })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun ReleaseNoteContent(notes: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        notes.replace("\r\n", "\n").lines().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                line.startsWith("#") -> Text(
                    text = line.trimStart('#').trim(),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
                line.startsWith("- ") || line.startsWith("* ") -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("•", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        cleanInlineMarkdown(line.drop(2)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                line.matches(Regex("^\\d+\\. .+")) -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val separator = line.indexOf('.')
                    Text(line.substring(0, separator + 1), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        cleanInlineMarkdown(line.substring(separator + 1).trim()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> Text(cleanInlineMarkdown(line), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun cleanInlineMarkdown(text: String): String = text
    .replace("**", "")
    .replace("__", "")
    .replace("`", "")
    .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
