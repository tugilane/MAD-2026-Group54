package ee.ut.cs.mad.strideset.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.ExerciseType
import ee.ut.cs.mad.strideset.data.model.Workout
import ee.ut.cs.mad.strideset.data.model.WorkoutExercise
import ee.ut.cs.mad.strideset.data.model.WorkoutSet
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkoutCompleteScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsSummaryAndOnlyCompletedSetsGroupedByExercise() {
        compose.setContent {
            StrideSetTheme {
                WorkoutCompleteScreen(
                    workout = finishedWorkout(),
                    onViewCalendar = {},
                    onDone = {},
                )
            }
        }

        compose.onNodeWithText("Workout complete").assertIsDisplayed()
        compose.onNodeWithText("24:18").assertIsDisplayed()
        compose.onNodeWithText("Completed exercises").assertIsDisplayed()
        compose.onNodeWithText("2 sets").assertIsDisplayed()
        compose.onNodeWithText("Back squat").assertIsDisplayed()
        compose.onNodeWithText("8 × 80 kg").assertIsDisplayed()
        compose.onNodeWithText("Pull-up").assertIsDisplayed()
        compose.onNodeWithText("7 reps").assertIsDisplayed()
        compose.onAllNodesWithText("10 × 100 kg").assertCountEquals(0)
    }

    @Test
    fun actionButtonsInvokeTheirCallbacks() {
        var viewedCalendar = false
        var done = false

        compose.setContent {
            StrideSetTheme {
                WorkoutCompleteScreen(
                    workout = finishedWorkout(),
                    onViewCalendar = { viewedCalendar = true },
                    onDone = { done = true },
                )
            }
        }

        compose.onNodeWithText("View calendar").performScrollTo().performClick()
        compose.onNodeWithText("Done").performScrollTo().performClick()

        assertTrue(viewedCalendar)
        assertTrue(done)
    }

    private fun finishedWorkout() = Workout(
        name = "Workout",
        startedAt = Instant.parse("2026-01-01T00:00:00Z"),
        finishedAt = Instant.parse("2026-01-01T00:24:18Z"),
        exercises = listOf(
            WorkoutExercise(
                id = 1,
                exercise = Exercise(id = 1, name = "Back squat", type = ExerciseType.BARBELL),
                sets = listOf(
                    WorkoutSet(id = 1, reps = 8, weightKg = 80.0, isCompleted = true),
                    WorkoutSet(id = 2, reps = 10, weightKg = 100.0, isCompleted = false),
                ),
            ),
            WorkoutExercise(
                id = 2,
                exercise = Exercise(id = 2, name = "Pull-up", type = ExerciseType.BODYWEIGHT),
                sets = listOf(WorkoutSet(id = 3, reps = 7, weightKg = 0.0, isCompleted = true)),
            ),
        ),
    )
}
