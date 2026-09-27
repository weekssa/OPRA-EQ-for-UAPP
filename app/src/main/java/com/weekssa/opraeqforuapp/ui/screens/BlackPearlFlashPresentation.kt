package com.weekssa.opraeqforuapp.ui.screens

import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult
import java.util.Locale

internal data class BlackPearlFlashPresentation(
    val message: String,
    val verified: Boolean,
)

internal fun blackPearlFlashPresentation(
    result: BlackPearlFlashResult,
): BlackPearlFlashPresentation = when (result) {
    is BlackPearlFlashResult.Success -> BlackPearlFlashPresentation(
        message = buildString {
            append("Flash successful · TRN Black Pearl EQ was saved and verified · Final hardware readback matched.")
            append(" Playback-gain adjustment: ")
            append(String.format(Locale.US, "%+.2f", result.appliedPlaybackGainDb))
            append(" dB.")
            result.warning?.let { warning ->
                append(' ')
                append(warning)
            }
        },
        verified = true,
    )
    is BlackPearlFlashResult.VerificationFailed -> BlackPearlFlashPresentation(
        message = blackPearlFlashNotVerifiedMessage(
            reason = result.reason,
            readbackAttempted = true,
        ),
        verified = false,
    )
    is BlackPearlFlashResult.TransferFailed -> BlackPearlFlashPresentation(
        message = blackPearlFlashNotVerifiedMessage(
            reason = result.reason,
            readbackAttempted = false,
        ),
        verified = false,
    )
    is BlackPearlFlashResult.NotRepresentable -> BlackPearlFlashPresentation(
        message = "Not flashable · ${result.reason}",
        verified = false,
    )
    is BlackPearlFlashResult.DeviceUnavailable -> BlackPearlFlashPresentation(
        message = result.reason,
        verified = false,
    )
}

private fun blackPearlFlashNotVerifiedMessage(
    reason: String,
    readbackAttempted: Boolean,
): String {
    val stateMessage = if (readbackAttempted) {
        "Final hardware readback did not confirm the requested EQ."
    } else {
        "The transfer did not complete, so the requested EQ was not confirmed."
    }
    return "TRN Black Pearl Flash was not verified. $stateMessage " +
        "Stop and reconnect or refresh before any later write. Reason: $reason"
}
