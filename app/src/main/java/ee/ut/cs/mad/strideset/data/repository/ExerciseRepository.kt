package ee.ut.cs.mad.strideset.data.repository

import ee.ut.cs.mad.strideset.data.model.Exercise
import ee.ut.cs.mad.strideset.data.model.ExerciseType
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    /** All exercises, built-in and custom, sorted by name. */
    fun observeExercises(): Flow<List<Exercise>>

    /**
     * Creates a custom exercise and returns it with its new id.
     * @throws IllegalArgumentException if the name is blank
     * @throws ExerciseNameTakenException if the name already exists (ignoring case)
     */
    suspend fun addCustomExercise(name: String, type: ExerciseType): Exercise
}