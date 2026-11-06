package com.weekssa.opraeqforuapp.ui.screens

import android.graphics.Rect
import android.view.KeyEvent
import android.view.WindowInsets
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.Espresso
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.ExportCurrentness
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
import com.weekssa.opraeqforuapp.data.hardware.completeFiioJa11Flash
import com.weekssa.opraeqforuapp.data.hardware.readFiioJa11SnapshotWithBoundedRetry
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqCategory
import com.weekssa.opraeqforuapp.domain.catalog.GeneralEqPreset
import com.weekssa.opraeqforuapp.domain.catalog.OpraBand
import com.weekssa.opraeqforuapp.domain.catalog.OpraCatalog
import com.weekssa.opraeqforuapp.domain.catalog.OpraEqProfile
import com.weekssa.opraeqforuapp.domain.catalog.OpraProduct
import com.weekssa.opraeqforuapp.domain.catalog.OpraVendor
import com.weekssa.opraeqforuapp.domain.dac.DacHeadroomStatus
import com.weekssa.opraeqforuapp.domain.dac.DacDeviceId
import com.weekssa.opraeqforuapp.domain.dac.DacRecognitionState
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditSpecs
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditor
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditorStartResult
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqEditWorkingCopy
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqFilter
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeBandFingerprint
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqNativeFingerprint
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshot
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotFactory
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotBundle
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.export.ExportDevice
import com.weekssa.opraeqforuapp.domain.fiio.FiioJa11DeviceSnapshot
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStage
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationStatus
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11OperationTrace
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Protocol
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Flasher
import com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11Transport
import com.weekssa.opraeqforuapp.domain.kt02h20.FiveBandOptimizationResult
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20FiveBandOptimizer
import com.weekssa.opraeqforuapp.domain.kt02h20.Kt02h20DeviceSpecs
import com.weekssa.opraeqforuapp.domain.library.FavoriteToggleResult
import com.weekssa.opraeqforuapp.domain.library.EqFilterType
import com.weekssa.opraeqforuapp.domain.library.SavedEqKind
import com.weekssa.opraeqforuapp.domain.library.SavedEqRecord
import com.weekssa.opraeqforuapp.domain.library.SavedGeneralEqRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedHeadphoneRecord
import com.weekssa.opraeqforuapp.domain.managed.ManagedProfileRecord
import com.weekssa.opraeqforuapp.domain.settings.AppPreferences
import com.weekssa.opraeqforuapp.domain.settings.ExportTargetPreferences
import com.weekssa.opraeqforuapp.domain.settings.ProfileVisibilityPreferences
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.domain.settings.EffectiveOutputResolver
import com.weekssa.opraeqforuapp.domain.settings.OutputBehavior
import com.weekssa.opraeqforuapp.ui.components.ExportReviewDialog
import com.weekssa.opraeqforuapp.ui.components.ExportReviewItem
import com.weekssa.opraeqforuapp.ui.components.WhatsNewDialog
import com.weekssa.opraeqforuapp.ui.components.TargetContextSelector
import com.weekssa.opraeqforuapp.ui.EqLibraryActions
import com.weekssa.opraeqforuapp.ui.EqLibraryApp
import com.weekssa.opraeqforuapp.ui.EqLibraryUiState
import com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiState
import com.weekssa.opraeqforuapp.ui.MyDacEditorApplyStatus
import com.weekssa.opraeqforuapp.ui.MyDacEditorError
import com.weekssa.opraeqforuapp.ui.MyDacEditorStage
import com.weekssa.opraeqforuapp.ui.MyDacEditorUiState
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

/** Shared fixtures for the context-bar capture test (top-level so other test files can use them). */

internal fun contextBarFixtureActions(
    onConnectDacForMyDac: (DacDeviceId) -> Unit = {},
    onApplyFiioJa11Editor: () -> Unit = {},
    onRefreshCatalog: () -> String = { "Fixture catalog is current." },
    onUnhideCanonicalProfiles: suspend (Set<String>) -> Unit = {},
    onReadBlackPearlEqSnapshot: () -> Unit = {},
    onFlashManagedProfile: suspend (String, String) -> String = { _, _ ->
        error("Hardware writes are not part of the app-shell test")
    },
) = EqLibraryActions(
        onConnectDacForMyDac = onConnectDacForMyDac,
        onOpenBlackPearlEditor = {},
        onBackMyDacEditor = { false },
        onCloseMyDacEditor = {},
        onSelectBlackPearlEditorBand = {},
        onShowBlackPearlEditorAllBands = {},
        onShowBlackPearlEditorReview = {},
        onUpdateBlackPearlEditorBand = { _, _, _, _, _ -> },
        onUseSafeBlackPearlEditorGain = {},
        onResetBlackPearlEditorLocalEdits = {},
        onApplyBlackPearlEditor = {},
        onOpenFiioJa11Editor = {},
        onBackFiioJa11Editor = { false },
        onCloseFiioJa11Editor = {},
        onSelectFiioJa11EditorBand = {},
        onShowFiioJa11EditorAllBands = {},
        onShowFiioJa11EditorReview = {},
        onUpdateFiioJa11EditorBand = { _, _, _, _, _ -> },
        onUseSafeFiioJa11EditorGain = {},
        onResetFiioJa11EditorLocalEdits = {},
        onApplyFiioJa11Editor = onApplyFiioJa11Editor,
        onOpenEw300Editor = {},
        onBackEw300Editor = { false },
        onCloseEw300Editor = {},
        onSelectEw300EditorBand = {},
        onShowEw300EditorAllBands = {},
        onShowEw300EditorReview = {},
        onUpdateEw300EditorBand = { _, _, _, _, _ -> },
        onUseSafeEw300EditorGain = {},
        onResetEw300EditorLocalEdits = {},
        onApplyEw300Editor = {},
        onCaptureBlackPearlDacEq = { _, _ -> error("Hardware capture is not part of the app-shell test") },
        onCaptureEw300DacEq = { _, _ -> error("Hardware capture is not part of the app-shell test") },
        onFlashBlackPearlFromMyDac = { error("Hardware writes are not part of the app-shell test") },
        onResetBlackPearlFromMyDac = { error("Hardware writes are not part of the app-shell test") },
        onReadBlackPearlQualificationControls = {},
        onReadBlackPearlEqSnapshot = onReadBlackPearlEqSnapshot,
        onSetBlackPearlDeviceControl = { _, _ -> },
        onReadFiioJa11DeviceControls = {},
        onSetFiioJa11OutputVolume = {},
        onSetFiioJa11EqProgram = {},
        onSetFiioJa11Defaults = {},
        onSetFiioJa11HeadsetControl = {},
        onSetFiioJa11UacMode = {},
            onSetJa11DcrTuning = {},
        onFlashFiioJa11FromMyDac = { error("Hardware writes are not part of the app-shell test") },
        onResetFiioJa11FromMyDac = { error("Hardware writes are not part of the app-shell test") },
        onFlashEw300FromMyDac = {},
        onResetEw300FromMyDac = { error("Hardware writes are not part of the app-shell test") },
        onRestoreEw300Baseline = { error("Hardware writes are not part of the app-shell test") },
        onRunEw300CapabilityBatch = { error("Hardware qualification is not part of the app-shell test") },
        onAdvanceEw300PersistenceQualification = { error("Hardware qualification is not part of the app-shell test") },
        onSetEw300PlaybackGain = {},
        onConnectBlackPearl = {},
        onResetBlackPearl = { error("Hardware writes are not part of the app-shell test") },
        onConnectFiioJa11 = {},
        onResetFiioJa11 = { error("Hardware writes are not part of the app-shell test") },
        onConnectEw300 = {},
        onResetEw300 = { error("Hardware writes are not part of the app-shell test") },
        onConnectJcallyJm12 = {},
        onResetJcallyJm12 = { error("Unsupported hardware is not part of the app-shell test") },
        onFlashManagedProfile = onFlashManagedProfile,
        onFlashSavedEq = { error("Hardware writes are not part of the app-shell test") },
        onFlashGeneralEq = { error("Hardware writes are not part of the app-shell test") },
        onRefreshCatalog = onRefreshCatalog,
        onLoadManagedHeadphone = { null },
        onSaveSelection = { _, _, _ -> },
        onRemoveHeadphone = {},
        onRemoveManagedProfile = { _, _, _ -> null },
        onRemoveManagedHeadphone = { _, _ -> null },
        onDeleteSavedFilesForProfiles = { ids -> PresetCleanupSummary(ids.size, 0, 0) },
        onDeleteSavedFilesForProduct = { PresetCleanupSummary(0, 0, 0) },
        onMarkReviewed = {},
        onToggleFavorite = { _, _, _ -> FavoriteToggleResult.SAVED },
        onSaveGeneralPresets = { false },
        onHideCanonicalProfiles = {},
        onUnhideCanonicalProfiles = onUnhideCanonicalProfiles,
        onImportPersonal = { _, _, _, _, _ -> error("Import writes are not part of the app-shell test") },
        onDeleteSavedEq = {},
        onRemoveGeneralEq = {},
        onPersistExportTree = { false },
        onExportSelected = { _, _ -> error("SAF export is not part of the app-shell test") },
        onExportProduct = { _, _, _ -> error("SAF export is not part of the app-shell test") },
        onExportManagedProfile = { _, _, _, _ -> error("SAF export is not part of the app-shell test") },
        onExportSavedEq = { _, _, _ -> error("SAF export is not part of the app-shell test") },
        onExportGeneralEq = { _, _, _ -> error("SAF export is not part of the app-shell test") },
        onExportGeneralEqs = { _, _, _ -> error("SAF export is not part of the app-shell test") },
        onCheckForUpdates = { "Update checks are not part of the app-shell test." },
        onDismissUpdate = {},
        onDismissPostUpdate = {},
        onOpenUrl = {},
        onThemeModeChange = {},
        onOutputBehaviorChange = {},
        onExportTargetChange = { _, _ -> },
        onSessionActiveExportTargetChange = {},
        onActiveExportTargetChange = {},
        onDirectBlackPearlFlashEnabledChange = {},
        onDirectFiioJa11FlashEnabledChange = {},
        onDirectEw300FlashEnabledChange = {},
        onDirectJcallyJm12FlashEnabledChange = {},
)

internal fun contextBarFixtureHardwareState(): HardwareEqSnapshotState {
    val bundle = requireNotNull(
        HardwareEqSnapshotFactory.fiioJa11(
            nativeBands = List(FiioJa11Protocol.BAND_COUNT) { index ->
                FiioJa11Protocol.Band(
                    type = "peak_dip",
                    frequencyHz = 100.0 * (index + 1),
                    gainDb = if (index == 0) 1.0 else 0.0,
                    q = 1.0,
                )
            },
            globalEqGainDb = 0.0,
            sessionGeneration = 1L,
            verifiedAtEpochMillis = 1L,
        ),
    )
    return HardwareEqSnapshotState().publishCurrent(bundle)
}
