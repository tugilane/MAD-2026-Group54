package ee.ut.cs.mad.strideset.data.repository

import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.model.UnitSettings
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** The current settings, re-emitted whenever they change. Defaults to km and kg. */
    val settings: Flow<UnitSettings>

    suspend fun setDistanceUnit(unit: DistanceUnit)

    suspend fun setWeightUnit(unit: WeightUnit)
}
