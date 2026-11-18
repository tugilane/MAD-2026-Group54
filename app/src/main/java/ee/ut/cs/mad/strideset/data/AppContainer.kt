package ee.ut.cs.mad.strideset.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.repository.ExerciseRepository
import ee.ut.cs.mad.strideset.data.repository.DataStoreSettingsRepository
import ee.ut.cs.mad.strideset.data.repository.RoomExerciseRepository
import ee.ut.cs.mad.strideset.data.repository.RoomWorkoutPlanRepository
import ee.ut.cs.mad.strideset.data.repository.WorkoutPlanRepository
import ee.ut.cs.mad.strideset.data.repository.SettingsRepository
import ee.ut.cs.mad.strideset.data.repository.RunRepository
import ee.ut.cs.mad.strideset.data.repository.RoomRunRepository
import ee.ut.cs.mad.strideset.running.GpsLocationSource
import ee.ut.cs.mad.strideset.running.LocationSource
import ee.ut.cs.mad.strideset.running.RunTracker

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** App-wide place where repositories are created (manual dependency injection). */
interface AppContainer {
    val exerciseRepository: ExerciseRepository
    val workoutPlanRepository: WorkoutPlanRepository
    val settingsRepository: SettingsRepository
    val runRepository: RunRepository
    val runTracker: RunTracker
    fun createRunLocationSource(): LocationSource
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    override val runTracker = RunTracker()
    override fun createRunLocationSource(): LocationSource = GpsLocationSource(appContext)
    override val runRepository: RunRepository by lazy { RoomRunRepository(database.runDao()) }
    private val database: StrideSetDatabase by lazy { StrideSetDatabase.create(context) }

    override val exerciseRepository: ExerciseRepository by lazy {
        RoomExerciseRepository(database.exerciseDao())
    }

    override val workoutPlanRepository: WorkoutPlanRepository by lazy {
        RoomWorkoutPlanRepository(database.workoutPlanDao())
    }
    override val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(context.applicationContext.settingsDataStore)
    }
}
