package ee.ut.cs.mad.strideset.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.model.UnitSettings
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val distanceInMilesKey = booleanPreferencesKey("distance_in_miles")
private val weightInPoundsKey = booleanPreferencesKey("weight_in_pounds")

class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    override val settings: Flow<UnitSettings> = dataStore.data.map { preferences ->
        UnitSettings(
            distanceUnit = if (preferences[distanceInMilesKey] == true) DistanceUnit.MILES
                else DistanceUnit.KILOMETERS,
            weightUnit = if (preferences[weightInPoundsKey] == true) WeightUnit.POUNDS
                else WeightUnit.KILOGRAMS
        )
    }

    override suspend fun setDistanceUnit(unit: DistanceUnit) {
        dataStore.edit { preferences ->
            preferences[distanceInMilesKey] = unit == DistanceUnit.MILES
        }
    }

    override suspend fun setWeightUnit(unit: WeightUnit) {
        dataStore.edit { preferences ->
            preferences[weightInPoundsKey] = unit == WeightUnit.POUNDS
        }
    }
}
