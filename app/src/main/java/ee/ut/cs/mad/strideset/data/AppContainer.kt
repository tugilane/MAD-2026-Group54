package ee.ut.cs.mad.strideset.data

import android.content.Context
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.repository.ExerciseRepository
import ee.ut.cs.mad.strideset.data.repository.RoomExerciseRepository
import ee.ut.cs.mad.strideset.data.repository.RoomWorkoutPlanRepository
import ee.ut.cs.mad.strideset.data.repository.WorkoutPlanRepository

/** App-wide place where repositories are created (manual dependency injection). */
interface AppContainer {
    val exerciseRepository: ExerciseRepository
    val workoutPlanRepository: WorkoutPlanRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val database: StrideSetDatabase by lazy { StrideSetDatabase.create(context) }

    override val exerciseRepository: ExerciseRepository by lazy {
        RoomExerciseRepository(database.exerciseDao())
    }

    override val workoutPlanRepository: WorkoutPlanRepository by lazy {
        RoomWorkoutPlanRepository(database.workoutPlanDao())
    }
}