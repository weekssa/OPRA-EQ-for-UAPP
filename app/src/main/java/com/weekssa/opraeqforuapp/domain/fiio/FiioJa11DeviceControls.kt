package com.weekssa.opraeqforuapp.domain.fiio

import com.weekssa.opraeqforuapp.domain.dac.DacControlDescriptor
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlSafetyClass
import com.weekssa.opraeqforuapp.domain.dac.DacControlSection
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacDiscreteOption
import com.weekssa.opraeqforuapp.domain.dac.DacNumericRange
import com.weekssa.opraeqforuapp.domain.dac.DacNumericUnit
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol

/**
 * Complete software-established JA11 DEVICE state that can be read without guessing.
 *
 * This type is not a physical-qualification claim. It deliberately includes only values with an
 * independently corroborated run-mode read path. SPDIF is absent because no JA11 SPDIF control has
 * been established; future FiiO models may add that capability independently.
 */
data class FiioJa11DeviceSnapshot(
    val sessionGeneration: Long,
    val usbProductId: Int,
    val firmwareVersion: String,
    val sampleRateLabel: String,
    val outputVolume: Int,
    val headsetControlEnabled: Boolean,
    val eqProgram: FiioJa11Protocol.EqProgram,
    val uacMode: FiioJa11Protocol.UacMode,
) {
    init {
        require(sessionGeneration > 0L)
        require(FiioJa11Protocol.supportsProductId(usbProductId))
        require(firmwareVersion.isNotBlank())
        require(sampleRateLabel.isNotBlank())
        require(outputVolume in FiioJa11Protocol.MIN_OUTPUT_VOLUME..FiioJa11Protocol.MAX_OUTPUT_VOLUME)
    }
}

/** Typed DEVICE-control contract for FiiO/JadeAudio JA11. */
object FiioJa11DeviceControls {
    val OUTPUT_VOLUME = DacControlId("fiio_ja11.output_volume")
    val EQ_PROGRAM = DacControlId("fiio_ja11.eq_program")
    val HEADSET_CONTROL = DacControlId("fiio_ja11.headset_control")
    val UAC_MODE = DacControlId("fiio_ja11.uac_mode")
    val SAMPLE_RATE = DacControlId("fiio_ja11.sample_rate")
    val FIRMWARE = DacControlId("fiio_ja11.firmware")

    val descriptors: List<DacControlDescriptor> = listOf(
        DacControlDescriptor.Numeric(
            id = OUTPUT_VOLUME,
            section = DacControlSection.PLAYBACK,
            safetyClass = DacControlSafetyClass.LEVEL_SENSITIVE,
            unit = DacNumericUnit.NONE,
            absoluteRange = DacNumericRange(
                FiioJa11Protocol.MIN_OUTPUT_VOLUME.toDouble(),
                FiioJa11Protocol.MAX_OUTPUT_VOLUME.toDouble(),
            ),
            step = 1.0,
        ),
        DacControlDescriptor.Discrete(
            id = EQ_PROGRAM,
            section = DacControlSection.ADVANCED,
            safetyClass = DacControlSafetyClass.LEVEL_SENSITIVE,
            options = FiioJa11Protocol.EqProgram.entries.map { program ->
                DacDiscreteOption(program.name.lowercase(), program.technicalLabel)
            },
        ),
        DacControlDescriptor.Toggle(
            id = HEADSET_CONTROL,
            section = DacControlSection.MICROPHONE_INPUT,
            safetyClass = DacControlSafetyClass.NORMAL,
        ),
        DacControlDescriptor.Discrete(
            id = UAC_MODE,
            section = DacControlSection.USB_SYSTEM,
            safetyClass = DacControlSafetyClass.NORMAL,
            options = FiioJa11Protocol.UacMode.entries.map { mode ->
                DacDiscreteOption(mode.name.lowercase(), mode.technicalLabel)
            },
        ),
        DacControlDescriptor.ReadOnlyText(id = SAMPLE_RATE, section = DacControlSection.USB_SYSTEM),
        DacControlDescriptor.ReadOnlyText(id = FIRMWARE),
    )

    /** Software paths exist, but physical JA11 production qualification is still pending. */
    val softwareImplementedWriteControlIds: Set<DacControlId> = setOf(
        OUTPUT_VOLUME,
        EQ_PROGRAM,
        HEADSET_CONTROL,
        UAC_MODE,
    )

    val productionQualifiedWriteControlIds: Set<DacControlId> = emptySet()

    /** These writes are expected to invalidate the current USB session/re-enumerate the JA11. */
    fun requiresSessionRestart(controlId: DacControlId): Boolean =
        controlId == HEADSET_CONTROL || controlId == UAC_MODE

    fun descriptor(controlId: DacControlId): DacControlDescriptor? =
        descriptors.firstOrNull { it.id == controlId }

    fun valueFromSnapshot(controlId: DacControlId, snapshot: FiioJa11DeviceSnapshot): DacControlValue? = when (controlId) {
        OUTPUT_VOLUME -> DacControlValue.Numeric(snapshot.outputVolume.toDouble())
        EQ_PROGRAM -> DacControlValue.Discrete(snapshot.eqProgram.name.lowercase())
        HEADSET_CONTROL -> DacControlValue.Toggle(snapshot.headsetControlEnabled)
        UAC_MODE -> DacControlValue.Discrete(snapshot.uacMode.name.lowercase())
        SAMPLE_RATE -> DacControlValue.Text(snapshot.sampleRateLabel)
        FIRMWARE -> DacControlValue.Text(snapshot.firmwareVersion)
        else -> null
    }

    fun eqProgram(valueId: String): FiioJa11Protocol.EqProgram? =
        FiioJa11Protocol.EqProgram.entries.firstOrNull { it.name.equals(valueId, ignoreCase = true) }

    fun uacMode(valueId: String): FiioJa11Protocol.UacMode? =
        FiioJa11Protocol.UacMode.entries.firstOrNull { it.name.equals(valueId, ignoreCase = true) }
}
