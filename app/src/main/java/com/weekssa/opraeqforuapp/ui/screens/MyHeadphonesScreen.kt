package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import kotlinx.coroutines.launch

@Composable
fun MyHeadphonesScreen(
    catalogState: CatalogState,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    onBrowseOpra: () -> Unit,
    onRefreshCatalog: () -> Unit,
    onExportPresets: () -> Unit,
    onOpenHeadphone: (String) -> Unit,
    onRemoveManagedHeadphone: suspend (String, Boolean) -> PresetCleanupSummary?,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (managedHeadphones.isEmpty()) {
        EmptyMyHeadphones(
            catalogState = catalogState,
            onBrowseOpra = onBrowseOpra,
            onRefreshCatalog = onRefreshCatalog,
            modifier = modifier,
        )
        return
    }

    val grouped = managedHeadphones
        .groupBy(ManagedHeadphoneRecord::vendorName)
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    val totalSelected = managedHeadphones.sumOf(ManagedHeadphoneRecord::selectedProfileCount)
    val scope = rememberCoroutineScope()
    var showClearLibraryDialog by remember { mutableStateOf(false) }
    var deleteSavedFiles by remember { mutableStateOf(false) }

    if (showClearLibraryDialog) {
        AlertDialog(
            onDismissRequest = { showClearLibraryDialog = false },
            title = { Text("Clear entire saved library?") },
            text = {
                Column {
                    Text(
                        "This removes all ${managedHeadphones.size} headphones and their saved preset selections from My Headphones. The source catalog, My EQs, settings, and selected root folder are kept.",
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
                        Checkbox(
                            checked = deleteSavedFiles,
                            onCheckedChange = null,
                        )
                        Text(
                            text = "Also delete exported files EQ Library owns for these headphones. Unknown files and My EQs exports will not be touched.",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val headphonesToRemove = managedHeadphones.toList()
                        val shouldDeleteFiles = deleteSavedFiles
                        showClearLibraryDialog = false
                        deleteSavedFiles = false
                        scope.launch {
                            var removedFiles = 0
                            var failedFiles = 0
                            headphonesToRemove.forEach { headphone ->
                                val cleanup = onRemoveManagedHeadphone(
                                    headphone.productId,
                                    shouldDeleteFiles,
                                )
                                if (cleanup != null) {
                                    removedFiles += cleanup.removedCount
                                    failedFiles += cleanup.failedCount
                                }
                            }
                            val message = when {
                                !shouldDeleteFiles ->
                                    "Saved headphone library cleared. Existing exported files were kept."
                                failedFiles > 0 ->
                                    "Saved headphone library cleared. $removedFiles exported files deleted; $failedFiles could not be removed."
                                else ->
                                    "Saved headphone library cleared. $removedFiles exported files deleted."
                            }
                            onMessage(message)
                        }
                    },
                ) {
                    Text("Clear entire library")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLibraryDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "export") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Button(
                    onClick = onExportPresets,
                    enabled = totalSelected > 0,
                ) {
                    Text("Export presets")
                }
                Text(
                    text = "$totalSelected selected presets across ${managedHeadphones.size} headphones",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }
        grouped.forEach { (manufacturer, headphones) ->
            item(key = "manufacturer:$manufacturer") {
                Text(
                    text = manufacturer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(
                items = headphones.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.productName }),
                key = ManagedHeadphoneRecord::productId,
            ) { headphone ->
                ListItem(
                    headlineContent = { Text(headphone.productName) },
                    supportingContent = {
                        Column {
                            Text("${headphone.selectedProfileCount} selected profiles")
                            attentionSummary(headphone)?.let { summary ->
                                Text(
                                    text = summary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenHeadphone(headphone.productId) },
                )
                HorizontalDivider()
            }
        }
        item(key = "library-cleanup") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                Text(
                    text = "Library cleanup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Remove individual presets or headphones by opening them above. Use this only when you want to clear the entire saved headphone library.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        deleteSavedFiles = false
                        showClearLibraryDialog = true
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("Clear saved library")
                }
            }
        }
    }
}

@Composable
private fun EmptyMyHeadphones(
    catalogState: CatalogState,
    onBrowseOpra: () -> Unit,
    onRefreshCatalog: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No headphones yet",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Choose EQ profiles to build your local preset library.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Your selections stay on this device.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onBrowseOpra,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Browse EQs")
        }

        when (catalogState) {
            CatalogState.Loading -> Row(
                modifier = Modifier.padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "Downloading EQ Library catalog…",
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is CatalogState.Unavailable -> {
                Text(
                    text = unavailableCatalogMessage(catalogState.reason),
                    modifier = Modifier.padding(top = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onRefreshCatalog) {
                    Text("Try again")
                }
            }
            is CatalogState.Ready -> Unit
        }
    }
}

private fun attentionSummary(headphone: ManagedHeadphoneRecord): String? {
    val newCount = headphone.profiles.count { it.isNewUnreviewed }
    val updatedCount = headphone.profiles.count { it.isUpdatedUnreviewed }
    val removedCount = headphone.profiles.count { it.noLongerAvailable }
    return buildList {
        if (newCount > 0) add("$newCount new")
        if (updatedCount > 0) add("$updatedCount updated")
        if (removedCount > 0) add("$removedCount no longer available in the EQ Library catalog")
    }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

internal fun unavailableCatalogMessage(reason: CatalogRefreshFailureReason): String = when (reason) {
    CatalogRefreshFailureReason.Network -> "The EQ Library catalog couldn’t be downloaded. Check your connection and try again."
    CatalogRefreshFailureReason.InvalidCatalog -> "The downloaded EQ Library catalog couldn’t be processed."
    CatalogRefreshFailureReason.Storage -> "The EQ Library catalog couldn’t be saved on this device."
}
