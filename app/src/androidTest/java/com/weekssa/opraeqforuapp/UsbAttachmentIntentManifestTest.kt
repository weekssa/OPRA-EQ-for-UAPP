package com.weekssa.opraeqforuapp

import android.content.Intent
import android.hardware.usb.UsbManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * EQ Library must not register as a system-wide USB attachment handler.
 *
 * USB sessions are opened only from the in-app exact-device path. Advertising a manifest
 * USB_DEVICE_ATTACHED handler makes Android show an app chooser and can relaunch the activity
 * when a DAC briefly re-enumerates during a verified hardware operation.
 */
@RunWith(AndroidJUnit4::class)
class UsbAttachmentIntentManifestTest {
    @Test
    fun usbAttachmentDoesNotResolveToEqLibrary() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED).setPackage(context.packageName)

        assertThat(context.packageManager.queryIntentActivities(intent, 0)).isEmpty()
    }
}
