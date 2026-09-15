package com.weekssa.opraeqforuapp.domain.dac

import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

enum class DacDeviceId {
    TRN_BLACK_PEARL,
    FIIO_JA11,
    JCALLY_JM12_STOCK,
}

enum class DacValidationStatus {
    HARDWARE_QUALIFIED,
    HARDWARE_VALIDATION_PENDING,
}

enum class DacMetadataOrigin {
    USB_REPORTED,
    DEVICE_REPORTED,
    STATIC_KNOWN,
}

data class DacMetadataValue<T>(
    val value: T,
    val origin: DacMetadataOrigin,
)

data class DacDeviceIdentity(
    val deviceId: DacDeviceId,
    val manufacturer: DacMetadataValue<String>,
    val model: DacMetadataValue<String>,
    val usbVendorId: Int,
    val usbProductId: Int,
    val usbProductName: DacMetadataValue<String>? = null,
    val serialNumber: DacMetadataValue<String>? = null,
    val firmwareVersion: DacMetadataValue<String>? = null,
    val validationStatus: DacValidationStatus,
) {
    init {
        require(usbVendorId in 0..0xffff) { "USB vendor ID must fit 16 bits" }
        require(usbProductId in 0..0xffff) { "USB product ID must fit 16 bits" }
        require(manufacturer.value.isNotBlank()) { "Manufacturer must not be blank" }
        require(model.value.isNotBlank()) { "Model must not be blank" }
    }
}

enum class DacSessionErrorReason {
    PERMISSION_DENIED,
    DEVICE_NOT_FOUND,
    INTERFACE_NOT_FOUND,
    READ_FAILED,
    PROTOCOL_MISMATCH,
    UNKNOWN,
}

sealed interface DacSessionState {
    data object NotPresent : DacSessionState

    data class PermissionRequired(
        val identity: DacDeviceIdentity,
    ) : DacSessionState

    data class Connecting(
        val identity: DacDeviceIdentity,
    ) : DacSessionState

    data class Reading(
        val identity: DacDeviceIdentity,
        val sessionGeneration: Long,
    ) : DacSessionState {
        init {
            require(sessionGeneration > 0) { "Session generation must be positive" }
        }
    }

    data class Connected(
        val identity: DacDeviceIdentity,
        val sessionGeneration: Long,
        val verifiedAtEpochMillis: Long,
    ) : DacSessionState {
        init {
            require(sessionGeneration > 0) { "Session generation must be positive" }
            require(verifiedAtEpochMillis >= 0) { "Verified timestamp must not be negative" }
        }
    }

    data class Disconnected(
        val identity: DacDeviceIdentity?,
        val lastReadAtEpochMillis: Long? = null,
    ) : DacSessionState {
        init {
            require(lastReadAtEpochMillis == null || lastReadAtEpochMillis >= 0) {
                "Last-read timestamp must not be negative"
            }
        }
    }

    data class Error(
        val identity: DacDeviceIdentity?,
        val reason: DacSessionErrorReason,
    ) : DacSessionState
}

data class DacControlId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Control ID must not be blank" }
    }
}

enum class DacControlSection {
    PLAYBACK,
    DAC_FILTER,
    OUTPUT,
    MICROPHONE_INPUT,
    USB_SYSTEM,
    ADVANCED,
    DEVICE_INFORMATION,
}

enum class DacControlSafetyClass {
    NORMAL,
    LEVEL_SENSITIVE,
    DESTRUCTIVE,
}

enum class DacNumericUnit {
    DB,
    HZ,
    PERCENT,
    NONE,
}

data class DacNumericRange(
    val minimum: Double,
    val maximum: Double,
) {
    init {
        require(minimum.isFinite() && maximum.isFinite()) { "Numeric range must be finite" }
        require(minimum <= maximum) { "Numeric range minimum must not exceed maximum" }
    }

    operator fun contains(value: Double): Boolean = value in minimum..maximum
}

data class DacDiscreteOption(
    val valueId: String,
    val technicalLabel: String,
) {
    init {
        require(valueId.isNotBlank()) { "Discrete option ID must not be blank" }
        require(technicalLabel.isNotBlank()) { "Discrete option label must not be blank" }
    }
}

sealed interface DacControlDescriptor {
    val id: DacControlId
    val section: DacControlSection
    val safetyClass: DacControlSafetyClass
    val readable: Boolean
    val writable: Boolean

    data class Numeric(
        override val id: DacControlId,
        override val section: DacControlSection,
        override val safetyClass: DacControlSafetyClass,
        override val readable: Boolean = true,
        override val writable: Boolean = true,
        val unit: DacNumericUnit,
        val absoluteRange: DacNumericRange,
        val step: Double,
        val normalRange: DacNumericRange? = null,
    ) : DacControlDescriptor {
        init {
            require(step.isFinite() && step > 0.0) { "Numeric step must be finite and positive" }
            normalRange?.let { normal ->
                require(normal.minimum >= absoluteRange.minimum && normal.maximum <= absoluteRange.maximum) {
                    "Normal range must fit inside absolute range"
                }
            }
        }
    }

    data class Discrete(
        override val id: DacControlId,
        override val section: DacControlSection,
        override val safetyClass: DacControlSafetyClass,
        override val readable: Boolean = true,
        override val writable: Boolean = true,
        val options: List<DacDiscreteOption>,
    ) : DacControlDescriptor {
        init {
            require(options.isNotEmpty()) { "Discrete controls require at least one option" }
            require(options.map { it.valueId }.distinct().size == options.size) {
                "Discrete option IDs must be unique"
            }
        }
    }

    data class Toggle(
        override val id: DacControlId,
        override val section: DacControlSection,
        override val safetyClass: DacControlSafetyClass,
        override val readable: Boolean = true,
        override val writable: Boolean = true,
    ) : DacControlDescriptor

    data class ReadOnlyText(
        override val id: DacControlId,
        override val section: DacControlSection = DacControlSection.DEVICE_INFORMATION,
        override val safetyClass: DacControlSafetyClass = DacControlSafetyClass.NORMAL,
        override val readable: Boolean = true,
        override val writable: Boolean = false,
    ) : DacControlDescriptor
}

sealed interface DacControlValue {
    data class Numeric(val value: Double) : DacControlValue {
        init {
            require(value.isFinite()) { "Numeric control values must be finite" }
        }
    }

    data class Discrete(val valueId: String) : DacControlValue {
        init {
            require(valueId.isNotBlank()) { "Discrete control value must not be blank" }
        }
    }

    data class Toggle(val enabled: Boolean) : DacControlValue

    data class Text(val value: String) : DacControlValue
}

sealed interface DacControlValidation {
    data object Valid : DacControlValidation

    data class CautionOutsideNormalRange(
        val normalRange: DacNumericRange,
    ) : DacControlValidation

    data object NotWritable : DacControlValidation
    data object WrongValueType : DacControlValidation

    data class OutOfRange(
        val absoluteRange: DacNumericRange,
    ) : DacControlValidation

    data class UnsupportedOption(
        val valueId: String,
    ) : DacControlValidation

    data class NotRepresentableAtStep(
        val step: Double,
    ) : DacControlValidation
}

fun DacControlDescriptor.validateForWrite(value: DacControlValue): DacControlValidation {
    if (!writable) return DacControlValidation.NotWritable

    return when (this) {
        is DacControlDescriptor.Numeric -> {
            val numericValue = (value as? DacControlValue.Numeric)?.value
                ?: return DacControlValidation.WrongValueType
            if (numericValue !in absoluteRange) {
                return DacControlValidation.OutOfRange(absoluteRange)
            }
            val stepsFromMinimum = (numericValue - absoluteRange.minimum) / step
            if (abs(stepsFromMinimum - round(stepsFromMinimum)) > REPRESENTABLE_EPSILON) {
                return DacControlValidation.NotRepresentableAtStep(step)
            }
            if (normalRange != null && numericValue !in normalRange) {
                DacControlValidation.CautionOutsideNormalRange(normalRange)
            } else {
                DacControlValidation.Valid
            }
        }

        is DacControlDescriptor.Discrete -> {
            val discreteValue = (value as? DacControlValue.Discrete)?.valueId
                ?: return DacControlValidation.WrongValueType
            if (options.none { it.valueId == discreteValue }) {
                DacControlValidation.UnsupportedOption(discreteValue)
            } else {
                DacControlValidation.Valid
            }
        }

        is DacControlDescriptor.Toggle -> {
            if (value is DacControlValue.Toggle) DacControlValidation.Valid
            else DacControlValidation.WrongValueType
        }

        is DacControlDescriptor.ReadOnlyText -> DacControlValidation.NotWritable
    }
}

enum class DacStateFreshness {
    CURRENT,
    LAST_READ_STALE,
}

data class DacControlState(
    val controlId: DacControlId,
    val value: DacControlValue,
    val freshness: DacStateFreshness,
    val sessionGeneration: Long,
    val verifiedAtEpochMillis: Long,
) {
    init {
        require(sessionGeneration > 0) { "Session generation must be positive" }
        require(verifiedAtEpochMillis >= 0) { "Verified timestamp must not be negative" }
    }
}

data class DacWriteIntent(
    val controlId: DacControlId,
    val requestedValue: DacControlValue,
    val expectedSessionGeneration: Long,
) {
    init {
        require(expectedSessionGeneration > 0) { "Expected session generation must be positive" }
    }
}

sealed interface VerifiedDacWriteResult {
    data class Verified(
        val controlId: DacControlId,
        val requestedValue: DacControlValue,
        val actualValue: DacControlValue,
        val sessionGeneration: Long,
    ) : VerifiedDacWriteResult

    data class ReadbackMismatch(
        val controlId: DacControlId,
        val requestedValue: DacControlValue,
        val actualValue: DacControlValue,
        val sessionGeneration: Long,
    ) : VerifiedDacWriteResult

    data class Disconnected(
        val controlId: DacControlId,
    ) : VerifiedDacWriteResult

    data class PermissionLost(
        val controlId: DacControlId,
    ) : VerifiedDacWriteResult

    data class StaleBaseline(
        val controlId: DacControlId,
        val expectedSessionGeneration: Long,
        val actualSessionGeneration: Long,
    ) : VerifiedDacWriteResult

    data class TransferFailed(
        val controlId: DacControlId,
        val stage: DacTransferStage,
    ) : VerifiedDacWriteResult
}

enum class DacTransferStage {
    READ_BASELINE,
    WRITE,
    READBACK,
    PERSIST,
    FINAL_READBACK,
}

data class HardwareEqFilter(
    val index: Int,
    val enabled: Boolean,
    val type: EqFilterType,
    val frequencyHz: Double,
    val gainDb: Double,
    val q: Double,
) {
    init {
        require(index >= 0) { "Hardware EQ filter index must not be negative" }
        require(frequencyHz.isFinite() && frequencyHz > 0.0) { "Frequency must be finite and positive" }
        require(gainDb.isFinite()) { "Gain must be finite" }
        require(q.isFinite() && q > 0.0) { "Q must be finite and positive" }
    }
}

data class HardwareEqSnapshot(
    val deviceId: DacDeviceId,
    val sessionGeneration: Long,
    val activeSlot: Int? = null,
    val filters: List<HardwareEqFilter>,
    /** Dedicated EQ preamp/headroom control only when its semantics are independently established. */
    val dedicatedEqPreampDb: Double? = null,
    /** Ordinary playback/global gain context, kept semantically distinct from source preamp. */
    val playbackGainDb: Double? = null,
    val verifiedAtEpochMillis: Long,
) {
    init {
        require(sessionGeneration > 0) { "Session generation must be positive" }
        require(activeSlot == null || activeSlot >= 0) { "Active slot must not be negative" }
        require(filters.map { it.index }.distinct().size == filters.size) {
            "Hardware EQ filter indices must be unique"
        }
        require(dedicatedEqPreampDb == null || dedicatedEqPreampDb.isFinite()) {
            "Dedicated EQ preamp must be finite"
        }
        require(playbackGainDb == null || playbackGainDb.isFinite()) {
            "Playback gain must be finite"
        }
        require(verifiedAtEpochMillis >= 0) { "Verified timestamp must not be negative" }
    }
}

data class SavedHardwareEqIdentity(
    val savedEqKey: String,
    val displayName: String,
) {
    init {
        require(savedEqKey.isNotBlank()) { "Saved EQ key must not be blank" }
        require(displayName.isNotBlank()) { "Saved EQ display name must not be blank" }
    }
}

enum class HardwareEqDifferenceField {
    ENABLED,
    FILTER_TYPE,
    FREQUENCY_HZ,
    GAIN_DB,
    Q,
    DEDICATED_EQ_PREAMP_DB,
}

data class HardwareEqDifference(
    val bandIndex: Int?,
    val field: HardwareEqDifferenceField,
    val expectedValue: String,
    val actualValue: String,
) {
    init {
        require(bandIndex == null || bandIndex >= 0) { "Band index must not be negative" }
        require(expectedValue.isNotBlank()) { "Expected value must not be blank" }
        require(actualValue.isNotBlank()) { "Actual value must not be blank" }
    }
}

sealed interface HardwareEqMatch {
    data object Flat : HardwareEqMatch

    data class Exact(
        val savedEq: SavedHardwareEqIdentity,
    ) : HardwareEqMatch

    data class ModifiedKnown(
        val savedEq: SavedHardwareEqIdentity,
        val differences: List<HardwareEqDifference>,
    ) : HardwareEqMatch {
        init {
            require(differences.isNotEmpty()) { "Modified-known EQ requires explicit differences" }
        }
    }

    data object Unknown : HardwareEqMatch
}

enum class DacHeadroomStatus {
    SAFE,
    ADJUSTMENT_REQUIRED,
    DEVICE_LIMITED,
}

data class DacHeadroomAssessment(
    /** Non-positive gain value required to keep the planned response at/below the digital ceiling. */
    val requiredGainDb: Double,
    /** Current local-plan gain value for the verified EQ-preamp/headroom mechanism, when available. */
    val plannedGainDb: Double?,
    /** Most negative verified gain the relevant mechanism can safely represent, when bounded. */
    val minimumVerifiedGainDb: Double?,
    val status: DacHeadroomStatus,
) {
    init {
        require(requiredGainDb.isFinite() && requiredGainDb <= 0.0) {
            "Required headroom gain must be finite and non-positive"
        }
        require(plannedGainDb == null || plannedGainDb.isFinite()) { "Planned gain must be finite" }
        require(minimumVerifiedGainDb == null || minimumVerifiedGainDb.isFinite()) {
            "Minimum verified gain must be finite"
        }
    }

    val additionalAttenuationDb: Double
        get() = max(0.0, (plannedGainDb ?: 0.0) - requiredGainDb)
}

private const val REPRESENTABLE_EPSILON = 1e-8
