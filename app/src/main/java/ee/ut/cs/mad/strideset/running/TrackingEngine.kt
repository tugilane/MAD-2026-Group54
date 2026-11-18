package ee.ut.cs.mad.strideset.running

import java.time.Duration
import kotlin.math.*

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val elapsedRealtimeMillis: Long,
)

enum class GpsState { SEARCHING, TRACKING, SIGNAL_LOST }

data class TrackingProgress(
    val distanceMeters: Double,
    val activeDuration: Duration,
    val isPaused: Boolean,
    val gpsState: GpsState,
)

/** All time here is monotonic; wall-clock changes cannot alter a run's duration. */
class TrackingEngine(
    private var distanceMeters: Double = 0.0,
    activeDuration: Duration = Duration.ZERO,
    private var paused: Boolean = false,
    private val nowMillis: () -> Long,
) {
    private var accumulatedMillis = activeDuration.toMillis()
    private var resumedAt = nowMillis()
    private var anchor: LocationFix? = null
    private var lastAcceptedAt: Long? = null
    private var searchingSince = resumedAt

    fun snapshot(): TrackingProgress {
        val now = nowMillis()
        val silence = now - (lastAcceptedAt ?: searchingSince)
        val gps = when {
            silence > 30_000 -> GpsState.SIGNAL_LOST
            lastAcceptedAt == null -> GpsState.SEARCHING
            else -> GpsState.TRACKING
        }
        return TrackingProgress(
            distanceMeters,
            Duration.ofMillis(accumulatedMillis + if (paused) 0 else (now - resumedAt).coerceAtLeast(0)),
            paused,
            gps,
        )
    }

    fun pause() {
        if (paused) return
        accumulatedMillis = snapshot().activeDuration.toMillis()
        paused = true
        anchor = null
    }

    fun resume() {
        if (!paused) return
        paused = false
        resumedAt = nowMillis()
        searchingSince = resumedAt
        anchor = null
        lastAcceptedAt = null
    }

    fun accept(fix: LocationFix) {
        if (paused) return
        val age = nowMillis() - fix.elapsedRealtimeMillis
        if (age !in 0..10_000 || fix.elapsedRealtimeMillis < resumedAt ||
            !fix.latitude.isFinite() || !fix.longitude.isFinite() ||
            fix.latitude !in -90.0..90.0 || fix.longitude !in -180.0..180.0 ||
            !fix.accuracyMeters.isFinite() || fix.accuracyMeters !in 0.0..30.0) return
        if (lastAcceptedAt != null && fix.elapsedRealtimeMillis <= lastAcceptedAt!!) return

        val previous = anchor
        if (previous != null && fix.elapsedRealtimeMillis - lastAcceptedAt!! <= 30_000) {
            val meters = distance(previous, fix)
            val seconds = (fix.elapsedRealtimeMillis - previous.elapsedRealtimeMillis) / 1000.0
            if (meters / seconds > 10.0) return
            // Suppress tiny movements inside normal GPS jitter; calibrate outdoors before relaxing this.
            if (meters >= max(3.0, max(previous.accuracyMeters, fix.accuracyMeters) / 2)) {
                distanceMeters += meters
                anchor = fix
            }
        } else {
            anchor = fix
        }
        lastAcceptedAt = fix.elapsedRealtimeMillis
    }

    private fun distance(a: LocationFix, b: LocationFix): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val haversine = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 6_371_000 * 2 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
    }
}
