package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import kotlinx.coroutines.launch

@Composable
fun ManagedHeadphoneDetailScreen(
    headphone: ManagedHeadphoneRecord,
    catalogState: CatalogState,
    profileVisibility: ProfileVisibilityPreferences,
    favoriteProfileIds: Set<String>,
    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,
    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,
    onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit,
    onRemoveHeadphone: suspend (String) -> Unit,
    onRemoveManagedProfile: suspend (String, String, Boolean) -> PresetCleanupSummary?,
    onRemoveManagedHeadphone: suspend (String, Boolean) -> PresetCleanupSummary?,
    onDeleteSavedFilesForProfiles: suspend (Set<String>) -> PresetCleanupSummary,
    onDeleteSavedFilesForProduct: suspend (String) -> PresetCleanupSummary,
    onMarkReviewed: suspend (String) -> Unit,
    onExportProduct: (String) -> Unit,
    onMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember(headphone.productId) { mutableStateOf(false) }
    val readyCatalog = catalogState as? CatalogState.Ready
    val product = readyCatalog?.catalog?.product(headphone.productId)
    val availableProfileCount = readyCatalog?.catalog?.profileCount(headphone.productId)
    val displayedProfiles = remember(headphone.profiles) {
        headphone.profiles.filter { it.selected || it.noLongerAvailable }
    }

    LaunchedEffect(headphone.productId) {
        onMarkReviewed(headphone.productId)
    }

    if (editing && readyCatalog != null && product != null) {
        ProfileSelectionEditor(
            catalog = readyCatalog.catalog,
            product = product,
            profileVisibility = profileVisibility,
            favoriteProfileIds = favoriteProfileIds,
            onToggleFavorite = onToggleFavorite,
            onLoadManagedHeadphone = onLoadManagedHeadphone,
            onSaveSelection = onSaveSelection,
            onRemoveHeadphone = onRemoveHeadphone,
            onDeleteSavedFilesForProfiles = onDeleteSavedFilesForProfiles,
            onDeleteSavedFilesForProduct = onDeleteSavedFilesForProduct,
            onExportProduct = onExportProduct,
            onMessage = onMessage,
            onBack = { editing = false },
            modifier = modifier,
        )
        return
    }

    BackHandler(onBack = onBack)
    var pendingProfileRemoval by remember { mutableStateOf<ManagedProfileRecord?>(null) }
    var showHeadphoneRemoval by remember { mutableStateOf(false) }
    var deleteSavedFiles by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    pendingProfileRemoval?.let { profile ->
        RemovalDialog(
            title = "Remove preset?",
            body = "This preset will be removed from this headphone.",
            deleteSavedFiles = deleteSavedFiles,
            onDeleteSavedFilesChange = { deleteSavedFiles = it },
            confirmLabel = "Remove preset",
            onConfirm = {
                pendingProfileRemoval = null
                scope.launch {
                    val cleanup = onRemoveManagedProfile(
                        headphone.productId,
                        profile.profileId,
                        deleteSavedFiles,
                    )
                    if (cleanup != null && cleanup.failedCount > 0) {
                        onMessage("Preset was removed locally, but ${cleanup.failedCount} exported files could not be removed.")
                    }
                }
            },
            onDismiss = { pendingProfileRemoval = null },
        )
    }

    if (showHeadphoneRemoval) {
        RemovalDialog(
            title = "Remove headphone?",
            body = "${headphone.productName} will be removed from My Headphones.",
            deleteSavedFiles = deleteSavedFiles,
            onDeleteSavedFilesChange = { deleteSavedFiles = it },
            confirmLabel = "Remove headphone",
            onConfirm = {
                showHeadphoneRemoval = false
                scope.launch {
                    val cleanup = onRemoveManagedHeadphone(headphone.productId, deleteSavedFiles)
                    onBack()
                    if (cleanup != null && cleanup.failedCount > 0) {
                        onMessage("Headphone was removed locally, but ${cleanup.failedCount} exported files could not be removed.")
                    }
                }
            },
            onDismiss = { showHeadphoneRemoval = false },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Text("My Headphones", modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            text = headphone.productName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = headphone.vendorName,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = buildString {
                append(headphone.selectedProfileCount)
                append(if (headphone.selectedProfileCount == 1) " selected" else " selected")
                availableProfileCount?.let {
                    append(" · ")
                    append(it)
                    append(" available")
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = if (headphone.autoIncludeNewProfiles) {
                "Automatically include new compatible EQ profiles: On"
            } else {
                "Automatically include new compatible EQ profiles: Off"
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (product != null) {
            Button(
                onClick = { editing = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Manage preset selection")
            }
        } else {
            Text(
                text = "This headphone is no longer present in the current EQ Library catalog. Retained presets remain available until you remove them.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(displayedProfiles, key = ManagedProfileRecord::profileId) { profile ->
                ManagedProfileRow(
                    profile = profile,
                    onRemove = {
                        deleteSavedFiles = false
                        pendingProfileRemoval = profile
                    },
                )
                HorizontalDivider()
            }
        }

        TextButton(
            onClick = {
                deleteSavedFiles = false
                showHeadphoneRemoval = true
            },
            modifier = Modifier.padding(16.dp),
        ) {
            Text("Remove headphone")
        }
    }
}

@Composable
private fun ManagedProfileRow(
    profile: ManagedProfileRecord,
    onRemove: (() -> Unit)?,
) {
    val source = profile.lastKnownProfile
    val compatibility = source.assessCompatibility().category
    ListItem(
        headlineContent = {
            Text(source.author?.takeIf { it.isNotBlank() } ?: "Creator information missing")
        },
        supportingContent = {
            Column {
                source.details?.let { Text(it) }
                when {
                    profile.noLongerAvailable -> Text("No longer available in EQ Library")
                    compatibility == ProfileCompatibility.NotCompatible -> Text("Not compatible · unavailable for selection")
                    compatibility == ProfileCompatibility.CompatibleWithLimitation -> Text("Compatible with limitation")
                    profile.selected -> Text("Selected")
                    profile.explicitlyExcluded -> Text("Not selected · excluded from automatic inclusion")
                    else -> Text("Not selected")
                }
            }
        },
        trailingContent = onRemove?.let { action ->
            { TextButton(onClick = action) { Text("Remove") } }
        },
    )
}

@Composable
private fun RemovalDialog(
    title: String,
    body: String,
    deleteSavedFiles: Boolean,
    onDeleteSavedFilesChange: (Boolean) -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(body)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .toggleable(
                            value = deleteSavedFiles,
                            role = Role.Checkbox,
                            onValueChange = onDeleteSavedFilesChange,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = deleteSavedFiles, onCheckedChange = null)
                    Text(
                        text = "Also delete exported files created by EQ Library for this item.",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text(
                    text = "Files not owned by EQ Library are never deleted.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
