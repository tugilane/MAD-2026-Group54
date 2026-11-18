package ee.ut.cs.mad.strideset.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import ee.ut.cs.mad.strideset.data.model.Run
import java.time.Duration
import java.time.Instant


@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startedAtMillis: Long,
    val finishedAtMillis: Long? = null,
    val activeDurationSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val isPaused: Boolean = false,
)
/** Converts databse row into the Run object used by the rest of the app */
fun RunEntity.toModel() = Run(
    id = id,
    startedAt = Instant.ofEpochMilli(startedAtMillis),
    finishedAt = finishedAtMillis?.let {Instant.ofEpochMilli(it)},
    activeDuration = Duration.ofSeconds(activeDurationSeconds),
    distanceMeters = distanceMeters,
    isPaused = isPaused
)