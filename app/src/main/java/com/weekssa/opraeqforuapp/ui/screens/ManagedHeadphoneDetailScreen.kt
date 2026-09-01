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
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.ExportCurrentness
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashPlan
import com.weekssa.opraeqforuapp.domain.blackpearl.buildBlackPearlFlashPlan
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DeviceExportability
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.assessDeviceExportability
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import kotlinx.coroutines.launch

@Composable
fun ManagedHeadphoneDetailScreen(
    headphone: ManagedHeadphoneRecord,
    catalogState: CatalogState,
    profileVisibility: ProfileVisibilityPreferences,
    exportTargets: ExportTargetPreferences = ExportTargetPreferences(),
    exportCurrentness: ExportCurrentness,
    favoriteProfileIds: Set<String>,
    directBlackPearlFlashEnabled: Boolean,
    blackPearlConnectionState: BlackPearlConnectionState,
    onConnectBlackPearl: () -> Unit,
    onFlashManagedProfile: suspend (String) -> String,
    onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,
    onHideCanonicalProfile: suspend (String) -> Unit,
    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,
    onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit,
    onRemoveHeadphone: suspend (String) -> Unit,
    onRemoveManagedProfile: suspend (String, String, Boolean) -> PresetCleanupSummary?,
    onRemoveManagedHeadphone: suspend (String, Boolean) -> PresetCleanupSummary?,
    onDeleteSavedFilesForProfiles: suspend (Set<String>) -> PresetCleanupSummary,
    onDeleteSavedFilesForProduct: suspend (String) -> PresetCleanupSummary,
    onMarkReviewed: suspend (String) -> Unit,
    onExportProduct: (String) -> Unit,
    onExportProfile: (String) -> Unit,
    onMessage: (String) -> Unit,
    onOpenUrl: (String) -> Unit = {},
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
    val activeOutput = exportTargets.activeTarget
    val isBlackPearlOutput = activeOutput == ExportDevice.BLACK_PEARL
    val flashEnabled = isBlackPearlOutput &&
        directBlackPearlFlashEnabled &&
        blackPearlConnectionState is BlackPearlConnectionState.Connected

    LaunchedEffect(headphone.productId) {
        onMarkReviewed(headphone.productId)
    }

    if (editing && readyCatalog != null && product != null) {
        ProfileSelectionEditor(
            catalog = readyCatalog.catalog,
            product = product,
            profileVisibility = profileVisibility,
            exportTargets = exportTargets,
            favoriteProfileIds = favoriteProfileIds,
            onToggleFavorite = onToggleFavorite,
            onHideCanonicalProfile = onHideCanonicalProfile,
            onLoadManagedHeadphone = onLoadManagedHeadphone,
            onSaveSelection = onSaveSelection,
            onRemoveHeadphone = onRemoveHeadphone,
            onDeleteSavedFilesForProfiles = onDeleteSavedFilesForProfiles,
            onDeleteSavedFilesForProduct = onDeleteSavedFilesForProduct,
            onExportProduct = onExportProduct,
            onMessage = onMessage,
            onOpenUrl = onOpenUrl,
            onBack = { editing = false },
            modifier = modifier,
        )
        return
    }

    BackHandler(onBack = onBack)
    var pendingProfileRemoval by remember { mutableStateOf<ManagedProfileRecord?>(null) }
    var pendingProfileFlash by remember { mutableStateOf<ManagedProfileRecord?>(null) }
    var showHeadphoneRemoval by remember { mutableStateOf(false) }
    var deleteSavedFiles by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    pendingProfileFlash?.let { profile ->
        val source = profile.lastKnownProfile
        val displayName = source.details?.takeIf(String::isNotBlank)
            ?: source.author?.takeIf(String::isNotBlank)
            ?: "this EQ"
        val flashPlan = buildBlackPearlFlashPlan(source, activeSlot = 0x00) as? BlackPearlFlashPlan.Ready
        AlertDialog(
            onDismissRequest = { pendingProfileFlash = null },
            title = { Text("Flash to Black Pearl?") },
            text = {
                Text(
                    blackPearlFlashConfirmation(
                        displayName = displayName,
                        gainAdjustmentDb = flashPlan?.requiredPlaybackGainDb ?: 0.0,
                        warning = flashPlan?.warning,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingProfileFlash = null
                        scope.launch { onMessage(onFlashManagedProfile(profile.profileId)) }
                    },
                ) { Text(if (flashPlan?.warning.isNullOrBlank()) "Flash" else "Flash anyway") }
            },
            dismissButton = {
                TextButton(onClick = { pendingProfileFlash = null }) { Text("Cancel") }
            },
        )
    }

    pendingProfileRemoval?.let { profile ->
        RemovalDialog(
            title = "Remove preset?",
            body = "This preset will be removed from this headphone in My EQs.",
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
            body = "${headphone.productName} will be removed from My EQs for this output.",
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
            Text("My EQs", modifier = Modifier.padding(start = 4.dp))
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
                append(" selected")
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
                "Automatically include new verified EQ profiles: On"
            } else {
                "Automatically include new verified EQ profiles: Off"
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (isBlackPearlOutput) {
            BlackPearlConnectionControl(
                enabled = directBlackPearlFlashEnabled,
                state = blackPearlConnectionState,
                onConnect = onConnectBlackPearl,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
            )
        }
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
                val outputStatus = assessDeviceExportability(profile.lastKnownProfile, activeOutput)
                val directFlashPlan = if (isBlackPearlOutput) {
                    buildBlackPearlFlashPlan(profile.lastKnownProfile, activeSlot = 0x00)
                } else {
                    null
                }
                val directFlashable = directFlashPlan is BlackPearlFlashPlan.Ready
                val directFlashUnavailableReason = (directFlashPlan as? BlackPearlFlashPlan.NotRepresentable)
                    ?.reason
                    ?.takeIf { profile.selected }
                ManagedProfileRow(
                    profile = profile,
                    isFavorite = profile.profileId in favoriteProfileIds,
                    activeOutput = activeOutput,
                    outputStatus = outputStatus,
                    showExport = profile.selected &&
                        exportCurrentness.needsExport(headphone.productId, profile.profileId),
                    onExport = { onExportProfile(profile.profileId) },
                    showFlash = isBlackPearlOutput,
                    flashEnabled = flashEnabled && profile.selected && directFlashable,
                    flashUnavailableReason = directFlashUnavailableReason,
                    onFlash = { pendingProfileFlash = profile },
                    onOpenSource = profile.lastKnownProfile.link?.let { sourceUrl -> { onOpenUrl(sourceUrl) } },
                    onToggleFavorite = {
                        scope.launch {
                            val favorited = onToggleFavorite(
                                profile.lastKnownProfile,
                                headphone.vendorName,
                                headphone.productName,
                            )
                            onMessage(
                                if (favorited) {
                                    "Saved to My EQs favorites."
                                } else {
                                    "Removed from My EQs favorites."
                                },
                            )
                        }
                    },
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
    isFavorite: Boolean,
    activeOutput: ExportDevice,
    outputStatus: DeviceExportability,
    showExport: Boolean,
    onExport: () -> Unit,
    showFlash: Boolean,
    flashEnabled: Boolean,
    flashUnavailableReason: String?,
    onFlash: () -> Unit,
    onOpenSource: (() -> Unit)?,
    onToggleFavorite: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    val source = profile.lastKnownProfile
    ListItem(
        headlineContent = {
            Text(source.author?.takeIf { it.isNotBlank() } ?: "Creator information missing")
        },
        supportingContent = {
            Column {
                if (!source.isVerified) {
                    Text(
                        text = "Community submission — not independently verified. Review the source before use.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                source.details?.let { Text(it) }
                onOpenSource?.let { action ->
                    TextButton(onClick = action) { Text("Source") }
                }
                when {
                    profile.noLongerAvailable -> Text("No longer available in EQ Library")
                    profile.selected -> Text("Selected")
                    profile.explicitlyExcluded -> Text("Not selected · excluded from automatic inclusion")
                    else -> Text("Not selected")
                }
                Text(
                    text = "${outputShortName(activeOutput)}: ${outputStatusLabel(outputStatus)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = when (outputStatus) {
                        DeviceExportability.EXACT -> MaterialTheme.colorScheme.onSurfaceVariant
                        DeviceExportability.OPTIMIZED -> MaterialTheme.colorScheme.tertiary
                        DeviceExportability.NOT_REPRESENTABLE -> MaterialTheme.colorScheme.error
                    },
                )
                if (showFlash && flashUnavailableReason != null) {
                    Text(
                        text = "Direct Flash unavailable: $flashUnavailableReason",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    )
                }
                if (showExport) {
                    TextButton(onClick = onExport) { Text("Export") }
                }
                if (showFlash) {
                    TextButton(
                        enabled = flashEnabled,
                        onClick = onFlash,
                    ) { Text("Flash") }
                }
                onRemove?.let { action ->
                    TextButton(onClick = action) { Text("Remove") }
                }
            }
        },
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
                        text = "Also delete exported preset files created by EQ Library",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
