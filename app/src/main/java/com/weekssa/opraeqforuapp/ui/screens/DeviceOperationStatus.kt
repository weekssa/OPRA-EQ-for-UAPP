package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.dac.DacControlId

internal data class DeviceOperationStatusPresentation(
    val heading: String,
    val staleMessage: String? = null,
    val pendingMessage: String? = null,
)

internal fun deviceOperationStatusPresentation(
    isReading: Boolean,
    isWriting: Boolean,
    activeWriteControlId: DacControlId?,
    pendingVerificationControlId: DacControlId?,
    hasSnapshot: Boolean,
    isCurrentSession: Boolean,
    controlName: (DacControlId) -> String,
): DeviceOperationStatusPresentation {
    val activeControlId = activeWriteControlId ?: pendingVerificationControlId
    return when {
        isWriting && activeControlId != null -> DeviceOperationStatusPresentation(
            heading = "Applying ${controlName(activeControlId)}…",
        )
        pendingVerificationControlId != null -> DeviceOperationStatusPresentation(
            heading = "Applying ${controlName(pendingVerificationControlId)}…",
            pendingMessage = "Reconnect the DAC so EQ Library can verify the change.",
        )
        isReading -> DeviceOperationStatusPresentation(heading = "Refreshing device…")
        isCurrentSession -> DeviceOperationStatusPresentation(heading = "Current device state")
        hasSnapshot -> DeviceOperationStatusPresentation(
            heading = "Last read",
            staleMessage = "Reconnect or refresh to update these values.",
        )
        else -> DeviceOperationStatusPresentation(heading = "Device settings")
    }
}

/**
 * Shared My DAC DEVICE operation header.
 *
 * Device adapters keep their own protocol/units. This surface only standardizes truthful progress,
 * verification, stale-state and error feedback so every supported DAC behaves consistently.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun DeviceOperationStatusHeader(
    isReading: Boolean,
    isWriting: Boolean,
    activeWriteControlId: DacControlId?,
    pendingVerificationControlId: DacControlId? = null,
    lastVerifiedWriteControlId: DacControlId?,
    hasSnapshot: Boolean,
    isCurrentSession: Boolean,
    error: String?,
    enabled: Boolean,
    busy: Boolean,
    onRefresh: () -> Unit,
    controlName: (DacControlId) -> String,
) {
    // The header is deliberately stable. Transient writes, verification and failures belong in the
    // shared bottom snackbar so setting rows never move while a device operation is running.
    val heading = when {
        isCurrentSession -> "Current device state"
        hasSnapshot -> "Last read"
        else -> "Device settings"
    }
    val supportingText = when {
        isCurrentSession -> "Values verified from the connected DAC."
        hasSnapshot -> "Reconnect or refresh to update these values."
        else -> "Connect a DAC to read its settings."
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = heading,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onRefresh, enabled = enabled && !busy) {
            Text("Refresh")
        }
    }
}

/**
 * Creates a readable fallback label from a capability/control identifier. Device adapters can
 * provide richer labels later without changing the shared operation-feedback lifecycle.
 */
internal fun deviceOperationControlLabel(controlId: DacControlId): String =
    controlId.value
        .substringAfterLast('.')
        .replace('_', ' ')
        .replace('-', ' ')
        .split(' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { token ->
            token.replaceFirstChar { char -> char.uppercase() }
        }
