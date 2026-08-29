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
import com.weekssa.opraeqforuapp.data.library.CanonicalCatalogRepository
import com.weekssa.opraeqforuapp.data.library.CanonicalFirstCatalogRepository
import com.weekssa.opraeqforuapp.data.library.HttpCanonicalCatalogSource
import com.weekssa.opraeqforuapp.data.library.SavedEqRepository
import com.weekssa.opraeqforuapp.data.managed.ManagedHeadphonesRepository
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.data.preferences.AppPreferencesRepository
import com.weekssa.opraeqforuapp.data.sync.BackgroundSyncScheduler
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncCoordinator
import com.weekssa.opraeqforuapp.data.update.AppUpdateCoordinator
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
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
        CanonicalFirstCatalogRepository(
            canonicalRepository = CanonicalCatalogRepository(
                filesDir = filesDir,
                source = HttpCanonicalCatalogSource(
                    userAgent = "EQ Library/${BuildConfig.VERSION_NAME}",
                ),
            ),
            legacyFallback = OpraCatalogRepository(
                filesDir = filesDir,
                source = HttpOpraCatalogSource(
                    userAgent = "EQ Library/${BuildConfig.VERSION_NAME}",
                ),
            ),
        )
    }

    private val database by lazy {
        OpraEqDatabase.create(applicationContext)
    }

    private val managedHeadphonesRepository by lazy {
        ManagedHeadphonesRepository(database)
    }

    private val savedEqRepository by lazy {
        SavedEqRepository(database)
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
            val savedEqs = savedEqRepository.observeAll().collectAsStateWithLifecycle(
                initialValue = emptyList<SavedEqRecord>(),
            ).value

            OpraEqTheme(themeMode = appPreferences.themeMode) {
                OpraEqApp(
                    appPreferences = appPreferences,
                    catalogState = catalogState,
                    managedHeadphones = managedHeadphones,
                    savedEqs = savedEqs,
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
                    onToggleFavorite = savedEqRepository::toggleFavorite,
                    onImportPersonal = ::importPersonalEq,
                    onDeleteSavedEq = savedEqRepository::delete,
                    onPersistExportTree = ::persistExportTree,
                    onExportSelected = { uri, device ->
                        exportRepository.exportSelected(
                            treeUri = uri,
                            headphones = managedHeadphones,
                            device = device,
                        )
                    },
                    onExportProduct = ::exportManagedProduct,
                    onExportSavedEq = ::exportSavedEq,
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

    private suspend fun importPersonalEq(
        manufacturer: String,
        model: String,
        displayName: String,
        target: String?,
        peqText: String,
    ): String? = runCatching {
        savedEqRepository.importPersonal(
            manufacturer = manufacturer,
            model = model,
            displayName = displayName,
            target = target,
            peqText = peqText,
        )
    }.fold(
        onSuccess = { null },
        onFailure = { error -> error.message ?: "Couldn’t import that PEQ." },
    )

    private suspend fun exportManagedProduct(
        treeUri: Uri,
        productId: String,
        device: ExportDevice,
    ): PresetExportSummary {
        val managed = managedHeadphonesRepository.getHeadphone(productId)
            ?: return PresetExportSummary(results = emptyList())
        return exportRepository.exportSelected(
            treeUri = treeUri,
            headphones = listOf(managed),
            device = device,
        )
    }

    private suspend fun exportSavedEq(
        treeUri: Uri,
        entryId: String,
        device: ExportDevice,
    ): PresetExportSummary {
        val record = savedEqRepository.get(entryId)
            ?: return PresetExportSummary(results = emptyList())
        return exportRepository.exportSelected(
            treeUri = treeUri,
            headphones = listOf(savedEqRepository.toManagedHeadphone(record)),
            device = device,
        )
    }

    private suspend fun removeManagedProfile(
        productId: String,
        profileId: String,
        deleteSavedFiles: Boolean,
    ): PresetCleanupSummary? {
        val managed = managedHeadphonesRepository.getHeadphone(productId) ?: return null
        val record = managed.profiles.firstOrNull { it.profileId == profileId } ?: return null
        val cleanup = if (deleteSavedFiles) {
            cleanupRepository.deleteForProfiles(setOf(profileId))
        } else {
            null
        }
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

        return cleanup
    }

    private suspend fun removeManagedHeadphone(
        productId: String,
        deleteSavedFiles: Boolean,
    ): PresetCleanupSummary? {
        val cleanup = if (deleteSavedFiles) {
            cleanupRepository.deleteForProduct(productId)
        } else {
            null
        }
        managedHeadphonesRepository.removeHeadphone(productId)
        return cleanup
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
