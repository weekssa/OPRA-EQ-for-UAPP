package com.weekssa.opraeqforuapp.data.hardware

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Test

/** Characterization probes, not a claim to reproduce the unidentified physical acquisition route. */
class FiioJa11ConvergenceContractProbeTest {
    @Test
    fun connectionObserverPolicyReturnsNormallyWithNoBundleAndZeroRetries() = runBlocking {
        var state = HardwareEqSnapshotState()
        var reads = 0
        val result = readFiioJa11SnapshotWithBoundedRetry(
            allowRetry = false,
            isConnected = { true },
            currentSessionGeneration = { 2L },
            isSessionCurrent = { it == 2L },
            read = { reads += 1; null },
            publishCurrent = { state = state.publishCurrent(it) },
            markReadFailed = { state = state.markReadFailed() },
            markStale = { error("replacement session is current") },
        )

        assertThat(result).isNull()
        assertThat(reads).isEqualTo(1)
        assertThat(state.bundle).isNull()
        assertThat(state.isReading).isFalse()
        assertThat(state.readFailed).isTrue()
    }

    @Test
    fun postFlashPolicyNecessarilyRetriesANormalNullReadOnCurrentSession() = runBlocking {
        var reads = 0
        var failures = 0
        readFiioJa11SnapshotWithBoundedRetry(
            allowRetry = true,
            isConnected = { true },
            currentSessionGeneration = { 2L },
            isSessionCurrent = { it == 2L },
            read = { reads += 1; null },
            publishCurrent = { error("no accepted bundle") },
            markReadFailed = { failures += 1 },
            markStale = { error("replacement session is current") },
        )

        assertThat(reads).isEqualTo(2)
        assertThat(failures).isEqualTo(1)
    }

    @Test
    fun completingBothReadsWithoutBundleStillReturnsSuccessfulFlashResult() = runBlocking {
        var state = HardwareEqSnapshotState()
        var writes = 0
        var reads = 0
        val result = completeFiioJa11Flash(
            flash = { writes += 1; "successful mutation" },
            isConnected = { true },
            refreshSnapshot = {
                readFiioJa11SnapshotWithBoundedRetry(
                    allowRetry = true,
                    isConnected = { true },
                    currentSessionGeneration = { 2L },
                    isSessionCurrent = { it == 2L },
                    read = { reads += 1; null },
                    publishCurrent = { state = state.publishCurrent(it) },
                    markReadFailed = { state = state.markReadFailed() },
                    markStale = { error("replacement session is current") },
                )
            },
            markSnapshotStale = { error("replacement session is current") },
        )

        assertThat(result).isEqualTo("successful mutation")
        assertThat(writes).isEqualTo(1)
        assertThat(reads).isEqualTo(2)
        assertThat(state.bundle).isNull()
        assertThat(state.readFailed).isTrue()
    }

    @Test
    fun callerCancellationCanAbandonCompletionBeforeAnyRetryingAcquisition() = runBlocking {
        val refreshEntered = CompletableDeferred<Unit>()
        val refreshGate = CompletableDeferred<Unit>()
        var writes = 0
        var reads = 0
        val caller = async(start = CoroutineStart.UNDISPATCHED) {
            completeFiioJa11Flash(
                flash = { writes += 1; "successful mutation" },
                isConnected = { true },
                refreshSnapshot = {
                    refreshEntered.complete(Unit)
                    refreshGate.await()
                    reads += 1
                },
                markSnapshotStale = { error("replacement session is current") },
            )
        }
        refreshEntered.await()
        caller.cancelAndJoin()
        refreshGate.complete(Unit)

        assertThat(writes).isEqualTo(1)
        assertThat(reads).isEqualTo(0)
        assertThat(caller.isCancelled).isTrue()
    }
}
