package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.export.ExportCurrentness
import com.weekssa.opraeqforuapp.domain.blackpearl.isBlackPearlDirectFlashable
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqCategory
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import kotlinx.coroutines.launch

private sealed interface PendingBlackPearlFlash {
    val displayName: String

    data class SavedEq(
        val entryId: String,
        override val displayName: String,
    ) : PendingBlackPearlFlash

    data class GeneralEq(
        val presetId: String,
        override val displayName: String,
    ) : PendingBlackPearlFlash
}

@Composable
fun MyEqsHomeScreen(
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    savedGeneralEqs: List<SavedGeneralEqRecord>,
    activeOutput: ExportDevice,
    exportCurrentness: ExportCurrentness,
    directBlackPearlFlashEnabled: Boolean,
    blackPearlConnectionState: BlackPearlConnectionState,
    onConnectBlackPearl: () -> Unit,
    onExportAll: () -> Unit,
    onOpenHeadphone: (String) -> Unit,
    onImportPersonal: suspend (
        manufacturer: String,
        model: String,
        displayName: String,
        target: String?,
        peqText: String,
    ) -> String?,
    onDeleteSavedEq: suspend (String) -> Unit,
    onExportSavedEq: (String) -> Unit,
    onFlashSavedEq: suspend (String) -> String,
    onRemoveGeneralEq: suspend (String) -> Unit,
    onExportGeneralEq: (String) -> Unit,
    onFlashGeneralEq: suspend (String) -> String,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var importOpen by remember { mutableStateOf(false) }
    var pendingFlash by remember { mutableStateOf<PendingBlackPearlFlash?>(null) }
    val selectedHeadphoneCount = managedHeadphones.sumOf(ManagedHeadphoneRecord::selectedProfileCount)
    val headphoneSavedEqs = remember(savedEqs) { savedEqs.toList() }
    val blackPearlConnected = blackPearlConnectionState is BlackPearlConnectionState.Connected
    val flashActionsEnabled = activeOutput == ExportDevice.BLACK_PEARL &&
        directBlackPearlFlashEnabled && blackPearlConnected

    if (importOpen) {
        PersonalEqDialog(
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

    pendingFlash?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingFlash = null },
            title = { Text("Flash to Black Pearl?") },
            text = {
                Text(
                    "Flash ${pending.displayName} to the Black Pearl's current EQ slot? " +
                        "This overwrites that EQ slot. Other DAC settings, including global volume, are not changed.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingFlash = null
                        scope.launch {
                            val message = when (pending) {
                                is PendingBlackPearlFlash.SavedEq -> onFlashSavedEq(pending.entryId)
                                is PendingBlackPearlFlash.GeneralEq -> onFlashGeneralEq(pending.presetId)
                            }
                            onMessage(message)
                        }
                    },
                ) { Text("Flash") }
            },
            dismissButton = {
                TextButton(onClick = { pendingFlash = null }) { Text("Cancel") }
            },
        )
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "my-eqs-actions") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (activeOutput == ExportDevice.BLACK_PEARL) {
                    BlackPearlConnectionControl(
                        enabled = directBlackPearlFlashEnabled,
                        state = blackPearlConnectionState,
                        onConnect = onConnectBlackPearl,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (exportCurrentness.hasPendingExport) {
                        Button(onClick = onExportAll) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null)
                            Text("Export all", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    OutlinedButton(onClick = { importOpen = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("Import PEQ", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Text(
                    text = exportStatusText(exportCurrentness, activeOutput),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$selectedHeadphoneCount headphone EQs · ${savedGeneralEqs.size} General EQs",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }

        item(key = "headphones-heading") {
            SectionHeading("Headphones")
        }
        if (managedHeadphones.isEmpty() && headphoneSavedEqs.isEmpty()) {
            item(key = "headphones-empty") {
                EmptyMessage("No headphone EQs saved for this output yet. Add them from EQ Library.")
            }
        } else {
            managedHeadphones
                .groupBy(ManagedHeadphoneRecord::vendorName)
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                .forEach { (manufacturer, headphones) ->
                    item(key = "manufacturer:$manufacturer") {
                        Text(
                            text = manufacturer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(
                        items = headphones.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.productName }),
                        key = { "managed:${it.productId}" },
                    ) { headphone ->
                        val pendingCount = headphone.profiles.count { profile ->
                            profile.selected && exportCurrentness.needsExport(headphone.productId, profile.profileId)
                        }
                        ListItem(
                            headlineContent = { Text(headphone.productName) },
                            supportingContent = {
                                Text(
                                    if (pendingCount > 0) {
                                        "${headphone.selectedProfileCount} selected · $pendingCount file ${if (pendingCount == 1) "export" else "exports"} pending"
                                    } else {
                                        "${headphone.selectedProfileCount} selected profiles"
                                    },
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenHeadphone(headphone.productId) },
                        )
                        HorizontalDivider()
                    }
                }

            if (headphoneSavedEqs.isNotEmpty()) {
                item(key = "saved-headphone-heading") {
                    Text(
                        text = "Saved snapshots & personal imports",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(headphoneSavedEqs, key = { "saved:${it.entryId}" }) { record ->
                    val needsExport = exportCurrentness.needsExport(record.productId, record.profile.id)
                    val blackPearlFlashable = record.profile.isBlackPearlDirectFlashable()
                    ListItem(
                        leadingContent = if (record.kind == SavedEqKind.Favorite) {
                            { Icon(Icons.Outlined.Star, contentDescription = null) }
                        } else {
                            null
                        },
                        headlineContent = { Text(record.displayName) },
                        supportingContent = { Text("${record.manufacturer} · ${record.model}") },
                        trailingContent = {
                            Row {
                                if (needsExport) {
                                    IconButton(onClick = { onExportSavedEq(record.entryId) }) {
                                        Icon(Icons.Outlined.FileUpload, contentDescription = "Export ${record.displayName}")
                                    }
                                }
                                if (activeOutput == ExportDevice.BLACK_PEARL) {
                                    TextButton(
                                        enabled = flashActionsEnabled && blackPearlFlashable,
                                        onClick = {
                                            pendingFlash = PendingBlackPearlFlash.SavedEq(
                                                entryId = record.entryId,
                                                displayName = record.displayName,
                                            )
                                        },
                                    ) { Text("Flash") }
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            onDeleteSavedEq(record.entryId)
                                            onMessage("EQ removed from My EQs. Existing exported files were kept.")
                                        }
                                    },
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Remove ${record.displayName}")
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }

        item(key = "general-heading") {
            SectionHeading("General EQs")
        }
        if (savedGeneralEqs.isEmpty()) {
            item(key = "general-empty") {
                EmptyMessage("No General EQs saved for this output yet. Add them from EQ Library → General EQs.")
            }
        } else {
            items(savedGeneralEqs, key = { "general:${it.presetId}" }) { record ->
                val needsExport = exportCurrentness.needsExport(generalExportProductId(record.presetId), record.presetId)
                val blackPearlFlashable = record.profile.isBlackPearlDirectFlashable()
                ListItem(
                    headlineContent = { Text(record.displayName) },
                    supportingContent = {
                        Column {
                            Text(generalCategoryLabel(record.category))
                            record.profile.details?.takeIf(String::isNotBlank)?.let { Text(it) }
                        }
                    },
                    trailingContent = {
                        Row {
                            if (needsExport) {
                                IconButton(onClick = { onExportGeneralEq(record.presetId) }) {
                                    Icon(Icons.Outlined.FileUpload, contentDescription = "Export ${record.displayName}")
                                }
                            }
                            if (activeOutput == ExportDevice.BLACK_PEARL) {
                                TextButton(
                                    enabled = flashActionsEnabled && blackPearlFlashable,
                                    onClick = {
                                        pendingFlash = PendingBlackPearlFlash.GeneralEq(
                                            presetId = record.presetId,
                                            displayName = record.displayName,
                                        )
                                    },
                                ) { Text("Flash") }
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        onRemoveGeneralEq(record.presetId)
                                        onMessage("${record.displayName} removed from this output. Existing exported files were kept.")
                                    }
                                },
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remove ${record.displayName}")
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun BlackPearlConnectionControl(
    enabled: Boolean,
    state: BlackPearlConnectionState,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        if (!enabled) {
            OutlinedButton(onClick = {}, enabled = false) { Text("Direct Flash disabled") }
            Text(
                text = "Enable direct Flash in Settings → Black Pearl before connecting to the DAC.",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val connected = state is BlackPearlConnectionState.Connected
        val connecting = state is BlackPearlConnectionState.Connecting
        val containerColor = if (connected) CONNECTED_GREEN else MaterialTheme.colorScheme.error
        Button(
            onClick = onConnect,
            enabled = !connected && !connecting,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = Color.White,
                disabledContainerColor = if (connected) CONNECTED_GREEN else MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = if (connected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(
                when {
                    connected -> "Connected"
                    connecting -> "Connecting…"
                    else -> "Connect to DAC"
                },
            )
        }
        if (state is BlackPearlConnectionState.Error) {
            Text(
                text = state.message,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun EmptyMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PersonalEqDialog(
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
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun exportStatusText(
    exportCurrentness: ExportCurrentness,
    activeOutput: ExportDevice,
): String {
    val pendingCount = exportCurrentness.needsExportItems.size
    return when {
        exportCurrentness.hasPendingExport -> {
            val noun = if (pendingCount == 1) "EQ file" else "EQ files"
            if (activeOutput == ExportDevice.BLACK_PEARL) {
                "$pendingCount selected $noun still need${if (pendingCount == 1) "s" else ""} export for ${outputTitle(activeOutput)}. File export is separate from direct Flash."
            } else {
                "$pendingCount selected $noun still need${if (pendingCount == 1) "s" else ""} export for ${outputTitle(activeOutput)}."
            }
        }
        exportCurrentness.hasAnythingToExport -> {
            if (activeOutput == ExportDevice.BLACK_PEARL) {
                "All exported Black Pearl preset files are current. File export is separate from direct Flash."
            } else {
                "All exportable EQ files are current for ${outputTitle(activeOutput)}."
            }
        }
        else -> "No saved EQs are exportable to ${outputTitle(activeOutput)}."
    }
}

private fun generalCategoryLabel(category: GeneralEqCategory): String = when (category) {
    GeneralEqCategory.SOUND -> "Sound"
    GeneralEqCategory.GENRE -> "Genre"
    GeneralEqCategory.UTILITY -> "Utility"
}

private fun generalExportProductId(presetId: String): String = "general-export:$presetId"

private fun outputTitle(device: ExportDevice): String = when (device) {
    ExportDevice.UAPP -> "USB Audio Player PRO / ToneBoosters"
    ExportDevice.BLACK_PEARL -> "TRN Black Pearl"
    ExportDevice.UNIVERSAL_PARAMETRIC -> "Universal Parametric EQ"
    ExportDevice.POWERAMP -> "Poweramp / Poweramp Equalizer"
    ExportDevice.WAVELET -> "Wavelet"
    ExportDevice.TOPPING_DX5_II -> "TOPPING DX5 II"
    ExportDevice.TOPPING_DX1_II -> "TOPPING DX1 II"
}

private val CONNECTED_GREEN = Color(0xFF2E7D32)
