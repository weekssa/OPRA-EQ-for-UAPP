package com.weekssa.opraeqforuapp.diagnostics

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme

/**
 * Debug-only, read-only enumeration for the EW300 discovery gate.
 *
 * It never opens a USB connection, requests access, or sends a control, bulk, interrupt, HID, or
 * audio command. Android exposes the fields below without device permission, so the first capture
 * cannot modify the cable's untouched state.
 */
class Ew300UsbDiscoveryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var report by mutableStateOf(readOnlyUsbReport())
            OpraEqTheme(themeMode = ThemeMode.System) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("EW300 USB discovery", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "This testing tool only lists USB information Android already exposes. " +
                            "It does not request USB access, open the cable, or change EQ.",
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { report = readOnlyUsbReport() },
                    ) { Text("Scan connected USB devices") }
                    Text(report, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    private fun readOnlyUsbReport(): String {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList.values.sortedBy(UsbDevice::getDeviceName)
        if (devices.isEmpty()) return "No USB devices are currently visible. Connect the EW300 DSP cable, then tap Scan."

        return buildString {
            appendLine("Capture type: read-only enumeration")
            appendLine("Device count: ${devices.size}")
            devices.forEachIndexed { deviceIndex, device ->
                appendLine()
                appendLine("Device ${deviceIndex + 1}")
                appendLine("VID:PID: %04X:%04X".format(device.vendorId, device.productId))
                appendLine("Class/subclass/protocol: ${device.deviceClass}/${device.deviceSubclass}/${device.deviceProtocol}")
                appendLine("Interfaces: ${device.interfaceCount}")
                repeat(device.interfaceCount) { interfaceIndex ->
                    val usbInterface = device.getInterface(interfaceIndex)
                    appendLine("  Interface $interfaceIndex: class/subclass/protocol ${usbInterface.interfaceClass}/${usbInterface.interfaceSubclass}/${usbInterface.interfaceProtocol}; endpoints ${usbInterface.endpointCount}")
                    repeat(usbInterface.endpointCount) { endpointIndex ->
                        val endpoint = usbInterface.getEndpoint(endpointIndex)
                        appendLine("    Endpoint $endpointIndex: address 0x%02X; direction %s; type %s; max packet %d; interval %d".format(
                            endpoint.address,
                            if (endpoint.direction == 0x80) "IN" else "OUT",
                            endpointTypeName(endpoint.type),
                            endpoint.maxPacketSize,
                            endpoint.interval,
                        ))
                    }
                }
            }
            appendLine()
            append("Do not grant USB access or use any EQ controls at this stage. Send a screenshot of this report.")
        }
    }

    private fun endpointTypeName(type: Int): String = when (type) {
        0 -> "control"
        1 -> "isochronous"
        2 -> "bulk"
        3 -> "interrupt"
        else -> "unknown($type)"
    }
}
