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

    @Query("DELETE FROM plan_exercises WHERE planId = :planId")
    suspend fun deleteItems(planId: Long)

    @Query("DELETE FROM workout_plans WHERE id = :id")
    suspend fun deletePlan(id: Long)

    /**
     * Saves a plan and its exercise list in one transaction: either everything is saved or nothing.
     * New plan (id == 0): insert it. Existing plan: update the name and replace its whole exercise list.
     * Returns the plan id.
     */
    @Transaction
    suspend fun savePlan(plan: WorkoutPlanEntity, items: List<PlanExerciseEntity>): Long {
        val planId = if (plan.id == 0L) {
            insertPlan(plan)
        } else {
            updatePlan(plan)
            deleteItems(plan.id)
            plan.id
        }
        insertItems(items.map { it.copy(planId = planId) })
        return planId
    }
}