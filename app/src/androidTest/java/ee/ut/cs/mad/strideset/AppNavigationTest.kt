package ee.ut.cs.mad.strideset

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun workoutOpensItsPlaceholderAndBackReturnsHome() {
        compose.onNodeWithText("Start workout").performClick()
        compose.onNodeWithText("Choose workout").assertIsDisplayed()
        compose.onNodeWithText("This screen is coming next.").assertIsDisplayed()
        compose.onNodeWithText("Calendar").assertDoesNotExist()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Start workout").assertIsDisplayed()
    }

    @Test
    fun runOpensItsPlaceholderAndAndroidBackReturnsHome() {
        compose.onNodeWithText("Start run").performClick()
        compose.onNodeWithText("Run").assertIsDisplayed()
        compose.onNodeWithText("Calendar").assertDoesNotExist()
        pressAndroidBack()
        compose.onNodeWithText("Start run").assertIsDisplayed()
    }

    @Test
    fun switchingAndReselectingTabsDoesNotAccumulateBackHistory() {
        tab("Calendar").performClick()
        heading("Calendar").assertIsDisplayed()
        tab("Calendar").assertIsSelected()
        tab("Settings").performClick()
        tab("Settings").performClick()
        heading("Settings").assertIsDisplayed()
        tab("Settings").assertIsSelected()
        pressAndroidBack()
        compose.onNodeWithText("Start workout").assertIsDisplayed()
        tab("Start").assertIsSelected()
    }

    @Test
    fun currentDestinationSurvivesActivityRecreation() {
        tab("Settings").performClick()
        compose.activityRule.scenario.recreate()
        heading("Settings").assertIsDisplayed()
        tab("Settings").assertIsSelected()
        tab("Start").performClick()
        compose.onNodeWithText("Start workout").assertIsDisplayed()
    }

    private fun tab(label: String) = compose.onNode(
        hasText(label) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
    )

    private fun heading(label: String) = compose.onNode(
        hasText(label) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
    )

    private fun pressAndroidBack() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }
}
