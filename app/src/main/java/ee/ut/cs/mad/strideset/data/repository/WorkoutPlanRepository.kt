package ee.ut.cs.mad.strideset.data.repository

import ee.ut.cs.mad.strideset.data.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

interface WorkoutPlanRepository {
    /** All saved plans, sorted by name. */
    fun observePlans(): Flow<List<WorkoutPlan>>

    suspend fun getPlan(id: Long): WorkoutPlan?

    /**
     * Inserts the plan when plan.id == 0, otherwise replaces the existing one. Returns the plan id.
     * @throws IllegalArgumentException if the name is blank or the plan has no exercises
     */
    suspend fun savePlan(plan: WorkoutPlan): Long

    suspend fun deletePlan(id: Long)
}