package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity

/**
 * Keeps informational Exact/Optimized representation text separate from safety cautions so the UI
 * can reserve the stronger "Flash anyway" action for an actual hardware caution.
 */
internal fun blackPearlFlashConfirmation(
    displayName: String,
    gainAdjustmentDb: Double,
    fidelity: DevicePresetFidelity,
    adaptationSummary: String,
    warning: String?,
): String {
    val base = blackPearlFlashConfirmation(displayName, gainAdjustmentDb)
    val fidelityLabel = when (fidelity) {
        DevicePresetFidelity.EXACT -> "Exact"
        DevicePresetFidelity.OPTIMIZED -> "Optimized"
    }
    val status = "$fidelityLabel · $adaptationSummary."
    return warning?.takeIf(String::isNotBlank)?.let { "$base\n\n$status\n\n$it" }
        ?: "$base\n\n$status"
}

/** Legacy overload retained for focused text tests and older callers while v0.5 UI is synchronized. */
internal fun blackPearlFlashConfirmation(
    displayName: String,
    gainAdjustmentDb: Double,
    warning: String?,
): String {
    val base = blackPearlFlashConfirmation(displayName, gainAdjustmentDb)
    return warning?.takeIf(String::isNotBlank)?.let { "$base\n\n$it" } ?: base
}
