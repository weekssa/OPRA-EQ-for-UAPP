package com.weekssa.opraeqforuapp.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.espresso.Espresso
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import kotlinx.coroutines.awaitCancellation
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BlackPearlDeviceResetLifecycleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun interruptedEqResetIsNotReplayedAndLeavesAnActionableRecoveryState() {
        val restorationTester = StateRestorationTester(composeRule)
        val state = BlackPearlQualificationUiState().success(
            BlackPearlDeviceQualificationSnapshot(
                sessionGeneration = 7L,
                firmwareVersion = "BP-1.2.3",
                filterCode = 2,
                gainModeCode = 1,
                ampTopologyCode = 1,
                micGainDb = 0,
                leftBalanceDb = 0,
                rightBalanceDb = 0,
                playbackGainRaw = -1024,
            ),
        )
        var eqResetRequests = 0
        var deviceWriteRequests = 0
        var eqReadRequests = 0
        var deviceReadRequests = 0
        val operationStatuses = mutableListOf<Pair<String, Boolean>>()
        lateinit var inputModeManager: InputModeManager

        restorationTester.setContent {
            val systemDensity = LocalDensity.current
            inputModeManager = LocalInputModeManager.current
            CompositionLocalProvider(LocalDensity provides Density(systemDensity.density, fontScale = 2f)) {
                OpraEqTheme(ThemeMode.Light) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        val destinationStateHolder = rememberSaveableStateHolder()
                        val tabStateHolder = rememberSaveableStateHolder()
                        var showDeviceRoot by rememberSaveable { mutableStateOf(true) }
                        var selectedTestTab by rememberSaveable { mutableStateOf("DEVICE") }
                        TextButton(onClick = { showDeviceRoot = !showDeviceRoot }) {
                            androidx.compose.material3.Text("Toggle My DAC root")
                        }
                        TextButton(onClick = { selectedTestTab = "EQ" }) {
                            androidx.compose.material3.Text("EQ test tab")
                        }
                        TextButton(onClick = { selectedTestTab = "DEVICE" }) {
                            androidx.compose.material3.Text("DEVICE test tab")
                        }
                        if (showDeviceRoot) {
                            destinationStateHolder.SaveableStateProvider("MyDac") {
                                if (selectedTestTab == "DEVICE") {
                                    tabStateHolder.SaveableStateProvider("black-pearl-device-reset") {
                                        BlackPearlDeviceResetSection(
                                            state = state,
                                            hardwareEqState = HardwareEqSnapshotState(),
                                            enabled = true,
                                            onSetDeviceControl = { _, _ -> deviceWriteRequests += 1 },
                                            onResetEqToFlat = {
                                                eqResetRequests += 1
                                                awaitCancellation()
                                            },
                                            onReadCurrentEq = { eqReadRequests += 1 },
                                            onRefreshDevice = { deviceReadRequests += 1 },
                                            onMessage = {},
                                            onOperationStatus = { message, running ->
                                                operationStatuses += message to running
                                            },
                                        )
                                    }
                                } else {
                                    androidx.compose.material3.Text("EQ tab content")
                                }
                            }
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("Restore defaults").performScrollTo().performClick()
        composeRule.onNodeWithText("Also reset EQ to flat").performClick()
        composeRule.onNodeWithText("Reset", substring = false).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { eqResetRequests == 1 }

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("Reset outcome needs review").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "The reset may have changed EQ or DEVICE settings.",
            substring = true,
        ).performScrollTo().assertIsDisplayed()

        AccessibilityChecks.enable()
            .setRunChecksFromRootView(true)
            .setSuppressingResultMatcher(object : TypeSafeMatcher<AccessibilityViewCheckResult>() {
                override fun describeTo(description: Description) {
                    description.appendText("Compose host wrapper speakable-text result")
                }

                override fun matchesSafely(item: AccessibilityViewCheckResult): Boolean =
                    item.view?.javaClass?.name == "androidx.compose.ui.platform.AndroidComposeView" &&
                        item.accessibilityHierarchyCheck == SpeakableTextPresentCheck::class.java
            })
        try {
            Espresso.onView(isRoot()).check(AccessibilityChecks.accessibilityAssertion())
        } finally {
            AccessibilityChecks.disable()
        }

        assertEquals("the in-flight EQ reset callback is not invoked again", 1, eqResetRequests)
        assertEquals("the device-default sequence never starts after the interrupted EQ reset", 0, deviceWriteRequests)
        assertTrue(operationStatuses.any { (message, running) ->
            !running && message.contains("status needs review")
        })
        composeRule.onNodeWithText("Restore defaults").assertHasNoClickAction()

        composeRule.runOnIdle {
            assertTrue("Keyboard input mode is available", inputModeManager.requestInputMode(InputMode.Keyboard))
        }
        composeRule.onNodeWithText("Read current EQ")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        sendAndroidKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER)
        composeRule.waitUntil(timeoutMillis = 5_000) { eqReadRequests == 1 }
        composeRule.onNodeWithText("No verified hardware EQ read yet.").performScrollTo().assertIsDisplayed()
        val refreshDevice = composeRule.onNodeWithText("Refresh DEVICE").performScrollTo()
        var tabKeyAttempts = 0
        while (!isFocused(refreshDevice) && tabKeyAttempts < 8) {
            sendAndroidKeyEvent(KeyEvent.KEYCODE_TAB)
            tabKeyAttempts += 1
        }
        refreshDevice.assertIsFocused()
        sendAndroidKeyEvent(KeyEvent.KEYCODE_ENTER)
        composeRule.waitUntil(timeoutMillis = 5_000) { deviceReadRequests == 1 }
        composeRule.runOnIdle {
            assertEquals(1, deviceReadRequests)
            assertEquals(1, eqResetRequests)
            assertEquals(0, deviceWriteRequests)
        }
        composeRule.onNodeWithText("EQ test tab").performScrollTo().performClick()
        composeRule.onNodeWithText("EQ tab content").assertIsDisplayed()
        composeRule.onNodeWithText("DEVICE test tab").performScrollTo().performClick()
        composeRule.onNodeWithText("Reset outcome needs review").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Restore defaults").assertHasNoClickAction()
        composeRule.onNodeWithText("Toggle My DAC root").performScrollTo().performClick()
        composeRule.onAllNodesWithText("EQ tab content").assertCountEquals(0)
        composeRule.onAllNodesWithText("Reset outcome needs review").assertCountEquals(0)
        composeRule.onNodeWithText("Toggle My DAC root").performScrollTo().performClick()
        composeRule.onNodeWithText("Reset outcome needs review").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Restore defaults").assertHasNoClickAction()
        composeRule.runOnIdle {
            assertEquals("reset is not replayed across tab/root removal", 1, eqResetRequests)
            assertEquals("read actions are not replayed across tab/root removal", 1, eqReadRequests)
            assertEquals("device refresh is not replayed across tab/root removal", 1, deviceReadRequests)
            assertEquals("no DEVICE write is replayed across tab/root removal", 0, deviceWriteRequests)
        }
    }

    private fun sendAndroidKeyEvent(keyCode: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode)
        composeRule.waitForIdle()
    }

    private fun isFocused(node: androidx.compose.ui.test.SemanticsNodeInteraction): Boolean =
        runCatching {
            node.fetchSemanticsNode().config[SemanticsProperties.Focused]
        }.getOrDefault(false)
}
