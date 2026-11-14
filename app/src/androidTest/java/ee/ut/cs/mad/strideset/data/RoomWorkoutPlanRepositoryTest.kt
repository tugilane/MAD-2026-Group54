package ee.ut.cs.mad.strideset.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.data.local.SeedDefaultExercises
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.PlanExercise
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

    private fun pushDay() = WorkoutPlan(
        name = "Push day",
        exercises = listOf(
            PlanExercise(exercise("Bench press"), targetSets = 3, targetReps = 8),
            PlanExercise(exercise("Overhead press"), targetSets = 3, targetReps = 10),
            PlanExercise(exercise("Dip"), targetSets = 2, targetReps = 12),
        ),
    )

    @Test
    fun savedPlanIsReadBackWithExercisesInOrder() = runBlocking {
        val id = plans.savePlan(pushDay())

        assertEquals(pushDay().copy(id = id), plans.getPlan(id))
    }

    @Test
    fun editingPlanReplacesNameAndExercises() = runBlocking {
        val id = plans.savePlan(pushDay())
        val edited = WorkoutPlan(
            id = id,
            name = "Push day B",
            exercises = listOf(
                PlanExercise(exercise("Dip"), targetSets = 4, targetReps = 10),
                PlanExercise(exercise("Bench press"), targetSets = 5, targetReps = 5),
            ),
        )

        assertEquals(id, plans.savePlan(edited))
        assertEquals(edited, plans.getPlan(id))
        assertEquals(1, plans.observePlans().first().size)
    }

    @Test
    fun deletingPlanAlsoDeletesItsExerciseRows() = runBlocking {
        val id = plans.savePlan(pushDay())

        plans.deletePlan(id)

        assertNull(plans.getPlan(id))
        db.query("SELECT COUNT(*) FROM plan_exercises", null).use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
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
    fun zeroSetsOrRepsIsRejected() {
        val bench = exercise("Bench press")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { plans.savePlan(pushDay().copy(exercises = listOf(PlanExercise(bench, 0, 8)))) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { plans.savePlan(pushDay().copy(exercises = listOf(PlanExercise(bench, 3, 0)))) }
        }
    }
}