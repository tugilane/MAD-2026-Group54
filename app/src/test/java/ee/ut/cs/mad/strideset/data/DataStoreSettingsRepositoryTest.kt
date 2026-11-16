package ee.ut.cs.mad.strideset.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.model.UnitSettings
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import ee.ut.cs.mad.strideset.data.repository.DataStoreSettingsRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var file: File
    private lateinit var storeJob: Job
    private lateinit var repository: DataStoreSettingsRepository

    @Before
    fun setUp() {
        file = File(temporaryFolder.root, "settings.preferences_pb")
        openRepository()
    }

    private fun openRepository() {
        storeJob = SupervisorJob()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + storeJob),
            produceFile = { file }
        )
        repository = DataStoreSettingsRepository(dataStore)
    }

    @After
    fun tearDown() = runBlocking {
        storeJob.cancelAndJoin()
    }

    @Test
    fun newStorageDefaultsToKilometersAndKilograms() = runBlocking {
        assertEquals(
            UnitSettings(DistanceUnit.KILOMETERS, WeightUnit.KILOGRAMS),
            repository.settings.first()
        )
    }

    @Test
    fun changingAndResettingEachUnitPreservesTheOther() = runBlocking {
        repository.setWeightUnit(WeightUnit.POUNDS)
        repository.setDistanceUnit(DistanceUnit.MILES)
        assertEquals(
            UnitSettings(DistanceUnit.MILES, WeightUnit.POUNDS),
            repository.settings.first()
        )

        repository.setDistanceUnit(DistanceUnit.KILOMETERS)
        assertEquals(
            UnitSettings(DistanceUnit.KILOMETERS, WeightUnit.POUNDS),
            repository.settings.first()
        )

        repository.setDistanceUnit(DistanceUnit.MILES)
        repository.setWeightUnit(WeightUnit.KILOGRAMS)
        assertEquals(
            UnitSettings(DistanceUnit.MILES, WeightUnit.KILOGRAMS),
            repository.settings.first()
        )
    }

    @Test
    fun choicesSurviveReopeningStorage() = runBlocking {
        repository.setDistanceUnit(DistanceUnit.MILES)
        repository.setWeightUnit(WeightUnit.POUNDS)

        storeJob.cancelAndJoin()
        openRepository()

        assertEquals(
            UnitSettings(DistanceUnit.MILES, WeightUnit.POUNDS),
            repository.settings.first()
        )
    }
}
