package ee.ut.cs.mad.strideset

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.ExerciseType
import ee.ut.cs.mad.strideset.data.model.PlanExercise
import ee.ut.cs.mad.strideset.data.model.PlanSet
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import ee.ut.cs.mad.strideset.data.model.WorkoutPlan
import ee.ut.cs.mad.strideset.data.model.toLb
import ee.ut.cs.mad.strideset.ui.PlanEditorScreen
import ee.ut.cs.mad.strideset.ui.theme.StrideSetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlanSaveTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun newPlanRestoresWhileExerciseLibraryIsLoading() {
        val restoration = StateRestorationTester(compose)
        val exercise = Exercise(1, "Bench press", ExerciseType.BARBELL)
        var restoring = false
        var saved: WorkoutPlan? = null
        restoration.setContent {
            StrideSetTheme {
                PlanEditorScreen(
                    null, WeightUnit.KILOGRAMS,
                    if (restoring) emptyList() else listOf(exercise),
                    onBack = {}, onSave = { saved = it },
                )
            }
        }
        compose.onNodeWithText("e.g. Lower body").performTextReplacement("New plan")
        compose.onNodeWithText("+ Add exercise").performClick()
        compose.onNodeWithText("Bench press").performClick()
        // Only the existing-exercise Apply button is enabled after choosing a library row.
        compose.onNode(hasText("Apply") and isEnabled()).performClick()
        restoring = true
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Bench press").assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        assertEquals(exercise, saved!!.exercises.single().exercise)
    }

    @Test
    fun replacedExerciseRestoresWithoutRevertingToOriginalExercise() {
        val restoration = StateRestorationTester(compose)
        val original = Exercise(1, "Bench press", ExerciseType.BARBELL)
        val replacement = Exercise(2, "Squat", ExerciseType.BARBELL)
        val plan = WorkoutPlan(12, "Original", listOf(PlanExercise(original, listOf(PlanSet(8, 20.0)))))
        var restoring = false
        var saved: WorkoutPlan? = null
        restoration.setContent {
            StrideSetTheme {
                PlanEditorScreen(plan, WeightUnit.KILOGRAMS,
                    if (restoring) emptyList() else listOf(original, replacement),
                    onBack = {}, onSave = { saved = it })
            }
        }
        compose.onNodeWithText("Bench press").performClick()
        compose.onNodeWithText("Squat").performClick()
        compose.onNode(hasText("Apply") and isEnabled()).performClick()
        restoring = true
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Squat").assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        assertEquals(replacement, saved!!.exercises.single().exercise)
        assertEquals(listOf(PlanSet(8, 20.0)), saved!!.exercises.single().sets)
    }

    @Test
    fun openPopupRestoresEditsAndCancelStillDiscardsThem() {
        val restoration = StateRestorationTester(compose)
        val exercise = Exercise(1, "Bench press", ExerciseType.BARBELL)
        val plan = WorkoutPlan(12, "Original", listOf(PlanExercise(exercise, listOf(PlanSet(8, 20.0)))))
        var saved: WorkoutPlan? = null
        restoration.setContent {
            StrideSetTheme {
                PlanEditorScreen(plan, WeightUnit.KILOGRAMS, listOf(exercise),
                    onBack = {}, onSave = { saved = it })
            }
        }
        compose.onNodeWithText("Click for details").performClick()
        compose.onNodeWithContentDescription("Reps").performTextReplacement("12")
        compose.onNodeWithContentDescription("Weight").performTextReplacement("25")
        compose.onNodeWithText("+ Add set").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText("25").assertIsDisplayed()
        compose.onNodeWithText("Set 2").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Click for details").performClick()
        compose.onNodeWithText("12").performTextReplacement("15")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("15").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        assertEquals(listOf(PlanSet(12, 25.0), PlanSet(8)), saved!!.exercises.single().sets)
    }

    @Test
    fun saveUsesPopupEditsAndConvertsPoundsToKilograms() {
        val exercise = Exercise(1, "Bench press", ExerciseType.BARBELL)
        val plan = WorkoutPlan(12, "Original", listOf(PlanExercise(exercise, listOf(PlanSet(8, 20.0)))))
        var saved: WorkoutPlan? = null
        var returned = false
        compose.setContent {
            StrideSetTheme {
                PlanEditorScreen(
                    plan, WeightUnit.POUNDS, listOf(exercise),
                    onBack = { returned = true }, onSave = { saved = it },
                )
            }
        }
        compose.onNodeWithText("Original").performTextReplacement("Updated")
        compose.onNodeWithText("Click for details").performClick()
        compose.onNodeWithText("8").performTextReplacement("12")
        compose.onNodeWithText(WeightUnit.POUNDS.toLb(20.0).toString()).performTextReplacement("100")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        assertTrue(returned)
        assertEquals(12L, saved!!.id)
        assertEquals("Updated", saved!!.name)
        assertEquals(12, saved!!.exercises.single().sets.single().reps)
        assertEquals(45.359237, saved!!.exercises.single().sets.single().weightKg!!, 0.000001)
    }
}
