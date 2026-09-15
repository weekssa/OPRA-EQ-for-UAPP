package com.weekssa.opraeqforuapp.domain.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlReadCodec
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20Band
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import org.junit.Test

class HardwareEqSnapshotFactoryTest {
    @Test
    fun blackPearlPreservesRawNativeUnitsAndKeepsPlaybackGainOutOfEqFingerprint() {
        val bundle = HardwareEqSnapshotFactory.blackPearl(
            nativeBands = blackPearlBands(gainRaw256 = -640, slot = 2),
            globalGainRaw = -4_608,
            sessionGeneration = 7,
            verifiedAtEpochMillis = 123,
        )

        assertThat(bundle).isNotNull()
        bundle!!
        assertThat(bundle.snapshot.activeSlot).isEqualTo(2)
        assertThat(bundle.snapshot.playbackGainDb).isEqualTo(-18.0)
        assertThat(bundle.snapshot.dedicatedEqPreampDb).isNull()
        assertThat(bundle.fingerprint.dedicatedEqPreampUnits).isNull()
        assertThat(bundle.fingerprint.bands.first().gainUnits).isEqualTo(-640)
        assertThat(bundle.fingerprint.bands.first().qUnits).isEqualTo(256)
    }

    @Test
    fun blackPearlRejectsIncompleteOrInconsistentReadSet() {
        assertThat(
            HardwareEqSnapshotFactory.blackPearl(
                nativeBands = blackPearlBands().dropLast(1),
                globalGainRaw = 0,
                sessionGeneration = 1,
                verifiedAtEpochMillis = 0,
            ),
        ).isNull()

        val inconsistentSlots = blackPearlBands().mapIndexed { index, band ->
            if (index == 9) band.copy(activeSlot = 3) else band
        }
        assertThat(
            HardwareEqSnapshotFactory.blackPearl(
                nativeBands = inconsistentSlots,
                globalGainRaw = 0,
                sessionGeneration = 1,
                verifiedAtEpochMillis = 0,
            ),
        ).isNull()
    }

    @Test
    fun ja11TreatsGlobalEqGainAsDedicatedEqPreampIdentity() {
        val bands = List(5) { index ->
            FiioJa11Protocol.Band(
                type = "peak_dip",
                frequencyHz = listOf(80.0, 250.0, 1_000.0, 4_000.0, 12_000.0)[index],
                gainDb = if (index == 0) 2.5 else 0.0,
                q = 0.7,
            )
        }
        val bundle = HardwareEqSnapshotFactory.fiioJa11(
            nativeBands = bands,
            globalEqGainDb = -3.0,
            sessionGeneration = 2,
            verifiedAtEpochMillis = 5,
        )

        assertThat(bundle).isNotNull()
        bundle!!
        assertThat(bundle.snapshot.dedicatedEqPreampDb).isEqualTo(-3.0)
        assertThat(bundle.snapshot.playbackGainDb).isNull()
        assertThat(bundle.fingerprint.dedicatedEqPreampUnits).isEqualTo(-7_680)
        assertThat(bundle.fingerprint.bands.first().gainUnits).isEqualTo(25)
        assertThat(bundle.fingerprint.bands.first().qUnits).isEqualTo(70)
    }

    @Test
    fun jm12EqEnableParticipatesInEqIdentityButPlaybackGainDoesNot() {
        val bands = List(5) { index ->
            Kt02h20Band(
                type = if (index == 0) "low_shelf" else "peak_dip",
                frequencyHz = listOf(80.0, 250.0, 1_000.0, 4_000.0, 12_000.0)[index],
                gainDb = if (index == 0) 3.3 else 0.0,
                q = 0.707,
            )
        }
        val first = HardwareEqSnapshotFactory.jcallyJm12Stock(
            nativeBands = bands,
            eqEnabled = true,
            digitalGainSteps = intArrayOf(-12),
            sessionGeneration = 4,
            verifiedAtEpochMillis = 8,
        )
        val second = HardwareEqSnapshotFactory.jcallyJm12Stock(
            nativeBands = bands,
            eqEnabled = true,
            digitalGainSteps = intArrayOf(-20),
            sessionGeneration = 4,
            verifiedAtEpochMillis = 8,
        )

        assertThat(first).isNotNull()
        assertThat(second).isNotNull()
        assertThat(first!!.snapshot.playbackGainDb).isEqualTo(-6.0)
        assertThat(second!!.snapshot.playbackGainDb).isEqualTo(-10.0)
        assertThat(first.fingerprint).isEqualTo(second.fingerprint)
        assertThat(first.fingerprint.bands.first().type).isEqualTo(EqFilterType.LOW_SHELF)
        assertThat(first.fingerprint.bands.first().gainUnits).isEqualTo(33)
        assertThat(first.fingerprint.bands.first().qUnits).isEqualTo(707)

        val bypassed = first.fingerprint.copy(eqEnabled = false)
        assertThat(bypassed.isFlatResponse).isTrue()
        assertThat(bypassed).isNotEqualTo(first.fingerprint)
    }

    @Test
    fun jm12StereoChannelImbalanceIsNotCollapsedIntoFalseSinglePlaybackGain() {
        val bands = List(5) { index ->
            Kt02h20Band("peak_dip", 100.0 * (index + 1), 0.0, 1.0)
        }
        val bundle = HardwareEqSnapshotFactory.jcallyJm12Stock(
            nativeBands = bands,
            eqEnabled = true,
            digitalGainSteps = intArrayOf(-12, -10),
            sessionGeneration = 1,
            verifiedAtEpochMillis = 0,
        )

        assertThat(bundle).isNotNull()
        assertThat(bundle!!.snapshot.playbackGainDb).isNull()
    }

    private fun blackPearlBands(
        gainRaw256: Int = 0,
        slot: Int = 1,
    ): List<BlackPearlReadCodec.NativeBand> = List(10) { index ->
        BlackPearlReadCodec.NativeBand(
            index = index,
            type = EqFilterType.PEAK,
            frequencyRawHz = listOf(31, 63, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)[index],
            gainRaw256 = if (index == 0) gainRaw256 else 0,
            qRaw256 = 256,
            activeSlot = slot,
        )
    }
}
