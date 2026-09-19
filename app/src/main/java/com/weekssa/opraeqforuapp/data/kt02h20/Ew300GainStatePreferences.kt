package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import com.weekssa.opraeqforuapp.domain.ew300.Ew300GainStateStore

class Ew300GainStatePreferences(context: Context) : Ew300GainStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun isGlobalGainQualified(): Boolean = preferences.getBoolean(KEY_QUALIFIED, false)

    override fun markGlobalGainQualified(qualified: Boolean) {
        preferences.edit().putBoolean(KEY_QUALIFIED, qualified).apply()
    }

    override fun readAppliedGainDeltaSteps(): Int = preferences.getInt(KEY_APPLIED_DELTA, 0)

    override fun writeAppliedGainDeltaSteps(steps: Int) {
        preferences.edit().putInt(KEY_APPLIED_DELTA, steps).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "ew300_flash_state"
        private const val KEY_QUALIFIED = "global_gain_qualified"
        private const val KEY_APPLIED_DELTA = "applied_gain_delta_steps"
    }
}
