package ee.ut.cs.mad.strideset.data.repository

import ee.ut.cs.mad.strideset.data.local.StartingExercise
import ee.ut.cs.mad.strideset.data.local.WorkoutDao
import ee.ut.cs.mad.strideset.data.local.WorkoutEntity
import ee.ut.cs.mad.strideset.data.local.WorkoutSetEntity
import ee.ut.cs.mad.strideset.data.local.toModel
import ee.ut.cs.mad.strideset.data.model.ExercisePerformance
import ee.ut.cs.mad.strideset.data.model.Workout
import ee.ut.cs.mad.strideset.data.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * Workouts are saved step by step: every call writes to the database right away,
 * so an unfinished workout survives the app being closed.
 * clock: where "now" and the time zone come from (tests pass a fixed one).
 */
class RoomWorkoutRepository(
    private val dao: WorkoutDao,
    private val planRepository: WorkoutPlanRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : WorkoutRepository {

    override fun observeActiveWorkout(): Flow<Workout?> =
        dao.observeActive().map { it?.toModel() }

    override fun observeWorkout(id: Long): Flow<Workout?> =
        dao.observeById(id).map { it?.toModel() }

    override suspend fun startWorkout(planId: Long?): Long {
        val plan = planId?.let { id ->
            requireNotNull(planRepository.getPlan(id)) { "No plan with id $id" }
        }
        // The plan's sets become the workout's first sets: same reps and kg, not completed yet.
        // A set without weight (bodyweight) becomes 0 kg.
        val exercises = plan?.exercises.orEmpty().map { item ->
            StartingExercise(
                exerciseId = item.exercise.id,
                sets = item.sets.mapIndexed { index, set ->
                    WorkoutSetEntity(
                        workoutExerciseId = 0, // the DAO sets the real id
                        position = index,
                        reps = set.reps,
                        weightKg = set.weightKg ?: 0.0,
                    )
                },
            )
        }
        val workout = WorkoutEntity(
            name = plan?.name ?: "Workout",
            planId = plan?.id,
            startedAt = clock.millis(),
        )
        return dao.startWorkout(workout, exercises)
    }

    override suspend fun addExercise(workoutId: Long, exerciseId: Long): Long =
        dao.addExercise(workoutId, exerciseId)

    override suspend fun removeExercise(workoutExerciseId: Long) = dao.deleteExercise(workoutExerciseId)

    override suspend fun addSet(workoutExerciseId: Long, reps: Int, weightKg: Double): Long {
        requireValidSet(reps, weightKg)
        return dao.addSet(workoutExerciseId, reps, weightKg)
    }

    override suspend fun updateSet(set: WorkoutSet) {
        requireValidSet(set.reps, set.weightKg)
        dao.updateSet(set.id, set.reps, set.weightKg, set.isCompleted)
    }

    override suspend fun deleteSet(setId: Long) = dao.deleteSet(setId)

    override suspend fun finishWorkout(workoutId: Long) {
        if (dao.countCompletedSets(workoutId) == 0) throw EmptyWorkoutException()
        dao.setFinishedAt(workoutId, clock.millis())
    }

    override suspend fun discardWorkout(workoutId: Long) = dao.deleteWorkout(workoutId)

    override fun observeFinishedWorkouts(start: LocalDate, endInclusive: LocalDate): Flow<List<Workout>> {
        // Whole days in the phone's time zone: from start 00:00 up to (not including) the day after endInclusive.
        val from = start.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val to = endInclusive.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        return dao.observeFinishedBetween(from, to).map { workouts -> workouts.map { it.toModel() } }
    }

    override suspend fun getLastPerformance(exerciseId: Long): ExercisePerformance? {
        val entry = dao.getLastFinishedEntry(exerciseId) ?: return null
        val startedAt = dao.getStartedAt(entry.workoutExercise.workoutId) ?: return null
        return ExercisePerformance(
            exerciseId = exerciseId,
            performedAt = Instant.ofEpochMilli(startedAt),
            sets = entry.toModel().sets.filter { it.isCompleted },
        )
    }

    private fun requireValidSet(reps: Int, weightKg: Double) {
        require(reps >= 0) { "Reps can't be negative" }
        require(weightKg >= 0) { "Weight can't be negative" }
    }
}