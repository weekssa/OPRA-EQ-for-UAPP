package com.weekssa.opraeqforuapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult

internal enum class FlashFeedbackPhase {
    STARTING,
    VERIFYING,
    COMPLETED,
    SENT,
    UNCERTAIN,
    FAILED,
}

internal fun FlashFeedbackPhase.expiresAutomatically(): Boolean = this == FlashFeedbackPhase.SENT ||
    this == FlashFeedbackPhase.COMPLETED

internal data class FlashFeedback(
    val deviceLabel: String,
    val phase: FlashFeedbackPhase,
    val detail: String? = null,
    val verified: Boolean = false,
)

internal fun flashDeviceLabel(device: ExportDevice): String = when (device) {
    ExportDevice.BLACK_PEARL -> "TRN Black Pearl"
    ExportDevice.FIIO_JA11 -> "FiiO JA11"
    ExportDevice.SIMGOT_EW300 -> "SIMGOT EW300 DSP"
    ExportDevice.JCALLY_JM12 -> "JCALLY JM12"
    else -> device.displayName
}

internal fun blackPearlFlashFeedback(result: BlackPearlFlashResult): FlashFeedback = when (result) {
    is BlackPearlFlashResult.Success -> FlashFeedback(
        deviceLabel = flashDeviceLabel(ExportDevice.BLACK_PEARL),
        phase = FlashFeedbackPhase.SENT,
        detail = "EQ reports sent. Final hardware state was not read back." +
            (result.warning?.let { " $it" } ?: ""),
    )
    is BlackPearlFlashResult.NotRepresentable -> FlashFeedback(
        deviceLabel = flashDeviceLabel(ExportDevice.BLACK_PEARL),
        phase = FlashFeedbackPhase.FAILED,
        detail = result.reason,
    )
    is BlackPearlFlashResult.DeviceUnavailable -> FlashFeedback(
        deviceLabel = flashDeviceLabel(ExportDevice.BLACK_PEARL),
        phase = FlashFeedbackPhase.FAILED,
        detail = result.reason,
    )
    is BlackPearlFlashResult.TransferFailed -> FlashFeedback(
        deviceLabel = flashDeviceLabel(ExportDevice.BLACK_PEARL),
        phase = FlashFeedbackPhase.FAILED,
        detail = result.reason,
    )
}

@Composable
internal fun FlashFeedbackBanner(
    feedback: FlashFeedback,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = feedback.phase == FlashFeedbackPhase.STARTING ||
        feedback.phase == FlashFeedbackPhase.VERIFYING
    val needsGuidance = feedback.phase == FlashFeedbackPhase.FAILED ||
        feedback.phase == FlashFeedbackPhase.UNCERTAIN
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isActive) CircularProgressIndicator()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = when (feedback.phase) {
                        FlashFeedbackPhase.STARTING -> "Flashing to ${feedback.deviceLabel}…"
                        FlashFeedbackPhase.VERIFYING -> "Verifying the DAC…"
                        FlashFeedbackPhase.COMPLETED -> if (feedback.verified) {
                            "Flash complete · EQ saved and verified"
                        } else {
                            "Flash complete · final state unverified"
                        }
                        FlashFeedbackPhase.SENT -> "Flash sent to ${feedback.deviceLabel}"
                        FlashFeedbackPhase.UNCERTAIN -> "Flash result could not be verified"
                        FlashFeedbackPhase.FAILED -> "Flash failed"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (needsGuidance) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                feedback.detail?.takeIf(String::isNotBlank)?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (needsGuidance) {
                    Text(
                        text = "Reconnect or refresh the DAC before any later hardware action. Do not retry automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!isActive) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss flash result",
                    )
                }
            }
        }
    }
}
