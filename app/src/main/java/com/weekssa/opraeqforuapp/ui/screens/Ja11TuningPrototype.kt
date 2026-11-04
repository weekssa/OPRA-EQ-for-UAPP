package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningApplyRequest
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningController
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningLevel
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningPersistence
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningSaveOutcome
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningTransport
import com.weekssa.opraeqforuapp.domain.fiio.Ja11TuningValues
import com.weekssa.opraeqforuapp.domain.fiio.SimulatedJa11TuningTransport
import com.weekssa.opraeqforuapp.ui.Ja11TuningPhase
import com.weekssa.opraeqforuapp.ui.Ja11TuningUiState
import com.weekssa.opraeqforuapp.ui.applyOutcome
import com.weekssa.opraeqforuapp.ui.ja11TuningCapabilityText
import com.weekssa.opraeqforuapp.ui.ja11TuningPersistenceText
import com.weekssa.opraeqforuapp.ui.ja11TuningRowValue
import com.weekssa.opraeqforuapp.ui.readOutcome
import kotlinx.coroutines.launch

internal const val JA11_TUNING_ROW_TAG = "ja11_tuning_row"
internal const val JA11_TUNING_DIALOG_TAG = "ja11_tuning_dialog"
internal const val JA11_TUNING_APPLY_TAG = "ja11_tuning_apply"
internal const val JA11_TUNING_SAVE_TAG = "ja11_tuning_save"
internal const val JA11_TUNING_UNPLUG_TAG = "ja11_tuning_unplug"

/**
 * Private-prototype tuning surface for My DAC -> DEVICE.
 *
 * It is labelled as simulated everywhere a value appears: the JA11 run-mode protocol has no
 * established tuning command, so this exists to evaluate the interaction and the honest state model
 * before any protocol evidence exists. It owns no USB session and sends nothing to the DAC.
 */
@Composable
internal fun Ja11TuningPrototypeSection(
    enabled: Boolean,
    transportFactory: () -> Ja11TuningTransport = { SimulatedJa11TuningTransport() },
    modifier: Modifier = Modifier,
) {
    val controller = remember { Ja11TuningController(transportFactory()) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(Ja11TuningUiState()) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(controller) {
        state = state.beginRead()
        state = state.readOutcome(controller.read())
    }

    fun applyTuning(values: Ja11TuningValues) {
        if (state.isBusy) return
        state = state.beginApply(values)
        scope.launch {
            val generation = state.sessionGeneration ?: run {
                state = state.readOutcome(controller.read())
                state.sessionGeneration
            }
            if (generation == null) return@launch
            state = state.verifying()
            state = state.applyOutcome(controller.apply(Ja11TuningApplyRequest(values, generation)))
        }
    }

    fun saveStartup() {
        if (state.isBusy) return
        scope.launch {
            state = when (val outcome = controller.saveStartupDefaults()) {
                is Ja11TuningSaveOutcome.Saved -> state.read(outcome.snapshot)
                Ja11TuningSaveOutcome.Unsupported -> state.unsupported()
                Ja11TuningSaveOutcome.PermissionDenied -> state.permissionDenied()
                is Ja11TuningSaveOutcome.Failed -> state.failed(outcome.reason, uncertain = false)
            }
        }
    }

    fun simulateUnplug() {
        if (state.isBusy) return
        scope.launch {
            state = state.readOutcome(controller.simulateReconnect())
        }
    }

    val value = ja11TuningRowValue(state)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(enabled = enabled, onClick = { dialogOpen = true })
            .semantics(mergeDescendants = true) {
                contentDescription = "Tuning, prototype, simulated. " + value
            }
            .testTag(JA11_TUNING_ROW_TAG)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Tuning", style = MaterialTheme.typography.labelLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = "Prototype · simulated. No command is sent to JA11.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

    if (dialogOpen) {
        Ja11TuningPrototypeDialog(
            state = state,
            enabled = enabled,
            onDismiss = { dialogOpen = false },
            onApply = ::applyTuning,
            onSaveToStartup = ::saveStartup,
            onSimulateUnplug = ::simulateUnplug,
        )
    }
}

@Composable
private fun Ja11TuningPrototypeDialog(
    state: Ja11TuningUiState,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onApply: (Ja11TuningValues) -> Unit,
    onSaveToStartup: () -> Unit,
    onSimulateUnplug: () -> Unit,
) {
    var stagedLevelName by rememberSaveable { mutableStateOf(state.level?.name ?: Ja11TuningLevel.STOCK.name) }
    var stagedGate by rememberSaveable { mutableStateOf(state.noiseGateEnabled ?: false) }
    val stagedLevel = Ja11TuningLevel.entries.firstOrNull { it.name == stagedLevelName } ?: Ja11TuningLevel.STOCK
    val stagedValues = Ja11TuningValues(stagedLevel, stagedGate)
    val changed = state.level != stagedValues.level || state.noiseGateEnabled != stagedValues.noiseGateEnabled
    val controlsEnabled = enabled && !state.isBusy && when (state.phase) {
        Ja11TuningPhase.PERMISSION_DENIED,
        Ja11TuningPhase.UNSUPPORTED,
        Ja11TuningPhase.DISCONNECTED,
        -> false
        else -> true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(JA11_TUNING_DIALOG_TAG),
        title = { Text("JA11 tuning (simulated)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = ja11TuningCapabilityText(state.capability),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Simulated level", style = MaterialTheme.typography.labelLarge)
                Ja11TuningLevel.entries.forEach { level ->
                    val selected = level == stagedLevel
                    TextButton(
                        onClick = { stagedLevelName = level.name },
                        enabled = controlsEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            buildString {
                                append(if (selected) "● " else "○ ")
                                append(level.label)
                                if (state.level == level) append(" · Current")
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Noise gate (simulated)", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = when (state.noiseGateEnabled) {
                                true -> "On"
                                false -> "Off"
                                null -> "Unknown"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = stagedGate,
                        enabled = controlsEnabled,
                        onCheckedChange = { stagedGate = it },
                    )
                }
                ja11TuningStatusText(state)?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.phase == Ja11TuningPhase.VERIFIED) {
                            MaterialTheme.colorScheme.primary
                        } else if (state.error != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Text(
                    text = ja11TuningPersistenceText(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.persistence == Ja11TuningPersistence.SESSION_ONLY) {
                    TextButton(
                        onClick = onSaveToStartup,
                        enabled = controlsEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(JA11_TUNING_SAVE_TAG),
                    ) { Text("Save to startup (simulated)") }
                }
                TextButton(
                    onClick = onSimulateUnplug,
                    enabled = controlsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(JA11_TUNING_UNPLUG_TAG),
                ) { Text("Simulate unplug") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(stagedValues) },
                enabled = controlsEnabled && changed,
                modifier = Modifier.testTag(JA11_TUNING_APPLY_TAG),
            ) { Text("Apply (simulated)") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

internal fun ja11TuningStatusText(state: Ja11TuningUiState): String? = when (state.phase) {
    Ja11TuningPhase.READING -> "Reading simulated tuning…"
    Ja11TuningPhase.APPLYING -> "Applying simulated tuning… Nothing is sent to JA11."
    Ja11TuningPhase.VERIFYING -> "Verifying simulated readback…"
    Ja11TuningPhase.VERIFIED -> "Simulated apply verified by readback."
    Ja11TuningPhase.FAILED, Ja11TuningPhase.DISCONNECTED, Ja11TuningPhase.PERMISSION_DENIED ->
        state.error ?: "Simulated tuning is not current."
    Ja11TuningPhase.UNSUPPORTED ->
        "Runtime tuning is not supported on this device. Nothing was written."
    Ja11TuningPhase.IDLE -> state.error
}
