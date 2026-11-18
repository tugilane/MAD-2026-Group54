package ee.ut.cs.mad.strideset.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // ---- Reading ----

    /** The workout with no finish time yet, or null. */
    @Transaction
    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL LIMIT 1")
    fun observeActive(): Flow<WorkoutWithExercises?>

    @Query("SELECT id FROM workouts WHERE finishedAt IS NULL LIMIT 1")
    suspend fun getActiveId(): Long?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutWithExercises?>

    /** Finished workouts that started at or after fromMillis and before toMillis, oldest first. */
    @Transaction
    @Query(
        """
        SELECT * FROM workouts
        WHERE finishedAt IS NOT NULL AND startedAt >= :fromMillis AND startedAt < :toMillis
        ORDER BY startedAt
        """
    )
    fun observeFinishedBetween(fromMillis: Long, toMillis: Long): Flow<List<WorkoutWithExercises>>

    /** This exercise in the most recent finished workout where at least one of its sets was completed. */
    @Transaction
    @Query(
        """
        SELECT workout_exercises.* FROM workout_exercises
        JOIN workouts ON workouts.id = workout_exercises.workoutId
        WHERE workout_exercises.exerciseId = :exerciseId
          AND workouts.finishedAt IS NOT NULL
          AND EXISTS (
              SELECT 1 FROM workout_sets
              WHERE workout_sets.workoutExerciseId = workout_exercises.id AND workout_sets.isCompleted = 1
          )
        ORDER BY workouts.startedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLastFinishedEntry(exerciseId: Long): WorkoutExerciseWithSets?

    @Query("SELECT startedAt FROM workouts WHERE id = :id")
    suspend fun getStartedAt(id: Long): Long?

    @Query(
        """
        SELECT COUNT(*) FROM workout_sets
        JOIN workout_exercises ON workout_exercises.id = workout_sets.workoutExerciseId
        WHERE workout_exercises.workoutId = :workoutId AND workout_sets.isCompleted = 1
        """
    )
    suspend fun countCompletedSets(workoutId: Long): Int

    // Next free position: 0 when empty, otherwise the highest + 1.
    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun nextExercisePosition(workoutId: Long): Int

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun nextSetPosition(workoutExerciseId: Long): Int

    // ---- Writing ----

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long

    @Insert
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("UPDATE workout_sets SET reps = :reps, weightKg = :weightKg, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateSet(id: Long, reps: Int, weightKg: Double, isCompleted: Boolean)

    @Query("UPDATE workouts SET finishedAt = :finishedAt WHERE id = :id")
    suspend fun setFinishedAt(id: Long, finishedAt: Long)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    // ---- Several steps in one transaction ----

    /**
     * Inserts a new workout and, when started from a plan, its exercises and sets.
     * @throws IllegalStateException if another workout is already in progress (nothing is saved)
     */
    @Transaction
    suspend fun startWorkout(workout: WorkoutEntity, exercises: List<StartingExercise>): Long {
        check(getActiveId() == null) { "Another workout is already in progress" }
        val workoutId = insertWorkout(workout)
        exercises.forEachIndexed { position, item ->
            val workoutExerciseId = insertExercise(
                WorkoutExerciseEntity(workoutId = workoutId, position = position, exerciseId = item.exerciseId)
            )
            insertSets(item.sets.map { it.copy(workoutExerciseId = workoutExerciseId) })
        }
        return workoutId
    }

    /** Adds an exercise after the existing ones and returns its id. */
    @Transaction
    suspend fun addExercise(workoutId: Long, exerciseId: Long): Long = insertExercise(
        WorkoutExerciseEntity(workoutId = workoutId, position = nextExercisePosition(workoutId), exerciseId = exerciseId)
    )

    /** Adds a not-yet-completed set after the existing ones and returns its id. */
    @Transaction
    suspend fun addSet(workoutExerciseId: Long, reps: Int, weightKg: Double): Long = insertSet(
        WorkoutSetEntity(
            workoutExerciseId = workoutExerciseId,
            position = nextSetPosition(workoutExerciseId),
            reps = reps,
            weightKg = weightKg,
        )
    )
}