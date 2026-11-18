package ee.ut.cs.mad.strideset

import ee.ut.cs.mad.strideset.data.model.DistanceUnit
import ee.ut.cs.mad.strideset.ui.running.formatRunDistance
import ee.ut.cs.mad.strideset.ui.running.formatRunDuration
import ee.ut.cs.mad.strideset.ui.running.formatRunPace
import java.time.Duration
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class RunFormattingTest {
    @Test fun distanceAndPaceUseTheSelectedUnit() {
        assertEquals("1.00", formatRunDistance(1609.344, DistanceUnit.MILES, Locale.US))
        assertEquals("1.61", formatRunDistance(1609.344, DistanceUnit.KILOMETERS, Locale.US))
        assertEquals("10:00", formatRunPace(1609.344, Duration.ofMinutes(10), DistanceUnit.MILES))
        assertEquals("6:13", formatRunPace(1609.344, Duration.ofMinutes(10), DistanceUnit.KILOMETERS))
    }

    @Test fun paceIsUnavailableUntilBothTimeAndDistanceExist() {
        assertEquals("—", formatRunPace(0.0, Duration.ofMinutes(1), DistanceUnit.KILOMETERS))
        assertEquals("—", formatRunPace(50.0, Duration.ZERO, DistanceUnit.MILES))
    }

    @Test fun durationPreservesHoursAndPaceDoesNotWrapAfterAnHour() {
        assertEquals("01:02:03", formatRunDuration(Duration.ofSeconds(3723)))
        assertEquals("60:00", formatRunPace(1000.0, Duration.ofHours(1), DistanceUnit.KILOMETERS))
    }
}
