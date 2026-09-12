package com.weekssa.opraeqforuapp.domain.dac

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlin.math.roundToLong
import org.junit.Test

class HardwareEqEditorTest {
    @Test
    fun editorRequiresCurrentHardwareSnapshot() {
        val stale = currentState(blackPearlSnapshot()).markStale()

        assertThat(
            HardwareEqEditor.startFromCurrent(
                snapshotState = stale,
                spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            ),
        ).isEqualTo(HardwareEqEditorStartResult.CurrentSnapshotRequired)
    }

    @Test
    fun editorRejectsSnapshotForDifferentDevice() {
        val result = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(dedicatedPreampSnapshot()),
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
        )

        assertThat(result).isEqualTo(HardwareEqEditorStartResult.WrongDevice)
    }

    @Test
    fun blackPearlTrackedHeadroomBaselineDoesNotCreateFalseEdit() {
        val result = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(blackPearlSnapshot()),
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            trackedPlaybackGainDeltaDb = -3.0,
        ) as HardwareEqEditorStartResult.Ready

        val working = result.workingCopy
        assertThat(working.baselineHeadroomGainDb).isEqualTo(-3.0)
        assertThat(working.plannedHeadroomGainDb).isEqualTo(-3.0)
        assertThat(working.headroomMechanism)
            .isEqualTo(HardwareEqHeadroomMechanism.TRACKED_PLAYBACK_GAIN_DELTA)
        assertThat(working.hasChanges).isFalse()
        assertThat(working.headroomPlanChanged).isFalse()
        assertThat(requireNotNull(working.headroomAssessment).status).isEqualTo(DacHeadroomStatus.SAFE)
        assertThat(working.baselineSnapshot.playbackGainDb).isEqualTo(-18.0)
    }

    @Test
    fun positiveDedicatedPreampReadbackIsPreservedAndNotCalledAnEdit() {
        val spec = dedicatedSpec()
        val result = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(dedicatedPreampSnapshot(preampDb = 3.0)),
            spec = spec,
        ) as HardwareEqEditorStartResult.Ready

        val working = result.workingCopy
        assertThat(working.baselineHeadroomGainDb).isEqualTo(3.0)
        assertThat(working.plannedHeadroomGainDb).isEqualTo(3.0)
        assertThat(working.hasChanges).isFalse()
        assertThat(requireNotNull(working.headroomAssessment).requiredGainDb).isWithin(1e-9).of(0.0)
        assertThat(requireNotNull(working.headroomAssessment).status)
            .isEqualTo(DacHeadroomStatus.ADJUSTMENT_REQUIRED)

        val safer = HardwareEqEditor.useSafeGain(working, spec)
        assertThat(safer.plannedHeadroomGainDb).isEqualTo(0.0)
        assertThat(safer.headroomPlanChanged).isTrue()
        assertThat(safer.hasChanges).isTrue()
        assertThat(requireNotNull(safer.headroomAssessment).status).isEqualTo(DacHeadroomStatus.SAFE)
    }

    @Test
    fun positiveTrackedPlaybackGainBaselineIsPreservedAndCanBeMadeSafeLocally() {
        val result = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(blackPearlSnapshot()),
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            trackedPlaybackGainDeltaDb = 2.0,
        ) as HardwareEqEditorStartResult.Ready

        val working = result.workingCopy
        assertThat(working.baselineHeadroomGainDb).isEqualTo(2.0)
        assertThat(working.plannedHeadroomGainDb).isEqualTo(2.0)
        assertThat(working.hasChanges).isFalse()
        assertThat(requireNotNull(working.headroomAssessment).requiredGainDb).isWithin(1e-9).of(0.0)
        assertThat(requireNotNull(working.headroomAssessment).status)
            .isEqualTo(DacHeadroomStatus.ADJUSTMENT_REQUIRED)

        val safer = HardwareEqEditor.useSafeGain(working, HardwareEqEditSpecs.TRN_BLACK_PEARL)
        assertThat(safer.plannedHeadroomGainDb).isEqualTo(0.0)
        assertThat(safer.baselineHeadroomGainDb).isEqualTo(2.0)
        assertThat(safer.baselineSnapshot.playbackGainDb).isEqualTo(-18.0)
        assertThat(safer.headroomPlanChanged).isTrue()
        assertThat(safer.hasChanges).isTrue()
        assertThat(requireNotNull(safer.headroomAssessment).status).isEqualTo(DacHeadroomStatus.SAFE)
    }

    @Test
    fun useSafeGainChangesOnlyLocalPlanAndPreservesAbsolutePlaybackVolume() {
        val boosted = blackPearlSnapshot(
            firstBand = hardwareFilter(index = 0, gainDb = 6.0),
        )
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(boosted),
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            trackedPlaybackGainDeltaDb = -2.0,
        ) as HardwareEqEditorStartResult.Ready

        val before = started.workingCopy
        assertThat(requireNotNull(before.headroomAssessment).status)
            .isEqualTo(DacHeadroomStatus.ADJUSTMENT_REQUIRED)

        val after = HardwareEqEditor.useSafeGain(before, HardwareEqEditSpecs.TRN_BLACK_PEARL)
        val assessment = requireNotNull(after.headroomAssessment)
        assertThat(after.plannedHeadroomGainDb).isEqualTo(assessment.requiredGainDb)
        assertThat(after.plannedHeadroomGainDb!!).isLessThan(-5.8)
        assertThat(after.baselineHeadroomGainDb).isEqualTo(-2.0)
        assertThat(after.baselineSnapshot.playbackGainDb).isEqualTo(-18.0)
        assertThat(after.baselineSnapshot).isEqualTo(before.baselineSnapshot)
        assertThat(after.headroomPlanChanged).isTrue()
        assertThat(assessment.status).isEqualTo(DacHeadroomStatus.SAFE)
    }

    @Test
    fun resetLocalEditsRestoresFiltersAndHeadroomBaseline() {
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(blackPearlSnapshot()),
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            trackedPlaybackGainDeltaDb = -1.0,
        ) as HardwareEqEditorStartResult.Ready
        val changedFilter = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 1_500.0,
            gainDb = 4.0,
            q = 1.0,
        )
        val changedGain = HardwareEqEditor.useSafeGain(changedFilter, HardwareEqEditSpecs.TRN_BLACK_PEARL)
        assertThat(changedGain.hasChanges).isTrue()

        val reset = HardwareEqEditor.resetLocalEdits(changedGain, HardwareEqEditSpecs.TRN_BLACK_PEARL)
        assertThat(reset.filters).containsExactlyElementsIn(reset.baselineSnapshot.filters).inOrder()
        assertThat(reset.plannedHeadroomGainDb).isEqualTo(-1.0)
        assertThat(reset.differences).isEmpty()
        assertThat(reset.hasChanges).isFalse()
    }

    @Test
    fun validFilterEditProducesExactReviewDifferencesAndRecalculatesResponse() {
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(blackPearlSnapshot()),
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
        ) as HardwareEqEditorStartResult.Ready

        val changed = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            bandIndex = 0,
            type = EqFilterType.LOW_SHELF,
            frequencyHz = 120.0,
            gainDb = 3.0,
            q = 0.75,
        )

        assertThat(changed.hasBlockingIssues).isFalse()
        assertThat(changed.differences.map(HardwareEqDifference::field))
            .containsExactly(
                HardwareEqDifferenceField.FILTER_TYPE,
                HardwareEqDifferenceField.FREQUENCY_HZ,
                HardwareEqDifferenceField.GAIN_DB,
                HardwareEqDifferenceField.Q,
            )
            .inOrder()
        assertThat(requireNotNull(changed.responseCurve).maximumGainDb).isGreaterThan(2.0)
        assertThat(requireNotNull(changed.headroomAssessment).status)
            .isEqualTo(DacHeadroomStatus.ADJUSTMENT_REQUIRED)
    }

    @Test
    fun blackPearlEncodableGainOutsideValidatedNormalRangeIsCautionNotBlocking() {
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(blackPearlSnapshot()),
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
        ) as HardwareEqEditorStartResult.Ready

        val changed = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 1_000.0,
            gainDb = 11.0,
            q = 1.0,
        )

        assertThat(changed.hasBlockingIssues).isFalse()
        assertThat(changed.cautions).hasSize(1)
        assertThat(changed.cautions.single()).isInstanceOf(HardwareEqEditIssue.GainOutsideNormalRange::class.java)
    }

    @Test
    fun absoluteOutOfRangeGainIsBlockingAndNeverSilentlyClamped() {
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(blackPearlSnapshot()),
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
        ) as HardwareEqEditorStartResult.Ready

        val changed = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 1_000.0,
            gainDb = 128.0,
            q = 1.0,
        )

        assertThat(changed.hasBlockingIssues).isTrue()
        val issue = changed.issues.filterIsInstance<HardwareEqEditIssue.OutOfRange>().single()
        assertThat(issue.bandIndex).isEqualTo(0)
        assertThat(issue.field).isEqualTo(HardwareEqDifferenceField.GAIN_DB)
        assertThat(changed.filters.first { it.index == 0 }.gainDb).isEqualTo(128.0)
        assertThat(changed.cautions).isEmpty()
    }

    @Test
    fun nativeStepMismatchIsBlockingAndNeverSilentlyRounded() {
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(blackPearlSnapshot()),
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
        ) as HardwareEqEditorStartResult.Ready

        val changed = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            bandIndex = 0,
            type = EqFilterType.PEAK,
            frequencyHz = 1_000.5,
            gainDb = 0.0,
            q = 1.0,
        )

        assertThat(changed.hasBlockingIssues).isTrue()
        val issue = changed.issues.filterIsInstance<HardwareEqEditIssue.NotRepresentableAtStep>().single()
        assertThat(issue.bandIndex).isEqualTo(0)
        assertThat(issue.field).isEqualTo(HardwareEqDifferenceField.FREQUENCY_HZ)
        assertThat(changed.filters.first { it.index == 0 }.frequencyHz).isEqualTo(1_000.5)
    }

    @Test
    fun unsupportedActiveFilterIsBlockingAndResponseIsNotInvented() {
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(blackPearlSnapshot()),
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
        ) as HardwareEqEditorStartResult.Ready

        val changed = HardwareEqEditor.updateFilter(
            workingCopy = started.workingCopy,
            spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
            bandIndex = 0,
            type = EqFilterType.LOW_PASS,
            frequencyHz = 1_000.0,
            gainDb = 0.0,
            q = 1.0,
        )

        assertThat(changed.hasBlockingIssues).isTrue()
        assertThat(changed.issues.any { it is HardwareEqEditIssue.UnsupportedFilterType }).isTrue()
        assertThat(changed.issues).contains(HardwareEqEditIssue.ResponseUnavailable)
        assertThat(changed.responseCurve).isNull()
        assertThat(changed.headroomAssessment).isNull()
    }

    @Test
    fun insufficientVerifiedAttenuationIsDeviceLimitedAndSafeGainDoesNothing() {
        val spec = dedicatedSpec(minimumHeadroomGainDb = -3.0)
        val snapshot = dedicatedPreampSnapshot(
            preampDb = 0.0,
            filter = hardwareFilter(index = 0, gainDb = 6.0),
        )
        val started = HardwareEqEditor.startFromCurrent(
            snapshotState = currentState(snapshot),
            spec = spec,
        ) as HardwareEqEditorStartResult.Ready

        val working = started.workingCopy
        assertThat(requireNotNull(working.headroomAssessment).requiredGainDb).isLessThan(-5.5)
        assertThat(requireNotNull(working.headroomAssessment).status).isEqualTo(DacHeadroomStatus.DEVICE_LIMITED)

        val unchanged = HardwareEqEditor.useSafeGain(working, spec)
        assertThat(unchanged).isEqualTo(working)
    }

    private fun blackPearlSnapshot(
        firstBand: HardwareEqFilter = hardwareFilter(index = 0),
    ): HardwareEqSnapshot {
        val filters = buildList {
            add(firstBand)
            for (index in 1 until 10) {
                add(
                    hardwareFilter(
                        index = index,
                        frequencyHz = 100.0 * (index + 1),
                    ),
                )
            }
        }
        return HardwareEqSnapshot(
            deviceId = DacDeviceId.TRN_BLACK_PEARL,
            sessionGeneration = 1,
            activeSlot = 2,
            filters = filters,
            dedicatedEqPreampDb = null,
            playbackGainDb = -18.0,
            verifiedAtEpochMillis = 1_000L,
        )
    }

    private fun dedicatedPreampSnapshot(
        preampDb: Double = 0.0,
        filter: HardwareEqFilter = hardwareFilter(index = 0),
    ): HardwareEqSnapshot = HardwareEqSnapshot(
        deviceId = DacDeviceId.FIIO_JA11,
        sessionGeneration = 1,
        filters = listOf(filter),
        dedicatedEqPreampDb = preampDb,
        playbackGainDb = null,
        verifiedAtEpochMillis = 1_000L,
    )

    private fun dedicatedSpec(
        minimumHeadroomGainDb: Double = -12.0,
    ): HardwareEqEditSpec = HardwareEqEditSpec(
        deviceId = DacDeviceId.FIIO_JA11,
        bandCount = 1,
        supportedFilterTypes = setOf(EqFilterType.PEAK, EqFilterType.LOW_SHELF, EqFilterType.HIGH_SHELF),
        frequencyRangeHz = DacNumericRange(20.0, 20_000.0),
        gainRangeDb = DacNumericRange(-12.0, 12.0),
        qRange = DacNumericRange(0.1, 10.0),
        frequencyStepHz = 1.0,
        gainStepDb = 0.1,
        qStep = 0.01,
        headroomMechanism = HardwareEqHeadroomMechanism.DEDICATED_EQ_PREAMP,
        headroomGainStepDb = 0.5,
        minimumVerifiedHeadroomGainDb = minimumHeadroomGainDb,
    )

    private fun currentState(snapshot: HardwareEqSnapshot): HardwareEqSnapshotState {
        val fingerprint = HardwareEqNativeFingerprint(
            deviceId = snapshot.deviceId,
            eqEnabled = true,
            bands = snapshot.filters.sortedBy(HardwareEqFilter::index).map { filter ->
                HardwareEqNativeBandFingerprint(
                    index = filter.index,
                    enabled = filter.enabled,
                    type = filter.type,
                    frequencyUnits = filter.frequencyHz.roundToLong().coerceAtLeast(1L),
                    gainUnits = (filter.gainDb * 256.0).roundToLong(),
                    qUnits = (filter.q * 256.0).roundToLong().coerceAtLeast(1L),
                )
            },
            dedicatedEqPreampUnits = snapshot.dedicatedEqPreampDb?.times(256.0)?.roundToLong(),
        )
        return HardwareEqSnapshotState().publishCurrent(
            HardwareEqSnapshotBundle(snapshot = snapshot, fingerprint = fingerprint),
        )
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
