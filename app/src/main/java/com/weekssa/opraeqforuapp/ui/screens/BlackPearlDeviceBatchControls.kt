package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControlReadCodec
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceControls
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationPolicy
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlVolumeScale
import com.weekssa.opraeqforuapp.domain.dac.DacControlDescriptor
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValidation
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacDiscreteOption
import com.weekssa.opraeqforuapp.domain.dac.validateForWrite
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Finished Black Pearl DEVICE surface for the consolidated v0.6 qualification batch.
 *
 * Every edit is local until Review -> Apply. Repository verification remains the authority for
 * freshness, target-only ownership, complete readback and unrelated-state preservation.
 */
@Composable
internal fun BlackPearlDeviceBatchControlPanel(
    state: BlackPearlQualificationUiState,
    enabled: Boolean,
    onRead: () -> Unit,
    onSetDeviceControl: (DacControlId, DacControlValue) -> Unit,
) {
    var choosingDiscreteControl by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDiscreteControl by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDiscreteValue by rememberSaveable { mutableStateOf<String?>(null) }

    var choosingIntegerControl by rememberSaveable { mutableStateOf<String?>(null) }
    var stagedIntegerDb by rememberSaveable { mutableStateOf(0f) }
    var pendingIntegerControl by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingIntegerDb by rememberSaveable { mutableStateOf<Int?>(null) }

    var choosingPlayback by rememberSaveable { mutableStateOf(false) }
    var playbackText by rememberSaveable { mutableStateOf("") }
    var pendingPlaybackDb by rememberSaveable { mutableStateOf<Double?>(null) }

    val snapshot = state.snapshot
    val sessionControlsEnabled = enabled && state.isCurrentSession && !state.isBusy

    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    Text(
        text = "Device controls",
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
    Text(
        text = "Choose a setting, review the exact Current → New change, then Apply once. EQ Library performs a fresh complete read before the target-only write and verifies the complete device state afterward.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "DAC filter is already hardware-qualified. Balance, microphone gain, amp topology, gain mode, and playback level are in one consolidated hardware-qualification batch. No DEVICE change sends Save to Flash or claims power-cycle persistence.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Button(
        onClick = onRead,
        enabled = enabled && !state.isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (state.isReading) "Reading…" else "Refresh device state")
    }

    state.error?.let { error ->
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    state.activeWriteControlId?.takeIf { state.isWriting }?.let { controlId ->
        Text(
            text = "Applying ${blackPearlControlName(controlId)} and verifying complete device readback…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (state.isCurrentSession) {
        state.lastVerifiedWriteControlId?.let { controlId ->
            Text(
                text = "${blackPearlControlName(controlId)} change verified by readback.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    snapshot?.let { current ->
        Text(
            text = if (state.isCurrentSession) "Current session read" else "Last read · USB session changed",
            style = MaterialTheme.typography.labelLarge,
        )
        BlackPearlBatchSnapshotRows(current)

        BatchSectionTitle("DAC / Digital")
        BatchControlRow(
            label = "DAC filter",
            value = blackPearlFilterLabel(current.filterCode),
            enabled = isControlEnabled(BlackPearlDeviceControls.DAC_FILTER, sessionControlsEnabled),
            onChange = { choosingDiscreteControl = BlackPearlDeviceControls.DAC_FILTER.value },
        )
        Text(
            text = "Hardware-qualified.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BatchSectionTitle("Output")
        BatchControlRow(
            label = "Amp topology",
            value = blackPearlAmpLabel(current.ampTopologyCode),
            enabled = isControlEnabled(BlackPearlDeviceControls.AMP_TOPOLOGY, sessionControlsEnabled),
            onChange = { choosingDiscreteControl = BlackPearlDeviceControls.AMP_TOPOLOGY.value },
        )
        BatchControlRow(
            label = "Gain mode",
            value = blackPearlGainLabel(current.gainModeCode),
            enabled = isControlEnabled(BlackPearlDeviceControls.GAIN_MODE, sessionControlsEnabled),
            onChange = { choosingDiscreteControl = BlackPearlDeviceControls.GAIN_MODE.value },
        )
        BatchControlRow(
            label = "Balance",
            value = current.signedBalanceDb?.let(::blackPearlBalanceLabel)
                ?: "Inconsistent channel balance read",
            enabled = current.signedBalanceDb != null &&
                isControlEnabled(BlackPearlDeviceControls.BALANCE_DB, sessionControlsEnabled),
            onChange = {
                current.signedBalanceDb?.let { value ->
                    stagedIntegerDb = value.toFloat()
                    choosingIntegerControl = BlackPearlDeviceControls.BALANCE_DB.value
                }
            },
        )

        BatchSectionTitle("Microphone / Input")
        BatchControlRow(
            label = "Microphone gain",
            value = blackPearlSignedDb(current.micGainDb),
            enabled = isControlEnabled(BlackPearlDeviceControls.MIC_GAIN_DB, sessionControlsEnabled),
            onChange = {
                stagedIntegerDb = current.micGainDb.toFloat()
                choosingIntegerControl = BlackPearlDeviceControls.MIC_GAIN_DB.value
            },
        )

        BatchSectionTitle("Playback")
        BatchControlRow(
            label = "Playback level",
            value = playbackLabel(current.playbackGainRaw),
            enabled = isControlEnabled(BlackPearlDeviceControls.PLAYBACK_GAIN_DB, sessionControlsEnabled),
            onChange = {
                playbackText = formatGainDb(current.playbackGainDb)
                choosingPlayback = true
            },
        )
        Text(
            text = "Level-sensitive. Playback changes are staged locally and require a separate Review before Apply. Stop playback before changing this control during qualification.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val discreteControl = choosingDiscreteControl?.let(::DacControlId)
    if (discreteControl != null && snapshot != null) {
        val descriptor = BlackPearlDeviceControls.descriptor(discreteControl) as? DacControlDescriptor.Discrete
        val currentValue = BlackPearlDeviceControls.valueFromSnapshot(discreteControl, snapshot) as? DacControlValue.Discrete
        if (descriptor != null && currentValue != null) {
            DiscreteChooserDialog(
                title = "Change ${blackPearlControlName(discreteControl)}",
                currentValueId = currentValue.valueId,
                options = descriptor.options,
                enabled = isControlEnabled(discreteControl, sessionControlsEnabled),
                onChoose = { valueId ->
                    choosingDiscreteControl = null
                    pendingDiscreteControl = discreteControl.value
                    pendingDiscreteValue = valueId
                },
                onDismiss = { choosingDiscreteControl = null },
            )
        }
    }

    val reviewDiscreteControl = pendingDiscreteControl?.let(::DacControlId)
    val reviewDiscreteValue = pendingDiscreteValue
    if (reviewDiscreteControl != null && reviewDiscreteValue != null && snapshot != null) {
        val descriptor = BlackPearlDeviceControls.descriptor(reviewDiscreteControl) as? DacControlDescriptor.Discrete
        val currentValue = BlackPearlDeviceControls.valueFromSnapshot(reviewDiscreteControl, snapshot) as? DacControlValue.Discrete
        val currentOption = descriptor?.options?.firstOrNull { it.valueId == currentValue?.valueId }
        val requestedOption = descriptor?.options?.firstOrNull { it.valueId == reviewDiscreteValue }
        if (descriptor != null && currentValue != null && currentOption != null && requestedOption != null) {
            ReviewDeviceControlDialog(
                title = "Review ${blackPearlControlName(reviewDiscreteControl)} change",
                current = currentOption.technicalLabel,
                requested = requestedOption.technicalLabel,
                warning = when (reviewDiscreteControl) {
                    BlackPearlDeviceControls.AMP_TOPOLOGY,
                    BlackPearlDeviceControls.GAIN_MODE,
                    -> "This setting can change output level or amplifier behavior. Stop playback and keep listening volume conservative before Apply."
                    BlackPearlDeviceControls.DAC_FILTER ->
                        "DAC filter is already hardware-qualified."
                    else -> null
                },
                enabled = isControlEnabled(reviewDiscreteControl, sessionControlsEnabled) &&
                    reviewDiscreteValue != currentValue.valueId,
                onApply = {
                    pendingDiscreteControl = null
                    pendingDiscreteValue = null
                    onSetDeviceControl(reviewDiscreteControl, DacControlValue.Discrete(reviewDiscreteValue))
                },
                onDismiss = {
                    pendingDiscreteControl = null
                    pendingDiscreteValue = null
                },
            )
        }
    }

    val integerControl = choosingIntegerControl?.let(::DacControlId)
    if (integerControl != null && snapshot != null) {
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
                title = { Text("Adjust ${blackPearlControlName(integerControl)}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Current: ${integerControlLabel(integerControl, currentValue)}")
                        Text("New: ${integerControlLabel(integerControl, staged)}", fontWeight = FontWeight.SemiBold)
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
                                ) { Text(if (integerControl == BlackPearlDeviceControls.BALANCE_DB) "Center" else "0 dB") }
                            }
                            TextButton(
                                onClick = { stagedIntegerDb = (staged + 1).coerceAtMost(range.last).toFloat() },
                                enabled = staged < range.last,
                            ) { Text("+1 dB") }
                        }
                        Text(
                            "The slider and step buttons change only this local draft. Review is required before any USB write.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            choosingIntegerControl = null
                            pendingIntegerControl = integerControl.value
                            pendingIntegerDb = staged
                        },
                        enabled = isControlEnabled(integerControl, sessionControlsEnabled) && staged != currentValue,
                    ) { Text("Review") }
                },
                dismissButton = {
                    TextButton(onClick = { choosingIntegerControl = null }) { Text("Cancel") }
                },
            )
        }
    }

    val reviewIntegerControl = pendingIntegerControl?.let(::DacControlId)
    val reviewIntegerDb = pendingIntegerDb
    if (reviewIntegerControl != null && reviewIntegerDb != null && snapshot != null) {
        val currentValue = (BlackPearlDeviceControls.valueFromSnapshot(reviewIntegerControl, snapshot) as? DacControlValue.Numeric)
            ?.value?.roundToInt()
        if (currentValue != null) {
            ReviewDeviceControlDialog(
                title = "Review ${blackPearlControlName(reviewIntegerControl)} change",
                current = integerControlLabel(reviewIntegerControl, currentValue),
                requested = integerControlLabel(reviewIntegerControl, reviewIntegerDb),
                warning = null,
                enabled = isControlEnabled(reviewIntegerControl, sessionControlsEnabled) && reviewIntegerDb != currentValue,
                onApply = {
                    pendingIntegerControl = null
                    pendingIntegerDb = null
                    onSetDeviceControl(reviewIntegerControl, DacControlValue.Numeric(reviewIntegerDb.toDouble()))
                },
                onDismiss = {
                    pendingIntegerControl = null
                    pendingIntegerDb = null
                },
            )
        }
    }

    if (choosingPlayback && snapshot != null) {
        val parsedDb = playbackText.trim().toDoubleOrNull()
        val requestedValue = parsedDb?.let { DacControlValue.Numeric(it) }
        val descriptor = BlackPearlDeviceControls.descriptor(BlackPearlDeviceControls.PLAYBACK_GAIN_DB)
        val validation = if (requestedValue != null) descriptor?.validateForWrite(requestedValue) else null
        val requestedRaw = if (validation == DacControlValidation.Valid || validation is DacControlValidation.CautionOutsideNormalRange) {
            parsedDb?.let(BlackPearlDeviceControls::playbackGainRaw)
        } else null
        val currentRaw = snapshot.playbackGainRaw
        val currentDb = snapshot.playbackGainDb

        AlertDialog(
            onDismissRequest = { choosingPlayback = false },
            title = { Text("Adjust playback level") },
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
                        Text(
                            text = "New: ${playbackLabel(raw)}",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (playbackText.isNotBlank() && requestedRaw == null) {
                        Text(
                            text = "Enter a value inside the verified hardware range on the native 1/256 dB grid.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            onClick = { playbackText = formatGainDb(currentDb - 0.5) },
                            enabled = BlackPearlDeviceControls.playbackGainRaw(currentDb - 0.5) != null,
                        ) { Text("−0.5 dB") }
                        TextButton(
                            onClick = { playbackText = formatGainDb(currentDb) },
                        ) { Text("Current") }
                        TextButton(
                            onClick = { playbackText = formatGainDb(currentDb + 0.5) },
                            enabled = BlackPearlDeviceControls.playbackGainRaw(currentDb + 0.5) != null,
                        ) { Text("+0.5 dB") }
                    }
                    Text(
                        "Level-sensitive: changes are local only on this screen. For hardware qualification, stop playback and test a small decrease before restoring the exact baseline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        choosingPlayback = false
                        pendingPlaybackDb = parsedDb
                    },
                    enabled = requestedRaw != null && requestedRaw != currentRaw,
                ) { Text("Review") }
            },
            dismissButton = {
                TextButton(onClick = { choosingPlayback = false }) { Text("Cancel") }
            },
        )
    }

    pendingPlaybackDb?.let { requestedDb ->
        val current = snapshot
        val requestedRaw = BlackPearlDeviceControls.playbackGainRaw(requestedDb)
        if (current != null && requestedRaw != null) {
            ReviewDeviceControlDialog(
                title = "Review playback level change",
                current = playbackLabel(current.playbackGainRaw),
                requested = playbackLabel(requestedRaw),
                warning = "Level-sensitive. Stop playback before Apply and keep downstream/listening volume conservative. During qualification, lower the level first; never use a larger upward jump as the first test.",
                enabled = isControlEnabled(BlackPearlDeviceControls.PLAYBACK_GAIN_DB, sessionControlsEnabled) &&
                    requestedRaw != current.playbackGainRaw,
                onApply = {
                    pendingPlaybackDb = null
                    onSetDeviceControl(
                        BlackPearlDeviceControls.PLAYBACK_GAIN_DB,
                        DacControlValue.Numeric(requestedDb),
                    )
                },
                onDismiss = { pendingPlaybackDb = null },
            )
        }
    }
}

@Composable
private fun BatchControlRow(
    label: String,
    value: String,
    enabled: Boolean,
    onChange: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onChange, enabled = enabled) { Text("Change") }
    }
}

@Composable
private fun BatchSectionTitle(title: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    Text(title, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun BlackPearlBatchSnapshotRows(snapshot: BlackPearlDeviceQualificationSnapshot) {
    BatchValue("Firmware", snapshot.firmwareVersion)
    BatchValue("DAC filter", blackPearlFilterLabel(snapshot.filterCode))
    BatchValue("Gain mode", blackPearlGainLabel(snapshot.gainModeCode))
    BatchValue("Amp topology", blackPearlAmpLabel(snapshot.ampTopologyCode))
    BatchValue("Microphone gain", blackPearlSignedDb(snapshot.micGainDb))
    BatchValue(
        "Balance",
        snapshot.signedBalanceDb?.let(::blackPearlBalanceLabel) ?: "Inconsistent channel balance read",
    )
    BatchValue("Playback level", playbackLabel(snapshot.playbackGainRaw))
}

@Composable
private fun BatchValue(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DiscreteChooserDialog(
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
                Text(
                    "Selecting a value only stages it. Review is required before any USB write.",
                    modifier = Modifier.padding(bottom = 8.dp),
                )
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

@Composable
private fun ReviewDeviceControlDialog(
    title: String,
    current: String,
    requested: String,
    warning: String?,
    enabled: Boolean,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current: $current")
                Text("New: $requested", fontWeight = FontWeight.SemiBold)
                warning?.let { Text(it) }
                Text(
                    "Apply performs a fresh complete device read, writes only this control, then reads the complete Black Pearl state again. Stale sessions, readback mismatch, or unrelated state changes fail verification.",
                )
                Text(
                    "This DEVICE transaction does not send Save to Flash or claim power-cycle persistence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onApply, enabled = enabled) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun isControlEnabled(controlId: DacControlId, sessionEnabled: Boolean): Boolean =
    sessionEnabled && BlackPearlDeviceQualificationPolicy.isWriteInteractive(controlId)

private fun blackPearlControlName(controlId: DacControlId): String = when (controlId) {
    BlackPearlDeviceControls.DAC_FILTER -> "DAC filter"
    BlackPearlDeviceControls.BALANCE_DB -> "balance"
    BlackPearlDeviceControls.MIC_GAIN_DB -> "microphone gain"
    BlackPearlDeviceControls.AMP_TOPOLOGY -> "amp topology"
    BlackPearlDeviceControls.GAIN_MODE -> "gain mode"
    BlackPearlDeviceControls.PLAYBACK_GAIN_DB -> "playback level"
    else -> "device control"
}

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

private fun playbackLabel(raw: Int): String =
    "${BlackPearlVolumeScale.percentFromRaw(raw)}% · ${formatGainDb(raw / 256.0)} dB · raw $raw"

private fun formatGainDb(value: Double): String {
    val formatted = String.format(Locale.US, "%.5f", value)
    return formatted.trimEnd('0').trimEnd('.').let { if ('.' in it) it else "$it.0" }
}
