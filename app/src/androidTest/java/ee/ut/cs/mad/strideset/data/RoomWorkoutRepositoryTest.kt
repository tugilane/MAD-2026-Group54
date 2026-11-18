package ee.ut.cs.mad.strideset.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ee.ut.cs.mad.strideset.data.local.SeedDefaultExercises
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.PlanExercise
import ee.ut.cs.mad.strideset.data.model.PlanSet
import ee.ut.cs.mad.strideset.data.model.Workout
import ee.ut.cs.mad.strideset.data.model.WorkoutPlan
import ee.ut.cs.mad.strideset.data.model.WorkoutSet
import ee.ut.cs.mad.strideset.data.repository.EmptyWorkoutException
import ee.ut.cs.mad.strideset.data.repository.RoomExerciseRepository
import ee.ut.cs.mad.strideset.data.repository.RoomWorkoutPlanRepository
import ee.ut.cs.mad.strideset.data.repository.RoomWorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RoomWorkoutRepositoryTest {
    private val zone = ZoneId.of("Europe/Tallinn")
    private val clock = TestClock(at(2026, 11, 17, 18, 0), zone)

    private lateinit var db: StrideSetDatabase
    private lateinit var plans: RoomWorkoutPlanRepository
    private lateinit var workouts: RoomWorkoutRepository
    private lateinit var exercises: List<Exercise>

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, StrideSetDatabase::class.java)
            .addCallback(SeedDefaultExercises)
            .build()
        plans = RoomWorkoutPlanRepository(db.workoutPlanDao())
        workouts = RoomWorkoutRepository(db.workoutDao(), plans, clock)
        exercises = RoomExerciseRepository(db.exerciseDao()).observeExercises().first()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---- Helpers ----

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant()

    private fun exercise(name: String) = exercises.first { it.name == name }

    private suspend fun loadWorkout(id: Long): Workout = workouts.observeWorkout(id).first()!!

    private fun rowCount(table: String): Int =
        db.query("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    /** Ticks every set in the workout. */
    private suspend fun completeAllSets(workoutId: Long) {
        loadWorkout(workoutId).exercises.flatMap { it.sets }.forEach { set ->
            workouts.updateSet(set.copy(isCompleted = true))
        }
    }

    /** An empty workout started at startedAt with one exercise and these (reps, kg) sets, all ticked, finished an hour later. */
    private suspend fun finishedWorkout(startedAt: Instant, exerciseName: String, vararg sets: Pair<Int, Double>): Long {
        clock.now = startedAt
        val id = workouts.startWorkout()
        val entry = workouts.addExercise(id, exercise(exerciseName).id)
        sets.forEach { (reps, kg) -> workouts.addSet(entry, reps, kg) }
        completeAllSets(id)
        clock.now = startedAt.plus(Duration.ofHours(1))
        workouts.finishWorkout(id)
        return id
    }

    // ---- Starting ----

    @Test
    fun emptyWorkoutStartsWithNoExercises() = runBlocking<Unit> {
        val id = workouts.startWorkout()

        val active = workouts.observeActiveWorkout().first()!!
        assertEquals(id, active.id)
        assertEquals("Workout", active.name)
        assertNull(active.planId)
        assertEquals(clock.now, active.startedAt)
        assertNull(active.finishedAt)
        assertTrue(active.exercises.isEmpty())
    }

    @Test
    fun startingFromPlanCopiesItsExercisesAndSets() = runBlocking<Unit> {
        val planId = plans.savePlan(
            WorkoutPlan(
                name = "Full body",
                exercises = listOf(
                    PlanExercise(exercise("Squat"), listOf(PlanSet(8, 80.0), PlanSet(6, 85.0))),
                    PlanExercise(exercise("Pull-up"), List(3) { PlanSet(8) }),
                ),
            )
        )

        val workout = loadWorkout(workouts.startWorkout(planId))

        assertEquals("Full body", workout.name)
        assertEquals(planId, workout.planId)
        assertEquals(
            listOf(
                "Squat" to listOf(8 to 80.0, 6 to 85.0),
                "Pull-up" to listOf(8 to 0.0, 8 to 0.0, 8 to 0.0), // no weight in the plan -> 0 kg
            ),
            workout.exercises.map { item -> item.exercise.name to item.sets.map { it.reps to it.weightKg } },
        )
        assertTrue(workout.exercises.flatMap { it.sets }.none { it.isCompleted })
    }

    @Test
    fun startingFromMissingPlanIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { workouts.startWorkout(planId = 999) }
        }
    }

    @Test
    fun onlyOneWorkoutCanBeInProgress() = runBlocking<Unit> {
        val first = workouts.startWorkout()

        assertThrows(IllegalStateException::class.java) {
            runBlocking { workouts.startWorkout() }
        }

        workouts.discardWorkout(first)
        workouts.startWorkout() // allowed again
        assertEquals(1, rowCount("workouts"))
    }

    // ---- During the workout ----

    @Test
    fun exercisesAndSetsCanBeAddedAsYouGo() = runBlocking<Unit> {
        val id = workouts.startWorkout()

        val squat = workouts.addExercise(id, exercise("Squat").id)
        workouts.addSet(squat, 8, 60.0)
        workouts.addSet(squat, 8, 65.0)
        val dip = workouts.addExercise(id, exercise("Dip").id)
        workouts.addSet(dip, 12, 0.0)

        assertEquals(
            listOf(
                "Squat" to listOf(8 to 60.0, 8 to 65.0),
                "Dip" to listOf(12 to 0.0),
            ),
            loadWorkout(id).exercises.map { item -> item.exercise.name to item.sets.map { it.reps to it.weightKg } },
        )
    }

    @Test
    fun setsCanBeTickedEditedAndDeleted() = runBlocking<Unit> {
        val id = workouts.startWorkout()
        val squat = workouts.addExercise(id, exercise("Squat").id)
        val firstSetId = workouts.addSet(squat, 8, 60.0)
        workouts.addSet(squat, 8, 60.0)

        val second = loadWorkout(id).exercises.single().sets[1]
        workouts.updateSet(second.copy(reps = 6, weightKg = 62.5, isCompleted = true))
        workouts.deleteSet(firstSetId)

        assertEquals(
            listOf(WorkoutSet(id = second.id, reps = 6, weightKg = 62.5, isCompleted = true)),
            loadWorkout(id).exercises.single().sets,
        )
    }

    @Test
    fun removingExerciseAlsoDeletesItsSets() = runBlocking<Unit> {
        val id = workouts.startWorkout()
        val squat = workouts.addExercise(id, exercise("Squat").id)
        workouts.addSet(squat, 8, 60.0)

        workouts.removeExercise(squat)

        assertTrue(loadWorkout(id).exercises.isEmpty())
        assertEquals(0, rowCount("workout_sets"))
    }

    @Test
    fun negativeRepsOrWeightIsRejected() = runBlocking<Unit> {
        val id = workouts.startWorkout()
        val squat = workouts.addExercise(id, exercise("Squat").id)

        assertThrows(IllegalArgumentException::class.java) { runBlocking { workouts.addSet(squat, -1, 60.0) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { workouts.addSet(squat, 8, -5.0) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { workouts.addSet(squat, 8, Double.NaN) } }
    }

    // ---- Finishing and discarding ----

    @Test
    fun workoutWithoutCompletedSetCantBeFinished() = runBlocking<Unit> {
        val id = workouts.startWorkout()
        val squat = workouts.addExercise(id, exercise("Squat").id)
        workouts.addSet(squat, 8, 60.0) // not ticked

        assertThrows(EmptyWorkoutException::class.java) {
            runBlocking { workouts.finishWorkout(id) }
        }
        assertEquals(id, workouts.observeActiveWorkout().first()?.id) // still in progress
    }

    @Test
    fun finishingEndsTheActiveWorkout() = runBlocking<Unit> {
        val id = workouts.startWorkout()
        val squat = workouts.addExercise(id, exercise("Squat").id)
        workouts.addSet(squat, 8, 60.0)
        completeAllSets(id)

        clock.now = clock.now.plus(Duration.ofMinutes(45))
        workouts.finishWorkout(id)

        assertNull(workouts.observeActiveWorkout().first())
        val finished = loadWorkout(id)
        assertEquals(clock.now, finished.finishedAt)
        assertEquals(Duration.ofMinutes(45), finished.duration())
    }

    @Test
    fun discardingDeletesTheWorkoutWithItsExercisesAndSets() = runBlocking<Unit> {
        val id = workouts.startWorkout()
        val squat = workouts.addExercise(id, exercise("Squat").id)
        workouts.addSet(squat, 8, 60.0)

        workouts.discardWorkout(id)

        assertNull(workouts.observeWorkout(id).first())
        assertEquals(0, rowCount("workout_exercises"))
        assertEquals(0, rowCount("workout_sets"))
    }

    // ---- Reading history (calendar, progression) ----

    @Test
    fun finishedWorkoutsAreFoundByDay() = runBlocking<Unit> {
        finishedWorkout(at(2026, 11, 9, 23, 30), "Squat", 8 to 60.0) // evening before the range
        val firstDay = finishedWorkout(at(2026, 11, 10, 0, 30), "Squat", 8 to 60.0)
        val lastDay = finishedWorkout(at(2026, 11, 16, 23, 30), "Squat", 8 to 60.0)
        finishedWorkout(at(2026, 11, 17, 0, 30), "Squat", 8 to 60.0) // just after the range
        clock.now = at(2026, 11, 12, 12, 0)
        workouts.startWorkout() // in progress: not shown in the calendar

        val range = workouts.observeFinishedWorkouts(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 16)).first()

        assertEquals(listOf(firstDay, lastDay), range.map { it.id })
    }

    @Test
    fun lastPerformanceIsTheMostRecentFinishedWorkoutsCompletedSets() = runBlocking<Unit> {
        val bench = exercise("Bench press")
        finishedWorkout(at(2026, 11, 10, 18, 0), "Bench press", 8 to 60.0, 8 to 60.0)

        // The most recent finished workout: one set ticked, one not.
        clock.now = at(2026, 11, 12, 18, 0)
        val id = workouts.startWorkout()
        val entry = workouts.addExercise(id, bench.id)
        val doneSetId = workouts.addSet(entry, 8, 62.5)
        workouts.addSet(entry, 6, 62.5)
        workouts.updateSet(WorkoutSet(id = doneSetId, reps = 8, weightKg = 62.5, isCompleted = true))
        workouts.finishWorkout(id)

        // An unfinished workout doesn't count yet.
        clock.now = at(2026, 11, 14, 18, 0)
        val active = workouts.startWorkout()
        workouts.addSet(workouts.addExercise(active, bench.id), 10, 70.0)
        completeAllSets(active)

        val last = workouts.getLastPerformance(bench.id)!!

        assertEquals(at(2026, 11, 12, 18, 0), last.performedAt)
        assertEquals(listOf(8 to 62.5), last.sets.map { it.reps to it.weightKg })
        assertNull(workouts.getLastPerformance(exercise("Deadlift").id)) // never done
    }

    @Test
    fun deletingPlanKeepsItsWorkoutsInHistory() = runBlocking<Unit> {
        val planId = plans.savePlan(
            WorkoutPlan(name = "Legs", exercises = listOf(PlanExercise(exercise("Squat"), listOf(PlanSet(8, 80.0)))))
        )
        val id = workouts.startWorkout(planId)
        completeAllSets(id)
        workouts.finishWorkout(id)

        plans.deletePlan(planId)

        val kept = loadWorkout(id)
        assertNull(kept.planId)
        assertEquals("Legs", kept.name)
    }
}

/** A clock the tests can move by setting now. */
private class TestClock(var now: Instant, private val zone: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = TestClock(now, zone)
    override fun instant(): Instant = now
}