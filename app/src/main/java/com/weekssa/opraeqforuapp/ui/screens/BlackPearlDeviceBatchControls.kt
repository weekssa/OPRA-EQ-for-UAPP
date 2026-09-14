package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControlReadCodec
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControls
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationPolicy
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlUsbAudioMode
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlVolumeScale
import com.weekssa.opraeqforuapp.domain.dac.DacControlDescriptor
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValidation
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacDiscreteOption
import com.weekssa.opraeqforuapp.domain.dac.validateForWrite
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Finished Black Pearl DEVICE settings surface.
 *
 * The UI stays intentionally simple. Repository/domain code remains responsible for fresh-session
 * validation, target-only writes, bounded settling reads, complete readback verification and
 * rejection of unrelated state changes.
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

    var choosingPlayback by rememberSaveable { mutableStateOf(false) }
    var playbackText by rememberSaveable { mutableStateOf("") }

    val snapshot = state.snapshot
    val sessionControlsEnabled = enabled && state.isCurrentSession && !state.isBusy

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    state.isReading -> "Reading device settings…"
                    state.isCurrentSession -> "Current device state"
                    snapshot != null -> "Last read"
                    else -> "Device settings"
                },
                fontWeight = FontWeight.SemiBold,
            )
            if (snapshot != null && !state.isCurrentSession && !state.isReading) {
                Text(
                    text = "Reconnect or refresh to make these values current.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(
            onClick = onRead,
            enabled = enabled && !state.isBusy,
        ) {
            Text(if (state.isReading) "Reading…" else "Refresh")
        }
    }

    state.error?.let { error ->
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    state.activeWriteControlId?.takeIf { state.isWriting }?.let { controlId ->
        Text(
            text = "Applying ${blackPearlControlName(controlId)}…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    if (snapshot == null) {
        Text(
            text = if (enabled) {
                "Current settings appear automatically after the Black Pearl is read."
            } else {
                "Connect the Black Pearl to view its settings."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        return
    }

    DeviceSettingsSection("Audio")
    DeviceSettingRow(
        label = "Playback level",
        value = playbackLabel(snapshot.playbackGainRaw),
        enabled = isControlEnabled(BlackPearlDeviceControls.PLAYBACK_GAIN_DB, sessionControlsEnabled),
        onClick = {
            playbackText = formatGainDb(snapshot.playbackGainDb)
            choosingPlayback = true
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
            modifier = Modifier.padding(top = 2.dp),
        )
    }
    Text(
        text = "UAC 1.0: unplug the Black Pearl, leave headphones connected, hold + and −, reconnect USB, then release after it powers on.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    Text(
        text = "UAC 2.0: reconnect normally without holding the buttons. EQ Library detects the active mode after reconnection.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp),
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

    if (choosingPlayback) {
        val parsedDb = playbackText.trim().toDoubleOrNull()
        val requestedValue = parsedDb?.let { DacControlValue.Numeric(it) }
        val descriptor = BlackPearlDeviceControls.descriptor(BlackPearlDeviceControls.PLAYBACK_GAIN_DB)
        val validation = if (requestedValue != null) descriptor?.validateForWrite(requestedValue) else null
        val requestedRaw = if (
            validation == DacControlValidation.Valid ||
            validation is DacControlValidation.CautionOutsideNormalRange
        ) {
            parsedDb?.let(BlackPearlDeviceControls::playbackGainRaw)
        } else {
            null
        }
        val currentRaw = snapshot.playbackGainRaw
        val currentDb = snapshot.playbackGainDb
        val stepBaseDb = parsedDb ?: currentDb
        val lowerStepDb = playbackWholeDbStep(stepBaseDb, direction = -1)
        val upperStepDb = playbackWholeDbStep(stepBaseDb, direction = 1)

        AlertDialog(
            onDismissRequest = { choosingPlayback = false },
            title = { Text("Playback level") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current: ${playbackLabel(currentRaw)}")
                    OutlinedTextField(
                        value = playbackText,
                        onValueChange = { playbackText = it },
                        label = { Text("New level (dB)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = playbackText.isNotBlank() && requestedRaw == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    requestedRaw?.let { raw ->
                        Text("New: ${playbackLabel(raw)}", fontWeight = FontWeight.SemiBold)
                    }
                    if (playbackText.isNotBlank() && requestedRaw == null) {
                        Text(
                            text = "Enter a supported whole-dB value.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            onClick = { playbackText = formatGainDb(lowerStepDb) },
                            enabled = BlackPearlDeviceControls.playbackGainRaw(lowerStepDb) != null,
                        ) { Text("−1 dB") }
                        TextButton(onClick = { playbackText = formatGainDb(currentDb) }) { Text("Current") }
                        TextButton(
                            onClick = { playbackText = formatGainDb(upperStepDb) },
                            enabled = BlackPearlDeviceControls.playbackGainRaw(upperStepDb) != null,
                        ) { Text("+1 dB") }
                    }
                    Text(
                        text = "Playback level changes immediately after Apply. Keep listening volume conservative.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        choosingPlayback = false
                        parsedDb?.let { requested ->
                            onSetDeviceControl(
                                BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
                                DacControlValue.Numeric(requested),
                            )
                        }
                    },
                    enabled = requestedRaw != null && requestedRaw != currentRaw &&
                        isControlEnabled(BlackPearlDeviceControls.PLAYBACK_GAIN_DB, sessionControlsEnabled),
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { choosingPlayback = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DeviceSettingsSection(title: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 14.dp, bottom = 10.dp))
    Text(title, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun DeviceSettingRow(
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
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun ReadOnlySettingRow(label: String, value: String) {
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
    BlackPearlDeviceControls.PLAYBACK_GAIN_DB -> "Playback level"
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

private fun playbackLabel(raw: Int): String =
    "${BlackPearlVolumeScale.percentFromRaw(raw)}% · ${formatGainDb(raw / 256.0)} dB"

/**
 * Normal DEVICE playback changes deliberately use whole-dB steps. If hardware is externally left
 * between whole-dB points, the first down/up step lands on the adjacent conservative whole value.
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

private fun formatGainDb(value: Double): String {
    val formatted = String.format(Locale.US, "%.5f", value)
    return formatted.trimEnd('0').trimEnd('.').let { if ('.' in it) it else "$it.0" }
}
