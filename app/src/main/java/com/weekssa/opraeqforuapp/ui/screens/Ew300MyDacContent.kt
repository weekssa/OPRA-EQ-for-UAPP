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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.dac.AmbiguousExactHardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqResponseEvaluator
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.dac.isAcousticallyActive
import com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityReport
import com.weekssa.opraeqforuapp.domain.ew300.Ew300DeviceControls
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationTrace
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationStatus
import com.weekssa.opraeqforuapp.domain.ew300.Ew300PersistenceQualificationResult
import com.weekssa.opraeqforuapp.domain.ew300.Ew300Protocol
import com.weekssa.opraeqforuapp.domain.ew300.Ew300QualificationExport
import com.weekssa.opraeqforuapp.domain.ew300.resolveEw300HardwareEq
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.ui.MyDacEditorApplyStatus
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import com.weekssa.opraeqforuapp.ui.Ew300PlaybackGainUiState
import com.weekssa.opraeqforuapp.ui.components.DacEqResponseGraph
import com.weekssa.opraeqforuapp.ui.components.PremiumSectionLabel
import com.weekssa.opraeqforuapp.ui.components.PremiumValueRow
import java.util.Locale
import kotlinx.coroutines.launch

internal fun ew300OperationControlsEnabled(operationStatus: Ew300OperationStatus): Boolean =
    operationStatus !is Ew300OperationStatus.Running

/** EW300 uses the same editor, graph, readback and operation semantics as Black Pearl. */
@Composable
internal fun Ew300MyDacContent(
    connectionState: Kt02h20ConnectionState,
    hardwareEqState: HardwareEqSnapshotState,
    editorState: MyDacEditorUiState,
    operationTrace: Ew300OperationTrace?,
    operationStatus: Ew300OperationStatus = Ew300OperationStatus.Idle,
    playbackGainState: Ew300PlaybackGainUiState = Ew300PlaybackGainUiState(),
    catalogState: CatalogState,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    savedGeneralEqs: List<SavedGeneralEqRecord> = emptyList(),
    onConnect: () -> Unit,
    onResetEq: () -> Unit,
    onRestoreBaseline: suspend () -> String,
    onRunCapabilityBatch: suspend () -> Ew300CapabilityReport,
    onAdvancePersistenceQualification: suspend () -> Ew300PersistenceQualificationResult,
    onSetPlaybackGain: (Double) -> Unit = {},
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
    val operationBusy = !ew300OperationControlsEnabled(operationStatus)
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var saveDacEqOpen by remember { mutableStateOf(false) }
    var capabilityReport by remember { mutableStateOf<Ew300CapabilityReport?>(null) }
    var capabilityBatchRunning by remember { mutableStateOf(false) }
    var persistenceResult by remember { mutableStateOf<Ew300PersistenceQualificationResult?>(null) }
    var persistenceRunning by remember { mutableStateOf(false) }
    var persistenceConfirmationOpen by remember { mutableStateOf(false) }
    var restorationRunning by remember { mutableStateOf(false) }
    if (saveDacEqOpen && !operationBusy && connectionState == Kt02h20ConnectionState.Connected) {
        BlackPearlSaveDacEqDialog(
            catalogState = catalogState,
            managedHeadphones = managedHeadphones,
            savedEqs = savedEqs,
            provenanceText = "The current verified five-band EQ will be saved as a Personal EQ with SIMGOT EW300 DSP provenance. Playback/global gain remains device state and is not included in the captured EQ.",
            namePlaceholder = "My EW300 EQ",
            onDismiss = { saveDacEqOpen = false },
            onSave = onCaptureDacEq,
            onMessage = onMessage,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("SIMGOT EW300 DSP", style = MaterialTheme.typography.titleLarge)
        Text("USB 31B2:0111 · five-band PEQ", style = MaterialTheme.typography.bodyMedium)
        when (connectionState) {
            Kt02h20ConnectionState.Connected -> {
                Text(
                    "Connected. EQ and DEVICE share one verified hardware session.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                when (val currentOperation = operationStatus) {
                    is Ew300OperationStatus.Running -> {
                        Text(
                            "EW300 ${currentOperation.operation.lowercase().replace('_', ' ')} is still being verified. Approve Android USB permission if it appears.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is Ew300OperationStatus.Completed -> {
                        val status = ew300OperationStatusPresentation(
                            trace = currentOperation.trace,
                        )
                        Text(
                            status.message,
                            color = if (status.verified) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    Ew300OperationStatus.Idle -> operationTrace?.let { trace ->
                        val status = ew300OperationStatusPresentation(
                            trace = trace,
                        )
                        Text(
                            status.message,
                            color = if (status.verified) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("EQ") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DEVICE") })
                }
                if (selectedTab == 0) {
                    if (operationBusy) {
                        Text(
                            "An EW300 operation is still being verified. Editing, capture, reset, and refresh are unavailable until it finishes.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (editorState.isOpening || editorState.isOpen || editorState.error != null) {
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
                            managedHeadphones = managedHeadphones,
                            savedEqs = savedEqs,
                            savedGeneralEqs = savedGeneralEqs,
                            canEdit = !operationBusy,
                            canCapture = !operationBusy && Ew300Protocol.CANONICAL_CAPTURE_QUALIFIED,
                            controlsEnabled = !operationBusy,
                            onRefresh = onConnect,
                            onEdit = onOpenEditor,
                            onCapture = { saveDacEqOpen = true },
                            onReset = onResetEq,
                        )
                        if (editorState.applyStatus != MyDacEditorApplyStatus.IDLE) {
                            Text(
                                editorState.applyFailureReason ?: "EW300 EQ Apply verified.",
                                color = if (editorState.applyFailureReason == null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                    }
                } else {
                    Ew300DeviceStatus(
                        report = capabilityReport,
                        hardwareEqState = hardwareEqState,
                        playbackGainState = playbackGainState,
                        running = capabilityBatchRunning || operationBusy,
                        operationBusy = operationBusy,
                        onRefresh = onConnect,
                        onSetPlaybackGain = onSetPlaybackGain,
                        onRun = {
                            if (!capabilityBatchRunning) {
                                capabilityBatchRunning = true
                                scope.launch {
                                    runCatching { onRunCapabilityBatch() }
                                        .onSuccess { capabilityReport = it }
                                        .onFailure {
                                            onMessage("The read-only EW300 report could not be completed. No write was sent.")
                                        }
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
                                    Ew300QualificationExport(
                                        BuildConfig.CANDIDATE_SOURCE_SHA,
                                        report,
                                        persistenceResult,
                                    ).toReadableText(),
                                )
                            }
                        },
                        onShareJson = capabilityReport?.let { report ->
                            {
                                shareReport(
                                    context,
                                    "EW300 capability report JSON",
                                    "application/json",
                                    Ew300QualificationExport(
                                        BuildConfig.CANDIDATE_SOURCE_SHA,
                                        report,
                                        persistenceResult,
                                    ).toJson(),
                                )
                            }
                        },
                        operationTrace = operationTrace,
                        restorationRunning = restorationRunning,
                        onRestoreBaseline = {
                            if (!restorationRunning) {
                                restorationRunning = true
                                scope.launch {
                                    runCatching { onRestoreBaseline() }
                                        .onSuccess(onMessage)
                                        .onFailure {
                                            onMessage("EW300 baseline restoration stopped before a verified result. Do not retry; stop and reconnect or refresh before any later write.")
                                        }
                                    restorationRunning = false
                                }
                            }
                        },
                        onShareTraceReadable = operationTrace?.let { trace ->
                            { shareReport(context, "EW300 operation report", "text/plain", trace.toReadableText()) }
                        },
                        onShareTraceJson = operationTrace?.let { trace ->
                            { shareReport(context, "EW300 operation report JSON", "application/json", trace.toJson()) }
                        },
                        candidateSourceSha = BuildConfig.CANDIDATE_SOURCE_SHA,
                        persistenceResult = persistenceResult,
                        persistenceRunning = persistenceRunning,
                        persistenceEnabled = capabilityReport?.status ==
                            com.weekssa.opraeqforuapp.domain.ew300.Ew300CapabilityCaseResult.Status.PASS,
                    )
                }
            }

            Kt02h20ConnectionState.Connecting -> {
                Text("Connecting to EW300…")
                Text(
                    "After Apply or Flash, the EW300 may briefly disconnect and Android may ask for USB permission again. Approve it so final hardware readback can complete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Kt02h20ConnectionState.Disconnected -> {
                if (operationBusy) {
                    Text(
                        "The EW300 operation is still being verified. Do not reconnect manually until its result is shown.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("Connect the EW300 USB cable to manage its EQ.")
                    Button(onClick = onConnect) { Text("Connect") }
                }
            }

            is Kt02h20ConnectionState.Error -> {
                Text(connectionState.message, color = MaterialTheme.colorScheme.error)
                if (operationBusy) {
                    Text(
                        "The EW300 operation is still being verified. Do not retry the connection manually yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Button(onClick = onConnect) { Text("Try again") }
                }
            }

            is Kt02h20ConnectionState.PermissionRequired -> {
                Text(connectionState.message, color = MaterialTheme.colorScheme.error)
                if (operationBusy) {
                    Text(
                        "The EW300 operation is still being verified. Wait for the result before granting a new USB session.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Button(onClick = onConnect) { Text("Grant USB permission") }
                }
            }
        }
    }
}

internal data class Ew300OperationStatusPresentation(
    val message: String,
    val verified: Boolean,
)

internal fun ew300OperationStatusPresentation(
    trace: Ew300OperationTrace,
): Ew300OperationStatusPresentation {
    val operation = trace.operation
        .lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
    return if (
        trace.stateKnown &&
        trace.finalReadbackMatched &&
        trace.outcome in setOf("Success", "Verified")
    ) {
        val reconnectMessage = if (trace.replacementObserved && trace.replacementIdentityMatched) {
            " The DAC reconnected and the replacement session was verified."
        } else {
            ""
        }
        val successMessage = when (trace.operation.uppercase()) {
            "FLASH" -> "Flash successful · SIMGOT EW300 DSP EQ was saved and verified."
            "RESET" -> "Reset successful · SIMGOT EW300 DSP EQ was reset to flat and verified."
            else -> "Last EW300 $operation verified."
        }
        Ew300OperationStatusPresentation(
            message = "✓ $successMessage$reconnectMessage Final hardware readback matched.",
            verified = true,
        )
    } else {
        val reconnectMessage = if (trace.replacementObserved) {
            " after USB reconnect"
        } else {
            ""
        }
        Ew300OperationStatusPresentation(
            message = "Last EW300 $operation was not verified$reconnectMessage. The previous Save may have completed, but final hardware readback was not verified. Stop and reconnect or refresh before any later write.",
            verified = false,
        )
    }
}

@Composable
private fun Ew300EqStatus(
    state: HardwareEqSnapshotState,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    savedGeneralEqs: List<SavedGeneralEqRecord>,
    canEdit: Boolean,
    canCapture: Boolean,
    controlsEnabled: Boolean = true,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onCapture: () -> Unit,
    onReset: () -> Unit,
) {
    if (state.isReading) {
        Text(
            "Reading the current EW300 EQ…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val bundle = state.bundle
    OutlinedButton(
        onClick = onRefresh,
        enabled = controlsEnabled && !state.isReading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (state.isReading) "Refreshing…" else "Refresh")
    }

    if (bundle == null) {
        Text(
            if (state.readFailed) {
                "The EW300 read failed. The app is not treating an unknown state as current; refresh or reconnect before editing."
            } else {
                "No verified EW300 EQ readback is available yet."
            },
            color = if (state.readFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    PremiumSectionLabel(
        text = if (state.freshness == DacStateFreshness.CURRENT) {
            "Current hardware EQ"
        } else {
            "Last read hardware EQ"
        },
        divider = false,
    )

    val matchResolution = remember(bundle.fingerprint, managedHeadphones, savedEqs, savedGeneralEqs) {
        resolveEw300HardwareEq(
            bundle = bundle,
            managedHeadphones = managedHeadphones,
            savedEqs = savedEqs,
            savedGeneralEqs = savedGeneralEqs,
        )
    }
    val match = matchResolution.match
    Text(
        text = when (match) {
            HardwareEqMatch.Flat -> "Flat"
            is HardwareEqMatch.Exact -> "Exact saved match"
            is AmbiguousExactHardwareEqMatch -> "Multiple exact saved matches"
            is HardwareEqMatch.ModifiedKnown -> "Modified known EQ"
            HardwareEqMatch.Unknown -> "Unknown EQ"
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    when (match) {
        is HardwareEqMatch.Exact -> {
            Text(match.savedEq.displayName, style = MaterialTheme.typography.bodyLarge)
            matchResolution.representation(match.savedEq.savedEqKey)?.let { representation ->
                val fidelity = if (representation.fidelity == DevicePresetFidelity.EXACT) "Exact" else "Optimized"
                Text(
                    "$fidelity · ${representation.adaptationSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is AmbiguousExactHardwareEqMatch -> Text(
            "More than one saved EQ resolves to the same native EW300 bytes, so the app will not invent a single source.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is HardwareEqMatch.ModifiedKnown -> Text(
            "This state has explicit saved-EQ lineage but no longer matches its complete native representation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HardwareEqMatch.Unknown -> Text(
            "The current non-flat hardware EQ does not exactly match a saved derived EW300 representation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HardwareEqMatch.Flat -> Unit
    }

    val responseCurve = remember(bundle.snapshot.filters) {
        HardwareEqResponseEvaluator.evaluate(bundle.snapshot.filters)
    }
    PremiumSectionLabel(text = "EQ response", divider = false)
    if (responseCurve == null) {
        Text(
            "The current hardware response could not be evaluated safely.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val activeBandCount = bundle.snapshot.filters.count { it.isAcousticallyActive() }
        DacEqResponseGraph(
            curve = responseCurve,
            filters = bundle.snapshot.filters,
            accessibilityDescription = String.format(
                Locale.US,
                "EW300 EQ response graph from 20 hertz to 20 kilohertz with %d active Peak bands. Response ranges from %.1f to %.1f decibels, with a zero-decibel reference line.",
                activeBandCount,
                responseCurve.minimumGainDb,
                responseCurve.maximumGainDb,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    bundle.snapshot.playbackGainDb?.let { gainDb ->
        Text(
            String.format(Locale.US, "Playback / global gain: %.1f dB", gainDb),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        "${bundle.snapshot.filters.count { it.isAcousticallyActive() }} active Peak bands. Playback/global gain is device state and is not included in captured Personal EQ identity.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (canEdit) {
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit current EQ") }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reset EQ to flat") }
    }
    OutlinedButton(
        onClick = onCapture,
        enabled = controlsEnabled && canCapture,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (canCapture) "Save readback as Personal EQ" else "Personal EQ capture is unavailable for this readback")
    }
}

/** Truthful EW300 DEVICE surface with only the verified output-gain mutation exposed. */
@Composable
internal fun Ew300DeviceStatus(
    report: Ew300CapabilityReport?,
    hardwareEqState: HardwareEqSnapshotState = HardwareEqSnapshotState(),
    playbackGainState: Ew300PlaybackGainUiState = Ew300PlaybackGainUiState(),
    running: Boolean,
    operationBusy: Boolean = false,
    onRefresh: () -> Unit = {},
    onSetPlaybackGain: (Double) -> Unit = {},
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
    validationEvidenceEnabled: Boolean = false,
    restorationRunning: Boolean = false,
    onRestoreBaseline: () -> Unit = {},
) {
    var gainDialogOpen by remember { mutableStateOf(false) }
    var stagedGainDb by remember { mutableStateOf(0.0) }
    val hardwareBundle = hardwareEqState.bundle
    val currentGainDb = hardwareBundle?.snapshot?.playbackGainDb
    val canChangeGain = currentGainDb != null &&
        hardwareEqState.freshness == DacStateFreshness.CURRENT &&
        !playbackGainState.isWriting &&
        !operationBusy

    if (gainDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!playbackGainState.isWriting) gainDialogOpen = false },
            title = { Text("EW300 output gain") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Set the resulting device playback gain. This changes the EW300 output level and may make audio much louder. Start conservatively and keep the Android volume low.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        String.format(Locale.US, "%+.1f dB", stagedGainDb),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Slider(
                        value = stagedGainDb.toFloat(),
                        onValueChange = { value -> stagedGainDb = value.toDouble() },
                        valueRange = Ew300DeviceControls.MIN_PLAYBACK_GAIN_DB.toFloat()..Ew300DeviceControls.MAX_PLAYBACK_GAIN_DB.toFloat(),
                        steps = 127,
                        enabled = !playbackGainState.isWriting && !operationBusy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Allowed range: -64.0 dB to 0.0 dB in 0.5 dB steps. Every change is saved once and verified by final hardware readback.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    playbackGainState.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        gainDialogOpen = false
                        onSetPlaybackGain(stagedGainDb)
                    },
                    enabled = !playbackGainState.isWriting && !operationBusy,
                ) { Text(if (playbackGainState.isWriting) "Saving…" else "Save and verify") }
            },
            dismissButton = {
                TextButton(
                    onClick = { gainDialogOpen = false },
                    enabled = !playbackGainState.isWriting,
                ) { Text("Cancel") }
            },
        )
    }
    PremiumSectionLabel(
        text = when (hardwareEqState.freshness) {
            DacStateFreshness.CURRENT -> "Current device state"
            DacStateFreshness.LAST_READ_STALE -> "Last read device state"
            null -> "Device state"
        },
        divider = false,
    )
    Text(
        when {
            hardwareEqState.isReading -> "Refreshing the verified EW300 readback…"
            hardwareEqState.readFailed -> "The latest device-state read failed; cached values are not current."
            hardwareBundle != null && hardwareEqState.freshness == DacStateFreshness.CURRENT ->
                "Values verified from the connected EW300."
            hardwareBundle != null -> "Showing the last verified readback. Refresh before relying on it as current."
            else -> "No verified device-state readback is available yet."
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (hardwareEqState.readFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(
        onClick = onRefresh,
        enabled = !hardwareEqState.isReading && !operationBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (hardwareEqState.isReading) "Refreshing…" else "Refresh")
    }
    PremiumValueRow(
        title = "Output gain",
        value = currentGainDb?.let {
            String.format(Locale.US, "%.1f dB", it)
        } ?: "Not read",
        supportingText = when {
            playbackGainState.isWriting -> "Saving and verifying the requested output level…"
            playbackGainState.error != null -> playbackGainState.error
            canChangeGain -> "Tap to adjust the EW300 playback level. Changes are persisted and read back before success is shown."
            else -> "Refresh the connected EW300 before changing its playback level."
        },
        enabled = canChangeGain,
        showDisclosure = true,
        onClick = if (canChangeGain) {
            {
                stagedGainDb = currentGainDb ?: 0.0
                gainDialogOpen = true
            }
        } else {
            null
        },
    )
    PremiumValueRow(
        title = "Equalizer",
        value = hardwareBundle?.snapshot?.filters?.let { filters ->
            "${filters.count { filter -> filter.isAcousticallyActive() }} active Peak bands"
        } ?: "Not read",
        supportingText = "The EQ tab owns response, editing, Apply, capture, and Reset.",
    )
    PremiumValueRow(
        title = "Connection",
        value = when {
            hardwareEqState.freshness == DacStateFreshness.CURRENT -> "Current session"
            hardwareBundle != null -> "Last read"
            else -> "Awaiting read"
        },
        supportingText = "One shared My DAC session. Refresh rereads it without opening a second USB path.",
    )

}

private fun shareReport(context: Context, subject: String, mimeType: String, contents: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, contents)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share EW300 report"))
}
