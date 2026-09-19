package com.weekssa.opraeqforuapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshFailureReason
import com.weekssa.opraeqforuapp.data.catalog.CatalogRefreshResult
import com.weekssa.opraeqforuapp.data.export.PresetExportItemResult
import com.weekssa.opraeqforuapp.data.export.PresetExportSummary
import com.weekssa.opraeqforuapp.data.preferences.SessionExportTarget
import com.weekssa.opraeqforuapp.data.sync.BackgroundSyncScheduler
import com.weekssa.opraeqforuapp.data.sync.CatalogSyncOutcome
import com.weekssa.opraeqforuapp.data.update.AppUpdateCheckResult
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditSpecs
import com.weekssa.opraeqforuapp.domain.dac.normalizeHardwareEqUserInput
import com.weekssa.opraeqforuapp.ui.EqLibraryActions
import com.weekssa.opraeqforuapp.ui.EqLibraryApp
import com.weekssa.opraeqforuapp.ui.EqLibraryViewModel
import com.weekssa.opraeqforuapp.ui.UnclaimedEqViewModel
import com.weekssa.opraeqforuapp.ui.resolve
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: EqLibraryViewModel
    private lateinit var unclaimedEqViewModel: UnclaimedEqViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BackgroundSyncScheduler.ensureScheduled(applicationContext)

        val runtimeDependencies = createEqLibraryRuntimeDependencies(applicationContext)
        viewModel = ViewModelProvider(
            this,
            EqLibraryViewModel.Factory { runtimeDependencies.eqLibrary },
        )[EqLibraryViewModel::class.java]
        unclaimedEqViewModel = ViewModelProvider(
            this,
            UnclaimedEqViewModel.Factory { runtimeDependencies.unclaimedEqRepository },
        )[UnclaimedEqViewModel::class.java]

        val initialMyDacOpenDeviceId = if (savedInstanceState == null) {
            intent.supportedAttachedDacDeviceId()
        } else {
            null
        }

        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            val unclaimedEqs = unclaimedEqViewModel.items.collectAsStateWithLifecycle().value
            val actions = remember(viewModel) { createUiActions() }

            OpraEqTheme(themeMode = uiState.appPreferences.themeMode) {
                EqLibraryApp(
                    state = uiState,
                    actions = actions,
                    unclaimedEqs = unclaimedEqs,
                    onRecoverUnclaimedEq = unclaimedEqViewModel::recover,
                    onDeleteUnclaimedEq = unclaimedEqViewModel::delete,
                    initialMyDacOpenDeviceId = initialMyDacOpenDeviceId,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.onAppResumed()
    }

    private fun createUiActions(): EqLibraryActions = EqLibraryActions(
        onConnectDacForMyDac = viewModel::connectDacForMyDac,
        onOpenBlackPearlEditor = viewModel::openBlackPearlEditor,
        onBackMyDacEditor = viewModel::backMyDacEditor,
        onCloseMyDacEditor = viewModel::closeMyDacEditor,
        onSelectBlackPearlEditorBand = viewModel::selectBlackPearlEditorBand,
        onShowBlackPearlEditorAllBands = viewModel::showBlackPearlEditorAllBands,
        onShowBlackPearlEditorReview = viewModel::showBlackPearlEditorReview,
        onUpdateBlackPearlEditorBand = { bandIndex, type, frequencyHz, gainDb, q ->
            val normalized = normalizeHardwareEqUserInput(
                spec = HardwareEqEditSpecs.TRN_BLACK_PEARL,
                frequencyHz = frequencyHz,
                gainDb = gainDb,
                q = q,
            )
            viewModel.updateBlackPearlEditorBand(
                bandIndex = bandIndex,
                type = type,
                frequencyHz = normalized.frequencyHz,
                gainDb = normalized.gainDb,
                q = normalized.q,
            )
        },
        onUseSafeBlackPearlEditorGain = viewModel::useSafeBlackPearlEditorGain,
        onResetBlackPearlEditorLocalEdits = viewModel::resetBlackPearlEditorLocalEdits,
        onApplyBlackPearlEditor = viewModel::applyBlackPearlEditor,
        onOpenEw300Editor = viewModel::openEw300Editor,
        onBackEw300Editor = viewModel::backEw300Editor,
        onCloseEw300Editor = viewModel::closeEw300Editor,
        onSelectEw300EditorBand = viewModel::selectEw300EditorBand,
        onShowEw300EditorAllBands = viewModel::showEw300EditorAllBands,
        onShowEw300EditorReview = viewModel::showEw300EditorReview,
        onUpdateEw300EditorBand = { bandIndex, type, frequencyHz, gainDb, q ->
            val normalized = normalizeHardwareEqUserInput(
                spec = HardwareEqEditSpecs.SIMGOT_EW300,
                frequencyHz = frequencyHz,
                gainDb = gainDb,
                q = q,
            )
            viewModel.updateEw300EditorBand(bandIndex, type, normalized.frequencyHz, normalized.gainDb, normalized.q)
        },
        onUseSafeEw300EditorGain = viewModel::useSafeEw300EditorGain,
        onResetEw300EditorLocalEdits = viewModel::resetEw300EditorLocalEdits,
        onApplyEw300Editor = viewModel::applyEw300Editor,
        onCaptureBlackPearlDacEq = { displayName, association ->
            resolve(viewModel.captureBlackPearlDacEq(displayName, association))
        },
        onCaptureEw300DacEq = { displayName, association ->
            resolve(viewModel.captureEw300DacEq(displayName, association))
        },
        onFlashBlackPearlFromMyDac = { profile ->
            resolve(viewModel.flashBlackPearlFromMyDac(profile))
        },
        onResetBlackPearlFromMyDac = {
            resolve(viewModel.resetBlackPearlFromMyDacToFlat())
        },
        onReadBlackPearlQualificationControls = viewModel::readBlackPearlQualificationControls,
        onSetBlackPearlDeviceControl = viewModel::setBlackPearlDeviceControl,
        onReadFiioJa11DeviceControls = viewModel::readFiioJa11DeviceControls,
        onSetFiioJa11OutputVolume = viewModel::setFiioJa11OutputVolume,
        onSetFiioJa11EqProgram = viewModel::setFiioJa11EqProgram,
        onSetFiioJa11HeadsetControl = viewModel::setFiioJa11HeadsetControl,
        onSetFiioJa11UacMode = viewModel::setFiioJa11UacMode,
        onFlashFiioJa11FromMyDac = { profile ->
            resolve(viewModel.flashFiioJa11FromMyDac(profile))
        },
        onResetFiioJa11FromMyDac = {
            resolve(viewModel.resetFiioJa11FromMyDacToFlat())
        },
        onFlashEw300FromMyDac = { profile -> resolve(viewModel.flashEw300FromMyDac(profile)) },
        onResetEw300FromMyDac = { resolve(viewModel.resetEw300ToFlat()) },
        onQualifyEw300GlobalGain = { resolve(viewModel.qualifyEw300GlobalGain()) },
        onRunEw300CapabilityBatch = viewModel::runEw300CapabilityBatch,
        onConnectBlackPearl = viewModel::connectBlackPearl,
        onResetBlackPearl = {
            resolve(viewModel.resetBlackPearlToFlat())
        },
        onConnectFiioJa11 = viewModel::connectFiioJa11,
        onResetFiioJa11 = {
            resolve(viewModel.resetFiioJa11ToFlat())
        },
        onConnectEw300 = viewModel::connectEw300,
        onResetEw300 = { resolve(viewModel.resetEw300ToFlat()) },
        onConnectJcallyJm12 = viewModel::connectJcallyJm12,
        onResetJcallyJm12 = {
            resolve(viewModel.resetJcallyJm12ToFlat())
        },
        onFlashManagedProfile = { productId, profileId ->
            resolve(viewModel.flashManagedProfile(productId, profileId))
        },
        onFlashSavedEq = { entryId ->
            resolve(viewModel.flashSavedEq(entryId))
        },
        onFlashGeneralEq = { presetId ->
            resolve(viewModel.flashGeneralEq(presetId))
        },
        onRefreshCatalog = {
            refreshCatalogMessage(viewModel.refreshCatalog())
        },
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
            exportMessage(viewModel.exportSelected(treeUri.toString(), device))
        },
        onExportProduct = { treeUri, productId, device ->
            exportMessage(viewModel.exportProduct(treeUri.toString(), productId, device))
        },
        onExportManagedProfile = { treeUri, productId, profileId, device ->
            exportMessage(
                viewModel.exportManagedProfile(
                    treeUri.toString(),
                    productId,
                    profileId,
                    device,
                ),
            )
        },
        onExportSavedEq = { treeUri, entryId, device ->
            exportMessage(viewModel.exportSavedEq(treeUri.toString(), entryId, device))
        },
        onExportGeneralEq = { treeUri, presetId, device ->
            exportMessage(viewModel.exportGeneralEq(treeUri.toString(), presetId, device))
        },
        onExportGeneralEqs = { treeUri, presetIds, device ->
            exportMessage(viewModel.exportGeneralEqs(treeUri.toString(), presetIds, device))
        },
        onCheckForUpdates = {
            updateCheckMessage(viewModel.checkForUpdates())
        },
        onDismissUpdate = viewModel::dismissUpdate,
        onDismissPostUpdate = viewModel::dismissPostUpdate,
        onOpenUrl = ::openExternalUrl,
        onThemeModeChange = viewModel::setThemeMode,
        onOutputBehaviorChange = viewModel::setOutputBehavior,
        onExportTargetChange = viewModel::setExportTargetEnabled,
        onSessionActiveExportTargetChange = SessionExportTarget::select,
        onActiveExportTargetChange = viewModel::setActiveExportTarget,
        onDirectBlackPearlFlashEnabledChange = viewModel::setDirectBlackPearlFlashEnabled,
        onDirectFiioJa11FlashEnabledChange = viewModel::setDirectFiioJa11FlashEnabled,
        onDirectEw300FlashEnabledChange = viewModel::setDirectEw300FlashEnabled,
        onDirectJcallyJm12FlashEnabledChange = viewModel::setDirectJcallyJm12FlashEnabled,
    )

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

    private fun exportMessage(summary: PresetExportSummary): String {
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
        return if (device == null) message else getString(R.string.device_prefixed_message, device, message)
    }

    private fun refreshCatalogMessage(outcome: CatalogSyncOutcome): String {
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

    private fun updateCheckMessage(result: AppUpdateCheckResult): String = when (result) {
        is AppUpdateCheckResult.UpdateAvailable -> getString(R.string.update_available_message, result.release.version)
        is AppUpdateCheckResult.UpToDate -> getString(R.string.update_up_to_date_message)
        AppUpdateCheckResult.Unavailable -> getString(R.string.update_check_unavailable_message)
    }

    private fun openExternalUrl(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
