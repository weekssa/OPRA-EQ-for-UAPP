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
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashPlan
import com.weekssa.opraeqforuapp.domain.blackpearl.buildBlackPearlFlashPlan
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqCategory
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20DeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import java.util.Locale
import kotlinx.coroutines.launch

private data class HardwareFlashPreview(
    val device: ExportDevice,
    val fidelity: DevicePresetFidelity,
    val playbackGainDb: Double,
    val warning: String? = null,
)

private sealed interface PendingHardwareFlash {
    val displayName: String
    val preview: HardwareFlashPreview

    data class SavedEq(
        val entryId: String,
        override val displayName: String,
        override val preview: HardwareFlashPreview,
    ) : PendingHardwareFlash

    data class GeneralEq(
        val presetId: String,
        override val displayName: String,
        override val preview: HardwareFlashPreview,
    ) : PendingHardwareFlash
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
    onResetBlackPearl: suspend () -> String,
    directFiioJa11FlashEnabled: Boolean,
    fiioJa11ConnectionState: Kt02h20ConnectionState,
    onConnectFiioJa11: () -> Unit,
    onResetFiioJa11: suspend () -> String,
    directJcallyJm12FlashEnabled: Boolean,
    jcallyJm12ConnectionState: Kt02h20ConnectionState,
    onConnectJcallyJm12: () -> Unit,
    onResetJcallyJm12: suspend () -> String,
    onExportAll: () -> Unit,
    onOpenHeadphone: (String) -> Unit,
    onImportPersonal: suspend (
        manufacturer: String,
        model: String,
        displayName: String,
        target: String?,
        peqText: String,
    ) -> SavedEqRecord,
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
    var pendingFlash by remember { mutableStateOf<PendingHardwareFlash?>(null) }
    var pendingResetDevice by remember { mutableStateOf<ExportDevice?>(null) }
    val selectedHeadphoneCount = managedHeadphones.sumOf(ManagedHeadphoneRecord::selectedProfileCount)
    val headphoneSavedEqs = remember(savedEqs) { savedEqs.toList() }
    val hardwareFlashOutput = activeOutput in HARDWARE_FLASH_OUTPUTS
    val flashActionsEnabled = when (activeOutput) {
        ExportDevice.BLACK_PEARL -> directBlackPearlFlashEnabled &&
            blackPearlConnectionState is BlackPearlConnectionState.Connected
        ExportDevice.FIIO_JA11 -> directFiioJa11FlashEnabled &&
            fiioJa11ConnectionState is Kt02h20ConnectionState.Connected
        ExportDevice.JCALLY_JM12 -> directJcallyJm12FlashEnabled &&
            jcallyJm12ConnectionState is Kt02h20ConnectionState.Connected
        else -> false
    }

    if (importOpen) {
        PersonalEqImportScreen(
            onBack = { importOpen = false },
            onSave = onImportPersonal,
            onSaved = { record ->
                importOpen = false
                onMessage("Personal EQ saved to My EQs.")
                onExportSavedEq(record.entryId)
            },
            onMessage = onMessage,
            modifier = modifier,
        )
        return
    }

    pendingFlash?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingFlash = null },
            title = { Text("Flash to ${hardwareDeviceTitle(pending.preview.device)}?") },
            text = {
                Text(hardwareFlashConfirmation(pending.displayName, pending.preview))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingFlash = null
                        scope.launch {
                            val message = when (pending) {
                                is PendingHardwareFlash.SavedEq -> onFlashSavedEq(pending.entryId)
                                is PendingHardwareFlash.GeneralEq -> onFlashGeneralEq(pending.presetId)
                            }
                            onMessage(message)
                        }
                    },
                ) {
                    Text(
                        if (pending.preview.device == ExportDevice.BLACK_PEARL &&
                            !pending.preview.warning.isNullOrBlank()
                        ) {
                            "Flash anyway"
                        } else {
                            "Flash"
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFlash = null }) { Text("Cancel") }
            },
        )
    }

    pendingResetDevice?.let { device ->
        AlertDialog(
            onDismissRequest = { pendingResetDevice = null },
            title = { Text("Reset EQ to flat?") },
            text = { Text(hardwareResetConfirmation(device)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingResetDevice = null
                        scope.launch {
                            val message = when (device) {
                                ExportDevice.BLACK_PEARL -> onResetBlackPearl()
                                ExportDevice.FIIO_JA11 -> onResetFiioJa11()
                                ExportDevice.JCALLY_JM12 -> onResetJcallyJm12()
                                else -> "Reset is not available for this output."
                            }
                            onMessage(message)
                        }
                    },
                ) { Text("Reset to flat") }
            },
            dismissButton = {
                TextButton(onClick = { pendingResetDevice = null }) { Text("Cancel") }
            },
        )
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "my-eqs-actions") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                when (activeOutput) {
                    ExportDevice.BLACK_PEARL -> BlackPearlConnectionControl(
                        enabled = directBlackPearlFlashEnabled,
                        state = blackPearlConnectionState,
                        onConnect = onConnectBlackPearl,
                        onReset = { pendingResetDevice = ExportDevice.BLACK_PEARL },
                    )
                    ExportDevice.FIIO_JA11 -> Kt02h20ConnectionControl(
                        device = ExportDevice.FIIO_JA11,
                        enabled = directFiioJa11FlashEnabled,
                        state = fiioJa11ConnectionState,
                        onConnect = onConnectFiioJa11,
                        onReset = { pendingResetDevice = ExportDevice.FIIO_JA11 },
                    )
                    ExportDevice.JCALLY_JM12 -> Kt02h20ConnectionControl(
                        device = ExportDevice.JCALLY_JM12,
                        enabled = directJcallyJm12FlashEnabled,
                        state = jcallyJm12ConnectionState,
                        onConnect = onConnectJcallyJm12,
                        onReset = { pendingResetDevice = ExportDevice.JCALLY_JM12 },
                    )
                    else -> Unit
                }
                if (exportCurrentness.hasPendingExport) {
                    Button(onClick = onExportAll) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null)
                        Text("Export all", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                if (exportCurrentness.hasPendingExport) {
                    Text(
                        text = exportStatusText(exportCurrentness),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                                Column {
                                    Text(
                                        if (pendingCount > 0) {
                                            "${headphone.selectedProfileCount} selected · $pendingCount ${if (pendingCount == 1) "preset needs" else "presets need"} export"
                                        } else {
                                            "${headphone.selectedProfileCount} selected profiles"
                                        },
                                    )
                                    newEqAttentionText(headphone)?.let { attention ->
                                        Text(
                                            attention,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.tertiary,
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

            if (headphoneSavedEqs.isNotEmpty()) {
                item(key = "saved-headphone-heading") {
                    SavedImportsHeading(onImport = { importOpen = true })
                }
                items(headphoneSavedEqs, key = { "saved:${it.entryId}" }) { record ->
                    val needsExport = exportCurrentness.needsExport(record.productId, record.profile.id)
                    val flashPreview = hardwareFlashPreview(record.profile, activeOutput)
                    ListItem(
                        headlineContent = { Text(record.displayName) },
                        supportingContent = { Text("${record.manufacturer} · ${record.model}") },
                        trailingContent = {
                            Row {
                                if (needsExport) {
                                    IconButton(onClick = { onExportSavedEq(record.entryId) }) {
                                        Icon(Icons.Outlined.FileUpload, contentDescription = "Export ${record.displayName}")
                                    }
                                }
                                if (hardwareFlashOutput) {
                                    TextButton(
                                        enabled = flashActionsEnabled && flashPreview != null,
                                        onClick = {
                                            flashPreview?.let { preview ->
                                                pendingFlash = PendingHardwareFlash.SavedEq(
                                                    entryId = record.entryId,
                                                    displayName = record.displayName,
                                                    preview = preview,
                                                )
                                            }
                                        },
                                    ) { Text("Flash") }
                                }
                                if (record.kind == SavedEqKind.Favorite) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                onDeleteSavedEq(record.entryId)
                                                onMessage("Removed from My EQs favorites. Existing exported files were kept.")
                                            }
                                        },
                                    ) {
                                        Icon(
                                            Icons.Outlined.Star,
                                            contentDescription = "Remove ${record.displayName} from favorites",
                                        )
                                    }
                                } else {
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
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }

        if (headphoneSavedEqs.isEmpty()) {
            item(key = "saved-headphone-heading-empty") {
                SavedImportsHeading(onImport = { importOpen = true })
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
                val flashPreview = hardwareFlashPreview(record.profile, activeOutput)
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
                            if (hardwareFlashOutput) {
                                TextButton(
                                    enabled = flashActionsEnabled && flashPreview != null,
                                    onClick = {
                                        flashPreview?.let { preview ->
                                            pendingFlash = PendingHardwareFlash.GeneralEq(
                                                presetId = record.presetId,
                                                displayName = record.displayName,
                                                preview = preview,
                                            )
                                        }
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

private fun hardwareFlashPreview(profile: OpraEqProfile, device: ExportDevice): HardwareFlashPreview? = when (device) {
    ExportDevice.BLACK_PEARL -> when (val plan = buildBlackPearlFlashPlan(profile, activeSlot = 0x00)) {
        is BlackPearlFlashPlan.NotRepresentable -> null
        is BlackPearlFlashPlan.Ready -> HardwareFlashPreview(
            device = device,
            fidelity = plan.fidelity,
            playbackGainDb = plan.requiredPlaybackGainDb,
            warning = plan.warning,
        )
    }
    ExportDevice.FIIO_JA11 -> fiveBandFlashPreview(profile, device, Kt02h20DeviceSpecs.FIIO_JA11)
    ExportDevice.JCALLY_JM12 -> fiveBandFlashPreview(profile, device, Kt02h20DeviceSpecs.JCALLY_JM12_STOCK)
    else -> null
}

private fun fiveBandFlashPreview(
    profile: OpraEqProfile,
    device: ExportDevice,
    spec: com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandDeviceSpec,
): HardwareFlashPreview? = when (val result = Kt02h20FiveBandOptimizer.optimize(profile, spec)) {
    is FiveBandOptimizationResult.NotSuitable -> null
    is FiveBandOptimizationResult.Ready -> HardwareFlashPreview(
        device = device,
        fidelity = result.representation.fidelity,
        playbackGainDb = result.representation.playbackGainDb,
    )
}

private fun hardwareFlashConfirmation(displayName: String, preview: HardwareFlashPreview): String {
    if (preview.device == ExportDevice.BLACK_PEARL) {
        return blackPearlFlashConfirmation(
            displayName = displayName,
            gainAdjustmentDb = preview.playbackGainDb,
            warning = preview.warning,
        )
    }

    val fidelity = when (preview.fidelity) {
        DevicePresetFidelity.EXACT -> "Exact"
        DevicePresetFidelity.OPTIMIZED -> "Optimized — adapted to the device’s 5-band PEQ to closely match the original EQ response"
    }
    val gain = String.format(Locale.US, "%+.2f", preview.playbackGainDb)
    val gainSentence = if (kotlin.math.abs(preview.playbackGainDb) < 0.000_001) {
        "The EQ-related gain will be 0.00 dB."
    } else {
        when (preview.device) {
            ExportDevice.FIIO_JA11 -> "The JA11 global EQ gain will be set to $gain dB."
            ExportDevice.JCALLY_JM12 -> "EQ Library will apply a $gain dB tracked playback-gain adjustment for this preset."
            else -> ""
        }
    }
    val persistence = when (preview.device) {
        ExportDevice.FIIO_JA11 -> "The five-band PEQ will be applied, read back, and saved to the JA11."
        ExportDevice.JCALLY_JM12 -> "The five-band PEQ will be written and read back. Persistence across a full power cycle is still hardware-validation pending for stock JM12 firmware."
        else -> ""
    }
    return "Flash $displayName to ${hardwareDeviceTitle(preview.device)}? $fidelity. $gainSentence $persistence Unrelated DAC settings are not changed."
}

private fun hardwareResetConfirmation(device: ExportDevice): String = when (device) {
    ExportDevice.BLACK_PEARL ->
        "This will overwrite all 10 EQ bands in the Black Pearl's current EQ slot with flat settings and remove any playback-gain adjustment previously applied by EQ Library. This may change listening volume. Other DAC settings will not be changed."
    ExportDevice.FIIO_JA11 ->
        "This will return all five JA11 PEQ bands and the global EQ gain to flat/0 dB, apply the result, verify it, and save it to the device. Listening volume may change. Other DAC settings will not be changed."
    ExportDevice.JCALLY_JM12 ->
        "This will return all five stock JM12 PEQ bands to flat and remove EQ Library's tracked playback-gain adjustment. Listening volume may change. Persistence across a full power cycle is still hardware-validation pending. Other DAC settings will not be changed."
    else -> "Reset is not available for this output."
}

private fun hardwareDeviceTitle(device: ExportDevice): String = when (device) {
    ExportDevice.BLACK_PEARL -> "Black Pearl"
    ExportDevice.FIIO_JA11 -> "FiiO JA11"
    ExportDevice.JCALLY_JM12 -> "JCALLY JM12"
    else -> device.folderName
}

private fun newEqAttentionText(headphone: ManagedHeadphoneRecord): String? {
    if (!headphone.autoIncludeNewProfiles) return null
    val newCount = headphone.profiles.count { it.isNewUnreviewed && !it.noLongerAvailable }
    val updatedCount = headphone.profiles.count {
        it.isUpdatedUnreviewed && !it.noLongerAvailable && !it.isNewUnreviewed
    }
    return buildList {
        if (newCount > 0) add("$newCount new ${if (newCount == 1) "EQ" else "EQs"}")
        if (updatedCount > 0) add("$updatedCount ${if (updatedCount == 1) "EQ updated" else "EQs updated"}")
    }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
fun BlackPearlConnectionControl(
    enabled: Boolean,
    state: BlackPearlConnectionState,
    onConnect: () -> Unit,
    onReset: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onConnect,
                enabled = !connected && !connecting,
                modifier = Modifier.weight(1f),
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
                        else -> "Connect"
                    },
                )
            }
            OutlinedButton(
                onClick = onReset,
                enabled = connected,
                modifier = Modifier.weight(1.25f),
            ) {
                Text("Reset EQ to flat")
            }
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
private fun Kt02h20ConnectionControl(
    device: ExportDevice,
    enabled: Boolean,
    state: Kt02h20ConnectionState,
    onConnect: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        if (!enabled) {
            OutlinedButton(onClick = {}, enabled = false) { Text("Direct Flash disabled") }
            Text(
                text = "Enable direct Flash in Settings → ${hardwareDeviceTitle(device)} before connecting to the DAC.",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val connected = state is Kt02h20ConnectionState.Connected
        val connecting = state is Kt02h20ConnectionState.Connecting
        val containerColor = if (connected) CONNECTED_GREEN else MaterialTheme.colorScheme.error
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onConnect,
                enabled = !connected && !connecting,
                modifier = Modifier.weight(1f),
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
                        else -> "Connect"
                    },
                )
            }
            OutlinedButton(
                onClick = onReset,
                enabled = connected,
                modifier = Modifier.weight(1.25f),
            ) { Text("Reset EQ to flat") }
        }
        if (state is Kt02h20ConnectionState.Error) {
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
private fun SavedImportsHeading(onImport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = "Saved snapshots & personal imports",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onImport) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text("Import", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

private fun exportStatusText(exportCurrentness: ExportCurrentness): String {
    val pendingCount = exportCurrentness.needsExportItems.size
    return "$pendingCount ${if (pendingCount == 1) "preset needs" else "presets need"} export."
}

internal fun blackPearlFlashConfirmation(displayName: String, gainAdjustmentDb: Double): String {
    val gainText = String.format(Locale.US, "%+.2f", gainAdjustmentDb)
    val gainSentence = if (kotlin.math.abs(gainAdjustmentDb) < 0.000_001) {
        "No playback-gain adjustment is required."
    } else {
        "EQ Library will adjust the Black Pearl global playback gain by $gainText dB to apply this preset's preamp/headroom. This changes listening volume."
    }
    return "Flash $displayName to the Black Pearl's current EQ slot? This overwrites that EQ slot. $gainSentence Other DAC settings are not changed."
}

private fun generalCategoryLabel(category: GeneralEqCategory): String = when (category) {
    GeneralEqCategory.SOUND -> "Sound"
    GeneralEqCategory.GENRE -> "Genre"
    GeneralEqCategory.UTILITY -> "Utility"
}

private fun generalExportProductId(presetId: String): String = "general-export:$presetId"

private val HARDWARE_FLASH_OUTPUTS = setOf(
    ExportDevice.BLACK_PEARL,
    ExportDevice.FIIO_JA11,
    ExportDevice.JCALLY_JM12,
)

private val CONNECTED_GREEN = Color(0xFF2E7D32)
