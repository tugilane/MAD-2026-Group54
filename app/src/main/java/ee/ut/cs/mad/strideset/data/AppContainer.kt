package ee.ut.cs.mad.strideset.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import ee.ut.cs.mad.strideset.data.local.StrideSetDatabase
import ee.ut.cs.mad.strideset.data.repository.ExerciseRepository
import ee.ut.cs.mad.strideset.data.repository.DataStoreSettingsRepository
import ee.ut.cs.mad.strideset.data.repository.RoomExerciseRepository
import ee.ut.cs.mad.strideset.data.repository.SettingsRepository

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** App-wide place where repositories are created (manual dependency injection). */
interface AppContainer {
    val exerciseRepository: ExerciseRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val database: StrideSetDatabase by lazy { StrideSetDatabase.create(context) }

    override val exerciseRepository: ExerciseRepository by lazy {
        RoomExerciseRepository(database.exerciseDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(context.applicationContext.settingsDataStore)
    }
}
