package ee.ut.cs.mad.strideset

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import ee.ut.cs.mad.strideset.data.model.UnitSettings
import ee.ut.cs.mad.strideset.ui.StrideSetApp
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import ee.ut.cs.mad.strideset.running.RunTrackingService
import ee.ut.cs.mad.strideset.ui.running.RunRoute
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var openRunRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeRunRequest(intent)
        val settingsRepository = (application as StrideSetApplication).container.settingsRepository
        val savedPlans = (application as StrideSetApplication).container.workoutPlanRepository.observePlans()
        val exercises = (application as StrideSetApplication).container.exerciseRepository.observeExercises()
        val settingsFlow = settingsRepository.settings.catch { error ->
            if (error is IOException) {
                Toast.makeText(this@MainActivity, R.string.settings_load_error, Toast.LENGTH_LONG).show()
            } else {
                throw error
            }
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            val settings by settingsFlow.collectAsState(initial = UnitSettings())
            StrideSetTheme {
                StrideSetApp(
                    settings = settings,
                    savedPlans = savedPlans,
                    availableExercises = exercises,
                    onSavePlan = { plan ->
                        (application as StrideSetApplication).container.workoutPlanRepository.savePlan(plan)
                    },
                    openRunRequest = openRunRequest,
                    runContent = { back ->
                        RunRoute(
                            activity = this@MainActivity,
                            container = (application as StrideSetApplication).container,
                            distanceUnit = settings.distanceUnit,
                            onBack = back,
                        )
                    },
                    onDistanceUnitSelected = { unit ->
                        saveSetting { settingsRepository.setDistanceUnit(unit) }
                    },
                    onWeightUnitSelected = { unit ->
                        saveSetting { settingsRepository.setWeightUnit(unit) }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeRunRequest(intent)
    }

    private fun consumeRunRequest(intent: Intent?) {
        if (intent?.getBooleanExtra(RunTrackingService.EXTRA_OPEN_RUN, false) == true) {
            // Handle navigation once, so rotation preserves the user's subsequent destination.
            intent.removeExtra(RunTrackingService.EXTRA_OPEN_RUN)
            openRunRequest++
        }
    }

    private fun saveSetting(update: suspend () -> Unit) {
        lifecycleScope.launch {
            try {
                update()
            } catch (error: IOException) {
                Toast.makeText(this@MainActivity, R.string.settings_save_error, Toast.LENGTH_LONG).show()
            }
        }
    }
}
