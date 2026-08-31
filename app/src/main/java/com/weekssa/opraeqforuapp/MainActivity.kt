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
import com.weekssa.opraeqforuapp.data.blackpearl.AndroidBlackPearlUsbTransport
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
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
import com.weekssa.opraeqforuapp.data.library.SavedGeneralEqRepository
import com.weekssa.opraeqforuapp.data.managed.ManagedHeadphonesRepository
import com.weekssa.opraeqforuapp.data.managed.OpraEqDatabase
import com.weekssa.opraeqforuapp.data.preferences.AppPreferencesRepository
import com.weekssa.opraeqforuapp.data.sync.BackgroundSyncScheduler
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncCoordinator
import com.weekssa.opraeqforuapp.data.update.AppUpdateCoordinator
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlashResult
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlFlasher
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.ui.EqLibraryApp
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

    private val savedGeneralEqRepository by lazy {
        SavedGeneralEqRepository(database)
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

    private val blackPearlTransportDelegate = lazy {
        AndroidBlackPearlUsbTransport(applicationContext)
    }
    private val blackPearlTransport by blackPearlTransportDelegate
    private val blackPearlFlasher by lazy {
        BlackPearlFlasher(blackPearlTransport)
    }

    private var lastForegroundRefreshAttemptMillis: Long = 0L

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
            refreshCatalogIfDue()
        }

        setContent {
            val appPreferences = appPreferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = AppPreferences(),
            ).value
            val activeOutputId = appPreferences.exportTargets.activeTarget.name
            val catalogState = catalogRepository.state.collectAsStateWithLifecycle().value
            val managedHeadphones = managedHeadphonesRepository
                .observeHeadphones(activeOutputId)
                .collectAsStateWithLifecycle(initialValue = emptyList<ManagedHeadphoneRecord>())
                .value
            val savedEqs = savedEqRepository.observeAll().collectAsStateWithLifecycle(
                initialValue = emptyList<SavedEqRecord>(),
            ).value
            val savedGeneralEqs = savedGeneralEqRepository
                .observeForOutput(activeOutputId)
                .collectAsStateWithLifecycle(initialValue = emptyList<SavedGeneralEqRecord>())
                .value
            val blackPearlConnectionState = blackPearlTransport.state.collectAsStateWithLifecycle(
                initialValue = BlackPearlConnectionState.Disconnected,
            ).value

            OpraEqTheme(themeMode = appPreferences.themeMode) {
                EqLibraryApp(
                    appPreferences = appPreferences,
                    catalogState = catalogState,
                    managedHeadphones = managedHeadphones,
                    savedEqs = savedEqs,
                    savedGeneralEqs = savedGeneralEqs,
                    blackPearlConnectionState = blackPearlConnectionState,
                    onConnectBlackPearl = {
                        if (
                            appPreferences.directBlackPearlFlashEnabled &&
                            appPreferences.exportTargets.activeTarget == ExportDevice.BLACK_PEARL
                        ) {
                            blackPearlTransport.connect()
                        }
                    },
                    onFlashManagedProfile = { productId, profileId ->
                        flashManagedProfile(productId, profileId, activeOutputId)
                    },
                    onFlashSavedEq = ::flashSavedEq,
                    onFlashGeneralEq = { presetId -> flashGeneralEq(presetId, activeOutputId) },
                    onRefreshCatalog = syncCoordinator::refresh,
                    onLoadManagedHeadphone = { productId ->
                        managedHeadphonesRepository.getHeadphone(productId, activeOutputId)
                    },
                    onSaveSelection = { productId, selectedIds, autoInclude ->
                        val ready = catalogRepository.state.value as? CatalogState.Ready
                        if (ready != null) {
                            managedHeadphonesRepository.saveSelection(
                                catalog = ready.catalog,
                                productId = productId,
                                stagedSelectedProfileIds = selectedIds,
                                autoIncludeNewProfiles = autoInclude,
                                outputId = activeOutputId,
                            )
                        }
                    },
                    onRemoveHeadphone = { productId ->
                        managedHeadphonesRepository.removeHeadphone(productId, activeOutputId)
                    },
                    onRemoveManagedProfile = { productId, profileId, deleteSavedFiles ->
                        removeManagedProfile(
                            productId = productId,
                            profileId = profileId,
                            deleteSavedFiles = deleteSavedFiles,
                            outputId = activeOutputId,
                        )
                    },
                    onRemoveManagedHeadphone = { productId, deleteSavedFiles ->
                        removeManagedHeadphone(
                            productId = productId,
                            deleteSavedFiles = deleteSavedFiles,
                            outputId = activeOutputId,
                        )
                    },
                    onDeleteSavedFilesForProfiles = cleanupRepository::deleteForProfiles,
                    onDeleteSavedFilesForProduct = cleanupRepository::deleteForProduct,
                    onMarkReviewed = managedHeadphonesRepository::markReviewed,
                    onToggleFavorite = savedEqRepository::toggleFavorite,
                    onToggleGeneralPreset = { preset ->
                        savedGeneralEqRepository.toggleForOutput(activeOutputId, preset)
                    },
                    onImportPersonal = ::importPersonalEq,
                    onDeleteSavedEq = savedEqRepository::delete,
                    onRemoveGeneralEq = { presetId ->
                        savedGeneralEqRepository.removeFromOutput(activeOutputId, presetId)
                    },
                    onPersistExportTree = ::persistExportTree,
                    onEvaluateExportCurrentness = { treeUri ->
                        val allRecords = buildList {
                            addAll(managedHeadphones)
                            addAll(savedEqs.map(savedEqRepository::toManagedHeadphone))
                            addAll(savedGeneralEqs.map(savedGeneralEqRepository::toExportRecord))
                        }
                        exportRepository.evaluateCurrentness(
                            treeUri = treeUri,
                            headphones = allRecords,
                            device = appPreferences.exportTargets.activeTarget,
                        )
                    },
                    onExportSelected = { uri, device ->
                        val allRecords = buildList {
                            addAll(managedHeadphones)
                            addAll(savedEqs.map(savedEqRepository::toManagedHeadphone))
                            addAll(savedGeneralEqs.map(savedGeneralEqRepository::toExportRecord))
                        }
                        exportRepository.exportSelected(
                            treeUri = uri,
                            headphones = allRecords,
                            device = device,
                        )
                    },
                    onExportProduct = { uri, productId, device ->
                        exportManagedProduct(uri, productId, device, activeOutputId)
                    },
                    onExportSavedEq = ::exportSavedEq,
                    onExportGeneralEq = { uri, presetId, device ->
                        exportGeneralEq(uri, presetId, device, activeOutputId)
                    },
                    onCheckForUpdates = updateCoordinator::checkNow,
                    onDismissUpdate = appPreferencesRepository::dismissUpdate,
                    onDismissPostUpdate = appPreferencesRepository::dismissPostUpdateCard,
                    onOpenUrl = ::openExternalUrl,
                    onThemeModeChange = { themeMode ->
                        lifecycleScope.launch {
                            appPreferencesRepository.setThemeMode(themeMode)
                        }
                    },
                    onExportTargetChange = { device, enabled ->
                        lifecycleScope.launch {
                            appPreferencesRepository.setExportTargetEnabled(device, enabled)
                        }
                    },
                    onActiveExportTargetChange = { device ->
                        lifecycleScope.launch {
                            appPreferencesRepository.setActiveExportTarget(device)
                        }
                    },
                    onDirectBlackPearlFlashEnabledChange = { enabled ->
                        lifecycleScope.launch {
                            appPreferencesRepository.setDirectBlackPearlFlashEnabled(enabled)
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { refreshCatalogIfDue() }
    }

    override fun onDestroy() {
        if (blackPearlTransportDelegate.isInitialized()) {
            blackPearlTransport.close()
        }
        super.onDestroy()
    }

    private suspend fun refreshCatalogIfDue() {
        val ready = catalogRepository.state.value as? CatalogState.Ready ?: return
        val now = System.currentTimeMillis()
        if (now - ready.lastSuccessfulRefreshMillis < FOREGROUND_REFRESH_INTERVAL_MILLIS) return
        if (now - lastForegroundRefreshAttemptMillis < FOREGROUND_RETRY_THROTTLE_MILLIS) return
        lastForegroundRefreshAttemptMillis = now
        syncCoordinator.refresh()
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

    private suspend fun flashManagedProfile(
        productId: String,
        profileId: String,
        outputId: String,
    ): String {
        val managed = managedHeadphonesRepository.getHeadphone(productId, outputId)
            ?: return "That headphone is no longer saved for this output."
        val profile = managed.profiles.firstOrNull { it.profileId == profileId && it.selected }
            ?: return "That EQ is no longer selected for this output."
        return flashBlackPearlProfile(profile.lastKnownProfile)
    }

    private suspend fun flashSavedEq(entryId: String): String {
        val record = savedEqRepository.get(entryId)
            ?: return "That EQ is no longer saved in My EQs."
        return flashBlackPearlProfile(record.profile)
    }

    private suspend fun flashGeneralEq(
        presetId: String,
        outputId: String,
    ): String {
        val record = savedGeneralEqRepository.getForOutput(outputId, presetId)
            ?: return "That General EQ is no longer saved for this output."
        return flashBlackPearlProfile(record.profile)
    }

    private suspend fun flashBlackPearlProfile(profile: OpraEqProfile): String {
        val preferences = appPreferencesRepository.snapshot()
        if (preferences.exportTargets.activeTarget != ExportDevice.BLACK_PEARL) {
            return "Select Black Pearl as the active output before using direct Flash."
        }
        if (!preferences.directBlackPearlFlashEnabled) {
            return "Enable direct Flash in Settings → Black Pearl before flashing."
        }
        if (blackPearlTransport.state.value !is BlackPearlConnectionState.Connected) {
            return "Connect to the Black Pearl from My EQs before flashing."
        }

        return when (val result = blackPearlFlasher.flash(profile)) {
            is BlackPearlFlashResult.Success -> result.warning?.let { warning ->
                "Flash successful · $warning"
            } ?: "Flash successful"
            is BlackPearlFlashResult.NotRepresentable -> "Not flashable · ${result.reason}"
            is BlackPearlFlashResult.DeviceUnavailable -> result.reason
            is BlackPearlFlashResult.TransferFailed -> result.reason
        }
    }

    private suspend fun exportManagedProduct(
        treeUri: Uri,
        productId: String,
        device: ExportDevice,
        outputId: String,
    ): PresetExportSummary {
        val managed = managedHeadphonesRepository.getHeadphone(productId, outputId)
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

    private suspend fun exportGeneralEq(
        treeUri: Uri,
        presetId: String,
        device: ExportDevice,
        outputId: String,
    ): PresetExportSummary {
        val record = savedGeneralEqRepository.getForOutput(outputId, presetId)
            ?: return PresetExportSummary(results = emptyList())
        return exportRepository.exportSelected(
            treeUri = treeUri,
            headphones = listOf(savedGeneralEqRepository.toExportRecord(record)),
            device = device,
        )
    }

    private suspend fun removeManagedProfile(
        productId: String,
        profileId: String,
        deleteSavedFiles: Boolean,
        outputId: String,
    ): PresetCleanupSummary? {
        val managed = managedHeadphonesRepository.getHeadphone(productId, outputId) ?: return null
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
            managedHeadphonesRepository.removeUnavailableProfile(productId, profileId, outputId)
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
                managedHeadphonesRepository.removeHeadphone(productId, outputId)
            } else {
                managedHeadphonesRepository.saveSelection(
                    catalog = ready.catalog,
                    productId = productId,
                    stagedSelectedProfileIds = remainingCurrentSelected,
                    autoIncludeNewProfiles = managed.autoIncludeNewProfiles,
                    outputId = outputId,
                )
            }
        }

        return cleanup
    }

    private suspend fun removeManagedHeadphone(
        productId: String,
        deleteSavedFiles: Boolean,
        outputId: String,
    ): PresetCleanupSummary? {
        val cleanup = if (deleteSavedFiles) {
            cleanupRepository.deleteForProduct(productId)
        } else {
            null
        }
        managedHeadphonesRepository.removeHeadphone(productId, outputId)
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

    companion object {
        private const val FOREGROUND_REFRESH_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
        private const val FOREGROUND_RETRY_THROTTLE_MILLIS = 15L * 60L * 1000L
    }
}
