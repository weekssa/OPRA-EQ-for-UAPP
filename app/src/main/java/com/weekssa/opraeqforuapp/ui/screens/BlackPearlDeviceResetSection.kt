package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceDefaults
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
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
 * ViewModel's beginWrite state has propagated back through the combined UI StateFlow. Treating that
 * stale pre-write snapshot as a failed readback can falsely abort the restore after the safety-volume
 * step. A restore step is therefore never judged until the ViewModel reports that this exact control
 * completed verification.
 */
internal fun blackPearlRestoreStepVerification(
    isBusy: Boolean,
    lastVerifiedWriteControlId: DacControlId?,
    expectedControlId: DacControlId,
    requestedValueSatisfied: Boolean,
): BlackPearlRestoreStepVerification = when {
    isBusy -> BlackPearlRestoreStepVerification.WAITING
    lastVerifiedWriteControlId != expectedControlId -> BlackPearlRestoreStepVerification.WAITING
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
    enabled: Boolean,
    onSetDeviceControl: (DacControlId, DacControlValue) -> Unit,
    onResetEqToFlat: suspend () -> String,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var includeEqReset by rememberSaveable { mutableStateOf(false) }
    var eqResetRunning by rememberSaveable { mutableStateOf(false) }
    var eqResetResult by rememberSaveable { mutableStateOf<String?>(null) }
    var resetStepIndex by rememberSaveable { mutableIntStateOf(IDLE_STEP) }
    var issuedStepIndex by rememberSaveable { mutableIntStateOf(IDLE_STEP) }

    val resetInProgress = eqResetRunning || resetStepIndex >= 0
    val canStartReset = enabled &&
        state.snapshot != null &&
        state.isCurrentSession &&
        !state.isBusy &&
        state.error == null &&
        !resetInProgress

    LaunchedEffect(
        resetStepIndex,
        issuedStepIndex,
        eqResetRunning,
        state.isBusy,
        state.activeWriteControlId,
        state.lastVerifiedWriteControlId,
        state.snapshot,
        state.isCurrentSession,
        state.error,
    ) {
        if (eqResetRunning || resetStepIndex < 0 || state.isBusy) return@LaunchedEffect

        val snapshot = state.snapshot
        if (state.error != null || snapshot == null || !state.isCurrentSession) {
            val safetyNote = if (resetStepIndex > 0) {
                " Volume may remain at the 0% safety level; refresh the device state before retrying."
            } else {
                ""
            }
            resetStepIndex = IDLE_STEP
            issuedStepIndex = IDLE_STEP
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
            eqResetResult = null
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
            issuedStepIndex = resetStepIndex
            onSetDeviceControl(step.controlId, step.requestedValue)
            return@LaunchedEffect
        }

        when (
            blackPearlRestoreStepVerification(
                isBusy = state.isBusy,
                lastVerifiedWriteControlId = state.lastVerifiedWriteControlId,
                expectedControlId = step.controlId,
                requestedValueSatisfied = BlackPearlDeviceDefaults.isStepSatisfied(step, snapshot),
            )
        ) {
            BlackPearlRestoreStepVerification.WAITING -> return@LaunchedEffect
            BlackPearlRestoreStepVerification.SATISFIED -> {
                issuedStepIndex = IDLE_STEP
                resetStepIndex += 1
            }
            BlackPearlRestoreStepVerification.MISMATCH -> {
                resetStepIndex = IDLE_STEP
                issuedStepIndex = IDLE_STEP
                onMessage(
                    "Restore stopped because the verified Black Pearl setting did not match the requested value. " +
                        "Volume may remain at the 0% safety level; refresh the device state before retrying.",
                )
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
    if (resetInProgress) {
        Text(
            text = if (eqResetRunning) "Resetting EQ to flat…" else "Restoring device defaults…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }

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
                        issuedStepIndex = IDLE_STEP
                        if (includeEqReset) {
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

private const val IDLE_STEP = -1
