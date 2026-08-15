package com.weekssa.opraeqforuapp.ui

import androidx.activity.compose.BackHandler
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
    onRefreshCatalog: suspend () -> CatalogRefreshResult,
    onThemeModeChange: (ThemeMode) -> Unit,
    onProfileVisibilityChange: (ProfileVisibilityCategory, Boolean) -> Unit,
) {
    var selectedDestinationIndex by rememberSaveable { mutableIntStateOf(0) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val destinations = remember { TopLevelDestination.entries }
    val selectedDestination = destinations[selectedDestinationIndex]
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val catalogBusy = catalogState is CatalogState.Loading ||
        (catalogState as? CatalogState.Ready)?.isRefreshing == true

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
                onThemeModeChange = onThemeModeChange,
                onProfileVisibilityChange = onProfileVisibilityChange,
                modifier = contentModifier,
            )
        } else {
            when (selectedDestination) {
                TopLevelDestination.MyHeadphones -> MyHeadphonesScreen(
                    catalogState = catalogState,
                    onBrowseOpra = { selectedDestinationIndex = TopLevelDestination.BrowseOpra.ordinal },
                    onRefreshCatalog = requestCatalogRefresh,
                    modifier = contentModifier,
                )
                TopLevelDestination.BrowseOpra -> BrowseOpraScreen(
                    catalogState = catalogState,
                    profileVisibility = appPreferences.profileVisibility,
                    onRefreshCatalog = requestCatalogRefresh,
                    modifier = contentModifier,
                )
            }
        }
    }
}

private fun refreshMessage(result: CatalogRefreshResult): String = when (result) {
    is CatalogRefreshResult.Success -> "OPRA catalog is up to date."
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
