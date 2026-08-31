package com.weekssa.opraeqforuapp.data.blackpearl

import android.content.Context
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlGainStateStore

class BlackPearlGainStatePreferences(
    context: Context,
) : BlackPearlGainStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun readAppliedGainDeltaRaw(): Int = preferences.getInt(KEY_APPLIED_GAIN_DELTA_RAW, 0)

    override fun writeAppliedGainDeltaRaw(rawDelta: Int) {
        preferences.edit().putInt(KEY_APPLIED_GAIN_DELTA_RAW, rawDelta).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "black_pearl_flash_state"
        private const val KEY_APPLIED_GAIN_DELTA_RAW = "applied_gain_delta_raw"
    }
}
