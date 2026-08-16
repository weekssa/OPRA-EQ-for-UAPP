package com.weekssa.opraeqforuapp.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.PresetExportSummary
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncOutcome
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.ui.screens.BrowseOpraScreen
import com.weekssa.opraeqforuapp.ui.screens.MyHeadphonesScreen
import com.weekssa.opraeqforuapp.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

private enum class TopLevelDestination(val label: String) {
    MyHeadphones("My Headphones"),
    BrowseOpra("Browse OPRA"),
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
    onPersistExportTree: suspend (Uri) -> Boolean,
    onExportSelected: suspend (Uri) -> PresetExportSummary,
    onThemeModeChange: (ThemeMode) -> Unit,
    onProfileVisibilityChange: (ProfileVisibilityCategory, Boolean) -> Unit,
) {
    var selectedDestinationIndex by rememberSaveable { mutableIntStateOf(0) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var exportAfterFolderPick by remember { mutableStateOf(false) }
    val destinations = remember { TopLevelDestination.entries }
    val selectedDestination = destinations[selectedDestinationIndex]
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val catalogBusy = catalogState is CatalogState.Loading ||
        (catalogState as? CatalogState.Ready)?.isRefreshing == true

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            exportAfterFolderPick = false
            return@rememberLauncherForActivityResult
        }
        val shouldExport = exportAfterFolderPick
        exportAfterFolderPick = false
        scope.launch {
            if (!onPersistExportTree(uri)) {
                snackbarHostState.showSnackbar("Couldn’t retain access to that folder. Choose another folder.")
            } else if (shouldExport) {
                snackbarHostState.showSnackbar(exportMessage(onExportSelected(uri)))
            }
        }
    }

    val chooseExportFolder: (Boolean) -> Unit = { exportAfterPick ->
        exportAfterFolderPick = exportAfterPick
        folderPicker.launch(appPreferences.exportTreeUri?.let(Uri::parse))
    }

    val requestExport: () -> Unit = {
        val storedUri = appPreferences.exportTreeUri?.let(Uri::parse)
        if (storedUri == null) {
            chooseExportFolder(true)
        } else {
            scope.launch {
                val summary = onExportSelected(storedUri)
                if (summary.accessLost) {
                    snackbarHostState.showSnackbar("Export folder access was lost. Choose the folder again.")
                } else {
                    snackbarHostState.showSnackbar(exportMessage(summary))
                }
            }
        }
    }

    val requestCatalogRefresh: () -> Unit = {
        if (!catalogBusy) {
            scope.launch {
                val message = refreshMessage(onRefreshCatalog())
                snackbarHostState.showSnackbar(message)
            }
        }
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
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "Back",
                            )
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
                                    contentDescription = "Refresh OPRA catalog",
                                )
                            }
                        }
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                            )
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
        val contentModifier = Modifier.padding(innerPadding)
        if (settingsOpen) {
            SettingsScreen(
                appPreferences = appPreferences,
                catalogState = catalogState,
                onRefreshCatalog = requestCatalogRefresh,
                onChangeExportFolder = { chooseExportFolder(false) },
                onThemeModeChange = onThemeModeChange,
                onProfileVisibilityChange = onProfileVisibilityChange,
                modifier = contentModifier,
            )
        } else {
            when (selectedDestination) {
                TopLevelDestination.MyHeadphones -> MyHeadphonesScreen(
                    catalogState = catalogState,
                    managedHeadphones = managedHeadphones,
                    onBrowseOpra = { selectedDestinationIndex = TopLevelDestination.BrowseOpra.ordinal },
                    onRefreshCatalog = requestCatalogRefresh,
                    onExportPresets = requestExport,
                    modifier = contentModifier,
                )
                TopLevelDestination.BrowseOpra -> BrowseOpraScreen(
                    catalogState = catalogState,
                    profileVisibility = appPreferences.profileVisibility,
                    managedHeadphones = managedHeadphones,
                    onLoadManagedHeadphone = onLoadManagedHeadphone,
                    onSaveSelection = onSaveSelection,
                    onRemoveHeadphone = onRemoveHeadphone,
                    onRefreshCatalog = requestCatalogRefresh,
                    modifier = contentModifier,
                )
            }
        }
    }
}

private fun exportMessage(summary: PresetExportSummary): String = when {
    summary.accessLost -> "Export folder access was lost. Choose the folder again."
    summary.results.isEmpty() -> "No selected presets are ready to export."
    summary.conflictCount > 0 || summary.failedCount > 0 ->
        "${summary.successfulCount} presets saved/current · ${summary.conflictCount + summary.failedCount} need review."
    summary.createdCount > 0 || summary.updatedCount > 0 ->
        "${summary.createdCount} new · ${summary.updatedCount} updated · ${summary.currentCount} already current."
    else -> "All ${summary.currentCount} selected presets are already current."
}

private fun refreshMessage(outcome: CatalogSyncOutcome): String {
    val result = outcome.catalogResult
    return when (result) {
        is CatalogRefreshResult.Success -> {
            val affected = outcome.managedChanges?.affectedProductIds?.size ?: 0
            when (affected) {
                0 -> "OPRA catalog is up to date."
                1 -> "1 of your headphones has changes."
                else -> "$affected of your headphones have changes."
            }
        }
        is CatalogRefreshResult.Failure -> when (result.reason) {
            CatalogRefreshFailureReason.Network -> if (result.usingSavedCatalog) {
                "Couldn’t refresh OPRA. Using your saved catalog."
            } else {
                "Couldn’t download the OPRA catalog."
            }
            CatalogRefreshFailureReason.InvalidCatalog -> if (result.usingSavedCatalog) {
                "Couldn’t use the new OPRA catalog. Your previous saved catalog is still available."
            } else {
                "The downloaded OPRA catalog couldn’t be processed."
            }
            CatalogRefreshFailureReason.Storage -> if (result.usingSavedCatalog) {
                "Couldn’t save the new OPRA catalog. Using your previous saved catalog."
            } else {
                "Couldn’t save the OPRA catalog on this device."
            }
        }
    }
}
