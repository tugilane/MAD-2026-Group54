package ee.ut.cs.mad.strideset

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
