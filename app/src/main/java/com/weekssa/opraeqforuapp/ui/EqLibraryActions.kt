package com.weekssa.opraeqforuapp.ui

import android.net.Uri
import androidx.compose.runtime.Stable
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.dac.DacControlId
import com.weekssa.opraeqforuapp.domain.dac.DacControlValue
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqHeadphoneAssociation
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.settings.OutputBehavior
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode

/** Stable event contract for the root Compose surface. */
@Stable
class EqLibraryActions(
    val onConnectDacForMyDac: (DacDeviceId) -> Unit,
    val onOpenBlackPearlEditor: () -> Unit,
    val onBackMyDacEditor: () -> Boolean,
    val onCloseMyDacEditor: () -> Unit,
    val onSelectBlackPearlEditorBand: (Int) -> Unit,
    val onShowBlackPearlEditorAllBands: () -> Unit,
    val onShowBlackPearlEditorReview: () -> Unit,
    val onUpdateBlackPearlEditorBand: (Int, EqFilterType, Double, Double, Double) -> Unit,
    val onUseSafeBlackPearlEditorGain: () -> Unit,
    val onResetBlackPearlEditorLocalEdits: () -> Unit,
    val onApplyBlackPearlEditor: (Boolean) -> Unit,
    val onCaptureBlackPearlDacEq: suspend (String, SavedEqHeadphoneAssociation?) -> String,
    val onFlashBlackPearlFromMyDac: suspend (OpraEqProfile) -> String,
    val onResetBlackPearlFromMyDac: suspend () -> String,
    val onReadBlackPearlQualificationControls: () -> Unit,
    val onSetBlackPearlDeviceControl: (DacControlId, DacControlValue) -> Unit,
    val onReadFiioJa11DeviceControls: () -> Unit,
    val onSetFiioJa11OutputVolume: (Int) -> Unit,
    val onSetFiioJa11EqProgram: (FiioJa11Protocol.EqProgram) -> Unit,
    val onSetFiioJa11HeadsetControl: (Boolean) -> Unit,
    val onSetFiioJa11UacMode: (FiioJa11Protocol.UacMode) -> Unit,
    val onFlashFiioJa11FromMyDac: suspend (OpraEqProfile) -> String,
    val onResetFiioJa11FromMyDac: suspend () -> String,
    val onFlashEw300FromMyDac: suspend (OpraEqProfile) -> String,
    val onResetEw300FromMyDac: suspend () -> String,
    val onConnectBlackPearl: () -> Unit,
    val onResetBlackPearl: suspend () -> String,
    val onConnectFiioJa11: () -> Unit,
    val onResetFiioJa11: suspend () -> String,
    val onConnectEw300: () -> Unit,
    val onResetEw300: suspend () -> String,
    val onConnectJcallyJm12: () -> Unit,
    val onResetJcallyJm12: suspend () -> String,
    val onFlashManagedProfile: suspend (String, String) -> String,
    val onFlashSavedEq: suspend (String) -> String,
    val onFlashGeneralEq: suspend (String) -> String,
    val onRefreshCatalog: suspend () -> String,
    val onLoadManagedHeadphone: suspend (String) -> ManagedHeadphoneRecord?,
    val onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit,
    val onRemoveHeadphone: suspend (String) -> Unit,
    val onRemoveManagedProfile: suspend (String, String, Boolean) -> PresetCleanupSummary?,
    val onRemoveManagedHeadphone: suspend (String, Boolean) -> PresetCleanupSummary?,
    val onDeleteSavedFilesForProfiles: suspend (Set<String>) -> PresetCleanupSummary,
    val onDeleteSavedFilesForProduct: suspend (String) -> PresetCleanupSummary,
    val onMarkReviewed: suspend (String) -> Unit,
    val onToggleFavorite: suspend (OpraEqProfile, String, String) -> Boolean,
    val onSaveGeneralPreset: suspend (GeneralEqPreset) -> Boolean,
    val onHideCanonicalProfiles: suspend (Set<String>) -> Unit,
    val onUnhideCanonicalProfiles: suspend (Set<String>) -> Unit,
    val onImportPersonal: suspend (String, String, String, String?, String) -> SavedEqRecord,
    val onDeleteSavedEq: suspend (String) -> Unit,
    val onRemoveGeneralEq: suspend (String) -> Unit,
    val onPersistExportTree: suspend (Uri) -> Boolean,
    val onExportSelected: suspend (Uri, ExportDevice) -> String,
    val onExportProduct: suspend (Uri, String, ExportDevice) -> String,
    val onExportManagedProfile: suspend (Uri, String, String, ExportDevice) -> String,
    val onExportSavedEq: suspend (Uri, String, ExportDevice) -> String,
    val onExportGeneralEq: suspend (Uri, String, ExportDevice) -> String,
    val onExportGeneralEqs: suspend (Uri, Set<String>, ExportDevice) -> String,
    val onCheckForUpdates: suspend () -> String,
    val onDismissUpdate: suspend (String) -> Unit,
    val onDismissPostUpdate: suspend () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onThemeModeChange: (ThemeMode) -> Unit,
    val onOutputBehaviorChange: (OutputBehavior) -> Unit,
    val onExportTargetChange: (ExportDevice, Boolean) -> Unit,
    val onSessionActiveExportTargetChange: (ExportDevice) -> Unit,
    val onActiveExportTargetChange: (ExportDevice) -> Unit,
    val onDirectBlackPearlFlashEnabledChange: (Boolean) -> Unit,
    val onDirectFiioJa11FlashEnabledChange: (Boolean) -> Unit,
    val onDirectEw300FlashEnabledChange: (Boolean) -> Unit,
    val onDirectJcallyJm12FlashEnabledChange: (Boolean) -> Unit,
)
