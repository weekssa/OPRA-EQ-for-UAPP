package com.weekssa.opraeqforuapp.domain.settings

import com.weekssa.opraeqforuapp.domain.export.ExportDevice

data class ExportTargetPreferences(
    val selectedTargets: Set<ExportDevice> = setOf(ExportDevice.UAPP),
    val showUnexportablePresets: Boolean = true,
) {
    fun isSelected(device: ExportDevice): Boolean = device in selectedTargets

    fun withTarget(device: ExportDevice, enabled: Boolean): ExportTargetPreferences =
        copy(
            selectedTargets = if (enabled) {
                selectedTargets + device
            } else {
                selectedTargets - device
            },
        )
}
