package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import com.weekssa.opraeqforuapp.data.dac.Ew300ReconnectGate
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Transport
import java.io.Closeable
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Android USB-host transport for the exact SIMGOT EW300 DSP beta identity. */
class AndroidEw300UsbTransport(
    context: Context,
    internal val reconnectGate: Ew300ReconnectGate = Ew300ReconnectGate(),
) : Ew300Transport, Closeable {
    private val hid = AndroidKt02h20HidSession(
        context = context,
        vendorId = Ew300Protocol.VENDOR_ID,
        productIds = setOf(Ew300Protocol.PRODUCT_ID),
        deviceLabel = "SIMGOT EW300 DSP",
        permissionSuffix = "SIMGOT_EW300",
        deviceIdentityMatcher = Ew300UsbIdentity::matches,
        hidInterfaceMatcher = Ew300UsbIdentity::matchesHidInterface,
        additionalFingerprintFields = { device -> listOf("deviceRevision" to device.version) },
    )
    private var reads: Long = 0L
    private var writes: Long = 0L
    private var saves: Long = 0L

    val state: StateFlow<Kt02h20ConnectionState> = hid.state
    val present: StateFlow<Boolean> = hid.present
    override val sessionGeneration: Long get() = hid.sessionGeneration
    override val deviceFingerprintKey: String?
        get() = hid.deviceFingerprintKey
    override val detachGeneration: Long
        get() = hid.detachGeneration
    override val permissionRequestCount: Long
        get() = hid.permissionRequestCount
    override val registerReadCount: Long
        get() = reads
    override val registerWriteCount: Long
        get() = writes
    override val saveCommandCount: Long
        get() = saves

    fun connect() = hid.connect()

    /** Re-opens the device after its reads stopped answering; see the HID session contract. */
    fun reconnect() = hid.reconnect()

    /**
     * Automatic reconnect has a second, invocation-time guard in addition to the repository
     * observer's StateFlow check. The observer can already have queued a connect callback when a
     * mutation begins; re-checking here prevents that stale callback from launching Android's USB
     * permission prompt before Save has released replacement-session reconnect.
     */
    fun connectAutomatically() {
        reconnectGate.runAutomaticReconnectIfAllowed { hid.connect() }
    }

    override suspend fun readRegister(register: Int): ByteArray? {
        reads += 1L
        // A connected EW300 can occasionally drop one HID input report while Android is
        // draining an unsolicited status report. Reads are idempotent, so retry only the
        // exact register read; mutating writes remain deliberately non-retried.
        repeat(3) { attempt ->
            val value = hid.exchange(
                report = Ew300Protocol.readRegisterReport(register),
                minResponseBytes = Ew300Protocol.REPORT_SIZE,
                // The EW300 can expose unsolicited HID input after a write. Do not let a valid-sized
                // non-read response satisfy this transaction; wait for the exact register read echo.
                acceptResponse = { response -> Ew300Protocol.decodeRead(register, response) != null },
            )?.let { Ew300Protocol.decodeRead(register, it) }
            if (value != null) return value
            if (attempt < 2) delay(100L)
        }
        return null
    }

    override suspend fun writeRegister(register: Int, data: ByteArray): Boolean {
        writes += 1L
        // Match the delay used by the physical qualification: the EW300 may emit an
        // unsolicited input report before the register is ready for the next transaction.
        return hid.send(Ew300Protocol.writeRegisterReport(register, data), settleMillis = 200L)
    }

    override suspend fun commit(): Boolean {
        // Persistence may reset the EW300 USB function. Keep the operation alive across that
        // expected re-enumeration and only let the flasher read back after a fresh HID handle is
        // available. This also prevents the shared auto-reconnect policy from racing a stale
        // connection while the old handle is being closed.
        val previousGeneration = hid.sessionGeneration
        val previousDetachGeneration = hid.detachGeneration
        saves += 1L
        if (!hid.send(Ew300Protocol.commitReport(), settleMillis = 1_000L)) return false
        reconnectGate.markSaveSent()
        return hid.awaitOptionalReconnectAfterMutation(previousGeneration, previousDetachGeneration)
    }

    override fun markRuntimeVerified() {
        reconnectGate.markRuntimeVerified()
    }

    @Volatile
    private var cachedUacDescriptor: com.weekssa.opraeqforuapp.domain.ew300.Ew300UacDescriptor? = null

    override suspend fun readUacStatus(): com.weekssa.opraeqforuapp.domain.ew300.Ew300UacStatus? =
        withContext(Dispatchers.IO) {
            if (deviceFingerprintKey == null) return@withContext null
            val raw = hid.configurationDescriptor() ?: return@withContext null
            val descriptor = com.weekssa.opraeqforuapp.domain.ew300.Ew300UacDescriptorParser.parse(raw)
                ?: return@withContext null
            cachedUacDescriptor = descriptor
            val stream = descriptor.streams.firstOrNull {
                it.endpointAddress != null && it.hasSamplingFrequencyControl &&
                    (it.endpointAddress and 0x80) == 0
            } ?: descriptor.streams.firstOrNull {
                it.endpointAddress != null && it.hasSamplingFrequencyControl
            }
            var rate: Int? = null
            var ratePath: String? = null
            if (stream != null) {
                val endpointRequest = com.weekssa.opraeqforuapp.domain.ew300.Ew300UacRequests
                    .currentSampleRateEndpoint(stream)
                val endpointValue = endpointRequest?.let { request ->
                    hid.controlTransferInOnInterface(
                        stream.interfaceNumber,
                        stream.alternateSetting,
                        request.requestType,
                        request.request,
                        request.value,
                        request.index,
                        request.length,
                    )
                }
                if (endpointValue != null && endpointValue.size >= 3) {
                    rate = (endpointValue[0].toInt() and 0xFF) or
                        ((endpointValue[1].toInt() and 0xFF) shl 8) or
                        ((endpointValue[2].toInt() and 0xFF) shl 16)
                    ratePath = "UAC1 GET_CUR endpoint"
                }
            }
            if (rate == 0) {
                rate = null
                ratePath = null
            }
            val mic = descriptor.microphoneFeature
            val controlInterface = descriptor.audioControlInterface
            val muted = if (mic != null && controlInterface != null) {
                val request = com.weekssa.opraeqforuapp.domain.ew300.Ew300UacRequests
                    .microphoneMuteGet(mic.unitId, controlInterface)
                hid.controlTransferInOnInterface(
                    controlInterface,
                    null,
                    request.requestType,
                    request.request,
                    request.value,
                    request.index,
                    request.length,
                )?.firstOrNull()?.let { (it.toInt() and 0x01) == 1 }
            } else {
                null
            }
            val reasons = buildList {
                if (rate == null) add("Current sample rate was not readable through UAC1 GET_CUR.")
                if (descriptor.uacVersion != "UAC 2.0") {
                    add("One configuration exposes " + descriptor.uacVersion + "; no exact UAC mode switch control was located.")
                }
                if (mic == null) add("No UAC1 microphone feature unit with mute control was found.")
                if (descriptor.headsetFeature == null) {
                    add("No headset feature unit or disable control was found; HID consumer report 0x01 remains passive.")
                }
            }
            com.weekssa.opraeqforuapp.domain.ew300.Ew300UacStatus(
                uacVersion = descriptor.uacVersion,
                currentSampleRateHz = rate,
                sampleRatePath = ratePath,
                microphoneMuteSupported = mic != null && controlInterface != null,
                microphoneMuted = muted,
                headsetFeaturePresent = descriptor.headsetFeature != null,
                headsetRemoteSwitchSupported = false,
                uacModeSwitchSupported = false,
                uacModeLabel = descriptor.uacVersion,
                descriptorSummary = "AC interface " + (descriptor.audioControlInterface ?: "?") +
                    " · " + descriptor.streams.size + " streaming interface(s) · " +
                    descriptor.featureUnits.size + " feature unit(s)",
                blockedReasons = reasons,
            )
        }

    override suspend fun setMicrophoneMuted(muted: Boolean): Boolean = withContext(Dispatchers.IO) {
        val descriptor = cachedUacDescriptor
            ?: com.weekssa.opraeqforuapp.domain.ew300.Ew300UacDescriptorParser.parse(
                hid.configurationDescriptor() ?: return@withContext false,
            )
            ?: return@withContext false
        val mic = descriptor.microphoneFeature ?: return@withContext false
        val controlInterface = descriptor.audioControlInterface ?: return@withContext false
        val request = com.weekssa.opraeqforuapp.domain.ew300.Ew300UacRequests.microphoneMuteSet(
            unitId = mic.unitId,
            audioControlInterface = controlInterface,
            muted = muted,
        )
        if (!hid.controlTransferOutOnInterface(
                controlInterface,
                null,
                request.requestType,
                request.request,
                request.value,
                request.index,
                request.data ?: byteArrayOf(if (muted) 1 else 0),
            )
        ) {
            return@withContext false
        }
        val verify = com.weekssa.opraeqforuapp.domain.ew300.Ew300UacRequests.microphoneMuteGet(
            mic.unitId,
            controlInterface,
        )
        val value = hid.controlTransferInOnInterface(
            controlInterface,
            null,
            verify.requestType,
            verify.request,
            verify.value,
            verify.index,
            verify.length,
        ) ?: return@withContext false
        val actual = value.firstOrNull()?.let { (it.toInt() and 0x01) == 1 } ?: return@withContext false
        actual == muted
    }

    override fun close() = hid.close()
}

/** Exact EW300 fingerprint boundary; VID/PID alone is intentionally insufficient. */
private object Ew300UsbIdentity {
    private const val EXPECTED_MANUFACTURER = "LE XIAN"
    private const val EXPECTED_PRODUCT = "SIMGOT EW300 DSP"
    private const val EXPECTED_INTERFACE_ID = 3
    private const val EXPECTED_PACKET_SIZE = 16

    fun matches(device: UsbDevice): Boolean {
        val manufacturer = runCatching { device.manufacturerName }.getOrNull()
        val product = runCatching { device.productName }.getOrNull()
        return manufacturer.equals(EXPECTED_MANUFACTURER, ignoreCase = true) &&
            product.equals(EXPECTED_PRODUCT, ignoreCase = true)
    }

    fun matchesHidInterface(usbInterface: UsbInterface): Boolean {
        if (usbInterface.id != EXPECTED_INTERFACE_ID) return false
        return (0 until usbInterface.endpointCount)
            .map(usbInterface::getEndpoint)
            .filter { it.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT }
            .any { it.direction == android.hardware.usb.UsbConstants.USB_DIR_IN && it.maxPacketSize == EXPECTED_PACKET_SIZE } &&
            (0 until usbInterface.endpointCount)
                .map(usbInterface::getEndpoint)
                .filter { it.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT }
                .any { it.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT && it.maxPacketSize == EXPECTED_PACKET_SIZE }
    }
}
