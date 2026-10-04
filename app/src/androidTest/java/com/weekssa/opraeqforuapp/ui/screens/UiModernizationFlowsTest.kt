package com.weekssa.opraeqforuapp.ui.screens

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.SemanticsMatcher
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import com.weekssa.opraeqforuapp.data.blackpearl.BlackPearlConnectionState
import com.weekssa.opraeqforuapp.data.catalog.CatalogState
import com.weekssa.opraeqforuapp.data.export.ExportCurrentness
import com.weekssa.opraeqforuapp.data.export.PresetCleanupSummary
import com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectionState
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

class UiModernizationFlowsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun productionAppShellHasThreeStableRootsAndAnIndependentOutputContext() {
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                EqLibraryApp(
                    state = EqLibraryUiState(
                        catalogState = CatalogState.Ready(testCatalog(), lastSuccessfulRefreshMillis = 1L),
                    ),
                    actions = noOpEqLibraryActions(),
                )
            }
        }

        composeRule.onNodeWithText("Build your EQ library").assertIsDisplayed()
        composeRule.onNodeWithText("Target: USB Audio Player PRO / ToneBoosters", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("My DAC").assertDoesNotExist()
        composeRule.waitForIdle()
        captureV080Screenshot("app-shell-my-eqs-empty-light-100")

        composeRule.onNodeWithText("EQ Library").performClick()
        composeRule.onNodeWithText("Search headphones…").assertIsDisplayed()
        assertTrue(
            composeRule.onNodeWithText("Search headphones…").fetchSemanticsNode().boundsInRoot.bottom <=
                composeRule.onNodeWithText("Headphones").fetchSemanticsNode().boundsInRoot.top,
        )
        composeRule.waitForIdle()
        captureV080Screenshot("app-shell-library-light-100")

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Output").assertIsDisplayed()
        composeRule.onNodeWithText("This controls how EQs are prepared, exported, and flashed. It does not change Android's audio output.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("app-shell-settings-light-100")
    }

    @Test
    fun recognizedDacStaysContextualAndRemainsSeparateFromTheExportTarget() {
        fun assertRootLabelExists(label: String) {
            assertTrue(
                "$label root remains available",
                composeRule.onAllNodesWithText(label, substring = false).fetchSemanticsNodes().isNotEmpty(),
            )
        }

        val state = mutableStateOf(
            EqLibraryUiState(
                catalogState = CatalogState.Ready(testCatalog(), lastSuccessfulRefreshMillis = 1L),
                dacRecognitionState = DacRecognitionState(
                    presentDeviceIds = setOf(DacDeviceId.FIIO_JA11),
                    recognizedDeviceIds = setOf(DacDeviceId.FIIO_JA11),
                ),
                fiioJa11ConnectionState = Kt02h20ConnectionState.Connected,
            ),
        )
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                EqLibraryApp(state = state.value, actions = noOpEqLibraryActions())
            }
        }

        composeRule.onNodeWithText("FiiO JA11").assertIsDisplayed()
        composeRule.onNodeWithText("Connected · Waiting for first device read").assertIsDisplayed()
        composeRule.onNodeWithText("Target: USB Audio Player PRO / ToneBoosters", substring = true)
            .assertIsDisplayed()
        assertRootLabelExists("My EQs")
        assertRootLabelExists("EQ Library")
        assertRootLabelExists("Settings")
        composeRule.onNodeWithText("My DAC", substring = false).assertDoesNotExist()
        captureV080Screenshot("app-shell-recognized-device-context")

        composeRule.onNodeWithText("Open My DAC").performClick()
        composeRule.onNodeWithText("My DAC").assertIsDisplayed()
        assertRootLabelExists("EQ Library")
        assertRootLabelExists("Settings")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("FiiO JA11").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                dacRecognitionState = DacRecognitionState(
                    recognizedDeviceIds = setOf(DacDeviceId.FIIO_JA11),
                ),
                fiioJa11ConnectionState = Kt02h20ConnectionState.Disconnected,
            )
        }
        composeRule.onNodeWithText("Disconnected · No current device state").assertIsDisplayed()
        composeRule.onNodeWithText("Target: USB Audio Player PRO / ToneBoosters", substring = true)
            .assertIsDisplayed()
        assertRootLabelExists("My EQs")
        assertRootLabelExists("EQ Library")
        assertRootLabelExists("Settings")
        composeRule.onNodeWithText("My DAC", substring = false).assertDoesNotExist()
    }

    @Test
    fun dpadMovesFromHeadphoneSearchToTabsAndDirectResults() {
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestBrowseScreen(testCatalog())
            }
        }

        composeRule.onNodeWithText("Search headphones…").performTextInput("WH-1000XM4")
        composeRule.onNodeWithText("Search headphones…").performKeyInput {
            keyDown(Key.DirectionDown)
            keyUp(Key.DirectionDown)
        }
        composeRule.onNodeWithText("Headphones").assertIsFocused()

        composeRule.onNodeWithText("Headphones").performKeyInput {
            keyDown(Key.DirectionDown)
            keyUp(Key.DirectionDown)
        }
        val resultRow = composeRule.onAllNodesWithText("WH-1000XM4")[1]
        resultRow.assertIsFocused()
        resultRow.performKeyInput {
            keyDown(Key.DirectionCenter)
            keyUp(Key.DirectionCenter)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Test creator").assertIsDisplayed()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        repeat(2) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP) }
        composeRule.onNodeWithText("Target: USB Audio Player PRO / ToneBoosters", substring = true)
            .assertIsFocused()
        repeat(4) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN) }
        val focusedLabels = composeRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Focused, true),
        ).fetchSemanticsNodes().map { it.config.toString() }
        assertTrue("D-pad focus should reach the first profile row; focused nodes: $focusedLabels", focusedLabels.any {
            it.contains("Test creator")
        })
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
        composeRule.onNodeWithContentDescription("Details for Test creator").assertIsFocused()
        composeRule.onNodeWithContentDescription("Details for Test creator").performKeyInput {
            keyDown(Key.DirectionCenter)
            keyUp(Key.DirectionCenter)
        }
        composeRule.onNodeWithText("Filter response preview").assertIsDisplayed()
        composeRule.onNodeWithText("Add to My EQs").assertIsDisplayed()
    }

    @Test
    fun dpadMovesFromPersonalEqInputToParseWithoutSaving() {
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestMyEqsScreen(
                    managedHeadphones = emptyList(),
                    savedEqs = emptyList(),
                    savedGeneralEqs = emptyList(),
                )
            }
        }

        composeRule.onNodeWithText("Import Personal EQ").performClick()
        composeRule.onNodeWithText("Equalizer APO / AutoEq text")
            .performTextInput("Filter 1: ON PK Fc 100 Hz Gain 1 dB Q 1.0")
        composeRule.onNodeWithText("Equalizer APO / AutoEq text").performKeyInput {
            keyDown(Key.DirectionDown)
            keyUp(Key.DirectionDown)
        }

        composeRule.onNodeWithText("Parse EQ text").assertIsEnabled().assertIsFocused()
        captureV080Screenshot("personal-eq-import-dpad-parse-focused")
        composeRule.onNodeWithText("Parse EQ text").performKeyInput {
            keyDown(Key.DirectionCenter)
            keyUp(Key.DirectionCenter)
        }
        composeRule.onNodeWithText("Step 2 of 5 · Parse").assertIsDisplayed()
        composeRule.onNodeWithText("Continue to description").assertIsEnabled()
        captureV080Screenshot("personal-eq-import-dpad-parse-result")
    }

    @Test
    fun personalEqImportRouteAndDraftSurviveSavedStateRestorationWithoutSavingAgain() {
        val restorationTester = StateRestorationTester(composeRule)
        val largeDraft = "x".repeat(PersonalEqInputDraftStore.MAX_INLINE_SAVED_CHARACTERS + 1)
        val validDraft = "Filter 1: ON PK Fc 100 Hz Gain 1 dB Q 1.0"
        restorationTester.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestMyEqsScreen(
                    managedHeadphones = emptyList(),
                    savedEqs = emptyList(),
                    savedGeneralEqs = emptyList(),
                )
            }
        }

        composeRule.onNodeWithText("Import Personal EQ").performClick()
        composeRule.waitForIdle()
        captureV080Screenshot("personal-eq-import-route-transition")
        val pasteAction = composeRule.onNodeWithText("Paste PEQ text")
        pasteAction.performScrollTo().assertIsDisplayed()
        val pasteBounds = pasteAction.fetchSemanticsNode().boundsInRoot
        val parseBounds = composeRule.onNodeWithText("Parse EQ text").fetchSemanticsNode().boundsInRoot
        val minimumActionSize = with(composeRule.density) { 48.dp.toPx() }
        assertTrue("Paste action is at least 48dp wide", pasteBounds.width >= minimumActionSize)
        assertTrue("Paste action is at least 48dp high", pasteBounds.height >= minimumActionSize)
        assertTrue("Scrollable Paste action does not overlap the pinned Parse action", pasteBounds.bottom <= parseBounds.top)
        scrollToTextInLazyContent("Step 1 of 5 · Input")
        composeRule.onNodeWithText("Step 1 of 5 · Input").assertIsDisplayed()
        scrollToTextInLazyContent("Equalizer APO / AutoEq text")
        composeRule.onNodeWithText("Equalizer APO / AutoEq text").performTextInput(largeDraft)
        composeRule.waitForIdle()
        restorationTester.emulateSavedInstanceStateRestore()

        scrollToTextInLazyContent("Step 1 of 5 · Input")
        composeRule.onNodeWithText("Step 1 of 5 · Input").assertIsDisplayed()
        scrollToTextInLazyContent("Equalizer APO / AutoEq text")
        val restoredText = composeRule.onNode(hasSetTextAction())
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text
        assertEquals(largeDraft, restoredText)
        composeRule.onNodeWithText("Equalizer APO / AutoEq text")
            .performTextReplacement(validDraft)
        dismissSoftwareKeyboard()
        scrollToTextInLazyContent("Parse EQ text")
        composeRule.onNodeWithText("Parse EQ text").performClick()
        scrollToTextInLazyContent("Continue to description")
        composeRule.onNodeWithText("Continue to description").performClick()
        scrollToTextInLazyContent("Manufacturer")
        composeRule.onNodeWithText("Manufacturer").performTextInput("Fixture")
        scrollToTextInLazyContent("Headphone model")
        composeRule.onNodeWithText("Headphone model").performTextInput("Model One")
        scrollToTextInLazyContent("EQ name")
        composeRule.onNodeWithText("EQ name").performTextInput("Warm")
        dismissSoftwareKeyboard()
        scrollToTextInLazyContent("Review EQ")
        composeRule.onNodeWithText("Review EQ").performClick()
        scrollToTextInLazyContent("Step 4 of 5 · Review")
        composeRule.onNodeWithText("Step 4 of 5 · Review").assertIsDisplayed()
        restorationTester.emulateSavedInstanceStateRestore()

        scrollToTextInLazyContent("Step 4 of 5 · Review")
        composeRule.onNodeWithText("Step 4 of 5 · Review").assertIsDisplayed()
        scrollToTextInLazyContent("Warm")
        composeRule.onNodeWithText("Warm").assertIsDisplayed()
        scrollToTextInLazyContent("Fixture · Model One")
        composeRule.onNodeWithText("Fixture · Model One").assertIsDisplayed()
        composeRule.onNodeWithText("Save to My EQs").assertDoesNotExist()
    }

    @Test
    fun productionAppKeepsCachedCatalogSearchableWhenRefreshFails() {
        var refreshRequests = 0
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                EqLibraryApp(
                    state = EqLibraryUiState(
                        catalogState = CatalogState.Ready(testCatalog(), lastSuccessfulRefreshMillis = 1L),
                    ),
                    actions = noOpEqLibraryActions(
                        onRefreshCatalog = {
                            refreshRequests += 1
                            "Couldn’t refresh EQ Library. Using your saved catalog."
                        },
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Refresh now").performScrollTo().performClick()
        composeRule.onNodeWithText("Couldn’t refresh EQ Library. Using your saved catalog.")
            .assertIsDisplayed()
        composeRule.onNode(hasText("EQ Library").and(isSelectable())).performClick()
        composeRule.onNodeWithText("Search headphones…")
            .performTextInput("WH-1000XM4")
        composeRule.onAllNodesWithText("WH-1000XM4")[1].assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, refreshRequests) }
    }

    @Test
    fun hiddenCanonicalProfileRemainsInMyEqsAndUnhidePreservesSavedMembership() {
        val profile = OpraEqProfile(
            id = "hidden-profile-revision",
            productId = "hidden-product",
            author = "Fixture creator",
            details = "Saved tuning",
            link = null,
            profileType = "parametric_eq",
            preampGainDb = 0.0,
            bands = listOf(OpraBand("peak_dip", 100.0, 2.0, 1.0, null)),
            canonicalProfileId = "hidden-lineage",
        )
        val catalog = OpraCatalog(
            vendors = listOf(OpraVendor("hidden-vendor", "Hidden Maker")),
            products = listOf(
                OpraProduct("hidden-product", "hidden-vendor", "Saved Headphone", "Headphone", ""),
            ),
            profiles = listOf(profile),
        )
        val managedHeadphone = ManagedHeadphoneRecord(
            productId = "hidden-product",
            vendorId = "hidden-vendor",
            vendorName = "Hidden Maker",
            productName = "Saved Headphone",
            autoIncludeNewProfiles = false,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
            profiles = listOf(
                ManagedProfileRecord(
                    profileId = profile.id,
                    selected = true,
                    explicitlyExcluded = false,
                    lastKnownProfile = profile,
                    fingerprint = "saved-profile",
                    firstSeenAtMillis = 1L,
                    lastSeenAtMillis = 1L,
                    isNewUnreviewed = false,
                    isUpdatedUnreviewed = false,
                    noLongerAvailable = false,
                    generatedPresetName = null,
                    generatedXml = null,
                    generatedFromFingerprint = null,
                    generatedAtMillis = null,
                ),
            ),
        )
        val uiState = mutableStateOf(
            EqLibraryUiState(
                appPreferences = AppPreferences(hiddenCanonicalProfileIds = setOf("hidden-lineage")),
                catalogState = CatalogState.Ready(catalog, lastSuccessfulRefreshMillis = 1L),
                managedHeadphones = listOf(managedHeadphone),
            ),
        )
        var unhiddenIds = emptySet<String>()
        val actions = noOpEqLibraryActions(
            onUnhideCanonicalProfiles = { ids ->
                unhiddenIds = ids
                uiState.value = uiState.value.copy(
                    appPreferences = uiState.value.appPreferences.copy(
                        hiddenCanonicalProfileIds = uiState.value.appPreferences.hiddenCanonicalProfileIds - ids,
                    ),
                )
            },
        )
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                EqLibraryApp(state = uiState.value, actions = actions)
            }
        }

        composeRule.onNodeWithText("Saved Headphone").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Hidden EQs · 1").performScrollTo().performClick()
        composeRule.onNodeWithText("Saved Headphone").performClick()
        composeRule.onNodeWithText("Unhide selected (1)").performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(setOf("hidden-lineage"), unhiddenIds) }

        composeRule.onNodeWithText("My EQs").performClick()
        composeRule.onNodeWithText("Saved Headphone").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, uiState.value.managedHeadphones.single().selectedProfileCount)
            assertTrue(uiState.value.appPreferences.hiddenCanonicalProfileIds.isEmpty())
        }
    }

    @Test
    fun productionMyDacContextShowsCurrentReadbackAndReturnsToTheSameRoot() {
        var connectRequests = 0
        val currentHardware = fiioJa11CurrentHardwareState()
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                EqLibraryApp(
                    state = EqLibraryUiState(
                        catalogState = CatalogState.Ready(testCatalog(), lastSuccessfulRefreshMillis = 1L),
                        dacRecognitionState = DacRecognitionState(
                            presentDeviceIds = setOf(DacDeviceId.FIIO_JA11),
                            recognizedDeviceIds = setOf(DacDeviceId.FIIO_JA11),
                        ),
                        fiioJa11ConnectionState = Kt02h20ConnectionState.Connected,
                        fiioJa11HardwareEqState = currentHardware,
                        fiioJa11DeviceState = FiioJa11DeviceUiState(
                            snapshot = fixtureFiioDeviceSnapshot(),
                            isCurrentSession = true,
                        ),
                    ),
                    actions = noOpEqLibraryActions(onConnectDacForMyDac = { connectRequests += 1 }),
                )
            }
        }

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Output").assertIsDisplayed()
        composeRule.onNodeWithText("Connected · State current").assertIsDisplayed()
        composeRule.onNodeWithText("Open My DAC").performClick()
        composeRule.onNodeWithText("Current hardware EQ").assertIsDisplayed()
        composeRule.onNodeWithText("Edit EQ").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("My DAC").assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("my-dac-current-fiio-production-shell-fixture-light-100")
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Output").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, connectRequests) }
    }

    @Test
    fun productionMyDacContextExplainsUsbPermissionAndKeepsHistoricalStateStale() {
        var state = mutableStateOf(
            EqLibraryUiState(
                catalogState = CatalogState.Ready(testCatalog(), lastSuccessfulRefreshMillis = 1L),
                dacRecognitionState = DacRecognitionState(
                    presentDeviceIds = setOf(DacDeviceId.FIIO_JA11),
                    recognizedDeviceIds = setOf(DacDeviceId.FIIO_JA11),
                ),
                fiioJa11ConnectionState = Kt02h20ConnectionState.PermissionRequired("Fixture permission state."),
            ),
        )
        var connectRequests = 0
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                EqLibraryApp(
                    state = state.value,
                    actions = noOpEqLibraryActions(onConnectDacForMyDac = { connectRequests += 1 }),
                )
            }
        }

        composeRule.onNodeWithText("Permission needed · Allow USB access in My DAC").assertIsDisplayed()
        composeRule.onNodeWithText("Open My DAC").performClick()
        composeRule.onNodeWithText("USB permission required", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Retry connect").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()

        state.value = state.value.copy(
            dacRecognitionState = DacRecognitionState(
                recognizedDeviceIds = setOf(DacDeviceId.FIIO_JA11),
            ),
            fiioJa11ConnectionState = Kt02h20ConnectionState.Disconnected,
            fiioJa11HardwareEqState = fiioJa11CurrentHardwareState().markStale(),
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Disconnected · Last read is historical").assertIsDisplayed()
        composeRule.onNodeWithText("Open My DAC").performClick()
        composeRule.onNodeWithText("Last read hardware EQ").assertIsDisplayed()
        composeRule.onNodeWithText("Last read · User 1 · 5-band PEQ").assertIsDisplayed()
        composeRule.onNodeWithText("1. PEAK · 100 Hz · +1.00 dB · Q 1.00")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Edit EQ").assertIsNotEnabled()
        composeRule.waitForIdle()
        captureV080Screenshot("my-dac-stale-fiio-production-shell-fixture-light-100")
        composeRule.runOnIdle { assertEquals(0, connectRequests) }
    }

    @Test
    fun productionMyDacRecoveryStateWarnsAgainstRepeatingAnUnverifiedApply() {
        val uncertainTrace = fixtureUncertainFiioApplyTrace()
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                EqLibraryApp(
                    state = EqLibraryUiState(
                        catalogState = CatalogState.Ready(testCatalog(), lastSuccessfulRefreshMillis = 1L),
                        dacRecognitionState = DacRecognitionState(
                            recognizedDeviceIds = setOf(DacDeviceId.FIIO_JA11),
                        ),
                        fiioJa11ConnectionState = Kt02h20ConnectionState.Disconnected,
                        fiioJa11HardwareEqState = fiioJa11CurrentHardwareState().markStale(),
                        fiioJa11OperationTrace = uncertainTrace,
                        fiioJa11OperationStatus = FiioJa11OperationStatus.Completed(uncertainTrace),
                    ),
                    actions = noOpEqLibraryActions(),
                )
            }
        }

        composeRule.onNodeWithText("Open My DAC").performClick()
        composeRule.onNodeWithText("JA11 Apply not verified").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Do not retry the hardware action", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("my-dac-recovery-fiio-production-shell-fixture-light-100")
    }

    @Test
    fun unsupportedRecognizedDeviceDoesNotCreateAProductionMyDacEntry() {
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                EqLibraryApp(
                    state = EqLibraryUiState(
                        catalogState = CatalogState.Ready(testCatalog(), lastSuccessfulRefreshMillis = 1L),
                        dacRecognitionState = DacRecognitionState(
                            presentDeviceIds = setOf(DacDeviceId.JCALLY_JM12_STOCK),
                            recognizedDeviceIds = setOf(DacDeviceId.JCALLY_JM12_STOCK),
                        ),
                    ),
                    actions = noOpEqLibraryActions(),
                )
            }
        }

        composeRule.onNodeWithText("Build your EQ library").assertIsDisplayed()
        composeRule.onNodeWithText("My DAC").assertDoesNotExist()
        composeRule.onNodeWithText("JCALLY JM12").assertDoesNotExist()
    }

    @Test
    fun productionExportActionForHardwareOnlyTargetExplainsNoFilePath() {
        val profile = testCatalog().profiles.single()
        val savedEq = SavedEqRecord(
            entryId = "ja11-export-guard",
            kind = SavedEqKind.Personal,
            sourceProfileId = null,
            productId = "personal-fixture",
            manufacturer = "Fixture Audio",
            model = "Studio One",
            displayName = "Guard fixture",
            profile = profile.copy(id = "personal-guard", productId = "personal-fixture"),
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                EqLibraryApp(
                    state = EqLibraryUiState(
                        appPreferences = AppPreferences(
                            exportTargets = ExportTargetPreferences(
                                selectedTargets = setOf(ExportDevice.UAPP, ExportDevice.FIIO_JA11),
                                activeTarget = ExportDevice.FIIO_JA11,
                            ),
                        ),
                        savedEqs = listOf(savedEq),
                    ),
                    actions = noOpEqLibraryActions(),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Export Guard fixture").performClick()
        composeRule.onNodeWithText("FiiO JA11 has no verified import file", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Review export").assertDoesNotExist()
    }

    @Test
    fun myDacEditorShowsReadFailureDraftAndReviewWithoutStartingHardwareWork() {
        val workingCopy = fiioJa11EditorWorkingCopy()
        val plannedWorkingCopy = HardwareEqEditor.useSafeGain(
            workingCopy,
            HardwareEqEditSpecs.FIIO_JA11,
        )
        assertTrue(plannedWorkingCopy.hasChanges)
        assertEquals(DacHeadroomStatus.SAFE, plannedWorkingCopy.headroomAssessment?.status)

        val state = mutableStateOf(MyDacEditorUiState(isOpening = true))
        var retryRequests = 0
        var applyRequests = 0
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestScreenShell(title = "My DAC", showTarget = true) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        DacEqEditorScreen(
                            state = state.value,
                            onRetryOpen = { retryRequests += 1 },
                            onClose = { error("Close is not part of this read-only state review") },
                            onSelectBand = { error("Band selection is not part of this state review") },
                            onShowAllBands = { error("Band navigation is not part of this state review") },
                            onShowReview = { error("Review navigation is not part of this state review") },
                            onUpdateBand = { _, _, _, _, _ -> error("No draft edits are submitted") },
                            onUseSafeGain = { error("No draft edits are submitted") },
                            onResetEdits = { error("No draft edits are submitted") },
                            onApply = { applyRequests += 1 },
                            dacLabel = "FiiO JA11",
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Reading fresh hardware EQ…").assertIsDisplayed()
        composeRule.onNodeWithText("Edits stay local until you review and explicitly apply them.")
            .assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = MyDacEditorUiState(error = MyDacEditorError.READ_FAILED)
        }
        composeRule.onNodeWithText("fresh verified EQ read", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()
        composeRule.runOnIdle { assertEquals(1, retryRequests) }

        composeRule.runOnIdle {
            state.value = MyDacEditorUiState(
                stage = MyDacEditorStage.EDIT,
                workingCopy = workingCopy,
                selectedBandIndex = 0,
            )
        }
        composeRule.onNodeWithText("Select a band").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Frequency (Hz)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Review changes").performScrollTo().assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = MyDacEditorUiState(
                stage = MyDacEditorStage.REVIEW,
                workingCopy = plannedWorkingCopy,
                selectedBandIndex = 0,
            )
        }
        composeRule.onNodeWithText("Complete reviewed hardware target").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Opening, editing, and reviewing are local only. Apply is the first hardware write.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Apply to DAC").performScrollTo().assertIsEnabled()
        composeRule.waitForIdle()
        captureV080Screenshot("my-dac-editor-review-light-100")

        composeRule.runOnIdle {
            state.value = state.value.copy(applyStatus = MyDacEditorApplyStatus.APPLYING)
        }
        composeRule.onNodeWithText("Applying and verifying…").assertIsDisplayed()
        composeRule.onNodeWithText("Apply to DAC").assertIsNotEnabled()
        composeRule.runOnIdle {
            assertEquals(0, applyRequests)
            assertEquals(1, retryRequests)
        }
    }

    @Test
    fun myDacNumericEntryCreatesAnExactLocalDraftBeforeReview() {
        val state = mutableStateOf(
            MyDacEditorUiState(
                stage = MyDacEditorStage.EDIT,
                workingCopy = fiioJa11EditorWorkingCopy(),
                selectedBandIndex = 0,
            ),
        )
        var applyRequests = 0
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestScreenShell(title = "My DAC", showTarget = true) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        DacEqEditorScreen(
                            state = state.value,
                            onRetryOpen = { error("Fresh snapshot is already available") },
                            onClose = { error("Closing is not part of this draft review") },
                            onSelectBand = { error("Only the selected band's exact input is tested") },
                            onShowAllBands = { error("All-band navigation is not part of this draft review") },
                            onShowReview = {
                                state.value = state.value.copy(stage = MyDacEditorStage.REVIEW)
                            },
                            onUpdateBand = { index, type, frequency, gain, q ->
                                val current = requireNotNull(state.value.workingCopy)
                                state.value = state.value.copy(
                                    workingCopy = HardwareEqEditor.updateFilter(
                                        workingCopy = current,
                                        spec = HardwareEqEditSpecs.FIIO_JA11,
                                        bandIndex = index,
                                        type = type,
                                        frequencyHz = frequency,
                                        gainDb = gain,
                                        q = q,
                                    ),
                                )
                            },
                            onUseSafeGain = { error("Safety controls are not invoked by numeric entry") },
                            onResetEdits = { error("Reset is not part of this draft review") },
                            onApply = { applyRequests += 1 },
                            dacLabel = "FiiO JA11",
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Frequency (Hz)")
            .performScrollTo()
            .performTextReplacement("155")
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(155.0, state.value.workingCopy?.filters?.first()?.frequencyHz ?: -1.0, 0.0)
            assertTrue(requireNotNull(state.value.workingCopy).hasChanges)
            assertEquals(0, applyRequests)
        }
        composeRule.onNodeWithText("Apply to DAC").assertDoesNotExist()
        captureV080Screenshot("my-dac-editor-draft-light-100")

        composeRule.onNodeWithText("Review changes").performScrollTo().performClick()
        composeRule.onNodeWithText("Complete reviewed hardware target").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("155 Hz", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Apply to DAC").performScrollTo().assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(0, applyRequests) }
    }

    @Test
    fun myDacEditorExactValuesAndReviewAreReachableWithKeyboardKeys() {
        val state = mutableStateOf(
            MyDacEditorUiState(
                stage = MyDacEditorStage.EDIT,
                workingCopy = fiioJa11EditorWorkingCopy(),
                selectedBandIndex = 0,
            ),
        )
        var applyRequests = 0
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestScreenShell(title = "My DAC", showTarget = true) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        DacEqEditorScreen(
                            state = state.value,
                            onRetryOpen = { error("A current snapshot is available") },
                            onClose = {},
                            onSelectBand = { error("Keyboard traversal retains the selected band") },
                            onShowAllBands = { error("The tested route is directly to Review") },
                            onShowReview = {
                                state.value = state.value.copy(stage = MyDacEditorStage.REVIEW)
                            },
                            onUpdateBand = { index, type, frequency, gain, q ->
                                val current = requireNotNull(state.value.workingCopy)
                                state.value = state.value.copy(
                                    workingCopy = HardwareEqEditor.updateFilter(
                                        workingCopy = current,
                                        spec = HardwareEqEditSpecs.FIIO_JA11,
                                        bandIndex = index,
                                        type = type,
                                        frequencyHz = frequency,
                                        gainDb = gain,
                                        q = q,
                                    ),
                                )
                            },
                            onUseSafeGain = { error("No device action is part of keyboard input") },
                            onResetEdits = { error("No device action is part of keyboard input") },
                            onApply = { applyRequests += 1 },
                            dacLabel = "FiiO JA11",
                        )
                    }
                }
            }
        }

        fun isFocused(label: String): Boolean = runCatching {
            composeRule.onNodeWithText(label)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Focused]
        }.getOrDefault(false)

        fun tabUntilFocused(label: String) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            var attempt = 0
            while (!isFocused(label) && attempt < 80) {
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
                composeRule.waitForIdle()
                attempt += 1
            }
            assertTrue("Keyboard Tab reaches $label", isFocused(label))
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        tabUntilFocused("Frequency (Hz)")
        repeat(3) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_FORWARD_DEL) }
        listOf(KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_5).forEach {
            instrumentation.sendKeyDownUpSync(it)
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(155.0, state.value.workingCopy?.filters?.first()?.frequencyHz ?: -1.0, 0.0)
            assertEquals(0, applyRequests)
        }

        tabUntilFocused("Review changes")
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Review changes").assertIsDisplayed()
        composeRule.onNodeWithText("Complete reviewed hardware target")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("155 Hz", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, applyRequests) }
    }

    @Test
    fun dpadCanReachMyDacReviewWithoutApplying() {
        val state = mutableStateOf(
            MyDacEditorUiState(
                stage = MyDacEditorStage.EDIT,
                workingCopy = fiioJa11EditorWorkingCopy(),
                selectedBandIndex = 0,
            ),
        )
        var applyRequests = 0
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestScreenShell(title = "My DAC", showTarget = true) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        DacEqEditorScreen(
                            state = state.value,
                            onRetryOpen = { error("A current snapshot is available") },
                            onClose = {},
                            onSelectBand = {},
                            onShowAllBands = { error("All-band navigation is not part of this check") },
                            onShowReview = {
                                state.value = state.value.copy(stage = MyDacEditorStage.REVIEW)
                            },
                            onUpdateBand = { _, _, _, _, _ -> error("No edit is part of this D-pad path") },
                            onUseSafeGain = { error("Safety controls are not part of this check") },
                            onResetEdits = { error("Reset is not part of this check") },
                            onApply = { applyRequests += 1 },
                            dacLabel = "FiiO JA11",
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Frequency (Hz)").performScrollTo().performClick()
        fun dpadDown(label: String) {
            composeRule.onNodeWithText(label).performKeyInput {
                keyDown(Key.DirectionDown)
                keyUp(Key.DirectionDown)
            }
        }

        dpadDown("Frequency (Hz)")
        composeRule.onNodeWithText("Gain (dB)").assertIsFocused()
        dpadDown("Gain (dB)")
        composeRule.onNodeWithText("Q").assertIsFocused()
        dpadDown("Q")
        composeRule.onNodeWithText("Use safe gain").assertIsFocused()
        dpadDown("Use safe gain")
        composeRule.onNodeWithText("All bands").assertIsFocused()
        dpadDown("All bands")
        val focusedNodes = composeRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Focused, true),
        ).fetchSemanticsNodes().map { it.config.toString() }
        assertTrue("D-pad Down reaches Review changes; focused nodes: $focusedNodes", runCatching {
            composeRule.onNodeWithText("Review changes")
                .fetchSemanticsNode()
                .config[SemanticsProperties.Focused]
        }.getOrDefault(false))
        composeRule.onNodeWithText("Review changes").performKeyInput {
            keyDown(Key.DirectionCenter)
            keyUp(Key.DirectionCenter)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Complete reviewed hardware target").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Apply to DAC").performScrollTo().assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(0, applyRequests) }
    }

    @Test
    fun myDacBandChipsExposeFilterValuesAndSelectionToScreenReaders() {
        val state = mutableStateOf(
            MyDacEditorUiState(
                stage = MyDacEditorStage.EDIT,
                workingCopy = fiioJa11EditorWorkingCopy(),
                selectedBandIndex = 1,
            ),
        )
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestScreenShell(title = "My DAC", showTarget = true) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        DacEqEditorScreen(
                            state = state.value,
                            onRetryOpen = { error("A current snapshot is available") },
                            onClose = {},
                            onSelectBand = {},
                            onShowAllBands = {},
                            onShowReview = {},
                            onUpdateBand = { _, _, _, _, _ -> },
                            onUseSafeGain = {},
                            onResetEdits = {},
                            onApply = {},
                            dacLabel = "FiiO JA11",
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Select a band").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Frequency (Hz)").performScrollTo().assertIsDisplayed()
        captureV080Screenshot("my-dac-editor-select-band-scrolled")
        composeRule.onNodeWithContentDescription(
            "Band 1, Peak, 100 hertz, +4 decibels, Q 1",
        ).let {
            scrollToDisplayedNode(hasContentDescription("Band 1, Peak, 100 hertz, +4 decibels, Q 1"))
            it.assertIsDisplayed().assertIsNotSelected()
        }
        composeRule.onNodeWithContentDescription(
            "Band 2, Peak, 200 hertz, 0 decibels, Q 1",
        ).let {
            scrollToDisplayedNode(hasContentDescription("Band 2, Peak, 200 hertz, 0 decibels, Q 1"))
            it.assertIsDisplayed().assertIsSelected()
            val bounds = it.fetchSemanticsNode().boundsInRoot
            val minimumTouchTarget = with(composeRule.density) { 48.dp.toPx() }
            assertTrue("Five-band control is at least 48dp wide", bounds.width >= minimumTouchTarget)
            assertTrue("Five-band control is at least 48dp high", bounds.height >= minimumTouchTarget)
        }
        composeRule.onNodeWithText("Gain (dB)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Q").performScrollTo().assertIsDisplayed()

        val tenBandState = MyDacEditorUiState(
            stage = MyDacEditorStage.EDIT,
            workingCopy = editorWorkingCopy(DacDeviceId.TRN_BLACK_PEARL, 10),
            selectedBandIndex = 9,
        )
        composeRule.runOnIdle { state.value = tenBandState }
        composeRule.onNodeWithText("Select a band").performScrollTo().assertIsDisplayed()
        val tenthBand = hasContentDescription("Band 10, Peak, 1000 hertz, 0 decibels, Q 1")
        scrollToDisplayedNode(tenthBand)
        composeRule.onNode(tenthBand).assertIsDisplayed().assertIsSelected()
        val tenthBandBounds = composeRule.onNode(tenthBand).fetchSemanticsNode().boundsInRoot
        val minimumTouchTarget = with(composeRule.density) { 48.dp.toPx() }
        assertTrue("Ten-band control is at least 48dp wide", tenthBandBounds.width >= minimumTouchTarget)
        assertTrue("Ten-band control is at least 48dp high", tenthBandBounds.height >= minimumTouchTarget)
    }

    @Test
    fun myEqsEmptyStateOffersBrowseAndImportAndFavoritesRemainOutsideOwnership() {
        var browseCount = 0
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestScreenShell(title = "My EQs", showTarget = true) {
                MyEqsHomeScreen(
                managedHeadphones = emptyList(),
                savedEqs = emptyList(),
                savedGeneralEqs = emptyList(),
                activeOutput = ExportDevice.UAPP,
                exportCurrentness = ExportCurrentness(),
                directBlackPearlFlashEnabled = false,
                blackPearlConnectionState = BlackPearlConnectionState.Disconnected,
                directFiioJa11FlashEnabled = false,
                fiioJa11ConnectionState = Kt02h20ConnectionState.Disconnected,
                directJcallyJm12FlashEnabled = false,
                jcallyJm12ConnectionState = Kt02h20ConnectionState.Disconnected,
                onBrowseLibrary = { browseCount += 1 },
                onExportAll = {},
                onOpenHeadphone = {},
                onImportPersonal = { _, _, _, _, _ -> error("Import was not submitted") },
                onDeleteSavedEq = {},
                onExportSavedEq = {},
                onFlashSavedEq = { "" },
                onRemoveGeneralEq = {},
                onExportGeneralEq = {},
                onFlashGeneralEq = { "" },
                onMessage = {},
                )
                }
            }
        }

        composeRule.onNodeWithText("Build your EQ library").assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("my-eqs-empty-light-100")
        composeRule.onNodeWithText("Browse EQ Library").performClick()
        composeRule.runOnIdle { assertEquals(1, browseCount) }
        composeRule.onNodeWithText("Import Personal EQ").performScrollTo().performClick()
        composeRule.onNodeWithText("Step 1 of 5 · Input").assertIsDisplayed()
        composeRule.onNodeWithText("Import stays on this device. It does not connect to or change hardware.")
            .assertIsDisplayed()
    }

    @Test
    fun favoriteOnlyRecordDoesNotTurnMyEqsIntoAnOwnedFavoritesSection() {
        val profile = OpraEqProfile(
            id = "favorite-profile",
            productId = "headphone-1",
            author = "Fixture creator",
            details = "Fixture profile",
            link = null,
            profileType = "ParametricEQ",
            preampGainDb = null,
            bands = emptyList(),
        )
        val favorite = SavedEqRecord(
            entryId = "favorite-1",
            kind = SavedEqKind.Favorite,
            sourceProfileId = profile.id,
            productId = profile.productId,
            manufacturer = "Fixture",
            model = "Headphone",
            displayName = "Favorite only profile",
            profile = profile,
            createdAtMillis = 0,
            updatedAtMillis = 0,
        )
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Dark) {
                TestScreenShell(title = "My EQs", showTarget = true) {
                MyEqsHomeScreen(
                managedHeadphones = emptyList(),
                savedEqs = listOf(favorite),
                savedGeneralEqs = emptyList(),
                activeOutput = ExportDevice.UAPP,
                exportCurrentness = ExportCurrentness(),
                directBlackPearlFlashEnabled = false,
                blackPearlConnectionState = BlackPearlConnectionState.Disconnected,
                directFiioJa11FlashEnabled = false,
                fiioJa11ConnectionState = Kt02h20ConnectionState.Disconnected,
                directJcallyJm12FlashEnabled = false,
                jcallyJm12ConnectionState = Kt02h20ConnectionState.Disconnected,
                onBrowseLibrary = {},
                onExportAll = {},
                onOpenHeadphone = {},
                onImportPersonal = { _, _, _, _, _ -> error("Import was not submitted") },
                onDeleteSavedEq = {},
                onExportSavedEq = {},
                onFlashSavedEq = { "" },
                onRemoveGeneralEq = {},
                onExportGeneralEq = {},
                onFlashGeneralEq = { "" },
                onMessage = {},
            )
                }
            }
        }

        composeRule.onNodeWithText("Build your EQ library").assertIsDisplayed()
        composeRule.onNodeWithText("Favorite only profile").assertDoesNotExist()
        composeRule.onNodeWithText("Favorites", substring = true).assertDoesNotExist()
        composeRule.waitForIdle()
        captureV080Screenshot("my-eqs-empty-dark-100")
    }

    @Test
    fun myEqsKeepsHeadphoneGeneralAndPersonalOwnershipSectionsDistinct() {
        val theme = mutableStateOf(ThemeMode.Light)
        val profile = OpraEqProfile(
            id = "owned-profile",
            productId = "owned-headphone",
            author = "Fixture creator",
            details = "Reference tuning",
            link = null,
            profileType = "parametric_eq",
            preampGainDb = -2.0,
            bands = listOf(OpraBand("peak_dip", 100.0, 1.0, 1.0, null)),
        )
        val headphone = ManagedHeadphoneRecord(
            productId = "owned-headphone",
            vendorId = "fixture-vendor",
            vendorName = "Fixture Audio",
            productName = "Studio One",
            autoIncludeNewProfiles = false,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
            profiles = listOf(
                ManagedProfileRecord(
                    profileId = profile.id,
                    selected = true,
                    explicitlyExcluded = false,
                    lastKnownProfile = profile,
                    fingerprint = "fixture-fingerprint",
                    firstSeenAtMillis = 1L,
                    lastSeenAtMillis = 1L,
                    isNewUnreviewed = false,
                    isUpdatedUnreviewed = false,
                    noLongerAvailable = false,
                    generatedPresetName = null,
                    generatedXml = null,
                    generatedFromFingerprint = null,
                    generatedAtMillis = null,
                ),
            ),
        )
        val personal = SavedEqRecord(
            entryId = "personal-owned",
            kind = SavedEqKind.Personal,
            sourceProfileId = null,
            productId = "personal-owned",
            manufacturer = "My audio",
            model = "Headphone two",
            displayName = "Warm personal EQ",
            profile = profile.copy(id = "personal-profile", productId = "personal-owned"),
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
        val general = SavedGeneralEqRecord(
            presetId = "general-owned",
            displayName = "Soft room correction",
            category = GeneralEqCategory.SOUND,
            profile = profile.copy(id = "general-profile", productId = "general-owned"),
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )

        composeRule.setContent {
            OpraEqTheme(theme.value) {
                TestMyEqsScreen(
                    managedHeadphones = listOf(headphone),
                    savedEqs = listOf(personal),
                    savedGeneralEqs = listOf(general),
                )
            }
        }

        listOf("Saved Headphones", "Studio One", "Saved General EQs", "Soft room correction", "Personal EQs", "Warm personal EQ")
            .forEach { label -> composeRule.onNodeWithText(label).performScrollTo().assertIsDisplayed() }
        scrollToTextInLazyContent("Saved Headphones")
        composeRule.onNodeWithText("Saved Headphones").performScrollTo()
        composeRule.waitForIdle()
        captureV080Screenshot("my-eqs-content-light-100")

        composeRule.runOnIdle { theme.value = ThemeMode.Dark }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Saved Headphones").performScrollTo().assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("my-eqs-content-dark-100")
        composeRule.onNodeWithText("Personal EQs").performScrollTo().assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("my-eqs-content-dark-personal-100")
    }

    @Test
    fun personalEqImportRequiresValidParseAndMovesThroughReviewBeforeSave() {
        var savedRecord: SavedEqRecord? = null
        val importedProfile = OpraEqProfile(
            id = "personal-import",
            productId = "personal-import",
            author = "Local import",
            details = null,
            link = null,
            profileType = "ParametricEQ",
            preampGainDb = null,
            bands = emptyList(),
        )
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestScreenShell(title = "My EQs", showTarget = true) {
            PersonalEqImportScreen(
                onBack = {},
                onSave = { manufacturer, model, name, _, _ ->
                    SavedEqRecord(
                        entryId = "import-1",
                        kind = SavedEqKind.Personal,
                        sourceProfileId = null,
                        productId = "personal-import",
                        manufacturer = manufacturer,
                        model = model,
                        displayName = name,
                        profile = importedProfile,
                        createdAtMillis = 0,
                        updatedAtMillis = 0,
                    )
                },
                onSaved = { savedRecord = it },
                onMessage = {},
            )
                }
            }
        }

        composeRule.onNodeWithText("Step 1 of 5 · Input").assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("personal-eq-input-light-100")
        scrollToTextInLazyContent("Equalizer APO / AutoEq text")
        composeRule.onNodeWithText("Equalizer APO / AutoEq text")
            .performScrollTo()
            .performTextInput("Filter 1: ON PK Fc 100 Hz Gain 1 dB Q 1.0")
        dismissSoftwareKeyboard()
        val parseButton = composeRule.onNodeWithText("Parse EQ text")
        composeRule.waitForIdle()
        captureV080Screenshot("personal-eq-input-parse-action-light-100")
        parseButton.assertIsDisplayed()
        parseButton.performClick()
        scrollToTextInLazyContent("Step 2 of 5 · Parse")
        composeRule.onNodeWithText("Step 2 of 5 · Parse").assertIsDisplayed()
        composeRule.onNodeWithText("1 active filters").assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("personal-eq-parse-light-100")
        scrollToTextInLazyContent("Continue to description")
        composeRule.onNodeWithText("Continue to description").assertIsDisplayed().performClick()
        scrollToTextInLazyContent("Step 3 of 5 · Describe")
        composeRule.onNodeWithText("Step 3 of 5 · Describe").assertIsDisplayed()
        scrollToTextInLazyContent("Manufacturer")
        composeRule.onNodeWithText("Manufacturer").performTextInput("Fixture")
        scrollToTextInLazyContent("Headphone model")
        composeRule.onNodeWithText("Headphone model").performTextInput("Model One")
        scrollToTextInLazyContent("EQ name")
        composeRule.onNodeWithText("EQ name").performTextInput("Warm")
        dismissSoftwareKeyboard()
        composeRule.waitForIdle()
        captureV080Screenshot("personal-eq-describe-light-100")
        scrollToTextInLazyContent("Review EQ")
        composeRule.waitForIdle()
        captureV080Screenshot("personal-eq-describe-action-light-100")
        composeRule.onNodeWithText("Review EQ").assertIsDisplayed().assertIsEnabled().performClick()
        composeRule.waitForIdle()
        scrollToTextInLazyContent("Step 4 of 5 · Review")
        composeRule.onNodeWithText("Step 4 of 5 · Review").assertIsDisplayed()
        scrollToTextInLazyContent("Source: pasted by you")
        composeRule.onNodeWithText("Source: pasted by you").assertIsDisplayed()
        scrollToTextInLazyContent("Filter response preview. This view does not adapt the EQ to an output.")
        composeRule.onNodeWithText("Filter response preview. This view does not adapt the EQ to an output.")
            .assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("personal-eq-review-light-100")
        scrollToTextInLazyContent("Continue to save")
        composeRule.onNodeWithText("Continue to save").assertIsDisplayed().performClick()
        scrollToTextInLazyContent("Step 5 of 5 · Save")
        composeRule.onNodeWithText("Step 5 of 5 · Save").assertIsDisplayed()
        scrollToTextInLazyContent("Ready to save")
        composeRule.onNodeWithText("Ready to save").assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("personal-eq-save-light-100")
        scrollToTextInLazyContent("Save to My EQs")
        composeRule.onNodeWithText("Save to My EQs").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals("Warm", savedRecord?.displayName)
            assertEquals(SavedEqKind.Personal, savedRecord?.kind)
        }
        composeRule.onNodeWithText("Ready to save").assertIsDisplayed()
    }

    @Test
    fun malformedPersonalEqCannotContinueToDescription() {
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestScreenShell(title = "My EQs", showTarget = true) {
                    PersonalEqImportScreen(
                        onBack = {},
                        onSave = { _, _, _, _, _ -> error("Invalid text must not be saved") },
                        onSaved = {},
                        onMessage = {},
                    )
                }
            }
        }

        scrollToTextInLazyContent("Equalizer APO / AutoEq text")
        composeRule.onNodeWithText("Equalizer APO / AutoEq text")
            .performScrollTo()
            .performTextInput("Filter 1: ON LP Fc 12000 Hz Q 0.7")
        dismissSoftwareKeyboard()
        val parseButton = composeRule.onNodeWithText("Parse EQ text")
        parseButton.assertIsDisplayed()
        parseButton.performClick()
        composeRule.waitForIdle()
        captureV080Screenshot("personal-eq-invalid-light-100")
        composeRule.onNodeWithText("Continue to description").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Parse result").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("unsupported active filter type LP", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun headphoneSearchShowsDirectModelResultsAndRestoresQueryAfterOpeningResult() {
        val expectedProfileId = testCatalog().profiles.single().id
        var savedProfileIds = emptySet<String>()
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestBrowseScreen(
                    catalog = testCatalog(),
                    onSaveSelection = { _, profileIds, _ -> savedProfileIds = profileIds },
                )
            }
        }

        composeRule.onNodeWithText("WH-1000XM4").assertDoesNotExist()
        composeRule.onNodeWithText("Search headphones…").performClick().performTextInput("XM4")
        composeRule.onNodeWithText("WH-1000XM4").assertIsDisplayed()
        composeRule.onNodeWithText("Sony").assertIsDisplayed()
        composeRule.onNodeWithText("1 profile").assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("headphone-search-results-light-100")
        composeRule.onNodeWithText("WH-1000XM4").performClick()
        composeRule.onNodeWithText("Database").assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("headphone-profile-selection-light-100")
        composeRule.onNodeWithText("Select all").performClick()
        composeRule.waitForIdle()
        captureV080Screenshot("headphone-profile-selection-selected-light-100")
        composeRule.onNodeWithContentDescription("Details for Test creator")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Not currently in My EQs").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Filter response preview").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Target fit: Exact").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Add to My EQs").performScrollTo().assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("headphone-profile-detail-action-light-100")
        composeRule.onNodeWithText("Technical details").performScrollTo().assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("headphone-profile-technical-light-100")
        composeRule.onNodeWithText("Add to My EQs").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertTrue(expectedProfileId in savedProfileIds) }
        composeRule.onNodeWithText("Sony").performClick()
        composeRule.onNodeWithText("XM4").assertIsDisplayed()
        composeRule.onNodeWithText("WH-1000XM4").assertIsDisplayed()
    }

    @Test
    fun headphoneManufacturerBrowseOpensModelAndProfileWithoutSearch() {
        composeRule.setContent { OpraEqTheme(ThemeMode.Light) { TestBrowseScreen(testCatalog()) } }

        composeRule.onNodeWithText("WH-1000XM4").assertDoesNotExist()
        composeRule.onNodeWithText("Search headphones…").assertIsDisplayed()
        composeRule.onNodeWithText("Sony").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Sony").assertIsDisplayed()
        composeRule.onNodeWithText("WH-1000XM4").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Database").assertIsDisplayed()
        scrollToTextInLazyContent("Test creator")
        composeRule.onNodeWithText("Test creator").assertIsDisplayed()
    }

    @Test
    fun headphoneSearchRestoresQueryFilterAndScrolledResultAfterBack() {
        val base = testCatalog()
        val products = (0..17).map { index ->
            base.products.single().copy(
                id = "sony-xm4-$index",
                name = "WH-1000XM4 model ${index.toString().padStart(2, '0')}",
            )
        }
        val profiles = products.mapIndexed { index, product ->
            base.profiles.single().copy(
                id = "sony-xm4-profile-$index",
                productId = product.id,
                details = "Database: DB $index · Target: Harman",
            )
        }
        val catalog = base.copy(products = products, profiles = profiles)
        val lastModel = products.last().name

        composeRule.setContent { OpraEqTheme(ThemeMode.Light) { TestBrowseScreen(catalog) } }
        composeRule.onNodeWithText("Search headphones…").performClick().performTextInput("XM4")
        scrollToTextInLazyContent(lastModel)
        composeRule.onNodeWithText(lastModel).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Target").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Harman").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Sony").performClick()
        composeRule.onNodeWithText("XM4").assertIsDisplayed()
        composeRule.onNodeWithText(lastModel).assertIsDisplayed()
        composeRule.onNodeWithText(lastModel).performClick()
        composeRule.onNodeWithText("Target ✓").assertIsDisplayed()
    }

    @Test
    fun impossibleHeadphoneSearchShowsNoResultsState() {
        composeRule.setContent { OpraEqTheme(ThemeMode.Light) { TestBrowseScreen(testCatalog()) } }
        composeRule.onNodeWithText("Search headphones…")
            .performClick()
            .performTextInput("no-such-headphone-model")
        composeRule.onNodeWithText("No headphones found").assertIsDisplayed()
        composeRule.onNodeWithText("No headphone EQs available").assertDoesNotExist()
    }

    @Test
    fun incompleteTrustedProfileRemainsVisibleAndCannotBeSaved() {
        val catalog = testCatalog().let { base ->
            base.copy(profiles = listOf(base.profiles.single().copy(bands = null)))
        }
        composeRule.setContent { OpraEqTheme(ThemeMode.Light) { TestBrowseScreen(catalog) } }

        composeRule.onNodeWithText("Search headphones…").performClick().performTextInput("XM4")
        composeRule.onNodeWithText("WH-1000XM4").assertIsDisplayed().performClick()
        scrollToTextInLazyContent("Test creator")
        composeRule.onNodeWithContentDescription("Details for Test creator").assertIsDisplayed().performClick()
        assertEquals(
            2,
            composeRule.onAllNodesWithText("Source data unavailable for selection").fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithText("Add to My EQs").assertIsNotEnabled()
    }

    @Test
    fun generalEqBatchActionsAppearOnlyInExplicitSelectionMode() {
        var saved = emptyList<GeneralEqPreset>()
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
            TestBrowseScreen(
                catalog = testCatalog(),
                onSaveGeneralPresets = { presets ->
                    saved = presets
                    true
                },
            )
            }
        }

        composeRule.onNodeWithText("General EQs").performClick()
        composeRule.onNodeWithText("Warm lift").assertIsDisplayed()
        composeRule.onNodeWithText("Test creator · Gentle bass lift").assertIsDisplayed()
        composeRule.onNodeWithText("Select EQs").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("About General EQs").assertIsDisplayed()
        composeRule.onNodeWithText("About").assertDoesNotExist()
        composeRule.onNodeWithText("Select all").assertDoesNotExist()
        composeRule.onNodeWithText("Clear selection").assertDoesNotExist()
        composeRule.waitForIdle()
        captureV080Screenshot("general-eq-browse-light-100")

        val generalSearchBounds = composeRule.onNodeWithText("Search General EQs…")
            .fetchSemanticsNode().boundsInRoot
        val generalTabBounds = composeRule.onNodeWithText("General EQs")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(generalSearchBounds.bottom <= generalTabBounds.top)
        composeRule.onNodeWithContentDescription("About General EQs").performClick()
        composeRule.onNodeWithText("About General EQs").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()

        composeRule.onNodeWithText("Select EQs").performClick()
        composeRule.onNodeWithText("Select all").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        captureV080Screenshot("general-eq-selection-light-100")
        composeRule.onNodeWithText("Save selected (1)").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals("general-warm", saved.single().id) }
        composeRule.onNodeWithText("Select EQs").assertIsDisplayed()
    }

    @Test
    fun settingsKeepsApprovedGroupsAndExplainsOutputAndLocalOwnership() {
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
            TestScreenShell(title = "Settings") {
            SettingsScreen(
                appPreferences = AppPreferences(),
                catalogState = CatalogState.Loading,
                onRefreshCatalog = {},
                onChangeExportFolder = {},
                onCheckForUpdates = {},
                onWhatsNew = {},
                onGetUpdate = {},
                onOpenUrl = {},
                onThemeModeChange = {},
                onOutputBehaviorChange = {},
                onExportTargetChange = { _, _ -> },
                onActiveExportTargetChange = {},
                onDirectBlackPearlFlashEnabledChange = {},
                onDirectFiioJa11FlashEnabledChange = {},
                onDirectEw300FlashEnabledChange = {},
                hiddenCanonicalProfileIds = emptySet(),
                onUnhideCanonicalProfiles = {},
                onMessage = {},
            )
            }
            }
        }

        composeRule.waitForIdle()
        captureV080Screenshot("settings-light-100")
        listOf("Output", "Library", "Appearance", "Updates", "Support & information").forEach { title ->
            composeRule.onNodeWithText(title).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("This controls how EQs are prepared, exported, and flashed. It does not change Android's audio output.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Help").performScrollTo().performClick()
        composeRule.onNodeWithText("Saving does not export a file or change connected hardware.", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("settings-help-light-100")
    }

    @Test
    fun whatsNewFormatsMarkdownInsteadOfExposingMarkup() {
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
            TestScreenShell(title = "EQ Library", showTarget = true) {
            TestBrowseContent(testCatalog())
            WhatsNewDialog(
                version = "0.8.0-beta",
                notes = "# What's New\n\n- **Fixed navigation**\n- [Read the guide](https://example.invalid/guide)",
                onDismiss = {},
            )
            }
            }
        }

        composeRule.onNodeWithText("What’s new in v0.8.0-beta").assertIsDisplayed()
        composeRule.onNodeWithText("What's New").assertIsDisplayed()
        composeRule.onNodeWithText("Fixed navigation").assertIsDisplayed()
        composeRule.onNodeWithText("Read the guide").assertIsDisplayed()
        composeRule.onNodeWithText("**Fixed navigation**").assertDoesNotExist()
        composeRule.waitForIdle()
        captureV080Screenshot("whats-new-light-100")
    }

    @Test
    fun uappExportReviewShowsCompatibilityAndRequiresExplicitExport() {
        var exportCount = 0
        val profile = testCatalog().profiles.single()
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
            TestScreenShell(title = "EQ Library", showTarget = true) {
            TestBrowseContent(testCatalog())
            ExportReviewDialog(
                device = ExportDevice.UAPP,
                items = listOf(ExportReviewItem("WH-1000XM4 · Test creator", profile)),
                onDismiss = {},
                onExport = { exportCount += 1 },
            )
            }
            }
        }

        composeRule.onNodeWithText("Export for UAPP").assertIsDisplayed()
        composeRule.onNodeWithText("Exact", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("1 source band", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("EQ Library does not route Android audio.", substring = true)
            .assertIsDisplayed()
        composeRule.waitForIdle()
        captureV080Screenshot("uapp-export-review-light-100")
        composeRule.onNodeWithText("Export XML").performClick()
        composeRule.runOnIdle { assertEquals(1, exportCount) }
    }

    @Test
    fun uappExportReviewLabelsExactOptimizedAndUnsuitableProfiles() {
        val exact = testCatalog().profiles.single().copy(author = "Exact creator")
        val optimized = exact.copy(
            id = "optimized-profile",
            author = "Optimized creator",
            bandOrderProvenance = com.weekssa.opraeqforuapp.domain.catalog.EqBandOrderProvenance.OPRA_SOURCE_PRIORITY,
            bands = (1..14).map { index -> OpraBand("peak_dip", 100.0 * index, 0.5, 1.0, null) },
        )
        val unsuitable = optimized.copy(
            id = "unsuitable-profile",
            author = "Unsuitable creator",
            bandOrderProvenance = null,
        )
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestScreenShell(title = "EQ Library", showTarget = true) {
                    ExportReviewDialog(
                        device = ExportDevice.UAPP,
                        items = listOf(
                            ExportReviewItem("Exact example", exact),
                            ExportReviewItem("Optimized example", optimized),
                            ExportReviewItem("Unsuitable example", unsuitable),
                        ),
                        onDismiss = {},
                        onExport = { error("Review must not export automatically") },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Exact · 1 source band", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Optimized · 14 source bands", substring = true).assertIsDisplayed()
        scrollToTextInLazyContent("Not suitable · 14 source bands")
        composeRule.onNodeWithText("Not suitable · 14 source bands", substring = true).assertIsDisplayed()
    }

    @Test
    fun hardwareOnlyTargetNeverShowsAFileExportStory() {
        composeRule.setContent {
            OpraEqTheme(ThemeMode.Light) {
                TestScreenShell(title = "EQ Library", showTarget = true) {
                    TestBrowseContent(testCatalog())
                    ExportReviewDialog(
                        device = ExportDevice.FIIO_JA11,
                        items = listOf(ExportReviewItem("WH-1000XM4 · Test creator", testCatalog().profiles.single())),
                        onDismiss = {},
                        onExport = { error("JA11 has no verified file-export action") },
                    )
                }
            }
        }

        composeRule.onNodeWithText("File export unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("no verified import file", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("creating FiiO JA11 files", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Export .txt").assertDoesNotExist()
    }

    @androidx.compose.runtime.Composable
    private fun TestMyEqsScreen(
        managedHeadphones: List<ManagedHeadphoneRecord>,
        savedEqs: List<SavedEqRecord>,
        savedGeneralEqs: List<SavedGeneralEqRecord>,
    ) {
        TestScreenShell(title = "My EQs", showTarget = true) {
        MyEqsHomeScreen(
            managedHeadphones = managedHeadphones,
            savedEqs = savedEqs,
            savedGeneralEqs = savedGeneralEqs,
            activeOutput = ExportDevice.UAPP,
            exportCurrentness = ExportCurrentness(),
            directBlackPearlFlashEnabled = false,
            blackPearlConnectionState = BlackPearlConnectionState.Disconnected,
            directFiioJa11FlashEnabled = false,
            fiioJa11ConnectionState = Kt02h20ConnectionState.Disconnected,
            directJcallyJm12FlashEnabled = false,
            jcallyJm12ConnectionState = Kt02h20ConnectionState.Disconnected,
            onBrowseLibrary = {},
            onExportAll = {},
            onOpenHeadphone = {},
            onImportPersonal = { _, _, _, _, _ -> error("Import was not submitted") },
            onDeleteSavedEq = {},
            onExportSavedEq = {},
            onFlashSavedEq = { "" },
            onRemoveGeneralEq = {},
            onExportGeneralEq = {},
            onFlashGeneralEq = { "" },
            onMessage = {},
        )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @androidx.compose.runtime.Composable
    private fun TestScreenShell(
        title: String,
        showTarget: Boolean = false,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        Scaffold(
            topBar = { TopAppBar(title = { Text(title) }) },
            bottomBar = {
                NavigationBar {
                    listOf("My EQs", "EQ Library", "Settings").forEach { destination ->
                        NavigationBarItem(
                            selected = title == destination,
                            onClick = {},
                            icon = {
                                Icon(
                                    imageVector = when (destination) {
                                        "My EQs" -> Icons.Outlined.Star
                                        "EQ Library" -> Icons.Outlined.Explore
                                        else -> Icons.Outlined.Settings
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = { Text(destination) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().padding(innerPadding)) {
                if (showTarget) {
                    TargetContextSelector(
                        activeTarget = ExportDevice.UAPP,
                        enabledTargets = ExportDevice.selectableOutputs,
                        onTargetChange = {},
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TestBrowseScreen(
        catalog: OpraCatalog,
        onSaveGeneralPresets: suspend (List<GeneralEqPreset>) -> Boolean = { false },
        onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit = { _, _, _ -> },
    ) {
        TestScreenShell(title = "EQ Library", showTarget = true) {
            TestBrowseContent(catalog, onSaveGeneralPresets, onSaveSelection)
        }
    }

    @androidx.compose.runtime.Composable
    private fun TestBrowseContent(
        catalog: OpraCatalog,
        onSaveGeneralPresets: suspend (List<GeneralEqPreset>) -> Boolean = { false },
        onSaveSelection: suspend (String, Set<String>, Boolean) -> Unit = { _, _, _ -> },
    ) {
        BrowseOpraScreen(
            catalogState = CatalogState.Ready(catalog, lastSuccessfulRefreshMillis = 1L),
            profileVisibility = ProfileVisibilityPreferences(),
            exportTargets = ExportTargetPreferences(),
            managedHeadphones = emptyList(),
            favoriteProfileIds = emptySet(),
            onToggleFavorite = { _, _, _ -> FavoriteToggleResult.SAVED },
            onSaveGeneralPresets = onSaveGeneralPresets,
            onLoadManagedHeadphone = { null },
            onSaveSelection = onSaveSelection,
            onRemoveHeadphone = {},
            onDeleteSavedFilesForProfiles = { ids -> PresetCleanupSummary(ids.size, 0, 0) },
            onDeleteSavedFilesForProduct = { PresetCleanupSummary(0, 0, 0) },
            onExportProduct = {},
            onMessage = {},
            onRefreshCatalog = {},
            onOpenUrl = {},
        )
    }

private fun noOpEqLibraryActions(
    onConnectDacForMyDac: (DacDeviceId) -> Unit = {},
    onRefreshCatalog: () -> String = { "Fixture catalog is current." },
    onUnhideCanonicalProfiles: suspend (Set<String>) -> Unit = {},
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
        onApplyFiioJa11Editor = {},
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
        onSetBlackPearlDeviceControl = { _, _ -> },
        onReadFiioJa11DeviceControls = {},
        onSetFiioJa11OutputVolume = {},
        onSetFiioJa11EqProgram = {},
        onSetFiioJa11HeadsetControl = {},
        onSetFiioJa11UacMode = {},
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
        onFlashManagedProfile = { _, _ -> error("Hardware writes are not part of the app-shell test") },
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

private fun fiioJa11CurrentHardwareState(): HardwareEqSnapshotState {
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

private fun fixtureFiioDeviceSnapshot() = FiioJa11DeviceSnapshot(
    sessionGeneration = 1L,
    usbProductId = FiioJa11Protocol.PRODUCT_ID_UAC_2,
    firmwareVersion = "Fixture firmware",
    sampleRateLabel = "48 kHz",
    outputVolume = 24,
    headsetControlEnabled = true,
    eqProgram = FiioJa11Protocol.EqProgram.USER_1,
    uacMode = FiioJa11Protocol.UacMode.UAC_2,
)

private fun fixtureUncertainFiioApplyTrace() = FiioJa11OperationTrace(
    operationId = "synthetic-uncertain-apply",
    operation = "EDITOR_APPLY",
    sourceCommit = "synthetic-ui-fixture",
    appVersion = "0.8.0-beta-test",
    signerVerified = false,
    deviceFingerprintKey = null,
    usbProductId = FiioJa11Protocol.PRODUCT_ID_UAC_2,
    sessionGeneration = 1L,
    detachGeneration = 0L,
    permissionRequestCount = 0L,
    sourceProfileId = "synthetic-profile",
    canonicalPreampGainDb = 0.0,
    generatedOrSelectedTargetGainDb = 0.0,
    quantizedWireTargetGainDb = 0.0,
    readbackGlobalGainDb = null,
    globalGainToleranceDb = 0.1,
    comparisonPhase = "FINAL_READBACK",
    sourceBandCount = FiioJa11Protocol.BAND_COUNT,
    targetBandCount = FiioJa11Protocol.BAND_COUNT,
    fidelity = "Exact",
    usesGeneratedHeadroom = false,
    usedResponseFit = false,
    targetBands = emptyList(),
    saveCommandCount = 1L,
    stateKnown = false,
    outcome = "Uncertain",
    stages = listOf(
        FiioJa11OperationStage.SAVE_SENT_ONCE,
        FiioJa11OperationStage.FINAL_READBACK,
        FiioJa11OperationStage.STATE_UNCERTAIN,
    ),
    events = emptyList(),
    failureReason = "Final readback unavailable in this synthetic screen fixture.",
)

private fun dismissSoftwareKeyboard() {
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
}

private fun scrollToTextInLazyContent(text: String) {
    val target = hasText(text, substring = true)
    val scrollContainers = composeRule.onAllNodes(hasScrollAction(), useUnmergedTree = true)
    val scrollContainerCount = scrollContainers.fetchSemanticsNodes().size
    repeat(scrollContainerCount) { index ->
        runCatching { scrollContainers[index].performScrollToNode(target) }
        if (runCatching {
                composeRule.onNodeWithText(text, substring = true).assertIsDisplayed()
            }.isSuccess
        ) {
            return
        }
    }
    composeRule.onNodeWithText(text, substring = true).assertIsDisplayed()
}

private fun scrollToDisplayedNode(target: SemanticsMatcher) {
    val scrollContainers = composeRule.onAllNodes(hasScrollAction(), useUnmergedTree = true)
    repeat(scrollContainers.fetchSemanticsNodes().size) { index ->
        runCatching { scrollContainers[index].performScrollToNode(target) }
        if (runCatching { composeRule.onNode(target).assertIsDisplayed() }.isSuccess) return
    }
    composeRule.onNode(target).assertIsDisplayed()
}

private fun fiioJa11EditorWorkingCopy(): HardwareEqEditWorkingCopy {
    return editorWorkingCopy(DacDeviceId.FIIO_JA11, 5)
}

private fun editorWorkingCopy(deviceId: DacDeviceId, bandCount: Int): HardwareEqEditWorkingCopy {
    val filters = (0 until bandCount).map { index ->
        HardwareEqFilter(
            index = index,
            enabled = index == 0,
            type = EqFilterType.PEAK,
            frequencyHz = 100.0 * (index + 1),
            gainDb = if (index == 0) 4.0 else 0.0,
            q = 1.0,
        )
    }
    val snapshot = HardwareEqSnapshot(
        deviceId = deviceId,
        sessionGeneration = 1L,
        filters = filters,
        dedicatedEqPreampDb = 0.0,
        verifiedAtEpochMillis = 1L,
    )
    val fingerprint = HardwareEqNativeFingerprint(
        deviceId = deviceId,
        eqEnabled = true,
        bands = filters.map { filter ->
            HardwareEqNativeBandFingerprint(
                index = filter.index,
                enabled = filter.enabled,
                type = filter.type,
                frequencyUnits = filter.frequencyHz.toLong(),
                gainUnits = (filter.gainDb * 10.0).toLong(),
                qUnits = (filter.q * 100.0).toLong(),
            )
        },
        dedicatedEqPreampUnits = 0L,
    )
    val currentSnapshot = HardwareEqSnapshotState().publishCurrent(
        HardwareEqSnapshotBundle(snapshot, fingerprint),
    )
    val spec = when (deviceId) {
        DacDeviceId.TRN_BLACK_PEARL -> HardwareEqEditSpecs.TRN_BLACK_PEARL
        DacDeviceId.FIIO_JA11 -> HardwareEqEditSpecs.FIIO_JA11
        DacDeviceId.SIMGOT_EW300 -> HardwareEqEditSpecs.SIMGOT_EW300
        DacDeviceId.JCALLY_JM12_STOCK -> error("Unsupported DAC fixtures are excluded")
    }
    return (HardwareEqEditor.startFromCurrent(
        snapshotState = currentSnapshot,
        spec = spec,
    ) as HardwareEqEditorStartResult.Ready).workingCopy
}

    private fun testCatalog() = OpraCatalog(
        vendors = listOf(OpraVendor(id = "sony", name = "Sony")),
        products = listOf(
            OpraProduct(
                id = "sony-wh-1000xm4",
                vendorId = "sony",
                name = "WH-1000XM4",
                type = "Headphone",
                subtype = "Over-ear",
            ),
        ),
        profiles = listOf(
            OpraEqProfile(
                id = "wh-xm4-profile",
                productId = "sony-wh-1000xm4",
                author = "Test creator",
                details = "Target: Harman",
                link = null,
                profileType = "parametric_eq",
                preampGainDb = -3.0,
                bands = listOf(OpraBand("peak_dip", 100.0, 1.0, 1.0, null)),
            ),
        ),
        generalPresets = listOf(
            GeneralEqPreset(
                id = "general-warm",
                displayName = "Warm lift",
                category = GeneralEqCategory.SOUND,
                creator = "Test creator",
                soundImpactSummary = "Gentle bass lift",
                sourceUrl = "https://example.invalid/source",
                preampGainDb = -1.0,
                bands = emptyList(),
            ),
        ),
    )
}
