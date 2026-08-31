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
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.export.DeviceExportability
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.assessDeviceExportability
import com.weekssa.opraeqforuapp.domain.managed.DEFAULT_AUTO_INCLUDE_NEW_PROFILES
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.defaultStagedSelectedProfileIds
import com.weekssa.opraeqforuapp.domain.managed.managedSelectionCommitEnabled
import com.weekssa.opraeqforuapp.domain.managed.selectableProfileIds
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import kotlinx.coroutines.launch

private enum class ProfileFilterDimension(val label: String) {
    Database("Database"),
    Creator("Creator"),
    Target("Target"),
}

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun ProfileSelectionEditor(
    catalog: OpraCatalog,
    product: OpraProduct,
    profileVisibility: ProfileVisibilityPreferences,
    exportTargets: ExportTargetPreferences = ExportTargetPreferences(),
    favoriteProfileIds: Set<String>,
    onToggleFavorite: (suspend (OpraEqProfile, String, String) -> Boolean)?,
    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,
    onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit,
    onRemoveHeadphone: suspend (String) -> Unit,
    onDeleteSavedFilesForProfiles: suspend (Set<String>) -> PresetCleanupSummary,
    onDeleteSavedFilesForProduct: suspend (String) -> PresetCleanupSummary,
    onExportProduct: (String) -> Unit,
    onMessage: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vendor = catalog.vendor(product.vendorId)
    val profiles = catalog.profilesForProduct(product.id)
    val historicalProfileCount = profiles.count(OpraEqProfile::isHistoricalRevision)
    val databaseOptions = remember(profiles) {
        profiles.mapNotNull { profile ->
            profile.detailMetadata("Database") ?: profile.detailMetadata("Source")
        }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val creatorOptions = remember(profiles) {
        profiles.mapNotNull { it.author?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val targetOptions = remember(profiles) {
        profiles.mapNotNull { it.detailMetadata("Target") }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val scope = rememberCoroutineScope()
    val selectionContextKey = "${product.id}:${exportTargets.activeTarget.name}"

    var databaseFilter by rememberSaveable(product.id) { mutableStateOf<String?>(null) }
    var creatorFilter by rememberSaveable(product.id) { mutableStateOf<String?>(null) }
    var targetFilter by rememberSaveable(product.id) { mutableStateOf<String?>(null) }
    var showHistoricalRevisions by rememberSaveable(product.id) { mutableStateOf(false) }
    var filterDialog by remember { mutableStateOf<ProfileFilterDimension?>(null) }

    LaunchedEffect(databaseOptions, creatorOptions, targetOptions) {
        if (databaseFilter != null && databaseFilter !in databaseOptions) databaseFilter = null
        if (creatorFilter != null && creatorFilter !in creatorOptions) creatorFilter = null
        if (targetFilter != null && targetFilter !in targetOptions) targetFilter = null
    }

    val revisionVisibleProfiles = if (showHistoricalRevisions) {
        profiles
    } else {
        profiles.filterNot(OpraEqProfile::isHistoricalRevision)
    }
    val visibleProfiles = revisionVisibleProfiles.filter { profile ->
        val database = profile.detailMetadata("Database") ?: profile.detailMetadata("Source")
        (databaseFilter == null || database == databaseFilter) &&
            (creatorFilter == null || profile.author?.trim() == creatorFilter) &&
            (targetFilter == null || profile.detailMetadata("Target") == targetFilter)
    }
    val filteredOutCount = revisionVisibleProfiles.size - visibleProfiles.size

    var initialized by remember(selectionContextKey) { mutableStateOf(false) }
    var managedRecord by remember(selectionContextKey) { mutableStateOf<ManagedHeadphoneRecord?>(null) }
    var stagedSelectedIds by remember(selectionContextKey) { mutableStateOf<Set<String>>(emptySet()) }
    var baselineSelectedIds by remember(selectionContextKey) { mutableStateOf<Set<String>>(emptySet()) }
    var autoInclude by remember(selectionContextKey) { mutableStateOf(DEFAULT_AUTO_INCLUDE_NEW_PROFILES) }
    var baselineAutoInclude by remember(selectionContextKey) { mutableStateOf(DEFAULT_AUTO_INCLUDE_NEW_PROFILES) }
    var showDiscardDialog by remember(selectionContextKey) { mutableStateOf(false) }
    var sourceProblemExplanation by remember(selectionContextKey) { mutableStateOf<String?>(null) }

    LaunchedEffect(selectionContextKey) {
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
    val retainedSelectedUnavailable = managedRecord?.profiles.orEmpty().count {
        it.selected && it.noLongerAvailable
    }
    val selectedHistoricalCount = profiles.count {
        it.isHistoricalRevision() && it.id in stagedSelectedIds
    }

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

    fun requestExport() {
        if (stagedSelectedIds.isEmpty()) return
        if (managedRecord == null || dirty) {
            completeSave(exportAfterSave = true)
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
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    },
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            },
        )
    }

    sourceProblemExplanation?.let { explanation ->
        AlertDialog(
            onDismissRequest = { sourceProblemExplanation = null },
            title = { Text("Source data unavailable") },
            text = { Text(explanation) },
            confirmButton = {
                TextButton(onClick = { sourceProblemExplanation = null }) { Text("OK") }
            },
        )
    }

    filterDialog?.let { dimension ->
        val options = when (dimension) {
            ProfileFilterDimension.Database -> databaseOptions
            ProfileFilterDimension.Creator -> creatorOptions
            ProfileFilterDimension.Target -> targetOptions
        }
        val selected = when (dimension) {
            ProfileFilterDimension.Database -> databaseFilter
            ProfileFilterDimension.Creator -> creatorFilter
            ProfileFilterDimension.Target -> targetFilter
        }
        ProfileFilterDialog(
            dimension = dimension,
            options = options,
            selected = selected,
            onSelect = { value ->
                when (dimension) {
                    ProfileFilterDimension.Database -> databaseFilter = value
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
                onClick = { filterDialog = ProfileFilterDimension.Database },
                enabled = databaseOptions.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (databaseFilter == null) "Database" else "Database ✓")
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
            databaseFilter?.let { "Database: $it" },
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
                TextButton(
                    onClick = {
                        databaseFilter = null
                        creatorFilter = null
                        targetFilter = null
                    },
                ) { Text("Clear") }
            }
        }

        if (historicalProfileCount > 0) {
            TextButton(
                onClick = { showHistoricalRevisions = !showHistoricalRevisions },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                val selectedSuffix = if (selectedHistoricalCount > 0) {
                    " · $selectedHistoricalCount selected"
                } else {
                    ""
                }
                Text(
                    if (showHistoricalRevisions) {
                        "Hide history"
                    } else {
                        "History ($historicalProfileCount)$selectedSuffix"
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = {
                    stagedSelectedIds = stagedSelectedIds +
                        selectableProfileIds(visibleProfiles, includeHistorical = true)
                },
            ) {
                Text("Select all")
            }
            TextButton(
                onClick = {
                    stagedSelectedIds = stagedSelectedIds - visibleProfiles.map(OpraEqProfile::id).toSet()
                },
            ) {
                Text("Select none")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Automatically include new EQs",
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(checked = autoInclude, onCheckedChange = { autoInclude = it })
        }

        Text(
            text = "Unverified EQs are never added automatically. Output compatibility never hides or silently removes a saved source curve.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (filteredOutCount > 0) {
            Text(
                text = "$filteredOutCount EQ profiles hidden by Database / Creator / Target filters.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (retainedSelectedUnavailable > 0) {
            Text(
                text = "$retainedSelectedUnavailable selected presets are retained because they are no longer available in the current EQ Library catalog. Manage them from My EQs.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(visibleProfiles, key = OpraEqProfile::id) { profile ->
                val sourceAssessment = profile.assessCompatibility()
                val outputStatus = assessDeviceExportability(profile, exportTargets.activeTarget)
                ProfileSelectionRow(
                    profile = profile,
                    selected = profile.id in stagedSelectedIds,
                    isFavorite = profile.id in favoriteProfileIds,
                    outputStatus = "${outputShortName(exportTargets.activeTarget)}: ${outputStatusLabel(outputStatus)}",
                    outputStatusCategory = outputStatus,
                    onSelectionChange = { selected ->
                        stagedSelectedIds = if (selected) {
                            stagedSelectedIds + profile.id
                        } else {
                            stagedSelectedIds - profile.id
                        }
                    },
                    onToggleFavorite = onToggleFavorite?.let { toggle ->
                        {
                            scope.launch {
                                val favorited = toggle(
                                    profile,
                                    vendor?.name ?: "Unknown manufacturer",
                                    product.name,
                                )
                                onMessage(
                                    if (favorited) {
                                        "Saved to My EQs favorites."
                                    } else {
                                        "Removed from My EQs favorites."
                                    },
                                )
                            }
                        }
                    },
                    onOpenSource = profile.link?.let { sourceUrl -> { onOpenUrl(sourceUrl) } },
                    onExplainSourceProblem = {
                        sourceProblemExplanation = sourceAssessment.reason
                            ?: "This catalog row is not a usable parametric EQ source."
                    },
                )
                HorizontalDivider()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { completeSave(exportAfterSave = false) },
                enabled = commitEnabled,
                modifier = Modifier.weight(1f),
            ) {
                val label = if (managedRecord == null) "Add" else "Save"
                Text("$label (${stagedSelectedIds.size})")
            }
            OutlinedButton(
                onClick = ::requestExport,
                enabled = stagedSelectedIds.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Export (${stagedSelectedIds.size})")
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun ProfileSelectionRow(
    profile: OpraEqProfile,
    selected: Boolean,
    isFavorite: Boolean,
    outputStatus: String,
    outputStatusCategory: DeviceExportability,
    onSelectionChange: (Boolean) -> Unit,
    onToggleFavorite: (() -> Unit)?,
    onOpenSource: (() -> Unit)?,
    onExplainSourceProblem: () -> Unit,
) {
    val compatibility = profile.assessCompatibility()
    val selectable = compatibility.category.isSelectable
    val displayDetails = profile.displayDetails()
    val rowModifier = if (selectable) {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = onSelectionChange,
            )
    } else {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onExplainSourceProblem)
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
            Text(
                text = profile.author?.takeIf(String::isNotBlank) ?: "Creator information missing",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Column(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)) {
                if (!profile.isVerified) {
                    Text(
                        text = "Community submission — not independently verified. Review the source before use.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Text(
                    text = outputStatus,
                    modifier = Modifier.padding(top = if (profile.isVerified) 0.dp else 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = when (outputStatusCategory) {
                        DeviceExportability.EXACT -> MaterialTheme.colorScheme.onSurfaceVariant
                        DeviceExportability.OPTIMIZED -> MaterialTheme.colorScheme.tertiary
                        DeviceExportability.NOT_REPRESENTABLE -> MaterialTheme.colorScheme.error
                    },
                )
                displayDetails.metadata?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                displayDetails.soundImpact?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                onOpenSource?.let { openSource ->
                    TextButton(onClick = openSource) {
                        Text("Source")
                    }
                }
                if (compatibility.category == ProfileCompatibility.NotCompatible) {
                    Text(
                        text = "Source data unavailable for selection",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    compatibility.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        trailingContent = onToggleFavorite?.let { action ->
            {
                IconButton(onClick = action) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    )
                }
            }
        },
        modifier = rowModifier,
    )
}

private fun outputStatusLabel(status: DeviceExportability): String = when (status) {
    DeviceExportability.EXACT -> "Exact"
    DeviceExportability.OPTIMIZED -> "Optimized"
    DeviceExportability.NOT_REPRESENTABLE -> "Not exportable"
}

private fun outputShortName(device: ExportDevice): String = when (device) {
    ExportDevice.UAPP -> "UAPP / ToneBoosters"
    ExportDevice.BLACK_PEARL -> "Black Pearl"
    ExportDevice.UNIVERSAL_PARAMETRIC -> "Universal PEQ"
    ExportDevice.POWERAMP -> "Poweramp"
    ExportDevice.WAVELET -> "Wavelet"
    ExportDevice.TOPPING_DX5_II -> "TOPPING DX5 II"
    ExportDevice.TOPPING_DX1_II -> "TOPPING DX1 II"
}

private data class ProfileDisplayDetails(
    val metadata: String?,
    val soundImpact: String?,
)

private fun OpraEqProfile.displayDetails(): ProfileDisplayDetails {
    val parts = details
        ?.split(" · ")
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty()
    val soundImpact = parts.lastOrNull(::isSoundImpactText)
    val metadata = parts
        .filterNot { it == soundImpact }
        .joinToString(" · ")
        .takeIf(String::isNotBlank)
    return ProfileDisplayDetails(metadata = metadata, soundImpact = soundImpact)
}

private fun isSoundImpactText(value: String): Boolean {
    val normalized = value.lowercase()
    return normalized.endsWith('.') && listOf(
        "adds ",
        "reduces ",
        "slightly adds ",
        "slightly reduces ",
        "noticeably adds ",
        "noticeably reduces ",
        "makes small ",
    ).any(normalized::startsWith)
}

private fun OpraEqProfile.detailMetadata(label: String): String? = details
    ?.split(" · ")
    ?.firstOrNull { part -> part.startsWith("$label:", ignoreCase = true) }
    ?.substringAfter(':')
    ?.trim()
    ?.takeIf(String::isNotEmpty)
