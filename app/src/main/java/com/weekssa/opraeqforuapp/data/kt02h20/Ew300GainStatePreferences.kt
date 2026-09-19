package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import com.weekssa.opraeqforuapp.domain.ew300.Ew300GainStateStore

class Ew300GainStatePreferences(context: Context) : Ew300GainStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun isGlobalGainQualified(deviceFingerprintKey: String): Boolean =
        preferences.getBoolean(qualifiedKey(deviceFingerprintKey), false)

    override fun markGlobalGainQualified(deviceFingerprintKey: String, qualified: Boolean) {
        preferences.edit().putBoolean(qualifiedKey(deviceFingerprintKey), qualified).apply()
    }

    override fun readAppliedGainDeltaSteps(deviceFingerprintKey: String): Int =
        preferences.getInt(deltaKey(deviceFingerprintKey), 0)

    override fun writeAppliedGainDeltaSteps(deviceFingerprintKey: String, steps: Int) {
        preferences.edit().putInt(deltaKey(deviceFingerprintKey), steps).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "ew300_flash_state"

        private fun safeKey(prefix: String, fingerprint: String): String =
            "$prefix:${fingerprint.take(160)}"

        private fun qualifiedKey(fingerprint: String) = safeKey("global_gain_qualified", fingerprint)
        private fun deltaKey(fingerprint: String) = safeKey("applied_gain_delta_steps", fingerprint)
    }
}
