package com.weekssa.opraeqforuapp.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.security.ReleaseSignatureGate
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.dac.isAcousticallyActive
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityReport
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.ew300.Ew300PersistenceQualificationResult
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationTrace
import com.weekssa.opraeqforuapp.domain.ew300.Ew300QualificationExport
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
    operationTrace: Ew300OperationTrace?,
    catalogState: CatalogState,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    onConnect: () -> Unit,
    onResetEq: suspend () -> String,
    onRunCapabilityBatch: suspend () -> Ew300CapabilityReport,
    onAdvancePersistenceQualification: suspend () -> Ew300PersistenceQualificationResult,
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
    var persistenceResult by remember { mutableStateOf<Ew300PersistenceQualificationResult?>(null) }
    var persistenceRunning by remember { mutableStateOf(false) }
    var persistenceConfirmationOpen by remember { mutableStateOf(false) }
    // The Save qualification was a bounded development gate and is not a product action. Its
    // verified result is now represented by the immutable EW300 capability profile.
    val qualificationBuild = false
    fun advancePersistenceQualification() {
        if (!qualificationBuild || persistenceRunning) return
        persistenceRunning = true
        scope.launch {
            runCatching { onAdvancePersistenceQualification() }
                .onSuccess { persistenceResult = it }
                .onFailure {
                    onMessage("The EW300 qualification stopped before a result. Do not repeat the action; share the report.")
                }
            persistenceRunning = false
        }
    }
    if (qualificationBuild && persistenceConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { persistenceConfirmationOpen = false },
            title = { Text("Start the one-time Save qualification?") },
            text = {
                Text(
                    "Exact signed source: ${BuildConfig.CANDIDATE_SOURCE_SHA.take(12)}. The app will lower playback gain by 0.5 dB and one Peak band by 0.1 dB, verify both, send Save once, then preserve the complete baseline. You will unplug and reconnect twice to verify persistence and exact restoration. Stop immediately if the app reports uncertainty.",
                )
            },
            confirmButton = {
                Button(onClick = {
                    persistenceConfirmationOpen = false
                    advancePersistenceQualification()
                }) { Text("Begin qualification") }
            },
            dismissButton = {
                TextButton(onClick = { persistenceConfirmationOpen = false }) { Text("Cancel") }
            },
        )
    }
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
                Text("Connected. Use My EQs or EQ Library to apply a verified Peak-only EQ profile.")
                Text(
                    "This exact EW300 profile supports five Peak bands, readback, capture, Apply, Flash, Reset EQ to flat, and reconnect recovery. Other device controls remain unsupported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("EQ") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DEVICE") })
                }
                if (selectedTab == 0) {
                    if (editorState.isOpening || editorState.isOpen || editorState.error != null) {
                        DacEqEditorScreen(
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
                            {
                                shareReport(
                                    context,
                                    "EW300 capability report",
                                    "text/plain",
                                    Ew300QualificationExport(BuildConfig.CANDIDATE_SOURCE_SHA, report, persistenceResult).toReadableText(),
                                )
                            }
                        },
                        onShareJson = capabilityReport?.let { report ->
                            {
                                shareReport(
                                    context,
                                    "EW300 capability report JSON",
                                    "application/json",
                                    Ew300QualificationExport(BuildConfig.CANDIDATE_SOURCE_SHA, report, persistenceResult).toJson(),
                                )
                            }
                        },
                        operationTrace = operationTrace,
                        onShareTraceReadable = operationTrace?.let { trace ->
                            { shareReport(context, "EW300 operation report", "text/plain", trace.toReadableText()) }
                        },
                        onShareTraceJson = operationTrace?.let { trace ->
                            { shareReport(context, "EW300 operation report JSON", "application/json", trace.toJson()) }
                        },
                        qualificationBuild = qualificationBuild,
                        candidateSourceSha = BuildConfig.CANDIDATE_SOURCE_SHA,
                        persistenceResult = persistenceResult,
                        persistenceRunning = persistenceRunning,
                        persistenceEnabled = capabilityReport?.status == com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityCaseResult.Status.PASS,
                        onStartPersistence = { persistenceConfirmationOpen = true },
                        onContinuePersistence = ::advancePersistenceQualification,
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
    bundle.snapshot.playbackGainDb?.let { Text("Playback gain: ${"%.1f".format(it)} dB") }
    Text(
        "${bundle.snapshot.filters.count { it.isAcousticallyActive() }} Peak bands were read. Playback gain is device state and is not included in captured Personal EQs.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (canEdit) {
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit current EQ") }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reset EQ to flat") }
    }
    OutlinedButton(onClick = onCapture, enabled = canCapture, modifier = Modifier.fillMaxWidth()) {
        Text(if (canCapture) "Save readback as Personal EQ" else "Personal EQ capture is unavailable for this readback")
    }
}

/**
 * Truthful EW300 DEVICE surface. Unlike Black Pearl, no independent non-EQ controls have been
 * verified for this cable yet, so the tab is present but deliberately exposes no guessed writes.
 */
@Composable
internal fun Ew300DeviceStatus(
    report: Ew300CapabilityReport?,
    running: Boolean,
    onRun: () -> Unit,
    onShareReadable: (() -> Unit)?,
    onShareJson: (() -> Unit)?,
    operationTrace: Ew300OperationTrace? = null,
    onShareTraceReadable: (() -> Unit)? = null,
    onShareTraceJson: (() -> Unit)? = null,
    qualificationBuild: Boolean = false,
    candidateSourceSha: String = "local-unqualified",
    persistenceResult: Ew300PersistenceQualificationResult? = null,
    persistenceRunning: Boolean = false,
    persistenceEnabled: Boolean = false,
    onStartPersistence: () -> Unit = {},
    onContinuePersistence: () -> Unit = {},
) {
    Text("SIMGOT EW300 DSP", style = MaterialTheme.typography.titleMedium)
    Text("USB 31B2:0111", style = MaterialTheme.typography.bodyMedium)
    Text(
        "Unrelated EW300 device controls are not verified here. Volume, headset, UAC, and other Black Pearl controls are not available without a separate exact EW300 protocol match.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "Available for this exact EW300 profile: five Peak-band readback, guarded Apply, capture, Flash, Reset EQ to flat, and reconnect recovery. The signed Save qualification is already complete for this validation and must not be run again.",
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
    operationTrace?.let { trace ->
        PremiumSectionLabel(text = "Last operation report", divider = false)
        Text(
            "${trace.operation} · ${trace.outcome}. Share this report after any guarded EW300 operation so the exact session, permission, Save, and readback evidence stays attached to the candidate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        onShareTraceReadable?.let { share ->
            OutlinedButton(onClick = share, modifier = Modifier.fillMaxWidth()) { Text("Share operation report") }
        }
        onShareTraceJson?.let { share ->
            OutlinedButton(onClick = share, modifier = Modifier.fillMaxWidth()) { Text("Share operation JSON") }
        }
    }
    if (qualificationBuild) {
        PremiumSectionLabel(text = "Signed-candidate Save qualification", divider = false)
        Text(
            "Candidate ${candidateSourceSha.take(12)}. This bounded flow makes two small safer changes and requires two complete power-removal checks before persistent Flash can unlock. It is absent from ordinary builds.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val persistenceMessage = when (val result = persistenceResult) {
            is Ew300PersistenceQualificationResult.AwaitingPowerCycle -> result.message
            is Ew300PersistenceQualificationResult.Verified -> result.message
            is Ew300PersistenceQualificationResult.NotPersistent -> result.message
            is Ew300PersistenceQualificationResult.Failed -> result.message
            null -> null
        }
        persistenceMessage?.let { message ->
            Text(
                message,
                color = if (persistenceResult is Ew300PersistenceQualificationResult.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        val awaitingPowerCycle = persistenceResult is Ew300PersistenceQualificationResult.AwaitingPowerCycle
        val terminalResult = persistenceResult is Ew300PersistenceQualificationResult.Verified ||
            persistenceResult is Ew300PersistenceQualificationResult.NotPersistent ||
            persistenceResult is Ew300PersistenceQualificationResult.Failed
        Button(
            onClick = if (awaitingPowerCycle) onContinuePersistence else onStartPersistence,
            enabled = !terminalResult && !persistenceRunning && (persistenceEnabled || awaitingPowerCycle),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    persistenceRunning -> "Checking…"
                    awaitingPowerCycle -> "Continue qualification"
                    else -> "Start Save qualification"
                },
            )
        }
        if (!persistenceEnabled && !awaitingPowerCycle) {
            Text(
                "Run the read-only report successfully first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
