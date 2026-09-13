package com.weekssa.opraeqforuapp.domain.blackpearl

import com.weekssa.opraeqforuapp.domain.dac.DacControlDescriptor
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlSafetyClass
import com.weekssa.opraeqforuapp.domain.dac.DacControlSection
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacDiscreteOption
import com.weekssa.opraeqforuapp.domain.dac.DacNumericRange
import com.weekssa.opraeqforuapp.domain.dac.DacNumericUnit

/**
 * Stable typed Black Pearl DEVICE-control contract.
 *
 * These descriptors are software capability, not physical-qualification claims. The app may expose
 * them only through the explicit candidate/qualification UX until each write path passes its own
 * hardware gate. Firmware remains read-only.
 */
object BlackPearlDeviceControls {
    val DAC_FILTER = DacControlId("black_pearl.dac_filter")
    val GAIN_MODE = DacControlId("black_pearl.gain_mode")
    val AMP_TOPOLOGY = DacControlId("black_pearl.amp_topology")
    val BALANCE_DB = DacControlId("black_pearl.balance_db")
    val MIC_GAIN_DB = DacControlId("black_pearl.mic_gain_db")
    val PLAYBACK_GAIN_DB = DacControlId("black_pearl.playback_gain_db")
    val FIRMWARE = DacControlId("black_pearl.firmware")

    const val FILTER_FAST_LL = "fast_ll"
    const val FILTER_FAST_PC = "fast_pc"
    const val FILTER_SLOW_LL = "slow_ll"
    const val FILTER_SLOW_PC = "slow_pc"
    const val FILTER_NOS = "nos"

    const val GAIN_LOW = "low"
    const val GAIN_HIGH = "high"

    const val AMP_CLASS_H = "class_h"
    const val AMP_CLASS_AB = "class_ab"

    val descriptors: List<DacControlDescriptor> = listOf(
        DacControlDescriptor.Numeric(
            id = PLAYBACK_GAIN_DB,
            section = DacControlSection.PLAYBACK,
            safetyClass = DacControlSafetyClass.LEVEL_SENSITIVE,
            unit = DacNumericUnit.DB,
            absoluteRange = DacNumericRange(
                minimum = BlackPearlProtocol.rawDeltaToGainDb(BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW),
                maximum = BlackPearlProtocol.rawDeltaToGainDb(BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW),
            ),
            step = 1.0 / BlackPearlProtocol.GLOBAL_GAIN_RAW_PER_DB,
        ),
        DacControlDescriptor.Discrete(
            id = DAC_FILTER,
            section = DacControlSection.DAC_FILTER,
            safetyClass = DacControlSafetyClass.NORMAL,
            options = listOf(
                DacDiscreteOption(FILTER_FAST_LL, "FAST-LL"),
                DacDiscreteOption(FILTER_FAST_PC, "Fast-PC"),
                DacDiscreteOption(FILTER_SLOW_LL, "Slow-LL"),
                DacDiscreteOption(FILTER_SLOW_PC, "SLOW-PC"),
                DacDiscreteOption(FILTER_NOS, "NOS"),
            ),
        ),
        DacControlDescriptor.Discrete(
            id = GAIN_MODE,
            section = DacControlSection.OUTPUT,
            safetyClass = DacControlSafetyClass.LEVEL_SENSITIVE,
            options = listOf(
                DacDiscreteOption(GAIN_LOW, "LOW"),
                DacDiscreteOption(GAIN_HIGH, "HIGH"),
            ),
        ),
        DacControlDescriptor.Discrete(
            id = AMP_TOPOLOGY,
            section = DacControlSection.OUTPUT,
            safetyClass = DacControlSafetyClass.LEVEL_SENSITIVE,
            options = listOf(
                DacDiscreteOption(AMP_CLASS_H, "CLASS H"),
                DacDiscreteOption(AMP_CLASS_AB, "CLASS AB"),
            ),
        ),
        DacControlDescriptor.Numeric(
            id = BALANCE_DB,
            section = DacControlSection.OUTPUT,
            safetyClass = DacControlSafetyClass.NORMAL,
            unit = DacNumericUnit.DB,
            absoluteRange = DacNumericRange(
                BlackPearlDeviceControlReadCodec.BALANCE_MIN_DB.toDouble(),
                BlackPearlDeviceControlReadCodec.BALANCE_MAX_DB.toDouble(),
            ),
            step = 1.0,
        ),
        DacControlDescriptor.Numeric(
            id = MIC_GAIN_DB,
            section = DacControlSection.MICROPHONE_INPUT,
            safetyClass = DacControlSafetyClass.NORMAL,
            unit = DacNumericUnit.DB,
            absoluteRange = DacNumericRange(
                BlackPearlDeviceControlReadCodec.MIC_GAIN_MIN_DB.toDouble(),
                BlackPearlDeviceControlReadCodec.MIC_GAIN_MAX_DB.toDouble(),
            ),
            step = 1.0,
        ),
        DacControlDescriptor.ReadOnlyText(id = FIRMWARE),
    )

    fun descriptor(controlId: DacControlId): DacControlDescriptor? =
        descriptors.firstOrNull { it.id == controlId }

    fun valueFromSnapshot(
        controlId: DacControlId,
        snapshot: BlackPearlDeviceQualificationSnapshot,
    ): DacControlValue? = when (controlId) {
        DAC_FILTER -> DacControlValue.Discrete(filterValueId(snapshot.filterCode))
        GAIN_MODE -> DacControlValue.Discrete(gainValueId(snapshot.gainModeCode))
        AMP_TOPOLOGY -> DacControlValue.Discrete(ampValueId(snapshot.ampTopologyCode))
        BALANCE_DB -> snapshot.signedBalanceDb?.let { DacControlValue.Numeric(it.toDouble()) }
        MIC_GAIN_DB -> DacControlValue.Numeric(snapshot.micGainDb.toDouble())
        PLAYBACK_GAIN_DB -> DacControlValue.Numeric(snapshot.playbackGainDb)
        FIRMWARE -> DacControlValue.Text(snapshot.firmwareVersion)
        else -> null
    }

    fun filterCode(valueId: String): Int? = when (valueId) {
        FILTER_FAST_LL -> BlackPearlDeviceControlReadCodec.FILTER_FAST_LL
        FILTER_FAST_PC -> BlackPearlDeviceControlReadCodec.FILTER_FAST_PC
        FILTER_SLOW_LL -> BlackPearlDeviceControlReadCodec.FILTER_SLOW_LL
        FILTER_SLOW_PC -> BlackPearlDeviceControlReadCodec.FILTER_SLOW_PC
        FILTER_NOS -> BlackPearlDeviceControlReadCodec.FILTER_NOS
        else -> null
    }

    fun gainModeCode(valueId: String): Int? = when (valueId) {
        GAIN_LOW -> BlackPearlDeviceControlReadCodec.GAIN_MODE_LOW
        GAIN_HIGH -> BlackPearlDeviceControlReadCodec.GAIN_MODE_HIGH
        else -> null
    }

    fun ampTopologyCode(valueId: String): Int? = when (valueId) {
        AMP_CLASS_H -> BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_H
        AMP_CLASS_AB -> BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_AB
        else -> null
    }

    fun playbackGainRaw(gainDb: Double): Int? {
        if (!gainDb.isFinite()) return null
        val raw = BlackPearlProtocol.gainDbToRawDelta(gainDb)
        return raw.takeIf { it in BlackPearlProtocol.GLOBAL_GAIN_MIN_RAW..BlackPearlProtocol.GLOBAL_GAIN_MAX_RAW }
    }

    private fun filterValueId(code: Int): String = when (code) {
        BlackPearlDeviceControlReadCodec.FILTER_FAST_LL -> FILTER_FAST_LL
        BlackPearlDeviceControlReadCodec.FILTER_FAST_PC -> FILTER_FAST_PC
        BlackPearlDeviceControlReadCodec.FILTER_SLOW_LL -> FILTER_SLOW_LL
        BlackPearlDeviceControlReadCodec.FILTER_SLOW_PC -> FILTER_SLOW_PC
        BlackPearlDeviceControlReadCodec.FILTER_NOS -> FILTER_NOS
        else -> error("Validated Black Pearl filter code unexpectedly missing.")
    }

    private fun gainValueId(code: Int): String = when (code) {
        BlackPearlDeviceControlReadCodec.GAIN_MODE_LOW -> GAIN_LOW
        BlackPearlDeviceControlReadCodec.GAIN_MODE_HIGH -> GAIN_HIGH
        else -> error("Validated Black Pearl gain mode unexpectedly missing.")
    }

    private fun ampValueId(code: Int): String = when (code) {
        BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_H -> AMP_CLASS_H
        BlackPearlDeviceControlReadCodec.AMP_TOPOLOGY_CLASS_AB -> AMP_CLASS_AB
        else -> error("Validated Black Pearl amplifier topology unexpectedly missing.")
    }
}
