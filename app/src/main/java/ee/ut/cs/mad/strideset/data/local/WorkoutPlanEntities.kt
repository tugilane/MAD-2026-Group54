package ee.ut.cs.mad.strideset.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import ee.ut.cs.mad.strideset.data.model.PlanExercise
import ee.ut.cs.mad.strideset.data.model.PlanSet
import ee.ut.cs.mad.strideset.data.model.WorkoutPlan

/** One saved plan. Its exercises live in plan_exercises. */
@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/**
 * Link table: one row per exercise in a plan. Its sets live in plan_sets.
 * position = the order the user arranged (0, 1, 2, ...).
 */
@Entity(
    tableName = "plan_exercises",
    primaryKeys = ["planId", "position"],
    foreignKeys = [
        // Deleting a plan deletes its rows here too.
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        // An exercise that is used in a plan can't be deleted.
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class PlanExerciseEntity(
    val planId: Long,
    val position: Int,
    val exerciseId: Long,
)

/**
 * One row per planned set. (planId, exercisePosition) is the plan_exercises row it belongs to.
 * setIndex = the order within that exercise (0 = SET 1).
 */
@Entity(
    tableName = "plan_sets",
    primaryKeys = ["planId", "exercisePosition", "setIndex"],
    foreignKeys = [
        // Deleting an exercise row from a plan deletes its sets too.
        ForeignKey(
            entity = PlanExerciseEntity::class,
            parentColumns = ["planId", "position"],
            childColumns = ["planId", "exercisePosition"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)

data class PlanSetEntity(
    val planId: Long,
    val exercisePosition: Int,
    val setIndex: Int,
    val reps: Int,
    /** Kilograms, or null when the set has no weight. */
    val weightKg: Double?,
)

/** A plan row plus its exercise rows and set rows; Room fills these in with @Relation. */
data class PlanWithExercises(
    @Embedded val plan: WorkoutPlanEntity,
    @Relation(entity = PlanExerciseEntity::class, parentColumn = "id", entityColumn = "planId")
    val items: List<PlanExerciseWithExercise>,
    // @Relation matches on one column only, so load all of the plan's sets and split in toModel().
    @Relation(parentColumn = "id", entityColumn = "planId")
    val sets: List<PlanSetEntity>,
)

/** One plan_exercises row plus the exercise it points to. */
data class PlanExerciseWithExercise(
    @Embedded val planExercise: PlanExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

fun PlanWithExercises.toModel(): WorkoutPlan {
    val setsByPosition = sets.groupBy { it.exercisePosition }
    return WorkoutPlan(
        id = plan.id,
        name = plan.name,
        exercises = items
            .sortedBy { it.planExercise.position }
            .map { item ->
                PlanExercise(
                    exercise = item.exercise.toModel(),
                    sets = setsByPosition[item.planExercise.position].orEmpty()
                        .sortedBy { it.setIndex }
                        .map { PlanSet(reps = it.reps, weightKg = it.weightKg) },
                )
            },
    )
}