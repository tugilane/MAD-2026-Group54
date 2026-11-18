package ee.ut.cs.mad.strideset

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.running.RunTrackingService
import org.junit.Rule
import org.junit.Test

class RunNotificationNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun coldNotificationLaunchIsConsumedBeforeActivityRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .putExtra(RunTrackingService.EXTRA_OPEN_RUN, true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            compose.onNodeWithText("Run").assertIsDisplayed()
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Settings").performClick()
            scenario.recreate()
            compose.onNodeWithText("Distance").assertIsDisplayed()
        }
    }
}
