package com.weekssa.opraeqforuapp.data.kt02h20

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Parcelable
import android.os.Looper
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Proxy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowUsbManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidKt02h20HidSessionTest {
    private lateinit var context: Context
    private lateinit var usbManager: UsbManager
    private lateinit var usbShadow: ShadowUsbManager
    private val requestedPermissions = mutableListOf<PendingIntent>()
    private var session: AndroidKt02h20HidSession? = null

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        usbShadow = Shadows.shadowOf(usbManager)
        requestedPermissions.clear()
    }

    @After
    fun tearDown() {
        session?.close()
        session = null
    }

    @Test
    fun sharedTransportLatePermissionGrantAfterRetryFallbackOpensFreshDescriptorOnce() = runBlocking {
        val deviceName = "/dev/bus/usb/001/003"
        val original = usbDevice(deviceName, productId = 0x0102, serial = "original-serial", interfaceId = 4)
        usbShadow.addOrUpdateUsbDevice(original, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = false,
            permissionResponseTimeoutMillis = 20,
            permissionPromptMaxDurationMillis = 400,
        )

        hid.connect()
        val permissionResult = requestedPermissions.single()
        val pendingState = awaitState {
            it is Kt02h20ConnectionState.PermissionRequired && it.retryAvailable
        }
        assertThat(pendingState).isInstanceOf(Kt02h20ConnectionState.PermissionRequired::class.java)

        // The callback device itself can be an obsolete descriptor. It must not start an open.
        val wrongDevice = usbDevice(
            "/dev/bus/usb/001/099",
            productId = original.productId,
            serial = "wrong-serial",
            interfaceId = 9,
        )
        usbShadow.addOrUpdateUsbDevice(wrongDevice, false)
        deliverPermission(permissionResult, wrongDevice, granted = true)
        delay(25)
        assertThat(hid.state.value).isEqualTo(pendingState)
        assertThat(hid.sessionGeneration).isEqualTo(0L)

        // Android re-enumerated the same endpoint while its permission prompt was open. The
        // callback carries the old descriptor, while UsbManager now exposes the replacement.
        val replacement = usbDevice(deviceName, productId = original.productId, serial = "replacement-serial", interfaceId = 7)
        usbShadow.addOrUpdateUsbDevice(replacement, true)
        deliverPermission(permissionResult, original, granted = true)

        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
        assertThat(hid.deviceFingerprintKey).contains("interface=7")

        // The exact same PendingIntent can be delivered twice by a queued platform callback.
        deliverPermission(permissionResult, original, granted = true)
        delay(25)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
        assertThat(hid.deviceFingerprintKey).contains("interface=7")
        assertThat(requestedPermissions).hasSize(1)
    }

    @Test
    fun ja11PromptRemainsWaitingPastFallbackUntilGrantOrHardTimeout() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/013", 0x0102, "ja11-pending", 2)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 20,
            permissionPromptMaxDurationMillis = 400,
        )

        hid.connect()
        val permissionResult = requestedPermissions.single()
        val waitingState = awaitState {
            it is Kt02h20ConnectionState.PermissionRequired && !it.retryAvailable
        }
        hid.connect()
        assertThat(requestedPermissions).hasSize(1)

        usbShadow.addOrUpdateUsbDevice(device, true)
        deliverPermission(permissionResult, device, granted = true)

        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
        assertThat(waitingState).isInstanceOf(Kt02h20ConnectionState.PermissionRequired::class.java)
    }

    @Test
    fun explicitRetrySupersedesTheStillEligibleLatePermissionAttempt() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/014", 0x0102, "retry", 2)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = false,
            permissionResponseTimeoutMillis = 20,
            permissionPromptMaxDurationMillis = 400,
        )

        hid.connect()
        val oldResult = requestedPermissions.single()
        awaitState { it is Kt02h20ConnectionState.PermissionRequired && it.retryAvailable }
        hid.connect()
        val newResult = requestedPermissions.last()
        assertThat(requestedPermissions).hasSize(2)
        assertThat(oldResult).isNotEqualTo(newResult)

        // A stale result from the superseded request cannot open the new attempt while Android
        // still reports that permission has not been granted.
        deliverPermission(oldResult, device, granted = true)
        delay(25)
        assertThat(hid.sessionGeneration).isEqualTo(0L)

        grantUsbPermission(device)
        deliverPermission(newResult, device, granted = true)

        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
    }

    @Test
    fun permissionCallbackFromClosedSessionCannotConsumeRecreatedSessionAttemptWithReusedAttemptId() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/016", 0x0102, "recreated", 2)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val oldSession = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 1_000,
            permissionPromptMaxDurationMillis = 4_000,
        )
        oldSession.connect()
        val stalePermissionResult = requestedPermissions.single()
        oldSession.close()

        val newSession = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 1_000,
            permissionPromptMaxDurationMillis = 4_000,
        )
        newSession.connect()
        val currentPermissionResult = requestedPermissions.last()
        assertThat(requestedPermissions).hasSize(2)
        assertThat(currentPermissionResult).isNotEqualTo(stalePermissionResult)

        // The old tracker and new tracker both start at attempt 1. Its old denial must not be
        // routed to the new receiver or turn the replacement attempt into a denial.
        deliverPermission(stalePermissionResult, device, granted = false)
        delay(25)
        assertThat(newSession.state.value).isEqualTo(Kt02h20ConnectionState.Connecting)
        assertThat(newSession.sessionGeneration).isEqualTo(0L)

        grantUsbPermission(device)
        deliverPermission(currentPermissionResult, device, granted = true)
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(newSession.sessionGeneration).isEqualTo(1L)
    }

    @Test
    fun sharedTransportLatePermissionGrantRemainsEligibleAfterRetryFallback() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/004", 0x0102, "late", 2)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = false,
            permissionResponseTimeoutMillis = 20,
            permissionPromptMaxDurationMillis = null,
        )

        hid.connect()
        val permissionResult = requestedPermissions.single()
        awaitState {
            it is Kt02h20ConnectionState.PermissionRequired && it.retryAvailable
        }
        delay(100)
        grantUsbPermission(device)
        deliverPermission(permissionResult, device, granted = true)
        delay(25)

        assertThat(hid.state.value).isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
        assertThat(requestedPermissions).hasSize(1)
    }

    @Test
    fun ja11LatePermissionGrantAfterHardDeadlineRequiresANewConnectAttempt() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/015", 0x0102, "ja11-expired", 2)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 20,
            permissionPromptMaxDurationMillis = 80,
        )

        hid.connect()
        val permissionResult = requestedPermissions.single()
        awaitState {
            it is Kt02h20ConnectionState.PermissionRequired && !it.retryAvailable
        }
        delay(90)
        awaitState {
            it is Kt02h20ConnectionState.PermissionRequired && it.retryAvailable
        }
        usbShadow.addOrUpdateUsbDevice(device, true)
        deliverPermission(permissionResult, device, granted = true)
        delay(25)

        assertThat(hid.sessionGeneration).isEqualTo(0L)
        hid.connect()
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
    }

    @Test
    fun detachInvalidatesPendingPermissionAndLateGrantCannotReopenTheDevice() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/005", 0x0102, "detached", 2)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 100,
            permissionPromptMaxDurationMillis = 400,
        )

        hid.connect()
        val permissionResult = requestedPermissions.single()
        usbShadow.removeUsbDevice(device)
        context.sendBroadcast(
            Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                .setPackage(context.packageName)
                .putExtra(UsbManager.EXTRA_DEVICE, device),
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(awaitState { it === Kt02h20ConnectionState.Disconnected })
            .isSameInstanceAs(Kt02h20ConnectionState.Disconnected)
        assertThat(hid.detachGeneration).isEqualTo(1L)

        usbShadow.addOrUpdateUsbDevice(device, true)
        deliverPermission(permissionResult, device, granted = true)
        delay(25)
        assertThat(hid.state.value).isSameInstanceAs(Kt02h20ConnectionState.Disconnected)
        assertThat(hid.sessionGeneration).isEqualTo(0L)

        hid.connect()
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
    }

    @Test
    fun replacementOpenIsRetiredWhenAnotherDetachAdvancesItsGeneration() = runBlocking {
        val deviceName = "/dev/bus/usb/001/015"
        val device = usbDevice(deviceName, 0x0102, "overlap", 2)
        usbShadow.addOrUpdateUsbDevice(device, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 800,
        )

        hid.connect()
        awaitState { it === Kt02h20ConnectionState.Connected }

        val sessionMutexField = AndroidKt02h20HidSession::class.java.getDeclaredField("mutex")
            .apply { isAccessible = true }
        val sessionMutex = sessionMutexField.get(hid) as Mutex
        sessionMutex.lock()
        try {
            usbShadow.removeUsbDevice(device)
            context.sendBroadcast(
                Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                    .setPackage(context.packageName)
                    .putExtra(UsbManager.EXTRA_DEVICE, device),
            )
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertThat(hid.detachGeneration).isEqualTo(1L)

            // Hold old-session close while a replacement open is queued against generation 1.
            usbShadow.addOrUpdateUsbDevice(device, true)
            hid.connect()
            delay(25)

            // A second detach can arrive before the stale UsbSession pointer is closed. It
            // advances the generation and must retire the already queued replacement attempt.
            usbShadow.removeUsbDevice(device)
            context.sendBroadcast(
                Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                    .setPackage(context.packageName)
                    .putExtra(UsbManager.EXTRA_DEVICE, device),
            )
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertThat(hid.detachGeneration).isEqualTo(2L)

            usbShadow.addOrUpdateUsbDevice(device, true)
            hid.connect()
        } finally {
            sessionMutex.unlock()
        }

        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(2L)
        assertThat(hid.detachGeneration).isEqualTo(2L)
    }

    @Test
    fun terminalCancellationStaysInErrorAndRejectsThePromptResult() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/006", 0x0102, "terminal", 2)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 800,
        )

        hid.connect()
        val permissionResult = requestedPermissions.single()
        hid.cancelPendingConnectAttempt("The JA11 change could not be verified.")
        val terminalState = hid.state.value
        assertThat(terminalState)
            .isEqualTo(Kt02h20ConnectionState.Error("The JA11 change could not be verified."))

        usbShadow.addOrUpdateUsbDevice(device, true)
        deliverPermission(permissionResult, device, granted = true)
        delay(25)

        assertThat(hid.state.value).isEqualTo(terminalState)
        assertThat(hid.sessionGeneration).isEqualTo(0L)
        assertThat(requestedPermissions).hasSize(1)
    }

    @Test
    fun terminalCancellationWithoutAnAttemptBlocksQueuedAutomaticReconnect() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/007", 0x0102, "no-attempt", 2)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 800,
        )

        assertThat(hid.state.value).isSameInstanceAs(Kt02h20ConnectionState.Disconnected)
        hid.cancelPendingConnectAttempt("The JA11 change could not be verified.")

        assertThat(hid.state.value)
            .isEqualTo(Kt02h20ConnectionState.Error("The JA11 change could not be verified."))
        hid.connectAutomatically()
        delay(25)

        assertThat(hid.state.value)
            .isEqualTo(Kt02h20ConnectionState.Error("The JA11 change could not be verified."))
        assertThat(hid.sessionGeneration).isEqualTo(0L)
        assertThat(requestedPermissions).isEmpty()
    }

    private fun newSession(
        blockRetryWhilePermissionPending: Boolean,
        permissionResponseTimeoutMillis: Long,
        permissionPromptMaxDurationMillis: Long?,
    ) = AndroidKt02h20HidSession(
        context = context,
        vendorId = 0x2972,
        productIds = setOf(0x0102, 0x0103),
        deviceLabel = "Test JA11",
        permissionSuffix = "TEST",
        blockRetryWhilePermissionPending = blockRetryWhilePermissionPending,
        permissionRequester = { _, pendingIntent -> requestedPermissions += pendingIntent },
        permissionResponseTimeoutMillis = permissionResponseTimeoutMillis,
        permissionPromptMaxDurationMillis = permissionPromptMaxDurationMillis,
    ).also { session = it }

    private suspend fun awaitState(
        predicate: (Kt02h20ConnectionState) -> Boolean,
    ): Kt02h20ConnectionState = withTimeout(2_000) {
        session!!.state.first(predicate)
    }

    private fun deliverPermission(
        pendingIntent: PendingIntent,
        device: UsbDevice,
        granted: Boolean,
    ) {
        pendingIntent.send(
            context,
            0,
            Intent()
                .putExtra(UsbManager.EXTRA_DEVICE, device)
                .putExtra(UsbManager.EXTRA_PERMISSION_GRANTED, granted),
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun grantUsbPermission(device: UsbDevice) {
        ShadowUsbManager::class.java
            .getDeclaredMethod("grantPermission", UsbDevice::class.java)
            .apply { isAccessible = true }
            .invoke(usbShadow, device)
    }

    private fun usbDevice(
        deviceName: String,
        productId: Int,
        serial: String,
        interfaceId: Int,
    ): UsbDevice {
        val endpointConstructor = constructorWithParameters(UsbEndpoint::class.java, 4)
            .apply { isAccessible = true }
        val endpointIn = endpointConstructor.newInstance(
            UsbConstants.USB_DIR_IN or 1,
            UsbConstants.USB_ENDPOINT_XFER_INT,
            64,
            1,
        ) as UsbEndpoint
        val endpointOut = endpointConstructor.newInstance(
            UsbConstants.USB_DIR_OUT or 1,
            UsbConstants.USB_ENDPOINT_XFER_INT,
            64,
            1,
        ) as UsbEndpoint

        val interfaceConstructor = constructorWithParameters(UsbInterface::class.java, 6)
            .apply { isAccessible = true }
        val usbInterface = interfaceConstructor.newInstance(
            interfaceId,
            0,
            "JA11 PEQ HID",
            UsbConstants.USB_CLASS_HID,
            0,
            0,
        ) as UsbInterface
        UsbInterface::class.java.getDeclaredMethod("setEndpoints", Array<Parcelable>::class.java).apply {
            isAccessible = true
        }.invoke(usbInterface, arrayOf<Parcelable>(endpointIn, endpointOut))

        val configurationConstructor = constructorWithParameters(UsbConfiguration::class.java, 4)
            .apply { isAccessible = true }
        val configuration = configurationConstructor.newInstance(1, "JA11", 0x80, 100) as UsbConfiguration
        UsbConfiguration::class.java.getDeclaredMethod("setInterfaces", Array<Parcelable>::class.java).apply {
            isAccessible = true
        }.invoke(configuration, arrayOf<Parcelable>(usbInterface))

        val constructor = constructorWithParameters(UsbDevice::class.java, 16)
            .apply { isAccessible = true }
        val serialReaderType = constructor.parameterTypes[10]
        val serialReader = Proxy.newProxyInstance(
            serialReaderType.classLoader,
            arrayOf(serialReaderType),
        ) { proxy, method, args ->
            when (method.name) {
                "getSerialNumber" -> serial
                "asBinder" -> null
                "toString" -> "TestUsbSerialReader($serial)"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> null
            }
        }
        return constructor.newInstance(
            deviceName,
            0x2972,
            productId,
            0,
            0,
            0,
            "FiiO",
            "JA11",
            "2.20",
            arrayOf(configuration),
            serialReader,
            false,
            false,
            false,
            false,
            false,
        ) as UsbDevice
    }

    private fun constructorWithParameters(type: Class<*>, count: Int) =
        type.declaredConstructors.singleOrNull { it.parameterCount == count }
            ?: error("${type.name} constructors: ${type.declaredConstructors.joinToString { it.parameterTypes.joinToString(prefix = "(", postfix = ")") { p -> p.simpleName } }}")
}
