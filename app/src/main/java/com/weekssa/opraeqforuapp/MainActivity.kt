package com.weekssa.opraeqforuapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.catalog.HttpOpraCatalogSource
import com.weekssa.opraeqforuapp.data.catalog.OpraCatalogRepository
import com.weekssa.opraeqforuapp.data.export.PresetCleanupRepository
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.data.export.PresetExportRepository
import com.weekssa.opraeqforuapp.data.export.PresetExportSummary
import com.weekssa.opraeqforuapp.data.managed.ManagedHeadphonesRepository
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.data.preferences.AppPreferencesRepository
import com.weekssa.opraeqforuapp.data.sync.BackgroundSyncScheduler
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncCoordinator
import com.weekssa.opraeqforuapp.data.update.AppUpdateCoordinator
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.ui.OpraEqApp
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val appPreferencesRepository by lazy {
        AppPreferencesRepository(applicationContext)
    }

    private val catalogRepository by lazy {
        OpraCatalogRepository(
            filesDir = filesDir,
            source = HttpOpraCatalogSource(
                userAgent = "OPRA EQ for UAPP/${BuildConfig.VERSION_NAME}",
            ),
        )
    }

    private val database by lazy {
        OpraEqDatabase.create(applicationContext)
    }

    private val managedHeadphonesRepository by lazy {
        ManagedHeadphonesRepository(database)
    }

    private val exportRepository by lazy {
        PresetExportRepository(applicationContext, database)
    }

    private val cleanupRepository by lazy {
        PresetCleanupRepository(applicationContext, database)
    }

    private val syncCoordinator by lazy {
        CatalogSyncCoordinator(
            catalogRepository = catalogRepository,
            managedHeadphonesRepository = managedHeadphonesRepository,
        )
    }

    private val updateCoordinator by lazy {
        AppUpdateCoordinator(
            installedVersion = BuildConfig.VERSION_NAME,
            preferencesRepository = appPreferencesRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BackgroundSyncScheduler.ensureScheduled(applicationContext)

        lifecycleScope.launch { updateCoordinator.initialize() }
        lifecycleScope.launch {
            catalogRepository.initialize()
            val ready = catalogRepository.state.value as? CatalogState.Ready
            if (ready != null) {
                managedHeadphonesRepository.reconcileCatalog(ready.catalog)
            }
        }

        setContent {
            val appPreferences = appPreferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = AppPreferences(),
            ).value
            val catalogState = catalogRepository.state.collectAsStateWithLifecycle().value
            val managedHeadphones = managedHeadphonesRepository.observeHeadphones().collectAsStateWithLifecycle(
                initialValue = emptyList<ManagedHeadphoneRecord>(),
            ).value

            OpraEqTheme(themeMode = appPreferences.themeMode) {
                OpraEqApp(
                    appPreferences = appPreferences,
                    catalogState = catalogState,
                    managedHeadphones = managedHeadphones,
                    onRefreshCatalog = syncCoordinator::refresh,
                    onLoadManagedHeadphone = managedHeadphonesRepository::getHeadphone,
                    onSaveSelection = { productId, selectedIds, autoInclude ->
                        val ready = catalogRepository.state.value as? CatalogState.Ready
                        if (ready != null) {
                            managedHeadphonesRepository.saveSelection(
                                catalog = ready.catalog,
                                productId = productId,
                                stagedSelectedProfileIds = selectedIds,
                                autoIncludeNewProfiles = autoInclude,
                            )
                        }
                    },
                    onRemoveHeadphone = managedHeadphonesRepository::removeHeadphone,
                    onRemoveManagedProfile = ::removeManagedProfile,
                    onRemoveManagedHeadphone = ::removeManagedHeadphone,
                    onDeleteSavedFilesForProfiles = cleanupRepository::deleteForProfiles,
                    onDeleteSavedFilesForProduct = cleanupRepository::deleteForProduct,
                    onMarkReviewed = managedHeadphonesRepository::markReviewed,
                    onPersistExportTree = ::persistExportTree,
                    onExportSelected = { uri ->
                        exportRepository.exportSelected(
                            treeUri = uri,
                            headphones = managedHeadphones,
                        )
                    },
                    onExportProduct = ::exportManagedProduct,
                    onCheckForUpdates = updateCoordinator::checkNow,
                    onDismissUpdate = appPreferencesRepository::dismissUpdate,
                    onDismissPostUpdate = appPreferencesRepository::dismissPostUpdateCard,
                    onOpenUrl = ::openExternalUrl,
                    onThemeModeChange = { themeMode ->
                        lifecycleScope.launch {
                            appPreferencesRepository.setThemeMode(themeMode)
                        }
                    },
                    onProfileVisibilityChange = { category, visible ->
                        lifecycleScope.launch {
                            appPreferencesRepository.setProfileVisibility(category, visible)
                        }
                    },
                )
            }
        }
    }

    private suspend fun exportManagedProduct(treeUri: Uri, productId: String): PresetExportSummary {
        val managed = managedHeadphonesRepository.getHeadphone(productId)
            ?: return PresetExportSummary(results = emptyList())
        return exportRepository.exportSelected(
            treeUri = treeUri,
            headphones = listOf(managed),
        )
    }

    private suspend fun removeManagedProfile(
        productId: String,
        profileId: String,
        deleteSavedFiles: Boolean,
    ): PresetCleanupSummary? {
        val managed = managedHeadphonesRepository.getHeadphone(productId) ?: return null
        val record = managed.profiles.firstOrNull { it.profileId == profileId } ?: return null
        val ready = catalogRepository.state.value as? CatalogState.Ready
        val currentProfiles = ready?.catalog?.profilesForProduct(productId).orEmpty()
        val currentProfile = currentProfiles.firstOrNull { it.id == profileId }

        if (record.noLongerAvailable || currentProfile == null || ready == null) {
            managedHeadphonesRepository.removeUnavailableProfile(productId, profileId)
        } else {
            val selectionState = managed.toSelectionState()
            val remainingCurrentSelected = currentProfiles
                .filter(selectionState::isSelected)
                .mapTo(mutableSetOf()) { it.id }
                .also { it.remove(profileId) }
            val retainedSelectedRemain = managed.profiles.any {
                it.profileId != profileId && it.selected && it.noLongerAvailable
            }
            if (remainingCurrentSelected.isEmpty() && !retainedSelectedRemain) {
                managedHeadphonesRepository.removeHeadphone(productId)
            } else {
                managedHeadphonesRepository.saveSelection(
                    catalog = ready.catalog,
                    productId = productId,
                    stagedSelectedProfileIds = remainingCurrentSelected,
                    autoIncludeNewProfiles = managed.autoIncludeNewProfiles,
                )
            }
        }

        return if (deleteSavedFiles) {
            cleanupRepository.deleteForProfiles(setOf(profileId))
        } else {
            null
        }
    }

    private suspend fun removeManagedHeadphone(
        productId: String,
        deleteSavedFiles: Boolean,
    ): PresetCleanupSummary? {
        managedHeadphonesRepository.removeHeadphone(productId)
        return if (deleteSavedFiles) {
            cleanupRepository.deleteForProduct(productId)
        } else {
            null
        }
    }

    private suspend fun persistExportTree(uri: Uri): Boolean {
        return try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            val label = DocumentFile.fromTreeUri(applicationContext, uri)?.name
                ?.takeIf { it.isNotBlank() }
                ?: "Selected folder"
            appPreferencesRepository.setExportTree(uri.toString(), label)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun openExternalUrl(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
