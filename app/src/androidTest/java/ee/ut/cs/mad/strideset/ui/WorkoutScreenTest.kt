package ee.ut.cs.mad.strideset.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.ExerciseType
import ee.ut.cs.mad.strideset.data.model.Workout
import ee.ut.cs.mad.strideset.data.model.WorkoutExercise
import ee.ut.cs.mad.strideset.data.model.WorkoutSet
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkoutScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyWorkoutShowsZeroCountAndBlocksFinishUntilASetIsCompleted() {
        var finished = false
        val startedAt = Instant.parse("2026-01-01T00:00:00Z")
        val workout = Workout(name = "Workout", startedAt = startedAt)

        compose.setContent {
            StrideSetTheme {
                WorkoutScreen(
                    workout = workout,
                    onBack = {},
                    onAddExercise = {},
                    onSetChanged = {},
                    onFinishWorkout = { finished = true },
                    onDiscardWorkout = {},
                    initialNow = startedAt.plusSeconds(3_661),
                )
            }
        }

        compose.onNodeWithText("0 exercises").assertIsDisplayed()
        compose.onNodeWithText("Empty workout").assertIsDisplayed()
        compose.onNodeWithText("01:01:01").assertIsDisplayed()
        compose.onNodeWithText("Finish workout").performClick()
        compose.onNodeWithText("Complete at least one set before finishing.").assertIsDisplayed()
        assertFalse(finished)
    }

    @Test
    fun populatedWorkoutShowsExerciseRowsAndSetToggleReportsUpdatedSet() {
        var changedSet: WorkoutSet? = null

        compose.setContent {
            StrideSetTheme {
                WorkoutScreen(
                    workout = workoutWithExercise(),
                    onBack = {},
                    onAddExercise = {},
                    onSetChanged = { changedSet = it },
                    onFinishWorkout = {},
                    onDiscardWorkout = {},
                )
            }
        }

        compose.onNodeWithText("1 exercise").assertIsDisplayed()
        compose.onNodeWithText("Back squat").assertIsDisplayed()
        compose.onNodeWithText("Barbell").assertIsDisplayed()
        compose.onNodeWithContentDescription("Repetitions for set 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Weight for set 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mark set 1 complete").performClick()

        assertTrue(changedSet?.isCompleted == true)
    }

    @Test
    fun addExerciseAndFinishActionsAreReported() {
        var addExerciseCalled = false
        var finishCalled = false

        compose.setContent {
            StrideSetTheme {
                WorkoutScreen(
                    workout = workoutWithExercise(completed = true),
                    onBack = {},
                    onAddExercise = { addExerciseCalled = true },
                    onSetChanged = {},
                    onFinishWorkout = { finishCalled = true },
                    onDiscardWorkout = {},
                )
            }
        }

        compose.onNodeWithText("+ Add exercise").performClick()
        compose.onNodeWithText("Finish workout").performClick()

        assertTrue(addExerciseCalled)
        assertTrue(finishCalled)
    }

    @Test
    fun discardRequiresConfirmation() {
        var discardCalled = false

        compose.setContent {
            StrideSetTheme {
                WorkoutScreen(
                    workout = Workout(name = "Workout", startedAt = Instant.now()),
                    onBack = {},
                    onAddExercise = {},
                    onSetChanged = {},
                    onFinishWorkout = {},
                    onDiscardWorkout = { discardCalled = true },
                )
            }
        }

        compose.onNodeWithText("Discard").performClick()
        compose.onNodeWithText("This workout and its sets will be deleted.").assertIsDisplayed()
        assertFalse(discardCalled)
        compose.onNodeWithText("Discard").performClick()

        assertTrue(discardCalled)
    }

    private fun workoutWithExercise(completed: Boolean = false) = Workout(
        name = "Leg day",
        startedAt = Instant.now(),
        exercises = listOf(
            WorkoutExercise(
                id = 1,
                exercise = Exercise(id = 1, name = "Back squat", type = ExerciseType.BARBELL),
                sets = listOf(WorkoutSet(id = 1, reps = 8, weightKg = 80.0, isCompleted = completed)),
            ),
        ),
    )
}
