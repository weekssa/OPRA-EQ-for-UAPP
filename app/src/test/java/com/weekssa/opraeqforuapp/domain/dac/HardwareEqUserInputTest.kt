package com.weekssa.opraeqforuapp.domain.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlin.math.roundToLong
import org.junit.Test

class HardwareEqUserInputTest {
    @Test
    fun editingGainDoesNotTurnRoundedDisplayQIntoFalseBlockingChange() {
        val exactQ = 179.0 / 256.0
        val started = startEditor(
            firstBand = hardwareFilter(
                index = 0,
                gainDb = -819.0 / 256.0,
                q = exactQ,
            ),
        )

        val changed = updateHardwareEqFilterFromUserInput(
            workingCopy = started,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 1_000.0,
            gainDb = -3.0,
            q = 0.6992,
        )

        val band = changed.filters.single { it.index == 0 }
        assertThat(band.q).isEqualTo(exactQ)
        assertThat(changed.hasBlockingIssues).isFalse()
        assertThat(changed.differences.map(HardwareEqDifference::field))
            .containsExactly(HardwareEqDifferenceField.GAIN_DB)
    }

    @Test
    fun ordinaryDecimalGainAndQSnapToNearestBlackPearlNativeStep() {
        val started = startEditor(firstBand = hardwareFilter(index = 0))

        val changed = updateHardwareEqFilterFromUserInput(
            workingCopy = started,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 1_611.0,
            gainDb = -2.2,
            q = 1.6992,
        )

        val band = changed.filters.single { it.index == 0 }
        assertThat(band.frequencyHz).isEqualTo(1_611.0)
        assertThat(band.gainDb).isEqualTo(-563.0 / 256.0)
        assertThat(band.q).isEqualTo(435.0 / 256.0)
        assertThat(changed.issues.filterIsInstance<HardwareEqEditIssue.NotRepresentableAtStep>()).isEmpty()
    }

    @Test
    fun outOfRangeUserGainRemainsBlockingAndIsNeverClamped() {
        val started = startEditor(firstBand = hardwareFilter(index = 0))

        val changed = updateHardwareEqFilterFromUserInput(
            workingCopy = started,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 1_000.0,
            gainDb = 128.0,
            q = 1.0,
        )

        val band = changed.filters.single { it.index == 0 }
        assertThat(band.gainDb).isEqualTo(128.0)
        assertThat(changed.hasBlockingIssues).isTrue()
        assertThat(changed.issues.filterIsInstance<HardwareEqEditIssue.OutOfRange>()).hasSize(1)
    }

    @Test
    fun fractionalFrequencyRemainsBlockingRatherThanBeingSilentlyRounded() {
        val started = startEditor(firstBand = hardwareFilter(index = 0))

        val changed = updateHardwareEqFilterFromUserInput(
            workingCopy = started,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 1_000.5,
            gainDb = 0.0,
            q = 1.0,
        )

        assertThat(changed.filters.single { it.index == 0 }.frequencyHz).isEqualTo(1_000.5)
        assertThat(changed.issues.filterIsInstance<HardwareEqEditIssue.NotRepresentableAtStep>())
            .containsExactly(
                HardwareEqEditIssue.NotRepresentableAtStep(
                    bandIndex = 0,
                    field = HardwareEqDifferenceField.FREQUENCY_HZ,
                    step = 1.0,
                ),
            )
    }

    private fun startEditor(firstBand: HardwareEqFilter): HardwareEqEditWorkingCopy {
        val filters = buildList {
            add(firstBand)
            for (index in 1 until 10) {
                add(hardwareFilter(index = index, frequencyHz = 100.0 * (index + 1)))
            }
        }
        val snapshot = HardwareEqSnapshot(
            deviceId = DacDeviceId.TRN_BLACK_PEARL,
            sessionGeneration = 1,
            activeSlot = 1,
            filters = filters,
            dedicatedEqPreampDb = null,
            playbackGainDb = -18.0,
            verifiedAtEpochMillis = 1_000L,
        )
        val fingerprint = HardwareEqNativeFingerprint(
            deviceId = snapshot.deviceId,
            eqEnabled = true,
            bands = filters.sortedBy(HardwareEqFilter::index).map { filter ->
                HardwareEqNativeBandFingerprint(
                    index = filter.index,
                    enabled = filter.enabled,
                    type = filter.type,
                    frequencyUnits = filter.frequencyHz.roundToLong().coerceAtLeast(1L),
                    gainUnits = (filter.gainDb * 256.0).roundToLong(),
                    qUnits = (filter.q * 256.0).roundToLong().coerceAtLeast(1L),
                )
            },
            dedicatedEqPreampUnits = null,
        )
        val state = HardwareEqSnapshotState().publishCurrent(
            HardwareEqSnapshotBundle(snapshot = snapshot, fingerprint = fingerprint),
        )
        return (HardwareEqEditor.startFromCurrent(
            snapshotState = state,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
        ) as HardwareEqEditorStartResult.Ready).workingCopy
    }

    private fun hardwareFilter(
        index: Int,
        enabled: Boolean = true,
        type: EqFilterType = EqFilterType.PEAK,
        frequencyHz: Double = 1_000.0,
        gainDb: Double = 0.0,
        q: Double = 1.0,
    ): HardwareEqFilter = HardwareEqFilter(
        index = index,
        enabled = enabled,
        type = type,
        frequencyHz = frequencyHz,
        gainDb = gainDb,
        q = q,
    )
}
