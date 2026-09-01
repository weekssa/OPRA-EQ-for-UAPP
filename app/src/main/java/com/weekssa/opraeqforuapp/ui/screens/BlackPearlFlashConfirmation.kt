package com.weekssa.opraeqforuapp.ui.screens

/**
 * Adds representable-but-not-yet-hardware-validated cautions to the existing Flash confirmation.
 * The two-argument overload remains the common confirmation text for profiles with no caution.
 */
internal fun blackPearlFlashConfirmation(
    displayName: String,
    gainAdjustmentDb: Double,
    warning: String?,
): String {
    val base = blackPearlFlashConfirmation(displayName, gainAdjustmentDb)
    return warning?.takeIf(String::isNotBlank)?.let { "$base\n\n$it" } ?: base
}
