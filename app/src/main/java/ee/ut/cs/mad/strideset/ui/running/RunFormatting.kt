package ee.ut.cs.mad.strideset.ui.running

import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.data.model.toMiles
import java.time.Duration
import java.util.Locale
import kotlin.math.roundToLong

internal fun formatRunDuration(duration: Duration): String {
    val seconds = duration.seconds
    return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
}

internal fun formatRunDistance(meters: Double, unit: DistanceUnit, locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%.2f", unit.toMiles(meters))

internal fun formatRunPace(meters: Double, duration: Duration, unit: DistanceUnit): String {
    if (meters <= 0 || duration.isZero) return "—"
    val seconds = (duration.toMillis() / 1000.0 / unit.toMiles(meters)).roundToLong()
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
