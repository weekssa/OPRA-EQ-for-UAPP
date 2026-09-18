package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import kotlinx.coroutines.launch

@Composable
internal fun Ew300MyDacContent(
    connectionState: Kt02h20ConnectionState,
    onConnect: () -> Unit,
    onResetEq: suspend () -> String,
    onQualifyGlobalGain: suspend () -> String,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("SIMGOT EW300 DSP", style = MaterialTheme.typography.titleLarge)
        Text("USB 31B2:0111 · five-band PEQ", style = MaterialTheme.typography.bodyMedium)
        when (connectionState) {
            Kt02h20ConnectionState.Connected -> {
                Text("Connected. Use My EQs or EQ Library to flash a selected profile.")
                Text(
                    "Before the first library flash, run the one-time reversible global-gain qualification. " +
                        "It briefly reconnects the USB device, restores the captured state, and unlocks gain-aware EW300 flashing only after exact readback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { scope.launch { onMessage(onQualifyGlobalGain()) } }) {
                    Text("Qualify global gain")
                }
                Button(onClick = { onMessage("Use Reset to flat from My EQs to restore the EW300.") }) {
                    Text("Manage EQ from My EQs")
                }
            }
            Kt02h20ConnectionState.Connecting -> Text("Connecting to EW300…")
            Kt02h20ConnectionState.Disconnected -> {
                Text("Connect the EW300 USB cable to manage its EQ.")
                Button(onClick = onConnect) { Text("Connect") }
            }
            is Kt02h20ConnectionState.Error -> {
                Text(connectionState.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onConnect) { Text("Try again") }
            }
        }
        if (connectionState is Kt02h20ConnectionState.Connected) {
            Button(onClick = { scope.launch { onMessage(onResetEq()) } }) { Text("Reset EQ to flat") }
        }
    }
}
