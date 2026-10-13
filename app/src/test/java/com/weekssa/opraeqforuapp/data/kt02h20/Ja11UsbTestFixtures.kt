package com.weekssa.opraeqforuapp.data.kt02h20

import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.os.Binder
import android.os.Parcel
import android.os.Parcelable
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import java.lang.reflect.Proxy

internal fun ja11TestDevice(deviceName: String, productId: Int): UsbDevice {
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
