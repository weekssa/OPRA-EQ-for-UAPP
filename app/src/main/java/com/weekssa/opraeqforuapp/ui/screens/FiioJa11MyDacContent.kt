package com.weekssa.opraeqforuapp.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqResponseEvaluator
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationTrace
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiState
import com.weekssa.opraeqforuapp.ui.MyDacEditorApplyStatus
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import com.weekssa.opraeqforuapp.ui.components.DacEqResponseGraph
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun FiioJa11MyDacContent(
    connectionState: Kt02h20ConnectionState,
    hardwareEqState: HardwareEqSnapshotState,
    editorState: MyDacEditorUiState,
    deviceState: FiioJa11DeviceUiState,
    onConnect: () -> Unit,
    onReadDeviceControls: () -> Unit,
    onSetOutputVolume: (Int) -> Unit,
    onSetEqProgram: (FiioJa11Protocol.EqProgram) -> Unit,
    onSetHeadsetControl: (Boolean) -> Unit,
    onSetUacMode: (FiioJa11Protocol.UacMode) -> Unit,
    onResetEq: suspend () -> String,
    onOpenEditor: () -> Unit,
    onCloseEditor: () -> Unit,
    onSelectBand: (Int) -> Unit,
    onShowAllBands: () -> Unit,
    onShowReview: () -> Unit,
    onUpdateBand: (Int, com.weekssa.opraeqforuapp.domain.library.EqFilterType, Double, Double, Double) -> Unit,
    onUseSafeGain: () -> Unit,
    onResetEdits: () -> Unit,
    onApply: (Boolean) -> Unit,
    operationTrace: FiioJa11OperationTrace? = null,
    operationStatus: FiioJa11OperationStatus = FiioJa11OperationStatus.Idle,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var confirmReset by remember { mutableStateOf(false) }
    var dismissedEditorApplyOperationId by rememberSaveable { mutableStateOf<String?>(null) }
    val connected = connectionState is Kt02h20ConnectionState.Connected
    val editorApplyTrace = when (val status = operationStatus) {
        is FiioJa11OperationStatus.Completed -> operationTrace?.takeIf { trace ->
            trace.operation == "EDITOR_APPLY" && trace.operationId == status.trace.operationId
        }
        else -> null
    }

    LaunchedEffect(editorApplyTrace?.operationId) {
        dismissedEditorApplyOperationId = null
        val operationId = editorApplyTrace?.operationId ?: return@LaunchedEffect
        if (editorApplyTrace.isSaveAndFinalReadbackVerified()) {
            delay(FIIO_JA11_TERMINAL_SUCCESS_EXPIRY_MILLIS)
            if (dismissedEditorApplyOperationId != operationId) {
                dismissedEditorApplyOperationId = operationId
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset JA11 EQ to flat?") },
            text = {
                Text(
                    "This overwrites all five User 1 PEQ bands, sets the JA11 global EQ gain to 0 dB, selects User 1, applies, saves, and verifies the final readback.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        scope.launch { onMessage(onResetEq()) }
                    },
                ) { Text("Reset to flat") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "FiiO JA11 · ${connectionLabel(connectionState)}",
            fontWeight = FontWeight.SemiBold,
        )
        if (!connected) {
            Text(
            text = if ((connectionState as? Kt02h20ConnectionState.PermissionRequired)
                        ?.retryAvailable == false
                ) {
                    "Waiting for the Android USB permission prompt. Approve it to continue."
                } else {
                    "Open the current Android USB session to read and manage this connected DAC."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                enabled = (connectionState as? Kt02h20ConnectionState.PermissionRequired)
                    ?.retryAvailable != false,
            ) {
                Text(
                    when {
                        (connectionState as? Kt02h20ConnectionState.PermissionRequired)
                            ?.retryAvailable == false -> "Waiting for permission…"
                        connectionState is Kt02h20ConnectionState.Error ||
                            connectionState is Kt02h20ConnectionState.PermissionRequired -> "Retry connect"
                        else -> "Connect"
                    },
                )
            }
        }

        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = TabRowDefaults.primaryContainerColor,
            contentColor = TabRowDefaults.primaryContentColor,
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("EQ") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DEVICE") })
        }

        if (selectedTab == 0) {
            val operationBusy = operationStatus is FiioJa11OperationStatus.Running
            val canEdit = fiioJa11EditActionEnabled(
                connectionState = connectionState,
                hardwareEqState = hardwareEqState,
                deviceState = deviceState,
                operationStatus = operationStatus,
            )
            if (operationBusy) {
                fiioJa11OperationInProgressMessage(operationStatus)?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (editorState.isOpening || editorState.isOpen || editorState.error != null) {
                DacEqEditorScreen(
                    state = editorState,
                    onRetryOpen = onOpenEditor,
                    onClose = onCloseEditor,
                    onSelectBand = onSelectBand,
                    onShowAllBands = onShowAllBands,
                    onShowReview = onShowReview,
                    onUpdateBand = onUpdateBand,
                    onUseSafeGain = onUseSafeGain,
                    onResetEdits = onResetEdits,
                    onApply = onApply,
                    dacLabel = "FiiO JA11 · User 1",
                )
            } else {
                FiioJa11EqStatus(
                    hardwareEqState = hardwareEqState,
                    deviceState = deviceState,
                    connected = connected,
                    onEdit = onOpenEditor,
                    canEdit = canEdit,
                    onReset = { confirmReset = true },
                    operationStatus = operationStatus,
                )
                if (
                    editorApplyTrace != null &&
                    editorApplyTrace.operationId != dismissedEditorApplyOperationId
                ) {
                    FiioJa11EditorTerminalResult(
                        trace = editorApplyTrace,
                        onDismiss = { dismissedEditorApplyOperationId = editorApplyTrace.operationId },
                    )
                } else if (editorState.applyStatus != MyDacEditorApplyStatus.IDLE) {
                    Text(
                        editorState.applyFailureReason
                            ?: "FiiO JA11 Apply completed, but its final verification report is unavailable.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else {
            CapabilityDrivenDeviceStatus(
                deviceId = DacDeviceId.FIIO_JA11,
                blackPearlQualificationState = BlackPearlQualificationUiState(),
                blackPearlQualificationEnabled = false,
                onReadBlackPearlQualification = {},
                fiioJa11DeviceState = deviceState,
                fiioJa11Connected = connected,
                onReadFiioJa11DeviceControls = onReadDeviceControls,
                onSetFiioJa11OutputVolume = onSetOutputVolume,
                onSetFiioJa11EqProgram = onSetEqProgram,
                onSetFiioJa11HeadsetControl = onSetHeadsetControl,
                onSetFiioJa11UacMode = onSetUacMode,
                fiioJa11OperationBusy = operationStatus is FiioJa11OperationStatus.Running,
            )
        }
    }
}

internal const val FIIO_JA11_TERMINAL_SUCCESS_EXPIRY_MILLIS = 8_000L

@Composable
private fun FiioJa11EditorTerminalResult(
    trace: FiioJa11OperationTrace,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val presentation = fiioJa11OperationStatusPresentation(trace)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (presentation.verified) {
                        "JA11 Apply verified"
                    } else {
                        "JA11 Apply not verified"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (presentation.verified) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss JA11 Apply result",
                    )
                }
            }
            Text(presentation.message)
            Text(
                text = fiioJa11OperationReportDescription(trace),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = {
                    shareFiioJa11Report(
                        context = context,
                        subject = "FiiO JA11 operation report",
                        mimeType = "text/plain",
                        contents = trace.toReadableText(),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(FIIO_JA11_READABLE_REPORT_LABEL)
            }
            TextButton(
                onClick = {
                    shareFiioJa11Report(
                        context = context,
                        subject = "FiiO JA11 operation report JSON",
                        mimeType = "application/json",
                        contents = trace.toJson(),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(FIIO_JA11_TECHNICAL_REPORT_LABEL)
            }
        }
    }
}

private fun shareFiioJa11Report(
    context: Context,
    subject: String,
    mimeType: String,
    contents: String,
) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, contents)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share FiiO JA11 report"))
}

@Composable
private fun FiioJa11EqStatus(
    hardwareEqState: HardwareEqSnapshotState,
    deviceState: FiioJa11DeviceUiState,
    connected: Boolean,
    onEdit: () -> Unit,
    canEdit: Boolean,
    onReset: () -> Unit,
    operationStatus: FiioJa11OperationStatus,
) {
    val disabledActionButtonColors = ButtonDefaults.buttonColors(
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val bundle = hardwareEqState.bundle
    val hasCurrentReadback = connected && hardwareEqState.freshness == DacStateFreshness.CURRENT
    val program = deviceState.snapshot?.eqProgram ?: bundle?.snapshot?.activeProgram
    Text(
        text = when {
            hasCurrentReadback -> "Current hardware EQ"
            bundle != null -> "Last read hardware EQ"
            else -> "Hardware EQ"
        },
        fontWeight = FontWeight.SemiBold,
    )
    if (!connected) {
        Text(
            "Disconnected. The last verified EQ remains visible but may no longer match the DAC.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (deviceState.snapshot == null && bundle == null) {
        Text(
            if (hardwareEqState.isReading) "Reading the current JA11 program…"
            else "Refresh the connected JA11 to read its current EQ state.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    when (program) {
        FiioJa11Protocol.EqProgram.OFF -> {
            Text(
                if (hasCurrentReadback) "Flat · EQ Off" else "Last read: Flat · EQ Off",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Stored User 1 coefficients are inactive and are not presented as the current acoustic response.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FiioJa11Protocol.EqProgram.VOCAL,
        FiioJa11Protocol.EqProgram.CLASSIC,
        FiioJa11Protocol.EqProgram.BASS -> {
            Text(
                if (hasCurrentReadback) requireNotNull(program).technicalLabel
                else "Last read: ${requireNotNull(program).technicalLabel}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "This built-in program is active. Its coefficients are not exposed by the maintained JA11 protocol evidence, so EQ Library does not invent a response graph.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FiioJa11Protocol.EqProgram.USER_1 -> {
            if (bundle == null) {
                Text(
                    "User 1 is active, but a verified five-band EQ read is not available yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val snapshot = bundle.snapshot
                val curve = HardwareEqResponseEvaluator.evaluate(snapshot.filters)
                Text(
                    if (hasCurrentReadback) "User 1 · 5-band PEQ" else "Last read · User 1 · 5-band PEQ",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!hasCurrentReadback) {
                    Text(
                        "These verified values are historical and may no longer match the DAC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                snapshot.dedicatedEqPreampDb?.let { gain ->
                    Text("Global EQ gain ${"%.2f".format(gain)} dB")
                }
                if (curve != null) {
                    DacEqResponseGraph(
                        curve = curve,
                        filters = snapshot.filters,
                        accessibilityDescription = if (hasCurrentReadback) {
                            "Current FiiO JA11 User 1 hardware EQ response"
                        } else {
                            "Last read FiiO JA11 User 1 hardware EQ response; it may no longer match the device"
                        },
                    )
                }
                snapshot.filters.forEach { filter ->
                    Text(
                        "${filter.index + 1}. ${filter.type.name.replace('_', ' ')} · ${"%.0f".format(filter.frequencyHz)} Hz · ${"%+.2f".format(filter.gainDb)} dB · Q ${"%.2f".format(filter.q)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        null -> if (deviceState.snapshot == null && bundle == null) {
            Text("No verified JA11 EQ readback is available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Current JA11 EQ state is unavailable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (program == FiioJa11Protocol.EqProgram.USER_1) {
        Button(
            onClick = onEdit,
            enabled = canEdit,
            colors = disabledActionButtonColors,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Edit EQ") }
    }

    Button(
        onClick = onReset,
        enabled = connected &&
            !deviceState.isBusy &&
            deviceState.pendingRestartWrite == null &&
            fiioJa11OperationControlsEnabled(operationStatus),
        colors = disabledActionButtonColors,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Reset EQ to flat") }

    fiioJa11OperationInProgressMessage(operationStatus)?.let { message ->
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Text(
        text = "Choose or flash EQs from My EQs or EQ Library. Automatic output mode uses JA11 as the current hardware output while it is the single supported DAC attached.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

}

internal fun fiioJa11EditActionEnabled(
    connectionState: Kt02h20ConnectionState,
    hardwareEqState: HardwareEqSnapshotState,
    deviceState: FiioJa11DeviceUiState,
    operationStatus: FiioJa11OperationStatus,
): Boolean {
    val deviceSnapshot = deviceState.snapshot ?: return false
    val editableSnapshot = hardwareEqState.bundle?.snapshot ?: return false
    return connectionState is Kt02h20ConnectionState.Connected &&
        deviceState.isCurrentSession &&
        deviceSnapshot.eqProgram == FiioJa11Protocol.EqProgram.USER_1 &&
        editableSnapshot.sessionGeneration == deviceSnapshot.sessionGeneration &&
        hardwareEqState.freshness == DacStateFreshness.CURRENT &&
        !hardwareEqState.isReading &&
        !hardwareEqState.readFailed &&
        editableSnapshot.activeProgram == FiioJa11Protocol.EqProgram.USER_1 &&
        editableSnapshot.filters.size == FiioJa11Protocol.BAND_COUNT &&
        editableSnapshot.dedicatedEqPreampDb != null &&
        !deviceState.isBusy &&
        deviceState.pendingRestartWrite == null &&
        operationStatus !is FiioJa11OperationStatus.Running
}

internal fun fiioJa11OperationInProgressMessage(
    operationStatus: FiioJa11OperationStatus,
): String? = when (operationStatus) {
    is FiioJa11OperationStatus.Running -> when (operationStatus.operation.uppercase()) {
        "FLASH" -> null
        "RESET" -> "Resetting JA11 EQ… Keep the DAC connected while final readback is verified."
        "EDITOR_APPLY" -> "Applying JA11 EQ… Keep the DAC connected while final readback is verified."
        else -> "Applying JA11 ${fiioJa11OperationLabel(operationStatus.operation).lowercase()}… Keep the DAC connected while final readback is verified."
    }
    else -> null
}

private fun connectionLabel(state: Kt02h20ConnectionState): String = when (state) {
    Kt02h20ConnectionState.Connected -> "Connected"
    Kt02h20ConnectionState.Connecting -> "Connecting…"
    Kt02h20ConnectionState.Disconnected -> "Disconnected"
    is Kt02h20ConnectionState.Error -> "Connection problem"
    is Kt02h20ConnectionState.PermissionRequired ->
        if (state.retryAvailable) "USB permission required" else "Waiting for USB permission…"
}
