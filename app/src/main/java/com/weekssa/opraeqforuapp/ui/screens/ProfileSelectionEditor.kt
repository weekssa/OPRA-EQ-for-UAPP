package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.assessCompatibility
import com.weekssa.opraeqforuapp.domain.catalog.isHistoricalRevision
import com.weekssa.opraeqforuapp.domain.export.DeviceExportability
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.assessDeviceExportability
import com.weekssa.opraeqforuapp.domain.export.deviceAdaptationSummary
import com.weekssa.opraeqforuapp.domain.managed.DEFAULT_NOTIFY_NEW_PROFILES
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.defaultStagedSelectedProfileIds
import com.weekssa.opraeqforuapp.domain.managed.managedSelectionCommitEnabled
import com.weekssa.opraeqforuapp.domain.managed.selectableProfileIds
import com.weekssa.opraeqforuapp.domain.model.ProfileCompatibility
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import com.weekssa.opraeqforuapp.ui.StringSetSaver
import kotlinx.coroutines.launch

private enum class ProfileFilterDimension(@param:StringRes val labelResId: Int) {
    Database(R.string.filter_database),
    Creator(R.string.filter_creator),
    Target(R.string.filter_target),
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
    onHideCanonicalProfile: suspend (String) -> Unit,
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

    val databaseLabel = stringResource(R.string.filter_database)
    val creatorLabel = stringResource(R.string.filter_creator)
    val targetLabel = stringResource(R.string.filter_target)
    val unknownManufacturer = stringResource(R.string.unknown_manufacturer)
    val favoriteSavedMessage = stringResource(R.string.favorite_saved_message)
    val favoriteRemovedMessage = stringResource(R.string.favorite_removed_message)
    val eqHiddenMessage = stringResource(R.string.eq_hidden_from_library_message)
    val sourceNotUsableDefault = stringResource(R.string.source_not_usable_default)

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
    var draftInitialized by rememberSaveable(selectionContextKey) { mutableStateOf(false) }
    var stagedSelectedIds by rememberSaveable(
        selectionContextKey,
        stateSaver = StringSetSaver,
    ) { mutableStateOf(emptySet<String>()) }
    var baselineSelectedIds by rememberSaveable(
        selectionContextKey,
        stateSaver = StringSetSaver,
    ) { mutableStateOf(emptySet<String>()) }
    var autoInclude by rememberSaveable(selectionContextKey) { mutableStateOf(DEFAULT_NOTIFY_NEW_PROFILES) }
    var baselineAutoInclude by rememberSaveable(selectionContextKey) {
        mutableStateOf(DEFAULT_NOTIFY_NEW_PROFILES)
    }
    var showDiscardDialog by rememberSaveable(selectionContextKey) { mutableStateOf(false) }
    var sourceProblemExplanation by rememberSaveable(selectionContextKey) { mutableStateOf<String?>(null) }

    LaunchedEffect(selectionContextKey) {
        val managed = onLoadManagedHeadphone(product.id)
        managedRecord = managed
        if (!draftInitialized) {
            val selected = if (managed == null) {
                defaultStagedSelectedProfileIds(profiles)
            } else {
                val selectionState = managed.toSelectionState()
                profiles.filter(selectionState::isSelected).mapTo(mutableSetOf(), OpraEqProfile::id)
            }
            stagedSelectedIds = selected
            baselineSelectedIds = selected
            autoInclude = managed?.autoIncludeNewProfiles ?: DEFAULT_NOTIFY_NEW_PROFILES
            baselineAutoInclude = autoInclude
            draftInitialized = true
        }
        initialized = true
    }

    val dirty = initialized && stagedSelectedIds != baselineSelectedIds
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

    fun completeSave() {
        scope.launch {
            onSaveSelection(product.id, stagedSelectedIds, autoInclude)
            baselineSelectedIds = stagedSelectedIds
            baselineAutoInclude = autoInclude
            managedRecord = onLoadManagedHeadphone(product.id)
            if (stagedSelectedIds.isNotEmpty()) {
                onExportProduct(product.id)
            } else {
                onBack()
            }
        }
    }

    fun requestBack() {
        if (dirty) showDiscardDialog = true else onBack()
    }

    BackHandler { requestBack() }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_selection_changes_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    },
                ) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.action_keep_editing))
                }
            },
        )
    }

    sourceProblemExplanation?.let { explanation ->
        AlertDialog(
            onDismissRequest = { sourceProblemExplanation = null },
            title = { Text(stringResource(R.string.source_data_unavailable_title)) },
            text = { Text(explanation) },
            confirmButton = {
                TextButton(onClick = { sourceProblemExplanation = null }) {
                    Text(stringResource(R.string.action_ok))
                }
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
            Text(
                vendor?.name ?: stringResource(R.string.models),
                modifier = Modifier.padding(start = 4.dp),
            )
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
                Text(stringResource(R.string.loading_saved_selections))
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
                Text(
                    if (databaseFilter == null) {
                        databaseLabel
                    } else {
                        stringResource(R.string.filter_selected_format, databaseLabel)
                    },
                )
            }
            OutlinedButton(
                onClick = { filterDialog = ProfileFilterDimension.Creator },
                enabled = creatorOptions.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (creatorFilter == null) {
                        creatorLabel
                    } else {
                        stringResource(R.string.filter_selected_format, creatorLabel)
                    },
                )
            }
            OutlinedButton(
                onClick = { filterDialog = ProfileFilterDimension.Target },
                enabled = targetOptions.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (targetFilter == null) {
                        targetLabel
                    } else {
                        stringResource(R.string.filter_selected_format, targetLabel)
                    },
                )
            }
        }

        val activeFilters = listOfNotNull(
            databaseFilter?.let { stringResource(R.string.filter_value_format, databaseLabel, it) },
            creatorFilter?.let { stringResource(R.string.filter_value_format, creatorLabel, it) },
            targetFilter?.let { stringResource(R.string.filter_value_format, targetLabel, it) },
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
                ) { Text(stringResource(R.string.action_clear)) }
            }
        }

        if (historicalProfileCount > 0) {
            TextButton(
                onClick = { showHistoricalRevisions = !showHistoricalRevisions },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                val historyLabel = if (showHistoricalRevisions) {
                    stringResource(R.string.history_hide)
                } else if (selectedHistoricalCount > 0) {
                    stringResource(
                        R.string.history_count_with_selected,
                        historicalProfileCount,
                        pluralStringResource(
                            R.plurals.selected_count,
                            selectedHistoricalCount,
                            selectedHistoricalCount,
                        ),
                    )
                } else {
                    stringResource(R.string.history_count, historicalProfileCount)
                }
                Text(historyLabel)
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
                Text(stringResource(R.string.action_select_all))
            }
            TextButton(
                onClick = {
                    stagedSelectedIds = stagedSelectedIds - visibleProfiles.map(OpraEqProfile::id).toSet()
                },
            ) {
                Text(stringResource(R.string.action_select_none))
            }
        }

        Text(
            text = stringResource(R.string.selection_behavior_explanation),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (filteredOutCount > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.profiles_hidden_by_filters,
                    filteredOutCount,
                    filteredOutCount,
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (retainedSelectedUnavailable > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.selected_presets_retained_unavailable,
                    retainedSelectedUnavailable,
                    retainedSelectedUnavailable,
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(visibleProfiles, key = OpraEqProfile::id) { profile ->
                val sourceAssessment = profile.assessCompatibility()
                val activeTarget = exportTargets.activeTarget
                val outputStatus = assessDeviceExportability(profile, activeTarget)
                val adaptation = deviceAdaptationSummary(profile, activeTarget)
                val statusLabel = outputStatusLabel(outputStatus, activeTarget)
                val statusText = buildString {
                    append(outputShortName(activeTarget))
                    append(": ")
                    append(statusLabel)
                    adaptation?.let { append(" · $it") }
                }
                ProfileSelectionRow(
                    profile = profile,
                    selected = profile.id in stagedSelectedIds,
                    isFavorite = profile.id in favoriteProfileIds,
                    outputStatus = statusText,
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
                                    vendor?.name ?: unknownManufacturer,
                                    product.name,
                                )
                                onMessage(if (favorited) favoriteSavedMessage else favoriteRemovedMessage)
                            }
                        }
                    },
                    onHide = {
                        scope.launch {
                            if (profile.id !in baselineSelectedIds) {
                                stagedSelectedIds = stagedSelectedIds - profile.id
                            }
                            onHideCanonicalProfile(profile.canonicalProfileId)
                            onMessage(eqHiddenMessage)
                        }
                    },
                    onOpenSource = profile.link?.let { sourceUrl -> { onOpenUrl(sourceUrl) } },
                    onExplainSourceProblem = {
                        sourceProblemExplanation = sourceAssessment.reason ?: sourceNotUsableDefault
                    },
                )
                HorizontalDivider()
            }
        }

        Button(
            onClick = ::completeSave,
            enabled = commitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(
                if (managedRecord == null) {
                    stringResource(R.string.add_to_my_eqs_count, stagedSelectedIds.size)
                } else {
                    stringResource(R.string.save_changes_count, stagedSelectedIds.size)
                },
            )
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
    val dimensionLabel = stringResource(dimension.labelResId)
    val allLabel = stringResource(R.string.filter_all)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_by_format, dimensionLabel.lowercase())) },
        text = {
            Column {
                TextButton(
                    onClick = { onSelect(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (selected == null) {
                            stringResource(R.string.filter_selected_format, allLabel)
                        } else {
                            allLabel
                        },
                    )
                }
                options.forEach { option ->
                    TextButton(
                        onClick = { onSelect(option) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (selected == option) {
                                stringResource(R.string.filter_selected_format, option)
                            } else {
                                option
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
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
    onHide: () -> Unit,
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
                text = profile.author?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.creator_information_missing),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Column(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)) {
                if (!profile.isVerified) {
                    Text(
                        text = stringResource(R.string.community_submission_unverified_review),
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
                        Text(stringResource(R.string.action_source))
                    }
                }
                if (compatibility.category == ProfileCompatibility.NotCompatible) {
                    Text(
                        text = stringResource(R.string.source_data_unavailable_for_selection),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    compatibility.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onHide) {
                    Icon(
                        Icons.Outlined.VisibilityOff,
                        contentDescription = stringResource(R.string.hide_from_eq_library_content_description),
                    )
                }
                onToggleFavorite?.let { action ->
                    IconButton(onClick = action) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(
                                if (isFavorite) {
                                    R.string.remove_favorite_content_description
                                } else {
                                    R.string.add_favorite_content_description
                                },
                            ),
                        )
                    }
                }
            }
        },
        modifier = rowModifier,
    )
}

@Composable
private fun outputStatusLabel(status: DeviceExportability, device: ExportDevice): String = stringResource(
    when (status) {
        DeviceExportability.EXACT -> R.string.output_status_exact
        DeviceExportability.OPTIMIZED -> R.string.output_status_optimized
        DeviceExportability.NOT_REPRESENTABLE -> if (device.isHardwareOutput) {
            R.string.output_status_not_suitable
        } else {
            R.string.output_status_not_exportable
        }
    },
)

private fun outputShortName(device: ExportDevice): String = device.displayName

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
