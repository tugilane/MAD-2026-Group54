package ee.ut.cs.mad.strideset

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.model.UnitSettings
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import ee.ut.cs.mad.strideset.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsPersistenceTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private lateinit var repository: SettingsRepository
    private lateinit var originalSettings: UnitSettings

    @Before
    fun setUp() = runBlocking {
        repository = (compose.activity.application as StrideSetApplication).container.settingsRepository
        originalSettings = repository.settings.first()
        repository.setDistanceUnit(DistanceUnit.KILOMETERS)
        repository.setWeightUnit(WeightUnit.KILOGRAMS)
    }

    @After
    fun restoreSettings() = runBlocking {
        repository.setDistanceUnit(originalSettings.distanceUnit)
        repository.setWeightUnit(originalSettings.weightUnit)
    }

    @Test
    fun selectionsAreSavedWhenLeavingSettingsAndReturning() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Miles (mi)").performClick()
        compose.onNodeWithText("Pounds (lb)").performClick()
        compose.onNodeWithText("Start").performClick()
        compose.onNodeWithText("Settings").performClick()

        awaitSelected("Miles (mi)")
        awaitSelected("Pounds (lb)")
        runBlocking {
            assertEquals(
                UnitSettings(DistanceUnit.MILES, WeightUnit.POUNDS),
                repository.settings.first()
            )
        }
    }

    @Test
    fun savedSelectionsAreLoadedAndSurviveActivityRecreation() {
        runBlocking {
            repository.setDistanceUnit(DistanceUnit.MILES)
            repository.setWeightUnit(WeightUnit.POUNDS)
        }
        compose.onNodeWithText("Settings").performClick()
        awaitSelected("Miles (mi)")
        awaitSelected("Pounds (lb)")

        compose.activityRule.scenario.recreate()

        awaitSelected("Miles (mi)")
        awaitSelected("Pounds (lb)")
    }

    private fun awaitSelected(label: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText(label) and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(label).assertIsSelected()
    }
}
