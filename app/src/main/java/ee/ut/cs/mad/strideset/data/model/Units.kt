package ee.ut.cs.mad.strideset.data.model

enum class DistanceUnit { KILOMETERS, MILES }

enum class WeightUnit { KILOGRAMS, POUNDS }

/** The user's display units. Data is always stored in kg and metres; these only change display and input. */
data class UnitSettings(
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
)

private const val KG_PER_LB = 0.45359237
private const val METERS_PER_MILE = 1609.344

/** Converts a stored kg value into this unit for display. */
fun WeightUnit.fromKg(kg: Double): Double = when (this) {
    WeightUnit.KILOGRAMS -> kg
    WeightUnit.POUNDS -> kg / KG_PER_LB
}

/** Converts a value the user typed in this unit into kg for storing. */
fun WeightUnit.toKg(value: Double): Double = when (this) {
    WeightUnit.KILOGRAMS -> value
    WeightUnit.POUNDS -> value * KG_PER_LB
}

/** Converts stored metres into this unit (km or mi) for display. */
fun DistanceUnit.fromMeters(meters: Double): Double = when (this) {
    DistanceUnit.KILOMETERS -> meters / 1000
    DistanceUnit.MILES -> meters / METERS_PER_MILE
}