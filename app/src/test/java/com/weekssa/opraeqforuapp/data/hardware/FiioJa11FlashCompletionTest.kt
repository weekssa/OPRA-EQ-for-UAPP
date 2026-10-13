package com.weekssa.opraeqforuapp.data.hardware

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol.Band
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
        assertThat(completion.isCompleted).isFalse()

        allowRefreshToComplete.complete(Unit)
        assertThat(completion.await()).isEqualTo("success")
        assertThat(refreshStarted.isCompleted).isTrue()
        assertThat(markedStale).isFalse()
    }

    @Test
    fun replacementSessionReadMissGetsOneReadOnlyRetryAndPublishesCurrentEqState() = runBlocking {
        var flashCount = 0
        var readCount = 0
        var published: HardwareEqSnapshotBundle? = null
        var state = HardwareEqSnapshotState()
        val current = replacementBundle(sessionGeneration = 2L)

        completeFiioJa11Flash(
            flash = { flashCount += 1; "success" },
            isConnected = { true },
            refreshSnapshot = {
                readFiioJa11SnapshotWithBoundedRetry(
                    allowRetry = true,
                    isConnected = { true },
                    currentSessionGeneration = { 2L },
                    isSessionCurrent = { it == 2L },
                    read = {
                        readCount += 1
                        if (readCount == 1) null else current
                    },
                    publishCurrent = {
                        published = it
                        state = state.publishCurrent(it)
                    },
                    markReadFailed = { state = state.markReadFailed() },
                    markStale = { state = state.markStale() },
                )
            },
            markSnapshotStale = { state = state.markStale() },
        )

        assertThat(flashCount).isEqualTo(1)
        assertThat(readCount).isEqualTo(2)
        assertThat(published).isEqualTo(current)
        assertThat(state.freshness).isEqualTo(DacStateFreshness.CURRENT)
        assertThat(state.bundle?.snapshot?.filters).hasSize(FiioJa11Protocol.BAND_COUNT)
        assertThat(state.bundle?.snapshot?.filters).isNotEmpty()
    }

    @Test
    fun twoFailedReplacementReadsStopWithoutLoopingOrPublishing() = runBlocking {
        var reads = 0
        var published = 0
        var failures = 0

        readFiioJa11SnapshotWithBoundedRetry(
            allowRetry = true,
            isConnected = { true },
            currentSessionGeneration = { 7L },
            isSessionCurrent = { it == 7L },
            read = { reads += 1; null },
            publishCurrent = { published += 1 },
            markReadFailed = { failures += 1 },
            markStale = { error("connected session must not be marked stale") },
        )

        assertThat(reads).isEqualTo(2)
        assertThat(published).isEqualTo(0)
        assertThat(failures).isEqualTo(1)
    }

    @Test
    fun staleReplacementBundleIsNeverPublished() = runBlocking {
        val stale = replacementBundle(sessionGeneration = 6L)
        var reads = 0
        var published = 0
        var failures = 0

        val result = readFiioJa11SnapshotWithBoundedRetry(
            allowRetry = true,
            isConnected = { true },
            currentSessionGeneration = { 7L },
            isSessionCurrent = { it == 7L },
            read = { reads += 1; stale },
            publishCurrent = { published += 1 },
            markReadFailed = { failures += 1 },
            markStale = { error("connected session must not be marked stale") },
        )

        assertThat(result).isNull()
        assertThat(reads).isEqualTo(2)
        assertThat(published).isEqualTo(0)
        assertThat(failures).isEqualTo(1)
    }

    private fun replacementBundle(sessionGeneration: Long): HardwareEqSnapshotBundle =
        HardwareEqSnapshotFactory.fiioJa11(
            nativeBands = List(FiioJa11Protocol.BAND_COUNT) { index ->
                Band(
                    type = "peak_dip",
                    frequencyHz = listOf(80.0, 250.0, 1_000.0, 4_000.0, 12_000.0)[index],
                    gainDb = if (index == 0) 2.0 else 0.0,
                    q = 0.7,
                )
            },
            globalEqGainDb = -3.0,
            sessionGeneration = sessionGeneration,
            verifiedAtEpochMillis = 100L,
        )!!
}
