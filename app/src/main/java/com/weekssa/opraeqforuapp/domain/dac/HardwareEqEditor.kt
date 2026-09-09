package com.weekssa.opraeqforuapp.domain.dac

import com.weekssa.opraeqforuapp.domain.hardware.HardwareEqDeviceSpecs
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandDeviceSpec
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

/**
 * Describes how a verified hardware EQ may be edited locally without saying anything about writes.
 * Values are derived from the authoritative direct-hardware capability registry rather than copied
 * into Compose.
 */
data class HardwareEqEditSpec(
    val deviceId: DacDeviceId,
    val bandCount: Int,
    val supportedFilterTypes: Set<EqFilterType>,
    val frequencyRangeHz: DacNumericRange,
    val gainRangeDb: DacNumericRange,
    val qRange: DacNumericRange,
    val frequencyStepHz: Double,
    val gainStepDb: Double,
    val qStep: Double,
    val normalGainRangeDb: DacNumericRange? = null,
    /** How a future Apply layer may represent editor-generated safety attenuation. */
    val headroomMechanism: HardwareEqHeadroomMechanism,
    /** Native step for the planned safety adjustment when independently established. */
    val headroomGainStepDb: Double? = null,
    /** Static lower bound only when it is meaningful without reading a fresh absolute baseline. */
    val minimumVerifiedHeadroomGainDb: Double? = null,
) {
    init {
        require(bandCount > 0) { "Hardware EQ editor requires a positive band count." }
        require(supportedFilterTypes.isNotEmpty()) { "Hardware EQ editor requires supported filter types." }
        require(frequencyStepHz.isFinite() && frequencyStepHz > 0.0)
        require(gainStepDb.isFinite() && gainStepDb > 0.0)
        require(qStep.isFinite() && qStep > 0.0)
        require(headroomGainStepDb == null || headroomGainStepDb.isFinite() && headroomGainStepDb > 0.0)
        require(
            minimumVerifiedHeadroomGainDb == null ||
                minimumVerifiedHeadroomGainDb.isFinite() && minimumVerifiedHeadroomGainDb <= 0.0,
        )
        normalGainRangeDb?.let { normal ->
            require(normal.minimum >= gainRangeDb.minimum && normal.maximum <= gainRangeDb.maximum)
        }
    }
}

enum class HardwareEqHeadroomMechanism {
    /** A dedicated EQ/preamp control whose semantics belong to the EQ path. */
    DEDICATED_EQ_PREAMP,

    /** A separately tracked relative playback-gain adjustment; never relabel absolute volume as preamp. */
    TRACKED_PLAYBACK_GAIN_DELTA,
}

/** Editor specs remain derived from the authoritative hardware registry. */
object HardwareEqEditSpecs {
    val TRN_BLACK_PEARL: HardwareEqEditSpec = fromFiniteHardwareSpec(
        deviceId = DacDeviceId.TRN_BLACK_PEARL,
        spec = HardwareEqDeviceSpecs.TRN_BLACK_PEARL,
        headroomMechanism = HardwareEqHeadroomMechanism.TRACKED_PLAYBACK_GAIN_DELTA,
        // Black Pearl final absolute representability depends on the fresh playback baseline and the
        // previously tracked EQ Library delta, so there is deliberately no static minimum here.
        minimumVerifiedHeadroomGainDb = null,
    )

    private fun fromFiniteHardwareSpec(
        deviceId: DacDeviceId,
        spec: FiveBandDeviceSpec,
        headroomMechanism: HardwareEqHeadroomMechanism,
        minimumVerifiedHeadroomGainDb: Double?,
    ): HardwareEqEditSpec {
        val capabilities = spec.capabilities
        val supported = capabilities.supportedBandTypes.mapNotNullTo(mutableSetOf()) { type ->
            when (type) {
                "peak_dip" -> EqFilterType.PEAK
                "low_shelf" -> EqFilterType.LOW_SHELF
                "high_shelf" -> EqFilterType.HIGH_SHELF
                else -> null
            }
        }
        val normalGainRange = DacNumericRange(
            minimum = spec.optimizerMinGainDb,
            maximum = spec.optimizerMaxGainDb,
        ).takeIf { normal ->
            normal.minimum > capabilities.minGainDb || normal.maximum < capabilities.maxGainDb
        }
        return HardwareEqEditSpec(
            deviceId = deviceId,
            bandCount = requireNotNull(capabilities.maxBands),
            supportedFilterTypes = supported,
            frequencyRangeHz = DacNumericRange(capabilities.minFrequencyHz, capabilities.maxFrequencyHz),
            gainRangeDb = DacNumericRange(capabilities.minGainDb, capabilities.maxGainDb),
            qRange = DacNumericRange(capabilities.minQ, capabilities.maxQ),
            frequencyStepHz = spec.quantization.frequencyStepHz,
            gainStepDb = spec.quantization.gainStepDb,
            qStep = spec.quantization.qStep,
            normalGainRangeDb = normalGainRange,
            headroomMechanism = headroomMechanism,
            headroomGainStepDb = spec.quantization.preampStepDb,
            minimumVerifiedHeadroomGainDb = minimumVerifiedHeadroomGainDb,
        )
    }
}

enum class HardwareEqEditIssueSeverity {
    BLOCKING,
    CAUTION,
}

sealed interface HardwareEqEditIssue {
    val severity: HardwareEqEditIssueSeverity
    val bandIndex: Int?

    data class BandCountMismatch(
        val expectedCount: Int,
        val actualCount: Int,
    ) : HardwareEqEditIssue {
        override val severity = HardwareEqEditIssueSeverity.BLOCKING
        override val bandIndex: Int? = null
    }

    data class UnsupportedFilterType(
        override val bandIndex: Int,
        val filterType: EqFilterType,
    ) : HardwareEqEditIssue {
        override val severity = HardwareEqEditIssueSeverity.BLOCKING
    }

    data class OutOfRange(
        override val bandIndex: Int,
        val field: HardwareEqDifferenceField,
        val allowedRange: DacNumericRange,
    ) : HardwareEqEditIssue {
        override val severity = HardwareEqEditIssueSeverity.BLOCKING
    }

    data class NotRepresentableAtStep(
        override val bandIndex: Int,
        val field: HardwareEqDifferenceField,
        val step: Double,
    ) : HardwareEqEditIssue {
        override val severity = HardwareEqEditIssueSeverity.BLOCKING
    }

    data class GainOutsideNormalRange(
        override val bandIndex: Int,
        val normalRange: DacNumericRange,
    ) : HardwareEqEditIssue {
        override val severity = HardwareEqEditIssueSeverity.CAUTION
    }

    data object ResponseUnavailable : HardwareEqEditIssue {
        override val severity = HardwareEqEditIssueSeverity.BLOCKING
        override val bandIndex: Int? = null
    }
}

data class HardwareEqEditWorkingCopy(
    /** Fresh hardware truth used to start this editor generation. Never mutated by local edits. */
    val baselineSnapshot: HardwareEqSnapshot,
    val filters: List<HardwareEqFilter>,
    /** Local EQ Library safety-adjustment delta. This is not the DAC's absolute playback volume. */
    val plannedSafetyGainDb: Double?,
    val headroomMechanism: HardwareEqHeadroomMechanism,
    val responseCurve: HardwareEqResponseCurve?,
    val headroomAssessment: DacHeadroomAssessment?,
    val issues: List<HardwareEqEditIssue>,
    val differences: List<HardwareEqDifference>,
) {
    init {
        require(filters.map(HardwareEqFilter::index).toSet() == baselineSnapshot.filters.map(HardwareEqFilter::index).toSet()) {
            "Local editor must preserve the baseline hardware band layout."
        }
        require(plannedSafetyGainDb == null || plannedSafetyGainDb.isFinite() && plannedSafetyGainDb <= 0.0) {
            "Planned safety gain must be finite and non-positive."
        }
    }

    val hasChanges: Boolean
        get() = differences.isNotEmpty() || plannedSafetyGainDb != null

    val hasBlockingIssues: Boolean
        get() = issues.any { issue -> issue.severity == HardwareEqEditIssueSeverity.BLOCKING }

    val cautions: List<HardwareEqEditIssue>
        get() = issues.filter { issue -> issue.severity == HardwareEqEditIssueSeverity.CAUTION }
}

sealed interface HardwareEqEditorStartResult {
    data class Ready(val workingCopy: HardwareEqEditWorkingCopy) : HardwareEqEditorStartResult
    data object CurrentSnapshotRequired : HardwareEqEditorStartResult
    data object WrongDevice : HardwareEqEditorStartResult
}

/**
 * Pure local editor operations. No method in this object owns or invokes a transport/write API.
 */
object HardwareEqEditor {
    fun startFromCurrent(
        snapshotState: HardwareEqSnapshotState,
        spec: HardwareEqEditSpec,
    ): HardwareEqEditorStartResult {
        if (snapshotState.freshness != DacStateFreshness.CURRENT) {
            return HardwareEqEditorStartResult.CurrentSnapshotRequired
        }
        val snapshot = snapshotState.bundle?.snapshot
            ?: return HardwareEqEditorStartResult.CurrentSnapshotRequired
        if (snapshot.deviceId != spec.deviceId) return HardwareEqEditorStartResult.WrongDevice
        return HardwareEqEditorStartResult.Ready(
            buildWorkingCopy(
                baselineSnapshot = snapshot,
                filters = snapshot.filters,
                plannedSafetyGainDb = snapshot.dedicatedEqPreampDb?.coerceAtMost(0.0),
                spec = spec,
            ),
        )
    }

    fun updateFilter(
        workingCopy: HardwareEqEditWorkingCopy,
        spec: HardwareEqEditSpec,
        bandIndex: Int,
        type: EqFilterType,
        frequencyHz: Double,
        gainDb: Double,
        q: Double,
    ): HardwareEqEditWorkingCopy {
        require(workingCopy.baselineSnapshot.deviceId == spec.deviceId) { "Editor spec/device mismatch." }
        val currentIndex = workingCopy.filters.indexOfFirst { filter -> filter.index == bandIndex }
        require(currentIndex >= 0) { "Band $bandIndex is not part of the editor baseline." }
        val current = workingCopy.filters[currentIndex]
        val updated = HardwareEqFilter(
            index = current.index,
            enabled = current.enabled,
            type = type,
            frequencyHz = frequencyHz,
            gainDb = gainDb,
            q = q,
        )
        val filters = workingCopy.filters.toMutableList().also { list -> list[currentIndex] = updated }
        return buildWorkingCopy(
            baselineSnapshot = workingCopy.baselineSnapshot,
            filters = filters,
            plannedSafetyGainDb = workingCopy.plannedSafetyGainDb,
            spec = spec,
        )
    }

    /** Updates only the local plan; it never writes the DAC. */
    fun useSafeGain(
        workingCopy: HardwareEqEditWorkingCopy,
        spec: HardwareEqEditSpec,
    ): HardwareEqEditWorkingCopy {
        val assessment = workingCopy.headroomAssessment ?: return workingCopy
        if (assessment.status == DacHeadroomStatus.DEVICE_LIMITED) return workingCopy
        return buildWorkingCopy(
            baselineSnapshot = workingCopy.baselineSnapshot,
            filters = workingCopy.filters,
            plannedSafetyGainDb = assessment.requiredGainDb,
            spec = spec,
        )
    }

    fun resetLocalEdits(
        workingCopy: HardwareEqEditWorkingCopy,
        spec: HardwareEqEditSpec,
    ): HardwareEqEditWorkingCopy = buildWorkingCopy(
        baselineSnapshot = workingCopy.baselineSnapshot,
        filters = workingCopy.baselineSnapshot.filters,
        plannedSafetyGainDb = workingCopy.baselineSnapshot.dedicatedEqPreampDb?.coerceAtMost(0.0),
        spec = spec,
    )

    private fun buildWorkingCopy(
        baselineSnapshot: HardwareEqSnapshot,
        filters: List<HardwareEqFilter>,
        plannedSafetyGainDb: Double?,
        spec: HardwareEqEditSpec,
    ): HardwareEqEditWorkingCopy {
        require(baselineSnapshot.deviceId == spec.deviceId) { "Editor spec/device mismatch." }
        require(filters.map(HardwareEqFilter::index).toSet() == baselineSnapshot.filters.map(HardwareEqFilter::index).toSet()) {
            "Local editor cannot add or remove hardware bands."
        }

        val issues = validate(filters, spec).toMutableList()
        val response = HardwareEqResponseEvaluator.evaluate(filters)
        if (response == null) issues += HardwareEqEditIssue.ResponseUnavailable
        val headroom = response?.let { curve ->
            val required = conservativeRequiredHeadroomDb(curve, spec.headroomGainStepDb)
            assessHeadroom(
                requiredGainDb = required,
                plannedGainDb = plannedSafetyGainDb,
                minimumVerifiedGainDb = spec.minimumVerifiedHeadroomGainDb,
            )
        }

        return HardwareEqEditWorkingCopy(
            baselineSnapshot = baselineSnapshot,
            filters = filters.sortedBy(HardwareEqFilter::index),
            plannedSafetyGainDb = plannedSafetyGainDb,
            headroomMechanism = spec.headroomMechanism,
            responseCurve = response,
            headroomAssessment = headroom,
            issues = issues,
            differences = differences(baselineSnapshot.filters, filters),
        )
    }

    private fun validate(
        filters: List<HardwareEqFilter>,
        spec: HardwareEqEditSpec,
    ): List<HardwareEqEditIssue> = buildList {
        if (filters.size != spec.bandCount) {
            add(HardwareEqEditIssue.BandCountMismatch(spec.bandCount, filters.size))
        }
        filters.forEach { filter ->
            if (filter.type !in spec.supportedFilterTypes) {
                add(HardwareEqEditIssue.UnsupportedFilterType(filter.index, filter.type))
            }
            validateNumeric(
                bandIndex = filter.index,
                field = HardwareEqDifferenceField.FREQUENCY_HZ,
                value = filter.frequencyHz,
                range = spec.frequencyRangeHz,
                step = spec.frequencyStepHz,
            )?.let(::add)
            validateNumeric(
                bandIndex = filter.index,
                field = HardwareEqDifferenceField.GAIN_DB,
                value = filter.gainDb,
                range = spec.gainRangeDb,
                step = spec.gainStepDb,
            )?.let(::add)
            validateNumeric(
                bandIndex = filter.index,
                field = HardwareEqDifferenceField.Q,
                value = filter.q,
                range = spec.qRange,
                step = spec.qStep,
            )?.let(::add)
            spec.normalGainRangeDb?.takeIf { normal ->
                filter.gainDb in spec.gainRangeDb && filter.gainDb !in normal
            }?.let { normal ->
                add(HardwareEqEditIssue.GainOutsideNormalRange(filter.index, normal))
            }
        }
    }

    private fun validateNumeric(
        bandIndex: Int,
        field: HardwareEqDifferenceField,
        value: Double,
        range: DacNumericRange,
        step: Double,
    ): HardwareEqEditIssue? {
        if (value !in range) return HardwareEqEditIssue.OutOfRange(bandIndex, field, range)
        val units = value / step
        if (abs(units - round(units)) > REPRESENTABLE_STEP_EPSILON) {
            return HardwareEqEditIssue.NotRepresentableAtStep(bandIndex, field, step)
        }
        return null
    }

    private fun conservativeRequiredHeadroomDb(
        curve: HardwareEqResponseCurve,
        stepDb: Double?,
    ): Double {
        val raw = -curve.maximumGainDb.coerceAtLeast(0.0)
        if (abs(raw) <= HEADROOM_EPSILON_DB) return 0.0
        return if (stepDb != null) floor(raw / stepDb) * stepDb else raw
    }

    private fun assessHeadroom(
        requiredGainDb: Double,
        plannedGainDb: Double?,
        minimumVerifiedGainDb: Double?,
    ): DacHeadroomAssessment {
        val status = when {
            minimumVerifiedGainDb != null && requiredGainDb < minimumVerifiedGainDb - HEADROOM_EPSILON_DB ->
                DacHeadroomStatus.DEVICE_LIMITED
            requiredGainDb >= -HEADROOM_EPSILON_DB -> DacHeadroomStatus.SAFE
            plannedGainDb == null -> DacHeadroomStatus.ADJUSTMENT_REQUIRED
            plannedGainDb > requiredGainDb + HEADROOM_EPSILON_DB -> DacHeadroomStatus.ADJUSTMENT_REQUIRED
            else -> DacHeadroomStatus.SAFE
        }
        return DacHeadroomAssessment(
            requiredGainDb = requiredGainDb,
            plannedGainDb = plannedGainDb,
            minimumVerifiedGainDb = minimumVerifiedGainDb,
            status = status,
        )
    }

    private fun differences(
        baseline: List<HardwareEqFilter>,
        working: List<HardwareEqFilter>,
    ): List<HardwareEqDifference> {
        val baselineByIndex = baseline.associateBy(HardwareEqFilter::index)
        return buildList {
            working.sortedBy(HardwareEqFilter::index).forEach { actual ->
                val expected = baselineByIndex[actual.index] ?: return@forEach
                if (expected.enabled != actual.enabled) {
                    addDifference(actual.index, HardwareEqDifferenceField.ENABLED, expected.enabled, actual.enabled)
                }
                if (expected.type != actual.type) {
                    addDifference(actual.index, HardwareEqDifferenceField.FILTER_TYPE, expected.type, actual.type)
                }
                if (!sameDouble(expected.frequencyHz, actual.frequencyHz)) {
                    addDifference(actual.index, HardwareEqDifferenceField.FREQUENCY_HZ, expected.frequencyHz, actual.frequencyHz)
                }
                if (!sameDouble(expected.gainDb, actual.gainDb)) {
                    addDifference(actual.index, HardwareEqDifferenceField.GAIN_DB, expected.gainDb, actual.gainDb)
                }
                if (!sameDouble(expected.q, actual.q)) {
                    addDifference(actual.index, HardwareEqDifferenceField.Q, expected.q, actual.q)
                }
            }
        }
    }

    private fun MutableList<HardwareEqDifference>.addDifference(
        bandIndex: Int,
        field: HardwareEqDifferenceField,
        expected: Any,
        actual: Any,
    ) {
        add(
            HardwareEqDifference(
                bandIndex = bandIndex,
                field = field,
                expectedValue = expected.toString(),
                actualValue = actual.toString(),
            ),
        )
    }

    private fun sameDouble(left: Double, right: Double): Boolean = abs(left - right) <= DIFFERENCE_EPSILON
}

private const val REPRESENTABLE_STEP_EPSILON = 1e-8
private const val HEADROOM_EPSILON_DB = 1e-9
private const val DIFFERENCE_EPSILON = 1e-9
