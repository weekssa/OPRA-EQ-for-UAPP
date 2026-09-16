package com.weekssa.opraeqforuapp.diagnostics

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
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
 * The first scan never opens a USB connection. The separate descriptor capture asks Android for
 * permission, opens the cable only to read its descriptors, and sends no EQ, bulk, interrupt, or
 * vendor-defined command.
 */
class Ew300UsbDiscoveryActivity : ComponentActivity() {
    private val usbManager by lazy { getSystemService(USB_SERVICE) as UsbManager }
    private val report = mutableStateOf("Tap Scan connected USB devices to begin.")
    private var descriptorReceiverRegistered = false

    private val descriptorPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_CAPTURE_PERMISSION) return
            val device = intent.getParcelableExtraCompat<UsbDevice>(UsbManager.EXTRA_DEVICE)
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            report.value = when {
                device == null -> "Android did not identify a device for the descriptor capture."
                !granted -> "USB permission was not granted. No connection was opened and no cable state changed."
                else -> readDescriptors(device)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            ContextCompat.registerReceiver(
                this,
                descriptorPermissionReceiver,
                IntentFilter(ACTION_CAPTURE_PERMISSION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            descriptorReceiverRegistered = true
        }.onFailure {
            report.value = "USB descriptor capture is unavailable on this Android setup. " +
                "The ordinary read-only scan remains available and does not open the cable."
        }
        enableEdgeToEdge()
        setContent {
            val reportText by report
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
                        "Scan lists USB information Android already exposes. Descriptor capture " +
                            "asks Android for access only to read standard USB descriptors; it does not change EQ.",
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { report.value = readOnlyUsbReport() },
                    ) { Text("Scan connected USB devices") }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = ::requestDescriptorCapture,
                    ) { Text("Request read-only descriptor capture") }
                    Text(reportText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    override fun onDestroy() {
        if (descriptorReceiverRegistered) unregisterReceiver(descriptorPermissionReceiver)
        super.onDestroy()
    }

    private fun requestDescriptorCapture() {
        if (!descriptorReceiverRegistered) {
            report.value = "USB descriptor capture could not be prepared on this Android setup. " +
                "No connection was opened and no cable state changed."
            return
        }
        val device = usbManager.deviceList.values.singleOrNull {
            it.vendorId == EW300_VENDOR_ID && it.productId == EW300_PRODUCT_ID
        }
        if (device == null) {
            report.value = "EW300 DSP cable (31B2:0111) is not currently visible. Connect it, then try again."
            return
        }
        if (usbManager.hasPermission(device)) {
            report.value = readDescriptors(device)
            return
        }
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_CAPTURE_PERMISSION).setPackage(packageName),
            PendingIntent.FLAG_MUTABLE,
        )
        report.value = "Waiting for Android's USB permission prompt. Approve it only to capture standard descriptors; no EQ command will be sent."
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun readOnlyUsbReport(): String {
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
            append("For the next capture, use Request read-only descriptor capture and approve Android's USB prompt. Do not use EQ controls.")
        }
    }

    private fun readDescriptors(device: UsbDevice): String {
        val connection = usbManager.openDevice(device)
            ?: return "Android granted permission but could not open the cable. No command was sent."
        return try {
            buildString {
                appendLine("Capture type: read-only standard USB descriptors")
                appendLine("VID:PID: %04X:%04X".format(device.vendorId, device.productId))
                appendLine("Manufacturer: ${device.manufacturerName ?: "unavailable"}")
                appendLine("Product: ${device.productName ?: "unavailable"}")
                appendLine("Serial: ${device.serialNumber ?: "unavailable"}")
                appendLine("Raw descriptors (${connection.rawDescriptors.size} bytes): ${connection.rawDescriptors.toHex()}")
                repeat(device.interfaceCount) { interfaceIndex ->
                    val usbInterface = device.getInterface(interfaceIndex)
                    if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_HID) {
                        val descriptor = ByteArray(HID_REPORT_DESCRIPTOR_MAX_BYTES)
                        val count = connection.controlTransfer(
                            UsbConstants.USB_DIR_IN or
                                UsbConstants.USB_TYPE_STANDARD or
                                USB_RECIP_INTERFACE,
                            USB_REQUEST_GET_DESCRIPTOR,
                            USB_DESCRIPTOR_TYPE_REPORT shl 8,
                            usbInterface.id,
                            descriptor,
                            descriptor.size,
                            DESCRIPTOR_READ_TIMEOUT_MS,
                        )
                        appendLine("HID interface ${usbInterface.id} standard report descriptor read: $count bytes")
                        if (count > 0) appendLine(descriptor.copyOf(count).toHex())
                    }
                }
                append("Only standard descriptor reads were used. No HID report, EQ, save, reset, or vendor-defined command was sent.")
            }
        } finally {
            connection.close()
        }
    }

    private fun endpointTypeName(type: Int): String = when (type) {
        0 -> "control"
        1 -> "isochronous"
        2 -> "bulk"
        3 -> "interrupt"
        else -> "unknown($type)"
    }

    @Suppress("DEPRECATION")
    private inline fun <reified T> Intent.getParcelableExtraCompat(key: String): T? =
        getParcelableExtra(key) as? T

    private fun ByteArray.toHex(): String = joinToString(separator = " ") { "%02X".format(it) }

    private companion object {
        const val ACTION_CAPTURE_PERMISSION = "com.weekssa.opraeqforuapp.diagnostics.USB_CAPTURE_PERMISSION"
        const val EW300_VENDOR_ID = 0x31B2
        const val EW300_PRODUCT_ID = 0x0111
        const val HID_REPORT_DESCRIPTOR_MAX_BYTES = 4096
        const val DESCRIPTOR_READ_TIMEOUT_MS = 1000
        const val USB_REQUEST_GET_DESCRIPTOR = 0x06
        const val USB_DESCRIPTOR_TYPE_REPORT = 0x22
        const val USB_RECIP_INTERFACE = 0x01
    }
}
