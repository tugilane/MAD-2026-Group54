package ee.ut.cs.mad.strideset.data.repository

import ee.ut.cs.mad.strideset.data.local.PlanExerciseEntity
import ee.ut.cs.mad.strideset.data.local.WorkoutPlanDao
import ee.ut.cs.mad.strideset.data.local.WorkoutPlanEntity
import ee.ut.cs.mad.strideset.data.local.toModel
import ee.ut.cs.mad.strideset.data.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorkoutPlanRepository(private val dao: WorkoutPlanDao) : WorkoutPlanRepository {

    override fun observePlans(): Flow<List<WorkoutPlan>> =
        dao.observeAll().map { plans -> plans.map { it.toModel() } }

    override suspend fun getPlan(id: Long): WorkoutPlan? = dao.getById(id)?.toModel()

    override suspend fun savePlan(plan: WorkoutPlan): Long {
        val name = plan.name.trim()
        require(name.isNotEmpty()) { "Plan name can't be blank" }
        require(plan.exercises.isNotEmpty()) { "A plan needs at least one exercise" }

        val items = plan.exercises.mapIndexed { index, item ->
            PlanExerciseEntity(
                planId = plan.id, // the DAO sets the real id for a new plan
                position = index,
                exerciseId = item.exercise.id,
                targetSets = item.targetSets,
                targetReps = item.targetReps,
            )
        }
        return dao.savePlan(WorkoutPlanEntity(id = plan.id, name = name), items)
    }

    override suspend fun deletePlan(id: Long) = dao.deletePlan(id)
}