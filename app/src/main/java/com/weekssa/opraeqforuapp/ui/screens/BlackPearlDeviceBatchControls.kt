package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControlReadCodec
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControls
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationPolicy
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlUsbAudioMode
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlVolumeScale
import com.weekssa.opraeqforuapp.domain.dac.DacControlDescriptor
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacDiscreteOption
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.components.PremiumSectionLabel
import com.weekssa.opraeqforuapp.ui.components.PremiumValueRow
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Finished Black Pearl DEVICE settings surface.
 *
 * User-facing volume is intentionally percentage-first because that is the Black Pearl's ordinary
 * controller presentation. The repository/domain transaction still uses the exact native raw/dB
 * protocol value and the physically qualified whole-dB DEVICE step internally.
 */
@Composable
internal fun BlackPearlDeviceBatchControlPanel(
    state: BlackPearlQualificationUiState,
    enabled: Boolean,
    onRead: () -> Unit,
    onSetDeviceControl: (DacControlId, DacControlValue) -> Unit,
) {
    var choosingDiscreteControl by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingLevelSensitiveControl by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingLevelSensitiveValue by rememberSaveable { mutableStateOf<String?>(null) }

    var choosingIntegerControl by rememberSaveable { mutableStateOf<String?>(null) }
    var stagedIntegerDb by rememberSaveable { mutableStateOf(0f) }

    var choosingVolume by rememberSaveable { mutableStateOf(false) }
    var stagedPlaybackDb by rememberSaveable { mutableStateOf<Double?>(null) }

    val snapshot = state.snapshot
    val sessionControlsEnabled = enabled && state.isCurrentSession && !state.isBusy

    DeviceOperationStatusHeader(
        isReading = state.isReading,
        isWriting = state.isWriting,
        activeWriteControlId = state.activeWriteControlId,
        lastVerifiedWriteControlId = state.lastVerifiedWriteControlId,
        hasSnapshot = snapshot != null,
        isCurrentSession = state.isCurrentSession,
        error = state.error,
        enabled = enabled,
        busy = state.isBusy,
        onRefresh = onRead,
        controlName = ::blackPearlControlName,
    )

    if (snapshot == null) {
        Text(
            text = if (enabled) {
                "Current settings appear automatically after the Black Pearl is read."
            } else {
                "Connect the Black Pearl to view its settings."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    DeviceSettingsSection("Audio")
    DeviceSettingRow(
        label = "Volume",
        value = blackPearlVolumeLabel(snapshot.playbackGainRaw),
        enabled = isControlEnabled(BlackPearlDeviceControls.PLAYBACK_GAIN_DB, sessionControlsEnabled),
        onClick = {
            stagedPlaybackDb = snapshot.playbackGainDb
            choosingVolume = true
        },
    )
    DeviceSettingRow(
        label = "DAC filter",
        value = blackPearlFilterLabel(snapshot.filterCode),
        enabled = isControlEnabled(BlackPearlDeviceControls.DAC_FILTER, sessionControlsEnabled),
        onClick = { choosingDiscreteControl = BlackPearlDeviceControls.DAC_FILTER.value },
    )
    DeviceSettingRow(
        label = "Gain",
        value = blackPearlGainLabel(snapshot.gainModeCode),
        enabled = isControlEnabled(BlackPearlDeviceControls.GAIN_MODE, sessionControlsEnabled),
        onClick = { choosingDiscreteControl = BlackPearlDeviceControls.GAIN_MODE.value },
    )
    DeviceSettingRow(
        label = "Amplifier",
        value = blackPearlAmpLabel(snapshot.ampTopologyCode),
        enabled = isControlEnabled(BlackPearlDeviceControls.AMP_TOPOLOGY, sessionControlsEnabled),
        onClick = { choosingDiscreteControl = BlackPearlDeviceControls.AMP_TOPOLOGY.value },
    )
    DeviceSettingRow(
        label = "Balance",
        value = snapshot.signedBalanceDb?.let(::blackPearlBalanceLabel)
            ?: "Inconsistent channel read",
        enabled = snapshot.signedBalanceDb != null &&
            isControlEnabled(BlackPearlDeviceControls.BALANCE_DB, sessionControlsEnabled),
        onClick = {
            snapshot.signedBalanceDb?.let { value ->
                stagedIntegerDb = value.toFloat()
                choosingIntegerControl = BlackPearlDeviceControls.BALANCE_DB.value
            }
        },
    )

    DeviceSettingsSection("Microphone")
    DeviceSettingRow(
        label = "Microphone gain",
        value = blackPearlSignedDb(snapshot.micGainDb),
        enabled = isControlEnabled(BlackPearlDeviceControls.MIC_GAIN_DB, sessionControlsEnabled),
        onClick = {
            stagedIntegerDb = snapshot.micGainDb.toFloat()
            choosingIntegerControl = BlackPearlDeviceControls.MIC_GAIN_DB.value
        },
    )

    DeviceSettingsSection("USB & System")
    ReadOnlySettingRow(
        label = "USB audio mode",
        value = blackPearlUsbAudioModeLabel(snapshot.usbAudioMode),
    )
    if (snapshot.usbAudioMode == null) {
        Text(
            text = "This USB session did not report a mode EQ Library could verify.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        text = "UAC 1.0: unplug the Black Pearl, leave headphones connected, hold + and −, reconnect USB, then release after it powers on.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "UAC 2.0: reconnect normally without holding the buttons. EQ Library detects the active mode after reconnection.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val discreteControl = choosingDiscreteControl?.let(::DacControlId)
    if (discreteControl != null) {
        val descriptor = BlackPearlDeviceControls.descriptor(discreteControl) as? DacControlDescriptor.Discrete
        val currentValue = BlackPearlDeviceControls.valueFromSnapshot(discreteControl, snapshot) as? DacControlValue.Discrete
        if (descriptor != null && currentValue != null) {
            DiscreteSettingDialog(
                title = blackPearlControlTitle(discreteControl),
                currentValueId = currentValue.valueId,
                options = descriptor.options,
                enabled = isControlEnabled(discreteControl, sessionControlsEnabled),
                onChoose = { valueId ->
                    choosingDiscreteControl = null
                    if (isLevelSensitiveDiscrete(discreteControl)) {
                        pendingLevelSensitiveControl = discreteControl.value
                        pendingLevelSensitiveValue = valueId
                    } else {
                        onSetDeviceControl(discreteControl, DacControlValue.Discrete(valueId))
                    }
                },
                onDismiss = { choosingDiscreteControl = null },
            )
        }
    }

    val levelSensitiveControl = pendingLevelSensitiveControl?.let(::DacControlId)
    val levelSensitiveValue = pendingLevelSensitiveValue
    if (levelSensitiveControl != null && levelSensitiveValue != null) {
        val descriptor = BlackPearlDeviceControls.descriptor(levelSensitiveControl) as? DacControlDescriptor.Discrete
        val current = BlackPearlDeviceControls.valueFromSnapshot(levelSensitiveControl, snapshot) as? DacControlValue.Discrete
        val currentLabel = descriptor?.options?.firstOrNull { it.valueId == current?.valueId }?.technicalLabel
        val requestedLabel = descriptor?.options?.firstOrNull { it.valueId == levelSensitiveValue }?.technicalLabel
        if (descriptor != null && current != null && currentLabel != null && requestedLabel != null) {
            AlertDialog(
                onDismissRequest = {
                    pendingLevelSensitiveControl = null
                    pendingLevelSensitiveValue = null
                },
                title = { Text("Change ${blackPearlControlName(levelSensitiveControl)}?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$currentLabel → $requestedLabel", fontWeight = FontWeight.SemiBold)
                        Text("This may change listening volume. Keep the level conservative.")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingLevelSensitiveControl = null
                            pendingLevelSensitiveValue = null
                            onSetDeviceControl(
                                levelSensitiveControl,
                                DacControlValue.Discrete(levelSensitiveValue),
                            )
                        },
                        enabled = isControlEnabled(levelSensitiveControl, sessionControlsEnabled) &&
                            current.valueId != levelSensitiveValue,
                    ) { Text("Change") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingLevelSensitiveControl = null
                            pendingLevelSensitiveValue = null
                        },
                    ) { Text("Cancel") }
                },
            )
        }
    }

    val integerControl = choosingIntegerControl?.let(::DacControlId)
    if (integerControl != null) {
        val currentValue = (BlackPearlDeviceControls.valueFromSnapshot(integerControl, snapshot) as? DacControlValue.Numeric)
            ?.value?.roundToInt()
        val range = when (integerControl) {
            BlackPearlDeviceControls.BALANCE_DB ->
                BlackPearlDeviceControlReadCodec.BALANCE_MIN_DB..BlackPearlDeviceControlReadCodec.BALANCE_MAX_DB
            BlackPearlDeviceControls.MIC_GAIN_DB ->
                BlackPearlDeviceControlReadCodec.MIC_GAIN_MIN_DB..BlackPearlDeviceControlReadCodec.MIC_GAIN_MAX_DB
            else -> null
        }
        if (currentValue != null && range != null) {
            val staged = stagedIntegerDb.roundToInt().coerceIn(range.first, range.last)
            AlertDialog(
                onDismissRequest = { choosingIntegerControl = null },
                title = { Text(blackPearlControlTitle(integerControl)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(integerControlLabel(integerControl, staged), fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = stagedIntegerDb,
                            onValueChange = { value -> stagedIntegerDb = value.roundToInt().toFloat() },
                            valueRange = range.first.toFloat()..range.last.toFloat(),
                            steps = range.last - range.first - 1,
                            enabled = isControlEnabled(integerControl, sessionControlsEnabled),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = { stagedIntegerDb = (staged - 1).coerceAtLeast(range.first).toFloat() },
                                enabled = staged > range.first,
                            ) { Text("−1 dB") }
                            if (0 in range) {
                                TextButton(
                                    onClick = { stagedIntegerDb = 0f },
                                    enabled = staged != 0,
                                ) {
                                    Text(if (integerControl == BlackPearlDeviceControls.BALANCE_DB) "Center" else "0 dB")
                                }
                            }
                            TextButton(
                                onClick = { stagedIntegerDb = (staged + 1).coerceAtMost(range.last).toFloat() },
                                enabled = staged < range.last,
                            ) { Text("+1 dB") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            choosingIntegerControl = null
                            onSetDeviceControl(integerControl, DacControlValue.Numeric(staged.toDouble()))
                        },
                        enabled = isControlEnabled(integerControl, sessionControlsEnabled) && staged != currentValue,
                    ) { Text("Apply") }
                },
                dismissButton = {
                    TextButton(onClick = { choosingIntegerControl = null }) { Text("Cancel") }
                },
            )
        }
    }

    if (choosingVolume) {
        val currentRaw = snapshot.playbackGainRaw
        val currentDb = snapshot.playbackGainDb
        val stagedDb = stagedPlaybackDb ?: currentDb
        val stagedRaw = BlackPearlDeviceControls.playbackGainRaw(stagedDb)
        val lowerStepDb = playbackWholeDbStep(stagedDb, direction = -1)
        val upperStepDb = playbackWholeDbStep(stagedDb, direction = 1)

        AlertDialog(
            onDismissRequest = { choosingVolume = false },
            title = { Text("Volume") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current: ${blackPearlVolumeLabel(currentRaw)}")
                    Text(
                        text = stagedRaw?.let(::blackPearlVolumeLabel) ?: blackPearlVolumeLabel(currentRaw),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { stagedPlaybackDb = lowerStepDb },
                            enabled = BlackPearlDeviceControls.playbackGainRaw(lowerStepDb) != null,
                        ) { Text("−") }
                        TextButton(
                            onClick = { stagedPlaybackDb = currentDb },
                            enabled = stagedRaw != currentRaw,
                        ) { Text("Current") }
                        TextButton(
                            onClick = { stagedPlaybackDb = upperStepDb },
                            enabled = BlackPearlDeviceControls.playbackGainRaw(upperStepDb) != null,
                        ) { Text("+") }
                    }
                    Text(
                        text = "The new volume becomes the current volume after EQ Library verifies it on the DAC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        choosingVolume = false
                        stagedPlaybackDb?.let { requested ->
                            onSetDeviceControl(
                                BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
                                DacControlValue.Numeric(requested),
                            )
                        }
                    },
                    enabled = stagedRaw != null && stagedRaw != currentRaw &&
                        isControlEnabled(BlackPearlDeviceControls.PLAYBACK_GAIN_DB, sessionControlsEnabled),
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { choosingVolume = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DeviceSettingsSection(title: String) {
    PremiumSectionLabel(
        text = title,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun DeviceSettingRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PremiumValueRow(
        title = label,
        value = value,
        enabled = enabled,
        showDisclosure = enabled,
        onClick = if (enabled) onClick else null,
    )
}

@Composable
private fun ReadOnlySettingRow(label: String, value: String) {
    PremiumValueRow(
        title = label,
        value = value,
    )
}

@Composable
private fun DiscreteSettingDialog(
    title: String,
    currentValueId: String,
    options: List<DacDiscreteOption>,
    enabled: Boolean,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    val current = option.valueId == currentValueId
                    TextButton(
                        onClick = { onChoose(option.valueId) },
                        enabled = enabled && !current,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (current) "${option.technicalLabel} · Current" else option.technicalLabel)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun isControlEnabled(controlId: DacControlId, sessionEnabled: Boolean): Boolean =
    sessionEnabled && BlackPearlDeviceQualificationPolicy.isWriteInteractive(controlId)

private fun isLevelSensitiveDiscrete(controlId: DacControlId): Boolean =
    controlId == BlackPearlDeviceControls.AMP_TOPOLOGY ||
        controlId == BlackPearlDeviceControls.GAIN_MODE

private fun blackPearlControlTitle(controlId: DacControlId): String = when (controlId) {
    BlackPearlDeviceControls.DAC_FILTER -> "DAC filter"
    BlackPearlDeviceControls.BALANCE_DB -> "Balance"
    BlackPearlDeviceControls.MIC_GAIN_DB -> "Microphone gain"
    BlackPearlDeviceControls.AMP_TOPOLOGY -> "Amplifier"
    BlackPearlDeviceControls.GAIN_MODE -> "Gain"
    BlackPearlDeviceControls.PLAYBACK_GAIN_DB -> "Volume"
    BlackPearlDeviceControls.USB_AUDIO_MODE -> "USB audio mode"
    else -> "Device setting"
}

private fun blackPearlControlName(controlId: DacControlId): String =
    blackPearlControlTitle(controlId).lowercase()

private fun integerControlLabel(controlId: DacControlId, value: Int): String =
    if (controlId == BlackPearlDeviceControls.BALANCE_DB) blackPearlBalanceLabel(value)
    else blackPearlSignedDb(value)

private fun blackPearlBalanceLabel(balanceDb: Int): String = when {
    balanceDb == 0 -> "Centered"
    balanceDb > 0 -> "+$balanceDb dB (right attenuated)"
    else -> "$balanceDb dB (left attenuated)"
}

private fun blackPearlSignedDb(value: Int): String = when {
    value > 0 -> "+$value dB"
    else -> "$value dB"
}

private fun blackPearlFilterLabel(code: Int): String = when (code) {
    BlackPearlDeviceControlReadCodec.FILTER_FAST_LL -> "FAST-LL"
    BlackPearlDeviceControlReadCodec.FILTER_FAST_PC -> "Fast-PC"
    BlackPearlDeviceControlReadCodec.FILTER_SLOW_LL -> "Slow-LL"
    BlackPearlDeviceControlReadCodec.FILTER_SLOW_PC -> "SLOW-PC"
    BlackPearlDeviceControlReadCodec.FILTER_NOS -> "NOS"
    else -> "Unknown ($code)"
}

private fun blackPearlGainLabel(code: Int): String = when (code) {
    BlackPearlDeviceControlReadCodec.GAIN_MODE_LOW -> "LOW"
    BlackPearlDeviceControlReadCodec.GAIN_MODE_HIGH -> "HIGH"
    else -> "Unknown ($code)"
}

private fun blackPearlAmpLabel(code: Int): String = when (code) {
    BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_H -> "CLASS H"
    BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_AB -> "CLASS AB"
    else -> "Unknown ($code)"
}

private fun blackPearlUsbAudioModeLabel(mode: BlackPearlUsbAudioMode?): String = when (mode) {
    BlackPearlUsbAudioMode.UAC_1_0 -> "UAC 1.0"
    BlackPearlUsbAudioMode.UAC_2_0 -> "UAC 2.0"
    null -> "Unknown"
}

private fun blackPearlVolumeLabel(raw: Int): String =
    "${BlackPearlVolumeScale.percentFromRaw(raw)}%"

/**
 * Normal DEVICE volume changes deliberately retain the physically observed conservative whole-dB
 * protocol step underneath the percentage UI. If hardware is externally left between whole-dB
 * points, the first down/up step lands on the adjacent conservative whole value.
 */
private fun playbackWholeDbStep(value: Double, direction: Int): Double {
    require(direction == -1 || direction == 1)
    val nearestWhole = round(value)
    val isWhole = kotlin.math.abs(value - nearestWhole) < 1e-9
    return when {
        direction < 0 && isWhole -> nearestWhole - 1.0
        direction < 0 -> floor(value)
        direction > 0 && isWhole -> nearestWhole + 1.0
        else -> ceil(value)
    }
}
