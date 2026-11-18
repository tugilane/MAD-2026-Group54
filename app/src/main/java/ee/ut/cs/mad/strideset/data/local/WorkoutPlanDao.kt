package ee.ut.cs.mad.strideset.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {
    // @Transaction: Room reads the plans and their exercises in several queries; this keeps them consistent.
    @Transaction
    @Query("SELECT * FROM workout_plans ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PlanWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id = :id")
    suspend fun getById(id: Long): PlanWithExercises?

    @Insert
    suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlanEntity)

    @Insert
    suspend fun insertItems(items: List<PlanExerciseEntity>)

    @Insert
    suspend fun insertSets(sets: List<PlanSetEntity>)

    @Insert
    suspend fun insertCustomExercise(exercise: ExerciseEntity): Long

    @Query("SELECT id FROM exercises WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun exerciseIdByName(name: String): Long?

    @Query("DELETE FROM plan_exercises WHERE planId = :planId")
    suspend fun deleteItems(planId: Long)

    @Query("DELETE FROM workout_plans WHERE id = :id")
    suspend fun deletePlan(id: Long)

    /**
     * Saves a plan with its exercises and sets in one transaction: either everything is saved or nothing.
     * New plan (id == 0): insert it. Existing plan: update the name and replace its exercises and sets.
     * Returns the plan id.
     */
    @Transaction
    suspend fun savePlan(
        plan: WorkoutPlanEntity,
        items: List<PlanExerciseEntity>,
        sets: List<PlanSetEntity>,
        customExercises: Map<Int, ExerciseEntity> = emptyMap(), // Optional argument. It's ignored when all exercises already exist.
    ): Long {
        // Create custom exercises inside the same transaction as the plan.
        // Repeated custom names share one library entry, including after a retry.
        val resolvedItems = items.map { item ->
            val custom = customExercises[item.position]
            if (custom == null) item else item.copy(
                exerciseId = exerciseIdByName(custom.name) ?: insertCustomExercise(custom)
            )
        }
        val planId = if (plan.id == 0L) {
            insertPlan(plan)
        } else {
            updatePlan(plan)
            deleteItems(plan.id) // CASCADE also deletes their plan_sets rows
            plan.id
        }
        // Exercises first: each set row must point at an existing exercise row (foreign key).
        insertItems(resolvedItems.map { it.copy(planId = planId) })
        insertSets(sets.map { it.copy(planId = planId) })
        return planId
    }
}
