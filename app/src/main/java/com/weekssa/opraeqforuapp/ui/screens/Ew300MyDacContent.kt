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
import com.weekssa.opraeqforuapp.domain.ew300.Ew300ShelfTestTarget
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
    onApplyShelfTestTarget: suspend (Ew300ShelfTestTarget) -> String = { "Shelf A/B is unavailable." },
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
                            onShelfTestTarget = { target ->
                                scope.launch { onMessage(onApplyShelfTestTarget(target)) }
                            },
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
                        ?: "After Apply or Flash, the EW300 may briefly disconnect and Android may ask for USB permission again. Approve it so the final check can complete.",
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
        "RESET" -> "Resetting EW300 EQ… Keep the DAC connected while the final check completes."
        else -> "Applying EW300 ${operationStatus.operation.lowercase()}… Keep the DAC connected while the final check completes."
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
            message = "✓ $successMessage$reconnectMessage Final hardware check matched.",
            verified = true,
        )
    } else {
        val reconnectMessage = if (trace.replacementObserved) {
            " after USB reconnect"
        } else {
            ""
        }
        Ew300OperationStatusPresentation(
            message = "Last EW300 $operation was not verified$reconnectMessage. The previous Save may have completed, but the final hardware check did not verify. Stop and reconnect or refresh before any later write.",
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
    onShelfTestTarget: (Ew300ShelfTestTarget) -> Unit = {},
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
                "No verified EW300 settings are available yet."
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
            "Experimental native shelf response · predicted from the written parameters · acoustics unmeasured.",
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

    if (BuildConfig.EW300_ENGINEERING_CONTROLS) {
        PremiumSectionLabel(text = "Native shelf engineering controls", divider = false)
        Text(
            "Experimental and unmeasured. Runtime-only, zero Save: each control writes the fixed type 3 / type 4 shelf block and verifies readback. Not a product feature.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onShelfTestTarget(Ew300ShelfTestTarget.FLAT) },
                enabled = canEdit,
                modifier = Modifier.weight(1f),
            ) { Text("Set Flat") }
            OutlinedButton(
                onClick = { onShelfTestTarget(Ew300ShelfTestTarget.LOW_SHELF) },
                enabled = canEdit,
                modifier = Modifier.weight(1f),
            ) { Text("Set Low Shelf") }
            OutlinedButton(
                onClick = { onShelfTestTarget(Ew300ShelfTestTarget.HIGH_SHELF) },
                enabled = canEdit,
                modifier = Modifier.weight(1f),
            ) { Text("Set High Shelf") }
        }
    }

    PremiumSectionLabel(text = "Saved settings", divider = false)
    Text(
        when (runtimePersistence) {
            Ew300RuntimePersistenceState.RUNTIME_ONLY ->
                "Runtime only · EQ and output gain have not been saved to the DAC."
            Ew300RuntimePersistenceState.SAVING -> "Saving the current EQ and output gain…"
            Ew300RuntimePersistenceState.SAVED_THIS_SESSION ->
                "Saved in this session · verified on the DAC."
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
        Text(if (canCapture) "Save device EQ as Personal EQ" else "Personal EQ capture is unavailable for these settings")
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
    var volumeDialogOpen by remember { mutableStateOf(false) }
    var stagedVolumeLevel by remember { mutableStateOf(60) }
    var aboutExpanded by rememberSaveable { mutableStateOf(false) }
    val hardwareBundle = hardwareEqState.bundle
    val currentGainDb = hardwareBundle?.snapshot?.playbackGainDb
    val volumeLevel = currentGainDb?.let(Ew300VolumeControl::gainDbToLevel)
    val controlsEnabled = hardwareBundle != null &&
        hardwareEqState.freshness == DacStateFreshness.CURRENT &&
        !hardwareEqState.isReading &&
        !playbackGainState.isWriting &&
        !operationBusy

    if (volumeDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!playbackGainState.isWriting) volumeDialogOpen = false },
            title = { Text("Set device volume") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "60 is the loudest setting and 0 is silent; each step is one dB. Changes apply for this session until you save from the EQ tab.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        String.format(
                            Locale.US,
                            "%d / 60 · %s%.1f dB",
                            stagedVolumeLevel,
                            if (Ew300VolumeControl.levelToGainDb(stagedVolumeLevel) > 0) "+" else "",
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
                ) {
                    Text(if (playbackGainState.isWriting) "Applying…" else "Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { volumeDialogOpen = false },
                    enabled = !playbackGainState.isWriting,
                ) { Text("Cancel") }
            },
        )
    }

    DeviceOperationStatusHeader(
        isReading = hardwareEqState.isReading,
        isWriting = playbackGainState.isWriting || uacState.busy,
        activeWriteControlId = null,
        lastVerifiedWriteControlId = null,
        hasSnapshot = hardwareBundle != null,
        isCurrentSession = hardwareEqState.freshness == DacStateFreshness.CURRENT,
        error = when {
            hardwareEqState.readFailed -> "Couldn't read the EW300 settings."
            else -> playbackGainState.error
        },
        enabled = !operationBusy,
        busy = hardwareEqState.isReading || operationBusy,
        onRefresh = onRefresh,
        controlName = { "device setting" },
    )

    if (hardwareBundle == null) {
        Text(
            text = "Connect SIMGOT EW300 DSP to view its settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        return
    }

    if (hardwareEqState.freshness != DacStateFreshness.CURRENT) {
        Text(
            text = "Showing the last read settings. Refresh before relying on them.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    DeviceSectionTitle("Audio")
    SimpleDeviceSettingRow(
        label = "Device volume",
        value = volumeLevel?.let { "$it / 60" } ?: "Not read",
        enabled = controlsEnabled && parityEnabled && volumeLevel != null,
        onClick = {
            stagedVolumeLevel = volumeLevel ?: 0
            volumeDialogOpen = true
        },
    )
    val startupLabel = when (runtimePersistence) {
        Ew300RuntimePersistenceState.SAVED_THIS_SESSION -> "Saved"
        Ew300RuntimePersistenceState.RUNTIME_ONLY -> "Not saved"
        else -> null
    }
    if (startupLabel != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Startup settings", style = MaterialTheme.typography.labelLarge)
                Text(startupLabel, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    DeviceSectionTitle("USB & System")
    ReadOnlyValueRow(
        label = "Current sample rate",
        value = uacState.status?.currentSampleRateHz?.let { rate ->
            String.format(Locale.US, "%,d Hz", rate)
        } ?: "Not read",
    )
    ReadOnlyValueRow(
        label = "USB Audio Class",
        value = uacState.status?.uacVersion ?: "Not read",
    )

    DeviceSectionTitle("About")
    ExpandableAboutHeader(
        expanded = aboutExpanded,
        onToggle = { aboutExpanded = !aboutExpanded },
    )
    if (aboutExpanded) {
        ReadOnlyValueRow(label = "Model", value = "SIMGOT EW300 DSP")
        report.firmwareVersion()?.let { firmware ->
            ReadOnlyValueRow(label = "Firmware", value = firmware)
        }
        ReadOnlyValueRow(label = "USB identity", value = "31B2:0111")
        ReadOnlyValueRow(label = "Filters", value = "Five-band Peak, verified")
        ReadOnlyValueRow(
            label = "Device limitations",
            value = "Headset-remote disable and UAC mode switching are chip-limited; native shelf acoustics are unmeasured; the device accepts a microphone mute request, but no effect on captured audio has been measured.",
        )
    }

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
