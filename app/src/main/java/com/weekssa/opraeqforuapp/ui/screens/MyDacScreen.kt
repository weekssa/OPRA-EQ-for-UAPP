package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.AmbiguousExactHardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqDifference
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqDifferenceField
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatch
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatchResolution
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqResponseEvaluator
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.dac.isAcousticallyActive
import com.weekssa.opraeqforuapp.domain.export.DevicePresetFidelity
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.MyDacEditorApplyStatus
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import com.weekssa.opraeqforuapp.ui.components.DacEqResponseGraph
import com.weekssa.opraeqforuapp.ui.components.PremiumSectionLabel
import kotlinx.coroutines.launch

/**
 * Black Pearl My DAC surface.
 *
 * EQ choice intentionally lives in My EQs / EQ Library. This screen shows the actual hardware EQ,
 * lets the user edit/capture/reset it, and exposes the current DEVICE settings without creating a
 * second library browser inside My DAC.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun MyDacScreen(
    recognitionState: DacRecognitionState,
    catalogState: CatalogState,
    blackPearlConnectionState: BlackPearlConnectionState,
    fiioJa11ConnectionState: Kt02h20ConnectionState,
    ew300ConnectionState: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
    jcallyJm12ConnectionState: Kt02h20ConnectionState,
    blackPearlHardwareEqState: HardwareEqSnapshotState,
    blackPearlHardwareEqMatch: HardwareEqMatchResolution?,
    blackPearlManagedHeadphones: List<ManagedHeadphoneRecord>,
    blackPearlSavedEqs: List<SavedEqRecord>,
    blackPearlSavedGeneralEqs: List<SavedGeneralEqRecord>,
    blackPearlEditorState: MyDacEditorUiState,
    blackPearlQualificationState: BlackPearlQualificationUiState,
    onConnectDac: (DacDeviceId) -> Unit,
    onOpenBlackPearlEditor: () -> Unit,
    onCloseBlackPearlEditor: () -> Unit,
    onSelectBlackPearlEditorBand: (Int) -> Unit,
    onShowBlackPearlEditorAllBands: () -> Unit,
    onShowBlackPearlEditorReview: () -> Unit,
    onUpdateBlackPearlEditorBand: (Int, EqFilterType, Double, Double, Double) -> Unit,
    onUseSafeBlackPearlEditorGain: () -> Unit,
    onResetBlackPearlEditorLocalEdits: () -> Unit,
    onApplyBlackPearlEditor: (Boolean) -> Unit,
    onCaptureBlackPearlDacEq: suspend (String, SavedEqHeadphoneAssociation?) -> String,
    onFlashBlackPearlFromMyDac: suspend (OpraEqProfile) -> String,
    onResetBlackPearlFromMyDac: suspend () -> String,
    onReadBlackPearlQualification: () -> Unit,
    onSetBlackPearlDeviceControl: (DacControlId, DacControlValue) -> Unit,
    onMessage: (String) -> Unit,
    onOperationStatus: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val recognized = recognitionState.recognizedDeviceIds.sortedBy(DacDeviceId::ordinal)
    val present = recognitionState.presentDeviceIds.sortedBy(DacDeviceId::ordinal)
    var selectedDeviceName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var saveDacEqOpen by remember { mutableStateOf(false) }
    var resetEqOpen by remember { mutableStateOf(false) }

    LaunchedEffect(recognized, present, selectedDeviceName) {
        val selected = selectedDeviceName?.let { name ->
            runCatching { DacDeviceId.valueOf(name) }.getOrNull()
        }
        if (selected !in recognized) {
            saveDacEqOpen = false
            resetEqOpen = false
            selectedDeviceName = when {
                recognized.size == 1 -> recognized.single().name
                present.size == 1 -> present.single().name
                else -> null
            }
        }
    }

    val selectedDevice = selectedDeviceName?.let { name ->
        runCatching { DacDeviceId.valueOf(name) }.getOrNull()
    }?.takeIf(recognized::contains)

    if (saveDacEqOpen && selectedDevice == DacDeviceId.TRN_BLACK_PEARL) {
        BlackPearlSaveDacEqDialog(
            catalogState = catalogState,
            managedHeadphones = blackPearlManagedHeadphones,
            savedEqs = blackPearlSavedEqs,
            onDismiss = { saveDacEqOpen = false },
            onSave = onCaptureBlackPearlDacEq,
            onMessage = onMessage,
        )
    }

    if (resetEqOpen && selectedDevice == DacDeviceId.TRN_BLACK_PEARL) {
        AlertDialog(
            onDismissRequest = { resetEqOpen = false },
            title = { Text(stringResource(R.string.my_dac_reset_title)) },
            text = { Text(stringResource(R.string.my_dac_reset_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetEqOpen = false
                        scope.launch { onMessage(onResetBlackPearlFromMyDac()) }
                    },
                ) {
                    Text(stringResource(R.string.my_dac_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { resetEqOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (selectedDevice == null) {
            Text(
                text = stringResource(R.string.my_dac_multiple_devices),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.my_dac_choose_device),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            recognized.forEach { deviceId ->
                TextButton(
                    onClick = {
                        saveDacEqOpen = false
                        resetEqOpen = false
                        onCloseBlackPearlEditor()
                        selectedDeviceName = deviceId.name
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(deviceLabel(deviceId))
                }
            }
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = deviceLabel(selectedDevice),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = connectionStatus(
                    deviceId = selectedDevice,
                    presentNow = selectedDevice in recognitionState.presentDeviceIds,
                    blackPearl = blackPearlConnectionState,
                    fiioJa11 = fiioJa11ConnectionState,
                    ew300 = ew300ConnectionState,
                    jcallyJm12 = jcallyJm12ConnectionState,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (
            shouldOfferMyDacConnect(
                deviceId = selectedDevice,
                recognitionState = recognitionState,
                blackPearl = blackPearlConnectionState,
                fiioJa11 = fiioJa11ConnectionState,
                ew300 = ew300ConnectionState,
                jcallyJm12 = jcallyJm12ConnectionState,
            )
        ) {
            Text(
                text = stringResource(R.string.my_dac_connect_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onConnectDac(selectedDevice) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (
                            hasMyDacConnectionError(
                                deviceId = selectedDevice,
                                blackPearl = blackPearlConnectionState,
                                fiioJa11 = fiioJa11ConnectionState,
                                ew300 = ew300ConnectionState,
                                jcallyJm12 = jcallyJm12ConnectionState,
                            )
                        ) {
                            R.string.my_dac_action_retry_connect
                        } else {
                            R.string.my_dac_action_connect
                        },
                    ),
                )
            }
        }

        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text(stringResource(R.string.my_dac_tab_eq)) },
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = {
                    saveDacEqOpen = false
                    resetEqOpen = false
                    onCloseBlackPearlEditor()
                    selectedTabIndex = 1
                },
                text = { Text(stringResource(R.string.my_dac_tab_device)) },
            )
        }

        when (selectedTabIndex) {
            0 -> when (selectedDevice) {
                DacDeviceId.TRN_BLACK_PEARL -> {
                    if (
                        blackPearlEditorState.isOpening ||
                        blackPearlEditorState.isOpen ||
                        blackPearlEditorState.error != null
                    ) {
                        DacEqEditorScreen(
                            state = blackPearlEditorState,
                            onRetryOpen = onOpenBlackPearlEditor,
                            onClose = onCloseBlackPearlEditor,
                            onSelectBand = onSelectBlackPearlEditorBand,
                            onShowAllBands = onShowBlackPearlEditorAllBands,
                            onShowReview = onShowBlackPearlEditorReview,
                            onUpdateBand = onUpdateBlackPearlEditorBand,
                            onUseSafeGain = onUseSafeBlackPearlEditorGain,
                            onResetEdits = onResetBlackPearlEditorLocalEdits,
                            onApply = onApplyBlackPearlEditor,
                        )
                    } else {
                        BlackPearlEditorApplyFeedback(blackPearlEditorState)
                        BlackPearlEqStatus(
                            snapshotState = blackPearlHardwareEqState,
                            matchResolution = blackPearlHardwareEqMatch,
                            canEdit = blackPearlConnectionState is BlackPearlConnectionState.Connected,
                            onEdit = onOpenBlackPearlEditor,
                            onReset = { resetEqOpen = true },
                            onSaveDacEq = { saveDacEqOpen = true },
                        )
                    }
                }
                DacDeviceId.FIIO_JA11,
                DacDeviceId.SIMGOT_EW300,
                DacDeviceId.JCALLY_JM12_STOCK,
                -> Text(stringResource(R.string.my_dac_hardware_pending_eq))
            }

            else -> {
                CapabilityDrivenDeviceStatus(
                    deviceId = selectedDevice,
                    blackPearlQualificationState = blackPearlQualificationState,
                    blackPearlQualificationEnabled =
                        selectedDevice == DacDeviceId.TRN_BLACK_PEARL &&
                            blackPearlConnectionState is BlackPearlConnectionState.Connected,
                    onReadBlackPearlQualification = onReadBlackPearlQualification,
                    onSetBlackPearlDeviceControl = onSetBlackPearlDeviceControl,
                )
                if (selectedDevice == DacDeviceId.TRN_BLACK_PEARL) {
                    BlackPearlDeviceResetSection(
                        state = blackPearlQualificationState,
                        enabled = blackPearlConnectionState is BlackPearlConnectionState.Connected,
                        onSetDeviceControl = onSetBlackPearlDeviceControl,
                        onResetEqToFlat = onResetBlackPearlFromMyDac,
                        onMessage = onMessage,
                        onOperationStatus = onOperationStatus,
                    )
                }
            }
        }
    }
}

@Composable
private fun BlackPearlEditorApplyFeedback(state: MyDacEditorUiState) {
    when (state.applyStatus) {
        MyDacEditorApplyStatus.VERIFIED -> Text(
            text = stringResource(R.string.my_dac_editor_apply_verified),
            fontWeight = FontWeight.SemiBold,
        )
        MyDacEditorApplyStatus.FAILED -> Text(
            text = stringResource(
                R.string.my_dac_editor_apply_failed,
                state.applyFailureReason.orEmpty(),
            ),
            color = MaterialTheme.colorScheme.error,
        )
        else -> Unit
    }
}

internal fun shouldOfferMyDacConnect(
    deviceId: DacDeviceId,
    recognitionState: DacRecognitionState,
    blackPearl: BlackPearlConnectionState,
    fiioJa11: Kt02h20ConnectionState,
    ew300: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
    jcallyJm12: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
): Boolean {
    if (deviceId !in recognitionState.presentDeviceIds) return false
    return when (deviceId) {
        DacDeviceId.TRN_BLACK_PEARL ->
            blackPearl is BlackPearlConnectionState.Disconnected ||
                blackPearl is BlackPearlConnectionState.Error
        DacDeviceId.FIIO_JA11 ->
            fiioJa11 is Kt02h20ConnectionState.Disconnected ||
                fiioJa11 is Kt02h20ConnectionState.Error
        DacDeviceId.SIMGOT_EW300 ->
            ew300 is Kt02h20ConnectionState.Disconnected || ew300 is Kt02h20ConnectionState.Error
        DacDeviceId.JCALLY_JM12_STOCK ->
            jcallyJm12 is Kt02h20ConnectionState.Disconnected ||
                jcallyJm12 is Kt02h20ConnectionState.Error
    }
}

internal fun hasMyDacConnectionError(
    deviceId: DacDeviceId,
    blackPearl: BlackPearlConnectionState,
    fiioJa11: Kt02h20ConnectionState,
    ew300: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
    jcallyJm12: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
): Boolean = when (deviceId) {
    DacDeviceId.TRN_BLACK_PEARL -> blackPearl is BlackPearlConnectionState.Error
    DacDeviceId.FIIO_JA11 -> fiioJa11 is Kt02h20ConnectionState.Error
    DacDeviceId.SIMGOT_EW300 -> ew300 is Kt02h20ConnectionState.Error
    DacDeviceId.JCALLY_JM12_STOCK -> jcallyJm12 is Kt02h20ConnectionState.Error
}

@Composable
private fun BlackPearlEqStatus(
    snapshotState: HardwareEqSnapshotState,
    matchResolution: HardwareEqMatchResolution?,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onReset: () -> Unit,
    onSaveDacEq: () -> Unit,
) {
    if (snapshotState.isReading) {
        Text(
            text = stringResource(R.string.my_dac_reading_eq),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val bundle = snapshotState.bundle
    if (bundle == null) {
        Text(
            text = stringResource(R.string.my_dac_no_verified_eq),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    PremiumSectionLabel(
        text = stringResource(
            if (snapshotState.freshness == DacStateFreshness.CURRENT) {
                R.string.my_dac_current_hardware
            } else {
                R.string.my_dac_last_read
            },
        ),
        divider = false,
    )

    val match = matchResolution?.match
    Text(
        text = when (match) {
            HardwareEqMatch.Flat -> stringResource(R.string.my_dac_eq_flat)
            is HardwareEqMatch.Exact -> stringResource(R.string.my_dac_eq_exact)
            is AmbiguousExactHardwareEqMatch -> stringResource(R.string.my_dac_eq_ambiguous)
            is HardwareEqMatch.ModifiedKnown -> stringResource(R.string.my_dac_eq_modified)
            HardwareEqMatch.Unknown, null -> stringResource(R.string.my_dac_eq_unknown)
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )

    when (match) {
        is HardwareEqMatch.Exact -> {
            Text(
                text = match.savedEq.displayName,
                style = MaterialTheme.typography.bodyLarge,
            )
            val representation = matchResolution.representation(match.savedEq.savedEqKey)
            if (representation != null) {
                val fidelity = stringResource(
                    if (representation.fidelity == DevicePresetFidelity.EXACT) {
                        R.string.output_status_exact
                    } else {
                        R.string.output_status_optimized
                    },
                )
                Text(
                    text = stringResource(
                        R.string.my_dac_adaptation,
                        fidelity,
                        representation.adaptationSummary,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is HardwareEqMatch.ModifiedKnown -> {
            Text(
                text = stringResource(R.string.my_dac_eq_modified_origin, match.savedEq.displayName),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.my_dac_eq_modified_difference_count, match.differences.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            match.differences.forEach { difference ->
                Text(
                    text = modifiedDifferenceText(difference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HardwareEqMatch.Unknown, null -> Text(
            text = stringResource(R.string.my_dac_eq_unknown_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> Unit
    }

    val responseCurve = remember(bundle.snapshot.filters) {
        HardwareEqResponseEvaluator.evaluate(bundle.snapshot.filters)
    }
    PremiumSectionLabel(
        text = stringResource(R.string.my_dac_eq_response),
        divider = false,
    )
    if (responseCurve == null) {
        Text(
            text = stringResource(R.string.my_dac_response_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val activeBandCount = bundle.snapshot.filters.count { filter -> filter.isAcousticallyActive() }
        DacEqResponseGraph(
            curve = responseCurve,
            filters = bundle.snapshot.filters,
            accessibilityDescription = stringResource(
                R.string.my_dac_response_graph_accessibility,
                activeBandCount,
                responseCurve.minimumGainDb,
                responseCurve.maximumGainDb,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    HorizontalDivider()
    Text(
        text = stringResource(R.string.my_dac_filter_count, bundle.snapshot.filters.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    bundle.snapshot.activeSlot?.let { slot ->
        Text(
            text = stringResource(R.string.my_dac_active_slot, slot),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    bundle.snapshot.playbackGainDb?.let { gainDb ->
        Text(
            text = stringResource(R.string.my_dac_playback_gain, gainDb),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Button(
        onClick = onEdit,
        enabled = canEdit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.my_dac_action_edit_eq))
    }

    if (
        shouldOfferSaveDacEq(
            match = match,
            freshness = snapshotState.freshness,
            connected = canEdit,
        )
    ) {
        OutlinedButton(
            onClick = onSaveDacEq,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.my_dac_action_save_eq))
        }
    }

    OutlinedButton(
        onClick = onReset,
        enabled = canEdit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.my_dac_action_reset_eq))
    }
}

@Composable
private fun modifiedDifferenceText(difference: HardwareEqDifference): String {
    val field = differenceFieldLabel(difference.field)
    val band = difference.bandIndex
    return if (band == null) {
        stringResource(
            R.string.my_dac_eq_modified_difference_global,
            field,
            difference.expectedValue,
            difference.actualValue,
        )
    } else {
        stringResource(
            R.string.my_dac_eq_modified_difference,
            band + 1,
            field,
            difference.expectedValue,
            difference.actualValue,
        )
    }
}

@Composable
private fun differenceFieldLabel(field: HardwareEqDifferenceField): String = stringResource(
    when (field) {
        HardwareEqDifferenceField.ENABLED -> R.string.my_dac_editor_field_enabled
        HardwareEqDifferenceField.FILTER_TYPE -> R.string.my_dac_editor_field_filter_type
        HardwareEqDifferenceField.FREQUENCY_HZ -> R.string.my_dac_editor_field_frequency
        HardwareEqDifferenceField.GAIN_DB -> R.string.my_dac_editor_field_gain
        HardwareEqDifferenceField.Q -> R.string.my_dac_editor_field_q
        HardwareEqDifferenceField.DEDICATED_EQ_PREAMP_DB -> R.string.my_dac_editor_field_preamp
    },
)

@Composable
private fun deviceLabel(deviceId: DacDeviceId): String = stringResource(
    when (deviceId) {
        DacDeviceId.TRN_BLACK_PEARL -> R.string.dac_trn_black_pearl
        DacDeviceId.FIIO_JA11 -> R.string.dac_fiio_ja11
        DacDeviceId.SIMGOT_EW300 -> R.string.dac_simgot_ew300
        DacDeviceId.JCALLY_JM12_STOCK -> R.string.dac_jcally_jm12
    },
)

@Composable
private fun connectionStatus(
    deviceId: DacDeviceId,
    presentNow: Boolean,
    blackPearl: BlackPearlConnectionState,
    fiioJa11: Kt02h20ConnectionState,
    ew300: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
    jcallyJm12: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
): String = when (deviceId) {
    DacDeviceId.TRN_BLACK_PEARL -> when (blackPearl) {
        BlackPearlConnectionState.Connected -> stringResource(R.string.my_dac_connected)
        BlackPearlConnectionState.Connecting -> stringResource(R.string.my_dac_connecting)
        BlackPearlConnectionState.Disconnected -> stringResource(
            if (presentNow) R.string.my_dac_detected_not_open else R.string.my_dac_disconnected,
        )
        is BlackPearlConnectionState.Error -> blackPearl.message
    }

    DacDeviceId.FIIO_JA11 -> ktConnectionStatus(fiioJa11, presentNow)
    DacDeviceId.SIMGOT_EW300 -> ktConnectionStatus(ew300, presentNow)
    DacDeviceId.JCALLY_JM12_STOCK -> ktConnectionStatus(jcallyJm12, presentNow)
}

@Composable
private fun ktConnectionStatus(
    state: Kt02h20ConnectionState,
    presentNow: Boolean,
): String = when (state) {
    Kt02h20ConnectionState.Connected -> stringResource(R.string.my_dac_connected)
    Kt02h20ConnectionState.Connecting -> stringResource(R.string.my_dac_connecting)
    Kt02h20ConnectionState.Disconnected -> stringResource(
        if (presentNow) R.string.my_dac_detected_not_open else R.string.my_dac_disconnected,
    )
    is Kt02h20ConnectionState.Error -> state.message
}
