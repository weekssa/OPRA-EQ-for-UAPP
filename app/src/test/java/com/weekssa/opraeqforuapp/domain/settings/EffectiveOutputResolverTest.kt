package com.weekssa.opraeqforuapp.domain.settings

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import org.junit.Test

class EffectiveOutputResolverTest {
    @Test
    fun automaticUsesSingleConnectedBlackPearlEvenWhenManualFallbackIsUapp() {
        val result = EffectiveOutputResolver.resolve(
            behavior = OutputBehavior.Automatic,
            manualFallback = ExportDevice.UAPP,
            presentDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL),
        )

        assertThat(result.output).isEqualTo(ExportDevice.BLACK_PEARL)
        assertThat(result.automaticDevice).isEqualTo(DacDeviceId.TRN_BLACK_PEARL)
        assertThat(result.isUsingAutomaticHardware).isTrue()
    }

    @Test
    fun automaticUsesSingleConnectedFiioWithoutPreselection() {
        val result = EffectiveOutputResolver.resolve(
            behavior = OutputBehavior.Automatic,
            manualFallback = ExportDevice.UAPP,
            presentDeviceIds = setOf(DacDeviceId.FIIO_JA11),
        )

        assertThat(result.output).isEqualTo(ExportDevice.FIIO_JA11)
        assertThat(result.automaticDevice).isEqualTo(DacDeviceId.FIIO_JA11)
    }

    @Test
    fun automaticFallsBackWhenNoCurrentProductDacIsPresent() {
        val result = EffectiveOutputResolver.resolve(
            behavior = OutputBehavior.Automatic,
            manualFallback = ExportDevice.POWERAMP,
            presentDeviceIds = emptySet(),
        )

        assertThat(result.output).isEqualTo(ExportDevice.POWERAMP)
        assertThat(result.isUsingAutomaticHardware).isFalse()
    }

    @Test
    fun automaticDoesNotGuessWhenTwoSupportedDacsArePresent() {
        val result = EffectiveOutputResolver.resolve(
            behavior = OutputBehavior.Automatic,
            manualFallback = ExportDevice.UAPP,
            presentDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL, DacDeviceId.FIIO_JA11),
        )

        assertThat(result.output).isEqualTo(ExportDevice.UAPP)
        assertThat(result.automaticDevice).isNull()
        assertThat(result.multipleSupportedDacsPresent).isTrue()
    }

    @Test
    fun legacyJcallyNeverBecomesAutomaticProductOutput() {
        val result = EffectiveOutputResolver.resolve(
            behavior = OutputBehavior.Automatic,
            manualFallback = ExportDevice.UAPP,
            presentDeviceIds = setOf(DacDeviceId.JCALLY_JM12_STOCK),
        )

        assertThat(result.output).isEqualTo(ExportDevice.UAPP)
        assertThat(result.isUsingAutomaticHardware).isFalse()
    }

    @Test
    fun manualAlwaysHonorsSavedOverride() {
        val result = EffectiveOutputResolver.resolve(
            behavior = OutputBehavior.Manual,
            manualFallback = ExportDevice.POWERAMP,
            presentDeviceIds = setOf(DacDeviceId.FIIO_JA11),
        )

        assertThat(result.output).isEqualTo(ExportDevice.POWERAMP)
        assertThat(result.isUsingAutomaticHardware).isFalse()
    }
}
