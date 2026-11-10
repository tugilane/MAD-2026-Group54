package ee.ut.cs.mad.strideset.data.repository

import ee.ut.cs.mad.strideset.data.model.ExercisePerformance
import ee.ut.cs.mad.strideset.data.model.Workout
import ee.ut.cs.mad.strideset.data.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface WorkoutRepository {
    /** The unfinished workout, or null. Survives app restarts, so Home can offer "Resume". */
    fun observeActiveWorkout(): Flow<Workout?>

    fun observeWorkout(id: Long): Flow<Workout?>

    /**
     * Starts a workout, pre-filled with the plan's exercises when planId is given. Returns its id.
     * @throws IllegalStateException if another workout is already in progress
     */
    suspend fun startWorkout(planId: Long? = null): Long

    /** Adds an exercise to the workout and returns the new WorkoutExercise id. */
    suspend fun addExercise(workoutId: Long, exerciseId: Long): Long

    suspend fun removeExercise(workoutExerciseId: Long)

    /** Adds a set (weight in kg) to the end of the exercise's sets and returns its id. */
    suspend fun addSet(workoutExerciseId: Long, reps: Int, weightKg: Double): Long

    suspend fun updateSet(set: WorkoutSet)

    suspend fun deleteSet(setId: Long)

    /** @throws EmptyWorkoutException if no set has been marked completed */
    suspend fun finishWorkout(workoutId: Long)

    /** Deletes the workout and all its sets. */
    suspend fun discardWorkout(workoutId: Long)

    /** Finished workouts that started between these days (inclusive, device time zone). For the Calendar. */
    fun observeFinishedWorkouts(start: LocalDate, endInclusive: LocalDate): Flow<List<Workout>>

    /** The most recent finished result for this exercise, or null if never done. For progressive overload. */
    suspend fun getLastPerformance(exerciseId: Long): ExercisePerformance?
}