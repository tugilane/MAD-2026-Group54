package ee.ut.cs.mad.strideset.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    /** Returns the new row id. Throws SQLiteConstraintException if the name is already taken. */
    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long
}