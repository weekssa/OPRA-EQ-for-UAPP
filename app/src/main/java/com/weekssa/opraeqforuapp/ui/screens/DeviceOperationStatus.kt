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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import kotlinx.coroutines.delay

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
    val verifiedId = lastVerifiedWriteControlId?.value
    var lastSeenVerifiedId by remember { mutableStateOf(verifiedId) }
    var visibleVerifiedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(verifiedId) {
        if (verifiedId == null) {
            lastSeenVerifiedId = null
            visibleVerifiedId = null
        } else if (verifiedId != lastSeenVerifiedId) {
            lastSeenVerifiedId = verifiedId
            visibleVerifiedId = verifiedId
            delay(2_200)
            if (visibleVerifiedId == verifiedId) visibleVerifiedId = null
        }
    }

    val presentation = deviceOperationStatusPresentation(
        isReading = isReading,
        isWriting = isWriting,
        activeWriteControlId = activeWriteControlId,
        pendingVerificationControlId = pendingVerificationControlId,
        hasSnapshot = hasSnapshot,
        isCurrentSession = isCurrentSession,
        controlName = controlName,
    )

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
                text = presentation.heading,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            presentation.staleMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onRefresh, enabled = enabled && !busy) {
            Text(if (isReading) "Refreshing…" else "Refresh")
        }
    }

    presentation.pendingMessage?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    error?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    visibleVerifiedId?.takeIf { !busy && error == null }?.let { id ->
        val name = controlName(DacControlId(id))
        val label = name.replaceFirstChar { char -> char.uppercase() }
        Text(
            text = "$label updated ✓",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
