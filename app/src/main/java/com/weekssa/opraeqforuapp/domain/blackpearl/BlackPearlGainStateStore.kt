package com.weekssa.opraeqforuapp.domain.blackpearl

/**
 * Persists the playback-gain delta last applied by EQ Library so a later Flash can replace that
 * adjustment instead of stacking another attenuation on top of it.
 */
interface BlackPearlGainStateStore {
    fun readAppliedGainDeltaRaw(): Int
    fun writeAppliedGainDeltaRaw(rawDelta: Int)
}
