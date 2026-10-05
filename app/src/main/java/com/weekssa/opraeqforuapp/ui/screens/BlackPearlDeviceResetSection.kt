package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceDefaults
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceDefaultStep
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqResponseEvaluator
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.dac.isAcousticallyActive
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.components.DacEqResponseGraph
import com.weekssa.opraeqforuapp.ui.components.PremiumSectionLabel
import com.weekssa.opraeqforuapp.ui.components.PremiumValueRow
import kotlinx.coroutines.launch

internal enum class BlackPearlRestoreStepVerification {
    WAITING,
    SATISFIED,
    MISMATCH,
}

/**
 * Decide whether an already-issued restore step is ready to advance.
 *
 * Compose can re-run the reset effect immediately after local issued-step state changes, before the
 * ViewModel's beginWrite state has propagated back through the combined UI StateFlow. The restore
 * also writes playback twice (safety floor, then final 50%), so control identity alone is not enough.
 * A step is judged only after the ViewModel's monotonically increasing write generation proves that
 * a new write cycle started after this step was issued and that exact cycle completed verification.
 */
internal fun blackPearlRestoreStepVerification(
    isBusy: Boolean,
    writeGeneration: Long,
    issuedFromWriteGeneration: Long?,
    lastVerifiedWriteControlId: DacControlId?,
    expectedControlId: DacControlId,
    requestedValueSatisfied: Boolean,
): BlackPearlRestoreStepVerification = when {
    issuedFromWriteGeneration == null -> BlackPearlRestoreStepVerification.WAITING
    writeGeneration <= issuedFromWriteGeneration -> BlackPearlRestoreStepVerification.WAITING
    isBusy -> BlackPearlRestoreStepVerification.WAITING
    writeGeneration != issuedFromWriteGeneration + 1L -> BlackPearlRestoreStepVerification.MISMATCH
    lastVerifiedWriteControlId != expectedControlId -> BlackPearlRestoreStepVerification.MISMATCH
    requestedValueSatisfied -> BlackPearlRestoreStepVerification.SATISFIED
    else -> BlackPearlRestoreStepVerification.MISMATCH
}

/**
 * Black Pearl reset surface for the project-owner selected EQ Library defaults.
 *
 * The sequence intentionally reuses the already-qualified individual DEVICE write path. Each step
 * therefore performs its own fresh complete read, target application when needed, persistence, and
 * verified readback. A failure stops the remaining DEVICE sequence and is never presented as success.
 *
 * The optional EQ action reuses the separately qualified Reset EQ to flat transaction. It is not
 * folded into or reimplemented by this DEVICE reset.
 */
@Composable
internal fun BlackPearlDeviceResetSection(
    state: BlackPearlQualificationUiState,
    hardwareEqState: HardwareEqSnapshotState,
    enabled: Boolean,
    onSetDeviceControl: (DacControlId, DacControlValue) -> Unit,
    onResetEqToFlat: suspend () -> String,
    onReadCurrentEq: () -> Unit,
    onRefreshDevice: () -> Unit,
    onMessage: (String) -> Unit,
    onOperationStatus: (String, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var includeEqReset by rememberSaveable { mutableStateOf(false) }
    var eqResetRunning by rememberSaveable { mutableStateOf(false) }
    var eqResetStartedInComposition by remember { mutableStateOf(false) }
    var eqResetResult by rememberSaveable { mutableStateOf<String?>(null) }
    var eqSnapshotRequested by rememberSaveable { mutableStateOf(false) }
    var resetStepIndex by rememberSaveable { mutableIntStateOf(IDLE_STEP) }
    var issuedStepIndex by rememberSaveable { mutableIntStateOf(IDLE_STEP) }
    var issuedFromWriteGeneration by rememberSaveable { mutableStateOf<Long?>(null) }
    var restoreSessionGeneration by rememberSaveable { mutableStateOf<Long?>(null) }

    val resetInProgress = eqResetRunning || resetStepIndex >= 0
    val canStartReset = enabled &&
        state.snapshot != null &&
        state.isCurrentSession &&
        !state.isBusy &&
        state.error == null &&
        !resetInProgress
    val eqResetRecoveryRequired = eqResetRunning && !eqResetStartedInComposition

    LaunchedEffect(eqResetRecoveryRequired) {
        if (eqResetRecoveryRequired) {
            onOperationStatus("Black Pearl reset status needs review.", false)
        }
    }

    LaunchedEffect(
        resetStepIndex,
        issuedStepIndex,
        issuedFromWriteGeneration,
        eqResetRunning,
        state.isBusy,
        state.activeWriteControlId,
        state.lastVerifiedWriteControlId,
        state.writeGeneration,
        state.snapshot,
        state.isCurrentSession,
        state.error,
    ) {
        if (eqResetRunning || resetStepIndex < 0 || state.isBusy) return@LaunchedEffect

        val snapshot = state.snapshot
        if (state.error != null || snapshot == null || !state.isCurrentSession ||
            snapshot.sessionGeneration != restoreSessionGeneration
        ) {
            val safetyNote = if (issuedStepIndex >= 0) {
                " Volume may remain at 0%. Refresh DEVICE, then adjust Volume when ready. No setting was automatically retried."
            } else {
                ""
            }
            resetStepIndex = IDLE_STEP
            issuedStepIndex = IDLE_STEP
            issuedFromWriteGeneration = null
            onMessage(
                (state.error?.let { "Restore stopped: $it" }
                    ?: "Restore stopped because the current Black Pearl state could not be verified.") + safetyNote,
            )
            return@LaunchedEffect
        }

        if (resetStepIndex >= BlackPearlDeviceDefaults.restoreSteps.size) {
            val eqResult = eqResetResult
            resetStepIndex = IDLE_STEP
            issuedStepIndex = IDLE_STEP
            issuedFromWriteGeneration = null
            eqResetResult = null
            if (!BlackPearlDeviceDefaults.finalTargets.all { (control, value) ->
                    BlackPearlDeviceDefaults.isStepSatisfied(
                        BlackPearlDeviceDefaultStep(control, value),
                        snapshot,
                    )
                }
            ) {
                onMessage("Restore stopped because the final device state did not match all defaults. Refresh DEVICE to review current values.")
                return@LaunchedEffect
            }
            onMessage(
                buildString {
                    append("Black Pearl defaults restored: 50% volume, FAST-LL, HIGH gain, CLASS AB, centered balance, 0 dB microphone gain.")
                    if (eqResult != null) append(" EQ reset: $eqResult")
                },
            )
            return@LaunchedEffect
        }

        val step = BlackPearlDeviceDefaults.restoreSteps[resetStepIndex]
        if (issuedStepIndex != resetStepIndex) {
            // Route every restore target through the normal verified DEVICE path once. If a target is
            // already current, the repository may resolve it as a verified no-op rather than writing it.
            issuedFromWriteGeneration = state.writeGeneration
            issuedStepIndex = resetStepIndex
            onSetDeviceControl(step.controlId, step.requestedValue)
            return@LaunchedEffect
        }

        when (
            blackPearlRestoreStepVerification(
                isBusy = state.isBusy,
                writeGeneration = state.writeGeneration,
                issuedFromWriteGeneration = issuedFromWriteGeneration,
                lastVerifiedWriteControlId = state.lastVerifiedWriteControlId,
                expectedControlId = step.controlId,
                requestedValueSatisfied = BlackPearlDeviceDefaults.isStepSatisfied(step, snapshot),
            )
        ) {
            BlackPearlRestoreStepVerification.WAITING -> return@LaunchedEffect
            BlackPearlRestoreStepVerification.SATISFIED -> {
                issuedStepIndex = IDLE_STEP
                issuedFromWriteGeneration = null
                resetStepIndex += 1
            }
            BlackPearlRestoreStepVerification.MISMATCH -> {
                resetStepIndex = IDLE_STEP
                issuedStepIndex = IDLE_STEP
                issuedFromWriteGeneration = null
                onMessage(
                    "Restore stopped because the verified Black Pearl setting did not match the requested value. " +
                    "Volume may remain at 0%. Refresh DEVICE, then adjust Volume when ready. No setting was automatically retried.",
                )
            }
        }
    }

    if (eqResetRecoveryRequired) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Reset outcome needs review",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "The reset may have changed EQ or DEVICE settings. Its final result is unknown, and it will not be repeated automatically. Read the current EQ and DEVICE values before using the remaining device controls. Restore defaults stays unavailable while this saved recovery state is active.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = {
                        eqSnapshotRequested = true
                        onReadCurrentEq()
                    },
                    enabled = enabled && !hardwareEqState.isReading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Read current EQ")
                }
                if (eqSnapshotRequested) BlackPearlRecoveryEqReadout(hardwareEqState)
                OutlinedButton(
                    onClick = onRefreshDevice,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Refresh DEVICE")
                }
            }
        }
    }

    PremiumSectionLabel(
        text = "Reset device",
        modifier = Modifier.padding(top = 16.dp),
    )
    PremiumValueRow(
        title = "Restore defaults",
        supportingText = "Restore EQ Library's Black Pearl defaults. Saved EQs are not affected.",
        enabled = canStartReset,
        showDisclosure = canStartReset,
        onClick = if (canStartReset) {
            {
                includeEqReset = false
                dialogOpen = true
            }
        } else {
            null
        },
    )
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text("Restore Black Pearl defaults?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This restores 50% volume, FAST-LL DAC filter, HIGH gain, CLASS AB amplifier, centered balance, and 0 dB microphone gain. Listening volume may change.",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { includeEqReset = !includeEqReset },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = includeEqReset,
                            onCheckedChange = { includeEqReset = it },
                        )
                        Text("Also reset EQ to flat")
                    }
                    Text(
                        "Your saved EQs in EQ Library are not affected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogOpen = false
                        eqResetResult = null
                        eqSnapshotRequested = false
                        onOperationStatus(
                            if (includeEqReset) "Resetting EQ to flat…" else "Restoring device defaults…",
                            true,
                        )
                        restoreSessionGeneration = state.snapshot?.sessionGeneration
                        issuedStepIndex = IDLE_STEP
                        issuedFromWriteGeneration = null
                        if (includeEqReset) {
                            eqResetStartedInComposition = true
                            eqResetRunning = true
                            scope.launch {
                                eqResetResult = onResetEqToFlat()
                                eqResetRunning = false
                                resetStepIndex = 0
                            }
                        } else {
                            resetStepIndex = 0
                        }
                    },
                    enabled = canStartReset,
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun BlackPearlRecoveryEqReadout(state: HardwareEqSnapshotState) {
    if (state.isReading) {
        Text(
            text = stringResource(R.string.my_dac_reading_eq),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val bundle = state.bundle
    if (bundle == null) {
        Text(
            text = stringResource(
                if (state.readFailed) R.string.my_dac_eq_read_failed else R.string.my_dac_no_verified_eq,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.readFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    PremiumSectionLabel(
        text = stringResource(
            if (state.freshness == DacStateFreshness.CURRENT && !state.isReading) {
                R.string.my_dac_current_hardware
            } else {
                R.string.my_dac_last_read
            },
        ),
        divider = false,
    )
    if (state.readFailed) {
        Text(
            text = stringResource(R.string.my_dac_eq_read_failed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    val filters = bundle.snapshot.filters
    val responseCurve = remember(filters) { HardwareEqResponseEvaluator.evaluate(filters) }
    if (responseCurve == null) {
        Text(
            text = stringResource(R.string.my_dac_response_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val activeBandCount = filters.count { filter -> filter.isAcousticallyActive() }
        DacEqResponseGraph(
            curve = responseCurve,
            filters = filters,
            accessibilityDescription = stringResource(
                R.string.my_dac_response_graph_accessibility,
                activeBandCount,
                responseCurve.minimumGainDb,
                responseCurve.maximumGainDb,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Text(
        text = stringResource(R.string.my_dac_filter_count, filters.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val IDLE_STEP = -1
