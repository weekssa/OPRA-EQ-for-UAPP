package com.weekssa.opraeqforuapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test

class MainActivityLifecycleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun selectedRootSurvivesRepeatedActivityRecreation() {
        val settingsTab = hasText("Settings") and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        composeRule.onNode(settingsTab).performClick()
        composeRule.onNodeWithText("Output").assertIsDisplayed()

        repeat(2) {
            val previousActivity = composeRule.activity
            composeRule.activityRule.scenario.recreate()
            composeRule.waitForIdle()

            assertNotSame(previousActivity, composeRule.activity)
            composeRule.onNode(settingsTab).assertIsDisplayed().assertIsSelected()
            composeRule.onNodeWithText("Output").assertIsDisplayed()
        }
    }
}
