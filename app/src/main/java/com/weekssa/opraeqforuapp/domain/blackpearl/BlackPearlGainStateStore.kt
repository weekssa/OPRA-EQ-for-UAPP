package com.weekssa.opraeqforuapp.domain.blackpearl

/**
 * Persists the playback-gain delta last applied by EQ Library so a later Flash can replace that
 * adjustment instead of stacking another attenuation on top of it.
 */
interface BlackPearlGainStateStore {
    /** Null means the last mutation did not leave a trustworthy anti-stacking baseline. */
    fun readAppliedGainDeltaRaw(): Int?
    fun writeAppliedGainDeltaRaw(rawDelta: Int)
    fun markAppliedGainDeltaUnknown()
}
