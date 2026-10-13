package com.weekssa.opraeqforuapp.data.hardware

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiioJa11FlashCompletionTest {
    @Test
    fun flashDoesNotCompleteBeforeReplacementSnapshotIsPublished() = runBlocking {
        val refreshStarted = CompletableDeferred<Unit>()
        val allowRefreshToComplete = CompletableDeferred<Unit>()
        var markedStale = false

        val completion = async {
            completeFiioJa11Flash(
                flash = { "success" },
                isConnected = { true },
                refreshSnapshot = {
                    refreshStarted.complete(Unit)
                    allowRefreshToComplete.await()
                },
                markSnapshotStale = { markedStale = true },
            )
        }

        refreshStarted.await()
        assertFalse(completion.isCompleted)

        allowRefreshToComplete.complete(Unit)
        assertEquals("success", completion.await())
        assertTrue(refreshStarted.isCompleted)
        assertFalse(markedStale)
    }
}
