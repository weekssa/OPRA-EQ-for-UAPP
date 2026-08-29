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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
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

private enum class ProfileFilterDimension(val label: String) {
    Source("Source"),
    Creator("Creator"),
    Target("Target"),
}

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun ProfileSelectionEditor(
    catalog: OpraCatalog,
    product: OpraProduct,
    profileVisibility: ProfileVisibilityPreferences,
    favoriteProfileIds: Set<String>,
    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,
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
    val compatibilityVisibleProfiles = profiles.filter { profile ->
        profileVisibility.isVisible(profile.assessCompatibility().category.visibilityCategory())
    }
    val hiddenCompatibilityCount = profiles.size - compatibilityVisibleProfiles.size
    val sourceOptions = remember(profiles) {
        profiles.mapNotNull { it.detailMetadata("Source") }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val creatorOptions = remember(profiles) {
        profiles.mapNotNull { it.author?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val targetOptions = remember(profiles) {
        profiles.mapNotNull { it.detailMetadata("Target") }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val scope = rememberCoroutineScope()

    var sourceFilter by rememberSaveable(product.id) { mutableStateOf<String?>(null) }
    var creatorFilter by rememberSaveable(product.id) { mutableStateOf<String?>(null) }
    var targetFilter by rememberSaveable(product.id) { mutableStateOf<String?>(null) }
    var filterDialog by remember { mutableStateOf<ProfileFilterDimension?>(null) }

    LaunchedEffect(sourceOptions, creatorOptions, targetOptions) {
        if (sourceFilter !in sourceOptions) sourceFilter = null
        if (creatorFilter !in creatorOptions) creatorFilter = null
        if (targetFilter !in targetOptions) targetFilter = null
    }

    val visibleProfiles = compatibilityVisibleProfiles.filter { profile ->
        (sourceFilter == null || profile.detailMetadata("Source") == sourceFilter) &&
            (creatorFilter == null || profile.author == creatorFilter) &&
            (targetFilter == null || profile.detailMetadata("Target") == targetFilter)
    }
    val filteredOutCount = compatibilityVisibleProfiles.size - visibleProfiles.size

    var initialized by remember(product.id) { mutableStateOf(false) }
    var managedRecord by remember(product.id) { mutableStateOf<ManagedHeadphoneRecord?>(null) }
    var stagedSelectedIds by remember(product.id) { mutableStateOf<Set<String>>(emptySet()) }
    var baselineSelectedIds by remember(product.id) { mutableStateOf<Set<String>>(emptySet()) }
    var autoInclude by remember(product.id) { mutableStateOf(DEFAULT_AUTO_INCLUDE_NEW_PROFILES) }
    var baselineAutoInclude by remember(product.id) { mutableStateOf(DEFAULT_AUTO_INCLUDE_NEW_PROFILES) }
    var showDiscardDialog by remember(product.id) { mutableStateOf(false) }
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
    val retainedSelectedUnavailable = managedRecord?.profiles.orEmpty().count { it.selected && it.noLongerAvailable }

    fun completeSave(exportAfterSave: Boolean) {
        scope.launch {
            onSaveSelection(product.id, stagedSelectedIds, autoInclude)
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
        completeSave(exportAfterSave)
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
            text = { Text("Your unsaved preset-selection changes will be discarded.") },
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

    filterDialog?.let { dimension ->
        val options = when (dimension) {
            ProfileFilterDimension.Source -> sourceOptions
            ProfileFilterDimension.Creator -> creatorOptions
            ProfileFilterDimension.Target -> targetOptions
        }
        val selected = when (dimension) {
            ProfileFilterDimension.Source -> sourceFilter
            ProfileFilterDimension.Creator -> creatorFilter
            ProfileFilterDimension.Target -> targetFilter
        }
        ProfileFilterDialog(
            dimension = dimension,
            options = options,
            selected = selected,
            onSelect = { value ->
                when (dimension) {
                    ProfileFilterDimension.Source -> sourceFilter = value
                    ProfileFilterDimension.Creator -> creatorFilter = value
                    ProfileFilterDimension.Target -> targetFilter = value
                }
                filterDialog = null
            },
            onDismiss = { filterDialog = null },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = ::requestBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(
                onClick = { filterDialog = ProfileFilterDimension.Source },
                enabled = sourceOptions.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (sourceFilter == null) "Source" else "Source ✓")
            }
            OutlinedButton(
                onClick = { filterDialog = ProfileFilterDimension.Creator },
                enabled = creatorOptions.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (creatorFilter == null) "Creator" else "Creator ✓")
            }
            OutlinedButton(
                onClick = { filterDialog = ProfileFilterDimension.Target },
                enabled = targetOptions.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (targetFilter == null) "Target" else "Target ✓")
            }
        }

        val activeFilters = listOfNotNull(
            sourceFilter?.let { "Source: $it" },
            creatorFilter?.let { "Creator: $it" },
            targetFilter?.let { "Target: $it" },
        )
        if (activeFilters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = activeFilters.joinToString(" · "),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = {
                    sourceFilter = null
                    creatorFilter = null
                    targetFilter = null
                }) {
                    Text("Clear")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
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
                text = "Automatically include new EQ profiles for this headphone",
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = autoInclude,
                onCheckedChange = { autoInclude = it },
            )
        }

        Text(
            text = "Changing this selection does not delete exported files. Remove presets or the headphone from My Headphones when you want saved files cleaned up.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (hiddenCompatibilityCount > 0) {
            Text(
                text = "$hiddenCompatibilityCount EQ profiles hidden by your compatibility filter.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (filteredOutCount > 0) {
            Text(
                text = "$filteredOutCount EQ profiles hidden by Source / Creator / Target filters.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (retainedSelectedUnavailable > 0) {
            Text(
                text = "$retainedSelectedUnavailable selected presets are retained because they are no longer available in the current EQ Library catalog. Manage them from My Headphones.",
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
            Text(if (managedRecord == null) "Add to My Headphones" else "Save selection")
        }
        OutlinedButton(
            onClick = ::requestExport,
            enabled = stagedSelectedIds.isNotEmpty(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text("Export selected presets")
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(visibleProfiles, key = OpraEqProfile::id) { profile ->
                val assessment = profile.assessCompatibility()
                ProfileSelectionRow(
                    profile = profile,
                    selected = profile.id in stagedSelectedIds,
                    isFavorite = profile.id in favoriteProfileIds,
                    onSelectionChange = { selected ->
                        stagedSelectedIds = if (selected) {
                            stagedSelectedIds + profile.id
                        } else {
                            stagedSelectedIds - profile.id
                        }
                    },
                    onToggleFavorite = {
                        scope.launch {
                            val favorited = onToggleFavorite(
                                profile,
                                vendor?.name ?: "Unknown manufacturer",
                                product.name,
                            )
                            onMessage(if (favorited) "Saved to My EQs favorites." else "Removed from My EQs favorites.")
                        }
                    },
                    onExplainIncompatibility = {
                        incompatibilityExplanation = assessment.reason
                            ?: "This EQ profile cannot be converted safely for the selected export target."
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ProfileFilterDialog(
    dimension: ProfileFilterDimension,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by ${dimension.label.lowercase()}") },
        text = {
            Column {
                TextButton(
                    onClick = { onSelect(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (selected == null) "All ✓" else "All")
                }
                options.forEach { option ->
                    TextButton(
                        onClick = { onSelect(option) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (selected == option) "$option ✓" else option)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
internal fun ProfileSelectionRow(
    profile: OpraEqProfile,
    selected: Boolean,
    isFavorite: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
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
        trailingContent = {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                )
            }
        },
        modifier = rowModifier,
    )
}

private fun OpraEqProfile.detailMetadata(label: String): String? = details
    ?.split(" · ")
    ?.firstOrNull { part -> part.startsWith("$label:", ignoreCase = true) }
    ?.substringAfter(':')
    ?.trim()
    ?.takeIf(String::isNotEmpty)
