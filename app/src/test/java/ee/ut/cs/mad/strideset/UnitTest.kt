package ee.ut.cs.mad.strideset

import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.model.WeightUnit
import ee.ut.cs.mad.strideset.data.model.UnitSettings
import ee.ut.cs.mad.strideset.data.model.toLb
import ee.ut.cs.mad.strideset.data.model.toKg
import ee.ut.cs.mad.strideset.data.model.toMiles
import ee.ut.cs.mad.strideset.data.model.toMeters
import junit.framework.TestCase.assertEquals
import org.junit.Test

class UnitTest {
    @Test
    fun kilometersAreConvertedToMeters() {
        assertEquals(1000.0, DistanceUnit.KILOMETERS.toMeters(1.0), 0.000001)
    }

    @Test
    fun milesAreConvertedToMeters() {
        assertEquals(1609.344, DistanceUnit.MILES.toMeters(1.0), 0.000001)
    }

    @Test
    fun metersAreConvertedToKilometersAndMiles() {
        assertEquals(1.5, DistanceUnit.KILOMETERS.toMiles(1500.0), 0.000001)
        assertEquals(1.0, DistanceUnit.MILES.toMiles(1609.344), 0.000001)
    }

    @Test
    fun weightUnitConversionsToAndFromKg() {
        // Kilograms
        assertEquals(10.0, WeightUnit.KILOGRAMS.toKg(10.0), 0.000001)
        assertEquals(10.0, WeightUnit.KILOGRAMS.toLb(10.0), 0.000001)

        // Pounds
        val pounds = 22.0462262185
        val expectedKg = 10.0
        assertEquals(expectedKg, WeightUnit.POUNDS.toKg(pounds), 0.001)
        assertEquals(pounds, WeightUnit.POUNDS.toLb(expectedKg), 0.001)
    }

    @Test
    fun unitSettingsDefaultsAndProperties() {
        val defaultSettings = UnitSettings()
        assertEquals(DistanceUnit.KILOMETERS, defaultSettings.distanceUnit)
        assertEquals(WeightUnit.KILOGRAMS, defaultSettings.weightUnit)

        val customSettings = UnitSettings(DistanceUnit.MILES, WeightUnit.POUNDS)
        assertEquals(DistanceUnit.MILES, customSettings.distanceUnit)
        assertEquals(WeightUnit.POUNDS, customSettings.weightUnit)

        val copiedSettings = customSettings.copy(distanceUnit = DistanceUnit.KILOMETERS)
        assertEquals(DistanceUnit.KILOMETERS, copiedSettings.distanceUnit)
        assertEquals(WeightUnit.POUNDS, copiedSettings.weightUnit)
    }
}
