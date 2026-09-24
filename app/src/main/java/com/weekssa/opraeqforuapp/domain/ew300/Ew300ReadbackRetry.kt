package com.weekssa.opraeqforuapp.domain.ew300

import kotlinx.coroutines.delay

/**
 * Bounded read-only reconciliation for the short HID settling window after EW300 Save.
 *
 * This helper is intentionally usable only for readback. It never repeats a write or Save
 * command, and it returns the final observation when the bounded window is exhausted so the
 * caller can preserve an uncertain result and its evidence.
 */
internal object Ew300ReadbackRetry {
    const val MAX_ATTEMPTS = 4
    const val RETRY_DELAY_MILLIS = 250L

    suspend fun <T> read(
        read: suspend () -> T,
        matches: (T) -> Boolean,
    ): T {
        var latest = read()
        if (matches(latest)) return latest
        repeat(MAX_ATTEMPTS - 1) {
            delay(RETRY_DELAY_MILLIS)
            latest = read()
            if (matches(latest)) return latest
        }
        return latest
    }
}
