package com.weekssa.opraeqforuapp.ui.screens

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

internal fun captureV080Screenshot(name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    if (InstrumentationRegistry.getArguments().getString("v080_screenshots") != "true") return

    instrumentation.waitForIdleSync()
    SystemClock.sleep(350)
    val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
        "Could not capture v0.8.0 UI screenshot: $name"
    }
    instrumentation.waitForIdleSync()
    val directory = InstrumentationRegistry.getArguments()
        .getString("additionalTestOutputDir")
        ?.let(::File)
        ?: requireNotNull(
            instrumentation.targetContext.getExternalFilesDir("v080-beta-screenshots"),
        ) {
            "No app-scoped external directory is available for v0.8.0 screenshots."
        }
    check(directory.isDirectory || directory.mkdirs()) {
        "Could not create v0.8.0 screenshot output directory: ${directory.path}"
    }
    val destination = File(directory, "$name.png")
    val temporary = File(directory, "$name.png.part")
    FileOutputStream(temporary).use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            "Could not encode v0.8.0 UI screenshot: $name"
        }
    }
    if (destination.exists()) {
        check(destination.delete()) { "Could not replace v0.8.0 UI screenshot: $name" }
    }
    check(temporary.renameTo(destination)) { "Could not finalize v0.8.0 UI screenshot: $name" }
    bitmap.recycle()
    instrumentation.waitForIdleSync()
}
