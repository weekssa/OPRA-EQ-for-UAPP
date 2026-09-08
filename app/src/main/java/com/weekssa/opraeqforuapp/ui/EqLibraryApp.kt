package com.weekssa.opraeqforuapp.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.R
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.PresetExportItemResult
import com.weekssa.opraeqforuapp.data.export.PresetExportSummary
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncOutcome
import com.weekssa.opraeqforuapp.data.update.AppUpdateCheckResult
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
import com.weekssa.opraeqforuapp.ui.screens.MyEqsHomeScreen
import com.weekssa.opraeqforuapp.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

private enum class EqLibraryDestination(@StringRes val labelResId: Int) {
    MyEqs(R.string.nav_my_eqs),
    EqLibrary(R.string.nav_eq_library),
    Settings(R.string.nav_settings),
}

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
    val context = LocalContext.current
    val appPreferences = state.appPreferences
    val catalogState = state.catalogState
    val managedHeadphones = state.managedHeadphones
    val savedEqs = state.savedEqs
    val savedGeneralEqs = state.savedGeneralEqs
    val exportCurrentness = state.exportCurrentness
    val blackPearlConnectionState = state.blackPearlConnectionState
    val fiioJa11ConnectionState = state.fiioJa11ConnectionState
    val jcallyJm12ConnectionState = state.jcallyJm12ConnectionState

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

    var selectedDestinationIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedManagedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var outputMenuExpanded by remember { mutableStateOf(false) }
    var pendingExportRequest by remember { mutableStateOf<ActiveOutputExportRequest?>(null) }
    var whatsNewVersion by remember { mutableStateOf<String?>(null) }
    var whatsNewNotes by remember { mutableStateOf("") }

    val destinations = remember { EqLibraryDestination.entries }
    val selectedDestination = destinations[selectedDestinationIndex]
    val activeOutput = appPreferences.exportTargets.activeTarget
    val enabledOutputs = remember(appPreferences.exportTargets) {
        ExportDevice.selectableOutputs.filter(appPreferences.exportTargets::isSelected)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
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
    ): PresetExportSummary? {
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
        val request = pendingExportRequest
        pendingExportRequest = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            if (!onPersistExportTree(uri)) {
                snackbarHostState.showSnackbar(context.getString(R.string.export_folder_permission_failed))
            } else {
                executeExport(uri, request)?.let {
                    snackbarHostState.showSnackbar(context.activeOutputExportMessage(it))
                }
            }
        }
    }

    val chooseExportFolder: (ActiveOutputExportRequest?) -> Unit = { request ->
        pendingExportRequest = request
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
                    executeExport(storedUri, request)?.let {
                        snackbarHostState.showSnackbar(context.activeOutputExportMessage(it))
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
                snackbarHostState.showSnackbar(context.activeOutputRefreshMessage(onRefreshCatalog()))
            }
        }
    }
    val requestUpdateCheck: () -> Unit = {
        scope.launch {
            val message = when (val result = onCheckForUpdates()) {
                is AppUpdateCheckResult.UpdateAvailable ->
                    context.getString(R.string.update_available_message, result.release.version)
                is AppUpdateCheckResult.UpToDate ->
                    context.getString(R.string.update_up_to_date_message)
                AppUpdateCheckResult.Unavailable ->
                    context.getString(R.string.update_check_unavailable_message)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = selectedDestination == EqLibraryDestination.Settings) {
        selectedManagedProductId = null
        selectedDestinationIndex = EqLibraryDestination.MyEqs.ordinal
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedDestination.labelResId)) },
                actions = {
                    if (selectedDestination != EqLibraryDestination.Settings) {
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
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestinationIndex == index,
                        onClick = {
                            selectedManagedProductId = null
                            selectedDestinationIndex = index
                        },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    EqLibraryDestination.MyEqs -> Icons.Outlined.Star
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
            if (selectedDestination != EqLibraryDestination.Settings) {
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
                            selectedDestinationIndex = EqLibraryDestination.MyEqs.ordinal
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

private fun Context.activeOutputExportMessage(summary: PresetExportSummary): String {
    val reviewResults = summary.results.filter {
        it is PresetExportItemResult.Conflict || it is PresetExportItemResult.Failed
    }
    val firstReviewReason = reviewResults.firstOrNull()?.let { result ->
        when (result) {
            is PresetExportItemResult.Conflict -> result.reason
            is PresetExportItemResult.Failed -> result.reason
            else -> null
        }
    }
    val reviewCount = summary.conflictCount + summary.failedCount
    val message = when {
        summary.accessLost -> getString(R.string.export_folder_access_lost)
        summary.results.isEmpty() -> getString(R.string.export_none_ready)
        reviewCount > 0 -> {
            val successful = resources.getQuantityString(
                R.plurals.export_saved_current_count,
                summary.successfulCount,
                summary.successfulCount,
            )
            val review = resources.getQuantityString(
                R.plurals.export_review_count,
                reviewCount,
                reviewCount,
            )
            if (firstReviewReason == null) {
                getString(R.string.export_review_summary, successful, review)
            } else {
                getString(
                    R.string.export_review_summary_with_reason,
                    successful,
                    review,
                    firstReviewReason,
                )
            }
        }
        summary.createdCount > 0 || summary.updatedCount > 0 -> getString(
            R.string.export_write_summary,
            summary.createdCount,
            summary.updatedCount,
            summary.currentCount,
        )
        else -> resources.getQuantityString(
            R.plurals.export_all_current,
            summary.currentCount,
            summary.currentCount,
        )
    }
    val device = summary.results.firstOrNull()?.candidate?.deviceName
    return if (device == null) {
        message
    } else {
        getString(R.string.device_prefixed_message, device, message)
    }
}

private fun Context.activeOutputRefreshMessage(outcome: CatalogSyncOutcome): String {
    val result = outcome.catalogResult
    return when (result) {
        is CatalogRefreshResult.Success -> {
            val affected = outcome.managedChanges?.affectedProductIds?.size ?: 0
            if (affected == 0) {
                getString(R.string.catalog_up_to_date)
            } else {
                resources.getQuantityString(
                    R.plurals.catalog_saved_headphones_changed,
                    affected,
                    affected,
                )
            }
        }
        is CatalogRefreshResult.Failure -> when (result.reason) {
            CatalogRefreshFailureReason.Network -> if (result.usingSavedCatalog) {
                getString(R.string.catalog_refresh_failed_saved)
            } else {
                getString(R.string.catalog_download_failed)
            }
            CatalogRefreshFailureReason.InvalidCatalog -> if (result.usingSavedCatalog) {
                getString(R.string.catalog_invalid_saved)
            } else {
                getString(R.string.catalog_invalid_download)
            }
            CatalogRefreshFailureReason.Storage -> if (result.usingSavedCatalog) {
                getString(R.string.catalog_storage_failed_saved)
            } else {
                getString(R.string.catalog_storage_failed)
            }
        }
    }
}
