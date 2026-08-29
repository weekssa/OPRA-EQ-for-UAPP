package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import kotlinx.coroutines.launch

@Composable
fun MyEqsScreen(
    savedEqs: List<SavedEqRecord>,
    onImportPersonal: suspend (
        manufacturer: String,
        model: String,
        displayName: String,
        target: String?,
        peqText: String,
    ) -> String?,
    onDeleteSavedEq: suspend (String, Boolean) -> PresetCleanupSummary?,
    onExportSavedEq: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favorites = remember(savedEqs) { savedEqs.filter { it.kind == SavedEqKind.Favorite } }
    val personal = remember(savedEqs) { savedEqs.filter { it.kind == SavedEqKind.Personal } }
    var importOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SavedEqRecord?>(null) }
    var deleteSavedFiles by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (importOpen) {
        PersonalEqImportDialog(
            onDismiss = { importOpen = false },
            onImport = { manufacturer, model, displayName, target, peqText ->
                scope.launch {
                    val error = onImportPersonal(manufacturer, model, displayName, target, peqText)
                    if (error == null) {
                        importOpen = false
                        onMessage("Personal EQ saved to My EQs.")
                    } else {
                        onMessage(error)
                    }
                }
            },
        )
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = {
                pendingDelete = null
                deleteSavedFiles = false
            },
            title = {
                Text(if (record.kind == SavedEqKind.Favorite) "Remove favorite?" else "Delete personal EQ?")
            },
            text = {
                Column {
                    Text(
                        if (record.kind == SavedEqKind.Favorite) {
                            "This removes the saved favorite snapshot from My EQs. It does not change any My Headphones selection."
                        } else {
                            "This removes ${record.displayName} from My EQs."
                        },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .toggleable(
                                value = deleteSavedFiles,
                                role = Role.Checkbox,
                                onValueChange = { deleteSavedFiles = it },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = deleteSavedFiles, onCheckedChange = null)
                        Text(
                            text = "Also delete exported files created by EQ Library for this saved EQ. Unknown files will not be touched.",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val shouldDeleteFiles = deleteSavedFiles
                    pendingDelete = null
                    deleteSavedFiles = false
                    scope.launch {
                        val cleanup = onDeleteSavedEq(record.entryId, shouldDeleteFiles)
                        val base = if (record.kind == SavedEqKind.Favorite) "Favorite removed." else "Personal EQ deleted."
                        val message = when {
                            !shouldDeleteFiles -> "$base Existing exported files were kept."
                            cleanup == null -> base
                            cleanup.failedCount > 0 -> "$base ${cleanup.removedCount} exported files deleted; ${cleanup.failedCount} could not be removed."
                            else -> "$base ${cleanup.removedCount} exported files deleted."
                        }
                        onMessage(message)
                    }
                }) {
                    Text(if (record.kind == SavedEqKind.Favorite) "Remove" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    deleteSavedFiles = false
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { importOpen = true }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("Import PEQ", modifier = Modifier.padding(start = 6.dp))
            }
        }

        Text(
            text = "Personal imports stay on this device and are never submitted to the public EQ Library automatically.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "favorites-heading") {
                SectionHeading("Favorites", favorites.size)
            }
            if (favorites.isEmpty()) {
                item(key = "favorites-empty") {
                    EmptySection("Star an EQ while browsing to keep a saved snapshot here.")
                }
            } else {
                items(favorites, key = { it.entryId }) { record ->
                    SavedEqRow(
                        record = record,
                        leadingFavorite = true,
                        onExport = { onExportSavedEq(record.entryId) },
                        onDelete = {
                            deleteSavedFiles = false
                            pendingDelete = record
                        },
                    )
                    HorizontalDivider()
                }
            }

            item(key = "personal-heading") {
                SectionHeading("Personal imports", personal.size)
            }
            if (personal.isEmpty()) {
                item(key = "personal-empty") {
                    EmptySection("Import EqualizerAPO / AutoEQ-style parametric text to save your own EQs locally.")
                }
            } else {
                items(personal, key = { it.entryId }) { record ->
                    SavedEqRow(
                        record = record,
                        leadingFavorite = false,
                        onExport = { onExportSavedEq(record.entryId) },
                        onDelete = {
                            deleteSavedFiles = false
                            pendingDelete = record
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, count: Int) {
    Text(
        text = "$title · $count",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun EmptySection(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SavedEqRow(
    record: SavedEqRecord,
    leadingFavorite: Boolean,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        leadingContent = if (leadingFavorite) {
            { Icon(Icons.Outlined.Star, contentDescription = null) }
        } else {
            null
        },
        headlineContent = { Text(record.displayName) },
        supportingContent = {
            Column {
                Text("${record.manufacturer} · ${record.model}")
                record.profile.details?.takeIf(String::isNotBlank)?.let { Text(it) }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onExport) {
                    Icon(Icons.Outlined.FileUpload, contentDescription = "Export ${record.displayName}")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Remove ${record.displayName}")
                }
            }
        },
    )
}

@Composable
private fun PersonalEqImportDialog(
    onDismiss: () -> Unit,
    onImport: (String, String, String, String?, String) -> Unit,
) {
    var manufacturer by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var peqText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import personal PEQ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Manufacturer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Headphone model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("EQ name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target / note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = peqText,
                    onValueChange = { peqText = it },
                    label = { Text("Parametric EQ text") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Example: Preamp: -5 dB; Filter 1: ON PK Fc 100 Hz Gain 3 dB Q 0.70")
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onImport(
                        manufacturer,
                        model,
                        displayName,
                        target.takeIf(String::isNotBlank),
                        peqText,
                    )
                },
                enabled = manufacturer.isNotBlank() && model.isNotBlank() &&
                    displayName.isNotBlank() && peqText.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
