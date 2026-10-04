package com.weekssa.opraeqforuapp.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacStateFreshness
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationOutcome
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationStatus
import com.weekssa.opraeqforuapp.domain.ew300.Ew300OperationTrace
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.withHiddenReviewPromptsSuppressed
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.update.SemVer
import com.weekssa.opraeqforuapp.ui.components.ConnectedDeviceSurface
import com.weekssa.opraeqforuapp.ui.components.ContextualDeviceChoiceSurface
import com.weekssa.opraeqforuapp.ui.components.ExportReviewDialog
import com.weekssa.opraeqforuapp.ui.components.ExportReviewItem
import com.weekssa.opraeqforuapp.ui.components.PostUpdateBanner
import com.weekssa.opraeqforuapp.ui.components.FlashFeedback
import com.weekssa.opraeqforuapp.ui.components.FlashFeedbackBanner
import com.weekssa.opraeqforuapp.ui.components.FlashFeedbackPhase
import com.weekssa.opraeqforuapp.ui.components.blackPearlFlashFeedback
import com.weekssa.opraeqforuapp.ui.components.TargetContextSelector
import com.weekssa.opraeqforuapp.ui.components.UpdateAvailableBanner
import com.weekssa.opraeqforuapp.ui.components.WhatsNewDialog
import com.weekssa.opraeqforuapp.ui.components.flashDeviceLabel
import com.weekssa.opraeqforuapp.ui.components.expiresAutomatically
import com.weekssa.opraeqforuapp.ui.screens.BrowseOpraScreen
import com.weekssa.opraeqforuapp.ui.screens.ManagedHeadphoneDetailScreen
import com.weekssa.opraeqforuapp.ui.screens.MyDacRootScreen
import com.weekssa.opraeqforuapp.ui.screens.deviceOperationControlLabel
import com.weekssa.opraeqforuapp.ui.screens.DeviceOperationPhase
import com.weekssa.opraeqforuapp.ui.screens.deviceOperationFeedback
import com.weekssa.opraeqforuapp.ui.screens.deviceOperationStateSignature
import com.weekssa.opraeqforuapp.ui.screens.fiioJa11OperationStatusPresentation
import com.weekssa.opraeqforuapp.ui.screens.hardwareFlashStartedMessage
import com.weekssa.opraeqforuapp.ui.screens.MyEqsHomeScreen
import com.weekssa.opraeqforuapp.ui.screens.SettingsScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface ActiveOutputExportRequest {
    val device: ExportDevice

    data class AllManaged(override val device: ExportDevice) : ActiveOutputExportRequest
    data class Product(val productId: String, override val device: ExportDevice) : ActiveOutputExportRequest
    data class ManagedProfile(
        val productId: String,
        val profileId: String,
        override val device: ExportDevice,
    ) : ActiveOutputExportRequest
    data class SavedEq(val entryId: String, override val device: ExportDevice) : ActiveOutputExportRequest
    data class GeneralEq(val presetId: String, override val device: ExportDevice) : ActiveOutputExportRequest
    data class GeneralEqBatch(val presetIds: Set<String>, override val device: ExportDevice) : ActiveOutputExportRequest
}

private fun ew300OperationSignature(trace: Ew300OperationTrace): String =
    listOf(trace.operationId, trace.outcome, trace.stateKnown, trace.finalReadbackMatched)
        .joinToString("|")

private data class DeviceContextSummary(
    val title: String,
    val status: String,
    val isCurrent: Boolean,
    val requiresChoice: Boolean = false,
)

private fun EqLibraryUiState.deviceContextSummary(): DeviceContextSummary? {
    val supported = dacRecognitionState.recognizedDeviceIds
        .filter { it == DacDeviceId.TRN_BLACK_PEARL || it == DacDeviceId.FIIO_JA11 || it == DacDeviceId.SIMGOT_EW300 }
        .sortedBy(DacDeviceId::ordinal)
    if (supported.isEmpty()) return null
    if (supported.size > 1) {
        return DeviceContextSummary(
            title = "Supported DACs",
            status = "${supported.size} devices recognized this app session · Choose in My DAC",
            isCurrent = false,
            requiresChoice = true,
        )
    }

    val device = supported.single()
    val title = when (device) {
        DacDeviceId.TRN_BLACK_PEARL -> "TRN Black Pearl"
        DacDeviceId.FIIO_JA11 -> "FiiO JA11"
        DacDeviceId.SIMGOT_EW300 -> "SIMGOT EW300 DSP"
        DacDeviceId.JCALLY_JM12_STOCK -> return null
    }
    val connection = when (device) {
        DacDeviceId.TRN_BLACK_PEARL -> blackPearlConnectionState
        DacDeviceId.FIIO_JA11 -> fiioJa11ConnectionState
        DacDeviceId.SIMGOT_EW300 -> ew300ConnectionState
        DacDeviceId.JCALLY_JM12_STOCK -> return null
    }
    val hardwareEq = when (device) {
        DacDeviceId.TRN_BLACK_PEARL -> blackPearlHardwareEqState
        DacDeviceId.FIIO_JA11 -> fiioJa11HardwareEqState
        DacDeviceId.SIMGOT_EW300 -> ew300HardwareEqState
        DacDeviceId.JCALLY_JM12_STOCK -> return null
    }
    val connected = when (connection) {
        BlackPearlConnectionState.Connected, Kt02h20ConnectionState.Connected -> true
        else -> false
    }
    val status = when (connection) {
        BlackPearlConnectionState.Connected, Kt02h20ConnectionState.Connected -> when {
            hardwareEq.freshness == DacStateFreshness.CURRENT -> "Connected · State current"
            hardwareEq.isReading -> "Connected · Reading device state"
            hardwareEq.bundle != null -> "Connected · Last read is stale"
            else -> "Connected · Waiting for first device read"
        }
        BlackPearlConnectionState.Connecting, Kt02h20ConnectionState.Connecting ->
            "Connecting · No settings have changed"
        is Kt02h20ConnectionState.PermissionRequired -> "Permission needed · Allow USB access in My DAC"
        is BlackPearlConnectionState.Error, is Kt02h20ConnectionState.Error ->
            "Connection issue · Open My DAC to review"
        else -> if (device in dacRecognitionState.presentDeviceIds) {
            "Device detected · Open My DAC to connect"
        } else if (hardwareEq.bundle != null) {
            "Disconnected · Last read is historical"
        } else {
            "Disconnected · No current device state"
        }
    }
    return DeviceContextSummary(
        title = title,
        status = status,
        isCurrent = connected && hardwareEq.freshness == DacStateFreshness.CURRENT,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqLibraryApp(
    state: EqLibraryUiState,
    actions: EqLibraryActions,
    initialMyDacOpenDeviceId: DacDeviceId? = null,
) {
    val appPreferences = state.appPreferences
    val catalogState = state.catalogState
    val managedHeadphones = state.managedHeadphones
    val savedEqs = state.savedEqs
    val savedGeneralEqs = state.savedGeneralEqs
    val exportCurrentness = state.exportCurrentness
    val blackPearlConnectionState = state.blackPearlConnectionState
    val fiioJa11ConnectionState = state.fiioJa11ConnectionState
    val ew300ConnectionState = state.ew300ConnectionState
    val jcallyJm12ConnectionState = state.jcallyJm12ConnectionState

    val onConnectDacForMyDac = actions.onConnectDacForMyDac
    val onOpenBlackPearlEditor = actions.onOpenBlackPearlEditor
    val onBackMyDacEditor = actions.onBackMyDacEditor
    val onCloseMyDacEditor = actions.onCloseMyDacEditor
    val onSelectBlackPearlEditorBand = actions.onSelectBlackPearlEditorBand
    val onShowBlackPearlEditorAllBands = actions.onShowBlackPearlEditorAllBands
    val onShowBlackPearlEditorReview = actions.onShowBlackPearlEditorReview
    val onUpdateBlackPearlEditorBand = actions.onUpdateBlackPearlEditorBand
    val onUseSafeBlackPearlEditorGain = actions.onUseSafeBlackPearlEditorGain
    val onResetBlackPearlEditorLocalEdits = actions.onResetBlackPearlEditorLocalEdits
    val onApplyBlackPearlEditor = actions.onApplyBlackPearlEditor
    val onOpenFiioJa11Editor = actions.onOpenFiioJa11Editor
    val onBackFiioJa11Editor = actions.onBackFiioJa11Editor
    val onCloseFiioJa11Editor = actions.onCloseFiioJa11Editor
    val onSelectFiioJa11EditorBand = actions.onSelectFiioJa11EditorBand
    val onShowFiioJa11EditorAllBands = actions.onShowFiioJa11EditorAllBands
    val onShowFiioJa11EditorReview = actions.onShowFiioJa11EditorReview
    val onUpdateFiioJa11EditorBand = actions.onUpdateFiioJa11EditorBand
    val onUseSafeFiioJa11EditorGain = actions.onUseSafeFiioJa11EditorGain
    val onResetFiioJa11EditorLocalEdits = actions.onResetFiioJa11EditorLocalEdits
    val onApplyFiioJa11Editor = actions.onApplyFiioJa11Editor
    val onOpenEw300Editor = actions.onOpenEw300Editor
    val onBackEw300Editor = actions.onBackEw300Editor
    val onCloseEw300Editor = actions.onCloseEw300Editor
    val onSelectEw300EditorBand = actions.onSelectEw300EditorBand
    val onShowEw300EditorAllBands = actions.onShowEw300EditorAllBands
    val onShowEw300EditorReview = actions.onShowEw300EditorReview
    val onUpdateEw300EditorBand = actions.onUpdateEw300EditorBand
    val onUseSafeEw300EditorGain = actions.onUseSafeEw300EditorGain
    val onResetEw300EditorLocalEdits = actions.onResetEw300EditorLocalEdits
    val onApplyEw300Editor = actions.onApplyEw300Editor
    val onCaptureBlackPearlDacEq = actions.onCaptureBlackPearlDacEq
    val onCaptureEw300DacEq = actions.onCaptureEw300DacEq
    val onFlashBlackPearlFromMyDac = actions.onFlashBlackPearlFromMyDac
    val onResetBlackPearlFromMyDac = actions.onResetBlackPearlFromMyDac
    val onReadBlackPearlQualificationControls = actions.onReadBlackPearlQualificationControls
    val onSetBlackPearlDeviceControl = actions.onSetBlackPearlDeviceControl
    val onReadFiioJa11DeviceControls = actions.onReadFiioJa11DeviceControls
    val onSetFiioJa11OutputVolume = actions.onSetFiioJa11OutputVolume
    val onSetFiioJa11EqProgram = actions.onSetFiioJa11EqProgram
    val onSetFiioJa11HeadsetControl = actions.onSetFiioJa11HeadsetControl
    val onSetFiioJa11UacMode = actions.onSetFiioJa11UacMode
    val onFlashFiioJa11FromMyDac = actions.onFlashFiioJa11FromMyDac
    val onResetFiioJa11FromMyDac = actions.onResetFiioJa11FromMyDac
    val onFlashEw300FromMyDac = actions.onFlashEw300FromMyDac
    val onResetEw300FromMyDac = actions.onResetEw300FromMyDac
    val onRestoreEw300Baseline = actions.onRestoreEw300Baseline
    val onRunEw300CapabilityBatch = actions.onRunEw300CapabilityBatch
    val onAdvanceEw300PersistenceQualification = actions.onAdvanceEw300PersistenceQualification
    val onSetEw300PlaybackGain = actions.onSetEw300PlaybackGain
    val onConnectBlackPearl = actions.onConnectBlackPearl
    val onResetBlackPearl = actions.onResetBlackPearl
    val onConnectFiioJa11 = actions.onConnectFiioJa11
    val onResetFiioJa11 = actions.onResetFiioJa11
    val onConnectEw300 = actions.onConnectEw300
    val onResetEw300 = actions.onResetEw300
    val onConnectJcallyJm12 = actions.onConnectJcallyJm12
    val onResetJcallyJm12 = actions.onResetJcallyJm12
    val onFlashManagedProfile = actions.onFlashManagedProfile
    val onFlashSavedEq = actions.onFlashSavedEq
    val onFlashGeneralEq = actions.onFlashGeneralEq
    val onRefreshCatalog = actions.onRefreshCatalog
    val onLoadManagedHeadphone = actions.onLoadManagedHeadphone
    val onSaveSelection = actions.onSaveSelection
    val onRemoveHeadphone = actions.onRemoveHeadphone
    val onRemoveManagedProfile = actions.onRemoveManagedProfile
    val onRemoveManagedHeadphone = actions.onRemoveManagedHeadphone
    val onDeleteSavedFilesForProfiles = actions.onDeleteSavedFilesForProfiles
    val onDeleteSavedFilesForProduct = actions.onDeleteSavedFilesForProduct
    val onMarkReviewed = actions.onMarkReviewed
    val onToggleFavorite = actions.onToggleFavorite
    val onSaveGeneralPresets = actions.onSaveGeneralPresets
    val onHideCanonicalProfiles = actions.onHideCanonicalProfiles
    val onUnhideCanonicalProfiles = actions.onUnhideCanonicalProfiles
    val onImportPersonal = actions.onImportPersonal
    val onDeleteSavedEq = actions.onDeleteSavedEq
    val onRemoveGeneralEq = actions.onRemoveGeneralEq
    val onPersistExportTree = actions.onPersistExportTree
    val onExportSelected = actions.onExportSelected
    val onExportProduct = actions.onExportProduct
    val onExportManagedProfile = actions.onExportManagedProfile
    val onExportSavedEq = actions.onExportSavedEq
    val onExportGeneralEq = actions.onExportGeneralEq
    val onExportGeneralEqs = actions.onExportGeneralEqs
    val onCheckForUpdates = actions.onCheckForUpdates
    val onDismissUpdate = actions.onDismissUpdate
    val onDismissPostUpdate = actions.onDismissPostUpdate
    val onOpenUrl = actions.onOpenUrl
    val onThemeModeChange = actions.onThemeModeChange
    val onOutputBehaviorChange = actions.onOutputBehaviorChange
    val onExportTargetChange = actions.onExportTargetChange
    val onSessionActiveExportTargetChange = actions.onSessionActiveExportTargetChange
    val onActiveExportTargetChange = actions.onActiveExportTargetChange
    val onDirectBlackPearlFlashEnabledChange = actions.onDirectBlackPearlFlashEnabledChange
    val onDirectFiioJa11FlashEnabledChange = actions.onDirectFiioJa11FlashEnabledChange
    val onDirectEw300FlashEnabledChange = actions.onDirectEw300FlashEnabledChange

    var selectedDestinationName by rememberSaveable { mutableStateOf(EqLibraryDestination.MyEqs.name) }
    var myDacWorkspaceOpen by rememberSaveable { mutableStateOf(false) }
    var selectedManagedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExportRequestState by rememberSaveable {
        mutableStateOf<List<String>?>(null)
    }
    var pendingExportReviewState by rememberSaveable {
        mutableStateOf<List<String>?>(null)
    }
    var whatsNewVersion by rememberSaveable { mutableStateOf<String?>(null) }
    var whatsNewNotes by rememberSaveable { mutableStateOf("") }
    var pendingInitialMyDacOpenDeviceName by rememberSaveable {
        mutableStateOf(initialMyDacOpenDeviceId?.name)
    }

    val rootDestinations = remember { eqLibraryRootDestinations() }
    val selectedRootDestination = restoreEqLibraryDestination(selectedDestinationName, rootDestinations)
    val selectedDestination = if (
        myDacWorkspaceOpen && state.dacRecognitionState.hasRecognizedDevice
    ) {
        EqLibraryDestination.MyDac
    } else {
        selectedRootDestination
    }
    val activeOutput = appPreferences.exportTargets.activeTarget
    val enabledOutputs = remember(appPreferences.exportTargets) {
        ExportDevice.selectableOutputs.filter(appPreferences.exportTargets::isSelected)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var flashFeedback by remember { mutableStateOf<FlashFeedback?>(null) }
    var pendingFlashResultMessage by remember { mutableStateOf<String?>(null) }
    var lastBlackPearlFlashSequence by remember {
        mutableStateOf(state.blackPearlFlashOutcome?.sequence)
    }
    LaunchedEffect(state.blackPearlFlashOutcome) {
        state.blackPearlFlashOutcome?.let { outcome ->
            if (outcome.sequence != lastBlackPearlFlashSequence) {
                lastBlackPearlFlashSequence = outcome.sequence
                flashFeedback = blackPearlFlashFeedback(outcome.result)
            }
        }
    }
    LaunchedEffect(
        flashFeedback?.deviceLabel,
        flashFeedback?.phase,
        flashFeedback?.detail,
        flashFeedback?.verified,
    ) {
        val feedback = flashFeedback ?: return@LaunchedEffect
        if (feedback.phase.expiresAutomatically()) {
            delay(8_000L)
            if (flashFeedback == feedback) flashFeedback = null
        }
    }
    val exportFolderPermissionFailedMessage = stringResource(R.string.export_folder_permission_failed)
    val favoriteProfileIds = remember(savedEqs) {
        savedEqs.asSequence()
            .filter { it.kind == SavedEqKind.Favorite }
            .mapNotNull { it.sourceProfileId }
            .toSet()
    }
    val savedGeneralPresetIds = remember(savedGeneralEqs) {
        savedGeneralEqs.mapTo(mutableSetOf(), SavedGeneralEqRecord::presetId)
    }
    val catalogBusy = catalogState is CatalogState.Loading ||
        (catalogState as? CatalogState.Ready)?.isRefreshing == true
    val managedHeadphonesForUi = remember(managedHeadphones, appPreferences.hiddenCanonicalProfileIds) {
        managedHeadphones.map { headphone ->
            headphone.withHiddenReviewPromptsSuppressed(appPreferences.hiddenCanonicalProfileIds)
        }
    }
    val selectedManagedHeadphone = selectedManagedProductId?.let { productId ->
        managedHeadphonesForUi.firstOrNull { it.productId == productId }
    }
    val deviceContextSummary = state.deviceContextSummary()

    LaunchedEffect(
        pendingInitialMyDacOpenDeviceName,
        state.dacRecognitionState.recognizedDeviceIds,
    ) {
        val requestedDeviceId = pendingInitialMyDacOpenDeviceName?.let { name ->
            runCatching { DacDeviceId.valueOf(name) }.getOrNull()
        }
        if (
            requestedDeviceId != null &&
            requestedDeviceId in state.dacRecognitionState.recognizedDeviceIds
        ) {
            onConnectDacForMyDac(requestedDeviceId)
            selectedManagedProductId = null
            myDacWorkspaceOpen = true
            pendingInitialMyDacOpenDeviceName = null
        }
    }

    LaunchedEffect(rootDestinations, selectedDestinationName) {
        val restored = restoreEqLibraryDestination(selectedDestinationName, rootDestinations)
        if (restored.name != selectedDestinationName) {
            selectedDestinationName = restored.name
        }
    }

    LaunchedEffect(myDacWorkspaceOpen, state.dacRecognitionState.hasRecognizedDevice) {
        if (myDacWorkspaceOpen && !state.dacRecognitionState.hasRecognizedDevice) {
            onCloseMyDacEditor()
            myDacWorkspaceOpen = false
        }
    }

    LaunchedEffect(selectedManagedProductId, selectedManagedHeadphone) {
        if (selectedManagedProductId != null && selectedManagedHeadphone == null) {
            selectedManagedProductId = null
        }
    }

    val latestVersion = appPreferences.updates.latestVersion
    val updateAvailable = latestVersion != null &&
        SemVer.parse(latestVersion)?.let { latest ->
            SemVer.parse(BuildConfig.VERSION_NAME)?.let { installed -> latest > installed }
        } == true
    val updateBannerVersion = latestVersion?.takeIf {
        updateAvailable && appPreferences.updates.dismissedVersion != it
    }
    val postUpdateVersion = appPreferences.updates.postUpdateVersionToShow
        ?.takeIf { it == BuildConfig.VERSION_NAME }

    fun beginFlash(device: ExportDevice) {
        pendingFlashResultMessage = null
        flashFeedback = FlashFeedback(
            deviceLabel = flashDeviceLabel(device),
            phase = FlashFeedbackPhase.STARTING,
        )
    }

    fun markFlashResultUncertain(device: ExportDevice, detail: String? = null) {
        flashFeedback = FlashFeedback(
            deviceLabel = flashDeviceLabel(device),
            phase = FlashFeedbackPhase.UNCERTAIN,
            detail = detail ?: "The Flash operation finished without an authoritative verified result.",
        )
    }

    suspend fun flashSuspendWithFeedback(device: ExportDevice, action: suspend () -> String): String {
        beginFlash(device)
        return try {
            val message = action()
            if (
                flashFeedback?.phase == FlashFeedbackPhase.STARTING ||
                flashFeedback?.phase == FlashFeedbackPhase.VERIFYING
            ) {
                markFlashResultUncertain(device)
            }
            pendingFlashResultMessage = message
            message
        } catch (cancellation: CancellationException) {
            flashFeedback = null
            throw cancellation
        } catch (_: Exception) {
            val message = "Flash did not finish and its final state is uncertain."
            if (
                flashFeedback?.phase == FlashFeedbackPhase.STARTING ||
                flashFeedback?.phase == FlashFeedbackPhase.VERIFYING
            ) {
                markFlashResultUncertain(device, message)
            }
            pendingFlashResultMessage = message
            message
        }
    }

    fun flashImmediateWithFeedback(device: ExportDevice, action: () -> Unit) {
        beginFlash(device)
        try {
            action()
        } catch (_: Exception) {
            markFlashResultUncertain(device, "Flash did not start and its final state is uncertain.")
        }
    }

    fun showMessage(message: String) {
        if (message == hardwareFlashStartedMessage(activeOutput)) {
            beginFlash(activeOutput)
            return
        }
        if (message == pendingFlashResultMessage) {
            pendingFlashResultMessage = null
            return
        }
        pendingFlashResultMessage = null
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    suspend fun showDeviceOperation(
        message: String,
        duration: SnackbarDuration,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = duration,
        )
        if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
    }

    fun showOperationStatus(message: String, persistent: Boolean) {
        scope.launch {
            showDeviceOperation(
                message = message,
                duration = if (persistent) SnackbarDuration.Indefinite else SnackbarDuration.Short,
            )
        }
    }

    fun ew300UnverifiedOperationMessage(trace: Ew300OperationTrace): String {
        val operation = "EW300 ${trace.operation.lowercase()}"
        val reason = trace.failureReason?.takeIf { it.isNotBlank() }
        return when (trace.outcome) {
            Ew300OperationOutcome.NOT_SUITABLE,
            Ew300OperationOutcome.DEVICE_UNAVAILABLE,
            Ew300OperationOutcome.INVALID_PLAN,
            Ew300OperationOutcome.STALE_BASELINE,
            Ew300OperationOutcome.CONFIRMATION_REQUIRED,
            Ew300OperationOutcome.NO_BASELINE,
            -> "$operation was not applied${reason?.let { ": $it" } ?: "."}"
            else -> "$operation did not finish with a verified state. Do not retry this operation; stop and reconnect or refresh before any later write${reason?.let { ": $it" } ?: "."}"
        }
    }

    var lastEw300StartedOperationId by remember {
        mutableStateOf<String?>(null)
    }
    var lastEw300CompletedOperationSignature by remember {
        mutableStateOf(
            when (val status = state.ew300OperationStatus) {
                is Ew300OperationStatus.Running -> null
                is Ew300OperationStatus.Completed -> ew300OperationSignature(status.trace)
                Ew300OperationStatus.Idle -> null
            },
        )
    }
    LaunchedEffect(state.ew300OperationStatus) {
        when (val status = state.ew300OperationStatus) {
            is Ew300OperationStatus.Running -> {
                if (status.operationId == lastEw300StartedOperationId) return@LaunchedEffect
                lastEw300StartedOperationId = status.operationId
                if (status.operation == "FLASH") {
                    flashFeedback = FlashFeedback(
                        deviceLabel = flashDeviceLabel(ExportDevice.SIMGOT_EW300),
                        phase = FlashFeedbackPhase.VERIFYING,
                    )
                } else if (status.operation == "RESET") {
                    flashFeedback = null
                    pendingFlashResultMessage = null
                }
            }
            is Ew300OperationStatus.Completed -> {
                val trace = status.trace
                val signature = ew300OperationSignature(trace)
                if (signature == lastEw300CompletedOperationSignature) return@LaunchedEffect
                lastEw300CompletedOperationSignature = signature
                when {
                    trace.operation == "FLASH" && trace.stateKnown && trace.outcome == Ew300OperationOutcome.SUCCESS && trace.finalReadbackMatched -> {
                        val reconnectObserved = trace.replacementObserved && trace.replacementIdentityMatched
                        flashFeedback = FlashFeedback(
                            deviceLabel = flashDeviceLabel(ExportDevice.SIMGOT_EW300),
                            phase = FlashFeedbackPhase.COMPLETED,
                            detail = if (reconnectObserved) {
                                "The DAC reconnected and the replacement session was verified."
                            } else {
                                "Final hardware readback matched."
                            },
                            verified = true,
                        )
                    }
                    trace.operation == "FLASH" && !trace.stateKnown -> flashFeedback = FlashFeedback(
                        deviceLabel = flashDeviceLabel(ExportDevice.SIMGOT_EW300),
                        phase = FlashFeedbackPhase.FAILED,
                        detail = ew300UnverifiedOperationMessage(trace),
                    )
                    trace.operation == "FLASH" -> flashFeedback = FlashFeedback(
                        deviceLabel = flashDeviceLabel(ExportDevice.SIMGOT_EW300),
                        phase = FlashFeedbackPhase.FAILED,
                        detail = "EW300 Flash stopped before verified persistence (${trace.outcome}).",
                    )
                }
            }
            Ew300OperationStatus.Idle -> Unit
        }
    }

    var fiioOperationStatusInitialized by remember { mutableStateOf(false) }
    var lastFiioOperationStatusSignature by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.fiioJa11OperationStatus) {
        val statusSignature = when (val status = state.fiioJa11OperationStatus) {
            FiioJa11OperationStatus.Idle -> "IDLE"
            is FiioJa11OperationStatus.Running -> "RUNNING:${status.operationId}"
            is FiioJa11OperationStatus.Completed -> "COMPLETED:${status.trace.operationId}:${status.trace.outcome}:${status.trace.stateKnown}"
        }
        if (!fiioOperationStatusInitialized) {
            fiioOperationStatusInitialized = true
            lastFiioOperationStatusSignature = statusSignature
            return@LaunchedEffect
        }
        if (statusSignature == lastFiioOperationStatusSignature) return@LaunchedEffect
        lastFiioOperationStatusSignature = statusSignature
        when (val status = state.fiioJa11OperationStatus) {
            is FiioJa11OperationStatus.Running -> when (status.operation) {
                "FLASH" -> flashFeedback = FlashFeedback(
                    deviceLabel = flashDeviceLabel(ExportDevice.FIIO_JA11),
                    phase = FlashFeedbackPhase.VERIFYING,
                )
                "RESET", "EDITOR_APPLY" -> {
                    flashFeedback = null
                    pendingFlashResultMessage = null
                }
            }
            is FiioJa11OperationStatus.Completed -> {
                val trace = status.trace
                when (trace.operation) {
                    "FLASH" -> {
                        val presentation = fiioJa11OperationStatusPresentation(trace)
                        flashFeedback = if (presentation.verified) {
                            FlashFeedback(
                                deviceLabel = flashDeviceLabel(ExportDevice.FIIO_JA11),
                                phase = FlashFeedbackPhase.COMPLETED,
                                detail = "Final hardware readback matched.",
                                verified = true,
                            )
                        } else {
                            FlashFeedback(
                                deviceLabel = flashDeviceLabel(ExportDevice.FIIO_JA11),
                                phase = FlashFeedbackPhase.FAILED,
                                detail = presentation.message,
                            )
                        }
                    }
                    "EDITOR_APPLY" -> {
                        flashFeedback = null
                        pendingFlashResultMessage = null
                    }
                }
            }
            FiioJa11OperationStatus.Idle -> Unit
        }
    }

    var lastBlackPearlOperationSignature by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(
        state.blackPearlQualificationState.isWriting,
        state.blackPearlQualificationState.activeWriteControlId,
        state.blackPearlQualificationState.lastVerifiedWriteControlId,
        state.blackPearlQualificationState.error,
    ) {
        val operationState = state.blackPearlQualificationState
        val signature = deviceOperationStateSignature(
            isWriting = operationState.isWriting,
            activeWriteControlId = operationState.activeWriteControlId,
            pendingVerificationControlId = null,
            lastVerifiedWriteControlId = operationState.lastVerifiedWriteControlId,
            error = operationState.error,
        )
        val feedback = deviceOperationFeedback(
            isWriting = operationState.isWriting,
            activeWriteControlId = operationState.activeWriteControlId,
            pendingVerificationControlId = null,
            lastVerifiedWriteControlId = operationState.lastVerifiedWriteControlId,
            error = operationState.error,
        )
        val previousSignature = lastBlackPearlOperationSignature
        lastBlackPearlOperationSignature = signature
        if (previousSignature == null || previousSignature == signature) return@LaunchedEffect

        when (feedback?.phase) {
            DeviceOperationPhase.APPLYING -> feedback.controlId?.let { controlId ->
                showDeviceOperation(
                    message = "Applying ${deviceOperationControlLabel(controlId)}…",
                    duration = SnackbarDuration.Indefinite,
                )
            }
            DeviceOperationPhase.RECONNECTING -> feedback.controlId?.let { controlId ->
                showDeviceOperation(
                    message = "Applying ${deviceOperationControlLabel(controlId)}… Reconnect the DAC to verify.",
                    duration = SnackbarDuration.Indefinite,
                )
            }
            DeviceOperationPhase.VERIFIED -> feedback.controlId?.let { controlId ->
                showDeviceOperation(
                    message = "✓ ${deviceOperationControlLabel(controlId)} updated",
                    duration = SnackbarDuration.Short,
                )
            }
            DeviceOperationPhase.FAILED -> showDeviceOperation(
                message = "Couldn’t update the DAC: ${feedback.error}",
                duration = SnackbarDuration.Indefinite,
                actionLabel = "Refresh",
                onAction = onReadBlackPearlQualificationControls,
            )
            null -> Unit
        }
    }

    var lastFiioJa11OperationSignature by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(
        state.fiioJa11DeviceState.isWriting,
        state.fiioJa11DeviceState.activeWriteControlId,
        state.fiioJa11DeviceState.pendingRestartWrite,
        state.fiioJa11DeviceState.lastVerifiedWriteControlId,
        state.fiioJa11DeviceState.error,
    ) {
        val operationState = state.fiioJa11DeviceState
        val signature = deviceOperationStateSignature(
            isWriting = operationState.isWriting,
            activeWriteControlId = operationState.activeWriteControlId,
            pendingVerificationControlId = operationState.pendingRestartWrite?.controlId,
            lastVerifiedWriteControlId = operationState.lastVerifiedWriteControlId,
            error = operationState.error,
        )
        val feedback = deviceOperationFeedback(
            isWriting = operationState.isWriting,
            activeWriteControlId = operationState.activeWriteControlId,
            pendingVerificationControlId = operationState.pendingRestartWrite?.controlId,
            lastVerifiedWriteControlId = operationState.lastVerifiedWriteControlId,
            error = operationState.error,
        )
        val previousSignature = lastFiioJa11OperationSignature
        lastFiioJa11OperationSignature = signature
        if (previousSignature == null || previousSignature == signature) return@LaunchedEffect

        when (feedback?.phase) {
            DeviceOperationPhase.APPLYING -> feedback.controlId?.let { controlId ->
                showDeviceOperation(
                    message = "Applying ${deviceOperationControlLabel(controlId)}…",
                    duration = SnackbarDuration.Indefinite,
                )
            }
            DeviceOperationPhase.RECONNECTING -> feedback.controlId?.let { controlId ->
                showDeviceOperation(
                    message = "Applying ${deviceOperationControlLabel(controlId)}… Reconnect the DAC to verify.",
                    duration = SnackbarDuration.Indefinite,
                )
            }
            DeviceOperationPhase.VERIFIED -> feedback.controlId?.let { controlId ->
                showDeviceOperation(
                    message = "✓ ${deviceOperationControlLabel(controlId)} updated",
                    duration = SnackbarDuration.Short,
                )
            }
            DeviceOperationPhase.FAILED -> showDeviceOperation(
                message = "Couldn’t update the DAC: ${feedback.error}",
                duration = SnackbarDuration.Indefinite,
                actionLabel = "Refresh",
                onAction = onReadFiioJa11DeviceControls,
            )
            null -> Unit
        }
    }

    fun showWhatsNew(version: String?, notes: String?) {
        val actualVersion = version ?: return
        whatsNewVersion = actualVersion
        whatsNewNotes = notes.orEmpty()
    }

    suspend fun executeExport(uri: Uri, request: ActiveOutputExportRequest?): String? {
        if (request != null && !request.device.supportsFileExport) return null
        return when (request) {
            is ActiveOutputExportRequest.AllManaged -> onExportSelected(uri, request.device)
            is ActiveOutputExportRequest.Product -> onExportProduct(uri, request.productId, request.device)
            is ActiveOutputExportRequest.ManagedProfile -> onExportManagedProfile(
                uri, request.productId, request.profileId, request.device,
            )
            is ActiveOutputExportRequest.SavedEq -> onExportSavedEq(uri, request.entryId, request.device)
            is ActiveOutputExportRequest.GeneralEq -> onExportGeneralEq(uri, request.presetId, request.device)
            is ActiveOutputExportRequest.GeneralEqBatch -> onExportGeneralEqs(uri, request.presetIds, request.device)
            null -> null
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val request = restoreActiveOutputExportRequest(pendingExportRequestState)
        pendingExportRequestState = null
        if (uri == null) {
            scope.launch {
                snackbarHostState.showSnackbar("No folder selected. Nothing was exported. Choose an export again to continue.")
            }
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            if (!onPersistExportTree(uri)) {
                snackbarHostState.showSnackbar(exportFolderPermissionFailedMessage)
            } else {
                executeExport(uri, request)?.let { message -> snackbarHostState.showSnackbar(message) }
            }
        }
    }

    val chooseExportFolder: (ActiveOutputExportRequest?) -> Unit = { request ->
        pendingExportRequestState = request?.toSaveableState()
        folderPicker.launch(appPreferences.exportTreeUri?.let(Uri::parse))
    }

    val runExportRequest: (ActiveOutputExportRequest) -> Unit = { request ->
        if (!request.device.supportsFileExport) {
            Unit
        } else {
            val storedUri = appPreferences.exportTreeUri?.let(Uri::parse)
            if (storedUri == null) {
                chooseExportFolder(request)
            } else {
                scope.launch {
                    executeExport(storedUri, request)?.let { message -> snackbarHostState.showSnackbar(message) }
                }
            }
        }
    }

    val openExportReview: (ActiveOutputExportRequest) -> Unit = { request ->
        if (request.device.supportsFileExport) {
            pendingExportReviewState = request.toSaveableState()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "${request.device.displayName} has no verified import file. Connect it and use My DAC for supported hardware actions, or select UAPP to export XML and import it there.",
                )
            }
        }
    }
    val requestExportAll = { openExportReview(ActiveOutputExportRequest.AllManaged(activeOutput)) }
    val requestExportProduct: (String) -> Unit = { productId ->
        openExportReview(ActiveOutputExportRequest.Product(productId, activeOutput))
    }
    val requestExportManagedProfile: (String, String) -> Unit = { productId, profileId ->
        openExportReview(ActiveOutputExportRequest.ManagedProfile(productId, profileId, activeOutput))
    }
    val requestExportSavedEq: (String) -> Unit = { entryId ->
        openExportReview(ActiveOutputExportRequest.SavedEq(entryId, activeOutput))
    }
    val requestExportGeneralEq: (String) -> Unit = { presetId ->
        openExportReview(ActiveOutputExportRequest.GeneralEq(presetId, activeOutput))
    }
    val requestExportGeneralEqs: (Set<String>) -> Unit = { presetIds ->
        openExportReview(ActiveOutputExportRequest.GeneralEqBatch(presetIds, activeOutput))
    }
    val requestCatalogRefresh = {
        if (!catalogBusy) {
            scope.launch { snackbarHostState.showSnackbar(onRefreshCatalog()) }
        }
    }
    val requestUpdateCheck: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar(onCheckForUpdates()) }
    }

    val handleMyDacEditorBack = {
        onBackMyDacEditor() || onBackFiioJa11Editor() || onBackEw300Editor()
    }
    val useNavigationRail = LocalConfiguration.current.screenWidthDp >= 600
    val selectRootDestination: (EqLibraryDestination) -> Unit = { destination ->
        if (selectedDestination == EqLibraryDestination.MyDac) onCloseMyDacEditor()
        myDacWorkspaceOpen = false
        selectedManagedProductId = null
        selectedDestinationName = destination.name
    }
    BackHandler(
        enabled = hasDestinationBackHandler(selectedDestination),
    ) {
        if (handleMyDacEditorBack()) return@BackHandler
        onCloseMyDacEditor()
        myDacWorkspaceOpen = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedDestination.labelResId)) },
                navigationIcon = {
                    if (selectedDestination == EqLibraryDestination.MyDac) {
                        IconButton(
                            onClick = {
                                if (!handleMyDacEditorBack()) {
                                    onCloseMyDacEditor()
                                    myDacWorkspaceOpen = false
                                }
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (selectedDestination == EqLibraryDestination.EqLibrary) {
                        IconButton(onClick = requestCatalogRefresh, enabled = !catalogBusy) {
                            if (catalogBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.refresh_eq_library_content_description),
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!useNavigationRail) {
                NavigationBar {
                    rootDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = !myDacWorkspaceOpen && selectedRootDestination == destination,
                            onClick = { selectRootDestination(destination) },
                            icon = { RootDestinationIcon(destination) },
                            label = { Text(stringResource(destination.labelResId)) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Row(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (useNavigationRail) {
                NavigationRail(modifier = Modifier.fillMaxHeight()) {
                    rootDestinations.forEach { destination ->
                        NavigationRailItem(
                            selected = !myDacWorkspaceOpen && selectedRootDestination == destination,
                            onClick = { selectRootDestination(destination) },
                            icon = { RootDestinationIcon(destination) },
                            label = { Text(stringResource(destination.labelResId)) },
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 960.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                ) {
                if (selectedDestination != EqLibraryDestination.MyDac) {
                    deviceContextSummary?.let { context ->
                        if (context.requiresChoice) {
                            ContextualDeviceChoiceSurface(
                                title = context.title,
                                status = context.status,
                                onOpen = { myDacWorkspaceOpen = true },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        } else {
                            ConnectedDeviceSurface(
                                deviceName = context.title,
                                status = context.status,
                                isCurrent = context.isCurrent,
                                onOpen = { myDacWorkspaceOpen = true },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                if (
                    selectedDestination == EqLibraryDestination.MyEqs ||
                    selectedDestination == EqLibraryDestination.EqLibrary
                ) {
                    TargetContextSelector(
                        activeTarget = activeOutput,
                        enabledTargets = enabledOutputs,
                        onTargetChange = onSessionActiveExportTargetChange,
                    )
                    when {
                        updateBannerVersion != null -> UpdateAvailableBanner(
                            version = updateBannerVersion,
                            onWhatsNew = { showWhatsNew(updateBannerVersion, appPreferences.updates.releaseNotes) },
                            onGetUpdate = { appPreferences.updates.releaseUrl?.let(onOpenUrl) },
                            onDismiss = { scope.launch { onDismissUpdate(updateBannerVersion) } },
                        )
                        postUpdateVersion != null -> PostUpdateBanner(
                            version = postUpdateVersion,
                            onWhatsNew = { showWhatsNew(postUpdateVersion, appPreferences.updates.releaseNotes) },
                            onDismiss = { scope.launch { onDismissPostUpdate() } },
                        )
                    }
                }

                if (selectedDestination != EqLibraryDestination.Settings) {
                    flashFeedback?.let { feedback ->
                        FlashFeedbackBanner(
                            feedback = feedback,
                            onDismiss = { flashFeedback = null },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    when (selectedDestination) {
                        EqLibraryDestination.MyEqs -> {
                            if (selectedManagedHeadphone != null) {
                                ManagedHeadphoneDetailScreen(
                                    headphone = selectedManagedHeadphone,
                                    catalogState = catalogState,
                                    profileVisibility = appPreferences.profileVisibility,
                                    exportTargets = appPreferences.exportTargets,
                                    exportCurrentness = exportCurrentness,
                                    favoriteProfileIds = favoriteProfileIds,
                                    directBlackPearlFlashEnabled = appPreferences.directBlackPearlFlashEnabled,
                                    blackPearlConnectionState = blackPearlConnectionState,
                                    onConnectBlackPearl = onConnectBlackPearl,
                                    directFiioJa11FlashEnabled = appPreferences.directFiioJa11FlashEnabled,
                                    fiioJa11ConnectionState = fiioJa11ConnectionState,
                                    onConnectFiioJa11 = onConnectFiioJa11,
                                    directEw300FlashEnabled = appPreferences.directEw300FlashEnabled,
                                    ew300ConnectionState = ew300ConnectionState,
                                    onConnectEw300 = onConnectEw300,
                                    directJcallyJm12FlashEnabled = appPreferences.directJcallyJm12FlashEnabled,
                                    jcallyJm12ConnectionState = jcallyJm12ConnectionState,
                                    onConnectJcallyJm12 = onConnectJcallyJm12,
                                    onFlashManagedProfile = { profileId ->
                                        flashSuspendWithFeedback(activeOutput) {
                                            onFlashManagedProfile(selectedManagedHeadphone.productId, profileId)
                                        }
                                    },
                                    onToggleFavorite = onToggleFavorite,
                                    onHideCanonicalProfile = { canonicalProfileId ->
                                        onHideCanonicalProfiles(setOf(canonicalProfileId))
                                    },
                                    onLoadManagedHeadphone = onLoadManagedHeadphone,
                                    onSaveSelection = onSaveSelection,
                                    onRemoveHeadphone = onRemoveHeadphone,
                                    onRemoveManagedProfile = onRemoveManagedProfile,
                                    onRemoveManagedHeadphone = onRemoveManagedHeadphone,
                                    onDeleteSavedFilesForProfiles = onDeleteSavedFilesForProfiles,
                                    onDeleteSavedFilesForProduct = onDeleteSavedFilesForProduct,
                                    onMarkReviewed = onMarkReviewed,
                                    onExportProduct = requestExportProduct,
                                    onExportProfile = { profileId ->
                                        requestExportManagedProfile(selectedManagedHeadphone.productId, profileId)
                                    },
                                    onMessage = ::showMessage,
                                    onOpenUrl = onOpenUrl,
                                    onBack = { selectedManagedProductId = null },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                MyEqsHomeScreen(
                                    managedHeadphones = managedHeadphonesForUi,
                                    savedEqs = savedEqs,
                                    savedGeneralEqs = savedGeneralEqs,
                                    activeOutput = activeOutput,
                                    exportCurrentness = exportCurrentness,
                                    directBlackPearlFlashEnabled = appPreferences.directBlackPearlFlashEnabled,
                                    blackPearlConnectionState = blackPearlConnectionState,
                                    directFiioJa11FlashEnabled = appPreferences.directFiioJa11FlashEnabled,
                                    fiioJa11ConnectionState = fiioJa11ConnectionState,
                                    directEw300FlashEnabled = appPreferences.directEw300FlashEnabled,
                                    ew300ConnectionState = ew300ConnectionState,
                                    directJcallyJm12FlashEnabled = appPreferences.directJcallyJm12FlashEnabled,
                                    jcallyJm12ConnectionState = jcallyJm12ConnectionState,
                                    onBrowseLibrary = {
                                        selectedManagedProductId = null
                                        myDacWorkspaceOpen = false
                                        selectedDestinationName = EqLibraryDestination.EqLibrary.name
                                    },
                                    onExportAll = requestExportAll,
                                    onOpenHeadphone = { selectedManagedProductId = it },
                                    onImportPersonal = onImportPersonal,
                                    onDeleteSavedEq = onDeleteSavedEq,
                                    onExportSavedEq = requestExportSavedEq,
                                    onFlashSavedEq = { entryId ->
                                        flashSuspendWithFeedback(activeOutput) { onFlashSavedEq(entryId) }
                                    },
                                    onRemoveGeneralEq = onRemoveGeneralEq,
                                    onExportGeneralEq = requestExportGeneralEq,
                                    onFlashGeneralEq = { presetId ->
                                        flashSuspendWithFeedback(activeOutput) { onFlashGeneralEq(presetId) }
                                    },
                                    onMessage = ::showMessage,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        EqLibraryDestination.MyDac -> MyDacRootScreen(
                            recognitionState = state.dacRecognitionState,
                            catalogState = state.catalogState,
                            blackPearlConnectionState = state.blackPearlConnectionState,
                            fiioJa11ConnectionState = state.fiioJa11ConnectionState,
                            ew300ConnectionState = state.ew300ConnectionState,
                            blackPearlHardwareEqState = state.blackPearlHardwareEqState,
                            fiioJa11HardwareEqState = state.fiioJa11HardwareEqState,
                            ew300HardwareEqState = state.ew300HardwareEqState,
                            blackPearlHardwareEqMatch = state.blackPearlHardwareEqMatch,
                            blackPearlManagedHeadphones = state.blackPearlManagedHeadphones,
                            blackPearlSavedEqs = state.blackPearlSavedEqs,
                            blackPearlSavedGeneralEqs = state.blackPearlSavedGeneralEqs,
                            blackPearlEditorState = state.blackPearlEditorState,
                            fiioJa11EditorState = state.fiioJa11EditorState,
                            ew300EditorState = state.ew300EditorState,
                            ew300OperationTrace = state.ew300OperationTrace,
                            ew300OperationStatus = state.ew300OperationStatus,
                            fiioJa11OperationTrace = state.fiioJa11OperationTrace,
                            fiioJa11OperationStatus = state.fiioJa11OperationStatus,
                            blackPearlQualificationState = state.blackPearlQualificationState,
                            fiioJa11DeviceState = state.fiioJa11DeviceState,
                            ew300PlaybackGainState = state.ew300PlaybackGainState,
                            onConnectDac = onConnectDacForMyDac,
                            onOpenBlackPearlEditor = onOpenBlackPearlEditor,
                            onCloseBlackPearlEditor = onCloseMyDacEditor,
                            onSelectBlackPearlEditorBand = onSelectBlackPearlEditorBand,
                            onShowBlackPearlEditorAllBands = onShowBlackPearlEditorAllBands,
                            onShowBlackPearlEditorReview = onShowBlackPearlEditorReview,
                            onUpdateBlackPearlEditorBand = onUpdateBlackPearlEditorBand,
                            onUseSafeBlackPearlEditorGain = onUseSafeBlackPearlEditorGain,
                            onResetBlackPearlEditorLocalEdits = onResetBlackPearlEditorLocalEdits,
                            onApplyBlackPearlEditor = onApplyBlackPearlEditor,
                            onOpenFiioJa11Editor = onOpenFiioJa11Editor,
                            onBackFiioJa11Editor = onBackFiioJa11Editor,
                            onCloseFiioJa11Editor = onCloseFiioJa11Editor,
                            onSelectFiioJa11EditorBand = onSelectFiioJa11EditorBand,
                            onShowFiioJa11EditorAllBands = onShowFiioJa11EditorAllBands,
                            onShowFiioJa11EditorReview = onShowFiioJa11EditorReview,
                            onUpdateFiioJa11EditorBand = onUpdateFiioJa11EditorBand,
                            onUseSafeFiioJa11EditorGain = onUseSafeFiioJa11EditorGain,
                            onResetFiioJa11EditorLocalEdits = onResetFiioJa11EditorLocalEdits,
                            onApplyFiioJa11Editor = onApplyFiioJa11Editor,
                            onOpenEw300Editor = onOpenEw300Editor,
                            onBackEw300Editor = onBackEw300Editor,
                            onCloseEw300Editor = onCloseEw300Editor,
                            onSelectEw300EditorBand = onSelectEw300EditorBand,
                            onShowEw300EditorAllBands = onShowEw300EditorAllBands,
                            onShowEw300EditorReview = onShowEw300EditorReview,
                            onUpdateEw300EditorBand = onUpdateEw300EditorBand,
                            onUseSafeEw300EditorGain = onUseSafeEw300EditorGain,
                            onResetEw300EditorLocalEdits = onResetEw300EditorLocalEdits,
                            onApplyEw300Editor = onApplyEw300Editor,
                            onCaptureBlackPearlDacEq = onCaptureBlackPearlDacEq,
                            onCaptureEw300DacEq = onCaptureEw300DacEq,
                            onFlashBlackPearlFromMyDac = { profile ->
                                flashSuspendWithFeedback(ExportDevice.BLACK_PEARL) {
                                    onFlashBlackPearlFromMyDac(profile)
                                }
                            },
                            onResetBlackPearlFromMyDac = onResetBlackPearlFromMyDac,
                            onReadBlackPearlQualification = onReadBlackPearlQualificationControls,
                            onSetBlackPearlDeviceControl = onSetBlackPearlDeviceControl,
                            onReadFiioJa11DeviceControls = onReadFiioJa11DeviceControls,
                            onSetFiioJa11OutputVolume = onSetFiioJa11OutputVolume,
                            onSetFiioJa11EqProgram = onSetFiioJa11EqProgram,
                            onSetFiioJa11HeadsetControl = onSetFiioJa11HeadsetControl,
                            onSetFiioJa11UacMode = onSetFiioJa11UacMode,
                            onResetFiioJa11FromMyDac = onResetFiioJa11FromMyDac,
                            onResetEw300FromMyDac = onResetEw300FromMyDac,
                            onRestoreEw300Baseline = onRestoreEw300Baseline,
                            onRunEw300CapabilityBatch = onRunEw300CapabilityBatch,
                            onAdvanceEw300PersistenceQualification = onAdvanceEw300PersistenceQualification,
                            onSetEw300PlaybackGain = onSetEw300PlaybackGain,
                            onMessage = ::showMessage,
                            onOperationStatus = ::showOperationStatus,
                            modifier = Modifier.fillMaxSize(),
                        )

                        EqLibraryDestination.EqLibrary -> BrowseOpraScreen(
                            catalogState = catalogState,
                            profileVisibility = appPreferences.profileVisibility,
                            exportTargets = appPreferences.exportTargets,
                            managedHeadphones = managedHeadphonesForUi,
                            favoriteProfileIds = favoriteProfileIds,
                            savedGeneralPresetIds = savedGeneralPresetIds,
                            hiddenCanonicalProfileIds = appPreferences.hiddenCanonicalProfileIds,
                            blackPearlConnectionState = blackPearlConnectionState,
                            onFlashBlackPearlProfile = { profile ->
                                flashSuspendWithFeedback(ExportDevice.BLACK_PEARL) {
                                    onFlashBlackPearlFromMyDac(profile)
                                }
                            },
                            fiioJa11ConnectionState = fiioJa11ConnectionState,
                            onFlashFiioJa11Profile = { profile ->
                                flashSuspendWithFeedback(ExportDevice.FIIO_JA11) {
                                    onFlashFiioJa11FromMyDac(profile)
                                }
                            },
                            ew300ConnectionState = ew300ConnectionState,
                            onFlashEw300Profile = { profile ->
                                flashImmediateWithFeedback(ExportDevice.SIMGOT_EW300) {
                                    onFlashEw300FromMyDac(profile)
                                }
                            },
                            onToggleFavorite = onToggleFavorite,
                            onSaveGeneralPresets = onSaveGeneralPresets,
                            onHideCanonicalProfiles = onHideCanonicalProfiles,
                            onLoadManagedHeadphone = onLoadManagedHeadphone,
                            onSaveSelection = onSaveSelection,
                            onRemoveHeadphone = onRemoveHeadphone,
                            onDeleteSavedFilesForProfiles = onDeleteSavedFilesForProfiles,
                            onDeleteSavedFilesForProduct = onDeleteSavedFilesForProduct,
                            onExportProduct = requestExportProduct,
                            onMessage = ::showMessage,
                            onRefreshCatalog = requestCatalogRefresh,
                            onOpenUrl = onOpenUrl,
                            modifier = Modifier.fillMaxSize(),
                        )

                        EqLibraryDestination.Settings -> SettingsScreen(
                            appPreferences = appPreferences,
                            catalogState = catalogState,
                            onRefreshCatalog = requestCatalogRefresh,
                            onChangeExportFolder = { chooseExportFolder(null) },
                            onCheckForUpdates = requestUpdateCheck,
                            onWhatsNew = { showWhatsNew(latestVersion, appPreferences.updates.releaseNotes) },
                            onGetUpdate = { appPreferences.updates.releaseUrl?.let(onOpenUrl) },
                            onOpenUrl = onOpenUrl,
                            onThemeModeChange = onThemeModeChange,
                            onOutputBehaviorChange = onOutputBehaviorChange,
                            onExportTargetChange = onExportTargetChange,
                            onActiveExportTargetChange = onActiveExportTargetChange,
                            onDirectBlackPearlFlashEnabledChange = onDirectBlackPearlFlashEnabledChange,
                            onDirectFiioJa11FlashEnabledChange = onDirectFiioJa11FlashEnabledChange,
                            onDirectEw300FlashEnabledChange = onDirectEw300FlashEnabledChange,
                            hiddenCanonicalProfileIds = appPreferences.hiddenCanonicalProfileIds,
                            onUnhideCanonicalProfiles = onUnhideCanonicalProfiles,
                            onMessage = ::showMessage,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                }
            }
        }
    }

    if (whatsNewVersion != null) {
        WhatsNewDialog(
            version = whatsNewVersion.orEmpty(),
            notes = whatsNewNotes,
            onDismiss = {
                whatsNewVersion = null
                whatsNewNotes = ""
            },
        )
    }

    restoreActiveOutputExportRequest(pendingExportReviewState)?.let { request ->
        ExportReviewDialog(
            device = request.device,
            items = request.reviewItems(
                managedHeadphones = managedHeadphones,
                savedEqs = savedEqs,
                savedGeneralEqs = savedGeneralEqs,
            ),
            onDismiss = { pendingExportReviewState = null },
            onExport = {
                pendingExportReviewState = null
                runExportRequest(request)
            },
        )
    }
}

private fun ActiveOutputExportRequest.reviewItems(
    managedHeadphones: List<ManagedHeadphoneRecord>,
    savedEqs: List<SavedEqRecord>,
    savedGeneralEqs: List<SavedGeneralEqRecord>,
): List<ExportReviewItem> {
    fun fromHeadphone(headphone: ManagedHeadphoneRecord): List<ExportReviewItem> =
        headphone.profiles.asSequence()
            .filter { it.selected }
            .map { profile ->
                ExportReviewItem(
                    title = "${headphone.productName} · ${profile.lastKnownProfile.author ?: "Creator information missing"}",
                    profile = profile.lastKnownProfile,
                )
            }
            .toList()

    return when (this) {
        is ActiveOutputExportRequest.AllManaged -> managedHeadphones.flatMap(::fromHeadphone)
        is ActiveOutputExportRequest.Product -> managedHeadphones
            .firstOrNull { it.productId == productId }
            ?.let(::fromHeadphone)
            .orEmpty()
        is ActiveOutputExportRequest.ManagedProfile -> managedHeadphones
            .firstOrNull { it.productId == productId }
            ?.let { headphone ->
                headphone.profiles
                    .firstOrNull { it.profileId == profileId && it.selected }
                    ?.let { profile ->
                        listOf(
                            ExportReviewItem(
                                "${headphone.productName} · ${profile.lastKnownProfile.author ?: "Creator information missing"}",
                                profile.lastKnownProfile,
                            ),
                        )
                    }
            }
            .orEmpty()
        is ActiveOutputExportRequest.SavedEq -> savedEqs
            .firstOrNull { it.entryId == entryId && it.kind == SavedEqKind.Personal }
            ?.let { record ->
                record.actionProfileOrNull()?.let { profile -> listOf(ExportReviewItem(record.displayName, profile)) }
            }
            .orEmpty()
        is ActiveOutputExportRequest.GeneralEq -> savedGeneralEqs
            .firstOrNull { it.presetId == presetId }
            ?.let { record -> record.actionProfileOrNull()?.let { listOf(ExportReviewItem(record.displayName, it)) } }
            .orEmpty()
        is ActiveOutputExportRequest.GeneralEqBatch -> savedGeneralEqs
            .filter { it.presetId in presetIds }
            .mapNotNull { record -> record.actionProfileOrNull()?.let { ExportReviewItem(record.displayName, it) } }
    }
}

@Composable
private fun RootDestinationIcon(destination: EqLibraryDestination) {
    when (destination) {
        EqLibraryDestination.MyEqs -> Icon(Icons.Outlined.Star, contentDescription = null)
        EqLibraryDestination.MyDac -> Icon(Icons.Outlined.Usb, contentDescription = null)
        EqLibraryDestination.EqLibrary -> Icon(Icons.Outlined.Explore, contentDescription = null)
        EqLibraryDestination.Settings -> Icon(Icons.Outlined.Settings, contentDescription = null)
    }
}

private fun ActiveOutputExportRequest.toSaveableState(): ArrayList<String> = when (this) {
    is ActiveOutputExportRequest.AllManaged -> arrayListOf(EXPORT_REQUEST_ALL_MANAGED, device.name)
    is ActiveOutputExportRequest.Product -> arrayListOf(EXPORT_REQUEST_PRODUCT, device.name, productId)
    is ActiveOutputExportRequest.ManagedProfile -> arrayListOf(
        EXPORT_REQUEST_MANAGED_PROFILE,
        device.name,
        productId,
        profileId,
    )
    is ActiveOutputExportRequest.SavedEq -> arrayListOf(EXPORT_REQUEST_SAVED_EQ, device.name, entryId)
    is ActiveOutputExportRequest.GeneralEq -> arrayListOf(EXPORT_REQUEST_GENERAL_EQ, device.name, presetId)
    is ActiveOutputExportRequest.GeneralEqBatch -> arrayListOf(
        EXPORT_REQUEST_GENERAL_EQ_BATCH,
        device.name,
    ).apply { addAll(presetIds.sorted()) }
}

private fun restoreActiveOutputExportRequest(state: List<String>?): ActiveOutputExportRequest? {
    val requestType = state?.getOrNull(0) ?: return null
    val device = state.getOrNull(1)?.let { deviceName ->
        runCatching { ExportDevice.valueOf(deviceName) }.getOrNull()
    } ?: return null

    return when (requestType) {
        EXPORT_REQUEST_ALL_MANAGED -> ActiveOutputExportRequest.AllManaged(device)
        EXPORT_REQUEST_PRODUCT -> state.getOrNull(2)?.let { productId ->
            ActiveOutputExportRequest.Product(productId, device)
        }
        EXPORT_REQUEST_MANAGED_PROFILE -> {
            val productId = state.getOrNull(2) ?: return null
            val profileId = state.getOrNull(3) ?: return null
            ActiveOutputExportRequest.ManagedProfile(productId, profileId, device)
        }
        EXPORT_REQUEST_SAVED_EQ -> state.getOrNull(2)?.let { entryId ->
            ActiveOutputExportRequest.SavedEq(entryId, device)
        }
        EXPORT_REQUEST_GENERAL_EQ -> state.getOrNull(2)?.let { presetId ->
            ActiveOutputExportRequest.GeneralEq(presetId, device)
        }
        EXPORT_REQUEST_GENERAL_EQ_BATCH -> ActiveOutputExportRequest.GeneralEqBatch(
            presetIds = state.drop(2).toSet(),
            device = device,
        )
        else -> null
    }
}

private const val EXPORT_REQUEST_ALL_MANAGED = "all-managed"
private const val EXPORT_REQUEST_PRODUCT = "product"
private const val EXPORT_REQUEST_MANAGED_PROFILE = "managed-profile"
private const val EXPORT_REQUEST_SAVED_EQ = "saved-eq"
private const val EXPORT_REQUEST_GENERAL_EQ = "general-eq"
private const val EXPORT_REQUEST_GENERAL_EQ_BATCH = "general-eq-batch"
