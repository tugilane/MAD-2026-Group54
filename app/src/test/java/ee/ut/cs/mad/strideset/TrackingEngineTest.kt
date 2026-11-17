package ee.ut.cs.mad.strideset

import ee.ut.cs.mad.strideset.running.GpsState
import ee.ut.cs.mad.strideset.running.LocationFix
import ee.ut.cs.mad.strideset.running.TrackingEngine
import ee.ut.cs.mad.strideset.running.FakeLocationSource
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test

class TrackingEngineTest {
    private var now = 0L
    private fun engine() = TrackingEngine(nowMillis = { now })
    private fun fix(latitude: Double = 0.0, longitude: Double = 0.0, accuracy: Double = 3.0, at: Long = now) =
        LocationFix(latitude, longitude, accuracy, at)

    @Test fun knownRouteAndMonotonicDuration() {
        val engine = engine()
        val source = FakeLocationSource()
        source.start(engine::accept) {}
        source.emit(fix())
        now = 10_000
        source.emit(fix(longitude = 0.0005))
        now = 20_000
        source.emit(fix(latitude = 0.0005, longitude = 0.0005))
        assertEquals(111.2, engine.snapshot().distanceMeters, 0.2)
        assertEquals(Duration.ofSeconds(20), engine.snapshot().activeDuration)
        assertEquals(GpsState.TRACKING, engine.snapshot().gpsState)
    }

    @Test fun stationaryJitterDoesNotAddDistance() {
        val engine = engine()
        engine.accept(fix())
        repeat(20) { index ->
            now += 1000
            engine.accept(fix(longitude = if (index % 2 == 0) 0.00001 else 0.0))
        }
        assertEquals(0.0, engine.snapshot().distanceMeters, 0.0)
    }

    @Test fun rejectsStaleInaccurateImpossibleAndOutOfOrderFixes() {
        val engine = engine()
        engine.accept(fix())
        now = 20_000
        engine.accept(fix(longitude = 0.0005, at = 1000))
        engine.accept(fix(longitude = 0.0005, accuracy = 31.0))
        engine.accept(fix(longitude = 1.0))
        engine.accept(fix(longitude = 0.0005, at = -1))
        engine.accept(fix(latitude = Double.NaN))
        assertEquals(0.0, engine.snapshot().distanceMeters, 0.0)
        engine.accept(fix(longitude = 0.0005))
        assertEquals(55.6, engine.snapshot().distanceMeters, 0.2)
    }

    @Test fun lostSignalReanchorsAcrossGaps() {
        val engine = engine()
        assertEquals(GpsState.SEARCHING, engine.snapshot().gpsState)
        engine.accept(fix())
        now = 30_001
        assertEquals(GpsState.SIGNAL_LOST, engine.snapshot().gpsState)
        engine.accept(fix(longitude = 0.01))
        assertEquals(0.0, engine.snapshot().distanceMeters, 0.0)
        now += 10_000
        engine.accept(fix(longitude = 0.0105))
        assertEquals(55.6, engine.snapshot().distanceMeters, 0.2)
    }

    @Test fun pauseFreezesProgressAndResumeClearsAnchor() {
        val engine = engine()
        engine.accept(fix())
        now = 10_000
        engine.pause()
        now = 100_000
        engine.accept(fix(longitude = 0.01))
        assertEquals(Duration.ofSeconds(10), engine.snapshot().activeDuration)
        assertEquals(0.0, engine.snapshot().distanceMeters, 0.0)
        engine.resume()
        engine.accept(fix(longitude = 0.02))
        now += 10_000
        engine.accept(fix(longitude = 0.0205))
        assertEquals(Duration.ofSeconds(20), engine.snapshot().activeDuration)
        assertEquals(55.6, engine.snapshot().distanceMeters, 0.2)
    }

    @Test fun recoveryStartsPausedWithCheckpointTotals() {
        val engine = TrackingEngine(100.0, Duration.ofSeconds(20), true) { now }
        now = 100_000
        assertEquals(Duration.ofSeconds(20), engine.snapshot().activeDuration)
        assertEquals(100.0, engine.snapshot().distanceMeters, 0.0)
        engine.resume()
        now += 5000
        assertEquals(Duration.ofSeconds(25), engine.snapshot().activeDuration)
    }
}
