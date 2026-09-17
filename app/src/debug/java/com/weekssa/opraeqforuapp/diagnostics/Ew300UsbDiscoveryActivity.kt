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
import android.os.SystemClock
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
 * public web tool's KT Micro READ framing. After an exact stock snapshot, a separately approved
 * diagnostic can make a bounded batch of tiny temporary EQ-field writes. Each is read back and
 * restored before the next begins; it never sends COMMIT, CLEAR, save, reset, or firmware commands.
 * The separate persistence qualification is a further explicit action: it sends one provisional
 * COMMIT only after the complete volatile checks pass, then requires two physical reconnects.
 * Android may require the app to detach its HID driver briefly; only the HID interface is claimed
 * and it is always released before the connection is closed.
 */
class Ew300UsbDiscoveryActivity : ComponentActivity() {
    private val usbManager by lazy { getSystemService(USB_SERVICE) as UsbManager }
    private val report = mutableStateOf("Tap Scan connected USB devices to begin.")
    private var provisionalSnapshotReady by mutableStateOf(false)
    private var reversibleProbeReady by mutableStateOf(false)
    private var filterTypeProbeReady by mutableStateOf(false)
    /** 0=ready, 1=marker saved/reconnect pending, 2=restore saved/reconnect pending, 3=done. */
    private var persistencePhase by mutableStateOf(0)
    private var persistenceBaseline: Map<Int, ByteArray>? = null
    private var persistenceMarker: Map<Int, ByteArray>? = null
    private var descriptorReceiverRegistered = false

    private val descriptorPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_CAPTURE_PERMISSION &&
                intent.action != ACTION_PROVISIONAL_SNAPSHOT_PERMISSION &&
                intent.action != ACTION_REVERSIBLE_WRITE_PERMISSION &&
                intent.action != ACTION_FILTER_TYPE_PERMISSION &&
                intent.action != ACTION_PERSISTENCE_PERMISSION
            ) return
            val device = intent.getParcelableExtraCompat<UsbDevice>(UsbManager.EXTRA_DEVICE)
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            report.value = when {
                device == null -> "Android did not identify a device for the descriptor capture."
                !granted -> "USB permission was not granted. No connection was opened and no cable state changed."
                intent.action == ACTION_CAPTURE_PERMISSION -> readDescriptors(device)
                intent.action == ACTION_PROVISIONAL_SNAPSHOT_PERMISSION -> readProvisionalSnapshot(device)
                intent.action == ACTION_REVERSIBLE_WRITE_PERMISSION -> runReversibleWriteTest(device)
                intent.action == ACTION_FILTER_TYPE_PERMISSION -> runFilterTypeTest(device)
                else -> runPersistenceQualification(device)
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
                    addAction(ACTION_REVERSIBLE_WRITE_PERMISSION)
                    addAction(ACTION_FILTER_TYPE_PERMISSION)
                    addAction(ACTION_PERSISTENCE_PERMISSION)
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
                        "Diagnostic build: ${Ew300ProvisionalProtocol.DIAGNOSTIC_BUILD}",
                        style = MaterialTheme.typography.bodySmall,
                    )
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
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = reversibleProbeReady,
                        onClick = ::requestReversibleWriteTest,
                    ) { Text("Run approved remaining EQ-field batch") }
                    Text(
                        if (reversibleProbeReady) {
                            "This approved batch first rechecks the complete stock snapshot. It tests " +
                                "small temporary remaining frequency and Q field changes one at a time, " +
                                "reads each back, restores it immediately, and stops on the first problem. " +
                                "It sends no commit, save, clear, reset, or firmware command."
                        } else {
                            "The reversible test remains locked until this installation reads the " +
                                "complete preserved stock snapshot exactly."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = filterTypeProbeReady,
                        onClick = ::requestFilterTypeTest,
                    ) { Text("Run approved filter-type test") }
                    Text(
                        if (filterTypeProbeReady) {
                            "This approved test tries Band 1 filter codes 1–4 one at a time, reads each " +
                                "temporary value back, restores the captured stock bytes after every try, " +
                                "and requires a final exact stock snapshot. It sends no persistence command."
                        } else {
                            "The filter-type test remains locked until this installation reads the " +
                                "complete preserved stock snapshot exactly."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = persistencePhase in 0..2 && reversibleProbeReady,
                        onClick = ::requestPersistenceQualification,
                    ) {
                        Text(
                            when (persistencePhase) {
                                1 -> "Reconnect, then verify saved marker"
                                2 -> "Reconnect, then verify restored stock"
                                3 -> "Persistence qualification complete"
                                else -> "Run consolidated persistence qualification"
                            },
                        )
                    }
                    Text(
                        when (persistencePhase) {
                            1 -> "Reconnect the cable now, then tap this button. The app will verify that the temporary marker survived the public COMMIT command."
                            2 -> "Reconnect the cable now, then tap this button. The app will verify the final untouched stock snapshot after restoration."
                            3 -> "The marker survived reconnect, the captured stock state was restored and saved, and the final reconnect matched it exactly."
                            else -> "This one consolidated test writes a reversible all-band marker, sends one provisional COMMIT, verifies it after reconnect, restores the captured stock bytes, and requires a final reconnect."
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

    private fun requestReversibleWriteTest() {
        if (!reversibleProbeReady) {
            report.value = "The reversible test is locked until this app confirms the exact preserved stock snapshot."
            return
        }
        requestPermissionOrRun(
            action = ACTION_REVERSIBLE_WRITE_PERMISSION,
            requestCode = 2,
            waitingMessage = "Waiting for Android's USB permission prompt. This approved diagnostic " +
                "runs bounded temporary EQ-field checks, restoring each captured value immediately. " +
                "It does not commit or save.",
            onGranted = ::runReversibleWriteTest,
        )
    }

    private fun requestFilterTypeTest() {
        if (!filterTypeProbeReady) {
            report.value = "The filter-type test is locked until this app confirms the exact preserved stock snapshot."
            return
        }
        requestPermissionOrRun(
            action = ACTION_FILTER_TYPE_PERMISSION,
            requestCode = 3,
            waitingMessage = "Waiting for Android's USB permission prompt. This approved diagnostic " +
                "tries four temporary Band 1 filter codes and restores the captured bytes after each one. " +
                "It does not commit or save.",
            onGranted = ::runFilterTypeTest,
        )
    }

    private fun requestPersistenceQualification() {
        if (!reversibleProbeReady) {
            report.value = "Persistence qualification is locked until the complete preserved stock snapshot matches."
            return
        }
        val phaseMessage = when (persistencePhase) {
            0 -> "This approved test will write a one-byte-per-field marker and send one provisional COMMIT. Keep listening volume low."
            1 -> "Reconnect the cable before continuing. The app will verify whether the saved marker survived."
            2 -> "Reconnect the cable before continuing. The app will verify the restored stock state."
            else -> "Persistence qualification is already complete."
        }
        if (persistencePhase == 3) {
            report.value = phaseMessage
            return
        }
        requestPermissionOrRun(
            action = ACTION_PERSISTENCE_PERMISSION,
            requestCode = 4,
            waitingMessage = "Waiting for Android's USB permission prompt. $phaseMessage",
            onGranted = ::runPersistenceQualification,
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
                                val exactEw300Descriptor = exactDescriptor.contentEquals(
                                    Ew300ProvisionalProtocol.EXPECTED_HID_DESCRIPTOR,
                                )
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
                        val responses = linkedMapOf<Int, ByteArray>()
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
                                responses[register] = response
                                appendLine("Decoded provisionally: ${Ew300ProvisionalProtocol.describe(register, response)}")
                            }
                        }
                        val exactStockSnapshot = !stopped &&
                            Ew300ProvisionalProtocol.matchesStockSnapshot(responses)
                        appendLine()
                        appendLine("Exact preserved stock snapshot match: $exactStockSnapshot")
                        if (!exactStockSnapshot) {
                            val mismatches = Ew300ProvisionalProtocol.stockSnapshotMismatchRegisters(responses)
                            appendLine(
                                "Stock-gate mismatch register(s): " +
                                    mismatches.joinToString { "0x%02X".format(it) },
                            )
                            mismatches.forEach { register ->
                                val actual = responses[register]
                                val expected = Ew300ProvisionalProtocol.STOCK_RESPONSE_PAYLOADS.getValue(register)
                                appendLine(
                                    "Stock-gate 0x%02X actual (%s bytes): %s".format(
                                        register,
                                        actual?.size ?: 0,
                                        actual?.toHex() ?: "missing",
                                    ),
                                )
                                appendLine(
                                    "Stock-gate 0x%02X expected (%s bytes): %s".format(
                                        register,
                                        expected.size,
                                        expected.toHex(),
                                    ),
                                )
                            }
                            appendLine("The raw received payloads above remain authoritative; no WRITE is unlocked.")
                        }
                        reversibleProbeReady = exactStockSnapshot
                        filterTypeProbeReady = exactStockSnapshot
                        persistencePhase = 0
                        persistenceBaseline = null
                        persistenceMarker = null
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

    private fun runFilterTypeTest(device: UsbDevice): String {
        filterTypeProbeReady = false
        if (device.vendorId != EW300_VENDOR_ID || device.productId != EW300_PRODUCT_ID ||
            device.productName != EW300_PRODUCT_NAME || device.manufacturerName != EW300_MANUFACTURER_NAME
        ) return "The filter-type test stopped because the exact cable identity did not match. No write was sent."
        val hidInterface = (0 until device.interfaceCount).map(device::getInterface).singleOrNull {
            it.id == EW300_HID_INTERFACE_ID && it.interfaceClass == UsbConstants.USB_CLASS_HID
        } ?: return "The filter-type test stopped because exact HID interface 3 was unavailable. No write was sent."
        val interruptIn = (0 until hidInterface.endpointCount).map(hidInterface::getEndpoint).singleOrNull {
            it.address == EW300_INTERRUPT_IN_ADDRESS && it.direction == UsbConstants.USB_DIR_IN &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_INT
        } ?: return "The filter-type test stopped because interrupt-IN 0x82 was unavailable. No write was sent."
        val interruptOut = (0 until hidInterface.endpointCount).map(hidInterface::getEndpoint).singleOrNull {
            it.address == EW300_INTERRUPT_OUT_ADDRESS && it.direction == UsbConstants.USB_DIR_OUT &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_INT
        } ?: return "The filter-type test stopped because interrupt-OUT 0x02 was unavailable. No write was sent."
        val connection = usbManager.openDevice(device)
            ?: return "Android could not open the exact cable. No write was sent."
        return try {
            if (!connection.claimInterface(hidInterface, true)) {
                "Android did not grant the isolated HID-interface claim. No write was sent."
            } else try {
                val log = StringBuilder()
                fun readRegister(label: String, register: Int): ByteArray? {
                    val reportBytes = Ew300ProvisionalProtocol.wireReport(
                        Ew300ProvisionalProtocol.readPayload(register),
                    )
                    log.appendLine("$label READ 0x%02X OUT: %s".format(register, reportBytes.toHex()))
                    val sent = connection.bulkTransfer(interruptOut, reportBytes, 0, reportBytes.size, PROVISIONAL_TRANSFER_TIMEOUT_MS)
                    log.appendLine("$label READ OUT result: $sent bytes")
                    if (sent != reportBytes.size) return null
                    repeat(REVERSIBLE_RESPONSE_ATTEMPTS) { attempt ->
                        val incoming = ByteArray(interruptIn.maxPacketSize)
                        val received = connection.bulkTransfer(interruptIn, incoming, 0, incoming.size, PROVISIONAL_TRANSFER_TIMEOUT_MS)
                        log.appendLine("$label IN ${attempt + 1}/$REVERSIBLE_RESPONSE_ATTEMPTS: $received bytes")
                        if (received > 0) {
                            val exact = incoming.copyOf(received)
                            log.appendLine("$label IN: ${exact.toHex()}")
                            Ew300ProvisionalProtocol.responsePayload(exact, register)?.let { return it }
                            log.appendLine("$label ignored a non-READ response while waiting for the exact echo.")
                        }
                    }
                    return null
                }
                fun writeRegister(label: String, register: Int, data: ByteArray): Int {
                    val reportBytes = Ew300ProvisionalProtocol.wireReport(
                        Ew300ProvisionalProtocol.writePayload(register, data),
                    )
                    log.appendLine("$label WRITE 0x%02X OUT: %s".format(register, reportBytes.toHex()))
                    val sent = connection.bulkTransfer(interruptOut, reportBytes, 0, reportBytes.size, PROVISIONAL_TRANSFER_TIMEOUT_MS)
                    log.appendLine("$label WRITE OUT result: $sent bytes")
                    return sent
                }
                log.appendLine("Capture type: approved reversible EW300 filter-type qualification")
                log.appendLine("Exact device: 31B2:0111 / LE XIAN / SIMGOT EW300 DSP")
                log.appendLine("Scope: Band 1 register 0x27 filter-type codes 1, 2, 3, and 4; each is restored immediately")
                log.appendLine("Public KT02H20 mapping is provisional; this test validates raw field transport only.")
                log.appendLine("No COMMIT, CLEAR, save, reset, slot, global-gain, or firmware command is present.")
                log.appendLine()
                val initial = linkedMapOf<Int, ByteArray>()
                Ew300ProvisionalProtocol.snapshotRegisters().forEach { register ->
                    readRegister("INITIAL", register)?.let { initial[register] = it }
                }
                var passed = Ew300ProvisionalProtocol.matchesStockSnapshot(initial)
                log.appendLine("Exact preserved full baseline match: $passed")
                if (passed) {
                    val register = 0x27
                    val stockPayload = Ew300ProvisionalProtocol.expectedStockPayload(register)
                    val stockData = Ew300ProvisionalProtocol.stockData(register)
                    Ew300ProvisionalProtocol.FILTER_TYPE_PROBES.forEachIndexed { index, probe ->
                        if (!passed) return@forEachIndexed
                        log.appendLine()
                        log.appendLine("CHECK ${index + 1}/${Ew300ProvisionalProtocol.FILTER_TYPE_PROBES.size}: Band 1 ${probe.label} code ${probe.code}")
                        val baseline = readRegister("BASELINE", register)
                        val baselineExact = baseline?.contentEquals(stockPayload) == true
                        log.appendLine("Exact preserved baseline match: $baselineExact")
                        if (!baselineExact) {
                            log.appendLine("STOP: Band 1 no longer matches the preserved stock capture. No write was sent for this code.")
                            passed = false
                            return@forEachIndexed
                        }
                        val temporaryData = stockData.copyOf().apply { this[2] = probe.code.toByte() }
                        val tempSent = writeRegister("TEMPORARY", register, temporaryData)
                        SystemClock.sleep(REVERSIBLE_SETTLE_MS)
                        val tempRead = if (tempSent == Ew300ProvisionalProtocol.WIRE_REPORT_SIZE) readRegister("TEMPORARY", register) else null
                        val tempVerified = tempRead?.contentEquals(Ew300ProvisionalProtocol.expectedReadPayload(register, temporaryData)) == true
                        log.appendLine("Temporary ${probe.label} raw-code readback verified: $tempVerified")
                        val restoreSent = writeRegister("RESTORE", register, stockData)
                        SystemClock.sleep(REVERSIBLE_SETTLE_MS)
                        val restoredRead = if (restoreSent == Ew300ProvisionalProtocol.WIRE_REPORT_SIZE) readRegister("RESTORE", register) else null
                        val restored = restoredRead?.contentEquals(stockPayload) == true
                        log.appendLine("Exact preserved restoration verified: $restored")
                        if (!tempVerified || !restored) {
                            log.appendLine("STOP: the filter-type batch will not continue after this check.")
                            passed = false
                        }
                    }
                }
                if (passed) {
                    val finalResponses = linkedMapOf<Int, ByteArray>()
                    Ew300ProvisionalProtocol.snapshotRegisters().forEach { register ->
                        readRegister("FINAL", register)?.let { finalResponses[register] = it }
                    }
                    passed = Ew300ProvisionalProtocol.matchesStockSnapshot(finalResponses)
                    log.appendLine()
                    log.appendLine("Exact preserved final full snapshot match: $passed")
                }
                log.appendLine("HID interface ${hidInterface.id} release follows this report.")
                log.append(if (passed) {
                    "RESTORED: every filter-code check and the final full snapshot match the untouched capture. "
                } else {
                    "ATTENTION: the filter-type batch did not complete an exact restoration. Stop playback and retain this report. "
                })
                log.append("No COMMIT, CLEAR, save, reset, slot, global-gain, or firmware command was sent. Reconnect the cable now.")
                log.toString()
            } finally {
                connection.releaseInterface(hidInterface)
            }
        } finally {
            connection.close()
        }
    }

    private fun runReversibleWriteTest(device: UsbDevice): String {
        reversibleProbeReady = false
        if (device.vendorId != EW300_VENDOR_ID || device.productId != EW300_PRODUCT_ID ||
            device.productName != EW300_PRODUCT_NAME ||
            device.manufacturerName != EW300_MANUFACTURER_NAME
        ) {
            return "The reversible test stopped because the exact cable identity did not match. No write was sent."
        }
        val hidInterface = (0 until device.interfaceCount)
            .map(device::getInterface)
            .singleOrNull { usbInterface ->
                usbInterface.id == EW300_HID_INTERFACE_ID &&
                    usbInterface.interfaceClass == UsbConstants.USB_CLASS_HID
            }
            ?: return "The reversible test stopped because exact HID interface 3 was unavailable. No write was sent."
        val interruptIn = (0 until hidInterface.endpointCount)
            .map(hidInterface::getEndpoint)
            .singleOrNull { endpoint ->
                endpoint.address == EW300_INTERRUPT_IN_ADDRESS &&
                    endpoint.direction == UsbConstants.USB_DIR_IN &&
                    endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT
            }
            ?: return "The reversible test stopped because interrupt-IN 0x82 was unavailable. No write was sent."
        val interruptOut = (0 until hidInterface.endpointCount)
            .map(hidInterface::getEndpoint)
            .singleOrNull { endpoint ->
                endpoint.address == EW300_INTERRUPT_OUT_ADDRESS &&
                    endpoint.direction == UsbConstants.USB_DIR_OUT &&
                    endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT
            }
            ?: return "The reversible test stopped because interrupt-OUT 0x02 was unavailable. No write was sent."
        val connection = usbManager.openDevice(device)
            ?: return "Android could not open the exact cable. No write was sent."
        return try {
            if (!connection.claimInterface(hidInterface, true)) {
                "Android did not grant the isolated HID-interface claim. No write was sent."
            } else {
                try {
                    val log = StringBuilder()
                    fun readRegister(label: String, register: Int): ByteArray? {
                        val payload = Ew300ProvisionalProtocol.readPayload(register)
                        val reportBytes = Ew300ProvisionalProtocol.wireReport(payload)
                        log.appendLine("$label READ 0x%02X OUT: %s".format(register, reportBytes.toHex()))
                        val sent = connection.bulkTransfer(
                            interruptOut,
                            reportBytes,
                            0,
                            reportBytes.size,
                            PROVISIONAL_TRANSFER_TIMEOUT_MS,
                        )
                        log.appendLine("$label READ OUT result: $sent bytes")
                        if (sent != reportBytes.size) return null
                        repeat(REVERSIBLE_RESPONSE_ATTEMPTS) { attempt ->
                            val incoming = ByteArray(interruptIn.maxPacketSize)
                            val received = connection.bulkTransfer(
                                interruptIn,
                                incoming,
                                0,
                                incoming.size,
                                PROVISIONAL_TRANSFER_TIMEOUT_MS,
                            )
                            log.appendLine("$label IN ${attempt + 1}/$REVERSIBLE_RESPONSE_ATTEMPTS: $received bytes")
                            if (received > 0) {
                                val exact = incoming.copyOf(received)
                                log.appendLine("$label IN: ${exact.toHex()}")
                                val response = Ew300ProvisionalProtocol.responsePayload(
                                    exact,
                                    register,
                                )
                                if (response != null) return response
                                log.appendLine("$label ignored a non-READ response while waiting for the exact echo.")
                            }
                        }
                        return null
                    }

                    fun writeRegister(label: String, register: Int, data: ByteArray): Int {
                        val reportBytes = Ew300ProvisionalProtocol.wireReport(
                            Ew300ProvisionalProtocol.writePayload(register, data),
                        )
                        log.appendLine("$label WRITE 0x%02X OUT: %s".format(register, reportBytes.toHex()))
                        val sent = connection.bulkTransfer(
                            interruptOut,
                            reportBytes,
                            0,
                            reportBytes.size,
                            PROVISIONAL_TRANSFER_TIMEOUT_MS,
                        )
                        log.appendLine("$label WRITE OUT result: $sent bytes")
                        return sent
                    }

                    log.appendLine("Capture type: approved reversible EW300 EQ-field batch write/readback/restore")
                    log.appendLine("Exact device: 31B2:0111 / LE XIAN / SIMGOT EW300 DSP")
                    log.appendLine("Scope: eight small temporary remaining frequency/Q field checks; each uses exact readback and immediate restoration")
                    log.appendLine("No COMMIT, CLEAR, save, reset, slot, global-gain, or firmware command is present.")
                    log.appendLine()

                    val initialResponses = linkedMapOf<Int, ByteArray>()
                    Ew300ProvisionalProtocol.snapshotRegisters().forEach { register ->
                        readRegister("INITIAL", register)?.let { initialResponses[register] = it }
                    }
                    val initialExact = Ew300ProvisionalProtocol.matchesStockSnapshot(initialResponses)
                    log.appendLine("Exact preserved full baseline match: $initialExact")
                    if (!initialExact) {
                        log.append("STOP: the full current snapshot differs from the preserved stock capture. No write was sent.")
                        log.toString()
                    } else {
                        var batchPassed = true
                        Ew300ProvisionalProtocol.APPROVED_REVERSIBLE_PROBES.forEachIndexed { index, probe ->
                            if (!batchPassed) return@forEachIndexed
                            val stockPayload = Ew300ProvisionalProtocol.expectedStockPayload(probe.register)
                            val stockData = Ew300ProvisionalProtocol.stockData(probe.register)
                            log.appendLine()
                            log.appendLine("CHECK ${index + 1}/${Ew300ProvisionalProtocol.APPROVED_REVERSIBLE_PROBES.size}: ${probe.label} (0x%02X)".format(probe.register))
                            val baseline = readRegister("BASELINE", probe.register)
                            val baselineExact = baseline?.contentEquals(stockPayload) == true
                            log.appendLine("Exact preserved baseline match: $baselineExact")
                            if (!baselineExact) {
                                log.appendLine("STOP: this field no longer matches the preserved stock capture. No write was sent for it.")
                                batchPassed = false
                                return@forEachIndexed
                            }
                            val temporarySent = writeRegister("TEMPORARY", probe.register, probe.temporaryData)
                            SystemClock.sleep(REVERSIBLE_SETTLE_MS)
                            val temporaryRead = if (temporarySent == Ew300ProvisionalProtocol.WIRE_REPORT_SIZE) {
                                readRegister("TEMPORARY", probe.register)
                            } else {
                                null
                            }
                            val expectedTemporary = Ew300ProvisionalProtocol.expectedReadPayload(
                                probe.register,
                                probe.temporaryData,
                            )
                            val temporaryVerified = temporaryRead?.contentEquals(expectedTemporary) == true
                            log.appendLine("Temporary readback verified: $temporaryVerified")

                            val restoreSent = writeRegister("RESTORE", probe.register, stockData)
                            SystemClock.sleep(REVERSIBLE_SETTLE_MS)
                            val restoredRead = if (restoreSent == Ew300ProvisionalProtocol.WIRE_REPORT_SIZE) {
                                readRegister("RESTORE", probe.register)
                            } else {
                                null
                            }
                            val restored = restoredRead?.contentEquals(stockPayload) == true
                            log.appendLine("Exact preserved restoration verified: $restored")
                            if (!restored || !temporaryVerified) {
                                log.appendLine("STOP: the batch will not continue after this check.")
                                batchPassed = false
                            }
                        }
                        if (batchPassed) {
                            val finalResponses = linkedMapOf<Int, ByteArray>()
                            Ew300ProvisionalProtocol.snapshotRegisters().forEach { register ->
                                readRegister("FINAL", register)?.let { finalResponses[register] = it }
                            }
                            batchPassed = Ew300ProvisionalProtocol.matchesStockSnapshot(finalResponses)
                            log.appendLine()
                            log.appendLine("Exact preserved final full snapshot match: $batchPassed")
                        }
                        log.appendLine("HID interface ${hidInterface.id} release follows this report.")
                        log.append(
                            if (batchPassed) {
                                "RESTORED: every completed check and the final full snapshot match the untouched capture. "
                            } else {
                                "ATTENTION: the batch did not complete an exact restoration. Stop playback and retain this report. "
                            },
                        )
                        log.append(
                            "No COMMIT, CLEAR, save, reset, slot, global-gain, or firmware command was sent. " +
                                "Reconnect the cable now.",
                        )
                        log.toString()
                    }
                } finally {
                    connection.releaseInterface(hidInterface)
                }
            }
        } finally {
            connection.close()
        }
    }

    /**
     * Debug-only consolidated persistence qualification. The first phase writes a one-byte-per-
     * field marker and sends exactly one provisional COMMIT. The caller must reconnect between
     * phases so the result cannot be satisfied by a cached in-memory state.
     */
    private fun runPersistenceQualification(device: UsbDevice): String {
        if (persistencePhase !in 0..2) return "Persistence qualification is already complete."
        if (device.vendorId != EW300_VENDOR_ID || device.productId != EW300_PRODUCT_ID ||
            device.productName != EW300_PRODUCT_NAME || device.manufacturerName != EW300_MANUFACTURER_NAME
        ) return "The persistence qualification stopped because the exact cable identity did not match. No command was sent."
        val hidInterface = (0 until device.interfaceCount).map(device::getInterface).singleOrNull {
            it.id == EW300_HID_INTERFACE_ID && it.interfaceClass == UsbConstants.USB_CLASS_HID
        } ?: return "The persistence qualification stopped because exact HID interface 3 was unavailable. No command was sent."
        val interruptIn = (0 until hidInterface.endpointCount).map(hidInterface::getEndpoint).singleOrNull {
            it.address == EW300_INTERRUPT_IN_ADDRESS && it.direction == UsbConstants.USB_DIR_IN &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_INT
        } ?: return "The persistence qualification stopped because interrupt-IN 0x82 was unavailable. No command was sent."
        val interruptOut = (0 until hidInterface.endpointCount).map(hidInterface::getEndpoint).singleOrNull {
            it.address == EW300_INTERRUPT_OUT_ADDRESS && it.direction == UsbConstants.USB_DIR_OUT &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_INT
        } ?: return "The persistence qualification stopped because interrupt-OUT 0x02 was unavailable. No command was sent."
        val connection = usbManager.openDevice(device)
            ?: return "Android could not open the exact cable. No command was sent."
        return try {
            if (!connection.claimInterface(hidInterface, true)) {
                "Android did not grant the isolated HID-interface claim. No command was sent."
            } else try {
                val log = StringBuilder()
                fun readRegister(label: String, register: Int): ByteArray? {
                    val reportBytes = Ew300ProvisionalProtocol.wireReport(
                        Ew300ProvisionalProtocol.readPayload(register),
                    )
                    log.appendLine("$label READ 0x%02X OUT: %s".format(register, reportBytes.toHex()))
                    val sent = connection.bulkTransfer(interruptOut, reportBytes, 0, reportBytes.size, PROVISIONAL_TRANSFER_TIMEOUT_MS)
                    log.appendLine("$label READ OUT result: $sent bytes")
                    if (sent != reportBytes.size) return null
                    repeat(REVERSIBLE_RESPONSE_ATTEMPTS) { attempt ->
                        val incoming = ByteArray(interruptIn.maxPacketSize)
                        val received = connection.bulkTransfer(interruptIn, incoming, 0, incoming.size, PROVISIONAL_TRANSFER_TIMEOUT_MS)
                        log.appendLine("$label IN ${attempt + 1}/$REVERSIBLE_RESPONSE_ATTEMPTS: $received bytes")
                        if (received > 0) {
                            val exact = incoming.copyOf(received)
                            log.appendLine("$label IN: ${exact.toHex()}")
                            Ew300ProvisionalProtocol.responsePayload(exact, register)?.let { return it }
                            log.appendLine("$label ignored a non-READ response while waiting for the exact echo.")
                        }
                    }
                    return null
                }
                fun readSnapshot(label: String): Map<Int, ByteArray>? {
                    val responses = linkedMapOf<Int, ByteArray>()
                    Ew300ProvisionalProtocol.snapshotRegisters().forEach { register ->
                        val response = readRegister(label, register) ?: return null
                        responses[register] = response
                    }
                    return responses
                }
                fun writeRegister(label: String, register: Int, data: ByteArray): Boolean {
                    val reportBytes = Ew300ProvisionalProtocol.wireReport(
                        Ew300ProvisionalProtocol.writePayload(register, data),
                    )
                    log.appendLine("$label WRITE 0x%02X OUT: %s".format(register, reportBytes.toHex()))
                    val sent = connection.bulkTransfer(interruptOut, reportBytes, 0, reportBytes.size, PROVISIONAL_TRANSFER_TIMEOUT_MS)
                    log.appendLine("$label WRITE OUT result: $sent bytes")
                    return sent == reportBytes.size
                }
                fun sendCommit(): Boolean {
                    val reportBytes = Ew300PersistenceQualification.commitReport()
                    log.appendLine("COMMIT 0x53 OUT: ${reportBytes.toHex()}")
                    val sent = connection.bulkTransfer(interruptOut, reportBytes, 0, reportBytes.size, PROVISIONAL_TRANSFER_TIMEOUT_MS)
                    log.appendLine("COMMIT OUT result: $sent bytes")
                    if (sent == reportBytes.size) SystemClock.sleep(COMMIT_SETTLE_MS)
                    return sent == reportBytes.size
                }
                fun restoreBaseline(baseline: Map<Int, ByteArray>): Boolean {
                    var restored = true
                    Ew300PersistenceQualification.MARKER_FIELDS.keys.forEach { register ->
                        if (!restored) return@forEach
                        val payload = baseline[register]
                        val data = payload?.copyOfRange(6, 10)
                        if (data == null || !writeRegister("RESTORE", register, data)) {
                            restored = false
                            return@forEach
                        }
                        SystemClock.sleep(REVERSIBLE_SETTLE_MS)
                        val readback = readRegister("RESTORE", register)
                        val expected = Ew300ProvisionalProtocol.expectedReadPayload(register, data)
                        val exact = readback?.contentEquals(expected) == true
                        log.appendLine("RESTORE 0x%02X exact readback: $exact".format(register))
                        restored = exact
                    }
                    restored
                }

                when (persistencePhase) {
                    0 -> {
                        log.appendLine("Capture type: consolidated EW300 persistence qualification")
                        log.appendLine("Exact device: 31B2:0111 / LE XIAN / SIMGOT EW300 DSP")
                        log.appendLine("Scope: all five band fields, Band 1 provisional type byte, and one raw global-gain byte")
                        log.appendLine("The marker is temporary and must be restored. Keep listening volume low.")
                        val baseline = readSnapshot("INITIAL")
                        val baselineExact = baseline != null && Ew300ProvisionalProtocol.matchesStockSnapshot(baseline)
                        log.appendLine("Exact preserved full baseline match: $baselineExact")
                        if (!baselineExact || baseline == null) {
                            log.append("STOP: baseline did not match the preserved stock capture. No marker write or COMMIT was sent.")
                            return@try log.toString()
                        }
                        var markerPassed = true
                        Ew300PersistenceQualification.MARKER_FIELDS.forEach { (register, markerData) ->
                            if (!markerPassed) return@forEach
                            val temporarySent = writeRegister("TEMPORARY", register, markerData)
                            SystemClock.sleep(REVERSIBLE_SETTLE_MS)
                            val temporaryRead = if (temporarySent) readRegister("TEMPORARY", register) else null
                            val expected = Ew300ProvisionalProtocol.expectedReadPayload(register, markerData)
                            markerPassed = temporaryRead?.contentEquals(expected) == true
                            log.appendLine("Temporary 0x%02X exact readback: $markerPassed".format(register))
                        }
                        if (!markerPassed) {
                            log.appendLine("ATTENTION: marker readback failed. Restoring the captured bytes without COMMIT.")
                            restoreBaseline(baseline)
                            persistencePhase = 0
                            return@try log.append("No persistence result is claimed. Reconnect and run a fresh baseline.").let { log.toString() }
                        }
                        persistenceBaseline = baseline.mapValues { it.value.copyOf() }
                        persistenceMarker = Ew300PersistenceQualification.MARKER_FIELDS.mapValues { it.value.copyOf() }
                        if (!sendCommit()) {
                            log.appendLine("ATTENTION: COMMIT report was not accepted. Restoring the captured bytes without retrying COMMIT.")
                            restoreBaseline(baseline)
                            persistenceBaseline = null
                            persistenceMarker = null
                            persistencePhase = 0
                            return@try log.append("No persistence result is claimed. Reconnect and run a fresh baseline.").let { log.toString() }
                        }
                        persistencePhase = 1
                        log.append("MARKER SAVED: disconnect and reconnect the cable, then tap Verify saved marker.")
                        log.toString()
                    }
                    1 -> {
                        val baseline = persistenceBaseline
                        val marker = persistenceMarker
                        if (baseline == null || marker == null) {
                            persistencePhase = 0
                            return@try "The saved marker is unavailable in this app session. Run a fresh baseline qualification."
                        }
                        log.appendLine("Phase 2: verify marker persistence after reconnect")
                        val current = readSnapshot("PERSISTED")
                        val markerPersisted = current != null && marker.all { (register, data) ->
                            current[register]?.contentEquals(Ew300ProvisionalProtocol.expectedReadPayload(register, data)) == true
                        }
                        val unrelatedUnchanged = current != null && Ew300ProvisionalProtocol.snapshotRegisters()
                            .filter { it !in marker.keys }
                            .all { current[it]?.contentEquals(baseline[it]) == true }
                        log.appendLine("Saved marker exact match after reconnect: $markerPersisted")
                        log.appendLine("Slot and unrelated stock fields unchanged: $unrelatedUnchanged")
                        val restored = restoreBaseline(baseline)
                        log.appendLine("Captured stock bytes restored before second COMMIT: $restored")
                        val commitRestored = restored && sendCommit()
                        if (!commitRestored) {
                            persistencePhase = 0
                            log.append("ATTENTION: restoration or COMMIT failed. Stop and retain this report; no automatic retry was made.")
                            return@try log.toString()
                        }
                        persistencePhase = 2
                        log.append("STOCK RESTORE SAVED: disconnect and reconnect the cable, then tap Verify restored stock.")
                        log.toString()
                    }
                    else -> {
                        val baseline = persistenceBaseline
                            ?: return@try "The original stock baseline is unavailable in this app session. Run a fresh qualification."
                        log.appendLine("Phase 3: verify final restored stock after reconnect")
                        val current = readSnapshot("FINAL")
                        val finalExact = current != null && Ew300ProvisionalProtocol.matchesStockSnapshot(current) &&
                            Ew300ProvisionalProtocol.snapshotRegisters().all { current[it]?.contentEquals(baseline[it]) == true }
                        log.appendLine("Exact preserved final stock snapshot match: $finalExact")
                        persistencePhase = if (finalExact) 3 else 2
                        log.append(
                            if (finalExact) {
                                "PERSISTED AND RESTORED: marker survived reconnect, captured stock was saved again, and the final snapshot matches exactly."
                            } else {
                                "ATTENTION: final stock snapshot did not match exactly. Stop and retain this report; do not retry writes."
                            },
                        )
                        log.toString()
                    }
                }
            } finally {
                connection.releaseInterface(hidInterface)
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
        const val ACTION_REVERSIBLE_WRITE_PERMISSION =
            "com.weekssa.opraeqforuapp.diagnostics.USB_REVERSIBLE_WRITE_PERMISSION"
        const val ACTION_FILTER_TYPE_PERMISSION =
            "com.weekssa.opraeqforuapp.diagnostics.USB_FILTER_TYPE_PERMISSION"
        const val ACTION_PERSISTENCE_PERMISSION =
            "com.weekssa.opraeqforuapp.diagnostics.USB_PERSISTENCE_PERMISSION"
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
        const val COMMIT_SETTLE_MS = 1000L
        const val REVERSIBLE_SETTLE_MS = 200L
        const val REVERSIBLE_RESPONSE_ATTEMPTS = 3
        const val USB_REQUEST_GET_DESCRIPTOR = 0x06
        const val USB_DESCRIPTOR_TYPE_REPORT = 0x22
        const val HID_REQUEST_GET_REPORT = 0x01
        const val HID_REPORT_TYPE_INPUT = 0x01
        const val USB_RECIP_INTERFACE = 0x01
    }
}
