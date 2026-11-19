package ee.ut.cs.mad.strideset

import android.content.Intent
import android.graphics.Color
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
import ee.ut.cs.mad.strideset.data.repository.EmptyWorkoutException
import ee.ut.cs.mad.strideset.data.repository.ExerciseNameTakenException
import ee.ut.cs.mad.strideset.ui.StrideSetApp
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import ee.ut.cs.mad.strideset.running.RunTrackingService
import ee.ut.cs.mad.strideset.ui.running.RunRoute
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var openRunRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        consumeRunRequest(intent)

        val container = (application as StrideSetApplication).container
        val settingsRepository = container.settingsRepository
        val savedPlans = container.workoutPlanRepository.observePlans()
        val exercises = container.exerciseRepository.observeExercises()
        val workoutRepository = container.workoutRepository
        val activeWorkout = workoutRepository.observeActiveWorkout()

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
                    activeWorkoutFlow = activeWorkout,
                    onStartWorkout = { planId ->
                        try {
                            val current = activeWorkout.first()
                            when {
                                current == null -> workoutRepository.startWorkout(planId)
                                current.exercises.isEmpty() && planId != null ->
                                    workoutRepository.startWorkoutReplacingEmpty(planId, current.id)
                            }
                            true
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            Toast.makeText(this@MainActivity, R.string.workout_start_error, Toast.LENGTH_LONG).show()
                            false
                        }
                    },
                    onDiscardWorkout = { workoutId ->
                        try {
                            workoutRepository.discardWorkout(workoutId)
                            true
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            Toast.makeText(this@MainActivity, R.string.workout_discard_error, Toast.LENGTH_LONG).show()
                            false
                        }
                    },
                    onUpdateWorkoutSet = { set ->
                        try {
                            workoutRepository.updateSet(set)
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            Toast.makeText(this@MainActivity, R.string.workout_update_error, Toast.LENGTH_LONG).show()
                        }
                    },
                    onAddWorkoutExercise = { workoutId, exercise ->
                        try {
                            val savedExercise = if (exercise.id == 0L) {
                                container.exerciseRepository.addCustomExercise(exercise.name, exercise.type)
                            } else exercise
                            val exerciseEntryId = workoutRepository.addExercise(workoutId, savedExercise.id)
                            workoutRepository.addSet(exerciseEntryId, reps = 0, weightKg = 0.0)
                            true
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            val message = if (error is ExerciseNameTakenException) {
                                R.string.exercise_name_taken
                            } else {
                                R.string.workout_add_exercise_error
                            }
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                            false
                        }
                    },
                    onAddWorkoutSet = { exerciseEntryId ->
                        try {
                            workoutRepository.addSet(exerciseEntryId, reps = 0, weightKg = 0.0)
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            Toast.makeText(this@MainActivity, R.string.workout_update_error, Toast.LENGTH_LONG).show()
                        }
                    },
                    onFinishWorkout = { workoutId ->
                        try {
                            workoutRepository.finishWorkout(workoutId)
                            workoutRepository.observeWorkout(workoutId).first()
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            val message = if (error is EmptyWorkoutException) {
                                R.string.workout_finish_requires_completed_set
                            } else {
                                R.string.workout_finish_error
                            }
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                            null
                        }
                    },
                    openRunRequest = openRunRequest,
                    runContent = { back ->
                        RunRoute(
                            activity = this@MainActivity,
                            container = (application as StrideSetApplication).container,
                            distanceUnit = settings.distanceUnit,
                            onBack = back
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
