package ee.ut.cs.mad.strideset.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
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
                    onAddExercise = {},
                    onAddSet = {},
                    onSetChanged = {},
                    onFinishWorkout = { finished = true },
                    onDiscardWorkout = {},
                    onMinimize = {},
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
                    onAddExercise = {},
                    onAddSet = {},
                    onSetChanged = { changedSet = it },
                    onFinishWorkout = {},
                    onDiscardWorkout = {},
                    onMinimize = {},
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
        var addedExercise: Exercise? = null
        var finishCalled = false

        compose.setContent {
            StrideSetTheme {
                WorkoutScreen(
                    workout = workoutWithExercise(completed = true),
                    exercises = listOf(Exercise(id = 1, name = "Back squat", type = ExerciseType.BARBELL)),
                    onAddExercise = { addedExercise = it },
                    onAddSet = {},
                    onSetChanged = {},
                    onFinishWorkout = { finishCalled = true },
                    onDiscardWorkout = {},
                    onMinimize = {},
                )
            }
        }

        compose.onNodeWithText("+ Add exercise").performClick()
        compose.onAllNodesWithText("Back squat").get(1).performClick()
        compose.onAllNodesWithText("Apply").get(1).performClick()
        compose.onNodeWithText("Finish workout").performClick()

        assertTrue(addedExercise?.id == 1L)
        assertTrue(finishCalled)
    }

    @Test
    fun discardRequiresConfirmation() {
        var discardCalled = false

        compose.setContent {
            StrideSetTheme {
                WorkoutScreen(
                    workout = Workout(name = "Workout", startedAt = Instant.now()),
                    onAddExercise = {},
                    onAddSet = {},
                    onSetChanged = {},
                    onFinishWorkout = {},
                    onDiscardWorkout = { discardCalled = true },
                    onMinimize = {},
                )
            }
        }

        compose.onNodeWithText("Discard", useUnmergedTree = true).performClick()
        compose.onNodeWithText("This workout and its sets will be deleted.").assertIsDisplayed()
        assertFalse(discardCalled)
        compose.onAllNodesWithText("Discard", useUnmergedTree = true).get(1).performClick()

        assertTrue(discardCalled)
    }

    @Test
    fun backShowsFinishDiscardAndContinueChoices() {
        var minimized = false

        compose.setContent {
            StrideSetTheme {
                WorkoutScreen(
                    workout = workoutWithExercise(),
                    onAddExercise = {},
                    onAddSet = {},
                    onSetChanged = {},
                    onFinishWorkout = {},
                    onDiscardWorkout = {},
                    onMinimize = { minimized = true },
                )
            }
        }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Leave this workout?").assertIsDisplayed()
        compose.onNodeWithText("Finish and save").assertIsDisplayed()
        compose.onNodeWithText("Keep workout running in background").assertIsDisplayed()
        compose.onAllNodesWithText("Discard").get(1).assertIsDisplayed()
        compose.onNodeWithText("Continue workout").performClick()
        compose.onNodeWithText("Leave this workout?").assertDoesNotExist()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Keep workout running in background").performClick()
        assertTrue(minimized)
        compose.onNodeWithText("Leave this workout?").assertDoesNotExist()
    }

    @Test
    fun backFinishWithNoCompletedSetsShowsWhyAndKeepsConfirmationOpen() {
        var finishCalled = false

        compose.setContent {
            StrideSetTheme {
                WorkoutScreen(
                    workout = workoutWithExercise(),
                    onAddExercise = {},
                    onAddSet = {},
                    onSetChanged = {},
                    onFinishWorkout = { finishCalled = true },
                    onDiscardWorkout = {},
                    onMinimize = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Finish and save").performClick()
        compose.onAllNodesWithText("Complete at least one set before finishing.").get(0).assertIsDisplayed()
        compose.onNodeWithText("Leave this workout?").assertIsDisplayed()
        assertFalse(finishCalled)
    }

    @Test
    fun backFinishWithCompletedSetCallsFinishCallback() {
        var finishCalled = false

        compose.setContent {
            StrideSetTheme {
                WorkoutScreen(
                    workout = workoutWithExercise(completed = true),
                    onAddExercise = {},
                    onAddSet = {},
                    onSetChanged = {},
                    onFinishWorkout = { finishCalled = true },
                    onDiscardWorkout = {},
                    onMinimize = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Finish and save").performClick()

        assertTrue(finishCalled)
        compose.onNodeWithText("Leave this workout?").assertDoesNotExist()
    }

    @Test
    fun backDiscardRequiresConfirmation() {
        var discardCalled = false

        compose.setContent {
            StrideSetTheme {
                WorkoutScreen(
                    workout = workoutWithExercise(),
                    onAddExercise = {},
                    onAddSet = {},
                    onSetChanged = {},
                    onFinishWorkout = {},
                    onDiscardWorkout = { discardCalled = true },
                    onMinimize = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onAllNodesWithText("Discard").get(1).performClick()

        compose.onNodeWithText("Discard workout?").assertIsDisplayed()
        assertFalse(discardCalled)
        compose.onAllNodesWithText("Discard").get(1).performClick()

        assertTrue(discardCalled)
        compose.onNodeWithText("Leave this workout?").assertDoesNotExist()
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
