package ee.ut.cs.mad.strideset.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Insert
import androidx.room.Transaction

@Dao
interface RunDao {
    /** Finds the active run based on it being unfinished */
    @Query("SELECT * FROM runs WHERE finishedAtMillis IS NULL LIMIT 1")
    fun observeActiveRun(): Flow<RunEntity?>
    /** Finds a run based on its id in the database */
    @Query("SELECT * FROM runs WHERE id = :id")
    fun observeRun(id: Long): Flow<RunEntity?>

    @Query("SELECT * FROM runs WHERE finishedAtMillis IS NOT NULL AND startedAtMillis >= :start AND startedAtMillis < :endExclusive ORDER BY startedAtMillis DESC")
    fun observeFinishedRuns(start: Long, endExclusive: Long): Flow<List<RunEntity>>

    @Query("UPDATE runs SET distanceMeters = :meters, activeDurationSeconds = :seconds WHERE id = :id AND finishedAtMillis IS NULL AND distanceMeters <= :meters AND activeDurationSeconds <= :seconds")
    suspend fun updateProgress(id: Long, meters: Double, seconds: Long): Int

    @Query("UPDATE runs SET isPaused = :paused WHERE id = :id AND finishedAtMillis IS NULL")
    suspend fun setPaused(id: Long, paused: Boolean): Int

    @Query("UPDATE runs SET finishedAtMillis = :finishedAtMillis, isPaused = 0 WHERE id = :id AND finishedAtMillis IS NULL")
    suspend fun finish(id: Long, finishedAtMillis: Long): Int

    @Query("DELETE FROM runs WHERE id = :id AND finishedAtMillis IS NULL")
    suspend fun discard(id: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM runs WHERE finishedAtMillis IS NULL)")
    suspend fun hasUnfinishedRun(): Boolean

    @Insert
    suspend fun insert(run: RunEntity): Long

    @Transaction
    suspend fun startRun(startedAtMillis: Long): Long {
        check(!hasUnfinishedRun()) {
            "Another run is already in progress"
        }

        return insert(RunEntity(startedAtMillis = startedAtMillis))
    }
}
