package com.weekssa.opraeqforuapp.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqResponseEvaluator
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.dac.isAcousticallyActive
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.ui.MyDacEditorApplyStatus
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import com.weekssa.opraeqforuapp.ui.components.DacEqResponseGraph
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
    var saveDacEqOpen by remember { mutableStateOf(false) }
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
                Text("Connected. Use My EQs or EQ Library to flash a selected profile.")
                Text(
                    "The EW300 global gain is a dedicated EQ control. Qualification is a one-time reversible check; after it passes, source preamp and generated headroom are included in every Flash and editor Apply.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                    Button(onClick = {
                        // Qualification and editor reads are separate transactions. Clear any
                        // stale editor error before starting the qualification read so its result
                        // cannot be mistaken for a qualification failure.
                        onCloseEditor()
                        scope.launch { onMessage(onQualifyGlobalGain()) }
                    }) { Text("Qualify global gain") }
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
                        canEdit = true,
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
    val response = HardwareEqResponseEvaluator.evaluate(bundle.snapshot.filters)
    if (response != null) {
        DacEqResponseGraph(
            curve = response,
            filters = bundle.snapshot.filters,
            accessibilityDescription = "EW300 five-band EQ response",
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Text("${bundle.snapshot.filters.count { it.isAcousticallyActive() }} active bands · five-band native PEQ", style = MaterialTheme.typography.bodySmall)
    Button(onClick = onEdit, enabled = canEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit current EQ") }
    OutlinedButton(onClick = onCapture, enabled = canEdit, modifier = Modifier.fillMaxWidth()) { Text("Save current EQ to My EQs") }
    OutlinedButton(onClick = onReset, enabled = canEdit, modifier = Modifier.fillMaxWidth()) { Text("Reset EQ to flat") }
}
