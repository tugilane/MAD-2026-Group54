package ee.ut.cs.mad.strideset.data.repository

import ee.ut.cs.mad.strideset.data.local.PlanExerciseEntity
import ee.ut.cs.mad.strideset.data.local.ExerciseEntity
import ee.ut.cs.mad.strideset.data.local.PlanSetEntity
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
        require(plan.exercises.all { it.sets.isNotEmpty() }) { "Every exercise needs at least one set" }

        val allSets = plan.exercises.flatMap { it.sets }
        require(allSets.all { it.reps > 0 }) { "Reps must be at least 1" }
        require(allSets.all { it.weightKg == null || it.weightKg >= 0 }) { "Weight can't be negative" }

        // planId = plan.id here; the DAO sets the real id for a new plan.
        val items = plan.exercises.mapIndexed { position, item ->
            PlanExerciseEntity(planId = plan.id, position = position, exerciseId = item.exercise.id)
        }
        val sets = plan.exercises.flatMapIndexed { position, item ->
            item.sets.mapIndexed { setIndex, set ->
                PlanSetEntity(
                    planId = plan.id,
                    exercisePosition = position,
                    setIndex = setIndex,
                    reps = set.reps,
                    weightKg = set.weightKg,
                )
            }
        }
        val customExercises = plan.exercises.mapIndexedNotNull { position, item ->
            item.exercise.takeIf { it.id == 0L }?.let {
                position to ExerciseEntity(name = it.name.trim(), type = it.type, isCustom = true)
            }
        }.toMap()
        return dao.savePlan(WorkoutPlanEntity(id = plan.id, name = name), items, sets, customExercises)
    }

    override suspend fun deletePlan(id: Long) = dao.deletePlan(id)
}
