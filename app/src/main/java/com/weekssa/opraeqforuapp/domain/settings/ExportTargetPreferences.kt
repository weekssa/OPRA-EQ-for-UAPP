package com.weekssa.opraeqforuapp.domain.settings

import com.weekssa.opraeqforuapp.domain.export.ExportDevice

data class ExportTargetPreferences(
    val selectedTargets: Set<ExportDevice> = setOf(ExportDevice.UAPP),
    val activeTarget: ExportDevice = ExportDevice.UAPP,
) {
    fun isSelected(device: ExportDevice): Boolean = device in selectedTargets

    fun withTarget(device: ExportDevice, enabled: Boolean): ExportTargetPreferences {
        if (!device.selectableInV03) return this
        val next = if (enabled) selectedTargets + device else selectedTargets - device
        if (next.isEmpty()) return this
        val nextActive = if (activeTarget in next) activeTarget else ordered(next).first()
        return copy(selectedTargets = next, activeTarget = nextActive)
    }

    fun withActiveTarget(device: ExportDevice): ExportTargetPreferences =
        if (device.selectableInV03) {
            copy(selectedTargets = selectedTargets + device, activeTarget = device)
        } else {
            this
        }

    companion object {
        fun normalize(
            selectedTargets: Set<ExportDevice>,
            activeTarget: ExportDevice?,
        ): ExportTargetPreferences {
            val enabled = selectedTargets
                .filterTo(linkedSetOf())(ExportDevice::selectableInV03)
                .ifEmpty { linkedSetOf(ExportDevice.UAPP) }
            val active = activeTarget?.takeIf { it in enabled } ?: ordered(enabled).first()
            return ExportTargetPreferences(enabled, active)
        }

        private fun ordered(devices: Set<ExportDevice>): List<ExportDevice> =
            ExportDevice.selectableOutputs.filter(devices::contains)
    }
}
