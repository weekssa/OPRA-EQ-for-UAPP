package com.weekssa.opraeqforuapp.data.hardware

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.fiio.Ja11ConvergencePhase
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Test

class Ja11ConvergenceReadPolicyTest {
    @Test
    fun enabledAndDisabledTracingHaveIdenticalReadAndPublicationPolicy() = runBlocking {
        for (retry in listOf(false, true)) {
            for (firstSucceeds in listOf(false, true)) {
                assertThat(runPolicy(retry, firstSucceeds, traced = true))
                    .isEqualTo(runPolicy(retry, firstSucceeds, traced = false))
            }
        }
    }

    @Test
    fun cancellationPropagatesWithoutAnotherReadOrPublication() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val neverReleased = CompletableDeferred<Unit>()
        var reads = 0
        var publications = 0
        val phases = mutableListOf<Ja11ConvergencePhase>()
        val caller = async(start = CoroutineStart.UNDISPATCHED) {
            readFiioJa11SnapshotWithBoundedRetry(
                true, { true }, { 2L }, { it == 2L },
                read = { reads += 1; entered.complete(Unit); neverReleased.await(); null },
                publishCurrent = { publications += 1 },
                markReadFailed = { error("cancellation must propagate") },
                markStale = { error("cancellation must propagate") },
                observe = { phase, _, _ -> phases += phase },
            )
        }
        entered.await()
        caller.cancelAndJoin()
        assertThat(caller.isCancelled).isTrue()
        assertThat(reads).isEqualTo(1)
        assertThat(publications).isEqualTo(0)
        assertThat(phases).containsExactly(Ja11ConvergencePhase.ATTEMPT_STARTED).inOrder()
    }

    @Test
    fun supersededBundleIsRejectedAndDisconnectedSessionIsNeverRetried() = runBlocking {
        for (traced in listOf(false, true)) {
            var connected = true
            var reads = 0
            var publications = 0
            val result = readFiioJa11SnapshotWithBoundedRetry(
                true, { connected }, { 2L }, { it == 2L },
                read = { reads += 1; connected = false; bundle(1) },
                publishCurrent = { publications += 1 },
                markReadFailed = {}, markStale = {},
                observe = if (traced) { _, _, _ -> } else null,
            )
            assertThat(result).isNull()
            assertThat(reads).isEqualTo(1)
            assertThat(publications).isEqualTo(0)
        }
    }

    private suspend fun runPolicy(retry: Boolean, firstSucceeds: Boolean, traced: Boolean): List<Int> {
        var reads = 0
        var publications = 0
        var failures = 0
        readFiioJa11SnapshotWithBoundedRetry(
            retry, { true }, { 2L }, { it == 2L },
            read = { reads += 1; if (firstSucceeds) bundle(2) else null },
            publishCurrent = { publications += 1 },
            markReadFailed = { failures += 1 }, markStale = {},
            observe = if (traced) { _, _, _ -> } else null,
        )
        return listOf(reads, publications, failures)
    }

    private fun bundle(generation: Long): HardwareEqSnapshotBundle = HardwareEqSnapshotFactory.fiioJa11(
        nativeBands = List(5) { FiioJa11Protocol.Band("peak_dip", 1_000.0, 0.0, 1.0) },
        globalEqGainDb = 0.0, sessionGeneration = generation, verifiedAtEpochMillis = 1L,
    )!!
}
