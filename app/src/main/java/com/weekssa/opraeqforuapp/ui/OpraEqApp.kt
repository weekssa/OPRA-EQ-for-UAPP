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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.BuildConfig
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.data.export.PresetExportSummary
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncOutcome
import com.weekssa.opraeqforuapp.data.update.AppUpdateCheckResult
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.domain.update.SemVer
import com.weekssa.opraeqforuapp.ui.components.PostUpdateBanner
import com.weekssa.opraeqforuapp.ui.components.UpdateAvailableBanner
import com.weekssa.opraeqforuapp.ui.components.WhatsNewDialog
import com.weekssa.opraeqforuapp.ui.screens.BrowseOpraScreen
import com.weekssa.opraeqforuapp.ui.screens.ManagedHeadphoneDetailScreen
import com.weekssa.opraeqforuapp.ui.screens.MyHeadphonesScreen
import com.weekssa.opraeqforuapp.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

private enum class TopLevelDestination(val label: String) {
    MyHeadphones("My Headphones"),
    BrowseOpra("Browse EQs"),
}

private sealed interface ExportScope {
    data object AllManaged : ExportScope
    data class Product(val productId: String) : ExportScope
}

private sealed interface ExportRequest {
    val device: ExportDevice

    data class AllManaged(
        override val device: ExportDevice,
    ) : ExportRequest

    data class Product(
        val productId: String,
        override val device: ExportDevice,
    ) : ExportRequest
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpraEqApp(
    appPreferences: AppPreferences,
    catalogState: CatalogState,
    managedHeadphones: List<ManagedHeadphoneRecord>,
    onRefreshCatalog: suspend () -> CatalogSyncOutcome,
    onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,
    onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit,
    onRemoveHeadphone: suspend (String) -> Unit,
    onRemoveManagedProfile: suspend (String, String, Boolean) -> PresetCleanupSummary?,
    onRemoveManagedHeadphone: suspend (String, Boolean) -> PresetCleanupSummary?,
    onDeleteSavedFilesForProfiles: suspend (Set<String>) -> PresetCleanupSummary,
    onDeleteSavedFilesForProduct: suspend (String) -> PresetCleanupSummary,
    onMarkReviewed: suspend (String) -> Unit,
    onPersistExportTree: suspend (Uri) -> Boolean,
    onExportSelected: suspend (Uri, ExportDevice) -> PresetExportSummary,
    onExportProduct: suspend (Uri, String, ExportDevice) -> PresetExportSummary,
    onCheckForUpdates: suspend () -> AppUpdateCheckResult,
    onDismissUpdate: suspend (String) -> Unit,
    onDismissPostUpdate: suspend () -> Unit,
    onOpenUrl: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onProfileVisibilityChange: (ProfileVisibilityCategory, Boolean) -> Unit,
) {
    var selectedDestinationIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedManagedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var pendingExportRequest by remember { mutableStateOf<ExportRequest?>(null) }
    var pendingExportScope by remember { mutableStateOf<ExportScope?>(null) }
    var whatsNewVersion by remember { mutableStateOf<String?>(null) }
    var whatsNewNotes by remember { mutableStateOf("") }
    val destinations = remember { TopLevelDestination.entries }
    val selectedDestination = destinations[selectedDestinationIndex]
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val catalogBusy = catalogState is CatalogState.Loading ||
        (catalogState as? CatalogState.Ready)?.isRefreshing == true
    val selectedManagedHeadphone = selectedManagedProductId?.let { productId ->
        managedHeadphones.firstOrNull { it.productId == productId }
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

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun showWhatsNew(version: String?, notes: String?) {
        val actualVersion = version ?: return
        whatsNewVersion = actualVersion
        whatsNewNotes = notes.orEmpty()
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val request = pendingExportRequest
        pendingExportRequest = null
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            if (!onPersistExportTree(uri)) {
                snackbarHostState.showSnackbar("Couldn’t retain access to that folder. Choose another folder.")
            } else {
                val summary = when (request) {
                    is ExportRequest.AllManaged -> onExportSelected(uri, request.device)
                    is ExportRequest.Product -> onExportProduct(uri, request.productId, request.device)
                    null -> null
                }
                summary?.let { snackbarHostState.showSnackbar(exportMessage(it)) }
            }
        }
    }

    val chooseExportFolder: (ExportRequest?) -> Unit = { request ->
        pendingExportRequest = request
        folderPicker.launch(appPreferences.exportTreeUri?.let(Uri::parse))
    }

    val runExportRequest: (ExportRequest) -> Unit = { request ->
        val storedUri = appPreferences.exportTreeUri?.let(Uri::parse)
        if (storedUri == null) {
            chooseExportFolder(request)
        } else {
            scope.launch {
                val summary = when (request) {
                    is ExportRequest.AllManaged -> onExportSelected(storedUri, request.device)
                    is ExportRequest.Product -> onExportProduct(storedUri, request.productId, request.device)
                }
                snackbarHostState.showSnackbar(exportMessage(summary))
            }
        }
    }

    val requestExportAll: () -> Unit = {
        pendingExportScope = ExportScope.AllManaged
    }

    val requestExportProduct: (String) -> Unit = { productId ->
        pendingExportScope = ExportScope.Product(productId)
    }

    val requestUpdateCheck: () -> Unit = {
        scope.launch {
            val message = when (val result = onCheckForUpdates()) {
                is AppUpdateCheckResult.UpdateAvailable -> "Version ${result.release.version} is available."
                is AppUpdateCheckResult.UpToDate -> "EQ Library is up to date."
                AppUpdateCheckResult.Unavailable -> "Couldn’t check for updates right now. Try again later."
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val requestCatalogRefresh: () -> Unit = {
        if (!catalogBusy) {
            scope.launch {
                snackbarHostState.showSnackbar(refreshMessage(onRefreshCatalog()))
            }
        }
    }

    pendingExportScope?.let { exportScope ->
        AlertDialog(
            onDismissRequest = { pendingExportScope = null },
            title = { Text("Export to device") },
            text = {
                Column {
                    Text(
                        text = "Choose one target. Only that device format will be exported.",
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    ExportDevice.entries.forEach { device ->
                        TextButton(
                            onClick = {
                                pendingExportScope = null
                                val request = when (exportScope) {
                                    ExportScope.AllManaged -> ExportRequest.AllManaged(device)
                                    is ExportScope.Product -> ExportRequest.Product(exportScope.productId, device)
                                }
                                runExportRequest(request)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(device.folderName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = exportDeviceDescription(device),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                device.validationStatus?.let { status ->
                                    Text(
                                        text = "$status · hardware validation pending",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingExportScope = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    BackHandler(enabled = settingsOpen) {
        settingsOpen = false
    }

    Scaffold(
        topBar = {
            if (settingsOpen) {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = { settingsOpen = false }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(selectedDestination.label) },
                    actions = {
                        IconButton(
                            onClick = requestCatalogRefresh,
                            enabled = !catalogBusy,
                        ) {
                            if (catalogBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Refresh EQ Library catalog",
                                )
                            }
                        }
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (!settingsOpen) {
                NavigationBar {
                    destinations.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = selectedDestinationIndex == index,
                            onClick = { selectedDestinationIndex = index },
                            icon = {
                                Icon(
                                    imageVector = when (destination) {
                                        TopLevelDestination.MyHeadphones -> Icons.Outlined.Headphones
                                        TopLevelDestination.BrowseOpra -> Icons.Outlined.Explore
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
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
            if (!settingsOpen) {
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
                if (settingsOpen) {
                    SettingsScreen(
                        appPreferences = appPreferences,
                        catalogState = catalogState,
                        onRefreshCatalog = requestCatalogRefresh,
                        onChangeExportFolder = { chooseExportFolder(null) },
                        onCheckForUpdates = requestUpdateCheck,
                        onWhatsNew = { showWhatsNew(latestVersion, appPreferences.updates.releaseNotes) },
                        onGetUpdate = { appPreferences.updates.releaseUrl?.let(onOpenUrl) },
                        onOpenUrl = onOpenUrl,
                        onThemeModeChange = onThemeModeChange,
                        onProfileVisibilityChange = onProfileVisibilityChange,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    when (selectedDestination) {
                        TopLevelDestination.MyHeadphones -> {
                            if (selectedManagedHeadphone != null) {
                                ManagedHeadphoneDetailScreen(
                                    headphone = selectedManagedHeadphone,
                                    catalogState = catalogState,
                                    profileVisibility = appPreferences.profileVisibility,
                                    onLoadManagedHeadphone = onLoadManagedHeadphone,
                                    onSaveSelection = onSaveSelection,
                                    onRemoveHeadphone = onRemoveHeadphone,
                                    onRemoveManagedProfile = onRemoveManagedProfile,
                                    onRemoveManagedHeadphone = onRemoveManagedHeadphone,
                                    onDeleteSavedFilesForProfiles = onDeleteSavedFilesForProfiles,
                                    onDeleteSavedFilesForProduct = onDeleteSavedFilesForProduct,
                                    onMarkReviewed = onMarkReviewed,
                                    onExportProduct = requestExportProduct,
                                    onMessage = ::showMessage,
                                    onBack = { selectedManagedProductId = null },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                MyHeadphonesScreen(
                                    catalogState = catalogState,
                                    managedHeadphones = managedHeadphones,
                                    onBrowseOpra = { selectedDestinationIndex = TopLevelDestination.BrowseOpra.ordinal },
                                    onRefreshCatalog = requestCatalogRefresh,
                                    onExportPresets = requestExportAll,
                                    onOpenHeadphone = { selectedManagedProductId = it },
                                    onRemoveManagedHeadphone = onRemoveManagedHeadphone,
                                    onMessage = ::showMessage,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        TopLevelDestination.BrowseOpra -> BrowseOpraScreen(
                            catalogState = catalogState,
                            profileVisibility = appPreferences.profileVisibility,
                            managedHeadphones = managedHeadphones,
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
}

private fun exportDeviceDescription(device: ExportDevice): String = when (device) {
    ExportDevice.UAPP -> "UAPP / ToneBoosters XML"
    ExportDevice.BLACK_PEARL -> "10-band Black Pearl-compatible PK text"
    ExportDevice.TOPPING_DX5_II -> "TOPPING Tune text for DX5 II"
    ExportDevice.TOPPING_DX1_II -> "TOPPING Tune text for DX1 II"
}

private fun exportMessage(summary: PresetExportSummary): String {
    val message = when {
        summary.accessLost -> "Export folder access was lost. Choose the folder again."
        summary.results.isEmpty() -> "No selected presets are ready to export."
        summary.conflictCount > 0 || summary.failedCount > 0 ->
            "${summary.successfulCount} presets saved/current · ${summary.conflictCount + summary.failedCount} need review."
        summary.createdCount > 0 || summary.updatedCount > 0 ->
            "${summary.createdCount} new · ${summary.updatedCount} updated · ${summary.currentCount} already current."
        else -> "All ${summary.currentCount} selected presets are already current."
    }
    val device = summary.results.firstOrNull()?.candidate?.deviceName
    return if (device == null) message else "$device · $message"
}

private fun refreshMessage(outcome: CatalogSyncOutcome): String {
    val result = outcome.catalogResult
    return when (result) {
        is CatalogRefreshResult.Success -> {
            val affected = outcome.managedChanges?.affectedProductIds?.size ?: 0
            when (affected) {
                0 -> "EQ Library catalog is up to date."
                1 -> "1 of your headphones has changes."
                else -> "$affected of your headphones have changes."
            }
        }
        is CatalogRefreshResult.Failure -> when (result.reason) {
            CatalogRefreshFailureReason.Network -> if (result.usingSavedCatalog) {
                "Couldn’t refresh EQ Library. Using your saved catalog."
            } else {
                "Couldn’t download the EQ Library catalog."
            }
            CatalogRefreshFailureReason.InvalidCatalog -> if (result.usingSavedCatalog) {
                "Couldn’t use the new EQ Library catalog. Your previous saved catalog is still available."
            } else {
                "The downloaded EQ Library catalog couldn’t be processed."
            }
            CatalogRefreshFailureReason.Storage -> if (result.usingSavedCatalog) {
                "Couldn’t save the new EQ Library catalog. Using your previous saved catalog."
            } else {
                "Couldn’t save the EQ Library catalog on this device."
            }
        }
    }
}
