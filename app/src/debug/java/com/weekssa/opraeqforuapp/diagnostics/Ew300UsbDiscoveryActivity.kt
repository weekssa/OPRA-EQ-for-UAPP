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
 * Debug-only evidence capture for the EW300 discovery gate.
 *
 * The first scan never opens a USB connection. The separate descriptor capture asks Android for
 * permission, opens the cable only to read its descriptors and the input reports declared by that
 * descriptor. It can also listen briefly on the HID interrupt-IN endpoint after the descriptor
 * has established that endpoint. A separately gated provisional snapshot reproduces only the
 * public web tool's KT Micro READ framing; it never sends WRITE, COMMIT, CLEAR, EQ, save, or reset.
 * Android may require the app to detach its HID driver briefly; only the HID interface is claimed
 * and it is always released before the connection is closed.
 */
class Ew300UsbDiscoveryActivity : ComponentActivity() {
    private val usbManager by lazy { getSystemService(USB_SERVICE) as UsbManager }
    private val report = mutableStateOf("Tap Scan connected USB devices to begin.")
    private var provisionalSnapshotReady by mutableStateOf(false)
    private var descriptorReceiverRegistered = false

    private val descriptorPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_CAPTURE_PERMISSION &&
                intent.action != ACTION_PROVISIONAL_SNAPSHOT_PERMISSION
            ) return
            val device = intent.getParcelableExtraCompat<UsbDevice>(UsbManager.EXTRA_DEVICE)
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            report.value = when {
                device == null -> "Android did not identify a device for the descriptor capture."
                !granted -> "USB permission was not granted. No connection was opened and no cable state changed."
                intent.action == ACTION_CAPTURE_PERMISSION -> readDescriptors(device)
                else -> readProvisionalSnapshot(device)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            ContextCompat.registerReceiver(
                this,
                descriptorPermissionReceiver,
                IntentFilter().apply {
                    addAction(ACTION_CAPTURE_PERMISSION)
                    addAction(ACTION_PROVISIONAL_SNAPSHOT_PERMISSION)
                },
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
                            "asks Android for access only to read USB descriptions, their declared " +
                            "input reports, and any unsolicited HID input briefly available. It sends no " +
                            "output report. It may " +
                            "briefly detach Android's media-button driver; it does not change EQ. " +
                            "Reconnect the cable after capture.",
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { report.value = readOnlyUsbReport() },
                    ) { Text("Scan connected USB devices") }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = ::requestDescriptorCapture,
                    ) { Text("Request read-only descriptor capture") }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = provisionalSnapshotReady,
                        onClick = ::requestProvisionalSnapshot,
                    ) { Text("Capture provisional stock-EQ snapshot") }
                    Text(
                        if (provisionalSnapshotReady) {
                            "The snapshot button sends only bounded READ requests reproduced from " +
                                "the public 31B2 web-tool fallback. The mapping is provisional; no " +
                                "WRITE, COMMIT, CLEAR, save, reset, or firmware command is present."
                        } else {
                            "Complete the descriptor capture first. The provisional snapshot remains " +
                                "locked unless the exact previously captured EW300 HID descriptor matches."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
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
        report.value = "Waiting for Android's USB permission prompt. Approve it only to capture standard descriptors and passive input; no EQ command will be sent."
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun requestProvisionalSnapshot() {
        if (!provisionalSnapshotReady) {
            report.value = "The provisional snapshot is locked until this app confirms the exact EW300 HID descriptor."
            return
        }
        requestPermissionOrRun(
            action = ACTION_PROVISIONAL_SNAPSHOT_PERMISSION,
            requestCode = 1,
            waitingMessage = "Waiting for Android's USB permission prompt. This provisional capture sends " +
                "only KT Micro READ requests; it sends no write, save, reset, clear, commit, or firmware command.",
            onGranted = ::readProvisionalSnapshot,
        )
    }

    private fun requestPermissionOrRun(
        action: String,
        requestCode: Int,
        waitingMessage: String,
        onGranted: (UsbDevice) -> String,
    ) {
        if (!descriptorReceiverRegistered) {
            report.value = "USB capture could not be prepared on this Android setup. No connection was opened."
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
            report.value = onGranted(device)
            return
        }
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            Intent(action).setPackage(packageName),
            PendingIntent.FLAG_MUTABLE,
        )
        report.value = waitingMessage
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
                val rawDescriptors = connection.rawDescriptors
                appendLine("Raw descriptors (${rawDescriptors.size} bytes): ${rawDescriptors.toHex()}")
                repeat(device.interfaceCount) { interfaceIndex ->
                    val usbInterface = device.getInterface(interfaceIndex)
                    if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_HID) {
                        val declaredLength = UsbDescriptorParser.hidReportDescriptorLength(
                            rawDescriptors = rawDescriptors,
                            interfaceNumber = usbInterface.id,
                        )
                        appendLine("HID interface ${usbInterface.id} declared report descriptor length: ${declaredLength ?: "unavailable"}")
                        if (declaredLength == null || declaredLength <= 0) {
                            appendLine("HID report descriptor was not read because its standard declared length was unavailable.")
                            return@repeat
                        }

                        val nonForcedClaim = connection.claimInterface(usbInterface, false)
                        appendLine("HID interface ${usbInterface.id} non-forced claim: $nonForcedClaim")
                        val claimed = nonForcedClaim || connection.claimInterface(usbInterface, true)
                        if (!nonForcedClaim) {
                            appendLine("HID interface ${usbInterface.id} isolated forced claim: $claimed")
                        }
                        if (!claimed) {
                            appendLine("HID report descriptor was not read because Android did not grant the isolated interface claim.")
                            return@repeat
                        }
                        try {
                            val descriptor = ByteArray(declaredLength)
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
                            if (count > 0) {
                                val exactDescriptor = descriptor.copyOf(count)
                                appendLine(exactDescriptor.toHex())
                                val exactEw300Descriptor = exactDescriptor.contentEquals(EXPECTED_EW300_HID_DESCRIPTOR)
                                appendLine("Exact previously captured EW300 HID descriptor match: $exactEw300Descriptor")
                                provisionalSnapshotReady = exactEw300Descriptor
                                val vendorInputs = HidReportDescriptorParser.vendorInputReports(exactDescriptor)
                                if (vendorInputs.isEmpty()) {
                                    appendLine("No complete vendor input report declaration was found; no input report read was attempted.")
                                }
                                vendorInputs.forEach { inputReport ->
                                    val input = ByteArray(inputReport.payloadBytes + 1)
                                    val inputCount = connection.controlTransfer(
                                        UsbConstants.USB_DIR_IN or
                                            UsbConstants.USB_TYPE_CLASS or
                                            USB_RECIP_INTERFACE,
                                        HID_REQUEST_GET_REPORT,
                                        (HID_REPORT_TYPE_INPUT shl 8) or inputReport.reportId,
                                        usbInterface.id,
                                        input,
                                        input.size,
                                        INPUT_REPORT_READ_TIMEOUT_MS,
                                    )
                                    appendLine(
                                        "HID input report 0x%02X read-only GET_REPORT: %d bytes".format(
                                            inputReport.reportId,
                                            inputCount,
                                        ),
                                    )
                                    if (inputCount > 0) appendLine(input.copyOf(inputCount).toHex())
                                }
                                val interruptIn = (0 until usbInterface.endpointCount)
                                    .map(usbInterface::getEndpoint)
                                    .singleOrNull { endpoint ->
                                        endpoint.direction == UsbConstants.USB_DIR_IN &&
                                            endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT
                                    }
                                if (interruptIn == null) {
                                    appendLine("No unique HID interrupt-IN endpoint was declared; no passive endpoint read was attempted.")
                                } else {
                                    repeat(PASSIVE_INTERRUPT_READ_ATTEMPTS) { attempt ->
                                        val input = ByteArray(interruptIn.maxPacketSize)
                                        val inputCount = connection.bulkTransfer(
                                            interruptIn,
                                            input,
                                            0,
                                            input.size,
                                            PASSIVE_INTERRUPT_READ_TIMEOUT_MS,
                                        )
                                        appendLine(
                                            "HID interrupt-IN 0x%02X passive read %d/%d: %d bytes".format(
                                                interruptIn.address,
                                                attempt + 1,
                                                PASSIVE_INTERRUPT_READ_ATTEMPTS,
                                                inputCount,
                                            ),
                                        )
                                        if (inputCount > 0) appendLine(input.copyOf(inputCount).toHex())
                                    }
                                }
                            }
                        } finally {
                            appendLine("HID interface ${usbInterface.id} released: ${connection.releaseInterface(usbInterface)}")
                        }
                    }
                }
                append("Only descriptor, declared input-report, and passive interrupt-IN reads were used. No output report, EQ, save, reset, or vendor-defined command was sent. Reconnect the cable after capture so Android can resume normal ownership.")
            }
        } finally {
            connection.close()
        }
    }

    private fun readProvisionalSnapshot(device: UsbDevice): String {
        if (device.vendorId != EW300_VENDOR_ID || device.productId != EW300_PRODUCT_ID) {
            return "The provisional snapshot stopped because the exact VID:PID did not match. No command was sent."
        }
        if (device.productName != EW300_PRODUCT_NAME || device.manufacturerName != EW300_MANUFACTURER_NAME) {
            return "The provisional snapshot stopped because the exact manufacturer/product strings did not match. " +
                "No command was sent."
        }
        val hidInterface = (0 until device.interfaceCount)
            .map(device::getInterface)
            .singleOrNull { usbInterface ->
                usbInterface.id == EW300_HID_INTERFACE_ID &&
                    usbInterface.interfaceClass == UsbConstants.USB_CLASS_HID
            }
            ?: return "The provisional snapshot stopped because exact HID interface 3 was unavailable. No command was sent."
        val interruptIn = (0 until hidInterface.endpointCount)
            .map(hidInterface::getEndpoint)
            .singleOrNull { endpoint ->
                endpoint.address == EW300_INTERRUPT_IN_ADDRESS &&
                    endpoint.direction == UsbConstants.USB_DIR_IN &&
                    endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT
            }
            ?: return "The provisional snapshot stopped because exact interrupt-IN 0x82 was unavailable. No command was sent."
        val interruptOut = (0 until hidInterface.endpointCount)
            .map(hidInterface::getEndpoint)
            .singleOrNull { endpoint ->
                endpoint.address == EW300_INTERRUPT_OUT_ADDRESS &&
                    endpoint.direction == UsbConstants.USB_DIR_OUT &&
                    endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT
            }
            ?: return "The provisional snapshot stopped because exact interrupt-OUT 0x02 was unavailable. No command was sent."
        val connection = usbManager.openDevice(device)
            ?: return "Android could not open the exact cable. No command was sent."
        return try {
            val claimed = connection.claimInterface(hidInterface, true)
            if (!claimed) {
                "Android did not grant the isolated HID-interface claim. No command was sent."
            } else {
                try {
                    buildString {
                        appendLine("Capture type: provisional bounded KT Micro READ snapshot")
                        appendLine("VID:PID: %04X:%04X".format(device.vendorId, device.productId))
                        appendLine("Manufacturer: ${device.manufacturerName}")
                        appendLine("Product: ${device.productName}")
                        appendLine("Source basis: public web-tool 31B2 fallback; not vendor-verified")
                        appendLine("Transport: HID report 0x4B over interrupt-OUT 0x02; responses on interrupt-IN 0x82")
                        var stopped = false
                        Ew300ProvisionalProtocol.snapshotRegisters().forEachIndexed { index, register ->
                            if (stopped) return@forEachIndexed
                            val slotHint = if (register == Ew300ProvisionalProtocol.CURRENT_SLOT_REGISTER) 3 else 0
                            val payload = Ew300ProvisionalProtocol.readPayload(register, slotHint)
                            val wireReport = Ew300ProvisionalProtocol.wireReport(payload)
                            appendLine()
                            appendLine("READ ${index + 1}/${Ew300ProvisionalProtocol.snapshotRegisters().size} register 0x%02X".format(register))
                            appendLine("OUT: ${wireReport.toHex()}")
                            val sent = connection.bulkTransfer(
                                interruptOut,
                                wireReport,
                                0,
                                wireReport.size,
                                PROVISIONAL_TRANSFER_TIMEOUT_MS,
                            )
                            appendLine("OUT result: $sent bytes")
                            if (sent != wireReport.size) {
                                appendLine("STOP: complete READ report was not accepted; no further request was sent.")
                                stopped = true
                                return@forEachIndexed
                            }
                            val incoming = ByteArray(interruptIn.maxPacketSize)
                            val received = connection.bulkTransfer(
                                interruptIn,
                                incoming,
                                0,
                                incoming.size,
                                PROVISIONAL_TRANSFER_TIMEOUT_MS,
                            )
                            appendLine("IN result: $received bytes")
                            if (received > 0) appendLine("IN: ${incoming.copyOf(received).toHex()}")
                            val response = if (received > 0) {
                                Ew300ProvisionalProtocol.responsePayload(incoming.copyOf(received), register)
                            } else {
                                null
                            }
                            if (response == null) {
                                appendLine("STOP: response did not exactly echo report 0x4B, register, and READ 0x52.")
                                stopped = true
                            } else {
                                appendLine("Decoded provisionally: ${Ew300ProvisionalProtocol.describe(register, response)}")
                            }
                        }
                        appendLine()
                        appendLine("HID interface ${hidInterface.id} release follows this report.")
                        append(
                            if (stopped) {
                                "Snapshot stopped at the first missing or unexpected response. "
                            } else {
                                "Provisional raw snapshot completed. "
                            },
                        )
                        append("Only command byte READ 0x52 was sent. No WRITE 0x57, COMMIT 0x53, CLEAR 0x43, save, reset, or firmware command was sent. Reconnect the cable now.")
                    }
                } finally {
                    connection.releaseInterface(hidInterface)
                }
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
        const val ACTION_PROVISIONAL_SNAPSHOT_PERMISSION =
            "com.weekssa.opraeqforuapp.diagnostics.USB_PROVISIONAL_SNAPSHOT_PERMISSION"
        const val EW300_VENDOR_ID = 0x31B2
        const val EW300_PRODUCT_ID = 0x0111
        const val EW300_MANUFACTURER_NAME = "LE XIAN"
        const val EW300_PRODUCT_NAME = "SIMGOT EW300 DSP"
        const val EW300_HID_INTERFACE_ID = 3
        const val EW300_INTERRUPT_IN_ADDRESS = 0x82
        const val EW300_INTERRUPT_OUT_ADDRESS = 0x02
        const val DESCRIPTOR_READ_TIMEOUT_MS = 1000
        const val INPUT_REPORT_READ_TIMEOUT_MS = 1000
        const val PASSIVE_INTERRUPT_READ_TIMEOUT_MS = 250
        const val PASSIVE_INTERRUPT_READ_ATTEMPTS = 3
        const val PROVISIONAL_TRANSFER_TIMEOUT_MS = 1000
        const val USB_REQUEST_GET_DESCRIPTOR = 0x06
        const val USB_DESCRIPTOR_TYPE_REPORT = 0x22
        const val HID_REQUEST_GET_REPORT = 0x01
        const val HID_REPORT_TYPE_INPUT = 0x01
        const val USB_RECIP_INTERFACE = 0x01
        val EXPECTED_EW300_HID_DESCRIPTOR = byteArrayOf(
            0x05, 0x0C, 0x09, 0x01, 0xA1.toByte(), 0x01, 0x85.toByte(), 0x01,
            0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x02,
            0x09, 0xE9.toByte(), 0x09, 0xEA.toByte(), 0x81.toByte(), 0x02,
            0x95.toByte(), 0x04, 0x09, 0xCD.toByte(), 0x09, 0xCE.toByte(),
            0x09, 0xB6.toByte(), 0x09, 0xB5.toByte(), 0x81.toByte(), 0x02,
            0x95.toByte(), 0x02, 0x81.toByte(), 0x01, 0x06, 0x01, 0xFF.toByte(),
            0x85.toByte(), 0x4B, 0x75, 0x08, 0x95.toByte(), 0x0A, 0x09, 0x01,
            0x81.toByte(), 0x03, 0x95.toByte(), 0x0A, 0x09, 0x02, 0x91.toByte(),
            0x02, 0x85.toByte(), 0x54, 0x75, 0x08, 0x95.toByte(), 0x0A,
            0x09, 0x03, 0x81.toByte(), 0x03, 0x95.toByte(), 0x0A, 0x09, 0x04,
            0x91.toByte(), 0x02, 0xC0.toByte(),
        )
    }
}
