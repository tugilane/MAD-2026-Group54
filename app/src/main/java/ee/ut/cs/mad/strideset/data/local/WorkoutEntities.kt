package ee.ut.cs.mad.strideset.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import ee.ut.cs.mad.strideset.data.model.Workout
import ee.ut.cs.mad.strideset.data.model.WorkoutExercise
import ee.ut.cs.mad.strideset.data.model.WorkoutSet
import java.time.Instant

/**
 * One training session. finishedAt == null means it is still in progress.
 * Times are epoch milliseconds.
 */
@Entity(
    tableName = "workouts",
    foreignKeys = [
        // Deleting a plan keeps its workouts in history; they just lose the link.
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    // startedAt: the calendar looks workouts up by date.
    indices = [Index("planId"), Index("startedAt")],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** The plan it was started from, or null for an empty workout. */
    val planId: Long?,
    val startedAt: Long,
    val finishedAt: Long? = null,
)

/** One exercise done in a workout. position = the order it was added (0, 1, 2, ...). */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        // Deleting a workout deletes its exercises (and, through them, their sets).
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        // An exercise that appears in workout history can't be deleted.
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val position: Int,
    val exerciseId: Long,
)

/** One set of an exercise in a workout. position = its order (0 = SET 1). */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val position: Int,
    val reps: Int,
    /** Kilograms. 0 for bodyweight. */
    val weightKg: Double,
    val isCompleted: Boolean = false,
)

/** An exercise and the sets to create for it when a workout starts from a plan. */
data class StartingExercise(
    val exerciseId: Long,
    val sets: List<WorkoutSetEntity>,
)

/** A workout row plus its exercises (each with its sets); Room fills these in with @Relation. */
data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(entity = WorkoutExerciseEntity::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExerciseWithSets>,
)

/** One workout_exercises row plus the exercise it points to and its sets. */
data class WorkoutExerciseWithSets(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<WorkoutSetEntity>,
)

fun WorkoutSetEntity.toModel() = WorkoutSet(id = id, reps = reps, weightKg = weightKg, isCompleted = isCompleted)

fun WorkoutExerciseWithSets.toModel() = WorkoutExercise(
    id = workoutExercise.id,
    exercise = exercise.toModel(),
    sets = sets.sortedBy { it.position }.map { it.toModel() },
)

fun WorkoutWithExercises.toModel() = Workout(
    id = workout.id,
    name = workout.name,
    planId = workout.planId,
    startedAt = Instant.ofEpochMilli(workout.startedAt),
    finishedAt = workout.finishedAt?.let { Instant.ofEpochMilli(it) },
    exercises = exercises.sortedBy { it.workoutExercise.position }.map { it.toModel() },
)