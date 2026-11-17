package ee.ut.cs.mad.strideset.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.data.local.SeedDefaultExercises
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.PlanExercise
import ee.ut.cs.mad.strideset.data.model.PlanSet
import ee.ut.cs.mad.strideset.data.model.WorkoutPlan
import ee.ut.cs.mad.strideset.data.repository.RoomExerciseRepository
import ee.ut.cs.mad.strideset.data.repository.RoomWorkoutPlanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomWorkoutPlanRepositoryTest {
    private lateinit var db: StrideSetDatabase
    private lateinit var plans: RoomWorkoutPlanRepository
    private lateinit var exercises: List<Exercise>

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, StrideSetDatabase::class.java)
            .addCallback(SeedDefaultExercises)
            .build()
        plans = RoomWorkoutPlanRepository(db.workoutPlanDao())
        exercises = RoomExerciseRepository(db.exerciseDao()).observeExercises().first()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun exercise(name: String) = exercises.first { it.name == name }

    private fun rowCount(table: String): Int =
        db.query("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    // Different weight per set, the same weight on every set, and a bodyweight exercise without weight.
    private fun pushDay() = WorkoutPlan(
        name = "Push day",
        exercises = listOf(
            PlanExercise(exercise("Bench press"), listOf(PlanSet(8, 60.0), PlanSet(8, 62.5), PlanSet(6, 65.0))),
            PlanExercise(exercise("Overhead press"), List(3) { PlanSet(10, 40.0) }),
            PlanExercise(exercise("Dip"), List(2) { PlanSet(12) }),
        ),
    )

    @Test
    fun savedPlanIsReadBackWithSetsInOrder() = runBlocking {
        val id = plans.savePlan(pushDay())

        assertEquals(pushDay().copy(id = id), plans.getPlan(id))
    }

    @Test
    fun editingPlanReplacesNameExercisesAndSets() = runBlocking {
        val id = plans.savePlan(pushDay())
        val edited = WorkoutPlan(
            id = id,
            name = "Push day B",
            exercises = listOf(
                PlanExercise(exercise("Dip"), listOf(PlanSet(10, 10.0))),
                PlanExercise(exercise("Bench press"), List(2) { PlanSet(5, 80.0) }),
            ),
        )

        assertEquals(id, plans.savePlan(edited))
        assertEquals(edited, plans.getPlan(id))
        assertEquals(1, plans.observePlans().first().size)
        // The old 8 set rows are gone; only the 3 new ones remain.
        assertEquals(3, rowCount("plan_sets"))
    }

    @Test
    fun deletingPlanAlsoDeletesItsExerciseAndSetRows() = runBlocking {
        val id = plans.savePlan(pushDay())

        plans.deletePlan(id)

        assertNull(plans.getPlan(id))
        assertEquals(0, rowCount("plan_exercises"))
        assertEquals(0, rowCount("plan_sets"))
    }

    @Test
    fun plansAreListedByName() = runBlocking {
        plans.savePlan(pushDay().copy(name = "pull day"))
        plans.savePlan(pushDay().copy(name = "Leg day"))
        plans.savePlan(pushDay())

        val names = plans.observePlans().first().map { it.name }

        assertEquals(listOf("Leg day", "pull day", "Push day"), names)
    }

    @Test
    fun blankNameOrNoExercisesIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { plans.savePlan(pushDay().copy(name = "  ")) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { plans.savePlan(pushDay().copy(exercises = emptyList())) }
        }
    }

    @Test
    fun exerciseWithoutSetsIsRejected() {
        val bench = exercise("Bench press")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { plans.savePlan(pushDay().copy(exercises = listOf(PlanExercise(bench, emptyList())))) }
        }
    }

    @Test
    fun zeroRepsOrInvalidWeightIsRejected() {
        val bench = exercise("Bench press")
        val badSets = listOf(PlanSet(reps = 0), PlanSet(8, weightKg = -5.0), PlanSet(8, weightKg = Double.NaN))
        for (badSet in badSets) {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { plans.savePlan(pushDay().copy(exercises = listOf(PlanExercise(bench, listOf(badSet))))) }
            }
        }
    }
}