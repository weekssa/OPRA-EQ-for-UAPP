package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqResponseEvaluator
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiState
import com.weekssa.opraeqforuapp.ui.components.DacEqResponseGraph
import kotlinx.coroutines.launch

@Composable
internal fun FiioJa11MyDacContent(
    connectionState: Kt02h20ConnectionState,
    hardwareEqState: HardwareEqSnapshotState,
    deviceState: FiioJa11DeviceUiState,
    onConnect: () -> Unit,
    onReadDeviceControls: () -> Unit,
    onSetOutputVolume: (Int) -> Unit,
    onSetEqProgram: (FiioJa11Protocol.EqProgram) -> Unit,
    onSetHeadsetControl: (Boolean) -> Unit,
    onSetUacMode: (FiioJa11Protocol.UacMode) -> Unit,
    onResetEq: suspend () -> String,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var confirmReset by remember { mutableStateOf(false) }
    val connected = connectionState is Kt02h20ConnectionState.Connected

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
                text = "Open the current Android USB session to read and manage this connected DAC.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (
                        connectionState is Kt02h20ConnectionState.Error ||
                        connectionState is Kt02h20ConnectionState.PermissionRequired
                    ) "Retry connect" else "Connect",
                )
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("EQ") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DEVICE") })
        }

        if (selectedTab == 0) {
            FiioJa11EqStatus(
                hardwareEqState = hardwareEqState,
                deviceState = deviceState,
                connected = connected,
                onReset = { confirmReset = true },
            )
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
            )
        }
    }
}

@Composable
private fun FiioJa11EqStatus(
    hardwareEqState: HardwareEqSnapshotState,
    deviceState: FiioJa11DeviceUiState,
    connected: Boolean,
    onReset: () -> Unit,
) {
    val program = deviceState.snapshot?.eqProgram
    Text("Current hardware EQ", fontWeight = FontWeight.SemiBold)
    when {
        !connected -> Text(
            "Reconnect the JA11 to refresh its current EQ state.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        deviceState.snapshot == null -> Text(
            "Reading the current JA11 program…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        program == FiioJa11Protocol.EqProgram.OFF -> {
            Text("Flat · EQ Off", style = MaterialTheme.typography.titleMedium)
            Text(
                "Stored User 1 coefficients are inactive and are not presented as the current acoustic response.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        program == FiioJa11Protocol.EqProgram.VOCAL ||
            program == FiioJa11Protocol.EqProgram.CLASSIC ||
            program == FiioJa11Protocol.EqProgram.BASS -> {
            Text(program.technicalLabel, style = MaterialTheme.typography.titleMedium)
            Text(
                "This built-in program is active. Its coefficients are not exposed by the maintained JA11 protocol evidence, so EQ Library does not invent a response graph.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        program == FiioJa11Protocol.EqProgram.USER_1 -> {
            val bundle = hardwareEqState.bundle
            if (bundle == null || hardwareEqState.freshness != DacStateFreshness.CURRENT) {
                Text(
                    "User 1 is active, but a current verified five-band read is not available yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val snapshot = bundle.snapshot
                val curve = HardwareEqResponseEvaluator.evaluate(snapshot.filters)
                Text("User 1 · 5-band PEQ", style = MaterialTheme.typography.titleMedium)
                snapshot.dedicatedEqPreampDb?.let { gain ->
                    Text("Global EQ gain ${"%.2f".format(gain)} dB")
                }
                if (curve != null) {
                    DacEqResponseGraph(
                        curve = curve,
                        filters = snapshot.filters,
                        accessibilityDescription = "Current FiiO JA11 User 1 hardware EQ response",
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
        else -> Text(
            "Current JA11 EQ state is unavailable.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Button(
        onClick = onReset,
        enabled = connected && !deviceState.isBusy && deviceState.pendingRestartWrite == null,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Reset EQ to flat") }

    Text(
        text = "Choose or flash EQs from My EQs or EQ Library. Automatic output mode uses JA11 as the current hardware output while it is the single supported DAC attached.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun connectionLabel(state: Kt02h20ConnectionState): String = when (state) {
    Kt02h20ConnectionState.Connected -> "Connected"
    Kt02h20ConnectionState.Connecting -> "Connecting…"
    Kt02h20ConnectionState.Disconnected -> "Disconnected"
    is Kt02h20ConnectionState.Error -> "Connection problem"
    is Kt02h20ConnectionState.PermissionRequired -> "USB permission required"
}
