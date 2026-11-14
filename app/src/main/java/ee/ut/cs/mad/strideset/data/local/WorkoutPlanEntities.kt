package ee.ut.cs.mad.strideset.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import ee.ut.cs.mad.strideset.data.model.PlanExercise
import ee.ut.cs.mad.strideset.data.model.WorkoutPlan

/** One saved plan. Its exercises live in plan_exercises. */
@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/**
 * Link table: one row per exercise in a plan, with the plan's targets for it.
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
    val targetSets: Int,
    val targetReps: Int,
)

/** A plan row plus its exercise rows; Room fills this in with @Relation. */
data class PlanWithExercises(
    @Embedded val plan: WorkoutPlanEntity,
    @Relation(entity = PlanExerciseEntity::class, parentColumn = "id", entityColumn = "planId")
    val items: List<PlanExerciseWithExercise>,
)

/** One plan_exercises row plus the exercise it points to. */
data class PlanExerciseWithExercise(
    @Embedded val planExercise: PlanExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

fun PlanWithExercises.toModel() = WorkoutPlan(
    id = plan.id,
    name = plan.name,
    exercises = items
        .sortedBy { it.planExercise.position }
        .map {
            PlanExercise(
                exercise = it.exercise.toModel(),
                targetSets = it.planExercise.targetSets,
                targetReps = it.planExercise.targetReps,
            )
        },
)