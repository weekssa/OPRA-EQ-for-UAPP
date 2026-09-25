package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.ExportCurrentness
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashPlan
import com.weekssa.opraeqforuapp.domain.blackpearl.buildBlackPearlFlashPlan
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.library.FavoriteToggleResult
import com.weekssa.opraeqforuapp.domain.export.DeviceExportability
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.assessDeviceExportability
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20DeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.adaptationSummary
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import java.util.Locale
import kotlinx.coroutines.launch

private data class ManagedHardwareFlashAssessment(
    val ready: Boolean,
    val reason: String? = null,
    val fidelity: DevicePresetFidelity? = null,
    val playbackGainDb: Double = 0.0,
    val adaptationSummary: String? = null,
    val warning: String? = null,
)

internal fun isManagedHardwareFlashEnabled(
    activeOutput: ExportDevice,
    directBlackPearlFlashEnabled: Boolean,
    blackPearlConnected: Boolean,
    directFiioJa11FlashEnabled: Boolean,
    fiioJa11Connected: Boolean,
    directJcallyJm12FlashEnabled: Boolean,
    jcallyJm12Connected: Boolean,
    directEw300FlashEnabled: Boolean,
    ew300Connected: Boolean,
): Boolean = when (activeOutput) {
    ExportDevice.BLACK_PEARL -> directBlackPearlFlashEnabled && blackPearlConnected
    ExportDevice.FIIO_JA11 -> directFiioJa11FlashEnabled && fiioJa11Connected
    ExportDevice.SIMGOT_EW300 -> directEw300FlashEnabled && ew300Connected
    ExportDevice.JCALLY_JM12 -> directJcallyJm12FlashEnabled && jcallyJm12Connected
    else -> false
}

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
    directFiioJa11FlashEnabled: Boolean,
    fiioJa11ConnectionState: Kt02h20ConnectionState,
    onConnectFiioJa11: () -> Unit,
    directJcallyJm12FlashEnabled: Boolean,
    jcallyJm12ConnectionState: Kt02h20ConnectionState,
    onConnectJcallyJm12: () -> Unit,
    directEw300FlashEnabled: Boolean = false,
    ew300ConnectionState: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
    onConnectEw300: () -> Unit = {},
    onFlashManagedProfile: suspend (String) -> String,
    onToggleFavorite: suspend (OpraEqProfile, String, String) -> FavoriteToggleResult,
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
    var reviewingNewEqs by remember(headphone.productId) { mutableStateOf(false) }
    val readyCatalog = catalogState as? CatalogState.Ready
    val product = readyCatalog?.catalog?.product(headphone.productId)
    val availableProfileCount = readyCatalog?.catalog?.profileCount(headphone.productId)
    val displayedProfiles = remember(headphone.profiles) {
        headphone.profiles.filter { it.selected || it.noLongerAvailable }
    }
    val activeOutput = exportTargets.activeTarget
    val isHardwareOutput = activeOutput in MANAGED_HARDWARE_FLASH_OUTPUTS
    val flashEnabled = isManagedHardwareFlashEnabled(
        activeOutput = activeOutput,
        directBlackPearlFlashEnabled = directBlackPearlFlashEnabled,
        blackPearlConnected = blackPearlConnectionState is BlackPearlConnectionState.Connected,
        directFiioJa11FlashEnabled = directFiioJa11FlashEnabled,
        fiioJa11Connected = fiioJa11ConnectionState is Kt02h20ConnectionState.Connected,
        directJcallyJm12FlashEnabled = directJcallyJm12FlashEnabled,
        jcallyJm12Connected = jcallyJm12ConnectionState is Kt02h20ConnectionState.Connected,
        directEw300FlashEnabled = directEw300FlashEnabled,
        ew300Connected = ew300ConnectionState is Kt02h20ConnectionState.Connected,
    )

    val pendingNewCount = headphone.profiles.count { it.isNewUnreviewed && !it.noLongerAvailable }
    val pendingUpdatedCount = headphone.profiles.count {
        it.isUpdatedUnreviewed && !it.noLongerAvailable && !it.isNewUnreviewed
    }
    val hasPendingReview = headphone.autoIncludeNewProfiles &&
        (pendingNewCount > 0 || pendingUpdatedCount > 0)
    val pendingReviewLabel = buildList {
        if (pendingNewCount > 0) add("$pendingNewCount new")
        if (pendingUpdatedCount > 0) add("$pendingUpdatedCount updated")
    }.joinToString(" · ")

    if (reviewingNewEqs) {
        NewEqReviewScreen(
            headphone = headphone,
            activeOutput = activeOutput,
            onAddSelected = { selectedNewIds ->
                val selectedIds = headphone.profiles
                    .filter(ManagedProfileRecord::selected)
                    .mapTo(mutableSetOf(), ManagedProfileRecord::profileId) + selectedNewIds
                onSaveSelection(headphone.productId, selectedIds, headphone.autoIncludeNewProfiles)
                onMarkReviewed(headphone.productId)
                reviewingNewEqs = false
                onMessage(
                    if (selectedNewIds.isEmpty()) {
                        "New EQ review completed."
                    } else {
                        "New EQ review completed. Selected EQs were saved to My EQs; Export or Flash is separate."
                    },
                )
            },
            onDismissBatch = {
                onMarkReviewed(headphone.productId)
                reviewingNewEqs = false
                onMessage("New EQs marked reviewed. They remain available in EQ Library.")
            },
            onOpenUrl = onOpenUrl,
            onBack = { reviewingNewEqs = false },
            modifier = modifier,
        )
        return
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
    val currentProfiles = readyCatalog?.catalog?.profilesForProduct(headphone.productId).orEmpty()
    var pendingProfileRemoval by remember { mutableStateOf<ManagedProfileRecord?>(null) }
    var pendingProfileFlash by remember { mutableStateOf<ManagedProfileRecord?>(null) }
    var showHeadphoneRemoval by remember { mutableStateOf(false) }
    var deleteSavedFiles by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val favoriteSourceUnavailableMessage = stringResource(R.string.favorite_source_unavailable_message)
    var headphoneMenuOpen by remember(headphone.productId) { mutableStateOf(false) }

    val onNotifyChanged: (Boolean) -> Unit = { enabled ->
        scope.launch {
            val selectedIds = headphone.profiles
                .filter(ManagedProfileRecord::selected)
                .mapTo(mutableSetOf(), ManagedProfileRecord::profileId)
            onSaveSelection(headphone.productId, selectedIds, enabled)
            if (!enabled) onMarkReviewed(headphone.productId)
            onMessage(
                if (enabled) "New-EQ reviews enabled for ${headphone.productName}."
                else "New-EQ reviews disabled for ${headphone.productName}.",
            )
        }
    }

    pendingProfileFlash?.let { profile ->
        val source = profile.lastKnownProfile
        val displayName = source.details?.takeIf(String::isNotBlank)
            ?: source.author?.takeIf(String::isNotBlank)
            ?: "this EQ"
        val assessment = managedHardwareFlashAssessment(source, activeOutput)
        AlertDialog(
            onDismissRequest = { pendingProfileFlash = null },
            title = { Text("Flash to ${managedHardwareTitle(activeOutput)}?") },
            text = { Text(managedHardwareFlashConfirmation(displayName, activeOutput, assessment)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingProfileFlash = null
                        scope.launch { onMessage(onFlashManagedProfile(profile.profileId)) }
                    },
                ) {
                    Text(
                        if (activeOutput == ExportDevice.BLACK_PEARL && !assessment.warning.isNullOrBlank()) {
                            "Flash anyway"
                        } else {
                            "Flash"
                        },
                    )
                }
            },
            dismissButton = { TextButton(onClick = { pendingProfileFlash = null }) { Text("Cancel") } },
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
                    val cleanup = onRemoveManagedProfile(headphone.productId, profile.profileId, deleteSavedFiles)
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
            body = "${headphone.productName} will be removed from My EQs. Exported files are kept unless you choose to delete files created by EQ Library.",
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

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to My EQs")
                }
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        text = headphone.productName,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = buildString {
                            append(headphone.vendorName)
                            append(" · ")
                            append(headphone.selectedProfileCount)
                            append(" selected")
                            availableProfileCount?.let {
                                append(" · ")
                                append(it)
                                append(" available")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { headphoneMenuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Headphone options")
                    }
                    DropdownMenu(expanded = headphoneMenuOpen, onDismissRequest = { headphoneMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Notify me about new EQs: " + if (headphone.autoIncludeNewProfiles) "On" else "Off") },
                            trailingIcon = { if (headphone.autoIncludeNewProfiles) Icon(Icons.Outlined.Check, contentDescription = null) },
                            onClick = {
                                headphoneMenuOpen = false
                                onNotifyChanged(!headphone.autoIncludeNewProfiles)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove headphone…") },
                            onClick = {
                                headphoneMenuOpen = false
                                deleteSavedFiles = false
                                showHeadphoneRemoval = true
                            },
                        )
                    }
                }
            }
            if (hasPendingReview) {
                TextButton(
                    onClick = { reviewingNewEqs = true },
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Text("Review $pendingReviewLabel EQs")
                }
            }
            if (isHardwareOutput || product != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (activeOutput) {
                        ExportDevice.BLACK_PEARL -> CompactBlackPearlConnectionAction(
                            enabled = directBlackPearlFlashEnabled,
                            state = blackPearlConnectionState,
                            onConnect = onConnectBlackPearl,
                            modifier = Modifier.weight(1f),
                        )
                        ExportDevice.FIIO_JA11 -> CompactKt02h20ConnectionAction(
                            enabled = directFiioJa11FlashEnabled,
                            state = fiioJa11ConnectionState,
                            onConnect = onConnectFiioJa11,
                            connectedLabel = "FiiO JA11 · Connected",
                            modifier = Modifier.weight(1f),
                        )
                        ExportDevice.SIMGOT_EW300 -> CompactKt02h20ConnectionAction(
                            enabled = directEw300FlashEnabled,
                            state = ew300ConnectionState,
                            onConnect = onConnectEw300,
                            connectedLabel = "SIMGOT EW300 DSP · Connected",
                            modifier = Modifier.weight(1f),
                        )
                        ExportDevice.JCALLY_JM12 -> CompactKt02h20ConnectionAction(
                            enabled = directJcallyJm12FlashEnabled,
                            state = jcallyJm12ConnectionState,
                            onConnect = onConnectJcallyJm12,
                            connectedLabel = "JCALLY JM12 · Connected",
                            modifier = Modifier.weight(1f),
                        )
                        else -> Unit
                    }
                    if (product != null) {
                        TextButton(
                            onClick = { editing = true },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text("Manage presets") }
                    }
                }
            }
            hardwareConnectionHelp(
                activeOutput = activeOutput,
                directBlackPearlFlashEnabled = directBlackPearlFlashEnabled,
                blackPearlConnectionState = blackPearlConnectionState,
                directFiioJa11FlashEnabled = directFiioJa11FlashEnabled,
                fiioJa11ConnectionState = fiioJa11ConnectionState,
                directEw300FlashEnabled = directEw300FlashEnabled,
                ew300ConnectionState = ew300ConnectionState,
                directJcallyJm12FlashEnabled = directJcallyJm12FlashEnabled,
                jcallyJm12ConnectionState = jcallyJm12ConnectionState,
            )?.let { (message, isError) ->
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (product == null) {
                Text(
                    text = "This headphone is no longer present in the current EQ Library catalog. Retained presets remain available until you remove them.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

        }
        items(displayedProfiles, key = ManagedProfileRecord::profileId) { profile ->
            val outputStatus = assessDeviceExportability(profile.lastKnownProfile, activeOutput)
            val assessment = if (isHardwareOutput) {
                managedHardwareFlashAssessment(profile.lastKnownProfile, activeOutput)
            } else {
                ManagedHardwareFlashAssessment(ready = false)
            }
            ManagedProfileRow(
                profile = profile,
                isFavorite = profile.profileId in favoriteProfileIds,
                activeOutput = activeOutput,
                outputStatus = outputStatus,
                outputAdaptationSummary = assessment.adaptationSummary,
                showExport = activeOutput.supportsFileExport && profile.selected &&
                    exportCurrentness.needsExport(headphone.productId, profile.profileId),
                onExport = { onExportProfile(profile.profileId) },
                showFlash = isHardwareOutput,
                flashEnabled = flashEnabled && profile.selected && assessment.ready,
                flashUnavailableReason = assessment.reason?.takeIf { profile.selected },
                onFlash = { pendingProfileFlash = profile },
                onOpenSource = profile.lastKnownProfile.link?.let { sourceUrl -> { onOpenUrl(sourceUrl) } },
                onToggleFavorite = {
                    scope.launch {
                        val favoriteProfile = resolveManagedFavoriteProfile(
                            profileId = profile.profileId,
                            lastKnownProfile = profile.lastKnownProfile,
                            currentProfiles = currentProfiles,
                        )
                        val result = onToggleFavorite(
                            favoriteProfile,
                            headphone.vendorName,
                            headphone.productName,
                        )
                        onMessage(
                            when (result) {
                                FavoriteToggleResult.SAVED -> "Saved to My EQs favorites."
                                FavoriteToggleResult.REMOVED -> "Removed from My EQs favorites."
                                FavoriteToggleResult.CANONICAL_SOURCE_UNAVAILABLE -> favoriteSourceUnavailableMessage
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

}

private fun managedHardwareFlashAssessment(
    profile: OpraEqProfile,
    device: ExportDevice,
): ManagedHardwareFlashAssessment = when (device) {
    ExportDevice.BLACK_PEARL -> when (val plan = buildBlackPearlFlashPlan(profile, activeSlot = 0x00)) {
        is BlackPearlFlashPlan.Ready -> ManagedHardwareFlashAssessment(
            ready = true,
            fidelity = plan.fidelity,
            playbackGainDb = plan.requiredPlaybackGainDb,
            adaptationSummary = plan.adaptationSummary,
            warning = plan.warning,
        )
        is BlackPearlFlashPlan.NotRepresentable -> ManagedHardwareFlashAssessment(
            ready = false,
            reason = plan.reason,
        )
    }
    ExportDevice.FIIO_JA11 -> managedFiveBandAssessment(profile, Kt02h20DeviceSpecs.FIIO_JA11)
    ExportDevice.SIMGOT_EW300 -> managedFiveBandAssessment(profile, com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs.SIMGOT_EW300)
    ExportDevice.JCALLY_JM12 -> managedFiveBandAssessment(profile, Kt02h20DeviceSpecs.JCALLY_JM12_STOCK)
    else -> ManagedHardwareFlashAssessment(ready = false)
}

private fun managedFiveBandAssessment(
    profile: OpraEqProfile,
    spec: com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandDeviceSpec,
): ManagedHardwareFlashAssessment = when (val result = Kt02h20FiveBandOptimizer.optimize(profile, spec)) {
    is FiveBandOptimizationResult.NotSuitable -> ManagedHardwareFlashAssessment(
        ready = false,
        reason = result.reason,
    )
    is FiveBandOptimizationResult.Ready -> ManagedHardwareFlashAssessment(
        ready = true,
        fidelity = result.representation.fidelity,
        playbackGainDb = result.representation.playbackGainDb,
        adaptationSummary = result.representation.adaptationSummary(),
    )
}

private fun managedHardwareFlashConfirmation(
    displayName: String,
    device: ExportDevice,
    assessment: ManagedHardwareFlashAssessment,
): String {
    if (device == ExportDevice.BLACK_PEARL) {
        return blackPearlFlashConfirmation(
            displayName = displayName,
            gainAdjustmentDb = assessment.playbackGainDb,
            fidelity = assessment.fidelity ?: DevicePresetFidelity.OPTIMIZED,
            adaptationSummary = assessment.adaptationSummary ?: "target-specific adaptation",
            warning = assessment.warning,
        )
    }
    val fidelity = when (assessment.fidelity) {
        DevicePresetFidelity.EXACT -> "Exact · ${assessment.adaptationSummary ?: "source values preserved"}"
        DevicePresetFidelity.OPTIMIZED -> "Optimized · ${assessment.adaptationSummary ?: "target-specific adaptation"}"
        null -> "Not suitable"
    }
    val gain = String.format(Locale.US, "%+.2f", assessment.playbackGainDb)
    val gainSentence = if (kotlin.math.abs(assessment.playbackGainDb) < 0.000_001) {
        "The EQ-related gain will be 0.00 dB."
    } else if (device == ExportDevice.FIIO_JA11) {
        "The JA11 global EQ gain will be set to $gain dB."
    } else {
        "EQ Library will apply a $gain dB tracked playback-gain adjustment for this preset."
    }
    val persistence = if (device == ExportDevice.FIIO_JA11) {
        "The five-band PEQ will be applied, read back, and saved to the JA11."
    } else if (device == ExportDevice.SIMGOT_EW300) {
        "The five-band Peak EQ will be applied and read back on the exact verified EW300 profile."
    } else {
        "The five-band PEQ will be written and read back. Persistence across a full power cycle is still hardware-validation pending for stock JM12 firmware."
    }
    return "Flash $displayName to ${managedHardwareTitle(device)}? $fidelity. $gainSentence $persistence Unrelated DAC settings are not changed."
}

private fun hardwareConnectionHelp(
    activeOutput: ExportDevice,
    directBlackPearlFlashEnabled: Boolean,
    blackPearlConnectionState: BlackPearlConnectionState,
    directFiioJa11FlashEnabled: Boolean,
    fiioJa11ConnectionState: Kt02h20ConnectionState,
    directEw300FlashEnabled: Boolean = false,
    ew300ConnectionState: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
    directJcallyJm12FlashEnabled: Boolean,
    jcallyJm12ConnectionState: Kt02h20ConnectionState,
): Pair<String, Boolean>? = when (activeOutput) {
    ExportDevice.BLACK_PEARL -> when {
        !directBlackPearlFlashEnabled ->
            "Enable direct Flash in Settings → Black Pearl before connecting to the DAC." to false
        blackPearlConnectionState is BlackPearlConnectionState.Error -> blackPearlConnectionState.message to true
        else -> null
    }
    ExportDevice.FIIO_JA11 -> when {
        !directFiioJa11FlashEnabled ->
            "Enable direct Flash in Settings → FiiO JA11 before connecting to the DAC." to false
        fiioJa11ConnectionState is Kt02h20ConnectionState.Error ||
            fiioJa11ConnectionState is Kt02h20ConnectionState.PermissionRequired -> fiioJa11ConnectionState.connectionMessage() to true
        else -> null
    }
    ExportDevice.SIMGOT_EW300 -> when {
        !directEw300FlashEnabled ->
            "Enable direct Flash in Settings → SIMGOT EW300 DSP before connecting to the DAC." to false
        ew300ConnectionState is Kt02h20ConnectionState.Error ||
            ew300ConnectionState is Kt02h20ConnectionState.PermissionRequired -> ew300ConnectionState.connectionMessage() to true
        else -> null
    }
    ExportDevice.JCALLY_JM12 -> when {
        !directJcallyJm12FlashEnabled ->
            "Enable direct Flash in Settings → JCALLY JM12 before connecting to the DAC." to false
        jcallyJm12ConnectionState is Kt02h20ConnectionState.Error ||
            jcallyJm12ConnectionState is Kt02h20ConnectionState.PermissionRequired -> jcallyJm12ConnectionState.connectionMessage() to true
        else -> null
    }
    else -> null
}

@Composable
private fun CompactBlackPearlConnectionAction(
    enabled: Boolean,
    state: BlackPearlConnectionState,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connected = state is BlackPearlConnectionState.Connected
    val connecting = state is BlackPearlConnectionState.Connecting
    if (connected) {
        Row(
            modifier = modifier.heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Black Pearl · Connected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Button(
            onClick = onConnect,
            enabled = enabled && !connecting,
            modifier = modifier.heightIn(min = 48.dp),
        ) {
            Text(if (connecting) "Connecting…" else "Connect")
        }
    }
}

@Composable
private fun CompactKt02h20ConnectionAction(
    enabled: Boolean,
    state: Kt02h20ConnectionState,
    onConnect: () -> Unit,
    connectedLabel: String,
    modifier: Modifier = Modifier,
) {
    val connected = state is Kt02h20ConnectionState.Connected
    val connecting = state is Kt02h20ConnectionState.Connecting
    if (connected) {
        Row(
            modifier = modifier.heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                connectedLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Button(
            onClick = onConnect,
            enabled = enabled && !connecting,
            modifier = modifier.heightIn(min = 48.dp),
        ) {
            Text(if (connecting) "Connecting…" else "Connect")
        }
    }
}

@Composable
private fun ManagedProfileRow(
    profile: ManagedProfileRecord,
    isFavorite: Boolean,
    activeOutput: ExportDevice,
    outputStatus: DeviceExportability,
    outputAdaptationSummary: String?,
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
                when {
                    profile.noLongerAvailable -> Text("No longer available in EQ Library")
                    profile.selected -> Text("Selected")
                    else -> Text("Not selected")
                }
                Text(
                    text = buildString {
                        append("${outputShortName(activeOutput)}: ${outputStatusLabel(outputStatus, activeOutput)}")
                        outputAdaptationSummary?.let { append(" · $it") }
                    },
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
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    onOpenSource?.let { action ->
                        TextButton(onClick = action) { Text("Source") }
                    }
                    if (showExport) {
                        TextButton(onClick = onExport) { Text("Export") }
                    }
                    if (showFlash) {
                        TextButton(enabled = flashEnabled, onClick = onFlash) { Text("Flash") }
                    }
                    onRemove?.let { action ->
                        TextButton(onClick = action) { Text("Remove") }
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
    )
}

private fun outputStatusLabel(status: DeviceExportability, device: ExportDevice): String = when (status) {
    DeviceExportability.EXACT -> "Exact"
    DeviceExportability.OPTIMIZED -> "Optimized"
    DeviceExportability.NOT_REPRESENTABLE -> if (device.isHardwareOutput) "Not suitable" else "Not exportable"
}

private fun outputShortName(device: ExportDevice): String = device.displayName

private fun managedHardwareTitle(device: ExportDevice): String = when (device) {
    ExportDevice.BLACK_PEARL -> "Black Pearl"
    ExportDevice.FIIO_JA11 -> "FiiO JA11"
    ExportDevice.SIMGOT_EW300 -> "SIMGOT EW300 DSP"
    ExportDevice.JCALLY_JM12 -> "JCALLY JM12"
    else -> device.folderName
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
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun Kt02h20ConnectionState.connectionMessage(): String = when (this) {
    is Kt02h20ConnectionState.Error -> message
    is Kt02h20ConnectionState.PermissionRequired -> message
    else -> "Connection unavailable."
}

private val MANAGED_HARDWARE_FLASH_OUTPUTS = setOf(
    ExportDevice.BLACK_PEARL,
    ExportDevice.FIIO_JA11,
    ExportDevice.SIMGOT_EW300,
    ExportDevice.JCALLY_JM12,
)
