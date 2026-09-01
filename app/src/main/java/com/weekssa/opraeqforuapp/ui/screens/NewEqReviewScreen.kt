package com.weekssa.opraeqforuapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.domain.export.DeviceExportability
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.export.assessDeviceExportability
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import kotlinx.coroutines.launch

@Composable
internal fun NewEqReviewScreen(
    headphone: ManagedHeadphoneRecord,
    activeOutput: ExportDevice,
    onAddSelected: suspend (Set<String>) -> Unit,
    onDismissBatch: suspend () -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val newProfiles = remember(headphone.profiles) {
        headphone.profiles.filter { it.isNewUnreviewed && !it.noLongerAvailable }
    }
    val updatedProfiles = remember(headphone.profiles) {
        headphone.profiles.filter { it.isUpdatedUnreviewed && !it.noLongerAvailable && !it.isNewUnreviewed }
    }
    var selectedIds by remember(headphone.productId, newProfiles.map { it.profileId }) {
        mutableStateOf<Set<String>>(emptySet())
    }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Text("My EQs", modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            text = "New EQs for ${headphone.productName}",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = buildList {
                if (newProfiles.isNotEmpty()) add("${newProfiles.size} new")
                if (updatedProfiles.isNotEmpty()) add("${updatedProfiles.size} updated")
            }.joinToString(" · "),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Choose any new EQs you want to add. Dismiss marks this batch reviewed without hiding or deleting anything. Back leaves the review pending.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (newProfiles.isNotEmpty()) {
                item(key = "new-heading") {
                    Text("New EQs", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
                }
                items(newProfiles, key = ManagedProfileRecord::profileId) { profile ->
                    val source = profile.lastKnownProfile
                    val checked = profile.profileId in selectedIds
                    ListItem(
                        leadingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                        headlineContent = { Text(source.author?.takeIf(String::isNotBlank) ?: "Creator information missing") },
                        supportingContent = {
                            Column {
                                Text(if (source.isVerified) "New · Verified" else "New · Unverified")
                                if (!source.isVerified) {
                                    Text(
                                        "Community submission — not independently verified. Review the source before use.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                source.details?.takeIf(String::isNotBlank)?.let { Text(it) }
                                Text(
                                    "${outputShortName(activeOutput)}: ${outputStatusLabel(assessDeviceExportability(source, activeOutput))}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                source.link?.takeIf(String::isNotBlank)?.let { url ->
                                    TextButton(onClick = { onOpenUrl(url) }) { Text("Source") }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { value ->
                                    selectedIds = if (value) selectedIds + profile.profileId else selectedIds - profile.profileId
                                },
                            ),
                    )
                    HorizontalDivider()
                }
            }

            if (updatedProfiles.isNotEmpty()) {
                item(key = "updated-heading") {
                    Text("Updated EQs", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
                }
                items(updatedProfiles, key = { "updated:${it.profileId}" }) { profile ->
                    val source = profile.lastKnownProfile
                    ListItem(
                        headlineContent = { Text(source.author?.takeIf(String::isNotBlank) ?: "Creator information missing") },
                        supportingContent = {
                            Column {
                                Text("Updated tuning · ${if (profile.selected) "already selected" else "not selected"}")
                                source.details?.takeIf(String::isNotBlank)?.let { Text(it) }
                                source.link?.takeIf(String::isNotBlank)?.let { url ->
                                    TextButton(onClick = { onOpenUrl(url) }) { Text("Source") }
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { scope.launch { onDismissBatch() } }) {
                Text(if (newProfiles.isEmpty()) "Done" else "Dismiss")
            }
            if (newProfiles.isNotEmpty()) {
                Button(
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { scope.launch { onAddSelected(selectedIds) } },
                ) { Text("Add selected (${selectedIds.size})") }
            }
        }
    }
}

private fun outputShortName(device: ExportDevice): String = when (device) {
    ExportDevice.UAPP -> "UAPP / ToneBoosters"
    ExportDevice.BLACK_PEARL -> "Black Pearl"
    ExportDevice.UNIVERSAL_PARAMETRIC -> "Universal PEQ"
    ExportDevice.POWERAMP -> "Poweramp"
    ExportDevice.WAVELET -> "Wavelet"
    ExportDevice.TOPPING_DX5_II -> "Topping DX5 II"
    ExportDevice.TOPPING_DX1_II -> "Topping DX1 II"
}

private fun outputStatusLabel(status: DeviceExportability): String = when (status) {
    DeviceExportability.EXACT -> "Exact"
    DeviceExportability.OPTIMIZED -> "Optimized"
    DeviceExportability.NOT_REPRESENTABLE -> "Not exportable"
}
