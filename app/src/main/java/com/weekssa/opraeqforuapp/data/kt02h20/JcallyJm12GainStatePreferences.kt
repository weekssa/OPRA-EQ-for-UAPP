package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import com.weekssa.opraeqforuapp.domain.kt02h20.JcallyJm12GainStateStore

class JcallyJm12GainStatePreferences(
    context: Context,
) : JcallyJm12GainStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun readAppliedGainDeltaSteps(): Int = preferences.getInt(KEY_APPLIED_GAIN_DELTA_STEPS, 0)

    override fun writeAppliedGainDeltaSteps(steps: Int) {
        preferences.edit().putInt(KEY_APPLIED_GAIN_DELTA_STEPS, steps).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "jcally_jm12_flash_state"
        const val KEY_APPLIED_GAIN_DELTA_STEPS = "applied_gain_delta_steps"
    }
}
