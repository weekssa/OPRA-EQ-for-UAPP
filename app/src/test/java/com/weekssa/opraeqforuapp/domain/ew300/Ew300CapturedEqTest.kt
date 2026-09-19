package com.weekssa.opraeqforuapp.domain.ew300

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import org.junit.Test

class Ew300CapturedEqTest {
    @Test
    fun capturePreservesFivePeakBandsButExcludesPlaybackGain() {
        val bundle = requireNotNull(
            HardwareEqSnapshotFactory.ew300(
                nativeBands = stockPeakBands(),
                globalGainDb = -4.0,
                sessionGeneration = 7L,
                verifiedAtEpochMillis = 1234L,
            ),
        )

        val draft = buildEw300CapturedEqDraft("capture-1", bundle, association = null)

        assertThat(draft.profile.preampGainDb).isNull()
        assertThat(draft.profile.bands).hasSize(5)
        assertThat(draft.profile.bands!![0].frequency).isEqualTo(100.0)
        assertThat(draft.profile.bands!![0].gainDb).isEqualTo(-1.1)
        assertThat(draft.profile.bands!![0].q).isEqualTo(0.8)
        assertThat(draft.captureMetadata.nativeFingerprint).isEqualTo(bundle.fingerprint)
    }

    private fun stockPeakBands(): List<Kt02h20Band> = listOf(
        Kt02h20Band("peak_dip", 100.0, -1.1, 0.8),
        Kt02h20Band("peak_dip", 200.0, -0.9, 0.8),
        Kt02h20Band("peak_dip", 300.0, -0.4, 1.0),
        Kt02h20Band("peak_dip", 8000.0, -4.8, 1.5),
        Kt02h20Band("peak_dip", 7000.0, 0.5, 0.5),
    )
}
