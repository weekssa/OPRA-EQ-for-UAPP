package com.weekssa.opraeqforuapp.domain.export

import com.google.common.truth.Truth.assertThat
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import org.junit.Test

class CurrentProductOutputRegistryTest {
    @Test
    fun jcallyLegacyEnumIsNotSelectableOrVisibleInCurrentOutputs() {
        assertThat(ExportDevice.JCALLY_JM12.selectableInV03).isFalse()
        assertThat(ExportDevice.JCALLY_JM12.eqCapabilities).isNull()
        assertThat(ExportDevice.selectableOutputs).doesNotContain(ExportDevice.JCALLY_JM12)
    }

    @Test
    fun currentHardwareRoadmapKeepsBlackPearlAndFiioVisible() {
        val hardware = ExportDevice.selectableOutputs.filter(ExportDevice::isHardwareOutput)
        assertThat(hardware).containsAtLeast(ExportDevice.BLACK_PEARL, ExportDevice.FIIO_JA11)
        assertThat(hardware).doesNotContain(ExportDevice.JCALLY_JM12)
    }

    @Test
    fun persistedLegacyJcallySelectionNormalizesBackToSafeCurrentOutput() {
        val normalized = ExportTargetPreferences.normalize(
            selectedTargets = setOf(ExportDevice.JCALLY_JM12),
            activeTarget = ExportDevice.JCALLY_JM12,
        )

        assertThat(normalized.selectedTargets).containsExactly(ExportDevice.UAPP)
        assertThat(normalized.activeTarget).isEqualTo(ExportDevice.UAPP)
    }
}
