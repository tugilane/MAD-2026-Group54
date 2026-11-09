package ee.ut.cs.mad.strideset.data.model

import java.time.Duration
import java.time.Instant

data class Run(
    val id: Long = 0,
    val startedAt: Instant,
    /** Null while the run is in progress. */
    val finishedAt: Instant? = null,
    /** Time spent running, excluding pauses. */
    val activeDuration: Duration = Duration.ZERO,
    val distanceMeters: Double = 0.0,
) {
    val isFinished: Boolean get() = finishedAt != null

    /** Seconds per kilometre, or null before any distance has been recorded. */
    val paceSecondsPerKm: Double?
        get() = if (distanceMeters > 0) activeDuration.seconds / (distanceMeters / 1000) else null
}