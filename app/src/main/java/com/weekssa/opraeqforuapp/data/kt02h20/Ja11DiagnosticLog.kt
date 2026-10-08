package com.weekssa.opraeqforuapp.data.kt02h20

import android.os.SystemClock
import android.util.Log
import com.weekssa.opraeqforuapp.BuildConfig

/** Isolated acceptance-build event logging; production and ordinary debug builds keep this disabled. */
internal object Ja11DiagnosticLog {
    private const val TAG = "JA11_DIAG"

    fun event(name: String, vararg fields: Pair<String, Any?>) {
        if (!BuildConfig.JA11_DIAGNOSTICS_ENABLED) return
        val details = fields.joinToString(separator = " ") { (key, value) ->
            "$key=${safeValue(value)}"
        }
        Log.i(TAG, "elapsedMs=${SystemClock.elapsedRealtime()} event=$name $details")
    }

    fun eventForDevice(deviceLabel: String, name: String, vararg fields: Pair<String, Any?>) {
        if (deviceLabel == "FiiO JA11") event(name, *fields)
    }

    private fun safeValue(value: Any?): String = value?.toString()
        ?.replace(' ', '_')
        ?.replace('\n', '_')
        ?.replace('\r', '_')
        ?.replace('=', '_')
        ?: "null"
}
