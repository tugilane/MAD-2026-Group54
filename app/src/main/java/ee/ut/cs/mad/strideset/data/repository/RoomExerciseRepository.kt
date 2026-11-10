package ee.ut.cs.mad.strideset.data.repository

import android.database.sqlite.SQLiteConstraintException
import ee.ut.cs.mad.strideset.data.local.ExerciseDao
import ee.ut.cs.mad.strideset.data.local.ExerciseEntity
import ee.ut.cs.mad.strideset.data.local.toModel
import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.ExerciseType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomExerciseRepository(private val dao: ExerciseDao) : ExerciseRepository {

    override fun observeExercises(): Flow<List<Exercise>> =
        dao.observeAll().map { rows -> rows.map { it.toModel() } }

    override suspend fun addCustomExercise(name: String, type: ExerciseType): Exercise {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Exercise name can't be blank" }

        val entity = ExerciseEntity(name = trimmed, type = type, isCustom = true)
        val id = try {
            dao.insert(entity)
        } catch (e: SQLiteConstraintException) {
            throw ExerciseNameTakenException(trimmed)
        }
        return entity.copy(id = id).toModel()
    }
}