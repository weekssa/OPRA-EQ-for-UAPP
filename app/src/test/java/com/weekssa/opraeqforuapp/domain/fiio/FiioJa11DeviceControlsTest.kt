package com.weekssa.opraeqforuapp.domain.fiio

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.DacControlSafetyClass
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import org.junit.Test

class FiioJa11DeviceControlsTest {
    private val snapshot = FiioJa11DeviceSnapshot(
        sessionGeneration = 7L,
        usbProductId = FiioJa11Protocol.PRODUCT_ID_UAC_2,
        firmwareVersion = "2.20",
        sampleRateLabel = "352.8 kHz",
        outputVolume = 59,
        headsetControlEnabled = true,
        eqProgram = FiioJa11Protocol.EqProgram.USER_1,
        uacMode = FiioJa11Protocol.UacMode.UAC_2,
    )

    @Test
    fun descriptorsCoverEstablishedJa11ControlsWithoutInventingSpdif() {
        assertThat(FiioJa11DeviceControls.descriptors.map { it.id.value }).containsExactly(
            "fiio_ja11.output_volume",
            "fiio_ja11.eq_program",
            "fiio_ja11.headset_control",
            "fiio_ja11.uac_mode",
            "fiio_ja11.sample_rate",
            "fiio_ja11.firmware",
        )
        assertThat(FiioJa11DeviceControls.descriptors.map { it.id.value }.any { "spdif" in it }).isFalse()
        assertThat(FiioJa11DeviceControls.descriptor(FiioJa11DeviceControls.OUTPUT_VOLUME)?.safetyClass)
            .isEqualTo(DacControlSafetyClass.LEVEL_SENSITIVE)
    }

    @Test
    fun allEstablishedWritesAreSoftwareImplementedButNotPhysicallyQualified() {
        assertThat(FiioJa11DeviceControls.softwareImplementedWriteControlIds).containsExactly(
            FiioJa11DeviceControls.OUTPUT_VOLUME,
            FiioJa11DeviceControls.EQ_PROGRAM,
            FiioJa11DeviceControls.HEADSET_CONTROL,
            FiioJa11DeviceControls.UAC_MODE,
        )
        assertThat(FiioJa11DeviceControls.productionQualifiedWriteControlIds).isEmpty()
        assertThat(FiioJa11DeviceControls.requiresSessionRestart(FiioJa11DeviceControls.HEADSET_CONTROL)).isTrue()
        assertThat(FiioJa11DeviceControls.requiresSessionRestart(FiioJa11DeviceControls.UAC_MODE)).isTrue()
        assertThat(FiioJa11DeviceControls.requiresSessionRestart(FiioJa11DeviceControls.OUTPUT_VOLUME)).isFalse()
    }

    @Test
    fun snapshotValuesRemainTypedAndActual() {
        assertThat(FiioJa11DeviceControls.valueFromSnapshot(FiioJa11DeviceControls.OUTPUT_VOLUME, snapshot))
            .isEqualTo(DacControlValue.Numeric(59.0))
        assertThat(FiioJa11DeviceControls.valueFromSnapshot(FiioJa11DeviceControls.HEADSET_CONTROL, snapshot))
            .isEqualTo(DacControlValue.Toggle(true))
        assertThat(FiioJa11DeviceControls.valueFromSnapshot(FiioJa11DeviceControls.EQ_PROGRAM, snapshot))
            .isEqualTo(DacControlValue.Discrete("user_1"))
        assertThat(FiioJa11DeviceControls.valueFromSnapshot(FiioJa11DeviceControls.UAC_MODE, snapshot))
            .isEqualTo(DacControlValue.Discrete("uac_2"))
        assertThat(FiioJa11DeviceControls.valueFromSnapshot(FiioJa11DeviceControls.SAMPLE_RATE, snapshot))
            .isEqualTo(DacControlValue.Text("352.8 kHz"))
    }
}
