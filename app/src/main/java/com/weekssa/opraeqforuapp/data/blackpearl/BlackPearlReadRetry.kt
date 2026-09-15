package com.weekssa.opraeqforuapp.data.blackpearl

import kotlinx.coroutines.delay

/**
 * Bounded retry helper for Black Pearl read requests only.
 *
 * This must never wrap a setting write. Each retry reissues only the same nondestructive read
 * request, and the caller supplies a current-session predicate so retry cannot cross a replaced
 * USB session.
 */
internal suspend fun <T> retryBlackPearlRead(
    maxAttempts: Int,
    retryDelayMillis: Long,
    isSessionCurrent: () -> Boolean,
    attempt: suspend () -> T?,
): T? {
    require(maxAttempts > 0) { "At least one Black Pearl read attempt is required." }
    require(retryDelayMillis >= 0L) { "Black Pearl read retry delay cannot be negative." }

    repeat(maxAttempts) { index ->
        if (!isSessionCurrent()) return null
        val value = attempt()
        if (!isSessionCurrent()) return null
        if (value != null) return value
        if (index < maxAttempts - 1 && retryDelayMillis > 0L) {
            delay(retryDelayMillis)
        }
    }
    return null
}
