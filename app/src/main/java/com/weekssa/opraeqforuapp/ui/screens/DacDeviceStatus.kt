package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.domain.dac.DacCapabilityCatalog
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacMetadataOrigin
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceControls
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiState
import com.weekssa.opraeqforuapp.ui.Ja11DcrTuningUiState
import com.weekssa.opraeqforuapp.ui.ja11DcrRowValue
import com.weekssa.opraeqforuapp.ui.FiioJa11StartupDefaultStatus
import com.weekssa.opraeqforuapp.ui.fiioJa11StartupStatusText
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsEqReadback
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsRequest
import com.weekssa.opraeqforuapp.data.dac.FiioJa11DefaultsStage
import com.weekssa.opraeqforuapp.ui.fiioJa11DefaultsConfirmationText
import com.weekssa.opraeqforuapp.ui.fiioJa11DefaultsEqReadbackText
import com.weekssa.opraeqforuapp.ui.fiioJa11DefaultsReportIsVerified
import com.weekssa.opraeqforuapp.ui.fiioJa11DefaultsReportText
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun CapabilityDrivenDeviceStatus(
    deviceId: DacDeviceId,
    blackPearlQualificationState: BlackPearlQualificationUiState,
    blackPearlQualificationEnabled: Boolean,
    onReadBlackPearlQualification: () -> Unit,
    onSetBlackPearlDeviceControl: (DacControlId, DacControlValue) -> Unit = { _, _ -> },
    fiioJa11DeviceState: FiioJa11DeviceUiState = FiioJa11DeviceUiState(),
    fiioJa11Connected: Boolean = false,
    onReadFiioJa11DeviceControls: () -> Unit = {},
    onSetFiioJa11OutputVolume: (Int) -> Unit = {},
    onSetFiioJa11EqProgram: (FiioJa11Protocol.EqProgram) -> Unit = {},
    onSetFiioJa11Defaults: (FiioJa11DefaultsRequest) -> Unit = {},
    onSetFiioJa11HeadsetControl: (Boolean) -> Unit = {},
    onSetFiioJa11UacMode: (FiioJa11Protocol.UacMode) -> Unit = {},
    fiioJa11OperationBusy: Boolean = false,
    ja11DcrTuningState: Ja11DcrTuningUiState = Ja11DcrTuningUiState(),
    onSetJa11DcrTuning: (Boolean) -> Unit = {},
) {
    when (deviceId) {
        DacDeviceId.TRN_BLACK_PEARL -> BlackPearlDeviceStatus(
            state = blackPearlQualificationState,
            enabled = blackPearlQualificationEnabled,
            onRead = onReadBlackPearlQualification,
            onSetDeviceControl = onSetBlackPearlDeviceControl,
        )
        DacDeviceId.FIIO_JA11 -> FiioJa11DeviceStatus(
            state = fiioJa11DeviceState,
            connected = fiioJa11Connected,
            onRead = onReadFiioJa11DeviceControls,
            onSetVolume = onSetFiioJa11OutputVolume,
            onSetEqProgram = onSetFiioJa11EqProgram,
            onSetDefaults = onSetFiioJa11Defaults,
            onSetHeadsetControl = onSetFiioJa11HeadsetControl,
            onSetUacMode = onSetFiioJa11UacMode,
            operationBusy = fiioJa11OperationBusy,
            dcrTuningState = ja11DcrTuningState,
            onSetDcrTuning = onSetJa11DcrTuning,
        )
        DacDeviceId.SIMGOT_EW300 -> Text(
            text = "EW300 five-band EQ is managed from My EQs / EQ Library.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DacDeviceId.JCALLY_JM12_STOCK -> Text(
            text = "This USB device is not supported.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BlackPearlDeviceStatus(
    state: BlackPearlQualificationUiState,
    enabled: Boolean,
    onRead: () -> Unit,
    onSetDeviceControl: (DacControlId, DacControlValue) -> Unit,
) {
    val identity = DacCapabilityCatalog.forDevice(DacDeviceId.TRN_BLACK_PEARL).identity
    var aboutExpanded by rememberSaveable { mutableStateOf(false) }

    BlackPearlDeviceBatchControlPanel(
        state = state,
        enabled = enabled,
        onRead = onRead,
        onSetDeviceControl = onSetDeviceControl,
    )

    HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    ExpandableAboutHeader(
        expanded = aboutExpanded,
        onToggle = { aboutExpanded = !aboutExpanded },
    )
    if (aboutExpanded) {
        DeviceInfoLine(
            label = stringResource(R.string.my_dac_device_model),
            value = "${identity.manufacturer.value} ${identity.model.value}",
            origin = identity.model.origin,
        )
        state.snapshot?.firmwareVersion?.let { firmware ->
            DeviceInfoLine(
                label = "Firmware",
                value = firmware,
                origin = DacMetadataOrigin.DEVICE_REPORTED,
            )
        }
        DeviceInfoLine(
            label = stringResource(R.string.my_dac_device_usb_identity),
            value = String.format(Locale.US, "%04X:%04X", identity.usbVendorId, identity.usbProductId),
            originLabel = stringResource(R.string.my_dac_origin_known_capability),
        )
        DeviceInfoLine(
            label = stringResource(R.string.my_dac_device_validation),
            value = stringResource(R.string.my_dac_validation_qualified_short),
            originLabel = stringResource(R.string.my_dac_origin_validation_status),
        )
    }
}

@Composable
private fun FiioJa11DeviceStatus(
    state: FiioJa11DeviceUiState,
    connected: Boolean,
    onRead: () -> Unit,
    onSetVolume: (Int) -> Unit,
    onSetEqProgram: (FiioJa11Protocol.EqProgram) -> Unit,
    onSetDefaults: (FiioJa11DefaultsRequest) -> Unit,
    onSetHeadsetControl: (Boolean) -> Unit,
    onSetUacMode: (FiioJa11Protocol.UacMode) -> Unit,
    operationBusy: Boolean,
    dcrTuningState: Ja11DcrTuningUiState = Ja11DcrTuningUiState(),
    onSetDcrTuning: (Boolean) -> Unit = {},
) {
    var stagedVolume by rememberSaveable { mutableStateOf<Int?>(null) }
    var choosingEqProgram by rememberSaveable { mutableStateOf(false) }
    var stagedEqProgramName by rememberSaveable { mutableStateOf<String?>(null) }
    var defaultsReview by remember { mutableStateOf<FiioJa11DefaultsRequest?>(null) }
    var pendingHeadsetValue by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var choosingUacMode by rememberSaveable { mutableStateOf(false) }
    var aboutExpanded by rememberSaveable { mutableStateOf(false) }

    val snapshot = state.snapshot
    val stagedProgram = stagedEqProgramName?.let { name ->
        FiioJa11Protocol.EqProgram.entries.firstOrNull { it.name == name }
    } ?: snapshot?.eqProgram
    val controlsEnabled = connected &&
        state.isCurrentSession &&
        !state.isBusy &&
        state.pendingRestartWrite == null &&
        !operationBusy

    DeviceOperationStatusHeader(
        isReading = state.isReading,
        isWriting = state.isWriting,
        activeWriteControlId = state.activeWriteControlId,
        pendingVerificationControlId = state.pendingRestartWrite?.controlId,
        lastVerifiedWriteControlId = state.lastVerifiedWriteControlId,
        hasSnapshot = snapshot != null,
        isCurrentSession = state.isCurrentSession,
        error = state.error,
        enabled = connected,
        busy = state.isBusy || state.pendingRestartWrite != null || operationBusy,
        operationMessage = if (state.isSavingDefaults) "Saving startup defaults…" else null,
        onRefresh = onRead,
        controlName = ::fiioControlName,
    )

    if (snapshot == null) {
        Text(
            text = if (connected) {
                "Current settings appear automatically after JA11 is read."
            } else {
                "Connect JA11 to view its settings."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        return
    }

    DeviceSectionTitle("Audio")
    SimpleDeviceSettingRow(
        label = "Device volume",
        value = snapshot.outputVolume.toString() + " / " + FiioJa11Protocol.MAX_OUTPUT_VOLUME,
        enabled = controlsEnabled,
        onClick = { stagedVolume = snapshot.outputVolume },
    )
    SimpleDeviceSettingRow(
        label = "EQ program",
        value = snapshot.eqProgram.technicalLabel,
        enabled = controlsEnabled,
        onClick = { choosingEqProgram = true },
    )
    if (BuildConfig.JA11_TUNING_ENABLED) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = dcrTuningState.tuningEnabled,
                    enabled = controlsEnabled && !dcrTuningState.isBusy,
                    role = Role.Switch,
                    onValueChange = onSetDcrTuning,
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tuning (Ellyn)", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = com.weekssa.opraeqforuapp.ui.ja11DcrToggleValue(dcrTuningState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (controlsEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                dcrTuningState.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Switch(checked = dcrTuningState.tuningEnabled, onCheckedChange = null)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Startup settings", style = MaterialTheme.typography.labelLarge)
            when (state.startupDefaultStatus) {
                FiioJa11StartupDefaultStatus.SAVED -> Text(
                    "Saved",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FiioJa11StartupDefaultStatus.RUNTIME_ONLY -> Text(
                    "Not saved",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FiioJa11StartupDefaultStatus.UNKNOWN -> Unit
            }
        }
        if (state.startupDefaultStatus == FiioJa11StartupDefaultStatus.RUNTIME_ONLY) {
            TextButton(
                onClick = {
                    onSetDefaults(
                        FiioJa11DefaultsRequest(
                            reviewedSnapshot = snapshot,
                            requestedVolume = snapshot.outputVolume,
                            requestedProgram = snapshot.eqProgram,
                            targetControlId = FiioJa11DeviceControls.OUTPUT_VOLUME,
                        ),
                    )
                },
                enabled = controlsEnabled,
            ) { Text("Save") }
        }
    }

    DeviceSectionTitle("Microphone")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = snapshot.headsetControlEnabled,
                enabled = controlsEnabled,
                role = Role.Switch,
                onValueChange = { pendingHeadsetValue = it },
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Headset / mic remote", style = MaterialTheme.typography.labelLarge)
            Text(
                text = if (snapshot.headsetControlEnabled) "On" else "Off",
                style = MaterialTheme.typography.bodyMedium,
                color = if (controlsEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Switch(
            checked = snapshot.headsetControlEnabled,
            onCheckedChange = null,
        )
    }

    DeviceSectionTitle("USB & System")
    SimpleDeviceSettingRow(
        label = "USB Audio Class",
        value = snapshot.uacMode.technicalLabel,
        enabled = controlsEnabled,
        onClick = { choosingUacMode = true },
    )
    ReadOnlyValueRow(label = "Current sample rate", value = snapshot.sampleRateLabel)

    HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    ExpandableAboutHeader(
        expanded = aboutExpanded,
        onToggle = { aboutExpanded = !aboutExpanded },
    )
    if (aboutExpanded) {
        DeviceInfoLine(
            label = stringResource(R.string.my_dac_device_model),
            value = "FiiO JA11",
            originLabel = stringResource(R.string.my_dac_origin_known_capability),
        )
        DeviceInfoLine(
            label = "Firmware",
            value = snapshot.firmwareVersion,
            origin = DacMetadataOrigin.DEVICE_REPORTED,
        )
        DeviceInfoLine(
            label = stringResource(R.string.my_dac_device_usb_identity),
            value = String.format(Locale.US, "2972:%04X", snapshot.usbProductId),
            origin = DacMetadataOrigin.USB_REPORTED,
        )
        DeviceInfoLine(
            label = stringResource(R.string.my_dac_device_validation),
            value = "Hardware validation pending",
            originLabel = stringResource(R.string.my_dac_origin_validation_status),
        )
    }

    stagedVolume?.takeIf { defaultsReview == null }?.let { staged ->
        AlertDialog(
            onDismissRequest = { stagedVolume = null },
            title = { Text("Device volume") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "$staged / ${FiioJa11Protocol.MAX_OUTPUT_VOLUME}",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Slider(
                        value = staged.toFloat(),
                        onValueChange = { value -> stagedVolume = value.roundToInt() },
                        valueRange = FiioJa11Protocol.MIN_OUTPUT_VOLUME.toFloat()..FiioJa11Protocol.MAX_OUTPUT_VOLUME.toFloat(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        stagedVolume = null
                        onSetVolume(staged)
                    },
                    enabled = controlsEnabled && staged != snapshot.outputVolume,
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { stagedVolume = null }) { Text("Cancel") }
            },
        )
    }

    if (choosingEqProgram && defaultsReview == null) {
        AlertDialog(
            onDismissRequest = { choosingEqProgram = false; stagedEqProgramName = null },
            title = { Text("EQ program") },
            text = {
                Column {
                    FiioJa11Protocol.EqProgram.entries.forEach { program ->
                        val current = program == snapshot.eqProgram
                        val selected = program == stagedProgram
                        TextButton(
                            onClick = {
                                stagedEqProgramName = program.name
                            },
                            enabled = controlsEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(buildString {
                                append(if (selected) "● " else "○ ")
                                append(program.technicalLabel)
                                if (current) append(" · Current")
                            })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        choosingEqProgram = false
                        stagedProgram?.let(onSetEqProgram)
                        stagedEqProgramName = null
                    },
                    enabled = controlsEnabled && stagedProgram != snapshot.eqProgram,
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { choosingEqProgram = false; stagedEqProgramName = null }) { Text("Cancel") }
            },
        )
    }

    pendingHeadsetValue?.let { requested ->
        AlertDialog(
            onDismissRequest = { pendingHeadsetValue = null },
            title = { Text("Change headset / mic remote?") },
            text = {
                Text("JA11 will reconnect.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingHeadsetValue = null
                        onSetHeadsetControl(requested)
                    },
                    enabled = controlsEnabled && requested != snapshot.headsetControlEnabled,
                ) { Text("Change") }
            },
            dismissButton = {
                TextButton(onClick = { pendingHeadsetValue = null }) { Text("Cancel") }
            },
        )
    }

    if (choosingUacMode) {
        AlertDialog(
            onDismissRequest = { choosingUacMode = false },
            title = { Text("USB Audio Class") },
            text = {
                Column {
                    Text(
                        text = "USB audio reconnects.",
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    FiioJa11Protocol.UacMode.entries.forEach { mode ->
                        val current = mode == snapshot.uacMode
                        TextButton(
                            onClick = {
                                choosingUacMode = false
                                onSetUacMode(mode)
                            },
                            enabled = controlsEnabled && !current,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (current) "${mode.technicalLabel} · Current" else mode.technicalLabel)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { choosingUacMode = false }) { Text("Cancel") }
            },
        )
    }
}

private fun fiioControlName(controlId: DacControlId): String = when (controlId) {
    FiioJa11DeviceControls.OUTPUT_VOLUME -> "volume"
    FiioJa11DeviceControls.EQ_PROGRAM -> "EQ program"
    FiioJa11DeviceControls.HEADSET_CONTROL -> "headset / mic remote"
    FiioJa11DeviceControls.UAC_MODE -> "USB Audio Class"
    else -> "device setting"
}

@Composable
internal fun DeviceSectionTitle(title: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 14.dp, bottom = 10.dp))
    Text(title, fontWeight = FontWeight.SemiBold)
}

@Composable
internal fun SimpleDeviceSettingRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (enabled) {
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
internal fun ReadOnlyValueRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun ExpandableAboutHeader(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("About this DAC", fontWeight = FontWeight.SemiBold)
        Text(
            text = if (expanded) "⌃" else "›",
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun DeviceInfoLine(
    label: String,
    value: String,
    origin: DacMetadataOrigin? = null,
    originLabel: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = originLabel ?: originLabel(origin),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun originLabel(origin: DacMetadataOrigin?): String = stringResource(
    when (origin) {
        DacMetadataOrigin.USB_REPORTED -> R.string.my_dac_origin_usb_reported
        DacMetadataOrigin.DEVICE_REPORTED -> R.string.my_dac_origin_device_reported
        DacMetadataOrigin.STATIC_KNOWN,
        null,
        -> R.string.my_dac_origin_known_capability
    },
)
