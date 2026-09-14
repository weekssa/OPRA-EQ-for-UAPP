package com.weekssa.opraeqforuapp.data.blackpearl

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlackPearlReadRetryTest {
    @Test
    fun transientReadFailureRetriesOnceAndReturnsValue() = runBlocking {
        var attempts = 0

        val result = retryBlackPearlRead(
            maxAttempts = 2,
            retryDelayMillis = 0L,
            isSessionCurrent = { true },
        ) {
            attempts += 1
            if (attempts == 1) null else 19
        }

        assertEquals(19, result)
        assertEquals(2, attempts)
    }

    @Test
    fun exhaustedReadRetryReturnsNullAfterBoundedAttempts() = runBlocking {
        var attempts = 0

        val result = retryBlackPearlRead<Int>(
            maxAttempts = 2,
            retryDelayMillis = 0L,
            isSessionCurrent = { true },
        ) {
            attempts += 1
            null
        }

        assertNull(result)
        assertEquals(2, attempts)
    }

    @Test
    fun sessionReplacementStopsBeforeAnyRetry() = runBlocking {
        var current = true
        var attempts = 0

        val result = retryBlackPearlRead<Int>(
            maxAttempts = 2,
            retryDelayMillis = 0L,
            isSessionCurrent = { current },
        ) {
            attempts += 1
            current = false
            null
        }

        assertNull(result)
        assertEquals(1, attempts)
    }
}
