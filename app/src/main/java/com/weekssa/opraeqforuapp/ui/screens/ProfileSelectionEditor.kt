package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.catalog.visibilityCategory
import com.weekssa.opraeqforuapp.domain.managed.DEFAULT_AUTO_INCLUDE_NEW_PROFILES
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.defaultStagedSelectedProfileIds
import com.weekssa.opraeqforuapp.domain.managed.managedSelectionCommitEnabled
import com.weekssa.opraeqforuapp.domain.managed.selectableProfileIds
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import kotlinx.coroutines.launch

@Composable
internal fun ProfileSelectionEditor(
    catalog: OpraCatalog,
    product: OpraProduct,
    profileVisibility: ProfileVisibilityPreferences,
    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,
    onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit,
    onRemoveHeadphone: suspend (String) -> Unit,
    onDeleteSavedFilesForProfiles: suspend (Set<String>) -> PresetCleanupSummary,
    onDeleteSavedFilesForProduct: suspend (String) -> PresetCleanupSummary,
    onExportProduct: (String) -> Unit,
    onMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vendor = catalog.vendor(product.vendorId)
    val profiles = catalog.profilesForProduct(product.id)
    val visibleProfiles = profiles.filter { profile ->
        profileVisibility.isVisible(profile.assessCompatibility().category.visibilityCategory())
    }
    val hiddenCount = profiles.size - visibleProfiles.size
    val scope = rememberCoroutineScope()

    var initialized by remember(product.id) { mutableStateOf(false) }
    var managedRecord by remember(product.id) { mutableStateOf<ManagedHeadphoneRecord?>(null) }
    var stagedSelectedIds by remember(product.id) { mutableStateOf<Set<String>>(emptySet()) }
    var baselineSelectedIds by remember(product.id) { mutableStateOf<Set<String>>(emptySet()) }
    var autoInclude by remember(product.id) { mutableStateOf(DEFAULT_AUTO_INCLUDE_NEW_PROFILES) }
    var baselineAutoInclude by remember(product.id) { mutableStateOf(DEFAULT_AUTO_INCLUDE_NEW_PROFILES) }
    var showDiscardDialog by remember(product.id) { mutableStateOf(false) }
    var pendingRemoval by remember(product.id) { mutableStateOf<RemovalRequest?>(null) }
    var pendingExportAfterRemoval by remember(product.id) { mutableStateOf(false) }
    var deleteSavedFiles by remember(product.id) { mutableStateOf(false) }
    var incompatibilityExplanation by remember(product.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(product.id) {
        val managed = onLoadManagedHeadphone(product.id)
        managedRecord = managed
        val selected = if (managed == null) {
            defaultStagedSelectedProfileIds(profiles)
        } else {
            val selectionState = managed.toSelectionState()
            profiles.filter(selectionState::isSelected).mapTo(mutableSetOf(), OpraEqProfile::id)
        }
        stagedSelectedIds = selected
        baselineSelectedIds = selected
        autoInclude = managed?.autoIncludeNewProfiles ?: DEFAULT_AUTO_INCLUDE_NEW_PROFILES
        baselineAutoInclude = autoInclude
        initialized = true
    }

    val dirty = initialized &&
        (stagedSelectedIds != baselineSelectedIds || autoInclude != baselineAutoInclude)
    val commitEnabled = initialized && managedSelectionCommitEnabled(
        isManaged = managedRecord != null,
        stagedSelectedProfileIds = stagedSelectedIds,
        baselineSelectedProfileIds = baselineSelectedIds,
        autoIncludeNewProfiles = autoInclude,
        baselineAutoIncludeNewProfiles = baselineAutoInclude,
    )
    val removedCurrentIds = baselineSelectedIds - stagedSelectedIds
    val retainedSelectedUnavailable = managedRecord?.profiles.orEmpty().count { it.selected && it.noLongerAvailable }

    fun completeSave(
        removeWholeHeadphone: Boolean,
        removeIds: Set<String>,
        deleteFiles: Boolean,
        exportAfterSave: Boolean,
    ) {
        scope.launch {
            if (removeWholeHeadphone) {
                onRemoveHeadphone(product.id)
            } else {
                onSaveSelection(product.id, stagedSelectedIds, autoInclude)
            }
            if (deleteFiles) {
                val cleanup = if (removeWholeHeadphone) {
                    onDeleteSavedFilesForProduct(product.id)
                } else {
                    onDeleteSavedFilesForProfiles(removeIds)
                }
                if (cleanup.failedCount > 0) {
                    onMessage("Selection was saved, but ${cleanup.failedCount} saved preset files could not be removed.")
                }
            }
            baselineSelectedIds = stagedSelectedIds
            baselineAutoInclude = autoInclude
            managedRecord = onLoadManagedHeadphone(product.id)
            if (exportAfterSave && stagedSelectedIds.isNotEmpty()) {
                onExportProduct(product.id)
            }
        }
    }

    fun requestSave(exportAfterSave: Boolean = false) {
        if (exportAfterSave && stagedSelectedIds.isEmpty()) return
        val removeWhole = stagedSelectedIds.isEmpty() && retainedSelectedUnavailable == 0 && managedRecord != null
        when {
            removeWhole -> {
                deleteSavedFiles = false
                pendingExportAfterRemoval = exportAfterSave
                pendingRemoval = RemovalRequest.WholeHeadphone
            }
            removedCurrentIds.isNotEmpty() -> {
                deleteSavedFiles = false
                pendingExportAfterRemoval = exportAfterSave
                pendingRemoval = RemovalRequest.Profiles(removedCurrentIds)
            }
            else -> completeSave(
                removeWholeHeadphone = false,
                removeIds = emptySet(),
                deleteFiles = false,
                exportAfterSave = exportAfterSave,
            )
        }
    }

    fun requestExport() {
        if (stagedSelectedIds.isEmpty()) return
        if (managedRecord == null || dirty) {
            requestSave(exportAfterSave = true)
        } else {
            onExportProduct(product.id)
        }
    }

    fun requestBack() {
        if (dirty) showDiscardDialog = true else onBack()
    }

    BackHandler(enabled = dirty) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved profile-selection changes will be discarded.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            },
        )
    }

    pendingRemoval?.let { request ->
        val wholeHeadphone = request is RemovalRequest.WholeHeadphone
        AlertDialog(
            onDismissRequest = {
                pendingRemoval = null
                pendingExportAfterRemoval = false
            },
            title = {
                Text(
                    if (wholeHeadphone) {
                        "Remove headphone?"
                    } else {
                        "Remove ${(request as RemovalRequest.Profiles).profileIds.size} profiles?"
                    },
                )
            },
            text = {
                Column {
                    Text(
                        if (wholeHeadphone) {
                            "This headphone will be removed from My Headphones."
                        } else {
                            "These profiles will no longer be selected for this headphone."
                        },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
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
                            text = "Also remove saved preset files created by OPRA EQ for UAPP",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val removeIds = (request as? RemovalRequest.Profiles)?.profileIds.orEmpty()
                    val exportAfterSave = pendingExportAfterRemoval
                    pendingRemoval = null
                    pendingExportAfterRemoval = false
                    completeSave(
                        removeWholeHeadphone = wholeHeadphone,
                        removeIds = removeIds,
                        deleteFiles = deleteSavedFiles,
                        exportAfterSave = exportAfterSave,
                    )
                }) {
                    Text(if (wholeHeadphone) "Remove headphone" else "Remove profiles")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingRemoval = null
                    pendingExportAfterRemoval = false
                }) { Text("Cancel") }
            },
        )
    }

    incompatibilityExplanation?.let { explanation ->
        AlertDialog(
            onDismissRequest = { incompatibilityExplanation = null },
            title = { Text("Not compatible") },
            text = { Text(explanation) },
            confirmButton = {
                TextButton(onClick = { incompatibilityExplanation = null }) { Text("OK") }
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = ::requestBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = null)
            Text(vendor?.name ?: "Models", modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            text = product.name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        vendor?.let {
            Text(
                text = it.name,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!initialized) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                Text("Loading saved selections…")
            }
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { stagedSelectedIds = selectableProfileIds(profiles) }) {
                Text("Select all")
            }
            TextButton(onClick = { stagedSelectedIds = emptySet() }) {
                Text("Select none")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Automatically include new OPRA profiles for this headphone",
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = autoInclude,
                onCheckedChange = { autoInclude = it },
            )
        }

        if (hiddenCount > 0) {
            Text(
                text = "$hiddenCount OPRA profiles hidden by your compatibility filter.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (retainedSelectedUnavailable > 0) {
            Text(
                text = "$retainedSelectedUnavailable selected presets are retained because they are no longer available in OPRA. Manage them from My Headphones.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = { requestSave() },
            enabled = commitEnabled,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(if (managedRecord == null) "Add to My Headphones" else "Save changes")
        }
        OutlinedButton(
            onClick = ::requestExport,
            enabled = stagedSelectedIds.isNotEmpty(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text("Export XMLs")
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(visibleProfiles, key = OpraEqProfile::id) { profile ->
                val assessment = profile.assessCompatibility()
                ProfileSelectionRow(
                    profile = profile,
                    selected = profile.id in stagedSelectedIds,
                    onSelectionChange = { selected ->
                        stagedSelectedIds = if (selected) {
                            stagedSelectedIds + profile.id
                        } else {
                            stagedSelectedIds - profile.id
                        }
                    },
                    onExplainIncompatibility = {
                        incompatibilityExplanation = assessment.reason
                            ?: "This OPRA profile cannot be converted safely by the established UAPP/ToneBoosters mapping."
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
internal fun ProfileSelectionRow(
    profile: OpraEqProfile,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onExplainIncompatibility: () -> Unit,
) {
    val compatibility = profile.assessCompatibility()
    val selectable = compatibility.category.isSelectable
    val rowModifier = if (selectable) {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = onSelectionChange,
            )
    } else {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onExplainIncompatibility)
    }

    ListItem(
        leadingContent = {
            Checkbox(
                checked = selected && selectable,
                onCheckedChange = null,
                enabled = selectable,
            )
        },
        headlineContent = {
            Text(profile.author?.takeIf { it.isNotBlank() } ?: "Creator information missing")
        },
        supportingContent = {
            Column {
                profile.details?.let { Text(it) }
                when (compatibility.category) {
                    ProfileCompatibility.FullyCompatible -> Unit
                    ProfileCompatibility.CompatibleWithLimitation -> {
                        Text("Compatible with limitation")
                        compatibility.reason?.let { Text(it) }
                    }
                    ProfileCompatibility.NotCompatible -> {
                        Text("Not compatible · unavailable for selection")
                        compatibility.reason?.let { Text(it) }
                    }
                }
            }
        },
        modifier = rowModifier,
    )
}

private sealed interface RemovalRequest {
    data object WholeHeadphone : RemovalRequest
    data class Profiles(val profileIds: Set<String>) : RemovalRequest
}
