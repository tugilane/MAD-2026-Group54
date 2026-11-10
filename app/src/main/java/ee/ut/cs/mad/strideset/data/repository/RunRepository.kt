package ee.ut.cs.mad.strideset.data.repository

import ee.ut.cs.mad.strideset.data.model.Run
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.LocalDate

interface RunRepository {
    /** The unfinished run, or null. */
    fun observeActiveRun(): Flow<Run?>

    fun observeRun(id: Long): Flow<Run?>

    /**
     * Starts a new run and returns its id.
     * @throws IllegalStateException if another run is already in progress
     */
    suspend fun startRun(): Long

    /** Saves progress. Call this every few seconds while tracking, so a killed app loses almost nothing. */
    suspend fun updateProgress(runId: Long, distanceMeters: Double, activeDuration: Duration)

    suspend fun finishRun(runId: Long)

    suspend fun discardRun(runId: Long)

    /** Finished runs that started between these days (inclusive, device time zone). For the Calendar. */
    fun observeFinishedRuns(start: LocalDate, endInclusive: LocalDate): Flow<List<Run>>
}