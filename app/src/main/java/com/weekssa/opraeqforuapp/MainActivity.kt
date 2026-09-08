package com.weekssa.opraeqforuapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weekssa.opraeqforuapp.data.sync.BackgroundSyncScheduler
import com.weekssa.opraeqforuapp.ui.EqLibraryApp
import com.weekssa.opraeqforuapp.ui.EqLibraryViewModel
import com.weekssa.opraeqforuapp.ui.resolve
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: EqLibraryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BackgroundSyncScheduler.ensureScheduled(applicationContext)

        viewModel = ViewModelProvider(
            this,
            EqLibraryViewModel.Factory {
                createEqLibraryDependencies(applicationContext)
            },
        )[EqLibraryViewModel::class.java]

        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

            OpraEqTheme(themeMode = uiState.appPreferences.themeMode) {
                EqLibraryApp(
                    appPreferences = uiState.appPreferences,
                    catalogState = uiState.catalogState,
                    managedHeadphones = uiState.managedHeadphones,
                    savedEqs = uiState.savedEqs,
                    savedGeneralEqs = uiState.savedGeneralEqs,
                    exportCurrentness = uiState.exportCurrentness,
                    blackPearlConnectionState = uiState.blackPearlConnectionState,
                    onConnectBlackPearl = viewModel::connectBlackPearl,
                    onResetBlackPearl = {
                        this@MainActivity.resolve(viewModel.resetBlackPearlToFlat())
                    },
                    fiioJa11ConnectionState = uiState.fiioJa11ConnectionState,
                    onConnectFiioJa11 = viewModel::connectFiioJa11,
                    onResetFiioJa11 = {
                        this@MainActivity.resolve(viewModel.resetFiioJa11ToFlat())
                    },
                    jcallyJm12ConnectionState = uiState.jcallyJm12ConnectionState,
                    onConnectJcallyJm12 = viewModel::connectJcallyJm12,
                    onResetJcallyJm12 = {
                        this@MainActivity.resolve(viewModel.resetJcallyJm12ToFlat())
                    },
                    onFlashManagedProfile = { productId, profileId ->
                        this@MainActivity.resolve(viewModel.flashManagedProfile(productId, profileId))
                    },
                    onFlashSavedEq = { entryId ->
                        this@MainActivity.resolve(viewModel.flashSavedEq(entryId))
                    },
                    onFlashGeneralEq = { presetId ->
                        this@MainActivity.resolve(viewModel.flashGeneralEq(presetId))
                    },
                    onRefreshCatalog = viewModel::refreshCatalog,
                    onLoadManagedHeadphone = viewModel::loadManagedHeadphone,
                    onSaveSelection = viewModel::saveSelection,
                    onRemoveHeadphone = viewModel::removeHeadphone,
                    onRemoveManagedProfile = viewModel::removeManagedProfile,
                    onRemoveManagedHeadphone = viewModel::removeManagedHeadphone,
                    onDeleteSavedFilesForProfiles = viewModel::deleteSavedFilesForProfiles,
                    onDeleteSavedFilesForProduct = viewModel::deleteSavedFilesForProduct,
                    onMarkReviewed = viewModel::markReviewed,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onSaveGeneralPreset = viewModel::saveGeneralPreset,
                    onHideCanonicalProfiles = viewModel::hideCanonicalProfiles,
                    onUnhideCanonicalProfiles = viewModel::unhideCanonicalProfiles,
                    onImportPersonal = viewModel::importPersonal,
                    onDeleteSavedEq = viewModel::deleteSavedEq,
                    onRemoveGeneralEq = viewModel::removeGeneralEq,
                    onPersistExportTree = ::persistExportTree,
                    onExportSelected = { treeUri, device ->
                        viewModel.exportSelected(treeUri.toString(), device)
                    },
                    onExportProduct = { treeUri, productId, device ->
                        viewModel.exportProduct(treeUri.toString(), productId, device)
                    },
                    onExportManagedProfile = { treeUri, productId, profileId, device ->
                        viewModel.exportManagedProfile(
                            treeUri.toString(),
                            productId,
                            profileId,
                            device,
                        )
                    },
                    onExportSavedEq = { treeUri, entryId, device ->
                        viewModel.exportSavedEq(treeUri.toString(), entryId, device)
                    },
                    onExportGeneralEq = { treeUri, presetId, device ->
                        viewModel.exportGeneralEq(treeUri.toString(), presetId, device)
                    },
                    onExportGeneralEqs = { treeUri, presetIds, device ->
                        viewModel.exportGeneralEqs(treeUri.toString(), presetIds, device)
                    },
                    onCheckForUpdates = viewModel::checkForUpdates,
                    onDismissUpdate = viewModel::dismissUpdate,
                    onDismissPostUpdate = viewModel::dismissPostUpdate,
                    onOpenUrl = ::openExternalUrl,
                    onThemeModeChange = viewModel::setThemeMode,
                    onExportTargetChange = viewModel::setExportTargetEnabled,
                    onActiveExportTargetChange = viewModel::setActiveExportTarget,
                    onDirectBlackPearlFlashEnabledChange = viewModel::setDirectBlackPearlFlashEnabled,
                    onDirectFiioJa11FlashEnabledChange = viewModel::setDirectFiioJa11FlashEnabled,
                    onDirectJcallyJm12FlashEnabledChange = viewModel::setDirectJcallyJm12FlashEnabled,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.onAppResumed()
    }

    private suspend fun persistExportTree(uri: Uri): Boolean {
        val label = withContext(Dispatchers.IO) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                DocumentFile.fromTreeUri(applicationContext, uri)?.name
                    ?.takeIf(String::isNotBlank)
                    ?: getString(R.string.selected_folder_fallback)
            } catch (_: SecurityException) {
                null
            }
        } ?: return false

        viewModel.setExportTree(uri.toString(), label)
        return true
    }

    private fun openExternalUrl(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
