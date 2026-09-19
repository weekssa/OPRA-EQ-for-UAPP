package com.weekssa.opraeqforuapp.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.dac.isAcousticallyActive
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityReport
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.ui.MyDacEditorApplyStatus
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import com.weekssa.opraeqforuapp.ui.components.PremiumSectionLabel
import kotlinx.coroutines.launch

/** EW300 uses the same editor, graph, readback and operation semantics as Black Pearl. */
@Composable
internal fun Ew300MyDacContent(
    connectionState: Kt02h20ConnectionState,
    hardwareEqState: HardwareEqSnapshotState,
    editorState: MyDacEditorUiState,
    catalogState: CatalogState,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    onConnect: () -> Unit,
    onResetEq: suspend () -> String,
    onQualifyGlobalGain: suspend () -> String,
    onRunCapabilityBatch: suspend () -> Ew300CapabilityReport,
    onCaptureDacEq: suspend (String, SavedEqHeadphoneAssociation?) -> String,
    onOpenEditor: () -> Unit,
    onCloseEditor: () -> Unit,
    onSelectBand: (Int) -> Unit,
    onShowAllBands: () -> Unit,
    onShowReview: () -> Unit,
    onUpdateBand: (Int, EqFilterType, Double, Double, Double) -> Unit,
    onUseSafeGain: () -> Unit,
    onResetEdits: () -> Unit,
    onApply: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var saveDacEqOpen by remember { mutableStateOf(false) }
    var capabilityReport by remember { mutableStateOf<Ew300CapabilityReport?>(null) }
    var capabilityBatchRunning by remember { mutableStateOf(false) }
    if (saveDacEqOpen && connectionState == Kt02h20ConnectionState.Connected) {
        BlackPearlSaveDacEqDialog(
            catalogState = catalogState,
            managedHeadphones = managedHeadphones,
            savedEqs = savedEqs,
            provenanceText = "The current five-band and global-gain values will be saved as a Personal EQ with SIMGOT EW300 DSP provenance.",
            namePlaceholder = "My EW300 EQ",
            onDismiss = { saveDacEqOpen = false },
            onSave = onCaptureDacEq,
            onMessage = onMessage,
        )
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("SIMGOT EW300 DSP", style = MaterialTheme.typography.titleLarge)
        Text("USB 31B2:0111 · five-band PEQ", style = MaterialTheme.typography.bodyMedium)
        when (connectionState) {
            Kt02h20ConnectionState.Connected -> {
                Text("Connected. Use My EQs or EQ Library to flash a selected profile after the beta hardware gate passes.")
                Text(
                    "EW300 software support is ready for the final exact-device gate. Global gain, persistence, and Reset remain locked until the one-time reversible qualification passes on this cable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                    Text(
                        "EW300 capability qualification is performed by the guided diagnostic utility, not by a normal device-setting action.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("EQ") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DEVICE") })
                }
                if (selectedTab == 0) {
                    if (editorState.isOpening || editorState.isOpen || editorState.error != null) {
                        BlackPearlEqEditorScreen(
                            state = editorState,
                            onRetryOpen = onOpenEditor,
                            onClose = onCloseEditor,
                            onSelectBand = onSelectBand,
                            onShowAllBands = onShowAllBands,
                            onShowReview = onShowReview,
                            onUpdateBand = onUpdateBand,
                            onUseSafeGain = onUseSafeGain,
                            onResetEdits = onResetEdits,
                            onApply = onApply,
                            dacLabel = "SIMGOT EW300 DSP",
                        )
                    } else {
                        Ew300EqStatus(
                            state = hardwareEqState,
                            canEdit = false,
                            canCapture = Ew300Protocol.CANONICAL_CAPTURE_QUALIFIED,
                            onEdit = onOpenEditor,
                            onCapture = { saveDacEqOpen = true },
                            onReset = { scope.launch { onMessage(onResetEq()) } },
                        )
                        if (editorState.applyStatus != MyDacEditorApplyStatus.IDLE) {
                            Text(
                                editorState.applyFailureReason ?: "EW300 EQ Apply verified.",
                                color = if (editorState.applyFailureReason == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                } else {
                    Ew300DeviceStatus(
                        report = capabilityReport,
                        running = capabilityBatchRunning,
                        onRun = {
                            if (!capabilityBatchRunning) {
                                capabilityBatchRunning = true
                                scope.launch {
                                    runCatching { onRunCapabilityBatch() }
                                        .onSuccess { capabilityReport = it }
                                        .onFailure { onMessage("The read-only EW300 report could not be completed. No write was sent.") }
                                    capabilityBatchRunning = false
                                }
                            }
                        },
                        onShareReadable = capabilityReport?.let { report ->
                            { shareReport(context, "EW300 capability report", "text/plain", report.toReadableText()) }
                        },
                        onShareJson = capabilityReport?.let { report ->
                            { shareReport(context, "EW300 capability report JSON", "application/json", report.toJson()) }
                        },
                    )
                }
            }
            Kt02h20ConnectionState.Connecting -> Text("Connecting to EW300…")
            Kt02h20ConnectionState.Disconnected -> {
                Text("Connect the EW300 USB cable to manage its EQ.")
                Button(onClick = onConnect) { Text("Connect") }
            }
            is Kt02h20ConnectionState.Error -> {
                Text(connectionState.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onConnect) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun Ew300EqStatus(
    state: HardwareEqSnapshotState,
    canEdit: Boolean,
    canCapture: Boolean,
    onEdit: () -> Unit,
    onCapture: () -> Unit,
    onReset: () -> Unit,
) {
    if (state.isReading) Text("Reading the current EW300 EQ…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    val bundle = state.bundle ?: run {
        Text("No verified EW300 EQ readback is available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    PremiumSectionLabel(
        text = if (state.freshness == DacStateFreshness.CURRENT) "Current hardware EQ" else "Last read hardware EQ",
        divider = false,
    )
    bundle.snapshot.dedicatedEqPreampDb?.let { Text("Global EQ gain: ${"%.1f".format(it)} dB") }
    Text(
        "${bundle.snapshot.filters.count { it.isAcousticallyActive() }} native bands were read. Acoustic filter labels and persistent writes remain pending exact-EW300 qualification.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (canEdit) {
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit current EQ") }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reset EQ to flat") }
    }
    OutlinedButton(onClick = onCapture, enabled = canCapture, modifier = Modifier.fillMaxWidth()) {
        Text(if (canCapture) "Save readback as Personal EQ" else "Personal EQ capture pending qualification")
    }
}

/**
 * Truthful EW300 DEVICE surface. Unlike Black Pearl, no independent non-EQ controls have been
 * verified for this cable yet, so the tab is present but deliberately exposes no guessed writes.
 */
@Composable
private fun Ew300DeviceStatus(
    report: Ew300CapabilityReport?,
    running: Boolean,
    onRun: () -> Unit,
    onShareReadable: (() -> Unit)?,
    onShareJson: (() -> Unit)?,
) {
    Text("SIMGOT EW300 DSP", style = MaterialTheme.typography.titleMedium)
    Text("USB 31B2:0111", style = MaterialTheme.typography.bodyMedium)
    Text(
        "No additional EW300 device controls are verified in this candidate yet. Volume, headset, UAC, and other Black Pearl settings are not copied to this device without an exact EW300 protocol match.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "Software-prepared in this candidate: five-band PEQ readback, guarded gain-aware EQ operations, capture, and reconnect recovery. Flash, persistence, and Reset remain hardware-validation pending.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PremiumSectionLabel(text = "Read-only capability report", divider = false)
    Text(
        "This automated check reads the exact EW300 identity, EQ registers, and gain register. It never writes, saves, resets, or retries a mutation.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = onRun, enabled = !running, modifier = Modifier.fillMaxWidth()) {
        Text(if (running) "Reading…" else "Run read-only report")
    }
    report?.let {
        Text("Result: ${it.status}", style = MaterialTheme.typography.titleSmall)
        Text(
            if (it.stateKnown) "The device state remained known after the check." else "The check stopped safely because device state could not be confirmed.",
            style = MaterialTheme.typography.bodySmall,
            color = if (it.stateKnown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )
        OutlinedButton(onClick = requireNotNull(onShareReadable), modifier = Modifier.fillMaxWidth()) {
            Text("Share readable report")
        }
        OutlinedButton(onClick = requireNotNull(onShareJson), modifier = Modifier.fillMaxWidth()) {
            Text("Share technical report")
        }
    }
}

private fun shareReport(context: Context, subject: String, mimeType: String, contents: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, contents)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share EW300 report"))
}
