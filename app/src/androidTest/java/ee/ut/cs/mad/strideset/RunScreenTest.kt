package ee.ut.cs.mad.strideset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.model.Run
import ee.ut.cs.mad.strideset.running.GpsState
import ee.ut.cs.mad.strideset.running.RunCommand
import ee.ut.cs.mad.strideset.ui.running.RunScreen
import ee.ut.cs.mad.strideset.ui.running.RunUiState
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RunScreenTest {
    @get:Rule val compose = createComposeRule()
    private val run = Run(1, Instant.EPOCH, activeDuration = Duration.ofMinutes(10), distanceMeters = 1609.344)

    @Test fun gpsStatusChangesKeepMetricsAndUseTheSelectedUnits() {
        var state by mutableStateOf(RunUiState(run, loading = false))
        compose.setContent {
            StrideSetTheme { RunScreen(state, DistanceUnit.MILES, {}, {}, {}) }
        }
        compose.onNodeWithText("Searching for GPS…").assertIsDisplayed()
        compose.onNodeWithText("1.00").assertIsDisplayed()
        compose.onNodeWithText("10:00").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(gpsState = GpsState.SIGNAL_LOST) }
        compose.onNodeWithText("GPS signal lost — time is still tracking").assertIsDisplayed()
        compose.onNodeWithText("00:10:00").assertIsDisplayed()
        compose.onNodeWithText("Finish run").assertIsDisplayed()
    }

    @Test fun anOpenDiscardConfirmationSurvivesRecreationAndStillRequiresConsent() {
        val restoration = StateRestorationTester(compose)
        var command: RunCommand? = null
        restoration.setContent {
            StrideSetTheme {
                RunScreen(RunUiState(run.copy(isPaused = true), loading = false), DistanceUnit.KILOMETERS,
                    { command = it }, {}, {})
            }
        }
        compose.onNodeWithText("Discard run").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Discard this run?").assertIsDisplayed()
        assertNull(command)
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Resume").assertIsDisplayed()
        assertNull(command)
        compose.onNodeWithText("Discard run").performClick()
        compose.onNodeWithText("Discard").performClick()
        assertEquals(RunCommand.DISCARD, command)
    }
}
