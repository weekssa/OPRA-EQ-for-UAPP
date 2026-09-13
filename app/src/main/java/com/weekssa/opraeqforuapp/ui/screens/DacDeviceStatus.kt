package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControlReadCodec
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlVolumeScale
import com.weekssa.opraeqforuapp.domain.dac.DacCapabilityCatalog
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacMetadataOrigin
import com.weekssa.opraeqforuapp.domain.dac.DacValidationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiState
import java.util.Locale

@Composable
internal fun CapabilityDrivenDeviceStatus(
    deviceId: DacDeviceId,
    blackPearlQualificationState: BlackPearlQualificationUiState,
    blackPearlQualificationEnabled: Boolean,
    onReadBlackPearlQualification: () -> Unit,
    fiioJa11DeviceState: FiioJa11DeviceUiState = FiioJa11DeviceUiState(),
    fiioJa11Connected: Boolean = false,
    onReadFiioJa11DeviceControls: () -> Unit = {},
    onSetFiioJa11OutputVolume: (Int) -> Unit = {},
    onSetFiioJa11EqProgram: (FiioJa11Protocol.EqProgram) -> Unit = {},
    onSetFiioJa11HeadsetControl: (Boolean) -> Unit = {},
    onSetFiioJa11UacMode: (FiioJa11Protocol.UacMode) -> Unit = {},
) {
    when (deviceId) {
        DacDeviceId.TRN_BLACK_PEARL -> BlackPearlDeviceStatus(
            state = blackPearlQualificationState,
            enabled = blackPearlQualificationEnabled,
            onRead = onReadBlackPearlQualification,
        )
        DacDeviceId.FIIO_JA11 -> FiioJa11DeviceStatus(
            state = fiioJa11DeviceState,
            connected = fiioJa11Connected,
            onRead = onReadFiioJa11DeviceControls,
            onSetVolume = onSetFiioJa11OutputVolume,
            onSetEqProgram = onSetFiioJa11EqProgram,
            onSetHeadsetControl = onSetFiioJa11HeadsetControl,
            onSetUacMode = onSetFiioJa11UacMode,
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
) {
    val identity = DacCapabilityCatalog.forDevice(DacDeviceId.TRN_BLACK_PEARL).identity
    state.snapshot?.let { snapshot ->
        Text("Overview", fontWeight = FontWeight.SemiBold)
        QualificationValue(
            label = "Volume",
            value = "${BlackPearlVolumeScale.percentFromRaw(snapshot.playbackGainRaw)}%",
        )
        QualificationValue(
            label = "DAC filter",
            value = filterLabel(snapshot.filterCode),
        )
        QualificationValue(
            label = "Gain",
            value = gainModeLabel(snapshot.gainModeCode),
        )
        QualificationValue(
            label = "Amp",
            value = ampTopologyLabel(snapshot.ampTopologyCode),
        )
        Text(
            text = if (state.isCurrentSession) "Current device state" else "Last read · USB session changed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        HorizontalDivider()
    }

    Text(
        text = stringResource(R.string.my_dac_device_info),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
    DeviceInfoLine(
        label = stringResource(R.string.my_dac_device_model),
        value = "${identity.manufacturer.value} ${identity.model.value}",
        origin = identity.model.origin,
    )
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

    BlackPearlQualificationPanel(state = state, enabled = enabled, onRead = onRead)
}

@Composable
private fun FiioJa11DeviceStatus(
    state: FiioJa11DeviceUiState,
    connected: Boolean,
    onRead: () -> Unit,
    onSetVolume: (Int) -> Unit,
    onSetEqProgram: (FiioJa11Protocol.EqProgram) -> Unit,
    onSetHeadsetControl: (Boolean) -> Unit,
    onSetUacMode: (FiioJa11Protocol.UacMode) -> Unit,
) {
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var pendingHeadsetValue by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var choosingEqProgram by rememberSaveable { mutableStateOf(false) }
    var choosingUacMode by rememberSaveable { mutableStateOf(false) }
    val snapshot = state.snapshot
    val controlsEnabled = connected && state.isCurrentSession && !state.isBusy && state.pendingRestartWrite == null

    Text("Overview", fontWeight = FontWeight.SemiBold)
    if (snapshot == null) {
        Text(
            text = "Read the connected JA11 to show current device settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        QualificationValue(label = "Volume", value = "${snapshot.outputVolume} / ${FiioJa11Protocol.MAX_OUTPUT_VOLUME}")
        QualificationValue(label = "EQ", value = snapshot.eqProgram.technicalLabel)
        QualificationValue(label = "Sample rate", value = snapshot.sampleRateLabel)
        Text(
            text = if (state.isCurrentSession) "Current device state" else "Last read · USB session changed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Button(
        onClick = onRead,
        enabled = connected && !state.isBusy && state.pendingRestartWrite == null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Text(if (state.isReading) "Reading…" else "Refresh device state")
    }
    state.error?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    state.pendingRestartWrite?.let {
        Text(
            text = "Waiting for the JA11 USB session to reconnect so the change can be verified. The setting will not be replayed automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    if (snapshot != null) {
        DeviceSectionTitle("Playback")
        Text("Device volume", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                onClick = { onSetVolume(snapshot.outputVolume - 1) },
                enabled = controlsEnabled && snapshot.outputVolume > FiioJa11Protocol.MIN_OUTPUT_VOLUME,
            ) { Text("−") }
            Text("${snapshot.outputVolume} / ${FiioJa11Protocol.MAX_OUTPUT_VOLUME}")
            Button(
                onClick = { onSetVolume(snapshot.outputVolume + 1) },
                enabled = controlsEnabled && snapshot.outputVolume < FiioJa11Protocol.MAX_OUTPUT_VOLUME,
            ) { Text("+") }
        }
        Text(
            text = "Volume changes one hardware step at a time so large level changes cannot happen accidentally.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DeviceSectionTitle("Microphone")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = controlsEnabled) {
                    pendingHeadsetValue = !snapshot.headsetControlEnabled
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Headset / mic remote")
                Text(
                    text = "May restart the JA11 USB session; EQ Library verifies the new session before reporting success.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = snapshot.headsetControlEnabled,
                onCheckedChange = if (controlsEnabled) {
                    { pendingHeadsetValue = it }
                } else null,
            )
        }

        DeviceSectionTitle("USB / System")
        QualificationValue(label = "Current sample rate", value = snapshot.sampleRateLabel)

        DeviceSectionTitle("Device info")
        QualificationValue(label = "Firmware", value = snapshot.firmwareVersion)
        QualificationValue(
            label = "USB identity",
            value = String.format(Locale.US, "2972:%04X", snapshot.usbProductId),
        )
        QualificationValue(label = "Validation", value = "Hardware validation pending")
        Text(
            text = "Software transaction paths are implemented from maintained protocol evidence. These JA11 controls are not claimed as physically qualified until hands-on validation is complete.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
        TextButton(
            onClick = { advancedExpanded = !advancedExpanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (advancedExpanded) "Hide Advanced" else "Show Advanced")
        }
        if (advancedExpanded) {
            Text(
                text = "Advanced settings can change the audible response or restart USB. Review the effect before applying.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text("EQ program", style = MaterialTheme.typography.labelLarge)
            Text(snapshot.eqProgram.technicalLabel)
            TextButton(
                onClick = { choosingEqProgram = true },
                enabled = controlsEnabled,
            ) { Text("Change EQ program") }

            Text("USB Audio Class", style = MaterialTheme.typography.labelLarge)
            Text(snapshot.uacMode.technicalLabel)
            Text(
                text = "Changing UAC mode re-enumerates the USB device. EQ Library waits for the replacement USB session and verifies the reported mode and PID.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = { choosingUacMode = true },
                enabled = controlsEnabled,
            ) { Text("Change UAC mode") }
        }
    }

    pendingHeadsetValue?.let { requested ->
        AlertDialog(
            onDismissRequest = { pendingHeadsetValue = null },
            title = { Text("Change headset / mic remote?") },
            text = {
                Text(
                    "The JA11 may disconnect and reconnect while this setting changes. EQ Library will verify the new USB session and will not replay the write automatically.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingHeadsetValue = null
                        onSetHeadsetControl(requested)
                    },
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { pendingHeadsetValue = null }) { Text("Cancel") }
            },
        )
    }

    if (choosingEqProgram && snapshot != null) {
        AlertDialog(
            onDismissRequest = { choosingEqProgram = false },
            title = { Text("Change EQ program") },
            text = {
                Column {
                    Text(
                        "This changes the active acoustic response immediately. User 1 is the editable five-band PEQ used by EQ Library.",
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    FiioJa11Protocol.EqProgram.entries.forEach { program ->
                        TextButton(
                            onClick = {
                                choosingEqProgram = false
                                onSetEqProgram(program)
                            },
                            enabled = program != snapshot.eqProgram,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(program.technicalLabel) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { choosingEqProgram = false }) { Text("Cancel") }
            },
        )
    }

    if (choosingUacMode && snapshot != null) {
        AlertDialog(
            onDismissRequest = { choosingUacMode = false },
            title = { Text("Change USB Audio Class") },
            text = {
                Column {
                    Text(
                        "The JA11 will re-enumerate as a different USB product. Audio will be interrupted until Android reconnects it. Choose the compatibility mode you need.",
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    FiioJa11Protocol.UacMode.entries.forEach { mode ->
                        TextButton(
                            onClick = {
                                choosingUacMode = false
                                onSetUacMode(mode)
                            },
                            enabled = mode != snapshot.uacMode,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(mode.technicalLabel) }
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

@Composable
private fun DeviceSectionTitle(title: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    Text(title, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun BlackPearlQualificationPanel(
    state: BlackPearlQualificationUiState,
    enabled: Boolean,
    onRead: () -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    Text(
        text = stringResource(R.string.my_dac_qualification_title),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
    Text(
        text = stringResource(R.string.my_dac_qualification_explanation),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Button(
        onClick = onRead,
        enabled = enabled && !state.isReading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(
                if (state.isReading) R.string.my_dac_qualification_reading
                else R.string.my_dac_qualification_action,
            ),
        )
    }

    state.error?.let { error ->
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    state.snapshot?.let { snapshot ->
        Text(
            text = stringResource(
                if (state.isCurrentSession) R.string.my_dac_qualification_current
                else R.string.my_dac_qualification_stale,
            ),
            style = MaterialTheme.typography.labelLarge,
        )
        QualificationSnapshotRows(snapshot)
    }
}

@Composable
private fun QualificationSnapshotRows(snapshot: BlackPearlDeviceQualificationSnapshot) {
    QualificationValue(
        label = stringResource(R.string.my_dac_device_firmware),
        value = snapshot.firmwareVersion,
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_filter),
        value = filterLabel(snapshot.filterCode),
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_gain_mode),
        value = gainModeLabel(snapshot.gainModeCode),
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_amp_topology),
        value = ampTopologyLabel(snapshot.ampTopologyCode),
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_mic_gain),
        value = stringResource(R.string.my_dac_qualification_db, snapshot.micGainDb.toDouble()),
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_balance),
        value = snapshot.signedBalanceDb?.let { balance ->
            if (balance == 0) stringResource(R.string.my_dac_qualification_balance_center)
            else stringResource(R.string.my_dac_qualification_balance_db, balance)
        } ?: stringResource(
            R.string.my_dac_qualification_balance_inconsistent,
            snapshot.leftBalanceDb,
            snapshot.rightBalanceDb,
        ),
    )
    QualificationValue(
        label = stringResource(R.string.my_dac_qualification_playback_gain),
        value = "${stringResource(R.string.my_dac_qualification_db, snapshot.playbackGainDb)} · " +
            stringResource(R.string.my_dac_qualification_raw, snapshot.playbackGainRaw),
    )
}

@Composable
private fun QualificationValue(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun filterLabel(code: Int): String = stringResource(
    when (code) {
        BlackPearlDeviceControlReadCodec.FILTER_FAST_LL -> R.string.my_dac_qualification_filter_fast_ll
        BlackPearlDeviceControlReadCodec.FILTER_FAST_PC -> R.string.my_dac_qualification_filter_fast_pc
        BlackPearlDeviceControlReadCodec.FILTER_SLOW_LL -> R.string.my_dac_qualification_filter_slow_ll
        BlackPearlDeviceControlReadCodec.FILTER_SLOW_PC -> R.string.my_dac_qualification_filter_slow_pc
        BlackPearlDeviceControlReadCodec.FILTER_NOS -> R.string.my_dac_qualification_filter_nos
        else -> error("Validated Black Pearl filter code unexpectedly missing.")
    },
)

@Composable
private fun gainModeLabel(code: Int): String = stringResource(
    when (code) {
        BlackPearlDeviceControlReadCodec.GAIN_MODE_LOW -> R.string.my_dac_qualification_gain_low
        BlackPearlDeviceControlReadCodec.GAIN_MODE_HIGH -> R.string.my_dac_qualification_gain_high
        else -> error("Validated Black Pearl gain mode unexpectedly missing.")
    },
)

@Composable
private fun ampTopologyLabel(code: Int): String = stringResource(
    when (code) {
        BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_H -> R.string.my_dac_qualification_amp_class_h
        BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_AB -> R.string.my_dac_qualification_amp_class_ab
        else -> error("Validated Black Pearl amp topology unexpectedly missing.")
    },
)

@Composable
private fun DeviceInfoLine(
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
