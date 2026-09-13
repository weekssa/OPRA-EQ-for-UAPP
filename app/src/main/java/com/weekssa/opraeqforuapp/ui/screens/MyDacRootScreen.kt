package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
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
    blackPearlHardwareEqState: HardwareEqSnapshotState,
    fiioJa11HardwareEqState: HardwareEqSnapshotState,
    blackPearlHardwareEqMatch: HardwareEqMatchResolution?,
    blackPearlManagedHeadphones: List<ManagedHeadphoneRecord>,
    blackPearlSavedEqs: List<SavedEqRecord>,
    blackPearlSavedGeneralEqs: List<SavedGeneralEqRecord>,
    blackPearlEditorState: MyDacEditorUiState,
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
    onCaptureBlackPearlDacEq: suspend (String, SavedEqHeadphoneAssociation?) -> String,
    onFlashBlackPearlFromMyDac: suspend (OpraEqProfile) -> String,
    onResetBlackPearlFromMyDac: suspend () -> String,
    onReadBlackPearlQualification: () -> Unit,
    onReadFiioJa11DeviceControls: () -> Unit,
    onSetFiioJa11OutputVolume: (Int) -> Unit,
    onSetFiioJa11EqProgram: (FiioJa11Protocol.EqProgram) -> Unit,
    onSetFiioJa11HeadsetControl: (Boolean) -> Unit,
    onSetFiioJa11UacMode: (FiioJa11Protocol.UacMode) -> Unit,
    onResetFiioJa11FromMyDac: suspend () -> String,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recognized = recognitionState.recognizedDeviceIds
        .filterTo(linkedSetOf()) { it == DacDeviceId.TRN_BLACK_PEARL || it == DacDeviceId.FIIO_JA11 }
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Choose connected DAC", fontWeight = FontWeight.SemiBold)
            Text(
                "More than one supported DAC has been recognized in this app session. Choose the physical device you want My DAC to manage.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            recognized.sortedBy(DacDeviceId::ordinal).forEach { deviceId ->
                TextButton(
                    onClick = { selectedName = deviceId.name },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when (deviceId) {
                            DacDeviceId.TRN_BLACK_PEARL -> "TRN Black Pearl"
                            DacDeviceId.FIIO_JA11 -> "FiiO JA11"
                            DacDeviceId.JCALLY_JM12_STOCK -> "Unsupported device"
                        },
                    )
                }
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
                onMessage = onMessage,
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

        DacDeviceId.JCALLY_JM12_STOCK -> Unit
    }
}
