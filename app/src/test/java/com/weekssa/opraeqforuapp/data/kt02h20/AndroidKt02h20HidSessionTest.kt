package com.weekssa.opraeqforuapp.data.kt02h20

import android.app.Application
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
import android.os.Binder
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import java.lang.reflect.Proxy
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowUsbDeviceConnection
import org.robolectric.shadows.ShadowUsbManager
import org.robolectric.shadow.api.Shadow
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [Ja11UsbManagerShadow::class, Ja11UsbConnectionShadow::class])
class AndroidKt02h20HidSessionTest {
    private lateinit var context: Application
    private lateinit var usbManager: UsbManager
    private lateinit var usbManagerShadow: Ja11UsbManagerShadow

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        usbManagerShadow = Shadow.extract(usbManager)
        Ja11UsbConnectionShadow.connectionSerial = "JA11-CONNECTION-SERIAL"
        Ja11UsbConnectionShadow.claimAllowed = true
        Ja11UsbManagerShadow.openAllowed = true
    }

    @After
    fun tearDown() {
        Ja11UsbConnectionShadow.connectionSerial = null
        Ja11UsbConnectionShadow.claimAllowed = true
        Ja11UsbManagerShadow.openAllowed = true
    }

    @Test
    fun seriallessSingleJa11UsesOpenConnectionSerialAndDetachInvalidatesOldSession() = runBlocking {
        val device = ja11Device("/dev/bus/usb/001/001", FiioJa11Protocol.PRODUCT_ID_UAC_2)
        usbManagerShadow.addOrUpdateUsbDevice(device, true)
        val session = newJa11Session("JA11")

        try {
            session.connect()
            awaitState(session) { it is Kt02h20ConnectionState.Connected }

            val generation = session.sessionGeneration
            val detachGeneration = session.detachGeneration
            assertThat(session.supportedCandidateCount).isEqualTo(1)
            assertThat(session.connectedDeviceSerial).isEqualTo("JA11-CONNECTION-SERIAL")
            assertThat(session.deviceFingerprintKey).contains("serial=JA11-CONNECTION-SERIAL")
            assertThat(session.isCurrentSession(generation, detachGeneration)).isTrue()

            usbManagerShadow.removeUsbDevice(device)
            context.sendBroadcast(
                Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                    .putExtra(UsbManager.EXTRA_DEVICE, device),
            )
            shadowOf(Looper.getMainLooper()).idle()
            awaitCondition { session.detachGeneration != detachGeneration }

            assertThat(session.isCurrentSession(generation, detachGeneration)).isFalse()
            assertThat(session.send(byteArrayOf(1), settleMillis = 0L)).isFalse()
        } finally {
            session.close()
        }
    }

    @Test
    fun multipleSupportedJa11CandidatesFailClosedBeforeOpen() = runBlocking {
        usbManagerShadow.addOrUpdateUsbDevice(
            ja11Device("/dev/bus/usb/001/001", FiioJa11Protocol.PRODUCT_ID_UAC_1),
            true,
        )
        usbManagerShadow.addOrUpdateUsbDevice(
            ja11Device("/dev/bus/usb/001/002", FiioJa11Protocol.PRODUCT_ID_UAC_2),
            true,
        )
        val session = newJa11Session()

        try {
            session.connect()
            awaitState(session) { it is Kt02h20ConnectionState.Error }

            assertThat(session.supportedCandidateCount).isEqualTo(2)
            assertThat(session.sessionGeneration).isEqualTo(0L)
            assertThat(session.state.value).isInstanceOf(Kt02h20ConnectionState.Error::class.java)
        } finally {
            session.close()
        }
    }

    @Test
    fun permissionGrantAfterConnectRequestOpensTheSameUniqueCandidate() = runBlocking {
        val device = ja11Device("/dev/bus/usb/001/001", FiioJa11Protocol.PRODUCT_ID_UAC_2)
        usbManagerShadow.addOrUpdateUsbDevice(device, false)
        val session = newJa11Session("JA11")

        try {
            session.connect()
            assertThat(session.permissionRequestCount).isEqualTo(1L)
            assertThat(session.sessionGeneration).isEqualTo(0L)

            usbManagerShadow.addOrUpdateUsbDevice(device, true)
            context.sendBroadcast(
                Intent("${context.packageName}.JA11.USB_PERMISSION")
                    .setPackage(context.packageName)
                    .putExtra(UsbManager.EXTRA_DEVICE, device),
            )
            shadowOf(Looper.getMainLooper()).idle()
            awaitState(session) { it is Kt02h20ConnectionState.Connected }

            assertThat(session.sessionGeneration).isGreaterThan(0L)
            assertThat(session.supportedCandidateCount).isEqualTo(1)
        } finally {
            session.close()
        }
    }

    @Test
    fun failedOpenAndFailedInterfaceClaimNeverPublishConnectedSession() = runBlocking {
        val openFailureDevice = ja11Device("/dev/bus/usb/001/001", FiioJa11Protocol.PRODUCT_ID_UAC_2)
        usbManagerShadow.addOrUpdateUsbDevice(openFailureDevice, true)
        Ja11UsbManagerShadow.openAllowed = false
        val openFailureSession = newJa11Session("OPEN_FAILURE")
        try {
            openFailureSession.connect()
            awaitState(openFailureSession) { it is Kt02h20ConnectionState.Error }
            assertThat(openFailureSession.sessionGeneration).isEqualTo(0L)
        } finally {
            openFailureSession.close()
        }

        usbManagerShadow.removeUsbDevice(openFailureDevice)
        Ja11UsbManagerShadow.openAllowed = true
        val claimFailureDevice = ja11Device("/dev/bus/usb/001/002", FiioJa11Protocol.PRODUCT_ID_UAC_2)
        usbManagerShadow.addOrUpdateUsbDevice(claimFailureDevice, true)
        Ja11UsbConnectionShadow.claimAllowed = false
        val claimFailureSession = newJa11Session("CLAIM_FAILURE")
        try {
            claimFailureSession.connect()
            awaitState(claimFailureSession) { it is Kt02h20ConnectionState.Error }
            assertThat(claimFailureSession.sessionGeneration).isEqualTo(0L)
        } finally {
            claimFailureSession.close()
        }
    }

    @Test
    fun reconnectRequiresFreshGenerationAndRejectsOnlyWhenBothSerialsDiffer() = runBlocking {
        assertThat(reconnectResult(oldSerial = "JA11-SERIAL", newSerial = "JA11-SERIAL")).isTrue()
        assertThat(reconnectResult(oldSerial = "JA11-SERIAL", newSerial = "OTHER-SERIAL")).isFalse()
        assertThat(reconnectResult(oldSerial = null, newSerial = null)).isTrue()
    }

    @Test
    fun multipleCandidatesAppearingDuringReconnectFailClosed() = runBlocking {
        usbManager.deviceList.values.toList().forEach(usbManagerShadow::removeUsbDevice)
        val oldDevice = ja11Device("/dev/bus/usb/003/001", FiioJa11Protocol.PRODUCT_ID_UAC_2)
        usbManagerShadow.addOrUpdateUsbDevice(oldDevice, true)
        val session = newJa11Session("AMBIGUOUS_RECONNECT")

        try {
            session.connect()
            awaitState(session) { it is Kt02h20ConnectionState.Connected }
            val oldGeneration = session.sessionGeneration
            val oldDetachGeneration = session.detachGeneration
            val pending = async(start = CoroutineStart.UNDISPATCHED) {
                session.awaitExpectedReplacementSession(
                    previousGeneration = oldGeneration,
                    previousDetachGeneration = oldDetachGeneration,
                    previousSerial = session.connectedDeviceSerial,
                    timeoutMillis = 2_000L,
                )
            }

            usbManagerShadow.removeUsbDevice(oldDevice)
            context.sendBroadcast(
                Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                    .putExtra(UsbManager.EXTRA_DEVICE, oldDevice),
            )
            shadowOf(Looper.getMainLooper()).idle()
            awaitCondition { session.detachGeneration != oldDetachGeneration }

            val firstCandidate = ja11Device("/dev/bus/usb/003/002", FiioJa11Protocol.PRODUCT_ID_UAC_2)
            val secondCandidate = ja11Device("/dev/bus/usb/003/003", FiioJa11Protocol.PRODUCT_ID_UAC_1)
            usbManagerShadow.addOrUpdateUsbDevice(firstCandidate, true)
            usbManagerShadow.addOrUpdateUsbDevice(secondCandidate, true)
            context.sendBroadcast(
                Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                    .putExtra(UsbManager.EXTRA_DEVICE, firstCandidate),
            )
            shadowOf(Looper.getMainLooper()).idle()

            assertThat(pending.await()).isFalse()
            assertThat(session.supportedCandidateCount).isEqualTo(2)
            assertThat(session.sessionGeneration).isEqualTo(0L)
        } finally {
            session.close()
        }
    }

    @Test
    fun replacementTimeoutLeavesSessionDisconnected() = runBlocking {
        val oldDevice = ja11Device("/dev/bus/usb/004/001", FiioJa11Protocol.PRODUCT_ID_UAC_2)
        usbManagerShadow.addOrUpdateUsbDevice(oldDevice, true)
        val session = newJa11Session("RECONNECT_TIMEOUT")

        try {
            session.connect()
            awaitState(session) { it is Kt02h20ConnectionState.Connected }
            val oldGeneration = session.sessionGeneration
            val oldDetachGeneration = session.detachGeneration
            val oldSerial = session.connectedDeviceSerial
            usbManagerShadow.removeUsbDevice(oldDevice)
            context.sendBroadcast(
                Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                    .putExtra(UsbManager.EXTRA_DEVICE, oldDevice),
            )
            shadowOf(Looper.getMainLooper()).idle()
            awaitCondition { session.detachGeneration != oldDetachGeneration }

            assertThat(
                session.awaitExpectedReplacementSession(
                    previousGeneration = oldGeneration,
                    previousDetachGeneration = oldDetachGeneration,
                    previousSerial = oldSerial,
                    timeoutMillis = 50L,
                ),
            ).isFalse()
            assertThat(session.sessionGeneration).isEqualTo(0L)
            assertThat(session.state.value).isEqualTo(Kt02h20ConnectionState.Disconnected)
        } finally {
            session.close()
        }
    }

    private suspend fun reconnectResult(oldSerial: String?, newSerial: String?): Boolean {
        usbManagerShadow = Shadow.extract(usbManager)
        usbManager.deviceList.values.toList().forEach(usbManagerShadow::removeUsbDevice)
        val oldDevice = ja11Device("/dev/bus/usb/002/001", FiioJa11Protocol.PRODUCT_ID_UAC_2)
        usbManagerShadow.addOrUpdateUsbDevice(oldDevice, true)
        Ja11UsbConnectionShadow.connectionSerial = oldSerial
        val session = newJa11Session("RECONNECT_${oldSerial}_$newSerial")
        try {
            session.connect()
            awaitState(session) { it is Kt02h20ConnectionState.Connected }
            val oldGeneration = session.sessionGeneration
            val oldDetachGeneration = session.detachGeneration
            val verified = coroutineScope {
                val pending = async(start = CoroutineStart.UNDISPATCHED) {
                    session.awaitExpectedReplacementSession(
                        previousGeneration = oldGeneration,
                        previousDetachGeneration = oldDetachGeneration,
                        previousSerial = oldSerial,
                        timeoutMillis = 2_000L,
                    )
                }

                usbManagerShadow.removeUsbDevice(oldDevice)
                context.sendBroadcast(
                    Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
                        .putExtra(UsbManager.EXTRA_DEVICE, oldDevice),
                )
                shadowOf(Looper.getMainLooper()).idle()
                awaitCondition { session.detachGeneration != oldDetachGeneration }

                val replacement = ja11Device("/dev/bus/usb/002/002", FiioJa11Protocol.PRODUCT_ID_UAC_2)
                Ja11UsbConnectionShadow.connectionSerial = newSerial
                usbManagerShadow.addOrUpdateUsbDevice(replacement, true)
                context.sendBroadcast(
                    Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                        .putExtra(UsbManager.EXTRA_DEVICE, replacement),
                )
                shadowOf(Looper.getMainLooper()).idle()
                pending.await()
            }
            if (newSerial == oldSerial) {
                assertThat(session.sessionGeneration).isNotEqualTo(oldGeneration)
                assertThat(session.isCurrentSession(session.sessionGeneration, session.detachGeneration)).isTrue()
            }
            return verified
        } finally {
            session.close()
        }
    }

    private fun newJa11Session(suffix: String = "TEST") = AndroidKt02h20HidSession(
        context = context,
        vendorId = FiioJa11Protocol.VENDOR_ID,
        productIds = FiioJa11Protocol.SUPPORTED_PRODUCT_IDS,
        deviceLabel = "FiiO JA11",
        permissionSuffix = suffix,
        requireUniqueCandidate = true,
    )

    private suspend fun awaitState(
        session: AndroidKt02h20HidSession,
        predicate: (Kt02h20ConnectionState) -> Boolean,
    ) = withTimeout(3_000L) {
        while (!predicate(session.state.value)) delay(5L)
    }

    private suspend fun awaitCondition(predicate: () -> Boolean) = withTimeout(3_000L) {
        while (!predicate()) delay(5L)
    }

    private fun ja11Device(deviceName: String, productId: Int): UsbDevice {
        val input = parcelled(UsbEndpoint.CREATOR) {
            writeInt(UsbConstants.USB_DIR_IN or 1)
            writeInt(UsbConstants.USB_ENDPOINT_XFER_INT)
            writeInt(64)
            writeInt(1)
        }
        val output = parcelled(UsbEndpoint.CREATOR) {
            writeInt(UsbConstants.USB_DIR_OUT or 1)
            writeInt(UsbConstants.USB_ENDPOINT_XFER_INT)
            writeInt(64)
            writeInt(1)
        }
        val usbInterface = parcelled(UsbInterface.CREATOR) {
            writeInt(0)
            writeInt(0)
            writeString("JA11 PEQ HID")
            writeInt(UsbConstants.USB_CLASS_HID)
            writeInt(0)
            writeInt(0)
            writeParcelableArray(arrayOf(input, output), 0)
        }
        val configuration = parcelled(UsbConfiguration.CREATOR) {
            writeInt(1)
            writeString("JA11")
            writeInt(0)
            writeInt(100)
            writeParcelableArray(arrayOf(usbInterface), 0)
        }

        val builderType = Class.forName("android.hardware.usb.UsbDevice\$Builder")
        val builderConstructor = builderType.declaredConstructors.firstOrNull { it.parameterCount == 16 }
            ?: error("Unexpected UsbDevice.Builder constructors: ${builderType.declaredConstructors.joinToString()}")
        val builder = builderConstructor
            .apply { isAccessible = true }.newInstance(
            deviceName,
            FiioJa11Protocol.VENDOR_ID,
            productId,
            0,
            0,
            0,
            "FiiO",
            "JA11",
            "2.20",
            arrayOf(configuration),
            null,
            true,
            false,
            false,
            false,
            false,
        )
        val serialReaderType = Class.forName("android.hardware.usb.IUsbSerialReader")
        val serialReader = Proxy.newProxyInstance(
            builderType.classLoader,
            arrayOf(serialReaderType),
        ) { proxy, method, args ->
            when (method.name) {
                "getSerial" -> null
                "asBinder" -> Binder()
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                "toString" -> "JA11 test serial reader"
                else -> null
            }
        }
        val build = builderType.methods.firstOrNull { it.name == "build" && it.parameterCount == 1 }
            ?: error("Unexpected UsbDevice.Builder build methods: ${builderType.methods.filter { it.name == "build" }.joinToString()}")
        return build.invoke(builder, serialReader) as UsbDevice
    }

    private fun <T> parcelled(creator: Parcelable.Creator<T>, write: Parcel.() -> Unit): T {
        val parcel = Parcel.obtain()
        return try {
            parcel.write()
            parcel.setDataPosition(0)
            creator.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }
}

@Implements(UsbManager::class)
class Ja11UsbManagerShadow : ShadowUsbManager() {
    @Implementation
    protected fun requestPermission(device: UsbDevice, pendingIntent: PendingIntent) = Unit

    @Implementation
    protected override fun openDevice(device: UsbDevice): UsbDeviceConnection? =
        if (openAllowed) super.openDevice(device) else null

    companion object {
        @JvmStatic
        var openAllowed = true
    }
}

@Implements(UsbDeviceConnection::class)
class Ja11UsbConnectionShadow : ShadowUsbDeviceConnection() {
    @Implementation
    protected fun getSerial(): String? = connectionSerial

    @Implementation
    protected override fun claimInterface(intf: UsbInterface, force: Boolean): Boolean =
        claimAllowed && super.claimInterface(intf, force)

    companion object {
        @JvmStatic
        var connectionSerial: String? = null

        @JvmStatic
        var claimAllowed = true
    }
}
