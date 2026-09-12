package com.weekssa.opraeqforuapp.domain.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlReadCodec
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import org.junit.Test

class HardwareEqSnapshotStateTest {
    @Test
    fun beginningFreshReadMakesRetainedSnapshotExplicitlyStale() {
        val current = HardwareEqSnapshotState().publishCurrent(snapshot())

        val reading = current.beginRead()

        assertThat(reading.bundle).isEqualTo(current.bundle)
        assertThat(reading.freshness).isEqualTo(DacStateFreshness.LAST_READ_STALE)
        assertThat(reading.isReading).isTrue()
        assertThat(reading.readFailed).isFalse()
    }

    @Test
    fun successfulReadReplacesStaleStateWithCurrentVerifiedSnapshot() {
        val next = snapshot(sessionGeneration = 2, verifiedAt = 200)
        val state = HardwareEqSnapshotState()
            .publishCurrent(snapshot())
            .beginRead()
            .publishCurrent(next)

        assertThat(state.bundle).isEqualTo(next)
        assertThat(state.freshness).isEqualTo(DacStateFreshness.CURRENT)
        assertThat(state.isReading).isFalse()
        assertThat(state.readFailed).isFalse()
    }

    @Test
    fun failedReconnectReadKeepsLastVerifiedSnapshotButNeverCallsItCurrent() {
        val prior = snapshot()
        val failed = HardwareEqSnapshotState()
            .publishCurrent(prior)
            .beginRead()
            .markReadFailed()

        assertThat(failed.bundle).isEqualTo(prior)
        assertThat(failed.freshness).isEqualTo(DacStateFreshness.LAST_READ_STALE)
        assertThat(failed.isReading).isFalse()
        assertThat(failed.readFailed).isTrue()
    }

    @Test
    fun disconnectMarksCurrentSnapshotStale() {
        val stale = HardwareEqSnapshotState()
            .publishCurrent(snapshot())
            .markStale()

        assertThat(stale.freshness).isEqualTo(DacStateFreshness.LAST_READ_STALE)
        assertThat(stale.isReading).isFalse()
    }

    private fun snapshot(
        sessionGeneration: Long = 1,
        verifiedAt: Long = 100,
    ) = HardwareEqSnapshotFactory.blackPearl(
        nativeBands = List(10) { index ->
            BlackPearlReadCodec.NativeBand(
                index = index,
                type = EqFilterType.PEAK,
                frequencyRawHz = listOf(31, 63, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)[index],
                gainRaw256 = if (index == 0) 256 else 0,
                qRaw256 = 256,
                activeSlot = 1,
            )
        },
        globalGainRaw = 0,
        sessionGeneration = sessionGeneration,
        verifiedAtEpochMillis = verifiedAt,
    )!!
}
