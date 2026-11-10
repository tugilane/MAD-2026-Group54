package ee.ut.cs.mad.strideset.data.model

import java.time.Duration
import java.time.Instant

data class Workout(
    val id: Long = 0,
    /** The plan's name, or "Workout" for an empty workout. */
    val name: String,
    /** The plan this workout was started from, or null for an empty workout. */
    val planId: Long? = null,
    val startedAt: Instant,
    /** Null while the workout is in progress. */
    val finishedAt: Instant? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
) {
    val isFinished: Boolean get() = finishedAt != null

    val completedSetCount: Int get() = exercises.sumOf { exercise -> exercise.sets.count { it.isCompleted } }

    /** Elapsed time so far, or the total once finished. */
    fun duration(now: Instant = Instant.now()): Duration = Duration.between(startedAt, finishedAt ?: now)
}

data class WorkoutExercise(
    val id: Long = 0,
    val exercise: Exercise,
    /** Copied from the plan when started from one; null in an empty workout. */
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    /** In order: the first item is SET 1. */
    val sets: List<WorkoutSet> = emptyList(),
)

data class WorkoutSet(
    val id: Long = 0,
    val reps: Int,
    /** Always kilograms. Convert with WeightUnit for display. */
    val weightKg: Double,
    val isCompleted: Boolean = false,
)

/** The last finished result for one exercise: used for "Last: 8 × 80 kg" and progressive overload. */
data class ExercisePerformance(
    val exerciseId: Long,
    val performedAt: Instant,
    val targetReps: Int?,
    val sets: List<WorkoutSet>,
)