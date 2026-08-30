package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityCategory
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode

/**
 * Transitional overload while the v0.3 app shell is being wired to the new export-target
 * preference callbacks. Remove once OpraEqApp passes the new callbacks directly.
 */
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    catalogState: CatalogState,
    onRefreshCatalog: () -> Unit,
    onChangeExportFolder: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onWhatsNew: () -> Unit,
    onGetUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onProfileVisibilityChange: (ProfileVisibilityCategory, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) = SettingsScreen(
    appPreferences = appPreferences,
    catalogState = catalogState,
    onRefreshCatalog = onRefreshCatalog,
    onChangeExportFolder = onChangeExportFolder,
    onCheckForUpdates = onCheckForUpdates,
    onWhatsNew = onWhatsNew,
    onGetUpdate = onGetUpdate,
    onOpenUrl = onOpenUrl,
    onThemeModeChange = onThemeModeChange,
    onProfileVisibilityChange = onProfileVisibilityChange,
    onExportTargetChange = { _, _ -> },
    onShowUnexportablePresetsChange = {},
    modifier = modifier,
)
