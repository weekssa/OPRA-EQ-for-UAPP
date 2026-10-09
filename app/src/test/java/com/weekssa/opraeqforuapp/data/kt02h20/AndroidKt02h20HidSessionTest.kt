package com.weekssa.opraeqforuapp.data.kt02h20

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Parcelable
import android.os.Looper
import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11SessionToken
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11ReportWriteOutcome
import com.weekssa.opraeqforuapp.domain.kt02h20.fiioJa11SerialIdentity
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
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

    @Test
    fun ja11SaveReconnectDeadlineCoversReconnectAndPermissionPromptWindows() {
        assertThat(JA11_SAVE_RECONNECT_TIMEOUT_MILLIS)
            .isAtLeast(JA11_PERMISSION_PROMPT_MAX_DURATION_MILLIS + 20_000L)
    }

    @Test
    fun openedConnectionSerialCanFillOnlyTheMissingJa11DeviceSerial() = runBlocking {
        val device = usbDevice(
            deviceName = "/dev/bus/usb/001/007",
            productId = 0x0102,
            serial = null,
            interfaceId = 3,
        )
        usbShadow.addOrUpdateUsbDevice(device, true)
        var connectionSerialReads = 0
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            allowConnectionSerialFallback = true,
            deviceSerialReader = { null },
            connectionSerialReader = {
                connectionSerialReads += 1
                "connection-only-serial"
            },
        )

        hid.connect()

        assertThat(awaitState { it is Kt02h20ConnectionState.Connected })
            .isEqualTo(Kt02h20ConnectionState.Connected)
        assertThat(connectionSerialReads).isEqualTo(1)
        assertThat(hid.deviceSerialIdentity).isEqualTo("connection-only-serial")
        assertThat(fiioJa11SerialIdentity(hid.deviceFingerprintKey))
            .isEqualTo("connection-only-serial")
    }

    @Test
    fun optionalSaveReconnectWaitsForDetachAfterTheFormer350MillisecondWindow() = runBlocking {
        val deviceName = "/dev/bus/usb/001/078"
        val original = usbDevice(deviceName, 0x0102, null, 3)
        val replacement = usbDevice(deviceName, 0x0102, null, 3)
        usbShadow.addOrUpdateUsbDevice(original, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        val previousGeneration = hid.sessionGeneration
        val previousDetachGeneration = hid.detachGeneration
        val transactionToken = "late-save-detach-test"
        assertThat(hid.setExpectedRestartTransactionToken(transactionToken)).isTrue()

        val reconnect = async {
            hid.awaitOptionalReconnectAfterMutation(
                previousGeneration = previousGeneration,
                previousDetachGeneration = previousDetachGeneration,
                observationMillis = JA11_SAVE_REENUMERATION_OBSERVATION_MILLIS,
                timeoutMillis = 2_000,
            )
        }
        delay(500)
        assertThat(reconnect.isCompleted).isFalse()

        usbShadow.removeUsbDevice(original)
        context.sendBroadcast(
            Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                .setPackage(context.packageName)
                .putExtra(UsbManager.EXTRA_DEVICE, original),
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(awaitState { it === Kt02h20ConnectionState.Disconnected })
            .isSameInstanceAs(Kt02h20ConnectionState.Disconnected)
        delay(25)

        usbShadow.addOrUpdateUsbDevice(replacement, true)
        context.sendBroadcast(
            Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                .setPackage(context.packageName)
                .putExtra(UsbManager.EXTRA_DEVICE, replacement),
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        hid.connectAutomatically()
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)

        assertThat(withTimeout(2_000) { reconnect.await() }).isTrue()
        assertThat(hid.sessionGeneration).isGreaterThan(previousGeneration)
        assertThat(hid.detachGeneration).isGreaterThan(previousDetachGeneration)
        hid.clearExpectedRestartTransactionToken(transactionToken)
    }

    @Test
    fun openedConnectionSerialWithFingerprintDelimitersRemainsExactInSessionToken() = runBlocking {
        val device = usbDevice(
            deviceName = "/dev/bus/usb/001/040",
            productId = 0x0102,
            serial = null,
            interfaceId = 3,
        )
        usbShadow.addOrUpdateUsbDevice(device, true)
        val expectedSerial = "ja11|serial=with-delimiters"
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            allowConnectionSerialFallback = true,
            deviceSerialReader = { null },
            connectionSerialReader = { expectedSerial },
        )

        hid.connect()

        assertThat(awaitState { it is Kt02h20ConnectionState.Connected })
            .isEqualTo(Kt02h20ConnectionState.Connected)
        val sessionToken = FiioJa11SessionToken(
            deviceFingerprintKey = checkNotNull(hid.deviceFingerprintKey),
            usbProductId = 0x0102,
            sessionGeneration = hid.sessionGeneration,
            detachGeneration = hid.detachGeneration,
            deviceSerialIdentity = hid.deviceSerialIdentity,
        )
        assertThat(sessionToken.deviceSerialIdentity).isEqualTo(expectedSerial)
    }

    @Test
    fun uniqueJa11SessionRejectsTwoSeriallessCandidatesAcrossSupportedPids() = runBlocking {
        val uacOne = usbDevice("/dev/bus/usb/001/030", 0x0101, null, 2)
        val uacTwo = usbDevice("/dev/bus/usb/001/031", 0x0102, null, 3)
        usbShadow.addOrUpdateUsbDevice(uacOne, true)
        usbShadow.addOrUpdateUsbDevice(uacTwo, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()

        assertThat(hid.present.value).isTrue()
        assertThat(hid.targetDeviceCount).isEqualTo(2)
        assertThat(hid.state.value).isInstanceOf(Kt02h20ConnectionState.Error::class.java)
        assertThat(hid.sessionGeneration).isEqualTo(0L)
        assertThat(requestedPermissions).isEmpty()
    }

    @Test
    fun duplicateSerialValuesDoNotCollapseTwoJa11Candidates() = runBlocking {
        val uacOne = usbDevice("/dev/bus/usb/001/032", 0x0101, "duplicate", 2)
        val uacTwo = usbDevice("/dev/bus/usb/001/033", 0x0102, "duplicate", 3)
        usbShadow.addOrUpdateUsbDevice(uacOne, true)
        usbShadow.addOrUpdateUsbDevice(uacTwo, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()

        assertThat(fiioJa11SerialIdentity(hid.deviceFingerprintKey)).isNull()
        assertThat(hid.targetDeviceCount).isEqualTo(2)
        assertThat(hid.state.value).isInstanceOf(Kt02h20ConnectionState.Error::class.java)
        assertThat(hid.sessionGeneration).isEqualTo(0L)
        assertThat(requestedPermissions).isEmpty()
    }

    @Test
    fun oneJa11PlusUnrelatedFiioDeviceKeepsJa11CandidateUnambiguous() = runBlocking {
        val ja11 = usbDevice("/dev/bus/usb/001/034", 0x0102, null, 3)
        val unrelatedFiio = usbDevice("/dev/bus/usb/001/035", 0x0201, null, 4)
        usbShadow.addOrUpdateUsbDevice(ja11, true)
        usbShadow.addOrUpdateUsbDevice(unrelatedFiio, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()

        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.targetDeviceCount).isEqualTo(1)
        assertThat(hid.connectedProductId).isEqualTo(0x0102)
        assertThat(fiioJa11SerialIdentity(hid.deviceFingerprintKey)).isNull()
    }

    @Test
    fun secondJa11AppearingWhilePermissionIsPendingBlocksDescriptorOpen() = runBlocking {
        val first = usbDevice("/dev/bus/usb/001/036", 0x0102, null, 3)
        val second = usbDevice("/dev/bus/usb/001/037", 0x0101, null, 2)
        usbShadow.addOrUpdateUsbDevice(first, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 1_000,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()
        val permissionResult = requestedPermissions.single()
        usbShadow.addOrUpdateUsbDevice(second, true)
        grantUsbPermission(first)
        deliverPermission(permissionResult, first, granted = true)
        delay(25)

        assertThat(hid.targetDeviceCount).isEqualTo(2)
        assertThat(hid.state.value).isInstanceOf(Kt02h20ConnectionState.Error::class.java)
        assertThat(hid.sessionGeneration).isEqualTo(0L)
        assertThat(requestedPermissions).hasSize(1)
    }

    @Test
    fun secondJa11AppearingAfterOpenInvalidatesPinnedReadsAndWritesBeforeTransfer() = runBlocking {
        val first = usbDevice("/dev/bus/usb/001/043", 0x0102, null, 3)
        val second = usbDevice("/dev/bus/usb/001/044", 0x0101, null, 2)
        usbShadow.addOrUpdateUsbDevice(first, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()

        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        val generation = hid.sessionGeneration
        val detachGeneration = hid.detachGeneration
        usbShadow.addOrUpdateUsbDevice(second, true)

        assertThat(hid.targetDeviceCount).isEqualTo(2)
        assertThat(hid.state.value).isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.isCurrentSession(generation, detachGeneration)).isFalse()
        assertThat(
            hid.sendJa11Report(
                report = FiioJa11Protocol.writeHeadsetControlReport(enabled = false),
                expectedGeneration = generation,
                expectedDetachGeneration = detachGeneration,
                settleMillis = 0,
            ),
        ).isEqualTo(FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND)
        assertThat(
            hid.exchangeJa11(
                report = FiioJa11Protocol.readGlobalGainReport(),
                expectedGeneration = generation,
                expectedDetachGeneration = detachGeneration,
                minResponseBytes = 7,
                timeoutMillis = 1,
            ),
        ).isNull()
    }

    @Test
    fun seriallessSamePathAttachBeforeDetachInvalidatesTheLiveJa11Session() = runBlocking {
        val deviceName = "/dev/bus/usb/001/047"
        val original = usbDevice(deviceName, 0x0102, null, 3)
        val replacement = usbDevice(deviceName, 0x0102, null, 3)
        usbShadow.addOrUpdateUsbDevice(original, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        val generation = hid.sessionGeneration
        val detachGeneration = hid.detachGeneration
        assertThat(hid.deviceSerialIdentity).isNull()

        usbShadow.removeUsbDevice(original)
        usbShadow.addOrUpdateUsbDevice(replacement, true)
        context.sendBroadcast(
            Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                .setPackage(context.packageName)
                .putExtra(UsbManager.EXTRA_DEVICE, replacement),
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(hid.targetDeviceCount).isEqualTo(1)
        assertThat(hid.detachGeneration).isEqualTo(detachGeneration)
        assertThat(hid.state.value).isInstanceOf(Kt02h20ConnectionState.Error::class.java)
        assertThat(hid.isCurrentSession(generation, detachGeneration)).isFalse()
        assertThat(
            hid.sendJa11Report(
                report = FiioJa11Protocol.writeHeadsetControlReport(enabled = false),
                expectedGeneration = generation,
                expectedDetachGeneration = detachGeneration,
                settleMillis = 0,
            ),
        ).isEqualTo(FiioJa11ReportWriteOutcome.STALE_BEFORE_SEND)
    }

    @Test
    fun delayedOldDetachDoesNotInvalidateCurrentSamePathSeriallessJa11Session() = runBlocking {
        val deviceName = "/dev/bus/usb/001/048"
        val original = usbDevice(deviceName, 0x0102, null, 3)
        val replacement = usbDevice(deviceName, 0x0102, null, 3)
        usbShadow.addOrUpdateUsbDevice(original, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        val oldGeneration = hid.sessionGeneration

        usbShadow.removeUsbDevice(original)
        usbShadow.addOrUpdateUsbDevice(replacement, true)
        context.sendBroadcast(
            Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                .setPackage(context.packageName)
                .putExtra(UsbManager.EXTRA_DEVICE, replacement),
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(awaitState { it is Kt02h20ConnectionState.Error })
            .isInstanceOf(Kt02h20ConnectionState.Error::class.java)

        hid.connect()
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        val replacementGeneration = hid.sessionGeneration
        val replacementDetachGeneration = hid.detachGeneration
        assertThat(replacementGeneration).isGreaterThan(oldGeneration)
        assertThat(replacementDetachGeneration).isEqualTo(0L)

        context.sendBroadcast(
            Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                .setPackage(context.packageName)
                .putExtra(UsbManager.EXTRA_DEVICE, original),
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        delay(25)

        assertThat(hid.state.value).isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(replacementGeneration)
        assertThat(hid.detachGeneration).isEqualTo(replacementDetachGeneration)
        assertThat(hid.isCurrentSession(replacementGeneration, replacementDetachGeneration)).isTrue()
    }

    @Test
    fun seriallessSamePathReplacementDuringOpenCannotPublishTheOlderConnection() = runBlocking {
        val deviceName = "/dev/bus/usb/001/049"
        val original = usbDevice(deviceName, 0x0102, null, 3)
        val replacement = usbDevice(deviceName, 0x0102, null, 3)
        val identityReadStarted = CountDownLatch(1)
        val allowIdentityRead = CountDownLatch(1)
        val serialReadCount = AtomicInteger()
        usbShadow.addOrUpdateUsbDevice(original, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            allowConnectionSerialFallback = true,
            requireUniqueTarget = true,
            deviceSerialReader = { null },
            connectionSerialReader = {
                if (serialReadCount.incrementAndGet() == 1) {
                    identityReadStarted.countDown()
                    check(allowIdentityRead.await(2, TimeUnit.SECONDS))
                    "opened-only-serial"
                } else {
                    "replacement-only-serial"
                }
            },
        )

        hid.connect()
        val identityReadReached = identityReadStarted.await(2, TimeUnit.SECONDS)
        if (identityReadReached) {
            usbShadow.removeUsbDevice(original)
            usbShadow.addOrUpdateUsbDevice(replacement, true)
            context.sendBroadcast(
                Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                    .setPackage(context.packageName)
                    .putExtra(UsbManager.EXTRA_DEVICE, replacement),
            )
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
        allowIdentityRead.countDown()

        assertThat(identityReadReached).isTrue()
        val completedState = awaitState {
            it is Kt02h20ConnectionState.Error || it === Kt02h20ConnectionState.Connected
        }
        assertThat(completedState).isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.targetDeviceCount).isEqualTo(1)
        assertThat(serialReadCount.get()).isEqualTo(2)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
        assertThat(hid.deviceSerialIdentity).isEqualTo("replacement-only-serial")
        assertThat(hid.isCurrentSession(1L, 0L)).isTrue()
    }

    @Test
    fun samePathReplacementWithDifferentSerialCannotCommitStaleOpenedConnection() = runBlocking {
        val deviceName = "/dev/bus/usb/001/045"
        val original = usbDevice(deviceName, 0x0102, "opened-unit", 3)
        val replacement = usbDevice(deviceName, 0x0102, "current-unit", 3)
        val identityReadStarted = CountDownLatch(1)
        val allowIdentityRead = CountDownLatch(1)
        val observedSerials = CopyOnWriteArrayList<String?>()
        val serialReadCount = AtomicInteger()
        usbShadow.addOrUpdateUsbDevice(original, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
            deviceSerialReader = {
                val serial = if (serialReadCount.incrementAndGet() == 1) {
                    identityReadStarted.countDown()
                    check(allowIdentityRead.await(2, TimeUnit.SECONDS))
                    "opened-unit"
                } else {
                    "current-unit"
                }
                serial.also(observedSerials::add)
            },
        )

        hid.connect()
        val identityReadReached = identityReadStarted.await(2, TimeUnit.SECONDS)
        if (identityReadReached) {
            usbShadow.removeUsbDevice(original)
            usbShadow.addOrUpdateUsbDevice(replacement, true)
        }
        allowIdentityRead.countDown()

        assertThat(identityReadReached).isTrue()
        val completedState = awaitState {
            it is Kt02h20ConnectionState.Error || it === Kt02h20ConnectionState.Connected
        }
        assertThat(observedSerials).containsExactly("opened-unit", "current-unit").inOrder()
        assertThat(completedState)
            .isInstanceOf(Kt02h20ConnectionState.Error::class.java)
        assertThat(hid.targetDeviceCount).isEqualTo(1)
        assertThat(hid.sessionGeneration).isEqualTo(0L)
        assertThat(hid.isCurrentSession(1L, 0L)).isFalse()
    }

    @Test
    fun clearedRestartTokenCancelsItsDelayedPermissionOpen() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/046", 0x0102, null, 3)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 1_000,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )
        assertThat(hid.setExpectedRestartTransactionToken("ja11-txn-old")).isTrue()

        hid.connect()

        val stalePermissionRequest = requestedPermissions.single()
        hid.clearExpectedRestartTransactionToken("ja11-txn-old")
        assertThat(hid.state.value).isSameInstanceAs(Kt02h20ConnectionState.Disconnected)
        grantUsbPermission(device)
        deliverPermission(stalePermissionRequest, device, granted = true)
        delay(25)

        assertThat(hid.sessionGeneration).isEqualTo(0L)
        assertThat(hid.state.value).isSameInstanceAs(Kt02h20ConnectionState.Disconnected)

        // A new, explicit connection starts a fresh unbound attempt and may open normally.
        hid.connect()
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
    }

    @Test
    fun permissionDenialFromAnOlderTransactionDoesNotChangeCurrentStatus() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/047", 0x0102, null, 3)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 1_000,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )
        assertThat(hid.setExpectedRestartTransactionToken("older-transaction")).isTrue()

        hid.connect()

        val staleRequest = requestedPermissions.single()
        hid.clearExpectedRestartTransactionToken("older-transaction")
        assertThat(hid.state.value).isSameInstanceAs(Kt02h20ConnectionState.Disconnected)
        assertThat(hid.setExpectedRestartTransactionToken("newer-transaction")).isTrue()

        hid.connect()
        val currentRequest = requestedPermissions.last()
        assertThat(currentRequest).isNotEqualTo(staleRequest)

        deliverPermission(staleRequest, device, granted = false)
        assertThat(hid.state.value).isSameInstanceAs(Kt02h20ConnectionState.Connecting)
        assertThat(hid.sessionGeneration).isEqualTo(0L)

        grantUsbPermission(device)
        deliverPermission(currentRequest, device, granted = true)
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
    }

    @Test
    fun transactionCannotClaimAnUnrelatedPendingUsbPermissionAttempt() = runBlocking {
        val device = usbDevice("/dev/bus/usb/001/048", 0x0102, null, 3)
        usbShadow.addOrUpdateUsbDevice(device, false)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 1_000,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()

        assertThat(hid.setExpectedRestartTransactionToken("unrelated-transaction")).isFalse()
        val request = requestedPermissions.single()
        deliverPermission(request, device, granted = false)

        assertThat(hid.state.value)
            .isInstanceOf(Kt02h20ConnectionState.PermissionRequired::class.java)
        assertThat(hid.sessionGeneration).isEqualTo(0L)
    }

    @Test
    fun unsolicitedJa11ReconnectStartsAFreshSessionWithoutPendingRestartTransaction() = runBlocking {
        val first = usbDevice("/dev/bus/usb/001/041", 0x0102, null, 3)
        val replacement = usbDevice("/dev/bus/usb/001/042", 0x0102, null, 2)
        usbShadow.addOrUpdateUsbDevice(first, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()
        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(1L)
        assertThat(hid.deviceSerialIdentity).isNull()

        usbShadow.removeUsbDevice(first)
        context.sendBroadcast(
            Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                .setPackage(context.packageName)
                .putExtra(UsbManager.EXTRA_DEVICE, first),
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(awaitState { it === Kt02h20ConnectionState.Disconnected })
            .isSameInstanceAs(Kt02h20ConnectionState.Disconnected)
        assertThat(hid.detachGeneration).isEqualTo(1L)

        usbShadow.addOrUpdateUsbDevice(replacement, true)
        hid.connectAutomatically()

        assertThat(awaitState { it === Kt02h20ConnectionState.Connected })
            .isSameInstanceAs(Kt02h20ConnectionState.Connected)
        assertThat(hid.sessionGeneration).isEqualTo(2L)
        assertThat(hid.detachGeneration).isEqualTo(1L)
        assertThat(hid.targetDeviceCount).isEqualTo(1)
        assertThat(hid.deviceSerialIdentity).isNull()
        assertThat(hid.isCurrentSession(2L, 1L)).isTrue()
    }

    @Test
    fun uniqueJa11SessionRejectsMultipleUsableHidInterfacesWithoutChoosingFirst() = runBlocking {
        val device = usbDevice(
            deviceName = "/dev/bus/usb/001/038",
            productId = 0x0102,
            serial = null,
            interfaceId = 2,
            additionalInterfaceIds = listOf(3),
        )
        usbShadow.addOrUpdateUsbDevice(device, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()

        assertThat(awaitState { it is Kt02h20ConnectionState.Error })
            .isInstanceOf(Kt02h20ConnectionState.Error::class.java)
        assertThat(hid.targetDeviceCount).isEqualTo(1)
        assertThat(hid.sessionGeneration).isEqualTo(0L)
    }

    @Test
    fun uniqueJa11SessionRejectsMultipleMatchingInterruptEndpointsWithoutChoosingFirst() = runBlocking {
        val device = usbDevice(
            deviceName = "/dev/bus/usb/001/039",
            productId = 0x0102,
            serial = null,
            interfaceId = 2,
            additionalInputEndpoint = true,
        )
        usbShadow.addOrUpdateUsbDevice(device, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            requireUniqueTarget = true,
        )

        hid.connect()

        assertThat(awaitState { it is Kt02h20ConnectionState.Error })
            .isInstanceOf(Kt02h20ConnectionState.Error::class.java)
        assertThat(hid.targetDeviceCount).isEqualTo(1)
        assertThat(hid.sessionGeneration).isEqualTo(0L)
    }

    @Test
    fun connectionSerialFallbackIsNotUsedWhenJa11DeviceSerialIsPresent() = runBlocking {
        val device = usbDevice(
            deviceName = "/dev/bus/usb/001/008",
            productId = 0x0102,
            serial = "device-serial",
            interfaceId = 3,
        )
        usbShadow.addOrUpdateUsbDevice(device, true)
        var connectionSerialReads = 0
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            allowConnectionSerialFallback = true,
            deviceSerialReader = { "device-serial" },
            connectionSerialReader = {
                connectionSerialReads += 1
                "connection-only-serial"
            },
        )

        hid.connect()

        assertThat(awaitState { it is Kt02h20ConnectionState.Connected })
            .isEqualTo(Kt02h20ConnectionState.Connected)
        assertThat(connectionSerialReads).isEqualTo(0)
        assertThat(fiioJa11SerialIdentity(hid.deviceFingerprintKey))
            .isEqualTo("device-serial")
    }

    @Test
    fun connectionSerialFallbackIsDisabledByDefault() = runBlocking {
        val device = usbDevice(
            deviceName = "/dev/bus/usb/001/009",
            productId = 0x0102,
            serial = null,
            interfaceId = 3,
        )
        usbShadow.addOrUpdateUsbDevice(device, true)
        var connectionSerialReads = 0
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            deviceSerialReader = { null },
            connectionSerialReader = {
                connectionSerialReads += 1
                "connection-only-serial"
            },
        )

        hid.connect()

        assertThat(awaitState { it is Kt02h20ConnectionState.Connected })
            .isEqualTo(Kt02h20ConnectionState.Connected)
        assertThat(connectionSerialReads).isEqualTo(0)
        assertThat(fiioJa11SerialIdentity(hid.deviceFingerprintKey)).isNull()
    }

    @Test
    fun blankDeviceSerialDoesNotTriggerConnectionFallback() = runBlocking {
        val device = usbDevice(
            deviceName = "/dev/bus/usb/001/010",
            productId = 0x0102,
            serial = null,
            interfaceId = 3,
        )
        usbShadow.addOrUpdateUsbDevice(device, true)
        var connectionSerialReads = 0
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            allowConnectionSerialFallback = true,
            deviceSerialReader = { "  " },
            connectionSerialReader = {
                connectionSerialReads += 1
                "connection-only-serial"
            },
        )

        hid.connect()

        assertThat(awaitState { it is Kt02h20ConnectionState.Connected })
            .isEqualTo(Kt02h20ConnectionState.Connected)
        assertThat(connectionSerialReads).isEqualTo(0)
        assertThat(fiioJa11SerialIdentity(hid.deviceFingerprintKey)).isNull()
    }

    @Test
    fun blankConnectionSerialCannotEstablishJa11Identity() = runBlocking {
        val device = usbDevice(
            deviceName = "/dev/bus/usb/001/011",
            productId = 0x0102,
            serial = null,
            interfaceId = 3,
        )
        usbShadow.addOrUpdateUsbDevice(device, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            allowConnectionSerialFallback = true,
            deviceSerialReader = { null },
            connectionSerialReader = { "   " },
        )

        hid.connect()

        assertThat(awaitState { it is Kt02h20ConnectionState.Connected })
            .isEqualTo(Kt02h20ConnectionState.Connected)
        assertThat(fiioJa11SerialIdentity(hid.deviceFingerprintKey)).isNull()
    }

    @Test
    fun deviceSerialReadExceptionDoesNotTriggerConnectionFallback() = runBlocking {
        val device = usbDevice(
            deviceName = "/dev/bus/usb/001/012",
            productId = 0x0102,
            serial = null,
            interfaceId = 3,
        )
        usbShadow.addOrUpdateUsbDevice(device, true)
        var connectionSerialReads = 0
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            allowConnectionSerialFallback = true,
            deviceSerialReader = { throw SecurityException("permission denied") },
            connectionSerialReader = {
                connectionSerialReads += 1
                "connection-only-serial"
            },
        )

        hid.connect()

        assertThat(awaitState { it is Kt02h20ConnectionState.Connected })
            .isEqualTo(Kt02h20ConnectionState.Connected)
        assertThat(connectionSerialReads).isEqualTo(0)
        assertThat(fiioJa11SerialIdentity(hid.deviceFingerprintKey)).isNull()
    }

    @Test
    fun connectionSerialReadExceptionLeavesJa11IdentityUnavailable() = runBlocking {
        val device = usbDevice(
            deviceName = "/dev/bus/usb/001/013",
            productId = 0x0102,
            serial = null,
            interfaceId = 3,
        )
        usbShadow.addOrUpdateUsbDevice(device, true)
        val hid = newSession(
            blockRetryWhilePermissionPending = true,
            permissionResponseTimeoutMillis = 500,
            permissionPromptMaxDurationMillis = 25_000,
            allowConnectionSerialFallback = true,
            deviceSerialReader = { null },
            connectionSerialReader = { throw SecurityException("connection serial unavailable") },
        )

        hid.connect()

        assertThat(awaitState { it is Kt02h20ConnectionState.Connected })
            .isEqualTo(Kt02h20ConnectionState.Connected)
        assertThat(fiioJa11SerialIdentity(hid.deviceFingerprintKey)).isNull()
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
        allowConnectionSerialFallback: Boolean = false,
        requireUniqueTarget: Boolean = false,
        deviceSerialReader: (UsbDevice) -> String? = { it.serialNumber },
        connectionSerialReader: (UsbDeviceConnection) -> String? = { it.serial },
    ) = AndroidKt02h20HidSession(
        context = context,
        vendorId = 0x2972,
        productIds = setOf(0x0101, 0x0102),
        deviceLabel = "Test JA11",
        permissionSuffix = "TEST",
        blockRetryWhilePermissionPending = blockRetryWhilePermissionPending,
        permissionRequester = { _, pendingIntent -> requestedPermissions += pendingIntent },
        permissionResponseTimeoutMillis = permissionResponseTimeoutMillis,
        permissionPromptMaxDurationMillis = permissionPromptMaxDurationMillis,
        allowConnectionSerialFallback = allowConnectionSerialFallback,
        requireUniqueTarget = requireUniqueTarget,
        deviceSerialReader = deviceSerialReader,
        connectionSerialReader = connectionSerialReader,
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
        serial: String?,
        interfaceId: Int,
        additionalInterfaceIds: List<Int> = emptyList(),
        additionalInputEndpoint: Boolean = false,
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
        val endpointInputSecond = if (additionalInputEndpoint) {
            endpointConstructor.newInstance(
                UsbConstants.USB_DIR_IN or 2,
                UsbConstants.USB_ENDPOINT_XFER_INT,
                64,
                1,
            ) as UsbEndpoint
        } else {
            null
        }

        val interfaceConstructor = constructorWithParameters(UsbInterface::class.java, 6)
            .apply { isAccessible = true }
        val usbInterfaces = (listOf(interfaceId) + additionalInterfaceIds).map { candidateInterfaceId ->
            val usbInterface = interfaceConstructor.newInstance(
                candidateInterfaceId,
                0,
                "JA11 PEQ HID",
                UsbConstants.USB_CLASS_HID,
                0,
                0,
            ) as UsbInterface
            UsbInterface::class.java.getDeclaredMethod("setEndpoints", Array<Parcelable>::class.java).apply {
                isAccessible = true
            }.invoke(
                usbInterface,
                listOfNotNull(endpointIn, endpointOut, endpointInputSecond).map { it as Parcelable }.toTypedArray(),
            )
            usbInterface
        }

        val configurationConstructor = constructorWithParameters(UsbConfiguration::class.java, 4)
            .apply { isAccessible = true }
        val configuration = configurationConstructor.newInstance(1, "JA11", 0x80, 100) as UsbConfiguration
        UsbConfiguration::class.java.getDeclaredMethod("setInterfaces", Array<Parcelable>::class.java).apply {
            isAccessible = true
        }.invoke(configuration, usbInterfaces.map { it as Parcelable }.toTypedArray())

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
