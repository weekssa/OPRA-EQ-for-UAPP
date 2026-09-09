package com.weekssa.opraeqforuapp.ui

import android.net.Uri
import androidx.compose.runtime.Stable
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode

/**
 * Stable event contract for the root Compose surface.
 *
 * The object is created once by the Activity and contains callbacks only. Keeping the event surface
 * behind one stable parameter avoids rebuilding a large callback parameter list on every root
 * recomposition while preserving unidirectional data flow from [EqLibraryViewModel].
 *
 * User-facing operation callbacks return already-resolved presentation text so Compose never needs
 * to know about repository/data-layer result types merely to build a snackbar message.
 */
@Stable
class EqLibraryActions(
    val onConnectBlackPearl: () -> Unit,
    val onResetBlackPearl: suspend () -> String,
    val onConnectFiioJa11: () -> Unit,
    val onResetFiioJa11: suspend () -> String,
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
    val onExportTargetChange: (ExportDevice, Boolean) -> Unit,
    val onActiveExportTargetChange: (ExportDevice) -> Unit,
    val onDirectBlackPearlFlashEnabledChange: (Boolean) -> Unit,
    val onDirectFiioJa11FlashEnabledChange: (Boolean) -> Unit,
    val onDirectJcallyJm12FlashEnabledChange: (Boolean) -> Unit,
)
