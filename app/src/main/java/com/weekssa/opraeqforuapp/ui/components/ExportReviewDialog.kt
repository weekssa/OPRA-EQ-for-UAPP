package com.weekssa.opraeqforuapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.assessUappCompatibility
import com.weekssa.opraeqforuapp.domain.export.DeviceExportability
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.assessDeviceExportability
import com.weekssa.opraeqforuapp.domain.export.deviceAdaptationSummary
import java.util.Locale

internal data class ExportReviewItem(
    val title: String,
    val profile: OpraEqProfile,
)

@Composable
internal fun ExportReviewDialog(
    device: ExportDevice,
    items: List<ExportReviewItem>,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
) {
    if (!device.supportsFileExport) {
        val okFocusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("File export unavailable") },
            text = {
                Text(
                    "${device.displayName} has no verified import file. Connect it and use My DAC to review supported hardware actions.",
                )
            },
            confirmButton = {
                LaunchedEffect(okFocusRequester) {
                    okFocusRequester.requestFocus()
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp).focusRequester(okFocusRequester),
                ) {
                    Text("OK")
                }
            },
        )
        return
    }

    val exportableItems = items.filter { assessDeviceExportability(it.profile, device) != DeviceExportability.NOT_REPRESENTABLE }
    val isUapp = device == ExportDevice.UAPP
    val cancelFocusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isUapp) "Export for UAPP" else "Review export") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isUapp) {
                        "Review the selected EQs before creating ToneBoosters XML. EQ Library does not route Android audio. Import an exported XML file in UAPP yourself."
                    } else {
                        "Review the selected EQs before creating ${device.displayName} files. This does not change Android audio output."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${exportableItems.size} of ${items.size} ${if (items.size == 1) "EQ is" else "EQs are"} representable for ${device.displayName}.",
                    style = MaterialTheme.typography.labelLarge,
                )
                if (exportableItems.isEmpty()) {
                    Text(
                        "No selected EQ is ready to export for this target.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(items, key = { index, item -> "$index:${item.title}" }) { _, item ->
                        ExportReviewItemContent(item = item, device = device)
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onExport,
                enabled = exportableItems.isNotEmpty() && device.supportsFileExport,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (isUapp) "Export XML" else "Export .${device.extension}")
            }
        },
        dismissButton = {
            LaunchedEffect(cancelFocusRequester) {
                cancelFocusRequester.requestFocus()
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp).focusRequester(cancelFocusRequester),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ExportReviewItemContent(
    item: ExportReviewItem,
    device: ExportDevice,
) {
    val profile = item.profile
    val exportability = assessDeviceExportability(profile, device)
    val representation = when (exportability) {
        DeviceExportability.EXACT -> "Exact"
        DeviceExportability.OPTIMIZED -> "Optimized"
        DeviceExportability.NOT_REPRESENTABLE -> "Not suitable"
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(item.title, style = MaterialTheme.typography.titleSmall)
        Text(
            "${profile.author?.takeIf(String::isNotBlank)?.let { "By $it · " }.orEmpty()}$representation · " +
                "${profile.bands?.size ?: "Unknown"} ${if (profile.bands?.size == 1) "source band" else "source bands"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val headroom = profile.eqLibrarySafetyHeadroomDb
        val sourcePreamp = profile.preampGainDb
        when {
            sourcePreamp != null -> Text(
                "Source preamp: ${String.format(Locale.US, "%+.2f dB", sourcePreamp)}",
                style = MaterialTheme.typography.bodySmall,
            )
            headroom != null -> Text(
                "EQ Library playback headroom: ${String.format(Locale.US, "%+.2f dB", headroom)}",
                style = MaterialTheme.typography.bodySmall,
            )
            else -> Text("No source preamp supplied", style = MaterialTheme.typography.bodySmall)
        }
        deviceAdaptationSummary(profile, device)?.let { adaptation ->
            Text(adaptation, style = MaterialTheme.typography.bodySmall)
        }
        val warning = when {
            exportability == DeviceExportability.NOT_REPRESENTABLE ->
                if (device == ExportDevice.UAPP) profile.assessUappCompatibility().reason
                    ?: "This profile cannot be represented for the selected target."
                else "This profile cannot be represented for the selected target."
            !profile.isVerified -> "Unverified source. Review its attribution before importing."
            else -> null
        }
        warning?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
