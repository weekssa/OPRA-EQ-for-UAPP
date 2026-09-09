package com.weekssa.opraeqforuapp.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.withHiddenReviewPromptsSuppressed
import com.weekssa.opraeqforuapp.domain.update.SemVer
import com.weekssa.opraeqforuapp.ui.components.PostUpdateBanner
import com.weekssa.opraeqforuapp.ui.components.UpdateAvailableBanner
import com.weekssa.opraeqforuapp.ui.components.WhatsNewDialog
import com.weekssa.opraeqforuapp.ui.screens.BrowseOpraScreen
import com.weekssa.opraeqforuapp.ui.screens.ManagedHeadphoneDetailScreen
import com.weekssa.opraeqforuapp.ui.screens.MyDacScreen
import com.weekssa.opraeqforuapp.ui.screens.MyEqsHomeScreen
import com.weekssa.opraeqforuapp.ui.screens.SettingsScreen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqLibraryApp(
    state: EqLibraryUiState,
    actions: EqLibraryActions,
) {
    val appPreferences = state.appPreferences
    val catalogState = state.catalogState
    val managedHeadphones = state.managedHeadphones
    val savedEqs = state.savedEqs
    val savedGeneralEqs = state.savedGeneralEqs
    val exportCurrentness = state.exportCurrentness
    val blackPearlConnectionState = state.blackPearlConnectionState
    val fiioJa11ConnectionState = state.fiioJa11ConnectionState
    val jcallyJm12ConnectionState = state.jcallyJm12ConnectionState

    val onConnectDacForMyDac = actions.onConnectDacForMyDac
    val onConnectBlackPearl = actions.onConnectBlackPearl
    val onResetBlackPearl = actions.onResetBlackPearl
    val onConnectFiioJa11 = actions.onConnectFiioJa11
    val onResetFiioJa11 = actions.onResetFiioJa11
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
    val onSaveGeneralPreset = actions.onSaveGeneralPreset
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
    val onExportTargetChange = actions.onExportTargetChange
    val onActiveExportTargetChange = actions.onActiveExportTargetChange
    val onDirectBlackPearlFlashEnabledChange = actions.onDirectBlackPearlFlashEnabledChange
    val onDirectFiioJa11FlashEnabledChange = actions.onDirectFiioJa11FlashEnabledChange
    val onDirectJcallyJm12FlashEnabledChange = actions.onDirectJcallyJm12FlashEnabledChange

    var selectedDestinationName by rememberSaveable {
        mutableStateOf(EqLibraryDestination.MyEqs.name)
    }
    var selectedManagedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var outputMenuExpanded by remember { mutableStateOf(false) }
    var pendingExportRequestState by rememberSaveable { mutableStateOf<ArrayList<String>?>(null) }
    var whatsNewVersion by rememberSaveable { mutableStateOf<String?>(null) }
    var whatsNewNotes by rememberSaveable { mutableStateOf("") }

    val destinations = remember(state.dacRecognitionState.hasRecognizedDevice) {
        eqLibraryDestinations(showMyDac = state.dacRecognitionState.hasRecognizedDevice)
    }
    val selectedDestination = restoreEqLibraryDestination(selectedDestinationName, destinations)
    val activeOutput = appPreferences.exportTargets.activeTarget
    val enabledOutputs = remember(appPreferences.exportTargets) {
        ExportDevice.selectableOutputs.filter(appPreferences.exportTargets::isSelected)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exportFolderPermissionFailedMessage = stringResource(R.string.export_folder_permission_failed)
    val myDacDetectedMessage = stringResource(R.string.my_dac_detected_prompt)
    val openMyDacActionLabel = stringResource(R.string.my_dac_action_open)
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

    MyDacRecognitionPromptEffect(
        recognitionState = state.dacRecognitionState,
        isMyDacOpen = selectedDestination == EqLibraryDestination.MyDac,
        snackbarHostState = snackbarHostState,
        detectedMessage = myDacDetectedMessage,
        openActionLabel = openMyDacActionLabel,
        onOpenMyDac = {
            selectedManagedProductId = null
            selectedDestinationName = EqLibraryDestination.MyDac.name
        },
    )

    LaunchedEffect(destinations, selectedDestinationName) {
        val restored = restoreEqLibraryDestination(selectedDestinationName, destinations)
        if (restored.name != selectedDestinationName) {
            selectedDestinationName = restored.name
        }
    }

    LaunchedEffect(selectedManagedProductId, selectedManagedHeadphone, activeOutput) {
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

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun showWhatsNew(version: String?, notes: String?) {
        val actualVersion = version ?: return
        whatsNewVersion = actualVersion
        whatsNewNotes = notes.orEmpty()
    }

    suspend fun executeExport(
        uri: Uri,
        request: ActiveOutputExportRequest?,
    ): String? {
        if (request != null && !request.device.supportsFileExport) return null
        return when (request) {
            is ActiveOutputExportRequest.AllManaged -> onExportSelected(uri, request.device)
            is ActiveOutputExportRequest.Product -> onExportProduct(uri, request.productId, request.device)
            is ActiveOutputExportRequest.ManagedProfile -> onExportManagedProfile(
                uri,
                request.productId,
                request.profileId,
                request.device,
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
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            if (!onPersistExportTree(uri)) {
                snackbarHostState.showSnackbar(exportFolderPermissionFailedMessage)
            } else {
                executeExport(uri, request)?.let { message ->
                    snackbarHostState.showSnackbar(message)
                }
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
                    executeExport(storedUri, request)?.let { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        }
    }

    val requestExportAll = {
        runExportRequest(ActiveOutputExportRequest.AllManaged(activeOutput))
    }
    val requestExportProduct: (String) -> Unit = { productId ->
        runExportRequest(ActiveOutputExportRequest.Product(productId, activeOutput))
    }
    val requestExportManagedProfile: (String, String) -> Unit = { productId, profileId ->
        runExportRequest(ActiveOutputExportRequest.ManagedProfile(productId, profileId, activeOutput))
    }
    val requestExportSavedEq: (String) -> Unit = { entryId ->
        runExportRequest(ActiveOutputExportRequest.SavedEq(entryId, activeOutput))
    }
    val requestExportGeneralEq: (String) -> Unit = { presetId ->
        runExportRequest(ActiveOutputExportRequest.GeneralEq(presetId, activeOutput))
    }
    val requestExportGeneralEqs: (Set<String>) -> Unit = { presetIds ->
        runExportRequest(ActiveOutputExportRequest.GeneralEqBatch(presetIds, activeOutput))
    }
    val requestCatalogRefresh = {
        if (!catalogBusy) {
            scope.launch {
                snackbarHostState.showSnackbar(onRefreshCatalog())
            }
        }
    }
    val requestUpdateCheck: () -> Unit = {
        scope.launch {
            snackbarHostState.showSnackbar(onCheckForUpdates())
        }
    }

    BackHandler(
        enabled = selectedDestination == EqLibraryDestination.Settings ||
            selectedDestination == EqLibraryDestination.MyDac,
    ) {
        selectedManagedProductId = null
        selectedDestinationName = EqLibraryDestination.MyEqs.name
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedDestination.labelResId)) },
                actions = {
                    if (
                        selectedDestination == EqLibraryDestination.MyEqs ||
                        selectedDestination == EqLibraryDestination.EqLibrary
                    ) {
                        Box {
                            TextButton(onClick = { outputMenuExpanded = true }) {
                                Text(
                                    stringResource(
                                        R.string.output_selector_format,
                                        outputTitle(activeOutput),
                                    ),
                                )
                            }
                            DropdownMenu(
                                expanded = outputMenuExpanded,
                                onDismissRequest = { outputMenuExpanded = false },
                            ) {
                                enabledOutputs.forEach { output ->
                                    DropdownMenuItem(
                                        text = { Text(outputTitle(output)) },
                                        onClick = {
                                            outputMenuExpanded = false
                                            selectedManagedProductId = null
                                            onActiveExportTargetChange(output)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (selectedDestination == EqLibraryDestination.EqLibrary) {
                        IconButton(onClick = requestCatalogRefresh, enabled = !catalogBusy) {
                            if (catalogBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = stringResource(
                                        R.string.refresh_eq_library_content_description,
                                    ),
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedDestination == destination,
                        onClick = {
                            selectedManagedProductId = null
                            selectedDestinationName = destination.name
                        },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    EqLibraryDestination.MyEqs -> Icons.Outlined.Star
                                    EqLibraryDestination.MyDac -> Icons.Outlined.Usb
                                    EqLibraryDestination.EqLibrary -> Icons.Outlined.Explore
                                    EqLibraryDestination.Settings -> Icons.Outlined.Settings
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(destination.labelResId)) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (
                selectedDestination == EqLibraryDestination.MyEqs ||
                selectedDestination == EqLibraryDestination.EqLibrary
            ) {
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
                                directJcallyJm12FlashEnabled = appPreferences.directJcallyJm12FlashEnabled,
                                jcallyJm12ConnectionState = jcallyJm12ConnectionState,
                                onConnectJcallyJm12 = onConnectJcallyJm12,
                                onFlashManagedProfile = { profileId ->
                                    onFlashManagedProfile(selectedManagedHeadphone.productId, profileId)
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
                                onConnectBlackPearl = onConnectBlackPearl,
                                onResetBlackPearl = onResetBlackPearl,
                                directFiioJa11FlashEnabled = appPreferences.directFiioJa11FlashEnabled,
                                fiioJa11ConnectionState = fiioJa11ConnectionState,
                                onConnectFiioJa11 = onConnectFiioJa11,
                                onResetFiioJa11 = onResetFiioJa11,
                                directJcallyJm12FlashEnabled = appPreferences.directJcallyJm12FlashEnabled,
                                jcallyJm12ConnectionState = jcallyJm12ConnectionState,
                                onConnectJcallyJm12 = onConnectJcallyJm12,
                                onResetJcallyJm12 = onResetJcallyJm12,
                                onExportAll = requestExportAll,
                                onOpenHeadphone = { selectedManagedProductId = it },
                                onImportPersonal = onImportPersonal,
                                onDeleteSavedEq = onDeleteSavedEq,
                                onExportSavedEq = requestExportSavedEq,
                                onFlashSavedEq = onFlashSavedEq,
                                onRemoveGeneralEq = onRemoveGeneralEq,
                                onExportGeneralEq = requestExportGeneralEq,
                                onFlashGeneralEq = onFlashGeneralEq,
                                onMessage = ::showMessage,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    EqLibraryDestination.MyDac -> MyDacScreen(
                        recognitionState = state.dacRecognitionState,
                        blackPearlConnectionState = state.blackPearlConnectionState,
                        fiioJa11ConnectionState = state.fiioJa11ConnectionState,
                        jcallyJm12ConnectionState = state.jcallyJm12ConnectionState,
                        blackPearlHardwareEqState = state.blackPearlHardwareEqState,
                        blackPearlHardwareEqMatch = state.blackPearlHardwareEqMatch,
                        onConnectDac = onConnectDacForMyDac,
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
                        onToggleFavorite = onToggleFavorite,
                        onSaveGeneralPresets = { presets ->
                            val presetIds = presets.mapTo(mutableSetOf(), GeneralEqPreset::id)
                            presets.forEach { preset -> onSaveGeneralPreset(preset) }
                            if (presetIds.isNotEmpty()) requestExportGeneralEqs(presetIds)
                            presetIds.size
                        },
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
                        onBackFromRoot = {
                            selectedManagedProductId = null
                            selectedDestinationName = EqLibraryDestination.MyEqs.name
                        },
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
                        onExportTargetChange = onExportTargetChange,
                        onDirectBlackPearlFlashEnabledChange = onDirectBlackPearlFlashEnabledChange,
                        onDirectFiioJa11FlashEnabledChange = onDirectFiioJa11FlashEnabledChange,
                        onDirectJcallyJm12FlashEnabledChange = onDirectJcallyJm12FlashEnabledChange,
                        hiddenCanonicalProfileIds = appPreferences.hiddenCanonicalProfileIds,
                        onUnhideCanonicalProfiles = onUnhideCanonicalProfiles,
                        onMessage = ::showMessage,
                        modifier = Modifier.fillMaxSize(),
                    )
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
}

private fun outputTitle(device: ExportDevice): String = device.displayName

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
