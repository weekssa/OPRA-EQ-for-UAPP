package com.weekssa.opraeqforuapp.data.kt02h20

import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol

/**
 * Per-session cache for the optional JA11 digital-filter probe (`0x11`).
 *
 * A unit that ignores the command costs one response timeout per read, so the result is probed
 * once per USB session. A `null` result is cached too: it means "this session did not report the
 * control", which keeps the row disabled without repeated timeouts. A filter write invalidates the
 * cache so the follow-up readback observes the device again.
 */
internal class FiioJa11FilterProbeCache {
    private var generation = 0L
    private var value: FiioJa11Protocol.DigitalFilterMode? = null
    private var valid = false

    fun isCachedFor(sessionGeneration: Long): Boolean = valid && sessionGeneration == generation

    fun value(): FiioJa11Protocol.DigitalFilterMode? = value

    fun record(sessionGeneration: Long, mode: FiioJa11Protocol.DigitalFilterMode?) {
        generation = sessionGeneration
        value = mode
        valid = sessionGeneration > 0L
    }

    fun invalidate() {
        valid = false
    }
}
