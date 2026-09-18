package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqMatchResolution
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiState
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import com.weekssa.opraeqforuapp.ui.components.PremiumValueRow

/**
 * Current product My DAC router. Only active supported product identities can enter this surface.
 * Historical protocol identities remain internal and never get a selector/card/roadmap presence.
 */
@Composable
fun MyDacRootScreen(
    recognitionState: DacRecognitionState,
    catalogState: CatalogState,
    blackPearlConnectionState: BlackPearlConnectionState,
    fiioJa11ConnectionState: Kt02h20ConnectionState,
    ew300ConnectionState: Kt02h20ConnectionState = Kt02h20ConnectionState.Disconnected,
    blackPearlHardwareEqState: HardwareEqSnapshotState,
    fiioJa11HardwareEqState: HardwareEqSnapshotState,
    ew300HardwareEqState: HardwareEqSnapshotState = HardwareEqSnapshotState(),
    blackPearlHardwareEqMatch: HardwareEqMatchResolution?,
    blackPearlManagedHeadphones: List<ManagedHeadphoneRecord>,
    blackPearlSavedEqs: List<SavedEqRecord>,
    blackPearlSavedGeneralEqs: List<SavedGeneralEqRecord>,
    blackPearlEditorState: MyDacEditorUiState,
    ew300EditorState: MyDacEditorUiState = MyDacEditorUiState(),
    blackPearlQualificationState: BlackPearlQualificationUiState,
    fiioJa11DeviceState: FiioJa11DeviceUiState,
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
    onOpenEw300Editor: () -> Unit = {},
    onBackEw300Editor: () -> Boolean = { false },
    onCloseEw300Editor: () -> Unit = {},
    onSelectEw300EditorBand: (Int) -> Unit = {},
    onShowEw300EditorAllBands: () -> Unit = {},
    onShowEw300EditorReview: () -> Unit = {},
    onUpdateEw300EditorBand: (Int, EqFilterType, Double, Double, Double) -> Unit = { _, _, _, _, _ -> },
    onUseSafeEw300EditorGain: () -> Unit = {},
    onResetEw300EditorLocalEdits: () -> Unit = {},
    onApplyEw300Editor: (Boolean) -> Unit = {},
    onCaptureBlackPearlDacEq: suspend (String, SavedEqHeadphoneAssociation?) -> String,
    onCaptureEw300DacEq: suspend (String, SavedEqHeadphoneAssociation?) -> String = { _, _ -> "Capture is not available." },
    onFlashBlackPearlFromMyDac: suspend (OpraEqProfile) -> String,
    onResetBlackPearlFromMyDac: suspend () -> String,
    onReadBlackPearlQualification: () -> Unit,
    onSetBlackPearlDeviceControl: (DacControlId, DacControlValue) -> Unit,
    onReadFiioJa11DeviceControls: () -> Unit,
    onSetFiioJa11OutputVolume: (Int) -> Unit,
    onSetFiioJa11EqProgram: (FiioJa11Protocol.EqProgram) -> Unit,
    onSetFiioJa11HeadsetControl: (Boolean) -> Unit,
    onSetFiioJa11UacMode: (FiioJa11Protocol.UacMode) -> Unit,
    onResetFiioJa11FromMyDac: suspend () -> String,
    onResetEw300FromMyDac: suspend () -> String = { "Reset is not available." },
    onQualifyEw300GlobalGain: suspend () -> String = { "EW300 gain qualification is not available." },
    onMessage: (String) -> Unit,
    onOperationStatus: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recognized = recognitionState.recognizedDeviceIds
        .filterTo(linkedSetOf()) { it == DacDeviceId.TRN_BLACK_PEARL || it == DacDeviceId.FIIO_JA11 || it == DacDeviceId.SIMGOT_EW300 }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(recognized, selectedName) {
        val selected = selectedName?.let { runCatching { DacDeviceId.valueOf(it) }.getOrNull() }
        if (selected !in recognized) {
            selectedName = when (recognized.size) {
                1 -> recognized.single().name
                else -> null
            }
        }
    }

    val selected = selectedName?.let { runCatching { DacDeviceId.valueOf(it) }.getOrNull() }
        ?.takeIf(recognized::contains)

    if (selected == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Choose connected DAC",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "More than one supported DAC is available. Choose the one you want My DAC to manage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            recognized.sortedBy(DacDeviceId::ordinal).forEach { deviceId ->
                PremiumValueRow(
                    title = when (deviceId) {
                        DacDeviceId.TRN_BLACK_PEARL -> "TRN Black Pearl"
                        DacDeviceId.FIIO_JA11 -> "FiiO JA11"
                        DacDeviceId.SIMGOT_EW300 -> "SIMGOT EW300 DSP"
                        DacDeviceId.JCALLY_JM12_STOCK -> "Unsupported device"
                    },
                    supportingText = "Manage this DAC",
                    showDisclosure = true,
                    onClick = { selectedName = deviceId.name },
                )
            }
        }
        return
    }

    when (selected) {
        DacDeviceId.TRN_BLACK_PEARL -> {
            val onlyBlackPearl = DacRecognitionState(
                presentDeviceIds = recognitionState.presentDeviceIds.filterTo(mutableSetOf()) {
                    it == DacDeviceId.TRN_BLACK_PEARL
                },
                recognizedDeviceIds = setOf(DacDeviceId.TRN_BLACK_PEARL),
            )
            MyDacScreen(
                recognitionState = onlyBlackPearl,
                catalogState = catalogState,
                blackPearlConnectionState = blackPearlConnectionState,
                fiioJa11ConnectionState = Kt02h20ConnectionState.Disconnected,
                ew300ConnectionState = Kt02h20ConnectionState.Disconnected,
                jcallyJm12ConnectionState = Kt02h20ConnectionState.Disconnected,
                blackPearlHardwareEqState = blackPearlHardwareEqState,
                blackPearlHardwareEqMatch = blackPearlHardwareEqMatch,
                blackPearlManagedHeadphones = blackPearlManagedHeadphones,
                blackPearlSavedEqs = blackPearlSavedEqs,
                blackPearlSavedGeneralEqs = blackPearlSavedGeneralEqs,
                blackPearlEditorState = blackPearlEditorState,
                blackPearlQualificationState = blackPearlQualificationState,
                onConnectDac = onConnectDac,
                onOpenBlackPearlEditor = onOpenBlackPearlEditor,
                onCloseBlackPearlEditor = onCloseBlackPearlEditor,
                onSelectBlackPearlEditorBand = onSelectBlackPearlEditorBand,
                onShowBlackPearlEditorAllBands = onShowBlackPearlEditorAllBands,
                onShowBlackPearlEditorReview = onShowBlackPearlEditorReview,
                onUpdateBlackPearlEditorBand = onUpdateBlackPearlEditorBand,
                onUseSafeBlackPearlEditorGain = onUseSafeBlackPearlEditorGain,
                onResetBlackPearlEditorLocalEdits = onResetBlackPearlEditorLocalEdits,
                onApplyBlackPearlEditor = onApplyBlackPearlEditor,
                onCaptureBlackPearlDacEq = onCaptureBlackPearlDacEq,
                onFlashBlackPearlFromMyDac = onFlashBlackPearlFromMyDac,
                onResetBlackPearlFromMyDac = onResetBlackPearlFromMyDac,
                onReadBlackPearlQualification = onReadBlackPearlQualification,
                onSetBlackPearlDeviceControl = onSetBlackPearlDeviceControl,
                onMessage = onMessage,
                onOperationStatus = onOperationStatus,
                modifier = modifier,
            )
        }

        DacDeviceId.FIIO_JA11 -> FiioJa11MyDacContent(
            connectionState = fiioJa11ConnectionState,
            hardwareEqState = fiioJa11HardwareEqState,
            deviceState = fiioJa11DeviceState,
            onConnect = { onConnectDac(DacDeviceId.FIIO_JA11) },
            onReadDeviceControls = onReadFiioJa11DeviceControls,
            onSetOutputVolume = onSetFiioJa11OutputVolume,
            onSetEqProgram = onSetFiioJa11EqProgram,
            onSetHeadsetControl = onSetFiioJa11HeadsetControl,
            onSetUacMode = onSetFiioJa11UacMode,
            onResetEq = onResetFiioJa11FromMyDac,
            onMessage = onMessage,
            modifier = modifier,
        )

        DacDeviceId.SIMGOT_EW300 -> Ew300MyDacContent(
            connectionState = ew300ConnectionState,
            hardwareEqState = ew300HardwareEqState,
            editorState = ew300EditorState,
            catalogState = catalogState,
            managedHeadphones = blackPearlManagedHeadphones,
            savedEqs = blackPearlSavedEqs,
            onConnect = { onConnectDac(DacDeviceId.SIMGOT_EW300) },
            onResetEq = onResetEw300FromMyDac,
            onQualifyGlobalGain = onQualifyEw300GlobalGain,
            onCaptureDacEq = onCaptureEw300DacEq,
            onOpenEditor = onOpenEw300Editor,
            onCloseEditor = onCloseEw300Editor,
            onSelectBand = onSelectEw300EditorBand,
            onShowAllBands = onShowEw300EditorAllBands,
            onShowReview = onShowEw300EditorReview,
            onUpdateBand = onUpdateEw300EditorBand,
            onUseSafeGain = onUseSafeEw300EditorGain,
            onResetEdits = onResetEw300EditorLocalEdits,
            onApply = onApplyEw300Editor,
            onMessage = onMessage,
            modifier = modifier,
        )

        DacDeviceId.JCALLY_JM12_STOCK -> Unit
    }
}
