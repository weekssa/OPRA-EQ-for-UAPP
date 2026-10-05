package com.weekssa.opraeqforuapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import kotlinx.coroutines.awaitCancellation
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
        val messages = mutableListOf<String>()
        val operationStatuses = mutableListOf<Pair<String, Boolean>>()

        restorationTester.setContent {
            OpraEqTheme(ThemeMode.Light) {
                Column(Modifier.fillMaxSize()) {
                    BlackPearlDeviceResetSection(
                        state = state,
                        enabled = true,
                        onSetDeviceControl = { _, _ -> deviceWriteRequests += 1 },
                        onResetEqToFlat = {
                            eqResetRequests += 1
                            awaitCancellation()
                        },
                        onMessage = messages::add,
                        onOperationStatus = { message, running ->
                            operationStatuses += message to running
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Restore defaults").performClick()
        composeRule.onNodeWithText("Also reset EQ to flat").performClick()
        composeRule.onNodeWithText("Reset", substring = false).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { eqResetRequests == 1 }

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            messages.any { it.contains("stopped before verification completed") }
        }

        assertEquals("the in-flight EQ reset callback is not invoked again", 1, eqResetRequests)
        assertEquals("the device-default sequence never starts after the interrupted EQ reset", 0, deviceWriteRequests)
        assertTrue(operationStatuses.any { (message, running) ->
            !running && message.contains("Refresh DEVICE")
        })
    }
}
