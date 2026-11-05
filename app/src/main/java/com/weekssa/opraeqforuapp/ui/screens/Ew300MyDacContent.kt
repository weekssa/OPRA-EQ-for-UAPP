package com.weekssa.opraeqforuapp.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
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
import com.weekssa.opraeqforuapp.domain.ew300.Ew300RuntimePersistenceState
import com.weekssa.opraeqforuapp.domain.ew300.Ew300VolumeControl
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
import com.weekssa.opraeqforuapp.ui.Ew300RuntimeSaveUiState
import com.weekssa.opraeqforuapp.ui.Ew300UacUiState
import com.weekssa.opraeqforuapp.ui.components.DacEqResponseGraph
import com.weekssa.opraeqforuapp.ui.components.PremiumSectionLabel
import com.weekssa.opraeqforuapp.ui.components.PremiumValueRow
import java.util.Locale
import kotlin.math.roundToInt
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
    runtimePersistence: Ew300RuntimePersistenceState = Ew300RuntimePersistenceState.UNKNOWN,
    runtimeSaveState: Ew300RuntimeSaveUiState = Ew300RuntimeSaveUiState(),
    uacState: Ew300UacUiState = Ew300UacUiState(),
    catalogState: CatalogState,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    savedGeneralEqs: List<SavedGeneralEqRecord> = emptyList(),
    onConnect: () -> Unit,
    onResetEq: suspend () -> String,
    onRestoreBaseline: suspend () -> String,
    onRunCapabilityBatch: suspend () -> Ew300CapabilityReport,
    onAdvancePersistenceQualification: suspend () -> Ew300PersistenceQualificationResult,
    onSetPlaybackGain: (Double) -> Unit = {},
    onSaveRuntime: () -> Unit = {},
    onSetVolumeLevel: (Int) -> Unit = {},
    onRefreshUac: () -> Unit = {},
    onSetMicrophoneMuted: (Boolean) -> Unit = {},
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
    LaunchedEffect(connectionState) {
        if (connectionState == Kt02h20ConnectionState.Connected &&
            BuildConfig.EW300_PARITY_EXPERIMENT_ENABLED
        ) {
            onRefreshUac()
        }
    }
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
            .imePadding()
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
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = TabRowDefaults.primaryContainerColor,
                    contentColor = TabRowDefaults.primaryContentColor,
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("EQ") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DEVICE") })
                }
                if (selectedTab == 0) {
                    if (operationBusy) {
                        ew300OperationInProgressMessage(operationStatus)?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                            onReset = {
                                scope.launch { onMessage(onResetEq()) }
                            },
                            parityEnabled = BuildConfig.EW300_PARITY_EXPERIMENT_ENABLED,
                            runtimePersistence = runtimePersistence,
                            runtimeSaveState = runtimeSaveState,
                            onSaveRuntime = onSaveRuntime,
                        )
                        if (editorState.applyStatus != MyDacEditorApplyStatus.IDLE) {
                            Text(
                                editorState.applyFailureReason ?: if (BuildConfig.EW300_PARITY_EXPERIMENT_ENABLED) {
                                    "Runtime EQ verified for this session. Use Save to keep it after unplugging."
                                } else {
                                    "EW300 EQ Apply verified."
                                },
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
                        runtimePersistence = runtimePersistence,
                        runtimeSaveState = runtimeSaveState,
                        uacState = uacState,
                        running = capabilityBatchRunning || operationBusy,
                        operationBusy = operationBusy,
                        onRefresh = onConnect,
                        onSetPlaybackGain = onSetPlaybackGain,
                        onSaveRuntime = onSaveRuntime,
                        onSetVolumeLevel = onSetVolumeLevel,
                        onRefreshUac = onRefreshUac,
                        onSetMicrophoneMuted = onSetMicrophoneMuted,
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
                    ew300OperationInProgressMessage(operationStatus)
                        ?: "After Apply or Flash, the EW300 may briefly disconnect and Android may ask for USB permission again. Approve it so final hardware readback can complete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Kt02h20ConnectionState.Disconnected -> {
                if (operationBusy) {
                    Text(
                        ew300OperationInProgressMessage(operationStatus)
                            ?: "The EW300 operation is still being verified. Do not reconnect manually until its result is shown.",
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
                        ew300OperationInProgressMessage(operationStatus)
                            ?: "The EW300 operation is still being verified. Do not retry the connection manually yet.",
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
                        ew300OperationInProgressMessage(operationStatus)
                            ?: "The EW300 operation is still being verified. Wait for the result before granting a new USB session.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Button(onClick = onConnect) { Text("Grant USB permission") }
                }
            }
        }
    }
}

internal fun ew300OperationInProgressMessage(
    operationStatus: Ew300OperationStatus,
): String? = when (operationStatus) {
    is Ew300OperationStatus.Running -> when (operationStatus.operation.uppercase()) {
        "FLASH" -> null
        "RESET" -> "Resetting EW300 EQ… Keep the DAC connected while final readback is verified."
        else -> "Applying EW300 ${operationStatus.operation.lowercase()}… Keep the DAC connected while final readback is verified."
    }
    else -> null
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
    parityEnabled: Boolean = false,
    runtimePersistence: Ew300RuntimePersistenceState = Ew300RuntimePersistenceState.UNKNOWN,
    runtimeSaveState: Ew300RuntimeSaveUiState = Ew300RuntimeSaveUiState(),
    onSaveRuntime: () -> Unit = {},
) {
    var resetConfirmationOpen by remember { mutableStateOf(false) }
    if (resetConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { resetConfirmationOpen = false },
            title = { Text("Reset EW300 EQ") },
            text = { Text("Reset and save the DAC EQ to flat? This sends one Save and may briefly disconnect the DAC.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetConfirmationOpen = false
                        onReset()
                    },
                ) { Text("Reset and save") }
            },
            dismissButton = {
                TextButton(onClick = { resetConfirmationOpen = false }) { Text("Cancel") }
            },
        )
    }
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
                "EW300 EQ response graph from 20 hertz to 20 kilohertz with %d active bands. Response ranges from %.1f to %.1f decibels, with a zero-decibel reference line.",
                activeBandCount,
                responseCurve.minimumGainDb,
                responseCurve.maximumGainDb,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (bundle.snapshot.filters.any {
            it.type == EqFilterType.LOW_SHELF || it.type == EqFilterType.HIGH_SHELF
        }
    ) {
        Text(
            "Experimental native shelf response · predicted · unmeasured until the owner listening test.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
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

    PremiumSectionLabel(text = "Current EQ · Five-band bank", divider = false)
    bundle.snapshot.filters.sortedBy { it.index }.forEach { filter ->
        val typeLabel = when (filter.type) {
            EqFilterType.PEAK -> "Peak"
            EqFilterType.LOW_SHELF -> "Low Shelf"
            EqFilterType.HIGH_SHELF -> "High Shelf"
            else -> filter.type.name
        }
        val spokenType = typeLabel
        Text(
            String.format(
                Locale.US,
                "%d   %s   %,d Hz   %+.1f dB   Q %.3f",
                filter.index + 1,
                typeLabel,
                filter.frequencyHz.roundToInt(),
                filter.gainDb,
                filter.q,
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = String.format(
                    Locale.US,
                    "Band %d, %s, %d hertz, %s decibels, Q %s.",
                    filter.index + 1,
                    spokenType,
                    filter.frequencyHz.roundToInt(),
                    String.format(Locale.US, "%+.1f", filter.gainDb),
                    String.format(Locale.US, "%.3f", filter.q),
                )
            },
        )
    }

    PremiumSectionLabel(text = "Saved settings", divider = false)
    Text(
        when (runtimePersistence) {
            Ew300RuntimePersistenceState.RUNTIME_ONLY ->
                "Runtime only · EQ and output gain have not been saved to the DAC."
            Ew300RuntimePersistenceState.SAVING -> "Saving the current EQ and output gain…"
            Ew300RuntimePersistenceState.SAVED_THIS_SESSION ->
                "Saved in this session · final readback matched."
            Ew300RuntimePersistenceState.SAVE_NOT_VERIFIED ->
                "Save not verified · the Save may have completed. Read the DAC before another action."
            Ew300RuntimePersistenceState.UNKNOWN ->
                "Saved settings are unknown until this app verifies and saves a change."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    runtimeSaveState.message?.let { message ->
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
    runtimeSaveState.error?.let { error ->
        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    if (parityEnabled) {
        OutlinedButton(
            onClick = onSaveRuntime,
            enabled = controlsEnabled && runtimePersistence == Ew300RuntimePersistenceState.RUNTIME_ONLY &&
                !runtimeSaveState.running,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (runtimeSaveState.running) "Saving…" else "Save EQ and output gain")
        }
        Text(
            "Saves the current EQ and output gain. Audio may briefly disconnect.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (canEdit) {
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit current EQ") }
        OutlinedButton(
            onClick = { resetConfirmationOpen = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset to flat") }
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
    runtimePersistence: Ew300RuntimePersistenceState = Ew300RuntimePersistenceState.UNKNOWN,
    runtimeSaveState: Ew300RuntimeSaveUiState = Ew300RuntimeSaveUiState(),
    uacState: Ew300UacUiState = Ew300UacUiState(),
    running: Boolean,
    operationBusy: Boolean = false,
    onRefresh: () -> Unit = {},
    onSetPlaybackGain: (Double) -> Unit = {},
    onSaveRuntime: () -> Unit = {},
    onSetVolumeLevel: (Int) -> Unit = {},
    onRefreshUac: () -> Unit = {},
    onSetMicrophoneMuted: (Boolean) -> Unit = {},
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
    val parityEnabled = BuildConfig.EW300_PARITY_EXPERIMENT_ENABLED
    var gainDialogOpen by remember { mutableStateOf(false) }
    var stagedGainDb by remember { mutableStateOf(0.0) }
    var volumeDialogOpen by remember { mutableStateOf(false) }
    var stagedVolumeLevel by remember { mutableStateOf(60) }
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
                        if (parityEnabled) {
                            "Allowed range: -64.0 dB to 0.0 dB in 0.5 dB steps. This applies for this session and is verified by readback; Save to DAC keeps it after unplugging."
                        } else {
                            "Allowed range: -64.0 dB to 0.0 dB in 0.5 dB steps. Every change is saved once and verified by final hardware readback."
                        },
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
                ) {
                    Text(
                        when {
                            playbackGainState.isWriting -> "Applying…"
                            parityEnabled -> "Apply for this session"
                            else -> "Save and verify"
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { gainDialogOpen = false },
                    enabled = !playbackGainState.isWriting,
                ) { Text("Cancel") }
            },
        )
    }
    if (volumeDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!playbackGainState.isWriting) volumeDialogOpen = false },
            title = { Text("EW300 device volume (0–60)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "EW300 0–60 mapping: 60 = 0 dB, 0 = −60 dB, one dB per step. This is not the JA11 0x02 command; it writes the EW300 output-gain register.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        String.format(
                            Locale.US,
                            "%d / 60 · %+.1f dB",
                            stagedVolumeLevel,
                            Ew300VolumeControl.levelToGainDb(stagedVolumeLevel),
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Slider(
                        value = stagedVolumeLevel.toFloat(),
                        onValueChange = { value -> stagedVolumeLevel = value.roundToInt().coerceIn(0, 60) },
                        valueRange = 0f..60f,
                        steps = 59,
                        enabled = !playbackGainState.isWriting && !operationBusy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Applies for this session. Save to DAC persists the current EQ and gain. Readback is verified after every change.",
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
                        volumeDialogOpen = false
                        onSetVolumeLevel(stagedVolumeLevel)
                    },
                    enabled = !playbackGainState.isWriting && !operationBusy,
                ) { Text(if (playbackGainState.isWriting) "Applying…" else "Apply for this session") }
            },
            dismissButton = {
                TextButton(onClick = { volumeDialogOpen = false }) { Text("Cancel") }
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
    if (parityEnabled) {
        PremiumValueRow(
            title = "Device volume (0–60)",
            value = currentGainDb?.let { gainDb ->
                val level = Ew300VolumeControl.gainDbToLevel(gainDb)
                if (level != null) {
                    String.format(Locale.US, "%d / 60 · %.1f dB", level, gainDb)
                } else {
                    String.format(Locale.US, "Unmapped · %.1f dB", gainDb)
                }
            } ?: "Not read",
            supportingText = "EW300 0–60 mapping: 60 = 0 dB, 0 = −60 dB. Both stereo 0x66 bytes are written and verified.",
            enabled = canChangeGain,
            showDisclosure = true,
            onClick = if (canChangeGain) {
                {
                    stagedVolumeLevel = currentGainDb?.let(Ew300VolumeControl::gainDbToLevel) ?: 0
                    volumeDialogOpen = true
                }
            } else {
                null
            },
        )
    }
    PremiumValueRow(
        title = "Output gain",
        value = currentGainDb?.let {
            String.format(Locale.US, "%.1f dB", it)
        } ?: "Not read",
        supportingText = when {
            playbackGainState.isWriting -> "Saving and verifying the requested output level…"
            playbackGainState.error != null -> playbackGainState.error
            canChangeGain && parityEnabled ->
                "Tap to adjust the EW300 output level for this session. Save to DAC persists the current EQ and gain."
            canChangeGain -> "Tap to adjust the EW300 playback level. Changes are persisted and read back before success is shown."
            else -> "Refresh the connected EW300 before changing its playback level."
        },
        enabled = canChangeGain,
        showDisclosure = true,
        onClick = if (canChangeGain) {
            {
                stagedGainDb = currentGainDb
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

    PremiumSectionLabel(text = "USB audio", divider = false)
    val uac = uacState.status
    PremiumValueRow(
        title = "Current sample rate",
        value = uac?.currentSampleRateHz?.let { "$it Hz" } ?: "Not read",
        supportingText = uac?.sampleRatePath
            ?: "Read-only UAC1 GET_CUR. Tap Read USB audio to query the exact streaming endpoint.",
    )
    PremiumValueRow(
        title = "USB Audio Class",
        value = uac?.uacVersion ?: "Not read",
        supportingText = if (uac?.uacModeSwitchSupported == true) {
            "Exact mode switch control available."
        } else {
            "Read-only descriptor state. One configuration/UAC1 is exposed; no exact mode switch control is established."
        },
    )
    val micMuteSupported = uac?.microphoneMuteSupported == true
    val micMuted = uac?.microphoneMuted
    if (micMuteSupported && micMuted != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = micMuted,
                    enabled = !uacState.busy && !operationBusy,
                    role = Role.Switch,
                    onValueChange = { onSetMicrophoneMuted(it) },
                )
                .semantics { contentDescription = "Microphone mute" }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Microphone mute", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (micMuted) "Muted · verified by GET_CUR after each change."
                    else "Live · verified by GET_CUR after each change.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = micMuted,
                onCheckedChange = null,
                enabled = !uacState.busy && !operationBusy,
            )
        }
    } else {
        PremiumValueRow(
            title = "Microphone mute",
            value = if (micMuteSupported) "Not read" else "Not established",
            supportingText = if (micMuteSupported) {
                "The feature unit exists but GET_CUR did not return a value. Read USB audio again."
            } else {
                "No UAC1 microphone feature unit with mute control was found on this connection."
            },
            enabled = false,
        )
    }
    PremiumValueRow(
        title = "Headset remote",
        value = if (uac?.headsetFeaturePresent == true) "Feature unit present" else "No disable control",
        supportingText = "HID consumer report 0x01 is passive; no exact EW300 headset-disable control is established.",
    )
    OutlinedButton(
        onClick = onRefreshUac,
        enabled = !uacState.busy && !operationBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (uacState.busy) "Reading USB audio…" else "Read USB audio")
    }
    uacState.error?.let { error ->
        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    uac?.blockedReasons?.forEach { reason ->
        Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    PremiumSectionLabel(text = "Saved settings", divider = false)
    PremiumValueRow(
        title = "Startup persistence",
        value = when (runtimePersistence) {
            Ew300RuntimePersistenceState.RUNTIME_ONLY -> "Runtime only"
            Ew300RuntimePersistenceState.SAVING -> "Saving"
            Ew300RuntimePersistenceState.SAVED_THIS_SESSION -> "Saved in this session"
            Ew300RuntimePersistenceState.SAVE_NOT_VERIFIED -> "Save not verified"
            Ew300RuntimePersistenceState.UNKNOWN -> "Unknown"
        },
        supportingText = runtimeSaveState.error ?: runtimeSaveState.message ?: when (runtimePersistence) {
            Ew300RuntimePersistenceState.RUNTIME_ONLY ->
                "The current EQ and output gain are live for this session only."
            Ew300RuntimePersistenceState.SAVED_THIS_SESSION ->
                "Final hardware readback matched after one Save."
            Ew300RuntimePersistenceState.SAVE_NOT_VERIFIED ->
                "Read the DAC before any later action; the Save may have completed."
            else -> "The app has not verified a saved change in this session."
        },
        enabled = parityEnabled && runtimePersistence == Ew300RuntimePersistenceState.RUNTIME_ONLY &&
            !runtimeSaveState.running && !operationBusy,
        showDisclosure = true,
        onClick = if (parityEnabled) onSaveRuntime else null,
    )

    PremiumSectionLabel(text = "About", divider = false)
    PremiumValueRow(title = "Model", value = "SIMGOT EW300 DSP")
    PremiumValueRow(title = "Firmware", value = report.firmwareVersion() ?: "Not read")
    PremiumValueRow(
        title = "USB identity",
        value = "31B2:0111 · revision 1.01 · HID interface 3",
    )
    PremiumValueRow(
        title = "Validation",
        value = "Qualified Peak profile",
        supportingText = "Physical Save/Flash/Reset evidence is tied to earlier exact artifacts; this candidate is local-only.",
    )
    PremiumValueRow(
        title = "Experimental features",
        value = if (parityEnabled) "Runtime Apply and output gain" else "Not enabled",
        supportingText = "Native Low/High Shelf controls remain dormant until the owner listening qualification.",
    )
    Text(
        "App controls for presets, microphone, UAC switching, and tuning have not been established for this EW300.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

}

private fun Ew300CapabilityReport?.firmwareVersion(): String? {
    val registers = this?.cases
        ?.flatMap { case -> case.registerValues.entries }
        ?.associate { it.key to it.value }
        ?: return null
    val bytes = mutableListOf<Byte>()
    listOf(0x04, 0x05).forEach { register ->
        val value = registers[register] ?: return null
        bytes += value.toList()
    }
    val text = bytes.takeWhile { (it.toInt() and 0xFF) != 0 }
        .map { (it.toInt() and 0xFF).toChar() }
        .joinToString("")
    return text.takeIf { it.isNotBlank() }
}

private fun shareReport(context: Context, subject: String, mimeType: String, contents: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, contents)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share EW300 report"))
}
